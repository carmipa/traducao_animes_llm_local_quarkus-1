package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

@Component
public class ContextoRevisaoLoreDanMachi implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: DanMachi / Is It Wrong to Try to Pick Up Girls in a Dungeon?.
        - Regra central: nao traduzir nomes de personagens, deuses, Familias, locais, tecnicas, armas, monstros especiais ou termos de sistema.
        - Personagens: Bell Cranel, Hestia, Ais Wallenstein, Liliruca Arde/Lili, Welf Crozzo, Ryuu Lion/Ryu Lion, Freya, Syr Flover, Hermes, Loki, Hephaistos, Mia Grand.
        - Termos de mundo: Orario, Dungeon, Familia, Hestia Familia, Loki Familia, Freya Familia, Status, Skill, Level, Magic Stone, Adventurer.
        - Tecnicas/equipamentos: Firebolt, Hestia Knife, Crozzo Magic Sword.
        - Alertas: Bell nao vira "sino"; Ryuu/Ryu nao vira "dragao"; Syr Flover nao vira "flor"; Familia como nome de grupo deve manter forma canonica.
        - Nao corrigir estilo da fala; corrigir somente nome, local, organizacao, objeto, tecnica, monstro ou termo de lore traduzido errado.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "danmachi"; }
    @Override public String getNomeExibicao() { return "DanMachi - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
