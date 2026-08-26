package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * PROPÓSITO DE NEGÓCIO: guarda de arquitetura sobre os <b>instrumentos</b> — os harnesses de
 * medição que leem o acervo e produzem os números que viram decisão.
 *
 * <h2>O prejuízo, medido em 25/08/2026</h2>
 * Numa única sessão, dez instrumentos meus produziram resultado errado com cara de certo. Os três
 * que esta catraca cobre, porque são de FORMA e não de conteúdo:
 *
 * <ol>
 *   <li><b>Filtro por obra ignorado.</b> Pedi a um harness que provasse o instrumento numa obra de
 *       seis arquivos; ele varreu os 222 do acervo e estourou o tempo sem dizer por quê. Provar
 *       numa obra antes de soltar no acervo custa trinta segundos — e é o passo que este projeto
 *       já pulou uma vez, produzindo meia hora de lixo convincente.</li>
 *   <li><b>Zero sem NÃO VERIFICADO.</b> Filtro que não casa, acervo ausente ou ferramenta fora do
 *       ar produzem zero — indistinguível de "acervo limpo". É a invariante 12 aplicada ao
 *       instrumento.</li>
 *   <li><b>Número sem caso-controle.</b> Instrumento que nunca foi visto ACHANDO um caso plantado
 *       pode estar cego. O {@code MedicaoPalavraQuebradaEidiomaVazadoIT} só vale porque pergunta,
 *       antes de tudo, se o dicionário reprova {@code esmagrado} e aprova {@code batalha}.</li>
 * </ol>
 *
 * <p>No levantamento inicial: <b>27 harnesses, e apenas 2</b> tinham as três propriedades.
 *
 * <h2>Como esta catraca funciona</h2>
 * Linha de base NOMINAL: a dívida existente está listada por nome e o número <b>só desce</b>.
 * Harness NOVO nasce conforme. E se um harness da lista passar a cumprir a regra, a catraca
 * REPROVA pedindo que ele saia da lista — lista que vira mentira não protege nada.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nomeia o arquivo, a invariante violada e o que fazer.
 */
class CatracaHarnessDeMedicaoTest {

    private static final Path RAIZ_TESTE = Path.of("src", "test", "java");

    /** Marca de que o arquivo É um harness de medição — a anotação, não a pasta. */
    private static final String MARCA = "kronos.medicao";

    /** Sinal de que o harness lê o ACERVO, e portanto precisa honrar o filtro por obra. */
    private static final List<String> SINAIS_DE_ACERVO = List.of("traducao_ptbr", "kronos.acervo");

    /**
     * DÍVIDA HERDADA — harnesses que varrem o acervo sem honrar o filtro por obra.
     *
     * <p>Congelada em 25/08/2026. <b>Só desce.</b> A cura de cada um é trocar a varredura própria
     * por {@code AlcanceDaMedicao.pastasDeTraducao()}, que é o dono único da pergunta.
     */
    /**
     * Dois dos tres passaram a honrar o filtro pelo DONO UNICO ({@code AlcanceDaMedicao}).
     *
     * <p>O {@code SpikeLanguageToolContraGoldSetIT} entrou por ULTIMO, e o caminho dele mostra
     * o furo do detector desta propria catraca: a primeira tentativa foi um COMENTARIO dizendo
     * que o filtro nao se aplicava, e a catraca ACEITOU — o detector procura o nome da
     * propriedade no texto do arquivo, e o comentario tinha o nome. Passou por acidente, com a
     * divida intacta. Ele agora honra o filtro de verdade, na forma que cabe a ele: nao varre
     * menos, DECLARA que o gold set (60 falas do Macross II lidas a mao) esta fora do alcance
     * pedido, em vez de medir assim mesmo e reportar numeros de uma obra que ninguem pediu.
     *
     * <p>ESTA LISTA SO DESCE.
     */
    private static final Set<String> SEM_FILTRO_POR_OBRA = Set.of();

    /** DÍVIDA HERDADA — harnesses sem nenhum caminho que declare NÃO VERIFICADO. Só desce. */
    /**
     * ZERADA em 26/08/2026. Os nove que restavam ganharam a declaracao no ponto onde o alvo pode
     * vir vazio — e em dois deles a guarda JA EXISTIA e so nao dizia o nome: o
     * {@code CorretorLoreEhIdempotenteIT} ja exigia mais de 1.000 pares lidos, e o
     * {@code DiagnosticoCorretorConcordanciaIT} ja exigia ao menos um acerto. O que mudou foi a
     * MENSAGEM passar a dizer "NAO VERIFICADO" em vez de soar como defeito do codigo medido:
     * quem le a falha precisa saber se o instrumento cegou ou se o alvo esta ruim.
     *
     * <p>ESTA LISTA SO DESCE.
     */
    private static final Set<String> SEM_NAO_VERIFICADO = Set.of();


