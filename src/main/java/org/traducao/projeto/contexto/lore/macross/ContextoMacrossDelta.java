package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoMacrossDelta implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Delta (Serie TV).
        - Premissa: Em 2067 no aglomerado de Brisingr, a Síndrome Var causa fúria incontrolável em humanos e Zentradi. A Unidade Tática Musical Walküre usa ondas de canção (Fold Waves) protegidas pelo Esquadrão Delta (Delta Flight) para conter a epidemia contra o Reino de Windermere.
        - Unidade Tática Musical Walküre (Todas Femininas):
          * Freyja Wion (mulher / cantora vinda de Windermere)
          * Mikumo Guynemer (mulher / cantora principal / Diva misteriosa)
          * Kaname Buccaneer (mulher / líder da Walküre)
          * Makina Nakajima (mulher / mecânica e cantora)
          * Reina Prowler (mulher / hacker e cantora)
        - Esquadrão Delta / Delta Flight (Pilotos Chaos / VF-31 Siegfried):
          * Hayate Immelmann (homem / piloto de elite)
          * Mirage Farina Jenius (mulher / piloto de elite / neta de Max e Milia Jenius)
          * Arad Molders (homem / comandante do Esquadrão Delta)
          * Chuck Mustang (homem / piloto e cozinheiro)
          * Messer Ihlefeld (homem / o "Ás Branco" / piloto)
        - Reino de Windermere & Cavaleiros Aéreos (Aerial Knights):
          * Keith Aero Windermere (homem / o "Cavaleiro Branco")
          * Heinz Nerich Windermere (homem / Rei Príncipe Cantor)
          * Roid Brehm (homem / chanceler)
          * Bogue Convaart (homem / cavaleiro aéreo)
          * Cassim Eberhard (homem), Herman Kroos (homem)
        - Organizações e Termos: Chaos (corporação militar privada), NUNS (New United Nations Spacy), Walküre, Delta Flight, Aerial Knights, Windermere, Planeta Ragna, Var Syndrome (Síndrome Var), Fold Waves (Ondas Fold), Protocultura.
        - Mechas/Naves: VF-31 Siegfried, Sv-262 Draken III, Macross Elysion.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross Delta (Série TV)", LORE);

    @Override
    public String getId() { return "macross_delta"; }

    @Override
    public String getNomeExibicao() { return "Macross Delta (Série TV)"; }

    @Override
    public String obterPromptSistema() { return PROMPT; }
}
