package org.traducao.projeto.trocaTipoLegenda.infrastructure;

import org.springframework.stereotype.Component;
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.trocaTipoLegenda.domain.exceptions.TrocaTipoLegendaException;
import org.traducao.projeto.trocaTipoLegenda.domain.ports.ArmazenamentoBackupPort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * PROPÓSITO DE NEGÓCIO: implementa a preservação do original sob a raiz {@code backups/}
 * do KRONOS, em uma pasta por execução carimbada com data e hora.
 *
 * <p>É a única classe da fatia que conhece {@link DiretorioBaseKronos}. Antes o caminho
 * era resolvido dentro do construtor dos casos de uso, ou seja, filesystem decidido em
 * regra de negócio.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Uma sessão por execução: o carimbo {@code yyyyMMdd_HHmmss} separa execuções, e
 *       nenhuma sobrescreve a anterior.</li>
 *   <li>{@link #preservar} COPIA — o original nunca é movido nem alterado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falha ao criar a sessão ou ao copiar vira {@link TrocaTipoLegendaException}, para o
 * caso de uso abortar aquele arquivo ANTES de gravá-lo alterado — sem backup, não grava.
 */
@Component
public class ArmazenamentoBackupKronosAdapter implements ArmazenamentoBackupPort {

    private static final DateTimeFormatter TIMESTAMP_DIR = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Path raizBackups;

    public ArmazenamentoBackupKronosAdapter() {
        this(DiretorioBaseKronos.resolver("backups"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aponta a raiz de backups para outro lugar — usado por teste,
     * que precisa gravar sob {@code @TempDir} e jamais sob a pasta real do projeto.
     *
     * <p>INVARIANTES DO DOMÍNIO: a raiz é normalizada; toda sessão nasce dentro dela.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: raiz nula falha imediatamente.
     */
    public ArmazenamentoBackupKronosAdapter(Path raizBackups) {
        this.raizBackups = raizBackups.toAbsolutePath().normalize();
    }

    @Override
    public Path abrirSessao(String rotuloOperacao) {
        Path sessao = raizBackups
            .resolve(rotuloOperacao + "_" + TIMESTAMP_DIR.format(LocalDateTime.now()))
            .normalize();
        try {
            Files.createDirectories(sessao);
        } catch (IOException e) {
            throw new TrocaTipoLegendaException("Falha ao criar diretório de backup: " + sessao, e);
        }
        return sessao;
    }

    @Override
    public void preservar(Path sessao, Path arquivoOriginal) {
        try {
            Files.copy(arquivoOriginal, sessao.resolve(arquivoOriginal.getFileName()),
                StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new TrocaTipoLegendaException(
                "Falha ao preservar backup de " + arquivoOriginal.getFileName() + ": " + e.getMessage(), e);
        }
    }
}
