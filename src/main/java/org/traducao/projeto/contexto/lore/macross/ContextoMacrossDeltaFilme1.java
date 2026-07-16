package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoMacrossDeltaFilme1 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Delta O Filme: Passionate Walküre (Gekijou no Walkuere).
        - Releitura dos eventos da série TV Macross Delta focando na batalha contra o Reino de Windermere e o plano da Síndrome Var.
        - Unidade Musical Walküre (Todas Femininas): Freyja Wion, Mikumo Guynemer, Kaname Buccaneer, Makina Nakajima, Reina Prowler.
        - Pilotos do Esquadrão Delta: Hayate Immelmann (homem), Mirage Farina Jenius (mulher), Arad Molders (homem), Chuck Mustang (homem), Messer Ihlefeld (homem).
        - Cavaleiros Aéreos de Windermere: Keith Aero Windermere (homem), Heinz Nerich Windermere (homem), Roid Brehm (homem).
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross Delta: Filme 1 - Passionate Walküre", LORE);

    @Override public String getId() { return "macross_delta_filme1"; }
    @Override public String getNomeExibicao() { return "Macross Delta: Filme 1 - Passionate Walküre"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
