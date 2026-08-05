package org.traducao.projeto.revisaoLore.contexto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: núcleo UC na fatia revisaoLore (espelho de
 * CorrecoesTerminologiaGundamUc — sem import cruzado da fatia contexto).
 *
 * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim PT; valor = canônico; extras sobrescrevem.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapas imutáveis; sem I/O.
 */
public final class CorrecoesTerminologiaGundamUcRevisao {

    private static final Map<String, String> NUCLEO = Map.ofEntries(
        Map.entry("Novo Tipo", "Newtype"),
        Map.entry("Neotipo", "Newtype"),
        Map.entry("Velho Tipo", "Oldtype"),
        Map.entry("Traje Móvel", "Mobile Suit"),
        Map.entry("Robô Móvel", "Mobile Suit"),
        Map.entry("Terno Móvel", "Mobile Suit"),
        Map.entry("Armadura Móvel", "Mobile Armor"),
        Map.entry("Móvel Suit", "Mobile Suit"),
        Map.entry("Suit Móvel", "Mobile Suit"),
        Map.entry("Móveis Suits", "Mobile Suits"),
        Map.entry("Suits Móveis", "Mobile Suits"),
        Map.entry("Trajes Móveis", "Mobile Suits"),
        Map.entry("Robôs Móveis", "Mobile Suits"),
        Map.entry("Ternos Móveis", "Mobile Suits"),
        Map.entry("Armaduras Móveis", "Mobile Armors"),
        Map.entry("Sabre de Raio", "Beam Saber"),
        Map.entry("Sabre de Feixe", "Beam Saber"),
        Map.entry("Fuzil de Feixe", "Beam Rifle"),
        Map.entry("Rifle de Feixe", "Beam Rifle"),
        // "Partículas Minovsky" foi REMOVIDA em 2026-07-27 por decisão do dono do acervo: a forma
        // em português é aceitável e forçar o inglês corromperia tradução legítima. Mesma régua de
        // "rifle de feixe" e "unidade móvel".
        Map.entry("Espacenóide", "Spacenoid"),
        Map.entry("Espacenoide", "Spacenoid"),
        Map.entry("Terranóide", "Earthnoid"),
        Map.entry("Terranoide", "Earthnoid"),
        // As 23 abaixo vieram do catálogo da TRADUÇÃO, onde já existiam. Este espelho é
        // deliberadamente uma cópia (fatia não importa fatia), mas ele derivou: as decisões do
        // dono do acervo sobre "terno", Beam Saber e Normal Suit foram registradas só de um lado
        // e a revisão de lore passou semanas sem elas. Duplicação consciente exige quem acuse a
        // divergência — ver ParidadeMapasTerminologiaTest.
        Map.entry("Novos Tipos", "Newtypes"),
        Map.entry("Neotipos", "Newtypes"),
        Map.entry("Velhos Tipos", "Oldtypes"),
        // "terno" é roupa social: JAMAIS serve para Mobile Suit, em nenhuma combinação.
        Map.entry("Terno de Combate", "Mobile Suit"),
        Map.entry("Ternos de Combate", "Mobile Suits"),
        Map.entry("Espada de Raio", "Beam Saber"),
        Map.entry("Espadas de Raio", "Beam Sabers"),
        Map.entry("Sabre de Luz", "Beam Saber"),
        Map.entry("Sabres de Luz", "Beam Sabers"),
        Map.entry("Lâmina de Energia", "Beam Saber"),
        Map.entry("Lâminas de Energia", "Beam Sabers"),
        Map.entry("Terno Normal", "Normal Suit"),
        Map.entry("Ternos Normais", "Normal Suits"),
        Map.entry("Traje Normal", "Normal Suit"),
        Map.entry("Trajes Normais", "Normal Suits"),
        Map.entry("Uniforme Normal", "Normal Suit"),
        Map.entry("Uniformes Normais", "Normal Suits"),
        Map.entry("Armadura Normal", "Normal Suit"),
        Map.entry("Armaduras Normais", "Normal Suits"),
        Map.entry("Roupa Normal", "Normal Suit"),
        Map.entry("Roupas Normais", "Normal Suits"),
        Map.entry("Terno Espacial Potenciado", "Powered Spacesuit"),
        Map.entry("Traje Espacial Potenciado", "Powered Spacesuit"),
        // ESPELHO das quatro mineradas no acervo em 2026-08-04 e aprovadas pelo dono do acervo.
        // A fala cujo texto visivel INTEIRO e "Mobile Suit" aparece 149x (ZZ 95, Zeta 54) e a
        // maioria sao CARTOES DE TITULO, com o ingles em caixa alta: "MOVEL DE ASSALTO" 74x,
        // "Movel de Assento" 31x, "MOVEL DE GUERRA" 2x, "Mobil Suit" 2x. Colisao medida: ZERO.
        //
        // "Movel de Combate" (8x) NAO entra dos dois lados: e a familia que o dono do acervo ja
        // isentou por ser aceitavel por contexto, junto de "unidade movel". "MS" tambem nao: e
        // abreviacao oficial.
        //
        // Entram AQUI no mesmo commit do outro catalogo de proposito. A nota acima registra que
        // este espelho ja derivou uma vez por decisao entrar de um lado so; a catraca de paridade
        // pegou esta em 04/08 antes do commit, que e exatamente para o que ela existe.
        Map.entry("Móvel de Assalto", "Mobile Suit"),
        Map.entry("Móvel de Assento", "Mobile Suit"),
        Map.entry("Móvel de Guerra", "Mobile Suit"),
        Map.entry("Mobil Suit", "Mobile Suit")
    );

    private CorrecoesTerminologiaGundamUcRevisao() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo UC para Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: mapa imutável.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa.
     */
    public static Map<String, String> mapa() {
        return NUCLEO;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo UC + extras da obra na Revisao.
     *
     * <p>INVARIANTES DO DOMÍNIO: extras sobrescrevem chaves repetidas; LinkedHashMap.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: extras vazios devolvem o núcleo.
     */
    public static Map<String, String> comExtras(Map<String, String> extras) {
        Map<String, String> combinado = new LinkedHashMap<>(NUCLEO);
        if (extras != null) {
            combinado.putAll(extras);
        }
        return Collections.unmodifiableMap(combinado);
    }
}
