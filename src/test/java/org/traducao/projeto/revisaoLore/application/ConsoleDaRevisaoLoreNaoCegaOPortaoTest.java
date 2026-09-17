package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * PROPÓSITO DE NEGÓCIO: o console da 3.2 passou a agregar por arquivo em vez de imprimir uma
 * linha por fala. Isso mexeu, sem parecer, num mecanismo de SEGURANÇA: o mesmo método que
 * escreve no console escreve no log de execução, e é esse log que o {@code pode-compilar.ps1}
 * lê para saber se há job vivo.
 *
 * <h2>O acoplamento, e por que ele precisa de guarda</h2>
 * Enquanto a tela imprimia em toda fala, o portão nunca via silêncio. Agregando, um arquivo
 * grande passaria minutos mudo — e o portão, cujo limite de ociosidade é 90s, concluiria "último
 * job já terminou" e liberaria a compilação. Compilar dispara live reload e MATA o arquivo em
 * curso: é exatamente o acidente de 14/08/2026, o episódio Stink Bomb perdido no lote 281 de 368.
 *
 * <p>Duas partes do sistema, dois arquivos, duas linguagens — e nenhum compilador liga uma na
 * outra. Só um teste.
 *
 * <h2>O que o console ganhou, medido</h2>
 * Numa corrida das sete obras o console imprimiu 10.563 linhas, das quais 10.013 (94,8%) eram
 * {@code "auditando lore"} e {@code "limpo pela heuristica"}, uma por fala. O sinal — LLM,
 * pendente, corrigida — era 2,5%. A 3.1 nunca fez isso: imprime a linha que TEM algo.
 *
 * <h2>Invariantes do domínio</h2>
 * Duas folgas, e o teste lê CADA número da sua fonte de verdade em vez de repeti-lo aqui —
 * senão congelaria uma cópia e deixaria de proteger no dia em que alguém mexesse num dos lados:
 * <ul>
 *   <li>o INTERVALO do batimento é uma fração do limite do portão (folga de pelo menos o dobro);</li>
 *   <li>o pior caso de SILÊNCIO — o bloqueio de UMA fala na chamada ao LLM, que é
 *       {@code read-timeout × MAX_TENTATIVAS + pausa}, porque o batimento só sai ENTRE falas e
 *       nunca durante a chamada bloqueante — tem de caber sob o limite do portão. Foi
 *       exatamente aqui que a premissa "~20s por fala" derivou: o {@code read-timeout} configurado
 *       era 180s, dando até 362s de silêncio, e o portão (90s) liberaria a compilação sobre o job.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem traz os valores lidos e lembra o custo do acidente.
 */
class ConsoleDaRevisaoLoreNaoCegaOPortaoTest {

    private static final Path USE_CASE = Path.of("src", "main", "java", "org", "traducao",
        "projeto", "revisaoLore", "application", "RevisarLoreUseCase.java");
    private static final Path ADAPTER = Path.of("src", "main", "java", "org", "traducao",
        "projeto", "revisaoLore", "infrastructure", "adapters", "RevisorLoreLlmAdapter.java");
    private static final Path YML = Path.of("src", "main", "resources", "application.yml");
    private static final Path PORTAO = Path.of("pode-compilar.ps1");

    @Test
    @DisplayName("o batimento do console sai bem antes de o portao dar o job por encerrado")
    void batimentoSaiAntesDoLimiteDoPortao() {
        long batimentoMs = lerBatimento();
        long limiteS = lerLimiteDoPortao();

        assertTrue(batimentoMs > 0 && limiteS > 0, () ->
            "nao consegui ler os dois valores das fontes (batimento=" + batimentoMs
                + "ms, limite=" + limiteS + "s). Isso NAO e aprovacao: o formato mudou e o teste "
                + "ficou cego — conserte a leitura antes de confiar no verde.");

        long limiteMs = limiteS * 1000L;
        assertTrue(batimentoMs * 2 <= limiteMs, () ->
            "o console da 3.2 pode ficar " + batimentoMs / 1000 + "s calado, e o pode-compilar.ps1 "
                + "considera o job encerrado depois de " + limiteS + "s de silencio. Sem pelo "
                + "menos o DOBRO de folga, um arquivo grande faz o portao liberar a compilacao "
                + "sobre um job vivo — e compilar dispara live reload e mata o arquivo em curso. "
                + "Em 14/08/2026 isso custou o episodio Stink Bomb no lote 281 de 368.");
    }

