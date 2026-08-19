package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.traducao.projeto.traducao.presentation.web.DocumentacaoController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: toda página que existe em {@code docs/} tem de ABRIR no painel
 * "Documentação" da aplicação. Documento correto no disco e inalcançável na tela é documentação
 * que não existe para quem opera — e é pior que documento ausente, porque o índice promete.
 *
 * <h2>O prejuízo que originou — medido em 19/08/2026</h2>
 * O padrão de nome do {@link DocumentacaoController} era {@code ^[a-zA-Z0-9_\-]+$}, sem o PONTO.
 * Em 06/08/2026 as páginas de etapa foram renomeadas para {@code etapa-G.N-nome.md}, e a partir
 * dali <b>as 14 páginas numeradas — o pipeline inteiro — respondiam HTTP 400</b>. Só abriam as
 * páginas sem ponto no nome ({@code arquitetura}, {@code catracas-e-fronteiras}, {@code ref-*}).
 *
 * <pre>
 *   400  etapa-1.1-analise-midia          &lt;- todas as 14
 *   200  arquitetura                       &lt;- as sem ponto seguiam abrindo
 * </pre>
 *
 * <p>Ninguém percebeu por treze dias porque a {@code CatracaOrdemDocumentacaoTest} confere a
 * NUMERAÇÃO, o NOME e a ORDEM — e nenhuma guarda perguntava se a página <b>carrega</b>. É a
 * cicatriz do projeto aplicada à própria documentação: <i>nome certo não prova página viva</i>,
 * do mesmo jeito que HTTP 200 não prova tela funcionando.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Todo {@code docs/*.md} responde {@code 200} pelo endpoint que a SPA usa.</li>
 *   <li>O caminho continua fechado para travessia: {@code ..}, separador e nome vazio são
 *       recusados — o ponto entrou para servir {@code 1.1}, não para abrir a árvore.</li>
 *   <li>A guarda falha com a LISTA dos arquivos que não abrem, não com um booleano.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprova o build nomeando cada página inalcançável e o status devolvido. Se a pasta
 * {@code docs/} não existir ou estiver vazia, a guarda também REPROVA em vez de passar: alvo
 * vazio não é aprovação.
 */
class CatracaPaginaDeDocumentacaoAbreTest {

    private static final Path DOCS = Path.of("docs");

    private final DocumentacaoController controller = new DocumentacaoController();

    @Test
    @DisplayName("toda pagina de docs/ abre pelo endpoint que a SPA usa")
    void todaPaginaAbre() throws IOException {
        List<String> paginas = paginas();

        assertTrue(paginas.size() >= 10,
            "docs/ tem " + paginas.size() + " pagina(s) — abaixo do esperado. Alvo vazio ou quase "
                + "vazio NAO e aprovacao: a guarda estaria cega.");

        List<String> quebradas = new ArrayList<>();
        for (String pagina : paginas) {
            ResponseEntity<String> resposta = controller.servirMarkdown(pagina);
            if (resposta.getStatusCode() != HttpStatus.OK
                || resposta.getBody() == null
                || resposta.getBody().isBlank()) {
                quebradas.add("  " + pagina + "  -> HTTP " + resposta.getStatusCode().value());
            }
        }

        assertTrue(quebradas.isEmpty(), () -> """
            PAGINA DE DOCUMENTACAO QUE NAO ABRE.

            O arquivo existe em docs/ e o endpoint recusa. Foi assim que as 14 paginas de etapa
            ficaram treze dias devolvendo 400 no painel da aplicacao, depois da renomeacao para
            etapa-G.N-nome.md: o padrao de nome do controller nao aceitava o PONTO.

            Paginas que nao abrem:
            """ + String.join(System.lineSeparator(), quebradas));
    }

    @Test
    @DisplayName("o ponto entrou, a travessia continua fora")
    void travessiaContinuaBloqueada() {
        List<String> perigosas = List.of(
            "..", "../application", "..%2fapplication", "docs/../build",
            "etapa-1.1-analise-midia/../../build", ".gitignore", "", " ");

        List<String> aceitas = new ArrayList<>();
        for (String tentativa : perigosas) {
            ResponseEntity<String> resposta = controller.servirMarkdown(tentativa);
            if (resposta.getStatusCode() == HttpStatus.OK) {
                aceitas.add("  \"" + tentativa + "\"");
            }
        }

        assertTrue(aceitas.isEmpty(), () -> """
            O ENDPOINT DE DOCUMENTACAO ACEITOU CAMINHO QUE DEVERIA RECUSAR.

            O ponto foi liberado para servir "etapa-1.1-...", nao para abrir a arvore do projeto.

            Aceitas indevidamente:
            """ + String.join(System.lineSeparator(), aceitas));
    }

    @Test
    @DisplayName("pagina inexistente responde 404, nao 200 vazio")
    void inexistenteResponde404() {
        ResponseEntity<String> resposta = controller.servirMarkdown("pagina-que-nunca-existiu-1.9");

        assertEquals(HttpStatus.NOT_FOUND, resposta.getStatusCode(),
            "pagina inexistente tem de responder 404 — 200 com corpo vazio faria a tela mostrar "
                + "documento em branco como se fosse conteudo");
        assertFalse(resposta.getStatusCode() == HttpStatus.OK, "nunca 200 para inexistente");
    }

    /** Os nomes de página (sem a extensão) que existem em {@code docs/}. */
    private static List<String> paginas() throws IOException {
        try (Stream<Path> arquivos = Files.list(DOCS)) {
            return arquivos
                .filter(Files::isRegularFile)
                .map(p -> p.getFileName().toString())
                .filter(n -> n.endsWith(".md"))
                .map(n -> n.substring(0, n.length() - 3))
                .sorted()
                .toList();
        }
    }
}
