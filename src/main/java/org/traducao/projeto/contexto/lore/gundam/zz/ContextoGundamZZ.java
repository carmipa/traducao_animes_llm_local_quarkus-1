package org.traducao.projeto.contexto.lore.gundam.zz;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore completa de Mobile Suit Gundam ZZ / Double Zeta (UC 0088) —
 * Neo Zeon, Shangri-La kids, Glemy Faction e ZZ Gundam.
 *
 * <p>INVARIANTES DO DOMÍNIO: Judau Ashta; Axis ≠ Eixo; Titans ≠ Titãs; Ple ≠ Plê;
 * Quin Mantha ≠ Rainha Mansa; ZZ Gundam ≠ Zeta Duplo; Newtype oficial; Lady Haman.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoGundamZZ implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam ZZ / Double Zeta (TV) — Universal Century U.C. 0088,
          sequela direta de Zeta; pos-Gryps Conflict; ascensao de Neo Zeon / Axis.
        - Premissa: Judau Ashta e a Blue Corps sobem a bordo da Argama / Nahel Argama;
          guerra contra Haman Karn (Lady Haman) e cisao Glemy Toto.
        - Tom: inicio aventureiro/comico → guerra politica e tragedia. Evitar girias modernas.

        === Nucleo UC ===
        - Newtype (NUNCA Novo Tipo); Cyber-Newtype; Oldtype; Psycommu; Minovsky particles;
          Spacenoid vs Earthnoid; Mobile Suit vs Mobile Armor;
          Beam Rifle / Beam Saber; Mega Particle Cannon.

        === Faccoes (NUNCA fundir) ===
        - A.E.U.G. / AEUG; Neo Zeon / Axis (NUNCA Eixo); Earth Federation; Karaba;
          Anaheim Electronics; Glemy Faction (cisao Neo Zeon).
        - Titans (mencoes historicas — NUNCA Titãs); Blue Corps (grupo de Judau em Shangri-La).

        === Roster — Shangri-La / Argama ===
        - Judau Ashta (m); Leina Ashta (f); Roux Louka (f); Elle Vianno (f);
          Beecha Oleg (m); Mondo Agake (m); Iino Abbav (m).
        - Bright Noa (m); Fa Yuiry (f); Kamille Bidan (m — aparicoes/hospital);
          Wong Lee (m); Astonaige Medoz (m); Torres (m); Emary Ounce (f) quando aparecer.

        === Roster — Neo Zeon / Axis / Glemy ===
        - Haman Karn / Lady Haman (f — NUNCA Senhorita Haman quando o EN trouxer Lady Haman);
          Mashymre Cello (m); Chara Soon (f); Glemy Toto (m);
          Elpeo Ple (f — NUNCA Plê); Ple Two (f); Mineva Lao Zabi (f, crianca);
          Rakan Dahkaran (m); Gottn Goh (m); August Guidan (m); Illia Pazom (f) quando aparecerem;
          Rasara Moon / Sarasa Moon conforme creditos.

        === Naves / lugares ===
        - Argama; Nahel Argama (refit); Endra; Sadalahn; Gwadan quando cruzar.
        - Shangri-La; Axis; Core 3; Dublin; Moon Moon; Tigerbaum quando o dialogo trouxer.

        === Nomes que se parecem e NAO se substituem ===
        - Original "Argama"      -> saida "Argama".        Nave.
        - Original "Nahel Argama"-> saida "Nahel Argama".  OUTRA nave, a sucessora.
        - Original "Zeta Gundam" -> saida "Zeta Gundam".   Mecha do Kamille.
        - Original "ZZ Gundam"   -> saida "ZZ Gundam".     Mecha do Judau, outro.
        - A serie usa os quatro. Escreva o que o original escreveu; nao promova nem rebaixe
          um nome para o outro.

        === Mecha ===
        - MSZ-010 ZZ Gundam (NUNCA Zeta Duplo; Double Zeta nao vira "Zeta Duplo");
          Full Armor ZZ quando aparecer; Core Top / Core Base / Core Fighter.
        - MSZ-006 Zeta Gundam; RX-178 Gundam Mk-II; MSN-00100 Hyaku Shiki.
        - AMX-004 Qubeley / Qubeley Mk-II; NZ-000 Quin Mantha / Queen Mansa
          (Quin Mantha NUNCA Rainha Mansa; MA quando a obra designar);
          Hamma Hamma; R-Jarja; Dreissen; Bawoo; Zaku III; Gaza-C; Ga-Zowmn;
          Geymalk; Psycho Gundam Mk-II (inicio) quando aparecer.

        === Regras duras ===
        - Axis nao vira Eixo; Titans nao vira Titãs; Ple nao vira Plê;
          Quin Mantha nao vira Rainha Mansa; ZZ Gundam / Double Zeta nao vira Zeta Duplo;
          Newtype nao vira Novo Tipo; Lady Haman grafia oficial.
        - Judau informal→maduro; Haman imperial; Glemy ambicioso; Mashymre teatral.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam ZZ", LORE);

    @Override
    public String getId() {
        return "gundam_zz";
    }

    @Override
    public String getNomeExibicao() {
        return "Mobile Suit Gundam ZZ";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege elenco ZZ/Neo Zeon, naves, mecha e facções canônicas.
     *
     * <p>INVARIANTES DO DOMÍNIO: só artefatos UC 0088 / ZZ; grafias oficiais.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Judau Ashta", "Leina Ashta", "Haman Karn", "Lady Haman", "Roux Louka", "Elle Vianno",
            "Beecha Oleg", "Mondo Agake", "Iino Abbav", "Glemy Toto",
            "Mashymre Cello", "Chara Soon", "Bright Noa", "Fa Yuiry", "Kamille Bidan",
            "Wong Lee", "Astonaige Medoz", "Torres", "Emary Ounce",
            "Elpeo Ple", "Ple", "Ple Two", "Mineva Lao Zabi",
            "Rakan Dahkaran", "Gottn Goh", "August Guidan", "Illia Pazom",
            "ZZ Gundam", "Double Zeta", "Full Armor ZZ", "Zeta Gundam", "Gundam Mk-II",
            "Hyaku Shiki", "Qubeley", "Qubeley Mk-II", "Quin Mantha", "Queen Mansa",
            "Hamma Hamma", "R-Jarja", "Dreissen", "Bawoo", "Zaku III",
            "Gaza-C", "Ga-Zowmn", "Geymalk", "Psycho Gundam Mk-II",
            "Shangri-La", "Argama", "Nahel Argama", "Endra", "Sadalahn",
            "Core 3", "Dublin", "Moon Moon", "Tigerbaum", "Axis", "Sieg Zeon",
            "Neo Zeon", "Glemy Faction", "Titans", "AEUG", "A.E.U.G.",
            "Earth Federation", "Karaba", "Anaheim Electronics", "Blue Corps",
            "Newtype", "Cyber-Newtype", "Oldtype", "Minovsky", "Psycommu",
            "Mobile Suit", "Mobile Armor", "Beam Rifle", "Beam Saber",
            "Spacenoid", "Earthnoid", "Mega Particle Cannon",
            // Traje pressurizado do piloto. Faltava aqui, e o run completo mostrou o custo:
            // 23 ocorrências de "normal suit" no inglês, ZERO preservadas, e nove renderizações
            // diferentes para a mesma peça ao longo dos 47 episódios.
            "Normal Suit", "Powered Suit", "Powered Spacesuit"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: restaura Axis/Titans/Ple/Quin Mantha/ZZ quando o LLM localiza.
     *
     * <p>INVARIANTES DO DOMÍNIO: mapa de {@link CorrecoesTerminologiaGundamZz}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGundamZz.mapa();
    }

    /**
     * PROPOSITO DE NEGOCIO: separa mechas e naves que o modelo trocou entre si, medido no cache
     * em 2026-07-28.
     *
     * <ul>
     *   <li><b>55 falas</b>: o ingles diz "Zeta Gundam" e a traducao gravou "ZZ Gundam". Sao
     *       mobile suits DIFERENTES, e a obra usa os dois.</li>
     *   <li><b>31 falas</b>: o ingles diz "Argama" e a traducao gravou "Nahel Argama". Sao naves
     *       DIFERENTES -- a Nahel Argama e a sucessora, e as duas aparecem na serie.</li>
     * </ul>
     *
     * <p>INVARIANTES DO DOMINIO: proibicao simetrica entre os dois termos do par. "Nahel Argama"
     * contem "Argama" como substring: quem consome o par tem de comparar por fronteira de
     * palavra sobre o termo INTEIRO, senao toda mencao a Nahel Argama acusaria falsamente.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutavel; sem I/O.
     */
    @Override
    public Set<List<String>> paresInconfundiveis() {
        return Set.of(
            List.of("Zeta Gundam", "ZZ Gundam"),
            List.of("Argama", "Nahel Argama")
        );
    }
}
