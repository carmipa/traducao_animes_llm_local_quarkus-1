package org.traducao.projeto.contexto.lore.gundam;

import java.util.Map;
import java.util.Set;

import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * ESQUELETO — NAO REGISTRAR. Falta o conteudo de LORE.
 *
 * <p>PROPOSITO DE NEGOCIO: lore de Mobile Suit Gundam MS IGLOO. O id {@code gundam_ms_igloo} ja existe em
 * {@code CatalogoObras} e o codigo la o declara "slot reservado (UC 0079), lore pendente".
 * Ate esta classe ganhar conteudo e {@code @Component}, a obra NAO aparece no seletor.
 *
 * <h2>Por que esta e prioridade</h2>
 * MS IGLOO (UC 0079) — as OVAs em CG do lado de Zeon: a 603a Unidade de Testes Tecnicos e
 * os prototipos que nunca entraram em producao. Diferente dos filmes de Reconguista, que ao menos tem a lore da SERIE registrada
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
public class ContextoGundamMsIgloo implements ProvedorContexto {

    /** VAZIA DE PROPOSITO — ver o Javadoc da classe antes de preencher. */
    private static final String LORE = """
        - Obra: Mobile Suit Gundam MS IGLOO — The Hidden One Year War / Apocalypse 0079 (OVA, U.C. 0079).
        - Personagens: Oliver May (homem), Monique Cadillac (mulher), Martin Prochnow (homem), Albert Schacht (homem), Domenico Marquez (homem), Erich Kruger (homem), Hideo Washiya (homem), Jean Xavier (mulher), Aleksandro Hemme (homem), Demeziere Sonnen (homem), Jean Luc Duvall (homem), Werner Holbein (homem), Erwin Cadillac (homem), Herbert von Kuspen (homem), Gihren Zabi (homem).
        - Faccoes/unidades: Principality of Zeon, 603rd Technical Evaluation Unit, Jotunheim, Earth Federation.
        - Mobile suits e tecnologia: EMS-10 Zudah, YMT-05 Hildolfr, QCX-76A Jormungand, MSM-07Di Ze'Gok, MP-02A Oggo, MA-05Ad Big Rang, Zaku II, Mobile Suit.
        - Termos do mundo: One Year War, Principality of Zeon, Earth Federation, Loum, Jaburo, Solomon, A Baoa Qu, Mobile Suit. Mantenha nomes proprios em ingles/romanizados.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam MS IGLOO", LORE);

    @Override
    public String getId() { return "gundam_ms_igloo"; }
    @Override
    public String getNomeExibicao() { return "Mobile Suit Gundam MS IGLOO"; }
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
            "Oliver May", "Monique Cadillac", "Martin Prochnow", "Albert Schacht",
            "Domenico Marquez", "Erich Kruger", "Hideo Washiya", "Jean Xavier",
            "Aleksandro Hemme", "Demeziere Sonnen", "Jean Luc Duvall", "Werner Holbein",
            "Erwin Cadillac", "Herbert von Kuspen", "Gihren Zabi",
            "Principality of Zeon", "603rd Technical Evaluation Unit", "Jotunheim",
            "Earth Federation", "EMS-10 Zudah", "Zudah", "YMT-05 Hildolfr", "Hildolfr",
            "QCX-76A Jormungand", "Jormungand", "MSM-07Di Ze'Gok", "Ze'Gok",
            "MP-02A Oggo", "Oggo", "MA-05Ad Big Rang", "Big Rang", "Zaku II",
            "Mobile Suit", "One Year War", "Zeon", "Federation", "Loum", "Jaburo",
            "Solomon", "A Baoa Qu"
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
