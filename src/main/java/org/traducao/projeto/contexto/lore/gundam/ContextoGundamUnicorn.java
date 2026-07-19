package org.traducao.projeto.contexto.lore.gundam;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore de Gundam Unicorn (UC 0096) com núcleo UC completo.
 *
 * <p>INVARIANTES DO DOMÍNIO: Laplace's Box; Unicorn/Banshee; Newtype/Spacenoid;
 * Mobile Suit vs Armor; Psycho-Frame; Sleeves.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoGundamUnicorn implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam Unicorn (OVA/serie) — Universal Century 0096.
        - Premissa: caca a Laplace's Box; Banagher Links e o RX-0 Unicorn Gundam;
          Federation, Sleeves (Neo Zeon) e Vist Foundation.

        === Nucleo UC ===
        - Newtype / Cyber-Newtype / Oldtype: oficiais; NUNCA "Novo Tipo".
        - Spacenoid vs Earthnoid: segregacao colonial vs elite terrestre — subtexto politico.
        - Mobile Suit (Unicorn, Banshee, Sinanju, Kshatriya como MS) vs Mobile Armor
          quando a obra designar unidade nao-humanoide de grande porte.
        - Minovsky particles; psycho-frame / Psycho-Frame; NT-D; psycommu; funnel.
        - Earth Federation vs Principality/remnants of Zeon (Sleeves).

        === Pessoas (genero) ===
        - Banagher Links (m), Mineva Lao Zabi / Audrey Burne (f), Full Frontal (m),
          Riddhe Marcenas (m), Marida Cruz (f), Suberoa Zinnerman (m), Otto Midas (m),
          Daguza Mackle (m), Martha Vist Carbine (f), Cardeas Vist (m), Alberto Vist (m),
          Angelo Sauper (m).

        === Mecha / termos ===
        - RX-0 Unicorn Gundam, RX-0 Unicorn Gundam 02 Banshee, MSN-06S Sinanju,
          NZ-666 Kshatriya, Nahel Argama, Garencieres.
        - Laplace's Box / Laplace Incident; Industrial 7; Anaheim Electronics; Vist Foundation.
        - Phenex NAO e unidade desta obra (aparece em NT) — nao misturar.

        === Regras ===
        - Unicorn/Banshee/Sinanju/Kshatriya/Laplace's Box oficiais.
        - Tom: politico-militar UC, melancolico, legado de Char / Newtype.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam Unicorn", LORE);

    @Override
    public String getId() {
        return "gundam_unicorn";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam Unicorn";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Banagher Links", "Mineva Lao Zabi", "Audrey Burne",
            "Full Frontal", "Marida Cruz", "Riddhe Marcenas",
            "Suberoa Zinnerman", "Otto Midas", "Daguza Mackle",
            "Martha Vist Carbine", "Cardeas Vist", "Alberto Vist",
            "Angelo Sauper", "Unicorn Gundam", "Banshee",
            "Sinanju", "Kshatriya", "Nahel Argama",
            "Garencieres", "Earth Federation", "Sleeves",
            "Neo Zeon", "Vist Foundation", "Anaheim Electronics",
            "Laplace's Box", "Industrial 7", "Newtype",
            "Cyber-Newtype", "Oldtype", "Spacenoid",
            "Earthnoid", "Minovsky", "Psycho-Frame",
            "NT-D", "psycommu", "funnel",
            "Mobile Suit", "Mobile Armor"
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
            Map.entry("Mangas", "Sleeves"),
            Map.entry("Manga", "Sleeves"),
            Map.entry("Moldura Psíquica", "Psycho-Frame"),
            Map.entry("Caixa de Laplace", "Laplace's Box"),
            Map.entry("Fundação Vist", "Vist Foundation")
        ));
    }
}
