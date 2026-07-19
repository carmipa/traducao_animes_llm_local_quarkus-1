package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore excepcional de Macross: Do You Remember Love?
 * (filme 1984 — releitura épica da Space War I).
 *
 * <p>INVARIANTES DO DOMÍNIO: Zentradi vs Meltrandi; Protoculture (NUNCA "Protocultura"
 * como canônico); Valkyrie; Minmay Attack; SDF-1; anti-Veritech/Robotech.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoMacrossFilme1 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross: Do You Remember Love? / Choujikuu Yousai Macross: Ai Oboete Imasu ka
          (filme 1984 — releitura cinematografica da Space War I; NAO e a serie TV frame-a-frame).
        - Premissa: humanos + cultura/cancao (Lynn Minmay) vs Zentradi e Meltrandi;
          Protoculture como chave civilizacional; SDF-1 Macross.

        === Personagens (genero) ===
        - Hikaru Ichijyo / Hikaru Ichijo (m); Lynn Minmay (f); Misa Hayase (f);
          Roy Focker (m); Maximilian Jenius / Max Jenius (m); Milia Fallyna / Milia Fallyna Jenius (f);
          Exsedol Folmo (m); Britai Kridanik (m); Boddole Zer / Bodolzaa (m) conforme creditos;
          Claudia LaSalle (f) quando aparecer; Global (m) quando aparecer.

        === Faccoes / termos ===
        - UN Spacy / U.N. Spacy; Zentradi; Meltrandi (facao feminina — grafia oficial);
          Protoculture (NUNCA deixar "Protocultura" como forma canonica — restaurar Protoculture);
          Minmay Attack; Overtechnology; Reaction Weaponry; Fold / space fold.

        === Mecha / naves ===
        - SDF-1 Macross; VF-1 Valkyrie; VF-1S Strike Valkyrie; Queadluun-Rau; Nousjadeul-Ger; Regult.
        - Variable Fighter / Valkyrie (mecha); modos Fighter / GERWALK / Battroid — NUNCA traduzir.
        - PROIBIDO lexico Robotech (Veritech etc.).

        === Regras / tom ===
        - Musica Minmay = doutrina cultural / comunicacao — nao "OST" / fundo.
        - Tom: epico-romantico; frases dramaticas e letras cantaveis em PT natural;
          Hikaru impulsivo; Minmay idol; Misa militar/romantica; Max/Milia parceiros de combate.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross: Do You Remember Love?", LORE);

    @Override
    public String getId() {
        return "macross_filme1";
    }

    @Override
    public String getNomeExibicao() {
        return "Macross: Do You Remember Love?";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege elenco DYRL e termos clássicos Macross.
     *
     * <p>INVARIANTES DO DOMÍNIO: Protoculture / Meltrandi / Zentradi oficiais.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Hikaru Ichijyo", "Lynn Minmay", "Misa Hayase", "Roy Focker",
            "Maximilian Jenius", "Max Jenius", "Milia Fallyna", "Exsedol Folmo",
            "Britai Kridanik", "Boddole Zer", "Claudia LaSalle",
            "SDF-1", "Macross", "Zentradi", "Meltrandi", "Protoculture",
            "Minmay Attack", "Valkyrie", "VF-1", "Strike Valkyrie",
            "Queadluun-Rau", "Nousjadeul-Ger", "GERWALK", "Battroid",
            "Overtechnology", "UN Spacy", "Variable Fighter"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico DYRL (Valkyrie/Protoculture/Meltrandi).
     *
     * <p>INVARIANTES DO DOMÍNIO: {@link CorrecoesTerminologiaMacrossDyrl}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacrossDyrl.mapa();
    }
}
