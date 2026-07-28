package org.traducao.projeto.contexto.lore.gundam;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * REGISTRADA em 2026-07-28: LORE preenchida e conferida (par lore/protecao completo).
 *
 * <p>PROPOSITO DE NEGOCIO: lore de Mobile Suit Gundam Thunderbolt. O id {@code gundam_thunderbolt} ja existe em
 * {@code CatalogoObras} e o codigo la o declara "slot reservado (UC 0079), lore pendente".
 * Registrada no CDI em 2026-07-28; a obra aparece no seletor.
 *
 * <h2>Por que esta e prioridade</h2>
 * Thunderbolt (UC 0079) — o Setor Thunderbolt, a Federacao contra os remanescentes de Zeon,
 * com elenco e mobile suits proprios. Diferente dos filmes de Reconguista, que ao menos tem a lore da SERIE registrada
 * como base, esta obra nao tem fallback NENHUM: nao existe classe para ela em lugar nenhum do
 * repositorio. Quem traduzir hoje escolhe outra obra do seletor ou vai sem contexto.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt, termos e mapa imutaveis.
 */
@Component
public class ContextoGundamThunderbolt implements ProvedorContexto {

    /** VAZIA DE PROPOSITO — ver o Javadoc da classe antes de preencher. */
    private static final String LORE = """
        - Obra: Mobile Suit Gundam Thunderbolt — December Sky / Thunderbolt Sector (U.C. 0079).
        - Personagens: Io Fleming (homem), Daryl Lorenz (homem), Claudia Peer (mulher), Cornelius KaKa (homem), Karla Mitchum (mulher).
        - Faccoes/unidades: Earth Federation, Moore Brotherhood, Beehive, Principality of Zeon, Living Dead Division, Dried Fish.
        - Mobile suits e tecnologia: Full Armor Gundam, Psycho Zaku, Reuse P. Device, Rick Dom, Zaku I, GM, Big Gun, Mobile Suit.
        - Termos do mundo: Thunderbolt Sector, One Year War, Side 4, Moore, A Baoa Qu, Principality of Zeon, Earth Federation, Mobile Suit. Mantenha nomes proprios em ingles/romanizados.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam Thunderbolt", LORE);

    @Override
    public String getId() { return "gundam_thunderbolt"; }
    @Override
    public String getNomeExibicao() { return "Mobile Suit Gundam Thunderbolt"; }
    @Override
    public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPOSITO DE NEGOCIO: nomes proprios, mobile suits, unidades e termos desta obra que a
     * traducao preserva no original.
     * <p>INVARIANTES DO DOMINIO: tem de cobrir TODO nome proprio declarado em {@code LORE};
     * conjunto imutavel.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Io Fleming", "Daryl Lorenz", "Claudia Peer", "Cornelius KaKa", "Karla Mitchum",
            "Earth Federation", "Moore Brotherhood", "Beehive", "Principality of Zeon",
            "Living Dead Division", "Dried Fish", "Full Armor Gundam", "Psycho Zaku",
            "Reuse P. Device", "Rick Dom", "Zaku I", "GM", "Big Gun", "Mobile Suit",
            "Thunderbolt Sector", "One Year War", "Side 4", "Moore", "A Baoa Qu",
            "Zeon", "Federation"
        );
    }

    /**
     * PROPOSITO DE NEGOCIO: reforco deterministico do nucleo UC (Newtype, Mobile Suit, Beam
     * Saber/Rifle, Mobile Armor, Oldtype).
     * <p>INVARIANTES DO DOMINIO: forma-ruim PT -> canonico; a contraparte de revisao tem de
     * declarar o MESMO mapa, senao a catraca de paridade acusa.
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutavel; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUc.mapa();
    }
}
