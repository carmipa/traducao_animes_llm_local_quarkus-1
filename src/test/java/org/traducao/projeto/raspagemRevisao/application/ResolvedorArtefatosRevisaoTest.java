package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.raspagemRevisao.domain.FrescorCache;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova o pareamento legenda↔cache↔original, que é a fundação silenciosa da
 * revisão — se o par sair errado, a fala de um episódio é comparada com o original de outro e as
 * "correções" resultantes são todas espúrias.
 *
 * <p>Estes doze métodos viveram como privados dentro de um caso de uso de 1.726 linhas e nunca
 * tiveram teste próprio: só eram exercidos de raspão pelos testes de integração da revisão, que
 * falham por motivos completamente diferentes. Extraí-los na FASE 4 tornou possível testá-los pelo
 * que eles são — regras de nome.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Sem rede. O disco é usado só para criar arquivos vazios num {@code @TempDir} e observar
 *       qual deles o resolvedor escolhe.</li>
 *   <li>Nenhum caso depende da ordem em que o sistema de arquivos lista a pasta: onde há empate, o
 *       teste afirma a política de desempate declarada, não o acaso da varredura.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer par diferente do declarado reprova nomeando os dois caminhos.
 */
class ResolvedorArtefatosRevisaoTest {

    @TempDir
    Path temp;

    private final ResolvedorArtefatosRevisao resolvedor = new ResolvedorArtefatosRevisao();

    // ---------- normalização de nomes ----------

    @Test
    @DisplayName("normalizarBaseLegenda remove sufixo de idioma e de faixa, na ordem certa")
    void normalizaBase() {
        assertEquals("Gundam ZZ S01E03",
            ResolvedorArtefatosRevisao.normalizarBaseLegenda("Gundam ZZ S01E03_PT-BR_Track3"),
            "PT-BR com faixa sai inteiro numa passada só");
        assertEquals("Gundam ZZ S01E03",
            ResolvedorArtefatosRevisao.normalizarBaseLegenda("Gundam ZZ S01E03_PTBR"),
            "grafia sem hífen também");
        assertEquals("Gundam ZZ S01E03",
            ResolvedorArtefatosRevisao.normalizarBaseLegenda("Gundam ZZ S01E03_ENG"));
        assertEquals("Gundam ZZ S01E03",
            ResolvedorArtefatosRevisao.normalizarBaseLegenda("Gundam ZZ S01E03_Track2"));
    }

    @Test
    @DisplayName("normalizarBaseLegenda NÃO amputa nome de obra que contém os sufixos no meio")
    void naoAmputaNoMeio() {
        assertEquals("Track Down_ENG_final",
            ResolvedorArtefatosRevisao.normalizarBaseLegenda("Track Down_ENG_final"),
            "as regras são ancoradas no FIM; sufixo no meio do nome é parte da obra");
    }

    @Test
    @DisplayName("extensaoLegenda distingue .ssa de .ass e ignora caixa")
    void extensao() {
        assertEquals(".ssa", ResolvedorArtefatosRevisao.extensaoLegenda("ep01.SSA"));
        assertEquals(".ass", ResolvedorArtefatosRevisao.extensaoLegenda("ep01.ass"));
        assertEquals(".ass", ResolvedorArtefatosRevisao.extensaoLegenda("ep01.mkv"),
            "desconhecido degrada para o formato padrão do pipeline");
    }

    @Test
    @DisplayName("extrairCodigoEpisodio devolve SxxEyy em maiúsculas, ou null")
    void codigoEpisodio() {
        assertEquals("S01E03", ResolvedorArtefatosRevisao.extrairCodigoEpisodio("Gundam s01e03_PT-BR"));
        assertEquals("S1E120", ResolvedorArtefatosRevisao.extrairCodigoEpisodio("Serie S1E120"));
        assertNull(ResolvedorArtefatosRevisao.extrairCodigoEpisodio("Filme sem codigo"));
    }

