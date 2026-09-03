package org.traducao.projeto.qualidadeTraducao.application;

import org.springframework.stereotype.Component;
import org.traducao.projeto.core.texto.FronteiraTermoAss;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: repõe o acento de {@code é} e {@code está} usando o ORIGINAL INGLÊS
 * como prova — os dois defeitos que Paulo leu na tela assistindo ao Gundam ZZ, e os únicos
 * dessa classe que nenhum dicionário consegue ver.
 *
 * <h2>Por que dicionário não resolve, e por que o inglês resolve</h2>
 * {@code e} e {@code esta} são palavras válidas em português. Nenhum corretor ortográfico as
 * acusa, porque não há nada de errado com elas — está errado o SENTIDO. O
 * {@link NormalizadorAcentosComuns} exclui esses homógrafos de propósito, e o Javadoc dele diz
 * por quê: sem contexto, trocar {@code e} por {@code é} introduz erro em toda coordenação.
 *
 * <p>O contexto que falta está no original. Uma legenda é tradução de uma frase inglesa
 * conhecida, e o inglês responde as duas perguntas de forma verificável:
 * <ul>
 *   <li><b>{@code e} → {@code é}</b>: o português não pode ter mais conjunção {@code e} do que
 *       o inglês tem coordenador. Se o original não traz {@code and}, {@code &}, vírgula,
 *       ponto-e-vírgula nem dois-pontos, <b>não há o que coordenar</b> — e todo {@code e} solto
 *       da tradução é o verbo {@code ser}.</li>
 *   <li><b>{@code esta} → {@code está}</b>: {@code esta} só é demonstrativo se o original
 *       trouxer um demonstrativo ({@code this}, {@code that}, {@code these}, {@code those}).
 *       Sem ele e com o verbo {@code to be} presente, {@code esta} é o verbo {@code estar}.</li>
 * </ul>
 *
 * <h2>Por que aqui, e não na fatia 3.3</h2>
 * A {@code RevisarConcordanciaUseCase} declara no próprio Javadoc que lê "o {@code .ass} PT-BR,
 * <b>sem inglês e sem cache</b>". Foi essa cegueira que obrigou o
 * {@code CorretorAcentoPorPadraoService} a decidir por LISTA FECHADA da palavra ANTERIOR —
 * {@code Isso|Isto|Essa|Esse|não|Você|Ela|Ele} — e é por isso que
 * <i>"também e motivo de preocupação"</i> passa: {@code também} não está na lista. A ironia está
 * escrita naquele mesmo arquivo, no padrão do {@code so}: <i>"Lista de palavra seguinte é frágil
 * por construção"</i>. A lição foi aprendida para o {@code so} e nunca aplicada ao {@code e}.
 *
 * <p>Este serviço mora no peer de QUALIDADE porque a mecânica é a mesma nas duas pontas: o
 * caminho de tradução tem o original em mãos ao gravar, e a tela 3.1 tem {@code originalEn} por
 * fala. A fatia decide QUANDO aplicar; a mecânica é uma só.
 *
 * <h2>O prejuízo que originou, medido em 03/09/2026</h2>
 * No acervo entregue (76.794 falas de diálogo, música fora, depois de a 3.3 já ter rodado em
 * agosto): <b>371 falas</b> com {@code e} onde cabe {@code é} e <b>195</b> com {@code esta} onde
 * cabe {@code está}. O ZZ sozinho tem 166. No episódio 1 do ZZ a 3.3 corrigiu 4 falas em 705.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li><b>Na dúvida, NÃO corrige.</b> Quando o inglês tem coordenador, qual {@code e} é o verbo
 *       vira adivinhação, e o serviço se abstém. Medido: a regra estrita alcança 350 das 371
 *       falas e recusa 21. Corretor que estraga texto certo é pior que corretor nenhum, e as 21
 *       ficam para a rota que consulta o LLM.</li>
 *   <li>Original ausente ou em branco NÃO autoriza palpite: devolve o texto intacto. É a mesma
 *       falha-fechada do resto do projeto — sem a prova, não há correção.</li>
 *   <li>Fronteira de termo pela {@link FronteiraTermoAss}: a quebra {@code \N} do ASS ocupa dois
 *       caracteres e o {@code N} é letra, então sem ela o {@code e} colado à quebra não é visto.
 *       Foi assim que 11 das 12 formas do dicionário sobreviveram no acervo em 04/08.</li>
 *   <li>Não toca em tag: o padrão exige fronteira de palavra, e {@code {\i1}} não a produz.</li>
 *   <li>Sem estado; só JDK e Spring. Não conhece cache, LLM nem legenda.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Texto ou original {@code null}/vazio devolve o texto como veio; nunca lança. Nenhuma troca
 * aplicável devolve a mesma instância.
 */
@Component
public class CorretorHomografoComOriginal {

    /** Formas de {@code to be} no original. Sem elas não há verbo para o {@code é} traduzir. */
    private static final Pattern VERBO_SER_INGLES = Pattern.compile(
        "\\b(?:is|are|am|was|were|be|been|isn't|aren't|wasn't|weren't)\\b"
            + "|(?<=\\w)'s\\b|(?<=\\w)'re\\b|(?<=\\w)'m\\b",
        Pattern.CASE_INSENSITIVE);

