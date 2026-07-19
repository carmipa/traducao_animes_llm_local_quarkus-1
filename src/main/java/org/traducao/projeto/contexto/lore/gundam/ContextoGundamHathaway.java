package org.traducao.projeto.contexto.lore.gundam;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoGundamHathaway implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam Hathaway.
        - Personagens: Hathaway Noa / Mafty Navue Erin (homem), Gigi Andalucia (mulher), Kenneth Sleg (homem), Lane Aim (homem), Gawman Noceria (homem).
        - Mechas / Termos: RX-105 Xi Gundam, RX-104FF Penelope, Organização Terrorista Mafty, Minovsky Flight System.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam Hathaway", LORE);

    @Override public String getId() { return "gundam_hathaway"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam Hathaway"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: elenco, mechas, naves, facções e terminologia canônica desta obra
     * que a tradução deve preservar no original — proteção upfront contra localização indevida.
     * <p>INVARIANTES DO DOMÍNIO: grafias oficiais; conjunto imutável.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Hathaway Noa", "Mafty Navue Erin", "Gigi Andalucia",
            "Kenneth Sleg", "Lane Aim", "Gawman Noceria",
            "Xi Gundam", "Penelope", "Odysseus Gundam",
            "Mafty", "Minovsky Flight System", "Minovsky",
            "Newtype", "Mobile Suit"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reforço determinístico do núcleo UC (Newtype, Mobile Suit, Beam
     * Saber/Rifle, Mobile Armor, Oldtype) mais os termos próprios desta obra.
     * <p>INVARIANTES DO DOMÍNIO: forma-ruim PT → canônico; só aplica se o EN contém o canônico.
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUc.comExtras(Map.ofEntries(
            Map.entry("Sistema de Voo Minovsky", "Minovsky Flight System")
        ));
    }
}
