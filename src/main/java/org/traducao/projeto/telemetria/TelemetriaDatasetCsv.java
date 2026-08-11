package org.traducao.projeto.telemetria;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: escreve as mesmas métricas já publicadas em JSON também em CSV, para que
 * quem consome o dataset possa abrir direto em pandas, R ou planilha sem escrever um parser de
 * JSON aninhado. O JSON continua sendo a foto legível por humanos no GitHub; o CSV é a porta de
 * entrada de quem vai analisar.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li><b>O CSV nasce do MESMO {@link JsonNode} que vai para o arquivo JSON.</b> Não existe
 *       segundo caminho de montagem: o que for sanitizado (ou deliberadamente publicado) num
 *       formato está idêntico no outro, por construção. Duas montagens independentes divergiriam
 *       na primeira mudança de schema, e a divergência apareceria justamente no formato que
 *       ninguém revisa.</li>
 *   <li><b>RFC 4180.</b> Vírgula como separador, aspas duplas para delimitar, aspas internas
 *       duplicadas. Campo que contém vírgula, aspas, {@code \n} ou {@code \r} é sempre citado —
 *       nome de anime com grupo de release traz vírgula, e fala de legenda traz aspas.</li>
 *   <li><b>Quebra de linha real nunca escapa para dentro do arquivo.</b> {@code \r} e {@code \n}
 *       literais viram o texto {@code \n} de dois caracteres. Um CSV com quebra real dentro de
 *       campo é válido pela norma, mas quebra `wc -l`, `split` e leitura em streaming — e o que
 *       se ganha em pureza se perde em todo consumidor simples.</li>
 *   <li>Cabeçalho sempre presente, mesmo em tabela sem linha: arquivo com cabeçalho e zero linhas
 *       é "medi e não houve"; arquivo vazio é indistinguível de "falhei ao gerar".</li>
 *   <li>Terminador de linha {@code \n} (LF), não CRLF: o arquivo vive num repositório Git e o
 *       diff de cada publicação precisa mostrar só as linhas novas.</li>
 * </ul>
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: campo ausente ou nulo vira string vazia — nunca o literal
 * {@code "null"}, que um leitor de CSV interpretaria como texto. Nó nulo devolve apenas o
 * cabeçalho. Nunca lança.
 */
public final class TelemetriaDatasetCsv {

    /** Separador de campo. Fixo: mudar por região transforma o dataset público em dialeto. */
    private static final char SEPARADOR = ',';
    private static final String FIM_DE_LINHA = "\n";

    private TelemetriaDatasetCsv() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte um array de objetos JSON — {@code traducoesLlm},
     * {@code operacoes} — na tabela CSV correspondente.
     *
     * <p>INVARIANTES DO DOMÍNIO: a ordem das colunas é a recebida, nunca a ordem de iteração do
     * JSON, para o cabeçalho ser estável entre publicações e o diff do commit mostrar dado novo em
     * vez de coluna remexida. Objeto sem um campo produz célula vazia, não pula coluna.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code array} nulo, vazio ou que não seja array devolve
     * só a linha de cabeçalho.
     */
    public static String deArray(JsonNode array, List<String> colunas) {
        StringBuilder csv = new StringBuilder();
        csv.append(linha(colunas));
        if (array == null || !array.isArray()) {
            return csv.toString();
        }
        for (JsonNode item : array) {
            csv.append(linha(valores(item, colunas)));
        }
        return csv.toString();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte um objeto JSON único — {@code resumo},
     * {@code ambienteExecucao} — numa tabela de uma linha só, para que também ele seja legível por
     * ferramenta de planilha sem tratamento especial.
     *
     * <p>INVARIANTES DO DOMÍNIO: sempre uma linha de dados, mesmo com todos os campos ausentes;
     * campo que seja array (ex.: {@code gpusDetectadas}) é achatado com {@code " | "}, porque
     * célula de CSV não aninha.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nó nulo devolve cabeçalho + uma linha de células vazias —
     * "publiquei e estava vazio" continua distinguível de "não publiquei".
     */
    public static String deObjeto(JsonNode objeto, List<String> colunas) {
        return linha(colunas) + linha(valores(objeto, colunas));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: monta uma tabela a partir de linhas já resolvidas em texto, para os
     * casos em que a linha do CSV não corresponde a um objeto do JSON — o caso real é o CSV de
     * avisos, em que UMA execução vira N linhas, uma por aviso.
     *
     * <p>INVARIANTES DO DOMÍNIO: cada linha recebida é escrita na ordem dada; nenhuma validação de
     * largura é feita aqui, porque quem monta as linhas é quem conhece as colunas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista nula de linhas devolve só o cabeçalho.
     */
    public static String deLinhas(List<String> colunas, List<List<String>> linhas) {
        StringBuilder csv = new StringBuilder();
        csv.append(linha(colunas));
        if (linhas != null) {
            for (List<String> l : linhas) {
                csv.append(linha(l));
            }
        }
        return csv.toString();
    }

    private static List<String> valores(JsonNode objeto, List<String> colunas) {
        List<String> valores = new ArrayList<>(colunas.size());
        for (String coluna : colunas) {
            valores.add(objeto == null ? "" : texto(objeto.get(coluna)));
        }
        return valores;
    }

    /**
     * Converte um valor JSON em célula. Array vira lista achatada; nulo e ausente viram vazio —
     * publicar o literal "null" faria o consumidor tratar ausência como o texto "null".
     */
    private static String texto(JsonNode valor) {
        if (valor == null || valor.isNull()) {
            return "";
        }
        if (valor.isArray()) {
            List<String> partes = new ArrayList<>();
            valor.forEach(item -> partes.add(texto(item)));
            return String.join(" | ", partes);
        }
        return valor.asText();
    }

    private static String linha(List<String> celulas) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < celulas.size(); i++) {
            if (i > 0) {
                sb.append(SEPARADOR);
            }
            sb.append(escapar(celulas.get(i)));
        }
        return sb.append(FIM_DE_LINHA).toString();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: transforma um valor em célula segura de CSV.
     *
     * <p>INVARIANTES DO DOMÍNIO: cita quando há separador, aspas ou quebra; duplica a aspa interna;
     * troca quebra real pelo texto {@code \n} para a linha física do arquivo seguir sendo uma linha
     * lógica. Espaço em branco nas pontas é preservado — em fala de legenda ele é dado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nulo vira string vazia.
     */
    static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        String plano = valor.replace("\r\n", "\\n").replace("\r", "\\n").replace("\n", "\\n");
        boolean precisaAspas = plano.indexOf(SEPARADOR) >= 0 || plano.indexOf('"') >= 0;
        if (!precisaAspas) {
            return plano;
        }
        return '"' + plano.replace("\"", "\"\"") + '"';
    }
}
