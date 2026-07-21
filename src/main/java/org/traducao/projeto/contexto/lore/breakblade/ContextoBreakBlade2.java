package org.traducao.projeto.contexto.lore.breakblade;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * PROPÓSITO DE NEGÓCIO: lore completa do Filme 2 — The Path of Separation /
 * The Split Path (O Caminho da Separação): cessar-fogo quebrado, custo humano e
 * escolha de Rygart como Heavy Knight.
 *
 * <p>INVARIANTES DO DOMÍNIO: Delphine já desperta (filme 1); NÃO adiantar captura de
 * Cleo / ferimento grave de Zess (filme 3) nem Borcuse (filme 4+).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoBreakBlade2 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Broken Blade / Break Blade — Movie 2: The Path of Separation / The Split Path
          (訣別ノ路 / Ketsubetsu no Michi) — PT: O Caminho da Separação.
        - Continuidade: 2º dos 6 filmes. Parte do despertar do filme 1; não misturar com
          o arco da lâmina/assassino do filme 3.
        - Foco deste filme: Athens invadiu Krisna também por Quartz (além da política Assam/
          Orlando); Rygart negocia cessar-fogo com Zess; General True quebra o cessar-fogo;
          combate com Lee e suicídio dela; Rygart decide permanecer e é feito Heavy Knight
          oficial do Delphine.

        === Mundo / facções ===
        - Kingdom of Krisna vs Athens Commonwealth; capital Binonten; Ilios (Athens).
        - Motivo declarado da invasão vs motivo real (minas de Quartz de Krisna).
        - Orlando Empire como fator do golpe em Assam (contexto narrativo).

        === Personagens (gênero) ===
        - Rygart Arrow (m), Hodr (m), Sigyn Erster (f), Zess (m).
        - Cleo Saburafu (f), Lee (f) — destino trágico de Lee neste filme.
        - General True (m) — Athens; força o fim do cessar-fogo.
        - General Baldr (m), Dan (m) — lado Krisna no campo.
        - Loquis (m) — Secretary of War (Ilios); recebe notícias da operação de Zess.

        === Mecha / termos ===
        - Delphine, Golem/Golems, Quartz, un-sorcerer, Heavy Knight, Under-Golem,
          Valkyrie Squadron, ceasefire / cessar-fogo narrativo.
        - Valkyrie Squadron ≠ Macross Valkyrie.

        === Regras ===
        - Preservar Broken Blade / Break Blade; Kingdom of Krisna; Athens Commonwealth.
        - Lee / Cleo / True / Baldr / Dan — nomes oficiais EN.
        - Tom: separação de amigos; horror da guerra; Rygart assume o Delphine.

        === Formas-ruim ===
        - Cavaleiro Pesado → Heavy Knight; Não-feiticeiro → un-sorcerer;
          Comunidade de Atenas → Athens Commonwealth; Delfine → Delphine.
        """;

    private static final String PROMPT = ContextoPrompt.montar(
        "Break Blade - Filme 2 - O Caminho da Separação", LORE);

    @Override
    public String getId() {
        return "break_blade_2";
    }

    @Override
    public String getNomeExibicao() {
        return "Break Blade - Filme 2 - O Caminho da Separação";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege roster e termos do Filme 2 (separação / Lee / Heavy Knight).
     *
     * <p>INVARIANTES DO DOMÍNIO: sem Borcuse/Girge/Hykelion como foco.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Rygart Arrow", "Hodr", "Sigyn Erster", "Zess",
            "Cleo Saburafu", "Lee", "General True", "General Baldr",
            "Dan", "Loquis", "Delphine", "Under-Golem",
            "Golem", "Golems", "Quartz", "un-sorcerer",
            "Heavy Knight", "Kingdom of Krisna", "Athens Commonwealth",
            "Orlando Empire", "Valkyrie Squadron", "Binonten", "Cruzon",
            "Assam", "Ilios", "Broken Blade", "Break Blade",
            "The Path of Separation", "The Split Path"
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
