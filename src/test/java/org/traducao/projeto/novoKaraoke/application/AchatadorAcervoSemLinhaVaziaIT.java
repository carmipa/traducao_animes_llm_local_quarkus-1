package org.traducao.projeto.novoKaraoke.application;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.medicao.AlcanceDaMedicao;
import org.traducao.projeto.novoKaraoke.domain.MedicaoEstiloKaraoke;
import org.traducao.projeto.novoKaraoke.domain.ports.TelemetriaKaraokePort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: guarda de ACERVO do achatador — roda o Passo 2 de produção sobre as obras
 * reais e reprova se qualquer linha simplificada renderizar uma LINHA VAZIA na tela ({@code \N} de
 * borda ou {@code \N\N}). É o invariante global do fix #2 (regra 9: correção vale para todas as
 * obras, não só o Unicorn). Nasceu por ordem de Paulo em 21/09/2026, quando a auditoria adversarial
 * profunda pediu que a capacidade de medir o acervo virasse guarda de verdade (regra 23), em vez de
 * um harness descartável.
 *
 * <h2>Três estados, nunca dois (regra 23)</h2>
 * <ul>
 *   <li><b>PASSOU</b>: rodou sobre pelo menos uma obra com karaokê e nenhuma linha vazia saiu.</li>
 *   <li><b>REPROVOU</b>: alguma obra produziu linha vazia — o fix #2 regrediu para aquela estrutura.</li>
 *   <li><b>NÃO VERIFICOU</b>: acervo ausente/filtro sem casamento ⇒ {@link Assumptions} ABORTA o
 *       teste (não é um verde silencioso). "Não achei" nunca vira "está limpo".</li>
 * </ul>
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Travado por {@code -Dkronos.medicao=true}: lê o acervo real, ausente no CI. A calibração do
 *       invariante (linha vazia por {@code \N} de borda) mora no teste sintético e mutado
 *       {@code ConversorKaraokeUseCaseTest.quebraNoFimNaoViraLinhaVazia}; esta guarda é o alcance
 *       GLOBAL do mesmo invariante.</li>
 *   <li>Alcance pelo dono único {@link AlcanceDaMedicao} (honra {@code -Dkronos.medicao.obra}).</li>
 *   <li>READ-ONLY sobre o acervo: só lê a origem e grava em pasta temporária.</li>
 *   <li>Exige {@code obrasComKaraoke > 0}: instrumento cego (nenhuma linha simples criada em obra
 *       nenhuma) reprova em vez de passar por vacuidade.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem nomeia a obra e o número de linhas vazias, para a correção não exigir nova varredura.
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class AchatadorAcervoSemLinhaVaziaIT {

    private static final String BR = "\\" + "N";

    @Test
    @DisplayName("achatador nao produz linha vazia em nenhuma obra do acervo")
    void nenhumaLinhaVaziaNoAcervo() throws IOException {
        // CONTROLE POSITIVO (regra 9): antes de confiar num "zero", provar que o detector sabe dizer
        // NAO. Uma linha simples com \N de borda TEM de ser flagrada; uma limpa NAO pode ser.
        assertTrue(temLinhaVazia("texto qualquer" + BR),
            "instrumento cego: nao detecta \\N de borda — o zero do acervo nao valeria");
        assertTrue(temLinhaVazia(BR + "texto"), "instrumento cego: nao detecta \\N no inicio");
        assertTrue(temLinhaVazia("a" + BR + BR + "b"), "instrumento cego: nao detecta \\N duplo");
        assertEquals(false, temLinhaVazia("original" + BR + "traducao"),
            "falso positivo: par bilingue legitimo (original\\Ntraducao) NAO e linha vazia");

        List<Path> pastas = AlcanceDaMedicao.pastasDeTraducao();
        Assumptions.assumeFalse(pastas.isEmpty(),
            "NAO VERIFICADO: sem acervo no alcance (passe -Dkronos.acervo e/ou -Dkronos.medicao.obra). "
                + "Vazio aqui seria \"nao ha linha vazia\" por cegueira — o teste ABORTA em vez de passar.");

        ConversorKaraokeUseCase conversor = new ConversorKaraokeUseCase();
        conversor.detectorKaraoke = new DetectorEfeitoKaraokeService();
        conversor.corretorOrtografico = new CorretorOrtograficoLegenda();
        conversor.logStream = new LogInerte();
        conversor.telemetriaKaraoke = new TelemetriaInerte();

        Path base = Files.createTempDirectory("guarda-acervo-");
        int idx = 0;
        int obrasComKaraoke = 0;
        List<String> ofensores = new ArrayList<>();
        for (Path pasta : pastas) {
            List<Path> arquivos = AlcanceDaMedicao.arquivosEntregues(pasta);
            if (arquivos.isEmpty()) {
                continue;
            }
            Path arquivo = arquivos.get(0);
            Path destino = base.resolve("saida-" + (idx++));
            Files.createDirectories(destino);
            conversor.converterArquivo(arquivo, destino, true);
            Path saida = destino.resolve(arquivo.getFileName());
            if (!Files.exists(saida)) {
                continue;
            }
            int ksimp = 0;
            int vazias = 0;
            for (String ln : Files.readString(saida).lines().toList()) {
                if (!ln.startsWith("Dialogue:")) {
                    continue;
                }
                String[] c = ln.substring("Dialogue:".length()).stripLeading().split(",", 10);
                if (c.length < 10 || !c[3].strip().equals("Karaoke Simples")) {
                    continue;
                }
                ksimp++;
                String vis = c[9].replaceAll("\\{[^}]*\\}", "").strip();
                if (temLinhaVazia(vis)) {
                    vazias++;
                }
            }
            if (ksimp > 0) {
                obrasComKaraoke++;
            }
            if (vazias > 0) {
                ofensores.add(AlcanceDaMedicao.obraDe(pasta) + " (" + arquivo.getFileName() + "): "
                    + vazias + " linha(s) vazia(s)");
            }
        }

        assertTrue(obrasComKaraoke > 0, () ->
            "INSTRUMENTO CEGO: nenhuma obra do alcance produziu linha simples de karaoke — o achatador "
                + "nao rodou de verdade, entao \"zero linha vazia\" nao prova nada. Alcance: " + pastas.size() + " pasta(s).");
        assertEquals(List.of(), ofensores, () ->
            "LINHA VAZIA na saida do achatador (fix #2 regrediu para esta estrutura):\n"
                + String.join("\n", ofensores));
    }

    /** O visível de uma linha simples renderiza uma linha VAZIA? ({@code \N} de borda ou duplo.) */
    private static boolean temLinhaVazia(String vis) {
        return vis.startsWith(BR) || vis.endsWith(BR) || vis.contains(BR + BR);
    }

    private static final class LogInerte extends LogStreamService {
        @Override
        public void publicarLog(String canal, String mensagem) {
        }
    }

    private static final class TelemetriaInerte implements TelemetriaKaraokePort {
        @Override
        public void publicarArquivo(String a, int b, int c, int d, int e, List<MedicaoEstiloKaraoke> f) {
        }

        @Override
        public void publicarOperacao(String a, Path b, Path c, long d, int e) {
        }
    }
}
