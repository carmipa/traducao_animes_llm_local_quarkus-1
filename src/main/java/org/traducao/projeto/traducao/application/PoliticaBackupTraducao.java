package org.traducao.projeto.traducao.application;

import org.traducao.projeto.core.io.DiretorioBaseKronos;

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
 *   <li>O arquivamento de cache para retradução NUNCA remove o cache do caminho ativo:
 *       apenas copia. Retirar o arquivo antes de a nova geração existir abriria uma
 *       janela de perda total do episódio se a execução fosse interrompida.</li>
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

    /**
     * PROPÓSITO DE NEGÓCIO: injeta o serviço de cache (para a gravação com backup) e o logger da
     * UI (para sinalizar ao operador cada preservação de geração anterior).
     *
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas; não as substitui nem cria
     * implementação própria.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não valida os argumentos; a injeção CDI garante os beans.
     *
     * @param cacheService serviço proprietário da persistência do cache
     * @param uiLogger canal de mensagens operacionais acompanhado pelo operador
     */
    public PoliticaBackupTraducao(CacheTraducaoService cacheService, ConsoleUILogger uiLogger) {
        this.cacheService = cacheService;
        this.uiLogger = uiLogger;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: inicia uma retradução integral do episódio preservando o
     * cache anterior — o operador liberou explicitamente refazer a obra do zero.
     *
     * <p>INVARIANTES DO DOMÍNIO: uma cópia fiel passa a existir no backup exclusivo
     * {@code backups/traducao-cache} ANTES de a tradução começar, e o cache ativo NÃO é
     * removido — a geração anterior deixa de valer apenas em memória, para o episódio
     * sobreviver a uma interrupção no meio da retradução. Caches de outras obras nunca são
     * tocados.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: falha ao copiar vira {@link ArquivoLegendaException}
     * e aborta o processamento, sem fingir que a geração anterior foi preservada.
     */
    public void arquivarCacheAntesDaRetraducao(Path arquivoCache) {
        Path raizBackup = DiretorioBaseKronos.resolver("backups", "traducao-cache").toAbsolutePath().normalize();
        try {
            Path backupCache = arquivarCacheParaRetraducao(arquivoCache, raizBackup);
            log.warn("Retradução liberada: cache anterior preservado em {} e ignorado nesta execução "
                + "(o arquivo ativo permanece até a nova geração ser gravada).", backupCache);
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
        Path raizBackup = DiretorioBaseKronos.resolver("backups", "traducao").toAbsolutePath().normalize();
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
            Path raizBackup = DiretorioBaseKronos.resolver("backups", "traducao-cache").toAbsolutePath().normalize();
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
     * PROPÓSITO DE NEGÓCIO: preserva o cache atual numa cópia datada antes de uma retradução
     * integral, sem heurística sobre se o cache antigo ainda é confiável.
     *
     * <p>INVARIANTES DO DOMÍNIO: COPIA e NUNCA remove o cache do caminho ativo. A retradução
     * ignora a geração anterior por decisão EM MEMÓRIA (o chamador não carrega o mapa), não
     * por o arquivo sumir do disco; o caminho ativo só muda quando a nova geração é gravada
     * atomicamente ao fim do episódio. Opera exclusivamente sobre o arquivo informado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga {@link IOException}; sem a cópia fiel, o
     * chamador aborta e nenhuma retradução começa. O cache ativo permanece intacto em
     * qualquer desfecho.
     *
     * <p>HISTÓRICO: até 2026-07-22 este método apagava o cache logo após copiá-lo, deixando o
     * caminho ativo vazio durante toda a tradução seguinte. Uma interrupção nessa janela
     * perdia o episódio inteiro — foi o caso do S00E02 do 08th MS Team.
     */
    public static Path arquivarCacheParaRetraducao(Path arquivoCache, Path raizBackup) throws IOException {
        return copiarParaBackupExclusivo(arquivoCache, raizBackup);
    }
}
