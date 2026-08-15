package org.traducao.projeto.lore.revisao;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Knights of Sidonia (serie, as duas temporadas).
 *
 * <p>INVARIANTES DO DOMÍNIO: derivada da lore de TRADUCAO da mesma obra
 * ({@code contexto.lore.sidonia.ContextoKnightsOfSidonia}) — reestruturacao de formato, nao
 * pesquisa nova. O passe da Opcao 7 e estreito: normaliza grafia de termo e NAO reescreve a fala,
 * entao a linha de Personagens aqui nao carrega genero (quem usa genero e a traducao).
 *
 * <p>Foi a ULTIMA das 26 contrapartes, e ficou por ultimo de proposito: o id {@code sidonia} cobre
 * DUAS temporadas, e havia duvida se seria dividido. A auditoria concluiu que NAO — so
 * {@code Tsumugi Shiraui} e exclusivo da temporada 2 e nenhum termo do glossario e exclusivo de
 * uma temporada. O motivo completo esta no Javadoc da classe de traducao. Derivar antes dessa
 * decisao significaria refazer.
 *
 * <p>Sem {@code correcoesTerminologia()}: a Traducao tambem usa o default {@code Map.of()}, e
 * espelhar exato e a regra — divergir poria a obra em {@code DIVERGENCIAS_DECLARADAS}.
 *
 * <p>LACUNA HERDADA: a lore de traducao esta subdeclarada na temporada 1 (faltam irmas Honoka,
 * Samari Ittan, esquadrao Akai, Benisuzume, Yure Shinatose). Esta contraparte reflete o que a
 * traducao declara — engrossar aqui sem engrossar la criaria a assimetria que a catraca de
 * paridade existe para pegar.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt imutavel.
 */
@Component
public class ContextoRevisaoLoreKnightsOfSidonia implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Knights of Sidonia / Sidonia no Kishi — serie (temporadas 1 e 2).
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Sidonia, Gauna, Garde/Gardes, Kabizashi, Ena, Heigus particles, placenta, gauna core, Toha Heavy Industries, Immortal Ship Committee, Residential Layer, Photosynthesis Chamber, Tsugumori, Ochiai.
        - Personagens: Nagate Tanikaze, Shizuka Hoshijiro, Izana Shinatose, Tsumugi Shiraui, Captain Kobayashi, Yuhata Midorikawa, Norio Kunato, Lala Hiyama.
        - Alertas: Izana Shinatose — NUNCA "Shinoshinari"; Garde/Gardes como mecha, "Guarda" so se for
          funcao comum; Ena e orgao bioenergetico e NAO vira "pele" nem "aura"; Kabizashi, Heigus
          particles, gauna core e placenta ficam na forma da obra.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "sidonia"; }
    @Override public String getNomeExibicao() { return "Knights of Sidonia - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
