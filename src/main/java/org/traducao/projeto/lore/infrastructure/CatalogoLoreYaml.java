package org.traducao.projeto.lore.infrastructure;

import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lê o ARQUIVO ÚNICO de lore da tradução e entrega as obras como
 * {@link ProvedorContexto}. É o que permite a lore ser dado editável num lugar só, em vez de 82
 * classes Java — ordem de Paulo em 2026-08-15: <i>"todas as lores devem ficar em um único
 * arquivo"</i>.
 *
 * <h2>Por que dado, e não classe</h2>
 * Medido antes de decidir: os 82 arquivos somam 11.369 linhas e têm <b>zero</b> lógica
 * condicional. Nenhuma lore decide nada — todas devolvem literais. Classe Java para guardar
 * literal é cerimônia que cobra o preço de compilar, revisar e duplicar; e foi essa duplicação
 * que deixou a tradução sem 69 termos que a revisão já conhecia.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li><b>Falha FECHADA.</b> Arquivo ausente, ilegível, sem a chave {@code obras}, com lista
 *       vazia, com id repetido ou com obra sem id/nome/prompt <b>lança</b>. Catálogo de lore
 *       silenciosamente vazio faria o pipeline traduzir sem lore nenhuma e gravar o resultado —
 *       o dano apareceria na legenda, não no boot.</li>
 *   <li><b>Campos ausentes têm padrão explícito</b>, e o padrão é o mesmo do contrato:
 *       conjuntos vazios, mapa vazio, {@code apareceNaLista} verdadeiro. Ausência é "esta obra
 *       não declara", nunca "não consegui ler".</li>
 *   <li>Os objetos devolvidos são imutáveis e sem I/O — a leitura acontece uma vez, na
 *       construção.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Lança {@link IllegalStateException} com o recurso e o motivo, ou {@link UncheckedIOException}
 * se a leitura falhar. Nunca devolve catálogo parcial: ou o arquivo inteiro é válido, ou nada
 * sobe.
 */
public final class CatalogoLoreYaml {

    /** Recurso único da lore de tradução, empacotado no jar. */
    public static final String RECURSO = "/lore/lore-traducao.yaml";

    private final List<ProvedorContexto> obras;

    /**
     * PROPÓSITO DE NEGÓCIO: carrega o catálogo do recurso padrão.
     * <p>INVARIANTES DO DOMÍNIO: ver a classe — falha fechada em qualquer defeito do arquivo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança; não existe catálogo parcial.
     */
    public CatalogoLoreYaml() {
        this(RECURSO);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: carrega o catálogo de um recurso nomeado — usado pelos testes para
     * exercitar arquivos doentes sem tocar no recurso real.
     * <p>INVARIANTES DO DOMÍNIO: mesmas do construtor padrão.
     * <p>COMPORTAMENTO EM CASO DE FALHA: recurso inexistente lança {@link IllegalStateException}.
     *
     * @param recurso caminho do recurso no classpath, começando por {@code /}
     */
    @SuppressWarnings("unchecked")
    public CatalogoLoreYaml(String recurso) {
        Object raiz;
        try (InputStream in = CatalogoLoreYaml.class.getResourceAsStream(recurso)) {
            if (in == null) {
                throw new IllegalStateException("Arquivo de lore não encontrado: " + recurso);
            }
            raiz = new Yaml().load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new UncheckedIOException("Falha ao ler o arquivo de lore: " + recurso, e);
        }
        if (!(raiz instanceof Map<?, ?> mapa)) {
            throw new IllegalStateException("Arquivo de lore não é um mapa YAML: " + recurso);
        }
        Object lista = mapa.get("obras");
        if (!(lista instanceof List<?> itens) || itens.isEmpty()) {
            throw new IllegalStateException(
                "Arquivo de lore sem a lista \"obras\", ou com ela vazia: " + recurso);
        }

        List<ProvedorContexto> carregadas = new ArrayList<>(itens.size());
        Set<String> idsVistos = new LinkedHashSet<>();
        for (Object item : itens) {
            if (!(item instanceof Map<?, ?> obra)) {
                throw new IllegalStateException("Entrada de obra que não é mapa em " + recurso);
            }
            Map<String, Object> o = (Map<String, Object>) obra;
            String id = texto(o, "id", recurso);
            if (!idsVistos.add(id)) {
                throw new IllegalStateException("Id de obra repetido no arquivo de lore: " + id);
            }
            carregadas.add(new ObraDeArquivo(
                id,
                texto(o, "nome", recurso),
                texto(o, "prompt", recurso),
                conjunto(o.get("apelidosPasta")),
                conjunto(o.get("termosProtegidos")),
                pares(o.get("paresInconfundiveis")),
                mapaDeTexto(o.get("correcoesTerminologia")),
                !(o.get("apareceNaLista") instanceof Boolean b) || b));
        }
        this.obras = List.copyOf(carregadas);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: as obras do arquivo, na ordem em que ele as declara.
     * <p>INVARIANTES DO DOMÍNIO: lista imutável e nunca vazia (a construção já teria falhado).
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public List<ProvedorContexto> obras() {
        return obras;
    }

    private static String texto(Map<String, Object> obra, String campo, String recurso) {
        Object v = obra.get(campo);
        if (!(v instanceof String s) || s.isBlank()) {
            throw new IllegalStateException("Obra sem \"" + campo + "\" no arquivo de lore "
                + recurso + " (id lido: " + obra.get("id") + ")");
        }
        return s;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> conjunto(Object v) {
        if (v == null) {
            return Set.of();
        }
        return Set.copyOf((List<String>) v);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> mapaDeTexto(Object v) {
        if (v == null) {
            return Map.of();
        }
        return Map.copyOf((Map<String, String>) v);
    }

    @SuppressWarnings("unchecked")
    private static Set<List<String>> pares(Object v) {
        if (v == null) {
            return Set.of();
        }
        Set<List<String>> resultado = new LinkedHashSet<>();
        for (Object par : (List<Object>) v) {
            List<String> l = (List<String>) par;
            if (l == null || l.size() != 2) {
                throw new IllegalStateException(
                    "paresInconfundiveis com aridade diferente de 2 no arquivo de lore: " + l);
            }
            resultado.add(List.copyOf(l));
        }
        return Collections.unmodifiableSet(resultado);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: uma obra do arquivo, apresentada pelo contrato que o resto do
     * sistema já consome — quem usa lore não sabe nem precisa saber que ela virou dado.
     * <p>INVARIANTES DO DOMÍNIO: todos os campos imutáveis; nenhum método faz I/O.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; valores ausentes já viraram vazios na carga.
     */
    private record ObraDeArquivo(
        String id,
        String nome,
        String prompt,
        Set<String> apelidos,
        Set<String> termos,
        Set<List<String>> pares,
        Map<String, String> correcoes,
        boolean apareceNaLista) implements ProvedorContexto {

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getNomeExibicao() {
            return nome;
        }

        @Override
        public String obterPromptSistema() {
            return prompt;
        }

        @Override
        public Set<String> apelidosPasta() {
            return apelidos;
        }

        @Override
        public Set<String> termosProtegidos() {
            return termos;
        }

        @Override
        public Set<List<String>> paresInconfundiveis() {
            return pares;
        }

        @Override
        public Map<String, String> correcoesTerminologia() {
            return new LinkedHashMap<>(correcoes);
        }

        @Override
        public boolean apareceNaListaDeObras() {
            return apareceNaLista;
        }
    }
}