    /**
     * O que pode virar {@code e} em português. A vírgula entra porque o português coordena com
     * {@code e} o que o inglês separa por vírgula — <i>"Come here, sit down"</i> vira
     * <i>"Venha aqui e sente-se"</i>. Contar o lado inglês por CIMA faz a correção sair por
     * BAIXO, que é o lado seguro de errar.
     */
    private static final Pattern COORDENADOR_INGLES =
        Pattern.compile("\\band\\b|&|,|;|:", Pattern.CASE_INSENSITIVE);

    /** Demonstrativo no original: só ele autoriza {@code esta} a ser demonstrativo. */
    private static final Pattern DEMONSTRATIVO_INGLES =
        Pattern.compile("\\b(?:this|that|these|those)\\b", Pattern.CASE_INSENSITIVE);

    /**
     * Conjunção que ABRE a fala, dos dois lados. {@code "And Kamille's condition is..."} vira
     * {@code "E o estado de Kamille..."}: aquele {@code E} coordena com a fala ANTERIOR e é
     * legítimo. Tirá-lo só de um lado desequilibra a conta — foi o primeiro caso-controle a
     * reprovar quando esta regra nasceu.
     */
    private static final Pattern ABERTURA_INGLES =
        Pattern.compile("^(?:And|But|So|Then)\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ABERTURA_PORTUGUES =
        Pattern.compile("^(?:E|Mas|Ent[ãa]o)\\s+", Pattern.CASE_INSENSITIVE);

    private static final Pattern E_SOLTO = Pattern.compile(
        FronteiraTermoAss.INICIO + "(e)" + FronteiraTermoAss.FIM);
    private static final Pattern ESTA_SOLTO = Pattern.compile(
        FronteiraTermoAss.INICIO + "([Ee]sta)" + FronteiraTermoAss.FIM);

    /**
     * PROPÓSITO DE NEGÓCIO: devolve a fala com {@code é} e {@code está} acentuados onde o
     * original inglês PROVA que são verbo.
     *
     * <p>INVARIANTES DO DOMÍNIO: só troca o que o original autoriza; preserva a caixa
     * ({@code Esta} vira {@code Está}); não altera mais nada do texto.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada nula/vazia, ou original sem verbo {@code to
     * be}, devolve o texto como veio.
     *
     * @param originalIngles a fala original; {@code null} ou vazia desliga a correção
     * @param traduzido      a fala traduzida a corrigir
     * @return a fala corrigida, ou a original quando nada se aplica
     */
    public String corrigir(String originalIngles, String traduzido) {
        if (traduzido == null || traduzido.isBlank()
            || originalIngles == null || originalIngles.isBlank()) {
            return traduzido;
        }
        if (!VERBO_SER_INGLES.matcher(originalIngles).find()) {
            return traduzido;
        }
        String saida = traduzido;
        if (podeTrocarE(originalIngles)) {
            saida = trocar(saida, E_SOLTO, "é", preservaAbertura(saida));
        }
        if (!DEMONSTRATIVO_INGLES.matcher(originalIngles).find()) {
            saida = trocar(saida, ESTA_SOLTO, "está", 0);
        }
        return saida;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: diz se o original permite afirmar que TODO {@code e} solto da
     * tradução é o verbo.
     *
     * <p>INVARIANTES DO DOMÍNIO: um único coordenador no inglês já basta para a abstenção — não
     * há como saber QUAL {@code e} do português corresponde a ele.
     */
    private boolean podeTrocarE(String originalIngles) {
        String semAbertura = ABERTURA_INGLES.matcher(originalIngles).replaceFirst("");
        return !COORDENADOR_INGLES.matcher(semAbertura).find();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: quantos caracteres do começo da fala ficam fora da troca, para a
     * conjunção que abre a fala sobreviver.
     */
    private int preservaAbertura(String traduzido) {
        Matcher m = ABERTURA_PORTUGUES.matcher(traduzido);
        return m.lookingAt() ? m.end() : 0;
    }

    /** Troca as ocorrências a partir de {@code desde}, preservando a caixa do achado. */
    private String trocar(String texto, Pattern padrao, String acentuado, int desde) {
        Matcher m = padrao.matcher(texto);
        StringBuilder saida = new StringBuilder(texto.length());
        int ultimo = 0;
        boolean achou = false;
        while (m.find()) {
            if (m.start(1) < desde) {
                continue;
            }
            achou = true;
            String original = m.group(1);
            String troca = Character.isUpperCase(original.charAt(0))
                ? Character.toUpperCase(acentuado.charAt(0)) + acentuado.substring(1)
                : acentuado;
            saida.append(texto, ultimo, m.start(1)).append(troca);
            ultimo = m.end(1);
        }
        if (!achou) {
            return texto;
        }
        return saida.append(texto, ultimo, texto.length()).toString();
    }
}
