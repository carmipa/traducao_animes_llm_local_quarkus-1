package org.traducao.projeto.traducao.infrastructure.legenda;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.domain.exceptions.ArquivoLegendaException;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: Lê legendas SubRip (.srt) para o mesmo
 * {@link DocumentoLegenda} usado pelo ASS, para que o pipeline de tradução
 * (cache, máscara de tags, validação) opere sobre SRT sem convertê-lo para ASS.
 * Numeração e timestamps ficam no {@code prefixo} do evento (a linha de tempo) e
 * no índice; só o texto é traduzido. Quebras internas viram {@code \N} (convenção
 * ASS), que o {@link EscritorLegendaSrt} devolve para quebras reais.
 *
 * <p>INVARIANTES DO DOMÍNIO: cada bloco SRT (índice + "start --> end" + texto)
 * vira um {@link EventoLegenda} {@code Dialogue} de estilo "Default"; o EOL e o
 * BOM originais são preservados no documento.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: erro de leitura do arquivo →
 * {@link ArquivoLegendaException}. Blocos malformados (índice não numérico) são
 * tolerados: o índice cai para a posição sequencial.
 */
@Component
public class LeitorLegendaSrt {

    public DocumentoLegenda ler(Path arquivo) {
        String conteudo;
        try {
            conteudo = Files.readString(arquivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ArquivoLegendaException("Falha ao ler arquivo SRT: " + arquivo, e);
        }

        boolean comBom = !conteudo.isEmpty() && conteudo.charAt(0) == '﻿';
        if (comBom) {
            conteudo = conteudo.substring(1);
        }
        String eol = conteudo.contains("\r\n") ? "\r\n" : "\n";
        String[] linhas = conteudo.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);

        List<EventoLegenda> eventos = new ArrayList<>();
        int i = 0;
        int n = linhas.length;
        while (i < n) {
            while (i < n && linhas[i].strip().isEmpty()) {
                i++;
            }
            if (i >= n) {
                break;
            }
            String linhaIndice = linhas[i];
            i++;
            if (i >= n) {
                break; // bloco truncado no fim do arquivo
            }
            String linhaTempo = linhas[i];
            i++;

            List<String> textoLinhas = new ArrayList<>();
            while (i < n && !linhas[i].strip().isEmpty()) {
                textoLinhas.add(linhas[i]);
                i++;
            }

            int indice;
            try {
                indice = Integer.parseInt(linhaIndice.strip());
            } catch (NumberFormatException e) {
                indice = eventos.size() + 1;
            }

            // prefixo = linha de tempo (o índice vem do próprio evento); texto com
            // quebras internas em \N para o LLM tratar o bloco como uma linha só.
            String texto = String.join("\\N", textoLinhas);
            eventos.add(new EventoLegenda(indice, "Dialogue", "Default", linhaTempo, texto));
        }

        return new DocumentoLegenda("", eventos, eol, comBom);
    }
}
