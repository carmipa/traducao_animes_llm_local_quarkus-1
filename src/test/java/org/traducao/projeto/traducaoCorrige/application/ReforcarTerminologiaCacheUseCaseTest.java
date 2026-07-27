package org.traducao.projeto.traducaoCorrige.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.cachetraducao.infrastructure.CacheManutencaoService;
import org.traducao.projeto.contexto.application.ValidadorCompatibilidadeObraContexto;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.qualidadeTraducao.application.EnforcadorTermosLore;
import org.traducao.projeto.traducaoCorrige.domain.EntradaAuditoriaCorrecaoCache;
import org.traducao.projeto.traducaoCorrige.domain.ResultadoReforcoTerminologia;
import org.traducao.projeto.traducaoCorrige.domain.ports.AuditoriaCorrecaoCachePort;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova a FASE 5 do Plano-Mestre do corretor — aplicar a terminologia
 * oficial da obra ao cache JÁ GRAVADO, sem retraduzir. Antes disso, levar uma regra nova de
 * terminologia ao acervo custava ~4h de GPU por obra; o ganho medido de {@code Beam Saber}
 * (0% → 100%) só existiu porque houve retradução completa.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>ENSAIO é previsão fiel: percorre e conta exatamente o que a execução faria, e o arquivo
 *       fica byte a byte igual. Um ensaio que contasse diferente da execução seria pior que não
 *       ter ensaio.</li>
 *   <li>O contador é por termo CANÔNICO e é o que separa "o modelo acertou" de "o enforcer
 *       consertou" — distinção que não existia porque o reforço roda antes da gravação.</li>
 *   <li>A guarda obra×contexto da FASE 1 continua valendo neste caminho: cache carimbado com a
 *       lore de outra obra é RECUSADO, não reescrito com a terminologia errada.</li>
 *   <li>Nunca degrada: fala cujo original inglês não sustenta o termo não é tocada.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falha aqui significa que a correção de acervo mudaria bytes que não devia — o defeito mais
 * caro possível, porque age sobre trabalho já pronto e em lote.
 */
@DisplayName("ReforcarTerminologiaCacheUseCase: terminologia no acervo, sem retraduzir")
class ReforcarTerminologiaCacheUseCaseTest {

    @TempDir
    Path temp;

    private final ObjectMapper mapper = new ObjectMapper();
    private AuditoriaStub auditoria;
    private ReforcarTerminologiaCacheUseCase useCase;

