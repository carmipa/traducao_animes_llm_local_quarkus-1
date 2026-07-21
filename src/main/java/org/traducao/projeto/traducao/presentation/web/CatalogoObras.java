package org.traducao.projeto.traducao.presentation.web;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: metadados de APRESENTAÇÃO das obras no seletor de contexto —
 * a qual franquia cada obra pertence (para agrupar com {@code <optgroup>}), a ordem
 * cronológica dentro do grupo e o rótulo padronizado do grupo Gundam. Resolve o
 * problema do select alfabético que enterrava, por exemplo, o Zeta no meio de 18
 * "Mobile Suit...".
 *
 * <p>INVARIANTES DO DOMÍNIO: opera só sobre {@code id}/nome (String) — sem dependência
 * da fatia {@code contexto}; grupo por palavra-chave no nome; ordem por {@code id}; a
 * padronização Gundam é um transform de EXIBIÇÃO (não renomeia o contexto).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: nome sem palavra-chave conhecida vira título solto
 * (grupo vazio); {@code id} sem ordem cadastrada vai para o fim do grupo
 * ({@link Integer#MAX_VALUE}); nome/id nulos são tratados como vazios.
 */
@Component
public class CatalogoObras {

    /** Franquias reconhecidas por palavra-chave no nome, na ordem de verificação. */
    private static final List<Map.Entry<String, String>> GRUPOS_POR_PALAVRA = List.of(
        // "Gundam SEED" ANTES de "Gundam": a Cosmic Era (SEED) tem submenu próprio.
        // Todos os títulos SEED contêm a substring "Gundam SEED" no nome de exibição.
        Map.entry("Gundam SEED", "Gundam SEED"),
        Map.entry("Gundam", "Gundam"),
        Map.entry("Macross", "Macross"),
        Map.entry("Evangelion", "Evangelion"),
        Map.entry("DanMachi", "DanMachi"),
        Map.entry("Sidonia", "Knights of Sidonia"),
        // Break Blade (título comum JP) e Broken Blade (título EN dos 6 filmes) — mesma franquia.
        Map.entry("Break Blade", "Break Blade"),
        Map.entry("Broken Blade", "Break Blade")
    );

    /**
     * Rótulo de exibição com o ANO da era (UC/CE/RC) na frente, para o select sair em
     * ordem cronológica legível. UC = Universal Century; C.E. = Cosmic Era (SEED, submenu
     * próprio); RC = Regild Century (Reconguista, fora da UC). Os ids IGLOO/Thunderbolt
     * são o contrato de nome para quando a lore deles for criada.
     */
    private static final Map<String, String> NOME_GUNDAM_PADRONIZADO = Map.ofEntries(
        // Universal Century (UC) — grupo "Gundam"
        Map.entry("gundam_origin", "UC 0068 - Mobile Suit Gundam: The Origin"),
        Map.entry("gundam_0079", "UC 0079 - Mobile Suit Gundam (0079)"),
        Map.entry("gundam_08ms", "UC 0079 - Mobile Suit Gundam: The 08th MS Team"),
        Map.entry("gundam_ms_igloo", "UC 0079 - Mobile Suit Gundam MS IGLOO"),
        Map.entry("gundam_thunderbolt", "UC 0079 - Mobile Suit Gundam Thunderbolt"),
        Map.entry("gundam_0080", "UC 0079 - Mobile Suit Gundam 0080: War in the Pocket"),
        Map.entry("gundam_0083", "UC 0083 - Mobile Suit Gundam 0083: Stardust Memory"),
        Map.entry("gundam_zeta", "UC 0087 - Mobile Suit Zeta Gundam"),
        Map.entry("gundam_zz", "UC 0088 - Mobile Suit Gundam ZZ"),
        Map.entry("gundam_cca", "UC 0093 - Mobile Suit Gundam: Char's Counterattack"),
        Map.entry("gundam_unicorn", "UC 0096 - Mobile Suit Gundam Unicorn"),
        Map.entry("gundam_nt", "UC 0097 - Mobile Suit Gundam Narrative"),
        Map.entry("gundam_hathaway", "UC 0105 - Mobile Suit Gundam Hathaway"),
        Map.entry("gundam_f91", "UC 0123 - Mobile Suit Gundam F91"),
        Map.entry("gundam_victory", "UC 0153 - Mobile Suit Victory Gundam"),
        Map.entry("gundam_greco", "RC 1014 - Mobile Suit Gundam: Reconguista in G"),
        // Cosmic Era (CE) — grupo "Gundam SEED"
        Map.entry("gundam_seed", "C.E. 71 - Mobile Suit Gundam SEED"),
        Map.entry("gundam_seed_astray", "C.E. 71 - Mobile Suit Gundam SEED MSV Astray (Side Story)"),
        Map.entry("gundam_seed_destiny", "C.E. 73 - Mobile Suit Gundam SEED Destiny"),
        Map.entry("gundam_seed_stargazer", "C.E. 73 - Mobile Suit Gundam SEED C.E. 73: Stargazer"),
        Map.entry("gundam_seed_freedom", "C.E. 75 - Mobile Suit Gundam SEED Freedom")
    );

    /** Ordem cronológica (release) dentro de cada grupo — id na sequência final desejada. */
    private static final List<String> ORDEM_CRONOLOGICA = List.of(
        // Gundam — Universal Century (UC), ordem cronológica; Reconguista (RC) ao fim.
        // gundam_ms_igloo/gundam_thunderbolt: slots reservados (UC 0079), lore pendente.
        "gundam_origin", "gundam_0079", "gundam_08ms",
        "gundam_ms_igloo", "gundam_thunderbolt",
        "gundam_0080", "gundam_0083", "gundam_zeta", "gundam_zz", "gundam_cca",
        "gundam_unicorn", "gundam_nt", "gundam_hathaway", "gundam_f91", "gundam_victory",
        "gundam_greco",
        // Gundam SEED — Cosmic Era (CE), submenu próprio
        "gundam_seed", "gundam_seed_astray", "gundam_seed_destiny",
        "gundam_seed_stargazer", "gundam_seed_freedom",
        // DanMachi
        "danmachi", "danmachi_s1", "danmachi_so", "danmachi_s2", "danmachi_movie",
        "danmachi_s3", "danmachi_s4", "danmachi_s5",
        // Evangelion
        "evangelion_tv", "evangelion_111", "evangelion_222", "evangelion_333", "evangelion_3010",
        // Knights of Sidonia
        "sidonia", "sidonia_movie",
        // Macross
        "macross_anime", "macross_filme1", "macross_dyrl", "macross_filme2", "macross_2",
        "macross_plus", "macross_7", "macross_7_encore", "macross_7_filme", "macross_dynamite_7",
        "macross_7_filmes", "macross_zero", "macross_frontier", "macross_frontier_filme1",
        "macross_frontier_filme2", "macross_frontier_filmes", "macross_delta",
        "macross_delta_filme1", "macross_delta_filme2", "macross_delta_filmes",
        // Break Blade (Broken Blade) — 6 filmes na ordem de lançamento.
        // Contrato: ids break_blade_1..6; nome contendo "Break Blade"
        // (ex.: "Break Blade - Filme 1 - O Tempo do Despertar").
        "break_blade_1", "break_blade_2", "break_blade_3",
        "break_blade_4", "break_blade_5", "break_blade_6",
        // Títulos solos (ordem interna irrelevante — cada um é uma opção única)
        "eight_six", "guilty_crown"
    );

    private static final Map<String, Integer> ORDEM_POR_ID = new HashMap<>();
    static {
        for (int i = 0; i < ORDEM_CRONOLOGICA.size(); i++) {
            ORDEM_POR_ID.put(ORDEM_CRONOLOGICA.get(i), i);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a franquia da obra, para virar o rótulo do {@code <optgroup>}.
     * <p>INVARIANTES DO DOMÍNIO: primeira palavra-chave encontrada no nome vence; obras sem
     * franquia conhecida (86, Guilty Crown) devolvem string vazia (viram opção solta).
     * <p>COMPORTAMENTO EM CASO DE FALHA: nome nulo/vazio devolve "".
     */
    public String grupo(String nome) {
        if (nome == null || nome.isBlank()) {
            return "";
        }
        for (Map.Entry<String, String> par : GRUPOS_POR_PALAVRA) {
            if (nome.contains(par.getKey())) {
                return par.getValue();
            }
        }
        return "";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o rótulo de exibição — para as obras Gundam, prefixa o ano da
     * era ("UC 0087 - ...", "C.E. 71 - ...", "RC 1014 - ...") para o select sair legível em
     * ordem cronológica; demais obras mantêm o nome original.
     * <p>INVARIANTES DO DOMÍNIO: transform apenas visual; não altera o contexto nem o id.
     * <p>COMPORTAMENTO EM CASO DE FALHA: id sem padronização devolve {@code nomeOriginal}.
     */
    public String nomePadronizado(String id, String nomeOriginal) {
        if (id == null) {
            return nomeOriginal;
        }
        return NOME_GUNDAM_PADRONIZADO.getOrDefault(id, nomeOriginal);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: posição cronológica da obra dentro do grupo, para ordenar as
     * opções do {@code <optgroup>}.
     * <p>INVARIANTES DO DOMÍNIO: menor valor aparece antes; ids na tabela seguem a ordem
     * de lançamento revisada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: id nulo ou não cadastrado vai para o fim
     * ({@link Integer#MAX_VALUE}).
     */
    public int ordem(String id) {
        if (id == null) {
            return Integer.MAX_VALUE;
        }
        return ORDEM_POR_ID.getOrDefault(id, Integer.MAX_VALUE);
    }
}
