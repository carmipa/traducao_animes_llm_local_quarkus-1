package org.traducao.projeto.lore.domain;

/**
 * PROPÓSITO DE NEGÓCIO: bloco fixo de regras de concordância de gênero, pronomes,
 * tratamentos e verbos, aplicável a qualquer obra — o inglês não marca gênero em
 * adjetivos/particípios e usa "you" genérico, o que leva o LLM a masculinizar tudo.
 * É injetado no prompt de tradução ({@link ContextoPrompt#montar}) e reaproveitado
 * no prompt de revisão de concordância.
 *
 * <p>INVARIANTES DO DOMÍNIO: constantes/textos imutáveis; {@code BLOCO_TRADUCAO} e o
 * template de {@link #montarPromptRevisao(String)} são conteúdo de negócio congelado
 * — espaçamento, quebras de linha e pontuação fazem parte do contrato do prompt e não
 * podem ser reformatados. Classe final, construtor privado, sem estado mutável.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: {@link #montarPromptRevisao(String)} trata
 * {@code null}/branco como "(sem lore adicional)" e nunca lança.
 */
public final class RegrasConcordanciaPtBr {

    private RegrasConcordanciaPtBr() {
    }

    public static final String BLOCO_TRADUCAO = """
        Concordância de gênero, pronomes, tratamentos e verbos (obrigatório em TODAS as falas):
        - O inglês não marca gênero em adjetivos/particípios; infira pelo falante, interlocutor e personagens citados.
        - Nunca use masculino como fallback automático quando o inglês não indicar gênero. Se não houver certeza, prefira formulações neutras naturais.
          Exemplos: "I'm tired" -> "Estou com cansaço" ou "Estou exausta/exausto" conforme falante; "Are you ready?" -> "Tudo pronto?" quando o gênero do interlocutor for incerto.
        - Para 1a/2a pessoa sem gênero claro, prefira reescrever sem adjetivo marcado:
          "I'm tired of this" -> "Não aguento mais isso"; "I'm scared" -> "Estou com medo";
          "I'm worried" -> "Isso me preocupa"; "Are you hurt?" -> "Se machucou?";
          "You're crazy" -> "Você perdeu o juízo?"; "I'm ready" -> "Estou com tudo pronto".
        - Artigos: o/a, um/uma, do/da, no/na, ao/a — concordem com o substantivo referido.
        - Pronomes pessoais: ele/ela, dele/dela, nele/nela, com ele/com ela, para ele/para ela.
        - Pronomes possessivos (seu/sua/seus/suas) concordam com o objeto possuído; quando ambíguo, prefira "dele/dela" para deixar claro.
        - "Dele/dela" indicam o possuidor, não o gênero do objeto possuído: "irmão dela" e "filha dele" podem estar corretos.
        - Particípios e adjetivos predicativos concordam com o sujeito: "Ela está pronta", "Ele está pronto", "Estou cansada" (falante mulher).
        - Adjetivos invariáveis não mudam por gênero: feliz, triste, grande, jovem, forte, gentil etc.
        - Verbos na 3a pessoa: "ela disse", "ele foi", "elas estão", "eles estão" — nunca inverta she->ele nem he->ela.
        - Objeto direto/indireto: "I saw her" -> "Eu a vi" / "Eu vi ela"; "Tell him" -> "Diga a ele"; não troque him/her.
        - Tratamentos e vocativos: senhor/senhora, moço/moça, garoto/garota, rapaz/menina, cara/moça — respeite o gênero de quem fala ou de quem é tratado.
        - "You" falando com mulher pode ser "você" (neutro) ou formas femininas quando o tom for íntimo ou claramente feminino; não masculinize a interlocutora.
        - Substantivos femininos (garota, deusa, princesa, aventureira, irmã, mãe...) exigem artigos/adjetivos femininos; masculinos (garoto, rei, herói, irmão, pai...) exigem masculinos.
        - Não padronize tudo no masculino por ser "padrão genérico" em português; legendas exigem precisão de gênero.
        - Preserve nomes próprios, termos de lore e marcadores [[TAGn]] sem alterar gênero de nomes estrangeiros.
        """;

    public static String montarPromptRevisao(String loreObra) {
        String lore = loreObra != null && !loreObra.isBlank() ? loreObra.strip() : "(sem lore adicional)";
        return """
            Você é revisor de legendas em português do Brasil. Corrija APENAS gênero e concordância.
            - Em falas ambíguas no masculino, use feminino se a lore indicar mulher, ou use neutro natural em PT-BR.
            - Não use masculino como fallback automático. Preserve marcadores [[TAGn]] e nomes próprios.

            Lore da obra:
            %s

            Responda APENAS com a fala corrigida em uma única linha, sem aspas ou explicações.
            """.formatted(lore);
    }
}
