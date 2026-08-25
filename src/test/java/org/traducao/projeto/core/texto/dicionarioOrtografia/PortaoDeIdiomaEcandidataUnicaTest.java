package org.traducao.projeto.core.texto.dicionarioOrtografia;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: as três travas que a leitura do acervo de 24/08/2026 obrigou a existir no
 * dono do dicionário.
 *
 * <h2>Os defeitos, todos com fala real</h2>
 * <ul>
 *   <li><b>Acentuação ambígua.</b> {@code apenasAcentuacoes} pegava a PRIMEIRA sugestão do
 *       hunspell e dava {@code break}. A ordem dele não é opinião sobre a frase, e o resultado foi
 *       {@code "A gata comeu sua linguá?"} — com acento no A, quando o certo é {@code língua}. É a
 *       mesma cicatriz do {@code avo → avó/avô}, resolvida em outro ponto do projeto e nunca
 *       aplicada aqui.</li>
 *   <li><b>Fala inglesa acentuada.</b> {@code "That might not be a bad idea."} virava
 *       {@code "a bad ideá"}; {@code have} virava {@code havê}; {@code place} virava
 *       {@code placê}. Oito falas.</li>
 *   <li><b>Memória do adapter.</b> Sem ela, o portão de inglês pagaria um processo externo POR
 *       FALA — o defeito de 80 ms/fala corrigido horas antes, reentrando por outra porta.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Dicionário fora do ar termina como NÃO VERIFICADO — e pular não é aprovar.
 */
class PortaoDeIdiomaEcandidataUnicaTest {

    private static CorretorOrtograficoLegenda corretor;
    private static boolean dePe;

    @BeforeAll
    static void montar() {
        corretor = new CorretorOrtograficoLegenda();
        corretor.corrigir("Uma sonda qualquer para acordar o dicionario.");
        dePe = corretor.disponivel();
    }

    @Test
    @DisplayName("PORTAO: a fala inglesa do acervo nao e tocada, e a portuguesa continua sendo")
    void portaoDeIdioma() {
        Assumptions.assumeTrue(dePe, "dicionario fora do ar — NAO VERIFICADO, e pular nao e aprovar");

        assertTrue(corretor.predominantementeInglesa("That might not be a bad idea."),
            "a fala inglesa do Zeta passou por portuguesa — 'idea' viraria 'ideá'");
        assertTrue(corretor.predominantementeInglesa(
                "Call again after restrictions have been lifted."),
            "a fala inglesa do Guilty Crown passou por portuguesa — 'have' viraria 'havê'");

        // CONTRA-TESTE, e ele e o que impede a guarda de virar "desligar tudo": a fala que a
        // regua ANTERIOR barrava injustamente tem de passar. Sem diacritico e sem palavra-funcao
        // da lista, "Chegamos a borda do territorio." e portugues normal.
        assertFalse(corretor.predominantementeInglesa("Chegamos a borda do territorio."),
            "barrou fala portuguesa normal: e o defeito que a regua anterior tinha");
        assertFalse(corretor.predominantementeInglesa("A gata comeu sua lingua?"));
        assertFalse(corretor.predominantementeInglesa("Nao ha transmissoes de radio permitidas."));
    }

    @Test
    @DisplayName("fala CURTA nao decide idioma — legenda e cheia delas")
    void falaCurtaNaoDecide() {
        Assumptions.assumeTrue(dePe, "dicionario fora do ar — NAO VERIFICADO");
        assertFalse(corretor.predominantementeInglesa("Go!"));
        assertFalse(corretor.predominantementeInglesa("No way"));
        assertFalse(corretor.predominantementeInglesa(""));
        assertFalse(corretor.predominantementeInglesa(null));
    }

