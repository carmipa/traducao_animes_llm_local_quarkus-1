package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Mobile Suit Gundam SEED Destiny.
 *
 * <p>INVARIANTES DO DOMÍNIO: derivada da lore de TRADUCAO da mesma obra
 * ({@code contexto.lore.gundam.ContextoGundamSEEDDestiny}) — reestruturacao de formato, nao
 * pesquisa nova. O passe da Opcao 7 e estreito: normaliza grafia de termo e NAO reescreve a fala,
 * entao a linha de Personagens aqui nao carrega genero (quem usa genero e a traducao).
 *
 * <p>Sem {@code correcoesTerminologia()}: a Traducao tambem usa o default {@code Map.of()}.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt imutavel.
 */
@Component
public class ContextoRevisaoLoreGundamSEEDDestiny implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam SEED Destiny (Cosmic Era) — sequela de SEED.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: ZAFT, Earth Alliance, PLANTs, LOGOS, Destiny Plan, Extended, Coordinator, Natural, Minerva, Archangel, Eternal, ZGMF-X42S Destiny Gundam, ZGMF-X20A Strike Freedom Gundam, ZGMF-X19A Infinite Justice Gundam, ZGMF-X56S Impulse Gundam, Saviour, Chaos, Abyss, Gaia, Legend Gundam.
        - Personagens: Shinn Asuka, Kira Yamato, Athrun Zala, Lacus Clyne, Meer Campbell, Cagalli Yula Athha, Lunamaria Hawke, Stella Loussier, Rey Za Burrel, Gilbert Durandal, Neo Roanoke/Mu La Flaga, Meyrin Hawke, Heine Westenfluss, Talia Gladys, Andrew Waltfeld, Yzak Joule.
        - Alertas: Shinn/Lunamaria/Stella/Durandal/Rey grafias oficiais; Destiny Plan como nome;
          Destiny Gundam nao reduzir a "Destino" no nome da unidade.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_seed_destiny"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam SEED Destiny - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
