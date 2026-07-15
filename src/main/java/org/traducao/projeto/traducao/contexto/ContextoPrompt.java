package org.traducao.projeto.traducao.contexto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ContextoPrompt {

    // Cada ContextoXxx monta seu PROMPT uma unica vez (campo static final), na
    // inicializacao da classe; este mapa guarda a lore "crua" por traz de cada
    // prompt completo para que outros usos (ex.: revisao de concordancia) nao
    // precisem reenviar o prompt de traducao inteiro - que ja inclui lore +
    // RegrasConcordanciaPtBr.BLOCO_TRADUCAO + regras de saida - como se fosse
    // so a lore, o que estourava o contexto do LLM (ver MistralClientAdapter).
    private static final Map<String, String> LORE_POR_PROMPT = new ConcurrentHashMap<>();

    private ContextoPrompt() {
    }

    public static String obterLore(String promptCompleto) {
        return LORE_POR_PROMPT.getOrDefault(promptCompleto, promptCompleto);
    }

    public static String montar(String obra, String lore) {
        String loreLimpa = lore.strip();
        String prompt = """
            Voce e um tradutor especializado em legendas de anime, traduzindo do ingles para portugues do Brasil.
            Contexto ativo da obra: %s.

            Prioridades de traducao:
            - Preserve sentido, subtexto, intencao emocional e continuidade da cena.
            - Use portugues brasileiro natural, fluido e adequado a legenda, sem ficar literal quando isso soar estranho.
            - Mantenha nomes proprios, nomes de mecha, naves, faccoes, cidades, organizacoes, patentes e codinomes conforme a lore abaixo.
            - Nao invente explicacoes, notas, parenteses editoriais ou glossarios na resposta.
            - Preserve honorificos japoneses somente quando vierem no texto original ou forem parte clara da relacao entre personagens.
            - Em falas militares, use tom objetivo e terminologia consistente: unidade, esquadrao, frota, comandante, tenente, capitão/capitao apenas quando o original indicar rank equivalente.

            Lore e terminologia obrigatoria:
            %s

            %s

            Regras obrigatorias de saida:
            1. Responda APENAS com a traducao, sem comentarios, sem preambulo e sem repetir o texto original.
            2. Traduza cada linha individualmente e devolva exatamente o mesmo numero de linhas recebidas, na mesma ordem, uma traducao por linha, sem numerar.
            3. Marcadores no formato [[TAG0]], [[TAG1]] etc. DEVEM ser copiados exatamente como estao para a traducao, na mesma posicao. NAO remova e nao traduza esses marcadores.
            4. Preserve quebras internas, pontuacao dramatica essencial, reticencias e enfase quando forem importantes para timing e atuacao.
            5. Nao traduza comandos de formatação, tags ASS/SSA mascaradas, nomes de arquivos, creditos tecnicos, karaoke ou textos decorativos quando eles estiverem claramente fora da fala narrativa.
            """.formatted(obra, loreLimpa, RegrasConcordanciaPtBr.BLOCO_TRADUCAO.strip());
        LORE_POR_PROMPT.put(prompt, loreLimpa);
        return prompt;
    }
}
