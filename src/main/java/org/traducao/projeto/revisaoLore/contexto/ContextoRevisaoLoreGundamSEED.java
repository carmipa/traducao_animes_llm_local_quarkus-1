package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Mobile Suit Gundam SEED.
 *
 * <p>INVARIANTES DO DOMÍNIO: derivada da lore de TRADUCAO da mesma obra
 * ({@code contexto.lore.gundam.ContextoGundamSEED}) — reestruturacao de formato, nao pesquisa
 * nova. O passe da Opcao 7 e estreito: normaliza grafia de termo e NAO reescreve a fala, entao a
 * linha de Personagens aqui nao carrega genero (quem usa genero e a traducao).
 *
 * <p>Sem {@code correcoesTerminologia()}: a Traducao tambem usa o default {@code Map.of()}.
 * Inventar mapa aqui quebraria a paridade trivial.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt imutavel.
 */
@Component
public class ContextoRevisaoLoreGundamSEED implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam SEED (Cosmic Era) — serie TV SEED apenas.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: ZAFT, Earth Alliance, OMNI, PLANTs, Coordinator, Natural, Blue Cosmos, Eurasian Federation, Orb Union, Archangel, Eternal, GAT-X105 Strike Gundam, ZGMF-X10A Freedom Gundam, ZGMF-X09A Justice Gundam, Aegis, Blitz, Duel, Buster, Providence Gundam, METEOR.
        - Personagens: Kira Yamato, Athrun Zala, Lacus Clyne, Cagalli Yula Athha, Mu La Flaga, Rau Le Creuset, Murrue Ramius, Natarle Badgiruel, Flay Allster, Sai Argyle, Dearka Elsman, Yzak Joule, Nicol Amalfi, Andrew Waltfeld, Patrick Zala, Siegel Clyne, Miriallia Haw.
        - Alertas: NAO incluir elenco exclusivo de SEED Destiny (Shinn Asuka, Lunamaria, Stella, Durandal);
          Coordinator/Natural/ZAFT/PLANT oficiais; Freedom/Justice/Strike como nomes de unidade;
          nao traduzir Kira/Lacus/Athrun/Cagalli.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_seed"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam SEED - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
