package org.traducao.projeto.auditoria;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * PROPÓSITO DE NEGÓCIO: responde "achatar antes de traduzir vale a pena?" com o único critério
 * que decide — em quantos estilos do acervo o achatamento seria a ÚNICA proteção contra a
 * camada de karaokê ir ao LLM.
 *
 * <h2>Por que a pergunta não se responde no Unicorn</h2>
 * Medido em 12/08/2026: lá o achatamento não mudou nada na tradução (eco 183 × 181, tempo
 * 30,4 × 30,2 min, pendências 2 × 2). Mas isso NÃO significa que ele é inútil — significa que
 * naquela obra outra proteção já cobria o caso: {@code OPL2} está na lista nominal do
 * {@code application.yml}. O achatamento foi uma rede de segurança que não precisou ser
 * acionada.
 *
 * <p>O valor dele, portanto, está nas obras onde essa rede é a única que existe: estilo com
 * volume alto de eventos que <b>nenhum</b> dos dois vetos alcança.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Os dois vetos são os REAIS da produção — {@link DetectorEfeitoKaraokeService} (forma e
 *       conteúdo) e {@link PoliticaEstiloMusical} com a lista lida do yml. Reimplementá-los
 *       aqui produziria a divergência que a regra da medição existe para impedir.</li>
 *   <li>Só reporta estilo com volume compatível com camada de karaokê. Estilo de diálogo tem
 *       muitos eventos e é normal; o que caracteriza KFX é o volume ALIADO à ausência de veto.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem o inventário do acervo, PULA por {@link Assumptions} — "não verifiquei" nunca sai com a
 * mesma cara de "não há nada".
 */
@DisplayName("medição: em que estilos do acervo o achatamento seria a única proteção")
class OndeOAchatamentoValeriaIT {

    private static final Path ACERVO = Path.of("C:", "animes");

    @Test
    @DisplayName("lista estilos sem NENHUM dos dois vetos, por volume de eventos")
    void estilosQueSoOAchatamentoPegaria() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(ACERVO), "acervo ausente — NÃO VERIFICADO");

        var detector = new DetectorEfeitoKaraokeService();
        var politica = new PoliticaEstiloMusical(MedicaoUnicornMistralXAyaIT.estilosIgnoradosDoYml());

        // estilo -> [eventos, eventos com assinatura de KFX]
        Map<String, int[]> porEstilo = new LinkedHashMap<>();
        List<Path> arquivos;
        try (var s = Files.walk(ACERVO)) {
            arquivos = s.filter(p -> p.toString().endsWith(".ass")).toList();
        }
        Assumptions.assumeFalse(arquivos.isEmpty(), "nenhum .ass no acervo — NÃO VERIFICADO");

        for (Path arq : arquivos) {
            List<String> linhas;
            try {
                linhas = Files.readAllLines(arq);
            } catch (Exception e) {
                continue;
            }
            for (String linha : linhas) {
                if (!linha.startsWith("Dialogue:")) {
                    continue;
                }
                String[] c = linha.substring(9).split(",", 10);
                if (c.length < 10) {
                    continue;
                }
                String estilo = c[3].trim();
                String texto = c[9];
                int[] v = porEstilo.computeIfAbsent(estilo, k -> new int[2]);
                v[0]++;
                // Assinatura de camada de efeito: transformação animada ou posicionamento, com
                // texto visível ínfimo. É o que o achatador descarta.
                if (detector.eEfeitoKaraoke(texto)) {
                    v[1]++;
                }
            }
        }

        List<String> descobertos = new ArrayList<>();
        int totalEventosDescobertos = 0;
        for (var e : porEstilo.entrySet()) {
            String estilo = e.getKey();
            int eventos = e.getValue()[0];
            int comKfx = e.getValue()[1];
            boolean vetadoPorForma = detector.eEstiloDeMusica(estilo) || detector.eEstiloDeRomaji(estilo);
            boolean vetadoPorNome = politica.estiloIgnorado(estilo);
            if (vetadoPorForma || vetadoPorNome) {
                continue;
            }
            if (comKfx >= 50) {
                descobertos.add(String.format("  %-28s eventos=%-7d comKFX=%-7d", estilo, eventos, comKfx));
                totalEventosDescobertos += comKfx;
            }
        }

        System.out.println("\n=== ESTILOS SEM NENHUM DOS DOIS VETOS, com assinatura de efeito >= 50 ===");
        System.out.println("  (acervo: " + arquivos.size() + " arquivos, " + porEstilo.size() + " estilos distintos)");
        descobertos.forEach(System.out::println);
        System.out.println("  soma bruta: " + totalEventosDescobertos);
        System.out.println("""

              LIMITE DECLARADO DESTE INSTRUMENTO — ler antes de usar o numero acima.
              eEfeitoKaraoke reconhece "saida de template": transformacao animada com texto
              visivel infimo. LETREIRO animado tem a MESMA assinatura, e o proprio Javadoc do
              detector avisa disso. Logo, esta lista mistura duas coisas:

                (a) camada de musica com nome proprio de cancao — Paradise, Swimsuit,
                    HestiaFamilia, NipSlip, Flower, EVERYTHING, Welcome, EG, 08thMS,
                    Transformation, Hey World English. AQUI o achatamento seria a unica
                    protecao, porque nenhum dos dois vetos alcanca o nome.

                (b) typesetting e letreiro — Signs, Zeta Episode Title, Mask, Logo, Title,
                    Next Ep. NAO sao karaoke, e para eles o pipeline ja tem a heuristica de
                    letreiro animado por repeticao. Somar (b) ao total infla a resposta.

              Separar (a) de (b) por automacao exigiria uma lista minha de nomes, que e
              exatamente o tipo de segunda implementacao que a regra da medicao proibe. A
              separacao acima foi feita por LEITURA dos nomes, e esta declarada como tal.
              """);

        assertFalse(porEstilo.isEmpty(), "instrumento cego: nenhum estilo lido do acervo");
    }
}
