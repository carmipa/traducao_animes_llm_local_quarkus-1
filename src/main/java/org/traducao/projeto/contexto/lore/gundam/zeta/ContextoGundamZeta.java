package org.traducao.projeto.contexto.lore.gundam.zeta;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore completa de Mobile Suit Zeta Gundam (UC 0087 / Gryps Conflict)
 * para Tradução Local EN→PT-BR — AEUG, Titans, Axis, Cyber-Newtypes e mecha transformável.
 *
 * <p>INVARIANTES DO DOMÍNIO: Kamille Bidan (masculino); A.E.U.G. / AEUG; Titans (NUNCA Titãs);
 * Quattro Bajeena (NUNCA Quatro); Axis (NUNCA Eixo); Hyaku Shiki (NUNCA Cem Estilos);
 * The O (NUNCA reduzir a O); Newtype oficial.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis; enforcer só restaura
 * com canônico no original EN.
 */
@Component
public class ContextoGundamZeta implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Zeta Gundam (TV) — Universal Century U.C. 0087, Gryps Conflict.
        - Premissa: AEUG vs Titans; Axis Zeon (Haman Karn / Mineva); Cyber-Newtypes;
          Kamille Bidan e o MSZ-006 Zeta Gundam. Tom militar/politico sombrio, trauma,
          abuso de autoridade. Evitar girias modernas.

        === Nucleo UC ===
        - Newtype (NUNCA Novo Tipo); Cyber-Newtype; Oldtype; Psycommu / psycommu;
          Minovsky particles; Spacenoid vs Earthnoid.
        - Mobile Suit vs Mobile Armor; Beam Rifle / Beam Saber; Mega Particle Cannon.
        - Earth Federation / Federation Forces; One Year War (legado).

        === Faccoes (NUNCA fundir / NUNCA mitologizar) ===
        - A.E.U.G. / AEUG (Anti-Earth Union Group) — preservar pontos quando o EN trouxer A.E.U.G.
        - Titans (NUNCA Titãs); Karaba (Terra); Anaheim Electronics.
        - Axis / Axis Zeon (NUNCA Eixo) — Haman Karn como regente de Mineva Lao Zabi.

        === Roster — AEUG / Argama / Karaba ===
        - Kamille Bidan (m — pronomes masculinos; piada de confusao de genero);
          Quattro Bajeena / Char Aznable (m); Bright Noa (m);
          Emma Sheen (f); Fa Yuiry (f); Reccoa Londe (f — defeita aos Titans depois);
          Katz Kobayashi (m); Henken Bekkener (m); Astonaige Medoz (m);
          Apolly Bay (m); Roberto (m); Torres (m); Wong Lee (m).
        - Amuro Ray (m); Hayato Kobayashi (m); Mirai Yashima (f); Hathaway Noa (m, crianca);
          Franklin Bidan (m); Hilda Bidan (f); Beltorchika Irma (f) quando aparecer.

        === Roster — Titans / Scirocco ===
        - Jerid Messa (m); Bask Om (m); Jamitov Hymen (m); Jamaican Daninghan (m);
          Paptimus Scirocco (m); Yazan Gable (m); Buran Blutarch (m);
          Lila Milla Rira (f); Mouar Pharaoh (f); Sarah Zabiarov (f);
          Kacricon Cacooler (m); Gates Capa (m) quando aparecerem.

        === Roster — Cyber-Newtype / Axis ===
        - Four Murasame (f); Rosamia Badam (f); Haman Karn (f); Mineva Lao Zabi (f, crianca).

        === Naves / lugares / eventos ===
        - Naves: Argama; Radish; Alexandria; Audhumla (Karaba); Jupitris; Gwadan; Dogosse Giar quando aparecer.
        - Lugares: Gryps / Gate of Zedan; Jaburo; Hong Kong; Dakar; Kilimanjaro; Axis;
          Shangri-La; Side colonies; Luna II / Von Braun quando o dialogo trouxer.
        - Eventos: Gryps Conflict; Colony 30 Incident (background); Colony Laser; Dakar Speech (Quattro).

        === Mecha ===
        - AEUG/Anaheim: MSZ-006 Zeta Gundam; RX-178 Gundam Mk-II / Super Gundam (G-Defenser);
          MSN-00100 Hyaku Shiki (NUNCA Cem Estilos); Rick Dias; Methuss; Nemo; GM II; Dijeh (Amuro).
        - Titans: Hizack; Marasai; Barzam; Gaplant; Gabthley; Hambrabi; Palace Athene;
          Byarlant; Asshimar; Galbaldy Beta; Messala; The O (NUNCA reduzir a O);
          Psycho Gundam / Psycho Gundam Mk-II.
        - Axis: Qubeley (Haman); Gaza-C quando aparecer.

        === Tres superficies parecidas, tres coisas diferentes ===
        - Original "Four"    -> saida "Four".     Personagem: Four Murasame.
        - Original "Quattro" -> saida "Quattro".  Personagem: Quattro Bajeena.
        - Original "four"    -> saida "quatro".   Numeral.
        - Nao troque o token do original pelo nome de OUTRO personagem.

        === Char e Quattro: a identidade oculta e o eixo da obra ===
        - Original "Char"    -> saida "Char".
        - Original "Quattro" -> saida "Quattro".
        - Nunca "Quattro Aznable" nem "Char Bajeena": esses nomes NAO existem.
        - Char Aznable se apresenta como Quattro Bajeena, e a obra esconde isso DE PROPOSITO.
          Quem pergunta "do you know of a man by the name of Char Aznable?" esta perguntando
          pela identidade OCULTA — trocar por "Quattro Bajeena" entrega o segredo e destroi
          a cena. Escreva o nome que o original escreveu, sempre, sem resolver a identidade.

        === Regras duras ===
        - Titans nao vira Titãs; Axis nao vira Eixo; Hyaku Shiki nao vira Cem Estilos;
          The O nao vira O; Newtype nao vira Novo Tipo.
        - Kamille masculino; Quattro estrategico; Titans autoritarios; Scirocco manipulador;
          Haman fria/regente Axis.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Zeta Gundam", LORE);

    @Override
    public String getId() {
        return "gundam_zeta";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Zeta Gundam";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: complementa a identidade canônica desta obra. A pasta real desta
     * árvore é {@code "[Joseki] Mobile Suit Z Gundam COMPLETE (1985)..."} — grafada com a LETRA
     * {@code Z}, enquanto o id ({@code gundam_zeta}) e o nome de exibição
     * ({@code "Mobile Suit Zeta Gundam"}) usam {@code Zeta} por extenso. Sem estes apelidos, a
     * obra não se reconheceria na própria pasta.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code "Z Gundam"} é seguro apesar de uma das palavras ter uma
     * letra só, porque o casamento é por sequência de palavras INTEIRAS: na pasta de ZZ
     * ({@code "Mobile Suit Gundam ZZ"}) as palavras são {@code gundam} e {@code zz}, e
     * {@code "z gundam"} não aparece ali em nenhuma ordem. É o par mínimo Z × ZZ que o teste de
     * catálogo mantém preso.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável; sem I/O.
     */
    @Override
    public Set<String> apelidosPasta() {
        return Set.of("Z Gundam", "Zeta Gundam", "Mobile Suit Z Gundam");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege elenco Gryps Conflict, facções, naves e mecha canônicos.
     *
     * <p>INVARIANTES DO DOMÍNIO: só artefatos UC 0087 / Zeta; grafias oficiais.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável; sem I/O.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Kamille Bidan", "Quattro Bajeena", "Char Aznable", "Amuro Ray", "Bright Noa",
            "Emma Sheen", "Fa Yuiry", "Reccoa Londe", "Haman Karn", "Mineva Lao Zabi",
            "Paptimus Scirocco", "Jerid Messa", "Four Murasame", "Rosamia Badam",
            "Katz Kobayashi", "Wong Lee", "Henken Bekkener", "Astonaige Medoz",
            "Apolly Bay", "Roberto", "Torres", "Beltorchika Irma",
            "Bask Om", "Jamitov Hymen", "Jamaican Daninghan", "Yazan Gable",
            "Buran Blutarch", "Lila Milla Rira", "Mouar Pharaoh", "Sarah Zabiarov",
            "Kacricon Cacooler", "Gates Capa",
            "Hayato Kobayashi", "Mirai Yashima", "Hathaway Noa",
            "Franklin Bidan", "Hilda Bidan",
            "AEUG", "A.E.U.G.", "Anti-Earth Union Group", "Titans", "Axis", "Sieg Zeon", "Axis Zeon",
            "Karaba", "Anaheim Electronics", "Earth Federation",
            "Zeta Gundam", "Gundam Mk-II", "Super Gundam", "G-Defenser", "Hyaku Shiki",
            "Rick Dias", "Methuss", "Nemo", "GM II", "Dijeh",
            "Hizack", "Marasai", "Barzam", "Gaplant", "Gabthley", "Hambrabi",
            "Palace Athene", "Byarlant", "Asshimar", "Galbaldy Beta", "Messala",
            "Psycho Gundam", "Psycho Gundam Mk-II", "The O", "Qubeley", "Gaza-C",
            "Argama", "Radish", "Audhumla", "Alexandria", "Jupitris", "Gwadan",
            "Gryps", "Gate of Zedan", "Jaburo", "Dakar", "Kilimanjaro", "Shangri-La",
            "Gryps Conflict", "Colony Laser", "Colony 30 Incident", "Dakar Speech",
            "Newtype", "Cyber-Newtype", "Oldtype", "Psycommu", "Minovsky",
            "Spacenoid", "Earthnoid", "Mobile Suit", "Mobile Armor",
            "Beam Rifle", "Beam Saber", "Mega Particle Cannon", "One Year War"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo UC + formas-ruim próprias do Zeta (Titans, Quattro, Axis…).
     *
     * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim PT; valor = canônico; só aplica se o EN
     * contém o canônico (ex.: numeral quatro sem Quattro no EN não é tocado).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; formas ausentes não casam.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUc.comExtras(Map.ofEntries(
            Map.entry("Titãs", "Titans"),
            Map.entry("Titas", "Titans"),
            Map.entry("Quatro", "Quattro"),
            Map.entry("Eixo", "Axis"),
            Map.entry("Cem Estilos", "Hyaku Shiki"),
            Map.entry("O O", "The O"),
            Map.entry("Grupo da União Anti-Terra", "AEUG"),
            Map.entry("Grupo da Uniao Anti-Terra", "AEUG"),
            Map.entry("União Anti-Terra", "AEUG"),
            Map.entry("Uniao Anti-Terra", "AEUG"),
            Map.entry("Conflito de Gryps", "Gryps Conflict"),
            Map.entry("Laser de Colônia", "Colony Laser"),
            Map.entry("Laser de Colonia", "Colony Laser"),
            Map.entry("Psico Gundam", "Psycho Gundam"),
            Map.entry("Cubely", "Qubeley"),
            Map.entry("Qubelei", "Qubeley"),
            Map.entry("Gundam Mark II", "Gundam Mk-II"),
            Map.entry("Gundam Mk II", "Gundam Mk-II"),

            // FA YUIRY — minerado em 2026-08-04. A fala cujo texto INTEIRO e "Fa" aparece 51x
            // no acervo e o Zeta concentra 43 delas; so 8 preservaram o nome. O modelo le "Fa!"
            // como palavra comum: "Fogo!" 27x, "Fala!" 7x, "Fá..." 6x, "Pá!" 1x, "Fale!" 1x.
            // "Fa Yuiry" ja esta em termosProtegidos e nao adiantou — aquele conjunto isenta da
            // checagem de residuo, nao restaura grafia.
            //
            // Seguro por construcao: so dispara com "Fa" no INGLES, e canonico de uma palavra e
            // comparado com SENSIBILIDADE A CAIXA. Colisao medida no acervo: zero legitima (as 6
            // encontradas sao o mesmo defeito em fala maior, e a regra as corrige).
            // Justificativa completa e risco residual em CorrecoesTerminologiaGundamZz.
            Map.entry("Fogo", "Fa"),
            Map.entry("Fala", "Fa"),
            Map.entry("Fá", "Fa"),
            Map.entry("Pá", "Fa"),
            Map.entry("Fale", "Fa"),

            // ---------------------------------------------------------------------------
            // Formas-ruim MEDIDAS nas 16.778 falas do acervo (cache, 2026-07-30).
            // Quinta obra a receber o mapa; a de maior volume absoluto de perdas (433).
            // ---------------------------------------------------------------------------

            // ERRO SEMÂNTICO, não de grafia: 8 de 19. G-Defenser é a unidade de apoio;
            // Super Gundam é o Mk-II JÁ acoplado a ela. São coisas diferentes e ambas
            // existem na obra. Seguro porque ZERO falas trazem as duas no inglês.
            Map.entry("Super Gundam", "G-Defenser"),

            // "Gate of Zedan" perdido em 30 de 30 — é o nome que a A Baoa Qu recebeu.
            Map.entry("Portão de Zedan", "Gate of Zedan"),
            Map.entry("Portao de Zedan", "Gate of Zedan"),

            // 7 de 7. O mapa do 08th já trazia esta entrada; o Zeta não.
            Map.entry("Guerra de Um Ano", "One Year War"),

            // 4 de 6, nas duas formas que apareceram.
            Map.entry("canhão de partículas megas", "Mega Particle Cannon"),
            Map.entry("mega canhão de partículas", "Mega Particle Cannon"),

            // 4 de 4.
            Map.entry("Incidente da Colônia 30", "Colony 30 Incident"),
            Map.entry("Incidente da Colonia 30", "Colony 30 Incident"),

            // 2 de 5 — o mobile suit virou residência.
            Map.entry("Palácio Atena", "Palace Athene"),
            Map.entry("Palacio Atena", "Palace Athene"),

            // 2 de 8 — "Four" é o NOME da personagem (Four Murasame), não o número.
            Map.entry("Quatro Murasame", "Four Murasame")

            // NÃO entram, e a medição é a razão:
            //   A.E.U.G. (178 de 211) -- o PT escreve "AEUG" sem pontos, de forma
            //     CONSISTENTE nas 178. É formatação, não erro de sentido, e mudá-la
            //     reescreveria 178 falas publicadas. Decisão do Paulo, não minha.
            //   Colony Laser (10 de 25) -- entrada seria INERTE: a legenda escreve
            //     "colony Laser" com c minúsculo nas 24 ocorrências, e o enforcer exige o
            //     canônico na grafia exata. Mesmo caso de Bio-Computer no F91.
            //   Earth Federation (33 de 33) -- "Forças Federais da Terra", decisão de
            //     produto consistente com CCA, F91, Unicorn e 08th.
            //   Mobile Suit (116 de 301) -- plural ou omissão da fala inteira.
        ));
    }

    /**
     * PROPOSITO DE NEGOCIO: separa as tres superficies que o modelo confundiu em 172 falas
     * medidas no cache em 2026-07-28 -- "Four" (Four Murasame, personagem), "Quattro"
     * (Quattro Bajeena / Char) e o numeral "quatro".
     *
     * <p>157 falas trocaram "Four" pelo numeral e 15 o trocaram por "Quattro", ou seja, uma
     * personagem virou OUTRA. Nenhuma dessas 15 tem "Quattro" no ingles: nao vieram do mapa
     * deterministico (que so age com o canonico no original) -- vieram do modelo, empurrado
     * pela propria lore, que repetia "NUNCA Quatro" tres vezes e nada dizia sobre "Four".
     *
     * <p>INVARIANTES DO DOMINIO: a proibicao e simetrica e vale SO entre os dois termos do par.
     * O numeral fica de fora de proposito -- "four" minusculo deve ser traduzido normalmente, e
     * proteger a palavra pegaria as 4 falas em que "Four" abre a frase como numero
     * ("Four units, confirmed!").
     *
     * <h2>Char x Quattro: o unico par aqui que protege ENREDO, e nao grafia</h2>
     * Medido em 2026-07-28: <b>34 falas</b> em que o ingles diz "Char" e a traducao gravou
     * "Quattro". A direcao e assimetrica -- 34 num sentido e ZERO no outro --, e a assimetria
     * denuncia a causa: o modelo nao esta confundindo dois nomes parecidos, esta RESOLVENDO uma
     * identidade que a obra esconde de proposito.
     * <pre>
     *   EN "Do you know of a man by the name of Char Aznable?"
     *   PT "Sabe de um homem chamado Quattro Bajeena?"        &lt;- entrega o segredo
     *
     *   EN "someone like Char Aznable"
     *   PT "alguem como Quattro Aznable"                      &lt;- quimera; esse nome nao existe
     * </pre>
     * Char Aznable se apresenta como Quattro Bajeena, e perguntar por "Char" e perguntar pela
     * identidade oculta. A traducao que resolve a charada estraga a cena para quem assiste.
     *
     * <p>A lore CAUSOU isto, em dois lugares: o roster trazia "Quattro Bajeena / Char Aznable" e
     * o bloco das tres superficies -- escrito no mesmo dia para separar "Four" do numeral --
     * explicava quem era Quattro dizendo "Quattro Bajeena / Char". Ensinar a equivalencia para o
     * modelo ENTENDER virou licenca para ele SUBSTITUIR. Mesmo padrao de Inori/Crow e
     * Sanders/Shinigami; aqui o custo e o eixo narrativo da obra.
     *
     * <p>Seguranca do par, medida antes de declarar: <b>ZERO</b> falas do ingles mencionam "Char"
     * e "Quattro" juntos, entao nao existe cena legitima ("Quattro Bajeena, or should I say Char
     * Aznable?") para acusar por engano neste acervo. Com a guarda de preservacao de
     * {@code trocou()}, uma fala assim tambem nao dispararia.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutavel; sem I/O.
     */
    @Override
    public Set<List<String>> paresInconfundiveis() {
        return Set.of(
            List.of("Four", "Quattro"),
            List.of("Zeta Gundam", "Gundam Mk-II"),
            List.of("Char", "Quattro")
        );
    }
}
