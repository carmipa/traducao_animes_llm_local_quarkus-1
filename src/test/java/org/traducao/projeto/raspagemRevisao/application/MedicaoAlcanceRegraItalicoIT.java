package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.traducao.projeto.qualidadeTraducao.application.RemovedorItalico;

/**
 * PROPÓSITO DE NEGÓCIO: medir, no ACERVO REAL e com as classes de PRODUÇÃO, quanto a regra do
 * itálico (22/08/2026) alcança — quantas falas mudariam, de quantas a regra SE ABSTEVE e
 * quantas são música e não podem ser tocadas.
 *
 * <p>É o instrumento que dimensiona a varredura RETROATIVA do acervo já traduzido: sem ele,
 * decidir se milhares de arquivos devem ser reescritos seria chute — e chute sobre número foi a
 * causa das 14 medições erradas de 07/08/2026, entre elas uma que errou por 8x dentro da
 * própria medição criada para provar esse ponto.
 *
 * <p>É também o instrumento que confere a telemetria: {@code falasItalicoRemovido} e
 * {@code falasItalicoPreservado} do episódio têm de bater com o que este harness conta no
 * arquivo. Número que só a própria feature sabe calcular não é medição, é autoafirmação.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Usa {@link RemovedorItalico}, {@link LeitorLegendaAss} e
 *       {@link DetectorEfeitoKaraokeService} de PRODUÇÃO. Não reimplementa nem o removedor, nem
 *       a leitura do ASS, nem o critério de música: a segunda implementação sempre diverge da
 *       primeira.</li>
 *   <li>NÃO escreve nada. Lê e imprime.</li>
 *   <li>O número é um <b>TETO</b>, e o harness diz isso na saída: a lista
 *       {@code estilos-ignorados} do {@code application.yml} não é aplicada aqui. Ela é
 *       CONFIGURAÇÃO do operador, não critério de código, e quem a aplica no pipeline é o
 *       {@code SeletorEventosTraduziveis}. Medido em 22/08/2026: os estilos
 *       {@code "Mobile Suit Gundam"} (136) e {@code "Char's Counterattack"} entram na conta
 *       aqui e ficam de fora na produção.</li>
 *   <li>Travado por {@code -Dkronos.medicao=true}: lê o acervo real, que não existe no CI.</li>
 *   <li>Acervo ausente <b>REPROVA</b>, não pula. Resultado vazio por acervo inacessível não
 *       pode ser lido como "não há itálico no acervo".</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Arquivo ilegível é CONTADO e reportado, nunca descartado em silêncio — guarda que descarta o
 * que não entende aprova por cegueira.
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoAlcanceRegraItalicoIT {

    private static final Path ACERVO =
        Path.of(System.getProperty("kronos.acervo", "C:\\animes"));

    /** A pasta de SAÍDA do pipeline: é a legenda entregue, que é o que a regra governa. */
    private static final String PASTA_PT = "traducao_ptbr";

    @Test
    @DisplayName("mede o alcance da regra do italico no acervo ja traduzido")
    void medeOAlcanceDaRegraNoAcervo() throws IOException {
        assertTrue(Files.isDirectory(ACERVO),
            "acervo inacessivel em " + ACERVO + " — sem ele o resultado vazio significaria "
                + "\"nao consegui medir\", nunca \"nao ha italico no acervo\"");

        RemovedorItalico removedor = new RemovedorItalico();
        LeitorLegendaAss leitor = new LeitorLegendaAss();
        // QUEM DECIDE o que a 3.1 toca e o FiltroAuditoriaLinha, nao o detector de karaoke
        // cru: ele veta musica, karaoke, conteudo VETORIAL e efeito protegido. E a diferenca
        // entre "quantas falas tem italico" e "quantas a tela do refinamento vai limpar" —
        // sao perguntas diferentes, e so a segunda dimensiona a operacao.
        //
        // A lista estilos-ignorados vai VAZIA: e configuracao do operador, e o proprio
        // application.yml e quem a preenche em producao. Por isso o numero daqui e TETO.
        FiltroAuditoriaLinha filtro = new FiltroAuditoriaLinha(
            new MascaradorTags(), new PoliticaEstiloMusical(List.of()),
            new DetectorEfeitoKaraokeService(), new ProtecaoLegendaAssService());

        List<Path> arquivos = new ArrayList<>();
        try (Stream<Path> caminhada = Files.walk(ACERVO)) {
            caminhada.filter(Files::isRegularFile)
                .filter(p -> p.getParent() != null
                    && PASTA_PT.equals(p.getParent().getFileName().toString()))
                .filter(p -> {
                    String nome = p.getFileName().toString().toLowerCase();
                    return nome.endsWith(".ass") && !nome.endsWith(".parcial.ass");
                })
                .forEach(arquivos::add);
        }
        assertTrue(!arquivos.isEmpty(),
            "nenhum " + PASTA_PT + "/*.ass encontrado sob " + ACERVO
                + " — instrumento cego, nao acervo limpo");

        int comTag = 0;
        int mudariam = 0;
        int abstencoes = 0;
        int vetadasPeloFiltro = 0;
        int ilegiveis = 0;
        Map<String, Integer> porObra = new LinkedHashMap<>();
        Map<String, Integer> porEstilo = new LinkedHashMap<>();
        Map<String, Integer> abstencaoPorObra = new LinkedHashMap<>();
        List<String> amostra = new ArrayList<>();

        for (Path arquivo : arquivos) {
            List<EventoLegenda> eventos;
            try {
                eventos = leitor.ler(arquivo).eventos();
            } catch (RuntimeException e) {
                ilegiveis++;
                continue;
            }
            Path pastaObra = arquivo.getParent().getParent();
            String obra = pastaObra == null ? "?" : pastaObra.getFileName().toString();
            for (EventoLegenda evento : eventos) {
                String texto = evento.texto();
                if (texto == null || texto.indexOf('{') < 0) {
                    continue;
                }
                comTag++;
                String limpo = removedor.remover(texto);
                boolean mudaria = !limpo.equals(texto);
                if (filtro.deveIgnorarLinha(evento)) {
                    if (mudaria) {
                        vetadasPeloFiltro++;
                    }
                    continue;
                }
                if (mudaria) {
                    mudariam++;
                    porObra.merge(obra, 1, Integer::sum);
                    porEstilo.merge(evento.estilo(), 1, Integer::sum);
                    if (amostra.size() < 8) {
                        amostra.add("[" + evento.estilo() + "] " + recorte(texto)
                            + "  ->  " + recorte(limpo));
                    }
                } else if (removedor.temItalico(texto)) {
                    abstencoes++;
                    abstencaoPorObra.merge(obra, 1, Integer::sum);
                }
            }
        }

        System.out.println("=== ALCANCE DA REGRA DO ITALICO NO ACERVO ===");
        System.out.println("arquivos " + PASTA_PT + " lidos : " + arquivos.size()
            + (ilegiveis > 0 ? "  (" + ilegiveis + " ILEGIVEIS)" : ""));
        System.out.println("falas com bloco de tag      : " + comTag);
        System.out.println("A 3.1 LIMPARIA              : " + mudariam);
        System.out.println("ABSTENCOES (i0 do Style)    : " + abstencoes);
        System.out.println("VETADAS pelo filtro da 3.1  : " + vetadasPeloFiltro + "  (musica, karaoke, vetorial, efeito protegido)");
        imprimir("por obra — a 3.1 limparia", porObra);
        imprimir("por obra — ABSTENCOES (onde a regra nao alcanca)", abstencaoPorObra);
        imprimir("por estilo — a 3.1 limparia", porEstilo);
        System.out.println("--- AVISO DE ESCOPO DO INSTRUMENTO:");
        System.out.println("   a lista 'estilos-ignorados' do application.yml NAO e aplicada aqui.");
        System.out.println("   Ela e CONFIGURACAO do operador, nao criterio de codigo, e o pipeline a");
        System.out.println("   aplica no SeletorEventosTraduziveis. Logo este numero e um TETO: estilos");
        System.out.println("   como 'Mobile Suit Gundam' e \"Char's Counterattack\" aparecem no quadro por");
        System.out.println("   estilo acima e na producao ficam de fora. Confira o quadro antes de somar.");
        System.out.println("--- amostra:");
        amostra.forEach(linha -> System.out.println("   " + linha));
    }

    private static void imprimir(String titulo, Map<String, Integer> mapa) {
        System.out.println("--- " + titulo + ":");
        if (mapa.isEmpty()) {
            System.out.println("   (nenhum)");
            return;
        }
        mapa.entrySet().stream()
            .sorted((x, y) -> Integer.compare(y.getValue(), x.getValue()))
            .forEach(e -> System.out.println("   " + e.getValue() + "  " + e.getKey()));
    }

    private static String recorte(String texto) {
        String umaLinha = texto.replace("\\N", " ");
        return umaLinha.length() <= 70 ? umaLinha : umaLinha.substring(0, 70) + "...";
    }
}
