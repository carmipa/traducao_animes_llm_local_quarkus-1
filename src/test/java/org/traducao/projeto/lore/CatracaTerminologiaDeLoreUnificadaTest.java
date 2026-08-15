package org.traducao.projeto.lore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;
import org.traducao.projeto.lore.infrastructure.CatalogoLoreYaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela a decisão de que a lore tem FONTE ÚNICA — quem traduz e quem
 * revisa enxergam exatamente a mesma terminologia.
 *
 * <h2>O prejuízo que originou</h2>
 * Ordem de Paulo em 2026-08-15: <i>"a lore tem de ser compartilhada, é a única exceção. Se
 * não, temos problemas que não valem a pena."</i> O gatilho foi {@code Spearhead} chegar à
 * legenda final do 86 como {@code Esquadroe de Ponta}, com a revisão já conhecendo formas que
 * a tradução ignorava.
 *
 * <p>Medido em duas etapas, e a segunda é o motivo desta catraca existir:
 * <pre>
 *   antes de juntar os arquivos ..... 17 de 68 obras divergentes, 18 só na tradução, 65 só na revisão
 *   com UM arquivo, duas seções ..... 17 de 68 — IDÊNTICO. Mudou o LUGAR, não a VERDADE.
 *   com a união no carregamento ..... 0 de 68
 * </pre>
 * Juntar num arquivo só não bastou porque as duas seções continuavam sendo lidas em separado.
 * Sem esta catraca, o próximo termo aprendido volta a nascer de um lado só e a divergência
 * recomeça — em silêncio, como da primeira vez.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Para toda obra presente nos DOIS lados, o mapa efetivo é o MESMO.</li>
 *   <li>A união não perde nada: o mapa efetivo contém tudo o que cada lado declarava.</li>
 *   <li>Conflito — mesma forma-ruim com canônicos diferentes — falha FECHADO no carregamento.
 *       Duas verdades sobre o mesmo termo é precisamente o que a decisão proíbe.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprovar aqui significa que a lore voltou a ter dois donos, ou que a fusão passou a engolir
 * entrada. As duas coisas terminam do mesmo jeito: termo perdido na legenda final.
 */
@DisplayName("catraca: a terminologia de lore tem fonte única")
class CatracaTerminologiaDeLoreUnificadaTest {

    private final CatalogoLoreYaml catalogo = new CatalogoLoreYaml();

    @Test
    @DisplayName("toda obra nos dois lados tem o MESMO mapa de terminologia")
    void osDoisLadosVeemAMesmaCoisa() {
        Map<String, Map<String, String>> daTraducao = new LinkedHashMap<>();
        for (ProvedorContexto p : catalogo.obras()) {
            daTraducao.put(p.getId(), p.correcoesTerminologia());
        }

        List<String> divergentes = new ArrayList<>();
        int comparadas = 0;
        for (ProvedorPromptRevisaoLore r : catalogo.obrasRevisao()) {
            Map<String, String> t = daTraducao.get(r.getId());
            if (t == null) {
                continue; // obra só do lado da revisão: não há par para comparar
            }
            comparadas++;
            if (!t.equals(r.correcoesTerminologia())) {
                var soT = new TreeSet<>(t.keySet());
                soT.removeAll(r.correcoesTerminologia().keySet());
                var soR = new TreeSet<>(r.correcoesTerminologia().keySet());
                soR.removeAll(t.keySet());
                divergentes.add(r.getId() + "  só na tradução=" + soT + "  só na revisão=" + soR);
            }
        }

        assertTrue(comparadas > 0,
            "NÃO VERIFICADO: nenhuma obra existe nos dois lados — a catraca não teve o que comparar");
        assertTrue(divergentes.isEmpty(),
            () -> "a lore voltou a ter dois donos em " + divergentes.size() + " obra(s):\n"
                + String.join("\n", divergentes));
    }

