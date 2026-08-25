package org.traducao.projeto.revisaoConcordancia.application;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * PROPÓSITO DE NEGÓCIO: impedir que a tela 3.3 acentue <b>nome próprio</b> e <b>fala em inglês</b>.
 *
 * <h2>As falas aqui são REAIS, e a leitura delas parou uma gravação</h2>
 * Em 24/08/2026 a medição do acervo disse que 1.156 falas mudariam. Antes de gravar, os pares
 * foram lidos um a um — e seis deles eram estrago:
 *
 * <pre>
 *   Artemis -> Ártemis   11 falas   DanMachi     nome da deusa, lore da obra
 *   Astrea  -> Ástrea     6 falas   DanMachi     nome
 *   Ingues  -> Ingués     4 falas   Macross II   nome (Lord Ingues, Imperador Ingues)
 *   Cardeas -> Cárdeas    6 falas   Unicorn      nome — E ESTE JA ERA CICATRIZ ESCRITA
 *   Cleo    -> Cléo       3 falas   Break Blade  nome
 *   idea    -> ideá       4 falas   Zeta         a fala inteira esta em INGLES
 * </pre>
 *
 * O {@code Cardeas} é o mais duro de todos: ele está escrito como cicatriz no Javadoc do
 * {@link CorretorAcentoDeDicionarioNaFalaService} desde 18/08/2026, com o número de falas e tudo.
 * A proteção que ele descreve existia — e mesmo assim deixou passar, porque só cobria a palavra
 * capitalizada NO MEIO da fala. Nome de personagem abre frase o tempo todo: {@code "Cárdeas Vist."},
 * {@code "Ártemis..."}, {@code "Cléo, minha filha..."}.
 *
 * <p>Uma guarda que nunca foi vista reprovando o caso doente pode estar aprovando por cegueira, e
 * era esse o caso: nenhum teste tinha um nome próprio ABRINDO a fala.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprova nomeando a fala e o que ela virou.
 */
class CorretorNaoAcentuaNomeProprioTest {

    private static CorretorAcentoDeDicionarioNaFalaService dicionario;
    private static CorretorCaractereForaDoPortuguesService caractere;
    private static CorretorAcentoPorPadraoService padrao;
    private static boolean dePe;

    /** Fala real do acervo → o que ela NÃO pode virar. */
    private static final Map<String, String> NAO_PODE_MUDAR = new LinkedHashMap<>();

    static {
        NAO_PODE_MUDAR.put("Ártemis...", "Artemis...");
        NAO_PODE_MUDAR.put("Você viu a Lady Artemis e a Lady Hestia?", "Ártemis");
        NAO_PODE_MUDAR.put("Astrea Record.", "Ástrea");
        NAO_PODE_MUDAR.put("Lady Astrea e todos me deram isso...", "Ástrea");
        NAO_PODE_MUDAR.put("Lord Ingues", "Ingués");
        NAO_PODE_MUDAR.put("Senhor Imperador Ingues", "Ingués");
        NAO_PODE_MUDAR.put("Cardeas Vist.", "Cárdeas");
        NAO_PODE_MUDAR.put("Cardeas, que se revela como o pai de Banagher,", "Cárdeas");
        NAO_PODE_MUDAR.put("Cleo, minha filha...", "Cléo");
        NAO_PODE_MUDAR.put("Cleo, pelo menos acerte um tiro!", "Cléo");
        NAO_PODE_MUDAR.put("That might not be a bad idea.", "ideá");
    }

    @BeforeAll
    static void montar() {
        dicionario = new CorretorAcentoDeDicionarioNaFalaService(new CorretorOrtograficoLegenda());
        caractere = new CorretorCaractereForaDoPortuguesService(new CorretorOrtograficoLegenda());
        padrao = new CorretorAcentoPorPadraoService();
        dicionario.corrigir("Uma sonda qualquer para acordar o dicionario.");
        dePe = dicionario.disponivel();
    }

    @Test
    @DisplayName("NENHUM elo acentua nome proprio que ABRE a fala — as 11 falas reais do acervo")
    void nomeProprioNaoEacentuado() {
        Assumptions.assumeTrue(dePe, "dicionario fora do ar — NAO VERIFICADO, e isso nao e aprovar");
        StringBuilder estrago = new StringBuilder();
        for (Map.Entry<String, String> caso : NAO_PODE_MUDAR.entrySet()) {
            String fala = caso.getKey();
            String proibido = caso.getValue();
            String depois = fala;
            depois = caractere.corrigir(depois).orElse(depois);
            depois = padrao.corrigir(depois).orElse(depois);
            depois = dicionario.corrigir(depois).orElse(depois);
            if (depois.contains(proibido)) {
                estrago.append(String.format("%n    '%s'%n      virou '%s'  (proibido: %s)",
                    fala, depois, proibido));
            }
        }
        if (!estrago.isEmpty()) {
            fail("a cadeia estragou nome proprio ou fala inglesa:" + estrago);
        }
    }

