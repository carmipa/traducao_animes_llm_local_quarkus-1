package org.traducao.projeto.contexto.lore.gundam.zeta;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc;

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
          Quattro Bajeena / Char Aznable (m — NUNCA Quatro); Bright Noa (m);
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

        === Regras duras ===
        - Titans nao vira Titãs; Quattro nao vira Quatro; Axis nao vira Eixo;
          Hyaku Shiki nao vira Cem Estilos; The O nao vira O; Newtype nao vira Novo Tipo.
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
            Map.entry("Gundam Mk II", "Gundam Mk-II")
        ));
    }
}
