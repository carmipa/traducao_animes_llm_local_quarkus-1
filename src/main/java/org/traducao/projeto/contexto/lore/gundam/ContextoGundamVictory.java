package org.traducao.projeto.contexto.lore.gundam;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoGundamVictory implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Victory Gundam.
        - Personagens: Uso Ewin (homem), Shakti Kareen (mulher), Marbet Fingerhat (mulher), Chronicle Asher (homem), Katejina Loos (mulher), Maria Pure Armonia (mulher), Fonse Kagatie (homem).
        - Mechas / Termos: LM312V04 Victory Gundam, Victory 2 Gundam (V2), Imperio Zanscare, Liga Militar (League Militaire), Angel Halo.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Victory Gundam", LORE);

    @Override public String getId() { return "gundam_victory"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Victory Gundam"; }
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
            "Uso Ewin", "Shakti Kareen", "Marbet Fingerhat",
            "Chronicle Asher", "Katejina Loos", "Maria Pure Armonia",
            "Fonse Kagatie", "Odelo Henrik", "Zanscare Empire",
            "League Militaire", "BESPA", "Earth Federation",
            "Victory Gundam", "Victory 2 Gundam", "V2",
            "Zanneck", "Angel Halo", "Motorad Squadron",
            "Mobile Suit", "Newtype"
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
            Map.entry("Império Zanscare", "Zanscare Empire"),
            Map.entry("Imperio Zanscare", "Zanscare Empire"),
            Map.entry("Liga Militar", "League Militaire"),
            Map.entry("Auréola do Anjo", "Angel Halo"),
            Map.entry("Halo do Anjo", "Angel Halo")
        ));
    }
}
