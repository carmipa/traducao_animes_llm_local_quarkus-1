package org.traducao.projeto.contexto.lore.gundam;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoGundamUnicorn implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam Unicorn (OVA / Serie).
        - Personagens: Banagher Links (homem), Mineva Lao Zabi / Audrey Burne (mulher), Full Frontal (homem / clone de Char Aznable), Riddhe Marcenas (homem), Marida Cruz (mulher), Suberoa Zinnerman (homem), Otto Midas (homem), Daguza Mackle (homem).
        - Mechas / Termos: RX-0 Unicorn Gundam, MSN-06S Sinanju, NZ-666 Kshatriya, Nahel Argama, Caixa de Laplace (Laplace's Box), Psycho-Frame, NT-D System.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam Unicorn", LORE);

    @Override public String getId() { return "gundam_unicorn"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam Unicorn"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
