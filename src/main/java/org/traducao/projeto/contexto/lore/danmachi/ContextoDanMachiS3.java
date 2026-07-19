package org.traducao.projeto.contexto.lore.danmachi;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore da 3ª temporada de DanMachi (arco Xenos) para
 * preservar nomes de monstros inteligentes e facções.
 *
 * <p>INVARIANTES DO DOMÍNIO: Xenos, Wiene, Fels, Dix Perdix, Asterios; Liliruca Arde.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoDanMachiS3 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Is It Wrong to Try to Pick Up Girls in a Dungeon? Season 3 (DanMachi III) — Xenos Arc.
        - Premissa: Bell encontra Wiene e os Xenos (monstros inteligentes) sob Orario; conflito com
          aventureiros caçadores e a Ikelos Familia; revelações sobre Asterios e o Dungeon.
        - Locais: Orario, Dungeon, Knossos, Rua Daedalus, Under Orario / base Xenos.
        - Grupos: Hestia Familia, Xenos, Hermes Familia, Ikelos Familia, Ganesha Familia, Guilda.
        - Personagens (gênero): Bell Cranel (m), Hestia (f), Wiene (f), Fels (ambíguo/capuz — tratar
          conforme o original; frequentemente neutro/"eles" evita erro), Dix Perdix (m),
          Asterios (m), Liliruca Arde / Lili (f), Welf Crozzo (m), Mikoto Yamato (f),
          Haruhime Sanjouno (f), Aiz Wallenstein (f), Ouranos (m), Lido (m), Rei (f), Gros (m).
        - Termos: Xenos, Knossos, Dungeon, Familia, Falna, Status, Irregular, Monster Rex.
        - Regras: Xenos não traduzir como "alienígenas" genéricos quando for o nome do grupo;
          Wiene/Asterios/Fels mantêm grafia; Bell ≠ "sino"; Liliruca ≠ "Lilisuka".
        - Tom: moralmente cinza, empatia com os Xenos, tensão e perseguição.
        """;

    private static final String PROMPT = ContextoPrompt.montar("DanMachi (Season 3)", LORE);

    @Override
    public String getId() {
        return "danmachi_s3";
    }

    @Override
    public String getNomeExibicao() {
        return "DanMachi (Season 3)";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege nomes do arco Xenos.
     * <p>INVARIANTES DO DOMÍNIO: só artefatos confirmados da temporada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Bell Cranel", "Hestia", "Liliruca Arde",
            "Lili", "Welf Crozzo", "Mikoto Yamato",
            "Haruhime Sanjouno", "Aiz Wallenstein", "Wiene",
            "Fels", "Dix Perdix", "Asterios",
            "Lido", "Rei", "Gros",
            "Xenos", "Knossos", "Ouranos",
            "Ikelos Familia", "Hermes Familia", "Ganesha Familia",
            "Hestia Familia", "Orario", "Dungeon",
            "Falna", "Familia", "Status",
            "Level", "Skill", "Magic",
            "Monster Rex", "Irregular", "Guild"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reforço determinístico da terminologia DanMachi (Familia sem
     * acento + grafias erradas de nomes proprios da obra).
     * <p>INVARIANTES DO DOMÍNIO: forma-ruim PT → canônico; só aplica se o EN contém o canônico.
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaDanMachi.comExtras(Map.ofEntries(
            Map.entry("Lilisuka", "Liliruca Arde"),
            Map.entry("Liriruca", "Liliruca Arde")
        ));
    }
}
