package org.traducao.projeto.medicao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.traducao.projeto.cachetraducao.domain.CacheDocumento;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: ler o acervo REAL de cache uma vez só, com os tipos do próprio projeto,
 * para que medir o acervo deixe de ser script descartável.
 *
 * <h2>Por que isto existe</h2>
 * Toda pergunta sobre o acervo ("quantas falas têm a quebra?", "quantos nomes partidos?") vinha
 * sendo respondida por Python de rascunho que reimplementava o formato do cache de cabeça. O
 * resultado, medido em 2026-08-04 numa única sessão:
 * <ul>
 *   <li>li o campo {@code traducao} quando o cache grava {@code traduzido} — reportei 551
 *       violações onde havia 41;</li>
 *   <li>regex montada por {@code .replace()} gerou classe aninhada — reportei <b>0</b> nomes
 *       partidos pela quebra, quando eram <b>435</b>;</li>
 *   <li>medi o FRAGMENTO do artigo em vez do padrão inteiro — ganho fantasma de +375 e +268.</li>
 * </ul>
 * Em todos, o número errado foi reportado com confiança. Aqui o nome do campo vem de
 * {@link EntradaCache}: divergir não compila.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Estritamente READ-ONLY. Nada nesta classe escreve no acervo. {@code cache/} é gitignorado
 *       desde {@code 12d720a} e apagá-lo é irreversível por git.</li>
 *   <li>Lê os DOIS formatos, como {@code CacheTraducaoService}: lista pura na raiz (legado) e
 *       objeto com proveniência + entradas. Ler só o novo esconderia o acervo antigo da conta.</li>
 *   <li>Arquivo ilegível é PULADO e contado, nunca silenciado — "0 arquivos lidos" tem a mesma
 *       aparência de "0 ocorrências".</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Raiz inexistente devolve lista vazia; quem chama decide se avisa ou aborta. Não lança por
 * arquivo corrompido.
 */
public final class LeitorAcervoCache {

    /** Sobrescreve a raiz do acervo, para medir um recorte sem mexer no de produção. */
    public static final String CHAVE_RAIZ = "kronos.medicao.cache";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LeitorAcervoCache() {
    }

    /** Uma fala do acervo, com a obra e a proveniência que a carimbou. */
    public record FalaDoAcervo(String obra, Path arquivo,
                               ProvenienciaCache proveniencia, EntradaCache entrada) {

        /** Texto traduzido, nunca {@code null} — entrada sem tradução vira string vazia. */
        public String traduzido() {
            return entrada.traduzido() == null ? "" : entrada.traduzido();
        }

        /** Texto original, nunca {@code null}. */
        public String original() {
            return entrada.original() == null ? "" : entrada.original();
        }
    }

    /** Resultado da leitura, com a contagem de arquivos ilegíveis à vista. */
    public record Acervo(List<FalaDoAcervo> falas, int arquivosLidos, int arquivosIlegiveis) {

        public boolean vazio() {
            return falas.isEmpty();
        }
    }

    public static Path raizPadrao() {
        String configurada = System.getProperty(CHAVE_RAIZ);
        return configurada != null ? Path.of(configurada) : Path.of("cache");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve todas as falas do acervo sob {@code raiz}.
     * <p>INVARIANTES DO DOMÍNIO: read-only; os dois formatos; ilegível é contado, não escondido.
     * <p>COMPORTAMENTO EM CASO DE FALHA: raiz ausente devolve acervo vazio com zero arquivos.
     */
    public static Acervo ler(Path raiz) throws IOException {
        if (!Files.isDirectory(raiz)) {
            return new Acervo(List.of(), 0, 0);
        }
        List<FalaDoAcervo> falas = new ArrayList<>();
        int lidos = 0;
        int ilegiveis = 0;
        try (Stream<Path> arquivos = Files.walk(raiz)) {
            for (Path arquivo : arquivos.filter(LeitorAcervoCache::eCacheDeTraducao).toList()) {
                List<EntradaCache> entradas;
                ProvenienciaCache proveniencia;
                try {
                    JsonNode raizJson = MAPPER.readTree(arquivo.toFile());
                    if (raizJson.isArray()) {
                        // Formato legado: lista pura de entradas, sem cabecalho de proveniencia.
                        CollectionType tipo = MAPPER.getTypeFactory()
                            .constructCollectionType(List.class, EntradaCache.class);
                        entradas = MAPPER.convertValue(raizJson, tipo);
                        proveniencia = null;
                    } else {
                        CacheDocumento doc = MAPPER.convertValue(raizJson, CacheDocumento.class);
                        entradas = doc.entradas() != null ? doc.entradas() : List.of();
                        proveniencia = doc.proveniencia();
                    }
                } catch (IOException | IllegalArgumentException e) {
                    ilegiveis++;
                    continue;
                }
                lidos++;
                String obra = arquivo.getParent() != null
                    ? arquivo.getParent().getFileName().toString() : "(raiz)";
                for (EntradaCache entrada : entradas) {
                    if (entrada != null) {
                        falas.add(new FalaDoAcervo(obra, arquivo, proveniencia, entrada));
                    }
                }
            }
        }
        return new Acervo(falas, lidos, ilegiveis);
    }

    private static boolean eCacheDeTraducao(Path p) {
        return p.getFileName().toString().endsWith(".cache.json");
    }
}
