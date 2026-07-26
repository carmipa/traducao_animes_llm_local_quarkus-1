package org.traducao.projeto.contexto.domain;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa as garantias do {@link SnapshotContexto} — a fotografia
 * imutável do contexto que um JOB carrega do início ao fim. Sem elas, a guarda
 * obra×contexto seria fachada: bastaria o snapshot observar mudanças posteriores para o
 * arquivo voltar a sair com prompt de uma obra e proveniência de outra.
 *
 * <p>INVARIANTES DO DOMÍNIO: o snapshot é coerente (todos os campos do mesmo provedor),
 * imutável (coleções copiadas) e ENXUTO — carrega o prompt congelado e nenhum hash, porque
 * derivar o carimbo do cache é assunto da fronteira de integração da fatia {@code traducao},
 * não deste domínio.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: provedor que devolva coleções nulas degrada para
 * vazias, nunca para {@code NullPointerException} no meio de uma tradução.
 */
class SnapshotContextoTest {

    /**
     * PROPÓSITO DE NEGÓCIO: provedor de teste com coleções MUTÁVEIS e mutantes, para provar
     * que o snapshot não observa alterações feitas depois da fotografia.
     * <p>INVARIANTES DO DOMÍNIO: as coleções devolvidas são as mesmas instâncias que o teste
     * altera em seguida.
     * <p>COMPORTAMENTO EM CASO DE FALHA: retornos determinísticos; não lança.
     */
    private static final class ContextoMutavel implements ProvedorContexto {
        final Set<String> termos = new HashSet<>(Set.of("Anavel Gato"));
        final Map<String, String> correcoes = new HashMap<>(Map.of("Coveiro", "Undertaker"));

        @Override public String getId() { return "gundam_0083"; }
        @Override public String getNomeExibicao() { return "Gundam 0083"; }
        @Override public String obterPromptSistema() { return ContextoPrompt.montar("0083", "Lore de 0083."); }
        @Override public Set<String> termosProtegidos() { return termos; }
        @Override public Map<String, String> correcoesTerminologia() { return correcoes; }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: provedor degenerado que devolve nulos, para provar a degradação
     * segura exigida pelo contrato.
     */
    private static final class ContextoComNulos implements ProvedorContexto {
        @Override public String getId() { return "nulo"; }
        @Override public String getNomeExibicao() { return "Nulo"; }
        @Override public String obterPromptSistema() { return "prompt"; }
        @Override public Set<String> termosProtegidos() { return null; }
        @Override public Map<String, String> correcoesTerminologia() { return null; }
    }

    @Test
    void snapshotNaoObservaMudancasPosterioresDoProvedor() {
        ContextoMutavel provedor = new ContextoMutavel();
        SnapshotContexto snapshot = SnapshotContexto.de(provedor);

        provedor.termos.add("Shu Ouma");
        provedor.correcoes.put("Vazios", "Voids");

        assertEquals(Set.of("Anavel Gato"), snapshot.termosProtegidos(),
            "o snapshot é uma fotografia: alterar a origem depois não pode alterá-lo");
        assertEquals(Map.of("Coveiro", "Undertaker"), snapshot.correcoesTerminologia());
    }

    @Test
    void colecoesDoSnapshotSaoImutaveis() {
        SnapshotContexto snapshot = SnapshotContexto.de(new ContextoMutavel());

        assertThrows(UnsupportedOperationException.class, () -> snapshot.termosProtegidos().add("X"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.correcoesTerminologia().put("a", "b"));
    }

    @Test
    void provedorComColecoesNulasDegradaParaVazioSemLancar() {
        SnapshotContexto snapshot = SnapshotContexto.de(new ContextoComNulos());

        assertEquals(Set.of(), snapshot.termosProtegidos());
        assertEquals(Map.of(), snapshot.correcoesTerminologia());
    }

    @Test
    void provedorNuloDevolveSnapshotNeutro() {
        assertEquals(SnapshotContexto.NEUTRO, SnapshotContexto.de(null));
        assertEquals(SnapshotContexto.PROMPT_NEUTRO, SnapshotContexto.NEUTRO.promptSistema());
        assertEquals(SnapshotContexto.NOME_NEUTRO, SnapshotContexto.NEUTRO.nomeExibicao());
        assertEquals(null, SnapshotContexto.NEUTRO.id(),
            "id nulo é o que faz a proveniência neutra divergir de qualquer geração anterior");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o snapshot NÃO calcula hash. Quem deriva o hash da proveniência é
     * a fronteira de integração da fatia {@code traducao}
     * ({@code ResolvedorCacheTraducao.provenienciaDe}), com
     * {@code ProvenienciaCache.hashDe(promptSistema())}. Este teste congela essa decisão por
     * REFLEXÃO porque uma reintrodução do algoritmo aqui compilaria em silêncio: teríamos
     * DUAS fontes do mesmo hash em módulos diferentes e, se divergissem, todo o cache já
     * gravado em disco passaria a ser lido como de outra origem e seria descartado.
     *
     * <p>INVARIANTES DO DOMÍNIO: o record expõe {@code promptSistema} (a matéria-prima do
     * hash) e nenhum membro de hash — nem componente, nem método utilitário.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer método/componente cujo nome contenha
     * "hash" (fora do {@code hashCode} herdado de record) reprova aqui.
     */
    @Test
    void snapshotNaoCarregaHashNemDuplicaOAlgoritmoDoCarimboDeCache() {
        assertNotNull(SnapshotContexto.de(new ContextoMutavel()).promptSistema(),
            "o prompt congelado é o que a fronteira de cache usa para derivar o hash");

        List<String> membrosDeHash = Stream.of(SnapshotContexto.class.getDeclaredMethods())
            .map(Method::getName)
            .filter(nome -> nome.toLowerCase(Locale.ROOT).contains("hash"))
            .filter(nome -> !nome.equals("hashCode"))
            .toList();

        assertEquals(List.of(), membrosDeHash,
            "SnapshotContexto não pode recalcular o hash da proveniência: a única fonte do "
                + "algoritmo é ProvenienciaCache.hashDe, aplicada em ResolvedorCacheTraducao");
    }

    @Test
    void loreDoSnapshotEhSoALoreCruaPorTrasDoPrompt() {
        String prompt = ContextoPrompt.montar("0083", "Lore de 0083.");
        SnapshotContexto snapshot = SnapshotContexto.de(new ContextoMutavel());

        assertEquals(ContextoPrompt.obterLore(prompt), snapshot.lore());
        assertNotEquals(snapshot.promptSistema(), snapshot.lore(),
            "a lore é um recorte do prompt, não o prompt inteiro");
        assertTrue(snapshot.promptSistema().contains(snapshot.lore()),
            "a lore precisa ser parte do prompt que foi congelado junto");
    }
}
