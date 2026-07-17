package org.traducao.projeto.contexto.lore.danmachi;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore da 5ª temporada de DanMachi (arco Freya / Goddess of
 * Fertility) com grafia canônica Syr Flova.
 *
 * <p>INVARIANTES DO DOMÍNIO: Syr Flova (nunca "Flover"); Folkvangr; Charm divino;
 * nomes da Freya Familia.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoDanMachiS5 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Is It Wrong to Try to Pick Up Girls in a Dungeon? V / DanMachi Season 5 (Goddess of Fertility Arc / Freya Arc).
        - Titulo e abreviacao: manter DanMachi, Familia e Orario como termos da obra; nao traduzir nomes de personagens, Familias, deuses, titulos, apelidos ou tecnicas.
        - Personagens principais: Bell Cranel (homem), Hestia (deusa, mulher), Freya (deusa, mulher), Syr Flova (mulher; também ligada a Horn), Horn (mulher), Ottar (homem), Allen Fromel (homem), Hedin Selland (homem), Hogni Ragnar (homem), Heith Velvet (mulher), Mia Grand (mulher), Ryuu Lion / Ryu Lion (mulher), Ais Wallenstein (mulher), Liliruca Arde/Lili (mulher), Welf Crozzo (homem), Haruhime Sanjouno (mulher), Mikoto Yamato (mulher).
        - Familias e grupos: Hestia Familia, Freya Familia, Loki Familia, Hostess of Fertility, Benevolent Mistress. Manter nomes oficiais; "Hostess of Fertility" e "Benevolent Mistress" nao devem ser traduzidos livremente quando usados como nome proprio.
        - Locais: Orario, Babel, Folkvangr, Hostess of Fertility, Pleasure Quarter. Preserve nomes canonicos.
        - Titulos/apelidos: Goddess of Beauty, Goddess of Fertility, War Game, Familia, Level, Status, Skill, Charm. "Charm" de Freya e poder divino; nao reduzir a "charme" casual.
        - Regras de nomes: Bell Cranel nao vira "sino"; Syr Flova (grafia canônica — nunca "Flover" nem adaptar para flor/florista); Freya, Hestia, Ottar, Hedin, Hogni, Allen, Horn, Mia e Heith mantem grafia; Ryuu/Ryu nao vira "dragao".
        - Tom: arco de romance, manipulacao divina e conflito emocional; Freya sedutora e soberana, Syr calorosa/misteriosa, Bell sincero e resistente, Hestia protetora.
        """;

    private static final String PROMPT = ContextoPrompt.montar("DanMachi (Season 5)", LORE);

    @Override
    public String getId() {
        return "danmachi_s5";
    }

    @Override
    public String getNomeExibicao() {
        return "DanMachi (Season 5)";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege nomes do arco Freya / Syr Flova.
     * <p>INVARIANTES DO DOMÍNIO: grafia Syr Flova canônica.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Bell Cranel", "Hestia", "Freya", "Syr Flova", "Horn", "Ottar",
            "Allen Fromel", "Hedin Selland", "Hogni Ragnar", "Heith Velvet",
            "Mia Grand", "Ryuu Lion", "Ais Wallenstein", "Liliruca Arde",
            "Orario", "Folkvangr", "Falna", "Familia"
        );
    }
}
