package org.traducao.projeto.medicao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: guarda de SAÍDA. Varre a legenda que foi ENTREGUE e reprova se o
 * encanamento do pipeline chegou lá — sentinela de mascaramento, negrito de markdown, token de
 * template de chat ou cerca de código.
 *
 * <h2>Por que a guarda de entrada não bastou</h2>
 * O veto em {@code ValidadorCandidatoLoreService} recusa a proposta ANTES de gravar, e fecha o
 * caminho conhecido. Mas documento e revisão provam a ENTRADA; só guarda executável prova a
 * SAÍDA. Em 18/08/2026 o resíduo chegou ao disco por um caminho que a validação de então
 * enxergava e aprovava:
 * <pre>
 *   Guilty Crown - 07_Track4_PT-BR.ass, evento 183
 *   É só questão de tempo até que as [[Anti Bodies]] sejam retiradas.
 * </pre>
 * A tokenização daquela validação olha PALAVRAS e é cega a colchete: {@code "[[Anti Bodies]]"}
 * tokenizava idêntico a {@code "Anti Bodies"}, o diff dizia "termo inserido que existe no
 * inglês", e a proposta era aprovada. Uma linha em 337.721 entregues — e uma basta, porque é a
 * que o espectador lê.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Olha o arquivo ENTREGUE, não a proposta. É o único lugar onde o dano é real.</li>
 *   <li>Colchete SIMPLES é uso legítimo de legenda ({@code [Rádio]}); o alvo é o par duplo.</li>
 *   <li>Varredura curta reprova: alvo vazio é cegueira do instrumento, não aprovação.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nomeia arquivo, linha e o resíduo exato, para o conserto ser direto.
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoResiduoNoAcervoIT {

    private static final Path ACERVO = Path.of("C:", "animes");

    /** Par duplo do sentinela, markdown e token de template. Colchete simples fica de fora. */
    private static final Pattern RESIDUO = Pattern.compile(
        "\\[\\[[^\\]]*\\]\\]|\\*\\*|__|<\\|[^|<>]{1,40}\\|>|```");

    @Test
    @DisplayName("nenhuma legenda ENTREGUE carrega residuo do pipeline")
    void nenhumaLegendaEntregueTemResiduo() {
        List<String> sujas = new ArrayList<>();
        int linhas = 0;

        for (Path pasta : pastasDeEntrega()) {
            try (Stream<Path> arquivos = Files.list(pasta)) {
                for (Path arq : arquivos.filter(p -> p.toString().endsWith(".ass")).toList()) {
                    int numero = 0;
                    for (String linha : Files.readAllLines(arq, StandardCharsets.UTF_8)) {
                        numero++;
                        if (!linha.startsWith("Dialogue:")) {
                            continue;
                        }
                        linhas++;
                        Matcher achado = RESIDUO.matcher(linha);
                        if (achado.find()) {
                            sujas.add(arq.getFileName() + ":" + numero + "  residuo=\""
                                + achado.group() + "\"  "
                                + linha.substring(Math.max(0, linha.length() - 70)));
                        }
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        int lidas = linhas;
        assertTrue(linhas > 10_000, () ->
            "a varredura leu " + lidas + " linhas de dialogo entregues, e o acervo tem mais de "
                + "300 mil. Isso NAO e aprovacao: o instrumento cegou.");

        assertTrue(sujas.isEmpty(), () ->
            "legenda ENTREGUE com encanamento do pipeline (" + sujas.size() + "):\n  "
                + String.join("\n  ", sujas.subList(0, Math.min(10, sujas.size()))));
    }

    private static List<Path> pastasDeEntrega() {
        List<Path> pastas = new ArrayList<>();
        try (Stream<Path> raiz = Files.list(ACERVO)) {
            for (Path obra : raiz.filter(Files::isDirectory).toList()) {
                Path direto = obra.resolve("traducao_ptbr");
                if (Files.isDirectory(direto)) {
                    pastas.add(direto);
                }
                try (Stream<Path> subs = Files.list(obra)) {
                    for (Path sub : subs.filter(Files::isDirectory).toList()) {
                        Path aninhada = sub.resolve("traducao_ptbr");
                        if (Files.isDirectory(aninhada)) {
                            pastas.add(aninhada);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return pastas;
    }
}
