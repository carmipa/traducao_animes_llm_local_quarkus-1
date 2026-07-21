package org.traducao.projeto.contexto.lore.breakblade;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * PROPÓSITO DE NEGÓCIO: lore completa do Filme 4 — The Land of Calamity /
 * The Earth of Calamity (A Terra da Calamidade): unidade especial de Narvi,
 * Girge, morte de True e avanço de Borcuse.
 *
 * <p>INVARIANTES DO DOMÍNIO: Cleo prisioneira / Zess fora do front (filme 3);
 * NÃO adiantar duelo Hykelion na aldeia de Rygart nem morte de Girge (filme 5–6).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoBreakBlade4 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Broken Blade / Break Blade — Movie 4: The Land of Calamity /
          The Earth of Calamity (惨禍ノ地 / Sanka no Chi) — PT: A Terra da Calamidade.
        - Continuidade: 4º dos 6 filmes. Amplia a guerra além do duelo Rygart/Zess.
        - Foco deste filme: Sigyn monta força especial (Narvi, Rygart, Nile, Loggin, Girge);
          Girge (filho de Baldr) recebe o Golem Eltemis de Cleo — piloto genial e perigoso;
          General True é emboscado e morto (Io / Nike); cerco Baldr vs General Borcuse
          no front (Arakan / wasteland). Pedido de ajuda de Hodr ao Orlando Empire.
          NÃO cobrir massacre da aldeia de Regatz nem duelo Hykelion (filme 5).

        === Personagens (gênero) ===
        - Rygart Arrow (m), Sigyn Erster (f), Hodr (m), Cleo Saburafu (f — cativa).
        - Narvi (f) — líder da unidade especial; irmã de Nile.
        - Nile (m), Loggin (m) — equipe Krisna.
        - Girge (m) — filho de General Baldr; passado criminoso; Eltemis.
        - General Baldr (m), General True (m — morto neste arco).
        - General Borcuse Deussenrudolf (m) — Athens; brutal; Hykelion em cena/ameaça.
        - Io (m/f conforme material — oficial Athens), Nike — emboscada a True.
        - Zess (m) — recuperando-se em Athens (fora do front principal deste filme).

        === Mecha / termos ===
        - Delphine, Eltemis, Hykelion (Golem de Borcuse), Golem/Golems, Quartz,
          un-sorcerer, Heavy Knight, Orlando Empire, Arakan (wasteland).
        - Sparta unit / forças de elite Athens podem aparecer em menções — manter EN.

        === Regras ===
        - Borcuse Deussenrudolf; Girge; Narvi; Nile; Loggin; True.
        - Tom: calamidade em escala de exército; tensão Girge/Rygart; Quartz como prêmio.

        === Formas-ruim ===
        - Hykélion → Hykelion; Eltemus → Eltemis; Império de Orlando → Orlando Empire.
        """;

    private static final String PROMPT = ContextoPrompt.montar(
        "Break Blade - Filme 4 - A Terra da Calamidade", LORE);

    @Override
    public String getId() {
        return "break_blade_4";
    }

    @Override
    public String getNomeExibicao() {
        return "Break Blade - Filme 4 - A Terra da Calamidade";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege roster e termos do Filme 4 (calamidade / Borcuse).
     *
     * <p>INVARIANTES DO DOMÍNIO: sem desfecho da aldeia/Regatz/morte de Girge.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Rygart Arrow", "Hodr", "Sigyn Erster", "Zess",
            "Cleo Saburafu", "Narvi", "Nile", "Loggin",
            "Girge", "General Baldr", "General True",
            "General Borcuse", "Borcuse Deussenrudolf", "Io", "Nike",
            "Delphine", "Eltemis", "Hykelion", "Under-Golem",
            "Golem", "Golems", "Quartz", "un-sorcerer",
            "Heavy Knight", "Kingdom of Krisna", "Athens Commonwealth",
            "Orlando Empire", "Binonten", "Cruzon", "Arakan",
            "Broken Blade", "Break Blade", "The Land of Calamity",
            "The Earth of Calamity"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Break Blade.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@link CorrecoesTerminologiaBreakBlade}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaBreakBlade.mapa();
    }
}
