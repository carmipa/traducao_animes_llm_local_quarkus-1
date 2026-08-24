package org.traducao.projeto.revisaoConcordancia.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;
import org.traducao.projeto.core.texto.gramatica.RevisorGramaticalMudo;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.revisaoConcordancia.domain.ContagemCorretor;
import org.traducao.projeto.revisaoConcordancia.domain.ResultadoConcordancia;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: provar que a cadeia de corretores <b>presta contas</b> — quem agiu, quem
 * se absteve, quem falhou e quem sequer pôde rodar.
 *
 * <h2>Os dois enganos que estes casos impedem</h2>
 * <ul>
 *   <li><b>Exceção engolida.</b> Um corretor que lança numa fala não pode derrubar o arquivo nem
 *       — pior — fazer a fala sumir sem deixar rastro. Ela volta como estava, o contador sobe, e
 *       o número aparece no relatório, não só no log.</li>
 *   <li><b>Zero ambíguo.</b> {@code agiu=0} com o corretor FORA DO AR é <i>NÃO VERIFIQUEI</i>, e
 *       nunca <i>está limpo</i>. Este projeto já leu doze passadas em dry-run como trabalho
 *       feito; a distinção é a mesma, e a invariante 12 existe por isso.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprova nomeando o corretor e o placar inteiro.
 */
class TelemetriaDaCadeiaDeCorretoresTest {

    /** Corretor que explode em TODA fala — o pior caso, para o contador ter o que contar. */
    static class CorretorQueExplode extends CorretorAcentoPorPadraoService {
        @Override
        public Optional<String> corrigir(String texto) {
            throw new IllegalStateException("falha proposital do caso-controle");
        }
    }

    static class TelemetriaEspia extends TelemetriaService {
        OperacaoTelemetria ultima;

        @Override
        public synchronized void registrarOperacao(OperacaoTelemetria op) {
            this.ultima = op;
        }
    }

    private static Path pastaComUmaFala(Path raiz, String fala) throws IOException {
        Path pasta = raiz.resolve("traducao_ptbr");
        Files.createDirectories(pasta);
        String ass = """
            [Script Info]
            ScriptType: v4.00+

            [V4+ Styles]
            Format: Name, Fontname
            Style: Default,Arial

            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,%s
            """.formatted(fala);
        Files.writeString(pasta.resolve("ep01.ass"), ass, StandardCharsets.UTF_8);
        return pasta;
    }

    private static RevisarConcordanciaUseCase useCase(
            CorretorAcentoPorPadraoService padrao, TelemetriaService telemetria) {
        return new RevisarConcordanciaUseCase(
            new LeitorLegendaAss(), new EscritorLegendaAss(),
            new CorretorConcordanciaGeneroService(),
            new CorretorAcentoQueColideComVerboService(new RevisorGramaticalMudo()),
            padrao,
            new CorretorAcentoDeDicionarioNaFalaService(new CorretorOrtograficoLegenda()),
            new CorretorCaractereForaDoPortuguesService(new CorretorOrtograficoLegenda()),
            telemetria,
            new PoliticaEstiloMusical(List.of()));
    }

    @Test
    @DisplayName("corretor que LANCA: a fala sobrevive, a falha e CONTADA e a passada continua")
    void excecaoEcontadaEnaoPropagada(@TempDir Path raiz) throws IOException {
        Path pasta = pastaComUmaFala(raiz, "Isso e tudo.");
        TelemetriaEspia espia = new TelemetriaEspia();

        ResultadoConcordancia r = useCase(new CorretorQueExplode(), espia).revisarPasta(pasta, false);

        ContagemCorretor padrao = doCorretor(r, "acento por padrao");
        assertEquals(1, padrao.falhou(),
            "a excecao nao foi contada — erro engolido some junto com a fala: " + padrao);
        assertEquals(0, padrao.agiu(), "corretor que explodiu nao pode contar como tendo agido");
        assertEquals(1, padrao.vistas(), "a fala tem de aparecer no total do corretor: " + padrao);
        assertTrue(r.arquivosAnalisados() >= 1,
            "a passada parou por causa da excecao, em vez de continuar");
    }

    @Test
    @DisplayName("corretor FORA DO AR aparece como NAO VERIFICADO, nunca como zero limpo")
    void indisponivelNaoEzeroLimpo(@TempDir Path raiz) throws IOException {
        Path pasta = pastaComUmaFala(raiz, "Uma fala qualquer.");

        ResultadoConcordancia r = useCase(new CorretorAcentoPorPadraoService(),
            new TelemetriaEspia()).revisarPasta(pasta, false);

        ContagemCorretor pos = doCorretor(r, "acento por POS tagger");
        assertFalse(pos.disponivel(),
            "o duble esta declaradamente fora do ar e o placar disse que estava disponivel");
        assertTrue(pos.linhaDeRelatorio().contains("NAO VERIFICADO"),
            "o relatorio mostrou zero em vez de NAO VERIFICADO: " + pos.linhaDeRelatorio());

        ContagemCorretor genero = doCorretor(r, "genero (determinante)");
        assertTrue(genero.disponivel(), "o corretor de genero nao depende de nada e some do ar");
        assertFalse(genero.linhaDeRelatorio().contains("NAO VERIFICADO"),
            "corretor que rodou nao pode sair como nao verificado: " + genero.linhaDeRelatorio());
    }

    @Test
    @DisplayName("a telemetria leva o placar por corretor ate o registro")
    void telemetriaLevaOplacar(@TempDir Path raiz) throws IOException {
        Path pasta = pastaComUmaFala(raiz, "Isso e tudo.");
        TelemetriaEspia espia = new TelemetriaEspia();

        useCase(new CorretorAcentoPorPadraoService(), espia).revisarPasta(pasta, false);

        assertNotNull(espia.ultima, "nenhuma operacao foi registrada na telemetria");
        String detalhe = espia.ultima.detalhe();
        assertTrue(detalhe.contains("acento por padrao="),
            "o detalhe da telemetria nao carrega o placar por corretor: " + detalhe);
        assertTrue(detalhe.contains("acento por POS tagger=NV"),
            "o corretor fora do ar tinha de aparecer como NV (nao verificado): " + detalhe);
    }

    @Test
    @DisplayName("o placar tem uma linha por elo da cadeia, sempre")
    void umaLinhaPorElo(@TempDir Path raiz) throws IOException {
        Path pasta = pastaComUmaFala(raiz, "Uma fala qualquer.");
        ResultadoConcordancia r = useCase(new CorretorAcentoPorPadraoService(),
            new TelemetriaEspia()).revisarPasta(pasta, false);
        assertEquals(5, r.porCorretor().size(),
            "a cadeia tem cinco elos e o placar mostrou outro numero: " + r.porCorretor());
    }

    private static ContagemCorretor doCorretor(ResultadoConcordancia r, String nome) {
        return r.porCorretor().stream()
            .filter(c -> c.nome().equals(nome)).findFirst()
            .orElseThrow(() -> new AssertionError("corretor ausente do placar: " + nome
                + " — placar: " + r.porCorretor()));
    }
}
