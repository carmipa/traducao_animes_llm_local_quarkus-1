package org.traducao.projeto.core.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o navegador de pastas exposto por HTTP não
 * consegue listar nada fora das raízes permitidas — e que "servidor cego" e
 * "pasta vazia" nunca produzem a mesma resposta.
 *
 * <h2>Por que estes testes são o coração da mudança</h2>
 * O diálogo nativo que este navegador substitui só podia ser aberto por quem
 * estava sentado na máquina. O endpoint atende qualquer requisição que chegue.
 * Trocar um pelo outro sem limite de raiz transformaria a escolha de pasta num
 * listador de sistema de arquivos.
 */
class GuardaRaizNavegacaoTest {

    private static GuardaRaizNavegacao comRaizes(Path... raizes) {
        return new GuardaRaizNavegacao(
            new NavegacaoProperties(List.of(raizes).stream().map(Path::toString).toList()));
    }

    /** O caminho feliz: dentro da raiz, lista as subpastas em ordem estável. */
    @Test
    @DisplayName("lista as subpastas de um caminho dentro da raiz")
    void listaDentroDaRaiz(@TempDir Path raiz) throws IOException {
        Files.createDirectory(raiz.resolve("Zeta Gundam"));
        Files.createDirectory(raiz.resolve("86"));
        Files.createFile(raiz.resolve("nao-e-pasta.ass"));

        List<GuardaRaizNavegacao.Pasta> pastas = comRaizes(raiz).listar(raiz.toString());

        assertEquals(List.of("86", "Zeta Gundam"), pastas.stream().map(GuardaRaizNavegacao.Pasta::nome).toList(),
            "deve listar SO pastas, em ordem alfabetica insensivel a caixa");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o teste central. Uma pasta que existe, é legível e
     * está fora da raiz não pode ser listada.
     */
    @Test
    @DisplayName("RECUSA caminho existente que esta fora da raiz")
    void recusaForaDaRaiz(@TempDir Path raiz, @TempDir Path fora) throws IOException {
        Files.createDirectory(fora.resolve("segredos"));

        var erro = assertThrows(GuardaRaizNavegacao.NavegacaoRecusadaException.class,
            () -> comRaizes(raiz).listar(fora.toString()));

        assertEquals(GuardaRaizNavegacao.Motivo.FORA_DA_RAIZ, erro.motivo());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: escapar por {@code ..} é a tentativa mais barata que
     * existe. Ela só falha porque a comparação é feita sobre o caminho REAL, não
     * sobre o texto — em texto, "raiz/../fora" começa com "raiz".
     */
    @Test
    @DisplayName("RECUSA fuga por .. mesmo com a raiz no comeco do texto")
    void recusaFugaPorPontoPonto(@TempDir Path base) throws IOException {
        Path raiz = Files.createDirectory(base.resolve("acervo"));
        Path fora = Files.createDirectory(base.resolve("fora"));
        Files.createDirectory(fora.resolve("segredos"));

        String textoQueParecefilho = raiz + java.io.File.separator + ".." + java.io.File.separator + "fora";

        var erro = assertThrows(GuardaRaizNavegacao.NavegacaoRecusadaException.class,
            () -> comRaizes(raiz).listar(textoQueParecefilho));

        assertEquals(GuardaRaizNavegacao.Motivo.FORA_DA_RAIZ, erro.motivo());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: link simbólico apontando para fora é a fuga que passa
     * por qualquer checagem textual — o texto é inteiramente filho da raiz.
     *
     * <p>Ignorado quando o sistema não permite criar link (Windows sem modo
     * desenvolvedor). Ignorar é honesto; fingir que passou não seria.
     */
    @Test
    @DisplayName("RECUSA link simbolico que aponta para fora da raiz")
    void recusaLinkSimbolicoParaFora(@TempDir Path base) throws IOException {
        Path raiz = Files.createDirectory(base.resolve("acervo"));
        Path fora = Files.createDirectory(base.resolve("fora"));
        Files.createDirectory(fora.resolve("segredos"));

        Path atalho = raiz.resolve("atalho");
        try {
            Files.createSymbolicLink(atalho, fora);
        } catch (IOException | UnsupportedOperationException e) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false,
                "sistema nao permite criar link simbolico: " + e.getMessage());
            return;
        }

        var erro = assertThrows(GuardaRaizNavegacao.NavegacaoRecusadaException.class,
            () -> comRaizes(raiz).listar(atalho.toString()));

        assertEquals(GuardaRaizNavegacao.Motivo.FORA_DA_RAIZ, erro.motivo(),
            "o link resolve para fora da raiz e tem de ser recusado");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: sem raiz configurada o servidor está CEGO. Devolver
     * lista vazia aqui faria um sistema mal configurado parecer um acervo vazio.
     */
    @Test
    @DisplayName("sem raiz configurada RECUSA com motivo proprio, nao devolve lista vazia")
    void semRaizConfiguradaRecusa(@TempDir Path qualquer) {
        var guarda = new GuardaRaizNavegacao(new NavegacaoProperties(List.of()));

        var erro = assertThrows(GuardaRaizNavegacao.NavegacaoRecusadaException.class,
            () -> guarda.listar(qualquer.toString()));

        assertEquals(GuardaRaizNavegacao.Motivo.SEM_RAIZ_CONFIGURADA, erro.motivo());
    }

    /** Chave ausente no YAML chega como nula e tem de recusar igual. */
    @Test
    @DisplayName("raizes nulas recusam tudo, como lista vazia")
    void raizesNulasRecusam(@TempDir Path qualquer) {
        var guarda = new GuardaRaizNavegacao(new NavegacaoProperties(null));

        assertEquals(GuardaRaizNavegacao.Motivo.SEM_RAIZ_CONFIGURADA,
            assertThrows(GuardaRaizNavegacao.NavegacaoRecusadaException.class,
                () -> guarda.listar(qualquer.toString())).motivo());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CONTRA-TESTE do anterior. Pasta legitimamente sem
     * subpastas devolve lista vazia com SUCESSO. Sem este par, "recusar tudo"
     * poderia ser confundido com "está vazio" — que é exatamente o defeito que a
     * separação de motivos existe para impedir.
     */
    @Test
    @DisplayName("pasta sem subpastas devolve lista VAZIA com sucesso, e nao recusa")
    void pastaVaziaNaoERecusa(@TempDir Path raiz) throws IOException {
        Path vazia = Files.createDirectory(raiz.resolve("sem-nada"));

        List<GuardaRaizNavegacao.Pasta> pastas = comRaizes(raiz).listar(vazia.toString());

        assertTrue(pastas.isEmpty(), "pasta vazia devolve lista vazia");
    }

    /** Sem caminho, a interface precisa dos pontos de partida. */
    @Test
    @DisplayName("caminho em branco devolve as raizes, nao a raiz do sistema")
    void caminhoEmBrancoDevolveRaizes(@TempDir Path raizA, @TempDir Path raizB) {
        List<GuardaRaizNavegacao.Pasta> pastas = comRaizes(raizA, raizB).listar("  ");

        assertEquals(2, pastas.size());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: sem isto, clicar em "subir um nível" na raiz sairia do
     * limite pela porta da frente, sem precisar de nenhuma fuga engenhosa.
     */
    @Test
    @DisplayName("o pai de uma RAIZ e nulo — subir nao escapa do limite")
    void paiDaRaizENulo(@TempDir Path raiz) throws IOException {
        Path filha = Files.createDirectory(raiz.resolve("dentro"));
        var guarda = comRaizes(raiz);

        assertNull(guarda.paiPermitido(raiz.toString()), "a raiz nao tem pai navegavel");
        assertEquals(raiz.toRealPath().toString(), guarda.paiPermitido(filha.toString()),
            "de dentro da raiz, subir volta para a raiz");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: é o que permite UMA configuração servir Windows
     * (C:/animes) e contêiner (/acervo) sem arquivo separado.
     */
    @Test
    @DisplayName("raiz declarada que nao existe e ignorada, sem derrubar as demais")
    void raizInexistenteEIgnorada(@TempDir Path existente) {
        var guarda = new GuardaRaizNavegacao(new NavegacaoProperties(
            List.of(existente.toString(), "/caminho-que-nao-existe-em-lugar-nenhum", "Z:/tambem-nao")));

        assertEquals(1, guarda.raizesExistentes().size(),
            "so a raiz existente entra, e a presenca das outras nao quebra nada");
    }

    /** Pasta inexistente é 404, não "fora da raiz" — motivos não podem se confundir. */
    @Test
    @DisplayName("pasta inexistente tem motivo proprio")
    void pastaInexistenteTemMotivoProprio(@TempDir Path raiz) {
        var erro = assertThrows(GuardaRaizNavegacao.NavegacaoRecusadaException.class,
            () -> comRaizes(raiz).listar(raiz.resolve("nunca-existiu").toString()));

        assertEquals(GuardaRaizNavegacao.Motivo.NAO_ENCONTRADO, erro.motivo());
    }
}
