package org.traducao.projeto.qualidadeTraducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * PROPÓSITO DE NEGÓCIO: prova a validação de PAR contra um corpus REAL, com positivos e
 * negativos, porque medir só o recall sobre os exemplos que originaram as regras mede
 * memorização do conjunto, não taxa de acerto. O corpus vem do run completo de Guilty Crown
 * (23 episódios, 5.794 entradas não-vazias): 27 defeitos confirmados um a um na legenda
 * ENTREGUE e 38 traduções legítimas escolhidas por ficarem PERTO do limiar de cada regra.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Falso positivo é o risco real desta validação: cada acusação indevida vira pendência
 *       e manda uma tradução boa de volta para a fila. Por isso os negativos incluem os
 *       casos que de fato derrubaram versões anteriores das regras — {@code "Gai—" -> "Gai:"}
 *       (troca de pontuação, não locutor inventado) e {@code "I repeat:" -> "Repito:"}
 *       (dois-pontos legítimo herdado do original).</li>
 *   <li>A lacuna de CONTRAÇÃO é declarada e testada como lacuna, não escondida: a regra foi
 *       medida e REPROVADA no corpus (3 capturas, 1 defeito real, 2 traduções legítimas
 *       acusadas — 33% de precisão). Embarcá-la trocaria um defeito por dois falsos
 *       positivos, então ela ficou de fora de propósito.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * As divergências são acumuladas e reportadas juntas, separadas por classe, para que um
 * ajuste de limiar não seja descoberto um caso por vez.
 */
@DisplayName("validação de par original×traduzido: corpus real de 27 defeitos × 38 legítimas")
class ValidadorParTraducaoTest {

    private static final String CORPUS = "/qualidadeTraducao/corpus-validacao-par.tsv";

    /** Classe cuja regra foi medida e reprovada por falso positivo; segue descoberta de propósito. */
    private static final String CLASSE_LACUNA_DECLARADA = "CONTRACAO";

    private final ValidadorTraducaoService validador = new ValidadorTraducaoService(LoreAtivaFake.vazia());

    private record Caso(boolean defeito, String classe, String original, String traduzido) {
    }

    private static List<Caso> carregarCorpus() {
        List<Caso> casos = new ArrayList<>();
        try (InputStream in = ValidadorParTraducaoTest.class.getResourceAsStream(CORPUS)) {
            assertNotNull(in, "corpus ausente do classpath: " + CORPUS);
            BufferedReader leitor = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String linha;
            while ((linha = leitor.readLine()) != null) {
                if (linha.isBlank() || linha.startsWith("#")) {
                    continue;
                }
                String[] campos = linha.split("\t", -1);
                if (campos.length < 4) {
                    continue;
                }
                casos.add(new Caso("RUIM".equals(campos[0]), campos[1], campos[2], campos[3]));
            }
        } catch (Exception e) {
            fail("falha ao ler o corpus " + CORPUS + ": " + e.getMessage());
        }
        return casos;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: nenhuma tradução legítima pode virar pendência. É a métrica que
     * faltava quando a regra foi proposta com "24/24 de recall" — recall sem falso positivo
     * medido não diz nada sobre o custo de operar a guarda.
     */
    @Test
    @DisplayName("ZERO falso positivo: nenhuma tradução legítima do corpus é acusada")
    void nenhumaTraducaoLegitimaEhAcusada() {
        List<String> acusadas = new ArrayList<>();
        for (Caso caso : carregarCorpus()) {
            if (caso.defeito()) {
                continue;
            }
            try {
                validador.validarPar(caso.original(), caso.traduzido());
            } catch (AlucinacaoDetectadaException e) {
                acusadas.add("[" + caso.classe() + "] \"" + caso.original() + "\" -> \""
                    + caso.traduzido() + "\"\n      motivo: " + e.getMessage());
            }
        }
        assertTrue(acusadas.isEmpty(),
            () -> "Traduções LEGÍTIMAS acusadas pela validação de par — cada uma seria devolvida à fila "
                + "como pendência sem necessidade:\n  " + String.join("\n  ", acusadas));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: todo defeito confirmado na legenda entregue precisa ser barrado,
     * exceto a classe cuja regra foi medida e reprovada — que é verificada separadamente para
     * a lacuna ficar visível em vez de virar silêncio.
     */
    @Test
    @DisplayName("captura: todo defeito confirmado é barrado, exceto a lacuna declarada")
    void todoDefeitoConfirmadoEhBarrado() {
        List<String> escaparam = new ArrayList<>();
        int barrados = 0;
        for (Caso caso : carregarCorpus()) {
            if (!caso.defeito() || CLASSE_LACUNA_DECLARADA.equals(caso.classe())) {
                continue;
            }
            try {
                validador.validarPar(caso.original(), caso.traduzido());
                escaparam.add("[" + caso.classe() + "] \"" + caso.original() + "\" -> \"" + caso.traduzido() + "\"");
            } catch (AlucinacaoDetectadaException e) {
                barrados++;
            }
        }
        assertTrue(escaparam.isEmpty(),
            () -> "Defeitos CONFIRMADOS na legenda entregue que a validação de par deixou passar:\n  "
                + String.join("\n  ", escaparam));
        assertEquals(26, barrados,
            "o corpus tem 27 defeitos confirmados e 1 lacuna declarada (CONTRAÇÃO); "
                + "mudou a contagem, atualize o corpus e a medição junto");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: registra, de forma executável, que a contração NÃO é coberta. Se
     * alguém implementar a regra depois, este teste falha e obriga a revisitar a medição em
     * vez de deixar a lacuna virar surpresa.
     */
    @Test
    @DisplayName("lacuna declarada: perda de conteúdo por contração ainda NÃO é detectada")
    void contracaoSegueDescoberta() {
        // Caso real, publicado no ep06: a fala inteira virou a interjeição final.
        validador.validarPar("And 200 meters below the dam, voilà!", "Voilà!");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: prende os três defeitos que o detector da primeira versão perdeu,
     * cada um por um motivo diferente. Sem eles o corpus não provaria as correções que a
     * contra-auditoria exigiu.
     */
    @Test
    @DisplayName("os três defeitos que a primeira versão do detector perdeu")
    void defeitosQueAPrimeiraVersaoPerdeu() {
        // Prefixo com DÍGITO: o regex original exigia prefixo em forma de nome próprio.
        assertThrows(AlucinacaoDetectadaException.class,
            () -> validador.validarPar("Done!", "Linha 1:"));
        // Abaixo do piso de 25 caracteres que a primeira versão usava (esta tem 23).
        assertThrows(AlucinacaoDetectadaException.class,
            () -> validador.validarPar("I...", "Eu sou o rei do mundo!"));
        // Meta-resposta AFIRMATIVA: não é recusa, então escapava do padrão de recusa/meta.
        assertThrows(AlucinacaoDetectadaException.class,
            () -> validador.validarPar("Menjo!", "Minha tradução para a sua linha é: \"Menjo!\""));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: par sem material comparável não pode gerar pendência inventada.
     */
    @Test
    @DisplayName("sem par comparável não há acusação: nulo, vazio e fala só de tags passam")
    void semParComparavelNaoAcusa() {
        validador.validarPar(null, "qualquer coisa");
        validador.validarPar("anything", null);
        validador.validarPar("", "Uma tradução longa que não tem original para ancorar nada.");
        validador.validarPar("{\\pos(0,0)}", "{\\an8}Uma tradução longa sem texto visível no original.");
    }
}
