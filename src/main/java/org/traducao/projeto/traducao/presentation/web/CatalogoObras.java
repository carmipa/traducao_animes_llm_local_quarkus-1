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
        Map.entry("Gundam", "Gundam"),
        Map.entry("Macross", "Macross"),
        Map.entry("Evangelion", "Evangelion"),
        Map.entry("DanMachi", "DanMachi"),
        Map.entry("Sidonia", "Knights of Sidonia")
    );

    /** Rótulo padronizado "Mobile Suit Gundam - ‹subtítulo›" por id de contexto Gundam. */
    private static final Map<String, String> NOME_GUNDAM_PADRONIZADO = Map.ofEntries(
        Map.entry("gundam_0079", "Mobile Suit Gundam (0079)"),
        Map.entry("gundam_zeta", "Mobile Suit Gundam - Zeta"),
        Map.entry("gundam_zz", "Mobile Suit Gundam - ZZ (Double Zeta)"),
        Map.entry("gundam_cca", "Mobile Suit Gundam - Char's Counterattack"),
        Map.entry("gundam_0080", "Mobile Suit Gundam - 0080: War in the Pocket"),
        Map.entry("gundam_0083", "Mobile Suit Gundam - 0083: Stardust Memory"),
        Map.entry("gundam_f91", "Mobile Suit Gundam - F91"),
        Map.entry("gundam_victory", "Mobile Suit Gundam - Victory"),
        Map.entry("gundam_08ms", "Mobile Suit Gundam - The 08th MS Team"),
        Map.entry("gundam_seed", "Mobile Suit Gundam - SEED"),
        Map.entry("gundam_seed_destiny", "Mobile Suit Gundam - SEED Destiny"),
        Map.entry("gundam_seed_stargazer", "Mobile Suit Gundam - SEED C.E. 73: Stargazer"),
        Map.entry("gundam_seed_astray", "Mobile Suit Gundam - SEED MSV Astray (Mangá/Side Story)"),
        Map.entry("gundam_unicorn", "Mobile Suit Gundam - Unicorn"),
        Map.entry("gundam_greco", "Mobile Suit Gundam - Reconguista in G"),
        Map.entry("gundam_origin", "Mobile Suit Gundam - The Origin"),
        Map.entry("gundam_nt", "Mobile Suit Gundam - NT (Narrative)"),
        Map.entry("gundam_hathaway", "Mobile Suit Gundam - Hathaway"),
        Map.entry("gundam_seed_freedom", "Mobile Suit Gundam - SEED Freedom")
    );

    /** Ordem cronológica (release) dentro de cada grupo — id na sequência final desejada. */
    private static final List<String> ORDEM_CRONOLOGICA = List.of(
        // Gundam
        "gundam_0079", "gundam_zeta", "gundam_zz", "gundam_cca", "gundam_0080",
        "gundam_0083", "gundam_f91", "gundam_victory", "gundam_08ms", "gundam_seed",
        "gundam_seed_destiny", "gundam_seed_stargazer", "gundam_seed_astray", "gundam_unicorn",
        "gundam_greco", "gundam_origin", "gundam_nt", "gundam_hathaway", "gundam_seed_freedom",
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
     * PROPÓSITO DE NEGÓCIO: o rótulo de exibição — padroniza o grupo Gundam para
     * "Mobile Suit Gundam - ‹subtítulo›"; demais obras mantêm o nome original.
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
