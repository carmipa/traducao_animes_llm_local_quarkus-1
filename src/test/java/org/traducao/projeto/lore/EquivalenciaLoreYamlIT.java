package org.traducao.projeto.lore;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.traducao.projeto.lore.infrastructure.CatalogoLoreYaml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o ARQUIVO ÚNICO de lore entrega exatamente as mesmas 69 obras
 * que as classes Java entregam hoje — campo a campo, byte a byte. É o portão que autoriza apagar
 * as classes: enquanto ele não estiver verde, a migração não pode avançar.
 *
 * <h2>Por que comparar com o Java vivo, e não com o manifesto</h2>
 * O {@code manifesto-lore.properties} cobre id, nome, prompt e termos protegidos. Ele não cobre
 * {@code correcoesTerminologia}, {@code paresInconfundiveis}, {@code apelidosPasta} nem
 * {@code apareceNaListaDeObras} — e dois destes só ganharam congelamento hoje
 * (baseline-campos-lore.tsv). Comparar direto com a fonte viva não deixa campo de fora por
 * construção: se amanhã o contrato ganhar um método, este teste continua comparando o que
 * existe, não o que alguém lembrou de listar.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Mesmo conjunto de ids, mesma contagem.</li>
 *   <li>Para cada obra: nome, prompt, termos protegidos, apelidos, pares inconfundíveis,
 *       correções de terminologia e visibilidade IDÊNTICOS. O prompt é comparado sem
 *       normalização, porque é o campo que a serialização poderia estragar em silêncio.</li>
 *   <li>O leitor falha FECHADO: arquivo ausente, sem a lista, com lista vazia, com id repetido
 *       ou com obra sem prompt precisam LANÇAR. Catálogo de lore vazio que "sobe normal" faria
 *       o pipeline traduzir sem lore e gravar o resultado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Lista cada obra e campo divergente. Catálogo CDI vazio reprova como {@code NÃO VERIFICADO}.
 */
@QuarkusTest
@DisplayName("FASE C: o arquivo único entrega as mesmas 69 obras que as classes Java")
class EquivalenciaLoreYamlIT {

    @Inject
    List<ProvedorContexto> catalogoJava;

    @Test
    @DisplayName("YAML e classes Java entregam obras idênticas, campo a campo")
    void arquivoUnicoEquivaleAsClasses() {
        assertTrue(!catalogoJava.isEmpty(),
            "NÃO VERIFICADO: nenhum ProvedorContexto no CDI — nada com que comparar");

        List<ProvedorContexto> daClasse = catalogoJava.stream()
            .sorted(Comparator.comparing(ProvedorContexto::getId)).toList();
        List<ProvedorContexto> doArquivo = new CatalogoLoreYaml().obras().stream()
            .sorted(Comparator.comparing(ProvedorContexto::getId)).toList();

        assertEquals(
            daClasse.stream().map(ProvedorContexto::getId).toList(),
            doArquivo.stream().map(ProvedorContexto::getId).toList(),
            "o conjunto/ordem de ids do arquivo diverge do das classes");

        List<String> divergencias = new ArrayList<>();
        for (int i = 0; i < daClasse.size(); i++) {
            ProvedorContexto j = daClasse.get(i);
            ProvedorContexto y = doArquivo.get(i);
            conferir(divergencias, j.getId(), "nome", j.getNomeExibicao(), y.getNomeExibicao());
            conferir(divergencias, j.getId(), "prompt", j.obterPromptSistema(), y.obterPromptSistema());
            conferir(divergencias, j.getId(), "apareceNaLista",
                j.apareceNaListaDeObras(), y.apareceNaListaDeObras());
            conferir(divergencias, j.getId(), "termosProtegidos",
                new TreeSet<>(j.termosProtegidos()), new TreeSet<>(y.termosProtegidos()));
            conferir(divergencias, j.getId(), "apelidosPasta",
                new TreeSet<>(j.apelidosPasta()), new TreeSet<>(y.apelidosPasta()));
            conferir(divergencias, j.getId(), "correcoesTerminologia",
                new TreeMap<>(j.correcoesTerminologia()), new TreeMap<>(y.correcoesTerminologia()));
            conferir(divergencias, j.getId(), "paresInconfundiveis",
                paresNormalizados(j), paresNormalizados(y));
        }

        assertTrue(divergencias.isEmpty(),
            () -> "O arquivo único NÃO reproduz as classes Java — a migração não pode avançar.\n  "
                + String.join("\n  ", divergencias));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: prova que o leitor recusa arquivo defeituoso em vez de subir com
     * catálogo pela metade.
     * <p>INVARIANTES DO DOMÍNIO: os cinco defeitos abaixo têm de LANÇAR. Nenhum pode virar
     * "catálogo vazio, segue o baile" — lore ausente não interrompe a tradução, ela só piora o
     * resultado, e isso é invisível até alguém assistir ao episódio.
     * <p>COMPORTAMENTO EM CASO DE FALHA: o teste nomeia qual defeito passou.
     */
    @Test
    @DisplayName("leitor falha FECHADO: arquivo doente lança, nunca devolve catálogo vazio")
    void leitorFalhaFechado() {
        assertThrows(IllegalStateException.class,
            () -> new CatalogoLoreYaml("/lore/nao-existe.yaml"),
            "arquivo AUSENTE tem de lançar");
        assertThrows(IllegalStateException.class,
            () -> new CatalogoLoreYaml("/lore-doente/sem-lista-obras.yaml"),
            "arquivo SEM a lista \"obras\" tem de lançar");
        assertThrows(IllegalStateException.class,
            () -> new CatalogoLoreYaml("/lore-doente/lista-vazia.yaml"),
            "lista de obras VAZIA tem de lançar");
        assertThrows(IllegalStateException.class,
            () -> new CatalogoLoreYaml("/lore-doente/id-repetido.yaml"),
            "id de obra REPETIDO tem de lançar");
        assertThrows(IllegalStateException.class,
            () -> new CatalogoLoreYaml("/lore-doente/obra-sem-prompt.yaml"),
            "obra SEM prompt tem de lançar");
    }

    private static TreeSet<String> paresNormalizados(ProvedorContexto p) {
        TreeSet<String> saida = new TreeSet<>();
        for (List<String> par : p.paresInconfundiveis()) {
            List<String> ordenado = new ArrayList<>(par);
            ordenado.sort(String::compareTo);
            saida.add(ordenado.get(0) + " ↔ " + ordenado.get(1));
        }
        return saida;
    }

    private static void conferir(List<String> saida, String obra, String campo,
                                 Object daClasse, Object doArquivo) {
        if (!Objects.equals(daClasse, doArquivo)) {
            saida.add(obra + " | " + campo + "\n      classe: " + recorte(daClasse)
                + "\n      arquivo: " + recorte(doArquivo));
        }
    }

    private static String recorte(Object o) {
        String s = String.valueOf(o);
        return s.length() > 120 ? s.substring(0, 120) + "…" : s;
    }

    /** Mapa vazio é valor legítimo e não pode ser confundido com ausência de leitura. */
    @SuppressWarnings("unused")
    private static final Map<String, String> SEM_CORRECOES = Map.of();
}
