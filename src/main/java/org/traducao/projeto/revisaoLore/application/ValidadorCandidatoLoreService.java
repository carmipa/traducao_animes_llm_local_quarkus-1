package org.traducao.projeto.revisaoLore.application;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: impede que a revisão de lore use uma suspeita
 * terminológica como autorização para retraduzir ou reescrever toda a fala.
 *
 * <p>INVARIANTES DO DOMÍNIO: uma alteração automática deve ser pequena e o
 * trecho canônico introduzido precisa existir tanto no original inglês quanto
 * na lore ativa; texto comum fora desse recorte permanece intocado.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: devolve o motivo da rejeição e o chamador
 * mantém integralmente a legenda PT-BR anterior.
 */
final class ValidadorCandidatoLoreService {

    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+(?:[-.'&’][\\p{L}\\p{N}]+)*");
    private static final int MAX_TOKENS_ALTERADOS = 4;

    /**
     * Resto de transporte que NUNCA pode chegar à legenda entregue: colchete duplo alucinado
     * ({@code [[palavras]]}), negrito de markdown e token de template de chat.
     *
     * <p>Nada disso é texto de legenda — é encanamento do pipeline e do modelo. A tokenização
     * desta classe olha PALAVRAS e por isso é cega a eles: {@code "[[Anti Bodies]]"} tokeniza
     * igual a {@code "Anti Bodies"} e passava pela validação inteira.
     *
     * <p>O prejuízo, medido em 18/08/2026 na primeira corrida em que a tela escreveu de verdade:
     * o Guilty Crown ep07 recebeu {@code "É só questão de tempo até que as [[Anti Bodies]] sejam
     * retiradas."} — os colchetes apareceriam na tela do espectador. Uma linha em 128 mil
     * entregues, achada varrendo o acervo depois da escrita.
     *
     * <p><b>EXCEÇÃO ESTREITA — o sentinela LEGÍTIMO {@code [[TAG<número>]]} não é resíduo.</b>
     * {@link #validar} é chamado com a proposta MASCARADA (RevisarLoreUseCase:802-803), porque o
     * diff de tokens exige a atual e a proposta na mesma régua — a quebra {@code \\N} e as tags da
     * fala viram {@code [[TAGn]]} nos dois lados. Esse marcador é sempre desmascarado ANTES de
     * gravar e nunca chega à legenda; barrá-lo aqui reprovava a correção CERTA do LLM em toda fala
     * com tag ou {@code \\N} (~24% do acervo), inflando {@code descartadas}/{@code pendentes} com um
     * diagnóstico falso ("resíduo {@code [[TAG0]]}"). A alucinação {@code [[Anti Bodies]]} não casa
     * {@code [[TAG<número>]]} e continua barrada; um {@code [[TAGword]]} mal-formado também. Um
     * marcador com índice inexistente já teria sido barrado antes, na desmascaração
     * ({@code MarcadorPerdidoException}), então o que sobra mascarado aqui corresponde a tag real.
     */
    private static final Pattern RESIDUO_DE_TRANSPORTE =
        Pattern.compile("\\[\\[(?!TAG\\d+\\]\\])[^\\]]*\\]\\]|\\*\\*|__|<\\|[^|<>]{1,40}\\|>|```");

    private ValidadorCandidatoLoreService() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: confirma que a proposta modifica somente um termo
     * canônico comprovado pela fala original e pelo arquivo de lore escolhido.
     *
     * <p>INVARIANTES DO DOMÍNIO: ignora caixa e acentos para comparação, limita
     * o recorte alterado e exige a sequência nova integral nas duas fontes.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorna um diagnóstico; retorna vazio
     * exclusivamente quando a proposta está dentro do escopo de lore.
     */
    static Optional<String> validar(
        String originalIngles,
        String traducaoAtual,
        String proposta,
        String loreCanonica
    ) {
        // ANTES de qualquer analise de termo: resto de transporte nunca vai para a legenda.
        // A tokenizacao abaixo IGNORA colchetes e asteriscos — ela olha palavras —, entao um
        // "[[Anti Bodies]]" atravessava a validacao inteira como se fosse "Anti Bodies" e era
        // GRAVADO com os colchetes. Medido em 18/08/2026: uma fala do Guilty Crown ep07 foi
        // entregue assim ("É só questão de tempo até que as [[Anti Bodies]] sejam retiradas"),
        // e o espectador leria os colchetes na tela.
        Matcher residuo = RESIDUO_DE_TRANSPORTE.matcher(proposta);
        if (residuo.find()) {
            return Optional.of("proposta traz residuo de transporte na legenda: \""
                + residuo.group() + "\"");
        }

        List<String> atual = tokenizar(traducaoAtual);
        List<String> nova = tokenizar(proposta);
        if (atual.equals(nova)) {
            return Optional.of("proposta sem alteração terminológica verificável");
        }

        int prefixo = prefixoComum(atual, nova);
        int sufixo = sufixoComum(atual, nova, prefixo);
        List<String> removidos = atual.subList(prefixo, atual.size() - sufixo);
        List<String> inseridos = nova.subList(prefixo, nova.size() - sufixo);

        if (inseridos.isEmpty()) {
            return Optional.of("proposta apenas remove conteúdo da fala");
        }
        // Correção de termo PRESERVA o contexto ao redor (prefixo ou sufixo comum). Uma proposta
        // que troca a fala INTEIRA por um trecho MENOR (nada preservado E mais curta) é truncamento,
        // não correção de lore — é exatamente o que esta classe existe para impedir ("usar suspeita
        // como autorização para reescrever toda a fala"). Ex.: "A Legião chegou" (3 tokens) → "Legion"
        // (1) passava pelo teto de 4 tokens, o termo existe no EN e na lore, e a fala era APAGADA.
        // Fala de UMA palavra só (o próprio termo, ex.: um cartaz "Legião") pode ser trocada inteira,
        // por isso a exigência atual.size() > 1; substituição de mesmo tamanho ("Robô Móvel" →
        // "Mobile Suit", 2→2) não é truncamento e continua passando.
        if (atual.size() > 1 && prefixo == 0 && sufixo == 0 && inseridos.size() < removidos.size()) {
            return Optional.of("proposta substitui a fala inteira por um trecho menor — "
                + "truncamento, nao correcao pontual de termo");
        }
        if (removidos.size() > MAX_TOKENS_ALTERADOS || inseridos.size() > MAX_TOKENS_ALTERADOS) {
            return Optional.of("proposta reescreve trecho amplo fora do escopo de lore");
        }

        List<String> original = tokenizar(originalIngles);
        List<String> lore = tokenizar(loreCanonica);
        if (!contemSequencia(original, inseridos)) {
            return Optional.of("termo proposto não existe no original inglês");
        }
        if (!contemSequencia(lore, inseridos)) {
            return Optional.of("termo proposto não está cadastrado na lore ativa");
        }
        return Optional.empty();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte frases em unidades comparáveis sem perder
     * termos compostos por hífen ou apóstrofo.
     * <p>INVARIANTES DO DOMÍNIO: caixa e diacríticos não alteram a identidade.
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo produz lista vazia.
     */
    private static List<String> tokenizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(texto);
        while (matcher.find()) {
            String semAcento = Normalizer.normalize(matcher.group(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
            tokens.add(semAcento);
        }
        return List.copyOf(tokens);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: localiza o início do único recorte alterado.
     * <p>INVARIANTES DO DOMÍNIO: nunca avança além da menor lista.
     * <p>COMPORTAMENTO EM CASO DE FALHA: listas vazias resultam em zero.
     */
    private static int prefixoComum(List<String> atual, List<String> nova) {
        int limite = Math.min(atual.size(), nova.size());
        int indice = 0;
        while (indice < limite && atual.get(indice).equals(nova.get(indice))) {
            indice++;
        }
        return indice;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fecha o recorte alterado preservando o final comum.
     * <p>INVARIANTES DO DOMÍNIO: não sobrepõe o prefixo já identificado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência de sufixo comum retorna zero.
     */
    private static int sufixoComum(List<String> atual, List<String> nova, int prefixo) {
        int limite = Math.min(atual.size(), nova.size()) - prefixo;
        int quantidade = 0;
        while (quantidade < limite
            && atual.get(atual.size() - 1 - quantidade).equals(nova.get(nova.size() - 1 - quantidade))) {
            quantidade++;
        }
        return quantidade;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: comprova a presença integral e ordenada de um termo
     * canônico numa fonte confiável.
     * <p>INVARIANTES DO DOMÍNIO: a sequência precisa ser contígua e não vazia.
     * <p>COMPORTAMENTO EM CASO DE FALHA: entradas insuficientes retornam falso.
     */
    private static boolean contemSequencia(List<String> fonte, List<String> trecho) {
        if (trecho.isEmpty() || fonte.size() < trecho.size()) {
            return false;
        }
        for (int i = 0; i <= fonte.size() - trecho.size(); i++) {
            if (fonte.subList(i, i + trecho.size()).equals(trecho)) {
                return true;
            }
        }
        return false;
    }
}