    /**
     * PROPÓSITO: o bug do PREFIXO CONSUMIDO, que nenhum caso cobria.
     *
     * <p>A guarda antiga lia o token anterior com {@code (\\S*)\\s*} dentro da mesma regex. Em
     * {@code "Você viu a Lady Artemis"} o casamento de {@code Lady} consumia o token, e
     * {@code Artemis} vinha com prefixo VAZIO — lido como início de frase, e desprotegido no meio
     * da fala. Duas maiúsculas seguidas eram tudo o que era preciso, e o acervo tem isso aos
     * montes: {@code Lady Artemis}, {@code Lord Ingues}, {@code Cardeas Vist}.
     */
    @Test
    @DisplayName("duas maiusculas seguidas: a SEGUNDA continua sendo vista no meio da fala")
    void prefixoConsumidoNaoCegaAsegunda() {
        assertTrue(
            CorretorAcentoDeDicionarioNaFalaService
                .nomesPropriosNoMeioDaFala("Você viu a Lady Artemis e a Lady Hestia?")
                .contains("Artemis"),
            "a segunda maiuscula seguida saiu como se abrisse frase — e o bug do prefixo consumido");
        assertTrue(
            CorretorAcentoDeDicionarioNaFalaService
                .nomesPropriosNoMeioDaFala("Senhor Imperador Ingues")
                .contains("Ingues"),
            "tres maiusculas seguidas: a ultima ficou desprotegida");
    }

    @Test
    @DisplayName("quem ABRE a fala continua fora dos nomes do meio — a regra nao inverteu")
    void quemAbreFraseNaoEnomeDoMeio() {
        assertFalse(
            CorretorAcentoDeDicionarioNaFalaService
                .nomesPropriosNoMeioDaFala("Necessario que venha agora.")
                .contains("Necessario"),
            "palavra que abre a fala virou 'nome do meio': a correcao comum morreria");
        assertFalse(
            CorretorAcentoDeDicionarioNaFalaService
                .nomesPropriosNoMeioDaFala("Tudo bem. Necessario que venha.")
                .contains("Necessario"),
            "palavra depois de ponto final abre frase e nao e nome do meio");
    }

    /**
     * A fala inteira em inglês do Zeta. Sem o portão de idioma, o dicionário português
     * transformava {@code idea} em {@code ideá} — quatro falas do acervo.
     */
    @Test
    @DisplayName("fala em INGLES nao e tocada pelo dicionario portugues")
    void falaInglesaNaoEtocada() {
        Assumptions.assumeTrue(dePe, "dicionario fora do ar — NAO VERIFICADO, e isso nao e aprovar");
        assertEquals(Optional.empty(), dicionario.corrigir("That might not be a bad idea."));
        assertEquals(Optional.empty(), dicionario.corrigir("I have to go back to the ship now."));
    }

    /**
     * O contra-teste que impede a correção virar "desligar tudo": palavra comum MINÚSCULA continua
     * sendo corrigida. Sem este caso, a guarda nova passaria simplesmente não fazendo mais nada —
     * verde e inútil.
     */
    @Test
    @DisplayName("CONTROLE: palavra comum minuscula CONTINUA sendo corrigida")
    void palavraComumAindaEcorrigida() {
        Assumptions.assumeTrue(dePe, "dicionario fora do ar — NAO VERIFICADO, e isso nao e aprovar");
        assertEquals(Optional.of("Não há território inimigo à frente."),
            dicionario.corrigir("Não há territorio inimigo à frente."),
            "a guarda contra nome proprio desligou a correcao de palavra comum: "
                + "guarda que reprova o codigo certo e pior que guarda nenhuma");
        assertEquals(Optional.of("Você não tem reforços a caminho."),
            dicionario.corrigir("Você não tem reforcos a caminho."));
    }

    /**
     * O PREÇO da guarda, escrito como caso de teste em vez de nota de rodapé.
     *
     * <h2>Por que o preço vira teste</h2>
     * A guarda larga custa: {@code "Territorio inimigo à frente."} deixa de ser corrigido, porque
     * a palavra abre a fala e está capitalizada. No acervo inteiro esse custo foi medido em
     * <b>três</b> falas ({@code Parabens→Parabéns}), contra ~48 nomes de lore salvos.
     *
     * <p>Deixar isso só no Javadoc faria a próxima leitura tratar o comportamento como bug e
     * "consertar". Aqui ele está declarado: é o preço aceito, com o número que o justifica, e o
     * contador {@code barradasPorMaiuscula} existe para avisar se ele crescer.
     */
    @Test
    @DisplayName("PRECO DECLARADO: palavra comum CAPITALIZADA deixa de ser corrigida — de proposito")
    void oPrecoDaGuardaEstaDeclarado() {
        Assumptions.assumeTrue(dePe, "dicionario fora do ar — NAO VERIFICADO, e isso nao e aprovar");
        assertEquals(Optional.empty(), dicionario.corrigir("Territorio inimigo à frente."),
            "se isto voltar a corrigir, a guarda da maiuscula caiu — e com ela os nomes de lore");
        assertTrue(dicionario.barradasPorMaiuscula() > 0,
            "o preco foi pago mas NAO foi contado: guarda cujo custo ninguem mede vira dogma");
    }
}
