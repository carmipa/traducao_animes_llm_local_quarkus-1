package org.traducao.projeto.lore.revisao;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Mobile Suit Gundam SEED C.E. 73: Stargazer.
 *
 * <p>INVARIANTES DO DOMÍNIO: derivada da lore de TRADUCAO da mesma obra
 * ({@code contexto.lore.gundam.ContextoGundamSEEDStargazer}) — reestruturacao de formato, nao
 * pesquisa nova. O passe da Opcao 7 e estreito: normaliza grafia de termo e NAO reescreve a fala,
 * entao a linha de Personagens aqui nao carrega genero (quem usa genero e a traducao).
 *
 * <p>Sem {@code correcoesTerminologia()}: a Traducao tambem usa o default {@code Map.of()}.
 * Lore magra no lado da Traducao — a revisao reflete isso de proposito.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt imutavel.
 */
@Component
public class ContextoRevisaoLoreGundamSEEDStargazer implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam SEED C.E. 73: Stargazer (OVA).
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: GSX-401FW Stargazer Gundam, GAT-X105E Strike Noir Gundam, Phantom Pain, DSSD (Deep Space Survey and Development Organization).
        - Personagens: Selene McGriff, Sven Cal Bayang, Sol Ryuune L'ange.
        - Alertas: Stargazer Gundam / Strike Noir / Phantom Pain / DSSD grafias oficiais.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_seed_stargazer"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam SEED C.E. 73: Stargazer - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
