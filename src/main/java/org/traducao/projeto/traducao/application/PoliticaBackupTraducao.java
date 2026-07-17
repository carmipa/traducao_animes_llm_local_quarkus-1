package org.traducao.projeto.traducao.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.cachetraducao.infrastructure.CacheTraducaoService;
import org.traducao.projeto.legenda.domain.ArquivoLegendaException;
import org.traducao.projeto.traducao.presentation.ui.ConsoleUILogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: política de backup e arquivamento da Tradução Local — preserva
 * cada geração de legenda e de cache antes de qualquer substituição autorizada e
 * centraliza a gravação do cache com backup obrigatório, isolando essa responsabilidade
 * da orquestração de {@link ProcessarArquivoUseCase} (FASE F, R3).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Toda substituição autorizada (retradução ou sobrescrita) copia a versão anterior
 *       para uma pasta EXCLUSIVA em {@code backups/} — com atributos preservados — ANTES
 *       de alterar o original; nenhum backup anterior é sobrescrito.</li>
 *   <li>O arquivamento de cache para retradução só remove o cache atual depois que a
 *       cópia fiel já existe no backup.</li>
 *   <li>A gravação do cache com {@code preservarAnterior} exige backup bem-sucedido antes
 *       de persistir a nova geração.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falha de I/O no backup vira {@link ArquivoLegendaException} e ABORTA a operação — a
 * versão anterior permanece recuperável e o fluxo não prossegue como se o backup tivesse
 * ocorrido. As primitivas estáticas propagam {@link IOException} para o chamador converter.
 */
@Component
public class PoliticaBackupTraducao {

    private static final Logger log = LoggerFactory.getLogger(PoliticaBackupTraducao.class);

    private final CacheTraducaoService cacheService;
    private final ConsoleUILogger uiLogger;

    public PoliticaBackupTraducao(CacheTraducaoService cacheService, ConsoleUILogger uiLogger) {
        this.cacheService = cacheService;
        this.uiLogger = uiLogger;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: inicia uma retradução integral do episódio preservando o
     * cache anterior — o operador liberou explicitamente refazer a obra do zero.
     *
     * <p>INVARIANTES DO DOMÍNIO: o cache atual só é removido depois que uma cópia fiel
     * existe no backup exclusivo {@code backups/traducao-cache}; caches de outras obras
     * nunca são tocados.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: falha ao copiar ou remover vira
     * {@link ArquivoLegendaException} e aborta o processamento, sem fingir que o cache
     * foi reiniciado.
     */
    public void arquivarCacheAntesDaRetraducao(Path arquivoCache) {
        Path raizBackup = Path.of("backups", "traducao-cache").toAbsolutePath().normalize();
        try {
            Path backupCache = arquivarCacheParaRetraducao(arquivoCache, raizBackup);
            log.warn("Retradução liberada: cache anterior removido do uso e preservado em {}", backupCache);
            uiLogger.log("[ CACHE REINICIADO ] Geração anterior preservada em: " + backupCache);
        } catch (IOException e) {
            throw new ArquivoLegendaException(
                "Falha ao preservar e reiniciar o cache antes da retradução: " + arquivoCache, e);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: preserva a versão PT-BR atualmente publicada antes de uma
     * substituição autorizada, permitindo recuperação após uma revisão ruim.
     *
     * <p>INVARIANTES DO DOMÍNIO: cada substituição recebe uma pasta exclusiva em
     * {@code backups/traducao}; o arquivo original é copiado com seus atributos e nunca é
     * alterado antes de o backup terminar com sucesso.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link ArquivoLegendaException} e impede a
     * escrita da nova saída final, mantendo intacta a versão anterior.
     */
    public Path criarBackupAntesSobrescrita(Path arquivoFinal) {
        Path raizBackup = Path.of("backups", "traducao").toAbsolutePath().normalize();
        try {
            Path backup = copiarParaBackupExclusivo(arquivoFinal, raizBackup);
            log.info("Backup da tradução final criado em {}", backup);
            uiLogger.log("[ BACKUP ] Tradução anterior preservada em: " + backup);
            return backup;
        } catch (IOException e) {
            throw new ArquivoLegendaException(
                "Falha ao criar backup obrigatório antes de sobrescrever: " + arquivoFinal, e);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: promove a nova geração validada do cache da obra selecionada
     * sem perder a versão que sustentava a legenda anterior.
     *
     * <p>INVARIANTES DO DOMÍNIO: a liberação explícita exige backup do cache existente
     * antes da substituição; sem liberação permanece a gravação atômica normal; caches de
     * outros episódios ou obras não são acessados.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: falha no backup ou na gravação lança
     * {@link ArquivoLegendaException}; o destino anterior permanece recuperável e a
     * legenda final não prossegue como se o cache tivesse sido atualizado.
     */
    public void salvarCacheDaExecucao(
            Path arquivoCache,
            ProvenienciaCache proveniencia,
            List<EntradaCache> entradas,
            boolean preservarAnterior) {
        if (preservarAnterior && Files.exists(arquivoCache)) {
            Path raizBackup = Path.of("backups", "traducao-cache").toAbsolutePath().normalize();
            try {
                Path backup = copiarParaBackupExclusivo(arquivoCache, raizBackup);
                log.info("Backup do cache anterior criado em {}", backup);
                uiLogger.log("[ BACKUP CACHE ] Geração anterior preservada em: " + backup);
            } catch (IOException e) {
                throw new ArquivoLegendaException(
                    "Falha ao criar backup obrigatório antes de atualizar o cache: " + arquivoCache, e);
            }
        }
        cacheService.salvar(arquivoCache, proveniencia, entradas);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: copia uma tradução (ou cache) publicada para uma pasta
     * exclusiva de histórico antes que uma nova versão ocupe o mesmo caminho final.
     *
     * <p>INVARIANTES DO DOMÍNIO: cria um diretório novo por operação, preserva os
     * atributos do arquivo e nunca substitui um backup anterior.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga {@link IOException}; o chamador converte
     * a falha em bloqueio da operação.
     */
    public static Path copiarParaBackupExclusivo(Path arquivoFinal, Path raizBackup) throws IOException {
        Files.createDirectories(raizBackup);
        Path pastaBackup = Files.createTempDirectory(raizBackup, "sobrescrita_");
        Path backup = pastaBackup.resolve(arquivoFinal.getFileName()).normalize();
        return Files.copy(arquivoFinal, backup, StandardCopyOption.COPY_ATTRIBUTES);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: retira o cache atual de uso e o preserva, para uma retradução
     * integral sem heurística sobre se o cache antigo ainda é confiável.
     *
     * <p>INVARIANTES DO DOMÍNIO: só remove o cache depois que a cópia fiel existe no
     * backup; opera exclusivamente sobre o arquivo de cache informado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga {@link IOException}; se a cópia ou a
     * remoção falhar, nenhuma geração nova começa fingindo cache reiniciado.
     */
    public static Path arquivarCacheParaRetraducao(Path arquivoCache, Path raizBackup) throws IOException {
        Path backup = copiarParaBackupExclusivo(arquivoCache, raizBackup);
        Files.delete(arquivoCache);
        return backup;
    }
}