    /**
     * A trava da candidata única, exercitada pelo caminho que a produção percorre.
     * {@code lingua} tem DUAS acentuações que o dicionário aceita, e escolher uma é inventar.
     */
    @Test
    @DisplayName("CANDIDATA UNICA: palavra com DUAS acentuacoes validas nao e corrigida")
    void acentuacaoAmbiguaNaoEescolhida() {
        Assumptions.assumeTrue(dePe, "dicionario fora do ar — NAO VERIFICADO");
        DicionarioOrtograficoPort pt = new HunspellDicionarioAdapter("hunspell", "pt_BR");
        Map<String, Set<String>> sugestoes =
            pt.sugestoes(new LinkedHashSet<>(List.of("lingua", "territorio")));

        Map<String, String> seguras = CorretorAcentoPorDicionario.apenasAcentuacoes(sugestoes);
        // Sem acento, pelo NFD — a mesma definicao que a producao usa, escrita aqui porque o
        // helper dela e privado do pacote de outra classe. Se as duas divergirem, o teste passa a
        // medir outra coisa; por isso ele so afirma com a `assumeTrue` abaixo, que confere que o
        // dicionario REALMENTE ofereceu duas.
        java.util.function.Function<String, String> semAcento = x ->
            java.text.Normalizer.normalize(x, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        long acentuacoesDeLingua = sugestoes.getOrDefault("lingua", Set.of()).stream()
            .filter(x -> semAcento.apply(x).equalsIgnoreCase("lingua"))
            .filter(x -> !semAcento.apply(x).equals(x))
            .count();

        // O caso so prova alguma coisa se o dicionario REALMENTE oferecer duas — senao estaria
        // verde por nao haver ambiguidade nenhuma para recusar.
        Assumptions.assumeTrue(acentuacoesDeLingua >= 2,
            "este hunspell so ofereceu " + acentuacoesDeLingua + " acentuacao(oes) para 'lingua': "
                + "sem ambiguidade o caso nao mede a trava — NAO VERIFICADO");
        assertFalse(seguras.containsKey("lingua"),
            "escolheu uma entre duas acentuacoes validas — foi assim que 'linguá' entrou no acervo");
        assertEquals("território", seguras.get("territorio"),
            "a palavra com UMA acentuacao valida tem de continuar sendo corrigida");
    }

    /**
     * A invariante que a mutacao denunciou como NAO COBERTA: dicionário fora do ar não memoriza.
     *
     * <h2>Por que ela é a mais perigosa das três</h2>
     * Com o processo morto, {@code desconhecidas()} devolve vazio — e vazio significa tanto
     * "todas as palavras estão certas" quanto "não perguntei a ninguém". Se a memória guardasse
     * esse vazio como resposta, a consulta seguinte encontraria "conhecida" e <b>nunca mais
     * perguntaria</b>, mesmo depois de o dicionário voltar. O sintoma seria uma legenda saindo
     * sem correção nenhuma, com cara de legenda limpa.
     */
    @Test
    @DisplayName("dicionario FORA DO AR nao memoriza nada — memoria envenenada nunca mais pergunta")
    void foraDoArNaoEnvenenaAmemoria() {
        DicionarioOrtograficoPort quebrado =
            new HunspellDicionarioAdapter("hunspell-que-nao-existe-nesta-maquina", "pt_BR");
        Set<String> lote = new LinkedHashSet<>(List.of("territorio", "reforcos", "maos"));

        Set<String> resposta = quebrado.desconhecidas(lote);

        assertFalse(quebrado.disponivel(),
            "o adapter apontado para um executavel inexistente se declarou disponivel");
        assertTrue(resposta.isEmpty(), "processo morto nao pode devolver veredicto");
        assertEquals(0, ((HunspellDicionarioAdapter) quebrado).palavrasMemorizadas(),
            "memorizou com o dicionario fora do ar: a proxima consulta encontraria 'conhecida' e "
                + "nunca mais perguntaria, e a legenda sairia sem correcao com cara de limpa");
    }

    /**
     * A memória do adapter, provada pelas duas propriedades que importam: mesma resposta e
     * consulta muito mais barata.
     */
    @Test
    @DisplayName("a memoria do adapter nao muda resposta e derruba o custo da segunda consulta")
    void memoriaDoAdapter() {
        Assumptions.assumeTrue(dePe, "dicionario fora do ar — NAO VERIFICADO");
        DicionarioOrtograficoPort pt = new HunspellDicionarioAdapter("hunspell", "pt_BR");
        Set<String> lote = new LinkedHashSet<>(List.of(
            "territorio", "batalha", "reforcos", "maos", "necessario", "cachorro", "menino"));

        long t1 = System.nanoTime();
        Set<String> primeira = pt.desconhecidas(lote);
        long custoPrimeira = System.nanoTime() - t1;

        long t2 = System.nanoTime();
        Set<String> segunda = pt.desconhecidas(lote);
        long custoSegunda = System.nanoTime() - t2;

        assertEquals(primeira, segunda,
            "a memoria mudou a resposta — cache que muda resultado e bug com desculpa");
        assertTrue(primeira.contains("territorio") && !primeira.contains("batalha"),
            "o experimento nao separou conhecida de desconhecida: " + primeira);
        System.out.printf("  1a consulta: %.0f ms · 2a (memoria): %.1f ms%n",
            custoPrimeira / 1e6, custoSegunda / 1e6);
        assertTrue(custoSegunda * 10 < custoPrimeira,
            String.format("a segunda consulta custou %.1f ms contra %.0f ms: a memoria nao pegou",
                custoSegunda / 1e6, custoPrimeira / 1e6));
    }
}
