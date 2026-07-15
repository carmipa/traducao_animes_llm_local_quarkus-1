package org.traducao.projeto.raspagemRevisao.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;
import org.traducao.projeto.traducao.infrastructure.cache.EntradaCache;
import org.traducao.projeto.traducao.infrastructure.cache.ProvenienciaCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: comprova a blindagem do modo "Cache" da Opção 6 — uma
 * entrada só vira referência quando casa com segurança (índice + estilo +
 * proveniência + texto); o resto fica SEM_REFERÊNCIA_SEGURA.
 * <p>INVARIANTES DO DOMÍNIO: placas/karaokê não exigem referência e não são
 * marcadas; cache sem proveniência não vincula nada.
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer vínculo indevido ou marcação
 * incorreta reprova o teste.
 */
@QuarkusTest
class RevisarLegendasCacheSeguroTest {

    @Inject
    RevisarLegendasUseCase useCase;

    @Inject
    SincronizadorLegendaCacheService sincronizador;

    private Set<Integer> indicesSeguros(DocumentoLegenda doc, List<EntradaCache> cache, ProvenienciaCache prov) {
        return useCase.montarReferenciaCacheSegura(doc, cache, prov).originaisPorIndice().keySet();
    }

    private EventoLegenda dialogo(int indice, String estilo, String texto) {
        return new EventoLegenda(indice, "Dialogue", estilo,
            "Dialogue: 0,0:00:01.00,0:00:03.00," + estilo + ",,0,0,0,,", texto);
    }

    private ProvenienciaCache proveniencia(String contextoId) {
        return new ProvenienciaCache(1, contextoId, "hash", "modelo", "en", "pt");
    }

    @Test
    void vinculaSomenteEntradasQueCasamComSeguranca() {
        DocumentoLegenda doc = new DocumentoLegenda("", List.of(
            dialogo(1, "Default", "Olá mundo"),        // casa com o cache -> referência segura
            dialogo(2, "Default", "Texto editado"),    // traduzido diverge do cache -> inseguro
            dialogo(3, "Italic", "Fala três"),         // estilo diverge do cache -> inseguro
            dialogo(4, "Sign", "{\\pos(9,9)}Placa")    // placa: ignorada, não é "insegura"
        ), "\n", false);

        List<EntradaCache> cache = List.of(
            new EntradaCache(1, "Default", "Hello world", "Olá mundo", "en", "pt"),
            new EntradaCache(2, "Default", "Two", "Texto do cache", "en", "pt"),
            new EntradaCache(3, "Default", "Three", "Fala três", "en", "pt"),
            new EntradaCache(4, "Sign", "Sign", "Placa", "en", "pt")
        );

        RevisarLegendasUseCase.ReferenciaCacheSegura ref =
            useCase.montarReferenciaCacheSegura(doc, cache, proveniencia("danmachi"));

        assertEquals("Hello world", ref.originaisPorIndice().get(1));
        assertFalse(ref.originaisPorIndice().containsKey(2));
        assertFalse(ref.originaisPorIndice().containsKey(3));
        assertTrue(ref.semReferenciaSegura().contains(2));
        assertTrue(ref.semReferenciaSegura().contains(3));
        assertFalse(ref.semReferenciaSegura().contains(1));
        assertFalse(ref.semReferenciaSegura().contains(4)); // placa não vira insegura
    }

    @Test
    void cacheSemProvenienciaMarcaTudoComoInseguro() {
        DocumentoLegenda doc = new DocumentoLegenda("", List.of(
            dialogo(1, "Default", "Olá mundo")
        ), "\n", false);
        List<EntradaCache> cache = List.of(
            new EntradaCache(1, "Default", "Hello world", "Olá mundo", "en", "pt")
        );

        RevisarLegendasUseCase.ReferenciaCacheSegura ref =
            useCase.montarReferenciaCacheSegura(doc, cache, proveniencia(""));

        assertTrue(ref.originaisPorIndice().isEmpty());
        assertTrue(ref.semReferenciaSegura().contains(1));
    }

    /**
     * Bug 1 — cache mais novo com ESTILO incompatível não pode alterar o ASS.
     */
    @Test
    void syncNaoAlteraAssQuandoEstiloIncompativel() {
        DocumentoLegenda doc = new DocumentoLegenda("",
            List.of(dialogo(1, "Default", "Texto PT atual")), "\n", false);
        List<EntradaCache> cache = List.of(
            new EntradaCache(1, "Italic", "Hello", "Cache diferente", "en", "pt"));

        Set<Integer> permitidos = indicesSeguros(doc, cache, proveniencia("danmachi"));
        assertTrue(permitidos.isEmpty());

        SincronizadorLegendaCacheService.Resultado r =
            sincronizador.sincronizar(doc, cache, true, Set.of(), permitidos);
        assertEquals(0, r.total());
        assertEquals("Texto PT atual", r.documento().eventos().get(0).texto());
    }

