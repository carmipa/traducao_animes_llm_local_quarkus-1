package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: todo banner de fechamento tem de dizer <b>em que pasta</b> ele mexeu.
 *
 * <h2>O prejuízo que originou (2026-08-24), e foi um susto de verdade</h2>
 * Paulo pediu uma <b>SIMULAÇÃO</b> do Macross II na tela 3.3, com "Apenas simular" marcado.
 * Enquanto isso, um lote de gravação rodava na mesma fila. O painel mostrou:
 *
 * <pre>
 *   [15:03:59] Iniciando revisão de concordância — Obra: Macross II   &lt;- NAVEGADOR (o pedido dele)
 *   [15:03:59] ...\traducao_ptbr | simular (dry-run)                  &lt;- NAVEGADOR (o modo dele)
 *   [15:03:59] [APLICADO] ... 11 arquivos ... 82 falas ... Backups: 11 &lt;- SERVIDOR (outro lote!)
 * </pre>
 *
 * O {@code [APLICADO]} nem era do Macross II — era do 86 Part 1, de outra execução. Mas ele
 * apareceu <b>imediatamente abaixo do cabeçalho "dry-run"</b> que o navegador escrevera, e a
 * leitura natural, correta e honesta é: <i>"eu mandei simular e ele gravou"</i>.
 *
 * <p>Nada foi gravado indevidamente — a execução do Paulo saiu
 * {@code [OK — NADA A CORRIGIR (dry-run)]} com {@code Backups: 0}. O defeito não é de escrita, é
 * de <b>prestação de contas</b>: o console é um canal só, o cabeçalho vem do CLIENTE e o banner
 * vem do SERVIDOR, e sem o alvo não há como conferir de quem é o quê.
 *
 * <p>É a mesma família de 22/08/2026, quando doze passadas em dry-run foram lidas como trabalho
 * feito — e a lição se repete: <b>a tela precisa dizer o que fez E onde</b>, porque o operador
 * atribui ao próprio clique tudo o que aparece depois dele.
 *
 * <h2>O critério</h2>
 * Arquivo que imprime um banner de fechamento (tem a régua {@code LINHA} e um rótulo de status
 * entre colchetes) precisa imprimir também uma linha com {@code alvo:}. Não basta receber o
 * caminho: tem de ESCREVER.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Todo banner conhecido nomeia o alvo, ou está na LINHA DE BASE com o motivo.</li>
 *   <li><b>Alvo vazio é NÃO VERIFICADO, nunca aprovação:</b> varredura que não acha banner
 *       nenhum reprova, porque existem pelo menos dois.</li>
 *   <li>Banner novo entra nomeando o alvo; a linha de base é catraca e só desce.</li>
 * </ul>
 *
 * <h2>Limite declarado</h2>
 * Prova que a linha é IMPRESSA, não que o caminho impresso seja o certo. Quem prova isso é a
 * leitura do console numa execução real — e ela está no commit que nasceu deste susto.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nomeia o arquivo exato. Nunca lança por conta própria.
 */
class CatracaBannerNomeiaOAlvoTest {

    private static final Path RAIZ = Path.of("src", "main", "java");

    /** A régua do banner: quem a tem, imprime fechamento de operação. */
    private static final String REGUA = "LINHA = \"=====";
    /** O que prova que o alvo foi escrito, e não só recebido. */
    private static final String ESCREVE_O_ALVO = "\"  alvo: \"";

    /**
     * LINHA DE BASE — exceção declarada, não dívida esquecida. Só desce.
     *
     * <p>{@code GeradorMapaProjetoUseCase} imprime régua, mas não é operação sobre pasta do
     * acervo: ele desenha o mapa do próprio projeto, e o "alvo" dele é o repositório onde já se
     * está. Não há o que confundir com a execução de outro operador.
     */
    private static final Set<String> SEM_ALVO_POR_NATUREZA = Set.of(
        "GeradorMapaProjetoUseCase.java");

    /** A própria catraca cita as marcas na documentação; exclusão nominal de UM arquivo. */
    private static final String EU_MESMA = "CatracaBannerNomeiaOAlvoTest.java";

