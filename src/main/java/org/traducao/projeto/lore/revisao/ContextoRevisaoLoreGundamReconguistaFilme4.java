package org.traducao.projeto.lore.revisao;

import org.springframework.stereotype.Component;
import org.traducao.projeto.lore.domain.PromptRevisaoLore;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Gundam Reconguista in G IV: Love That Cries Out in Battle.
 *
 * <p>INVARIANTES DO DOMÍNIO: derivada da lore de TRADUCAO da mesma obra
 * ({@code contexto.lore.gundam.reconguista.ContextoGundamReconguistaFilme4}) — reestruturacao de
 * formato, nao pesquisa nova. O passe da Opcao 7 e estreito: normaliza grafia de termo e NAO
 * reescreve a fala, entao a linha de Personagens aqui nao carrega genero (quem usa genero e a
 * traducao).
 *
 * <p>O mapa de terminologia espelha EXATAMENTE o do lado da Traducao. Divergir poe a obra em
 * {@code DIVERGENCIAS_DECLARADAS} do {@code ParidadeMapasTerminologiaTest} — divida que so se
 * assume com motivo escrito, uma obra por vez.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutaveis.
 */
@Component
public class ContextoRevisaoLoreGundamReconguistaFilme4 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Gundam Reconguista in G IV: Love That Cries Out in Battle.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Venus Globe, G-IT, Hermes Foundation, Ameria, Amerian Army, G-Self, G-Arcane, G-Lucifer, Kabakali, Photon Battery, Regild Century, Rayhunton Code, Reconguista.
        - Personagens: Bellri Zenam, Aida Surugan/Aida Rayhunton, Raraiya Monday/Raraiya Akuparl, Noredo Nug, Klim Nick, Luin Lee/Mask, Mick Jack, Manny Ambassada, La Gu.
        - Alertas: Reconguista NUNCA vira "Reconquista"; Photon Battery nao vira "Bateria de Foton";
          nomes proprios em ingles/romanizados.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_greco_4"; }
    @Override public String getNomeExibicao() { return "Gundam: Reconguista in G IV - Love That Cries Out in Battle - Revisao de Lore"; }
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
            Map.entry("Reconquista", "Reconguista"),
            Map.entry("Bateria de Fóton", "Photon Battery")
        ));
    }
}
