package org.traducao.projeto.contexto.lore.danmachi;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoDanMachiS5 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Is It Wrong to Try to Pick Up Girls in a Dungeon? V / DanMachi Season 5 (Goddess of Fertility Arc).
        - Titulo e abreviacao: manter DanMachi, Familia e Orario como termos da obra; nao traduzir nomes de personagens, Familias, deuses, titulos, apelidos ou tecnicas.
        - Personagens principais: Bell Cranel (homem), Hestia (deusa, mulher), Freya (deusa, mulher), Syr Flover (mulher), Horn (mulher), Ottar (homem), Allen Fromel (homem), Hedin Selland (homem), Hogni Ragnar (homem), Heith Velvet (mulher), Mia Grand (mulher), Ryu/Ryuu Lion (mulher), Ais Wallenstein (mulher), Liliruca Arde/Lili (mulher), Welf Crozzo (homem), Haruhime Sanjouno (mulher).
        - Familias e grupos: Hestia Familia, Freya Familia, Loki Familia, Hostess of Fertility, Benevolent Mistress. Manter nomes oficiais; "Hostess of Fertility" e "Benevolent Mistress" podem aparecer como nomes de estabelecimento/grupo e nao devem ser traduzidos livremente quando usados como nome proprio.
        - Locais: Orario, Babel, Folkvangr, Hostess of Fertility, Pleasure Quarter. Preserve nomes canonicos quando forem lugar ou instituicao.
        - Titulos/apelidos: Goddess of Beauty, Goddess of Fertility, War Game, Familia, Level, Status, Skill, Charm. "Charm" de Freya e conceito especifico; nao traduzir como charme casual se for poder divino.
        - Regras de nomes: Bell Cranel nao deve virar "sino"; Syr Flover nao deve ser alterado para flor/florista; Freya, Hestia, Ottar, Hedin, Hogni, Allen, Horn, Mia e Heith mantem grafia original.
        - Tom: arco de romance, manipulacao divina e conflito emocional; Freya e sedutora e soberana, Syr e calorosa/misteriosa, Bell e sincero e resistente, Hestia e protetora.
        """;

    private static final String PROMPT = ContextoPrompt.montar("DanMachi (Season 5)", LORE);

    @Override public String getId() { return "danmachi_s5"; }
    @Override public String getNomeExibicao() { return "DanMachi (Season 5)"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
