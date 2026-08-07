package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: manter NOMINAL a lista de arquivos que decidem alguma coisa a partir de
 * palavra musical, para que um novo nascer deixe de ser silencioso.
 *
 * <h2>O prejuízo que originou</h2>
 * Até 07/08/2026 a pergunta "o NOME deste estilo declara música?" tinha DUAS respostas no
 * código, e elas discordavam em <b>1.385 linhas do acervo</b> — medido por
 * {@code MedicaoDivergenciaPadraoMusicalIT} sobre 1.719.242 falas. {@code PoliticaEstiloMusical}
 * usava {@code \b} e não alcançava {@code OP_S2} (o {@code _} é caractere de palavra);
 * {@code DetectorEfeitoKaraokeService} usava lookaround de letra mas não conhecia {@code romaji}
 * nem o plural de {@code songs}. Na auditoria do mesmo dia isso produziu <b>364 falsos
 * "defeitos de tradução"</b> no Gundam Unicorn.
 *
 * <p>A {@code CatracaRegraDuplicadaEntreFatiasTest} já DECLARAVA essa duplicação desde
 * 2026-08-03. Declarar não bastou — a divergência sobreviveu quatro dias.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Reprovar aqui NÃO significa "apague o padrão". Significa: delegue a
 *       {@code PadraoEstiloMusical}, ou declare em {@link #ARQUIVOS_CONHECIDOS} por que este
 *       arquivo responde OUTRA pergunta. O que não pode é nascer calado.</li>
 *   <li>O DONO tem de aparecer na varredura. Se ele sumir, o instrumento está cego e aprovaria
 *       qualquer divergência nova.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Lista os arquivos novos e os que sumiram. Nunca lança por conta própria.
 */
class CatracaPadraoMusicalTemDonoUnicoTest {

    private static final Path RAIZ = Path.of("src/main/java");

    private static final String PKG = "org/traducao/projeto/";

    private static final String DONO = PKG + "legenda/domain/PadraoEstiloMusical.java";

    /** Arquivos que decidem por palavra musical, com o motivo de cada um existir. */
    private static final Map<String, String> ARQUIVOS_CONHECIDOS = Map.of(
        DONO,
            "O DONO. Fonte unica desde 07/08/2026: substring + fronteira de LETRA.",
        PKG + "legenda/application/DetectorEfeitoKaraokeService.java",
            "ESTILO_MUSICA_PATTERN, deliberadamente MAIS ESTREITO (usa \\b), consultado so por "
            + "temIndicadorDeMusica. Alarga-lo arrastaria OP2/ED_S2 para o simplificador de "
            + "karaoke e o fluxo de correcao de uma vez — o Javadoc do metodo declara isso como "
            + "mudanca de outra ordem, da fase de retrabalho do karaoke simples. Os dois metodos "
            + "de DECISAO (eEstiloDeMusica e podeSerCamadaMusical) ja delegam ao dono.",
        PKG + "qualidadeTraducao/application/ProtecaoLegendaAssService.java",
            "PADRAO_ESTILO_TECNICO responde OUTRA pergunta — 'este estilo e tecnico?' — e inclui "
            + "signs/title/next ep/credits, que nao sao musica. Nao delega de proposito.",
        PKG + "remuxer/application/MapeadorMidiaService.java",
            "SUFIXOS_LEGENDA casa sufixo de NOME DE ARQUIVO (track1, signs, songs) para escolher "
            + "qual legenda entra no remux. Nao decide estilo nenhum.",
        PKG + "novoKaraoke/application/ConversorKaraokeUseCase.java",
            "PALAVRA_ROMAJI_PATTERN e decomposicao silabica Hepburn — responde 'este TEXTO e "
            + "romaji?', nao 'este ESTILO declara musica?'. Sao perguntas diferentes: o romaji e "
            + "reconhecido pelo conteudo justamente porque o nome do estilo nao e confiavel "
            + "(Paulo normaliza fontes e renomeia estilos).",
        PKG + "traducaoKaraoke/application/ClassificadorLetraKaraokeService.java",
            "ESTILO_JAPONES_ROMAJI_PATTERN e PALAVRA_ROMAJI_PATTERN — mesma pergunta do "
            + "ConversorKaraokeUseCase (romaji por conteudo), na fatia dona do karaoke.");

    /**
     * Assinatura: constante {@code Pattern} ou {@code String[]} cuja declaração traz palavra
     * musical. Pega o DONO (que usa {@code String[]}) e os padrões compilados.
     *
     * <p>{@code karaoke} sozinho não bastaria — {@code TAG_KARAOKE_PATTERN} casa a tag
     * {@code \k} e é outra pergunta —, por isso a alternância exige uma das palavras que só
     * aparecem em NOME de estilo.
     */
    private static final Pattern PALAVRA_MUSICAL_EM_CONSTANTE = Pattern.compile(
        "(?is)static\\s+final\\s+(?:Pattern|String\\[\\])[^;]*?"
        + "(song|opening|ending|romaji|lyric)[^;]*?;");

    /**
     * PROPÓSITO DE NEGÓCIO: a varredura. Recebe a raiz por parâmetro para que o caso-controle
     * possa exercitá-la contra uma árvore montada à mão, sem tocar no código real.
     */
    private static Set<String> mapear(Path raiz) throws IOException {
        Set<String> arquivos = new TreeSet<>();
        try (Stream<Path> caminhos = Files.walk(raiz)) {
            for (Path arquivo : caminhos.filter(p -> p.toString().endsWith(".java")).toList()) {
                String fonte = Files.readString(arquivo, StandardCharsets.UTF_8);
                Matcher m = PALAVRA_MUSICAL_EM_CONSTANTE.matcher(fonte);
                if (m.find()) {
                    arquivos.add(raiz.relativize(arquivo).toString().replace('\\', '/'));
                }
            }
        }
        return arquivos;
    }

    @Test
    @DisplayName("nenhum arquivo novo decide por palavra musical sem estar declarado")
    void criterioMusicalTemDonoUnico() throws IOException {
        Set<String> encontrados = mapear(RAIZ);

        assertTrue(encontrados.contains(DONO),
            "instrumento CEGO: nao encontrou nem o DONO (" + DONO + "). Se a classe mudou de "
                + "lugar, corrija o caminho — nao apague a catraca. Achou: " + encontrados);

        List<String> novos = new ArrayList<>(encontrados);
        novos.removeAll(ARQUIVOS_CONHECIDOS.keySet());
        assertTrue(novos.isEmpty(), () -> """
            Arquivo NOVO decidindo por palavra musical: %s

            Ate 07/08/2026 a pergunta "este estilo declara musica?" tinha DUAS respostas no
            codigo e elas discordavam em 1.385 linhas do acervo — o que produziu 364 falsos
            defeitos de traducao no Gundam Unicorn.

            Delegue a legenda.domain.PadraoEstiloMusical, que e o dono. Se este arquivo responde
            OUTRA pergunta — como ProtecaoLegendaAssService ("e estilo tecnico?") ou
            MapeadorMidiaService (sufixo de nome de ARQUIVO) — declare o motivo em
            ARQUIVOS_CONHECIDOS. O que nao pode e nascer calado.
            """.formatted(novos));

        List<String> sumiram = new ArrayList<>(ARQUIVOS_CONHECIDOS.keySet());
        sumiram.removeAll(encontrados);
        assertTrue(sumiram.isEmpty(),
            "arquivo conhecido sumiu da varredura: " + sumiram + ". Se delegou ao dono, tire da "
                + "lista e diga onde. Se so mudou de lugar, corrija o caminho.");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE. A varredura só vale se enxergar — monta uma árvore
     * com um arquivo que decide por palavra musical e outro que não, e exige que ela separe os
     * dois. Sem isto, um erro de escape no {@code Pattern} deixaria a catraca verde para sempre.
     */
    @Test
    @DisplayName("instrumento calibrado: acha o arquivo plantado e ignora tag de karaoke")
    void instrumentoAchaPlantadoEIgnoraOResto(@TempDir Path temp) throws IOException {
        Files.writeString(temp.resolve("ComPadrao.java"), """
            class ComPadrao {
                private static final Pattern P =
                    Pattern.compile("(?i)(song|opening|ending)");
            }
            """);
        Files.writeString(temp.resolve("SoTagDeKaraoke.java"), """
            class SoTagDeKaraoke {
                private static final Pattern TAG = Pattern.compile("tag");
                private static final Pattern LIMPA = Pattern.compile("limpa");
            }
            """);

        Set<String> achados = mapear(temp);

        assertEquals(1, achados.size(),
            "a varredura tinha de achar EXATAMENTE o arquivo plantado. Achou: " + achados);
        assertTrue(achados.contains("ComPadrao.java"), "achou: " + achados);
        assertFalse(achados.contains("SoTagDeKaraoke.java"),
            "constante sem palavra de NOME musical nao pode ser acusada: " + achados);
    }
}
