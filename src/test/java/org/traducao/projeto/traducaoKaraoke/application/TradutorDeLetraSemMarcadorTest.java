package org.traducao.projeto.traducaoKaraoke.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.llm.domain.Lote;
import org.traducao.projeto.llm.domain.StatusLlm;
import org.traducao.projeto.llm.domain.TraducaoLote;
import org.traducao.projeto.qualidadeTraducao.application.LoreAtivaFake;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova, pelo caminho REAL de envio ({@code traduzirViaLlm}), que a linha
 * com tag no meio que o mascarador perdia ganha o segundo tiro sem marcador — e que esse tiro
 * não abre a porta para a alucinação que o mascarador já tinha produzido.
 *
 * <h2>De onde vêm as respostas do dublê</h2>
 * São as respostas que o aya deu ao Guilty Crown na 4.1 de 09/10/2026, tiradas do console real:
 * a tradução boa sem marcador ("que os seus olhos foram dados a você para reconhecer os
 * outros,"), o pedido de contexto em três linhas para "Notice" e o "Relatório recebido. Início
 * da transmissão." para "rely". O dublê devolve a resposta do MASCARADOR quando a entrada tem
 * {@code [[TAG}, e a da frase limpa quando não tem.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Cada asserção nomeia o caminho que quebrou e mostra os avisos do manifesto.
 */
class TradutorDeLetraSemMarcadorTest {

    private static final String PREFIXO = "{\\fad(0,0)\\blur4.5\\3c&H4331EA&}";
    private static final String OLHOS = PREFIXO + "that your eyes were given to you to {\\c&HEAEEEB&}acknowledge others,";
    private static final String OLHOS_PT = "que os seus olhos foram dados a você para reconhecer os outros,";
    private static final String NOTICE = "{\\blur4.5\\3c&HF6D6B3&}N{\\3c&HDCDDD2&}o{\\3c&HD6D9C4&}t"
        + "{\\3c&HD9D2A9&}i{\\3c&HE2C9AA&}c{\\3c&HF4BDCB&}e";
    private static final String RELY = "{\\blur4.5\\fad(100,350)\\3c&HF6D6B3&}r{\\3c&HD7DED9&}e"
        + "{\\3c&HDCCD9E&}l{\\3c&HF4BDCB&}y";

    /** Responde ao texto MASCARADO com uma resposta e à frase LIMPA com outra; conta as chamadas. */
    private static final class LlmDoGuiltyCrown implements LlmPort {
        private final List<String> respostaAoMascarado;
        private final Map<String, List<String>> respostaALimpa;
        final List<String> recebidas = new ArrayList<>();

        LlmDoGuiltyCrown(List<String> respostaAoMascarado, Map<String, List<String>> respostaALimpa) {
            this.respostaAoMascarado = respostaAoMascarado;
            this.respostaALimpa = respostaALimpa;
        }

        @Override
        public TraducaoLote traduzir(Lote lote) {
            String entrada = lote.linhasOriginais().getFirst();
            recebidas.add(entrada);
            List<String> linhas = entrada.contains("[[TAG")
                ? respostaAoMascarado
                : respostaALimpa.getOrDefault(entrada, List.of("(sem resposta configurada)"));
            return new TraducaoLote(lote.idLote(), linhas, true, null);
        }

        @Override
        public StatusLlm verificarDisponibilidade() {
            return new StatusLlm(true, true, "teste");
        }

        @Override
        public Optional<String> revisarConcordancia(String original, String traducao, List<String> problemas) {
            return Optional.empty();
        }

        @Override
        public Optional<String> corrigirTraducao(String original, String traducao, String motivo) {
            return Optional.empty();
        }
    }

    private static TradutorDeLetraKaraoke tradutor(LlmPort llm) {
        TradutorDeLetraKaraoke t = new TradutorDeLetraKaraoke();
        t.llmPort = llm;
        t.mascarador = new MascaradorTags();
        t.validador = new ValidadorTraducaoService(LoreAtivaFake.vazia());
        t.telemetriaService = new TraduzirKaraokeUseCaseTest.MockTelemetria();
        t.logStream = new TraduzirKaraokeUseCaseTest.MockLogStream();
        return t;
    }

    @Test
    @DisplayName("marcador perdido com traducao CERTA: o segundo tiro a salva, com a cor recolocada em inicio de palavra")
    void traducaoCertaDescartadaPeloMarcadorESalva() {
        LlmDoGuiltyCrown llm = new LlmDoGuiltyCrown(List.of(OLHOS_PT),
            Map.of("that your eyes were given to you to acknowledge others,", List.of(OLHOS_PT)));
        List<String> avisos = new ArrayList<>();

        String saida = tradutor(llm).traduzirViaLlm(OLHOS, avisos, new AtomicInteger(), "prompt");

        assertEquals(PREFIXO + "que os seus olhos foram dados a você para {\\c&HEAEEEB&}reconhecer os outros,", saida,
            () -> "a traducao certa tinha de chegar a legenda. Avisos: " + avisos);
        assertEquals(2, llm.recebidas.size(), "um tiro mascarado e UM segundo tiro limpo, nada mais");
        assertTrue(avisos.size() == 1 && avisos.getFirst().startsWith("Refeita sem marcador (Marcador perdido"),
            () -> "A7: o manifesto tem de dizer que a linha foi REFEITA, com o motivo da recusa, e nunca 'mantida': " + avisos);
    }

    @Test
    @DisplayName("Notice: o pedido de contexto em 3 linhas cai, e a palavra limpa volta com as 6 cores")
    void noticeVoltaComGradiente() {
        LlmDoGuiltyCrown llm = new LlmDoGuiltyCrown(
            List.of("Não há contexto suficiente para traduzir a frase \"Noticiando\".", "**Saída:**", "(Incompável)"),
            Map.of("Notice", List.of("Perceba")));
        List<String> avisos = new ArrayList<>();

        String saida = tradutor(llm).traduzirViaLlm(NOTICE, avisos, new AtomicInteger(), "prompt");

        assertEquals("{\\blur4.5\\3c&HF6D6B3&}P{\\3c&HDCDDD2&}e{\\3c&HD6D9C4&}rc{\\3c&HD9D2A9&}e"
            + "{\\3c&HE2C9AA&}b{\\3c&HF4BDCB&}a", saida, () -> "Avisos: " + avisos);
        assertTrue(avisos.getFirst().contains("3 linha(s) para 1 verso"),
            () -> "A7: a causa da primeira recusa tem de sobreviver no aviso: " + avisos);
    }

    @Test
    @DisplayName("rely: se o segundo tiro repetir a invencao, o par a reprova (desproporcional) e a letra fica")
    void invencaoNoSegundoTiroEReprovada() {
        String invencao = "Relatório recebido. Início da transmissão.";
        LlmDoGuiltyCrown llm = new LlmDoGuiltyCrown(List.of(invencao), Map.of("rely", List.of(invencao)));
        List<String> avisos = new ArrayList<>();

        String saida = tradutor(llm).traduzirViaLlm(RELY, avisos, new AtomicInteger(), "prompt");

        assertNull(saida, "texto fluente sem ancora no original nao pode virar legenda");
        assertTrue(avisos.stream().anyMatch(a -> a.startsWith("Marcador perdido")), () -> "recusa 1: " + avisos);
        assertTrue(avisos.stream().anyMatch(a -> a.startsWith("Segundo tiro reprovado") && a.contains("desproporcional")),
            () -> "recusa 2, com o motivo: " + avisos);
    }

    @Test
    @DisplayName("A1 do rely: a MESMA palavra curta com traducao legitima passa pelo mesmo portao")
    void palavraCurtaLegitimaPassa() {
        LlmDoGuiltyCrown llm = new LlmDoGuiltyCrown(List.of("Relatório recebido."), Map.of("rely", List.of("confie")));
        String saida = tradutor(llm).traduzirViaLlm(RELY, new ArrayList<>(), new AtomicInteger(), "prompt");
        assertEquals("confie", saida == null ? null : saida.replaceAll("\\{[^}]*}", ""));
    }

    @Test
    @DisplayName("eco no segundo tiro (o modelo devolve 'rely') e reprovado pelo par")
    void ecoNoSegundoTiroEReprovado() {
        LlmDoGuiltyCrown llm = new LlmDoGuiltyCrown(List.of("rely"), Map.of("rely", List.of("rely")));
        List<String> avisos = new ArrayList<>();
        assertNull(tradutor(llm).traduzirViaLlm(RELY, avisos, new AtomicInteger(), "prompt"));
        assertTrue(avisos.stream().anyMatch(a -> a.startsWith("Segundo tiro reprovado")), () -> "" + avisos);
    }

    @Test
    @DisplayName("onde o mascarador ACERTA, nao ha segundo tiro: a tag fica onde o modelo a pos")
    void mascaradorQueAcertaNaoGanhaSegundoTiro() {
        LlmDoGuiltyCrown llm = new LlmDoGuiltyCrown(
            List.of("[[TAG0]]que os seus olhos foram dados a você para [[TAG1]]reconhecer os outros,"), Map.of());
        List<String> avisos = new ArrayList<>();

        String saida = tradutor(llm).traduzirViaLlm(OLHOS, avisos, new AtomicInteger(), "prompt");

        assertEquals(PREFIXO + "que os seus olhos foram dados a você para {\\c&HEAEEEB&}reconhecer os outros,", saida);
        assertEquals(1, llm.recebidas.size(), "o segundo tiro so existe para quando o primeiro falha");
        assertTrue(avisos.isEmpty(), () -> "" + avisos);
    }

    @Test
    @DisplayName("fora do recorte (\\k no meio): marcador perdido continua mantendo a linha, sem segundo tiro")
    void foraDoRecorteNaoGanhaSegundoTiro() {
        String karaoke = "{\\k20}Ka{\\k30}ze";
        LlmDoGuiltyCrown llm = new LlmDoGuiltyCrown(List.of("Vento"), Map.of());
        List<String> avisos = new ArrayList<>();

        assertNull(tradutor(llm).traduzirViaLlm(karaoke, avisos, new AtomicInteger(), "prompt"));
        assertEquals(1, llm.recebidas.size(), "timing de silaba nunca vai a um segundo tiro");
        assertTrue(avisos.size() == 1 && avisos.getFirst().startsWith("Marcador perdido"), () -> "" + avisos);
    }
}
