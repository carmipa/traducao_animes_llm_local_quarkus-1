package org.traducao.projeto.lore.revisao;

import org.springframework.stereotype.Component;
import org.traducao.projeto.lore.domain.PromptRevisaoLore;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Macross Frontier: Labyrinth of Time.
 *
 * <p>INVARIANTES DO DOMÍNIO: Valkyrie/Zentradi oficiais; sem Veritech/Valquíria. Espelha o
 * elenco declarado no lado da Tradução ({@code contexto.lore.macross.ContextoMacrossFrontierFilme3}) —
 * os dois catálogos existem em par, e uma obra presente em só um deles é acusada pela catraca
 * de paridade.
 *
 * <p>Sem linha de Naves / Mechas, pelo mesmo motivo do lado da Tradução: não foi possível
 * confirmar aparição de mecha neste filme. Se a linha for acrescentada lá, acrescentar aqui
 * junto — divergir entre os dois catálogos é exatamente o que a catraca de paridade existe para
 * pegar.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacrossFrontierFilme3 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross Frontier: Labyrinth of Time.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Macross Frontier, Labyrinth of Time, Toki no Meikyu, Alto Saotome, Sheryl Nome, Ranka Lee, Michael Blanc, Luca Angeloni, Klan Klang, Ozma Lee, Nanase Matsuura.
        - Personagens: Alto Saotome (homem), Sheryl Nome (mulher), Ranka Lee (mulher), Michael Blanc (homem), Luca Angeloni (homem), Klan Klang (mulher), Ozma Lee (homem), Nanase Matsuura (mulher).
        - Cancoes: Toki no Meikyu (Labyrinth of Time), Sacrifice, Hoshi Kira — titulos de cancao NAO sao traduzidos.
        - Alertas: Valkyrie nao vira Valquiria/Valquíria; Zentradi grafia oficial; proibido Veritech;
          GERWALK/Battroid/Fighter Mode — nao traduzir nomes dos modos.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "macross_frontier_filme3"; }
    @Override public String getNomeExibicao() { return "Macross Frontier: Labyrinth of Time - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Macross na Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local. Os extras {@code Fold Fault} e
     * {@code Vajra} são os mesmos das irmãs Filme1/Filme2 — a paridade entre os catálogos é
     * verificada por teste, então divergir aqui reprova.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacrossRevisao.comExtras(Map.ofEntries(
            Map.entry("Falha Fold", "Fold Fault"),
            Map.entry("Falha de Fold", "Fold Fault"),
            Map.entry("Vajras", "Vajra")
        ));
    }
}
