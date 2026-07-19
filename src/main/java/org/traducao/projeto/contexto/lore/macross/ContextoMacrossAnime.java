package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore clássica de Macross (cânone JP — tríade música/mecha/geopolítica).
 *
 * <p>INVARIANTES DO DOMÍNIO: Valkyrie/VF; Fighter/GERWALK/Battroid; Overtechnology;
 * Deculture; Zentradi; proibir léxico Robotech (Veritech).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
 */
@Component
public class ContextoMacrossAnime implements ProvedorContexto {

    private static final String LORE = """
        - Obra: The Super Dimension Fortress Macross (serie TV classica) — CANONE JAPONES.
        - PROIBIDO: lexico Robotech americano (Veritech, etc.). Use apenas termos Macross.

        === Premissa ===
        - A nave SDF-1 Macross (ex-ASS-1 alienigena) e a humanidade entram em guerra contra
          os gigantes Zentradi; cultura, musica e sentimentos humanos mudam o conflito.

        === Variable Fighter / Valkyrie (transformacao) ===
        - Variable Fighter (VF) ou Valkyrie: caca transformavel. Manter Valkyrie / Variable Fighter.
        - Tres modos (grafia estavel — NUNCA traduzir os nomes dos modos):
          * Fighter Mode — caca/jato aeronautico.
          * GERWALK Mode (ou Gerwalk) — hibrido: caca com pernas/bracos, vetorizacao de empuxo.
          * Battroid Mode — mecha humanoide completo.
        - Destroid: mecha nao-transformavel de apoio terrestre. Manter Destroid.

        === Overtechnology ===
        - Overtechnology: engenharia reversa da tecnologia alienigena (ASS-1 → SDF-1).
        - Reaction Weaponry / Armas de Reacao: termo tecnico; nao reduzir a "explosivo generico".
        - Macross Cannon, Fold, Space War I: manter formas oficiais.

        === Cultura como arma ===
        - Zentradi / Meltrandi (Meltran): gigantes militares; muitos nao conhecem cultura/amor.
        - Deculture (e choque cultural associado): expressao de choque alienigena perante cultura humana.
          Musica NAO e "fundo musical" — atua como interferencia psicologica real na guerra.
        - Protoculture / Protocultura: termo de lore; pode aparecer em PT como Protocultura.
        - Minmay Attack (em continuidades posteriores): doutrina de cancao como arma — manter nome.

        === Pessoas e mecha ===
        - Hikaru Ichijyo, Lynn Minmay, Misa Hayase, Roy Focker, Maximilian Jenius / Max Jenius,
          Milia Fallyna, Bruno J. Global, Claudia LaSalle, Exsedol Folmo, Boddole Zer, Kamjin Kravshera.
        - VF-1 Valkyrie (VF-1S/J/D), Monster, Tomahawk, Defender, Queadluun-Rau, Regult, SDF-1.

        === Tom ===
        - Opera espacial, romance triangular, idol e guerra; letras cantaveis sem notas editoriais.
        """;

    private static final String PROMPT = ContextoPrompt.montar(
        "The Super Dimension Fortress Macross", LORE);

    @Override
    public String getId() {
        return "macross_anime";
    }

    @Override
    public String getNomeExibicao() {
        return "The Super Dimension Fortress Macross";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Hikaru Ichijyo", "Lynn Minmay", "Misa Hayase", "Roy Focker",
            "Valkyrie", "Variable Fighter", "GERWALK", "Battroid", "Fighter Mode",
            "Overtechnology", "Zentradi", "Deculture", "SDF-1", "Protoculture"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: restaura grafias oficiais Macross (Valkyrie/Zentradi) quando
     * o LLM localiza indevidamente — mapa compartilhado da franquia.
     *
     * <p>INVARIANTES DO DOMÍNIO: só aplica com canônico no original EN.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public java.util.Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacross.mapa();
    }

}
