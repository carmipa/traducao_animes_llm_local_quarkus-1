package org.traducao.projeto.traducaoKaraoke.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.core.io.DiretorioBaseKronos;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o cache de letras do karaokê (4.1) mora sob a raiz operacional
 * efetiva, e não em {@code ./cache/karaoke} real, quando a raiz é redirecionada — a suíte de testes
 * e qualquer medição isolada com {@code -Dkronos.dir.base}. É regressão de um dano medido em
 * 08/10/2026: uma tradução de teste isolada regravou 3 caches reais de karaokê e criou 2, porque
 * {@code CacheDoArquivo.arquivoDe} montava o caminho com {@code Path.of} cru.
 *
 * <p>INVARIANTES DO DOMÍNIO: diretório de cache RELATIVO (padrão ou configurado) resolve SOB a raiz;
 * diretório ABSOLUTO configurado passa intocado (é o caminho de produção de quem configurou e o
 * dos testes que já apontam para um {@code @TempDir}).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: a pré-condição reprova se a suíte rodar sem o
 * redirecionamento (sem ele o teste não discrimina nada); o caminho que escapar da raiz reprova
 * mostrando o valor resolvido.
 */
class CacheDoArquivoIsolamentoTest {

    private static CacheDoArquivo cacheCom(Optional<String> diretorio) {
        CacheDoArquivo cache = new CacheDoArquivo();
        cache.diretorioCache = diretorio;
        return cache;
    }

    @Test
    void cachePadraoFicaSobARaizRedirecionadaENuncaEmCacheKaraokeReal() {
        String raiz = System.getProperty(DiretorioBaseKronos.PROPRIEDADE_BASE);
        assertTrue(raiz != null && !raiz.isBlank(),
            "pré-condição: a suíte roda com kronos.dir.base redirecionado (ver build.gradle)");
        Path base = DiretorioBaseKronos.base().toAbsolutePath().normalize();

        for (Optional<String> diretorio : java.util.List.of(Optional.<String>empty(), Optional.of(""),
                Optional.of("cache"))) {
            Path resolvido = cacheCom(diretorio).arquivoDe(Path.of("obra", "Ep01_Track4_PT-BR.ass"))
                .toAbsolutePath().normalize();
            assertTrue(resolvido.startsWith(base),
                "cache do karaokê (" + diretorio + ") escapou da raiz: " + resolvido);
            assertEquals(base.resolve(Path.of("cache", "karaoke", "Ep01_Track4_PT-BR.cache.json")), resolvido);
            assertNotEquals(Path.of("cache", "karaoke", "Ep01_Track4_PT-BR.cache.json").toAbsolutePath().normalize(),
                resolvido, "não pode ser o ./cache/karaoke real do repositório");
        }
    }

    @Test
    void cacheAbsolutoConfiguradoPassaIntocado() {
        Path absoluto = Path.of("/kronos-cache-absoluto-do-usuario").toAbsolutePath().normalize();
        Path resolvido = cacheCom(Optional.of(absoluto.toString()))
            .arquivoDe(Path.of("Ep01_Track4_PT-BR.ass")).toAbsolutePath().normalize();
        assertEquals(absoluto.resolve(Path.of("karaoke", "Ep01_Track4_PT-BR.cache.json")), resolvido,
            "caminho absoluto configurado não pode ser reancorado na raiz operacional");
    }
}