    /**
     * Unificar não pode ser sinônimo de perder — e este teste compara contra o ARQUIVO, não
     * contra o outro lado já carregado.
     *
     * <h2>Por que contra o YAML cru</h2>
     * A primeira versão comparava a saída da tradução com a saída da revisão. Passava por
     * CONSTRUÇÃO: como os dois lados recebem a mesma instância, qualquer implementação
     * degenerada — inclusive uma que jogasse fora o mapa da revisão e entregasse o da tradução
     * aos dois — deixaria os dois "iguais" e o teste verde. A mutação provou isso: com a união
     * desligada, este teste continuou passando enquanto 65 entradas sumiam.
     *
     * <p>Só a fonte serve de referência. O mapa efetivo tem de conter, entrada por entrada, o
     * que CADA seção do arquivo declara.
     */
    @Test
    @DisplayName("a união não engole entrada de nenhum dos lados (conferido contra o YAML)")
    void nadaSeperdeNaUniao() {
        Map<String, Map<String, String>> cruObras = terminologiaDeclaradaNoArquivo("obras");
        Map<String, Map<String, String>> cruRevisao = terminologiaDeclaradaNoArquivo("revisao");
        assertTrue(!cruObras.isEmpty() && !cruRevisao.isEmpty(),
            "NÃO VERIFICADO: não consegui ler a terminologia declarada nas duas seções do lore.yaml");

        Map<String, Map<String, String>> efetivoTraducao = new LinkedHashMap<>();
        for (ProvedorContexto p : catalogo.obras()) {
            efetivoTraducao.put(p.getId(), p.correcoesTerminologia());
        }
        Map<String, Map<String, String>> efetivoRevisao = new LinkedHashMap<>();
        for (ProvedorPromptRevisaoLore r : catalogo.obrasRevisao()) {
            efetivoRevisao.put(r.getId(), r.correcoesTerminologia());
        }

        List<String> perdidas = new ArrayList<>();
        int conferidas = 0;
        for (var secao : List.of(Map.entry("obras", cruObras), Map.entry("revisao", cruRevisao))) {
            for (var obra : secao.getValue().entrySet()) {
                String id = obra.getKey();
                for (Map.Entry<String, String> e : obra.getValue().entrySet()) {
                    conferidas++;
                    for (var lado : List.of(Map.entry("tradução", efetivoTraducao),
                                            Map.entry("revisão", efetivoRevisao))) {
                        Map<String, String> efetivo = lado.getValue().get(id);
                        if (efetivo == null) {
                            continue; // obra ausente daquele lado: não há o que conferir
                        }
                        if (!e.getValue().equals(efetivo.get(e.getKey()))) {
                            perdidas.add("declarada em " + secao.getKey() + "." + id + " (\""
                                + e.getKey() + "\" -> \"" + e.getValue() + "\") NÃO chegou ao lado "
                                + lado.getKey());
                        }
                    }
                }
            }
        }
        final int total = conferidas;
        assertTrue(total > 0, "NÃO VERIFICADO: nenhuma entrada declarada para conferir");
        assertTrue(perdidas.isEmpty(),
            () -> "a fusão perdeu " + perdidas.size() + " de " + total + " entrada(s) declarada(s):\n"
                + String.join("\n", perdidas.subList(0, Math.min(15, perdidas.size()))));
    }

    /** Lê a terminologia como o ARQUIVO a declara, sem passar pelo carregador sob teste. */
    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, String>> terminologiaDeclaradaNoArquivo(String secao) {
        Object raiz;
        try (java.io.InputStream in =
                 CatalogoLoreYaml.class.getResourceAsStream(CatalogoLoreYaml.RECURSO)) {
            raiz = new org.yaml.snakeyaml.Yaml().load(
                new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("não consegui ler o lore.yaml para conferir", e);
        }
        Map<String, Map<String, String>> porObra = new LinkedHashMap<>();
        Object lista = ((Map<String, Object>) raiz).get(secao);
        if (!(lista instanceof List<?> itens)) {
            return porObra;
        }
        for (Object item : itens) {
            Map<String, Object> o = (Map<String, Object>) item;
            Object mapa = o.get("correcoesTerminologia");
            if (!(mapa instanceof Map<?, ?> m) || m.isEmpty()) {
                continue;
            }
            Map<String, String> entradas = new LinkedHashMap<>();
            m.forEach((k, v) -> entradas.put(String.valueOf(k), String.valueOf(v)));
            porObra.put(String.valueOf(o.get("id")), entradas);
        }
        return porObra;
    }

    /**
     * O CASO-CONTROLE. Sem ele a catraca acima poderia estar passando por cegueira — um
     * catálogo que unificasse escolhendo um lado ao acaso também deixaria os dois "iguais".
     */
    @Test
    @DisplayName("CASO DOENTE: canônicos diferentes para a mesma forma derrubam o carregamento")
    void conflitoFalhaFechado() {
        IllegalStateException erro = assertThrows(IllegalStateException.class,
            () -> new CatalogoLoreYaml("/lore/lore-conflito-terminologia.yaml"),
            "conflito de terminologia passou: um dos lados venceria em silêncio e ninguém saberia qual");

        String msg = String.valueOf(erro.getMessage());
        assertTrue(msg.contains("obra_de_teste") && msg.contains("Lanca-Flanco"),
            "a mensagem precisa nomear a obra e a forma em conflito, senão não se acha o cadastro errado: " + msg);
        assertTrue(msg.contains("Spearhead") && msg.contains("Ponta de Lanca"),
            "a mensagem precisa mostrar OS DOIS canônicos, senão não se sabe qual está errado: " + msg);
    }

    /** O arquivo real de produção carrega sem conflito — se um entrar, o boot para. */
    @Test
    @DisplayName("o lore.yaml de produção carrega sem conflito")
    void producaoCarregaLimpo() {
        assertEquals(catalogo.obras().size(), new CatalogoLoreYaml().obras().size());
        assertTrue(!catalogo.obras().isEmpty(), "NÃO VERIFICADO: catálogo de produção vazio");
    }
}