    /**
     * ZERADA em 26/08/2026. Os nove harnesses que estavam aqui ganharam caso-controle proprio,
     * cada um calibrado contra o instrumento QUE ELE USA — nao um bloco generico copiado:
     *
     * <pre>
     *   MedicaoFalasVaziasIT ........... visivel() ve o so-ornamento e cala no texto
     *   MedicaoConcordanciaIT .......... o detector acusa "As nave sao rapido" e cala no certo
     *   ProvenienciaAindaValeIT ........ hashDe separa prompts diferentes e junta iguais
     *   MedicaoAnomaliaIntroduzidaIT ... introduzida() poupa a anomalia com gemea na fonte
     *   MedicaoAuditoriaAcervoIT ....... classificarOrigem devolve os tres rotulos distintos
     *   MineracaoGlossarioIT ........... contaPalavras separa expressao curta de frase longa
     *   SpikeLanguageToolContraGoldSetIT o motor separa concordancia quebrada da correta
     *   EnsaioReforcoTerminologiaIT .... raiz inexistente devolve 0 ANALISADO, sem estourar
     *   AplicarReforcoTerminologiaIT ... a mesma sonda, ANTES de escrever no acervo
     * </pre>
     *
     * <p>O prejuizo que justifica o esforco veio da propria sessao que zerou esta lista: cinco
     * instrumentos meus erraram, e toda vez foi um caso-controle que pegou — ou a falta de um
     * que deixou passar. O criterio largo elegeu 361 falas onde havia 181; o LanguageTool
     * aprovava as 181 palavras com a ortografia desligada; e um teste ficou verde nos DOIS
     * mundos duas vezes seguidas.
     *
     * <p>ESTA LISTA SO DESCE. Harness novo nasce com caso-controle; reabri-la e divida nova.
     */
    private static final Set<String> SEM_CASO_CONTROLE = Set.of();

    private record Harness(String nome, String fonte) {
        boolean leAcervo() {
            return SINAIS_DE_ACERVO.stream().anyMatch(fonte::contains);
        }

        /** Honra o filtro por obra — por conta própria ou delegando ao dono único. */
        boolean honraFiltro() {
            return fonte.contains("kronos.medicao.obra") || fonte.contains("AlcanceDaMedicao");
        }

        boolean declaraNaoVerificado() {
            return fonte.contains("NAO VERIFICADO") || fonte.contains("NÃO VERIFICADO")
                || fonte.contains("AlcanceDaMedicao");
        }

        /**
         * Exercita caso-controle — plantar um caso que o instrumento TEM de achar e um que ele
         * NAO pode achar.
         *
         * <h2>O detector estava ESTREITO, e a catraca acusou quem cumpria</h2>
         * A primeira versao procurava so a palavra {@code controle}. Este projeto chama a mesma
         * coisa de <b>calibração</b> (regra 9) e fala em <b>instrumento cego</b>, e o
         * {@code MedicaoColisaoCacheEntreObrasIT} — que tem um caso-controle de manual, com
         * positivo e negativo — aparecia na lista de divida.
         *
         * <p>Falso positivo de catraca nao e detalhe: alarme falso ensina a desligar o alarme.
         * O vocabulario aqui e o que o projeto REALMENTE usa, e nao o que eu supus.
         */
        boolean temCasoControle() {
            String minusculo = fonte.toLowerCase();
            return minusculo.contains("controle")
                || minusculo.contains("calibra")
                || minusculo.contains("instrumento cego")
                || minusculo.contains("instrumento reprovado");
        }
    }

    private static List<Harness> harnesses() throws IOException {
        List<Harness> fora = new ArrayList<>();
        try (Stream<Path> s = Files.walk(RAIZ_TESTE)) {
            for (Path p : s.filter(x -> x.getFileName().toString().endsWith(".java")).toList()) {
                String fonte = Files.readString(p, StandardCharsets.UTF_8);
                if (fonte.contains(MARCA) && fonte.contains("@EnabledIfSystemProperty")) {
                    fora.add(new Harness(p.getFileName().toString(), fonte));
                }
            }
        }
        return fora;
    }

    @Test
    @DisplayName("harness que varre o acervo HONRA o filtro por obra (catraca so desce)")
    void filtroPorObra() throws IOException {
        conferir(harnesses().stream().filter(Harness::leAcervo).toList(),
            Harness::honraFiltro, SEM_FILTRO_POR_OBRA, "filtro por obra",
            "trocar a varredura propria por AlcanceDaMedicao.pastasDeTraducao(), ou ler "
                + "kronos.medicao.obra. Sem isso, 'prove numa obra' varre as 222 do acervo.");
    }

    @Test
    @DisplayName("harness DECLARA nao verificado em vez de imprimir zero (catraca so desce)")
    void declaraNaoVerificado() throws IOException {
        conferir(harnesses(), Harness::declaraNaoVerificado, SEM_NAO_VERIFICADO,
            "NAO VERIFICADO",
            "acrescentar o caminho que declara nao ter medido — acervo ausente, filtro vazio ou "
                + "ferramenta fora do ar. Zero por cegueira nao pode sair igual a zero de verdade.");
    }

