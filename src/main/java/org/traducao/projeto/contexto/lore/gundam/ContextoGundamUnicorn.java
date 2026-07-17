package org.traducao.projeto.contexto.lore.gundam;

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
            "Banagher Links", "Mineva Lao Zabi", "Audrey Burne", "Full Frontal",
            "Marida Cruz", "Riddhe Marcenas", "Unicorn Gundam", "Banshee", "Sinanju",
            "Kshatriya", "Laplace's Box", "Psycho-Frame", "Nahel Argama", "Sleeves",
            "Newtype", "Cyber-Newtype", "Spacenoid", "NT-D", "Minovsky"
        );
    }
}