    @BeforeEach
    void preparar() {
        GerenciadorContexto contexto = new GerenciadorContexto(List.of(
            new ContextoZZ(), new ContextoGuiltyCrown()));
        auditoria = new AuditoriaStub();
        useCase = new ReforcarTerminologiaCacheUseCase(
            new CacheServiceTeste(mapper, temp.resolve("backups")),
            new ContextoManutencaoCacheService(contexto, new ValidadorCompatibilidadeObraContexto()),
            new EnforcadorTermosLore(),
            auditoria);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o ensaio precisa medir sem escrever — é assim que o operador decide
     * se autoriza a mudança no acervo.
     */
    @Test
    @DisplayName("ensaio conta as restaurações e não altera um byte do arquivo")
    void ensaioContaSemEscrever() throws Exception {
        Path cache = escreverCacheZZ();
        String antes = Files.readString(cache);

        ResultadoReforcoTerminologia r = useCase.ensaiar(temp.resolve("cache"), null);

        assertEquals(antes, Files.readString(cache), "ENSAIO não pode tocar o disco");
        assertFalse(r.aplicado());
        assertEquals("ENSAIO_COM_PENDENCIAS", r.status());
        assertEquals(0, r.arquivosAlterados(), "nenhum arquivo é alterado em ensaio");
        assertEquals(4, r.falasAlteradas());
        assertEquals(Map.of("Beam Saber", 2, "Beam Sabers", 1, "Axis", 1), r.restauracoesPorTermo());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: prende o defeito que este contador teve na primeira versão. Contar
     * ocorrência literal é simples e não duplica a regra de fronteira do enforcer, mas
     * {@code "Beam Sabers"} CONTÉM {@code "Beam Saber"} — e os mapas de lore são cheios de pares
     * singular/plural ({@code Mobile Suit}/{@code Mobile Suits}, {@code Newtype}/{@code Newtypes}).
     * Sem atribuir cada trecho ao termo mais longo, toda restauração de plural creditava também o
     * singular, inflando justamente o termo mais frequente. Num relatório cuja única razão de
     * existir é ser um número honesto, esse é o pior defeito possível.
     */
    @Test
    @DisplayName("plural NÃO infla o singular: cada trecho é creditado a um único termo")
    void pluralNaoInflaOSingular() throws Exception {
        escreverCacheZZ();

        var porTermo = useCase.ensaiar(temp.resolve("cache"), null).restauracoesPorTermo();

        assertEquals(1, porTermo.get("Beam Sabers"));
        assertEquals(2, porTermo.get("Beam Saber"),
            "só as DUAS falas de singular contam aqui — a de plural pertence a \"Beam Sabers\"");
        assertEquals(3, porTermo.get("Beam Saber") + porTermo.get("Beam Sabers"),
            "o total das duas famílias tem de bater com as 3 falas de sabre");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a execução real escreve, e os números têm de ser IDÊNTICOS aos do
     * ensaio — senão o ensaio não serve para decidir.
     */
    @Test
    @DisplayName("execução aplica no disco com exatamente os números que o ensaio previu")
    void execucaoConfereComOEnsaio() throws Exception {
        Path cache = escreverCacheZZ();
        ResultadoReforcoTerminologia ensaio = useCase.ensaiar(temp.resolve("cache"), null);

        ResultadoReforcoTerminologia real = useCase.executar(temp.resolve("cache"), null, true);

        assertEquals(ensaio.restauracoesPorTermo(), real.restauracoesPorTermo(),
            "o ensaio deixaria de ser uma previsão se divergisse da execução");
        assertEquals(ensaio.falasAlteradas(), real.falasAlteradas());
        assertTrue(real.aplicado());
        assertEquals(1, real.arquivosAlterados());
        assertEquals("CONCLUIDO", real.status());

        var salvo = mapper.readTree(cache.toFile()).path("entradas");
        assertEquals("Ative o Beam Saber!", salvo.get(0).path("traduzido").asText());
        assertEquals("Saque sua Beam Saber, Judau!", salvo.get(1).path("traduzido").asText());
        // "Duas", não "Dois": o enforcer troca o TERMO, não reescreve a frase. A concordância do
        // artigo com o termo em inglês está FORA do escopo dele — é assunto do corretor de
        // concordância, e fingir aqui que ele resolve esconderia a limitação real.
        assertEquals("Duas Beam Sabers cruzaram.", salvo.get(2).path("traduzido").asText());
        assertEquals("A frota vai atacar o Axis.", salvo.get(3).path("traduzido").asText());
        assertEquals("Bom dia, Judau.", salvo.get(4).path("traduzido").asText(),
            "fala sem forma-ruim não pode ser tocada");
        assertTrue(Files.walk(temp.resolve("backups")).anyMatch(Files::isRegularFile),
            "escrita no acervo exige backup");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o teto do enforcer vale aqui — a fala cujo inglês não traz o termo
     * canônico não pode ser tocada, senão a correção de acervo vira corrupção em lote.
     */
    @Test
    @DisplayName("fala sem o canônico no inglês continua intacta")
    void naoTocaFalaSemCanonicoNoIngles() throws Exception {
        Path cache = temp.resolve("cache/Mobile Suit Gundam ZZ/ep02.cache.json");
        Files.createDirectories(cache.getParent());
        Files.writeString(cache, """
            {"proveniencia":{"schemaVersion":1,"contextoId":"gundam_zz","contextoHash":"h","modeloLlm":"m","idiomaOrigem":"en","idiomaDestino":"pt-br"},
             "entradas":[
               {"indice":1,"estilo":"Default","original":"The truck broke its rear axle.","traduzido":"O caminhão quebrou o eixo traseiro."}
             ]}
            """);
        String antes = Files.readString(cache);

        ResultadoReforcoTerminologia r = useCase.executar(temp.resolve("cache"), null, true);

        assertEquals(antes, Files.readString(cache), "\"eixo\" de caminhão não é o Axis da lore");
        assertEquals(0, r.falasAlteradas());
        assertEquals("CONCLUIDO_SEM_ALTERACOES", r.status());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a guarda da FASE 1 continua ativa neste caminho. Sem ela, um cache de
     * ZZ carimbado como Guilty Crown seria "corrigido" com a terminologia da obra errada — o dano
     * que a guarda existe para impedir, agora aplicado em lote sobre o acervo.
     */
    @Test
    @DisplayName("cache com carimbo de outra obra é RECUSADO, não corrigido com a lore errada")
    void cacheComCarimboDeOutraObraEhRecusado() throws Exception {
        Path cache = temp.resolve("cache/Mobile Suit Gundam ZZ/ep03.cache.json");
        Files.createDirectories(cache.getParent());
        Files.writeString(cache, """
            {"proveniencia":{"schemaVersion":1,"contextoId":"guilty_crown","contextoHash":"h","modeloLlm":"m","idiomaOrigem":"en","idiomaDestino":"pt-br"},
             "entradas":[
               {"indice":1,"estilo":"Default","original":"Activate the Beam Saber!","traduzido":"Ative o Sabre de Raio!"}
             ]}
            """);
        String antes = Files.readString(cache);

        ResultadoReforcoTerminologia r = useCase.executar(temp.resolve("cache"), null, true);

        assertEquals(antes, Files.readString(cache));
        assertEquals(1, r.falhas());
        assertEquals("CONCLUIDO_COM_FALHAS", r.status());
        assertTrue(auditoria.entradas.stream().anyMatch(e -> "FALHA_ARQUIVO".equals(e.resultado())),
            "a recusa precisa ficar auditada, não sumir em silêncio");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: cache legado (sem carimbo), em pasta que nenhuma lore reconhece, com a
     * obra escolhida à mão na tela: nenhuma testemunha sustenta a lore. A guarda obra×contexto
     * deixa passar de propósito — falhar fechado ali pararia obra sem vocabulário declarado —, mas
     * reescrever terminologia sobre um palpite é o mesmo dano que a guarda existe para impedir, só
     * que em lote e sobre trabalho já pronto.
     *
     * <p>O cenário é o de uma obra cuja pasta ainda não é reconhecida por lore nenhuma — obra nova,
     * ou pasta com nome que o catálogo não reivindica — guardando cache antigo, de antes da
     * proveniência existir. NÃO é o caso de {@code cache/karaoke/}: aquelas pastas já são excluídas
     * pelo peer {@code cachetraducao} em {@code estaEmSubpastaAuxiliar}, e nunca chegam aqui.
     */
    @Test
    @DisplayName("cache legado em pasta irreconhecível é PULADO, e o status não finge cobertura")
    void cacheSemTestemunhaDaObraEhPulado() throws Exception {
        Path cache = temp.resolve("cache/Serie Que Ninguem Declarou/ep01.cache.json");
        Files.createDirectories(cache.getParent());
        // Formato LEGADO: array cru, sem envelope de proveniência.
        Files.writeString(cache, """
            [{"indice":1,"estilo":"Default","original":"Activate the Beam Saber!","traduzido":"Ative o Sabre de Raio!"}]
            """);
        String antes = Files.readString(cache);

        // O operador escolheu ZZ na tela. A pasta "karaoke" nao e reconhecida por lore nenhuma.
        ResultadoReforcoTerminologia r = useCase.executar(temp.resolve("cache"), "gundam_zz", true);

        assertEquals(antes, Files.readString(cache),
            "sem testemunha da obra, a terminologia da tela NAO pode ser imposta ao acervo");
        assertEquals(1, r.arquivosNaoVerificaveis());
        assertEquals(0, r.falasAlteradas());
        assertEquals(0, r.falhas(), "pular NAO e falha: o arquivo esta intacto e a decisao foi correta");
        assertEquals("CONCLUIDO_COM_PULADOS", r.status(),
            "um lote que nao cobriu parte do acervo nao pode se apresentar igual a um que cobriu tudo");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a contrapartida — cache legado numa pasta que a lore RECONHECE tem
     * testemunha (o caminho), então segue normalmente. Sem isto, o conserto acima viraria um
     * bloqueio geral de todo cache legado.
     */
    @Test
    @DisplayName("cache legado em pasta RECONHECIDA segue: o caminho é testemunha suficiente")
    void cacheLegadoEmPastaReconhecidaSegue() throws Exception {
        Path cache = temp.resolve("cache/Mobile Suit Gundam ZZ/ep09.cache.json");
        Files.createDirectories(cache.getParent());
        Files.writeString(cache, """
            [{"indice":1,"estilo":"Default","original":"Activate the Beam Saber!","traduzido":"Ative o Sabre de Raio!"}]
            """);

        ResultadoReforcoTerminologia r = useCase.executar(temp.resolve("cache"), "gundam_zz", true);

        assertEquals(0, r.arquivosNaoVerificaveis());
        assertEquals(1, r.falasAlteradas());
        assertEquals("Ative o Beam Saber!",
            mapper.readTree(cache.toFile()).get(0).path("traduzido").asText());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: prende o defeito mais grave que este relatório já teve. O contador
     * era deduzido comparando ocorrências do canônico antes e depois — uma segunda implementação
     * da regra, feita fora de quem a executa. Quando a forma-ruim CONTÉM o canônico, o canônico
     * já estava lá antes da troca, o delta dava zero, e o relatório saía vazio TENDO REESCRITO O
     * ARQUIVO: {@code CONCLUIDO_SEM_ALTERACOES} com {@code arquivosAlterados == 1}.
     *
     * <p>Não é hipótese: dos 205 pares do catálogo de lore, três disparam isto, em três obras
     * distintas — {@code "Plee"→"Ple"} (ZZ), {@code "Gouf Customizado"→"Gouf Custom"} (08th MS
     * Team) e {@code "Gundam Alexandre"→"Gundam Alex"} (War in the Pocket).
     *
     * <p>Hoje a contagem vem do próprio {@code EnforcadorTermosLore}, que sabe quantas
     * substituições fez. Quem executa é quem conta.
     */
    @Test
    @DisplayName("forma-ruim que CONTÉM o canônico é creditada — e o status não diz 'sem alterações'")
    void formaRuimQueContemOCanonicoEhCreditada() throws Exception {
        Path cache = temp.resolve("cache/Mobile Suit Gundam ZZ/ep05.cache.json");
        Files.createDirectories(cache.getParent());
        Files.writeString(cache, """
            {"proveniencia":{"schemaVersion":1,"contextoId":"gundam_zz","contextoHash":"h","modeloLlm":"m","idiomaOrigem":"en","idiomaDestino":"pt-br"},
             "entradas":[
               {"indice":1,"estilo":"Default","original":"Ple, are you all right?","traduzido":"Plee, você está bem?"}
             ]}
            """);

        ResultadoReforcoTerminologia r = useCase.executar(temp.resolve("cache"), null, true);

        assertEquals("Ple, você está bem?",
            mapper.readTree(cache.toFile()).path("entradas").get(0).path("traduzido").asText());
        assertEquals(Map.of("Ple", 1), r.restauracoesPorTermo(),
            "\"Plee\" contém \"Ple\": o contador antigo via delta zero e não creditava nada");
        assertEquals("CONCLUIDO", r.status(),
            "reescreveu o arquivo — não pode se declarar CONCLUIDO_SEM_ALTERACOES");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: se a gravação falha, o disco fica intacto — e o relatório tem de
     * ficar intacto junto. Creditar antes de salvar fazia o lote anunciar restaurações que nunca
     * existiram no acervo, e gravar linhas de auditoria {@code TERMINOLOGIA_REFORCADA} para falas
     * que continuavam com a forma-ruim no arquivo.
     */
    @Test
    @DisplayName("gravação que falha NÃO credita restaurações nem audita como reforçada")
    void gravacaoQueFalhaNaoCredita() throws Exception {
        Path cache = escreverCacheZZ();
        String antes = Files.readString(cache);
        var useCaseQueFalhaAoSalvar = new ReforcarTerminologiaCacheUseCase(
            new CacheServiceQueFalhaAoSalvar(mapper, temp.resolve("backups")),
            new ContextoManutencaoCacheService(
                new GerenciadorContexto(List.of(new ContextoZZ(), new ContextoGuiltyCrown())),
                new ValidadorCompatibilidadeObraContexto()),
            new EnforcadorTermosLore(), auditoria);

        ResultadoReforcoTerminologia r = useCaseQueFalhaAoSalvar.executar(temp.resolve("cache"), null, true);

        assertEquals(antes, Files.readString(cache), "o disco tem de ficar intacto");
        assertEquals(1, r.falhas());
        assertEquals(0, r.falasAlteradas(), "nada chegou ao disco: nada pode ser creditado");
        assertEquals(Map.of(), r.restauracoesPorTermo());
        assertTrue(auditoria.entradas.stream().noneMatch(e -> "TERMINOLOGIA_REFORCADA".equals(e.resultado())),
            "TERMINOLOGIA_REFORCADA tem de significar \"chegou ao disco\"");
        assertTrue(auditoria.entradas.stream().anyMatch(e -> "FALHA_ARQUIVO".equals(e.resultado())));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o contador é indexado pelo termo CANÔNICO porque várias formas-ruim
     * convergem para o mesmo termo oficial, e o que se mede é a conformidade do termo.
     */
    @Test
    @DisplayName("formas-ruim diferentes creditam o MESMO termo canônico")
    void formasRuimDiferentesCreditamOMesmoCanonico() throws Exception {
        escreverCacheZZ();

        var r = useCase.ensaiar(temp.resolve("cache"), null);

        assertEquals(2, r.restauracoesPorTermo().get("Beam Saber"),
            "\"Sabre de Raio\" e \"Espada de Raio\" são a mesma coisa oficialmente");
        assertEquals(List.of("Beam Saber", "Axis", "Beam Sabers"),
            List.copyOf(r.porFrequencia().keySet()),
            "o relatório sai do termo mais restaurado para o menos, com empate por nome");
    }

    /**
     * Cache de ZZ com os quatro casos que importam: duas formas-ruim distintas convergindo para
     * {@code Beam Saber}, uma para o PLURAL {@code Beam Sabers}, uma para {@code Axis}, e uma
     * fala já correta que não pode ser tocada.
     */
    private Path escreverCacheZZ() throws Exception {
        Path cache = temp.resolve("cache/Mobile Suit Gundam ZZ/ep01.cache.json");
        Files.createDirectories(cache.getParent());
        Files.writeString(cache, """
            {"proveniencia":{"schemaVersion":1,"contextoId":"gundam_zz","contextoHash":"h","modeloLlm":"m","idiomaOrigem":"en","idiomaDestino":"pt-br"},
             "entradas":[
               {"indice":1,"estilo":"Default","original":"Activate the Beam Saber!","traduzido":"Ative o Sabre de Raio!"},
               {"indice":2,"estilo":"Default","original":"Draw your Beam Saber, Judau!","traduzido":"Saque sua Espada de Raio, Judau!"},
               {"indice":3,"estilo":"Default","original":"Two Beam Sabers clashed.","traduzido":"Duas Espadas de Raio cruzaram."},
               {"indice":4,"estilo":"Default","original":"The fleet will attack Axis.","traduzido":"A frota vai atacar o Eixo."},
               {"indice":5,"estilo":"Default","original":"Good morning, Judau.","traduzido":"Bom dia, Judau."}
             ]}
            """);
        return cache;
    }

    /** Cache de teste que redireciona backups para o diretório temporário. */
    private static final class CacheServiceTeste extends CacheManutencaoService {
        private final Path backup;

        CacheServiceTeste(ObjectMapper mapper, Path backup) {
            super(mapper);
            this.backup = backup;
        }

        @Override
        public Sessao iniciarSessao(Path raizCache, String operacao) {
            return new Sessao(raizCache.toAbsolutePath().normalize(),
                backup.toAbsolutePath().normalize(), operacao);
        }
    }

    /** Cache cuja gravação atômica sempre falha, para provar que nada é creditado sem disco. */
    private static final class CacheServiceQueFalhaAoSalvar extends CacheManutencaoService {
        private final Path backup;

        CacheServiceQueFalhaAoSalvar(ObjectMapper mapper, Path backup) {
            super(mapper);
            this.backup = backup;
        }

        @Override
        public Sessao iniciarSessao(Path raizCache, String operacao) {
            return new Sessao(raizCache.toAbsolutePath().normalize(),
                backup.toAbsolutePath().normalize(), operacao);
        }

        @Override
        public Path salvarAtomico(DocumentoEditavel documento, Sessao sessao) throws java.io.IOException {
            throw new java.io.IOException("There is not enough space on the disk");
        }
    }

    /** Auditoria em memória para não escrever artefatos do teste no projeto. */
    private static final class AuditoriaStub implements AuditoriaCorrecaoCachePort {
        private final List<EntradaAuditoriaCorrecaoCache> entradas = new ArrayList<>();

        @Override
        public void registrar(EntradaAuditoriaCorrecaoCache entrada) {
            entradas.add(entrada);
        }
    }

    private record ContextoZZ() implements ProvedorContexto {
        @Override public String getId() { return "gundam_zz"; }
        @Override public String getNomeExibicao() { return "Mobile Suit Gundam ZZ"; }
        @Override public String obterPromptSistema() { return "lore de ZZ"; }
        @Override public Map<String, String> correcoesTerminologia() {
            return Map.of(
                "Sabre de Raio", "Beam Saber",
                "Espada de Raio", "Beam Saber",
                "Espadas de Raio", "Beam Sabers",
                "Eixo", "Axis",
                // Par REAL de CorrecoesTerminologiaGundamZz: a forma-ruim CONTÉM o canônico.
                // É o caso que fazia o contador antigo devolver zero.
                "Plee", "Ple");
        }
    }

    private record ContextoGuiltyCrown() implements ProvedorContexto {
        @Override public String getId() { return "guilty_crown"; }
        @Override public String getNomeExibicao() { return "Guilty Crown"; }
        @Override public String obterPromptSistema() { return "lore de GC"; }
    }
}
