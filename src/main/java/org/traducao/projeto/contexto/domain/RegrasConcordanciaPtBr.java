package org.traducao.projeto.contexto.domain;

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
        Concordancia de genero, pronomes, tratamentos e verbos (obrigatorio em TODAS as falas):
        - O ingles nao marca genero em adjetivos/participios; infira pelo falante, interlocutor e personagens citados.
        - Nunca use masculino como fallback automatico quando o ingles nao indicar genero. Se nao houver certeza, prefira formulacoes neutras naturais.
          Exemplos: "I'm tired" -> "Estou com cansaco" ou "Estou exausta/exausto" conforme falante; "Are you ready?" -> "Tudo pronto?" quando o genero do interlocutor for incerto.
        - Para 1a/2a pessoa sem genero claro, prefira reescrever sem adjetivo marcado:
          "I'm tired of this" -> "Nao aguento mais isso"; "I'm scared" -> "Estou com medo";
          "I'm worried" -> "Isso me preocupa"; "Are you hurt?" -> "Se machucou?";
          "You're crazy" -> "Voce perdeu o juizo?"; "I'm ready" -> "Estou com tudo pronto".
        - Artigos: o/a, um/uma, do/da, no/na, ao/a — concordem com o substantivo referido.
        - Pronomes pessoais: ele/ela, dele/dela, nele/nela, com ele/com ela, para ele/para ela.
        - Pronomes possessivos (seu/sua/seus/suas) concordam com o objeto possuido; quando ambiguo, prefira "dele/dela" para deixar claro.
        - "Dele/dela" indicam o possuidor, nao o genero do objeto possuido: "irmao dela" e "filha dele" podem estar corretos.
        - Participios e adjetivos predicativos concordam com o sujeito: "Ela esta pronta", "Ele esta pronto", "Estou cansada" (falante mulher).
        - Adjetivos invariaveis nao mudam por genero: feliz, triste, grande, jovem, forte, gentil etc.
        - Verbos na 3a pessoa: "ela disse", "ele foi", "elas estao", "eles estao" — nunca inverta she->ele nem he->ela.
        - Objeto direto/indireto: "I saw her" -> "Eu a vi" / "Eu vi ela"; "Tell him" -> "Diga a ele"; nao troque him/her.
        - Tratamentos e vocativos: senhor/senhora, moço/moça, garoto/garota, rapaz/menina, cara/moça — respeite o genero de quem fala ou de quem e tratado.
        - "You" falando com mulher pode ser "voce" (neutro) ou formas femininas quando o tom for intimo ou claramente feminino; nao masculinize a interlocutora.
        - Substantivos femininos (garota, deusa, princesa, aventureira, irma, mae...) exigem artigos/adjetivos femininos; masculinos (garoto, rei, heroi, irmao, pai...) exigem masculinos.
        - Nao padronize tudo no masculino por ser "padrao generico" em portugues; legendas exigem precisao de genero.
        - Preserve nomes proprios, termos de lore e marcadores [[TAGn]] sem alterar genero de nomes estrangeiros.
        """;

    public static String montarPromptRevisao(String loreObra) {
        String lore = loreObra != null && !loreObra.isBlank() ? loreObra.strip() : "(sem lore adicional)";
        return """
            Voce e revisor de legendas em portugues do Brasil. Corrija APENAS genero e concordancia.
            - Em falas ambiguas no masculino, use feminino se a lore indicar mulher, ou use neutro natural em PT-BR.
            - Nao use masculino como fallback automatico. Preserve marcadores [[TAGn]] e nomes proprios.

            Lore da obra:
            %s

            Responda APENAS com a fala corrigida em uma unica linha, sem aspas ou explicacoes.
            """.formatted(lore);
    }
}
