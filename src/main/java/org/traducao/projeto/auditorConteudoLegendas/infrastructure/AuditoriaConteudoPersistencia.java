package org.traducao.projeto.auditorConteudoLegendas.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.auditorConteudoLegendas.domain.AuditoriaConteudoRelatorioJson;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PROPÓSITO DE NEGÓCIO: grava cada relatório de auditoria como um arquivo JSON
 * imutável e único, para que execuções não sobrescrevam umas às outras.
 *
 * <p>INVARIANTES DO DOMÍNIO: o nome combina timestamp em milissegundos com um
 * contador atômico; a gravação usa {@code CREATE_NEW} para nunca substituir um
 * relatório existente; a pasta de destino é decidida pelo chamador.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: colisão de nome tenta o próximo contador;
 * esgotadas as tentativas, lança {@link IOException} sem sobrescrever nada.
 */
@ApplicationScoped
public class AuditoriaConteudoPersistencia {

    private static final DateTimeFormatter TIMESTAMP =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final AtomicLong SEQUENCIA = new AtomicLong();
    private static final int MAX_TENTATIVAS = 1000;

    private final ObjectMapper objectMapper;

    public AuditoriaConteudoPersistencia() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: persiste o relatório na pasta de destino informada com
     * nome único e imutável.
     * <p>INVARIANTES DO DOMÍNIO: cria a pasta se necessário; nunca sobrescreve um
     * arquivo já existente (CREATE_NEW).
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga {@link IOException}; após
     * {@value #MAX_TENTATIVAS} colisões, desiste com mensagem clara.
     */
    public Path salvarRelatorioJson(Path pastaDestino, AuditoriaConteudoRelatorioJson relatorio) throws IOException {
        Files.createDirectories(pastaDestino);
        byte[] conteudo = objectMapper.writeValueAsBytes(relatorio);
        for (int tentativa = 0; tentativa < MAX_TENTATIVAS; tentativa++) {
            String timestamp = TIMESTAMP.format(LocalDateTime.now());
            long sequencia = SEQUENCIA.incrementAndGet();
            Path arquivo = pastaDestino.resolve(
                "auditoria_conteudo_" + timestamp + "_" + sequencia + ".json");
            try {
                Files.write(arquivo, conteudo, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                return arquivo.toAbsolutePath();
            } catch (FileAlreadyExistsException e) {
                // Colisão improvável (mesmo ms e mesmo contador): tenta o próximo nome.
            }
        }
        throw new IOException("Nao foi possivel gerar nome unico para o relatorio de auditoria em: " + pastaDestino);
    }
}
