package org.traducao.projeto.cachetraducao.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre o cache versionado por proveniência: reuso só quando lore/modelo batem,
 * invalidação + arquivamento quando divergem, migração do formato antigo e
 * preservação (não sobrescrita) de cache corrompido.
 *
 * <p>Cobre também a propriedade TRANSACIONAL do caminho ativo (hotfix de 2026-07-22):
 * invalidar por proveniência é decisão de memória e copia a geração anterior para o lado,
 * mas nunca esvazia o caminho ativo — só {@code salvar} troca o conteúdo ativo, de uma vez.
 * Antes do hotfix o arquivo era MOVIDO na invalidação, e uma interrupção durante a
 * retradução deixava o episódio sem cache nenhum.
 */
class CacheTraducaoServiceTest {

    @TempDir
    Path dir;

    private final CacheTraducaoService svc = new CacheTraducaoService(new ObjectMapper());

    private static ProvenienciaCache prov(String hash) {
        return new ProvenienciaCache(ProvenienciaCache.SCHEMA_ATUAL, "danmachi", hash, "gemma", "en", "pt-BR");
    }

    private static EntradaCache ent(String original, String traduzido) {
        return new EntradaCache(0, "Default", original, traduzido, "en", "pt-BR");
    }

    private boolean existeArquivoContendo(String fragmento) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.anyMatch(p -> p.getFileName().toString().contains(fragmento));
        }
    }

    /**
     * Escreve um cache no formato OBJETO (com proveniência), controlando o JSON cru
     * da proveniência para exercitar schema 0 explícito e schema ausente — casos que
     * {@code svc.salvar} nunca produz porque sempre grava {@code SCHEMA_ATUAL}.
     */
    private void escreverCacheObjeto(Path f, String provenienciaJson) throws IOException {
        Files.writeString(f, "{\"proveniencia\":" + provenienciaJson
            + ",\"entradas\":[{\"indice\":0,\"estilo\":\"Default\",\"original\":\"Hi\","
            + "\"traduzido\":\"Oi\",\"idiomaOriginal\":\"en\",\"idiomaTraduzido\":\"pt-BR\"}]}");
    }

    @Test
    void salvarECarregarComMesmaProvenienciaReaproveita() {
        Path f = dir.resolve("ep.cache.json");
        svc.salvar(f, prov("h1"), List.of(ent("Hi", "Oi")));

        CacheTraducaoService.ResultadoCarga r = svc.carregar(f, prov("h1"));

        assertEquals(1, r.mapa().size());
        assertEquals("Oi", r.mapa().get("Hi"));
        assertEquals(0, r.invalidadas());
        assertFalse(r.migrado());
    }

    @Test
    void provenienciaDiferenteInvalidaEArquivaSemReutilizar() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        svc.salvar(f, prov("h1"), List.of(ent("Hi", "Oi"), ent("Bye", "Tchau")));

        CacheTraducaoService.ResultadoCarga r = svc.carregar(f, prov("h2")); // lore mudou

        assertTrue(r.mapa().isEmpty(), "cache de outro lore não pode ser reutilizado");
        assertEquals(2, r.invalidadas());
        assertTrue(Files.exists(f),
            "o cache ativo NÃO pode sair do lugar: a invalidação é em memória, e o arquivo só "
                + "será substituído quando a nova geração estiver inteira");
        assertTrue(existeArquivoContendo(".geracao_"), "geração anterior deve ser preservada");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: uma execução interrompida entre "invalidar por proveniência" e
     * "gravar a nova geração" NÃO pode deixar o episódio sem cache. Enquanto a nova geração
     * não existe, o cache ativo continua sendo o antigo, íntegro e reaproveitável pela
     * proveniência que o gerou — o operador não perde horas de LLM por um Ctrl-C.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code carregar} com proveniência divergente é uma decisão
     * de MEMÓRIA (mapa vazio + contagem de invalidadas) e não pode ter efeito destrutivo no
     * disco. O conteúdo do caminho ativo permanece byte a byte o mesmo até {@code salvar}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se este teste falhar, voltou a existir uma janela em
     * que o episódio fica sem cache — o defeito que apagou o S00E02 do 08th MS Team em
     * 2026-07-22, recuperado na mão a partir de {@code backups/traducao-cache}.
     */
    @Test
    void interrupcaoAposInvalidarPorProvenienciaPreservaOCacheAtivoIntacto() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        svc.salvar(f, prov("h1"), List.of(ent("Hi", "Oi"), ent("Bye", "Tchau")));
        String conteudoAntes = Files.readString(f);

        // Lore mudou: invalida em memória. A execução MORRE aqui (nenhum salvar depois).
        CacheTraducaoService.ResultadoCarga r = svc.carregar(f, prov("h2"));
        assertTrue(r.mapa().isEmpty());
        assertEquals(2, r.invalidadas());

        assertTrue(Files.exists(f), "o cache ativo não pode ter sumido com a execução abortada");
        assertEquals(conteudoAntes, Files.readString(f),
            "o cache ativo deve estar byte a byte igual: nada foi gravado depois da invalidação");

        // E ele continua servindo para quem tem a proveniência original — nada se perdeu.
        CacheTraducaoService.ResultadoCarga reaberto = svc.carregar(f, prov("h1"));
        assertEquals(2, reaberto.mapa().size(), "as duas falas traduzidas continuam recuperáveis");
        assertEquals("Oi", reaberto.mapa().get("Hi"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: concluída a retradução, a nova geração ocupa o caminho ativo de
     * uma vez só — é o único momento em que o conteúdo ativo troca.
     *
     * <p>INVARIANTES DO DOMÍNIO: depois de {@code salvar}, o ativo tem exatamente a geração
     * nova e a cópia datada da anterior continua ao lado para auditoria.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: cache ativo com conteúdo velho após a gravação, ou
     * cópia histórica ausente.
     */
    @Test
    void novaGeracaoSubstituiOAtivoSomenteAoSalvarEPreservaACopiaAnterior() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        svc.salvar(f, prov("h1"), List.of(ent("Hi", "Oi")));

        svc.carregar(f, prov("h2"));                       // invalida por lore nova
        svc.salvar(f, prov("h2"), List.of(ent("Hi", "Olá"))); // retradução termina

        CacheTraducaoService.ResultadoCarga r = svc.carregar(f, prov("h2"));
        assertEquals("Olá", r.mapa().get("Hi"), "o ativo passa a ser a geração nova");
        assertTrue(existeArquivoContendo(".geracao_"), "a geração anterior segue auditável ao lado");
    }

    @Test
    void schemaExplicitamenteDiferenteNaoReutilizaEArquiva() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        svc.salvar(f, prov("h1"), List.of(ent("Hi", "Oi"))); // grava SCHEMA_ATUAL
        // Só o schema difere (demais 5 campos iguais): app subiu de versão de schema.
        ProvenienciaCache schemaFuturo = new ProvenienciaCache(
            ProvenienciaCache.SCHEMA_ATUAL + 1, "danmachi", "h1", "gemma", "en", "pt-BR");

        CacheTraducaoService.ResultadoCarga r = svc.carregar(f, schemaFuturo);

        assertTrue(r.mapa().isEmpty(), "schema divergente não pode ser reutilizado");
        assertEquals(1, r.invalidadas());
        assertTrue(Files.exists(f), "o cache ativo permanece; a cópia da geração fica ao lado");
        assertTrue(existeArquivoContendo(".geracao_"));
    }

    @Test
    void schemaZeroExplicitoNaoReutilizaEArquiva() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        // Só o schema difere (0 vs SCHEMA_ATUAL); os outros 5 campos batem com prov("h1").
        escreverCacheObjeto(f, "{\"schemaVersion\":0,\"contextoId\":\"danmachi\",\"contextoHash\":\"h1\","
            + "\"modeloLlm\":\"gemma\",\"idiomaOrigem\":\"en\",\"idiomaDestino\":\"pt-BR\"}");

        CacheTraducaoService.ResultadoCarga r = svc.carregar(f, prov("h1"));

        assertTrue(r.mapa().isEmpty(), "schema 0 não é normalizado para atual");
        assertEquals(1, r.invalidadas());
        assertTrue(Files.exists(f), "o cache ativo permanece; a cópia da geração fica ao lado");
        assertTrue(existeArquivoContendo(".geracao_"));
    }

    @Test
    void provenienciaSemSchemaVersionNaoReutilizaEArquivaSemTratarComoListaLegada() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        // Objeto com proveniência mas SEM o campo schemaVersion → materializa 0 → incompatível.
        escreverCacheObjeto(f, "{\"contextoId\":\"danmachi\",\"contextoHash\":\"h1\","
            + "\"modeloLlm\":\"gemma\",\"idiomaOrigem\":\"en\",\"idiomaDestino\":\"pt-BR\"}");

        CacheTraducaoService.ResultadoCarga r = svc.carregar(f, prov("h1"));

        assertTrue(r.mapa().isEmpty(), "objeto sem schema não é reutilizado");
        assertEquals(1, r.invalidadas());
        assertFalse(r.migrado(), "objeto sem schema NÃO é a lista pura legada — são formatos diferentes");
        assertTrue(Files.exists(f), "o cache ativo permanece; a cópia da geração fica ao lado");
        assertTrue(existeArquivoContendo(".geracao_"));
    }

    @Test
    void formatoAntigoListaPuraEhMigradoAssumindoCompativel() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        // Formato legado: lista pura de EntradaCache, sem cabeçalho de proveniência.
        new ObjectMapper().writeValue(f.toFile(), List.of(ent("Hi", "Oi")));

        CacheTraducaoService.ResultadoCarga r = svc.carregar(f, prov("h1"));

        assertEquals(1, r.mapa().size());
        assertEquals("Oi", r.mapa().get("Hi"));
        assertEquals(0, r.invalidadas());
        assertTrue(r.migrado());
    }

    @Test
    void cacheCorrompidoEhPreservadoENaoSobrescrito() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        Files.writeString(f, "{ isto nao e json valido ");

        CacheTraducaoService.ResultadoCarga r = svc.carregar(f, prov("h1"));

        assertTrue(r.mapa().isEmpty());
        assertFalse(Files.exists(f), "corrompido deve ser movido, não lido como vazio");
        assertTrue(existeArquivoContendo(".corrompido_"));
    }

    @Test
    void arquivoInexistenteRetornaVazioSemErro() {
        CacheTraducaoService.ResultadoCarga r = svc.carregar(dir.resolve("nao_existe.cache.json"), prov("h1"));
        assertTrue(r.mapa().isEmpty());
        assertEquals(0, r.invalidadas());
        assertFalse(r.migrado());
    }

    @Test
    void metodoAntigoListaPuraSegueFuncionandoParaOsFluxosNaoVersionados() {
        Path f = dir.resolve("karaoke.cache.json");
        svc.salvar(f, List.of(ent("Hi", "Oi")));
        assertEquals("Oi", svc.carregar(f).get("Hi"));
    }

    // ------------------------------------------------------------------------------------------
    // LENTE DE BOA-FÉ (regra 15): os testes acima provam que o arquivo .geracao_ é CRIADO. Nenhum
    // prova que ele SERVE. A promessa feita ao operador — "o trabalho daquela geração continua
    // recuperável" — só é verdadeira se o arquivo voltar a ser um cache carregável, e é nisso que
    // ele vai confiar depois de horas de LLM.
    // ------------------------------------------------------------------------------------------

    /**
     * PROPÓSITO DE NEGÓCIO: o arquivo de geração é um BACKUP de verdade, não um despejo. Recolocado
     * no caminho ativo, ele volta a servir para a proveniência que o gerou, com as mesmas falas.
     *
     * <p>É a pergunta de boa-fé aplicada ao cache: o operador troca o modelo sem perceber, perde a
     * geração anterior do caminho ativo, e vai buscar o {@code .geracao_}. Se o que ele encontrar
     * for ilegível, truncado ou sem proveniência, a promessa do Javadoc de {@code arquivarGeracao}
     * é falsa — e ele só descobre no pior momento possível.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: reprova se o arquivamento gravar algo que o próprio
     * serviço não consegue reler, ou se as falas não voltarem.
     */
    @Test
    void oArquivoDeGeracaoVOLTAaSERvirComoCache() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        svc.salvar(f, prov("h1"), List.of(ent("Hi", "Oi"), ent("Bye", "Tchau")));

        svc.carregar(f, prov("h2"));                        // troca de lore: arquiva a geração h1
        svc.salvar(f, prov("h2"), List.of(ent("Hi", "Olá"))); // a retradução conclui e ocupa o ativo

        Path arquivada;
        try (Stream<Path> s = Files.list(dir)) {
            arquivada = s.filter(p -> p.getFileName().toString().contains(".geracao_"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("nenhuma geração foi arquivada"));
        }

        // O operador faz o que qualquer um faria: devolve a cópia ao caminho ativo.
        Path restaurado = dir.resolve("restaurado.cache.json");
        Files.copy(arquivada, restaurado);
        CacheTraducaoService.ResultadoCarga r = svc.carregar(restaurado, prov("h1"));

        assertEquals(2, r.mapa().size(),
            "O BACKUP NÃO SERVE. O .geracao_ foi criado, mas recolocado no lugar não devolve as "
                + "falas — a promessa de 'continua recuperável' seria falsa justamente para quem "
                + "acabou de perder horas de LLM.");
        assertEquals("Oi", r.mapa().get("Hi"), "a tradução da geração antiga tem de voltar íntegra");
        assertEquals("Tchau", r.mapa().get("Bye"));
        assertEquals(0, r.invalidadas(),
            "recolocado sob a proveniência que o gerou, o arquivo é reuso legítimo, não invalidação");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o arquivamento não pode COMER a geração ativa. Se a cópia falhasse e o
     * fluxo seguisse, o {@code salvar} seguinte sobrescreveria o ativo e a geração anterior sumiria
     * — este teste fixa que, no caminho normal, o ativo e a cópia coexistem com conteúdos DISTINTOS.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se os dois tiverem o mesmo conteúdo, ou o ativo ainda for o
     * antigo, a troca de gerações deixou de ser transacional.
     */
    @Test
    void aCopiaEOAtivoCoexistemComConteudosDIFERENTES() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        svc.salvar(f, prov("h1"), List.of(ent("Hi", "Oi")));

        svc.carregar(f, prov("h2"));
        svc.salvar(f, prov("h2"), List.of(ent("Hi", "Olá")));

        Path arquivada;
        try (Stream<Path> s = Files.list(dir)) {
            arquivada = s.filter(p -> p.getFileName().toString().contains(".geracao_"))
                .findFirst().orElseThrow(() -> new AssertionError("nenhuma geração arquivada"));
        }

        assertTrue(Files.readString(f).contains("Olá"), "o ativo tem de ser a geração NOVA");
        assertTrue(Files.readString(arquivada).contains("Oi"),
            "a cópia tem de ser a geração ANTIGA — se trouxer 'Olá', o arquivamento aconteceu "
                + "depois da sobrescrita e não guardou nada de útil");
        assertFalse(Files.readString(arquivada).contains("Olá"),
            "a cópia não pode conter a geração nova");
    }

    // ------------------------------------------------------------------------------------------
    // REUSO ENTRE MODELOS (12/08/2026, pedido do Paulo). Exercitar 6 falas pendentes do Zeta
    // custava retraduzir 17.090 — só porque o titular mudou. O reuso torna o experimento viável;
    // o preço é o cache DIZER que herdou, para não mentir sobre quem traduziu o quê.
    // ------------------------------------------------------------------------------------------

    private static ProvenienciaCache provComModelo(String hash, String modelo) {
        return new ProvenienciaCache(ProvenienciaCache.SCHEMA_ATUAL, "danmachi", hash, modelo,
            "en", "pt-BR");
    }

    /**
     * O CASO-CONTROLE MAIS IMPORTANTE desta mudança: o acervo já gravado — 94.721 falas em
     * 12/08/2026, todas sem o campo {@code modeloHerdado} — tem de continuar batendo. Se
     * {@code mesmaProveniencia} passasse a comparar o campo novo, o campo invalidaria o acervo
     * inteiro no instante em que nasceu.
     */
    @Test
    void oCampoNOVOnaoInvalidaOacervoJAgravado() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        svc.salvar(f, prov("h1"), List.of(ent("Hi", "Oi")));

        assertFalse(Files.readString(f).contains("modeloHerdado"),
            "o ARQUIVO do caso normal não pode ganhar o campo novo nem como nulo — é o que o mixin "
                + "NON_NULL garante, e o que mantém os .cache.json byte a byte como sempre foram");
        assertEquals(1, svc.carregar(f, prov("h1")).mapa().size(),
            "cache gravado sem modeloHerdado tem de continuar reaproveitável pela forma de 6 campos");

        // E a herança declarada não muda a identidade de reuso: o cache do experimento continua
        // servindo à execução seguinte do MESMO modelo.
        svc.salvar(f, prov("h1").herdandoDe("mistral-nemo"), List.of(ent("Hi", "Oi")));
        assertEquals(1, svc.carregar(f, prov("h1")).mapa().size(),
            "um cache herdado não pode virar inutilizável para quem o gerou");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: com o reuso autorizado, trocar o modelo reaproveita o trabalho e só o
     * que faltar vai ao LLM — que é o que torna comparar modelos numa obra grande viável.
     */
    @Test
    void reusoAutorizadoHerdaAsTraducoesDoOutroModelo() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        svc.salvar(f, provComModelo("h1", "mistral-nemo"), List.of(ent("Hi", "Oi"), ent("Bye", "Tchau")));

        CacheTraducaoService.ResultadoCarga r =
            svc.carregar(f, provComModelo("h1", "aya-expanse-8b"), true);

        assertEquals(2, r.mapa().size(),
            "REUSO NÃO ACONTECEU: com lore e prompt idênticos e só o modelo diferente, as falas "
                + "do modelo anterior deveriam ser reaproveitadas");
        assertEquals("Oi", r.mapa().get("Hi"));
        assertEquals(0, r.invalidadas(), "reaproveitado não é invalidado");
        assertTrue(r.herdouDeOutroModelo(), "quem salvar precisa saber que herdou");
        assertEquals("mistral-nemo", r.modeloHerdado());
        assertTrue(existeArquivoContendo(".geracao_"),
            "reuso NÃO dispensa o histórico: a geração anterior continua arquivada");
    }

    /**
     * O preço do reuso, e o que impede a proveniência de mentir: o cache resultante declara de quem
     * herdou. Sem isto, o arquivo afirmaria que a aya traduziu o que o mistral traduziu — e é a
     * proveniência que sustenta a comparação entre modelos feita no Unicorn em 12/08.
     */
    @Test
    void oCacheHerdadoDECLARAdeQuemHerdou() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        svc.salvar(f, provComModelo("h1", "mistral-nemo"), List.of(ent("Hi", "Oi")));

        ProvenienciaCache aya = provComModelo("h1", "aya-expanse-8b");
        CacheTraducaoService.ResultadoCarga r = svc.carregar(f, aya, true);
        svc.salvar(f, aya.herdandoDe(r.modeloHerdado()), List.of(ent("Hi", "Oi")));

        String gravado = Files.readString(f);
        assertTrue(gravado.contains("mistral-nemo"),
            "O CACHE ESTÁ MENTINDO: herdou do mistral e não registrou. Uma auditoria futura leria "
                + "estas falas como produzidas pela aya.\n" + gravado);
        assertTrue(gravado.contains("aya-expanse-8b"), "o modelo atual continua sendo o carimbo principal");
    }

    /** FALHA FECHADA: sem autorização explícita, o comportamento é o de sempre — invalida. */
    @Test
    void semAutorizacaoOreusoEntreModelosNAOacontece() {
        Path f = dir.resolve("ep.cache.json");
        svc.salvar(f, provComModelo("h1", "mistral-nemo"), List.of(ent("Hi", "Oi")));

        CacheTraducaoService.ResultadoCarga r = svc.carregar(f, provComModelo("h1", "aya-expanse-8b"));

        assertTrue(r.mapa().isEmpty(),
            "reuso entre modelos não pode acontecer por omissão — o default é falha fechada");
        assertEquals(1, r.invalidadas());
        assertFalse(r.herdouDeOutroModelo());
    }

    /**
     * O LIMITE do reuso: LORE diferente continua invalidando mesmo com a autorização ligada. A
     * tradução muda com o prompt, e reaproveitá-la seria servir texto de outra obra — o dano que a
     * proveniência existe para impedir.
     */
    @Test
    void mesmoAutorizadoLOREdiferenteContinuaInvalidando() {
        Path f = dir.resolve("ep.cache.json");
        svc.salvar(f, provComModelo("h1", "mistral-nemo"), List.of(ent("Hi", "Oi")));

        CacheTraducaoService.ResultadoCarga r =
            svc.carregar(f, provComModelo("h2", "aya-expanse-8b"), true);

        assertTrue(r.mapa().isEmpty(),
            "a autorização vale só para o MODELO; lore nova exige retradução, senão a legenda sai "
                + "com a terminologia da obra errada");
        assertEquals(1, r.invalidadas());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: trocar o modelo e VOLTAR ATRÁS é o arrependimento mais comum de quem
     * mexe no LM Studio. Enquanto a retradução não concluiu, voltar à proveniência original tem de
     * devolver o cache inteiro — sem custo, sem perda, sem precisar do backup.
     *
     * <p>Complementa {@code interrupcaoAposInvalidarPorProvenienciaPreservaOCacheAtivoIntacto}: lá
     * a execução morre; aqui ela continua, e o operador simplesmente corrige o modelo.
     */
    @Test
    void trocarOModeloEVoltarAtrasNaoCustaNada() {
        Path f = dir.resolve("ep.cache.json");
        ProvenienciaCache comGemma = prov("h1");
        ProvenienciaCache comAya = new ProvenienciaCache(
            ProvenienciaCache.SCHEMA_ATUAL, "danmachi", "h1", "aya-expanse-8b", "en", "pt-BR");
        svc.salvar(f, comGemma, List.of(ent("Hi", "Oi"), ent("Bye", "Tchau")));

        CacheTraducaoService.ResultadoCarga trocado = svc.carregar(f, comAya);
        assertTrue(trocado.mapa().isEmpty(), "modelo diferente não reaproveita — é o invariante");
        assertEquals(2, trocado.invalidadas());

        CacheTraducaoService.ResultadoCarga voltou = svc.carregar(f, comGemma);
        assertEquals(2, voltou.mapa().size(),
            "voltar ao modelo original tem de devolver o cache inteiro: enquanto nada foi salvo, "
                + "nenhuma tradução se perdeu e o operador não paga pelo arrependimento");
        assertEquals(0, voltou.invalidadas());
    }
}
