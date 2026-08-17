package org.traducao.projeto.raspagemRevisao.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.raspagemRevisao.domain.ModoReferenciaRevisao;
import org.traducao.projeto.raspagemRevisao.domain.ModoRevisaoLegendas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: MEDIR o que a tela 3.1 responde quando o operador aponta a pasta de
 * legendas em inglês para o lugar errado — o cenário de erro de BOA-FÉ, não de ataque.
 *
 * <h2>Por que este teste existe</h2>
 * A regra 12 do protocolo: <i>"saída vazia ambígua é bug — 'nada a processar' e 'cego' não podem
 * produzir o mesmo sinal"</i>. A 3.1 depende de duas pastas, e em 16/08/2026 a ORDEM dos dois
 * campos foi invertida na tela (o inglês passou a vir primeiro). Quem tem a memória muscular da
 * ordem antiga põe a pasta errada no primeiro campo — e sem original em inglês a tela não tem
 * espelho, não tem o que comparar, e passa por todas as falas sem auditar nenhuma.
 *
 * <p>Não é hipótese de laboratório: Paulo tem ~19 obras para passar por esta tela, uma a uma, à
 * mão. Um {@code [SUCESSO]} verde numa obra que não foi olhada é pior que um erro — encerra o
 * assunto.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O teste é de CARACTERIZAÇÃO: ele afirma o que o código faz HOJE, para o conserto ter uma
 *       linha de base medida em vez de uma impressão de leitura.</li>
 *   <li>Modo GOOGLE não toca a rede neste cenário: sem original inglês, o laço por fala devolve
 *       cada evento intacto antes de qualquer chamada externa.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se este teste passar a reprovar, o desfecho da tela mudou — confira se mudou para MELHOR (a
 * cegueira passou a ser declarada) antes de mexer nas asserções.
 */
@QuarkusTest
class CegueiraDaPastaEnCaracterizacaoTest {

    @Inject
    RevisarLegendasUseCase useCase;

    private static final String CABECALHO = """
        [Script Info]
        ScriptType: v4.00+

        [V4+ Styles]
        Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
        Style: Default,Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,0,2,10,10,10,1

        [Events]
        Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        """;

    private void escreverAss(Path arquivo, List<String> textos) throws IOException {
        StringBuilder sb = new StringBuilder(CABECALHO);
        for (String texto : textos) {
            sb.append("Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,").append(texto).append('\n');
        }
        Files.writeString(arquivo, sb.toString(), StandardCharsets.UTF_8);
    }

    /**
     * O CENÁRIO DE BOA-FÉ: a pasta PT está certa, a pasta "inglês" existe mas está vazia (é a
     * pasta errada), e não há cache. Nenhuma fala tem espelho.
     */
    @Test
    @DisplayName("MEDIÇÃO: pasta EN errada — o que a 3.1 responde?")
    void pastaEnErradaProduzQualDesfecho(@TempDir Path temp) throws IOException {
        Path pastaPt = Files.createDirectory(temp.resolve("traducao_ptbr"));
        Path pastaEnErrada = Files.createDirectory(temp.resolve("pasta_que_nao_tem_ingles"));
        Path pastaCache = Files.createDirectory(temp.resolve("cache"));
        Path pastaSaida = Files.createDirectory(temp.resolve("saida"));

        escreverAss(pastaPt.resolve("ep01_PT-BR.ass"), List.of(
            "Bom dia, comandante.",
            "O inimigo avança pelo flanco leste.",
            "Nao vamos conseguir segurar essa posicao.",
            "Recuar é a única opção que nos resta."));

        ResultadoRevisaoLegendas resultado = useCase.executar(
            pastaPt, pastaEnErrada, pastaCache, pastaSaida,
            ModoRevisaoLegendas.GOOGLE, null, ModoReferenciaRevisao.AMBOS);

        assertEquals(1, resultado.arquivosAnalisados(), "o arquivo PT foi lido");
        assertEquals(0, resultado.falasCorrigidas(), "sem espelho, nada foi corrigido");
        assertEquals(0, resultado.falasComProblema(), "sem espelho, nada foi detectado");
        assertEquals(0, resultado.falasPendentes(), "e nada foi marcado como pendente");

        assertEquals(1, resultado.arquivosCegos(),
            "o arquivo passou inteiro sem uma unica fala comparada — isso tem de ser CONTADO");
        assertEquals("CONCLUIDO_SEM_REFERENCIA", resultado.status(),
            "quatro falas nao olhadas NAO podem sair como o mesmo [SUCESSO] verde de uma obra "
                + "limpa: cego e nada-a-fazer tem de dar sinais DIFERENTES (regra 12).");
    }

    /**
     * O CONTRA-CASO, na mesma corrida: as MESMAS falas, com a pasta EN certa. Serve para provar
     * que o desfecho acima vem da cegueira e não de o arquivo ser trivial.
     */
    @Test
    @DisplayName("contra-caso: com a pasta EN certa, as mesmas falas SÃO auditadas")
    void comPastaEnCertaAsMesmasFalasSaoAuditadas(@TempDir Path temp) throws IOException {
        Path pastaPt = Files.createDirectory(temp.resolve("traducao_ptbr"));
        Path pastaEn = Files.createDirectory(temp.resolve("legendas_extraidas_ass"));
        Path pastaCache = Files.createDirectory(temp.resolve("cache"));
        Path pastaSaida = Files.createDirectory(temp.resolve("saida"));

        escreverAss(pastaPt.resolve("ep01_PT-BR.ass"), List.of(
            "Bom dia, comandante.",
            "O inimigo avança pelo flanco leste.",
            "Nao vamos conseguir segurar essa posicao.",
            "Recuar é a única opção que nos resta."));
        escreverAss(pastaEn.resolve("ep01_ENG.ass"), List.of(
            "Good morning, commander.",
            "The enemy is advancing on the east flank.",
            "We are not going to hold this position.",
            "Retreating is the only option we have left."));

        ResultadoRevisaoLegendas resultado = useCase.executar(
            pastaPt, pastaEn, pastaCache, pastaSaida,
            ModoRevisaoLegendas.GOOGLE, null, ModoReferenciaRevisao.AMBOS);

        assertEquals(1, resultado.arquivosAnalisados());
        assertEquals(0, resultado.arquivosCegos(),
            "com espelho, o arquivo enxergou — e o alarme de cegueira NAO pode disparar aqui: "
                + "guarda que reprova o caso são ensina a desligar o alarme");
        assertEquals("CONCLUIDO", resultado.status(),
            "com espelho, as falas sao comparadas e o desfecho e legitimo");
    }
}
