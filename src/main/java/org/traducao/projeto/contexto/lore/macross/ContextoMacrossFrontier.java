package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore enriquecida de Macross Frontier (série TV).
 *
 * <p>INVARIANTES DO DOMÍNIO: Klan Klang; SMS; Vajra; Sheryl Nome; Ranka Lee;
 * Macross Frontier fleet.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoMacrossFrontier implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Frontier (série TV).
        - Premissa: a frota emigrante Macross Frontier enfrenta a ameaça alienígena Vajra;
          SMS e NUNS; idol Sheryl Nome e a novata Ranka Lee; piloto Alto Saotome.
        - Personagens (gênero): Alto Saotome (m), Sheryl Nome (f), Ranka Lee (f),
          Michael Blanc (m), Luca Angeloni (m), Klan Klang (f — Zentradi; manter grafia),
          Ozma Lee (m), Cathy Glass (f), Jeffrey Wilder (m), Grace O'Connor (f),
          Brera Sterne (m), Bobby Margot (m), Nanase Matsuura (f), Leon Mishima (m).
        - Organizações/termos: SMS (Strategic Military Services), NUNS (New United Nations Spacy),
          Vajra, Fold, Fold Bacteria, Galaxy, Island-1 / Macross Frontier fleet, Protoculture,
          Zentradi, Song Energy / fold waves via canção.
        - Mecha/naves: VF-25 Messiah, VF-27 Lucifer, Queadluun-Rhea, Macross Quarter, Battle Frontier.

        === Nucleo Macross (canone JP) ===
        - PROIBIDO Robotech/Veritech.
        - VF/Valkyrie; Fighter Mode / GERWALK Mode / Battroid Mode (nomes oficiais).
        - Overtechnology; Fold; Song Energy via cancao = arma psicologica, nao OST.
        - Zentradi (Klan Klang); Deculture se aparecer.
        - Regras: Klan Klang / Sheryl Nome / Ranka Lee / Alto Saotome oficiais;
          Vajra nao traduzir; SMS/NUNS como siglas; cancoes cantaveis sem notas editoriais.
        - Tom: idol + mecha + romance triangular; Sheryl diva, Ranka ingenua, Alto entre teatro e cockpit.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross Frontier", LORE);

    @Override
    public String getId() {
        return "macross_frontier";
    }

    @Override
    public String getNomeExibicao() {
        return "Macross Frontier";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Alto Saotome", "Sheryl Nome", "Ranka Lee", "Michael Blanc", "Luca Angeloni",
            "Klan Klang", "Ozma Lee", "Brera Sterne", "Vajra", "SMS", "NUNS", "VF-25 Messiah",
            "GERWALK", "Battroid", "Valkyrie", "Fold", "Overtechnology"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: restaura grafias oficiais Macross (Valkyrie/Zentradi) quando
     * o LLM localiza indevidamente — mapa compartilhado da franquia.
     *
     * <p>INVARIANTES DO DOMÍNIO: só aplica com canônico no original EN.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public java.util.Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacross.mapa();
    }

}
