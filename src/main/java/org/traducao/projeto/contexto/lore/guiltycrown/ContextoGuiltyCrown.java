package org.traducao.projeto.contexto.lore.guiltycrown;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore completa de Guilty Crown (TV) — roster Funeral Parlor/GHQ,
 * virus/tecnologia de Voids e mapa de formas-ruim para Tradução Local EN→PT-BR.
 *
 * <p>INVARIANTES DO DOMÍNIO: Funeral Parlor ≠ Undertaker ≠ Funerária; Void Genome ≠ Genoma do Vazio;
 * Guilty Crown ≠ Coroa Culpada; Apocalypse Virus; Lost Christmas; Endlave; Anti Bodies; Da'ath.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoGuiltyCrown implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Guilty Crown (serie TV Production I.G, 2011-2012).
        - Premissa: Toquio sob ocupacao da GHQ apos o colapso biologico do Apocalypse Virus;
          Shu Ouma herda o Void Genome (King's Power) e se alia ao Funeral Parlor.
          Lost Christmas (OVA) e prequela — so trazer nomes se o dialogo trouxer.

        === Apocalipse biologico ===
        - Apocalypse Virus: nome proprio oficial (NUNCA "Virus do Apocalipse" como titulo).
        - Lost Christmas (2029): evento historico do surto/ocupacao — manter "Lost Christmas".
        - Crystallization: estagio avancado da infeccao (corpo cristaliza).
        - Genomic Resonance; Leukocyte (unidade/sistema GHQ); Norma Gene (droga ilegal).
        - Eve / Eve of the Apocalypse quando o dialogo ligar Mana/Inori ao nucleo do virus.

        === Tecnologia de almas (Voids) ===
        - Void Genome: cilindro/arma genetica que concede King's Power. NUNCA traduzir.
        - King's Power / Power of the King: extrair Voids de menores de 17.
        - Void / Voids: materializacao da psique em arma/ferramenta.
          NUNCA traduzir Void capitalizado como "vazio" filosofico generico.
        - Endlave / Endlaves: mecha por interface neural (NUNCA "Endslave" / "escravo final").

        === Faccoes (NUNCA fundir) ===
        - GHQ: forca militar internacional de ocupacao do Japao.
        - Anti Bodies: unidade especial da GHQ (Segai / Daryl) — NUNCA "Anticorpos".
        - Funeral Parlor (葬儀社): resistencia de Gai Tsutsugami.
          NUNCA "Funeraria" / "Empresa Funeraria".
        - Undertaker: codinome de combate de Gai — NAO e o nome do grupo.
        - Da'ath: conspiracao acima da GHQ (grafia com apostrofo).
        - Sephirah Genomics: laboratorio/empresa ligada ao virus e a familia Ouma.
        - Kuhouin Group quando o dialogo trouxer (Arisa / Okina).

        === Roster — Funeral Parlor ===
        - Shu Ouma (m) — herda Void Genome; King's Power.
        - Inori Yuzuriha (f) — cantora; parceira de Shu; tambem Crow no palco quando aparecer.
        - Gai Tsutsugami (m) — lider; Undertaker.
        - Ayase Shinomiya (f) — piloto de Endlave (cadeira de rodas).
        - Tsugumi (f) — hacker / suporte.
        - Yahiro Samukawa (m); Hare Menjou (f); Souta Tamadate (m).
        - Shibungi (m) — staff/conselheiro militar.
        - Kenji Kido (m); Argo Tsukishima (m); Oogumo (m) quando nomeados.

        === Roster — GHQ / Anti Bodies / politicos ===
        - Shuichiro Keido (m) — antagonista politico/cientifico (tambem "Keido").
        - Makoto Waltz Segai (m) — Anti Bodies.
        - Daryl Yan (m); Major General Yan (quando aparecer).
        - Dan Eagleman (m); Arisa Kuhouin (f); Okina Kuhouin (m) quando aparecerem.

        === Roster — familia / escola / virus ===
        - Mana Ouma (f) — Eve / nucleo do Apocalypse Virus.
        - Kurosu Ouma (m); Haruka Ouma (f).
        - Jun Samukawa (m) — irmao de Yahiro.
        - Tennouzu High / Second Hand (clube); Loop 7; Roppongi Fort quando o dialogo trouxer.

        === Regras duras ===
        - Funeral Parlor ≠ Undertaker; Void/Void Genome/Endlave/Lost Christmas/Guilty Crown oficiais.
        - Anti Bodies ≠ Anticorpos; Da'ath mantem apostrofo; Crow e persona de palco de Inori.
        - Tom: acao, idol/cancao (Inori/Crow), drama adolescente, opressao politica, biologia distopica.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Guilty Crown", LORE);

    @Override
    public String getId() {
        return "guilty_crown";
    }

    @Override
    public String getNomeExibicao() {
        return "Guilty Crown";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege elenco, facções, locais e termos canônicos de Guilty Crown.
     *
     * <p>INVARIANTES DO DOMÍNIO: grafias oficiais EN; Funeral Parlor ≠ Undertaker.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável; sem I/O.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Shu Ouma", "Inori Yuzuriha", "Gai Tsutsugami", "Mana Ouma",
            "Ayase Shinomiya", "Tsugumi", "Yahiro Samukawa", "Hare Menjou",
            "Souta Tamadate", "Shibungi", "Kenji Kido", "Argo Tsukishima", "Oogumo",
            "Crow", "Arisa Kuhouin", "Okina Kuhouin", "Daryl Yan", "Makoto Waltz Segai",
            "Shuichiro Keido", "Keido", "Dan Eagleman", "Major General Yan",
            "Kurosu Ouma", "Haruka Ouma", "Jun Samukawa",
            "Funeral Parlor", "Undertaker", "GHQ", "Anti Bodies", "Da'ath",
            "Sephirah Genomics", "Kuhouin Group",
            "Void", "Voids", "Void Genome", "King's Power", "Power of the King",
            "Apocalypse Virus", "Apocalypse", "Lost Christmas", "Eve",
            "Endlave", "Endlaves", "Leukocyte", "Genomic Resonance", "Crystallization",
            "Norma Gene", "Guilty Crown",
            "Tennouzu", "Tennouzu High", "Second Hand", "Loop 7", "Roppongi Fort"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: restaura grafias oficiais quando o LLM localiza indevidamente.
     *
     * <p>INVARIANTES DO DOMÍNIO: mapa de {@link CorrecoesTerminologiaGuiltyCrown}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaGuiltyCrown.mapa();
    }
}
