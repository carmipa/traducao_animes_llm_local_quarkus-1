package org.traducao.projeto.contexto.lore.gundam;

import java.util.Map;
import java.util.Set;

import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * ESQUELETO — NAO REGISTRAR. Falta o conteudo de LORE.
 *
 * <p>PROPOSITO DE NEGOCIO: lore de Mobile Suit Gundam MS IGLOO 2: Gravity Front.
 *
 * <h2>Por que este id existe separado</h2>
 * MS IGLOO 2: Gravity Front tem ponto de vista da FEDERACAO e elenco inteiramente outro --
 * a lore de {@link ContextoGundamMsIgloo} cobre os blocos de ZEON (603a / Jotunheim) e nao serve. Juntar os dois numa lore so seria criar uma AGREGADORA -- oferecer ao modelo
 * vocabulario de um recorte enquanto ele traduz outro. E o mesmo erro que esta area ja removeu
 * das lores agregadas de Macross; ver {@code CatracaAgregadorasForaDoCdiTest}.
 *
 * <h2>Como completar</h2>
 * Preencher {@code LORE} com o que aparece NESTA obra, com o GENERO de cada personagem entre
 * parenteses, e completar {@link #termosProtegidos()} com TODO nome proprio declarado na lore --
 * nome que entra como elenco e falta na protecao volta TRADUZIDO.
 *
 * <p>UMA ENTRADA POR PESSOA: personagem com nome revelado ou apelido vai com barra numa entrada
 * so ({@code Luin Lee/Mask (homem)}). Duas entradas fazem o modelo ler dois personagens onde ha
 * um -- ja aconteceu tres vezes neste projeto.
 *
 * <p>Depois: {@code @Component}, total em {@code RegistroProvedoresContextoIT}, contagem em
 * {@code FronteiraContextoArchTest}, saida de {@code SLOTS_LORE_PENDENTE} e de
 * {@code ESQUELETOS_SEM_CONTEUDO}, e o hash no manifesto E7a.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O. Nao e descoberta pelo CDI enquanto estiver assim.
 */
public class ContextoGundamMsIgloo2 implements ProvedorContexto {

    /** VAZIA DE PROPOSITO — ver o Javadoc da classe antes de preencher. */
    private static final String LORE = "";

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam MS IGLOO 2: Gravity Front", LORE);

    @Override
    public String getId() { return "gundam_ms_igloo_2"; }
    @Override
    public String getNomeExibicao() { return "Mobile Suit Gundam MS IGLOO 2: Gravity Front"; }
    @Override
    public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPOSITO DE NEGOCIO: nomes proprios desta obra que a traducao preserva no original.
     * <p>INVARIANTES DO DOMINIO: tem de cobrir TODO nome proprio declarado em {@code LORE}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of("Mobile Suit", "Zeon", "Federation");
    }

    /**
     * PROPOSITO DE NEGOCIO: reforco deterministico do nucleo UC.
     * <p>INVARIANTES DO DOMINIO: forma-ruim PT -> canonico.
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutavel; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUc.mapa();
    }
}
