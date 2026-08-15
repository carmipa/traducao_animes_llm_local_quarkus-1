package org.traducao.projeto.lore.revisao;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Gundam Reconguista in G (serie).
 *
 * <p>INVARIANTES DO DOMÍNIO: derivada da lore de TRADUCAO da mesma obra
 * ({@code contexto.lore.gundam.reconguista.ContextoGundamReconguista}) — reestruturacao de formato,
 * nao pesquisa nova. O passe da Opcao 7 e estreito: normaliza grafia de termo e NAO reescreve a
 * fala, entao a linha de Personagens aqui nao carrega genero (quem usa genero e a traducao).
 *
 * <p>O mapa de terminologia espelha EXATAMENTE o do lado da Traducao. Divergir poe a obra em
 * {@code DIVERGENCIAS_DECLARADAS} do {@code ParidadeMapasTerminologiaTest} — divida que so se
 * assume com motivo escrito, uma obra por vez.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutaveis.
 */
@Component
public class ContextoRevisaoLoreGundamReconguista implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Gundam Reconguista in G / G-Reco, Regild Century.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Capital Tower, Capital Guard, Capital Army, Ameria, Towasanga, Venus Globe, Dorette Fleet, G-Self, G-Arcane, Montero, Mack Knife, Kabakali, G-Lucifer, Photon Battery, Universal Standard, SU-Cordism, Rayhunton Code, Amerian Army, Towasangan, Regild Century, Reconguista.
        - Personagens: Bellri Zenam, Aida Surugan/Aida Rayhunton, Raraiya Monday, Noredo Nug, Luin Lee/Mask, Klim Nick, Mick Jack, Cumpa Rusita, Wilmit Zenam.
        - Alertas: Reconguista NUNCA vira "Reconquista" no titulo; Photon Battery nao vira "Bateria de Foton";
          nomes proprios em ingles/romanizados.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_greco"; }
    @Override public String getNomeExibicao() { return "Gundam: Reconguista in G - Revisao de Lore"; }
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
