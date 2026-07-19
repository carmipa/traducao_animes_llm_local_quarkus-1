package org.traducao.projeto.contexto.lore.gundam;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoGundamOrigin implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam: The Origin.
        - Personagens: Char Aznable / Casval Rem Deikun (homem), Sayla Mass / Artesia Som Deikun (mulher), Amuro Ray (homem), Degwin Sodo Zabi (homem), Gihren Zabi (homem), Dozle Zabi (homem), Kycilia Zabi (mulher), Ramba Ral (homem), Crowley Hamon (mulher).
        - Termos/Mechas: principado de Zeon, Federação Terrestre, Side 3, Munzo, MS-06S Zaku II, RX-78-02 Gundam, Mobile Worker.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam: The Origin", LORE);

    @Override public String getId() { return "gundam_origin"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam: The Origin"; }
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
            "Char Aznable", "Casval Rem Deikun", "Sayla Mass",
            "Artesia Som Deikun", "Amuro Ray", "Zeon Zum Deikun",
            "Degwin Sodo Zabi", "Gihren Zabi", "Dozle Zabi",
            "Kycilia Zabi", "Garma Zabi", "Ramba Ral",
            "Crowley Hamon", "Zeon", "Principality of Zeon",
            "Earth Federation", "Side 3", "Munzo",
            "Zaku II", "Gundam", "Mobile Worker",
            "Mobile Suit", "Newtype", "Minovsky",
            "White Base"
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
            Map.entry("Base Branca", "White Base"),
            Map.entry("Trabalhador Móvel", "Mobile Worker")
        ));
    }
}
