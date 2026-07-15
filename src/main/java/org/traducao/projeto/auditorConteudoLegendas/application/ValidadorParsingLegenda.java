package org.traducao.projeto.auditorConteudoLegendas.application;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: audita o arquivo BRUTO para expor corrupções que os
 * leitores tolerantes escondem — bloco SRT truncado, índice SRT não numérico e
 * linha Dialogue/Comment ASS malformada. Sem isso, uma linha que deveria ser
 * auditada era silenciosamente descartada e o arquivo saía "limpo".
 *
 * <p>INVARIANTES DO DOMÍNIO: é 100% leitura e nunca altera os leitores de legenda
 * compartilhados pelo pipeline; reporta apenas o que só é visível no texto cru.
 * A validação de sintaxe de tempo fica com {@code RegraTimestampInvalido}.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: arquivo ilegível gera uma anomalia crítica em
 * vez de exceção; formato desconhecido devolve lista vazia.
 */
@ApplicationScoped
public class ValidadorParsingLegenda {

    /**
     * PROPÓSITO DE NEGÓCIO: devolve as anomalias de parsing do arquivo indicado.
     * <p>INVARIANTES DO DOMÍNIO: SRT e ASS/SSA têm verificações distintas; o rótulo
     * {@code papel} apenas prefixa a descrição para o modo comparativo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: I/O falho vira anomalia crítica.
     */
    public List<AnomaliaConteudo> validar(Path arquivo, String formato, String papel) {
        String prefixo = papel == null || papel.isBlank() ? "" : "[" + papel + "] ";
        String conteudo;
        try {
            conteudo = lerBruto(arquivo);
        } catch (IOException e) {
            return List.of(new AnomaliaConteudo(AnomaliaConteudo.TipoSeveridade.CRITICAL,
                getNome(), prefixo + "Arquivo não pôde ser lido para validação de parsing: " + e.getMessage(),
                null, null, "Verificar codificação/integridade do arquivo."));
        }
        String semBom = !conteudo.isEmpty() && conteudo.charAt(0) == '﻿'
            ? conteudo.substring(1) : conteudo;
        String[] linhas = semBom.split("\r\n|\r|\n", -1);

        return switch (formato) {
            case "SRT" -> validarSrt(linhas, prefixo);
            case "ASS", "SSA" -> validarAss(linhas, prefixo);
            default -> List.of();
        };
    }

