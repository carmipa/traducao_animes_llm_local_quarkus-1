package org.traducao.projeto.contexto.lore.sidonia;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore da série Knights of Sidonia — as DUAS temporadas
 * (2014 e Battle for Planet Nine, 2015) num contexto só. O filme tem id próprio
 * ({@code sidonia_movie}).
 *
 * <h2>Por que as duas temporadas ficam JUNTAS</h2>
 * Esta classe cobre mais de uma obra, o que neste projeto normalmente é sinal de "lore agregada"
 * — o defeito que tirou três lores de Macross do registro e dividiu MS IGLOO e Thunderbolt em ids
 * separados. O critério escrito é: <em>agregadora fica fora quando a união introduz vocabulário
 * EXCLUSIVO de um título que contamina outro</em>.
 *
 * <p>Aplicado aqui, o critério diz para NÃO separar. Auditoria de 2026-07-28, nome a nome e termo
 * a termo: do elenco declarado, só {@code Tsumugi Shiraui} é exclusivo da temporada 2 — Nagate,
 * Shizuka, Izana, Kobayashi, Kunato, Lala e Yuhata aparecem já na 1. Do glossário
 * ({@code Gauna}, {@code Garde}, {@code Kabizashi}, {@code Ena}, {@code Heigus}, {@code placenta},
 * {@code gauna core}, {@code Toha Heavy Industries}, {@code Immortal Ship Committee}) NENHUM é
 * exclusivo de uma temporada. Série contínua não é o mesmo que filmes independentes: separar
 * produziria duas lores quase idênticas, e o ganho não paga o custo.
 *
 * <p>Se um dia houver {@code sidonia_s1} / {@code sidonia_s2}, a única remoção obrigatória da
 * temporada 1 seria {@code Tsumugi Shiraui}.
 *
 * <h2>Lacuna conhecida: a lore está SUBDECLARADA na temporada 1</h2>
 * A mesma auditoria achou o problema oposto ao esperado. Não há viés para a temporada 2 — há
 * ausência de nomes da 1: irmãs Honoka, Samari Ittan, esquadrão Akai, Benisuzume, Yure Shinatose.
 * E {@code Ochiai} e {@code Tsugumori} estão em {@link #termosProtegidos()} mas FORA do texto da
 * lore — quem escreveu a proteção conhecia os dois e não os declarou.
 *
 * <p>Essa direção (protegido sem declarar) é a INÓCUA; a perigosa é a inversa, declarar na lore e
 * não proteger, que volta traduzido. Ainda assim é dívida: o prompt não conhece elenco que a obra
 * tem. Enriquecer a temporada 1 é tarefa do lado da TRADUÇÃO, pendente.
 *
 * <p>INVARIANTES DO DOMÍNIO: Izana Shinatose; Gauna; Garde; Ena; Heigus.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoKnightsOfSidonia implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Knights of Sidonia / Sidonia no Kishi.
        - A humanidade vive na nave-semente Sidonia apos a destruicao da Terra pelos Gauna.
        - Termos centrais: Sidonia, Gauna, Gardes, Kabizashi, Ena, Heigus particles, placenta, gauna core.
        - Use "Guarda" apenas se o contexto for funcao comum; para mechas/pilotos, mantenha Garde/Gardes como termo da obra.
        - "Ena" e o orgao/membrana bioenergetica que cobre certos humanos modificados, permite fotossintese e potencializa o uso de particulas Heigus; nunca traduza como simples "pele" ou "aura".
        - Organizacoes e lugares: Toha Heavy Industries, Immortal Ship Committee, Residential Layer, Photosynthesis Chamber, hangar de Gardes.
        - Principais nomes: Nagate Tanikaze (m), Shizuka Hoshijiro (f), Izana Shinatose (terceiro genero/intersexo conforme a obra — NUNCA "Shinoshinari"), Tsumugi Shiraui (f), Captain Kobayashi (f), Yuhata Midorikawa (f), Norio Kunato (m), Lala Hiyama (f).
        - Conceitos biologicos/sociais: fotossintese humana, clones, terceiro genero de Izana, imortalidade de certos lideres, hibridizacao Gauna.
        - Tom: ficcao cientifica militar e existencial; traduza termos tecnicos de forma clara, mantendo estranhamento biologico e tensao de sobrevivencia.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Knights of Sidonia", LORE);

    @Override
    public String getId() {
        return "sidonia";
    }

    @Override
    public String getNomeExibicao() {
        return "Knights of Sidonia";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege nomes e termos biológicos de Sidonia.
     * <p>INVARIANTES DO DOMÍNIO: Izana Shinatose canônico.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Nagate Tanikaze", "Izana Shinatose", "Shizuka Hoshijiro",
            "Tsumugi Shiraui", "Norio Kunato", "Yuhata Midorikawa",
            "Kobayashi", "Lala Hiyama", "Ochiai",
            "Sidonia", "Gauna", "Garde",
            "Tsugumori", "Kabizashi", "Ena",
            "Heigus", "placenta", "Gauna core",
            "Toha Heavy Industries", "Immortal Ship Committee", "Residential Layer",
            "Photosynthesis Chamber"
        );
    }
}
