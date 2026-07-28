package org.traducao.projeto.contexto.lore.gundam;

import java.util.Map;
import java.util.Set;

import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * ESQUELETO — NAO REGISTRAR. Falta o conteudo de LORE.
 *
 * <p>PROPOSITO DE NEGOCIO: lore de Mobile Suit Gundam Thunderbolt. O id {@code gundam_thunderbolt} ja existe em
 * {@code CatalogoObras} e o codigo la o declara "slot reservado (UC 0079), lore pendente".
 * Ate esta classe ganhar conteudo e {@code @Component}, a obra NAO aparece no seletor.
 *
 * <h2>Por que esta e prioridade</h2>
 * Thunderbolt (UC 0079) — o Setor Thunderbolt, a Federacao contra os remanescentes de Zeon,
 * com elenco e mobile suits proprios. Diferente dos filmes de Reconguista, que ao menos tem a lore da SERIE registrada
 * como base, esta obra nao tem fallback NENHUM: nao existe classe para ela em lugar nenhum do
 * repositorio. Quem traduzir hoje escolhe outra obra do seletor ou vai sem contexto.
 *
 * <h2>Como completar</h2>
 * Preencher {@code LORE} com o que aparece NESTA obra, no formato das irmas UC, com o GENERO de
 * cada personagem entre parenteses. O genero nao e parseado por codigo: e texto que vai no
 * prompt, e o prompt injeta {@code RegrasConcordanciaPtBr.BLOCO_TRADUCAO}, que manda nunca cair
 * em masculino automatico e, na duvida, usar formulacao NEUTRA. Sem genero declarado o dialogo
 * fica achatado em neutro onde caberia "exausta"/"exausto".
 *
 * <p>ATENCAO ao par lore/protecao: todo nome proprio que entrar em {@code LORE} tem de entrar
 * tambem em {@link #termosProtegidos()}. Nome declarado como elenco e ausente da protecao volta
 * TRADUZIDO — furo ja encontrado duas vezes neste projeto.
 *
 * <p>Depois: {@code @Component}, contraparte em {@code revisaoLore.contexto}, total em
 * {@code RegistroProvedoresContextoIT}, contagem em {@code FronteiraContextoArchTest}, saida de
 * {@code SLOTS_LORE_PENDENTE} e de {@code ESQUELETOS_SEM_CONTEUDO}, e o hash no manifesto E7a.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O. Nao e descoberta pelo CDI enquanto estiver assim.
 */
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
