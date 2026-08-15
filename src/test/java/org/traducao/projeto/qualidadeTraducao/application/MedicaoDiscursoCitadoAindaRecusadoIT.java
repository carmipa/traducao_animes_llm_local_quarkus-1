package org.traducao.projeto.qualidadeTraducao.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: diz quantas das falas de DISCURSO CITADO que o histórico registra
 * como recusadas ainda seriam recusadas pelo validador de HOJE. É o número que
 * {@code ConectivoDeDiscursoNaoEhLocutorInventadoTest#oLimiteQuePermanece} exige antes de
 * alargar o padrão, e que nunca tinha sido levantado.
 *
 * <h2>De onde vêm os casos</h2>
 * Das 296 linhas de recusa por "locutor inventado" em {@code logs/console-web.log}, que
 * deduplicadas dão 75 falas distintas. Destas, 10 trazem discurso citado nos DOIS lados —
 * aspas no original e aspas depois dos dois-pontos na tradução — e são as reproduzidas
 * abaixo, byte a byte como o log as registrou.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Pergunta ao validador REAL. Nenhum critério é reimplementado aqui.</li>
 *   <li>Não afirma que recusar é errado: apenas separa o que a allowlist de hoje já poupa do
 *       que continua caindo, para a decisão de alargar ter denominador.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Não reprova pelo número — ele é o objeto da medição. A saída vai para o stdout do teste.
 */
@QuarkusTest
@DisplayName("MEDIÇÃO: discurso citado que o validador de hoje ainda recusa")
class MedicaoDiscursoCitadoAindaRecusadoIT {

    /** Os 10 pares distintos com discurso citado nos dois lados, extraídos do log. */
    private static final List<String[]> CITACOES = List.of(
        new String[] { "And he said, \"I climb the mountain because it is there.",
            "E ele disse: \"Eu escalo a montanha porque ela está lá.\"" },
        new String[] { "I just said, \"You want to meet Char, don't you?\"",
            "Eu acabei de dizer: \"Você quer conhecer o Char, não é?\"." },
        new String[] { "I tell them, \"You're able to grow up because you have a father.\"",
            "Eu lhes digo: \"Vocês podem crescer porque têm um pai.\"" },
        new String[] { "Just tell him, \"I'm happy as long as you're alive\".",
            "Diga a ele: \"Estou feliz desde que voce esteja vivo.\"" },
        new String[] { "Remember this, \"A bird in a cage is nothing but a tool for one's enjoyment.\"",
            "Lembre-se disso: \"Um pássaro em uma gaiola não é nada além de um objeto para o prazer de alguém.\"" },
        new String[] { "Rygart boasted, \"Tonight's the night!\"",
            "Rygart exclamou: \"Esta é a noite!\"" },
        new String[] { "She means, \"After we're killed.\"",
            "Ela quer dizer: \"Depois que fomos mortos.\"" },
        new String[] { "She'll greet you with, \"And a fine morning to you!\"",
            "Ela vai lhe cumprimentar com: \"E um bom dia para você!\"" },
        new String[] { "The saying goes, \"make haste slowly\".",
            "A sabedoria popular diz: \"Apresse-se devagar.\"" },
        new String[] { "Then you say \"Please, help yourself.\"",
            "Então você diz: \"Por favor, sirva-se.\"" }
    );

    @Inject
    ValidadorTraducaoService validador;

    @Test
    void quantasCitacoesAindaCaem() {
        int recusadas = 0;
        System.out.println();
        System.out.println("=== DISCURSO CITADO x VALIDADOR DE HOJE ===");
        for (String[] par : CITACOES) {
            String veredito;
            try {
                validador.validarPar(par[0], par[1]);
                veredito = "PASSA ";
            } catch (RuntimeException e) {
                veredito = "RECUSA";
                recusadas++;
            }
            System.out.println("  " + veredito + "  EN: " + par[0]);
            System.out.println("          PT: " + par[1]);
        }
        System.out.println();
        System.out.println("  total ..... " + CITACOES.size());
        System.out.println("  ainda cai . " + recusadas);
        System.out.println("  ja poupada  " + (CITACOES.size() - recusadas));
    }
}
