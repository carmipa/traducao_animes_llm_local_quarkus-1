package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Mobile Suit Gundam SEED FREEDOM.
 *
 * <p>INVARIANTES DO DOMÍNIO: derivada da lore de TRADUCAO da mesma obra
 * ({@code contexto.lore.gundam.ContextoGundamSEEDFreedom}) — reestruturacao de formato, nao
 * pesquisa nova. O passe da Opcao 7 e estreito: normaliza grafia de termo e NAO reescreve a fala,
 * entao a linha de Personagens aqui nao carrega genero (quem usa genero e a traducao).
 *
 * <p>Sem {@code correcoesTerminologia()}: a Traducao tambem usa o default {@code Map.of()}.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt imutavel.
 */
@Component
public class ContextoRevisaoLoreGundamSEEDFreedom implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam SEED FREEDOM (filme Cosmic Era).
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Compass, Kingdom of Foundation, ZAFT, Orb, Eurasia, Rising Freedom Gundam, Immortal Justice Gundam, Mighty Strike Freedom Gundam, Destiny Gundam Spec II, Black Knight Squad, Black Knights.
        - Personagens: Kira Yamato, Lacus Clyne, Athrun Zala, Shinn Asuka, Lunamaria Hawke, Cagalli Yula Athha, Agnes Giebenrath, Orphee Lam Tao, Ingrid Tradoll, Daniela Chandler, Redelard Tradoll, Mu La Flaga, Meyrin Hawke, Yzak Joule, Dearka Elsman.
        - Alertas: nomes oficiais EN dos MS; Foundation/Compass; Orphee/Agnes grafias;
          nao traduzir Freedom/Justice no nome da unidade.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_seed_freedom"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam SEED Freedom - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
