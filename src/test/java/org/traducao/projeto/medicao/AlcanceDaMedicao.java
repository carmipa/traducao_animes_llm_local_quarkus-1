package org.traducao.projeto.medicao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: dono único da pergunta <i>"que pastas do acervo esta medição vai ler?"</i>
 * — com o filtro por obra respeitado e o vazio declarado como NÃO VERIFICADO.
 *
 * <h2>O prejuízo que obrigou isto a existir</h2>
 * Em 25/08/2026 havia <b>27 harnesses de medição e só 2</b> honravam o filtro por obra. As
 * consequências foram medidas na própria sessão:
 *
 * <ul>
 *   <li>Pedi a um harness novo que provasse o instrumento numa obra de <b>seis</b> arquivos, e ele
 *       varreu os <b>222</b> do acervo — estourou o tempo sem dizer por quê. Provar numa obra antes
 *       de soltar no acervo custa trinta segundos, e é o passo que este projeto já pulou uma vez
 *       produzindo meia hora de lixo convincente.</li>
 *   <li>Um filtro que não casa com nada devolve lista vazia, e lista vazia percorrida em silêncio
 *       imprime <i>zero</i> — que é indistinguível de <i>acervo limpo</i>. É a invariante 12
 *       aplicada ao alcance da medição.</li>
 * </ul>
 *
 * <h2>Por que um dono único e não uma correção em cada harness</h2>
 * Treze harnesses precisavam do mesmo conserto. Treze cópias da mesma regra divergem — neste
 * projeto a segunda implementação de um critério <b>sempre</b> divergiu da primeira. Aqui a regra
 * mora num lugar só, e a {@code CatracaHarnessDeMedicaoTest} exige que quem varre o acervo passe
 * por ela.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY: só lista caminhos.</li>
 *   <li>O filtro {@code -Dkronos.medicao.obra} é casado contra o nome da pasta da OBRA (a mãe da
 *       {@code traducao_ptbr}), sem diferenciar caixa.</li>
 *   <li>Acervo ausente ou filtro sem casamento IMPRIME NÃO VERIFICADO e devolve lista vazia —
 *       quem chama termina sem afirmar número.</li>
 *   <li>Ordem estável (alfabética por caminho), para duas execuções darem o mesmo relatório.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Erro de I/O ao percorrer vira {@link IOException} para quem chamou — medição que não conseguiu
 * ler o acervo não pode continuar como se tivesse lido.
 */
public final class AlcanceDaMedicao {

    /** Raiz do acervo; {@code -Dkronos.acervo} aponta outra. */
    public static final Path RAIZ = Path.of(System.getProperty("kronos.acervo", "C:\\animes"));

    /** Filtro OPCIONAL por obra. Vazio significa "o acervo inteiro". */
    public static final String FILTRO_OBRA = System.getProperty("kronos.medicao.obra", "");

