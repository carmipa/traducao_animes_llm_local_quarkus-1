package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Evangelion: Filme 2 - 2.22 You Can (Not) Advance.
 *
 * <p>INVARIANTES DO DOMÍNIO: derivada da lore de TRADUCAO da mesma obra
 * ({@code contexto.lore.evangelion.ContextoEvangelion222}) — reestruturacao de formato, nao pesquisa
 * nova. O passe da Opcao 7 e estreito: normaliza grafia de termo e NAO reescreve a fala, entao a
 * linha de Personagens aqui nao carrega genero (quem usa genero e a traducao).
 *
 * <p>O mapa de terminologia espelha EXATAMENTE o do lado da Traducao. Divergir poe a obra em
 * {@code DIVERGENCIAS_DECLARADAS} do {@code ParidadeMapasTerminologiaTest} — divida que so se
 * assume com motivo escrito, uma obra por vez.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutaveis.
 */
@Component
public class ContextoRevisaoLoreEvangelion222 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Evangelion: 2.22 You Can (Not) Advance — tetralogia Rebuild.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: NERV, SEELE, EVA Unit-02, EVA Unit-03, AT Field, LCL, Entry Plug, Beast Mode, Near Third Impact, Zeruel, Bardiel.
        - Personagens: Shinji Ikari, Asuka Shikinami Langley, Mari Illustrious Makinami, Rei Ayanami, Misato Katsuragi, Gendo Ikari, Ritsuko Akagi, Ryoji Kaji, Toji Suzuhara.
        - Alertas: Asuka Shikinami Langley e a grafia DESTA continuidade — "Asuka Langley Soryu" e da TV classica e deve ser corrigida;
          Mari Illustrious Makinami de nome completo, preferivel a "Mari Makinami"; nao misturar continuidades.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "evangelion_222"; }
    @Override public String getNomeExibicao() { return "Evangelion: Filme 2 - 2.22 You Can (Not) Advance - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa deterministico de Evangelion na Opcao 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho exato do lado da Traducao desta obra.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutavel; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaEvangelionRevisao.comExtras(Map.ofEntries(
            Map.entry("Asuka Langley Soryu", "Asuka Shikinami Langley")
        ));
    }
}