    @Test
    @DisplayName("preferenciaArquivoOriginal: _Track2 vence _ENG, que vence _Track1")
    void preferencia() {
        int track2 = ResolvedorArtefatosRevisao.preferenciaArquivoOriginal("obra_Track2.ass");
        int eng = ResolvedorArtefatosRevisao.preferenciaArquivoOriginal("obra_ENG.ass");
        int track1 = ResolvedorArtefatosRevisao.preferenciaArquivoOriginal("obra_Track1.ass");
        int outro = ResolvedorArtefatosRevisao.preferenciaArquivoOriginal("obra.ass");
        assertTrue(track2 < eng && eng < track1 && track1 < outro,
            "a ordem É a política de desempate; invertê-la troca a faixa de referência do acervo");
    }

    @Test
    @DisplayName("candidatosNomeCache não repete e mantém ordem estável")
    void candidatos() {
        List<String> c = ResolvedorArtefatosRevisao.candidatosNomeCache("ep01", "ep01");
        assertEquals(List.of("ep01_ENG.cache.json", "ep01.cache.json"), c,
            "base e mídia iguais colapsam de 4 para 2 candidatos, sem duplicata");
    }

    @Test
    @DisplayName("correspondeCache exige extensão de cache e casa por base ou por código+_ENG")
    void correspondencia() {
        assertTrue(ResolvedorArtefatosRevisao.correspondeCache("ep01_ENG.cache.json", "ep01", "S01E01"));
        assertTrue(ResolvedorArtefatosRevisao.correspondeCache("qualquer_S01E01_ENG.cache.json", "outra", "S01E01"),
            "nem toda obra nomeia o cache pela mídia; o código de episódio é o segundo caminho");
        assertFalse(ResolvedorArtefatosRevisao.correspondeCache("ep01_ENG.json", "ep01", "S01E01"),
            "sem .cache.json não é cache");
        assertFalse(ResolvedorArtefatosRevisao.correspondeCache("qualquer_S01E01.cache.json", "outra", "S01E01"),
            "código sozinho não basta: sem _ENG poderia ser o cache de outra faixa");
    }

    // ---------- pareamento no disco ----------

    /**
     * PROPÓSITO DE NEGÓCIO: prova a PRIORIDADE do candidato direto, não apenas que algum arquivo é
     * encontrado.
     *
     * <p>A subpasta chama-se {@code AAA} de propósito. Com um nome como {@code Obra}, a varredura
     * ordenada devolveria o MESMO arquivo que os candidatos diretos — e o teste passaria mesmo com a
     * prioridade removida, afirmando algo que não estaria verificando. Com {@code AAA}, o arquivo da
     * subpasta ordena ANTES do da raiz, então só a prioridade declarada explica o resultado.
     */
    @Test
    @DisplayName("resolverArquivoCache prefere candidato DIRETO mesmo quando a varredura acharia outro antes")
    void cacheDiretoVenceRecursivo() throws Exception {
        Path cacheDir = Files.createDirectories(temp.resolve("cache"));
        Path subpasta = Files.createDirectories(cacheDir.resolve("AAA"));
        Path direto = Files.createFile(cacheDir.resolve("ep01_ENG.cache.json"));
        Path naSubpasta = Files.createFile(subpasta.resolve("ep01_ENG.cache.json"));

        assertTrue(naSubpasta.compareTo(direto) < 0,
            "pré-condição do teste: a varredura ordenada encontraria o da subpasta primeiro");

        Path achado = resolvedor.resolverArquivoCache(temp.resolve("ep01_PT-BR.ass"), cacheDir);

        assertEquals(direto, achado, "o candidato direto tem prioridade declarada sobre a varredura");
    }

