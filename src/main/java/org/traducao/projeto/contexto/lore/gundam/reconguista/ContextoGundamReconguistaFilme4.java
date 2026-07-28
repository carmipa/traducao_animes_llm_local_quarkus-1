package org.traducao.projeto.contexto.lore.gundam.reconguista;

import java.util.Map;
import java.util.Set;
import org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc;

import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * ESQUELETO — NAO REGISTRAR. Falta o conteudo de LORE.
 *
 * <p>PROPOSITO DE NEGOCIO: lore do filme IV de Gundam Reconguista in G
 * ("Love That Cries Out in Battle"). O id {@code gundam_greco_4} ja existe em {@code CatalogoObras} com nome de
 * exibicao cadastrado, e o proprio codigo la declara "Contrato p/ lore: ids gundam_greco_1..5".
 * Ate esta classe ganhar conteudo e {@code @Component}, a obra NAO aparece no seletor.
 *
 * <h2>Por que nao basta apontar para a lore da serie</h2>
 * {@link ContextoGundamReconguista} cobre a serie inteira. Os cinco filmes sao recompilacao com
 * material novo: podem OMITIR personagens que a serie tem e reorganizar terminologia. Registrar
 * a serie no lugar do filme e a mesma especie de erro das lores agregadas Macross — oferecer ao
 * LLM vocabulario de um corte que nao e o que esta sendo traduzido.
 *
 * <h2>Como completar</h2>
 * Preencher {@code LORE} com o que aparece NESTE filme, no formato das obras que ja declaram
 * genero — {@code - Personagens: Nome (homem), Nome (mulher), ...}. O genero NAO e decoracao:
 * o prompt de traducao injeta {@code RegrasConcordanciaPtBr.BLOCO_TRADUCAO}, que manda o modelo
 * inferir genero e, na duvida, cair em formulacao NEUTRA. Sem genero na lore o modelo nao erra
 * para masculino, mas achata o dialogo em neutro onde caberia "exausta"/"exausto".
 * A lore da SERIE tem esse buraco: ela lista "Principais nomes" sem genero nenhum.
 *
 * <p>Depois: {@code @Component}, contraparte em {@code revisaoLore.contexto}, total em
 * {@code RegistroProvedoresContextoIT}, contagem em {@code FronteiraContextoArchTest}, saida de
 * {@code SLOTS_LORE_PENDENTE} e do {@code ESQUELETOS_SEM_CONTEUDO}, e o hash no manifesto E7a.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O. Nao e descoberta pelo CDI enquanto estiver assim.
 */
public class ContextoGundamReconguistaFilme4 implements ProvedorContexto {

    /** VAZIA DE PROPOSITO — ver o Javadoc da classe antes de preencher. */
    private static final String LORE = """
        - Obra: Gundam Reconguista in G IV: Love That Cries Out in Battle.
        - Personagens: Bellri Zenam (homem), Aida Surugan/Aida Rayhunton (mulher), Raraiya Monday/Raraiya Akuparl (mulher), Noredo Nug (mulher), Klim Nick (homem), Luin Lee/Mask (homem), Mick Jack (mulher), Manny Ambassada (mulher), La Gu (homem).
        - Faccoes: Venus Globe, G-IT, Hermes Foundation, Ameria.
        - Mobile suits e tecnologia: G-Self, G-Arcane, G-Lucifer, Kabakali, Photon Battery.
        - Termos do mundo: Venus Globe, Photon Battery, Regild Century, Hermes Foundation, Reconguista, Rayhunton Code, Amerian Army. Mantenha nomes proprios em ingles/romanizados. Nao converter "Reconguista" para "Reconquista"; mantenha Reconguista.
        """;

    private static final String PROMPT = ContextoPrompt.montar(
        "Gundam Reconguista in G IV: Love That Cries Out in Battle", LORE);

    @Override
    public String getId() { return "gundam_greco_4"; }
    @Override
    public String getNomeExibicao() { return "Gundam: Reconguista in G IV - Love That Cries Out in Battle"; }
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
            "Amerian Army",
            "G-IT",
            "G-Lucifer",
            "Hermes Foundation",
            "La Gu",
            "Manny Ambassada",
            "Raraiya Akuparl"));
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
