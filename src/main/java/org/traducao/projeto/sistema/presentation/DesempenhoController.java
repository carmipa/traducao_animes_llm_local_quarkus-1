package org.traducao.projeto.sistema.presentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * PROPÓSITO DE NEGÓCIO: serve ao painel "Desempenho" o último relatório de tempo por operação,
 * medido pelo {@code MedicaoDesempenhoDoPipelineIT} sobre o acervo real.
 *
 * <h2>Por que este endpoint só LÊ, e não executa a medição</h2>
 * Medir desempenho custa CPU e disco, e a aplicação é a mesma que traduz. Um botão "medir agora"
 * na tela disputaria máquina com um episódio em tradução, e o número sairia contaminado
 * justamente na hora em que alguém quisesse confiar nele. Pior: compilar mata tradução em
 * andamento nesta máquina — a medição roda pelo Gradle, e o Gradle recompila.
 *
 * <p>Então a divisão é: o harness MEDE e grava o artefato; esta tela MOSTRA. Quem quiser um número
 * novo roda o harness, com a máquina livre, e a tela reflete.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY: nunca escreve, nunca dispara medição.</li>
 *   <li>Relatório ausente devolve <b>404 com instrução</b>, e não um JSON vazio. "Ainda não
 *       mediram" e "mediram e deu zero" não podem chegar iguais na tela.</li>
 *   <li>Devolve também a DATA do arquivo: relatório sem data envelhece sem avisar, e um número
 *       de três semanas atrás lido como atual é pior que número nenhum.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Arquivo ilegível responde 500 com a causa no log; nunca devolve corpo parcial que o painel
 * possa renderizar como se fosse a medição.
 */
@RestController
@RequestMapping("/api/desempenho")
public class DesempenhoController {

    private static final Logger log = LoggerFactory.getLogger(DesempenhoController.class);

    /** O mesmo caminho que o harness grava. Se um mudar, o outro para de achar. */
    static final Path RELATORIO = Path.of("relatorios", "desempenho.json");

    /**
     * PROPÓSITO DE NEGÓCIO: devolve o relatório com a data de quando foi medido.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: 404 quando nunca se mediu, com o comando exato para
     * medir; 500 quando o arquivo existe e não pode ser lido.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> ultimoRelatorio() {
        Path alvo = RELATORIO.toAbsolutePath().normalize();
        if (!Files.isRegularFile(alvo)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("""
                {"medido": false, "instrucao": "Nenhuma medicao gravada ainda. Rode com a maquina \
                livre: gradlew test --tests \\"*MedicaoDesempenhoDoPipelineIT*\\" \
                -Dkronos.medicao=true"}""");
        }
        try {
            String corpo = Files.readString(alvo);
            FileTime quando = Files.getLastModifiedTime(alvo);
            // A data entra por FORA do JSON medido: o harness nao carimba hora (Date.now no meio
            // de uma medicao e ruido), e quem sabe quando o arquivo foi escrito e o sistema de
            // arquivos.
            String comData = corpo.replaceFirst("\\{",
                "{\n  \"medidoEm\": \"" + quando.toInstant() + "\",");
            return ResponseEntity.ok(comData);
        } catch (IOException e) {
            log.error("Erro ao ler o relatorio de desempenho {}: {}", alvo, e.getMessage());
            return ResponseEntity.internalServerError()
                .body("{\"erro\": \"Nao foi possivel ler o relatorio de desempenho.\"}");
        }
    }
}
