package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.medicao.LeitorAcervoCache.Acervo;
import org.traducao.projeto.medicao.LeitorAcervoCache.FalaDoAcervo;
import org.traducao.projeto.revisaoLore.application.DetectorTermosLoreService;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * PROPÓSITO DE NEGÓCIO: medir quantas pendências de lore o detector produz, separando as falas
 * QUE TÊM a quebra {@code \N} das que não têm — que é a única divisão em que a mecânica da quebra
 * pode mudar alguma coisa.
 *
 * <h2>Para que serve o número</h2>
 * O detector é read-only e alimenta a revisão humana. Torná-lo mais sensível só é um ganho se as
 * pendências novas forem REAIS; se ele passar a acusar em massa, some a revisão junto. Este
 * harness existe para responder isso com contagem, e não com impressão: rodando-o antes e depois
 * de uma mudança na mecânica da quebra, a diferença nas falas COM quebra é o efeito da mudança, e
 * a diferença nas falas SEM quebra tem de ser ZERO — se não for, a mudança vazou para onde não
 * devia.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY. Chama o serviço de produção pelo CDI, não uma cópia da regra.</li>
 *   <li>Usa a sobrecarga SEM lore ativa, de propósito: mede a régua global, que é a mais
 *       sensível. Com lore por obra os números caem, e aí a comparação antes/depois passaria a
 *       depender de qual contexto está carregado.</li>
 *   <li>A coluna "sem quebra" é o GRUPO DE CONTROLE. Mudança na mecânica da quebra que a mova
 *       está errada por construção.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo ausente termina com aviso; não lança.
 *
 * <p>Uso: {@code gradlew test --tests "*MedicaoLoreQuebraIT*" "-Dkronos.medicao=true"}
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoLoreQuebraIT {

    private static final int TETO_MOTIVOS = 12;

    @Inject
    DetectorTermosLoreService detector;

    @Test
    @DisplayName("acervo: pendencias de lore, separadas por presenca da quebra \\N")
    void medir() throws IOException {
        Acervo acervo = LeitorAcervoCache.ler(LeitorAcervoCache.raizPadrao());
        if (acervo.vazio()) {
            System.out.println("SEM ACERVO — nada medido.");
            return;
        }

        int comQuebra = 0;
        int semQuebra = 0;
        int pendenteComQuebra = 0;
        int pendenteSemQuebra = 0;
        int motivosComQuebra = 0;
        Map<String, Integer> porMotivo = new TreeMap<>();

        for (FalaDoAcervo f : acervo.falas()) {
            if (f.original() == null || f.original().isBlank()
                || f.traduzido() == null || f.traduzido().isBlank()) {
                continue;
            }
            boolean temQuebra = f.original().contains("\\N") || f.traduzido().contains("\\N");
            ResultadoDeteccaoLore r = detector.auditar(f.original(), f.traduzido());
            if (temQuebra) {
                comQuebra++;
                if (r.suspeito()) {
                    pendenteComQuebra++;
                    motivosComQuebra += r.motivos().size();
                    r.motivos().forEach(m -> porMotivo.merge(rotulo(m), 1, Integer::sum));
                }
            } else {
                semQuebra++;
                if (r.suspeito()) {
                    pendenteSemQuebra++;
                }
            }
        }

        System.out.printf("%n%-18s %10s %12s %9s%n", "GRUPO", "falas", "com pendencia", "%");
        System.out.printf("%-18s %10d %12d %8.1f%%%n", "COM quebra \\N", comQuebra, pendenteComQuebra,
            comQuebra == 0 ? 0 : 100.0 * pendenteComQuebra / comQuebra);
        System.out.printf("%-18s %10d %12d %8.1f%%   <- CONTROLE: nao pode mudar%n",
            "SEM quebra", semQuebra, pendenteSemQuebra,
            semQuebra == 0 ? 0 : 100.0 * pendenteSemQuebra / semQuebra);
        System.out.printf("%nmotivos emitidos nas falas COM quebra: %d%n%n", motivosComQuebra);

        porMotivo.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
            .limit(TETO_MOTIVOS)
            .forEach(e -> System.out.printf("  %5d  %s%n", e.getValue(), e.getKey()));
    }

    /** Agrupa o motivo pelo texto fixo, descartando o termo citado entre aspas. */
    private static String rotulo(String motivo) {
        int aspas = motivo.indexOf('"');
        return aspas < 0 ? motivo : motivo.substring(0, aspas).strip();
    }
}
