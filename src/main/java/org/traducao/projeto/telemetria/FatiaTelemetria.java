package org.traducao.projeto.telemetria;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: agrupa os tipos de operação registrados pelas fatias numa
 * FATIA lógica, para o painel ter uma aba por assunto e cada uma publicar o
 * próprio dataset.
 *
 * <h2>Por que um mapa aqui, e não um campo em OperacaoTelemetria</h2>
 * {@link OperacaoTelemetria} é contrato de <b>13 fatias</b> que chamam
 * {@code registrarOperacao}. Acrescentar um campo obrigatório ali significaria
 * mexer nas treze, e o agrupamento é assunto de APRESENTAÇÃO — quem registra não
 * precisa saber em que aba vai aparecer. O mapa vive de um lado só e o contrato
 * permanece intacto.
 *
 * <h2>Casamento NOMINAL exato, nunca por pedaço de nome</h2>
 * Nada de {@code contains("Revisão")}: "Revisão de Lore" e "Revisão Gramatical
 * (cache LLM)" são fatias diferentes, e regra genérica juntaria as duas. O
 * projeto já se queimou com guarda por nome genérico onde o correto era
 * inventário nominal.
 *
 * <h2>INVARIANTES DO DOMÍNIO</h2>
 * <ul>
 *   <li>Tipo desconhecido cai em {@link #OUTROS} — nunca é descartado. Registro
 *       que some porque ninguém o mapeou é dado perdido em silêncio.</li>
 *   <li>A chave de fluxo é minúscula e sem espaço, porque vira nome de stream e
 *       de arquivo de dataset.</li>
 * </ul>
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: tipo nulo ou em branco devolve
 * {@link #OUTROS}. Nunca lança.
 *
 * <p>NÚMEROS QUE ORIGINARAM O AGRUPAMENTO — medidos no acervo em 06/08/2026,
 * sobre 6.601 operações registradas:
 * <pre>
 *   auditoria      2.930      karaoke          214
 *   cache          1.610      arquivos         180
 *   revisao          809      terminologia     177
 *   extracao         496      legenda          185
 * </pre>
 * Vinte e dois tipos livres colapsam em oito fatias com volume real. Aba por
 * tipo daria vinte e duas, três delas com menos de quarenta registros.
 */
public final class FatiaTelemetria {

    /** Destino de tipo não mapeado. Existe para nada sumir, e para a catraca ter o que apontar. */
    public static final String OUTROS = "outros";

    private static final Map<String, String> POR_TIPO = criarMapa();

    private FatiaTelemetria() {
    }

    private static Map<String, String> criarMapa() {
        Map<String, String> m = new LinkedHashMap<>();

        // Auditoria de conteúdo — 2.930 operações, a fatia mais movimentada.
        m.put("auditoria de conteudo de legendas", "auditoria");
        m.put("auditoria de conteúdo de legendas", "auditoria");
        m.put("auditoria de conteudo (.ass)", "auditoria");
        m.put("auditoria de conteúdo (.ass)", "auditoria");

        // Cache de tradução — limpeza e as correções que operam sobre o cache.
        m.put("limpeza de cache", "cache");
        m.put("correção google (cache)", "cache");
        m.put("correcao google (cache)", "cache");
        m.put("revisão gramatical (cache llm)", "cache");
        m.put("revisao gramatical (cache llm)", "cache");

        // Revisão — lore, concordância e revisão de legenda pronta.
        m.put("revisão legendas (.ass google)", "revisao");
        m.put("revisao legendas (.ass google)", "revisao");
        m.put("revisão concordância (.ass llm)", "revisao");
        m.put("revisao concordancia (.ass llm)", "revisao");
        m.put("revisão de concordância", "revisao");
        m.put("revisao de concordancia", "revisao");
        m.put("revisao de lore (.ass llm)", "revisao");
        m.put("revisão de lore (.ass llm)", "revisao");
        m.put("revisão de lore pt-only", "revisao");
        m.put("revisao de lore pt-only", "revisao");

        // Extração e mídia — o que entra antes de qualquer tradução.
        m.put("extracao de legendas (ass)", "extracao");
        m.put("extração de legendas (ass)", "extracao");
        m.put("remux (mkvmerge)", "extracao");
        m.put("analise de midia", "extracao");
        m.put("análise de mídia", "extracao");

        // Karaokê.
        m.put("novo_karaoke", "karaoke");
        m.put("tradução de karaokê (llm)", "karaoke");
        m.put("traducao de karaoke (llm)", "karaoke");
        m.put("karaokê simples", "karaoke");
        m.put("karaoke simples", "karaoke");

        // Terminologia e glossário.
        m.put("reforço de terminologia (ensaio)", "terminologia");
        m.put("reforco de terminologia (ensaio)", "terminologia");
        m.put("reforço de terminologia", "terminologia");
        m.put("reforco de terminologia", "terminologia");

        // Organização de arquivos.
        m.put("renomear arquivos", "arquivos");

        // Preparo do .ass: fonte, estilo e correção estrutural.
        m.put("troca de fontes ass", "legenda");
        m.put("achatar estilos decorativos", "legenda");
        m.put("correcao de legendas (.ass original->traduzida)", "legenda");
        m.put("correção de legendas (.ass original->traduzida)", "legenda");

        return Map.copyOf(m);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: diz em que fatia uma operação deve aparecer.
     *
     * <p>INVARIANTES DO DOMÍNIO: comparação em minúsculas e sem espaço nas
     * bordas, porque o mesmo tipo aparece no acervo com e sem acento — o registro
     * vem de treze pontos diferentes e a grafia nunca foi centralizada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@link #OUTROS} para nulo, vazio
     * ou tipo não mapeado. Nunca lança e nunca devolve nulo.
     */
    public static String de(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return OUTROS;
        }
        return POR_TIPO.getOrDefault(tipo.trim().toLowerCase(Locale.ROOT), OUTROS);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: expõe o inventário para a catraca conferir cobertura
     * e para o painel montar as abas sem adivinhar nomes.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; o mapa é imutável.
     */
    public static Map<String, String> inventario() {
        return POR_TIPO;
    }
}
