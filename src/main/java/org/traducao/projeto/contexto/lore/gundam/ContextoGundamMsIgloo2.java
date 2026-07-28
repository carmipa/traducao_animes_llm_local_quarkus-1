package org.traducao.projeto.contexto.lore.gundam;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * REGISTRADA em 2026-07-28: LORE preenchida e par lore/protecao conferido.
 *
 * <p>PROPOSITO DE NEGOCIO: lore de Mobile Suit Gundam MS IGLOO 2: Gravity Front.
 *
 * <h2>Por que este id existe separado</h2>
 * MS IGLOO 2: Gravity Front tem ponto de vista da FEDERACAO e elenco inteiramente outro --
 * a lore de {@link ContextoGundamMsIgloo} cobre os blocos de ZEON (603a / Jotunheim) e nao serve. Juntar os dois numa lore so seria criar uma AGREGADORA -- oferecer ao modelo
 * vocabulario de um recorte enquanto ele traduz outro. E o mesmo erro que esta area ja removeu
 * das lores agregadas de Macross; ver {@code CatracaAgregadorasForaDoCdiTest}.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt, termos e mapa imutaveis.
 */
@Component
public class ContextoGundamMsIgloo2 implements ProvedorContexto {

    /** VAZIA DE PROPOSITO — ver o Javadoc da classe antes de preencher. */
    private static final String LORE = """
        - Obra: Mobile Suit Gundam MS IGLOO 2: Gravity Front (OVA, U.C. 0079).
        - Personagens: Ben Barberry (homem), Papa Sidney Lewis (homem), Michael Colmatta (homem), Harman Yandell (homem), Rayban Surat (homem), Arleen Nazon (mulher), Clyde Bettany (homem), Milos Karppi (homem), Doroba Kuzwayo (homem), Elmer Snell/White Ogre (homem), Death Deity (mulher), Kycilia Zabi (mulher).
        - Faccoes/unidades: Earth Federation, 44th Hybrid Regiment, 301st Tank Squadron, Principality of Zeon.
        - Mobile suits e tecnologia: RTX-440 Ground Assault Type Guntank, Type 61 Tank, Zaku II, Dabude, Mobile Suit.
        - Termos do mundo: One Year War, Earth Federation, Principality of Zeon, Odessa, Mobile Suit. Mantenha nomes proprios em ingles/romanizados.
        """;

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
        return Set.of(
            "Ben Barberry", "Papa Sidney Lewis", "Michael Colmatta", "Harman Yandell",
            "Rayban Surat", "Arleen Nazon", "Clyde Bettany", "Milos Karppi",
            "Doroba Kuzwayo", "Elmer Snell", "White Ogre", "Death Deity", "Kycilia Zabi",
            "Earth Federation", "44th Hybrid Regiment", "301st Tank Squadron",
            "Principality of Zeon", "RTX-440 Ground Assault Type Guntank", "RTX-440",
            "Type 61 Tank", "Type 61", "Zaku II", "Dabude", "Mobile Suit",
            "One Year War", "Zeon", "Federation", "Odessa"
        );
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
