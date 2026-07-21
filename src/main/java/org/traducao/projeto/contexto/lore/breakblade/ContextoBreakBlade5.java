package org.traducao.projeto.contexto.lore.breakblade;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * PROPÓSITO DE NEGÓCIO: lore completa do Filme 5 — The Gap Between Life and Death /
 * The Horizon Between Life and Death (A Fronteira entre a Vida e a Morte): aldeia de
 * Rygart, duelo Delphine vs Hykelion e caminho de sacrifício de Girge.
 *
 * <p>INVARIANTES DO DOMÍNIO: Borcuse já no front (filme 4); NÃO adiantar cerco final
 * de Binonten / forma final do Delphine / desfecho anime do filme 6.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoBreakBlade5 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Broken Blade / Break Blade — Movie 5: The Gap Between Life and Death /
          The Horizon Between Life and Death (死線ノ涯 / Shisen no Hate)
          — PT: A Fronteira entre a Vida e a Morte.
        - Continuidade: 5º dos 6 filmes. Horizonte entre sobrevivência e aniquilação.
        - Foco deste filme: Borcuse avança em direção à terra natal de Rygart; Regatz
          (irmão un-sorcerer) e aldeões; Rygart desobedece Narvi; duelo Delphine vs
          Hykelion; Girge intervém / questiona motivos; elite Athens (Sparta) aperta o
          cerco. Peso da morte iminente — sem o assalto final a Binonten do filme 6.

        === Personagens (gênero) ===
        - Rygart Arrow (m), Regatz (m) — irmão un-sorcerer.
        - Narvi (f), Nile (m), Loggin (m), Girge (m).
        - General Borcuse Deussenrudolf (m) — Hykelion; prazer em duelos dignos.
        - General Baldr (m), Sigyn Erster (f), Hodr (m), Cleo Saburafu (f).
        - Zess (m) — pode ressurgir no quadro Athens conforme o corte do filme;
          não confundir com o clímax da capital (filme 6).

        === Mecha / termos ===
        - Delphine, Hykelion, Eltemis, Golem/Golems, Quartz, un-sorcerer,
          Heavy Knight, Sparta (unidade elite Athens — manter EN quando aparecer).
        - "Horizon" / "Gap" / "Death Line" = títulos EN equivalentes do mesmo filme.

        === Regras ===
        - Regatz; Borcuse; Girge; Hykelion — grafias oficiais.
        - Filme 6 do anime tem desfecho próprio (≠ mangá) — NÃO importar aqui.
        - Tom: limite vida/morte; família; fúria de Rygart; instabilidade de Girge.

        === Formas-ruim ===
        - Hykélion → Hykelion; Não-feiticeiro → un-sorcerer; Delfine → Delphine.
        """;

    private static final String PROMPT = ContextoPrompt.montar(
        "Break Blade - Filme 5 - A Fronteira entre a Vida e a Morte", LORE);

    @Override
    public String getId() {
        return "break_blade_5";
    }

    @Override
    public String getNomeExibicao() {
        return "Break Blade - Filme 5 - A Fronteira entre a Vida e a Morte";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege roster e termos do Filme 5 (fronteira vida/morte).
     *
     * <p>INVARIANTES DO DOMÍNIO: sem fortaleza final / Greta / forma final Delphine.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Rygart Arrow", "Regatz", "Hodr", "Sigyn Erster",
            "Zess", "Cleo Saburafu", "Narvi", "Nile",
            "Loggin", "Girge", "General Baldr",
            "General Borcuse", "Borcuse Deussenrudolf",
            "Delphine", "Eltemis", "Hykelion", "Under-Golem",
            "Golem", "Golems", "Quartz", "un-sorcerer",
            "Heavy Knight", "Kingdom of Krisna", "Athens Commonwealth",
            "Sparta", "Binonten", "Cruzon", "Broken Blade", "Break Blade",
            "The Gap Between Life and Death", "The Horizon Between Life and Death"
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
