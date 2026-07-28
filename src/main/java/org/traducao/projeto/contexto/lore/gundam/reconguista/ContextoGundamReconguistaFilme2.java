package org.traducao.projeto.contexto.lore.gundam.reconguista;

import java.util.Map;
import java.util.Set;
import org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc;

import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * ESQUELETO — NAO REGISTRAR. Falta o conteudo de LORE.
 *
 * <p>PROPOSITO DE NEGOCIO: lore do filme II de Gundam Reconguista in G
 * ("Bellri's Fierce Charge"). O id {@code gundam_greco_2} ja existe em {@code CatalogoObras} com nome de
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
public class ContextoGundamReconguistaFilme2 implements ProvedorContexto {

    /** VAZIA DE PROPOSITO — ver o Javadoc da classe antes de preencher. */
    private static final String LORE = "";

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
        return new ContextoGundamReconguista().termosProtegidos();
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
