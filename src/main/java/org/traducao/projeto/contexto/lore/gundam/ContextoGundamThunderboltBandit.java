package org.traducao.projeto.contexto.lore.gundam;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * REGISTRADA em 2026-07-28: LORE preenchida e par lore/protecao conferido.
 *
 * <p>PROPOSITO DE NEGOCIO: lore de Mobile Suit Gundam Thunderbolt: Bandit Flower.
 *
 * <h2>Por que este id existe separado</h2>
 * Bandit Flower se passa DEPOIS da guerra, com Atlas Gundam, South Seas Alliance e Bianca --
 * vocabulario que nao existe no December Sky coberto por {@link ContextoGundamThunderbolt}. Juntar os dois numa lore so seria criar uma AGREGADORA -- oferecer ao modelo
 * vocabulario de um recorte enquanto ele traduz outro. E o mesmo erro que esta area ja removeu
 * das lores agregadas de Macross; ver {@code CatracaAgregadorasForaDoCdiTest}.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt, termos e mapa imutaveis.
 */
@Component
public class ContextoGundamThunderboltBandit implements ProvedorContexto {

    /** VAZIA DE PROPOSITO — ver o Javadoc da classe antes de preencher. */
    private static final String LORE = """
        - Obra: Mobile Suit Gundam Thunderbolt: Bandit Flower (filme, pos One Year War / U.C. 0080).
        - Personagens: Io Fleming (homem), Daryl Lorenz (homem), Claudia Peer (mulher), Cornelius KaKa (homem), Bianca Carlyle (mulher), Karla Mitchum (mulher), Vincent Pike (homem), Monica Humphrey (mulher), Levan Fuu (homem), Chow Ming (mulher), Bull (homem).
        - Faccoes/unidades: Earth Federation, Spartan, South Seas Alliance, Principality of Zeon, Zeon remnants, Republic of Zeon.
        - Mobile suits e tecnologia: Atlas Gundam, Guncannon Aqua, Psycho Zaku, Reuse P. Device, Acguy, Gogg, Grublo, Gouf, GM, Mobile Suit.
        - Termos do mundo: South Seas Alliance, Spartan, Reuse P. Device, One Year War, Antarctica, Newtype, Principality of Zeon, Earth Federation, Mobile Suit. Mantenha nomes proprios em ingles/romanizados.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam Thunderbolt: Bandit Flower", LORE);

    @Override
    public String getId() { return "gundam_thunderbolt_bandit"; }
    @Override
    public String getNomeExibicao() { return "Mobile Suit Gundam Thunderbolt: Bandit Flower"; }
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
            "Io Fleming", "Daryl Lorenz", "Claudia Peer", "Cornelius KaKa",
            "Bianca Carlyle", "Karla Mitchum", "Vincent Pike", "Monica Humphrey",
            "Levan Fuu", "Chow Ming", "Bull",
            "Earth Federation", "Spartan", "South Seas Alliance", "Principality of Zeon",
            "Zeon remnants", "Republic of Zeon", "Atlas Gundam", "Guncannon Aqua",
            "Psycho Zaku", "Reuse P. Device", "Acguy", "Gogg", "Grublo", "Gouf", "GM",
            "Mobile Suit", "One Year War", "Antarctica", "Newtype", "Zeon", "Federation"
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
