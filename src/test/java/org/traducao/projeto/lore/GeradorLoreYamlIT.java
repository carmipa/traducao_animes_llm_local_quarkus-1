package org.traducao.projeto.lore;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * PROPÓSITO DE NEGÓCIO: gera o arquivo ÚNICO de lore da tradução a partir dos provedores REAIS,
 * e prova no mesmo experimento que o YAML relido reproduz cada obra campo a campo. É a FASE B da
 * ordem de Paulo em 2026-08-15: <i>"todas as lores devem ficar em um único arquivo"</i>.
 *
 * <h2>Por que gerar em vez de escrever</h2>
 * São 82 arquivos e 69 obras. Transcrever à mão prompt, termos e mapas seria reintroduzir
 * exatamente o defeito que esta frente existe para eliminar — foi digitando em dois lugares que
 * os catálogos divergiram. O arquivo nasce da fonte viva, e o que é humano na migração é só a
 * CICATRIZ (os 477 comentários medidos), que entra depois, obra a obra.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li><b>Ida e volta byte a byte.</b> O YAML é serializado, RELIDO e comparado com o provedor
 *       vivo em todos os campos. Qualquer diferença ABORTA sem escrever arquivo aproveitável —
 *       é a regra "guarda que aborta vale mais que cuidado" aplicada à própria migração.</li>
 *   <li><b>Ordem determinística.</b> Obras por id; conjuntos e mapas por chave. Isto não é
 *       cosmético: {@code Map.ofEntries} tem ordem de iteração <b>não especificada e
 *       aleatorizada por execução da JVM</b>, então o desempate do enforcador entre duas
 *       formas-ruim de mesmo comprimento já variava entre execuções. Ordenar troca um
 *       comportamento dependente de sal por um estável — é ganho, não regressão, e fica
 *       declarado aqui porque parece detalhe de formatação e não é.</li>
 *   <li>O arquivo sai em {@code build/tmp/} (árvore descartável). Promovê-lo a recurso
 *       versionado é ato deliberado de quem migra, nunca efeito colateral de rodar a suíte.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Catálogo CDI vazio reprova como {@code NÃO VERIFICADO}. Divergência na ida e volta reprova
 * nomeando obra e campo. Não reprova por conteúdo de lore — não é este o julgamento dele.
 */
@QuarkusTest
@DisplayName("FASE B: gera o arquivo único de lore da tradução e prova a ida e volta")
class GeradorLoreYamlIT {

    private static final Path SAIDA = Path.of("build", "tmp", "lore-traducao.yaml");

    /**
     * FASE E: o lado da REVISÃO, emitido no MESMO arquivo. Sai separado em {@code build/tmp/}
     * para ser fundido no arquivo versionado sem sobrescrever as cicatrizes já migradas.
     */
    private static final Path SAIDA_REVISAO = Path.of("build", "tmp", "lore-revisao.yaml");

    @Inject
    List<ProvedorContexto> catalogo;

    @Inject
    org.traducao.projeto.revisaoLore.application.GerenciadorPromptRevisaoLore catalogoRevisao;

