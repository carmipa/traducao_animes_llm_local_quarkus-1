package org.traducao.projeto.revisaoConcordancia.application;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;
import org.traducao.projeto.core.texto.gramatica.LanguageToolRevisorAdapter;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.revisaoConcordancia.domain.ResultadoConcordancia;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: a tela 3.3 tem de <b>convergir</b> — clicar duas vezes não pode mudar o
 * arquivo duas vezes.
 *
 * <h2>As cinco falas reais que descobriram o defeito</h2>
 * Em 25/08/2026 a 3.3 gravou 1.086 falas no acervo e a SEGUNDA passada, simulada, ainda acusou
 * cinco. Todas com a mesma assinatura: falas que o elo de PADRÃO já havia mudado na primeira.
 *
 * <pre>
 *   "Então não ha vitimas."             1a: ha→há        2a: vitimas→vítimas
 *   "Não ha outras maquinas por perto." 1a: ha→há        2a: maquinas→máquinas
 *   "Isso e negocio de família!"        1a: e→é          2a: negocio→negócio
 *   "Mas Judau so esta fazendo algo…"   1a: so→só        2a: esta→está
 * </pre>
 *
 * <h2>A causa, e por que a cura não foi reordenar</h2>
 * O POS tagger roda ANTES do elo de padrão. Com {@code ha} sem acento o LanguageTool não analisa
 * a frase e não dispara; depois de {@code ha→há} ele dispararia, mas o elo dele já passou.
 *
 * <p>Reordenar consertaria ESTES casos e deixaria a convergência dependendo da ordem da fila —
 * frágil, e invisível até a próxima combinação aparecer. Repetir a cadeia até estabilizar torna a
 * convergência uma propriedade da CADEIA. O custo é baixo: só a fala que mudou paga outra volta.
 *
 * <h2>Por que o teste passa pelo CASO DE USO e não pelos corretores</h2>
 * O laço mora no caso de uso. Um teste que encadeasse os corretores à mão provaria uma cadeia que
 * a produção não executa — e foi exatamente assim que este defeito passou despercebido: uma volta
 * só sempre pareceu suficiente.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprova mostrando quantas falas a segunda passada ainda mudaria, e o conteúdo do arquivo.
 */
class CadeiaEidempotenteTest {

    /** Falas REAIS do acervo, como estavam ANTES da passada de 25/08/2026. */
    private static final List<String> FALAS = List.of(
        "Então não ha vitimas.",
        "Não ha outras maquinas por perto.",
        "Isso e negocio de família!",
        "Mas Judau so esta fazendo algo porque quer salvar sua irma.",
        "Claro que sim! Você so esta sendo teimoso!");

    private static boolean dePe;

    static class TelemetriaMuda extends TelemetriaService {
        @Override
        public synchronized void registrarOperacao(OperacaoTelemetria op) {
            // não persiste
        }
    }

    @BeforeAll
    static void conferirFerramental() {
        CorretorOrtograficoLegenda dic = new CorretorOrtograficoLegenda();
        dic.corrigir("Uma sonda qualquer para acordar o dicionario.");
        LanguageToolRevisorAdapter lt = new LanguageToolRevisorAdapter();
        lt.revisar("Uma sonda qualquer para acordar o revisor.");
        dePe = dic.disponivel() && lt.disponivel();
    }

    private static RevisarConcordanciaUseCase useCase() {
        return new RevisarConcordanciaUseCase(
            new LeitorLegendaAss(), new EscritorLegendaAss(),
            new CorretorConcordanciaGeneroService(),
            // O REVISOR DE VERDADE. Com o dublê mudo o elo do POS tagger não faz nada, e o
            // defeito de convergência — que é DELE — ficaria invisível.
            new CorretorAcentoQueColideComVerboService(new LanguageToolRevisorAdapter()),
            new CorretorAcentoPorPadraoService(),
            new CorretorAcentoDeDicionarioNaFalaService(new CorretorOrtograficoLegenda()),
            new CorretorCaractereForaDoPortuguesService(new CorretorOrtograficoLegenda()),
            new TelemetriaMuda(),
            new PoliticaEstiloMusical(List.of()));
    }

    private static Path montarPasta(Path raiz) throws IOException {
        Path pasta = raiz.resolve("traducao_ptbr");
        Files.createDirectories(pasta);
        StringBuilder ass = new StringBuilder("""
            [Script Info]
            ScriptType: v4.00+

            [V4+ Styles]
            Format: Name, Fontname
            Style: Default,Arial

            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            """);
        int i = 0;
        for (String fala : FALAS) {
            ass.append(String.format("Dialogue: 0,0:00:%02d.00,0:00:%02d.00,Default,,0,0,0,,%s%n",
                i, i + 2, fala));
            i += 3;
        }
        Files.writeString(pasta.resolve("ep01.ass"), ass.toString(), StandardCharsets.UTF_8);
        return pasta;
    }

    @Test
    @DisplayName("a SEGUNDA passada nao muda mais nada — as 5 falas reais do acervo")
    void segundaPassadaNaoMudaNada(@TempDir Path raiz) throws IOException {
        Assumptions.assumeTrue(dePe,
            "dicionario ou revisor fora do ar — NAO VERIFICADO, e pular nao e aprovar");
        Path pasta = montarPasta(raiz);

        ResultadoConcordancia primeira = useCase().revisarPasta(pasta, true);
        ResultadoConcordancia segunda = useCase().revisarPasta(pasta, false);

        String conteudo = Files.readString(pasta.resolve("ep01.ass"), StandardCharsets.UTF_8);
        assertEquals(0, segunda.falasCorrigidas(),
            "a segunda passada ainda mudaria " + segunda.falasCorrigidas() + " fala(s): a cadeia "
                + "nao converge e cada clique muda o arquivo de novo.\n" + conteudo);
        assertEquals(FALAS.size(), primeira.falasCorrigidas(),
            "a primeira passada tinha de corrigir as cinco falas: " + conteudo);
    }

    /**
     * O controle: a convergência não pode ser obtida deixando de corrigir. Estas são as formas
     * FINAIS das cinco falas, e uma guarda que apenas parasse a cadeia reprovaria aqui.
     */
    @Test
    @DisplayName("CONTROLE: a cadeia corrige mesmo, e ate o fim")
    void aCadeiaCorrigeAteOfim(@TempDir Path raiz) throws IOException {
        Assumptions.assumeTrue(dePe, "dicionario ou revisor fora do ar — NAO VERIFICADO");
        Path pasta = montarPasta(raiz);
        useCase().revisarPasta(pasta, true);
        String conteudo = Files.readString(pasta.resolve("ep01.ass"), StandardCharsets.UTF_8);

        for (String esperado : List.of(
                "Então não há vítimas.",
                "Não há outras máquinas por perto.",
                "Isso é negócio de família!",
                "Mas Judau só está fazendo algo porque quer salvar sua irma.",
                "Claro que sim! Você só está sendo teimoso!")) {
            assertTrue(conteudo.contains(esperado),
                "a cadeia nao chegou em '" + esperado + "'.\n" + conteudo);
        }
    }
}
