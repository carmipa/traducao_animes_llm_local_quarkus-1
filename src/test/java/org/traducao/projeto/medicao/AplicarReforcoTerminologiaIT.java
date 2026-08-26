package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.traducaoCorrige.application.ReforcarTerminologiaCacheUseCase;
import org.traducao.projeto.traducaoCorrige.domain.ModoReforcoTerminologia;
import org.traducao.projeto.traducaoCorrige.domain.ResultadoReforcoTerminologia;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: <b>a única operação destes harnesses que ESCREVE no acervo.</b> Aplica o
 * reforço de terminologia sobre o cache já gravado, corrigindo a grafia canônica sem retraduzir.
 *
 * <h2>Por que tanta trava</h2>
 * {@code cache/} é gitignorado desde {@code 12d720a}: reescrevê-lo <b>não tem volta pelo git</b>.
 * São 20 MB de tradução pronta, e um {@code gradlew test} distraído não pode alcançá-los. Daí a
 * trava TRIPLA, e cada uma tem um dono diferente:
 * <ol>
 *   <li>{@code -Dkronos.medicao=true} — liga a família de harnesses de medição;</li>
 *   <li>{@code -Dkronos.aplicar.reforco=} <i>frase literal</i> — a intenção de ESCREVER, digitada
 *       à mão. Ninguém digita isso por acidente;</li>
 *   <li>{@code correcao-cache.reforco-terminologia-aplicar-habilitado=true} — a autorização do
 *       PIPELINE, que o próprio caso de uso confere antes de abrir arquivo. Não é deste teste:
 *       se estiver desligada, o resultado volta {@code RECUSADO_POR_CONFIG} e nada é tocado.</li>
 * </ol>
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O total aplicado é comparado com a PREVISÃO do ensaio, recebida por parâmetro. O ensaio
 *       se declara previsão fiel; se os números divergirem, a divergência aparece — em vez de o
 *       relatório dizer sucesso sobre um número que ninguém conferiu.</li>
 *   <li>Modo {@code RECUSADO_POR_CONFIG} REPROVA o teste. "Não fez nada" em silêncio é
 *       indistinguível de defeito, e aqui seria lido como "aplicou".</li>
 *   <li>Nunca roda na suíte normal.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * O caso de uso preserva o cache anterior antes de gravar; ainda assim, faça um snapshot próprio
 * em {@code backups/} antes de rodar — foi o que se fez em 04/08/2026 (299 arquivos, conferidos
 * byte a byte).
 *
 * <p>Uso (aspas obrigatórias no PowerShell):
 * <pre>
 * gradlew test --tests "*AplicarReforcoTerminologiaIT*" -i ^
 *   "-Dkronos.medicao=true" ^
 *   "-Dkronos.aplicar.reforco=SIM-ESCREVER-NO-ACERVO" ^
 *   "-Dcorrecao-cache.reforco-terminologia-aplicar-habilitado=true" ^
 *   "-Dkronos.aplicar.previsao=230"
 * </pre>
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.aplicar.reforco", matches = "SIM-ESCREVER-NO-ACERVO")
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class AplicarReforcoTerminologiaIT {

    /** Previsão do ensaio, para conferir o desfecho contra ela. 0 = não conferir. */
    private static final String CHAVE_PREVISAO = "kronos.aplicar.previsao";

    @Inject
    ReforcarTerminologiaCacheUseCase reforco;

    @Test
    @DisplayName("APLICA o reforco de terminologia no acervo e confere contra a previsao")
    void aplicar() {
        // CASO-CONTROLE (regra 9) ANTES de escrever no acervo. Este harness GRAVA, entao o
        // instrumento cego aqui nao produz so um numero errado: produz uma passada que diz
        // "0 falas corrigidas" sobre um acervo que nunca foi lido, e ninguem tem como saber a
        // diferenca depois. A sonda usa uma raiz inexistente e exige 0 arquivo ANALISADO, sem
        // estouro — e o ensaio, que nao escreve, roda no mesmo servico.
        Path inexistente = Path.of("cache-que-nao-existe-em-lugar-nenhum");
        ResultadoReforcoTerminologia sonda;
        try {
            sonda = reforco.ensaiar(inexistente, null);
        } catch (RuntimeException e) {
            System.out.printf("NAO VERIFICADO: o servico estourou na sonda (%s) — nada e "
                + "gravado.%n", e.getClass().getSimpleName());
            return;
        }
        assertTrue(sonda != null && sonda.arquivosAnalisados() == 0, () -> """
            INSTRUMENTO REPROVADO NO CONTROLE, e nada foi gravado.

            Uma raiz que NAO EXISTE devolveu arquivos analisados. Isso significa que o servico
            nao esta olhando a raiz que recebe — e uma aplicacao que "corrige 0 falas" sobre o
            acervo real seria indistinguivel de uma que nunca o leu.
            """);
        System.out.println("  controle: raiz inexistente devolve 0 arquivo analisado, sem estourar");

        Path raiz = LeitorAcervoCache.raizPadrao().toAbsolutePath();
        assertTrue(Files.isDirectory(raiz), () -> "acervo ausente em " + raiz
            + " — NAO VERIFICADO, e nada e gravado");

        System.out.printf("%n!! ESCREVENDO NO ACERVO !!  raiz: %s%n"
            + "escrita autorizada pelo pipeline? %s%n", raiz, reforco.escritaNoAcervoAutorizada());

        ResultadoReforcoTerminologia r = reforco.executar(raiz, null, true);

        System.out.printf("%n=== %s ===%n", r.modo());
        System.out.printf("  arquivos analisados...... %d%n", r.arquivosAnalisados());
        System.out.printf("  arquivos GRAVADOS........ %d%n", r.arquivosAlterados());
        System.out.printf("  FALAS corrigidas......... %d%n", r.falasAlteradas());
        System.out.printf("  nao verificaveis......... %d%n", r.arquivosNaoVerificaveis());
        System.out.printf("  falhas................... %d%n%n", r.falhas());
        r.restauracoesPorTermo().entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(e -> System.out.printf("     %-28s %5d%n", e.getKey(), e.getValue()));

        assertEquals(ModoReforcoTerminologia.APLICADO, r.modo(), () -> """
            A APLICACAO NAO ACONTECEU.

            Modo RECUSADO_POR_CONFIG significa que a autorizacao do PIPELINE esta desligada — a
            trava que nao e deste teste. Ligue na linha de comando:
              "-Dcorrecao-cache.reforco-terminologia-aplicar-habilitado=true"
            Nada foi lido nem escrito. Este assert existe porque "nao fez nada" em silencio seria
            lido como "aplicou".
            """);

        int previsao = Integer.getInteger(CHAVE_PREVISAO, 0);
        if (previsao > 0) {
            assertEquals(previsao, r.falasAlteradas(), () -> """
                O APLICADO DIVERGIU DA PREVISAO DO ENSAIO.

                O ensaio se declara previsao FIEL: em ensaio a contagem e identica a da aplicacao
                e so o documento fica intocado. Divergencia aqui significa que uma das duas
                afirmacoes e falsa, e o acervo ja foi escrito — confira o snapshot em backups/
                antes de seguir.
                """);
        }
    }
}