    @Test
    @DisplayName("resolverArquivoCache encontra em subpasta por obra quando não há direto")
    void cacheEmSubpasta() throws Exception {
        Path cacheDir = Files.createDirectories(temp.resolve("cache"));
        Path subpasta = Files.createDirectories(cacheDir.resolve("Mobile Suit Gundam ZZ"));
        Path naSubpasta = Files.createFile(subpasta.resolve("Gundam ZZ S01E03_ENG.cache.json"));

        Path achado = resolvedor.resolverArquivoCache(
            temp.resolve("Gundam ZZ S01E03_PT-BR_Track3.ass"), cacheDir);

        assertEquals(naSubpasta, achado);
    }

    @Test
    @DisplayName("resolverArquivoCache sem par devolve o caminho convencional, não nulo")
    void cacheAusenteDevolveConvencional() throws Exception {
        Path cacheDir = Files.createDirectories(temp.resolve("cache"));

        Path achado = resolvedor.resolverArquivoCache(temp.resolve("ep99_PT-BR.ass"), cacheDir);

        assertEquals(cacheDir.resolve("ep99_ENG.cache.json"), achado,
            "devolver um palpite explícito deixa o leitor tratar como ausente, sem derrubar o lote");
    }

    @Test
    @DisplayName("resolverArquivoOriginal NUNCA devolve outra legenda traduzida")
    void nuncaDevolveTraducaoComoOriginal() throws Exception {
        Path pasta = Files.createDirectories(temp.resolve("en"));
        Path pt = Files.createFile(pasta.resolve("Serie S01E01_PT-BR.ass"));
        Files.createFile(pasta.resolve("Serie S01E01_PTBR_Track3.ass"));
        Path ingles = Files.createFile(pasta.resolve("Serie S01E01_Track2.ass"));

        Path achado = resolvedor.resolverArquivoOriginal(pt, pasta);

        assertEquals(ingles, achado,
            "comparar tradução com tradução produziria correções sem original");
    }

    @Test
    @DisplayName("resolverArquivoOriginal aplica a preferência de faixa entre vários candidatos")
    void preferenciaEntreCandidatosDoMesmoEpisodio() throws Exception {
        Path pasta = Files.createDirectories(temp.resolve("en"));
        Path pt = Files.createFile(pasta.resolve("Obra Sem Base Comum S02E07_PT-BR.ass"));
        Files.createFile(pasta.resolve("outro nome S02E07_Track1.ass"));
        Path preferido = Files.createFile(pasta.resolve("outro nome S02E07_Track2.ass"));

        Path achado = resolvedor.resolverArquivoOriginal(pt, pasta);

        assertEquals(preferido, achado, "_Track2 vence _Track1 na varredura por código de episódio");
    }

    // ---------- frescor ----------

    @Test
    @DisplayName("compararFrescor: cache mais novo autoriza; empate NÃO autoriza")
    void frescor() throws Exception {
        Path cache = Files.createFile(temp.resolve("ep.cache.json"));
        Path legenda = Files.createFile(temp.resolve("ep.ass"));

        FileTime antigo = FileTime.fromMillis(1_000_000);
        FileTime novo = FileTime.fromMillis(2_000_000);

        Files.setLastModifiedTime(cache, novo);
        Files.setLastModifiedTime(legenda, antigo);
        assertEquals(FrescorCache.CACHE_MAIS_NOVO, resolvedor.compararFrescor(cache, legenda));

        Files.setLastModifiedTime(cache, antigo);
        assertEquals(FrescorCache.LEGENDA_ATUAL, resolvedor.compararFrescor(cache, legenda),
            "empate de data NÃO autoriza sobrescrever a legenda");
    }

    @Test
    @DisplayName("compararFrescor: arquivo ausente é INDETERMINADO, não 'legenda atual'")
    void frescorIndeterminado() throws Exception {
        Path legenda = Files.createFile(temp.resolve("ep.ass"));

        assertEquals(FrescorCache.INDETERMINADO,
            resolvedor.compararFrescor(temp.resolve("nao-existe.cache.json"), legenda),
            "o desfecho 'não deu para saber' é o que o caso de uso avisa ao operador; "
                + "colapsá-lo em 'legenda atual' desligaria a sincronização em silêncio");
    }
}
