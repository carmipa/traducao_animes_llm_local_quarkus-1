package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.domain.LoreAtivaPort;
import org.traducao.projeto.traducao.domain.fallback.ProvedorFallback;
import org.traducao.projeto.traducao.domain.fallback.ResultadoFallback;
import org.traducao.projeto.traducao.domain.ports.FallbackTraducaoMaquinaPort;
import org.traducao.projeto.traducao.infrastructure.config.FallbackOnlineProperties;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: a guarda de lore do fallback decompõe termos compostos em tokens e exige
 * que cada um sobreviva à tradução. Substantivo comum dentro do termo NÃO pode virar exigência —
 * senão {@code "Solar System II"} transforma toda fala com "system" numa recusa.
 *
 * <h2>Os casos são reais, colhidos ao vivo</h2>
 * <pre>
 *   0083, 23:24  EN "The shield! Aim for Unit 02's cooling system!"
 *                -> recusada: termo protegido "system" não sobreviveu
 *   0083, 00:23  EN "To make the pursuit fleet use up all its propellant!"
 *                -> recusada: termo protegido "fleet" não sobreviveu
 * </pre>
 * Nenhuma tem a ver com {@code Solar System II} nem com nave nomeada. "system" vira "sistema" e
 * "fleet" vira "frota" — a tradução está CERTA, e a guarda a jogou fora.
 *
 * <h2>Como a lista cresceu, e por que não por intuição</h2>
 * Medição sobre os 60.336 pares EN/PT do acervo, com DOIS eixos:
 * <ol>
 *   <li>o token sobrevive à tradução? Nome próprio sobrevive, substantivo comum não (&lt; 25%);</li>
 *   <li>aparece em MINÚSCULA no inglês? (&gt;= 60%).</li>
 * </ol>
 * O eixo (2) é o que impede o desastre: sem ele a medição mandava incluir {@code "gundam"},
 * {@code "sanders"} e {@code "char"} — porque eles também "não sobrevivem" quando o modelo os
 * traduz errado, que é justamente o defeito. Foram três critérios até um separar as categorias.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Alargar esta lista ENFRAQUECE a guarda: cada palavra liberada deixa de ser exigida
 *       também dentro do termo composto que a contém. Por isso todo caso liberado aqui vem com
 *       o vizinho que precisa continuar sendo recusado.</li>
 *   <li>{@code "08th"} ficou FORA de propósito: passa nos dois eixos, mas não é palavra — é
 *       fragmento de {@code "08th MS Team"}, que a lore exige preservar inteiro.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se a lista encolher, volta a recusar tradução correta e a inflar a contagem de pendências; se
 * crescer para nomes próprios, o fallback passa a aceitar entidade trocada.
 */
class GuardaLoreNaoExigePalavraComumTest {

    private static RecuperarPendenciaFallbackService servico(
            FallbackTraducaoMaquinaPort porta, Set<String> termosProtegidos) {
        LoreAtivaPort lore = new LoreAtivaPort() {
            @Override public Set<String> termosProtegidosAtivos() { return termosProtegidos; }
            @Override public String obterLoreAtiva() { return ""; }
        };
        return new RecuperarPendenciaFallbackService(
            new FallbackOnlineProperties(true), porta, lore, new VerificadorIdentificadorNumerico());
    }

    private static FallbackTraducaoMaquinaPort porta(Function<String, ResultadoFallback> f) {
        return new FallbackTraducaoMaquinaPort() {
            @Override public ResultadoFallback traduzir(String o) { return f.apply(o); }
            @Override public ProvedorFallback provedor() { return ProvedorFallback.GOOGLE; }
        };
    }

    private static LinkedHashSet<String> conjunto(String... itens) {
        return new LinkedHashSet<>(Set.of(itens));
    }

    @Test
    @DisplayName("substantivo comum de dentro do termo composto não é exigido — os dois casos reais")
    void palavraComumNaoViraExigencia() {
        var svc = servico(
            porta(o -> ResultadoFallback.recuperada(
                o.contains("cooling system")
                    ? "O escudo! Mire no sistema de refrigeração da Unidade 02!"
                    : "Para fazer a frota de perseguição gastar todo o propelente!",
                ProvedorFallback.GOOGLE)),
            Set.of("Solar System II", "Albion", "Gato"));

        var r = svc.recuperar(conjunto(
            "The shield! Aim for Unit 02's cooling system!",
            "To make the pursuit fleet use up all its propellant!"));

        assertEquals(2, r.recuperadas().size(),
            () -> "as duas traduções estão corretas e foram recusadas: " + r.porCausa());
    }

