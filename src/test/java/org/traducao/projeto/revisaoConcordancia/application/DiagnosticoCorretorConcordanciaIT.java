package org.traducao.projeto.revisaoConcordancia.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.raspagemRevisao.application.DetectorConcordanciaService;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fotografar o que a tela 3.3 (Revisão de Concordância) DE FATO faz hoje
 * com um conjunto de falas de resposta conhecida — as que ela deve corrigir, as que ela deve
 * deixar em paz, e as que o detector acusa mas nada conserta. É diagnóstico, não contrato: por
 * isso IMPRIME o veredito em vez de reprovar, e fica desligado sem
 * {@code -Dkronos.medicao=true}.
 *
 * <h2>Por que não é um teste comum</h2>
 * Um teste que reprova serve para congelar comportamento JÁ correto. Aqui a pergunta é outra —
 * <i>o que está errado hoje?</i> —, e transformar cada suspeita em reprovação deixaria a suíte
 * vermelha antes de existir decisão sobre o conserto. Quando o conserto for aprovado, os casos
 * marcados {@code DEVE MANTER}/{@code DEVE CORRIGIR} viram teste de verdade, no mesmo commit.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Pergunta aos objetos de PRODUÇÃO ({@link CorretorConcordanciaGeneroService} e
 *       {@link DetectorConcordanciaService}) — nenhum padrão é reimplementado aqui. Copiar o
 *       critério é o erro que a regra da medição existe para impedir.</li>
 *   <li>O detector é chamado com original {@code null} de propósito: é assim que a 3.3 vive —
 *       PT-only, sem inglês e sem cache. Só as regras internas ao português respondem.</li>
 *   <li><b>Controle positivo obrigatório:</b> se o corretor não corrigir NENHUM dos casos
 *       sabidamente doentes, o instrumento reprova — instrumento cego não produz diagnóstico,
 *       produz zero com cara de saúde.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nunca lança por causa de uma fala; a única reprovação possível é a do controle positivo.
 *
 * <p>Uso: {@code gradlew test --tests "*DiagnosticoCorretorConcordanciaIT*" "-Dkronos.medicao=true"}
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class DiagnosticoCorretorConcordanciaIT {

    private final CorretorConcordanciaGeneroService corretor = new CorretorConcordanciaGeneroService();
    private final DetectorConcordanciaService detector = new DetectorConcordanciaService();

    /** O que se espera do caso — a coluna que transforma saída em veredito. */
    private enum Esperado {
        /** Erro real de gênero: o corretor TEM de mexer. */
        CORRIGIR,
        /** Fala correta: o corretor NÃO pode mexer. */
        MANTER
    }

    private record Caso(String familia, Esperado esperado, String texto) {}

    private static final List<Caso> CASOS = List.of(
        // ---------------- famílias que o corretor de hoje já cobre ----------------
        new Caso("artigo masc + subst fem", Esperado.CORRIGIR, "Vi o menina no parque."),
        new Caso("artigo fem + subst masc", Esperado.CORRIGIR, "Chamei uma menino."),
        new Caso("predicativo de ela", Esperado.CORRIGIR, "Ela está cansado."),
        new Caso("predicativo de ele", Esperado.CORRIGIR, "Ele parece perdida."),

        // ---------------- famílias que o DETECTOR acusa e nada corrige ----------------
        new Caso("adjetivo anteposto", Esperado.CORRIGIR, "Chegou o meu menina."),
        new Caso("adjetivo posposto", Esperado.CORRIGIR, "A menina parecia cansado."),
        new Caso("substantivo + adj fem", Esperado.CORRIGIR, "O rapaz estava perdida."),
        new Caso("pronome obliquo", Esperado.CORRIGIR, "Entreguei o ela."),
        new Caso("plural: elas + masc", Esperado.CORRIGIR, "Elas estão cansados."),
        new Caso("plural: eles + fem", Esperado.CORRIGIR, "Eles estão cansadas."),
        new Caso("plural nominal", Esperado.CORRIGIR, "As menino chegaram."),
        new Caso("idiomatica", Esperado.CORRIGIR, "Gracas ao deus, voce esta vivo."),

        // ---------------- falas CORRETAS: o corretor não pode encostar ----------------
        new Caso("preposicao 'a' + masc", Esperado.MANTER, "Graças a Deus, você está vivo."),
        new Caso("preposicao 'a' + masc", Esperado.MANTER, "Vamos conversar de homem a homem."),
        new Caso("preposicao 'a' + masc", Esperado.MANTER, "Diga a senhor Kelley que cheguei."),
        new Caso("preposicao 'a' + masc", Esperado.MANTER, "Ele voltou a filho pródigo nenhum."),
        new Caso("concordancia correta", Esperado.MANTER, "A menina está cansada."),
        new Caso("1a/2a pessoa", Esperado.MANTER, "Você está certo, ela está cansada."),
        new Caso("sujeito omitido", Esperado.MANTER, "Estou pronto para partir."),
        new Caso("nome proprio", Esperado.MANTER, "O Deus da guerra não perdoa."),
        new Caso("tag ASS preservada", Esperado.MANTER, "{\\i1}A menina chegou."),
        new Caso("quebra \\N no meio", Esperado.MANTER, "A menina\\Nestá cansada.")
    );

    @Test
    @DisplayName("diagnostico: o que a 3.3 corrige, o que ela ignora e o que ela estraga")
    void diagnosticar() {
        int acertos = 0;
        int erros = 0;
        int semCorretor = 0;

        System.out.printf("%n%-26s %-9s %-40s %s%n", "FAMILIA", "ESPERADO", "ENTRADA", "SAIDA DO CORRETOR");
        System.out.println("-".repeat(120));

        for (Caso caso : CASOS) {
            Optional<String> saida = corretor.corrigir(caso.texto());
            boolean mexeu = saida.isPresent();
            String veredito;
            if (caso.esperado() == Esperado.CORRIGIR) {
                if (mexeu) {
                    veredito = "[ok] corrigiu";
                    acertos++;
                } else {
                    veredito = "[GAP] detecta e nao corrige";
                    semCorretor++;
                }
            } else {
                if (mexeu) {
                    veredito = "[DEFEITO] estragou fala correta";
                    erros++;
                } else {
                    veredito = "[ok] manteve";
                    acertos++;
                }
            }
            System.out.printf("%-26s %-9s %-40s %s%n", caso.familia(), caso.esperado(),
                recortar(caso.texto()), veredito);
            if (mexeu) {
                System.out.printf("%59s-> %s%n", "", saida.get());
            }
        }

        System.out.printf("%n%-14s %d%n%-14s %d%n%-14s %d%n", "acertos", acertos,
            "GAPS", semCorretor, "DEFEITOS", erros);

        System.out.printf("%n%s%n", "O QUE O DETECTOR PT-ONLY ACUSA (original null, como a 3.3 vive):");
        for (Caso caso : CASOS) {
            ResultadoDeteccaoConcordancia r = detector.analisar(null, caso.texto());
            boolean corrigivel = corretor.corrigir(caso.texto()).isPresent();
            if (r.suspeito()) {
                System.out.printf("  %-40s %-14s %s%n", recortar(caso.texto()),
                    corrigivel ? "TEM corretor" : "SEM CORRETOR", r.motivos().get(0));
            }
        }

        assertTrue(acertos > 0, "NAO VERIFICADO (controle positivo): o instrumento nao viu o "
            + "corretor agir em caso nenhum, entao os numeros acima nao medem o corretor — "
            + "medem o silencio dele.");
    }

    private static String recortar(String t) {
        String visivel = t.replace("\\N", "|N|");
        return visivel.length() <= 38 ? visivel : visivel.substring(0, 38) + "…";
    }
}
