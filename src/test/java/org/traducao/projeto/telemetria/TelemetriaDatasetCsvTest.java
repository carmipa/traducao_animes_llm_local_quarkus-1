package org.traducao.projeto.telemetria;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: garante que o CSV publicado no dataset público seja legível por pandas,
 * R e planilha sem corromper o dado — o formato existe para ser consumido por terceiros, e um
 * escape errado só aparece na máquina de quem baixou.
 *
 * <p>INVARIANTES DO DOMÍNIO: separador, aspas e quebra de linha seguem RFC 4180; nome de obra com
 * vírgula e fala de legenda com aspas sobrevivem; uma linha física é sempre um registro.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: escape que deixe de citar campo com vírgula reprova mostrando
 * a linha gerada — é o defeito que parte a tabela em coluna a mais sem ninguém notar.
 */
class TelemetriaDatasetCsvTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("campo simples NAO leva aspas — citar tudo incha o arquivo sem ganho")
    void campoSimplesNaoEhCitado() {
        assertEquals("CONCLUIDO", TelemetriaDatasetCsv.escapar("CONCLUIDO"));
        assertEquals("", TelemetriaDatasetCsv.escapar(null));
    }

    /**
     * O caso real: {@code "[Joseki] Mobile Suit Gundam The 08th MS Team COMPLETE (1996)(BD AV1
     * 1080p Opus)[Sub Eng]"} não tem vírgula, mas grupo de release com vírgula existe no acervo e
     * partiria a linha em duas colunas.
     */
    @Test
    @DisplayName("virgula no nome da obra obriga aspas, senao a linha ganha uma coluna fantasma")
    void virgulaEhCitada() {
        assertEquals("\"Gundam, Zeta\"", TelemetriaDatasetCsv.escapar("Gundam, Zeta"));
    }

    @Test
    @DisplayName("aspas da fala sao DUPLICADAS dentro do campo citado")
    void aspasSaoDuplicadas() {
        assertEquals("\"ele disse \"\"Kamille\"\", e saiu\"",
            TelemetriaDatasetCsv.escapar("ele disse \"Kamille\", e saiu"));
    }

    @Test
    @DisplayName("quebra de linha real vira o texto \\n — uma linha fisica, um registro")
    void quebraDeLinhaNaoParteORegistro() {
        String saida = TelemetriaDatasetCsv.escapar("primeira\nsegunda");
        assertFalse(saida.contains("\n"), "sobrou quebra real na celula: [" + saida + "]");
        assertEquals("primeira\\nsegunda", saida);
        assertEquals("a\\nb", TelemetriaDatasetCsv.escapar("a\r\nb"));
    }

    @Test
    @DisplayName("a fala de legenda inteira sobrevive: virgula, aspas e \\N juntos")
    void falaDeLegendaSobrevive() {
        String fala = "Fala mantida sem tradução (tags corrompidas): "
            + "{\\i0}All communications from here\\Non are to be \"pointcast\" only!{\\i}";
        String celula = TelemetriaDatasetCsv.escapar(fala);

        assertTrue(celula.startsWith("\"") && celula.endsWith("\""), "deveria estar citada: " + celula);
        assertTrue(celula.contains("\\N"), "o \\N do ASS e' dado, nao pode sumir");
        assertTrue(celula.contains("\"\"pointcast\"\""), "aspas internas precisam vir duplicadas");
        // desfaz o escape como um leitor de CSV faria
        String lido = celula.substring(1, celula.length() - 1).replace("\"\"", "\"");
        assertEquals(fala, lido, "round-trip perdeu conteudo");
    }

    @Test
    @DisplayName("array nulo devolve SO o cabecalho — arquivo vazio nao distingue 'nada' de 'falhei'")
    void arrayNuloDevolveCabecalho() {
        String csv = TelemetriaDatasetCsv.deArray(null, List.of("a", "b"));
        assertEquals("a,b\n", csv);
    }

    @Test
    @DisplayName("coluna ausente no objeto vira celula VAZIA, nao o texto 'null' nem coluna pulada")
    void colunaAusenteViraCelulaVazia() {
        ObjectNode no = mapper.createObjectNode();
        no.put("tipo", "TRADUCAO");
        // 'tempoTotalMs' ausente de proposito
        String csv = TelemetriaDatasetCsv.deObjeto(no, List.of("tipo", "tempoTotalMs", "registradoEm"));

        String[] linhas = csv.split("\n");
        assertEquals("tipo,tempoTotalMs,registradoEm", linhas[0]);
        assertEquals("TRADUCAO,,", linhas[1]);
        assertFalse(csv.contains("null"), "publicou o literal 'null': " + csv);
    }

    @Test
    @DisplayName("array dentro da celula e' achatado com ' | ' — celula de CSV nao aninha")
    void arrayInternoEhAchatado() {
        ObjectNode no = mapper.createObjectNode();
        ArrayNode gpus = no.putArray("gpusDetectadas");
        gpus.add("RTX 4060");
        gpus.add("Intel UHD");
        String csv = TelemetriaDatasetCsv.deObjeto(no, List.of("gpusDetectadas"));
        assertEquals("gpusDetectadas\nRTX 4060 | Intel UHD\n", csv);
    }

    @Test
    @DisplayName("uma execucao com N avisos vira N linhas — tidy data, nao celula gigante")
    void avisosViramUmaLinhaCadaUm() {
        List<List<String>> linhas = List.of(
            List.of("2026-08-10T00:00:00Z", "ep01.ass", "Zeta", "1", "aviso um"),
            List.of("2026-08-10T00:00:00Z", "ep01.ass", "Zeta", "2", "aviso, dois"),
            List.of("2026-08-10T00:00:00Z", "ep01.ass", "Zeta", "3", "aviso \"tres\""));

        String csv = TelemetriaDatasetCsv.deLinhas(
            List.of("registradoEm", "nomeEpisodio", "animeNome", "ordem", "aviso"), linhas);

        String[] out = csv.split("\n");
        assertEquals(4, out.length, "cabecalho + 3 avisos");
        assertTrue(out[2].endsWith("\"aviso, dois\""), "aviso com virgula precisa de aspas: " + out[2]);
        assertTrue(out[3].endsWith("\"aviso \"\"tres\"\"\""), "aviso com aspas: " + out[3]);
    }

    @Test
    @DisplayName("o numero de colunas e' o MESMO em todas as linhas, com virgula no dado")
    void larguraDaTabelaEhEstavel() {
        ArrayNode array = mapper.createArrayNode();
        array.addObject().put("anime", "Gundam, Zeta").put("episodio", "ep01.ass");
        array.addObject().put("anime", "86").put("episodio", "ep02.ass");

        String csv = TelemetriaDatasetCsv.deArray(array, List.of("anime", "episodio"));
        for (String linha : csv.split("\n")) {
            assertEquals(2, contarCampos(linha), "largura mudou na linha: [" + linha + "]");
        }
    }

    /** Conta campos respeitando aspas — é assim que um leitor de CSV honesto separa. */
    private static int contarCampos(String linha) {
        int campos = 1;
        boolean dentroDeAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (c == '"') {
                dentroDeAspas = !dentroDeAspas;
            } else if (c == ',' && !dentroDeAspas) {
                campos++;
            }
        }
        return campos;
    }
}
