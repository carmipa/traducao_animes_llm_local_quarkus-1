package org.traducao.projeto.traducaoKaraoke.domain;

/**
 * Classificação de cada evento da legenda sob a ótica da tradução de letras
 * de música. O ponto delicado é a letra japonesa romanizada com palavras em
 * inglês misturadas (comum em música japonesa: "kimi no heart ni fly away") —
 * ela é ORIGINAL_JAPONES e nunca pode ir ao LLM, enquanto a camada de
 * TRADUÇÃO em inglês da mesma música é TRADUZIVEL_INGLES.
 */
public enum ClasseLinhaKaraoke {

    /** Não é música: diálogo, placa, Comment. Copiada byte a byte. */
    FORA_DE_MUSICA("fora de música"),

    /** Efeito KFX (sílaba/frame, alta densidade de tags). Preservado intacto. */
    EFEITO_KFX("efeito KFX preservado"),

    /** Letra original (kana/kanji/romaji, mesmo com inglês misturado). Nunca traduz. */
    ORIGINAL_JAPONES("letra original JP/romaji"),

    /** Letra que já está em português. Nada a fazer. */
    JA_PORTUGUES("letra já em PT-BR"),

    /** Camada de tradução em inglês da música: é o que este módulo traduz. */
    TRADUZIVEL_INGLES("tradução EN → PT-BR");

    private final String rotulo;

    ClasseLinhaKaraoke(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
