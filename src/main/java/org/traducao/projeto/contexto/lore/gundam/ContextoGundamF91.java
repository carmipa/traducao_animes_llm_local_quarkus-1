package org.traducao.projeto.contexto.lore.gundam;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoGundamF91 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam F91, Universal Century 0123 (Cosmo Babylonia War).
        - Personagens: Seabook Arno (homem), Cecily Fairchild / Berah Ronah (mulher),
          Carozzo Ronah / Iron Mask (homem), Zabine Chareux (homem), Annamarie Bourget (mulher),
          Dorel Ronah (homem), Nadia Ronah (mulher), Theo Fairchild (homem), Birgit (homem),
          Reese (homem), Sam (homem), Arthur (homem), Azuma (homem), Leahlee (mulher),
          Monica Arno (mulher), Rafaella (mulher), Gillet (homem).
        - Familia Ronah: Meitzer, Carozzo, Dorel, Nadia e Berah sao pessoas DIFERENTES.
          Iron Mask e o codinome de Carozzo — cada fala usa a forma que o original usou.
        - Colonias/lugares: Frontier I, Frontier II, Frontier III, Frontier IV (o "Frontier Ill"
          que aparece na legenda e erro de OCR do fansub, leia Frontier III); Frontier Side;
          Richmond; Earth Federation.
        - Naves: Space Ark (NUNCA "Arca Espacial"); Zamouth Garr (nave, NUNCA "Garra de Zamouth").
        - Mechas / Termos: F91 Gundam Formula 91, Crossbone Vanguard (NUNCA "Vanguard Crossbone"),
          VSBR (Variable Speed Beam Rifle), MEPE (Afterimage Effect), Bio-Computer,
          Denan Zon, Denan Gei, Berga-Giros, Vigna-Ghina, Rafflesia.
        - Bugs: armas autonomas de extermínio, nome proprio. NUNCA "insetos".
        - Cosmo Babylonia (o Estado) e Cosmo Aristocracy (a doutrina) sao coisas distintas.
        - Newtype no feminino continua "Newtype" — nunca "Nova Tipo".
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
            "Bio-Computer", "Newtype", "Mobile Suit",
            // Medidos na legenda do filme (2.171 falas) em 2026-07-30: 84 de 109 nomes
            // proprios NAO estavam cadastrados. Os abaixo tem ocorrencia confirmada.
            "Dorel Ronah", "Nadia Ronah", "Theo Fairchild",
            "Birgit", "Reese", "Arthur", "Azuma", "Leahlee",
            "Monica Arno", "Gillet",
            "Frontier I", "Frontier II", "Frontier III", "Frontier IV",
            "Frontier Side", "Richmond", "Earth Federation",
            "Space Ark", "Zamouth Garr", "Cosmo Aristocracy",
            "MEPE"
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
            Map.entry("Cosmo Babilônia", "Cosmo Babylonia"),

            // ---------------------------------------------------------------------------
            // Formas-ruim MEDIDAS na tradução de 2026-07-30 (2.069 falas). Estar em
            // termosProtegidos() NÃO impede a localização — aquele conjunto é permissivo;
            // quem GARANTE é o EnforcadorTermosLore lendo este mapa. Cada entrada tem uma
            // fala real por trás.
            // ---------------------------------------------------------------------------

            // O pior caso do filme: as armas autônomas de extermínio viraram bichos.
            // "...be finished before the Bugs are activated?" -> "antes que os insetos
            // sejam ativados?". Perdido em 5 de 6 falas.
            Map.entry("insetos", "Bugs"),

            // "Space Ark" perdido em 11 de 12 — e com gênero errado ("no Arca Espacial").
            Map.entry("Arca Espacial", "Space Ark"),

            // Colônias: "Frontier" perdido em 11 de 21, em várias formas.
            Map.entry("Fronteira I", "Frontier I"),
            Map.entry("Fronteira II", "Frontier II"),
            Map.entry("Fronteira III", "Frontier III"),
            Map.entry("Fronteira IV", "Frontier IV"),
            Map.entry("IV Fronteira", "Frontier IV"),

            // Nave confundida com parte do corpo: "Garra de Zamouth", "Zamouth Gar".
            Map.entry("Garra de Zamouth", "Zamouth Garr"),
            Map.entry("Zamouth Gar", "Zamouth Garr"),

            // Ordem invertida e invenção: "Vanguard Crossbone", "Vanguard da Cruz Branca".
            Map.entry("Vanguard Crossbone", "Crossbone Vanguard"),
            Map.entry("Vanguard da Cruz Branca", "Crossbone Vanguard"),

            // Doutrina (Aristocracy) vs Estado (Babylonia): perdidos em 2/2 e 2/7.
            Map.entry("Aristocracia Cósmica", "Cosmo Aristocracy"),
            Map.entry("Cosmo Aristocracia", "Cosmo Aristocracy"),
            Map.entry("Babylonia do Cosmo", "Cosmo Babylonia"),
            Map.entry("Cosmo Babylônia", "Cosmo Babylonia"),

            // NÃO existe entrada para "bio-computador". As 2 falas do filme escrevem
            // "bio-computer" em MINÚSCULA ("your old lady designed its bio-computer"), e o
            // enforcer exige o canônico na grafia exata — "Bio-Computer" nunca casaria.
            // Minúscula ali é substantivo comum, e "bio-computador" é tradução legítima.

            // O mapa base do UC cobre "Novo Tipo"/"Neotipo" mas não o FEMININO, e o filme
            // produziu "Nova Tipo" em 2 de 9 falas ("Is it a Newtype?" -> "É um Nova Tipo?").
            Map.entry("Nova Tipo", "Newtype"),

            // ÍMÃ entre dois termos protegidos: "Iron Mask!" saiu "Carozzo Ronah!" em 2 de
            // 10 falas. São a mesma pessoa, mas a fala usa o codinome de propósito.
            // LIMITE: "Dorel Ronah?" também saiu "Carozzo Ronah", e a chave é a forma-ruim
            // — não dá para mapear "Carozzo Ronah" para dois canônicos no mesmo mapa.
            // Escolhida a de maior frequência; ver TerminologiaF91FormasMedidasTest.
            Map.entry("Carozzo Ronah", "Iron Mask")
        ));
    }
}
