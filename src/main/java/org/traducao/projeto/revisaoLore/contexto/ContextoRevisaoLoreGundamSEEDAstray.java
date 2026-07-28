package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Mobile Suit Gundam SEED MSV Astray.
 *
 * <p>INVARIANTES DO DOMÍNIO: derivada da lore de TRADUCAO da mesma obra
 * ({@code contexto.lore.gundam.ContextoGundamSEEDAstray}) — reestruturacao de formato, nao
 * pesquisa nova. O passe da Opcao 7 e estreito: normaliza grafia de termo e NAO reescreve a fala,
 * entao a linha de Personagens aqui nao carrega genero (quem usa genero e a traducao).
 *
 * <p>Sem {@code correcoesTerminologia()}: a Traducao tambem usa o default {@code Map.of()}.
 * Lore magra no lado da Traducao — a revisao reflete isso de proposito.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt imutavel.
 */
@Component
public class ContextoRevisaoLoreGundamSEEDAstray implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam SEED MSV Astray (Manga / Side Story).
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: MBF-P02 Gundam Astray Red Frame, MBF-P03 Gundam Astray Blue Frame, MBF-P01 Gundam Astray Gold Frame, Gerbera Straight, Junk Guild, Serpent Tail.
        - Personagens: Lowe Guele, Gai Murakumo, Rondo Gina Sahaku, Rondo Mina Sahaku, Elijah Kiel.
        - Alertas: Astray Red/Blue/Gold Frame grafias oficiais; Junk Guild / Serpent Tail nao localizar.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_seed_astray"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam SEED MSV Astray (Mangá/Side Story) - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
