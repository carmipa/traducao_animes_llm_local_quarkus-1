package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: protege por regressão as decisões que impedem o tradutor
 * de publicar linhas ASS suspeitas ou substituir uma legenda sem autorização.
 *
 * <p>INVARIANTES DO DOMÍNIO: entradas protegidas permanecem bloqueadas, a chave
 * de liberação escolhe o destino correto e toda sobrescrita preserva um backup.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer desvio interrompe a suíte antes de
 * o comportamento inseguro alcançar arquivos reais.
 */
class ProcessarArquivoUseCaseGuardTest {

    @TempDir
    Path pastaTemporaria;

    /**
     * PROPÓSITO DE NEGÓCIO: comprova que a chave de proteção mantém resultados
     * incompletos isolados, mas permite publicar a retomada quando Paulo a libera.
     *
     * <p>INVARIANTES DO DOMÍNIO: resultado completo sempre usa o nome final;
     * resultado parcial protegido recebe o sufixo {@code .parcial}; liberação
     * explícita volta a selecionar o nome final.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer regressão na seleção dos caminhos
     * falha o teste antes de uma versão insegura chegar à execução real.
     */
    @Test
    void selecionaSaidaConformeChaveDeProtecao() {
        Path finalPtBr = Path.of("episodio_PT-BR.ass");

        assertEquals(finalPtBr,
            new ResolvedorSaidaLegenda().selecionar(finalPtBr, false, false));
        assertEquals(Path.of("episodio_PT-BR.parcial.ass"),
            new ResolvedorSaidaLegenda().selecionar(finalPtBr, true, false));
        assertEquals(finalPtBr,
            new ResolvedorSaidaLegenda().selecionar(finalPtBr, true, true));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que liberar a proteção não destrua a tradução
     * publicada sem antes guardar uma cópia recuperável e fiel.
     *
     * <p>INVARIANTES DO DOMÍNIO: o backup fica fora do caminho original, conserva
     * exatamente o conteúdo e usa uma pasta exclusiva sob a raiz indicada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de criação ou cópia propaga
     * {@link IOException} e faz o teste falhar sem alterar o arquivo de origem.
     */
    @Test
    void criaBackupExclusivoAntesDaSobrescrita() throws IOException {
        Path publicado = Files.writeString(pastaTemporaria.resolve("episodio_PT-BR.ass"), "versao anterior");
        Path raizBackup = pastaTemporaria.resolve("backups");

        Path backup = PoliticaBackupTraducao.copiarParaBackupExclusivo(publicado, raizBackup);

        assertTrue(Files.exists(backup));
        assertTrue(backup.startsWith(raizBackup));
        assertEquals("versao anterior", Files.readString(backup));
        assertEquals("versao anterior", Files.readString(publicado));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: comprova que uma retradução liberada preserva o cache anterior
     * numa cópia recuperável SEM tirar o arquivo do caminho ativo. A geração antiga deixa de
     * valer por decisão em memória de quem chama; o arquivo em disco continua sendo a rede de
     * segurança do episódio até a nova geração estar inteira.
     *
     * <p>INVARIANTES DO DOMÍNIO: a cópia fiel passa a existir no backup exclusivo E o cache
     * ativo permanece byte a byte igual. Nenhum instante da execução tem o caminho ativo vazio.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se o cache ativo sumir de novo, voltou a existir a
     * janela em que uma interrupção perde o episódio inteiro — foi o que apagou o S00E02 do
     * 08th MS Team em 2026-07-22, recuperado na mão a partir de {@code backups/traducao-cache}.
     */
    @Test
    void retraducaoLiberadaArquivaSemRetirarOCacheAtivoDoLugar() throws IOException {
        Path cache = pastaTemporaria.resolve("episodio.cache.json");
        Files.writeString(cache, "cache antigo");
        Path raizBackup = pastaTemporaria.resolve("backups-cache");

        Path backup = PoliticaBackupTraducao.arquivarCacheParaRetraducao(cache, raizBackup);

        assertTrue(Files.exists(backup));
        assertEquals("cache antigo", Files.readString(backup));
        assertTrue(Files.exists(cache),
            "o cache ativo NÃO pode ser removido: ele é a rede de segurança até a nova geração ser gravada");
        assertEquals("cache antigo", Files.readString(cache),
            "o cache ativo continua íntegro; quem ignora a geração anterior é o chamador, em memória");
    }

    @Test
    void detectaSaidaDeLlmEmLinhaAssPesadaCurta() {
        String original = "{=68}{\\pos(1192,40)\\1c&H00B2BA&\\3c&HB2BABB&"
            + "\\t(0,253,\\blur4\\bord4)\\t(253,758,\\blur0.6\\bord0)}Sa";
        String traduzido = "Saída: {=68}{\\pos(1192,40)\\1c&H00B2BA&\\3c&HB2BABB&"
            + "\\t(0,253,\\blur4\\bord4)\\t(253,758,\\blur0.6\\bord0)}";

        assertTrue(new ProtecaoLegendaAssService().respostaSuspeita(original, traduzido));
    }

    @Test
    void detectaSilabaDeEfeitoInfladaParaFrase() {
        String original = "{\\pos(1214,40)\\1c&H00B2BA&\\3c&HB2BABB&"
            + "\\t(0,30,\\blur4\\bord4)\\t(30,90,\\blur0.6\\bord0)}mi";
        String traduzido = "{\\pos(1214,40)\\1c&H00B2BA&\\3c&HB2BABB&"
            + "\\t(0,30,\\blur4\\bord4)\\t(30,90,\\blur0.6\\bord0)}[ininteligível]";

        assertTrue(new ProtecaoLegendaAssService().respostaSuspeita(original, traduzido));
    }

    @Test
    void naoBloqueiaDialogoNormalComTagSimples() {
        String original = "{\\i1}I will protect everyone.{\\i0}";
        String traduzido = "{\\i1}Eu vou proteger todos.{\\i0}";

        assertFalse(new ProtecaoLegendaAssService().respostaSuspeita(original, traduzido));
    }

    @Test
    void bloqueiaLetraSoltaComClipGiganteAntesDoLlm() {
        String linha = "{\\bord0\\shad0\\fs75\\an5\\pos(885.38,350.217)\\alpha&H80&"
            + "\\clip(m 928 368 b 928 370 929 373 929 376 931 376 934 377 937 377 "
            + "945 377 949 373 949 367 949 362 946 360 940 356 936 353 935 352 "
            + "935 349 935 346 936 345 939 345 943 345 944 346 945 351 l 948 351 "
            + "947 348 947 345 947 343 945 343 942 342 939 342 931 342 928 347 "
            + "928 351 928 356 931 358 936 361 941 365 942 366 942 369 942 373 "
            + "940 374 937 374 934 374 932 373 931 367)}S";

        assertTrue(new ProtecaoLegendaAssService().deveBloquearLinhaAntesDoLlm("Default", linha, 1));
    }

    @Test
    void bloqueiaTituloVetorialDuplicadoAntesDoLlm() {
        String linha = "{\\an2\\iclip(m 1411 799 l 1408 799 b 1407 803 1407 804 "
            + "1405 804 l 1400 804 b 1400 803 1399 802 l 1399 793 1402 793 "
            + "1404 793 1404 796 l 1407 796 1407 787 1404 787 b 1404 790 "
            + "1402 791 1399 791 1399 782 1401 781 1404 781 1406 781 1407 785 "
            + "1410 785 1408 779 1391 779 1391 780 1394 780 1394 783 1394 801 "
            + "1394 803 1393 804 1391 804 l 1391 806 1409 806 c)\\pos(1475.5,819.5)"
            + "\\blur0.5\\t(3899,0,1,\\alpha&HFF&)}Einherjar";

        assertTrue(new ProtecaoLegendaAssService().deveBloquearLinhaAntesDoLlm("Ep Title", linha, 2));
    }

    @Test
    void naoBloqueiaTituloSimplesComPosETransformacao() {
        String linha = "{\\pos(1565.5,822.5)\\c&H000000&\\blur0.7\\t(4188,0,1,\\1a&HFF&)}Prologue";

        assertFalse(new ProtecaoLegendaAssService().deveBloquearLinhaAntesDoLlm("Ep Title", linha, 1));
    }

    @Test
    void detectaCaminhoDeLegendaJaTraduzida() {
        assertTrue(new ProtecaoLegendaAssService().caminhoPareceTraduzido(
            Path.of("C:\\animes\\DanMachi\\legendas_ptbr\\episodio_PT-BR.ass")));
        assertTrue(new ProtecaoLegendaAssService().caminhoPareceTraduzido(
            Path.of("C:\\animes\\DanMachi\\traducao_ptbr\\episodio.ass")));
        assertFalse(new ProtecaoLegendaAssService().caminhoPareceTraduzido(
            Path.of("C:\\animes\\DanMachi\\legendas_eng\\episodio_ENG.ass")));
    }
}
