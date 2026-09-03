package org.traducao.projeto.lore.domain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PROPÓSITO DE NEGÓCIO: monta o prompt de sistema completo de tradução a partir da
 * lore de cada obra (juntando prioridades, {@link RegrasConcordanciaPtBr} e regras de
 * saída) e mantém, por trás de cada prompt, a lore "crua" correspondente — para que
 * usos pontuais (ex.: revisão de concordância) recuperem só a lore sem reenviar o
 * prompt inteiro ao LLM.
 *
 * <p>INVARIANTES DO DOMÍNIO: utilitário de domínio autocontido — sem I/O, sem
 * configuração e sem dependência funcional externa —, porém com estado estático
 * interno ({@code LORE_POR_PROMPT}, um cache prompt→lore). A ordem/estrutura do
 * template e o mapeamento prompt→lore fazem parte do contrato e não podem mudar sem
 * quebrar a recuperação da lore. Classe final, construtor privado.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: {@link #obterLore(String)} devolve o próprio
 * argumento quando o prompt não foi registrado (nunca lança); {@link #montar(String,
 * String)} propaga {@code NullPointerException} se {@code lore} for nulo (via
 * {@code strip()}).
 */
public final class ContextoPrompt {

    // Cada ContextoXxx monta seu PROMPT uma unica vez (campo static final), na
    // inicializacao da classe; este mapa guarda a lore "crua" por traz de cada
    // prompt completo para que outros usos (ex.: revisao de concordancia) nao
    // precisem reenviar o prompt de traducao inteiro - que ja inclui lore +
    // RegrasConcordanciaPtBr.BLOCO_TRADUCAO + regras de saida - como se fosse
    // so a lore, o que estourava o contexto do LLM (ver LlmClientAdapter).
    private static final Map<String, String> LORE_POR_PROMPT = new ConcurrentHashMap<>();

    private ContextoPrompt() {
    }

    public static String obterLore(String promptCompleto) {
        return LORE_POR_PROMPT.getOrDefault(promptCompleto, promptCompleto);
    }

    public static String montar(String obra, String lore) {
        String loreLimpa = lore.strip();
        String prompt = """
            Você é um tradutor especializado em legendas de anime, traduzindo do inglês para português do Brasil.
            Contexto ativo da obra: %s.

            Prioridades de tradução:
            - Preserve sentido, subtexto, intenção emocional e continuidade da cena.
            - Use português brasileiro natural, fluido e adequado à legenda, sem ficar literal quando isso soar estranho.
            - Mantenha nomes próprios, nomes de mecha, naves, facções, cidades, organizações, patentes e codinomes conforme a lore abaixo.
            - Não invente explicações, notas, parênteses editoriais ou glossários na resposta.
            - Preserve honoríficos japoneses somente quando vierem no texto original ou forem parte clara da relação entre personagens.
            - Em falas militares, use tom objetivo e terminologia consistente: unidade, esquadrão, frota, comandante, tenente, capitão/capitão apenas quando o original indicar rank equivalente.

            Lore e terminologia obrigatória:
            %s

            %s

            Regras obrigatórias de saída:
            1. Responda APENAS com a tradução, sem comentários, sem preâmbulo e sem repetir o texto original.
            2. Traduza cada linha individualmente e devolva exatamente o mesmo número de linhas recebidas, na mesma ordem, uma tradução por linha, sem numerar.
            3. Marcadores no formato [[TAG0]], [[TAG1]] etc. DEVEM ser copiados exatamente como estão para a tradução, na mesma posição. NÃO remova e não traduza esses marcadores.
            4. Preserve quebras internas, pontuação dramática essencial, reticências e ênfase quando forem importantes para timing e atuação.
            5. Não traduza comandos de formatação, tags ASS/SSA mascaradas, nomes de arquivos, créditos técnicos, karaoke ou textos decorativos quando eles estiverem claramente fora da fala narrativa.
            6. Traduza palavrões, xingamentos e linguagem chula fielmente e por extenso, mantendo o peso do original. NUNCA censure com asteriscos (ex.: "p***", "*****") e NÃO use marcação markdown (*, **, _, __) em nenhuma parte da resposta.
            """.formatted(obra, loreLimpa, RegrasConcordanciaPtBr.BLOCO_TRADUCAO.strip());
        LORE_POR_PROMPT.put(prompt, loreLimpa);
        return prompt;
    }
}
