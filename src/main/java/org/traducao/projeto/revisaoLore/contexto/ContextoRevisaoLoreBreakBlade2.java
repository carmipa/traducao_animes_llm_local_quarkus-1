package org.traducao.projeto.revisaoLore.contexto;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore excepcional do Filme 2 — The Path of Separation.
 *
 * <p>INVARIANTES DO DOMÍNIO: Lee/True/Heavy Knight; sem captura de Cleo (filme 3).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreBreakBlade2 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Broken Blade / Break Blade — Movie 2: The Path of Separation / The Split Path
          (O Caminho da Separação).
        - Regra: cessar-fogo com Zess; General True quebra trégua; destino de Lee;
          Rygart vira Heavy Knight do Delphine. SEM captura de Cleo (filme 3).

        === Roster ===
        - Rygart Arrow, Hodr, Sigyn Erster, Zess, Cleo Saburafu, Lee,
          General True, General Baldr, Dan, Loquis.

        === Termos ===
        - Delphine, Under-Golem, Golem, Quartz, un-sorcerer, Heavy Knight,
          Kingdom of Krisna, Athens Commonwealth, Orlando Empire, Valkyrie Squadron,
          Binonten, Cruzon, Ilios, Broken Blade, The Path of Separation.

        === Formas-ruim ===
        - Cavaleiro Pesado → Heavy Knight; Não-feiticeiro → un-sorcerer;
          Comunidade de Atenas → Athens Commonwealth; Delfine → Delphine.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "break_blade_2"; }
    @Override public String getNomeExibicao() {
        return "Break Blade - Filme 2 - O Caminho da Separação - Revisao de Lore";
    }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Break Blade na Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaBreakBladeRevisao.mapa();
    }
}
