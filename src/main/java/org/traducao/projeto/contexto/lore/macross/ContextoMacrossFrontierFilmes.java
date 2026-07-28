package org.traducao.projeto.contexto.lore.macross;

import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * PROPÓSITO DE NEGÓCIO: lore agregada dos filmes Macross Frontier (Itsuwari no Utahime /
 * Itsuka no Tsubasa / Labyrinth of Time) — referência/agregadora, sem {@code @Component}
 * (fora do registro CDI), pelo mesmo motivo de {@link ContextoMacrossDeltaFilmes}.
 *
 * <p>INVARIANTES DO DOMÍNIO: preferir os contextos específicos
 * ({@code macross_frontier_filme1} / {@code macross_frontier_filme2}) quando o arquivo for
 * de um filme só. Oferecer a lore misturada no seletor convida a traduzir o primeiro filme
 * com termos que só aparecem no segundo — que é exatamente o risco que a exclusão evita.
 * Não entra no manifesto E7a.
 *
 * <p>Esta classe estava sem {@code @Component} e SEM esta explicação, mas COM o import da
 * anotação sobrando — o que a fazia parecer anotação esquecida, e não exclusão deliberada.
 * A ausência de documentação era o defeito; a ausência da anotação, não.
 *
 * <p>LACUNA CONHECIDA: "Labyrinth of Time" (o terceiro filme) não tem contexto próprio, então
 * hoje só esta agregadora o cobre — e ela não é selecionável. Traduzi-lo cai no
 * {@code macross_frontier} da série. Registrar ESTA classe não é a correção: seria dar ao
 * operador uma lore de três filmes misturados. A correção é um contexto próprio para ele.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; mapa de terminologia imutável.
 */
public class ContextoMacrossFrontierFilmes implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Frontier (Filmes: Itsuwari no Utafihme / Itsuka no Tsubasa / Labyrinth of Time).
        - Personagens: Alto Saotome (homem), Sheryl Nome (mulher), Ranka Lee (mulher), Michael Blanc (homem), Luca Angeloni (homem), Klan Klang (mulher), Ozma Lee (homem), Brera Sterne (homem), Grace O'Connor (mulher).
        - Naves / Mechas: VF-25 Messiah, VF-27 Lucifer, YF-29 Durandal, Vajra.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross Frontier (Filmes)", LORE);

    @Override public String getId() { return "macross_frontier_filmes"; }
    @Override public String getNomeExibicao() { return "Macross Frontier: Filmes (False Songstress / Wings of Farewell)"; }
    @Override public String obterPromptSistema() { return PROMPT; }

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
