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

    /**
     * PROPÓSITO DE NEGÓCIO: o defeito REAL de 08/08/2026, reproduzido com os nomes exatos do log de
     * produção. O Gundam 0083 carregou o cache do Gundam ZZ porque ambos têm um episódio
     * {@code S01E02} — e, como a proveniência do cache tem precedência sobre a seleção manual, a
     * LORE ativa virou a da obra errada.
     *
     * <pre>
     *   Analisando: ...0083 - Stardust Memory - S01E02_5_Track3_PT-BR.ass
     *   Cache carregado: Mobile.Suit.Gundam.ZZ.S01E02_ENG.cache.json   (289 entradas)
     *   [CONTEXTO] Seleção manual "gundam_0083" ignorada: a proveniência exige "gundam_zz"
     * </pre>
     *
     * <p>Se este teste voltar a passar como {@code true}, a revisão volta a poder comparar uma obra
     * com o original de outra — e a injetar terminologia alheia na legenda publicada.
     */
    @Test
    @DisplayName("BUG 08/08: cache de OUTRA obra com o mesmo episodio NAO casa")
    void cacheDeOutraObraComMesmoEpisodioNaoCasa() {
        String baseMidia0083 = "Mobile Suit Gundam 0083 - Stardust Memory - S01E02_5";

        assertFalse(ResolvedorArtefatosRevisao.correspondeCache(
                "Mobile.Suit.Gundam.ZZ.S01E02_ENG.cache.json", baseMidia0083, "S01E02"),
            "cache do Double Zeta NAO pode casar com arquivo do Stardust Memory");

        for (String alheio : new String[]{
            "Mobile.Suit.Gundam.ZZ.S01E04_ENG.cache.json",
            "Mobile Suit Gundam Unicorn - S01E02_ENG.cache.json",
            "Mobile.Suit.Gundam.The.08th.MS.Team.S01E02_ENG.cache.json"}) {
            assertFalse(ResolvedorArtefatosRevisao.correspondeCache(alheio, baseMidia0083, "S01E02"),
                "casou com obra alheia: " + alheio);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CONTRA-TESTE — a correção não pode cegar o casamento legítimo. O
     * fallback por episódio existe porque nem toda obra nomeia o cache pela mídia, e ele continua
     * valendo DENTRO da mesma obra.
     */
    @Test
    @DisplayName("cache da MESMA obra continua casando pelo episodio")
    void cacheDaMesmaObraContinuaCasando() {
        String baseMidia = "Mobile Suit Gundam 0083 - Stardust Memory - S01E02";

        assertTrue(ResolvedorArtefatosRevisao.correspondeCache(
                "Mobile Suit Gundam 0083 - Stardust Memory - S01E02_PTBR_Track3_ENG.cache.json",
                baseMidia, "S01E02"),
            "cache da PROPRIA obra tem de casar — senao a fala fica sem referencia");

        assertTrue(ResolvedorArtefatosRevisao.correspondeCache(
                "Stardust.Memory.S01E02_ENG.cache.json", baseMidia, "S01E02"),
            "nome abreviado da mesma obra ainda compartilha o token 'stardust'");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: as palavras de franquia não podem servir de prova de identidade — se
     * {@code mobile}, {@code suit} e {@code gundam} contassem, 0083, ZZ, Unicorn e 08th seriam
     * todos "a mesma obra" e o defeito voltaria por outra porta.
     */
    @Test
    @DisplayName("palavra de franquia nao identifica obra")
    void palavraDeFranquiaNaoIdentificaObra() {
        assertFalse(ResolvedorArtefatosRevisao.mesmaObra(
                "Mobile.Suit.Gundam.ZZ.S01E02_ENG", "Mobile Suit Gundam 0083 - Stardust Memory - S01E02"),
            "'Mobile Suit Gundam' em comum NAO torna ZZ e 0083 a mesma obra");
        assertTrue(ResolvedorArtefatosRevisao.mesmaObra(
                "Mobile.Suit.Gundam.ZZ.S01E07_ENG", "Mobile Suit Gundam ZZ - S01E07"),
            "'zz' e token distintivo e tem de casar");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: falha fechada. Nome sem nenhum token distintivo não prova identidade de
     * obra nenhuma — e o viés, num pareamento que alimenta revisão de lore, é RECUSAR.
     */
    @Test
    @DisplayName("FALHA FECHADA: nome generico nao casa com obra alguma")
    void nomeGenericoNaoCasa() {
        assertFalse(ResolvedorArtefatosRevisao.mesmaObra("S01E02_ENG", "Mobile Suit Gundam 0083 - S01E02"));
        assertFalse(ResolvedorArtefatosRevisao.mesmaObra("", "qualquer"));
        assertFalse(ResolvedorArtefatosRevisao.mesmaObra(null, "qualquer"));
    }

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

    /**
     * ATUALIZADO em 08/08/2026. A asserção do meio afirmava que
     * {@code correspondeCache("qualquer_S01E01_ENG.cache.json", "outra", "S01E01")} era
     * {@code true} — obras DIFERENTES casando só pelo código do episódio. Ela documentava o
     * comportamento que causou o incidente do Gundam 0083 carregando cache do Gundam ZZ (ver
     * {@link #cacheDeOutraObraComMesmoEpisodioNaoCasa()}), e por isso foi INVERTIDA, não removida:
     * o caso continua no teste, agora exigindo recusa.
     *
     * <p>Isto não é afrouxar a régua — é o contrário. O segundo caminho (código + {@code _ENG})
     * segue existindo para quem nomeia o cache de outro jeito, mas passou a exigir também
     * afinidade de obra.
     */
    @Test
    @DisplayName("correspondeCache exige extensão, e o código de episódio só vale na MESMA obra")
    void correspondencia() {
        assertTrue(ResolvedorArtefatosRevisao.correspondeCache("ep01_ENG.cache.json", "ep01", "S01E01"));
        assertFalse(ResolvedorArtefatosRevisao.correspondeCache("qualquer_S01E01_ENG.cache.json", "outra", "S01E01"),
            "INVERTIDO em 08/08/2026: episodio igual em obra diferente NAO casa — foi assim que o "
                + "0083 carregou o cache do ZZ e revisou com a lore errada");
        assertTrue(ResolvedorArtefatosRevisao.correspondeCache("stardust_S01E01_ENG.cache.json", "stardust memory", "S01E01"),
            "o segundo caminho continua vivo DENTRO da mesma obra: 'stardust' e o token em comum");
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
