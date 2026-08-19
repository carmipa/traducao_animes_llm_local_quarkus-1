package org.traducao.projeto.traducao.presentation.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Serve o conteúdo bruto das páginas de documentação (pasta {@code docs/} na
 * raiz do projeto) para o painel "Documentação" da SPA, que renderiza o
 * markdown no navegador (ver static/documentacao/documentacao.js). O README
 * raiz é o índice canônico no GitHub; este endpoint espelha a mesma pasta
 * docs/ dentro do próprio app, sem precisar sair dele.
 */
@RestController
@RequestMapping("/api/docs")
public class DocumentacaoController {

    private static final Logger log = LoggerFactory.getLogger(DocumentacaoController.class);

    /**
     * Nome de página aceito. O PONTO é obrigatório aqui: desde 06/08/2026 as páginas de etapa se
     * chamam {@code etapa-G.N-nome.md} ({@code etapa-1.1-analise-midia}), e o padrão anterior
     * ({@code ^[a-zA-Z0-9_\-]+$}) recusava todas elas com HTTP 400.
     *
     * <h2>O prejuízo, medido em 19/08/2026</h2>
     * As <b>14 páginas numeradas</b> — o pipeline inteiro — respondiam 400 no painel
     * "Documentação" da aplicação. Só as páginas sem ponto no nome ({@code arquitetura},
     * {@code ref-docker}) abriam. A {@code CatracaOrdemDocumentacaoTest} conferia a NUMERAÇÃO e a
     * ORDEM dos arquivos, e nenhuma guarda perguntava se a página <b>abre</b> — documento
     * corretamente nomeado e inalcançável passava por todas.
     *
     * <p>O ponto entra, mas {@code ..} continua fora: a travessia é barrada aqui e, em segunda
     * camada, pelo {@code normalize()} + {@code startsWith(pastaDocs)} abaixo. Ver
     * {@code CatracaPaginaDeDocumentacaoAbreTest}.
     */
    private static final Pattern NOME_SEGURO = Pattern.compile("^[a-zA-Z0-9_][a-zA-Z0-9_.\\-]*$");

    @GetMapping(value = "/{pagina}", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> servirMarkdown(@PathVariable String pagina) {
        // O ".." é recusado explicitamente, e não só por consequência do padrão: separador de
        // caminho já não passa no NOME_SEGURO, mas deixar a intenção escrita evita que um ajuste
        // futuro no padrão reabra a travessia sem ninguém perceber.
        if (pagina == null || pagina.contains("..") || !NOME_SEGURO.matcher(pagina).matches()) {
            return ResponseEntity.badRequest().body("Nome de página de documentação inválido.");
        }

        Path pastaDocs = Path.of("docs").toAbsolutePath().normalize();
        Path alvo = pastaDocs.resolve(pagina + ".md").normalize();

        if (!alvo.startsWith(pastaDocs)) {
            log.warn("Tentativa de path traversal bloqueada ao servir documentação: {}", pagina);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acesso negado.");
        }

        if (!Files.exists(alvo) || !Files.isRegularFile(alvo)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Página de documentação não encontrada: " + pagina);
        }

        try {
            String conteudo = Files.readString(alvo);
            return ResponseEntity.ok(conteudo);
        } catch (IOException e) {
            log.error("Erro ao ler página de documentação {}: {}", alvo, e.getMessage());
            return ResponseEntity.internalServerError().body("Erro ao carregar documentação.");
        }
    }
}
