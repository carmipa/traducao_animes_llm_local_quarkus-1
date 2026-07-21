package org.traducao.projeto.renomearArquivos.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.core.util.ArquivoAtomicoUtil;
import org.traducao.projeto.core.util.DuracaoUtil;
import org.traducao.projeto.renomearArquivos.domain.OperacaoRenomeacao;
import org.traducao.projeto.renomearArquivos.domain.ResultadoRenomeacao;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.core.presentation.web.LogStreamService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: padroniza nomes de vídeos e legendas de uma pasta local,
 * oferecendo pré-visualização, aplicação segura e reversão persistente.
 *
 * <p>INVARIANTES DO DOMÍNIO: extensões são preservadas; nenhum destino pode sair
 * da pasta escolhida; arquivos existentes nunca são sobrescritos; cada pasta
 * admite uma operação por vez; toda aplicação começa com manifesto recuperável.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: itens já concluídos permanecem registrados
 * no manifesto, conflitos são preservados como pendências e nenhuma exceção de
 * validação realiza alterações em disco.
 */
@ApplicationScoped
public class RenomeadorUseCase {
    private static final Logger log = LoggerFactory.getLogger(RenomeadorUseCase.class);
    private static final Path PASTA_UNDO_PROJETO =
        TelemetriaService.resolverPastaArtefatosOperacionais("renomear-arquivos").resolve("undo");
    private static final String PREFIXO_ARQUIVO_UNDO = "kronos_undo_renomeacao_";
    private static final int TAMANHO_MAXIMO_NOME_PADRAO = 180;

