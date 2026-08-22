package org.traducao.projeto.raspagemRevisao.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.raspagemRevisao.domain.ContextoRevisao;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa o portão único de correção. Ele decide o que entra numa legenda já
 * publicada, e as três origens de proposta (regra determinística, memória do arquivo, IA/Google)
 * passam pela MESMA régua — este teste é o que impede a régua de se soltar de uma delas.
 *
 * <p>INVARIANTES DO DOMÍNIO: o portão só aprova com melhora mensurável; termo canônico alterado e
 * proposta idêntica à fala atual são vetos incondicionais.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer aprovação indevida reprova o teste.
 */
@QuarkusTest
class GuardaCorrecaoSeguraTest {

    @Inject
    GuardaCorrecaoSegura guarda;

    /**
     * O MESMO auditor que a produção usa. Motivo escrito à mão no teste não casa com o
     * vocabulário real, e a comparação "problema novo?" passa a acusar tudo — foi assim que a
     * última pergunta do portão ficou sem teste apesar de existir um com esse nome.
     */
    @Inject
    AuditorProblemasLegendaService auditor;

    private static final ContextoRevisao SEM_LORE = new ContextoRevisao("teste", "", Set.of());

    private ContextoRevisao comTermo(String termo) {
        return new ContextoRevisao("teste", "", Set.of(termo));
    }

    private ResultadoDeteccaoConcordancia suspeitaCom(String... motivos) {
        return new ResultadoDeteccaoConcordancia(true, List.of(motivos));
    }

    /**
     * A FALA REAL DO ZETA, S01E25 evento 9, medida no acervo em 17/08/2026. Ela sobreviveu à
     * corrida da 3.1 ainda em inglês porque a pergunta de repetição a recusou — e recusou também
     * a proposta do Google, na etapa seguinte da cascata.
     *
     * <p>O mecanismo: o inglês tem {@code to} DUAS vezes ({@code went to} … {@code to prepare}),
     * e a tradução honesta repete {@code para} duas vezes. Como {@code para} aparecia ZERO vezes
     * no texto que estava na legenda — que era o próprio inglês —, a regra comparativa leu
     * "repetição introduzida". Comparar contagem de palavra PORTUGUESA contra texto INGLÊS não
     * mede nada.
     *
     * <p>Quando a fala ainda é o original, a proposta é TRADUÇÃO e não refinamento: não existe
     * "antes" em português com que comparar fluência, então essa pergunta se abstém. As outras
     * cinco continuam valendo — inclusive nesta fala.
     */
    @Test
    void traduzirFalaAindaEmInglesNaoTropecaNaRegraDeRepeticao() {
        String ingles = "{\\i1}So, you're saying the Titans went to{\\i0}\\N"
            + "{\\i1}Side Four to prepare a colony drop?{\\i0}";
        String traducao = "{\\i1}Então você está dizendo que os Titans foram{\\i0}\\N"
            + "{\\i1}para o Side Four para preparar uma queda de colônia?{\\i0}";

        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            ingles, ingles, traducao,
            suspeitaCom("Fala não traduzida (idêntica ao original em inglês)"), SEM_LORE);

