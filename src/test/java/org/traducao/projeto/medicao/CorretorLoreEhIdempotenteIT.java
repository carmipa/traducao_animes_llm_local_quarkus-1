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
        List<String> aCorrigir = new ArrayList<>();
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
                // QUAL fala, e nao so quantas. "1 fala corrigiria" sem dizer qual obriga quem le
                // a caçar no acervo inteiro — e foi exatamente o que aconteceu em 27/08/2026,
                // quando o numero 1 sobreviveu a uma correcao que eu achava ser essa mesma.
                if (aCorrigir.size() < 8) {
                    aCorrigir.add(entrada.getValue() + "\n      ANTES  " + par.pt()
                        + "\n      DEPOIS " + primeira.get());
                }
                Optional<String> segunda = corretor.corrigir(par.en(), primeira.get(), correcoes);
                if (segunda.isPresent() && !segunda.get().equals(primeira.get())) {
                    oscilando.add(entrada.getValue() + "\n      EN : " + par.en()
                        + "\n      1a : " + primeira.get()
                        + "\n      2a : " + segunda.get());
                }
            }
        }

        // O ALCANCE, impresso. Sem este numero o teste so diz "nao oscila" e ninguem sabe SOBRE
        // QUANTO — e "a segunda passada nao muda nada" e verdade trivial se a primeira tambem
        // nao mudou nada. O placar aqui responde "o corretor deterministico de lore ainda tem o
        // que fazer no acervo?", que e a pergunta operacional.
        System.out.printf("%n=== ALCANCE DO CORRETOR DETERMINISTICO DE LORE ===%n");
        System.out.printf("  pares EN/PT lidos ....... %d%n", paresLidos);
        System.out.printf("  falas que ele CORRIGIRIA . %d  (%.2f%%)%n",
            corrigidos, paresLidos == 0 ? 0.0 : 100.0 * corrigidos / paresLidos);
        System.out.printf("  oscilando na 2a passada .. %d%n", oscilando.size());
        if (!aCorrigir.isEmpty()) {
            System.out.println("  o que ele trocaria:");
            aCorrigir.forEach(c -> System.out.println("    " + c));
        }

        int lidos = paresLidos;
        assertTrue(paresLidos > 1000, () ->
            "NAO VERIFICADO: a varredura leu " + lidos + " pares EN/PT e o acervo tem dezenas de "
                + "milhares. Isso NAO e aprovacao: os arquivos nao casaram e o instrumento "
                + "ficou cego.");

        // ANTES: esta linha era `assertTrue(corrigidos > 0)`, como guarda de cegueira. Em
        // 27/08/2026 ela passou a REPROVAR O ACERVO LIMPO: o corretor chegou a zero falas a
        // corrigir, que e o estado desejado, e o teste leu isso como instrumento cego.
        //
        // "Guarda que reprova codigo correto e pior que guarda nenhuma" — alarme falso ensina a
        // desligar o alarme. Mas afrouxar seria pior ainda: zero no acervo VOLTARIA a significar
        // as duas coisas. A saida e a de sempre: CASO-CONTROLE. Uma fala doente montada aqui
        // prova que o corretor enxerga; provado isso, o zero do acervo e um zero de verdade.
        Map<String, String> mapaDeControle = Map.of("Robô Móvel", "Mobile Suit");
        Optional<String> noDoente = corretor.corrigir(
            "The Mobile Suit is ready.", "O Robô Móvel está pronto.", mapaDeControle);
        Optional<String> noSao = corretor.corrigir(
            "The Mobile Suit is ready.", "O Mobile Suit está pronto.", mapaDeControle);
        assertTrue(noDoente.isPresent() && noSao.isEmpty(), () ->
            "NAO VERIFICADO: o corretor nao passou no proprio caso-controle — doente="
                + noDoente + " sao=" + noSao + ". Sem isso, as " + lidos + " falas lidas nao "
                + "provam nada: 'nada a corrigir' e 'nao consigo corrigir' sairiam iguais.");
        System.out.printf("  controle: corrige 'Robô Móvel'->'Mobile Suit' · cala no ja correto%n");

        // A IDEMPOTENCIA tambem passa pelo controle, e nao pelo acervo: com zero correcoes reais,
        // "a segunda passada nao muda nada" seria verdade trivial.
        assertTrue(corretor.corrigir("The Mobile Suit is ready.", noDoente.get(), mapaDeControle)
                .isEmpty(),
            "o corretor NAO e idempotente: a segunda passada sobre a saida da primeira mexeu de "
                + "novo. Rodar a tela duas vezes continuaria alterando a legenda.");

        int total = corrigidos;

        assertTrue(oscilando.isEmpty(), () ->
            "o corretor NAO e idempotente — rodar a tela de novo continua mexendo na legenda, em "
                + oscilando.size() + " de " + total + " falas corrigidas:\n  "
                + String.join("\n  ", oscilando.subList(0, Math.min(6, oscilando.size()))));
    }

    private record Par(String en, String pt) {
    }

    private static Path acharBase(String marca) {
        // ALCANCE PELO DONO UNICO: honra -Dkronos.medicao.obra e declara NAO VERIFICADO
        // quando o filtro nao casa. A varredura propria daqui ignorava o filtro, entao pedir
        // uma obra e receber o acervo inteiro saia com a mesma cara de medicao dirigida.
        try {
            for (Path obra : org.traducao.projeto.medicao.AlcanceDaMedicao.obras()) {
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
