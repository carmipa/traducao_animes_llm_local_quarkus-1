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
    public static final String RECURSO = "/lore/lore.yaml";

    private final List<ProvedorContexto> obras;
    private final List<org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore> obrasRevisao;

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

        // Lê o lado da REVISÃO ANTES de montar as obras: a terminologia dos dois lados é
        // unificada por id, e para unificar é preciso ter os dois em mãos.
        List<Map<String, Object>> itensRevisao = new ArrayList<>();
        Object listaRevisao = mapa.get("revisao");
        if (listaRevisao instanceof List<?> brutos) {
            for (Object item : brutos) {
                if (!(item instanceof Map<?, ?> obra)) {
                    throw new IllegalStateException("Entrada de revisão que não é mapa em " + recurso);
                }
                itensRevisao.add((Map<String, Object>) obra);
            }
        }
        Map<String, Map<String, String>> terminologiaRevisao = new LinkedHashMap<>();
        Set<String> idsRevisao = new LinkedHashSet<>();
        for (Map<String, Object> o : itensRevisao) {
            String id = texto(o, "id", recurso);
            if (!idsRevisao.add(id)) {
                throw new IllegalStateException("Id repetido no lado da revisão: " + id);
            }
            terminologiaRevisao.put(id, mapaDeTexto(o.get("correcoesTerminologia")));
        }

        List<ProvedorContexto> carregadas = new ArrayList<>(itens.size());
        Map<String, Map<String, String>> terminologiaUnificada = new LinkedHashMap<>();
        // Os NOMES da obra, guardados por id para o lado da REVISAO recebe-los logo abaixo.
        // Sao a mesma coisa nos dois lados — quem traduz e quem revisa falam do mesmo
        // personagem — e ate 18/08/2026 a revisao nao os enxergava: usava um roster de 94
        // termos no codigo Java que nao continha 9 dos 11 protagonistas medidos.
        Map<String, Set<String>> protegidosPorId = new LinkedHashMap<>();
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
            Map<String, String> unificada = unificarTerminologia(
                id, mapaDeTexto(o.get("correcoesTerminologia")),
                terminologiaRevisao.getOrDefault(id, Map.of()), recurso);
            terminologiaUnificada.put(id, unificada);
            protegidosPorId.put(id, conjunto(o.get("termosProtegidos")));
            carregadas.add(new ObraDeArquivo(
                id,
                texto(o, "nome", recurso),
                texto(o, "prompt", recurso),
                conjunto(o.get("apelidosPasta")),
                conjunto(o.get("termosProtegidos")),
                pares(o.get("paresInconfundiveis")),
                unificada,
                mapaDeTexto(o.get("traducoesObrigatorias")),
                !(o.get("apareceNaLista") instanceof Boolean b) || b));
        }
        this.obras = List.copyOf(carregadas);

        // FASE E — o lado da REVISÃO, no MESMO arquivo, sob a chave "revisao".
        List<org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore> revisoes = new ArrayList<>();
        for (Map<String, Object> o : itensRevisao) {
            String id = texto(o, "id", recurso);
            // O MESMO mapa que a tradução recebe. Obra que só existe do lado da revisão não
            // tem par para unificar e fica com o próprio — não se inventa contraparte.
            Map<String, String> unificada = terminologiaUnificada.containsKey(id)
                ? terminologiaUnificada.get(id)
                : terminologiaRevisao.getOrDefault(id, Map.of());
            revisoes.add(new RevisaoDeArquivo(
                id, texto(o, "nome", recurso), texto(o, "prompt", recurso), unificada,
                mapaDeListas(o.get("equivalenciasAceitas")),
                protegidosPorId.getOrDefault(id, Set.of())));
        }
        this.obrasRevisao = List.copyOf(revisoes);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: funde a terminologia dos dois lados da mesma obra num mapa só, para
     * que quem TRADUZ enxergue tudo o que quem REVISA aprendeu, e vice-versa.
     *
     * <h2>O prejuízo que originou</h2>
     * Decisão de Paulo em 2026-08-15: <i>"a lore tem de ser compartilhada, é a única exceção.
     * Se não, temos problemas que não valem a pena."</i> O gatilho foi {@code Spearhead} sair
     * como {@code Esquadroe de Ponta} na legenda final do 86 — a revisão conhecia
     * {@code Canela→Shin}, {@code Para RAID→Para-RAID} e {@code Jugernaut→Juggernaut}, e a
     * tradução não. Medido antes de unificar: <b>17 de 68 obras divergentes, 18 entradas só na
     * tradução, 65 só na revisão, ZERO conflito</b>. Juntar os dois arquivos num só resolveu o
     * LUGAR e não a VERDADE: as duas seções continuavam sendo lidas separadamente.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li>A fusão é SIMÉTRICA e acontece no carregamento: os dois lados recebem a mesma
     *       instância. Divergir deixa de ser possível, em vez de depender de alguém lembrar de
     *       cadastrar dos dois lados.</li>
     *   <li><b>Conflito falha FECHADO.</b> Mesma forma-ruim apontando para canônicos diferentes
     *       é duas verdades sobre a mesma coisa — exatamente o que esta decisão existe para
     *       acabar. A medição encontrou zero; se um aparecer, é defeito de cadastro e o boot
     *       para, em vez de um dos lados vencer em silêncio.</li>
     * </ul>
     *
     * <h2>Comportamento em caso de falha</h2>
     * Conflito lança {@link IllegalStateException} nomeando obra, chave e os dois valores.
     *
     * @param id identificador da obra, usado só na mensagem de erro
     * @param daTraducao mapa declarado sob {@code obras}
     * @param daRevisao mapa declarado sob {@code revisao} para o mesmo id
     * @param recurso caminho do arquivo, para a mensagem de erro
     * @return mapa imutável com a união das duas entradas
     */
    private static Map<String, String> unificarTerminologia(
            String id, Map<String, String> daTraducao, Map<String, String> daRevisao, String recurso) {
        if (daRevisao.isEmpty()) {
            return daTraducao;
        }
        Map<String, String> uniao = new LinkedHashMap<>(daTraducao);
        for (Map.Entry<String, String> e : daRevisao.entrySet()) {
            String jaExistente = uniao.putIfAbsent(e.getKey(), e.getValue());
            if (jaExistente != null && !jaExistente.equals(e.getValue())) {
                throw new IllegalStateException(
                    "Terminologia de lore em CONFLITO na obra \"" + id + "\" de " + recurso
                        + ": a forma \"" + e.getKey() + "\" aponta para \"" + jaExistente
                        + "\" em obras e para \"" + e.getValue() + "\" em revisao. "
                        + "São duas verdades sobre o mesmo termo — corrija o arquivo.");
            }
        }
        return Map.copyOf(uniao);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: as obras do lado da REVISÃO de lore, lidas do MESMO arquivo.
     *
     * <p>INVARIANTES DO DOMÍNIO: lista imutável. Pode ser VAZIA sem lançar — o lado da revisão é
     * opcional no arquivo, e um arquivo só com tradução é válido. É diferente do lado da
     * tradução, cuja ausência derruba a aplicação: sem lore de tradução o pipeline traduziria
     * sem lore nenhuma; sem lore de revisão a Opção 7 simplesmente não tem obra para oferecer, e
     * quem consome já trata catálogo vazio.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: id repetido ou obra sem id/nome/prompt lançam na carga.
     */
    public List<org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore> obrasRevisao() {
        return obrasRevisao;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: uma obra do lado da revisão, pelo contrato que a Opção 7 consome.
     * <p>INVARIANTES DO DOMÍNIO: campos imutáveis; sem I/O.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    private record RevisaoDeArquivo(String id, String nome, String prompt, Map<String, String> correcoes,
        Map<String, List<String>> equivalencias, Set<String> protegidos)
        implements org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore {

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Set<String> termosProtegidos() {
            return protegidos;
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
        public Map<String, String> correcoesTerminologia() {
            return correcoes;
        }

        @Override
        public Map<String, List<String>> equivalenciasAceitas() {
            return equivalencias;
        }
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

    /**
     * PROPÓSITO DE NEGÓCIO: lê o mapa "termo do inglês -> formas PT-BR aceitas" do arquivo.
     * <p>INVARIANTES DO DOMÍNIO: chaves e valores normalizados para MINÚSCULAS, porque é assim
     * que o detector compara; mapa e listas imutáveis.
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausente devolve mapa vazio — que significa "a obra não
     * declara equivalência", nunca "aceite qualquer coisa".
     */
    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> mapaDeListas(Object v) {
        if (v == null) {
            return Map.of();
        }
        Map<String, List<String>> resultado = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : ((Map<String, Object>) v).entrySet()) {
            List<String> formas = ((List<String>) e.getValue()).stream()
                .map(f -> f.toLowerCase(java.util.Locale.ROOT))
                .toList();
            resultado.put(e.getKey().toLowerCase(java.util.Locale.ROOT), List.copyOf(formas));
        }
        return Map.copyOf(resultado);
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
        Map<String, String> obrigatorias,
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
        public Map<String, String> traducoesObrigatorias() {
            return new LinkedHashMap<>(obrigatorias);
        }

        @Override
        public boolean apareceNaListaDeObras() {
            return apareceNaLista;
        }
    }
}
