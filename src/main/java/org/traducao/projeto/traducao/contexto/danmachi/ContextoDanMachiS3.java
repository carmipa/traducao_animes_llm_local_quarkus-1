package org.traducao.projeto.traducao.contexto.danmachi;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoDanMachiS3 implements ProvedorContexto {
    private static final String PROMPT = ContextoPrompt.montar("DanMachi (Season 3)", "- Obra: DanMachi Season 3 (Xenos Arc).\n- Personagens: Bell Cranel, Hestia, Wiene, Fels, Dix, Asterius.");
    @Override public String getId() { return "danmachi_s3"; }
    @Override public String getNomeExibicao() { return "DanMachi (Season 3)"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
