package org.traducao.projeto.cachetraducao.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.stereotype.Service;
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.core.util.ArquivoAtomicoUtil;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: fornece uma porta única e segura para os módulos que
 * corrigem os arquivos persistentes da pasta {@code cache}, aceitando tanto a
 * lista JSON histórica quanto o documento versionado com proveniência.
 *
 * <p>INVARIANTES DO DOMÍNIO: campos desconhecidos e o formato original são
 * preservados; um cache só é substituído depois de backup, serialização em
 * temporário e validação estrutural; a proveniência nunca é removida.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link IOException} sem substituir o
 * arquivo original. O temporário é removido e o backup permanece disponível em
 * {@code backups/correcao-cache/}.
 */
@Service
public class CacheManutencaoService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final ObjectMapper mapper;

    /**
     * PROPÓSITO DE NEGÓCIO: configura a serialização canônica usada para abrir e
     * regravar os bancos de cache.
     * <p>INVARIANTES DO DOMÍNIO: o mesmo mapper lê, escreve e valida o temporário.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência nula impede o uso do serviço.
     */
    public CacheManutencaoService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: representa um cache aberto para manutenção sem
     * perder o envelope de proveniência nem extensões futuras do JSON.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code raiz} é array legado ou objeto
     * versionado; {@code entradas} é exatamente o array mutável contido nessa
     * raiz; {@code proveniencia} pode ser nula somente no formato legado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: a construção inválida é impedida pelo
     * método {@link #carregar(Path)}, que lança {@link IOException}.
     */
    public record DocumentoEditavel(
        Path arquivo,
        JsonNode raiz,
        ArrayNode entradas,
        ProvenienciaCache proveniencia,
        boolean versionado
    ) {}

    /**
     * PROPÓSITO DE NEGÓCIO: agrupa os backups de uma execução numa pasta única,
     * facilitando restaurar integralmente uma limpeza ou revisão equivocada.
     *
     * <p>INVARIANTES DO DOMÍNIO: a raiz e a pasta de backup são absolutas e
     * normalizadas; arquivos externos à raiz não podem ser salvos pela sessão.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: caminhos fora da raiz causam
     * {@link IOException} antes de qualquer gravação.
     */
    public record Sessao(Path raizCache, Path pastaBackup, String operacao) {}

    /**
     * PROPÓSITO DE NEGÓCIO: abre um cache para inspeção/correção aceitando os
     * dois formatos persistidos pelo projeto.
     *
     * <p>INVARIANTES DO DOMÍNIO: objeto versionado precisa possuir
     * {@code entradas} como array; escalares e objetos incompletos são
     * rejeitados; nenhuma leitura modifica o disco.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link IOException} para JSON
     * ilegível ou estrutura incompatível, preservando o arquivo original.
     */
    public DocumentoEditavel carregar(Path arquivo) throws IOException {
        JsonNode raiz = mapper.readTree(arquivo.toFile());
        if (raiz == null || raiz.isNull()) {
            throw new IOException("Cache vazio ou nulo: " + arquivo);
        }
        if (raiz.isArray()) {
            return new DocumentoEditavel(arquivo, raiz, (ArrayNode) raiz, null, false);
        }
        if (!raiz.isObject() || !raiz.path("entradas").isArray()) {
            throw new IOException("Formato de cache desconhecido (esperado array legado ou objeto com entradas): " + arquivo);
        }

        ProvenienciaCache proveniencia = null;
        JsonNode noProveniencia = raiz.get("proveniencia");
        if (noProveniencia != null && !noProveniencia.isNull()) {
            if (!noProveniencia.isObject()) {
                throw new IOException("Proveniência inválida no cache: " + arquivo);
            }
            proveniencia = new ProvenienciaCache(
                noProveniencia.path("schemaVersion").asInt(ProvenienciaCache.SCHEMA_ATUAL),
                textoOuNulo(noProveniencia, "contextoId"),
                textoOuNulo(noProveniencia, "contextoHash"),
                textoOuNulo(noProveniencia, "modeloLlm"),
                textoOuNulo(noProveniencia, "idiomaOrigem"),
                textoOuNulo(noProveniencia, "idiomaDestino")
            );
        }
        return new DocumentoEditavel(
            arquivo, raiz, (ArrayNode) raiz.path("entradas"), proveniencia, true);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lê os campos conhecidos da proveniência sem rejeitar
     * extensões futuras adicionadas ao documento JSON.
     *
     * <p>INVARIANTES DO DOMÍNIO: campo ausente/nulo vira nulo; valores presentes
     * são mantidos textualmente.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança para campos opcionais.
     */
    private static String textoOuNulo(JsonNode no, String campo) {
        JsonNode valor = no.get(campo);
        return valor == null || valor.isNull() ? null : valor.asText();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: inicia uma unidade restaurável de manutenção para
     * uma pasta de cache escolhida na interface.
     *
     * <p>INVARIANTES DO DOMÍNIO: backups ficam dentro do projeto, sob
     * {@code backups/correcao-cache}, nunca nas pastas de mídia.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: apenas calcula os caminhos; a criação
     * física ocorre no primeiro salvamento e pode lançar {@link IOException}.
     */
    public Sessao iniciarSessao(Path raizCache, String operacao) {
        Path raiz = raizCache.toAbsolutePath().normalize();
        String nomeOperacao = operacao == null ? "manutencao" : operacao.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9_-]+", "_");
        Path backup = DiretorioBaseKronos.resolver("backups", "correcao-cache", nomeOperacao + "_" + LocalDateTime.now().format(TS))
            .toAbsolutePath().normalize();
        return new Sessao(raiz, backup, nomeOperacao);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lista somente caches da tradução-base que pertencem
     * ao menu Correção do Cache, sem misturar o banco específico de karaokê nem
     * os arquivos append-only de auditoria.
     *
     * <p>INVARIANTES DO DOMÍNIO: busca recursiva, ordem determinística e filtro
     * exclusivo por sufixo {@code .cache.json}; diretórios auxiliares canônicos
     * {@code karaoke} e {@code auditoria} são excluídos.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link IOException}; nenhuma
     * alteração é feita durante a listagem.
     */
    public List<Path> listarCachesTraducaoBase(Path raizCache) throws IOException {
        Path raiz = raizCache.toAbsolutePath().normalize();
        try (Stream<Path> caminhos = Files.walk(raiz)) {
            return caminhos.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".cache.json"))
                .filter(p -> !estaEmSubpastaAuxiliar(raiz, p.toAbsolutePath().normalize()))
                .sorted()
                .toList();
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege bancos de outros módulos que convivem sob a
     * mesma raiz cache.
     *
     * <p>INVARIANTES DO DOMÍNIO: comparação ignora maiúsculas/minúsculas e
     * considera qualquer segmento relativo do caminho.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: caminho fora da raiz é tratado como
     * auxiliar/não autorizado e não entra na manutenção.
     */
    private boolean estaEmSubpastaAuxiliar(Path raiz, Path arquivo) {
        if (!arquivo.startsWith(raiz)) return true;
        for (Path segmento : raiz.relativize(arquivo)) {
            String nome = segmento.toString();
            if ("karaoke".equalsIgnoreCase(nome) || "auditoria".equalsIgnoreCase(nome)) return true;
        }
        return false;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: persiste uma correção no banco de cache sem risco de
     * truncar horas de traduções acumuladas e mantendo uma cópia restaurável.
     *
     * <p>INVARIANTES DO DOMÍNIO: o arquivo pertence à raiz da sessão; o backup é
     * criado antes da escrita; o temporário precisa ser JSON válido com o mesmo
     * formato estrutural; a troca final é atômica quando suportada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link IOException}, remove o
     * temporário e mantém intocado o cache original.
     */
    public Path salvarAtomico(DocumentoEditavel documento, Sessao sessao) throws IOException {
        Path arquivo = documento.arquivo().toAbsolutePath().normalize();
        if (!arquivo.startsWith(sessao.raizCache())) {
            throw new IOException("Arquivo de cache fora da raiz autorizada: " + arquivo);
        }

        Path relativo = sessao.raizCache().relativize(arquivo);
        Path backup = sessao.pastaBackup().resolve(relativo).normalize();
        if (!backup.startsWith(sessao.pastaBackup())) {
            throw new IOException("Caminho de backup inválido para: " + arquivo);
        }
        Files.createDirectories(backup.getParent());
        // Checkpoints sucessivos da mesma sessão nunca podem sobrescrever a
        // fotografia anterior à manutenção: ela é a unidade real de rollback.
        if (Files.notExists(backup)) {
            Files.copy(arquivo, backup, StandardCopyOption.COPY_ATTRIBUTES);
        }

        Path pasta = arquivo.getParent();
        Path temporario = Files.createTempFile(pasta, arquivo.getFileName().toString(), ".tmp");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(temporario.toFile(), documento.raiz());
            validarTemporario(temporario, documento.versionado());
            ArquivoAtomicoUtil.substituirAtomico(temporario, arquivo);
            return backup;
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede que uma serialização incompleta seja aceita
     * como novo banco de cache.
     *
     * <p>INVARIANTES DO DOMÍNIO: formato legado permanece array; formato
     * versionado permanece objeto com array {@code entradas}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link IOException} e bloqueia a
     * substituição do arquivo definitivo.
     */
    private void validarTemporario(Path temporario, boolean versionado) throws IOException {
        JsonNode validacao = mapper.readTree(temporario.toFile());
        boolean valido = versionado
            ? validacao != null && validacao.isObject() && validacao.path("entradas").isArray()
            : validacao != null && validacao.isArray();
        if (!valido) {
            throw new IOException("Cache temporário falhou na validação estrutural: " + temporario);
        }
    }
}
