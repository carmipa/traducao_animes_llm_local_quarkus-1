package org.traducao.projeto.cachetraducao.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: a raiz {@code cache/} guarda o acervo inteiro lado a lado — em
 * 2026-07-30 eram 172 arquivos de 8 obras. A tela de manutenção pergunta "qual obra?",
 * valida a resposta e, até esta mudança, processava TODAS. Quem escolhia Guilty Crown
 * (23 arquivos) via 172 sendo reescritos.
 *
 * <p>Este teste fixa o escopo: com obra escolhida, só os caches DELA entram; sem obra
 * escolhida, tudo entra — o modo de manutenção do acervo, que continua existindo.
 *
 * <p>INVARIANTES DO DOMÍNIO: o casamento é pela PROVENIÊNCIA gravada no arquivo, não pelo
 * nome da pasta — independe de o usuário apontar o diretório certo. Cache legado (sem
 * proveniência) permanece alcançável, porque é para ele que existe o contexto de fallback.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer arquivo de outra obra que volte a entrar na
 * lista reprova a suíte.
 */
class ManutencaoCacheEscopoPorObraTest {

    private final CacheManutencaoService servico = new CacheManutencaoService(new ObjectMapper());

    private void cache(Path raiz, String pasta, String nome, String contextoId) throws IOException {
        Path destino = raiz.resolve(pasta);
        Files.createDirectories(destino);
        String json = contextoId == null
            ? "[{\"indice\":1,\"original\":\"Hi\",\"traduzido\":\"Oi\"}]"
            : ("{\"proveniencia\":{\"schemaVersion\":1,\"contextoId\":\"" + contextoId
                + "\",\"contextoHash\":\"h\",\"modeloLlm\":\"m\",\"idiomaOrigem\":\"en\","
                + "\"idiomaDestino\":\"pt-br\"},\"entradas\":"
                + "[{\"indice\":1,\"estilo\":\"Default\",\"original\":\"Hi\","
                + "\"traduzido\":\"Oi\",\"idiomaOriginal\":\"en\",\"idiomaTraduzido\":\"pt-br\"}]}");
        Files.writeString(destino.resolve(nome + ".cache.json"), json);
    }

    private Path acervo(Path raiz) throws IOException {
        cache(raiz, "Guilty Crown BD", "ep01", "guilty_crown");
        cache(raiz, "Guilty Crown BD", "ep02", "guilty_crown");
        cache(raiz, "Zeta Gundam", "ep01", "gundam_zeta");
        cache(raiz, "Zeta Gundam", "ep02", "gundam_zeta");
        cache(raiz, "Unicorn", "ep01", "gundam_unicorn");
        return raiz;
    }

    @Test
    @DisplayName("Com obra escolhida, só os caches DELA entram na manutenção")
    void escopoRestringeAObraEscolhida(@TempDir Path raiz) throws IOException {
        acervo(raiz);

        List<Path> daObra = servico.listarCachesDaObra(raiz, "guilty_crown");

        assertEquals(2, daObra.size(), "o acervo tem 5 caches; a obra pedida tem 2");
        assertTrue(daObra.stream().allMatch(p -> p.toString().contains("Guilty Crown")),
            "nenhum arquivo de outra obra pode entrar: " + daObra);
    }

    @Test
    @DisplayName("Sem obra escolhida, tudo entra — manutenção do acervo continua existindo")
    void semObraProcessaTudo(@TempDir Path raiz) throws IOException {
        acervo(raiz);

        assertEquals(5, servico.listarCachesDaObra(raiz, null).size());
        assertEquals(5, servico.listarCachesDaObra(raiz, "   ").size());
    }

    @Test
    @DisplayName("O filtro é pela PROVENIÊNCIA, não pelo nome da pasta")
    void filtraPelaProvenienciaNaoPelaPasta(@TempDir Path raiz) throws IOException {
        // arquivo do Zeta guardado numa pasta com nome de outra obra
        cache(raiz, "Pasta Com Nome Errado", "solto", "gundam_zeta");
        cache(raiz, "Guilty Crown BD", "ep01", "guilty_crown");

        List<Path> zeta = servico.listarCachesDaObra(raiz, "gundam_zeta");

        assertEquals(1, zeta.size());
        assertTrue(zeta.get(0).toString().contains("Pasta Com Nome Errado"),
            "o carimbo do arquivo manda, não o diretório");
    }

    @Test
    @DisplayName("Cache legado sem proveniência continua alcançável pela manutenção")
    void legadoPermaneceAlcancavel(@TempDir Path raiz) throws IOException {
        cache(raiz, "Obra Antiga", "ep01", null);
        cache(raiz, "Guilty Crown BD", "ep01", "guilty_crown");

        List<Path> escolhida = servico.listarCachesDaObra(raiz, "guilty_crown");

        assertEquals(2, escolhida.size(),
            "o legado entra porque é para ele que existe o contexto de fallback");
    }

    @Test
    @DisplayName("Obra sem nenhum cache devolve lista vazia, não o acervo inteiro")
    void obraSemCacheDevolveVazio(@TempDir Path raiz) throws IOException {
        acervo(raiz);

        assertTrue(servico.listarCachesDaObra(raiz, "gundam_f91").isEmpty(),
            "obra sem cache não pode cair no comportamento de 'processar tudo'");
    }
}
