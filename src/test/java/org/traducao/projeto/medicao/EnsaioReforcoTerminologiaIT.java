package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.traducaoCorrige.application.ReforcarTerminologiaCacheUseCase;
import org.traducao.projeto.traducaoCorrige.domain.ResultadoReforcoTerminologia;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: responder "quantas falas do acervo JÁ GRAVADO seriam corrigidas pelas
 * regras de terminologia atuais?" — sem escrever nada.
 *
 * <h2>Por que pelo caso de uso REAL, e não por uma varredura própria</h2>
 * O {@code ReforcarTerminologiaCacheUseCase} faz mais do que casar termo: resolve o contexto pelo
 * carimbo de proveniência, BLOQUEIA arquivo cuja obra não confere com a lore, e pula cache sem
 * carimbo em pasta não reconhecida. Uma varredura própria contaria falas que o fluxo real
 * recusaria — seria previsão de outro programa.
 *
 * <p>O próprio {@code ensaiar} é declarado como previsão fiel: em ensaio a contagem é idêntica à
 * da aplicação e o documento fica intocado.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>ENSAIO. Chama {@link ReforcarTerminologiaCacheUseCase#ensaiar} — nunca {@code executar}
 *       com {@code aplicar=true}. Escrever no acervo é decisão do dono, e {@code cache/} é
 *       gitignorado desde {@code 12d720a}: apagá-lo ou reescrevê-lo não tem volta pelo git.</li>
 *   <li>NÃO roda na suíte normal: exige {@code -Dkronos.medicao=true}.</li>
 *   <li>O estado da flag de escrita é IMPRESSO junto do resultado. Ler "ensaio" sem saber se a
 *       escrita está ligada é a diferença entre previsão e relatório do que já aconteceu.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo ausente termina com aviso. Arquivos não verificáveis e falhas vêm no próprio resultado
 * e são impressos — não são zerados para a saída ficar bonita.
 *
 * <p>Uso: {@code gradlew test --tests "*EnsaioReforcoTerminologiaIT*" "-Dkronos.medicao=true"}
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class EnsaioReforcoTerminologiaIT {

    @Inject
    ReforcarTerminologiaCacheUseCase reforco;

    @Test
    @DisplayName("acervo: quantas falas as regras de terminologia atuais corrigiriam")
    void ensaiar() {
        Path raiz = LeitorAcervoCache.raizPadrao().toAbsolutePath();
        if (!Files.isDirectory(raiz)) {
            System.out.println("SEM ACERVO em " + raiz + " — nada ensaiado.");
            return;
        }
        System.out.printf("%nraiz: %s%nescrita no acervo autorizada? %s  (ENSAIO nao escreve"
                + " em nenhum caso)%n", raiz, reforco.escritaNoAcervoAutorizada());

        ResultadoReforcoTerminologia r = reforco.ensaiar(raiz, null);

        System.out.printf("%n=== ENSAIO — modo %s ===%n", r.modo());
        System.out.printf("  arquivos analisados...... %d%n", r.arquivosAnalisados());
        System.out.printf("  FALAS que mudariam....... %d   <- a PREVISAO%n", r.falasAlteradas());
        // arquivosAlterados conta arquivos EFETIVAMENTE GRAVADOS, e em ensaio nada e gravado —
        // e sempre 0 aqui, por construcao. Rotula-lo "arquivos que mudariam" fazia a saida dizer
        // "0 arquivos / 230 falas" lado a lado, que se le como "nada mudaria". O rotulo mente
        // mais rapido do que o numero.
        System.out.printf("  arquivos GRAVADOS........ %d   (ensaio nao grava: 0 por"
            + " construcao, nao e previsao)%n", r.arquivosAlterados());
        System.out.printf("  nao verificaveis......... %d  (sem carimbo em pasta desconhecida)%n",
            r.arquivosNaoVerificaveis());
        System.out.printf("  falhas................... %d%n", r.falhas());

        System.out.printf("%n  restauracoes por termo canonico:%n");
        r.restauracoesPorTermo().entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(e -> System.out.printf("     %-28s %5d%n", e.getKey(), e.getValue()));
        if (r.restauracoesPorTermo().isEmpty()) {
            System.out.println("     (nenhuma) — as regras atuais nao alcancam o cache gravado");
        }
    }
}
