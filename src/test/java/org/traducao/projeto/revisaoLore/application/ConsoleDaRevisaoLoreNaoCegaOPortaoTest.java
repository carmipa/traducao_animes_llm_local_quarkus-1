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
 * O intervalo do batimento é uma FRAÇÃO do limite do portão, com folga para o pior caso (uma
 * chamada ao LLM com retentativa, ~20s). O teste lê os DOIS valores das fontes de verdade — o
 * campo Java e o parâmetro do script — em vez de repetir os números aqui, senão ele congelaria
 * uma cópia e deixaria de proteger no dia em que alguém mexesse num dos lados.
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem traz os dois valores e lembra o custo do acidente.
 */
class ConsoleDaRevisaoLoreNaoCegaOPortaoTest {

    private static final Path USE_CASE = Path.of("src", "main", "java", "org", "traducao",
        "projeto", "revisaoLore", "application", "RevisarLoreUseCase.java");
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

    private static long lerBatimento() {
        Matcher m = Pattern.compile("INTERVALO_BATIMENTO_MS\\s*=\\s*([\\d_]+)L")
            .matcher(ler(USE_CASE));
        return m.find() ? Long.parseLong(m.group(1).replace("_", "")) : -1;
    }

    private static long lerLimiteDoPortao() {
        Matcher m = Pattern.compile("\\[int\\]\\$OciosoSegundos\\s*=\\s*(\\d+)").matcher(ler(PORTAO));
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
