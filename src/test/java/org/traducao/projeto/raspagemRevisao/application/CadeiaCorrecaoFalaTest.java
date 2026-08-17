package org.traducao.projeto.raspagemRevisao.application;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.raspagemRevisao.domain.ContextoRevisao;
import org.traducao.projeto.raspagemRevisao.domain.DecisaoFala;
import org.traducao.projeto.raspagemRevisao.domain.ModoRevisaoLegendas;
import org.traducao.projeto.raspagemRevisao.domain.PoliticaRetraducao;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova as duas coisas que a cadeia de correção promete e que nenhum outro
 * teste consegue afirmar — que as fontes são tentadas na ORDEM DE CUSTO, e que uma tentativa barrada
 * no meio do caminho não some do console.
 *
 * <p>INVARIANTES DO DOMÍNIO: chamada externa é a ÚLTIMA opção. O dublê conta as chamadas, então
 * "não chamou" é uma afirmação medida, não uma leitura de código.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer chamada externa desnecessária reprova — num lote de
 * 23 episódios, uma fonte fora de ordem é a diferença entre minutos e dezenas de minutos.
 */
@QuarkusTest
@TestProfile(CadeiaCorrecaoFalaTest.PerfilComTradutorDublado.class)
class CadeiaCorrecaoFalaTest {

