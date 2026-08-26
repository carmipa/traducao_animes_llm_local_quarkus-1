package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.medicao.LeitorAcervoCache.Acervo;
import org.traducao.projeto.medicao.LeitorAcervoCache.FalaDoAcervo;
import org.traducao.projeto.raspagemRevisao.application.DetectorConcordanciaService;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * PROPÓSITO DE NEGÓCIO: medir a taxa de disparo do detector de concordância sobre o acervo —
 * quantas falas ele elege para revisão, e por qual regra.
 *
 * <h2>Por que este número decide alguma coisa</h2>
 * Este detector não é um relatório: é o PORTÃO da revisão. Em
 * {@code RevisarCacheUseCase} ele escolhe o que vai ao LLM e, depois, decide se o resultado
 * ficou pendente. Taxa alta demais gasta LLM com fala correta; taxa perto de zero significa que
 * 58 expressões regulares estão paradas e a revisão de concordância é decorativa. Nenhuma das
 * duas hipóteses se resolve lendo o código.
 *
 * <h2>A medição que este harness SUBSTITUI</h2>
 * Em 2026-08-05 eu estimei "2 disparos em 67.778 falas" reconstruindo QUATRO dos padrões num
 * script. O serviço tem muito mais regra do que quatro, e o número media o script, não o
 * detector. Aqui quem responde é o próprio serviço, resolvido pelo CDI.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY. Chama {@code analisar} exatamente como {@code RevisarCacheUseCase} chama:
 *       original em inglês e tradução, sem pré-tratamento.</li>
 *   <li>Separa as falas COM e SEM a quebra {@code \N}. É o mesmo corte que expôs o ruído do
 *       detector de lore; se as duas taxas divergirem muito, a quebra está mexendo aqui
 *       também.</li>
 *   <li>Agrupa o motivo pelo texto fixo, descartando o trecho citado, para o inventário ser de
 *       REGRAS e não de ocorrências.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo ausente termina com aviso; não lança.
 *
 * <p>Uso: {@code gradlew test --tests "*MedicaoConcordanciaIT*" "-Dkronos.medicao=true"}
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoConcordanciaIT {

    private static final int TETO_REGRAS = 15;
    private static final int TETO_EXEMPLOS = 8;

    @Inject
    DetectorConcordanciaService detector;

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE (regra 9) do instrumento desta medição — o próprio
     * {@link DetectorConcordanciaService} de produção, resolvido pelo CDI.
     *
     * <p>INVARIANTES DO DOMÍNIO: tem de ACUSAR uma concordância quebrada montada à mão e tem de
     * CALAR numa frase correta. Sem isso, "taxa de disparo 0%" significaria tanto "o acervo está
     * limpo" quanto "o detector não enxerga nada" — e este harness existe justamente para
     * afirmar essa taxa.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: imprime e devolve {@code false}; nenhuma taxa é afirmada.
     */
    private boolean instrumentoCalibrado() {
        var doente = detector.analisar("The ships are fast.", "As nave sao rapido.");
        var sao = detector.analisar("The ships are fast.", "As naves sao rapidas.");
        boolean acusaDoente = doente != null && !doente.motivos().isEmpty();
        boolean calaNoSao = sao != null && sao.motivos().isEmpty();
        if (acusaDoente && calaNoSao) {
            System.out.printf("  controle: acusa 'As nave sao rapido' (%d motivo(s)) · cala em "
                + "'As naves sao rapidas'%n", doente.motivos().size());
            return true;
        }
        System.out.printf("INSTRUMENTO REPROVADO NO CONTROLE — acusa doente=%s cala no sao=%s. "
            + "Nenhuma taxa e afirmada.%n", acusaDoente, calaNoSao);
        return false;
    }

    @Test
    @DisplayName("acervo: taxa de disparo do detector de concordancia, por regra")
    void medir() throws IOException {
        if (!instrumentoCalibrado()) {
            return;
        }
        Acervo acervo = LeitorAcervoCache.ler(LeitorAcervoCache.raizPadrao());
        if (acervo.vazio()) {
            System.out.println("NAO VERIFICADO: acervo de cache vazio — sem fala nenhuma nao ha "
                + "taxa de disparo a afirmar.");
            return;
        }

        int analisadas = 0;
        int comQuebra = 0;
        int semQuebra = 0;
        int suspeitasComQuebra = 0;
        int suspeitasSemQuebra = 0;
        int motivosTotal = 0;
        Map<String, Integer> porRegra = new TreeMap<>();
        List<String> exemplos = new ArrayList<>();

        for (FalaDoAcervo f : acervo.falas()) {
            if (f.traduzido() == null || f.traduzido().isBlank()) {
                continue;
            }
            analisadas++;
            boolean quebra = f.original() != null && f.original().contains("\\N")
                || f.traduzido().contains("\\N");
            if (quebra) {
                comQuebra++;
            } else {
                semQuebra++;
            }

            ResultadoDeteccaoConcordancia r = detector.analisar(f.original(), f.traduzido());
            if (!r.suspeito()) {
                continue;
            }
            if (quebra) {
                suspeitasComQuebra++;
            } else {
                suspeitasSemQuebra++;
            }
            motivosTotal += r.motivos().size();
            r.motivos().forEach(m -> porRegra.merge(rotulo(m), 1, Integer::sum));
            if (exemplos.size() < TETO_EXEMPLOS) {
                exemplos.add(String.format("  %s%n     EN  %s%n     PT  %s%n     ->  %s",
                    f.obra(), recortar(visivel(f.original())), recortar(visivel(f.traduzido())),
                    r.motivos().get(0)));
            }
        }

        int suspeitas = suspeitasComQuebra + suspeitasSemQuebra;
        System.out.printf("%nfalas analisadas ......... %d%n", analisadas);
        System.out.printf("SUSPEITAS ................ %d  (%.2f%%)  — motivos emitidos: %d%n%n",
            suspeitas, analisadas == 0 ? 0 : 100.0 * suspeitas / analisadas, motivosTotal);
        System.out.printf("%-16s %10s %12s %9s%n", "GRUPO", "falas", "suspeitas", "%");
        System.out.printf("%-16s %10d %12d %8.2f%%%n", "COM quebra \\N", comQuebra,
            suspeitasComQuebra, comQuebra == 0 ? 0 : 100.0 * suspeitasComQuebra / comQuebra);
        System.out.printf("%-16s %10d %12d %8.2f%%%n%n", "SEM quebra", semQuebra,
            suspeitasSemQuebra, semQuebra == 0 ? 0 : 100.0 * suspeitasSemQuebra / semQuebra);

        System.out.println("REGRAS QUE DISPARARAM:");
        porRegra.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
            .limit(TETO_REGRAS)
            .forEach(e -> System.out.printf("  %6d  %s%n", e.getValue(), e.getKey()));
        System.out.printf("%nregras distintas que dispararam: %d%n%n", porRegra.size());

        exemplos.forEach(System.out::println);
    }

    /** Agrupa o motivo pelo texto fixo, descartando o trecho citado. */
    private static String rotulo(String motivo) {
        int corte = motivo.indexOf(':');
        int aspas = motivo.indexOf('"');
        int fim = corte < 0 ? aspas : (aspas < 0 ? corte : Math.min(corte, aspas));
        return fim < 0 ? motivo : motivo.substring(0, fim).strip();
    }

    private static String visivel(String t) {
        return t == null ? "" : t.replaceAll("\\{[^}]*}", "").replace("\\N", " ").strip();
    }

    private static String recortar(String t) {
        return t.length() <= 62 ? t : t.substring(0, 62) + "…";
    }
}
