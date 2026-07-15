package org.traducao.projeto.analisadorMidia.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.analisadorMidia.domain.AuditoriaResultado;
import org.traducao.projeto.analisadorMidia.domain.LegendaInfo;
import org.traducao.projeto.analisadorMidia.domain.VideoInfo;
import org.traducao.projeto.telemetria.MidiaTelemetria;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: converte o resultado técnico de uma mídia auditada em
 * um registro de telemetria anonimizado, alimentando o dataset permanente do
 * projeto (diagnóstico + melhoria futura).
 *
 * <p>INVARIANTES DO DOMÍNIO: o caminho é relativizado à entrada para preservar
 * privacidade (não grava caminhos pessoais absolutos); a telemetria carrega
 * apenas metadados técnicos, nunca falas ou conteúdo da legenda.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: se a relativização falhar, cai para o nome
 * simples do arquivo, sem lançar exceção.
 */
@Service
public class TelemetriaMidiaMapper {

    /**
     * PROPÓSITO DE NEGÓCIO: monta o {@link MidiaTelemetria} de uma mídia a partir
     * do resultado da auditoria, relativizando o caminho à {@code entrada}.
     * <p>INVARIANTES DO DOMÍNIO: nomeArquivo relativo (privacidade); métricas de
     * vídeo vêm da primeira faixa quando existir.
     * <p>COMPORTAMENTO EM CASO DE FALHA: exceção na relativização é engolida e o
     * nome simples do arquivo é usado como fallback.
     */
    public MidiaTelemetria mapear(AuditoriaResultado resultado, Path entrada, String registradoEm) {
        String nomeRelativo = resultado.caminhoArquivo().getFileName().toString();
        try {
            nomeRelativo = entrada.toAbsolutePath()
                .relativize(resultado.caminhoArquivo().toAbsolutePath()).toString();
        } catch (Exception ignored) {
            // Mantém o nome simples do arquivo como fallback de privacidade.
        }

        double tamanhoMB = resultado.container().tamanhoBytes() / (1024.0 * 1024.0);

        String codecVideo = "N/A";
        String resolucao = "N/A";
        double fps = 0.0;
        if (!resultado.videos().isEmpty()) {
            VideoInfo v = resultado.videos().getFirst();
            codecVideo = v.codecId();
            resolucao = v.width() + "x" + v.height();
            fps = v.fps();
        }

        List<MidiaTelemetria.LegendaTelemetria> legTels = new ArrayList<>();
        for (LegendaInfo leg : resultado.legendas()) {
            legTels.add(new MidiaTelemetria.LegendaTelemetria(
                leg.indexRelativo() + 1,
                leg.idioma(),
                leg.formato(),
                leg.tipoCurto(),
                leg.categoria(),
                leg.traduzivel(),
                leg.diferencaFimSegundos()
            ));
        }

        return new MidiaTelemetria(
            nomeRelativo,
            resultado.container().formato(),
            tamanhoMB,
            resultado.container().duracaoSegundos(),
            codecVideo,
            resolucao,
            fps,
            legTels,
            registradoEm
        );
    }
}