    @Test
    void gerarEProvarIdaEVolta() throws IOException {
        assertTrue(!catalogo.isEmpty(),
            "NÃO VERIFICADO: nenhum ProvedorContexto no CDI — nada a gerar");

        List<ProvedorContexto> ordenado = catalogo.stream()
            .sorted(Comparator.comparing(ProvedorContexto::getId))
            .toList();

        List<Map<String, Object>> obras = new ArrayList<>();
        for (ProvedorContexto p : ordenado) {
            obras.add(comoMapa(p));
        }
        Map<String, Object> raiz = new LinkedHashMap<>();
        raiz.put("obras", obras);

        String yaml = dumper().dump(raiz);

        // IDA E VOLTA — o ponto do teste. Relê o texto gerado e confere contra o provedor VIVO.
        // Sem isto, o arquivo poderia sair bonito e com um prompt truncado por regra de bloco do
        // YAML, e só a próxima tradução mostraria.
        Object recarregado = new Yaml().load(yaml);
        List<String> divergencias = new ArrayList<>();
        conferir(recarregado, ordenado, divergencias);

        Files.createDirectories(SAIDA.getParent());
        if (divergencias.isEmpty()) {
            Files.writeString(SAIDA, yaml, StandardCharsets.UTF_8);
        }

        assertTrue(divergencias.isEmpty(),
            () -> "O YAML relido NÃO reproduz os provedores — arquivo não foi escrito.\n  "
                + String.join("\n  ", divergencias));

        System.out.println();
        System.out.println("=== ARQUIVO ÚNICO DE LORE (TRADUÇÃO) ===");
        System.out.println("  obras .......... " + obras.size());
        System.out.println("  bytes .......... " + yaml.getBytes(StandardCharsets.UTF_8).length);
        System.out.println("  linhas ......... " + yaml.lines().count());
        System.out.println("  ida e volta .... OK em todos os campos de todas as obras");
        System.out.println("  escrito em ..... " + SAIDA.toAbsolutePath());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: FASE E — emite o lado da REVISÃO para o MESMO arquivo de lore, sob
     * a chave {@code revisao} de cada obra. É o passo em que a lore passa a existir INTEIRA num
     * lugar só: até aqui, a lore real de uma obra era a união de dois pacotes, e essa união não
     * existia em lugar nenhum do código.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li>Sai em arquivo PRÓPRIO em {@code build/tmp/}, para ser fundido no versionado sem
     *       sobrescrever as 377 linhas de cicatriz já migradas. Regenerar o arquivo inteiro é
     *       exatamente o acidente que a catraca de cicatriz existe para reprovar.</li>
     *   <li>Ida e volta provada aqui também: id, nome, prompt e mapa de terminologia.</li>
     *   <li>O PROMPT da revisão é diferente do da tradução <b>de propósito</b> e por isso vai em
     *       campo separado. Unificar o texto do prompt é outra decisão, declarada fora de escopo.</li>
     * </ul>
     *
     * <h2>Comportamento em caso de falha</h2>
     * Catálogo vazio reprova como {@code NÃO VERIFICADO}; divergência na ida e volta reprova
     * nomeando obra e campo, e o arquivo não é escrito.
     */
    @Test
    @DisplayName("FASE E: emite o lado da REVISÃO para o mesmo arquivo, com ida e volta provada")
    void gerarLadoDaRevisao() throws IOException {
        var provedores = catalogoRevisao.getProvedores().stream()
            .sorted(Comparator.comparing(
                org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore::getId))
            .toList();
        assertTrue(!provedores.isEmpty(),
            "NÃO VERIFICADO: nenhum ProvedorPromptRevisaoLore no CDI — nada a gerar");

        List<Map<String, Object>> obras = new ArrayList<>();
        for (var p : provedores) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("nome", p.getNomeExibicao());
            if (p.correcoesTerminologia() != null && !p.correcoesTerminologia().isEmpty()) {
                m.put("correcoesTerminologia", new TreeMap<>(p.correcoesTerminologia()));
            }
            m.put("prompt", p.obterPromptSistema());
            obras.add(m);
        }
        Map<String, Object> raiz = new LinkedHashMap<>();
        raiz.put("revisao", obras);

        String yaml = dumper().dump(raiz);

        @SuppressWarnings("unchecked")
        Map<String, Object> lido = (Map<String, Object>) new Yaml().load(yaml);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> relidas = (List<Map<String, Object>>) lido.get("revisao");
        List<String> divergencias = new ArrayList<>();
        if (relidas == null || relidas.size() != provedores.size()) {
            divergencias.add("contagem mudou na ida e volta");
        } else {
            for (int i = 0; i < provedores.size(); i++) {
                var p = provedores.get(i);
                Map<String, Object> o = relidas.get(i);
                comparar(divergencias, p.getId(), "id", p.getId(), o.get("id"));
                comparar(divergencias, p.getId(), "nome", p.getNomeExibicao(), o.get("nome"));
                comparar(divergencias, p.getId(), "prompt", p.obterPromptSistema(), o.get("prompt"));
                @SuppressWarnings("unchecked")
                Map<String, String> lidoMapa = o.get("correcoesTerminologia") == null
                    ? Map.of() : (Map<String, String>) o.get("correcoesTerminologia");
                comparar(divergencias, p.getId(), "correcoesTerminologia",
                    new TreeMap<>(p.correcoesTerminologia()), new TreeMap<>(lidoMapa));
            }
        }

        Files.createDirectories(SAIDA_REVISAO.getParent());
        if (divergencias.isEmpty()) {
            Files.writeString(SAIDA_REVISAO, yaml, StandardCharsets.UTF_8);
        }
        assertTrue(divergencias.isEmpty(),
            () -> "O YAML da revisão relido NÃO reproduz os provedores — arquivo não escrito.\n  "
                + String.join("\n  ", divergencias));

        System.out.println();
        System.out.println("=== LADO DA REVISÃO (FASE E) ===");
        System.out.println("  obras .......... " + obras.size());
        System.out.println("  linhas ......... " + yaml.lines().count());
        System.out.println("  ida e volta .... OK");
        System.out.println("  escrito em ..... " + SAIDA_REVISAO.toAbsolutePath());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte um provedor no registro de dados que vai para o arquivo.
     * <p>INVARIANTES DO DOMÍNIO: só campos de DADO; nenhum comportamento é serializado, porque
     * nenhuma lore tem comportamento (medido: zero condicional em 82 arquivos). Conjuntos e
     * mapas saem ordenados.
     * <p>COMPORTAMENTO EM CASO DE FALHA: par inconfundível com aridade diferente de 2 aborta —
     * é violação de contrato e não pode virar linha silenciosamente estranha no arquivo.
     */
    private Map<String, Object> comoMapa(ProvedorContexto p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("nome", p.getNomeExibicao());
        m.put("apareceNaLista", p.apareceNaListaDeObras());
        if (!p.apelidosPasta().isEmpty()) {
            m.put("apelidosPasta", new ArrayList<>(new TreeSet<>(p.apelidosPasta())));
        }
        if (!p.termosProtegidos().isEmpty()) {
            m.put("termosProtegidos", new ArrayList<>(new TreeSet<>(p.termosProtegidos())));
        }
        if (!p.paresInconfundiveis().isEmpty()) {
            List<List<String>> pares = new ArrayList<>();
            for (List<String> par : p.paresInconfundiveis()) {
                if (par == null || par.size() != 2) {
                    fail("paresInconfundiveis() de " + p.getId() + " viola o contrato de 2 termos");
                }
                List<String> ordenado = new ArrayList<>(par);
                ordenado.sort(String::compareTo);
                pares.add(ordenado);
            }
            pares.sort(Comparator.comparing(par -> par.get(0) + ' ' + par.get(1)));
            m.put("paresInconfundiveis", pares);
        }
        if (!p.correcoesTerminologia().isEmpty()) {
            m.put("correcoesTerminologia", new TreeMap<>(p.correcoesTerminologia()));
        }
        m.put("prompt", p.obterPromptSistema());
        return m;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: confere o que foi relido contra a fonte viva, campo a campo.
     * <p>INVARIANTES DO DOMÍNIO: compara CONTEÚDO, não formatação — conjunto contra conjunto,
     * mapa contra mapa, texto contra texto exato. O prompt é comparado sem normalização nenhuma,
     * porque é justamente ele que a serialização pode estragar.
     * <p>COMPORTAMENTO EM CASO DE FALHA: acumula a divergência em vez de lançar na primeira,
     * para o relatório mostrar o padrão do defeito e não só a primeira ocorrência.
     */
    @SuppressWarnings("unchecked")
    private void conferir(Object recarregado, List<ProvedorContexto> vivos, List<String> saida) {
        Map<String, Object> raiz = (Map<String, Object>) recarregado;
        List<Map<String, Object>> obras = (List<Map<String, Object>>) raiz.get("obras");
        if (obras == null || obras.size() != vivos.size()) {
            saida.add("contagem de obras mudou na ida e volta: "
                + (obras == null ? "null" : obras.size()) + " != " + vivos.size());
            return;
        }
        for (int i = 0; i < vivos.size(); i++) {
            ProvedorContexto p = vivos.get(i);
            Map<String, Object> o = obras.get(i);
            comparar(saida, p.getId(), "id", p.getId(), o.get("id"));
            comparar(saida, p.getId(), "nome", p.getNomeExibicao(), o.get("nome"));
            comparar(saida, p.getId(), "apareceNaLista",
                p.apareceNaListaDeObras(), o.get("apareceNaLista"));
            comparar(saida, p.getId(), "prompt", p.obterPromptSistema(), o.get("prompt"));
            comparar(saida, p.getId(), "apelidosPasta",
                new TreeSet<>(p.apelidosPasta()),
                new TreeSet<>(lista(o.get("apelidosPasta"))));
            comparar(saida, p.getId(), "termosProtegidos",
                new TreeSet<>(p.termosProtegidos()),
                new TreeSet<>(lista(o.get("termosProtegidos"))));
            comparar(saida, p.getId(), "correcoesTerminologia",
                new TreeMap<>(p.correcoesTerminologia()),
                new TreeMap<>(o.get("correcoesTerminologia") == null
                    ? Map.<String, String>of() : (Map<String, String>) o.get("correcoesTerminologia")));

            TreeSet<String> paresVivos = new TreeSet<>();
            for (List<String> par : p.paresInconfundiveis()) {
                List<String> ord = new ArrayList<>(par);
                ord.sort(String::compareTo);
                paresVivos.add(ord.get(0) + " ↔ " + ord.get(1));
            }
            TreeSet<String> paresLidos = new TreeSet<>();
            for (Object par : lista(o.get("paresInconfundiveis"))) {
                List<String> l = (List<String>) par;
                paresLidos.add(l.get(0) + " ↔ " + l.get(1));
            }
            comparar(saida, p.getId(), "paresInconfundiveis", paresVivos, paresLidos);
        }
    }

    private static List<Object> lista(Object o) {
        return o == null ? List.of() : (List<Object>) o;
    }

    private static void comparar(List<String> saida, String obra, String campo,
                                 Object vivo, Object lido) {
        if (!java.util.Objects.equals(vivo, lido)) {
            saida.add(obra + " | " + campo + " | vivo=" + recorte(vivo) + " lido=" + recorte(lido));
        }
    }

    private static String recorte(Object o) {
        String s = String.valueOf(o);
        return s.length() > 90 ? "«" + s.substring(0, 90) + "…»" : "«" + s + "»";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: configura a serialização para o arquivo ser LEGÍVEL e EDITÁVEL por
     * quem cuida da lore — bloco literal para o prompt, uma obra por bloco, sem largura máxima
     * quebrando frase no meio.
     * <p>INVARIANTES DO DOMÍNIO: {@code setSplitLines(false)} é obrigatório; com a quebra
     * automática do SnakeYAML uma frase longa do prompt vira duas linhas e volta com um espaço
     * a mais — a ida e volta acusaria, mas o arquivo já teria saído feio.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; nunca lança.
     */
    private static Yaml dumper() {
        DumperOptions opcoes = new DumperOptions();
        opcoes.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opcoes.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        opcoes.setPrettyFlow(true);
        opcoes.setIndent(2);
        opcoes.setIndicatorIndent(1);
        opcoes.setIndentWithIndicator(true);
        opcoes.setSplitLines(false);
        opcoes.setLineBreak(DumperOptions.LineBreak.UNIX);
        return new Yaml(opcoes);
    }
}
