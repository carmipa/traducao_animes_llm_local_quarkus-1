package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: fonte canônica da Revisao de Lore (Opção 7) para
 * Mobile Suit Zeta Gundam — mesma riqueza e política de nomenclatura da
 * Tradução Local ({@code ContextoGundamZeta}), adaptada ao papel de revisor
 * (corrigir grafia indevida em PT-BR, sem retraduzir o diálogo).
 *
 * <p>INVARIANTES DO DOMÍNIO: Titans ≠ Titãs; Quattro ≠ Quatro; Axis ≠ Eixo;
 * Hyaku Shiki ≠ Cem Estilos; The O ≠ O; Newtype ≠ Novo Tipo; A.E.U.G. preserva
 * pontos; Kamille Bidan permanece masculino quando o original assim exige.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt imutável em compile-time.
 */
@Component
public class ContextoRevisaoLoreGundamZeta implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Mobile Suit Zeta Gundam, Universal Century 0087, Gryps Conflict.
        - Papel desta lore: corrigir APENAS nomenclatura fora do padrao oficial.
          Nomes proprios, mechas, naves e faccoes especificas NAO sao localizados para portugues.
        - Tom da obra (so para julgamento de coerencia de nome/patente): militar/politico sombrio,
          trauma e abuso de autoridade — nao usar para reescrever estilo.

        === Faccoes / organizacoes (grafia oficial — NAO "melhorar" para PT) ===
        - A.E.U.G. / AEUG (Anti-Earth Union Group): preservar pontos quando o texto/origem trouxer A.E.U.G.
        - Titans: nome canônico da forca especial (NUNCA "Titãs" / mitologia grega).
        - Earth Federation / Federation Forces pode aparecer como Federacao Terrestre quando ja estiver
          natural em PT; Axis Zeon / Axis NUNCA vira "Eixo"; Karaba; Anaheim Electronics.

        === Personagens (genero; preservar grafia) ===
        - AEUG / Argama: Kamille Bidan (m — pronomes masculinos; piada recorrente de confusao de genero),
          Quattro Bajeena / Char Aznable (m — NUNCA "Quatro"), Bright Noa (m), Emma Sheen (f),
          Fa Yuiry (f), Reccoa Londe (f), Katz Kobayashi (m), Henken Bekkener (m),
          Astonaige Medoz (m), Apolly Bay (m), Roberto (m), Wong Lee (m).
        - Continuacao UC / aliados: Amuro Ray (m), Hayato Kobayashi (m), Mirai Yashima (f),
          Hathaway Noa (m, crianca), Franklin Bidan (m), Hilda Bidan (f).
        - Titans: Jerid Messa (m), Bask Om (m), Jamitov Hymen (m), Jamaican Daninghan (m),
          Buran Blutarch (m), Lila Milla Rira (f), Mouar Pharaoh (f), Sarah Zabiarov (f),
          Yazan Gable (m), Paptimus Scirocco (m).
        - Cyber-Newtype / Axis: Four Murasame (f), Rosamia Badam (f), Haman Karn (f).

        === Naves / lugares / eventos ===
        - Naves: Argama, Alexandria, Audhumla (Karaba), Radish, Jupitris, Gwadan.
        - Lugares: Gryps, Jaburo, Hong Kong, Dakar, Kilimanjaro, Axis, Side colonies.
        - Eventos: Gryps Conflict, Colony Laser, Universal Century / U.C.

        === Mobile suits / armors (designacao oficial) ===
        - AEUG/Anaheim: MSZ-006 Zeta Gundam, RX-178 Gundam Mk-II, MSN-00100 Hyaku Shiki
          (NUNCA "Cem Estilos"), Rick Dias, Methuss, Nemo, GM II.
        - Titans: Marasai, Gaplant, Gabthley, Hambrabi, Palace Athene, Byarlant.
        - Outros: Psycho Gundam (MRX-009), The O (NUNCA reduzir a "O"), Qubeley (Haman).

        === Termos UC ===
        - Newtype (NUNCA "Novo Tipo"), Cyber-Newtype, Oldtype, Psycommu, Minovsky particles,
          Spacenoid / Earthnoid, Mobile Suit vs Mobile Armor, beam rifle, beam saber.

        === Regras duras / formas-ruim observadas ===
        - Titans nao vira "Titãs"; Quattro nao vira "Quatro"; Axis nao vira "Eixo";
          Hyaku Shiki nao vira "Cem Estilos"; The O nao vira "O"; Newtype nao vira "Novo Tipo".
        - Restaurar: Titãs/Titas → Titans; Quatro → Quattro; Eixo → Axis (so quando for a faccao).
        - A.E.U.G. deve manter os pontos; Titans/Quattro/Axis/Hyaku Shiki/The O/Newtype na grafia oficial.
        - Kamille masculino; Quattro estrategico; Titans autoritarios; Scirocco manipulador.
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
     * PROPÓSITO DE NEGÓCIO: formas-ruim de terminologia do Zeta que a revisão restaura
     * deterministicamente — mesmo mapa da tradução local ({@code ContextoGundamZeta}).
     *
     * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim PT; valor = canônico; só aplica quando
     * o original EN contém o canônico na grafia exata (ex.: numeral "quatro" sem "Quattro"
     * no EN não é tocado).
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
            Map.entry("O O", "The O")
        ));
    }
}
