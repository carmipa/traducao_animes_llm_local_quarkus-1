package org.traducao.projeto.lore.revisao;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Mobile Suit Victory Gundam.
 *
 * <p>INVARIANTES DO DOMÍNIO: derivada da lore de TRADUCAO da mesma obra
 * ({@code contexto.lore.gundam.ContextoGundamVictory}) — reestruturacao de formato, nao pesquisa
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
public class ContextoRevisaoLoreGundamVictory implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Victory Gundam.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: LM312V04 Victory Gundam, Victory 2 Gundam (V2), Zanscare Empire, League Militaire, Angel Halo.
        - Personagens: Uso Ewin, Shakti Kareen, Marbet Fingerhat, Chronicle Asher, Katejina Loos, Maria Pure Armonia, Fonse Kagatie.
        - Alertas: Zanscare Empire nao vira "Imperio Zanscare"; League Militaire nao vira "Liga Militar";
          Angel Halo nao vira "Aureola do Anjo" nem "Halo do Anjo".
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_victory"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Victory Gundam - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa deterministico UC na Opcao 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho exato do lado da Traducao desta obra.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutavel; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUcRevisao.comExtras(Map.ofEntries(
            Map.entry("Império Zanscare", "Zanscare Empire"),
            Map.entry("Imperio Zanscare", "Zanscare Empire"),
            Map.entry("Liga Militar", "League Militaire"),
            Map.entry("Auréola do Anjo", "Angel Halo"),
            Map.entry("Halo do Anjo", "Angel Halo")
        ));
    }
}
