package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Component;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;

import java.util.List;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: recupera traduções BOAS que o pipeline descartava por um detalhe de
 * formatação. O LLM traduz corretamente mas frequentemente não repete o marcador de controle
 * {@code [[TAGn]]} — devolve {@code "Uma ponte, é?"} para {@code "[[TAG0]]A bridge, huh?"} —
 * e o texto útil era jogado fora, mantendo o inglês na legenda. Na corrida de 2026-07-22 isso
 * respondeu por 393 das 412 falas perdidas (95%), quase todas com tradução aproveitável.
 * Esta classe repõe o marcador quando isso é DETERMINÍSTICO, sem afrouxar a validação.
 *
 * <p>INVARIANTES DO DOMÍNIO: o reparo nunca enfraquece a guarda — toda candidata é submetida
 * ao MESMO {@link MascaradorTags#marcadoresPreservados} estrito antes de ser devolvida, de
 * modo que o reparador só consegue produzir textos que a regra original já aceitaria. Só
 * duas transformações são permitidas, ambas sem ambiguidade: (a) normalizar variantes
 * sintáticas do mesmo marcador ({@code [ TAG0 ]}, {@code [TAG0]}) para a forma canônica;
 * (b) repor marcadores que no original ocupam EXCLUSIVAMENTE as bordas (prefixo e/ou
 * sufixo), preservando ordem e conteúdo. Marcador interno perdido, marcador inventado onde o
 * original não tinha, e duplicação continuam REPROVADOS: a posição de um marcador no meio da
 * frase não é recuperável sem adivinhar onde o tradutor a colocaria.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@link Optional#empty()} em todo caso não
 * determinístico — incluindo argumento nulo, tentativa em branco e original sem texto visível
 * — deixando o chamador seguir pelo caminho de rejeição já existente (retry e, esgotado,
 * pendência com causa registrada). Não lança e não altera estado.
 */
@Component
public class ReparadorMarcadoresLlm {

    /** Forma canônica gerada por {@link MascaradorTags#mascarar}: {@code [[TAG12]]}. */
    private static final Pattern MARCADOR_CANONICO = Pattern.compile("\\[\\[TAG(\\d+)]]");

    /**
     * Variantes que o LLM produz para o MESMO marcador — colchete simples e/ou espaços
     * internos ({@code [TAG0]}, {@code [ TAG0 ]}, {@code [[ TAG0 ]]}). Só a grafia muda; o
     * índice é preservado, então a normalização não inventa nem move nada.
     */
    private static final Pattern MARCADOR_VARIANTE = Pattern.compile("\\[\\s*\\[?\\s*TAG\\s*(\\d+)\\s*]?\\s*]");

    private final MascaradorTags mascarador;

    /**
     * PROPÓSITO DE NEGÓCIO: injeta a regra de qualidade compartilhada que decide se os
     * marcadores foram preservados, para que o reparador valide pela MESMA régua do pipeline.
     *
     * <p>INVARIANTES DO DOMÍNIO: o reparador não reimplementa a checagem de marcadores; ele
     * a consome do peer {@code qualidadeTraducao}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede a criação do componente
     * pelo contêiner.
     */
    public ReparadorMarcadoresLlm(MascaradorTags mascarador) {
        this.mascarador = mascarador;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: tenta transformar uma resposta do LLM com marcadores corrompidos
     * numa resposta íntegra, aproveitando a tradução em vez de descartá-la.
     *
     * <p>INVARIANTES DO DOMÍNIO: o texto devolvido, quando presente, SEMPRE passa em
     * {@link MascaradorTags#marcadoresPreservados} contra {@code mascaradoOriginal}; o texto
     * traduzido em si nunca é editado, apenas envolvido pelos marcadores de borda do original.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@link Optional#empty()} sempre que o reparo não for
     * determinístico (marcador interno, inventado, duplicado, tentativa em branco ou
     * argumento nulo). Não lança.
     *
     * @param mascaradoOriginal fala original já mascarada, fonte da verdade dos marcadores
     * @param tentativa resposta bruta do LLM, possivelmente sem os marcadores
     * @return a resposta reparada, ou vazio quando o reparo seria uma adivinhação
     */
    public Optional<String> reparar(String mascaradoOriginal, String tentativa) {
        if (mascaradoOriginal == null || tentativa == null || tentativa.isBlank()) {
            return Optional.empty();
        }

        // (a) Variante sintática: o modelo escreveu o marcador certo com a grafia errada.
        String normalizada = normalizarVariantes(tentativa);
        if (mascarador.marcadoresPreservados(mascaradoOriginal, normalizada)) {
            return Optional.of(normalizada);
        }

        // Sobrou marcador na tentativa e ainda assim divergente => inventado/duplicado/
        // faltando um de vários. Nada aqui é determinístico: rejeita.
        if (MARCADOR_CANONICO.matcher(normalizada).find()) {
            return Optional.empty();
        }

        // (b) Tentativa sem nenhum marcador: só recuperável se o original os tiver apenas
        // nas bordas, onde a posição é conhecida sem adivinhação.
        return reconstruirBordas(mascaradoOriginal, normalizada);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: uniformiza a grafia dos marcadores devolvidos pelo LLM, para que
     * uma diferença puramente tipográfica não custe a fala inteira.
     *
     * <p>INVARIANTES DO DOMÍNIO: só a grafia do marcador muda; o índice é copiado
     * literalmente e o texto ao redor é preservado byte a byte.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto sem nenhuma variante volta idêntico; não lança.
     */
    private static String normalizarVariantes(String tentativa) {
        Matcher matcher = MARCADOR_VARIANTE.matcher(tentativa);
        StringBuilder resultado = new StringBuilder();
        int ultimoFim = 0;
        while (matcher.find()) {
            resultado.append(tentativa, ultimoFim, matcher.start());
            resultado.append("[[TAG").append(matcher.group(1)).append("]]");
            ultimoFim = matcher.end();
        }
        resultado.append(tentativa, ultimoFim, tentativa.length());
        return resultado.toString();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: repõe os marcadores de borda em volta da tradução — o caso
     * dominante na prática, porque a maioria das falas é envolvida por tags de itálico ou
     * posicionamento ({@code {\i1}texto{\i}}) que abrem e fecham a linha inteira.
     *
     * <p>INVARIANTES DO DOMÍNIO: só reconstrói quando TODOS os marcadores do original estão
     * na corrida de prefixo ou na de sufixo; um único marcador interno (ex.: a quebra
     * {@code \N} no meio da frase) reprova, porque a tradução pode reordenar as orações e o
     * ponto de quebra deixaria de corresponder. Exige texto visível entre as bordas e
     * resultado DIFERENTE do original — reparar um eco devolveria o inglês como se fosse
     * tradução e ainda consumiria o retry que poderia produzir a tradução de verdade.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@link Optional#empty()} para marcador interno,
     * original sem marcadores, original sem texto visível, eco do texto de origem, ou se a
     * candidata reconstruída não passar na verificação estrita (guarda defensiva). Não lança.
     */
    private Optional<String> reconstruirBordas(String mascaradoOriginal, String tentativaSemMarcador) {
        List<MatchResult> marcadores = MARCADOR_CANONICO.matcher(mascaradoOriginal).results().toList();
        if (marcadores.isEmpty()) {
            return Optional.empty();
        }

        // Corrida de prefixo: marcadores colados no início, um após o outro.
        int consumidosNoPrefixo = 0;
        int fimDoPrefixo = 0;
        while (consumidosNoPrefixo < marcadores.size()
                && marcadores.get(consumidosNoPrefixo).start() == fimDoPrefixo) {
            fimDoPrefixo = marcadores.get(consumidosNoPrefixo).end();
            consumidosNoPrefixo++;
        }

        // Corrida de sufixo: marcadores colados no fim, andando de trás para frente.
        int restantes = marcadores.size();
        int inicioDoSufixo = mascaradoOriginal.length();
        while (restantes > consumidosNoPrefixo && marcadores.get(restantes - 1).end() == inicioDoSufixo) {
            inicioDoSufixo = marcadores.get(restantes - 1).start();
            restantes--;
        }

        if (restantes != consumidosNoPrefixo) {
            return Optional.empty(); // sobrou marcador no meio: posição não recuperável
        }
        if (mascaradoOriginal.substring(fimDoPrefixo, inicioDoSufixo).isBlank()) {
            return Optional.empty(); // original é só formatação: não havia o que traduzir
        }

        String reconstruida = mascaradoOriginal.substring(0, fimDoPrefixo)
            + tentativaSemMarcador
            + mascaradoOriginal.substring(inicioDoSufixo);

        // Eco: o modelo devolveu o próprio texto de origem, só que sem os marcadores.
        // Reparar aqui produziria uma "tradução" idêntica ao original e ainda ROUBARIA o
        // retry (que é a chance real de obter tradução de verdade). Deixa reprovar.
        if (reconstruida.equals(mascaradoOriginal)) {
            return Optional.empty();
        }

        return mascarador.marcadoresPreservados(mascaradoOriginal, reconstruida)
            ? Optional.of(reconstruida)
            : Optional.empty();
    }
}
