package org.traducao.projeto.lore.revisao;

import org.springframework.stereotype.Component;
import org.traducao.projeto.lore.domain.PromptRevisaoLore;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Evangelion (Série TV Clássica).
 *
 * <p>INVARIANTES DO DOMÍNIO: derivada da lore de TRADUCAO da mesma obra
 * ({@code contexto.lore.evangelion.ContextoEvangelionTV}) — reestruturacao de formato, nao pesquisa
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
public class ContextoRevisaoLoreEvangelionTV implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Neon Genesis Evangelion (serie TV classica, 1995) — NAO e a tetralogia Rebuild.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: NERV, SEELE, Gehirn, MAGI, Evangelion, EVA, EVA Unit-00, EVA Unit-01, EVA Unit-02, AT Field, LCL, Dummy Plug, Entry Plug, Second Impact, Third Impact, Human Instrumentality Project, Tokyo-3, GeoFront, Sachiel, Shamshel, Ramiel.
        - Personagens: Shinji Ikari, Rei Ayanami, Asuka Langley Soryu, Misato Katsuragi, Gendo Ikari, Ritsuko Akagi, Kaworu Nagisa, Toji Suzuhara, Kensuke Aida, Hikari Horaki, Kozo Fuyutsuki, Ryoji Kaji, Maya Ibuki, Makoto Hyuga, Shigeru Aoba.
        - Alertas: Asuka Langley Soryu e a grafia DESTA continuidade — NAO trocar por "Asuka Shikinami Langley", que e do Rebuild;
          NERV/SEELE/MAGI/LCL/AT Field mantem forma oficial; nomes de Anjos (Sachiel, Shamshel, Ramiel) ficam oficiais;
          "Human Instrumentality Project" e o nome oficial — nao inventar "Human Instrumentation".
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "evangelion_tv"; }
    @Override public String getNomeExibicao() { return "Evangelion (Série TV Clássica) - Revisao de Lore"; }
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
        return CorrecoesTerminologiaEvangelionRevisao.mapa();
    }
}
