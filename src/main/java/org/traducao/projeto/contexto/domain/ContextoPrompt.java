package org.traducao.projeto.contexto.domain;

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
