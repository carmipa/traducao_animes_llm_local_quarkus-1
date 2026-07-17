package org.traducao.projeto.contexto.lore.danmachi;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore do filme Arrow of the Orion — correção de grafia
 * Liliruca e elenco do filme.
 *
 * <p>INVARIANTES DO DOMÍNIO: Liliruca Arde (nunca Liriruca/Lilisuka); Artemis;
 * nomes oficiais do filme.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoDanMachiOrion implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Is It Wrong to Try to Pick Up Girls in a Dungeon?: Arrow of the Orion (filme).
        - Premissa: a deusa Artemis chega a Orario; Bell e companheiros enfrentam uma ameaça
          ligada a um monstro ancestral e a uma flecha/ritual divino.
        - Locais: Orario, Dungeon, arredores / vilarejo ligado à trama do filme.
        - Personagens (gênero): Bell Cranel (m), Hestia (f), Artemis (f), Ais Wallenstein (f),
          Hermes (m), Liliruca Arde / Lili (f), Welf Crozzo (m), Mikoto Yamato (f),
          Haruhime Sanjouno (f), Finn Deimne (m), Riveria Ljos Alf (f), Lefiya Viridis (f).
        - Regras: Liliruca Arde NUNCA "Liriruca" nem "Lilisuka"; Bell ≠ "sino";
          Artemis/Hermes/Hestia mantêm grafia divina; Ais = Sword Princess de forma consistente.
        - Termos: Familia, Falna, Dungeon, Status, Level, Orario.
        - Tom: aventura cinematográfica, humor leve e momentos heroicos.
        """;

    private static final String PROMPT = ContextoPrompt.montar("DanMachi: Arrow of the Orion", LORE);

    @Override
    public String getId() {
        return "danmachi_movie";
    }

    @Override
    public String getNomeExibicao() {
        return "DanMachi: Arrow of the Orion (Filme)";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege nomes do filme Orion.
     * <p>INVARIANTES DO DOMÍNIO: grafia Liliruca correta.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Bell Cranel", "Hestia", "Artemis", "Ais Wallenstein", "Hermes",
            "Liliruca Arde", "Lili", "Orario", "Dungeon", "Falna"
        );
    }
}