    /**
     * Bug 1 — cache de OUTRO episódio com o mesmo índice (texto não corresponde)
     * não pode alterar o ASS, mesmo com cache mais novo.
     */
    @Test
    void syncNaoAlteraAssDeOutroEpisodioComMesmoIndice() {
        DocumentoLegenda doc = new DocumentoLegenda("",
            List.of(dialogo(5, "Default", "Fala do episódio A")), "\n", false);
        List<EntradaCache> cache = List.of(
            new EntradaCache(5, "Default", "Episode B EN", "Episode B PT", "en", "pt"));

        Set<Integer> permitidos = indicesSeguros(doc, cache, proveniencia("danmachi"));
        assertTrue(permitidos.isEmpty());

        SincronizadorLegendaCacheService.Resultado r =
            sincronizador.sincronizar(doc, cache, true, Set.of(), permitidos);
        assertEquals(0, r.total());
        assertEquals("Fala do episódio A", r.documento().eventos().get(0).texto());
    }

    /**
     * Bug 1 — cache SEM proveniência não sincroniza nem serve de referência,
     * ainda que a fala tenha regredido exatamente ao inglês.
     */
    @Test
    void syncNaoAlteraNemReferenciaSemProveniencia() {
        DocumentoLegenda doc = new DocumentoLegenda("",
            List.of(dialogo(1, "Default", "Hello world")), "\n", false);
        List<EntradaCache> cache = List.of(
            new EntradaCache(1, "Default", "Hello world", "Olá mundo", "en", "pt"));

        RevisarLegendasUseCase.ReferenciaCacheSegura ref =
            useCase.montarReferenciaCacheSegura(doc, cache, proveniencia(""));
        assertTrue(ref.originaisPorIndice().isEmpty());

        SincronizadorLegendaCacheService.Resultado r =
            sincronizador.sincronizar(doc, cache, true, Set.of(), ref.originaisPorIndice().keySet());
        assertEquals(0, r.total());
        assertEquals("Hello world", r.documento().eventos().get(0).texto());
    }

    /**
     * Bug 2 — após sincronização segura, o original inglês continua disponível
     * como referência (a fala regredida ao EN é vínculo seguro e é restaurada).
     */
    @Test
    void syncSeguroRestauraTraduzidoEMantemReferenciaEn() {
        DocumentoLegenda doc = new DocumentoLegenda("",
            List.of(dialogo(1, "Default", "Hello world")), "\n", false); // regrediu ao inglês
        List<EntradaCache> cache = List.of(
            new EntradaCache(1, "Default", "Hello world", "Olá mundo", "en", "pt"));

        RevisarLegendasUseCase.ReferenciaCacheSegura ref =
            useCase.montarReferenciaCacheSegura(doc, cache, proveniencia("danmachi"));
        assertEquals("Hello world", ref.originaisPorIndice().get(1)); // referência EN disponível

        SincronizadorLegendaCacheService.Resultado r =
            sincronizador.sincronizar(doc, cache, true, Set.of(), ref.originaisPorIndice().keySet());
        assertEquals(1, r.total());
        assertEquals("Olá mundo", r.documento().eventos().get(0).texto()); // tradução restaurada
    }

    /**
     * Test 10 — o backup é criado antes de qualquer sobrescrita (origem == destino);
     * quando não há sobrescrita (origem != destino) nenhum backup é gerado.
     */
    @Test
    void criaBackupAntesDeSobrescrever(@TempDir Path tempDir) throws IOException {
        Path arquivo = tempDir.resolve("ep_PT-BR.ass");
        Files.writeString(arquivo, "conteudo original");
        Path pastaBackup = tempDir.resolve("bk");

        Path backup = useCase.criarBackupSeSobrescrever(arquivo, arquivo, pastaBackup);
        assertNotNull(backup);
        assertTrue(Files.exists(backup));
        assertEquals("conteudo original", Files.readString(backup));

        assertNull(useCase.criarBackupSeSobrescrever(arquivo, tempDir.resolve("outro.ass"), pastaBackup));
    }
}