    @Test
    @DisplayName("o bloqueio maximo de uma fala na chamada ao LLM cabe sob o limite do portao")
    void bloqueioDeUmaFalaNoLlmCabeSobOLimiteDoPortao() {
        long readTimeoutS = lerReadTimeoutRevisaoLore();
        long maxTentativas = lerMaxTentativas();
        long pausaS = lerPausaEntreTentativas();
        long limiteS = lerLimiteDoPortao();

        if (readTimeoutS <= 0 || maxTentativas <= 0 || pausaS < 0 || limiteS <= 0) {
            fail("nao consegui ler os valores das fontes (readTimeout=" + readTimeoutS + "s, "
                + "tentativas=" + maxTentativas + ", pausa=" + pausaS + "s, limite=" + limiteS
                + "s). Isso NAO e aprovacao: um formato mudou e a leitura ficou cega — conserte "
                + "antes de confiar no verde.");
        }

        // O silencio maximo do console e o tempo preso numa unica fala: cada tentativa espera ate
        // read-timeout, com a pausa entre elas. O batimento NAO cobre isso — ele so sai entre falas.
        long maxBloqueioS = readTimeoutS * maxTentativas + pausaS * (maxTentativas - 1);
        assertTrue(maxBloqueioS < limiteS, () ->
            "uma fala pode prender o console por ate " + maxBloqueioS + "s na chamada ao LLM "
                + "(read-timeout " + readTimeoutS + "s x " + maxTentativas + " tentativas + "
                + pausaS + "s de pausa), e o pode-compilar.ps1 da o job por encerrado depois de "
                + limiteS + "s de silencio. Um LLM lento cega o portao e libera live reload sobre "
                + "o job vivo — o acidente Stink Bomb (14/08/2026). Reduza o read-timeout da "
                + "revisao-lore (nao o do tradutor, que traduz lotes) ate caber sob " + limiteS + "s.");
    }

    private static long lerBatimento() {
        Matcher m = Pattern.compile("INTERVALO_BATIMENTO_MS\\s*=\\s*([\\d_]+)L")
            .matcher(ler(USE_CASE));
        return m.find() ? Long.parseLong(m.group(1).replace("_", "")) : -1;
    }

    private static long lerLimiteDoPortao() {
        Matcher m = Pattern.compile("\\[int\\]\\$OciosoSegundos\\s*=\\s*(\\d+)").matcher(ler(PORTAO));
        return m.find() ? Long.parseLong(m.group(1)) : -1;
    }

    /** O read-timeout EFETIVO em runtime e o do bloco revisao-lore.llm do application.yml (ordinal vence). */
    private static long lerReadTimeoutRevisaoLore() {
        Matcher m = Pattern.compile("(?s)revisao-lore:\\s*\\n\\s+llm:.*?read-timeout:\\s*(\\d+)s")
            .matcher(ler(YML));
        return m.find() ? Long.parseLong(m.group(1)) : -1;
    }

    private static long lerMaxTentativas() {
        Matcher m = Pattern.compile("MAX_TENTATIVAS_REVISAO\\s*=\\s*(\\d+)").matcher(ler(ADAPTER));
        return m.find() ? Long.parseLong(m.group(1)) : -1;
    }

    private static long lerPausaEntreTentativas() {
        Matcher m = Pattern.compile("pausaEntreTentativas\\s*=\\s*Duration\\.ofSeconds\\((\\d+)\\)")
            .matcher(ler(Path.of("src", "main", "java", "org", "traducao", "projeto", "revisaoLore",
                "infrastructure", "config", "RevisaoLoreLlmProperties.java")));
        return m.find() ? Long.parseLong(m.group(1)) : -1;
    }

    private static String ler(Path arquivo) {
        try {
            return Files.readString(arquivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
