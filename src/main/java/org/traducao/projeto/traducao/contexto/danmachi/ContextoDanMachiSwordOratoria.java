package org.traducao.projeto.traducao.contexto.danmachi;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoDanMachiSwordOratoria implements ProvedorContexto {
    private static final String PROMPT = ContextoPrompt.montar("DanMachi: Sword Oratoria", "- Obra: Sword Oratoria: Is It Wrong to Try to Pick Up Girls in a Dungeon? On the Side.\n- Personagens: Ais Wallenstein, Lefiya Viridis, Finn Deimne, Riveria Ljos Alf, Gareth Landrock, Tiona Hiryute, Tione Hiryute, Bete Loga.");
    @Override public String getId() { return "danmachi_so"; }
    @Override public String getNomeExibicao() { return "DanMachi: Sword Oratoria"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