    public String getNome() {
        return "Integridade de Parsing";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: detecta blocos SRT truncados e índices não numéricos —
     * casos em que o leitor descarta a linha ou inventa um número sequencial.
     * <p>INVARIANTES DO DOMÍNIO: um bloco é [índice, "início --> fim", texto...];
     * a falta de qualquer parte é corrupção.
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; percorre até o fim do arquivo.
     */
    private List<AnomaliaConteudo> validarSrt(String[] linhas, String prefixo) {
        List<AnomaliaConteudo> anomalias = new ArrayList<>();
        java.util.Set<String> indicesVistos = new java.util.LinkedHashSet<>();
        int i = 0;
        int n = linhas.length;
        while (i < n) {
            while (i < n && linhas[i].strip().isEmpty()) {
                i++;
            }
            if (i >= n) {
                break;
            }
            String linhaIndice = linhas[i].strip();
            i++;

            if (!linhaIndice.isEmpty() && linhaIndice.chars().allMatch(Character::isDigit)) {
                if (!indicesVistos.add(linhaIndice)) {
                    anomalias.add(new AnomaliaConteudo(AnomaliaConteudo.TipoSeveridade.CRITICAL, getNome(),
                        prefixo + "Índice de bloco SRT duplicado: \"" + linhaIndice
                            + "\". Blocos com o mesmo número tornam o pareamento ambíguo.",
                        null, null, "Renumerar os blocos para que cada índice seja único."));
                }
            } else {
                anomalias.add(new AnomaliaConteudo(AnomaliaConteudo.TipoSeveridade.CRITICAL, getNome(),
                    prefixo + "Índice de bloco SRT não numérico: \"" + linhaIndice
                        + "\". O leitor usaria uma posição sequencial no lugar.",
                    null, null, "Numerar cada bloco SRT com um inteiro sequencial."));
            }

            if (i >= n) {
                anomalias.add(new AnomaliaConteudo(AnomaliaConteudo.TipoSeveridade.ERROR, getNome(),
                    prefixo + "Bloco SRT truncado no fim do arquivo (índice \"" + linhaIndice
                        + "\" sem linha de tempo). O leitor descartaria o bloco.",
                    null, null, "Completar o bloco com a linha de tempo e o texto."));
                break;
            }
            String linhaTempo = linhas[i];
            i++;

            List<String> texto = new ArrayList<>();
            while (i < n && !linhas[i].strip().isEmpty()) {
                texto.add(linhas[i]);
                i++;
            }
            if (texto.isEmpty()) {
                anomalias.add(new AnomaliaConteudo(AnomaliaConteudo.TipoSeveridade.WARNING, getNome(),
                    prefixo + "Bloco SRT sem texto (índice \"" + linhaIndice + "\", tempo \""
                        + linhaTempo.strip() + "\"). Bloco possivelmente truncado.",
                    null, null, "Adicionar o texto da fala ou remover o bloco vazio."));
            }
        }
        return anomalias;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: detecta linhas Dialogue/Comment ASS com campos de menos,
     * que o leitor transforma em evento inerte e nenhuma regra audita.
     * <p>INVARIANTES DO DOMÍNIO: o número de campos vem da linha {@code Format:} da
     * seção {@code [Events]}; a última coluna (Text) pode conter vírgulas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem seção [Events]/Format devolve alerta.
     */
    private List<AnomaliaConteudo> validarAss(String[] linhas, String prefixo) {
        List<AnomaliaConteudo> anomalias = new ArrayList<>();
        int idxEvents = -1;
        for (int i = 0; i < linhas.length; i++) {
            if (linhas[i].strip().equalsIgnoreCase("[Events]")) {
                idxEvents = i;
                break;
            }
        }
        if (idxEvents < 0) {
            return List.of(new AnomaliaConteudo(AnomaliaConteudo.TipoSeveridade.CRITICAL, getNome(),
                prefixo + "Arquivo ASS/SSA sem a seção [Events]. Nenhuma fala pode ser auditada.",
                null, null, "Restaurar a seção [Events] com Format: e as linhas Dialogue."));
        }
        int numCampos = -1;
        int idxFormat = -1;
        for (int i = idxEvents + 1; i < linhas.length; i++) {
            if (linhas[i].strip().toLowerCase().startsWith("format:")) {
                numCampos = linhas[i].substring(linhas[i].indexOf(':') + 1).split(",").length;
                idxFormat = i;
                break;
            }
        }
        if (numCampos < 2) {
            return List.of(new AnomaliaConteudo(AnomaliaConteudo.TipoSeveridade.CRITICAL, getNome(),
                prefixo + "Seção [Events] sem linha Format: válida.",
                null, null, "Restaurar a linha Format: da seção [Events]."));
        }

        for (int i = idxFormat + 1; i < linhas.length; i++) {
            String linha = linhas[i];
            String semEspaco = linha.stripLeading();
            if (!semEspaco.startsWith("Dialogue:") && !semEspaco.startsWith("Comment:")) {
                continue;
            }
            String resto = semEspaco.substring(semEspaco.indexOf(':') + 1);
            // split(",", numCampos) produz < numCampos partes quando faltam colunas.
            if (resto.split(",", numCampos).length < numCampos) {
                anomalias.add(new AnomaliaConteudo(AnomaliaConteudo.TipoSeveridade.CRITICAL, getNome(),
                    prefixo + "Linha " + semEspaco.substring(0, semEspaco.indexOf(':'))
                        + " ASS malformada (campos de menos para o Format de " + numCampos
                        + " colunas): \"" + resumir(semEspaco) + "\". O leitor a ignora silenciosamente.",
                    null, null, "Restaurar todas as colunas do Format antes do texto."));
            }
        }
        return anomalias;
    }

    private String resumir(String linha) {
        String limpa = linha.strip();
        return limpa.length() <= 80 ? limpa : limpa.substring(0, 80) + "…";
    }

    private String lerBruto(Path arquivo) throws IOException {
        byte[] bytes = Files.readAllBytes(arquivo);
        try {
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new String(bytes, StandardCharsets.ISO_8859_1);
        }
    }
}
