package org.traducao.projeto.contexto.lore.gundam.reconguista;

import java.util.Map;
import java.util.Set;
import org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * REGISTRADA em 2026-07-28: LORE preenchida e conferida (par lore/protecao completo).
 *
 * <p>PROPOSITO DE NEGOCIO: lore do filme II de Gundam Reconguista in G
 * ("Bellri's Fierce Charge"). O id {@code gundam_greco_2} ja existe em {@code CatalogoObras} com nome de
 * exibicao cadastrado, e o proprio codigo la declara "Contrato p/ lore: ids gundam_greco_1..5".
 * Registrada no CDI em 2026-07-28; a obra aparece no seletor.
 *
 * <h2>Por que nao basta apontar para a lore da serie</h2>
 * {@link ContextoGundamReconguista} cobre a serie inteira. Os cinco filmes sao recompilacao com
 * material novo: podem OMITIR personagens que a serie tem e reorganizar terminologia. Registrar
 * a serie no lugar do filme e a mesma especie de erro das lores agregadas Macross — oferecer ao
 * LLM vocabulario de um corte que nao e o que esta sendo traduzido.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt, termos e mapa imutaveis.
 */
@Component
public class ContextoGundamReconguistaFilme2 implements ProvedorContexto {

    /** VAZIA DE PROPOSITO — ver o Javadoc da classe antes de preencher. */
    private static final String LORE = """
        - Obra: Gundam Reconguista in G II: Bellri's Fierce Charge.
        - Personagens: Bellri Zenam (homem), Aida Surugan (mulher), Raraiya Monday (mulher), Noredo Nug (mulher), Klim Nick (homem), Luin Lee/Mask (homem), Mick Jack (mulher), Cumpa Rusita (homem), Wilmit Zenam (mulher).
        - Faccoes: Capital Tower, Capital Guard, Capital Army, Ameria.
        - Mobile suits e tecnologia: G-Self, G-Arcane, Montero, Mack Knife, Photon Battery.
        - Termos do mundo: Capital Tower, Photon Battery, Regild Century, Capital Guard, Capital Army, Amerian Army. Mantenha nomes proprios em ingles/romanizados. Nao converter "Reconguista" para "Reconquista"; mantenha Reconguista.
        """;

    private static final String PROMPT = ContextoPrompt.montar(
        "Gundam Reconguista in G II: Bellri's Fierce Charge", LORE);

    @Override
    public String getId() { return "gundam_greco_2"; }
    @Override
    public String getNomeExibicao() { return "Gundam: Reconguista in G II - Bellri's Fierce Charge"; }
    @Override
    public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPOSITO DE NEGOCIO: termos canonicos do Regild Century que a traducao preserva no
     * original. Herda o nucleo da serie; ajustar quando a LORE deste filme for escrita.
     * <p>INVARIANTES DO DOMINIO: grafias oficiais; conjunto imutavel.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O.
     */
    @Override
    public Set<String> termosProtegidos() {
        Set<String> termos = new java.util.LinkedHashSet<>(
            new ContextoGundamReconguista().termosProtegidos());
        // Nomes que ESTE filme declara e o conjunto da serie nao conhece. Sem isto eles
        // entrariam no prompt como elenco e ficariam SEM protecao contra localizacao --
        // "Manny Ambassada" poderia voltar como "Embaixadora Manny". Lore que declara e
        // protecao que nao cobre e o mesmo furo ja registrado na linha de mechas do
        // Labyrinth of Time.
        termos.addAll(Set.of(
            "Amerian Army"));
        return java.util.Collections.unmodifiableSet(termos);
    }

    /**
     * PROPOSITO DE NEGOCIO: reforco deterministico do nucleo UC mais os termos proprios da obra.
     * <p>INVARIANTES DO DOMINIO: espelho do que a serie declara; a contraparte de revisao tem de
     * declarar o MESMO mapa, senao a catraca de paridade acusa.
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutavel; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUc.comExtras(Map.ofEntries(
            Map.entry("Reconquista", "Reconguista"),
            Map.entry("Bateria de Fóton", "Photon Battery")
        ));
    }
}
