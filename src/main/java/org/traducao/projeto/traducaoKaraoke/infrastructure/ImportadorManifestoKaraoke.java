package org.traducao.projeto.traducaoKaraoke.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.traducaoKaraoke.domain.TelemetriaKaraoke;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: reconstrói linhas do acervo a partir dos MANIFESTOS antigos, para o
 * dataset de karaokê não nascer vazio esperando a próxima execução.
 *
 * <h2>O que existe para importar, medido em 2026-08-20</h2>
 * 19 manifestos desde 30/07, com <b>383 arquivos</b> e nove contadores reais em cada um —
 * eventos, efeito KFX, camada japonesa preservada, já em português, a traduzir, cache,
 * traduzidas, mantidas sem tradução e os avisos. É o que permite a pergunta que o acervo existe
 * para responder ("esta mudança melhorou?") já na primeira publicação, comparando obras
 * traduzidas antes e depois da régua de música.
 *
 * <h2>O que NÃO dá para reconstruir, e por que isso não vira mentira</h2>
 * <b>Zero dos 19</b> manifestos tem {@code statusFinal}, {@code estadoDicionario},
 * {@code cacheIgnorado}, {@code arquivosComFalha}, {@code acentosRepostos} ou
 * {@code entradasCacheDescartadas} — esses campos nasceram depois. Onze dos 19 também não têm
 * {@code modeloLlm} nem contexto. Toda linha importada sai marcada
 * {@code origemDoRegistro=MANIFESTO_HISTORICO}, e é essa coluna — não o zero — que diz "não
 * medido". Sem ela, {@code acentosRepostos=0} significaria ao mesmo tempo "medi e deu zero" e
 * "não havia medição".
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li><b>Não duplica.</b> A chave {@code registradoEm + arquivo} já presente no acervo é
 *       pulada, então importar duas vezes é inofensivo.</li>
 *   <li>O carimbo vem do {@code executadoEm} do manifesto, que é {@code LocalDateTime} sem fuso.
 *       É convertido pelo fuso DESTA máquina, que foi a que gerou o arquivo — inferência
 *       declarada, não invenção.</li>
 *   <li>Manifesto ilegível é pulado com aviso; um arquivo corrompido não pode custar os outros
 *       dezoito.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Pasta inexistente devolve lista vazia. Nunca lança para o chamador por causa de um manifesto
 * individual.
 */
public class ImportadorManifestoKaraoke {

    private static final Logger log = LoggerFactory.getLogger(ImportadorManifestoKaraoke.class);

    private final ObjectMapper mapper;

    public ImportadorManifestoKaraoke(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lê a pasta de manifestos e devolve as linhas prontas para o acervo,
     * já sem as que ele possui.
     *
     * <p>INVARIANTES DO DOMÍNIO: a ordem é cronológica pelo nome do arquivo, que carrega o
     * carimbo — o acervo é append-only e a ordem de escrita é a ordem dos fatos.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: pasta ausente devolve lista vazia (caso normal em
     * máquina que nunca traduziu karaokê).
     */
    public List<TelemetriaKaraoke> importar(Path pastaManifestos, List<String> chavesJaNoAcervo)
            throws IOException {
        if (pastaManifestos == null || !Files.isDirectory(pastaManifestos)) {
            return List.of();
        }
        List<Path> manifestos;
        try (Stream<Path> fluxo = Files.list(pastaManifestos)) {
            manifestos = fluxo
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .sorted()
                .toList();
        }

        List<TelemetriaKaraoke> linhas = new ArrayList<>();
        for (Path manifesto : manifestos) {
            try {
                linhas.addAll(linhasDe(mapper.readTree(manifesto.toFile()), chavesJaNoAcervo));
            } catch (IOException | RuntimeException e) {
                log.warn("Manifesto de karaoke ilegivel, pulado: {} ({})", manifesto, e.toString());
            }
        }
        return linhas;
    }

    private List<TelemetriaKaraoke> linhasDe(JsonNode raiz, List<String> chavesJaNoAcervo) {
        JsonNode arquivos = raiz.get("arquivos");
        if (arquivos == null || !arquivos.isArray() || arquivos.isEmpty()) {
            return List.of();
        }
        String registradoEm = carimboDe(texto(raiz, "executadoEm"));
        if (registradoEm == null) {
            return List.of();
        }
        long duracaoMs = raiz.path("duracaoMs").asLong(0L);
        List<TelemetriaKaraoke> linhas = new ArrayList<>(arquivos.size());
        for (JsonNode item : arquivos) {
            String nome = texto(item, "arquivo");
            if (nome == null || chavesJaNoAcervo.contains(registradoEm + '|' + nome)) {
                continue;
            }
            linhas.add(TelemetriaKaraoke.deManifesto(
                registradoEm, nome,
                texto(raiz, "contextoId"), texto(raiz, "contextoNome"),
                texto(raiz, "contextoHash"), texto(raiz, "modeloLlm"),
                duracaoMs, arquivos.size(),
                item.path("eventosTotais").asInt(0),
                item.path("efeitosKfxPreservados").asInt(0),
                item.path("preservadasOriginalJapones").asInt(0),
                item.path("jaEmPortugues").asInt(0),
                item.path("paraTraduzir").asInt(0),
                item.path("reaproveitadasCache").asInt(0),
                item.path("traduzidas").asInt(0),
                item.path("mantidasSemTraducao").asInt(0),
                avisosDe(item)));
        }
        return linhas;
    }

    private static List<String> avisosDe(JsonNode item) {
        JsonNode avisos = item.get("avisos");
        if (avisos == null || !avisos.isArray()) {
            return List.of();
        }
        List<String> saida = new ArrayList<>(avisos.size());
        for (JsonNode a : avisos) {
            if (a != null && a.isTextual()) {
                saida.add(a.asText());
            }
        }
        return saida;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte o {@code executadoEm} do manifesto — {@code LocalDateTime},
     * sem fuso — no instante UTC que o acervo usa como chave e ordenação.
     *
     * <p>INVARIANTES DO DOMÍNIO: o fuso aplicado é o DESTA máquina, que foi a que gerou o
     * manifesto. É inferência declarada; sem fuso nenhum o carimbo não ordena junto com as linhas
     * novas, e o acervo perderia a cronologia que é a razão de ele existir.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: carimbo ausente ou ilegível devolve {@code null}, e o
     * manifesto inteiro é pulado — linha sem chave seria descartada calada pelo publicador.
     */
    static String carimboDe(String executadoEm) {
        if (executadoEm == null || executadoEm.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(executadoEm.trim())
                .atZone(ZoneId.systemDefault()).toInstant().toString();
        } catch (DateTimeParseException e) {
            log.warn("Carimbo de manifesto ilegivel, manifesto pulado: {}", executadoEm);
            return null;
        }
    }

    private static String texto(JsonNode no, String campo) {
        JsonNode v = no.get(campo);
        return v == null || v.isNull() ? null : v.asText();
    }
}
