package org.traducao.projeto.contexto.lore.eightsix;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Map;
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
     * PROPÓSITO DE NEGÓCIO: complementa a identidade canônica desta obra, que o id
     * ({@code eight_six}) e o nome de exibição ({@code "86 (Eighty-Six)"}) não cobrem: as
     * pastas reais desta árvore se chamam {@code "86 Part 1"} e {@code "86 Part 2"} — o título
     * É o número, sem nenhuma palavra em volta.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code "86"} é IDENTIDADE, não ruído de release, e por isso
     * precisa mesmo estar aqui. Declará-lo é seguro porque o casamento é por palavra INTEIRA:
     * {@code "86"} não casa dentro de {@code "(1986)"} — o ano da pasta de Gundam ZZ —, nem
     * dentro de {@code "x265"} ou {@code "1080p"}. É exatamente esse par mínimo que o teste de
     * catálogo mantém preso.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável; sem I/O.
     */
    @Override
    public Set<String> apelidosPasta() {
        return Set.of("86", "Eighty-Six");
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

    /**
     * PROPÓSITO DE NEGÓCIO: reforço determinístico de terminologia — formas-ruim que o LLM
     * produziu ao traduzir termos de mundo que a lore manda manter em inglês (medidas nas
     * duas temporadas de 86). Restauradas SÓ quando o original contém o termo canônico.
     * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim em PT; valor = grafia canônica oficial.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        // MEDIDO no cache do 86 em 2026-08-05, na PRIMEIRA traducao completa da obra pelo KRONOS
        // (23 episodios, 7.255 falas). As cinco entradas originais cobriam parte do problema; o
        // modelo produziu outras formas que passaram inteiras. Cada linha abaixo tem a contagem
        // real ao lado — nenhuma entrou por suposicao.
        //
        // Map.ofEntries e nao Map.of: este ultimo aceita no maximo 10 pares.
        return Map.ofEntries(
            // LEGION — "Legiao" SEM cedilha era o furo: o mapa so tinha a forma acentuada, e o
            // modelo escreve as duas. Mesma familia do plural, que tambem faltava.
            Map.entry("Legião", "Legion"),          // ja existia; 0 restantes
            Map.entry("Legiao", "Legion"),          // 7
            Map.entry("Legiões", "Legions"),        // 1
            Map.entry("Handler Um", "Handler One"),

            // PROCESSOR — o singular ja estava mapeado e ainda restaram 14, porque nas falas em
            // que o INGLES diz "Processors" a checagem do canonico singular nao casa (a fronteira
            // a direita barra o "s"). E a mesma lacuna que "Newtypes" teve no nucleo UC.
            Map.entry("Processador", "Processor"),  // ja existia
            Map.entry("Processadores", "Processors"), // 12

            // UNDERTAKER — codinome do Shin. O mapa cobria "Cavaleiro da Morte" e "Coveiro", que
            // NUNCA ocorreram; o modelo inventou outras duas. Sao palavras comuns em PT, e por
            // isso so a condicao salva: a restauracao exige "Undertaker" no ingles, entao um
            // carrasco de verdade ou um clima funebre jamais sao tocados.
            Map.entry("Cavaleiro da Morte", "Undertaker"), // ja existia; 0 ocorrencias
            Map.entry("Coveiro", "Undertaker"),            // ja existia; 0 ocorrencias
            Map.entry("Fúnebre", "Undertaker"),            // 4
            Map.entry("Carrasco", "Undertaker"),           // 1

            // EIGHTY-SIX — o maior volume, e o nome que da titulo a obra. 67 falas chamavam o
            // grupo de "Oitenta e Seis".
            Map.entry("Oitenta e Seis", "Eighty-Six"),     // 67

            // SPEARHEAD — esquadrao. "Esquadrao Lanca-Flanco" vira "Esquadrao Spearhead".
            Map.entry("Lança-Flanco", "Spearhead"),        // 5

            // SPEARHEAD, segunda leva — MEDIDO na retraducao completa de 2026-08-15 (23
            // episodios, aya-expanse-8b): das 27 ocorrencias de "Spearhead" no ingles, 23
            // sobreviveram e 4 nao. "Lanca-Flanco" nao apareceu nenhuma vez nesta rodada; o
            // modelo inventou tres formas NOVAS, e duas delas nem sao palavras:
            //   "Officially called the Spearhead Squadron."   -> "...como Esquadroe de Ponta."
            //   "This is the captain of the Spearhead Squadron." -> "...do Esquadroa de Ponta."
            //   "Spearhead."                                  -> "Espada-Faca."
            // Mapear forma errada uma a uma e jogo de gato e rato — na proxima rodada vem uma
            // quarta. A alternativa, restaurar todo nome proprio automaticamente, JA foi
            // tentada e removida deste projeto por gerar 323 falso positivo em 560 pendencias
            // (57,7%); enquanto isso nao mudar, a entrada medida e o mecanismo disponivel.
            Map.entry("Esquadroe de Ponta", "Spearhead"),  // 1
            Map.entry("Esquadroa de Ponta", "Spearhead"),  // 1
            Map.entry("Espada-Faca", "Spearhead"),         // 1

            // FASE 1 DA FONTE ÚNICA DE LORE (2026-08-15) — as quatro entradas abaixo já existiam
            // no catálogo da REVISÃO de lore desta mesma obra e a tradução não as tinha. Quem
            // traduz não enxergava o que quem revisa aprendeu, e é a tradução que escreve o .ass.
            //
            // O EFEITO NO ACERVO DE HOJE É ZERO, e isso está MEDIDO, não suposto
            // (MedicaoEfeitoDaUniaoDeLoreIT): nenhuma das quatro ocorre nas 16.120 falas já
            // traduzidas do 86. O ganho é nas traduções FUTURAS — dizer o contrário seria
            // prometer ganho que a medição não mostra.
            //
            // "Canela" é o caso que exigiu olhar antes de confiar: é homógrafo da parte do corpo
            // em português. Ela é segura pelo MECANISMO, não por sorte — contarCanonico usa
            // flags=0 para canônico de uma palavra, então só dispara com "Shin" MAIÚSCULO no
            // inglês; a canela do corpo é "shin" minúsculo e nunca conta. É a mesma separação que
            // SpearheadMinusculoContinuaTraduzidoTest congela para "spearhead".
            Map.entry("Canela", "Shin"),
            Map.entry("Jugernaut", "Juggernaut"),
            Map.entry("Para RAID", "Para-RAID"),
            Map.entry("Para Raid", "Para-RAID")

            // FICA DE FORA: "mecha" (3 ocorrencias), usado no lugar de "Juggernaut". E palavra
            // comum do genero e aceitavel por contexto — mapea-la reescreveria fala legitima,
            // que e o dano que este mapa existe para evitar. Mesma regua de "unidade movel" no
            // nucleo UC. "M1A4" tambem fica: e a designacao oficial do Juggernaut.
            //
            // FICA DE FORA TAMBEM: "ponta de lanca". A quarta ocorrencia perdida na medicao de
            // 15/08 foi o ep 06 da Part 2 — ingles "A spearhead.", MINUSCULO, traduzido como "A
            // ponta de lanca.". Ali a palavra e a arma, nao o esquadrao, e a traducao esta
            // CERTA: mapea-la trocaria uma linha boa por uma errada, exatamente o oposto do que
            // se quer. O mecanismo ja separa os dois sozinho — contarCanonico usa flags=0 para
            // termo de uma palavra, entao "Spearhead" nunca casa com "spearhead" minusculo e a
            // entrada nem chega a disparar naquela fala. A distincao esta congelada em
            // SpearheadMinusculoContinuaTraduzidoTest.
        );
    }
}
