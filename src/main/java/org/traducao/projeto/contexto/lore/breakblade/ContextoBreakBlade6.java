package org.traducao.projeto.contexto.lore.breakblade;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * PROPÓSITO DE NEGÓCIO: lore completa do Filme 6 — Fortress of Lamentation /
 * Enclave of Lamentations (Fortaleza do Lamentar): cerco de Binonten, Delphine
 * reforçado e desfecho cinematográfico vs Borcuse.
 *
 * <p>INVARIANTES DO DOMÍNIO: desfecho do ANIME dos 6 filmes (pode divergir do mangá);
 * NÃO importar arcos posteriores do mangá nem misturar com a edição TV como id.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoBreakBlade6 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Broken Blade / Break Blade — Movie 6: Fortress of Lamentation /
          Enclave of Lamentations (慟哭ノ砦 / Doukoku no Toride)
          — PT: Fortaleza do Lamentar.
        - Continuidade: 6º e último filme teatral. Desfecho ANIME próprio (staff diverge
          do mangá no fechamento) — NÃO traduzir como se fosse o final impresso completo.
        - Foco deste filme: após perdas (Girge); Baldr traz Rygart/Delphine a Binonten;
          Borcuse invade a capital sem esperar o restante do exército; Sigyn + engenheiros
          + Greta (comerciante Orlando) forjam nova armadura/arma de Quartz para Delphine;
          Captain Sakura organiza defesa urbana; Io / Bades nos eixos de ataque; Narvi,
          Nile e Loggin retomam Golems; batalha de rua e forma final de combate do Delphine
          contra Borcuse / Hykelion.

        === Personagens (gênero) ===
        - Rygart Arrow (m), Sigyn Erster (f), Hodr (m), General Baldr (m).
        - Narvi (f), Nile (m), Loggin (m); memória de Girge (m).
        - General Borcuse Deussenrudolf (m), Io, Bades — Athens no cerco.
        - Captain Sakura (f/m conforme material — líder da defesa civil/militar local).
        - Greta (f) — comerciante Orlando; ajuda material no Quartz/armadura.
        - Cleo Saburafu (f), Zess (m) — conforme aparição no corte final do filme.
        - Gram — coruja de Hodr (pode aparecer em cenas íntimas; manter nome).

        === Mecha / termos ===
        - Delphine (forma final / nova armadura Quartz), Hykelion, Eltemis,
          Golem/Golems, Quartz, un-sorcerer, Heavy Knight, Fortress of Lamentation.
        - Binonten como fortaleza de lamento — capital sitiada.

        === Regras ===
        - Títulos: Fortress of Lamentation / Enclave of Lamentations; PT Fortaleza do Lamentar.
        - NÃO inventar epílogo do mangá; manter canônicos EN de Golems/facções.
        - Tom: lamento coletivo; esperança frágil; mecha + drama de amizade/guerra.

        === Formas-ruim ===
        - Fortaleza do Lamento (se o EN tiver Fortress of Lamentation, reforçar canônico);
          Quartzo → Quartz; Hykélion → Hykelion; Delfine → Delphine.
        """;

    private static final String PROMPT = ContextoPrompt.montar(
        "Break Blade - Filme 6 - Fortaleza do Lamentar", LORE);

    @Override
    public String getId() {
        return "break_blade_6";
    }

    @Override
    public String getNomeExibicao() {
        return "Break Blade - Filme 6 - Fortaleza do Lamentar";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege roster e termos do Filme 6 (fortaleza / desfecho anime).
     *
     * <p>INVARIANTES DO DOMÍNIO: desfecho teatral; sem arcos mangá posteriores.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Rygart Arrow", "Hodr", "Sigyn Erster", "Zess",
            "Cleo Saburafu", "Narvi", "Nile", "Loggin",
            "Girge", "General Baldr", "General Borcuse",
            "Borcuse Deussenrudolf", "Captain Sakura", "Greta",
            "Io", "Bades", "Gram", "Delphine", "Eltemis",
            "Hykelion", "Under-Golem", "Golem", "Golems",
            "Quartz", "un-sorcerer", "Heavy Knight",
            "Kingdom of Krisna", "Athens Commonwealth", "Orlando Empire",
            "Binonten", "Cruzon", "Broken Blade", "Break Blade",
            "Fortress of Lamentation", "Enclave of Lamentations"
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
