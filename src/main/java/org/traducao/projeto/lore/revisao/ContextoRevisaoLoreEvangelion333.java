package org.traducao.projeto.lore.revisao;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Evangelion: 3.33 You Can (Not) Redo.
 *
 * <p>INVARIANTES DO DOMÍNIO: derivada da lore de TRADUCAO da mesma obra
 * ({@code contexto.lore.evangelion.ContextoEvangelion333}) — reestruturacao de formato, nao pesquisa
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
public class ContextoRevisaoLoreEvangelion333 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Evangelion: 3.33 You Can (Not) Redo — tetralogia Rebuild.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: WILLE, AAA Wunder, NERV, SEELE, EVA Unit-13, EVA Unit-08, Central Dogma, Spear of Longinus, Spear of Cassius, DSS Choker, AT Field, LCL.
        - Personagens: Shinji Ikari, Kaworu Nagisa, Asuka Shikinami Langley, Mari Illustrious Makinami, Misato Katsuragi, Ritsuko Akagi, Rei Ayanami, Gendo Ikari, Kozo Fuyutsuki, Ryoji Kaji.
        - Alertas: Asuka Shikinami Langley — nao aceitar "Asuka Langley" solto nem "Soryu";
          as Lancas mantem nome EN: Spear of Longinus e Spear of Cassius, nunca "Lanca de Longinus";
          Wunder e WILLE sao nomes oficiais; nao misturar com a TV classica.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "evangelion_333"; }
    @Override public String getNomeExibicao() { return "Evangelion: 3.33 You Can (Not) Redo - Revisao de Lore"; }
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
            Map.entry("Asuka Langley Soryu", "Asuka Shikinami Langley"),
            Map.entry("Lança de Longinus", "Spear of Longinus"),
            Map.entry("Lança de Cassius", "Spear of Cassius")
        ));
    }
}
