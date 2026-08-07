package org.traducao.projeto.contexto.lore.danmachi;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore da 5ª temporada de DanMachi (arco Freya / Goddess of
 * Fertility) com grafia canônica Syr Flover.
 *
 * <p>INVARIANTES DO DOMÍNIO: Syr Flover (nunca "Flover"); Folkvangr; Charm divino;
 * nomes da Freya Familia.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoDanMachiS5 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Is It Wrong to Try to Pick Up Girls in a Dungeon? V / DanMachi Season 5 (Goddess of Fertility Arc / Freya Arc).
        - Titulo e abreviacao: manter DanMachi, Familia e Orario como termos da obra; nao traduzir nomes de personagens, Familias, deuses, titulos, apelidos ou tecnicas.
        - Personagens principais: Bell Cranel (homem), Hestia (deusa, mulher), Freya (deusa, mulher), Syr Flover (mulher; também ligada a Horn), Horn (mulher), Ottar (homem), Allen Fromel (homem), Hedin Selland (homem), Hogni Ragnar (homem), Heith Velvet (mulher), Mia Grand (mulher), Ryu Lion / Ryu Lion (mulher), Aiz Wallenstein (mulher), Liliruca Arde/Lili (mulher), Welf Crozzo (homem), Haruhime Sanjouno (mulher), Mikoto Yamato (mulher).
        - Familias e grupos: Hestia Familia, Freya Familia, Loki Familia, Hostess of Fertility, Benevolent Mistress. Manter nomes oficiais; "Hostess of Fertility" e "Benevolent Mistress" nao devem ser traduzidos livremente quando usados como nome proprio.
        - Locais: Orario, Babel, Folkvangr, Hostess of Fertility, Pleasure Quarter. Preserve nomes canonicos.
        - Titulos/apelidos: Goddess of Beauty, Goddess of Fertility, War Game, Familia, Level, Status, Skill, Charm. "Charm" de Freya e poder divino; nao reduzir a "charme" casual.
        - Regras de nomes: Bell Cranel nao vira "sino"; Syr Flover (grafia canônica — nunca "Flover" nem adaptar para flor/florista); Freya, Hestia, Ottar, Hedin, Hogni, Allen, Horn, Mia e Heith mantem grafia; Ryu/Ryu nao vira "dragao".
        - Tom: arco de romance, manipulacao divina e conflito emocional; Freya sedutora e soberana, Syr calorosa/misteriosa, Bell sincero e resistente, Hestia protetora.
        """;

    private static final String PROMPT = ContextoPrompt.montar("DanMachi (Season 5)", LORE);

    @Override
    public String getId() {
        return "danmachi_s5";
    }

    @Override
    public String getNomeExibicao() {
        return "DanMachi (Season 5)";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege nomes do arco Freya / Syr Flover.
     * <p>INVARIANTES DO DOMÍNIO: grafia Syr Flover canônica.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Bell Cranel", "Hestia", "Freya",
            "Syr Flover", "Horn", "Ottar",
            "Allen Fromel", "Hedin Selland", "Hogni Ragnar",
            "Heith Velvet", "Mia Grand", "Ryu Lion",
            "Aiz Wallenstein", "Liliruca Arde", "Lili",
            "Welf Crozzo", "Mikoto Yamato", "Haruhime Sanjouno",
            "Orario", "Folkvangr", "Babel",
            "Hostess of Fertility", "Falna", "Familia",
            "Status", "Level", "Skill",
            "Magic", "War Game", "Charm",
            "Freya Familia", "Loki Familia", "Hestia Familia",
            "Goddess of Beauty"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reforço determinístico da terminologia DanMachi (Familia sem
     * acento + grafias erradas de nomes proprios da obra).
     * <p>INVARIANTES DO DOMÍNIO: forma-ruim PT → canônico; só aplica se o EN contém o canônico.
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaDanMachi.mapa();
    }

    /**
     * PROPOSITO DE NEGOCIO: nomes de pasta que identificam ESTA temporada.
     *
     * <p>O prejuizo, medido em 07/08/2026: a pasta chamava-se apenas
     * {@code Season 05}, e a obra derivada do caminho saia "Season 05". O portao
     * obra x contexto nao reconhecia nenhuma lore e se declarava CEGO - avisava e
     * seguia, entao lore errada passaria sem bloqueio. E o mesmo nome vira
     * diretorio de cache: ja existia {@code cache/Season 1/} com 22 arquivos do
     * Unicorn, e qualquer obra com pasta {@code Season 1} cairia la dentro.
     *
     * <p>INVARIANTES DO DOMINIO: a frase precisa ser ESPECIFICA desta temporada.
     * Declarar so "DanMachi" faria as oito lores reivindicarem a mesma pasta com
     * a mesma especificidade - veredicto AMBIGUO, que BLOQUEIA a traducao.
     */
    @Override
    public Set<String> apelidosPasta() {
        return Set.of("DanMachi Season 05", "DanMachi Season 5");
    }
}
