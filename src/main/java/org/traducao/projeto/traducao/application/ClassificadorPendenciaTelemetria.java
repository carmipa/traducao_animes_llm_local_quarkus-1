package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducao.domain.CategoriaConteudo;
import org.traducao.projeto.traducao.domain.CausaRaizPendencia;
import org.traducao.projeto.traducao.domain.ResumoPendencia;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: transforma o motivo final de uma pendência (texto já produzido
 * pelo avaliador) e o estilo/texto do evento em métricas ESTRUTURADAS — causa-raiz e
 * balde de conteúdo — e as consolida no resumo por episódio. Serve para o painel de
 * telemetria medir a Tradução Local sem re-interpretar {@code errosOcorridos} em texto
 * livre (fonte de leituras divergentes).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O balde de conteúdo vem do {@link DetectorEfeitoKaraokeService} (classificador de
 *       karaokê real), NUNCA de densidade de tags — para não confundir diálogo com KFX.</li>
 *   <li>{@link #consolidar} soma FALAS distintas por (categoria, causa); a precedência de
 *       causa (ex.: marcador corrompido vence eco) é aplicada por quem chama, ao decidir a
 *       causa de cada fala, via {@link CausaRaizPendencia#maisGrave}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Serviço sem estado; motivo nulo/desconhecido classifica como {@link CausaRaizPendencia#ECO}
 * (desfecho mais comum e menos grave), nunca lança. Não faz I/O.
 */
@Service
public class ClassificadorPendenciaTelemetria {

    // Estilos de typesetting/letreiro que NÃO são música mas também não são diálogo.
    private static final Pattern ESTILO_LETREIRO_PATTERN = Pattern.compile(
        "(?i)\\b(signs?|letreiro|placa|title|caption|nota|note)\\b");

    private final DetectorEfeitoKaraokeService detector;

    public ClassificadorPendenciaTelemetria(DetectorEfeitoKaraokeService detector) {
        this.detector = detector;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapeia o motivo final do avaliador para a causa-raiz estruturada.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@link CausaRaizPendencia#MARCADORES_CORROMPIDOS} NÃO é
     * produzida aqui — vem da via de desmascaramento e é aplicada por quem chama, com
     * precedência sobre esta. Resíduo/idioma/preâmbulo/recusa colapsam em {@code RESIDUO}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: motivo nulo ou não reconhecido devolve {@code ECO}.
     */
    public CausaRaizPendencia causaDeMotivoFinal(String motivoFalha) {
        if (motivoFalha == null || motivoFalha.isBlank()) {
            return CausaRaizPendencia.ECO;
        }
        String m = motivoFalha.toLowerCase(java.util.Locale.ROOT);
        if (m.contains("resposta vazia")) {
            return CausaRaizPendencia.VAZIA;
        }
        if (m.contains("divergent") || m.contains("quebras de linha") || m.contains("tags ass/ssa")) {
            return CausaRaizPendencia.ESTRUTURA_DIVERGENTE;
        }
        if (m.contains("resíduo") || m.contains("residuo") || m.contains("idioma incorreto")
                || m.contains("preâmbulo") || m.contains("preambulo") || m.contains("recusa")
                || m.contains("marcador de erro")) {
            return CausaRaizPendencia.RESIDUO;
        }
        // "modelo devolveu o texto original sem tradução" e demais casos.
        return CausaRaizPendencia.ECO;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide o balde de conteúdo de uma fala usando o classificador
     * de karaokê, para o KPI separar diálogo das camadas musicais.
     *
     * <p>INVARIANTES DO DOMÍNIO: romaji/japonês preservado é reconhecido antes de KFX;
     * KFX antes de música latina traduzível; letreiro por nome de estilo; o restante é
     * diálogo. Reusa a MESMA regra que decide o que vai ao LLM.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entradas nulas/vazias resolvem para {@code DIALOGO}.
     */
    public CategoriaConteudo categoria(String estilo, String texto) {
        if (detector.devePreservarKaraokeOriginal(estilo, texto)) {
            return CategoriaConteudo.ROMAJI_PRESERVADO;
        }
        if (detector.temTagKaraoke(texto) || detector.eEfeitoKaraoke(texto)) {
            return CategoriaConteudo.KFX;
        }
        if (detector.eKaraokeOuMusicaTraduzivel(estilo, texto)) {
            return CategoriaConteudo.MUSICA_LATINA;
        }
        if (estilo != null && ESTILO_LETREIRO_PATTERN.matcher(estilo).find()) {
            return CategoriaConteudo.LETREIRO;
        }
        return CategoriaConteudo.DIALOGO;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: consolida uma lista de pendências unitárias (uma por fala,
     * cada uma com {@code quantidade == 1}) no resumo agregado por (categoria, causa).
     *
     * <p>INVARIANTES DO DOMÍNIO: soma as quantidades por combinação; preserva a ordem de
     * primeira aparição; não deduplica falas (isso é responsabilidade de quem monta a
     * lista de entrada, já com uma causa por fala).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista nula ou vazia devolve lista vazia.
     */
    public List<ResumoPendencia> consolidar(List<ResumoPendencia> unitarios) {
        if (unitarios == null || unitarios.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> soma = new LinkedHashMap<>();
        for (ResumoPendencia r : unitarios) {
            String chave = r.categoria() + "|" + r.causaRaiz();
            soma.merge(chave, r.quantidade(), (a, b) -> a + b);
        }
        List<ResumoPendencia> resultado = new ArrayList<>(soma.size());
        for (Map.Entry<String, Integer> e : soma.entrySet()) {
            String[] partes = e.getKey().split("\\|", 2);
            resultado.add(new ResumoPendencia(partes[0], partes[1], e.getValue()));
        }
        return resultado;
    }
}
