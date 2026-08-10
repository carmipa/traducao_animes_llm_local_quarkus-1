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
        - Blex Forer; Dr. Hasan; Kai Shiden; Fraw Bow; Shinta; Qum; Haro;
          Luio Woomin; Stephanie Luio; Ben Wooder; Namicar Cornell.

        === Roster — Titans / Scirocco ===
        - Jerid Messa; Bask Om; Jamitov Hymen; Jamaican Daninghan; Paptimus Scirocco;
          Yazan Gable; Buran Blutarch; Lila Milla Rira; Mouar Pharaoh; Sarah Zabiarov;
          Kacricon Cacooler; Gates Capa.
        - Tripulantes e pilotos recorrentes: Gady; Siddeley; Batch; Saegusa; Ramsus;
          Botty; Manack; Hamil; Addis; Dava Baro.

        === Roster — Cyber-Newtype / Axis ===
        - Four Murasame; Rosamia Badam; Haman Karn; Mineva Lao Zabi.
        - Rosammy e o apelido usado por Kamille para Rosamia. Preserve exatamente Rosammy quando
          o original usar Rosammy; nao normalize o apelido para Rosamia.

        === Naves / lugares / eventos ===
        - Argama; Mont Blanc; Radish; Alexandria; Audhumla; Jupitris; Gwadan;
          Dogosse Gier; Bosnia; Sudori; Garuda; White Base.
        - Gryps / Gate of Zedan; Jaburo; Dakar; Kilimanjaro; Axis; Shangri-La;
          Granada; Von Braun City; Green Oasis; Green Noa; Hickory; Amman;
          New Hong Kong; Murasame Laboratory.
        - Gryps Conflict; Colony Laser; Colony 30 Incident; Dakar Speech;
          Operation Apollo; Operation Maelstrom.

        === Mecha ===
        - Zeta Gundam; Gundam Mk-II / Super Gundam / G-Defenser; Hyaku Shiki (NUNCA Cem Estilos);
          Rick Dias; Methuss; Nemo; Dijeh; Hizack; Marasai; Gaplant; Gabthley; Hambrabi;
          Palace Athene; Byarlant; Messala; Baund Doc; The O (NUNCA reduzir a O);
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
            Map.entry("Gundam Mk II", "Gundam Mk-II"),
            Map.entry("Dogosse Giar", "Dogosse Gier"),
            Map.entry("Rosamia", "Rosammy"),
            Map.entry("Quem", "Qum"),
            Map.entry("Quim", "Qum"),
            Map.entry("Mancack", "Manack"),
            Map.entry("Ramus", "Ramsus"),
            Map.entry("Lote", "Batch"),
            Map.entry("Oásis Verde", "Green Oasis"),
            Map.entry("Oasis Verde", "Green Oasis"),
            Map.entry("Paraíso Verde", "Green Oasis"),
            Map.entry("Paraiso Verde", "Green Oasis"),
            Map.entry("Verde Noa", "Green Noa"),
            Map.entry("cidade de Von Braun", "Von Braun City"),
            Map.entry("Nova Hong Kong", "New Hong Kong"),
            // ESPELHO de FA YUIRY — minerado no acervo em 2026-08-04. O Zeta concentra 43 das 51
            // falas cujo texto INTEIRO e "Fa", e so 8 preservaram o nome: "Fogo!" 27x, "Fala!"
            // 7x, "Fá..." 6x, "Pá!" 1x, "Fale!" 1x. Colisao legitima medida: zero.
            // Justificativa completa e risco residual em CorrecoesTerminologiaGundamZz.
            Map.entry("Fogo", "Fa"),
            Map.entry("Fala", "Fa"),
            Map.entry("Fá", "Fa"),
            Map.entry("Pá", "Fa"),
            Map.entry("Fale", "Fa"),
            // ESPELHO EXATO dos extras da Traducao — formas medidas nas 16.778 falas.
            Map.entry("Super Gundam", "G-Defenser"),
            Map.entry("Portão de Zedan", "Gate of Zedan"),
            Map.entry("Portao de Zedan", "Gate of Zedan"),
            Map.entry("Guerra de Um Ano", "One Year War"),
            Map.entry("canhão de partículas megas", "Mega Particle Cannon"),
            Map.entry("mega canhão de partículas", "Mega Particle Cannon"),
            Map.entry("Incidente da Colônia 30", "Colony 30 Incident"),
            Map.entry("Incidente da Colonia 30", "Colony 30 Incident"),
            Map.entry("Palácio Atena", "Palace Athene"),
            Map.entry("Palacio Atena", "Palace Athene"),
            Map.entry("Quatro Murasame", "Four Murasame")
        ));
    }
}
