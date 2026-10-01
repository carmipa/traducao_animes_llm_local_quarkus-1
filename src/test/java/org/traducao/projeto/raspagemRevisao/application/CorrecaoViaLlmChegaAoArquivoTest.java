package org.traducao.projeto.raspagemRevisao.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.cachetraducao.domain.CacheDocumento;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.raspagemRevisao.domain.ModoReferenciaRevisao;
import org.traducao.projeto.raspagemRevisao.domain.ModoRevisaoLegendas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fecha a metade que {@code CorrecaoChegaAoArquivoTest} não alcançou. Aquele
 * prova o caminho DETERMINÍSTICO; este prova o caminho do LLM — que é a única rota para a maioria
 * das classes de defeito, porque a regra local só cobre parentesco, "graças a Deus", insulto forte
 * e o artigo de mobile suit.
 *
 * <h2>O defeito escolhido é o que NÃO tem conserto local</h2>
 * {@code "Ela está cansado"} é detectada pela concordância nominal e nenhuma regra determinística
 * a resolve — foi exatamente o caso que o teste anterior deixou registrado como limite,
 * preservando a fala sem corrigi-la. Aqui ela chega ao modelo.
 *
 * <p>O caso começou como {@code "Ela está MUITO cansado"} e o LLM nunca era consultado: zero
 * chamadas. A fala não estava sendo detectada, porque o padrão exige o particípio imediatamente
 * após o verbo. Erro meu na montagem — mas virou a lacuna medida em
 * {@code MedicaoAdverbioEntreVerboEParticipioIT} (0 casos no acervo).
 *
 * <h2>O que este teste prova, e o que continua aberto</h2>
 * Prova o MECANISMO: detectar → mascarar → pedir ao modelo → desmascarar → julgar na
 * {@code GuardaCorrecaoSegura} → gravar no {@code .ass}. Não prova, e não tem como provar sem
 * infraestrutura, se a aya ou o mistral REAIS escrevem português melhor — isso é medição de
 * modelo, e depende do LM Studio no ar.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A correção do modelo só entra no arquivo depois de aprovada pela guarda.</li>
 *   <li>Resposta que não melhora a auditoria é RECUSADA, e a fala original é preservada — não
 *       basta o modelo responder.</li>
 *   <li>Fala sã não pode chegar ao modelo: consultar custa segundos e cota.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem rede: {@link LlmCorretorDublado} responde localmente aplicando a troca SOBRE o texto
 * recebido, para não perder os marcadores de tag e de lore.
 */
@QuarkusTest
@TestProfile(CorrecaoViaLlmChegaAoArquivoTest.PerfilComLlmDublado.class)
class CorrecaoViaLlmChegaAoArquivoTest {