    private AlcanceDaMedicao() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: as pastas {@code traducao_ptbr} que esta medição deve ler.
     *
     * <p>INVARIANTES DO DOMÍNIO: respeita o filtro por obra; imprime o alcance escolhido, para o
     * relatório dizer sobre o que ele fala.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: acervo ausente ou filtro sem casamento imprime NÃO
     * VERIFICADO e devolve lista vazia. <b>Quem chama tem de tratar a lista vazia como "não
     * medi"</b>, e nunca como "não há nada".
     */
    public static List<Path> pastasDeTraducao() throws IOException {
        if (!Files.isDirectory(RAIZ)) {
            System.out.printf("NAO VERIFICADO: acervo ausente em %s — nenhum numero e afirmado.%n",
                RAIZ);
            return List.of();
        }
        List<Path> pastas = new ArrayList<>();
        try (Stream<Path> s = Files.walk(RAIZ)) {
            s.filter(Files::isDirectory)
                .filter(d -> d.getFileName().toString().equals("traducao_ptbr"))
                .filter(AlcanceDaMedicao::casaComOfiltro)
                .sorted(Comparator.comparing(Path::toString))
                .forEach(pastas::add);
        }
        if (pastas.isEmpty()) {
            System.out.printf("NAO VERIFICADO: nenhuma pasta casou com o filtro \"%s\". "
                + "Zero aqui seria zero por cegueira, entao nenhum numero e afirmado.%n",
                FILTRO_OBRA);
            return List.of();
        }
        if (!FILTRO_OBRA.isBlank()) {
            System.out.printf("  FILTRO por obra: \"%s\" -> %d pasta(s) de %s%n",
                FILTRO_OBRA, pastas.size(), RAIZ);
        }
        return pastas;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: as pastas de OBRA no topo do acervo que esta medição deve ler.
     *
     * <h2>Por que existe ao lado de {@link #pastasDeTraducao()}</h2>
     * Nem toda medição olha só a tradução. Várias comparam pastas irmãs dentro da obra
     * ({@code legendas_eng} contra {@code legendas_ptbr}), ou varrem a obra inteira atrás de
     * qualquer legenda. Para essas, a unidade é a OBRA e não a pasta de tradução — e sem esta
     * pergunta elas ficariam com varredura própria, que foi como as treze ignoraram o filtro.
     *
     * <p>INVARIANTES DO DOMÍNIO: só o PRIMEIRO nível sob a raiz, que é onde as obras moram. O
     * filtro casa contra o nome dessa pasta, então {@code -Dkronos.medicao.obra=86} pega a obra
     * {@code 86} inteira, com as partes aninhadas dentro dela.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: acervo ausente ou filtro sem casamento imprime NÃO
     * VERIFICADO e devolve lista vazia — que quem chama trata como "não medi".
     */
    public static List<Path> obras() throws IOException {
        if (!Files.isDirectory(RAIZ)) {
            System.out.printf("NAO VERIFICADO: acervo ausente em %s — nenhum numero e afirmado.%n",
                RAIZ);
            return List.of();
        }
        List<Path> obras;
        try (Stream<Path> s = Files.list(RAIZ)) {
            obras = s.filter(Files::isDirectory)
                .filter(o -> FILTRO_OBRA.isBlank()
                    || o.getFileName().toString().toLowerCase()
                        .contains(FILTRO_OBRA.toLowerCase()))
                .sorted(Comparator.comparing(Path::toString))
                .toList();
        }
        if (obras.isEmpty()) {
            System.out.printf("NAO VERIFICADO: nenhuma obra casou com o filtro \"%s\". "
                + "Zero aqui seria zero por cegueira, entao nenhum numero e afirmado.%n",
                FILTRO_OBRA);
            return List.of();
        }
        if (!FILTRO_OBRA.isBlank()) {
            System.out.printf("  FILTRO por obra: \"%s\" -> %d obra(s) de %s%n",
                FILTRO_OBRA, obras.size(), RAIZ);
        }
        return obras;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: os arquivos {@code .ass} entregues de uma pasta — sem {@code .parcial},
     * que não é entrega.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga {@link IOException}; pasta vazia devolve lista
     * vazia, e cabe a quem chama declarar isso.
     */
    public static List<Path> arquivosEntregues(Path pasta) throws IOException {
        try (Stream<Path> s = Files.list(pasta)) {
            return s.filter(a -> a.getFileName().toString().toLowerCase().endsWith(".ass"))
                .filter(a -> !a.getFileName().toString().toLowerCase().contains(".parcial."))
                .sorted()
                .toList();
        }
    }

    /** O nome da obra de uma pasta {@code traducao_ptbr} — a pasta mãe. */
    public static String obraDe(Path pastaDeTraducao) {
        Path mae = pastaDeTraducao.getParent();
        return mae == null ? "?" : mae.getFileName().toString();
    }

    private static boolean casaComOfiltro(Path pastaDeTraducao) {
        if (FILTRO_OBRA.isBlank()) {
            return true;
        }
        return obraDe(pastaDeTraducao).toLowerCase().contains(FILTRO_OBRA.toLowerCase());
    }
}
