package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Evangelion: 3.0+1.0 Thrice Upon a Time.
 *
 * <p>INVARIANTES DO DOMÍNIO: derivada da lore de TRADUCAO da mesma obra
 * ({@code contexto.lore.evangelion.ContextoEvangelion3010}) — reestruturacao de formato, nao pesquisa
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
public class ContextoRevisaoLoreEvangelion3010 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Evangelion: 3.0+1.0 Thrice Upon a Time (tambem 3.0+1.01) — final da tetralogia Rebuild.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Village-3, WILLE, AAA Wunder, NERV, Anti-Universe, Golgotha Object, Additional Impact, Neon Genesis, Spear of Longinus, Spear of Cassius, AT Field, LCL, EVA Unit-08.
        - Personagens: Shinji Ikari, Rei Ayanami, Asuka Shikinami Langley, Mari Illustrious Makinami, Gendo Ikari, Misato Katsuragi, Ritsuko Akagi, Kaworu Nagisa, Kozo Fuyutsuki, Kensuke Aida, Toji Suzuhara, Hikari Horaki.
        - Alertas: Asuka Shikinami Langley — nao aceitar "Asuka Langley" generico nem "Soryu";
          Village-3 e nome de lugar; "Neon Genesis" nao se traduz quando e conceito ou titulo;
          as Lancas mantem nome EN; nao misturar com a TV classica.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "evangelion_3010"; }
    @Override public String getNomeExibicao() { return "Evangelion: 3.0+1.0 Thrice Upon a Time - Revisao de Lore"; }
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
