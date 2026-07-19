package org.traducao.projeto.contexto.lore.gundam.zeta;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.contexto.lore.gundam.CorrecoesTerminologiaGundamUc;

import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore de Zeta Gundam (Gryps Conflict / UC 0087) para tradução
 * EN→PT-BR com nomes e facções na grafia canônica oficial.
 *
 * <p>INVARIANTES DO DOMÍNIO: Kamille Bidan (masculino); AEUG / A.E.U.G.; Titans
 * (NUNCA "Titãs"); Quattro Bajeena (NUNCA "Quatro"); Axis (NUNCA "Eixo"); Hyaku Shiki
 * (NUNCA "Cem Estilos"). Nomes próprios, mechas e organizações não são localizados.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos e mapa de correção
 * imutáveis; o enforcer só restaura quando o original EN contém o canônico.
 */
@Component
public class ContextoGundamZeta implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Zeta Gundam, Universal Century 0087, Gryps Conflict.
        - Direcao: ingles → portugues brasileiro; tom militar/politico sombrio, trauma e abuso de autoridade.

        === Faccoes / organizacoes (grafia oficial — NAO traduzir) ===
        - A.E.U.G. / AEUG (Anti-Earth Union Group): preservar pontos quando o original trouxer A.E.U.G.
        - Titans: nome canônico da forca especial da Federacao (NUNCA "Titãs" / mitologia grega).
        - Earth Federation / Federation Forces; Axis Zeon / Axis (NUNCA "Eixo"); Karaba; Anaheim Electronics.

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
        - Eventos: Gryps Conflict, Colony Laser.

        === Mobile suits / armors (designacao oficial) ===
        - AEUG/Anaheim: MSZ-006 Zeta Gundam, RX-178 Gundam Mk-II, MSN-00100 Hyaku Shiki
          (NUNCA "Cem Estilos"), Rick Dias, Methuss, Nemo, GM II.
        - Titans: Marasai, Gaplant, Gabthley, Hambrabi, Palace Athene, Byarlant.
        - Outros: Psycho Gundam (MRX-009; MA/MS conforme obra), The O (NUNCA reduzir a "O"),
          Qubeley (Haman).

        === Termos UC ===
        - Newtype (NUNCA "Novo Tipo"), Cyber-Newtype, Oldtype, Psycommu, Minovsky particles,
          Spacenoid / Earthnoid, Mobile Suit vs Mobile Armor, beam rifle, beam saber.

        === Regras duras de nomenclatura ===
        - Titans, Axis, Quattro, Hyaku Shiki, A.E.U.G., Newtype, The O: grafia oficial sempre.
        - Kamille e masculino; Quattro estrategico; Titans autoritarios; Scirocco manipulador.
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
     * PROPÓSITO DE NEGÓCIO: protege elenco, facções e unidades canônicas do Zeta
     * contra tradução/localização indevida pelo detector de fala idêntica e pela lore ativa.
     *
     * <p>INVARIANTES DO DOMÍNIO: só artefatos UC 0087 / Gryps Conflict; grafias oficiais.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável; sem I/O.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Kamille Bidan", "Quattro Bajeena", "Char Aznable", "Amuro Ray", "Bright Noa",
            "Emma Sheen", "Fa Yuiry", "Reccoa Londe", "Haman Karn", "Paptimus Scirocco",
            "Jerid Messa", "Four Murasame", "Rosamia Badam", "Katz Kobayashi", "Wong Lee",
            "Henken Bekkener", "Astonaige Medoz", "Bask Om", "Jamitov Hymen",
            "Jamaican Daninghan", "Lila Milla Rira", "Yazan Gable", "Hayato Kobayashi",
            "Mirai Yashima", "AEUG", "A.E.U.G.", "Titans", "Axis", "Karaba",
            "Anaheim Electronics", "Zeta Gundam", "Gundam Mk-II", "Hyaku Shiki",
            "Rick Dias", "Methuss", "Psycho Gundam", "The O", "Qubeley", "Argama",
            "Audhumla", "Alexandria", "Gryps", "Newtype", "Cyber-Newtype", "Oldtype",
            "Psycommu", "Minovsky", "Spacenoid", "Earthnoid", "Mobile Suit", "Mobile Armor"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: restaura grafias oficiais quando o LLM localiza indevidamente
     * nomes canônicos (Titãs, Quatro, Eixo) — padrão do enforcer do 86.
     *
     * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim em PT; valor = canônico; só aplica
     * se o original EN contém o canônico (ex.: "quatro" numeral sem "Quattro" no EN
     * não é tocado).
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
            Map.entry("O O", "The O")
        ));
    }
}
