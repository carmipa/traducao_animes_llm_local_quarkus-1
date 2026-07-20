package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore completa (Opção 7) para Mobile Suit Zeta Gundam —
 * mesma política da Tradução Local, sem importar {@code contexto.lore}.
 *
 * <p>INVARIANTES DO DOMÍNIO: Titans ≠ Titãs; Quattro ≠ Quatro; Axis ≠ Eixo;
 * Hyaku Shiki ≠ Cem Estilos; The O ≠ O; Newtype ≠ Novo Tipo; A.E.U.G. preserva pontos;
 * Kamille Bidan permanece masculino.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreGundamZeta implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Zeta Gundam, Universal Century 0087, Gryps Conflict.
        - Papel: corrigir APENAS nomenclatura. Nomes/mechas/naves/faccoes NAO sao localizados.

        === Faccoes ===
        - A.E.U.G. / AEUG (Anti-Earth Union Group) — preservar pontos quando houver A.E.U.G.
        - Titans (NUNCA Titãs); Karaba; Anaheim Electronics; Earth Federation.
        - Axis / Axis Zeon (NUNCA Eixo) — Haman Karn / Mineva Lao Zabi.

        === Roster — AEUG / Argama / Karaba ===
        - Kamille Bidan (m); Quattro Bajeena / Char Aznable (NUNCA Quatro); Bright Noa;
          Emma Sheen; Fa Yuiry; Reccoa Londe; Katz Kobayashi; Henken Bekkener;
          Astonaige Medoz; Apolly Bay; Roberto; Torres; Wong Lee;
          Amuro Ray; Hayato Kobayashi; Mirai Yashima; Hathaway Noa;
          Franklin Bidan; Hilda Bidan; Beltorchika Irma.

        === Roster — Titans / Scirocco ===
        - Jerid Messa; Bask Om; Jamitov Hymen; Jamaican Daninghan; Paptimus Scirocco;
          Yazan Gable; Buran Blutarch; Lila Milla Rira; Mouar Pharaoh; Sarah Zabiarov;
          Kacricon Cacooler; Gates Capa.

        === Roster — Cyber-Newtype / Axis ===
        - Four Murasame; Rosamia Badam; Haman Karn; Mineva Lao Zabi.

        === Naves / lugares / eventos ===
        - Argama; Radish; Alexandria; Audhumla; Jupitris; Gwadan.
        - Gryps / Gate of Zedan; Jaburo; Dakar; Kilimanjaro; Axis; Shangri-La.
        - Gryps Conflict; Colony Laser; Colony 30 Incident; Dakar Speech.

        === Mecha ===
        - Zeta Gundam; Gundam Mk-II / Super Gundam / G-Defenser; Hyaku Shiki (NUNCA Cem Estilos);
          Rick Dias; Methuss; Nemo; Dijeh; Hizack; Marasai; Gaplant; Gabthley; Hambrabi;
          Palace Athene; Byarlant; Messala; The O (NUNCA reduzir a O);
          Psycho Gundam / Psycho Gundam Mk-II; Qubeley; Gaza-C.

        === Formas-ruim (restaurar) ===
        - Titans nao vira Titãs; Quattro nao vira Quatro; Axis nao vira Eixo;
          Hyaku Shiki nao vira Cem Estilos; The O nao vira O; Newtype nao vira Novo Tipo.
        - Titãs/Titas → Titans; Quatro → Quattro; Eixo → Axis;
          Cem Estilos → Hyaku Shiki; O O → The O;
          União Anti-Terra → AEUG; Conflito de Gryps → Gryps Conflict;
          Laser de Colônia → Colony Laser; Psico Gundam → Psycho Gundam;
          Cubely → Qubeley; Gundam Mark II → Gundam Mk-II.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override
    public String getId() {
        return "gundam_zeta";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Zeta Gundam - Revisao de Lore";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo UC + extras Zeta na Opção 7 (espelho da Tradução Local).
     *
     * <p>INVARIANTES DO DOMÍNIO: sem import cruzado; só aplica com canônico no EN.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamUcRevisao.comExtras(Map.ofEntries(
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
