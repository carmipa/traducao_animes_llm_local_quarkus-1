package org.traducao.projeto.raspagemRevisao.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.cachetraducao.domain.CacheDocumento;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.raspagemRevisao.domain.ModoReferenciaRevisao;
import org.traducao.projeto.raspagemRevisao.domain.ModoRevisaoLegendas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que uma correção de concordância chega ao BYTE do arquivo .ass —
 * o único elo do corretor que nenhum teste cobria. A fatia tem teste para cada peça (o detector
 * acusa, a cadeia decide {@code Corrigir}, a guarda aprova) e teste para o disco (o dreno grava
 * todas as falas), mas nada ligava as duas pontas: defeito plantado saindo corrigido em disco.
 *
 * <h2>A cicatriz: o corretor só foi visto NÃO mexendo</h2>
 * Em 2026-08-12 a Revisão de Legendas (opção 3.1) rodou sobre 5.620 falas do Gundam Unicorn,
 * apontou 3 problemas e corrigiu 0 — e estava CERTA, porque os 3 eram falsos positivos, todos
 * corrigidos no mesmo dia. Só que o desfecho observado foi sempre o mesmo: nada mudou. Um
 * corretor que perdesse a capacidade de escrever produziria exatamente esse relatório, e a
 * varredura seguiria "limpa" para sempre.
 *
 * <p>É a regra 9 do protocolo — instrumento calibrado contra caso-controle. Uma guarda
 * exercitada só no arquivo são pode estar aprovando por não enxergar nada.
 *
 * <h2>Por que este caso-controle não precisa de LLM</h2>
 * O parentesco invertido é o único defeito que as duas pontas alcançam sozinhas:
 * {@code DetectorParentescoInvertido} o ACUSA e {@code CorretorDeterministicoConcordanciaService}
 * o CORRIGE, na 1ª fonte da cadeia, antes de qualquer chamada externa. O teste roda sem LM Studio
 * e sem rede, e a contagem do dublê prova que nenhum provedor foi consultado.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Defeito plantado que a regra determinística alcança SAI corrigido no arquivo de saída.</li>
 *   <li>Fala sã ao lado da doente permanece byte a byte — corrigir uma linha não pode reescrever
 *       a vizinha.</li>
 *   <li>Defeito FORA do alcance determinístico preserva a fala original. Nunca vazia, nunca
 *       parcial: o limite do corretor é ficar pendente, não destruir.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se alguém cegar o caminho de escrita — trocar a gravação por log, apertar a guarda a ponto de
 * rejeitar correção legítima, ou desligar a fonte determinística —, o primeiro teste reprova com
 * o texto encontrado no arquivo. O terceiro reprova se o corretor passar a apagar o que não sabe
 * consertar.
 */
@QuarkusTest
@TestProfile(CorrecaoChegaAoArquivoTest.PerfilComTradutorDublado.class)
class CorrecaoChegaAoArquivoTest {

