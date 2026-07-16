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
 * Reconstroi o arquivo .ass a partir do {@link DocumentoLegenda}, repetindo o
 * cabecalho original e as linhas nao traduziveis byte a byte, e so trocando o
 * campo Text dos eventos Dialogue pela versao traduzida.
 */
@Component
public class EscritorLegendaAss {

    private static final char BOM = '﻿';

    public void escrever(Path destino, DocumentoLegenda documento) {
        StringBuilder conteudo = new StringBuilder();
        if (documento.comBom()) {
            conteudo.append(BOM);
        }
        conteudo.append(documento.cabecalho());

        for (EventoLegenda evento : documento.eventos()) {
            conteudo.append(evento.prefixo());
            if (evento.temTexto()) {
                conteudo.append(evento.texto());
            }
            conteudo.append(documento.quebraDeLinha());
        }

        // Escreve num arquivo temporario e so substitui o destino com um move
        // atomico ao final. Isso evita que uma falha de IO no meio da escrita
        // (disco, antivirus, processo concorrente segurando o arquivo) deixe o
        // .ass de saida truncado/vazio: o pior caso passa a ser "fica com a
        // versao anterior", nunca "fica vazio e quebra o remux depois".
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
            throw new ArquivoLegendaException("Falha ao escrever arquivo de legenda: " + destino, e);
        }
    }

}
