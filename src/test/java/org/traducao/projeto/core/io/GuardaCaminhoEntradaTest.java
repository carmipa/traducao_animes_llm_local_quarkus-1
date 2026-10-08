package org.traducao.projeto.core.io;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa o aviso anti-baseline da revisão (Achado 0, 2026-09-16). A revisão
 * sobrescreve no lugar a pasta que lê; apontá-la para um baseline de comparação apaga a referência
 * de um experimento em silêncio. O aviso torna isso visível SEM bloquear o uso legítimo.
 *
 * <p>INVARIANTES DO DOMÍNIO: só o NOME da pasta decide; alvo genérico da revisão não dispara.
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer aviso indevido no alvo normal reprova o teste.
 */
class GuardaCaminhoEntradaTest {

    private final GuardaCaminhoEntrada guarda = new GuardaCaminhoEntrada();

    /**
     * E7 da auditoria de 08/10/2026: o "Copiar como caminho" do Windows entrega o caminho entre
     * aspas, e a guarda o recusava como inválido (HTTP 400 reproduzido na 1.2). CONTROLE (A1): o
     * caminho de uma pasta que NÃO existe continua recusado mesmo entre aspas, e aspa só de um
     * lado não é par.
     */
    @Test
    void caminhoEntreAspasDoExplorerEAceito(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) {
        String pasta = dir.toAbsolutePath().toString();
        assertFalse(guarda.conferirDiretorio("Pasta", "\"" + pasta + "\"").isPresent(),
            "pasta existente entre aspas tem de passar");
        assertFalse(guarda.conferirDiretorio("Pasta", "  \"" + pasta + "\"  ").isPresent(),
            "espaco em volta das aspas tambem");
        assertTrue(guarda.conferirDiretorio("Pasta", "\"" + pasta + "\\nao-existe\"").isPresent(),
            "CONTROLE: pasta inexistente entre aspas continua recusada");
        assertTrue(guarda.conferirDiretorio("Pasta", "\"" + pasta).isPresent(),
            "CONTROLE: aspa so de um lado nao e' par e o caminho segue invalido");
    }

    /**
     * M4 da auditoria de 08/10/2026: a 1.1 promete "pasta ou arquivo de vídeo" e recusava o
     * arquivo. A porta de pasta OU arquivo aceita os dois; a porta só de pasta (CONTROLE) continua
     * recusando o arquivo.
     */
    @Test
    void portaDePastaOuArquivoAceitaArquivoEADePastaNao(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir)
            throws java.io.IOException {
        String arquivo = java.nio.file.Files.writeString(dir.resolve("ep01.mkv"), "x").toString();
        assertFalse(guarda.conferirDiretorioOuArquivo("Midia", arquivo).isPresent(), "arquivo tem de passar na 1.1");
        assertFalse(guarda.conferirDiretorioOuArquivo("Midia", dir.toString()).isPresent(), "pasta tambem");
        assertTrue(guarda.conferirDiretorio("Pasta", arquivo).isPresent(),
            "CONTROLE: a porta so de pasta continua recusando arquivo");
        assertTrue(guarda.conferirDiretorioOuArquivo("Midia", dir.resolve("nao-existe.mkv").toString()).isPresent(),
            "CONTROLE: arquivo inexistente continua recusado");
    }

    /** DEFEITO/HAZARD: revisar um baseline de comparação avisa. */
    @Test
    void avisaAoRevisarBaselineDeComparacao() {
        assertTrue(guarda.avisoRevisaoSobrescreveBaseline("animes/86/traducao_aya").isPresent(),
            "traducao_aya e baseline de comparacao");
        assertTrue(guarda.avisoRevisaoSobrescreveBaseline("animes/86/traducao_mistral").isPresent(),
            "traducao_mistral e baseline de comparacao");
        assertTrue(guarda.avisoRevisaoSobrescreveBaseline("animes/86/legenda-simplificada").isPresent(),
            "legenda-simplificada foi a cicatriz dos 17 arquivos");
    }

    /**
     * CONTRA-CASO (A1): o alvo NORMAL da revisao e uma pasta comum NAO disparam — avisar neles seria
     * o alarme falso que ensina a ignorar o aviso.
     */
    @Test
    void naoAvisaNoAlvoNormalNemEmPastaComum() {
        assertFalse(guarda.avisoRevisaoSobrescreveBaseline("animes/86/traducao_ptbr").isPresent(),
            "traducao_ptbr e o alvo NORMAL da revisao; avisar nele seria ruido");
        assertFalse(guarda.avisoRevisaoSobrescreveBaseline("animes/86/legendas_extraidas_ass").isPresent(),
            "pasta comum nao avisa");
        assertFalse(guarda.avisoRevisaoSobrescreveBaseline("").isPresent());
        assertFalse(guarda.avisoRevisaoSobrescreveBaseline(null).isPresent());
    }
}