    /**
     * O vizinho obrigatório: a parte DISTINTIVA do termo composto continua exigida. "system" foi
     * liberado, "solar" não — e é ele que identifica o termo.
     *
     * <p>A comparação da guarda ignora caixa, então o teste precisa fazer o token sumir de
     * verdade ("Solar" -> "Estelar"), e não apenas mudar de maiúscula para minúscula.
     */
    @Test
    @DisplayName("a parte distintiva do termo composto continua sendo exigida")
    void parteDistintivaContinuaExigida() {
        var svc = servico(
            porta(o -> ResultadoFallback.recuperada("Aquilo é o Sistema Estelar II?", ProvedorFallback.GOOGLE)),
            Set.of("Solar System II"));

        var r = svc.recuperar(conjunto("Is that the Solar System II?"));

        assertTrue(r.recuperadas().isEmpty(),
            "\"Solar\" sumiu da tradução: liberar \"system\" não pode liberar o termo inteiro");
    }

    /**
     * E o nome próprio, que é a razão de a guarda existir. "Gato" é o antagonista do 0083; se a
     * tradução o perder, não pode entrar no cache.
     *
     * <p>Note que a tradução aqui NÃO diz "gato" em lugar nenhum: a guarda ignora caixa, então
     * "O gato fugiu" passaria — e passar seria correto, porque o nome continua na fala.
     */
    @Test
    @DisplayName("nome próprio perdido continua sendo recusado")
    void nomeProprioPerdidoContinuaRecusado() {
        var svc = servico(
            porta(o -> ResultadoFallback.recuperada("O piloto fugiu com a unidade!", ProvedorFallback.GOOGLE)),
            Set.of("Anavel Gato", "Albion"));

        var r = svc.recuperar(conjunto("Gato escaped with the unit!"));

        assertTrue(r.recuperadas().isEmpty(),
            "o nome do personagem sumiu da tradução: a guarda tem de recusar");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: DECLARADO vence INCIDENTAL. A allowlist existe para o token que a
     * DECOMPOSIÇÃO produz — "Solar System II" gerar "system" é acidente da quebra. Quando a obra
     * declara a palavra SOZINHA como termo protegido, é decisão dela, e decisão da obra vence
     * lista global.
     *
     * <p>Sem esta distinção, a allowlist vira um jeito silencioso de desproteger terminologia que
     * alguém escolheu proteger — e foi o que aconteceu: acrescentar "battle" quebrou um teste
     * anterior que existia justamente para provar que afrouxar a guarda não a tinha desligado.
     */
    @Test
    @DisplayName("palavra da allowlist DECLARADA sozinha pela obra volta a ser exigida")
    void declaradoVenceIncidental() {
        var traduzSemBattle = porta(o ->
            ResultadoFallback.recuperada("A Batalha em Três Dimensões", ProvedorFallback.GOOGLE));

        var semDeclaracao = servico(traduzSemBattle, Set.of("Solar System II"));
        assertEquals(1, semDeclaracao.recuperar(conjunto("The Battle in Three Dimensions")).recuperadas().size(),
            "\"battle\" incidental: título comum tem de poder ser traduzido");

        var comDeclaracao = servico(traduzSemBattle, Set.of("Battle"));
        assertTrue(comDeclaracao.recuperar(conjunto("The Battle in Three Dimensions")).recuperadas().isEmpty(),
            "a obra declarou \"Battle\" sozinho: a allowlist global não pode anular isso");
    }

    /**
     * PATENTE: o eixo de caixa da medição derrubou "captain" porque ele aparece capitalizado
     * antes do nome, mas ali "Captain" -> "Capitão" é a tradução certa — 660 ocorrências no
     * acervo, 1% de sobrevivência. Entrou à mão, e este teste é o que prende a decisão.
     */
    @Test
    @DisplayName("patente traduzida não é termo perdido")
    void patenteTraduzidaNaoEhTermoPerdido() {
        var svc = servico(
            porta(o -> ResultadoFallback.recuperada("Obrigado, Capitão Burning.", ProvedorFallback.GOOGLE)),
            Set.of("Captain Burning", "South Burning"));

        var r = svc.recuperar(conjunto("Thank you, Captain Burning."));

        assertEquals(1, r.recuperadas().size(),
            () -> "\"Burning\" sobreviveu; exigir \"Captain\" em inglês é falso positivo: "
                + r.porCausa());
    }
}
