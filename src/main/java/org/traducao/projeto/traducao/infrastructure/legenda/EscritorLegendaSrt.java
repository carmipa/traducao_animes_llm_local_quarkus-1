package org.traducao.projeto.traducao.infrastructure.legenda;

import org.springframework.stereotype.Component;
import org.traducao.projeto.core.util.ArquivoAtomicoUtil;
import org.traducao.projeto.traducao.domain.exceptions.ArquivoLegendaException;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: Reescreve um .srt a partir do {@link DocumentoLegenda},
 * preservando numeração e timestamps (guardados no índice e no {@code prefixo}
 * do evento) e trocando apenas o texto pela versão traduzida. É o par de saída
 * do {@link LeitorLegendaSrt}.
 *
 * <p>INVARIANTES DO DOMÍNIO: cada evento vira um bloco SRT válido (índice, linha
 * de tempo, texto, linha em branco de separação); as marcas {@code \N} de quebra
 * interna voltam a ser quebras reais no EOL do documento; escrita atômica.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: erro de IO → {@link ArquivoLegendaException},
 * sem deixar arquivo truncado (grava em temporário e move atomicamente).
 */
@Component
public class EscritorLegendaSrt {

    private static final char BOM = '﻿';

    public void escrever(Path destino, DocumentoLegenda documento) {
        String eol = documento.quebraDeLinha() != null ? documento.quebraDeLinha() : "\n";

        StringBuilder conteudo = new StringBuilder();
        if (documento.comBom()) {
            conteudo.append(BOM);
        }
        for (EventoLegenda evento : documento.eventos()) {
            String texto = evento.temTexto() ? evento.texto() : "";
            conteudo.append(evento.indice()).append(eol);
            conteudo.append(evento.prefixo()).append(eol); // prefixo = linha "start --> end"
            conteudo.append(texto.replace("\\N", eol)).append(eol);
            conteudo.append(eol); // linha em branco separando os blocos
        }

        try {
            Path pasta = destino.toAbsolutePath().getParent();
            if (pasta != null) {
                Files.createDirectories(pasta);
            }
            Path temp = Files.createTempFile(pasta, destino.getFileName().toString(), ".tmp");
            try {
                Files.writeString(temp, conteudo.toString(), StandardCharsets.UTF_8);
                ArquivoAtomicoUtil.substituirAtomico(temp, destino);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new ArquivoLegendaException("Falha ao escrever arquivo SRT: " + destino, e);
        }
    }
}
