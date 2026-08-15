package org.traducao.projeto.lore.revisao;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Mobile Suit Gundam MS IGLOO.
 *
 * <p>INVARIANTES DO DOMÍNIO: derivada da lore de TRADUCAO da mesma obra
 * ({@code contexto.lore.gundam.ContextoGundamMsIgloo}) — reestruturacao de formato, nao pesquisa
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
public class ContextoRevisaoLoreGundamMsIgloo implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam MS IGLOO — The Hidden One Year War / Apocalypse 0079 (OVA, U.C. 0079).
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Principality of Zeon, 603rd Technical Evaluation Unit, Jotunheim, Earth Federation, EMS-10 Zudah, YMT-05 Hildolfr, QCX-76A Jormungand, MSM-07Di Ze'Gok, MP-02A Oggo, MA-05Ad Big Rang, Zaku II, Mobile Suit, One Year War, Loum, Jaburo, Solomon, A Baoa Qu.
        - Personagens: Oliver May, Monique Cadillac, Martin Prochnow, Albert Schacht, Domenico Marquez, Erich Kruger, Hideo Washiya, Jean Xavier, Aleksandro Hemme, Demeziere Sonnen, Jean Luc Duvall, Werner Holbein, Erwin Cadillac, Herbert von Kuspen, Gihren Zabi.
        - Alertas: nomes proprios e designacoes de mecha em ingles/romanizados;
          Ze'Gok / Zudah / Hildolfr / Jormungand / Oggo / Big Rang grafias oficiais.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "gundam_ms_igloo"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam MS IGLOO - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa deterministico UC na Opcao 7 (nucleo sem extras).
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho exato do lado da Traducao desta obra.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutavel; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUcRevisao.mapa();
    }
}
