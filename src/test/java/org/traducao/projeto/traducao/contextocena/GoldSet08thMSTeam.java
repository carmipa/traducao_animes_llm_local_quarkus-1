package org.traducao.projeto.traducao.contextocena;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: conjunto-ouro de regressão dos casos REAIS de inversão de gênero
 * observados por Paulo no Gundam 08th MS Team — a verdade-base rotulada contra a qual a
 * futura correção por contexto de cena será medida (linha-base do A/B). Cada caso traz a
 * fala original em inglês, sobre quem a flexão concorda, o gênero esperado (pela lore
 * {@code ContextoGundam08thMSTeam}, onde Shiro é (m) e Aina é (f)) e a saída PT-BR ERRADA
 * observada hoje.
 *
 * <p>INVARIANTES DO DOMÍNIO: cobre as duas direções da falha — homem tratado como mulher
 * (Shiro/piloto → flexão feminina) e mulher tratada como homem (Aina → flexão masculina) —
 * e os dois papéis relevantes (FALANTE e DESTINATÁRIO). Todo {@code baselineAtual} é uma
 * inversão real, jamais um gabarito. As janelas de vizinhança ficam vazias em D0: ainda não
 * extraímos o inglês original das linhas adjacentes; o campo existe no schema para as
 * subfases seguintes. Lista imutável.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: provedor estático puro; não faz I/O e não lança.
 */
public final class GoldSet08thMSTeam {

    private GoldSet08thMSTeam() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve os casos-ouro rotulados do 08th MS Team.
     * <p>INVARIANTES DO DOMÍNIO: lista imutável não vazia; cada caso tem gênero esperado
     * marcado (M/F) e uma baseline de inversão real.
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca devolve {@code null}.
     */
    public static List<GoldCaseConcordancia> casos() {
        return List.of(
            // Homem tratado como mulher — Shiro Amada (m), 1ª pessoa.
            new GoldCaseConcordancia(
                0, "I'm sure...", List.of(), List.of(),
                "Shiro Amada", PapelSemantico.FALANTE, GeneroFlexao.MASCULINO, true,
                "Estou certa..."),
            // Homem tratado como mulher — piloto masculino da 08th, 1ª pessoa.
            new GoldCaseConcordancia(
                0, "I'm saved!", List.of(), List.of(),
                "Piloto masculino (08th)", PapelSemantico.FALANTE, GeneroFlexao.MASCULINO, true,
                "Estou salva!"),
            // Mulher tratada como homem — Aina Sahalin (f) agradece, 1ª pessoa.
            new GoldCaseConcordancia(
                0, "Thank you, Norris.", List.of(), List.of(),
                "Aina Sahalin", PapelSemantico.FALANTE, GeneroFlexao.FEMININO, true,
                "Obrigado, Norris."),
            // Mulher tratada como homem — pergunta dirigida a Aina (f), 2ª pessoa/destinatário.
            new GoldCaseConcordancia(
                0, "Are you mad?!", List.of(), List.of(),
                "Aina Sahalin", PapelSemantico.DESTINATARIO, GeneroFlexao.FEMININO, true,
                "Estás louco?!")
        );
    }
}
