package org.traducao.projeto.raspagemRevisao.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.traducao.infrastructure.cache.CacheManutencaoService;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: prova que a Opção 6 consome tanto caches históricos
 * quanto o formato versionado atualmente produzido pelas Opções 4 e 5.
 *
 * <p>INVARIANTES DO DOMÍNIO: índice, original e tradução permanecem idênticos
 * ao JSON e a proveniência não interfere na leitura das entradas.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: incompatibilidade de schema reprova o teste.
 */
class LeitorCacheReferenciaServiceTest {

    @TempDir
    Path temp;

    /**
     * PROPÓSITO DE NEGÓCIO: cobre a fronteira real que antes tentava abrir o
     * objeto versionado como se fosse uma lista pura.
     * <p>INVARIANTES DO DOMÍNIO: envelope e entrada são aceitos juntos.
     * <p>COMPORTAMENTO EM CASO DE FALHA: leitura vazia ou campo divergente falha.
     */
    @Test
    void leCacheVersionadoProduzidoPelaTraducaoLocal() throws Exception {
        Path arquivo = temp.resolve("ep.cache.json");
        Files.writeString(arquivo, """
            {"proveniencia":{"schemaVersion":1,"contextoId":"gundam_nt"},
             "entradas":[{"indice":7,"estilo":"Default","original":"Jona!","traduzido":"Jona!","idiomaOriginal":"en","idiomaTraduzido":"pt-br"}]}
            """);
        var leitor = criarLeitor();

        var documento = leitor.carregarDocumento(arquivo);
        var entradas = documento.entradas();

        assertEquals(1, entradas.size());
        assertEquals(7, entradas.get(0).indice());
        assertEquals("Jona!", entradas.get(0).traduzido());
        assertEquals("gundam_nt", documento.proveniencia().contextoId());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém utilizáveis caches anteriores à proveniência.
     * <p>INVARIANTES DO DOMÍNIO: lista raiz continua aceita sem migração destrutiva.
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada ausente reprova o teste.
     */
    @Test
    void leCacheLegadoEmListaPura() throws Exception {
        Path arquivo = temp.resolve("legado.cache.json");
        Files.writeString(arquivo, """
            [{"indice":3,"estilo":"Default","original":"Help!","traduzido":"Ajude!","idiomaOriginal":"en","idiomaTraduzido":"pt-br"}]
            """);

        var entradas = criarLeitor().carregar(arquivo);

        assertEquals(1, entradas.size());
        assertEquals("Ajude!", entradas.get(0).traduzido());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: cria o leitor isolado com a mesma composição JSON
     * usada em produção, sem subir o contêiner web.
     * <p>INVARIANTES DO DOMÍNIO: serviço de manutenção e leitor compartilham mapper.
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de construção reprova o teste.
     */
    private LeitorCacheReferenciaService criarLeitor() {
        ObjectMapper mapper = new ObjectMapper();
        return new LeitorCacheReferenciaService(new CacheManutencaoService(mapper), mapper);
    }
}