    /**
     * Escopa as alternativas a este teste; um dublê global mudaria a suíte inteira.
     *
     * <p>O tradutor externo entrou aqui em 2026-08-16 junto com a cascata LLM→Google. A cascata foi
     * REVERTIDA em 2026-09-16 (Paulo, "isola o botão 1 e deixa o 2 com o Google apenas"): o Google
     * agora é uma passada SEPARADA (modo {@code GOOGLE}, botão "Só o Google"), não a 2ª etapa do
     * botão LLM. O dublê continua necessário porque {@link #falaCorrigidaNaoVoltaComItalico} exercita
     * essa passada Google, e sem ele o teste bateria na REDE de verdade.
     */
    public static class PerfilComLlmDublado implements QuarkusTestProfile {
        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(LlmCorretorDublado.class, RecuperacaoExternaContadora.class);
        }
    }

    @Inject
    RevisarLegendasUseCase useCase;

    @Inject
    LlmCorretorDublado llm;

    @Inject
    RecuperacaoExternaContadora tradutorExterno;

    @Inject
    ObjectMapper mapper;

    private static final String CABECALHO = """
        [Script Info]
        ScriptType: v4.00+
        PlayResX: 1920
        PlayResY: 1080

        [V4+ Styles]
        Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
        Style: Default,Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,0,2,10,10,10,1

        [Events]
        Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        """;

    /**
     * Os dublês são {@code @ApplicationScoped} e SOBREVIVEM aos testes. Esquecer um deles aqui
     * custou uma investigação inteira em 2026-08-16: o contador do tradutor externo vazou do teste
     * da cascata para o vizinho, e o resultado parecia produção quebrada — "a fala foi resolvida
     * pelo LLM E desceu para o Google". Era o instrumento, não o pipeline.
     */
    @BeforeEach
    void limparDuble() {
        llm.reiniciar();
        tradutorExterno.reiniciar();
    }

    private record Fala(String ingles, String portugues) {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: monta a legenda PT a auditar e o cache com o inglês de referência.
     * <p>INVARIANTES DO DOMÍNIO: índice, estilo e ordem casam entre o .ass e o cache.
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga {@link IOException}.
     */
    private Path montar(Path temp, List<Fala> falas) throws IOException {
        return montar(temp, falas, "danmachi");
    }

    /**
     * A PROVENIENCIA do cache e quem decide o contexto efetivo — nao o id passado ao use
     * case. Descoberto em 22/08/2026: um teste com contextoId "gundam_zz" e cache com
     * proveniencia "danmachi" rodava com a lore do DanMachi (42 termos em vez de 83) e
     * reprovava por motivo que nao era o assunto dele.
     */
    private Path montar(Path temp, List<Fala> falas, String lore) throws IOException {
        Path pastaPt = Files.createDirectory(temp.resolve("pt"));
        Path pastaCache = Files.createDirectory(temp.resolve("cache"));
        StringBuilder ass = new StringBuilder(CABECALHO);
        List<EntradaCache> entradas = new ArrayList<>();
        for (int i = 0; i < falas.size(); i++) {
            ass.append("Dialogue: 0,0:00:0").append(i).append(".00,0:00:09.00,Default,,0,0,0,,")
               .append(falas.get(i).portugues()).append('\n');
            entradas.add(new EntradaCache(
                i, "Default", falas.get(i).ingles(), falas.get(i).portugues(), "en", "pt"));
        }
        Files.writeString(pastaPt.resolve("show_PT-BR.ass"), ass.toString(), StandardCharsets.UTF_8);
        mapper.writerWithDefaultPrettyPrinter().writeValue(
            pastaCache.resolve("show_ENG.cache.json").toFile(),
            new CacheDocumento(new ProvenienciaCache(
                ProvenienciaCache.SCHEMA_ATUAL, lore, "h", "m", "en", "pt"), entradas));
        return pastaPt;
    }

    /** Roda a revisão em modo LLM e devolve o arquivo de saída, se houve gravação. */
    private Optional<String> revisar(Path temp, Path pastaPt) throws IOException {
        Path pastaSaida = Files.createDirectory(temp.resolve("saida"));
        useCase.executar(pastaPt, null, temp.resolve("cache"), pastaSaida,
            ModoRevisaoLegendas.LLM_CONCORDANCIA, "danmachi", ModoReferenciaRevisao.AMBOS);
        Path destino = pastaSaida.resolve("show_PT-BR.ass");
        return Files.exists(destino)
            ? Optional.of(Files.readString(destino, StandardCharsets.UTF_8))
            : Optional.empty();
    }

    /** A passada "Só o Google" (modo GOOGLE, sem LLM). Referência EN vem do cache, como em revisar. */
    private Optional<String> revisarSoGoogle(Path temp, Path pastaPt) throws IOException {
        Path pastaSaida = Files.createDirectory(temp.resolve("saida"));
        useCase.executar(pastaPt, null, temp.resolve("cache"), pastaSaida,
            ModoRevisaoLegendas.GOOGLE, "danmachi", ModoReferenciaRevisao.AMBOS);
        Path destino = pastaSaida.resolve("show_PT-BR.ass");
        return Files.exists(destino)
            ? Optional.of(Files.readString(destino, StandardCharsets.UTF_8))
            : Optional.empty();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: quando a rota por metade desiste, ela DIZ por quê no log — senão o
     * operador lê o motivo do OUTRO caminho e conclui errado.
     *
     * <h2>A cicatriz, da própria corrida de 22/08/2026</h2>
     * A fala do ZZ continuou presa e o relatório dizia
     * {@code REVISAO_QUEBRA_DE_LINHA_PERDIDA} — motivo do caminho de sempre. A rota nova havia
     * desistido em silêncio, e não havia como saber em qual metade nem por quê sem sonda no
     * código.
     *
     * <p>Aqui o dublê devolve a metade SEM traduzir (é o que o modelo real faz quando não
     * entende o fragmento), e o log tem de dizer isso.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: rota que desiste calada faz o operador diagnosticar a
     * causa errada.
     */
    @Test
    @DisplayName("rota por metade que desiste DIZ o motivo no log")
    void rotaPorMetadeQueDesisteDizOMotivo(@TempDir Path temp) throws IOException {
        llm.responderSemAlterar();
        Path pastaPt = montar(temp, List.of(new Fala(
            "I will wait for you here,\\Nuntil the sun goes down.",
            "I will wait for you here,\\Nuntil the sun goes down.")));

        java.io.ByteArrayOutputStream capturado = new java.io.ByteArrayOutputStream();
        java.io.PrintStream anterior = System.out;
        System.setOut(new java.io.PrintStream(capturado, true, StandardCharsets.UTF_8));
        try {
            revisar(temp, pastaPt);
        } finally {
            System.setOut(anterior);
        }

        String log = capturado.toString(StandardCharsets.UTF_8);
        assertTrue(log.contains("[SEGMENTO] rota por metade desistiu"),
            "a desistencia tem de aparecer no log do operador: " + log);
        assertTrue(log.contains("EM INGLES"),
            "e tem de dizer QUAL foi o motivo: " + log);
    }

    private static long contarDialogos(String conteudo) {
        return conteudo.lines().filter(l -> l.startsWith("Dialogue:")).count();
    }

    /**
     * FIXTURE TROCADA em 2026-08-16, com o motivo em voz alta: era {@code "Ela está cansado."} —
     * concordância pura, que saiu do escopo desta tela por decisão de Paulo (a 3.3 é a dona). A
     * propriedade que este teste protege não mudou: <b>defeito sem conserto local vai ao LLM e a
     * correção CHEGA ao arquivo</b>. Só o exemplo precisava passar a ser do escopo novo — uma fala
     * que não foi traduzida.
     */
    @Test
    @DisplayName("o defeito sem conserto local vai ao LLM e VOLTA corrigido no .ass")
    void concordanciaNominalCorrigidaPeloLlmChegaAoArquivo(@TempDir Path temp) throws IOException {
        llm.ensinar("Get out of there!", "Saia daí!");
        Path pastaPt = montar(temp, List.of(
            new Fala("Get out of there!", "Get out of there!"),
            new Fala("The weather is nice today.", "O tempo está bom hoje.")));

        String saida = revisar(temp, pastaPt).orElseThrow(() -> new AssertionError(
            "O CAMINHO DO LLM NÃO GRAVOU NADA. O defeito é detectado, o modelo respondeu com a "
                + "correção certa, e mesmo assim nada chegou ao arquivo — é assim que a fatia "
                + "falharia se a guarda rejeitasse correção legítima ou a escrita fosse cegada."));

        assertTrue(saida.contains("Saia daí!"),
            "a correção do modelo não chegou ao arquivo:\n" + saida);
        assertTrue(saida.contains("O tempo está bom hoje."),
            "a fala sã ao lado foi alterada");
        assertEquals(2, contarDialogos(saida), "nenhuma fala pode sumir na gravação");
        assertEquals(1, llm.chamadas(),
            "só a fala defeituosa pode ir ao modelo — consultar a sã custa segundos e cota");
        // A asserção é sobre QUAL fala desceu, não sobre quantas: a contagem global não distingue
        // esta fala de outra do mesmo arquivo, e um número certo com o conjunto errado é o defeito
        // clássico de instrumento desta casa.
        assertFalse(tradutorExterno.pedidos().stream().anyMatch(p -> p.contains("Get out of there")),
            "o LLM resolveu esta fala: ela não podia ter descido para a 2ª etapa. Foram ao Google: "
                + tradutorExterno.pedidos());
    }

    /**
     * BOTÃO 1 ISOLADO (Paulo, 2026-09-16: "isola o botão 1 e deixa o 2 com o Google apenas"): a
     * passada "Traduzir o que faltou" é do LLM local + dicionários. Quando o LLM não resolve, a fala
     * fica <b>PENDENTE</b> e preservada — o Google <b>NÃO</b> é acionado na mesma passada.
     *
     * <h2>Por que a reversão da cascata de 16/08</h2>
     * O Google retraduz o inglês INTEIRO a partir do original, e numa fala PARCIALMENTE traduzida
     * isso SUBSTITUIRIA o que o tradutor já acertou. Paulo pediu para separar os dois: o Google só
     * roda quando o operador escolhe explicitamente o botão "Só o Google" (modo {@code GOOGLE}), que
     * {@link #falaCorrigidaNaoVoltaComItalico} cobre. Aqui provo o outro lado: sem cascata.
     *
     * <p>O controle que separa "o pipeline não chamou o modelo" de "chamou e não resolveu" continua:
     * o LLM responde sem alterar (foi consultado), e o desfecho é pendência honesta, não silêncio.
     */
    @Test
    @DisplayName("botão 1 isolado: LLM não resolve, a fala fica PENDENTE e o Google NÃO é chamado")
    void botao1LlmNaoResolveFicaPendenteSemGoogle(@TempDir Path temp) throws IOException {
        llm.responderSemAlterar();
        Path pastaPt = montar(temp, List.of(
            new Fala("Get out of there!", "Get out of there!")));

        Optional<String> saida = revisar(temp, pastaPt);

        assertTrue(llm.chamadas() >= 1,
            "o modelo TEM de ter sido consultado — senão este não é o controle que eu penso que é");
        assertEquals(0, tradutorExterno.chamadas(),
            "botão 1 é LLM+dicionários: o Google NÃO pode ser acionado na mesma passada (Paulo 2026-09-16). "
                + "Foram ao Google: " + tradutorExterno.pedidos());
        String texto = saida.orElse(Files.readString(
            pastaPt.resolve("show_PT-BR.ass"), StandardCharsets.UTF_8));
        assertTrue(texto.contains("Get out of there!"),
            "sem o Google, a fala que o LLM não resolveu fica preservada (pendente honesto):\n" + texto);
        assertEquals(1, contarDialogos(texto), "a fala não pode sumir");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a fala CORRIGIDA não pode voltar com o itálico que a regra tirou.
     *
     * <h2>A cicatriz, medida na corrida real do 0080 em 22/08/2026</h2>
     * A tela limpou 144 falas naquela corrida e UMA voltou suja — exatamente a que foi
     * corrigida. Uma fala em inglês teve o itálico removido pelo preparador, foi ao Google por
     * estar em inglês, e voltou com a tag do ORIGINAL colada:
     * <pre>
     * referência EN : {\i1}Get out of there!
     * PT corrigido  : {\i1}Saia daí!   &lt;- o itálico VOLTOU
     * </pre>
     * Uma varredura no disco depois da corrida encontrou a única linha com itálico nos seis
     * arquivos — o instrumento independente confirmou o que o log já dizia.
     *
     * <h2>Agora pela passada "Só o Google" (Paulo, 2026-09-16)</h2>
     * A cascata LLM→Google foi revertida. A fala totalmente em inglês é o caso do botão "Só o
     * Google" (modo {@code GOOGLE}), e é ele que reproduz o defeito: o dublê do tradutor externo
     * devolve a tradução com o prefixo {@code {\i1}} colado, exatamente como o Google real fez no
     * 0080. Se a regra final não tirar o itálico, ele chega ao {@code .ass}. Trocado o exemplo do
     * "We'll land..." (que dependia da legenda inglesa em disco) por uma fala detectada pelo cache,
     * sem mudar a invariante testada.
     *
     * <p>INVARIANTES DO DOMÍNIO: LLM, Google e determinístico desembocam todos em
     * {@code DecisaoFala.Corrigir}, e é por isso que a regra mora lá e não em cada caminho.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: itálico no arquivo depois da correção significa que a
     * limpeza vale para a fala que a tela NÃO tocou e falha justamente na que ela tocou.
     */
    @Test
    @DisplayName("a fala corrigida nao volta com o italico que a regra tirou")
    void falaCorrigidaNaoVoltaComItalico(@TempDir Path temp) throws IOException {
        // Passada "Só o Google" (modo GOOGLE): a fala inteira em inglês vai ao tradutor externo, que
        // recompoe a estrutura a partir do original EN — e e de la que o italico voltava. O dublê
        // devolve "{\\i1}" + traducao (como o Google real), entao so a regra final de Corrigir tira
        // o italico. Com o texto ja em portugues o defeito nao apareceria: a fala precisa DESCER.
        Path pastaPt = montar(temp, List.of(
            new Fala("{\\i1}Get out of there!", "{\\i1}Get out of there!")));

        Optional<String> saida = revisarSoGoogle(temp, pastaPt);

        String texto = saida.orElse(Files.readString(
            pastaPt.resolve("show_PT-BR.ass"), StandardCharsets.UTF_8));
        assertFalse(texto.contains("Get out of there"),
            "a fala tinha de sair do ingles:" + texto);
        assertFalse(texto.contains("{\\i1}"),
            "o italico nao pode voltar na fala corrigida: " + texto);
        assertEquals(1, tradutorExterno.chamadas(),
            "o teste só vale se a fala foi ao Google — é lá que o itálico voltava");
        assertEquals(1, contarDialogos(texto), "a fala não pode sumir");
    }
    /**
     * PROPÓSITO DE NEGÓCIO: a fala partida por {@code \N} sai traduzida <b>com a quebra no
     * mesmo lugar</b>. É o defeito que sobrou depois de tudo o mais fechar.
     *
     * <h2>O prejuízo, medido no acervo em 22/08/2026</h2>
     * A medição de prontidão varreu 66.976 falas auditáveis e achou as que continuavam presas em
     * inglês. Quase todas tinham a MESMA forma — quebra no meio da frase:
     * <pre>
     * "When you're ready to see me, just go\Nto Port Blanc and say your name is Candy."
     * "If we don't get back to Port Blanc soon,\NGottn's gonna yell at us again."
     * "Well, I don't actually know which of\Nthe three ships is the Sadalahn."
     * </pre>
     * O caminho de sempre tirava a quebra, traduzia a frase inteira e RECOLOCAVA a quebra por
     * heurística de posição. Quando errava, o portão reprovava por quebra perdida e a fala
     * continuava em inglês — pior que qualquer quebra mal posta.
     *
     * <p>INVARIANTES DO DOMÍNIO: o número de linhas não muda, a quebra fica onde estava, e o
     * inglês some das duas metades.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: fala em inglês no arquivo depois das três rotas
     * significa que a tela não cumpre a própria promessa.
     */
    @Test
    @DisplayName("fala partida por \\N sai traduzida com a quebra no mesmo lugar")
    void falaPartidaPorQuebraSaiTraduzidaComAQuebraNoLugar(@TempDir Path temp) throws IOException {
        // O dublê só sabe traduzir texto SEM marcador — como na produção. Cada metade vai
        // separada, então cada uma casa.
        // FALA SEM NOME PROPRIO ESTRANGEIRO de proposito. A primeira versao usava a fala real
        // do ZZ ("...to Port Blanc...") e o teste reprovava por um motivo que NAO e o assunto
        // dele: o contexto do teste e "danmachi" e nao conhece "Port Blanc", entao o detector
        // de idioma barrava a traducao CORRETA como "nao e PT-BR". Isso e um achado separado,
        // e virou entrada na lore do ZZ — mas aqui o que se prova e a MECANICA da quebra.
        llm.ensinar("I will wait for you here,", "Vou esperar por você aqui,");
        llm.ensinar("until the sun goes down.", "até o sol se pôr.");
        Path pastaPt = montar(temp, List.of(new Fala(
            "I will wait for you here,\\Nuntil the sun goes down.",
            "I will wait for you here,\\Nuntil the sun goes down.")));

        Optional<String> saida = revisar(temp, pastaPt);

        String texto = saida.orElse(Files.readString(
            pastaPt.resolve("show_PT-BR.ass"), StandardCharsets.UTF_8));
        assertFalse(texto.contains("I will wait"),
            "a fala tinha de sair do ingles:" + texto);
        assertTrue(texto.contains("\\N"),
            "a quebra tinha de continuar la, no mesmo lugar:" + texto);
        assertTrue(texto.contains("Vou esperar por você aqui,\\Naté o sol se pôr."),
            "as duas metades traduzidas, unidas pela MESMA quebra:" + texto);
        assertEquals(1, contarDialogos(texto), "a fala nao pode virar duas");
    }

    /**
     * CONTRA-CASO: quebra que NÃO parte frase — decoração de typesetting, com a quebra no fim —
     * não entra nesta rota. Sem ele, a rota tentaria dividir cartões de letreiro em pedaços.
     */
    @Test
    @DisplayName("quebra que nao parte frase nao entra na rota por segmento")
    void quebraDeDecoracaoNaoEntraNaRota(@TempDir Path temp) throws IOException {
        llm.ensinar("Next Episode", "Próximo Episódio");
        Path pastaPt = montar(temp, List.of(new Fala(
            "Next Episode\\N\\N\\N", "Next Episode\\N\\N\\N")));

        Optional<String> saida = revisar(temp, pastaPt);

        String texto = saida.orElse(Files.readString(
            pastaPt.resolve("show_PT-BR.ass"), StandardCharsets.UTF_8));
        assertEquals(1, contarDialogos(texto), "a fala nao pode sumir nem virar duas");
    }
    /**
     * O ESPELHO — ideia de Paulo (2026-08-16): a legenda ORIGINAL é a referência de estrutura,
     * e ela sempre existe. Medido no Guilty Crown no mesmo dia: 4 falas não traduzidas COM tag
     * inline que nenhuma das duas etapas resolvia — o LLM devolvia
     * {@code LLM_SEM_CONTEUDO_UTILIZAVEL} (marcador perdido) e o Google, {@code TAG_CORROMPIDA}.
     *
     * <p>O dublê aqui reproduz exatamente esse defeito: ele só sabe traduzir o texto <b>sem
     * marcador</b>. Com o mascaramento normal a fala chega como {@code [[TAG0]]…} e ele não casa,
     * então a 1ª tentativa falha — como na produção. O espelho refaz o pedido com o texto visível.
     *
     * <p>E o que se perde é o que Paulo autorizou perder: <b>a ênfase inline sai</b>. O prefixo
     * fica, porque ali mora posicionamento.
     */
    @Test
    @DisplayName("espelho: fala não traduzida com tag inline sai traduzida, sem o itálico")
    void falaNaoTraduzidaComTagInlineSaiTraduzidaPeloEspelho(@TempDir Path temp) throws IOException {
        llm.ensinar("What is this?!", "O que é isso?!");
        Path pastaPt = montar(temp, List.of(
            new Fala("What {\\i1}is{\\i0} this?!", "What {\\i1}is{\\i0} this?!")));

        String saida = revisar(temp, pastaPt).orElseThrow(() -> new AssertionError(
            "a fala com tag inline continuou sem tradução: é exatamente o caso que o espelho existe "
                + "para resolver"));

        assertTrue(saida.contains("O que é isso?!"),
            "a tradução do espelho não chegou ao arquivo:\n" + saida);
        assertFalse(saida.contains("What {\\i1}is{\\i0} this?!"),
            "a fala não podia continuar em inglês:\n" + saida);
        assertFalse(saida.contains("{\\i1}"),
            "a ênfase inline sai de propósito (decisão de Paulo): recolocá-la por posição "
                + "italicizaria a palavra errada em português");
    }

    @Test
    @DisplayName("arquivo são não consulta o modelo nem regrava")
    void arquivoSaoNaoChegaAoModelo(@TempDir Path temp) throws IOException {
        llm.ensinar("cansado", "cansada");
        Path pastaPt = montar(temp, List.of(
            new Fala("She is very tired.", "Ela está cansada."),
            new Fala("The weather is nice today.", "O tempo está bom hoje.")));

        Optional<String> saida = revisar(temp, pastaPt);

        assertEquals(0, llm.chamadas(),
            "fala correta chegou ao modelo: em um episódio de 400 falas isso é custo puro");
        assertTrue(saida.isEmpty(),
            "arquivo são foi regravado sem nada ter mudado:\n" + saida.orElse(""));
    }
}