    /** Escopa a alternativa a este teste; um dublê global mudaria a suíte inteira. */
    public static class PerfilComTradutorDublado implements QuarkusTestProfile {
        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(RecuperacaoExternaContadora.class);
        }
    }

    @Inject
    RevisarLegendasUseCase useCase;

    @Inject
    RecuperacaoExternaContadora tradutor;

    @Inject
    ObjectMapper mapper;

    private static final String CABECALHO = """
        [Script Info]
        ScriptType: v4.00+
        PlayResX: 1920
        PlayResY: 1080

        [V4+ Styles]
        Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
        Style: Default,Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,0,2,10,10,10,1

        [Events]
        Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        """;

    @BeforeEach
    void limparDuble() {
        tradutor.reiniciar();
    }

    /**
     * Uma fala do arquivo: o inglês que vai para o cache e o português que vai para o .ass. São
     * campos separados de propósito — é a divergência entre os dois que o detector enxerga.
     */
    private record Fala(String ingles, String portugues) {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: monta em disco o par que a revisão precisa — a legenda PT a auditar e
     * o cache com o inglês de referência.
     *
     * <p>INVARIANTES DO DOMÍNIO: índice, estilo e ordem casam entre o .ass e o cache; sem isso o
     * resolvedor não pareia e a revisão não teria referência para comparar.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga {@link IOException} — pasta temporária que não
     * pode ser escrita é falha do teste, não cenário de domínio.
     */
    private Path montar(Path temp, List<Fala> falas) throws IOException {
        Path pastaPt = Files.createDirectory(temp.resolve("pt"));
        Path pastaCache = Files.createDirectory(temp.resolve("cache"));

        StringBuilder ass = new StringBuilder(CABECALHO);
        List<EntradaCache> entradas = new ArrayList<>();
        for (int i = 0; i < falas.size(); i++) {
            ass.append("Dialogue: 0,0:00:0").append(i).append(".00,0:00:09.00,Default,,0,0,0,,")
               .append(falas.get(i).portugues()).append('\n');
            entradas.add(new EntradaCache(
                i, "Default", falas.get(i).ingles(), falas.get(i).portugues(), "en", "pt"));
        }
        Files.writeString(pastaPt.resolve("show_PT-BR.ass"), ass.toString(), StandardCharsets.UTF_8);
        mapper.writerWithDefaultPrettyPrinter().writeValue(
            pastaCache.resolve("show_ENG.cache.json").toFile(),
            new CacheDocumento(new ProvenienciaCache(
                ProvenienciaCache.SCHEMA_ATUAL, "danmachi", "h", "m", "en", "pt"), entradas));
        return pastaPt;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: roda a revisão real sobre a pasta montada e devolve o arquivo de saída
     * quando ele existe.
     *
     * <p>INVARIANTES DO DOMÍNIO: modo GOOGLE porque a 1ª fonte da cadeia é determinística e resolve
     * antes de qualquer provedor; o que passar dela cai no dublê, nunca na rede.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: a AUSÊNCIA do arquivo é resultado legítimo, não erro —
     * MEDIDO nesta bancada em 2026-08-12: sem nenhuma correção aplicada a revisão não regrava nada,
     * coerente com a recusa a "marcar o arquivo como modificado sem nada ter mudado". Por isso o
     * método devolve {@link Optional}: quem chama decide se esperava escrita ou silêncio, e a
     * existência do arquivo vira o discriminador de "houve correção".
     */
    private Optional<String> revisar(Path temp, Path pastaPt) throws IOException {
        Path pastaSaida = Files.createDirectory(temp.resolve("saida"));
        useCase.executar(pastaPt, null, temp.resolve("cache"), pastaSaida,
            ModoRevisaoLegendas.GOOGLE, "danmachi", ModoReferenciaRevisao.AMBOS);
        Path destino = pastaSaida.resolve("show_PT-BR.ass");
        return Files.exists(destino)
            ? Optional.of(Files.readString(destino, StandardCharsets.UTF_8))
            : Optional.empty();
    }

    /** O .ass de entrada como está agora — é onde a fala vive quando nada foi regravado. */
    private String entradaAtual(Path pastaPt) throws IOException {
        return Files.readString(pastaPt.resolve("show_PT-BR.ass"), StandardCharsets.UTF_8);
    }

    private static long contarDialogos(String conteudo) {
        return conteudo.lines().filter(l -> l.startsWith("Dialogue:")).count();
    }

    /**
     * O elo que faltava: o defeito plantado tem de SUMIR do arquivo em disco.
     *
     * <p>"My father" com "Minha mãe" é contradição objetiva — o detector acusa e a regra
     * determinística troca o parentesco E o possessivo que o acompanha, produzindo "Meu pai".
     */
    @Test
    @DisplayName("Caso doente: parentesco invertido sai CORRIGIDO no .ass de saída")
    void defeitoDeterministicoEhCorrigidoNoArquivo(@TempDir Path temp) throws IOException {
        Path pastaPt = montar(temp, List.of(
            new Fala("My father is waiting for me.", "Minha mãe está me esperando."),
            new Fala("The weather is nice today.", "O tempo está bom hoje.")));

        String saida = revisar(temp, pastaPt).orElseThrow(() -> new AssertionError(
            "O CORRETOR NÃO ESCREVEU NADA. Arquivo de saída ausente significa nenhuma correção "
                + "aplicada — e este defeito tem detector que o acusa e regra determinística que "
                + "o conserta. É exatamente assim que a fatia falharia se o caminho de escrita "
                + "fosse cegado."));

        assertTrue(saida.contains("Meu pai está me esperando."),
            "O CORRETOR ESCREVEU, MAS NÃO CORRIGIU o defeito plantado. Saída:\n" + saida);
        assertFalse(saida.contains("mãe"),
            "o parentesco errado continua no arquivo:\n" + saida);
        assertTrue(saida.contains("O tempo está bom hoje."),
            "a fala sã ao lado foi alterada — corrigir uma linha não pode reescrever a vizinha");
        assertEquals(2, contarDialogos(saida), "nenhuma fala pode sumir na gravação");
        assertEquals(0, tradutor.chamadas(),
            "a regra determinística é a 1ª fonte e resolve sem custo; chamada externa aqui "
                + "significa que ela deixou de ser consultada primeiro");
    }

    /**
     * O controle são, que dá sentido ao teste anterior: sem ele, um corretor que reescrevesse
     * TUDO também passaria no caso doente.
     */
    @Test
    @DisplayName("Controle são: arquivo sem defeito atravessa intacto e sem chamar ninguém")
    void arquivoSaoNaoEhAlterado(@TempDir Path temp) throws IOException {
        Path pastaPt = montar(temp, List.of(
            new Fala("My father is waiting for me.", "Meu pai está me esperando."),
            new Fala("The weather is nice today.", "O tempo está bom hoje.")));

        Optional<String> saida = revisar(temp, pastaPt);

        assertTrue(saida.isEmpty(),
            "arquivo são gerou regravação — marcar como modificado o que não mudou destrói a data "
                + "do arquivo e faz o operador achar que houve correção. Gravado:\n" + saida.orElse(""));
        assertTrue(entradaAtual(pastaPt).contains("Meu pai está me esperando."),
            "a fala correta foi alterada na própria entrada");
        assertEquals(0, tradutor.chamadas(),
            "arquivo são não pode gerar chamada externa — seria custo e risco por nada");
    }

    /**
     * O LIMITE, medido em vez de suposto: concordância nominal é DETECTADA mas nenhuma regra
     * determinística a conserta, e o Google não é acionado para problema não objetivo.
     *
     * <p>O desfecho correto é a fala ficar como está — pendente para o LLM. O que este teste
     * impede é o outro desfecho: o corretor apagar ou truncar o que não sabe consertar.
     */
    @Test
    @DisplayName("Limite: defeito sem regra determinística preserva a fala, nunca a destrói")
    void defeitoForaDoAlcanceDeterministicoPreservaAFala(@TempDir Path temp) throws IOException {
        Path pastaPt = montar(temp, List.of(
            new Fala("She is very tired.", "Ela está muito cansado.")));

        String texto = revisar(temp, pastaPt).orElseGet(() -> {
            try {
                return entradaAtual(pastaPt);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        });

        assertEquals(1, contarDialogos(texto), "a fala não pode sumir por não ter conserto local");
        assertTrue(texto.contains("Ela está muito cansado.") || texto.contains("Ela está muito cansada."),
            "a fala foi destruída em vez de preservada ou corrigida:\n" + texto);
    }

    // ------------------------------------------------------------------------------------------
    // LENTE DE BOA-FÉ (regra 15): não "como alguém quebra isto de propósito", mas como uma pessoa
    // honesta, seguindo interpretação razoável da tela, causa dano sem perceber. Esta fatia
    // SOBRESCREVE legenda — é a classe de ferramenta em que a pergunta é obrigatória.
    // ------------------------------------------------------------------------------------------

    /**
     * BOA-FÉ: o operador roda, o console não mostra correção nenhuma no arquivo que ele esperava,
     * e ele roda DE NOVO. É o comportamento mais natural do mundo diante de uma tela que não
     * confirma o que fez — e o mais perigoso numa ferramenta que reescreve texto.
     *
     * <p>O risco concreto é a correção acumulativa: a regra de parentesco trocou "mãe" por "pai" na
     * primeira passada; se a segunda reprocessasse a versão já corrigida contra o mesmo inglês, uma
     * regra mal condicionada poderia trocar de novo, oscilar, ou empilhar possessivo. O resultado
     * correto é a segunda passada não ter NADA a fazer.
     */
    @Test
    @DisplayName("Boa-fé: rodar duas vezes seguidas não muda o resultado da primeira")
    void segundaPassadaSobreOJaCorrigidoNaoAlteraNada(@TempDir Path temp) throws IOException {
        Path pastaPt = montar(temp, List.of(
            new Fala("My father is waiting for me.", "Minha mãe está me esperando.")));

        String primeira = revisar(temp, pastaPt).orElseThrow(
            () -> new AssertionError("a primeira passada não corrigiu; cenário não montado"));

        // A saída da primeira passada vira a ENTRADA da segunda — exatamente o que acontece quando
        // o operador aponta a revisão para a pasta que ela mesma acabou de gravar.
        Path segundaEntrada = Files.createDirectory(temp.resolve("pt2"));
        Files.writeString(segundaEntrada.resolve("show_PT-BR.ass"), primeira, StandardCharsets.UTF_8);
        Path segundaSaida = Files.createDirectory(temp.resolve("saida2"));
        useCase.executar(segundaEntrada, null, temp.resolve("cache"), segundaSaida,
            ModoRevisaoLegendas.GOOGLE, "danmachi", ModoReferenciaRevisao.AMBOS);

        Path regravado = segundaSaida.resolve("show_PT-BR.ass");
        String depois = Files.exists(regravado)
            ? Files.readString(regravado, StandardCharsets.UTF_8)
            : Files.readString(segundaEntrada.resolve("show_PT-BR.ass"), StandardCharsets.UTF_8);

        assertEquals(primeira, depois,
            "NÃO É IDEMPOTENTE: rodar a revisão de novo sobre o resultado dela mudou o texto. "
                + "Quem roda duas vezes por não ver confirmação na tela corrompe a legenda.\n"
                + "1ª:\n" + primeira + "\n2ª:\n" + depois);
    }

    /**
     * BOA-FÉ: a cicatriz do projeto, aplicada a ESTA fatia. Em 06/08/2026 uma tradução foi apontada
     * para {@code legenda-simplificada} — pasta de SAÍDA — e sobrescreveu 17 arquivos limpos. O
     * código fez o que foi mandado; a interface permitiu mandar.
     *
     * <p>Aqui o equivalente é entrada e saída na MESMA pasta. Não é uso absurdo: é o que alguém faz
     * ao querer "corrigir no lugar". O que este teste exige não é a recusa — é que, se a operação
     * acontecer, ela não perca fala nem deixe o arquivo pela metade.
     */
    @Test
    @DisplayName("Boa-fé: entrada e saída na mesma pasta não perde fala")
    void corrigirNoLugarNaoPerdeFala(@TempDir Path temp) throws IOException {
        Path pastaPt = montar(temp, List.of(
            new Fala("My father is waiting for me.", "Minha mãe está me esperando."),
            new Fala("The weather is nice today.", "O tempo está bom hoje."),
            new Fala("We should go now.", "Devemos ir agora.")));
        long antes = contarDialogos(entradaAtual(pastaPt));

        useCase.executar(pastaPt, null, temp.resolve("cache"), pastaPt,
            ModoRevisaoLegendas.GOOGLE, "danmachi", ModoReferenciaRevisao.AMBOS);

        String depois = entradaAtual(pastaPt);
        assertEquals(antes, contarDialogos(depois),
            "corrigir no lugar perdeu fala — é a cicatriz dos 17 arquivos repetida nesta fatia:\n"
                + depois);
        assertTrue(depois.contains("O tempo está bom hoje.") && depois.contains("Devemos ir agora."),
            "as falas sãs não sobreviveram à gravação no lugar:\n" + depois);
    }

    // ------------------------------------------------------------------------------------------
    // LENTE ADVERSARIAL: como alguém — ou o próprio acervo — quebra isto de propósito. Diferente
    // da boa-fé acima: aqui a entrada é hostil, não ingênua.
    // ------------------------------------------------------------------------------------------

    /**
     * ADVERSARIAL: a fala real do acervo não é texto limpo — tem tag de override e quebra {@code \N}
     * no meio. A correção precisa atravessar as duas sem tocar na formatação.
     *
     * <p>O alvo é específico e tem endereço: {@code CorretorDeterministicoConcordanciaService}
     * declara no próprio Javadoc que a fronteira que reconhece {@code \N} como separador serve a UM
     * único padrão. Os pares possessivo+parentesco casam com {@code \s+}, que não cobre a quebra —
     * então "Minha\Nmãe" pode virar "Minha\Npai", com o parentesco corrigido e o possessivo
     * desconcordado. Seria uma correção que INTRODUZ o erro que a fatia existe para achar.
     */
    @Test
    @DisplayName("Adversarial: quebra \\N e tag ASS entre possessivo e parentesco")
    void quebraDeLinhaEntreOPossessivoEOParentesco(@TempDir Path temp) throws IOException {
        Path pastaPt = montar(temp, List.of(
            new Fala("My father is waiting for me.", "{\\i1}Minha\\Nmãe{\\i0} está me esperando.")));

        String texto = revisar(temp, pastaPt).orElseGet(() -> {
            try {
                return entradaAtual(pastaPt);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        });

        assertTrue(texto.contains("{\\i1}") && texto.contains("{\\i0}") && texto.contains("\\N"),
            "a correção comeu tag de override ou a quebra de linha:\n" + texto);
        assertTrue(texto.contains("Meu\\Npai"),
            "DESFECHO OBSERVADO:\n" + texto);
    }

    /**
     * ADVERSARIAL: o inglês cita as DUAS relações. Não há como saber a qual delas o termo português
     * se refere, e trocar seria adivinhar — o detector bloqueia por isso, e o corretor tem de
     * bloquear pelo mesmo motivo. Uma regra que agisse aqui reescreveria falas corretas em massa.
     */
    @Test
    @DisplayName("Adversarial: original cita pai E mãe — nada pode ser trocado por adivinhação")
    void originalComAsDuasRelacoesNaoAutorizaTroca(@TempDir Path temp) throws IOException {
        Path pastaPt = montar(temp, List.of(
            new Fala("My father told my mother about it.", "Minha mãe me contou sobre isso.")));

        Optional<String> saida = revisar(temp, pastaPt);

        assertTrue(saida.isEmpty() || saida.get().contains("Minha mãe"),
            "trocou o parentesco sem evidência inequívoca — com pai E mãe no original, qualquer "
                + "troca é chute e reescreve fala legítima:\n" + saida.orElse(""));
    }
}
