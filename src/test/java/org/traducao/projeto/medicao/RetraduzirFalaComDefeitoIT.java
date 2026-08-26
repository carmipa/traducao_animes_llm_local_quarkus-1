package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;
import org.traducao.projeto.core.texto.dicionarioOrtografia.VeredictoPalavra;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.traducao.projeto.lore.domain.SnapshotContexto;
import org.traducao.projeto.traducao.application.ContextoCongeladoDaExecucao;
import org.traducao.projeto.traducao.application.TradutorLotesService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: retraduzir <b>só as falas com defeito de palavra</b>, a partir do inglês
 * original, e comparar antes de gravar.
 *
 * <h2>Por que retradução, e não corretor</h2>
 * Dois caminhos automáticos foram medidos e reprovados em 26/08/2026:
 *
 * <pre>
 *   corretor por sugestao do hunspell ... 3 certas de 319
 *   reparo mecanico de diacritico ....... 8 de 143, alcancando 10 falas de 85.503
 * </pre>
 *
 * <p>A razão é de CLASSE: {@code pasaje} não é erro de ortografia, é espanhol; {@code levarigo}
 * não é typo, é o modelo embolando. Corretor opera na palavra, e o defeito está na frase. A única
 * operação que pode consertar isso é traduzir a frase de novo.
 *
 * <h2>O que este harness NÃO faz</h2>
 * Não retraduz o acervo. O alvo é a fala que contém palavra que <b>nenhum dicionário reconhece</b>
 * ou que é <b>espanhol vazado</b> — e nome próprio, termo da franquia e onomatopeia ficam de fora,
 * porque não são defeito. Confundir os três foi o que fez uma medição anterior reportar 6.315
 * falas onde havia 420.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Sem {@code -Dkronos.aplicar.retraducao=SIM-ESCREVER-NO-ACERVO} nada é gravado, e o
 *       harness DIZ o que recebeu — "ninguém autorizou" e "a autorização não chegou" não podem
 *       sair iguais.</li>
 *   <li>Quem traduz é o {@link TradutorLotesService} de PRODUÇÃO, com o prompt congelado da obra.
 *       Nenhum prompt é escrito aqui.</li>
 *   <li>A fala só é substituída se a retradução <b>não contiver mais</b> palavra defeituosa —
 *       trocar um defeito por outro é pior que não mexer.</li>
 *   <li>Música, {@code Comment} e {@code .parcial} ficam fora, como na tela 3.3.</li>
 *   <li>Backup por arquivo alterado, antes de escrever.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * LLM fora do ar, contexto da obra ausente ou original indisponível terminam DECLARANDO, sem
 * gravar. Fala que a retradução não melhora fica como está e é contada.
 *
 * <p>Uso: {@code gradlew test --tests "*RetraduzirFalaComDefeitoIT*" "-Dkronos.medicao=true"
 * "-Dkronos.acervo=C:\animes\ANIMES-TESTES"}
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class RetraduzirFalaComDefeitoIT {

    private static final String AUTORIZACAO = "SIM-ESCREVER-NO-ACERVO";
    private static final String CHAVE_ESCRITA = "kronos.aplicar.retraducao";
    private static final Pattern TAG_ASS = Pattern.compile("\\{[^{}]*}");
    private static final Pattern PALAVRA = Pattern.compile("[\\p{L}][\\p{L}'-]*");

    /**
     * A lista DECLARADA de palavras que autorizam retradução. Ela é ENTRADA, não derivação.
     *
     * <p>O prejuízo que a exige, medido em 26/08/2026: perguntar ao dicionário "quem você não
     * conhece?" elegeu 361 falas, entre elas {@code psycommu} (16×, termo da franquia),
     * {@code Hahaha} (3×, onomatopeia) e {@code Falkes}/{@code Shree} (nomes de personagem).
     * Nenhum dicionário conhece nenhuma delas, e nenhuma é defeito — retraduzi-las reescreveria
     * fala CORRETA com o mesmo modelo que produziu os defeitos. Separar as seis classes foi
     * julgamento humano, e julgamento entra declarado, nunca escondido dentro do critério.
     */
    private static final String LISTA_DECLARADA = "medicao/palavras-defeituosas.txt";

    @Inject
    LeitorLegendaAss leitor;

    @Inject
    EscritorLegendaAss escritor;

    @Inject
    PoliticaEstiloMusical politicaEstiloMusical;

    @Inject
    CorretorOrtograficoLegenda dicionario;

    @Inject
    TradutorLotesService tradutor;

    @Inject
    ContextoCongeladoDaExecucao contextoCongelado;

    /**
     * O portão de qualidade DA PRODUÇÃO. {@code traduzirPendentes} é só a mecânica: quem decide
     * se uma tradução presta é este avaliador, e o {@code ProcessarArquivoUseCase} o consulta
     * para toda fala antes de gravar.
     *
     * <p>O ensaio de 26/08/2026 mostrou o que custa pulá-lo: {@code "an appropriate mission for
     * the Eighty-Six"} voltou como {@code "para o Oitenta e Seis"} — o nome da obra TRADUZIDO — e
     * o meu julgamento aceitou, porque só olhava se sobrara palavra quebrada. O critério existe,
     * está escrito, e é consultado; escrever um segundo aqui faria os dois divergirem.
     */
    @Inject
    org.traducao.projeto.traducao.application.AvaliadorTraducaoCache avaliadorCache;

    @Inject
    List<ProvedorContexto> provedores;

    /** Uma fala alvo: onde ela está, o que diz, e qual palavra a condenou. */
    private record Alvo(Path arquivo, int indice, String pt, String palavra) {}

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE (regra 9) dos dois instrumentos — o dicionário, que
     * escolhe as falas, e o tradutor, que as reescreve.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: imprime e devolve {@code false}; nada é gravado.
     */
    private boolean instrumentoCalibrado(Set<String> declaradas) {
        Map<String, VeredictoPalavra> v = dicionario.classificar(
            new LinkedHashSet<>(List.of("esmagrado", "misil", "batalha", "Gundam")));
        boolean achaQuebrada = v.get("esmagrado") == VeredictoPalavra.DESCONHECIDA;
        boolean achaEspanhol = v.get("misil") == VeredictoPalavra.TERMO_ESPANHOL;
        boolean calaNoCerto = v.get("batalha") == VeredictoPalavra.PORTUGUES_OK;
        if (!(achaQuebrada && achaEspanhol && calaNoCerto)) {
            System.out.printf("INSTRUMENTO REPROVADO NO CONTROLE — quebrada=%s espanhol=%s "
                + "certo=%s. Nenhuma fala e retraduzida.%n",
                achaQuebrada, achaEspanhol, calaNoCerto);
            return false;
        }
        System.out.println("  controle do dicionario: 'esmagrado'=quebrada · 'misil'=espanhol "
            + "· 'batalha'=portugues");

        // A LISTA tambem passa por controle. Palavra que o dicionario aceita como portugues nao
        // pode estar aqui: foi assim que 'nada' e 'porque' sairam da minha lista de espanhol, e
        // sem esta conferencia elas teriam mandado 883 falas corretas ao LLM.
        Map<String, VeredictoPalavra> daLista = dicionario.classificar(
            new LinkedHashSet<>(declaradas));
        List<String> aceitasComoPortugues = declaradas.stream()
            .filter(p -> daLista.get(p) == VeredictoPalavra.PORTUGUES_OK)
            .sorted()
            .toList();
        if (!aceitasComoPortugues.isEmpty()) {
            System.out.printf("LISTA POLUIDA: %d palavras declaradas defeituosas sao portugues "
                + "aceito pelo dicionario — %s. Nenhuma fala e retraduzida.%n",
                aceitasComoPortugues.size(), aceitasComoPortugues);
            return false;
        }
        System.out.printf("  controle da lista: as %d palavras declaradas sao TODAS recusadas "
            + "pelo dicionario%n", declaradas.size());

        // CASO-CONTROLE da guarda de lore, montado com o caso real que a originou.
        SnapshotContexto loreDeControle = new SnapshotContexto("controle", "controle", "", "",
            Set.of("Eighty-Six"), Map.of(), Set.of(), Map.of());
        boolean acusaPerda = "Eighty-Six".equals(termoDeLorePerdido(
            "uma missao adequada para o Eighty-Six.",
            "uma missao adequada para o Oitenta e Seis.", loreDeControle));
        boolean calaQuandoMantem = termoDeLorePerdido(
            "uma missao adequada para o Eighty-Six.",
            "uma missao apropriada para o Eighty-Six.", loreDeControle) == null;
        if (!(acusaPerda && calaQuandoMantem)) {
            System.out.printf("GUARDA DE LORE REPROVADA NO CONTROLE — acusa perda=%s, cala quando "
                + "mantem=%s. Nenhuma fala e retraduzida.%n", acusaPerda, calaQuandoMantem);
            return false;
        }
        System.out.println("  controle da lore: acusa 'Eighty-Six'->'Oitenta e Seis' · cala "
            + "quando o termo fica");

        // CASO-CONTROLE da guarda de moldura, com o bloco real que a originou.
        String comMove = "{\\blur1.5\\fs70\\move(294.8,550.2,-200.04,632.82,150,1234)}Mechanismo";
        boolean acusaTroca = !blocosDeTag(comMove)
            .equals(blocosDeTag("{\\pos(294.8,550.2)\\fad(314,0)\\blur1.5\\fs70}Mecha movel"));
        boolean molduraCalaQuandoMantem = blocosDeTag(comMove).equals(blocosDeTag(
            "{\\blur1.5\\fs70\\move(294.8,550.2,-200.04,632.82,150,1234)}Mecha movel"));
        if (!(acusaTroca && molduraCalaQuandoMantem)) {
            System.out.printf("GUARDA DE MOLDURA REPROVADA NO CONTROLE — acusa troca=%s, cala "
                + "quando mantem=%s. Nenhuma fala e retraduzida.%n", acusaTroca, molduraCalaQuandoMantem);
            return false;
        }
        System.out.println("  controle da moldura: acusa a troca de '\\move' por '\\pos' · cala "
            + "quando o bloco fica igual");
        return true;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lê a lista declarada de palavras defeituosas do classpath.
     *
     * <p>INVARIANTES DO DOMÍNIO: ausência da lista NÃO cai de volta no critério largo. O critério
     * largo é justamente o defeito que a lista existe para consertar.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: recurso ausente ou vazio devolve conjunto vazio, e o
     * chamador declara NÃO VERIFICADO sem retraduzir nada.
     */
    private Set<String> carregarDeclaradas() throws IOException {
        Set<String> palavras = new LinkedHashSet<>();
        try (java.io.InputStream entrada =
                 getClass().getClassLoader().getResourceAsStream(LISTA_DECLARADA)) {
            if (entrada == null) {
                return palavras;
            }
            try (java.io.BufferedReader leitura = new java.io.BufferedReader(
                     new java.io.InputStreamReader(entrada, StandardCharsets.UTF_8))) {
                String linha;
                while ((linha = leitura.readLine()) != null) {
                    String limpa = linha.trim();
                    if (!limpa.isEmpty() && !limpa.startsWith("#")) {
                        palavras.add(limpa);
                    }
                }
            }
        }
        return palavras;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a primeira palavra DECLARADA defeituosa que aparece na fala — é o que
     * autoriza retraduzir.
     *
     * <p>INVARIANTES DO DOMÍNIO: casamento sensível a maiúscula, porque {@code Estibor} e
     * {@code estibor} entraram na lista separadamente; e com fronteira de letra dos dois lados,
     * senão {@code organo} casaria dentro de {@code organograma}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: padrão não montado devolve {@code null} — nada é alvo.
     */
    private String palavraDeclarada(String texto, Pattern padrao) {
        if (padrao == null) {
            return null;
        }
        String visivel = TAG_ASS.matcher(texto).replaceAll(" ")
            .replace("\\N", " ").replace("\\n", " ").replace("\\h", " ");
        Matcher m = padrao.matcher(visivel);
        return m.find() ? m.group(1) : null;
    }

    private static Pattern padraoDe(Set<String> declaradas) {
        if (declaradas.isEmpty()) {
            return null;
        }
        String alternativa = declaradas.stream().map(Pattern::quote)
            .collect(java.util.stream.Collectors.joining("|"));
        return Pattern.compile("(?<![\\p{L}\\p{N}])(" + alternativa + ")(?![\\p{L}\\p{N}])");
    }

    @Test
    @DisplayName("retraduz SO as falas com palavra defeituosa, a partir do ingles original")
    void retraduzir() throws Exception {
        String recebida = System.getProperty(CHAVE_ESCRITA);
        boolean escrever = AUTORIZACAO.equals(recebida);
        System.out.printf("%n=== RETRADUCAO DIRIGIDA — modo %s ===%n",
            escrever ? "!! ESCRITA !!" : "ENSAIO (nada e gravado)");
        if (!escrever) {
            System.out.printf("  autorizacao: -D%s=%s%n", CHAVE_ESCRITA,
                recebida == null
                    ? "(NAO CHEGOU ao JVM de teste — ou nao foi passada, ou falta declarar a "
                        + "chave na lista nominal do build.gradle)"
                    : "'" + recebida + "' — DIFERENTE do exigido '" + AUTORIZACAO + "'");
        }

        List<Path> pastas = AlcanceDaMedicao.pastasDeTraducao();
        if (pastas.isEmpty()) {
            return;
        }
        Set<String> declaradas = carregarDeclaradas();
        if (declaradas.isEmpty()) {
            System.out.printf("NAO VERIFICADO: a lista declarada '%s' nao foi encontrada no "
                + "classpath, ou veio vazia. Sem ela nao ha retraducao — e cair de volta no "
                + "criterio largo e o proprio defeito que ela conserta.%n", LISTA_DECLARADA);
            return;
        }
        System.out.printf("  palavras declaradas defeituosas: %d (%s)%n",
            declaradas.size(), LISTA_DECLARADA);
        if (!instrumentoCalibrado(declaradas)) {
            return;
        }
        Pattern padraoDeclarado = padraoDe(declaradas);

        // PRIMEIRA PASSADA: o corpus diz quais palavras sao nome proprio. Sem isso, `Gundam`
        // (3.573 ocorrencias) entra na lista de defeito — foi o que aconteceu em 26/08/2026.
        NomesConfirmadosPeloCorpus nomes = new NomesConfirmadosPeloCorpus();
        Map<Path, DocumentoLegenda> documentos = new LinkedHashMap<>();
        for (Path pasta : pastas) {
            for (Path arquivo : AlcanceDaMedicao.arquivosEntregues(pasta)) {
                try {
                    DocumentoLegenda doc = leitor.ler(arquivo);
                    documentos.put(arquivo, doc);
                    doc.eventos().stream()
                        .filter(EventoLegenda::temTexto)
                        .filter(e -> !foraDoAlcance(e))
                        .forEach(e -> nomes.observar(e.texto()));
                } catch (RuntimeException e) {
                    System.out.printf("  ILEGIVEL (NAO VERIFICADO): %s%n", arquivo.getFileName());
                }
            }
        }
        System.out.printf("  nomes proprios confirmados pelo corpus: %d%n", nomes.quantidade());

        // SEGUNDA PASSADA: as falas alvo. Quem elege e a LISTA DECLARADA, e nao o dicionario —
        // ver LISTA_DECLARADA. Isto tambem tira o aquecimento do dicionario dos 222 arquivos, que
        // sozinho respondia por 36 minutos de execucao para produzir uma selecao errada.
        List<Alvo> alvos = new ArrayList<>();
        for (Map.Entry<Path, DocumentoLegenda> e : documentos.entrySet()) {
            List<EventoLegenda> eventos = e.getValue().eventos();
            for (int i = 0; i < eventos.size(); i++) {
                EventoLegenda ev = eventos.get(i);
                if (!ev.temTexto() || foraDoAlcance(ev)) {
                    continue;
                }
                String defeituosa = palavraDeclarada(ev.texto(), padraoDeclarado);
                if (defeituosa != null) {
                    alvos.add(new Alvo(e.getKey(), i, ev.texto(), defeituosa));
                }
            }
        }
        System.out.printf("  falas ALVO: %d%n%n", alvos.size());
        if (alvos.isEmpty()) {
            System.out.println("NADA A RETRADUZIR — e o controle acima provou que a lista ve.");
            return;
        }

        Map<String, Origem> cache = indiceDoCache();
        if (cache.isEmpty()) {
            System.out.println("NAO VERIFICADO: o cache veio vazio. Sem o ingles original nao ha "
                + "retraducao, so reescrita a esmo — nada e gravado.");
            return;
        }
        relatar(retraduzir(alvos, cache, nomes, padraoDeclarado), escrever, documentos);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a primeira palavra da fala que é defeito — quebrada ou espanhola.
     *
     * <p>INVARIANTES DO DOMÍNIO: nome próprio confirmado PELO CORPUS fica de fora; palavra com
     * menos de quatro letras também, porque sigla e interjeição enchem a lista sem serem defeito.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: dicionário mudo devolve {@code null} — nada é alvo.
     */
    private String palavraDefeituosa(String texto, NomesConfirmadosPeloCorpus nomes) {
        String visivel = TAG_ASS.matcher(texto).replaceAll(" ")
            .replace("\\N", " ").replace("\\n", " ").replace("\\h", " ");
        Set<String> candidatas = new LinkedHashSet<>();
        Matcher m = PALAVRA.matcher(visivel);
        while (m.find()) {
            if (m.group().length() >= 4 && !nomes.eNomeProprio(m.group())) {
                candidatas.add(m.group());
            }
        }
        if (candidatas.isEmpty()) {
            return null;
        }
        Map<String, VeredictoPalavra> v = dicionario.classificar(candidatas);
        for (String p : candidatas) {
            VeredictoPalavra veredicto = v.get(p);
            if (veredicto == VeredictoPalavra.DESCONHECIDA
                || veredicto == VeredictoPalavra.TERMO_ESPANHOL) {
                return p;
            }
        }
        return null;
    }

    private boolean foraDoAlcance(EventoLegenda evento) {
        if ("Comment".equals(evento.tipoLinha())) {
            return true;
        }
        return evento.estilo() != null && politicaEstiloMusical.estiloIgnorado(evento.estilo());
    }

    /** De onde a fala veio: o inglês original e a LORE que a traduziu — ambos ditos pelo cache. */
    private record Origem(String original, String contextoId) {}

    /** O que aconteceu com uma fala alvo depois de o LLM traduzir de novo. */
    private record Desfecho(Alvo alvo, String original, String novo, String veredicto, String lore) {
        boolean aceita() {
            return "MELHOROU".equals(veredicto);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a chave que casa a fala do {@code .ass} com a entrada do cache.
     *
     * <p>INVARIANTES DO DOMÍNIO: sem tags, sem quebra e <b>sem acento</b>. O acento sai porque a
     * própria tela 3.3 já corrigiu parte do acervo — a igualdade exata perdia 47 das 198 falas,
     * e todas eram justamente as que o pipeline havia consertado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo devolve string vazia, que não casa com nada.
     */
    private static String chaveDeCasamento(String texto) {
        if (texto == null) {
            return "";
        }
        String visivel = TAG_ASS.matcher(texto).replaceAll(" ")
            .replace("\\N", " ").replace("\\n", " ").replace("\\h", " ");
        String semAcento = java.text.Normalizer.normalize(visivel, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return semAcento.replaceAll("\\s+", " ").trim().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: índice tradução → (inglês original, lore que traduziu).
     *
     * <p>INVARIANTES DO DOMÍNIO: o {@code contextoId} vem da PROVENIÊNCIA gravada no cache, e não
     * do nome da pasta. Adivinhar a lore pelo nome da obra retraduziria com um prompt diferente do
     * que produziu a fala, e a comparação antes/depois deixaria de medir o que diz medir.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: cache ausente devolve mapa vazio, e o chamador declara.
     */
    private Map<String, Origem> indiceDoCache() throws IOException {
        LeitorAcervoCache.Acervo acervo = LeitorAcervoCache.ler(LeitorAcervoCache.raizPadrao());
        Map<String, Origem> indice = new java.util.HashMap<>();
        for (LeitorAcervoCache.FalaDoAcervo f : acervo.falas()) {
            if (f.traduzido().isBlank() || f.original().isBlank()) {
                continue;
            }
            indice.putIfAbsent(chaveDeCasamento(f.traduzido()), new Origem(f.original(),
                f.proveniencia() == null ? null : f.proveniencia().contextoId()));
        }
        System.out.printf("  cache: %d arquivos, %d ilegiveis, %d chaves%n",
            acervo.arquivosLidos(), acervo.arquivosIlegiveis(), indice.size());
        return indice;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: manda o {@link TradutorLotesService} de produção traduzir de novo, uma
     * lore por vez, com o prompt daquela lore.
     *
     * <p>INVARIANTES DO DOMÍNIO: nenhum prompt é escrito aqui; o contexto congelado é sempre
     * limpo, senão a lore de uma obra vazaria para a próxima.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lore não registrada ou LLM fora do ar DECLARAM e as falas
     * daquele grupo ficam sem desfecho — nunca são gravadas por omissão.
     */
    private List<Desfecho> retraduzir(List<Alvo> alvos, Map<String, Origem> cache,
                                      NomesConfirmadosPeloCorpus nomes, Pattern padraoDeclarado) {
        Map<String, ProvedorContexto> porId = new LinkedHashMap<>();
        for (ProvedorContexto p : provedores) {
            porId.putIfAbsent(p.getId(), p);
        }
        Map<String, List<Alvo>> porLore = new LinkedHashMap<>();
        int semOriginal = 0;
        for (Alvo a : alvos) {
            Origem o = cache.get(chaveDeCasamento(a.pt()));
            if (o == null || o.original().isBlank()) {
                semOriginal++;
                continue;
            }
            String lore = o.contextoId() == null ? ProvedorContexto.ID_SEM_LORE : o.contextoId();
            porLore.computeIfAbsent(lore, k -> new ArrayList<>()).add(a);
        }
        System.out.printf("  sem o ingles original no cache (NAO retraduzidas): %d%n", semOriginal);

        List<Desfecho> desfechos = new ArrayList<>();
        for (Map.Entry<String, List<Alvo>> grupo : porLore.entrySet()) {
            ProvedorContexto provedor = porId.get(grupo.getKey());
            if (provedor == null) {
                System.out.printf("  LORE '%s' NAO REGISTRADA — %d falas NAO VERIFICADAS%n",
                    grupo.getKey(), grupo.getValue().size());
                continue;
            }
            SnapshotContexto snapshot = SnapshotContexto.de(provedor);
            LinkedHashSet<String> pendentes = new LinkedHashSet<>();
            Map<String, List<Alvo>> porOriginal = new LinkedHashMap<>();
            for (Alvo a : grupo.getValue()) {
                String original = cache.get(chaveDeCasamento(a.pt())).original();
                pendentes.add(original);
                porOriginal.computeIfAbsent(original, k -> new ArrayList<>()).add(a);
            }
            System.out.printf("  lore '%s' (%s): %d falas, %d originais distintos ... ",
                provedor.getId(), provedor.getNomeExibicao(), grupo.getValue().size(),
                pendentes.size());
            System.out.flush();
            Map<String, String> traduzidos;
            contextoCongelado.definir(snapshot);
            long inicio = System.nanoTime();
            try {
                traduzidos = tradutor.traduzirPendentes(pendentes, Set.of(),
                    "retraducao-dirigida-" + provedor.getId(), new ArrayList<>(),
                    snapshot.promptSistema());
            } catch (Exception ex) {
                System.out.printf("LLM FALHOU (%s: %s) — %d falas NAO VERIFICADAS%n",
                    ex.getClass().getSimpleName(), ex.getMessage(), grupo.getValue().size());
                continue;
            } finally {
                contextoCongelado.limpar();
            }
            System.out.printf("%d devolvidas em %ds%n", traduzidos.size(),
                (System.nanoTime() - inicio) / 1_000_000_000L);
            // O dicionario so e aquecido AQUI, com as retraducoes — algumas centenas de textos
            // curtos, e nao os 85 mil do acervo. Ele julga o dano NOVO, que a lista declarada
            // (fechada em 26/08) por definicao nao conhece.
            dicionario.aquecerComTextos(traduzidos.values());
            for (Map.Entry<String, List<Alvo>> par : porOriginal.entrySet()) {
                for (Alvo a : par.getValue()) {
                    desfechos.add(julgar(a, par.getKey(), traduzidos.get(par.getKey()), nomes,
                        padraoDeclarado, snapshot));
                }
            }
        }
        return desfechos;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se a retradução vale a troca.
     *
     * <p>INVARIANTES DO DOMÍNIO: só MELHOROU é aceito. Se a palavra defeituosa continua, ou se
     * outra apareceu no lugar, a fala fica como está — trocar um defeito por outro é pior que não
     * mexer, e o modelo que produziu o defeito é o mesmo que está reescrevendo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: retradução nula ou vazia vira veredicto próprio, jamais
     * gravável — "o LLM não devolveu" e "o LLM devolveu vazio" não podem virar apagar a fala.
     */
    private Desfecho julgar(Alvo a, String original, String novo,
                            NomesConfirmadosPeloCorpus nomes, Pattern padraoDeclarado,
                            SnapshotContexto snapshot) {
        if (novo == null || novo.isBlank()) {
            return new Desfecho(a, original, novo, "LLM NAO DEVOLVEU", snapshot == null ? "" : snapshot.id());
        }
        if (novo.equals(a.pt())) {
            return new Desfecho(a, original, novo, "IGUAL AO ANTERIOR", snapshot == null ? "" : snapshot.id());
        }
        // PRIMEIRO o portao da PRODUCAO, e so depois o meu criterio. A ordem importa: o meu
        // criterio olha palavra, e o da producao olha a fala inteira contra o original — troca de
        // entidade, marcador perdido, traducao identica. Foi ele que faltou quando `Eighty-Six`
        // virou `Oitenta e Seis` e o desfecho saiu MELHOROU.
        String motivoDaProducao = avaliadorCache.motivoFalhaFinal(original, novo);
        if (motivoDaProducao != null) {
            return new Desfecho(a, original, novo, "PRODUCAO RECUSA: " + motivoDaProducao, snapshot.id());
        }
        // O portao da producao compara com o INGLES, e por isso nao ve este caso: a fala anterior
        // escrevia `Eighty-Six` e a nova escreveu `Oitenta e Seis` — o nome da obra TRADUZIDO.
        // Para o avaliador as duas traduzem o mesmo original. Quem sabe que aquilo nao se traduz e
        // a LORE da obra, e ela esta no snapshot que acabou de traduzir esta fala.
        String perdido = termoDeLorePerdido(a.pt(), novo, snapshot);
        if (perdido != null) {
            return new Desfecho(a, original, novo,
                "PERDEU TERMO DE LORE '" + perdido + "'", snapshot.id());
        }
        // A MOLDURA DA FALA NO DISCO TEM DE SOBREVIVER, e este e um defeito MEU, nao do modelo.
        // Eu agrupo as falas pelo INGLES original e mando um texto so ao LLM; quando N linhas do
        // `.ass` compartilham o mesmo original mas tem tags DIFERENTES, a resposta unica carrega
        // a moldura de uma delas para todas. Medido no ensaio de 26/08/2026: 4 das 93 aceitas,
        // e entre elas um bloco com `\move` e `\clip` — movimento e recorte de letreiro — que
        // seria trocado por um `\pos` simples. A tela quebra e nenhum dicionario ve.
        if (!blocosDeTag(a.pt()).equals(blocosDeTag(novo))) {
            return new Desfecho(a, original, novo, "TROCOU A MOLDURA DE TAGS DA FALA", snapshot == null ? "" : snapshot.id());
        }
        // ASSIMETRIA DELIBERADA. A SELECAO usa so a lista declarada, e erra para nao mexer: fala
        // correta jamais e reescrita. O JULGAMENTO usa a lista E o dicionario, e erra para nao
        // aceitar: se o LLM inventou uma palavra nova que nenhum dicionario conhece, a lista
        // fechada em 26/08 nao teria como saber, e a fala passaria com defeito novo.
        String defeito = palavraDeclarada(novo, padraoDeclarado);
        if (defeito == null) {
            defeito = palavraDefeituosa(novo, nomes);
        }
        if (defeito == null) {
            return new Desfecho(a, original, novo, "MELHOROU", snapshot == null ? "" : snapshot.id());
        }
        return new Desfecho(a, original, novo, defeito.equals(a.palavra())
            ? "MANTEVE '" + defeito + "'" : "TROCOU POR '" + defeito + "'",
            snapshot == null ? "" : snapshot.id());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: grava as falas aceitas, com backup por arquivo.
     *
     * <p>INVARIANTES DO DOMÍNIO: a fala só é trocada se o texto no disco ainda for o que foi
     * medido; se mudou por baixo, ela é pulada. O backup nunca é sobrescrito, senão a segunda
     * execução guardaria o estado JÁ alterado e o desfazer deixaria de existir.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: arquivo sem nenhuma troca válida não é reescrito.
     */
    private void aplicar(List<Desfecho> aceitos, Map<Path, DocumentoLegenda> documentos)
            throws IOException {
        Map<Path, List<Desfecho>> porArquivo = new LinkedHashMap<>();
        for (Desfecho d : aceitos) {
            porArquivo.computeIfAbsent(d.alvo().arquivo(), k -> new ArrayList<>()).add(d);
        }
        int arquivos = 0;
        int falas = 0;
        int desatualizadas = 0;
        for (Map.Entry<Path, List<Desfecho>> e : porArquivo.entrySet()) {
            DocumentoLegenda doc = documentos.get(e.getKey());
            if (doc == null) {
                continue;
            }
            List<EventoLegenda> eventos = new ArrayList<>(doc.eventos());
            int trocadas = 0;
            for (Desfecho d : e.getValue()) {
                int i = d.alvo().indice();
                if (i < 0 || i >= eventos.size()) {
                    desatualizadas++;
                    continue;
                }
                EventoLegenda ev = eventos.get(i);
                if (!d.alvo().pt().equals(ev.texto())) {
                    desatualizadas++;
                    continue;
                }
                eventos.set(i, ev.comTexto(d.novo()));
                trocadas++;
            }
            if (trocadas == 0) {
                continue;
            }
            Path backup = e.getKey().resolveSibling(
                e.getKey().getFileName().toString() + ".antes-da-retraducao");
            if (!Files.exists(backup)) {
                Files.copy(e.getKey(), backup);
            }
            escritor.escrever(e.getKey(), new DocumentoLegenda(
                doc.cabecalho(), eventos, doc.quebraDeLinha(), doc.comBom()));
            arquivos++;
            falas += trocadas;
        }
        System.out.printf("%n  GRAVADO: %d falas em %d arquivos (backup .antes-da-retraducao)%n",
            falas, arquivos);
        if (desatualizadas > 0) {
            System.out.printf("  PULADAS por o texto no disco ja nao ser o medido: %d%n",
                desatualizadas);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o relatório antes→depois, e o portão que decide se grava.
     *
     * <p>INVARIANTES DO DOMÍNIO: se NENHUMA retradução voltou do LLM, o instrumento está cego e
     * nada é gravado — "o modelo não melhorou nenhuma" e "o modelo não respondeu" não podem sair
     * com o mesmo desfecho (regra 12).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: pasta de relatórios ausente é criada; falha de escrita
     * do CSV propaga, porque relatório que some em silêncio é pior que erro.
     */
    private void relatar(List<Desfecho> desfechos, boolean escrever,
                         Map<Path, DocumentoLegenda> documentos) throws IOException {
        Map<String, Integer> porVeredicto = new TreeMap<>();
        StringBuilder csv =
            new StringBuilder("obra;arquivo;indice;palavra;veredicto;lore;en;antes;depois\n");
        for (Desfecho d : desfechos) {
            porVeredicto.merge(agrupar(d.veredicto()), 1, Integer::sum);
            csv.append(campo(d.alvo().arquivo().getParent().getParent().getFileName().toString()))
               .append(campo(d.alvo().arquivo().getFileName().toString()))
               .append(d.alvo().indice()).append(';')
               .append(campo(d.alvo().palavra()))
               .append(campo(d.veredicto()))
               .append(campo(d.lore()))
               .append(campo(d.original()))
               .append(campo(d.alvo().pt()))
               .append(campo(d.novo())).append('\n');
        }
        System.out.printf("%n=== DESFECHO de %d falas ===%n", desfechos.size());
        porVeredicto.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(e -> System.out.printf("   %5d  %s%n", e.getValue(), e.getKey()));

        List<Desfecho> aceitos = desfechos.stream().filter(Desfecho::aceita).toList();
        System.out.println("\n=== AMOSTRA: EN -> antes -> depois (ate 25 aceitas) ===");
        aceitos.stream().limit(25).forEach(d -> System.out.printf(
            "  [%s]%n    EN     %s%n    ANTES  %s%n    DEPOIS %s%n",
            d.alvo().palavra(), corte(d.original()), corte(d.alvo().pt()), corte(d.novo())));
        System.out.println("\n=== AMOSTRA: RECUSADAS (ate 12) ===");
        desfechos.stream().filter(d -> !d.aceita()).limit(12).forEach(d -> System.out.printf(
            "  %-26s ANTES  %s%n%29sDEPOIS %s%n", d.veredicto(), corte(d.alvo().pt()), "",
            corte(d.novo())));

        Path saida = Path.of("relatorios", "retraducao-desfecho.csv");
        Files.createDirectories(saida.getParent());
        Files.writeString(saida, csv.toString(), StandardCharsets.UTF_8);
        System.out.printf("%n  desfecho completo: %s%n", saida.toAbsolutePath());

        // O QUE A RETRADUCAO CUSTA, medido e nao suposto: o aya devolve muita fala SEM ACENTO
        // ("Nao e correto" no lugar de "Não é correto"). Nao e motivo para recusar — quem conserta
        // acento e a tela 3.3, que roda depois — mas tem de sair no relatorio, senao a troca
        // parece de graca.
        int semAcento = 0;
        for (Desfecho d : aceitos) {
            if (temAcentoFaltando(d.novo()) && !temAcentoFaltando(d.alvo().pt())) {
                semAcento++;
            }
        }
        System.out.printf("%n  das %d aceitas, %d perderam acento que a fala anterior tinha — "
            + "e divida da tela 3.3, que roda depois desta gravacao%n", aceitos.size(), semAcento);

        long devolvidas = desfechos.stream()
            .filter(d -> d.novo() != null && !d.novo().isBlank()).count();
        if (devolvidas == 0) {
            System.out.println("\nNAO VERIFICADO: o LLM nao devolveu UMA traducao sequer. "
                + "Isso e instrumento cego, nao 'nenhuma melhorou' — nada e gravado.");
            return;
        }
        if (!escrever) {
            System.out.printf("%nENSAIO: %d falas SERIAM gravadas. Para gravar: -D%s=%s%n",
                aceitos.size(), CHAVE_ESCRITA, AUTORIZACAO);
            return;
        }
        aplicar(aceitos, documentos);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: informa se a fala tem alguma palavra que é português sem acento.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: dicionário mudo devolve {@code false} — o relatório dirá
     * zero, e o zero é honesto porque o controle já provou que o dicionário enxerga.
     */
    /**
     * PROPÓSITO DE NEGÓCIO: o termo de lore que a fala ANTERIOR escrevia certo e a retradução
     * deixou cair.
     *
     * <p>INVARIANTES DO DOMÍNIO: a lista de termos vem do {@link SnapshotContexto} da obra —
     * {@code termosProtegidos} mais os valores canônicos de {@code correcoesTerminologia}, que é
     * quem de fato garante a grafia oficial. Só conta o termo que a versão anterior JÁ tinha:
     * cobrar da retradução um termo que nunca esteve ali seria inventar defeito.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: snapshot nulo ou sem termos devolve {@code null}, e a
     * fala segue para os outros critérios — nunca é aprovada por este caminho.
     */
    /**
     * PROPÓSITO DE NEGÓCIO: os blocos {@code {...}} da fala, na ordem — a moldura de tipografia
     * e efeito que tem de sobreviver à retradução.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo devolve lista vazia; duas listas vazias são
     * iguais, e uma fala sem tag nenhuma passa como deve.
     */
    private static List<String> blocosDeTag(String texto) {
        List<String> blocos = new ArrayList<>();
        if (texto == null) {
            return blocos;
        }
        Matcher m = TAG_ASS.matcher(texto);
        while (m.find()) {
            blocos.add(m.group());
        }
        return blocos;
    }

    private static String termoDeLorePerdido(String antes, String depois, SnapshotContexto lore) {
        if (lore == null || antes == null || depois == null) {
            return null;
        }
        Set<String> termos = new LinkedHashSet<>();
        if (lore.termosProtegidos() != null) {
            termos.addAll(lore.termosProtegidos());
        }
        if (lore.correcoesTerminologia() != null) {
            termos.addAll(lore.correcoesTerminologia().values());
        }
        for (String termo : termos) {
            if (termo == null || termo.length() < 3) {
                continue;
            }
            if (antes.contains(termo) && !depois.contains(termo)) {
                return termo;
            }
        }
        return null;
    }

    private boolean temAcentoFaltando(String texto) {
        if (texto == null) {
            return false;
        }
        String visivel = TAG_ASS.matcher(texto).replaceAll(" ")
            .replace("\\N", " ").replace("\\n", " ").replace("\\h", " ");
        Set<String> candidatas = new LinkedHashSet<>();
        Matcher m = PALAVRA.matcher(visivel);
        while (m.find()) {
            if (m.group().length() >= 3) {
                candidatas.add(m.group());
            }
        }
        return !candidatas.isEmpty()
            && dicionario.classificar(candidatas).containsValue(VeredictoPalavra.ACENTO_FALTANDO);
    }

    private static String agrupar(String veredicto) {
        if (veredicto.startsWith("MANTEVE")) {
            return "MANTEVE a mesma palavra defeituosa";
        }
        return veredicto.startsWith("TROCOU") ? "TROCOU por outra defeituosa" : veredicto;
    }

    private static String campo(String valor) {
        return (valor == null ? "" : valor.replace(';', ',').replace("\n", " ").replace("\r", ""))
            + ";";
    }

    private static String corte(String valor) {
        if (valor == null) {
            return "(nulo)";
        }
        String s = valor.replace("\n", " ").trim();
        return s.length() > 88 ? s.substring(0, 88) + "..." : s;
    }
}
