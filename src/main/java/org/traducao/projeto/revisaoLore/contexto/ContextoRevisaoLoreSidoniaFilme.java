package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Knights of Sidonia: Love Woven in the Stars.
 *
 * <p>INVARIANTES DO DOMÍNIO: derivada da lore de TRADUCAO da mesma obra
 * ({@code contexto.lore.sidonia.ContextoSidoniaFilme}) — reestruturacao de formato, nao pesquisa
 * nova. O passe da Opcao 7 e estreito: normaliza grafia de termo e NAO reescreve a fala, entao a
 * linha de Personagens aqui nao carrega genero (quem usa genero e a traducao).
 *
 * <p>Sem {@code correcoesTerminologia()}: a Traducao tambem usa o default {@code Map.of()}.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt imutavel.
 */
@Component
public class ContextoRevisaoLoreSidoniaFilme implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Knights of Sidonia: Love Woven in the Stars (Sidonia no Kishi: Ai Tsumugu Hoshi) — filme.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Sidonia, Gauna, Garde/Gardes, Kabizashi, Ena, Heigus particles, Toha Heavy Industries, Immortal Ship Committee.
        - Personagens: Nagate Tanikaze, Tsumugi Shiraui, Izana Shinatose, Yuhata Midorikawa, Captain Kobayashi, Norio Kunato, Lala Hiyama, Shizuka Hoshijiro.
        - Alertas: Izana Shinatose — NUNCA "Shinoshinari"; Garde como mecha;
          Ena nao vira "pele"; Gauna core / placenta como termos biologicos da obra.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "sidonia_movie"; }
    @Override public String getNomeExibicao() { return "Knights of Sidonia: Love Woven in the Stars (Filme) - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
