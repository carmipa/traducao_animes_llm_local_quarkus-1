package org.traducao.projeto.contexto.lore.gundam.warInpocket;

import java.util.Map;
import java.util.Set;
import org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoWarInPocket implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam 0080: War in the Pocket, Universal Century 0079/0080, fim da Guerra de Um Ano.
        - Faccao/locais: Federacao Terrestre, Principado de Zeon, Side 6, Republica de Riah, colonia Libot, Base Antartica.
        - Principais nomes e genero: Alfred "Al" Izuruha (menino), Bernard "Bernie" Wiseman (homem), Christina "Chris" Mackenzie (mulher),
          Mikhail "Misha" Kaminsky (homem), Hardy Steiner (homem), Gabriel Ramirez Garcia (homem), Andy Strauss (homem).
        - Chris/Christina e piloto mulher da Federacao; falas dela em 1a pessoa devem usar feminino quando houver indicio de que ela fala.
        - Al e menino/crianca; quando adultos falam com ele, use garoto/menino, nao garota/menina.
        - Unidades: Cyclops Team, Forcas Especiais de Zeon, guarnicao da Federacao.
        - Mobile suits: RX-78NT-1 Gundam Alex, Zaku II Kai, Kampfer, Hygogg, Z'Gok-E, GM Cold Districts Type, GM Sniper II.
        - Termos UC: mobile suit, mobile armor, beam rifle, beam saber, colony, Side, Zeon, Federation, Newtype. Use mobile suit/mobile armor em ingles como termo da franquia.
        - Tom: tragedia de guerra vista por uma crianca; preserve inocencia de Al, humanidade de Bernie e ambiguidade moral. Evite glamourizar combate quando a cena e anti-guerra.
        """;

    private static final String PROMPT_GUNDAM_0080 = ContextoPrompt.montar("Mobile Suit Gundam 0080: War in the Pocket", LORE);

    @Override
    public String getId() {
        return "gundam_0080";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam 0080: War in the Pocket";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT_GUNDAM_0080;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: elenco, mechas, naves, facções e terminologia canônica desta obra
     * que a tradução deve preservar no original — proteção upfront contra localização indevida.
     * <p>INVARIANTES DO DOMÍNIO: grafias oficiais; conjunto imutável.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Alfred Izuruha", "Al", "Bernard Wiseman",
            "Bernie", "Christina Mackenzie", "Chris",
            "Mikhail Kaminsky", "Misha", "Hardy Steiner",
            "Gabriel Ramirez Garcia", "Andy Strauss", "Cyclops Team",
            "Earth Federation", "Principality of Zeon", "Zeon",
            "Side 6", "Republic of Riah", "Libot",
            "Gundam Alex", "Zaku II Kai", "Kampfer",
            "Hygogg", "Z'Gok-E", "GM Sniper II",
            "Mobile Suit", "Mobile Armor", "Beam Rifle",
            "Beam Saber", "Newtype"
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
            Map.entry("Equipe Cyclops", "Cyclops Team"),
            Map.entry("Equipe Cíclope", "Cyclops Team"),
            Map.entry("República de Riah", "Republic of Riah")
        ));
    }
}