        assertTrue(aprovou(veredicto),
            "a fala está EM INGLÊS: a proposta é tradução, não refinamento. Recusar aqui é "
                + "recusar exatamente o trabalho que a 3.1 existe para fazer. Veredicto: "
                + veredicto);
    }

    /**
     * FOUR MURASAME — a decisão do Paulo em 17/08: *"usa a própria regra do sistema, nada de
     * reescrever"*.
     *
     * <p>O mapa de terminologia NÃO podia resolver: ele é indexado pela forma ERRADA, e a chave
     * {@code Quatro} já pertence ao Quattro Bajeena (o Char). Dois personagens chegam ao
     * português como a mesma palavra, e o mapa não sabe separá-los. A REGRA sabe, porque olha o
     * inglês: se o original tem {@code Four} e a proposta não tem, a proposta é recusada e a fala
     * fica como está.
     *
     * <p>Medido nos 50 ASS do Zeta: 188 falas com {@code Four} maiúsculo isolado, das quais 126
     * já viraram {@code Quatro} no acervo. Esta regra NÃO conserta essas — reparo seria reescrita,
     * e reescrever foi vetado. Ela impede que novas quebrem.
     */
    @Test
    void propostaQueTraduzOnomeFourEhRecusada() {
        ContextoRevisao zeta = comTermo("Four");

        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            "Four could be to me what that person was to Amuro.",
            "Four could be to me what that person was to Amuro.",
            "Quatro poderia ser para mim o que aquela pessoa foi para Amuro.",
            suspeitaCom("Fala não traduzida (idêntica ao original em inglês)"), zeta);

        assertFalse(aprovou(veredicto), "Four Murasame é personagem, não numeral");
        assertEquals(GuardaCorrecaoSegura.MotivoRecusa.TERMO_CANONICO, motivo(veredicto));
    }

    /**
     * O CONTRA-CASO: a MESMA fala, com o nome preservado, passa. Sem isto a entrada nova seria
     * só um jeito de deixar tudo em inglês para sempre.
     */
    @Test
    void mesmaFalaComOnomeFourPreservadoPassa() {
        ContextoRevisao zeta = comTermo("Four");

        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            "Four could be to me what that person was to Amuro.",
            "Four could be to me what that person was to Amuro.",
            "Four poderia ser para mim o que aquela pessoa foi para Amuro.",
            suspeitaCom("Fala não traduzida (idêntica ao original em inglês)"), zeta);

        assertTrue(aprovou(veredicto),
            "com o nome preservado a tradução tem de entrar. Veredicto: " + veredicto);
    }

    /**
     * O CASO REAL do Zeta S01E, medido no acervo em 21/08/2026: a traducao juntou o conteudo
     * das duas metades e a segunda linha da legenda ficou com um unico "!".
     */
    @Test
    void propostaQueDeixaSoPontuacaoDentroDoParEhRecusada() {
        String ingles = "{\\i1}Stop it! Don't stick pins into him{\\i0}\\N{\\i1}any further!{\\i0}";
        String quebrada = "{\\i1}Pare com isso! Nao coloque mais alfinetes nele{\\i0}\\N{\\i1}!{\\i0}";

        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            ingles, ingles, quebrada,
            suspeitaCom("Fala nao traduzida (identica ao original em ingles)"), SEM_LORE);

        assertFalse(aprovou(veredicto), "a 2a linha viraria so um '!' na tela");
        assertEquals(GuardaCorrecaoSegura.MotivoRecusa.TAG_SEM_CONTEUDO, motivo(veredicto));
    }

    /** Guilty Crown ep14 #114: o par ficou LITERALMENTE vazio e a enfase morreu. */
    @Test
    void propostaComParDeTagsVazioEhRecusada() {
        String ingles = "there's no {\\i1}love{\\i0} in this kind of killing.";
        String vazia = "nao ha amor {\\i1}{\\i0} neste tipo de assassinato.";

        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            ingles, ingles, vazia,
            suspeitaCom("Fala nao traduzida (identica ao original em ingles)"), SEM_LORE);

        assertFalse(aprovou(veredicto));
        assertEquals(GuardaCorrecaoSegura.MotivoRecusa.TAG_SEM_CONTEUDO, motivo(veredicto));
    }

    /**
     * CONTROLE 1 — a fala que sai CERTA. Guilty Crown ep03: a tag muda de lugar porque a ordem
     * das palavras muda entre os idiomas, e continua envolvendo palavra. Das 401 falas expostas
     * no acervo, 385 sao assim: recusar aqui reprovaria o trabalho correto.
     */
    @Test
    void propostaQueMantemPalavraDentroDoParPassa() {
        String ingles = "It's all {\\i1}your{\\i0} fault!";
        String boa = "E tudo {\\i1}sua{\\i0} culpa!";

        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            ingles, ingles, boa,
            suspeitaCom("Fala nao traduzida (identica ao original em ingles)"), SEM_LORE);

        assertTrue(aprovou(veredicto), "tag no miolo com palavra dentro e correto. " + veredicto);
    }

    /**
     * CONTROLE 2 — legenda de DUAS LINHAS. A quebra entre os dois pares nao tem letra nenhuma,
     * e e legitima: sem esta excecao a regra acusaria toda legenda de duas linhas do acervo.
     */
    @Test
    void quebraDeLinhaEntreDoisParesNaoEhParVazio() {
        String ingles = "{\\i1}I'm going to have Gryps hurry{\\i0}\\N{\\i1}their mobilization.{\\i0}";
        String boa = "{\\i1}Vou fazer Gryps apressar{\\i0}\\N{\\i1}a mobilizacao deles.{\\i0}";

        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            ingles, ingles, boa,
            suspeitaCom("Fala nao traduzida (identica ao original em ingles)"), SEM_LORE);

        assertTrue(aprovou(veredicto), "a quebra entre pares e separador, nao par vazio. " + veredicto);
    }

    /**
     * CONTROLE 3 — proposta SEM tag nenhuma. Perder o italico e decisao do Paulo (17/08: "pode
     * eliminar o italico sem problema algum"); sem par de tags, esta regra nao tem o que dizer.
     */
    @Test
    void propostaSemTagNenhumaNaoEhAcusadaPorParVazio() {
        String ingles = "She let {\\i1}you{\\i0} use the Void Genome.";
        String semTag = "Ela permitiu que voce usasse o Void Genome.";

        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            ingles, ingles, semTag,
            suspeitaCom("Fala nao traduzida (identica ao original em ingles)"), SEM_LORE);

        assertNotEquals(GuardaCorrecaoSegura.MotivoRecusa.TAG_SEM_CONTEUDO,
            veredicto instanceof GuardaCorrecaoSegura.Veredicto.Rejeitada r ? r.motivo() : null,
            "sem par de tags a regra nao se aplica");
    }

    /**
     * O SEGUNDO efeito colateral da abstenção, pego em PRODUÇÃO quatro dias depois — Gundam ZZ
     * ep29, corrida de 21/08/2026:
     * <pre>
     * EN : It {\i1}is{\i0} you, Roux Louka!
     * PT : É {\i1}é{\i0} você, Roux Louka!     &lt;- "It is" virou "É é"
     * </pre>
     *
     * <p>A tag no miolo parte a frase e o tradutor duplica a palavra. O portão JÁ TINHA a
     * checagem que pega isso — {@code colarPalavrasIguais} normaliza caixa e parte em
     * não-letras, então {@code "É é você"} vira {@code [é, é, você]}, adjacentes e iguais.
     *
     * <p><b>Ela não rodou porque eu a desliguei.</b> A abstenção de {@code 50ab80ac} — criada
     * para destravar a tradução de fala em inglês — desligava as DUAS perguntas de uma vez,
     * porque moravam no mesmo método. Contar palavra portuguesa contra texto inglês não mede
     * nada e devia mesmo se abster; "a proposta colou duas palavras iguais?" independe de
     * idioma e nunca devia ter parado.
     */
    @Test
    void propostaQueColaPalavraEmFalaAindaEmInglesEhRecusada() {
        String ingles = "It {\\i1}is{\\i0} you, Roux Louka!";
        String colada = "É {\\i1}é{\\i0} você, Roux Louka!";

        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            ingles, ingles, colada,
            suspeitaCom("Fala não traduzida (idêntica ao original em inglês)"), SEM_LORE);

        assertFalse(aprovou(veredicto),
            "'É é' é palavra colada que o original não colava — a fala em inglês não isenta "
                + "desta pergunta, só da contagem. Veredicto: " + veredicto);
        assertEquals(GuardaCorrecaoSegura.MotivoRecusa.REPETICAO_INTRODUZIDA, motivo(veredicto));
    }

    /**
     * O CONTRA-CASO que impede o conserto acima de reabrir o que foi consertado em
     * {@code 50ab80ac}: a fala do Zeta repete {@code para} duas vezes por gramática, e os dois
     * NÃO são adjacentes. A contagem continua abstendo; a adjacência não acusa.
     */
    @Test
    void repeticaoLegitimaEmFalaEmInglesContinuaPassando() {
        String ingles = "{\\i1}So, you're saying the Titans went to{\\i0}\\N"
            + "{\\i1}Side Four to prepare a colony drop?{\\i0}";
        String traducao = "{\\i1}Então, você está dizendo que os Titans foram{\\i0}\\N"
            + "{\\i1}para Side Four para preparar uma queda de colônia?{\\i0}";

        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            ingles, ingles, traducao,
            suspeitaCom("Fala não traduzida (idêntica ao original em inglês)"), SEM_LORE);

        assertTrue(aprovou(veredicto),
            "os dois 'para' não são adjacentes; recusar aqui devolveria a fala ao inglês. "
                + "Veredicto: " + veredicto);
    }

    /**
     * E a fala que JÁ colava continua passando — {@code "Ha ha ha ha ha!"} é gargalhada, não
     * defeito. A pergunta só acusa o que a PROPOSTA acrescenta.
     */
    @Test
    void falaQueJaColavaPalavraContinuaPassando() {
        String atual = "Ha ha ha ha ha!";
        String proposta = "Ha ha ha ha ha, Judau!";

        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            "Ha ha ha ha ha!", atual, proposta,
            suspeitaCom("Fala não traduzida (idêntica ao original em inglês)"), SEM_LORE);

        assertNotEquals(GuardaCorrecaoSegura.MotivoRecusa.REPETICAO_INTRODUZIDA,
            veredicto instanceof GuardaCorrecaoSegura.Veredicto.Rejeitada r ? r.motivo() : null,
            "repetição que a fala JÁ tinha não pode ser acusada de introduzida");
    }

    /**
     * O EFEITO COLATERAL da correção acima, pego em PRODUÇÃO no mesmo dia: destravada a
     * tradução, o Google devolveu a fala com o {@code \N} e três tags a menos — duas linhas de
     * legenda viraram uma linha longa com itálico que nunca fecha. As cinco perguntas antigas
     * aprovaram, porque nenhuma media ESTRUTURA.
     *
     * <p>É por isso que a correção de um gap tem de rodar contra o acervo antes de ser dada por
     * boa: o defeito seguinte estava escondido atrás do primeiro.
     */
    @Test
    void propostaQueJuntaAsDuasLinhasEhRejeitada() {
        String ingles = "{\\i1}So, you're saying the Titans went to{\\i0}\\N"
            + "{\\i1}Side Four to prepare a colony drop?{\\i0}";
        String semQuebra = "{\\i1}Então, você está dizendo que o Titans foi para Side Four "
            + "se preparar para um lançamento de colônia?";

        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            ingles, ingles, semQuebra,
            suspeitaCom("Fala não traduzida (idêntica ao original em inglês)"), SEM_LORE);

        assertFalse(aprovou(veredicto),
            "perder o \\N junta duas linhas de legenda numa só: é layout, não estilo");
        assertEquals(GuardaCorrecaoSegura.MotivoRecusa.QUEBRA_DE_LINHA_PERDIDA, motivo(veredicto));
    }

    /**
     * O CONTRA-CASO da regra de quebra: a MESMA tradução, preservando o {@code \N}, passa. Sem
     * isto a regra nova seria só um jeito novo de recusar tudo.
     */
    @Test
    void mesmaTraducaoComQuebraPreservadaPassa() {
        String ingles = "{\\i1}So, you're saying the Titans went to{\\i0}\\N"
            + "{\\i1}Side Four to prepare a colony drop?{\\i0}";
        String comQuebra = "{\\i1}Então, você está dizendo que os Titans foram{\\i0}\\N"
            + "{\\i1}para Side Four preparar uma queda de colônia?{\\i0}";

        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            ingles, ingles, comQuebra,
            suspeitaCom("Fala não traduzida (idêntica ao original em inglês)"), SEM_LORE);

        assertTrue(aprovou(veredicto),
            "tradução que respeita a estrutura tem de passar. Veredicto: " + veredicto);
    }

    /**
     * O CONTRA-CASO, e ele é o que impede a correção acima de virar afrouxamento: quando a fala
     * JÁ está em português, a pergunta de repetição continua valendo integralmente. É a cicatriz
     * do 86 — o mistral dobrando o advérbio a cada rodada.
     */
    @Test
    void falaJaEmPortuguesContinuaBarrandoRepeticaoIntroduzida() {
        String ingles = "It probably just thinks that he's a good bed.";
        String atual = "Provavelmente ela só acha que ele é uma boa cama.";
        String dobrado = "Provavelmente, ela provavelmente pensa que ele é uma boa cama.";

        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            ingles, atual, dobrado, suspeitaCom("concordância de gênero"), SEM_LORE);

        assertFalse(aprovou(veredicto),
            "com a fala já em português a regra tem base de comparação e TEM de barrar");
        assertEquals(GuardaCorrecaoSegura.MotivoRecusa.REPETICAO_INTRODUZIDA, motivo(veredicto));
    }

    private boolean aprovou(GuardaCorrecaoSegura.Veredicto veredicto) {
        return veredicto instanceof GuardaCorrecaoSegura.Veredicto.Aprovada;
    }

    private List<String> avisos(GuardaCorrecaoSegura.Veredicto veredicto) {
        return assertInstanceOf(GuardaCorrecaoSegura.Veredicto.Rejeitada.class, veredicto)
            .avisosAoOperador();
    }

    private GuardaCorrecaoSegura.MotivoRecusa motivo(GuardaCorrecaoSegura.Veredicto veredicto) {
        return assertInstanceOf(GuardaCorrecaoSegura.Veredicto.Rejeitada.class, veredicto).motivo();
    }

    @Test
    void propostaVaziaOuNulaEhRejeitadaEmSilencio() {
        for (String candidata : new String[]{null, "", "   "}) {
            GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
                "He is tired.", "Ele está cansada.", candidata,
                suspeitaCom("concordância de gênero"), SEM_LORE);

            assertFalse(aprovou(veredicto), "candidata=[" + candidata + "] não podia passar");
            assertTrue(avisos(veredicto).isEmpty(),
                "rejeição por texto vazio não fala com o operador: o desfecho já aparece como pendente");
        }
    }

    /**
     * Não é purismo: aprovar uma proposta igual à fala atual contaria uma correção que não houve,
     * marcaria o arquivo como modificado e dispararia regravação e backup de um .ass idêntico.
     */
    @Test
    void propostaIdenticaAFalaAtualEhRejeitada() {
        String atual = "Ele está cansada.";

        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            "He is tired.", atual, atual, suspeitaCom("concordância de gênero"), SEM_LORE);

        assertFalse(aprovou(veredicto));
    }

    /**
     * O veto de lore é o único que o operador PRECISA ver: a proposta parecia boa e foi barrada por
     * um termo da obra, e é ele quem decide se o termo está certo no catálogo.
     */
    @Test
    void alterarTermoCanonicoRejeitaEAvisaOperador() {
        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            "The Hyaku Shiki is ready.", "O Hyaku Shiki está pronta.", "O Cem Tipos está pronto.",
            suspeitaCom("concordância de gênero"), comTermo("Hyaku Shiki"));

        assertFalse(aprovou(veredicto), "trocar termo canônico não pode passar");
        assertEquals(1, avisos(veredicto).size());
        assertTrue(avisos(veredicto).get(0).contains("Hyaku Shiki"),
            "o aviso precisa nomear o termo, senão o operador não sabe o que revisar");
    }

    /** A concordância corrigida é o caso central da Opção 6: sai suspeita, entra limpa. */
    @Test
    void correcaoQueResolveOProblemaEhAprovada() {
        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            "He is tired.", "Ele está cansada.", "Ele está cansado.",
            suspeitaCom("concordância de gênero"), SEM_LORE);

        assertTrue(aprovou(veredicto), "correção válida foi barrada pelo portão");
    }

    /**
     * A pergunta que dá sentido às outras quatro: gastar uma chamada externa, um backup e uma
     * regravação para trocar um defeito por outro do mesmo tamanho não é melhora.
     */
    @Test
    void propostaQueNaoMelhoraAAuditoriaEhRejeitada() {
        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            "He is tired.", "Ele está cansada.", "Ela está cansado.",
            suspeitaCom("concordância de gênero"), SEM_LORE);

        assertFalse(aprovou(veredicto),
            "proposta ainda suspeita e sem redução de motivos não pode substituir a fala");
    }

    /**
     * O CASO REAL, byte a byte do relatório de 2026-08-16 (86 Part 1, ep03, evento 271). O detector
     * acertou — o inglês diz {@code he} e a tradução dizia {@code ela} —, e a proposta do LLM
     * conserta o gênero DOBRANDO o advérbio. Antes desta guarda a fala foi gravada assim; rodando a
     * mesma opção de menu de novo, a via seguinte apagou o sujeito.
     */
    @Test
    void propostaQueDobraAdverbioEhRejeitadaEAvisaOperador() {
        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            "It probably just thinks that he's a good bed.",
            "Provavelmente, ele apenas pensa que ela é uma boa cama.",
            "Provavelmente, ela provavelmente pensa que ele é uma boa cama.",
            suspeitaCom("concordância de gênero"), SEM_LORE);

        assertFalse(aprovou(veredicto), "proposta que dobra o advérbio não pode entrar na legenda");
        assertEquals(1, avisos(veredicto).size());
        assertTrue(avisos(veredicto).get(0).contains("repete uma palavra"),
            "o operador precisa saber POR QUE a correção foi barrada, senão parece defeito da fila");
        assertEquals(GuardaCorrecaoSegura.MotivoRecusa.REPETICAO_INTRODUZIDA, motivo(veredicto),
            "sem o motivo tipado o relatório volta a dizer só 'sem melhoria'");
    }

    /**
     * O motivo viaja com a recusa INCLUSIVE quando ela é silenciosa no console. Silencioso é sobre
     * o console; o relatório continua tendo de dizer por que a fala ficou pendente — e é isso que
     * separa "não havia o que fazer" de "não consegui".
     */
    @Test
    void todaRecusaCarregaOMotivoTipado() {
        assertEquals(GuardaCorrecaoSegura.MotivoRecusa.VAZIA_OU_IGUAL,
            motivo(guarda.avaliar("He is tired.", "Ele está cansada.", "   ",
                suspeitaCom("concordância de gênero"), SEM_LORE)));

        assertEquals(GuardaCorrecaoSegura.MotivoRecusa.TERMO_CANONICO,
            motivo(guarda.avaliar("The Hyaku Shiki is ready.", "O Hyaku Shiki está pronta.",
                "O Cem Tipos está pronto.", suspeitaCom("concordância de gênero"),
                comTermo("Hyaku Shiki"))));

        // Descoberto ao escrever este teste, e vale registrar: esta proposta NÃO chega ao
        // "sem melhoria". Ela é barrada antes, por PROBLEMA_NOVO — trocar "Ele está cansada"
        // por "Ela está cansado" inverte o defeito em vez de repeti-lo. O teste vizinho
        // propostaQueNaoMelhoraAAuditoriaEhRejeitada, que só afirma "não passou", nunca
        // exercitou a última pergunta apesar do nome. O motivo tipado é o que tornou isso visível.
        assertEquals(GuardaCorrecaoSegura.MotivoRecusa.PROBLEMA_NOVO,
            motivo(guarda.avaliar("He is tired.", "Ele está cansada.", "Ela está cansado.",
                suspeitaCom("concordância de gênero"), SEM_LORE)));
    }

    /**
     * A ÚLTIMA pergunta do portão, exercitada de verdade — e ela exige o vocabulário REAL de
     * motivos, não uma string sintética: {@code SEM_MELHORIA} só é alcançado quando a proposta
     * não traz problema novo E repete exatamente os motivos que a fala já tinha. Com motivo
     * inventado no teste, qualquer motivo apurado pelo auditor conta como "novo" e a execução
     * para uma pergunta antes.
     */
    @Test
    void propostaComOMesmoDefeitoDoOriginalParaNaUltimaPergunta() {
        String en = "He is tired.";
        String atual = "Ele está cansada.";

        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            en, atual, "Ele está cansada!", auditor.auditar(en, atual), SEM_LORE);

        assertFalse(aprovou(veredicto));
        assertEquals(GuardaCorrecaoSegura.MotivoRecusa.SEM_MELHORIA, motivo(veredicto),
            "mudar a pontuação e manter o defeito gasta chamada externa e backup para nada");
    }

    /**
     * O contra-teste que impede a guarda de virar alarme falso, e ele é o mais importante dos dois:
     * repetição legítima existe em 197 das 7.022 falas de diálogo do 86 (2,81%) — {@code "Pare,
     * pare!"}, {@code "Certo, certo."}. A régua é COMPARATIVA: repetição que a fala já tinha passa,
     * porque a proposta não a introduziu. Uma régua absoluta reprovaria as 197.
     */
    @Test
    void repeticaoQueJaExistiaNaFalaNaoBarraACorrecao() {
        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            "Stop, stop! He is tired.",
            "Pare, pare! Ele está cansada.",
            "Pare, pare! Ele está cansado.",
            suspeitaCom("concordância de gênero"), SEM_LORE);

        assertTrue(aprovou(veredicto),
            "a repetição já estava na fala: barrar aqui é o alarme falso que faz desligar a guarda");
    }

    /**
     * O FURO que a lente adversarial achou sobre a própria guarda: o piso de 4 letras deixa passar
     * {@code "ele ele"}. Não é hipótese — na 2ª rodada o mistral devolveu
     * {@code "Provavelmente, provavelmente pensa…"}, adjacente, e só não escapou porque a palavra é
     * longa. A adjacência fecha isso sem baixar o piso.
     */
    @Test
    void propostaQueColaPalavraCurtaRepetidaEhRejeitada() {
        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            "He thinks she is tired.", "Ele pensa que ela está cansado.",
            "Ele ele pensa que ela está cansada.",
            suspeitaCom("concordância de gênero"), SEM_LORE);

        assertFalse(aprovou(veredicto), "'ele ele' escapava do piso de 4 letras");
        assertTrue(avisos(veredicto).get(0).contains("repete uma palavra"));
    }

    /**
     * O contra-teste que protege o TRABALHO PRINCIPAL da 3.1, e é o motivo de a adjacência ser
     * exigida em vez de simplesmente baixar o piso: corrigir pronome legitimamente ACRESCENTA o
     * sujeito, e {@code ele} passa de 1 para 2 sem nenhum defeito. Piso baixo reprovaria justamente
     * a correção que a ferramenta existe para fazer; a adjacência não, porque pronome acrescentado
     * nunca cola no anterior.
     */
    @Test
    void pronomeAcrescentadoLongeDoOutroNaoBarraACorrecao() {
        GuardaCorrecaoSegura.Veredicto veredicto = guarda.avaliar(
            "He said he would come.", "Ele disse que viria cansada.",
            "Ele disse que ele viria cansado.",
            suspeitaCom("concordância de gênero"), SEM_LORE);

        assertTrue(aprovou(veredicto),
            "acrescentar o pronome é o trabalho da 3.1: barrar aqui inutilizaria a ferramenta");
    }

    /**
     * A lista de avisos é entregue ao laço, que a imprime. Se fosse mutável, um chamador distraído
     * poderia alterar a narração de uma decisão já tomada.
     */
    @Test
    void avisosSaoImutaveis() {
        List<String> lista = avisos(guarda.avaliar(
            "The Hyaku Shiki is ready.", "O Hyaku Shiki está pronta.", "O Cem Tipos está pronto.",
            suspeitaCom("concordância de gênero"), comTermo("Hyaku Shiki")));

        assertThrowsUnsupported(() -> lista.add("intruso"));
    }

    private void assertThrowsUnsupported(Runnable acao) {
        try {
            acao.run();
        } catch (UnsupportedOperationException esperado) {
            return;
        }
        throw new AssertionError("a lista de avisos aceitou mutação");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: marcador técnico na legenda é defeito que o ESPECTADOR lê.
     *
     * <h2>A cicatriz, achada no acervo em 22/08/2026</h2>
     * Depois de as 12 obras retraduzidas passarem pela 3.1, uma varredura nos 287.052 eventos
     * encontrou DUAS falas gravadas com marcador cru:
     * <pre>
     * ZZ      : Oh, [[Lady Haman]]. [[Lady Haman]]! [[Lady Haman]]!
     * DanMachi: {\\an8}[[]TAG0] Atenção, hóspedes, o Spoon Aqua partirá agora.
     * </pre>
     * O primeiro o MODELO inventou — nem o protetor de lore (que usa {@code ZXQLORE0QXZ}) nem o
     * mascarador (que usa {@code [[TAG0]]}) produzem {@code [[Nome]]}. O segundo é um
     * {@code [[TAG0]]} MUTILADO. As duas passaram por TODAS as validações do portão, porque
     * nenhuma olhava para isto.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: a proposta com marcador é gravada e o espectador lê
     * "[[Lady Haman]]" na tela.
     */
    @Test
    @DisplayName("proposta com marcador tecnico [[...]] e recusada")
    void propostaComMarcadorTecnicoERecusada() {
        GuardaCorrecaoSegura.Veredicto v = guarda.avaliar(
            "Oh, Lady Haman. Lady Haman! Lady Haman!",
            "Oh, Lady Haman. Lady Haman! Lady Haman!",
            "Oh, [[Lady Haman]]. [[Lady Haman]]! [[Lady Haman]]!",
            suspeitaCom("Fala não traduzida (idêntica ao original em inglês)"), SEM_LORE);

        assertInstanceOf(GuardaCorrecaoSegura.Veredicto.Rejeitada.class, v);
        assertEquals(GuardaCorrecaoSegura.MotivoRecusa.MARCADOR_TECNICO_VAZADO,
            ((GuardaCorrecaoSegura.Veredicto.Rejeitada) v).motivo());
    }

    /** O marcador MUTILADO — a outra forma medida no acervo — também é recusado. */
    @Test
    @DisplayName("marcador de tag mutilado tambem e recusado")
    void marcadorMutiladoTambemERecusado() {
        GuardaCorrecaoSegura.Veredicto v = guarda.avaliar(
            "Attention, guests.", "Attention, guests.",
            "{\\\\an8}[[]TAG0] Atenção, hóspedes.",
            suspeitaCom("Fala não traduzida (idêntica ao original em inglês)"), SEM_LORE);

        assertInstanceOf(GuardaCorrecaoSegura.Veredicto.Rejeitada.class, v);
    }

    /**
     * CONTRA-CASO: a proposta LIMPA continua sendo aceita. Sem ele, a guarda poderia recusar
     * tudo e o teste anterior passaria do mesmo jeito.
     */
    @Test
    @DisplayName("proposta sem marcador continua sendo aceita")
    void propostaSemMarcadorContinuaAceita() {
        GuardaCorrecaoSegura.Veredicto v = guarda.avaliar(
            "Oh, Lady Haman.", "Oh, Lady Haman.", "Ah, Lady Haman.",
            suspeitaCom("Fala não traduzida (idêntica ao original em inglês)"), SEM_LORE);

        assertInstanceOf(GuardaCorrecaoSegura.Veredicto.Aprovada.class, v,
            "a guarda de marcador não pode barrar proposta limpa");
    }
}
