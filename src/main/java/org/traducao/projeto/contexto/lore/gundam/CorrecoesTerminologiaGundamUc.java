package org.traducao.projeto.contexto.lore.gundam;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa canônico COMPARTILHADO das obras Gundam da Universal Century —
 * formas-ruim (PT) → grafia oficial (EN) que o LLM tende a localizar indevidamente (Newtype,
 * Mobile Suit, Beam Saber). Obras com termos próprios (ex.: Axis em Char's Counterattack,
 * Sleeves em Unicorn/NT) combinam este núcleo com seus extras via {@link #comExtras(Map)}.
 *
 * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim em PT; valor = canônico EN/oficial; o enforcer
 * só aplica quando o original EN contém o canônico na grafia exata (ex.: só troca "Eixo"→"Axis"
 * se a fala tinha "Axis"), nunca tocando a palavra comum fora de contexto.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapas imutáveis; sem I/O.
 */
public final class CorrecoesTerminologiaGundamUc {

    private static final Map<String, String> NUCLEO = Map.ofEntries(
        Map.entry("Novo Tipo", "Newtype"),
        Map.entry("Neotipo", "Newtype"),
        // PLURAL: o canônico "Newtype" é palavra ÚNICA e a checagem exige fronteira à direita,
        // então "Newtypes" no inglês NÃO casa com ele e a restauração nunca dispara. Medido no
        // run de ZZ: "Is this a meeting of fellow Newtypes?" saiu como "uma reunião de novos
        // tipos". Mesma lacuna que "Beam Sabers" tinha; as famílias Mobile Suit e Mobile Armor
        // já traziam singular e plural desde o início.
        Map.entry("Novos Tipos", "Newtypes"),
        Map.entry("Neotipos", "Newtypes"),
        Map.entry("Velho Tipo", "Oldtype"),
        Map.entry("Velhos Tipos", "Oldtypes"),
        Map.entry("Traje Móvel", "Mobile Suit"),
        Map.entry("Robô Móvel", "Mobile Suit"),
        Map.entry("Terno Móvel", "Mobile Suit"),
        Map.entry("Armadura Móvel", "Mobile Armor"),
        // Compostos MEIO-traduzidos (o LLM localiza "Mobile"->"Móvel"/"Móveis" mas mantém "Suit"),
        // e os plurais das formas-ruim acima — o canônico plural "Mobile Suits" casa o EN
        // "Mobile suits" (checagem multi-palavra é case-insensitive no EnforcadorTermosLore).
        Map.entry("Móvel Suit", "Mobile Suit"),
        Map.entry("Suit Móvel", "Mobile Suit"),
        Map.entry("Móveis Suits", "Mobile Suits"),
        Map.entry("Suits Móveis", "Mobile Suits"),
        Map.entry("Trajes Móveis", "Mobile Suits"),
        Map.entry("Robôs Móveis", "Mobile Suits"),
        Map.entry("Ternos Móveis", "Mobile Suits"),
        // "terno" é roupa social: JAMAIS serve para Mobile Suit, em nenhuma combinação — decisão
        // do dono do acervo. "Terno Móvel" já estava coberto; falta a variante sem o adjetivo.
        Map.entry("Terno de Combate", "Mobile Suit"),
        Map.entry("Ternos de Combate", "Mobile Suits"),
        Map.entry("Armaduras Móveis", "Mobile Armors"),
        Map.entry("Sabre de Raio", "Beam Saber"),
        Map.entry("Sabre de Feixe", "Beam Saber"),
        Map.entry("Fuzil de Feixe", "Beam Rifle"),
        Map.entry("Rifle de Feixe", "Beam Rifle"),
        // Variantes de "Beam Saber" MEDIDAS no run completo de Gundam ZZ (47 episódios, 16.716
        // pares), não imaginadas: o termo sobreviveu em 0% das 7 ocorrências e NENHUMA destas
        // formas estava aqui, por isso passaram inteiras.
        //
        // "sabre de luz" é terminologia de OUTRA franquia e apareceu 3 vezes; as demais são
        // localizações plausíveis mas fora do glossário UC.
        //
        // Decisão do dono do acervo sobre "Mobile Suit", registrada porque a fronteira aqui é
        // fina e não se deduz do código:
        //   - o canônico é "Mobile Suit" e as entradas acima que o restauram FICAM, incluindo
        //     "Traje Móvel" e "Terno Móvel";
        //   - mas "móvel de combate", "unidade móvel" e "unidades móveis" são aceitáveis por
        //     contexto e por isso NÃO entram no mapa. Foram 101 falas do run de ZZ que este
        //     enforcer deixará em paz de propósito — reescrevê-las seria corromper tradução
        //     legítima, que é o dano que ele existe para evitar.
        // Mesma lógica para "Beam Rifle": "rifle de feixe" e "arma de feixe" passam.
        Map.entry("Espada de Raio", "Beam Saber"),
        Map.entry("Espadas de Raio", "Beam Sabers"),
        Map.entry("Sabre de Luz", "Beam Saber"),
        Map.entry("Sabres de Luz", "Beam Sabers"),
        Map.entry("Lâmina de Energia", "Beam Saber"),
        Map.entry("Lâminas de Energia", "Beam Sabers"),
        // NORMAL SUIT / POWERED SUIT — o traje pressurizado do piloto.
        // Medido no run completo de ZZ: 23 ocorrências de "normal suit" no inglês e ZERO
        // preservadas. O termo não estava na lore, nem no termosProtegidos, nem aqui: nada o
        // protegia, e o modelo produziu NOVE renderizações diferentes para a mesma coisa
        // ("terno normal", "traje normal", "uniforme normal", "armadura normal", "roupa
        // normal"...). O custo não é só terminológico: a mesma peça muda de nome entre
        // episódios e o espectador perde o referente.
        //
        // As entradas abaixo cobrem as variantes LEXICAIS, que é o que restauração determinística
        // sabe fazer. Onde o modelo troca o traje por outro substantivo do universo, o mapa não
        // alcança — isso é leitura de cena, não variante de palavra, e exige revisão humana.
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
        // Cartão de título "POWERED SPACESUIT" do ep01, única ocorrência observada.
        Map.entry("Terno Espacial Potenciado", "Powered Spacesuit"),
        Map.entry("Traje Espacial Potenciado", "Powered Spacesuit"),
        // Vieram do catálogo ESPELHO da revisão de lore, onde já existiam. A duplicação entre os
        // dois catálogos é deliberada (fatia não importa fatia), mas ela derivou nos DOIS sentidos:
        // as variantes de Beam Saber e Normal Suit acima só existiam aqui, e estas seis só lá.
        // Duplicação consciente exige quem acuse a divergência — ver ParidadeMapasTerminologiaTest.
        Map.entry("Espacenóide", "Spacenoid"),
        Map.entry("Espacenoide", "Spacenoid"),
        Map.entry("Terranóide", "Earthnoid"),
        Map.entry("Terranoide", "Earthnoid"),
        // MINERADAS no acervo em 2026-08-04 (61.051 falas): a fala cujo texto visivel INTEIRO e
        // "Mobile Suit" aparece 149 vezes, em ZZ (95) e Zeta (54), e a MAIORIA sao CARTOES DE
        // TITULO -- o ingles vem em caixa alta ("MOBILE SUIT"), texto em tela, nao dialogo. Ali
        // nao ha contexto que salve: e o nome da franquia sendo reescrito.
        //
        //   MOVEL DE ASSALTO  74x     MOVEL DE GUERRA   2x
        //   Movel de Assento  31x     Mobil Suit        2x   (erro de grafia do proprio canonico)
        //
        // COLISAO MEDIDA: ZERO. Nenhuma fala do acervo traz "Mobile Suit" no ingles e uma destas
        // formas em PT sem ser exatamente este defeito -- entao a restauracao so corrige.
        //
        // "Movel de Combate" (8x) FICA DE FORA: e a decisao registrada acima, de que essa familia
        // e aceitavel por contexto. Confirmada pelo dono do acervo em 04/08 ao aprovar estas
        // quatro. "MS" (2x) tambem fica: e abreviacao oficial, nao forma-ruim.
        //
        // GAP DECLARADO: os PLURAIS destas quatro nao foram medidos e por isso nao entram. E a
        // mesma lacuna que "Newtypes" teve (ver acima); se aparecerem, medir antes de adicionar.
        Map.entry("Móvel de Assalto", "Mobile Suit"),
        Map.entry("Móvel de Assento", "Mobile Suit"),
        Map.entry("Móvel de Guerra", "Mobile Suit"),
        Map.entry("Mobil Suit", "Mobile Suit"),
        // "MOVEL DE COMBATE" — a exclusao acima foi REVISTA em 05/08/2026, com o numero na mao.
        //
        // A regra "aceitavel por contexto" foi escrita pensando em DIALOGO, e continua certa la.
        // Mas a mesma forma aparece em CARTAO DE TITULO, onde o ingles vem "MOBILE SUIT" em caixa
        // alta e nao existe contexto que a salve: e o nome da franquia escrito errado na tela.
        //
        // MEDIDO no acervo, e a separacao e TOTAL:
        //     EN-CAIXA-ALTA / PT-Capitalizado ....... 8   (cartao de titulo)
        //     en-normal     / pt-minusculo .......... 1   (dialogo)
        //
        // CUSTO ACEITO, e ele e real: a unica fala de dialogo tem "a combat mobile suit" no
        // ingles, entao a restauracao TAMBEM a alcanca e ela perde o "de combate":
        //     EN "My Geze has the mobility of a combat mobile suit!"
        //     PT "Meu Geze tem a mobilidade de um móvel de combate!"  ->  "...de um Mobile Suit!"
        // Oito cartoes com o nome da franquia errado valem mais que uma fala perdendo um
        // adjetivo. Se a leitura mudar, e esta linha unica que sai.
        //
        // "unidade movel" e "unidades moveis" seguem FORA — aquela parte da decisao nao mudou.
        Map.entry("Móvel de Combate", "Mobile Suit")
        // "Partículas Minovsky" NÃO entra, por decisão do dono do acervo: a forma em português é
        // aceitável e forçar o inglês corromperia tradução legítima. Mesma régua de "rifle de
        // feixe", "arma de feixe" e "unidade móvel". O espelho da revisão também a perdeu, para
        // os dois catálogos seguirem dizendo a mesma coisa.
    );

    private CorrecoesTerminologiaGundamUc() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o núcleo comum de terminologia UC, sem extras da obra.
     * <p>INVARIANTES DO DOMÍNIO: mapa imutável; forma-ruim → canônico.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa.
     */
    public static Map<String, String> mapa() {
        return NUCLEO;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o núcleo UC combinado com os termos específicos de uma obra
     * (ex.: {@code Eixo→Axis} em Char's Counterattack).
     * <p>INVARIANTES DO DOMÍNIO: os extras sobrescrevem o núcleo em chave repetida; o resultado
     * é imutável.
     * <p>COMPORTAMENTO EM CASO DE FALHA: extras vazios devolvem o núcleo.
     */
    public static Map<String, String> comExtras(Map<String, String> extras) {
        Map<String, String> combinado = new LinkedHashMap<>(NUCLEO);
        combinado.putAll(extras);
        return Collections.unmodifiableMap(combinado);
    }
}