    /** Escopa a alternativa a este teste; um dublê global mudaria a suíte inteira. */
    public static class PerfilComTradutorDublado implements QuarkusTestProfile {
        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(RecuperacaoExternaContadora.class, LlmCorretorDublado.class);
        }
    }

    @Inject
    CadeiaCorrecaoFala cadeia;

    @Inject
    RecuperacaoExternaContadora tradutorExterno;

    @Inject
    LlmCorretorDublado llm;

    private static final ContextoRevisao SEM_LORE = new ContextoRevisao("teste", "", Set.of());

    @BeforeEach
    void limpar() {
        tradutorExterno.reiniciar();
        llm.reiniciar();
    }

    private EventoLegenda fala(String texto) {
        return new EventoLegenda(7, "Dialogue", "Default",
            "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,", texto);
    }

    /**
     * O motivo NAO e decorativo: o provedor Google so aceita falas cujo motivo esteja na
     * {@link PoliticaRetraducao} dele. Com um motivo qualquer ("concordancia"), ele RECUSA antes de
     * tocar a rede -- e um teste que contasse chamadas veria zero por um motivo que nao e o que
     * queria medir. Descoberto exatamente assim.
     */
    private CadeiaCorrecaoFala.FalaSuspeita suspeita(String originalEn, String traducaoAtual) {
        return new CadeiaCorrecaoFala.FalaSuspeita(fala(traducaoAtual), originalEn, traducaoAtual,
            true, new ResultadoDeteccaoConcordancia(true, List.of(PoliticaRetraducao.NAO_TRADUZIDA)));
    }

    private CadeiaCorrecaoFala.Tentativa decidir(SessaoRevisaoArquivo sessao, String en, String pt) {
        return cadeia.decidir(sessao, suspeita(en, pt), "ep01.ass",
            ModoRevisaoLegendas.GOOGLE, SEM_LORE);
    }

    /**
     * A promessa central: o que uma regra local resolve NÃO vai para a rede. Sem isto, a cadeia
     * seria só uma sequência de tentativas, e a economia existiria por acidente.
     */
    @Test
    void regraDeterministicaResolveESemNenhumaChamadaExterna() {
        CadeiaCorrecaoFala.Tentativa tentativa = decidir(
            new SessaoRevisaoArquivo(), "My dad needs to know!", "Minha mãe precisa saber disso!");

        DecisaoFala.Corrigir corrigir = assertInstanceOf(DecisaoFala.Corrigir.class, tentativa.decisao());
        assertEquals("Meu pai precisa saber disso!", corrigir.texto());
        assertEquals(0, tradutorExterno.chamadas(),
            "a regra local resolveu: nenhuma fala podia ter ido para a rede");
    }

    /** A evidência é o que alimenta o relatório e o dataset; sem ela a correção fica invisível. */
    @Test
    void correcaoDeterministicaRegistraEvidenciaComOSeuProprioRotulo() {
        CadeiaCorrecaoFala.Tentativa tentativa = decidir(
            new SessaoRevisaoArquivo(), "My dad needs to know!", "Minha mãe precisa saber disso!");

        assertEquals(1, tentativa.evidencias().size());
        assertEquals("CORRIGIDA_REGRA_SEGURA", tentativa.evidencias().get(0).resultado(),
            "o rótulo distingue no dataset o que custou rede do que não custou");
    }

    /**
     * A segunda fonte: a mesma frase, de novo, no mesmo arquivo. A rede é consultada UMA vez.
     * É o que torna suportável um episódio cheio de bordões repetidos.
     */
    @Test
    void segundaOcorrenciaDaMesmaFalaReaproveitaEmVezDeChamarDeNovo() {
        SessaoRevisaoArquivo sessao = new SessaoRevisaoArquivo();

        decidir(sessao, "Get out of there!", "Get out of there!");
        int aposPrimeira = tradutorExterno.chamadas();
        decidir(sessao, "Get out of there!", "Get out of there!");

        assertEquals(1, aposPrimeira, "a primeira ocorrência precisa mesmo pagar a chamada");
        assertEquals(1, tradutorExterno.chamadas(),
            "a segunda ocorrência tinha de sair da memória do arquivo, não da rede");
    }

    /** A memória morre com o arquivo: outra sessão não herda decisão nenhuma. */
    @Test
    void memoriaNaoAtravessaArquivos() {
        decidir(new SessaoRevisaoArquivo(), "Get out of there!", "Get out of there!");
        decidir(new SessaoRevisaoArquivo(), "Get out of there!", "Get out of there!");

        assertEquals(2, tradutorExterno.chamadas(),
            "a mesma frase em OUTRO episódio pode ter outro sujeito e outro gênero");
    }

    /**
     * O caso que exigiu acumular os avisos: a regra local propôs, o portão vetou por lore, e a
     * cadeia SEGUIU. Se o veto não viajasse até a decisão final, a tentativa barrada sumiria do
     * console e o operador veria a fala ir para a rede sem saber por quê.
     */
    @Test
    void vetoDeUmaFonteAnteriorSobreviveAteADecisaoFinal() {
        // O termo protegido e procurado no ORIGINAL INGLES e exigido na proposta: a regra local
        // troca "Minha mãe" por "Meu pai", e com isso "dad" nao sobrevive.
        ContextoRevisao comLore = new ContextoRevisao("teste", "", Set.of("dad"));

        CadeiaCorrecaoFala.Tentativa tentativa = cadeia.decidir(
            new SessaoRevisaoArquivo(),
            suspeita("My dad needs to know!", "Minha mãe precisa saber disso!"),
            "ep01.ass", ModoRevisaoLegendas.GOOGLE, comLore);

        assertFalse(tentativa.decisao().avisosAoOperador().isEmpty(),
            "o veto da fonte anterior tem de aparecer, mesmo que outra fonte resolva depois");
        assertTrue(tentativa.decisao().avisosAoOperador().get(0).contains("[LORE]"),
            "e tem de vir PRIMEIRO: os avisos contam a história na ordem em que aconteceu");
    }

    /**
     * O DEFEITO MEDIDO EM 2026-08-16, e reproduzido de propósito aqui: a passada Google do 86
     * reportou <b>"Problemas detectados: 2 · Falas pendentes: 2"</b> seguido de <b>"Nenhuma
     * ocorrência detalhada registrada"</b>. Motivo de concordância não é falha objetiva, então o
     * Google não é acionado — e a recusa saía com código {@code null}, que não gera
     * {@link org.traducao.projeto.raspagemRevisao.domain.DetalheRevisao}. A fala era CONTADA como
     * pendente e não aparecia em lugar nenhum.
     *
     * <p>É a regra da saída vazia ambígua: "nada a fazer aqui" e "não consegui" não podem produzir
     * o mesmo sinal. O operador precisa saber que a fala é trabalho da OUTRA passada da mesma tela.
     */
    @Test
    void defeitoForaDoEscopoDaTelaNaoGastaRedeEDeixaEvidencia() {
        String pt = "Provavelmente, ele apenas pensa que ela é uma boa cama.";
        CadeiaCorrecaoFala.FalaSuspeita concordancia = new CadeiaCorrecaoFala.FalaSuspeita(
            fala(pt), "It probably just thinks that he's a good bed.", pt, true,
            new ResultadoDeteccaoConcordancia(true,
                List.of("Original usa 'he' sem referência feminina, mas a tradução contém o feminino 'ela'")));

        CadeiaCorrecaoFala.Tentativa tentativa = cadeia.decidir(
            new SessaoRevisaoArquivo(), concordancia, "ep01.ass",
            ModoRevisaoLegendas.GOOGLE, SEM_LORE);

        assertInstanceOf(DecisaoFala.Pendente.class, tentativa.decisao());
        assertEquals(1, tentativa.evidencias().size(),
            "pendência contada sem evidência é a saída vazia ambígua que o relatório não pode produzir");
        assertEquals("FORA_DO_ESCOPO_DA_TELA", tentativa.evidencias().get(0).resultado(),
            "a tela precisa dizer que viu o defeito e que ele pertence à 3.3");
        assertEquals(0, tradutorExterno.chamadas(),
            "defeito de outra etapa não pode gastar rede aqui");
        assertEquals(0, llm.chamadas(),
            "nem o LLM: o escopo vale para as DUAS etapas, não só para o Google");
    }

    /**
     * O DICIONÁRIO NÃO MEXE EM TERMO DA LORE — guarda nascida de um defeito MEU, medido em
     * produção no Guilty Crown poucas horas depois de eu plugar o dicionário como ajudante:
     *
     * <pre>"Apocalypse Virus"  ->  "Apocalypse Vírus"</pre>
     *
     * <p>Ele acentua {@code Virus} porque em português é assim, e com isso quebra o termo canônico.
     * O portão então recusava a proposta INTEIRA e a fala continuava em inglês — o ajudante custava
     * a tradução que deveria ajudar a entregar. Confirmado com a classe de produção antes de
     * consertar, e visto em 3 rodadas seguidas do acervo.
     */
    @Test
    void dicionarioEhIgnoradoQuandoAlterariaTermoDaLore() {
        // O modelo devolve uma proposta que JÁ preserva o termo canônico. Quem o quebraria depois
        // é o dicionário, acentuando "Virus" — e é exatamente esse passo que a guarda impede.
        llm.ensinar("esforcos", "esforços");
        String pt = "Dedicaremos nossos esforcos para erradicar o Apocalypse Virus";
        CadeiaCorrecaoFala.FalaSuspeita fala = new CadeiaCorrecaoFala.FalaSuspeita(
            fala(pt), "We will devote our efforts to eradicating the Apocalypse Virus", pt, true,
            new ResultadoDeteccaoConcordancia(true, List.of(PoliticaRetraducao.NAO_TRADUZIDA)));

        CadeiaCorrecaoFala.Tentativa tentativa = cadeia.decidir(
            new SessaoRevisaoArquivo(), fala, "ep01.ass",
            ModoRevisaoLegendas.LLM_CONCORDANCIA,
            new ContextoRevisao("teste", "", Set.of("Apocalypse Virus")));

        DecisaoFala.Corrigir corrigir = assertInstanceOf(DecisaoFala.Corrigir.class, tentativa.decisao(),
            "com o dicionário quebrando 'Apocalypse Virus', o portão de lore recusa a proposta "
                + "INTEIRA e a fala fica pendente — foi o que aconteceu 3 rodadas seguidas no acervo");
        assertTrue(corrigir.texto().contains("Apocalypse Virus"),
            "o termo canônico tinha de sobreviver ao dicionário: " + corrigir.texto());
        assertFalse(corrigir.texto().contains("Apocalypse Vírus"),
            "é exatamente esta acentuação que quebra o termo da lore: " + corrigir.texto());
    }

    /**
     * A CASCATA, que é a razão de ser da tela depois da decisão de Paulo (2026-08-16): a 3.1 existe
     * para que uma fala que faltou traduzir <b>não saia daqui sem tradução</b>. O LLM é a 1ª etapa
     * porque conhece a lore; o Google é a 2ª e só entra quando a 1ª não resolveu.
     *
     * <p>Antes disto eram dois botões, e a garantia dependia de o operador lembrar a ordem — a lente
     * de boa-fé chama isso de interface que permite errar.
     */
    @Test
    void quandoOLlmNaoResolveACascataChamaOGoogle() {
        llm.responderSemAlterar();

        CadeiaCorrecaoFala.Tentativa tentativa = cadeia.decidir(
            new SessaoRevisaoArquivo(), suspeita("Get out of there!", "Get out of there!"),
            "ep01.ass", ModoRevisaoLegendas.LLM_CONCORDANCIA, SEM_LORE);

        assertEquals(1, tradutorExterno.chamadas(),
            "o LLM recusou: a fala tinha de seguir para a 2ª etapa em vez de virar pendência");
        assertInstanceOf(DecisaoFala.Corrigir.class, tentativa.decisao());
    }

    /**
     * O rótulo tem de dizer QUEM resolveu. Numa cascata é fácil o texto vir do Google e a evidência
     * sair como {@code CORRIGIDA_LLM}, porque o modo pedido continua sendo o do botão — e aí toda
     * comparação futura entre provedores nasce mentindo.
     */
    @Test
    void correcaoVindaDoGoogleNaCascataNaoEhRotuladaComoLlm() {
        llm.responderSemAlterar();

        CadeiaCorrecaoFala.Tentativa tentativa = cadeia.decidir(
            new SessaoRevisaoArquivo(), suspeita("Get out of there!", "Get out of there!"),
            "ep01.ass", ModoRevisaoLegendas.LLM_CONCORDANCIA, SEM_LORE);

        assertTrue(tentativa.evidencias().stream()
                .anyMatch(e -> "CORRIGIDA_GOOGLE".equals(e.resultado())),
            "quem corrigiu foi o Google; rotular como LLM contamina o dataset de comparação");
        assertTrue(tentativa.evidencias().stream()
                .anyMatch(e -> "LLM_SEM_ALTERACAO".equals(e.resultado())),
            "a recusa da 1ª etapa também é evidência: sem ela ninguém sabe que o LLM foi tentado");
    }
}
