package org.traducao.projeto.traducaoKaraoke.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.core.util.ArquivoAtomicoUtil;
import org.traducao.projeto.traducaoKaraoke.domain.TelemetriaKaraoke;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: única escritora do acervo próprio desta fatia —
 * {@code logs/telemetria_karaoke_execucoes.jsonl}, uma linha JSON por ARQUIVO processado. É este
 * arquivo que o publicador lê para o karaokê entrar no dataset público.
 *
 * <h2>Por que a fatia escreve o PRÓPRIO acervo</h2>
 * Regra de Paulo: a camada resolve o problema DELA e não atravessa para as outras. O karaokê tem
 * contadores que a Tradução Local não tem — acento reposto, entrada de cache descartada, efeito
 * KFX preservado, camada japonesa preservada — e não cabem no schema por episódio da fatia de
 * diálogo. Espremer um no outro obrigaria a mexer no arquivo canônico da Tradução Local toda vez
 * que o karaokê medisse algo novo. O módulo de telemetria só LÊ e consolida, como já faz com
 * {@code telemetria_traducao.json}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li><b>APPEND puro.</b> Nenhuma linha é reescrita ou editada: a pergunta que o acervo responde
 *       é "esta mudança melhorou?", e ela exige o histórico, não a foto.</li>
 *   <li><b>Teto LOCAL</b> de {@value #LIMITE_LOCAL} linhas, com folga de {@value #FOLGA_PODA} para
 *       não reescrever o arquivo a cada arquivo traduzido. Saem as MAIS ANTIGAS, que continuam no
 *       repositório do dataset — o corte é de armazenamento, nunca de acervo.</li>
 *   <li>A poda é ATÔMICA (temporário + substituição): queda no meio dela não trunca o histórico.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * NUNCA propaga. Telemetria é observabilidade: perder uma linha do dataset não pode custar a
 * legenda que acabou de ser gravada. Falha de I/O sai em WARN e a execução segue.
 */
@ApplicationScoped
public class TelemetriaKaraokeDataset {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaKaraokeDataset.class);

    /** O nome é lido também pelo publicador do dataset; mudar aqui exige mudar lá. */
    public static final String NOME_ARQUIVO = "telemetria_karaoke_execucoes.jsonl";

    private static final String SUBPASTA = "logs";
    private static final int LIMITE_LOCAL = 20_000;
    private static final int FOLGA_PODA = 1_000;

    @Inject
    ObjectMapper objectMapper;

    /**
     * PROPÓSITO DE NEGÓCIO: acrescenta ao acervo as linhas de UMA execução de karaokê.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li>Lista VAZIA é gravada como nada — mas quem chama nunca manda vazio: a execução sem
     *       arquivo nenhum manda a linha {@code NAO_ALCANCADO}. Aqui a lista vazia significa
     *       apenas "o chamador já decidiu que não há o que registrar", e o {@code log.debug}
     *       existe para essa decisão não ficar invisível.</li>
     *   <li>Elemento {@code null} na lista é pulado, nunca vira linha {@code "null"} no JSONL —
     *       linha malformada num acervo é defeito, não ruído tolerável.</li>
     * </ul>
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de I/O ou de serialização vira WARN e retorna; a
     * execução de tradução segue intacta.
     *
     * @return quantas linhas foram efetivamente gravadas
     */
    public int registrar(List<TelemetriaKaraoke> linhas) {
        if (linhas == null || linhas.isEmpty()) {
            log.debug("[TELEMETRIA] karaoke: nada a acrescentar ao acervo nesta execucao");
            return 0;
        }
        try {
            Path pasta = DiretorioBaseKronos.resolver(SUBPASTA);
            Files.createDirectories(pasta);
            Path acervo = pasta.resolve(NOME_ARQUIVO);

            StringBuilder conteudo = new StringBuilder();
            int gravadas = 0;
            for (TelemetriaKaraoke linha : linhas) {
                if (linha == null) {
                    continue;
                }
                conteudo.append(objectMapper.writeValueAsString(linha))
                    .append(System.lineSeparator());
                gravadas++;
            }
            if (gravadas == 0) {
                return 0;
            }
            Files.writeString(acervo, conteudo.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            podarSeNecessario(acervo);
            log.debug("[TELEMETRIA] karaoke: {} linha(s) acrescentada(s) a {}", gravadas, acervo);
            return gravadas;
        } catch (IOException | RuntimeException e) {
            log.warn("[TELEMETRIA] falha ao acrescentar a execucao de karaoke ao acervo: {}",
                e.toString());
            return 0;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém o acervo local dentro do teto sem perder dado já publicado.
     *
     * <p>INVARIANTES DO DOMÍNIO: descarta apenas as MAIS ANTIGAS, preserva a ordem de escrita e
     * troca o arquivo de forma atômica.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga {@link IOException} para o chamador, que apenas
     * loga; o arquivo anterior fica íntegro porque a troca só acontece no fim.
     */
    private void podarSeNecessario(Path acervo) throws IOException {
        List<String> linhas = Files.readAllLines(acervo, StandardCharsets.UTF_8);
        if (linhas.size() <= LIMITE_LOCAL + FOLGA_PODA) {
            return;
        }
        List<String> mantidas = linhas.subList(linhas.size() - LIMITE_LOCAL, linhas.size());
        Path temporario = acervo.resolveSibling(NOME_ARQUIVO + ".tmp");
        Files.write(temporario, mantidas, StandardCharsets.UTF_8);
        ArquivoAtomicoUtil.substituirAtomico(temporario, acervo);
        log.info("Acervo local de karaoke podado para as {} linhas mais recentes "
            + "(as anteriores permanecem no repositorio do dataset).", LIMITE_LOCAL);
    }
}
