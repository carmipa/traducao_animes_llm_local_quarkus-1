package org.traducao.projeto.correcaoLegendas.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;

import java.util.Optional;

@Service
public class CorretorTraducaoLlmService {

    private final LlmPort llmPort;
    private final MascaradorTags mascaradorTags;
    private final ValidadorTraducaoService validador;
    private final ProtecaoLegendaAssService protecaoAss;

    public CorretorTraducaoLlmService(
        LlmPort llmPort,
        MascaradorTags mascaradorTags,
        ValidadorTraducaoService validador,
        ProtecaoLegendaAssService protecaoAss
    ) {
        this.llmPort = llmPort;
        this.mascaradorTags = mascaradorTags;
        this.validador = validador;
        this.protecaoAss = protecaoAss;
    }

    /**
     * Retorna a tradução corrigida via LLM apenas se a tradução atual estiver com
     * resíduo em inglês/preâmbulo (ValidadorTraducaoService) — evita chamar o LLM
     * para falas que já estão corretas.
     */
    public Optional<String> corrigirSeNecessario(String originalEn, String traduzidoAtual) {
        String motivo;
        try {
            validador.validarFala(traduzidoAtual);
            return Optional.empty();
        } catch (AlucinacaoDetectadaException e) {
            motivo = e.getMessage();
        }

        MascaradorTags.Mascarado mascOriginal = mascaradorTags.mascarar(originalEn != null ? originalEn : "");
        MascaradorTags.Mascarado mascTraduzido = mascaradorTags.mascarar(traduzidoAtual);

        Optional<String> resposta = llmPort.corrigirTraducao(
            mascOriginal.texto(),
            mascTraduzido.texto(),
            motivo
        );
        if (resposta.isEmpty()) {
            return Optional.empty();
        }

        try {
            String desmascarado = mascaradorTags.desmascarar(resposta.get(), mascTraduzido.tags());
            validador.validarFala(desmascarado);
            if (protecaoAss.respostaSuspeita(originalEn, desmascarado)) {
                return Optional.empty();
            }
            return Optional.of(desmascarado);
        } catch (AlucinacaoDetectadaException e) {
            return Optional.empty();
        }
    }
}
