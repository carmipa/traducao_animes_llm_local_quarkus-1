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
        Map.entry("Traje Espacial Potenciado", "Powered Spacesuit")
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
