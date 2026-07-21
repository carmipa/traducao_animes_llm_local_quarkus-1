package org.traducao.projeto.contexto.lore.breakblade;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * PROPÓSITO DE NEGÓCIO: lore completa do Filme 3 — Scars from an Assassin's Blade /
 * The Mark of the Assassin's Dagger (Cicatrizes da Lâmina do Assassino): ataque final
 * da Valkyrie Squadron, ferimento de Zess e captura de Cleo.
 *
 * <p>INVARIANTES DO DOMÍNIO: Rygart já é Heavy Knight do Delphine; NÃO adiantar
 * unidade especial Narvi/Girge em campanha (filme 4) nem Borcuse/Hykelion.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoBreakBlade3 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Broken Blade / Break Blade — Movie 3: Scars from an Assassin's Blade
          / The Mark of the Assassin's Dagger (凶刃ノ痕 / Kyoujin no Ato)
          — PT: Cicatrizes da Lâmina do Assassino.
        - Continuidade: 3º dos 6 filmes. Após a escolha de Rygart no filme 2.
        - Foco deste filme: treinamento do Delphine com blindagem pesada; último ataque
          da Valkyrie Squadron a Binonten; morte de Argath; Zess gravemente ferido por
          Rygart; Cleo luta em fúria e é capturada (Narvi); Erekt evacua Zess.
          NÃO cobrir marcha de Borcuse / morte de True (filme 4).

        === Personagens (gênero) ===
        - Rygart Arrow (m), Hodr (m), Sigyn Erster (f), Zess (m).
        - Cleo Saburafu (f) — prisioneira em Krisna após o combate.
        - Argath (m) — morto neste arco; Erekt (m) — retorna com Zess a Athens.
        - Narvi (f) — piloto Krisna; dispara e incapacita o Golem de Cleo.
        - General Baldr (m) — treina/alerta Rygart sobre o custo de poupar inimigos.
        - Lee (f) — memória/trauma do filme 2 (não reescrever o suicídio aqui).

        === Mecha / termos ===
        - Delphine (heavy armor / espada grande), Golem, Quartz, un-sorcerer,
          Heavy Knight, Valkyrie Squadron, Eltemis (Golem Athens associado a Cleo —
          grafias Eltemus/Eltemis; preferir Eltemis).
        - "Assassin's Blade" / lâmina do título = metáfora do arco; manter nomes EN.

        === Regras ===
        - Cleo Saburafu (não "Saburac" inventado); Narvi; Argath; Erekt.
        - Zess fora de ação por ferimentos — sem spoiler de retorno precoce do filme 5+.
        - Tom: cicatrizes físicas e emocionais; amizade destruída no cockpit.

        === Formas-ruim ===
        - Eltemus → Eltemis; Delfine → Delphine; Esquadrão Valquíria → Valkyrie Squadron.
        """;

    private static final String PROMPT = ContextoPrompt.montar(
        "Break Blade - Filme 3 - Cicatrizes da Lâmina do Assassino", LORE);

    @Override
    public String getId() {
        return "break_blade_3";
    }

    @Override
    public String getNomeExibicao() {
        return "Break Blade - Filme 3 - Cicatrizes da Lâmina do Assassino";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege roster e termos do Filme 3 (cicatrizes / Cleo).
     *
     * <p>INVARIANTES DO DOMÍNIO: sem Borcuse/Hykelion/Girge como foco.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Rygart Arrow", "Hodr", "Sigyn Erster", "Zess",
            "Cleo Saburafu", "Narvi", "Argath", "Erekt",
            "General Baldr", "Lee", "Delphine", "Under-Golem",
            "Eltemis", "Golem", "Golems", "Quartz",
            "un-sorcerer", "Heavy Knight", "Kingdom of Krisna",
            "Athens Commonwealth", "Valkyrie Squadron", "Binonten",
            "Cruzon", "Broken Blade", "Break Blade",
            "Scars from an Assassin's Blade", "The Mark of the Assassin's Dagger"
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