    private static Set<String> arquivosComBanner(Path raiz) throws IOException {
        Set<String> achados = new TreeSet<>();
        try (Stream<Path> caminhos = Files.walk(raiz)) {
            for (Path arquivo : caminhos.filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !p.getFileName().toString().equals(EU_MESMA)).toList()) {
                if (Files.readString(arquivo, StandardCharsets.UTF_8).contains(REGUA)) {
                    achados.add(arquivo.getFileName().toString());
                }
            }
        }
        return achados;
    }

    @Test
    @DisplayName("todo banner de fechamento diz em que pasta mexeu")
    void bannerNomeiaOAlvo() throws IOException {
        assertTrue(Files.isDirectory(RAIZ),
            "NAO VERIFICADO: " + RAIZ.toAbsolutePath() + " nao existe");

        Set<String> comBanner = arquivosComBanner(RAIZ);
        assertFalse(comBanner.isEmpty(),
            "instrumento CEGO: nao achou NENHUM banner de fechamento, e existem pelo menos dois. "
                + "Se a forma da regua mudou, corrija o criterio — nao apague a catraca.");

        List<String> semAlvo = new ArrayList<>();
        for (String nome : comBanner) {
            if (SEM_ALVO_POR_NATUREZA.contains(nome)) {
                continue;
            }
            if (!fonteDe(nome).contains(ESCREVE_O_ALVO)) {
                semAlvo.add(nome);
            }
        }
        assertTrue(semAlvo.isEmpty(), () -> """
            Banner de fechamento que nao diz em que pasta mexeu: %s

            Em 24/08/2026 Paulo mandou SIMULAR o Macross II e o painel mostrou, logo abaixo do
            cabecalho "simular (dry-run)" escrito pelo NAVEGADOR, um banner [APLICADO] com 11
            arquivos vindo do SERVIDOR — de outra pasta, de outra execucao. Nada foi gravado
            indevidamente, e mesmo assim a leitura honesta era "mandei simular e ele gravou".

            O console e um canal so. Imprima uma linha `alvo: <pasta>` no banner, para que o
            operador consiga conferir de quem e o que ele esta lendo.
            """.formatted(semAlvo));

        List<String> sumiram = new ArrayList<>(SEM_ALVO_POR_NATUREZA);
        sumiram.removeAll(comBanner);
        assertTrue(sumiram.isEmpty(),
            "excecao declarada sumiu da varredura: " + sumiram + ". Se o arquivo deixou de "
                + "imprimir banner, tire da linha de base — a catraca so desce.");
    }

    private static String fonteDe(String nomeDoArquivo) throws IOException {
        try (Stream<Path> caminhos = Files.walk(RAIZ)) {
            Path achado = caminhos.filter(p -> p.getFileName().toString().equals(nomeDoArquivo))
                .findFirst().orElse(null);
            return achado == null ? "" : Files.readString(achado, StandardCharsets.UTF_8);
        }
    }

    /**
     * CASO-CONTROLE: a varredura só vale se separar os três casos — banner que nomeia o alvo,
     * banner que não nomeia, e arquivo sem banner nenhum.
     */
    @Test
    @DisplayName("instrumento calibrado: pega o banner mudo e ignora quem nao tem banner")
    void instrumentoDiscrimina(@TempDir Path temp) throws IOException {
        Files.writeString(temp.resolve("BannerBom.java"), """
            class BannerBom {
                private static final String LINHA = "========";
                void fim(Object alvo) {
                    System.out.println("  [APLICADO] OPERACAO");
                    System.out.println("  alvo: " + alvo);
                }
            }
            """);
        Files.writeString(temp.resolve("BannerMudo.java"), """
            class BannerMudo {
                private static final String LINHA = "========";
                void fim() {
                    System.out.println("  [APLICADO] OPERACAO");
                }
            }
            """);
        Files.writeString(temp.resolve("SemBanner.java"), """
            class SemBanner {
                void nada() { System.out.println("  alvo: irrelevante"); }
            }
            """);

        Set<String> comBanner = arquivosComBanner(temp);
        assertEquals(Set.of("BannerBom.java", "BannerMudo.java"), comBanner,
            "a varredura tinha de achar os DOIS banners e mais nada. Achou: " + comBanner);

        String bom = Files.readString(temp.resolve("BannerBom.java"), StandardCharsets.UTF_8);
        String mudo = Files.readString(temp.resolve("BannerMudo.java"), StandardCharsets.UTF_8);
        assertTrue(bom.contains(ESCREVE_O_ALVO), "o banner bom tem de passar no criterio do alvo");
        assertFalse(mudo.contains(ESCREVE_O_ALVO), "o banner mudo nao pode passar");
    }
}