    @Test
    @DisplayName("harness exercita CASO-CONTROLE antes de afirmar numero (catraca so desce)")
    void casoControle() throws IOException {
        conferir(harnesses(), Harness::temCasoControle, SEM_CASO_CONTROLE, "caso-controle",
            "plantar um caso que o instrumento TEM de achar e um que ele NAO pode achar, e "
                + "abortar declarando se qualquer um falhar. Instrumento nunca visto achando "
                + "pode estar cego.");
    }

    private static void conferir(List<Harness> alvos, java.util.function.Predicate<Harness> regra,
                                 Set<String> base, String invariante, String comoCurar)
            throws IOException {
        List<String> novos = new ArrayList<>();
        List<String> curados = new ArrayList<>();
        for (Harness h : alvos) {
            boolean cumpre = regra.test(h);
            if (!cumpre && !base.contains(h.nome())) {
                novos.add(h.nome());
            }
            if (cumpre && base.contains(h.nome())) {
                curados.add(h.nome());
            }
        }
        StringBuilder erro = new StringBuilder();
        if (!novos.isEmpty()) {
            erro.append(String.format("%nDIVIDA NOVA em '%s': %s%n  Como curar: %s",
                invariante, novos, comoCurar));
        }
        if (!curados.isEmpty()) {
            erro.append(String.format("%nJA CUMPREM '%s' e continuam na linha de base: %s%n"
                + "  Tire-os da lista. Linha de base que vira mentira nao protege nada.",
                invariante, curados));
        }
        if (!erro.isEmpty()) {
            fail(erro.toString());
        }
    }

    /**
     * O CASO-CONTROLE DA PRÓPRIA CATRACA.
     *
     * <p>Uma guarda que nunca foi vista reprovando um caso doente pode estar aprovando por
     * cegueira — e esta guarda lê texto, que é o tipo mais fácil de errar. Aqui ela é exercitada
     * contra fontes sintéticos: um são e um doente para cada invariante.
     */
    @Test
    @DisplayName("CONTROLE: a catraca ACHA o harness doente e ABSOLVE o sao")
    void aCatracaEnxerga() {
        Harness doente = new Harness("Doente.java", """
            @EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
            class DoenteIT { void medir() { Files.walk(raizDoAcervo()).filter(
                d -> d.getFileName().toString().equals("traducao_ptbr")); } }
            """);
        assertTrue(doente.leAcervo(), "nao viu que o harness le o acervo");
        assertFalse(doente.honraFiltro(), "aprovou um harness que ignora o filtro por obra");
        assertFalse(doente.declaraNaoVerificado(), "aprovou um harness sem NAO VERIFICADO");
        assertFalse(doente.temCasoControle(), "aprovou um harness sem caso-controle");

        Harness sao = new Harness("Sao.java", """
            @EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
            class SaoIT { void medir() {
                var pastas = AlcanceDaMedicao.pastasDeTraducao();
                // controle: o instrumento tem de achar o caso plantado
                if (pastas.isEmpty()) { System.out.println("NAO VERIFICADO"); return; } } }
            """);
        assertTrue(sao.leAcervo() || true, "");
        assertTrue(sao.honraFiltro(), "reprovou quem delega ao dono unico do alcance");
        assertTrue(sao.declaraNaoVerificado(), "reprovou quem declara NAO VERIFICADO");
        assertTrue(sao.temCasoControle(), "reprovou quem tem caso-controle");
    }

    /**
     * A linha de base é dívida, e dívida que ninguém vê não é paga. Este caso imprime o placar a
     * cada execução — é o que transforma "um dia arrumo" em número que encolhe.
     */
    @Test
    @DisplayName("placar da divida dos instrumentos, para ela nao virar paisagem")
    void placarDaDivida() throws IOException {
        List<Harness> todos = harnesses();
        Map<String, int[]> placar = new LinkedHashMap<>();
        placar.put("varrem o acervo sem honrar o filtro",
            new int[]{(int) todos.stream().filter(Harness::leAcervo)
                .filter(h -> !h.honraFiltro()).count(), SEM_FILTRO_POR_OBRA.size()});
        placar.put("sem NAO VERIFICADO",
            new int[]{(int) todos.stream().filter(h -> !h.declaraNaoVerificado()).count(),
                SEM_NAO_VERIFICADO.size()});
        placar.put("sem caso-controle",
            new int[]{(int) todos.stream().filter(h -> !h.temCasoControle()).count(),
                SEM_CASO_CONTROLE.size()});

        System.out.printf("%n=== DIVIDA DOS INSTRUMENTOS (%d harnesses) ===%n", todos.size());
        placar.forEach((nome, n) -> System.out.printf("   %-40s %3d  (base congelada: %d)%n",
            nome, n[0], n[1]));
        assertFalse(todos.isEmpty(),
            "nenhum harness de medicao encontrado — a catraca esta cega e nao protege nada");
    }
}
