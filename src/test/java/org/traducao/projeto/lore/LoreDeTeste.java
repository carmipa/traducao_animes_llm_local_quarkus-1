package org.traducao.projeto.lore;

import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.traducao.projeto.lore.infrastructure.CatalogoLoreYaml;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * PROPÓSITO DE NEGÓCIO: dá aos testes a obra de lore pelo ID, agora que a lore vive no arquivo
 * único e não em 78 classes Java. Substitui o {@code new ContextoX()} que os testes usavam.
 *
 * <h2>Por que um resolvedor, e não cada teste lendo o arquivo</h2>
 * São 13 arquivos de teste que amarravam classe concreta. Espalhar {@code new CatalogoLoreYaml()}
 * por 13 lugares significaria treze leituras do mesmo YAML por execução e treze cópias da mesma
 * decisão. Aqui o catálogo é lido UMA vez por JVM de teste.
 *
 * <p>E há um ganho que não é de desempenho: os testes passam a exercitar <b>a mesma fonte que a
 * produção usa</b>. Antes eles instanciavam a classe diretamente e podiam ficar verdes com uma
 * lore que o CDI nem registrava; agora, se a obra sumir do arquivo, o teste que fala dela
 * reprova nomeando o id.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Id desconhecido REPROVA nomeando o id pedido — nunca devolve nulo nem uma obra vazia.
 *       Obra vazia faria o teste passar afirmando sobre nada.</li>
 *   <li>Só leitura; nenhum teste altera o catálogo.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Arquivo ausente ou defeituoso propaga a exceção do {@link CatalogoLoreYaml}, que falha
 * fechado — o mesmo comportamento da produção.
 */
public final class LoreDeTeste {

    private static final Map<String, ProvedorContexto> POR_ID = carregar();

    private LoreDeTeste() {
    }

    private static Map<String, ProvedorContexto> carregar() {
        Map<String, ProvedorContexto> m = new LinkedHashMap<>();
        for (ProvedorContexto p : new CatalogoLoreYaml().obras()) {
            m.put(p.getId(), p);
        }
        return m;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a obra de lore com este id, como a produção a enxerga.
     * <p>INVARIANTES DO DOMÍNIO: id desconhecido reprova o teste em vez de devolver nulo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code fail} com o id pedido e quantas obras existem.
     *
     * @param id id da obra no arquivo de lore (ex.: {@code eight_six}, {@code gundam_unicorn})
     */
    public static ProvedorContexto obra(String id) {
        ProvedorContexto p = POR_ID.get(id);
        if (p == null) {
            fail("Obra de lore \"" + id + "\" não existe no arquivo único ("
                + POR_ID.size() + " obras carregadas). Se ela foi renomeada, o teste precisa "
                + "saber — este resolvedor não adivinha.");
        }
        return p;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: atalho para o mapa de terminologia da obra, que é o que a maioria
     * destes testes afirma.
     * <p>INVARIANTES DO DOMÍNIO: mesmas de {@link #obra(String)}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: idem.
     */
    public static Map<String, String> terminologia(String id) {
        return obra(id).correcoesTerminologia();
    }

    private static final Map<String, org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore>
        REVISAO_POR_ID = carregarRevisao();

    private static Map<String, org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore> carregarRevisao() {
        Map<String, org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore> m = new LinkedHashMap<>();
        for (var p : new CatalogoLoreYaml().obrasRevisao()) {
            m.put(p.getId(), p);
        }
        return m;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a obra do lado da REVISÃO de lore, pelo id — substitui o
     * {@code new ContextoRevisaoLoreX()} depois que as 80 classes viraram o arquivo único.
     * <p>INVARIANTES DO DOMÍNIO: id desconhecido reprova nomeando o id.
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code fail} com o id e quantas obras foram carregadas.
     */
    public static org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore revisao(String id) {
        var p = REVISAO_POR_ID.get(id);
        if (p == null) {
            fail("Obra de REVISÃO de lore \"" + id + "\" não existe no arquivo único ("
                + REVISAO_POR_ID.size() + " carregadas).");
        }
        return p;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: atalho para o mapa de terminologia do lado da revisão.
     * <p>INVARIANTES DO DOMÍNIO: mesmas de {@link #revisao(String)}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: idem.
     */
    public static Map<String, String> terminologiaRevisao(String id) {
        return revisao(id).correcoesTerminologia();
    }
}
