package org.traducao.projeto.contexto.lore.guiltycrown;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore completa de Guilty Crown (roster + termos + mapa) para
 * tradução EN→PT-BR com grafia canônica oficial.
 *
 * <p>INVARIANTES DO DOMÍNIO: Funeral Parlor ≠ Undertaker; Void Genome ≠ "Genoma do Vazio";
 * Guilty Crown ≠ "Coroa Culpada"; Apocalypse Virus; Lost Christmas; Endlave; GHQ.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoGuiltyCrown implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Guilty Crown (serie TV Production I.G, 2011-2012).
        - Premissa: Toquio sob ocupacao da GHQ apos o colapso biologico do Apocalypse Virus;
          Shu Ouma herda o Void Genome (King's Power) e se alia ao Funeral Parlor.

        === Apocalipse biologico ===
        - Apocalypse Virus: virus do apocalipse (NUNCA "Virus do Apocalipse" como nome proprio).
        - Lost Christmas (2029): evento historico do surto/ocupacao — manter "Lost Christmas".
        - Crystallization: estagio avancado da infeccao (corpo cristaliza) — termo tecnico da obra.
        - Genomic Resonance; Leukocyte (unidade/sistema GHQ quando aparecer); Norma Gene (droga ilegal).

        === Tecnologia de almas (Voids) ===
        - Void Genome: cilindro/arma genetica que concede King's Power. NUNCA traduzir.
        - King's Power / Power of the King: capacidade de extrair Voids.
        - Void / Voids: materializacao da psique (menores de 17) em arma/ferramenta.
          NUNCA traduzir Void como "vazio" filosofico generico.
        - Endlave / Endlaves: mecha de combate por interface neural. Manter Endlave.

        === Faccoes (NUNCA fundir) ===
        - GHQ: forca militar internacional de ocupacao do Japao.
        - Anti Bodies: unidade especial da GHQ (Segai / Daryl).
        - Funeral Parlor (葬儀社): resistencia guerrilheira liderada por Gai Tsutsugami.
          NUNCA "Funeraria" / "Empresa Funeraria".
        - Undertaker: codinome/identidade de combate de Gai — NAO e o nome do grupo.
        - Da'ath: organizacao/conspiracao acima da GHQ quando o dialogo trouxer.
        - Sephirah Genomics: laboratorio/empresa ligada ao virus e a familia Ouma.

        === Roster — Funeral Parlor ===
        - Shu Ouma (m) — herda Void Genome; King's Power.
        - Inori Yuzuriha (f) — cantora; parceira de Shu; Void ligado a Mana.
        - Gai Tsutsugami (m) — lider; Undertaker.
        - Ayase Shinomiya (f) — piloto de Endlave (cadeira de rodas).
        - Tsugumi (f) — hacker / suporte.
        - Yahiro Samukawa (m); Hare Menjou (f); Souta Tamadate (m).
        - Shibungi (m) — staff/conselheiro militar do Funeral Parlor.
        - Kenji Kido (m) e demais soldados quando nomeados.

        === Roster — GHQ / Anti Bodies / politicos ===
        - Shuichiro Keido (m) — antagonista politico/cientifico (tambem "Keido").
        - Makoto Waltz Segai (m) — Anti Bodies.
        - Daryl Yan (m); Major General Yan (quando aparecer).
        - Dan Eagleman (m); Arisa Kuhouin (f); Okina Kuhouin (m) quando aparecerem.

        === Roster — familia / escola / virus ===
        - Mana Ouma (f) — Eve / nucleo do Apocalypse Virus.
        - Kurosu Ouma (m); Haruka Ouma (f).
        - Jun Samukawa (m) — irmao de Yahiro.
        - Tennouzu High / Second Hand / Loop 7 / Roppongi Fort quando o dialogo trouxer.

        === Regras duras ===
        - Funeral Parlor ≠ Undertaker; Void/Void Genome/Endlave/Lost Christmas/Guilty Crown oficiais.
        - Tom: acao, idol/cancao (Inori), drama adolescente, opressao politica, biologia distopica.
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
     * PROPÓSITO DE NEGÓCIO: protege elenco, facções e termos canônicos de Guilty Crown.
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
            "Souta Tamadate", "Shibungi", "Kenji Kido", "Arisa Kuhouin",
            "Okina Kuhouin", "Daryl Yan", "Makoto Waltz Segai", "Shuichiro Keido",
            "Keido", "Dan Eagleman", "Kurosu Ouma", "Haruka Ouma", "Jun Samukawa",
            "Funeral Parlor", "Undertaker", "GHQ", "Anti Bodies", "Da'ath",
            "Sephirah Genomics", "Void", "Voids", "Void Genome", "King's Power",
            "Power of the King", "Apocalypse Virus", "Apocalypse", "Lost Christmas",
            "Endlave", "Endlaves", "Leukocyte", "Genomic Resonance", "Crystallization",
            "Norma Gene", "Guilty Crown", "Tennouzu"
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
