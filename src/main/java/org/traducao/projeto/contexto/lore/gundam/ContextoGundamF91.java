package org.traducao.projeto.contexto.lore.gundam;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoGundamF91 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam F91.
        - Personagens: Seabook Arno (homem), Cecily Fairchild / Berah Ronah (mulher), Carozzo Ronah / Iron Mask (homem), Zabine Chareux (homem), Annamarie Bourget (mulher).
        - Mechas / Termos: F91 Gundam Formula 91, Crossbone Vanguard, VSBR (Variable Speed Beam Rifle), MEPE (Afterimage Effect).
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam F91", LORE);

    @Override public String getId() { return "gundam_f91"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam F91"; }
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
            "Seabook Arno", "Cecily Fairchild", "Berah Ronah",
            "Carozzo Ronah", "Iron Mask", "Zabine Chareux",
            "Annamarie Bourget", "Crossbone Vanguard", "Cosmo Babylonia",
            "F91", "Gundam F91", "VSBR",
            "Denan Zon", "Denan Gei", "Berga-Giros",
            "Vigna-Ghina", "Rafflesia", "Bugs",
            "Bio-Computer", "Newtype", "Mobile Suit"
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
            Map.entry("Máscara de Ferro", "Iron Mask"),
            Map.entry("Vanguarda Crossbone", "Crossbone Vanguard"),
            Map.entry("Cosmo Babilônia", "Cosmo Babylonia")
        ));
    }
}
