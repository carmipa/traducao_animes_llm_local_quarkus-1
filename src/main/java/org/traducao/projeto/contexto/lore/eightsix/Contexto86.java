package org.traducao.projeto.contexto.lore.eightsix;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore de 86 — Eighty-Six (segregação estatal, guerra psicológica).
 *
 * <p>INVARIANTES DO DOMÍNIO: Shin ≠ canela; Alba/Colorata/Pig; Handler/Processor;
 * Para-RAID; Legion e unidades em latim/alemão.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class Contexto86 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: 86 - Eighty-Six (ambas as temporadas / Part 1 e Part 2).
        - Densidade: literatura de guerra psicológica e preconceito estatal institucionalizado.
          A Republica de San Magnolia mente que luta com "drones"; na verdade envia humanos do Distrito 86.

        === Segregacao (NUNCA suavizar) ===
        - Eighty-Six / 86: cidadaos desumanizados do Distrito 86. Nao traduzir como "oitenta e seis"
          salvo fala explicitamente numerica.
        - Alba: elite de pleno direito (cabelo e olhos prateados).
        - Colorata: rotulo pejorativo estatal para nao-Alba; justifica a propaganda dos "drones".
        - Colorata Pig / Pig / "porcos coloridos": violencia verbal institucional. Nao eufemizar.
          Se o original usa Pig/Colorata Pig, preserve a crueza equivalente em PT-BR.

        === Engrenagem de guerra ===
        - Juggernaut (ex.: M1A4 Juggernaut): mecha dos 86. A Republica chama de drone nao tripulado.
        - Processor: piloto 86 tratado pelo Estado como peca de hardware descartavel.
          Nao reduzir a "operador" generico quando for o termo oficial interno.
        - Handler: oficial Alba que comanda Processors a distancia (ex.: Lena = Handler One).
          Manter Handler; nao so "operador de radio".
        - Para-RAID: dispositivo de sincronizacao neural/sensorial Handler↔Processor. Nao traduzir.

        === Inimigo: Legion ===
        - Legion: IA autonoma inimiga. Manter "Legion".
        - Unidades (latim/alemao — NUNCA traduzir nomes): Scavenger, Ameise, Lowe/Löwe, Dinosauria,
          Morpho, e demais designacoes oficiais da obra.
        - Feldress / Reginleif: mechas do lado Giad/Federacao quando aparecerem; manter nomes.

        === Pessoas e unidades ===
        - Nomes: Shinei "Shin" Nouzen, Vladilena "Lena" Milize, Raiden Shuga, Anju Emma,
          Theoto Rikka, Kurena Kukumila, Frederica Rosenfort, Ernst Zimmerman, Eugene Rantz.
        - PROTECAO CRITICA: "Shin" e SEMPRE apelido de Shinei Nouzen. Nunca "canela"
          (Shin!, Shin?, Shin... inclusive).
        - Codinomes: Undertaker (Shin); Bloodstained Queen (Lena) quando aparecer.
        - Esquadroes: Spearhead Squadron, Nordlicht Squadron.
        - Faccoes: Republica de San Magnolia, Imperio/Federacao de Giad.

        === Regras de traducao ===
        - dud rounds = municao falha / projeteis falhos (nao "rodadas aleatorias").
        - Nao suavizar racismo institucional, trauma ou desumanizacao.
        - Tom: militar, contido; Shin seco; Lena formal/idealista; Spearhead com ironia amarga.
        """;

    private static final String PROMPT = ContextoPrompt.montar("86 - Eighty-Six", LORE);

    @Override
    public String getId() {
        return "eight_six";
    }

    @Override
    public String getNomeExibicao() {
        return "86 (Eighty-Six)";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege léxico de segregação e guerra de 86.
     * <p>INVARIANTES DO DOMÍNIO: Shin e Colorata/Pig nunca viram tradução literal destrutiva.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Shin", "Shinei Nouzen", "Vladilena Milize", "Lena", "Raiden Shuga",
            "Anju Emma", "Theoto Rikka", "Kurena Kukumila", "Frederica Rosenfort",
            "Eighty-Six", "Handler", "Processor", "Para-RAID", "Legion", "Juggernaut",
            "Spearhead", "Alba", "Colorata", "Ameise", "Dinosauria", "Morpho",
            "Feldress", "Reginleif", "Undertaker"
        );
    }
}
