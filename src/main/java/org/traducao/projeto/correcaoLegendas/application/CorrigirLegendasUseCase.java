package org.traducao.projeto.correcaoLegendas.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.correcaoLegendas.domain.CorrecaoLegendasRelatorioJson;
import org.traducao.projeto.correcaoLegendas.domain.LogEventoCorrecaoLegendas;
import org.traducao.projeto.correcaoLegendas.domain.ResultadoCorrecaoLegendas;
import org.traducao.projeto.correcaoLegendas.infrastructure.CorrecaoLegendasLogPersistencia;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.traducao.infrastructure.legenda.MascaradorTags;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class CorrigirLegendasUseCase {

    private static final Logger log = LoggerFactory.getLogger(CorrigirLegendasUseCase.class);
    private static final String TIPO_OPERACAO = "Correcao de Legendas (.ass original->traduzida)";

    private final LeitorLegendaAss leitor;
    private final EscritorLegendaAss escritor;
    private final SanitizadorTagsService sanitizador;
    private final CorretorTraducaoLlmService corretorLlm;
    private final GerenciadorContexto gerenciadorContexto;
    private final TelemetriaService telemetriaService;
    private final CorrecaoLegendasLogPersistencia logPersistencia;
    private final DetectorEfeitoKaraokeService detectorKaraoke;
    private final PoliticaEstiloMusical politicaEstiloMusical;
    private final MascaradorTags mascarador;
    private final ProtecaoLegendaAssService protecaoAss;

    public CorrigirLegendasUseCase(
        LeitorLegendaAss leitor,
        EscritorLegendaAss escritor,
        SanitizadorTagsService sanitizador,
        CorretorTraducaoLlmService corretorLlm,
        GerenciadorContexto gerenciadorContexto,
        TelemetriaService telemetriaService,
        CorrecaoLegendasLogPersistencia logPersistencia,
        DetectorEfeitoKaraokeService detectorKaraoke,
        PoliticaEstiloMusical politicaEstiloMusical,
        MascaradorTags mascarador,
        ProtecaoLegendaAssService protecaoAss
    ) {
        this.leitor = leitor;
        this.escritor = escritor;
        this.sanitizador = sanitizador;
        this.corretorLlm = corretorLlm;
        this.gerenciadorContexto = gerenciadorContexto;
        this.telemetriaService = telemetriaService;
        this.logPersistencia = logPersistencia;
        this.detectorKaraoke = detectorKaraoke;
        this.politicaEstiloMusical = politicaEstiloMusical;
        this.mascarador = mascarador;
        this.protecaoAss = protecaoAss;
    }

    public ResultadoCorrecaoLegendas corrigirPasta(Path pastaBase, String contextoId) {
        return corrigirPasta(pastaBase, pastaBase, contextoId);
    }

    public ResultadoCorrecaoLegendas corrigirPasta(Path pastaOriginal, Path pastaTraduzida, String contextoId) {
        Instant inicio = Instant.now();
        List<LogEventoCorrecaoLegendas> eventos = new ArrayList<>();

        if (!Files.isDirectory(pastaOriginal) || !Files.isDirectory(pastaTraduzida)) {
            String msg = "Pastas não encontradas — esperava " + pastaOriginal + " e " + pastaTraduzida;
            out(eventos, "WARN", null, msg, AnsiCores.YELLOW);
            ResultadoCorrecaoLegendas resultado = new ResultadoCorrecaoLegendas(0, 0, 0, 0, 0, 0, 1, List.of(msg), null);
            registrarTelemetria(pastaOriginal, pastaTraduzida, inicio, false, null, resultado, eventos);
            return resultado;
        }

        boolean llmHabilitado = aplicarContextoLlm(contextoId);
        String contextoAtivo = llmHabilitado ? gerenciadorContexto.obterNomeContextoAtivo() : null;

        out(eventos, "INFO", null, "=== Iniciando Correcao de Legendas ===", AnsiCores.CYAN);
        out(eventos, "INFO", null, "Pasta original/ref: " + pastaOriginal, AnsiCores.WHITE);
        out(eventos, "INFO", null, "Pasta traduzida/alvo: " + pastaTraduzida, AnsiCores.WHITE);
        if (llmHabilitado) {
            out(eventos, "INFO", null, "Correcao de traducao via LLM ativa (contexto: "
                + contextoAtivo + ")", AnsiCores.CYAN);
        }

        int[] curados = {0};
        int[] falasCuradas = {0};
        int[] corrigidosLlm = {0};
        int[] semAlteracao = {0};
        int[] semPar = {0};
        int[] traducaoAusente = {0};
        List<String> erros = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(pastaOriginal)) {
            // Ignora os próprios arquivos traduzidos (*_PT-BR.ass): quando a
            // pasta original é a mesma da traduzida, tratá-los como "originais"
            // fazia cada um procurar um par *_PT-BR_PT-BR.ass inexistente e
            // inflar o contador "sem par" com ruído.
            List<Path> originais = stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".ass"))
                    .filter(p -> !ehLegendaTraduzida(p))
                    .toList();

            // Os originais são varridos recursivamente; o par traduzido também
            // precisa ser procurado em subpastas (ex.: Season 04\legendas_eng
            // vs Season 04\legendas_ptbr) — resolver só contra a raiz da pasta
            // traduzida dava "sem par" em acervos organizados por subpasta.
            Map<String, List<Path>> indiceTraduzidas = indexarLegendasTraduzidas(pastaTraduzida);

            for (Path arqOriginal : originais) {
                // Parada cooperativa (botão "Parar" da UI): arquivos já
                // corrigidos ficaram salvos; os restantes não são tocados.
                if (Thread.currentThread().isInterrupted()) {
                    out(eventos, "WARN", null,
                        "[STOP] Correção interrompida pelo usuário — arquivos restantes não processados.",
                        AnsiCores.YELLOW);
                    break;
                }
                corrigirArquivo(arqOriginal, pastaTraduzida, indiceTraduzidas, llmHabilitado, inicio, eventos,
                    curados, falasCuradas, corrigidosLlm, semAlteracao, semPar, traducaoAusente, erros);
            }
        } catch (IOException e) {
            log.error("Erro ao percorrer pasta original de legendas: {}", pastaOriginal, e);
            String msg = "Erro ao percorrer pasta original: " + e.getMessage();
            erros.add(msg);
            out(eventos, "ERROR", null, msg, AnsiCores.RED);
        }

        ResultadoCorrecaoLegendas resultado = new ResultadoCorrecaoLegendas(
            curados[0], falasCuradas[0], corrigidosLlm[0], semAlteracao[0], semPar[0], traducaoAusente[0], erros.size(), erros, null);
        resultado = registrarTelemetria(pastaOriginal, pastaTraduzida, inicio, llmHabilitado, contextoAtivo, resultado, eventos);

        if (erros.isEmpty()) {
            out(eventos, "INFO", null, "Correcao de legendas concluida: " + curados[0]
                + " arquivo(s) curado(s) (" + falasCuradas[0] + " fala(s)), " + corrigidosLlm[0] + " fala(s) corrigida(s) via LLM, "
                + semAlteracao[0] + " ja perfeito(s), " + traducaoAusente[0] + " fala(s) sem traducao.", AnsiCores.GREEN);
        } else {
            out(eventos, "WARN", null, "Correcao de legendas concluida com " + erros.size()
                + " erro(s): " + curados[0] + " arquivo(s) curado(s) (" + falasCuradas[0] + " fala(s)), " + corrigidosLlm[0] + " fala(s) corrigida(s) via LLM, "
                + semAlteracao[0] + " ja perfeito(s), " + traducaoAusente[0] + " fala(s) sem traducao.", AnsiCores.RED);
        }
        log.info("Correcao de legendas finalizada em {}: {} arquivo(s) curado(s) ({} fala(s)), {} corrigida(s) via LLM, {} sem alteração, {} sem par traduzido, {} fala(s) sem traducao, {} erro(s)",
            pastaOriginal.getFileName(), curados[0], falasCuradas[0], corrigidosLlm[0], semAlteracao[0], semPar[0], traducaoAusente[0], erros.size());
        return resultado;
    }

    private boolean ehLegendaTraduzida(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        return nome.contains("_pt-br") || nome.contains("_ptbr");
    }

    /**
     * Define o contexto ativo (lore/system prompt) usado pelo MistralPort quando
     * a correção via LLM está habilitada. Sem contextoId, a correção permanece
     * 100% estrutural/regex (sem chamadas ao LLM).
     */
    private boolean aplicarContextoLlm(String contextoId) {
        if (contextoId == null || contextoId.isBlank()) {
            return false;
        }
        if (!gerenciadorContexto.existeContexto(contextoId)) {
            System.out.println(AnsiCores.YELLOW + "Contexto desconhecido \"" + contextoId
                + "\" — cura seguirá apenas estrutural (sem LLM)." + AnsiCores.RESET);
            return false;
        }
        gerenciadorContexto.definirContextoAtivo(contextoId);
        return true;
    }

    private void corrigirArquivo(
        Path arqOriginal,
        Path pastaTraduzida,
        Map<String, List<Path>> indiceTraduzidas,
        boolean llmHabilitado,
        Instant inicio,
        List<LogEventoCorrecaoLegendas> eventos,
        int[] curados,
        int[] falasCuradas,
        int[] corrigidosLlm,
        int[] semAlteracao,
        int[] semPar,
        int[] traducaoAusente,
        List<String> erros
    ) {
        String nomeOriginal = arqOriginal.getFileName().toString();
        Path arqTraduzido = localizarArquivoTraduzido(arqOriginal, pastaTraduzida, indiceTraduzidas);

        if (!Files.exists(arqTraduzido)) {
            semPar[0]++;
            out(eventos, "WARN", nomeOriginal, "Sem legenda traduzida pareada para " + nomeOriginal, AnsiCores.YELLOW);
            return;
        }

        try {
            DocumentoLegenda docOriginal = leitor.ler(arqOriginal);
            DocumentoLegenda docTraduzido = leitor.ler(arqTraduzido);

            if (docOriginal.eventos().size() != docTraduzido.eventos().size()) {
                // As legendas não estão alinhadas 1:1 (ex.: original foi re-extraído
                // depois da tradução). Tentar curar por posição aqui arrisca cortar
                // ou embaralhar falas sem nenhum aviso — mais seguro recusar e avisar
                // do que gravar um arquivo truncado.
                String msg = arqTraduzido.getFileName() + ": contagem de eventos não corresponde ("
                    + docOriginal.eventos().size() + " no original vs " + docTraduzido.eventos().size()
                    + " na tradução) — arquivo pulado, nenhuma alteração feita.";
                log.warn(msg);
                out(eventos, "WARN", arqTraduzido.getFileName().toString(), "[Pulado] " + msg, AnsiCores.YELLOW);
                erros.add(msg);
                return;
            }

            boolean houveModificacao = false;
            int linhasCuradas = 0;
            int linhasCorrigidasLlm = 0;
            List<EventoLegenda> novosEventos = new ArrayList<>(docTraduzido.eventos().size());
            Map<String, String> cacheCorrecaoMasc = new HashMap<>();

            for (int i = 0; i < docOriginal.eventos().size(); i++) {
                EventoLegenda evtOriginal = docOriginal.eventos().get(i);
                EventoLegenda evtTraduzido = docTraduzido.eventos().get(i);

                if (evtOriginal.isDialogo() && evtTraduzido.isDialogo()
                    && evtOriginal.temTexto() && evtTraduzido.temTexto()) {
                    String textoOriginal = evtOriginal.texto();
                    String textoPtBrAntigo = evtTraduzido.texto();

                    if (!textoOriginal.isBlank() && textoPtBrAntigo.isBlank()) {
                        // Original tem fala real, mas a traducao chegou vazia (falha
                        // silenciosa de um passo anterior do pipeline). Gravar aqui so
                        // o prefixo de tags da original criaria uma legenda "corrigida"
                        // sem nenhum texto — reportar como pendente em vez de mascarar.
                        traducaoAusente[0]++;
                        novosEventos.add(evtTraduzido);
                        out(eventos, "WARN", arqTraduzido.getFileName().toString(),
                            "Fala " + (i + 1) + " sem traducao (vazia) — original possui texto; nao corrigido, revisao manual necessaria.",
                            AnsiCores.YELLOW);
                        continue;
                    }

                    String textoCurado = sanitizador.curarTags(textoOriginal, textoPtBrAntigo);
                    boolean corrigidoPorLlm = false;

                    // Karaokê protegido (japonês/romaji): a referência original é
                    // imutável. Se a tradução alterou a linha (ex.: 86 T1, romaji
                    // com tags leves "traduzido" pelo LLM), restaura a original.
                    if (!textoOriginal.equals(textoPtBrAntigo)
                        && detectorKaraoke.devePreservarKaraokeOriginal(evtOriginal.estilo(), textoOriginal)) {
                        textoCurado = textoOriginal;
                        out(eventos, "INFO", arqTraduzido.getFileName().toString(),
                            "Fala " + (i + 1) + ": karaokê japonês/romaji restaurado da legenda original.",
                            AnsiCores.CYAN);
                    }

                    // A cura estrutural só restaura o prefixo; timing de karaokê
                    // (\k por sílaba) perdido no meio da linha não tem restauração
                    // automática — sinaliza para revisão manual no Aegisub.
                    int karaokeOriginal = sanitizador.contarTagsKaraoke(textoOriginal);
                    int karaokeTraduzido = sanitizador.contarTagsKaraoke(textoCurado);
                    if (karaokeOriginal > 0 && karaokeTraduzido < karaokeOriginal) {
                        out(eventos, "WARN", arqTraduzido.getFileName().toString(),
                            "Fala " + (i + 1) + ": original tem " + karaokeOriginal
                                + " tag(s) de karaoke (\\k), traducao tem " + karaokeTraduzido
                                + " — timing por silaba possivelmente perdido; revise manualmente.",
                            AnsiCores.YELLOW);
                    }

                    if (llmHabilitado) {
                        MascaradorTags.Mascarado mascOriginal = mascarador.mascarar(textoOriginal);
                        String textoMascOriginal = mascOriginal.texto();

                        if (deveIgnorarCuraLlm(evtOriginal, textoOriginal)) {
                            // Letreiros e karaokê pulam a correção estrutural/semântica da LLM
                        } else if (cacheCorrecaoMasc.containsKey(textoMascOriginal)) {
                            String respostaMascCorrigida = cacheCorrecaoMasc.get(textoMascOriginal);
                            if (!respostaMascCorrigida.equals(textoMascOriginal)) {
                                try {
                                    String textoCorrigidoCache = mascarador.desmascarar(respostaMascCorrigida, mascOriginal.tags());
                                    textoCurado = sanitizador.curarTags(textoOriginal, textoCorrigidoCache);
                                    corrigidoPorLlm = true;
                                    out(eventos, "INFO", arqTraduzido.getFileName().toString(),
                                        "Fala " + (i + 1) + " corrigida via LLM (Reutilizando cache local).", AnsiCores.MAGENTA);
                                } catch (AlucinacaoDetectadaException e) {
                                    out(eventos, "WARN", arqTraduzido.getFileName().toString(),
                                        "Fala " + (i + 1) + ": cache local ignorado por marcadores de tags incompatíveis.",
                                        AnsiCores.YELLOW);
                                }
                            }
                        } else {
                            Optional<String> corrigidoLlm = corretorLlm.corrigirSeNecessario(textoOriginal, textoCurado);
                            if (corrigidoLlm.isPresent()) {
                                // Passagem estrutural final: garante que a retradução do LLM
                                // não perdeu/alucinou as tags de formatação do original.
                                textoCurado = sanitizador.curarTags(textoOriginal, corrigidoLlm.get());
                                corrigidoPorLlm = true;
                                out(eventos, "INFO", arqTraduzido.getFileName().toString(),
                                    "Fala " + (i + 1) + " corrigida via LLM apos validacao.", AnsiCores.MAGENTA);

                                MascaradorTags.Mascarado mascNova = mascarador.mascarar(corrigidoLlm.get());
                                cacheCorrecaoMasc.put(textoMascOriginal, mascNova.texto());
                            } else {
                                cacheCorrecaoMasc.put(textoMascOriginal, textoMascOriginal);
                            }
                        }
                    }

                    if (!textoPtBrAntigo.equals(textoCurado)) {
                        novosEventos.add(evtTraduzido.comTexto(textoCurado));
                        houveModificacao = true;
                        if (corrigidoPorLlm) {
                            linhasCorrigidasLlm++;
                        } else {
                            linhasCuradas++;
                        }
                    } else {
                        novosEventos.add(evtTraduzido);
                    }
                } else {
                    if (evtOriginal.isDialogo() && evtOriginal.temTexto() && !evtOriginal.texto().isBlank()
                        && (!evtTraduzido.isDialogo() || !evtTraduzido.temTexto())) {
                        // Original tem fala real na posicao i, mas o evento traduzido no
                        // mesmo indice nao e reconhecido como dialogo/texto (tipo de linha
                        // divergente). Contagem de eventos bateu, mas o alinhamento 1:1
                        // pode estar quebrado — melhor avisar do que corrigir as cegas.
                        out(eventos, "WARN", arqTraduzido.getFileName().toString(),
                            "Fala " + (i + 1) + ": estrutura do evento traduzido diverge da original (tipo/texto ausente) — mantido sem alteracao.",
                            AnsiCores.YELLOW);
                    }
                    novosEventos.add(evtTraduzido);
                }
            }

            if (houveModificacao) {
                DocumentoLegenda documentoCurado = new DocumentoLegenda(
                    docTraduzido.cabecalho(),
                    novosEventos,
                    docTraduzido.quebraDeLinha(),
                    docTraduzido.comBom()
                );
                escritor.escrever(arqTraduzido, documentoCurado);
                curados[0]++;
                falasCuradas[0] += linhasCuradas;
                corrigidosLlm[0] += linhasCorrigidasLlm;
                out(eventos, "INFO", arqTraduzido.getFileName().toString(), "[Corrigido] "
                    + arqTraduzido.getFileName() + " (" + linhasCuradas + " tags restauradas, "
                    + linhasCorrigidasLlm + " corrigidas via LLM)", AnsiCores.GREEN);
            } else {
                semAlteracao[0]++;
                out(eventos, "INFO", arqTraduzido.getFileName().toString(), "[OK] "
                    + arqTraduzido.getFileName() + " (sem alteracao)", AnsiCores.DIM);
            }

        } catch (Exception e) {
            String msg = "Falha ao curar " + arqTraduzido.getFileName() + ": " + e.getMessage();
            log.error(msg, e);
            out(eventos, "ERROR", arqTraduzido.getFileName().toString(), "[Erro] " + msg, AnsiCores.RED);
            erros.add(msg);
        }
    }

    private boolean deveIgnorarCuraLlm(EventoLegenda evento, String texto) {
        if (detectorKaraoke.devePreservarKaraokeOriginal(evento.estilo(), texto)) {
            return true;
        }
        if (evento.estilo() != null
            && politicaEstiloMusical.estiloIgnorado(evento.estilo())
            && !detectorKaraoke.eKaraokeOuMusicaTraduzivel(evento.estilo(), texto)) {
            return true;
        }
        if (detectorKaraoke.eEfeitoKaraoke(texto)
            && !detectorKaraoke.eKaraokeOuMusicaTraduzivel(evento.estilo(), texto)) {
            return true;
        }
        if (protecaoAss.deveIgnorarIntervencaoIa(evento.estilo(), texto)) {
            return true;
        }
        String estilo = evento.estilo() != null ? evento.estilo().toLowerCase() : "";
        if (estilo.contains("sign")) {
            return true;
        }
        return false;
    }

    private Path localizarArquivoTraduzido(
        Path arqOriginal, Path pastaTraduzida, Map<String, List<Path>> indiceTraduzidas
    ) {
        String nomeOriginal = arqOriginal.getFileName().toString();
        String nomeBase = nomeOriginal.substring(0, nomeOriginal.lastIndexOf("."));
        Set<String> candidatos = new LinkedHashSet<>();

        candidatos.add(nomeBase + "_PT-BR.ass");
        candidatos.add(nomeBase + "_PTBR.ass");
        candidatos.add(nomeBase.replace("_ENG", "_PT-BR") + ".ass");
        candidatos.add(nomeBase.replace("_ENG", "_PTBR") + ".ass");
        candidatos.add(nomeBase.replace("_EN", "_PT-BR") + ".ass");
        candidatos.add(nomeBase.replace("_EN", "_PTBR") + ".ass");
        candidatos.add(nomeBase.replace("_eng", "_PT-BR") + ".ass");
        candidatos.add(nomeBase.replace("_eng", "_PTBR") + ".ass");
        candidatos.add(nomeBase.replace("_en", "_PT-BR") + ".ass");
        candidatos.add(nomeBase.replace("_en", "_PTBR") + ".ass");

        for (String candidato : candidatos) {
            // 1. Ao lado do próprio original (pastas mistas EN+PT).
            Path aoLado = arqOriginal.resolveSibling(candidato);
            if (Files.exists(aoLado)) {
                return aoLado;
            }
            // 2. Raiz da pasta traduzida (layout plano, comportamento original).
            Path naRaiz = pastaTraduzida.resolve(candidato);
            if (Files.exists(naRaiz)) {
                return naRaiz;
            }
            // 3. Qualquer subpasta da pasta traduzida, via índice pré-computado.
            List<Path> nomeIgual = indiceTraduzidas.get(candidato.toLowerCase());
            if (nomeIgual != null && !nomeIgual.isEmpty()) {
                return nomeIgual.get(0);
            }
        }

        return pastaTraduzida.resolve(nomeBase + "_PT-BR.ass");
    }

    /**
     * Indexa (nome de arquivo em minúsculas -> caminhos) todas as legendas
     * traduzidas sob a pasta, para o pareamento funcionar em acervos
     * organizados por subpasta sem varrer o disco a cada original.
     */
    private Map<String, List<Path>> indexarLegendasTraduzidas(Path pastaTraduzida) {
        Map<String, List<Path>> indice = new HashMap<>();
        try (Stream<Path> stream = Files.walk(pastaTraduzida)) {
            stream.filter(Files::isRegularFile)
                .filter(p -> p.toString().toLowerCase().endsWith(".ass"))
                .filter(this::ehLegendaTraduzida)
                .forEach(p -> indice
                    .computeIfAbsent(p.getFileName().toString().toLowerCase(), k -> new ArrayList<>())
                    .add(p));
        } catch (IOException e) {
            log.warn("Falha ao indexar legendas traduzidas em {}: {}", pastaTraduzida, e.getMessage());
        }
        return indice;
    }

    private ResultadoCorrecaoLegendas registrarTelemetria(
        Path pastaOriginal,
        Path pastaTraduzida,
        Instant inicio,
        boolean llmHabilitado,
        String contexto,
        ResultadoCorrecaoLegendas resultado,
        List<LogEventoCorrecaoLegendas> eventos
    ) {
        long tempoTotalMs = Duration.between(inicio, Instant.now()).toMillis();
        // Unidade única: FALAS. Antes somava curados (arquivos) com corrigidosLlm
        // (falas), o que distorcia a taxa de sucesso exibida no painel.
        int itensDetectados = resultado.falasCuradas() + resultado.corrigidosLlm() + resultado.traducaoAusente();
        int itensCorrigidos = resultado.falasCuradas() + resultado.corrigidosLlm();
        OperacaoTelemetria operacao = TelemetriaService.criarOperacao(
            TIPO_OPERACAO,
            "Original: " + pastaOriginal + " | Traduzida: " + pastaTraduzida,
            tempoTotalMs,
            resultado.totalArquivosAnalisados(),
            itensDetectados,
            itensCorrigidos
        );

        String relatorioJson = null;
        try {
            CorrecaoLegendasRelatorioJson relatorio = new CorrecaoLegendasRelatorioJson(
                operacao,
                pastaOriginal.toString(),
                pastaTraduzida.toString(),
                llmHabilitado,
                contexto,
                resultado,
                List.copyOf(eventos)
            );
            relatorioJson = logPersistencia.salvarRelatorioJson(pastaTraduzida, relatorio).toString();
            out(eventos, "INFO", null, "Relatorio JSON salvo em: " + relatorioJson, AnsiCores.CYAN);
        } catch (Exception e) {
            log.warn("Falha ao salvar relatorio JSON de correcao de legendas: {}", e.getMessage());
        }

        telemetriaService.registrarOperacao(operacao);
        telemetriaService.salvar(TelemetriaService.resolverPastaRelatorios(pastaTraduzida));
        return new ResultadoCorrecaoLegendas(
            resultado.curados(),
            resultado.falasCuradas(),
            resultado.corrigidosLlm(),
            resultado.semAlteracao(),
            resultado.semPar(),
            resultado.traducaoAusente(),
            resultado.totalErros(),
            resultado.erros(),
            relatorioJson
        );
    }

    // O console web carimba a hora local no navegador; a linha sai sem prefixo
    // de relógio. O instante UTC preciso segue registrado no campo próprio de
    // cada LogEventoCorrecaoLegendas persistido.
    private void out(
        List<LogEventoCorrecaoLegendas> eventos,
        String nivel,
        String arquivo,
        String mensagem,
        String cor
    ) {
        System.out.println(cor + mensagem + AnsiCores.RESET);
        eventos.add(new LogEventoCorrecaoLegendas(Instant.now().toString(), nivel, arquivo, mensagem));
        if ("ERROR".equals(nivel)) {
            log.error(mensagem);
        } else if ("WARN".equals(nivel)) {
            log.warn(mensagem);
        } else {
            log.info(mensagem);
        }
    }
}
