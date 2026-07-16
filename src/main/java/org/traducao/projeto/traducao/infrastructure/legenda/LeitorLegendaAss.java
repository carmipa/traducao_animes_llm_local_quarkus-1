package org.traducao.projeto.traducao.infrastructure.legenda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.domain.exceptions.ArquivoLegendaException;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Le arquivos .ass/.ssa preservando byte a byte tudo que nao for o campo Text
 * dos eventos Dialogue (estilos, timestamps, secoes de metadados). So o campo
 * Text e exposto para traducao; o resto e reconstruido identico pelo
 * {@link EscritorLegendaAss}.
 */
@Component
public class LeitorLegendaAss {

    private static final Logger log = LoggerFactory.getLogger(LeitorLegendaAss.class);

    private static final char BOM = '﻿';

    public DocumentoLegenda ler(Path arquivo) {
        String conteudo;
        try {
            conteudo = decodificar(Files.readAllBytes(arquivo), arquivo);
        } catch (IOException e) {
            throw new ArquivoLegendaException("Falha ao ler arquivo de legenda: " + arquivo, e);
        }

        boolean comBom = !conteudo.isEmpty() && conteudo.charAt(0) == BOM;
        if (comBom) {
            conteudo = conteudo.substring(1);
        }

        String quebraDeLinha = conteudo.contains("\r\n") ? "\r\n" : "\n";
        String[] linhas = conteudo.split("\r\n|\n", -1);

        int indiceEvents = -1;
        for (int i = 0; i < linhas.length; i++) {
            if (linhas[i].trim().equalsIgnoreCase("[Events]")) {
                indiceEvents = i;
                break;
            }
        }
        if (indiceEvents < 0) {
            throw new ArquivoLegendaException(
                "Arquivo nao parece ser uma legenda .ass/.ssa valida (secao [Events] nao encontrada): " + arquivo);
        }

        int indiceFormat = -1;
        for (int i = indiceEvents + 1; i < linhas.length; i++) {
            if (linhas[i].trim().startsWith("Format:")) {
                indiceFormat = i;
                break;
            }
        }
        if (indiceFormat < 0) {
            throw new ArquivoLegendaException("Secao [Events] sem linha 'Format:' em: " + arquivo);
        }

        List<String> camposFormato = Arrays.stream(
                linhas[indiceFormat].substring(linhas[indiceFormat].indexOf(':') + 1).split(","))
            .map(String::trim)
            .toList();
        int numCampos = camposFormato.size();
        int indiceEstilo = camposFormato.indexOf("Style");

        String cabecalho = String.join(quebraDeLinha, Arrays.asList(linhas).subList(0, indiceFormat + 1)) + quebraDeLinha;

        List<EventoLegenda> eventos = new ArrayList<>();
        int indiceAtual = 0;
        for (int i = indiceFormat + 1; i < linhas.length; i++) {
            if (i == linhas.length - 1 && linhas[i].isEmpty()) {
                // ultima "linha" vazia gerada pelo split por causa da quebra final do arquivo.
                continue;
            }
            eventos.add(parseLinha(linhas[i], indiceAtual++, numCampos, indiceEstilo));
        }

        return new DocumentoLegenda(cabecalho, eventos, quebraDeLinha, comBom);
    }

    /**
     * Decodifica detectando o encoding real: UTF-16 via BOM (comum em legendas
     * extraídas de BDs antigos), UTF-8 estrito e, como último recurso,
     * Windows-1252 (legados ANSI). Antes o leitor assumia UTF-8 e qualquer
     * arquivo UTF-16/ANSI morria com "Falha ao ler arquivo" genérico.
     * Fontes UTF-16 ganham o marcador BOM no texto decodificado para que o
     * documento seja gravado de volta como UTF-8 COM BOM (players detectam o
     * encoding pelo BOM).
     */
    private String decodificar(byte[] bytes, Path arquivo) {
        if (bytes.length >= 2) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;
            if ((b0 == 0xFF && b1 == 0xFE) || (b0 == 0xFE && b1 == 0xFF)) {
                log.info("Legenda {} em UTF-16 (BOM detectado); convertendo para UTF-8 com BOM.", arquivo.getFileName());
                // O charset UTF-16 escolhe o endianness pelo BOM e o consome.
                return BOM + new String(bytes, StandardCharsets.UTF_16);
            }
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
        } catch (CharacterCodingException e) {
            log.warn("Legenda {} não é UTF-8 válido; decodificando como Windows-1252 (melhor esforço).",
                arquivo.getFileName());
            return new String(bytes, Charset.forName("windows-1252"));
        }
    }

    private EventoLegenda parseLinha(String linha, int indice, int numCampos, int indiceEstilo) {
        int idxColon = linha.indexOf(':');
        if (idxColon < 0) {
            return new EventoLegenda(indice, "", "", linha, null);
        }

        String tipo = linha.substring(0, idxColon);
        if (!tipo.equals("Dialogue") && !tipo.equals("Comment")) {
            return new EventoLegenda(indice, "", "", linha, null);
        }

        String resto = linha.substring(idxColon + 1);
        if (resto.startsWith(" ")) {
            resto = resto.substring(1);
        }

        String[] partes = resto.split(",", numCampos);
        if (partes.length < numCampos) {
            return new EventoLegenda(indice, "", "", linha, null);
        }

        String estilo = indiceEstilo >= 0 ? partes[indiceEstilo].trim() : "";
        String prefixoCampos = String.join(",", Arrays.copyOf(partes, numCampos - 1));
        String texto = partes[numCampos - 1];
        String prefixo = tipo + ": " + prefixoCampos + ",";
        return new EventoLegenda(indice, tipo, estilo, prefixo, texto);
    }
}
