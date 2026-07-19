package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Macross Zero.
 *
 * <p>INVARIANTES DO DOMÍNIO: Valkyrie/Zentradi oficiais; sem Veritech/Valquíria.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacrossZero implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross Zero.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Macross Zero, OVA, Shin Kudo, Sara Nome, Mao Nome, Sheryl Nome, Roy Focker, Edgar LaSalle, Ivanov, Nora Polyansky, Mechas, Termos, Phoenix, Ilha Mayan, Bird Human, Homem, Protocultura.
        - Personagens: Shin Kudo (homem), Sara Nome (mulher), Mao Nome (mulher / avó de Sheryl Nome), Roy Focker (homem), Edgar LaSalle (homem), D.D. Ivanov (homem), Nora Polyansky (mulher).
        - Mechas / Termos: VF-0 Phoenix, SV-51, Ilha Mayan, Bird Human (Homem Pássaro), Protocultura.
        - Alertas: Valkyrie nao vira Valquiria/Valquíria; Zentradi grafia oficial; proibido Veritech;
          GERWALK/Battroid/Fighter Mode — nao traduzir nomes dos modos.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "macross_zero"; }
    @Override public String getNomeExibicao() { return "Macross Zero - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Macross na Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacrossRevisao.mapa();
    }
}
