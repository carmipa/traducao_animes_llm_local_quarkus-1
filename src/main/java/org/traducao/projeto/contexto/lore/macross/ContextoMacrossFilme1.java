package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoMacrossFilme1 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross: Do You Remember Love? / Ai Oboete Imasu ka.
        - Filme reconta Macross com guerra entre humanos, Zentradi e Meltrandi, tendo a musica/cultura como chave para comunicacao e paz.
        - Termos centrais: SDF-1 Macross, Zentradi, Meltrandi, Protocultura, Valkyrie, Variable Fighter, Minmay Attack.
        - Principais nomes: Hikaru Ichijyo, Lynn Minmay, Misa Hayase, Roy Focker, Maximilian Jenius/Max Jenius, Milia Fallyna, Exsedol Folmo, Boddole Zer, Britai Kridanik.
        - Mechas/naves: VF-1 Valkyrie, VF-1S Strike Valkyrie, Queadluun-Rau, Nousjadeul-Ger, SDF-1.
        - Use "Protocultura" em portugues e mantenha Zentradi/Meltrandi/Valkyrie.
        - Tom: mais epico e romantico que a serie TV, com frases dramaticas e letras de musica importantes; preserve solenidade sem exagerar formalidade.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross: Do You Remember Love?", LORE);

    @Override
    public String getId() {
        return "macross_filme1";
    }

    @Override
    public String getNomeExibicao() {
        return "Macross: Do You Remember Love?";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: restaura grafias oficiais Macross (Valkyrie/Zentradi) quando
     * o LLM localiza indevidamente — mapa compartilhado da franquia.
     *
     * <p>INVARIANTES DO DOMÍNIO: só aplica com canônico no original EN.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public java.util.Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacross.mapa();
    }

}
