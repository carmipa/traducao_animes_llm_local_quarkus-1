package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.revisaoLore.application.GerenciadorPromptRevisaoLore;
import org.traducao.projeto.revisaoLore.application.CorretorLoreDeterministico;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: o corretor determinístico da tela 3.2 escreve na legenda ENTREGUE. Se
 * ele não for idempotente, rodar a tela duas vezes na mesma obra continua mexendo no arquivo — e
 * o operador não tem como saber quando parou de mudar.
 *
 * <h2>Por que este teste existe</h2>
 * Em 18/08/2026 a tela passou a corrigir de verdade: 45 escritas pela via determinística numa
 * corrida só. A partir daí "rodar de novo" deixou de ser barato: cada corrida é uma chance de
 * churn. Idempotência é a propriedade que transforma "rodar de novo" em operação segura, e ela
 * NÃO sai de graça — basta uma entrada cujo canônico case a própria regra para o arquivo oscilar
 * a cada passada.
 *
 * <p>Um mapa com {@code A -> B} onde {@code B} também case a chave de outra entrada produziria
 * exatamente isso. A tabela do projeto tem 5.800 entradas; auditar na cabeça não é opção.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Segunda passada sobre o resultado da primeira não muda NADA. Se mudar, o teste nomeia a
 *       obra, a fala e as duas formas que se alternam.</li>
 *   <li>O par EN/PT vem dos arquivos REAIS, não de fixture: fixture escolhida por conveniência
 *       já escondeu o defeito do acervo uma vez nesta mesma tela (o veto de cartaz usava
 *       estilo "Sign" enquanto o acervo usava "Titles").</li>
 *   <li>Alvo vazio reprova: se a varredura não achar par nenhum, o instrumento cegou.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem traz obra, texto EN, primeira saída e segunda saída — o suficiente para achar a
 * entrada do mapa que oscila.
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class CorretorLoreEhIdempotenteIT {

    private static final Path ACERVO = Path.of("C:", "animes");

    @Inject
    CorretorLoreDeterministico corretor;

    @Inject
    GerenciadorPromptRevisaoLore gerenciador;

    /** Obra do acervo → contexto de lore, para as sete que o dono do acervo trabalha. */
    private static final Map<String, String> OBRAS = Map.of(
        "08th MS Team", "gundam_08ms",
        "0080 War", "gundam_0080",
        "0083 Stardust", "gundam_0083",
        "Guilty Crown + OVA", "guilty_crown",
        "Unicorn Re0096", "gundam_unicorn",
        "Zeta Gundam", "gundam_zeta",
        "Gundam ZZ", "gundam_zz"
    );

    @Test
    @DisplayName("segunda passada do corretor deterministico nao muda mais nada")
    void segundaPassadaNaoMudaNada() {
        List<String> oscilando = new ArrayList<>();
        int paresLidos = 0;
        int corrigidos = 0;

        for (Map.Entry<String, String> entrada : OBRAS.entrySet()) {
            Path base = acharBase(entrada.getKey());
            if (base == null) {
                continue;
            }
            Map<String, String> correcoes = gerenciador.correcoesTerminologia(entrada.getValue());
            for (Par par : paresDe(base)) {
                paresLidos++;
                Optional<String> primeira = corretor.corrigir(par.en(), par.pt(), correcoes);
                if (primeira.isEmpty()) {
                    continue;
                }
                corrigidos++;
                Optional<String> segunda = corretor.corrigir(par.en(), primeira.get(), correcoes);
                if (segunda.isPresent() && !segunda.get().equals(primeira.get())) {
                    oscilando.add(entrada.getValue() + "\n      EN : " + par.en()
                        + "\n      1a : " + primeira.get()
                        + "\n      2a : " + segunda.get());
                }
            }
        }

        int lidos = paresLidos;
        assertTrue(paresLidos > 1000, () ->
            "a varredura leu " + lidos + " pares EN/PT e o acervo tem dezenas de milhares. Isso "
                + "NAO e aprovacao: os arquivos nao casaram e o instrumento ficou cego.");

        // O acervo ja foi corrigido em 18/08/2026, entao a maioria das falas nao tem mais o que
        // trocar. Se NENHUMA tiver, a assercao de idempotencia e vazia: "nao oscilou" porque
        // nada foi corrigido. O piso abaixo garante que o instrumento exercitou o corretor de
        // verdade — e a mesma regra do alvo vazio, aplicada ao proprio teste.
        int total = corrigidos;
        assertTrue(corrigidos > 0, () ->
            "o corretor nao corrigiu NENHUMA das " + lidos + " falas lidas, entao a assercao de "
                + "idempotencia abaixo nao exercitou nada. Isso NAO e aprovacao: ou o mapa da "
                + "obra sumiu, ou o casamento EN/PT quebrou.");

        assertTrue(oscilando.isEmpty(), () ->
            "o corretor NAO e idempotente — rodar a tela de novo continua mexendo na legenda, em "
                + oscilando.size() + " de " + total + " falas corrigidas:\n  "
                + String.join("\n  ", oscilando.subList(0, Math.min(6, oscilando.size()))));
    }

    private record Par(String en, String pt) {
    }

    private static Path acharBase(String marca) {
        try (Stream<Path> raiz = Files.list(ACERVO)) {
            for (Path obra : raiz.toList()) {
                if (!Files.isDirectory(obra) || !obra.getFileName().toString().contains(marca)) {
                    continue;
                }
                if (Files.isDirectory(obra.resolve("legendas_extraidas_ass"))) {
                    return obra;
                }
                try (Stream<Path> subs = Files.list(obra)) {
                    for (Path sub : subs.toList()) {
                        if (Files.isDirectory(sub.resolve("legendas_extraidas_ass"))) {
                            return sub;
                        }
                    }
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    /** Casa EN e PT por POSIÇÃO, e descarta o arquivo cuja contagem de eventos divirja. */
    private static List<Par> paresDe(Path base) {
        List<Par> pares = new ArrayList<>();
        Path en = base.resolve("legendas_extraidas_ass");
        Path pt = base.resolve("traducao_ptbr");
        try (Stream<Path> arquivos = Files.list(en)) {
            for (Path arqEn : arquivos.filter(p -> p.toString().endsWith(".ass")).sorted().toList()) {
                Path arqPt = casarPt(pt, arqEn);
                if (arqPt == null) {
                    continue;
                }
                List<String> linhasEn = dialogos(arqEn);
                List<String> linhasPt = dialogos(arqPt);
                if (linhasEn.size() != linhasPt.size()) {
                    continue;
                }
                for (int i = 0; i < linhasEn.size(); i++) {
                    pares.add(new Par(linhasEn.get(i), linhasPt.get(i)));
                }
            }
        } catch (IOException e) {
            return pares;
        }
        return pares;
    }

    private static Path casarPt(Path pastaPt, Path arqEn) {
        String chave = arqEn.getFileName().toString().replace("_Track3.ass", "").replace(".ass", "");
        try (Stream<Path> arquivos = Files.list(pastaPt)) {
            return arquivos
                .filter(p -> p.toString().endsWith(".ass"))
                .filter(p -> !p.getFileName().toString().contains(".parcial."))
                .filter(p -> p.getFileName().toString().startsWith(chave))
                .findFirst()
                .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private static List<String> dialogos(Path arquivo) {
        try {
            List<String> texto = new ArrayList<>();
            for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
                if (!linha.startsWith("Dialogue:")) {
                    continue;
                }
                String[] partes = linha.split(",", 10);
                texto.add(partes.length > 9 ? partes[9] : "");
            }
            return texto;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
