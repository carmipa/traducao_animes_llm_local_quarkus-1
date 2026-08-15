package org.traducao.projeto.lore.revisao;

import org.springframework.stereotype.Component;
import org.traducao.projeto.lore.domain.PromptRevisaoLore;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Evangelion: Filme 1 - 1.11 You Are (Not) Alone.
 *
 * <p>INVARIANTES DO DOMÍNIO: derivada da lore de TRADUCAO da mesma obra
 * ({@code contexto.lore.evangelion.ContextoEvangelion111}) — reestruturacao de formato, nao pesquisa
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
public class ContextoRevisaoLoreEvangelion111 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Evangelion: 1.11 You Are (Not) Alone — tetralogia Rebuild.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: NERV, SEELE, EVA Unit-00, EVA Unit-01, AT Field, LCL, Entry Plug, Dummy System, Tokyo-3, GeoFront, Sachiel, Shamshel, Ramiel.
        - Personagens: Shinji Ikari, Rei Ayanami, Misato Katsuragi, Gendo Ikari, Ritsuko Akagi, Kozo Fuyutsuki, Ryoji Kaji, Toji Suzuhara, Kensuke Aida, Maya Ibuki.
        - Alertas: Asuka NAO aparece nesta obra (entra no 2.22) — nao introduzir nenhuma das duas grafias dela;
          nomes oficiais EN; NERV/SEELE/AT Field/LCL; nao misturar com a serie TV classica.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "evangelion_111"; }
    @Override public String getNomeExibicao() { return "Evangelion: Filme 1 - 1.11 You Are (Not) Alone - Revisao de Lore"; }
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