    // Marcador canônico de tracker "S01E02" (com separador opcional): é a origem
    // MAIS confiável do número do episódio e precisa ter prioridade sobre o
    // fallback numérico. O EPISODE_LABEL_PATTERN abaixo NÃO o captura porque o
    // "E" dentro de "01E02" não tem fronteira de palavra (\b) após o dígito.
    private static final Pattern SEASON_EPISODE_PATTERN = Pattern.compile(
        "(?i)\\bS(\\d{1,2})[\\s._]?E(\\d{1,4})(?=$|[\\s._\\-\\[(]|v\\d)");
    private static final Pattern EPISODE_SEPARATOR_PATTERN = Pattern.compile(
        "(?:^|[\\s._])[-–—][\\s._]*(\\d{1,4})(?=$|[\\s._(\\[]|v\\d)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EPISODE_LABEL_PATTERN = Pattern.compile(
        "\\b(?:Ep|Epis[oó]dio|Episode|E)\\s*(\\d{1,4})(?=$|[\\s._(\\[]|v\\d)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EPISODE_FALLBACK = Pattern.compile("(?<!\\d)(\\d{1,4})(?!\\d)");
    private static final Pattern TEMPORADA_PATTERN = Pattern.compile(
        "(?i)\\b(?:season|temporada|temp(?:orada)?|s)\\s*[-_. ]?(\\d{1,2})\\b");
    private static final Pattern CONTEUDO_ESPECIAL_PATTERN = Pattern.compile(
        "(?i)(?:^|[\\s._\\-\\[(])(?:NC(?:OP|ED)\\d*|OVA|OAD|SP\\d*|PV\\d*|Menu|Preview|Special)(?=$|[\\s._\\-\\])(])");
    private static final Pattern CARACTERE_CONTROLE = Pattern.compile("[\\p{Cc}]");
    private static final Pattern TRAVERSAL_CAMINHO = Pattern.compile("(?:^|[/\\\\])\\.\\.(?:[/\\\\]|$)");
    private static final Pattern SEPARADOR_EDITORIAL_INVALIDO_WINDOWS = Pattern.compile("\\s*[:/\\\\|]+\\s*");
    private static final Pattern PONTUACAO_INVALIDA_WINDOWS = Pattern.compile("[<>\"?*]");
    private static final Pattern NOME_RESERVADO_WINDOWS = Pattern.compile(
        "(?i)^(?:CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?$");
    private static final Pattern DESCRITOR_TRACK = Pattern.compile("(?i)\\bTrack\\s*([0-9]+)\\b");
    private static final Pattern DESCRITOR_IDIOMA = Pattern.compile(
        "(?i)(?:^|[\\s._-])(PT[-_ ]?BR|PTBR|ENG(?:LISH)?|EN|FORCED|SIGNS)(?=$|[\\s._-])");

    private static final List<String> EXTENSOES_VIDEO = List.of(
        ".mkv", ".mp4", ".avi", ".webm", ".mov", ".m4v", ".wmv", ".flv", ".ts");
    private static final List<String> EXTENSOES_LEGENDA = List.of(
        ".ass", ".ssa", ".srt", ".vtt", ".sub");
    private static final ConcurrentMap<String, ReentrantLock> BLOQUEIOS_POR_PASTA = new ConcurrentHashMap<>();

    @Inject
    ObjectMapper objectMapper;

    @Inject
    TelemetriaService telemetriaService;

    @Inject
    LogStreamService logStream;

    /**
     * PROPÓSITO DE NEGÓCIO: mantém compatibilidade com consumidores que precisam
     * somente da lista do dry-run, usando temporada inferida ou temporada 1.
     *
     * <p>INVARIANTES DO DOMÍNIO: não altera nenhum arquivo e devolve apenas
     * destinos validados dentro da pasta.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança erro de validação ou conflito de
     * operação antes de modificar o disco.
     */
    public List<OperacaoRenomeacao.ItemRenomeado> simularRenomeacao(Path pasta, String nomePadrao) {
        return simularComResultado(pasta, nomePadrao, null).itens();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: calcula e relata todo o plano de renomeação sem
     * alterar a coleção de mídia.
     *
     * <p>INVARIANTES DO DOMÍNIO: a temporada fica entre 1 e 99; conflitos e
     * arquivos ignorados são contabilizados, nunca sobrescritos.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada inválida gera exceção didática;
     * pasta ocupada gera conflito HTTP por meio do controller.
     */
    public ResultadoRenomeacao simularComResultado(Path pasta, String nomePadrao, Integer temporadaSolicitada) {
        Path pastaSegura = validarPasta(pasta);
        String padraoSeguro = validarNomePadrao(nomePadrao);
        int temporada = resolverTemporada(temporadaSolicitada, padraoSeguro);
        return executarComBloqueio(pastaSegura, () -> {
            long inicioMs = System.currentTimeMillis();
            PlanoRenomeacao plano = simularInterno(pastaSegura, padraoSeguro, temporada);
            String status = plano.conflitos() > 0 ? "CONCLUIDO_COM_PENDENCIAS" : "CONCLUIDO";
            String mensagem = "Simulação concluída: " + plano.itens().size() + " arquivo(s) seriam renomeados"
                + (plano.conflitos() > 0 ? "; " + plano.conflitos() + " conflito(s) exigem revisão." : ".");
            logStream.publicarLog("renomear-arquivos", mensagem);
            logStream.publicarLog("renomear-arquivos",
                DuracaoUtil.linhaRelatorioFinal("Renomear Arquivos (simulação)", inicioMs));
            return resultado("SIMULACAO", status, plano, 0, 0, plano.conflitos(), mensagem, plano.itens());
        });
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aplica o plano com temporada inferida ou padrão 1,
     * preservando a API Java usada pelos testes e integrações existentes.
     *
     * <p>INVARIANTES DO DOMÍNIO: o manifesto precede o primeiro movimento e
     * somente destinos livres são usados.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorna status parcial e mantém no
     * manifesto somente os movimentos efetivamente aplicados.
     */
    public ResultadoRenomeacao aplicarRenomeacao(Path pasta, String nomePadrao) {
        return aplicarRenomeacao(pasta, nomePadrao, null);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: renomeia em lote vídeos e legendas aprovados pelo
     * plano determinístico da opção 13.
     *
     * <p>INVARIANTES DO DOMÍNIO: nenhuma mídia é movida sem manifesto prévio;
     * extensão, pasta e ausência de sobrescrita são verificadas novamente.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: continua nos demais itens, registra
     * falhas na telemetria e conserva undo suficiente para os sucessos.
     */
    public ResultadoRenomeacao aplicarRenomeacao(Path pasta, String nomePadrao, Integer temporadaSolicitada) {
        Path pastaSegura = validarPasta(pasta);
        String padraoSeguro = validarNomePadrao(nomePadrao);
        int temporada = resolverTemporada(temporadaSolicitada, padraoSeguro);
        return executarComBloqueio(pastaSegura,
            () -> aplicarInterno(pastaSegura, padraoSeguro, temporada));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: desfaz a última aplicação conhecida para a pasta,
     * inclusive retomando uma reversão que tenha sido interrompida.
     *
     * <p>INVARIANTES DO DOMÍNIO: o manifesto deve pertencer à mesma pasta; itens
     * já revertidos são reconhecidos; conflitos nunca sobrescrevem originais.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: o manifesto é regravado apenas com os
     * itens pendentes e só é removido quando todos estiverem resolvidos.
     */
    public ResultadoRenomeacao reverterRenomeacao(Path pasta) {
        Path pastaSegura = validarPasta(pasta);
        return executarComBloqueio(pastaSegura, () -> reverterInterno(pastaSegura));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: executa uma aplicação já validada e produz um resumo
     * final adequado ao painel e ao dataset de telemetria.
     *
     * <p>INVARIANTES DO DOMÍNIO: o plano completo é persistido antes do primeiro
     * move; o manifesto final contém somente sucessos ainda reversíveis.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: falha ao criar o manifesto aborta tudo;
     * falhas por item não eliminam a capacidade de desfazer os sucessos.
     */
    private ResultadoRenomeacao aplicarInterno(Path pasta, String nomePadrao, int temporada) {
        long inicioMs = System.currentTimeMillis();
        logStream.publicarLog("renomear-arquivos", "Iniciando APLICAÇÃO segura em: " + pasta);
        PlanoRenomeacao plano = simularInterno(pasta, nomePadrao, temporada);
        if (plano.itens().isEmpty()) {
            String status = plano.conflitos() > 0 ? "CONCLUIDO_COM_PENDENCIAS" : "SEM_ALTERACOES";
            String mensagem = plano.conflitos() > 0
                ? "Nenhum arquivo renomeado: existem conflitos que precisam ser resolvidos."
                : "Nenhum arquivo precisa ser renomeado.";
            registrarTelemetria("Renomear Arquivos", status, inicioMs, plano, 0, plano.conflitos());
            publicarFinal("Renomear Arquivos (aplicação)", inicioMs, status, 0, plano.conflitos());
            return resultado("APLICACAO", status, plano, 0, 0, plano.conflitos(), mensagem, List.of());
        }

        Path arquivoUndo = resolverArquivoUndo(pasta);
        if (!salvarManifesto(pasta, plano.itens())) {
            String mensagem = "Aplicação abortada: não foi possível preparar o manifesto de reversão; nenhuma mídia foi alterada.";
            logStream.publicarLog("renomear-arquivos", "[ERRO FATAL] " + mensagem);
            registrarTelemetria("Renomear Arquivos", "FALHOU", inicioMs, plano, 0, 1);
            publicarFinal("Renomear Arquivos (aplicação)", inicioMs, "FALHOU", 0, 1);
            return resultado("APLICACAO", "FALHOU", plano, 0, 1, plano.itens().size(), mensagem, List.of());
        }
        logStream.publicarLog("renomear-arquivos", "Manifesto preventivo preparado em: " + arquivoUndo);

        List<OperacaoRenomeacao.ItemRenomeado> aplicados = new ArrayList<>();
        int falhas = 0;
        for (int indice = 0; indice < plano.itens().size(); indice++) {
            OperacaoRenomeacao.ItemRenomeado item = plano.itens().get(indice);
            Path origem = resolverDestinoSeguro(pasta, item.nomeOriginal());
            Path destino = resolverDestinoSeguro(pasta, item.nomeNovo());
            logStream.publicarLog("renomear-arquivos", "[RENOMEANDO " + (indice + 1) + "/"
                + plano.itens().size() + "] " + item.nomeOriginal() + " -> " + item.nomeNovo());
            try {
                if (!Files.isRegularFile(origem)) {
                    throw new IOException("arquivo de origem não encontrado ou deixou de ser arquivo regular");
                }
                if (Files.exists(destino)) {
                    throw new IOException("arquivo de destino já existe");
                }
                moverSemSobrescrever(origem, destino);
                aplicados.add(item);
                logStream.publicarLog("renomear-arquivos", "[OK " + (indice + 1) + "/"
                    + plano.itens().size() + "] " + item.nomeNovo());
            } catch (IOException e) {
                falhas++;
                logStream.publicarLog("renomear-arquivos", "[FALHA " + (indice + 1) + "/"
                    + plano.itens().size() + "] " + item.nomeOriginal() + " — " + e.getMessage());
            }
        }

        if (aplicados.isEmpty()) {
            apagarManifesto(arquivoUndo);
        } else if (!salvarManifesto(pasta, aplicados)) {
            logStream.publicarLog("renomear-arquivos",
                "[ATENÇÃO] Não foi possível compactar o manifesto; o plano preventivo completo foi preservado e continua reversível.");
        }

        telemetriaService.registrarArquivosSanitizados(aplicados.size());
        int pendentes = falhas + plano.conflitos();
        String status = falhas > 0 ? "CONCLUIDO_COM_FALHAS"
            : plano.conflitos() > 0 ? "CONCLUIDO_COM_PENDENCIAS" : "CONCLUIDO";
        String mensagem = "Renomeação " + status.toLowerCase(Locale.ROOT).replace('_', ' ')
            + ": " + aplicados.size() + " concluído(s), " + falhas + " falha(s), "
            + plano.conflitos() + " conflito(s).";
        registrarTelemetria("Renomear Arquivos", status, inicioMs, plano, aplicados.size(), pendentes);
        publicarFinal("Renomear Arquivos (aplicação)", inicioMs, status, aplicados.size(), pendentes);
        return resultado("APLICACAO", status, plano, aplicados.size(), falhas, pendentes, mensagem, aplicados);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: retoma ou conclui a reversão descrita no manifesto da
     * pasta sem repetir como erro o que já foi revertido.
     *
     * <p>INVARIANTES DO DOMÍNIO: ambos os lados de cada mapeamento permanecem na
     * pasta original e um original existente jamais é substituído.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: itens ausentes ou em conflito permanecem
     * no manifesto para nova tentativa; itens resolvidos são removidos dele.
     */
    private ResultadoRenomeacao reverterInterno(Path pasta) {
        long inicioMs = System.currentTimeMillis();
        Path arquivoUndo = resolverArquivoUndo(pasta);
        logStream.publicarLog("renomear-arquivos", "Iniciando REVERSÃO segura em: " + pasta);
        if (!Files.isRegularFile(arquivoUndo)) {
            String mensagem = "Manifesto de reversão não encontrado para esta pasta.";
            publicarFinal("Renomear Arquivos (reversão)", inicioMs, "SEM_MANIFESTO", 0, 0);
            return new ResultadoRenomeacao("REVERSAO", "SEM_MANIFESTO", 0, 0, 0, 0, 0, 0, 0,
                mensagem, List.of());
        }

        OperacaoRenomeacao operacao;
        try {
            operacao = objectMapper.readValue(arquivoUndo.toFile(), OperacaoRenomeacao.class);
            validarProvenienciaManifesto(pasta, operacao);
        } catch (IOException | IllegalArgumentException e) {
            String mensagem = "Manifesto inválido ou incompatível: " + e.getMessage();
            logStream.publicarLog("renomear-arquivos", "[ERRO] " + mensagem);
            publicarFinal("Renomear Arquivos (reversão)", inicioMs, "FALHOU", 0, 1);
            return new ResultadoRenomeacao("REVERSAO", "FALHOU", 0, 0, 0, 0, 0, 1, 1,
                mensagem, List.of());
        }

        List<OperacaoRenomeacao.ItemRenomeado> pendentes = new ArrayList<>();
        List<OperacaoRenomeacao.ItemRenomeado> revertidos = new ArrayList<>();
        int jaResolvidos = 0;
        int falhas = 0;
        for (int indice = 0; indice < operacao.itens().size(); indice++) {
            OperacaoRenomeacao.ItemRenomeado item = operacao.itens().get(indice);
            try {
                Path atual = resolverDestinoSeguro(pasta, item.nomeNovo());
                Path original = resolverDestinoSeguro(pasta, item.nomeOriginal());
                boolean existeAtual = Files.exists(atual);
                boolean existeOriginal = Files.exists(original);
                logStream.publicarLog("renomear-arquivos", "[REVERTENDO " + (indice + 1) + "/"
                    + operacao.itens().size() + "] " + item.nomeNovo() + " -> " + item.nomeOriginal());
                if (existeAtual && !existeOriginal) {
                    moverSemSobrescrever(atual, original);
                    revertidos.add(item);
                    logStream.publicarLog("renomear-arquivos", "[REVERTIDO] " + item.nomeOriginal());
                } else if (!existeAtual && existeOriginal) {
                    jaResolvidos++;
                    logStream.publicarLog("renomear-arquivos", "[JÁ REVERTIDO] " + item.nomeOriginal());
                } else {
                    falhas++;
                    pendentes.add(item);
                    String motivo = existeAtual ? "o nome atual e o original existem" : "nenhum dos dois arquivos foi encontrado";
                    logStream.publicarLog("renomear-arquivos", "[PENDENTE] " + item.nomeNovo() + " — " + motivo + ".");
                }
            } catch (IOException | IllegalArgumentException e) {
                falhas++;
                pendentes.add(item);
                logStream.publicarLog("renomear-arquivos", "[FALHA] " + item.nomeNovo() + " — " + e.getMessage());
            }
        }

        if (pendentes.isEmpty()) {
            apagarManifesto(arquivoUndo);
            logStream.publicarLog("renomear-arquivos", "Manifesto removido: todos os itens estão nos nomes originais.");
        } else if (!salvarManifesto(pasta, pendentes)) {
            logStream.publicarLog("renomear-arquivos", "[ATENÇÃO] Falha ao atualizar pendências; o manifesto anterior foi preservado.");
        }

        String status = pendentes.isEmpty() ? "CONCLUIDO" : "CONCLUIDO_COM_PENDENCIAS";
        int concluidos = revertidos.size() + jaResolvidos;
        String mensagem = "Reversão " + status.toLowerCase(Locale.ROOT).replace('_', ' ')
            + ": " + revertidos.size() + " revertido(s), " + jaResolvidos
            + " já estava(m) original(is), " + pendentes.size() + " pendente(s).";
        PlanoRenomeacao plano = new PlanoRenomeacao(operacao.itens(), operacao.itens().size(), 0, 0);
        registrarTelemetria("Reverter Renomeação", status, inicioMs, plano, concluidos, pendentes.size());
        publicarFinal("Renomear Arquivos (reversão)", inicioMs, status, concluidos, pendentes.size());
        return resultado("REVERSAO", status, plano, concluidos, falhas, pendentes.size(), mensagem, revertidos);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: monta o plano determinístico, classificando filme,
     * série, especiais, conflitos e múltiplas faixas de legenda.
     *
     * <p>INVARIANTES DO DOMÍNIO: filme único sem marcador explícito não usa ano
     * ou número do título como episódio; nomes duplicados recebem descritor de
     * faixa somente quando forem legendas distintas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de leitura da pasta interrompe o
     * plano com exceção; arquivos ambíguos são ignorados, não movidos.
     */
    private PlanoRenomeacao simularInterno(Path pasta, String nomePadrao, int temporada) {
        logStream.publicarLog("renomear-arquivos", "Iniciando simulação (Dry-Run) em: " + pasta);
        List<Path> arquivos;
        try (Stream<Path> stream = Files.list(pasta)) {
            arquivos = stream.filter(Files::isRegularFile)
                .filter(a -> isVideoFile(a) || isLegendaFile(a))
                .sorted()
                .toList();
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível listar a pasta: " + e.getMessage(), e);
        }

        List<Path> renomeaveis = arquivos.stream()
            .filter(a -> !ehConteudoEspecial(a.getFileName().toString()))
            .toList();
        List<Path> videos = renomeaveis.stream().filter(this::isVideoFile).toList();
        List<Path> legendas = renomeaveis.stream().filter(this::isLegendaFile).toList();
        boolean pastaEhFilme = detectarPastaDeFilme(videos, legendas);
        logStream.publicarLog("renomear-arquivos", "Classificação automática: "
            + (pastaEhFilme ? "FILME" : "SÉRIE") + " | temporada usada: S" + String.format("%02d", temporada));

        List<OperacaoRenomeacao.ItemRenomeado> itens = new ArrayList<>();
        Set<String> nomesGerados = new HashSet<>();
        int conflitos = 0;
        int ignorados = 0;
        for (Path arquivo : arquivos) {
            String original = arquivo.getFileName().toString();
            if (ehConteudoEspecial(original)) {
                ignorados++;
                logStream.publicarLog("renomear-arquivos", "[IGNORADO] " + original
                    + " — conteúdo especial (SP/NCOP/NCED/OVA/PV).");
                continue;
            }

            String extensao = obterExtensao(original);
            EpisodioDetectado detectado = extrairEpisodio(original);
            String episodio = pastaEhFilme ? null : detectado.numero();
            String novo = gerarNomeNovo(original, nomePadrao, extensao, episodio, pastaEhFilme, temporada);
            if (original.equals(novo)) {
                ignorados++;
                logStream.publicarLog("renomear-arquivos", episodio == null && !pastaEhFilme
                    ? "[IGNORADO] " + original + " — episódio não detectado com segurança."
                    : "[IGNORADO] " + original + " — já está no padrão.");
                continue;
            }

            String chave = novo.toLowerCase(Locale.ROOT);
            if (nomesGerados.contains(chave) && isLegendaFile(arquivo)) {
                novo = gerarNomeLegendaAlternativo(novo, original, itens.size() + 1);
                chave = novo.toLowerCase(Locale.ROOT);
            }
            Path destino = resolverDestinoSeguro(pasta, novo);
            if (nomesGerados.contains(chave) || Files.exists(destino)) {
                conflitos++;
                logStream.publicarLog("renomear-arquivos", "[CONFLITO] " + original
                    + " não será alterado: o destino " + novo + " já existe ou foi reservado.");
                continue;
            }
            nomesGerados.add(chave);
            itens.add(new OperacaoRenomeacao.ItemRenomeado(original, novo));
            logStream.publicarLog("renomear-arquivos", "[DRY-RUN] " + original + " -> " + novo);
        }
        if (arquivos.isEmpty()) {
            logStream.publicarLog("renomear-arquivos", "Nenhum vídeo ou legenda compatível foi encontrado.");
        }
        return new PlanoRenomeacao(itens, arquivos.size(), conflitos, ignorados);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide conservadoramente se uma pasta representa um
     * filme para não transformar ano ou número do título em episódio.
     *
     * <p>INVARIANTES DO DOMÍNIO: um único vídeo só é série quando traz marcador
     * explícito de episódio; vários vídeos são tratados como série.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência de vídeos usa uma única legenda
     * como filme; múltiplas legendas sem vídeo permanecem série/ambíguas.
     */
    private boolean detectarPastaDeFilme(List<Path> videos, List<Path> legendas) {
        if (videos.size() == 1) {
            return !extrairEpisodio(videos.get(0).getFileName().toString()).explicito();
        }
        return videos.isEmpty() && legendas.size() == 1
            && !extrairEpisodio(legendas.get(0).getFileName().toString()).explicito();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: produz nomes padronizados para filme ou episódio sem
     * alterar a extensão original.
     *
     * <p>INVARIANTES DO DOMÍNIO: filmes usam {@code Nome.ext}; séries usam
     * {@code Nome - SxxEyy.ext}; arquivo ambíguo mantém o nome original.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: episódio ausente fora do modo filme não
     * gera destino e devolve o nome original.
     */
    private String gerarNomeNovo(String original, String padrao, String extensao, String episodio,
                                 boolean filme, int temporada) {
        if (filme) {
            return padrao + extensao;
        }
        if (episodio == null) {
            return original;
        }
        return padrao + " - S" + String.format("%02d", temporada) + "E" + episodio + extensao;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: conserva múltiplas legendas do mesmo episódio ou
     * filme sem descartar idioma, faixa, forced ou signs.
     *
     * <p>INVARIANTES DO DOMÍNIO: a extensão fica no final e o descritor não
     * contém caracteres inválidos de arquivo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem descritor reconhecível usa um índice
     * determinístico de faixa para continuar sem colisão silenciosa.
     */
    private String gerarNomeLegendaAlternativo(String nomeBase, String original, int indice) {
        String extensao = obterExtensao(nomeBase);
        String semExtensao = nomeBase.substring(0, nomeBase.length() - extensao.length());
        List<String> descritores = new ArrayList<>();
        Matcher track = DESCRITOR_TRACK.matcher(original.replace('_', ' '));
        if (track.find()) {
            descritores.add("Track" + track.group(1));
        }
        Matcher idioma = DESCRITOR_IDIOMA.matcher(original);
        while (idioma.find()) {
            String normalizado = idioma.group(1).toUpperCase(Locale.ROOT).replace('_', '-').replace(" ", "-");
            if (normalizado.equals("PTBR")) normalizado = "PT-BR";
            if (!descritores.contains(normalizado)) descritores.add(normalizado);
        }
        String sufixo = descritores.isEmpty() ? "Faixa " + indice : String.join(" ", descritores);
        return semExtensao + " - " + sufixo + extensao;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: extrai episódio dando prioridade a marcadores
     * explícitos de trackers e separando-os do fallback numérico ambíguo.
     *
     * <p>INVARIANTES DO DOMÍNIO: números possuem de 1 a 4 dígitos e são formatados
     * com ao menos dois; metadados técnicos não participam do fallback.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve episódio nulo; números encontrados
     * apenas pelo fallback são marcados como não explícitos.
     */
    private EpisodioDetectado extrairEpisodio(String nome) {
        String semExtensao = nome.replaceAll("(?:\\.[A-Za-z0-9]{1,5})+$", "");
        String semBrackets = semExtensao.replaceAll("\\[.*?\\]", " ").trim();
        Matcher seasonEp = SEASON_EPISODE_PATTERN.matcher(semBrackets);
        if (seasonEp.find()) return new EpisodioDetectado(formatarNumero(seasonEp.group(2)), true);
        Matcher separador = EPISODE_SEPARATOR_PATTERN.matcher(semBrackets);
        if (separador.find()) return new EpisodioDetectado(formatarNumero(separador.group(1)), true);
        Matcher rotulo = EPISODE_LABEL_PATTERN.matcher(semBrackets);
        if (rotulo.find()) return new EpisodioDetectado(formatarNumero(rotulo.group(1)), true);

        String semRuido = semBrackets.replaceAll("\\([^\\)]*\\)", " ")
            // Codecs de áudio com contagem de canais colada por ponto (AAC2.0,
            // DDP5.1, DTS-HD MA...) removidos ANTES de trocar separadores por
            // espaço, enquanto o ponto ainda os une. Sem isto, "AAC2.0" vira
            // "AAC2 0" e o "0" solto vence o fallback como falso episódio 00.
            .replaceAll("(?i)\\b(?:AAC|AC-?3|E-?AC-?3|DDP?|DTS(?:[-. ]?HD)?(?:[-. ]?MA)?|TrueHD|FLAC|Opus|MP3|PCM)[\\s._]?\\d?(?:[._]\\d)?\\b", " ")
            .replaceAll("[_.-]+", " ")
            .replaceAll("(?i)\\b(1080p|720p|2160p|4k|BD|BDRip|WEBRip|WEB\\s*DL|Dual\\s*Audio|Multi\\s*Audio|10bit|8bit|HEVC|AV1|x264|x265|Track\\s*\\d+|PTBR|PT\\s*BR)\\b", " ")
            .replaceAll("\\s+", " ").trim();
        Matcher fallback = EPISODE_FALLBACK.matcher(semRuido);
        String ultimo = null;
        while (fallback.find()) ultimo = fallback.group(1);
        return new EpisodioDetectado(ultimo == null ? null : formatarNumero(ultimo), false);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: formata números de episódio sem truncar episódios
     * acima de 99.
     *
     * <p>INVARIANTES DO DOMÍNIO: entrada aceita pelo regex está entre 0 e 9999.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: número inválido propaga erro de formato,
     * impedindo que um nome inconsistente seja aplicado.
     */
    private String formatarNumero(String numero) {
        return String.format("%02d", Integer.parseInt(numero));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: valida a pasta local selecionada antes de calcular
     * bloqueio, manifesto ou destinos.
     *
     * <p>INVARIANTES DO DOMÍNIO: a saída é absoluta, normalizada e representa um
     * diretório existente.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link IllegalArgumentException}
     * antes de qualquer escrita.
     */
    private Path validarPasta(Path pasta) {
        if (pasta == null) throw new IllegalArgumentException("Pasta de origem não fornecida.");
        Path normalizada = pasta.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizada)) {
            throw new IllegalArgumentException("A pasta informada não existe ou não é um diretório: " + normalizada);
        }
        return normalizada;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: transforma títulos editoriais de obras em nomes
     * compatíveis com Windows e impede tentativas de sair da coleção selecionada.
     *
     * <p>INVARIANTES DO DOMÍNIO: pontuação comum de títulos, como dois-pontos,
     * barra e interrogação, é normalizada sem mudar a pasta; traversal e caracteres
     * de controle continuam proibidos; o resultado não é reservado, possui até
     * 180 caracteres e não termina com ponto/espaço.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada vazia, traversal, controle ou
     * resultado reservado/vazio lança erro didático antes de analisar ou mover.
     */
    private String validarNomePadrao(String nomePadrao) {
        if (nomePadrao == null || nomePadrao.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome padrão não fornecido.");
        }
        String entrada = nomePadrao.trim();
        if (CARACTERE_CONTROLE.matcher(entrada).find() || TRAVERSAL_CAMINHO.matcher(entrada).find()) {
            throw new IllegalArgumentException("Nome padrão contém controle ou caminho inválido.");
        }

        String nome = SEPARADOR_EDITORIAL_INVALIDO_WINDOWS.matcher(entrada).replaceAll(" - ");
        nome = PONTUACAO_INVALIDA_WINDOWS.matcher(nome).replaceAll("");
        nome = nome.replaceAll("\\s+", " ")
            .replaceAll("(?:\\s+-){2,}\\s*", " - ")
            .replaceAll("[. ]+$", "")
            .trim();
        if (nome.isEmpty()) {
            throw new IllegalArgumentException("Nome padrão ficou vazio após remover caracteres incompatíveis com Windows.");
        }
        if (nome.length() > TAMANHO_MAXIMO_NOME_PADRAO) {
            throw new IllegalArgumentException("Nome padrão excede " + TAMANHO_MAXIMO_NOME_PADRAO + " caracteres.");
        }
        if (NOME_RESERVADO_WINDOWS.matcher(nome).matches() || nome.equals(".") || nome.equals("..")) {
            throw new IllegalArgumentException("Nome padrão é reservado pelo Windows.");
        }
        return nome;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: escolhe a temporada solicitada ou a infere de nomes
     * como Season 4, Temporada 2 e Temp1.
     *
     * <p>INVARIANTES DO DOMÍNIO: temporada válida fica entre 1 e 99.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: valor explícito fora da faixa é recusado;
     * ausência de indicação usa temporada 1.
     */
    private int resolverTemporada(Integer solicitada, String nomePadrao) {
        int temporada;
        if (solicitada != null) {
            temporada = solicitada;
        } else {
            Matcher matcher = TEMPORADA_PATTERN.matcher(nomePadrao);
            temporada = matcher.find() ? Integer.parseInt(matcher.group(1)) : 1;
        }
        if (temporada < 1 || temporada > 99) {
            throw new IllegalArgumentException("Temporada deve estar entre 1 e 99.");
        }
        return temporada;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que nomes vindos da entrada ou do manifesto
     * continuem filhos diretos da pasta autorizada.
     *
     * <p>INVARIANTES DO DOMÍNIO: o resultado normalizado tem exatamente a pasta
     * base como diretório pai.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: rejeita traversal, caminho absoluto ou
     * subdiretório com {@link IllegalArgumentException}.
     */
    private Path resolverDestinoSeguro(Path pasta, String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            throw new IllegalArgumentException("Manifesto ou plano contém nome de arquivo vazio.");
        }
        Path relativo;
        try {
            relativo = Path.of(nomeArquivo);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Nome de arquivo inválido: " + nomeArquivo, e);
        }
        if (relativo.isAbsolute() || relativo.getNameCount() != 1) {
            throw new IllegalArgumentException("Nome de arquivo tenta sair da pasta autorizada: " + nomeArquivo);
        }
        Path base = pasta.toAbsolutePath().normalize();
        Path destino = base.resolve(relativo).normalize();
        if (!base.equals(destino.getParent())) {
            throw new IllegalArgumentException("Destino fora da pasta autorizada: " + nomeArquivo);
        }
        return destino;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: move um arquivo dentro da pasta local sem substituir
     * destino e com compatibilidade para sistemas sem movimento atômico.
     *
     * <p>INVARIANTES DO DOMÍNIO: nunca utiliza {@code REPLACE_EXISTING}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: tenta move comum somente quando o
     * provedor declara não suportar atomicidade; demais erros são propagados.
     */
    private void moverSemSobrescrever(Path origem, Path destino) throws IOException {
        try {
            Files.move(origem, destino, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(origem, destino);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: grava o estado reversível fora da pasta de mídia com
     * substituição atômica do JSON.
     *
     * <p>INVARIANTES DO DOMÍNIO: o manifesto identifica a pasta e contém uma cópia
     * imutável dos mapeamentos recebidos.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: remove temporário, preserva manifesto
     * anterior quando possível e devolve {@code false}.
     */
    private boolean salvarManifesto(Path pasta, List<OperacaoRenomeacao.ItemRenomeado> itens) {
        Path destino = resolverArquivoUndo(pasta);
        Path temporario = null;
        try {
            Files.createDirectories(destino.getParent());
            temporario = Files.createTempFile(destino.getParent(), destino.getFileName().toString(), ".tmp");
            OperacaoRenomeacao operacao = new OperacaoRenomeacao(
                UUID.randomUUID().toString(), Instant.now().toString(), pasta.toString(), List.copyOf(itens));
            objectMapper.writeValue(temporario.toFile(), operacao);
            ArquivoAtomicoUtil.substituirAtomico(temporario, destino);
            return true;
        } catch (IOException e) {
            log.error("Falha ao salvar manifesto de renomeação em {}", destino, e);
            logStream.publicarLog("renomear-arquivos", "[ERRO] Falha ao salvar manifesto: " + e.getMessage());
            return false;
        } finally {
            if (temporario != null) {
                try { Files.deleteIfExists(temporario); } catch (IOException ignorada) { log.debug("Temporário já indisponível: {}", temporario); }
            }
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: confirma que um manifesto central pertence exatamente
     * à pasta solicitada antes de mover qualquer mídia.
     *
     * <p>INVARIANTES DO DOMÍNIO: pasta gravada e pasta atual são comparadas em
     * forma absoluta e normalizada; lista de itens não pode ser nula.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança erro de validação e mantém o
     * manifesto intacto para investigação.
     */
    private void validarProvenienciaManifesto(Path pasta, OperacaoRenomeacao operacao) {
        if (operacao == null || operacao.pastaBase() == null || operacao.itens() == null) {
            throw new IllegalArgumentException("estrutura obrigatória ausente");
        }
        Path declarada = Path.of(operacao.pastaBase()).toAbsolutePath().normalize();
        if (!pasta.toAbsolutePath().normalize().equals(declarada)) {
            throw new IllegalArgumentException("o manifesto pertence a outra pasta: " + declarada);
        }
        for (OperacaoRenomeacao.ItemRenomeado item : operacao.itens()) {
            resolverDestinoSeguro(pasta, item.nomeOriginal());
            resolverDestinoSeguro(pasta, item.nomeNovo());
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: remove o manifesto somente quando ele já não contém
     * estado reversível útil.
     *
     * <p>INVARIANTES DO DOMÍNIO: falha de remoção não afeta as mídias.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: registra aviso e conserva o arquivo para
     * limpeza posterior.
     */
    private void apagarManifesto(Path manifesto) {
        try {
            Files.deleteIfExists(manifesto);
        } catch (IOException e) {
            logStream.publicarLog("renomear-arquivos", "[AVISO] Não foi possível remover o manifesto: " + e.getMessage());
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: calcula a localização persistente do undo sem criar
     * artefatos dentro da pasta de mídia.
     *
     * <p>INVARIANTES DO DOMÍNIO: pastas distintas produzem nomes estáveis por hash.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: indisponibilidade de SHA-256 é tratada
     * como erro de configuração da JVM.
     */
    Path resolverArquivoUndo(Path pasta) {
        return PASTA_UNDO_PROJETO.resolve(PREFIXO_ARQUIVO_UNDO + hashPasta(pasta) + ".json");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: cria chave curta e estável para associar pasta e
     * manifesto central.
     *
     * <p>INVARIANTES DO DOMÍNIO: usa caminho absoluto normalizado em minúsculas e
     * os primeiros 64 bits de SHA-256.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança estado ilegal se a JVM não oferecer
     * SHA-256.
     */
    private String hashPasta(Path pasta) {
        String chave = pasta.toAbsolutePath().normalize().toString().toLowerCase(Locale.ROOT);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(chave.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível para manifesto de reversão.", e);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: serializa operações por pasta sem impedir que pastas
     * diferentes sejam processadas em paralelo.
     *
     * <p>INVARIANTES DO DOMÍNIO: somente o proprietário do lock executa a ação e
     * o bloqueio sempre é liberado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se a pasta estiver ocupada lança exceção
     * antes da ação; exceções da ação são propagadas após liberar o lock.
     */
    private <T> T executarComBloqueio(Path pasta, Supplier<T> acao) {
        String chave = pasta.toString().toLowerCase(Locale.ROOT);
        ReentrantLock bloqueio = BLOQUEIOS_POR_PASTA.computeIfAbsent(chave, ignorada -> new ReentrantLock());
        if (!bloqueio.tryLock()) {
            throw new OperacaoRenomeacaoEmAndamentoException(
                "Já existe uma operação da opção 13 em andamento nesta pasta. Aguarde a conclusão exibida no console.");
        }
        try {
            return acao.get();
        } finally {
            bloqueio.unlock();
            BLOQUEIOS_POR_PASTA.remove(chave, bloqueio);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: registra a execução da opção 13 como dataset canônico,
     * incluindo status, conflitos, ignorados, falhas e pendências.
     *
     * <p>INVARIANTES DO DOMÍNIO: contagem de corrigidos representa movimentos
     * efetivamente concluídos.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: a telemetria é secundária e não desfaz
     * movimentos; falhas são tratadas pelo serviço central.
     */
    private void registrarTelemetria(String tipo, String status, long inicioMs, PlanoRenomeacao plano,
                                     int concluidos, int pendentes) {
        String detalhe = "status=" + status + "; conflitos=" + plano.conflitos()
            + "; ignorados=" + plano.ignorados() + "; pendentes=" + pendentes;
        telemetriaService.registrarOperacao(TelemetriaService.criarOperacao(
            tipo, detalhe, System.currentTimeMillis() - inicioMs, plano.analisados(),
            plano.itens().size(), concluidos));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: padroniza o encerramento didático do console com
     * status real, contagens e duração.
     *
     * <p>INVARIANTES DO DOMÍNIO: sucesso nunca é impresso quando há pendências.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: apenas publica logs; não modifica o
     * resultado já obtido.
     */
    private void publicarFinal(String operacao, long inicioMs, String status, int concluidos, int pendentes) {
        String marcador = "CONCLUIDO".equals(status) ? "[SUCESSO]" : "[ATENÇÃO]";
        logStream.publicarLog("renomear-arquivos", marcador + " " + operacao.toUpperCase(Locale.ROOT)
            + " — status=" + status + ", concluídos=" + concluidos + ", pendentes=" + pendentes);
        logStream.publicarLog("renomear-arquivos", DuracaoUtil.linhaRelatorioFinal(operacao, inicioMs));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte plano e contadores de execução no contrato
     * único consumido pela API.
     *
     * <p>INVARIANTES DO DOMÍNIO: todos os contadores são não negativos e a lista
     * é imutável no record de resultado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: valores recebidos são protegidos com
     * {@code Math.max(0, valor)}.
     */
    private ResultadoRenomeacao resultado(String operacao, String status, PlanoRenomeacao plano,
                                           int concluidos, int falhas, int pendentes, String mensagem,
                                           List<OperacaoRenomeacao.ItemRenomeado> itens) {
        return new ResultadoRenomeacao(operacao, status, Math.max(0, plano.analisados()),
            Math.max(0, plano.itens().size()), Math.max(0, concluidos), Math.max(0, plano.conflitos()),
            Math.max(0, plano.ignorados()), Math.max(0, falhas), Math.max(0, pendentes), mensagem, itens);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: extrai a extensão final preservada no destino.
     *
     * <p>INVARIANTES DO DOMÍNIO: inclui o ponto e nunca altera capitalização.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nome sem extensão devolve vazio.
     */
    private String obterExtensao(String arquivo) {
        int index = arquivo.lastIndexOf('.');
        return index > 0 ? arquivo.substring(index) : "";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: classifica extensões de vídeo aceitas pela opção 13.
     *
     * <p>INVARIANTES DO DOMÍNIO: comparação ignora maiúsculas/minúsculas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: extensão desconhecida devolve falso.
     */
    private boolean isVideoFile(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase(Locale.ROOT);
        return EXTENSOES_VIDEO.stream().anyMatch(nome::endsWith);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: classifica extensões de legenda textual ou auxiliar
     * aceitas pela opção 13.
     *
     * <p>INVARIANTES DO DOMÍNIO: comparação ignora maiúsculas/minúsculas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: extensão desconhecida devolve falso.
     */
    private boolean isLegendaFile(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase(Locale.ROOT);
        return EXTENSOES_LEGENDA.stream().anyMatch(nome::endsWith);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: evita aplicar numeração episódica a extras e conteúdo
     * creditless conhecido.
     *
     * <p>INVARIANTES DO DOMÍNIO: classificação é independente de caixa.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência de marcador devolve falso.
     */
    private boolean ehConteudoEspecial(String nome) {
        return CONTEUDO_ESPECIAL_PATTERN.matcher(nome).find();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: descreve o número encontrado e se sua origem é forte
     * o suficiente para classificar arquivo único como episódio.
     *
     * <p>INVARIANTES DO DOMÍNIO: número pode ser nulo; explícito só indica uso de
     * separador ou rótulo formal.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência é representada por nulo/falso.
     */
    private record EpisodioDetectado(String numero, boolean explicito) {}

    /**
     * PROPÓSITO DE NEGÓCIO: agrega o dry-run e suas ocorrências para aplicação,
     * interface e telemetria.
     *
     * <p>INVARIANTES DO DOMÍNIO: itens são imutáveis e contadores não negativos.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: construção ocorre somente após a leitura
     * completa da pasta.
     */
    private record PlanoRenomeacao(
        List<OperacaoRenomeacao.ItemRenomeado> itens,
        int analisados,
        int conflitos,
        int ignorados
    ) {
        private PlanoRenomeacao {
            itens = List.copyOf(itens);
        }
    }
}
