package org.traducao.projeto.core.texto;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: fazer com que uma fala chegue ao LLM como TEXTO PURO — sem nenhuma tag
 * ASS e sem nenhum marcador de controle — e volte vestida com a formatação original. É a ideia de
 * Paulo (07/08/2026): <i>"e se mandássemos os textos puros sem marcação alguma… e ao voltar ele
 * recriaria o arquivo completo já com todas as tags e frescuras"</i>.
 *
 * <h2>O prejuízo que originou</h2>
 * O pipeline mascara cada tag como {@code [[TAGn]]} e exige que o modelo a devolva intacta. Ele
 * frequentemente não devolve, e a tradução — correta — era descartada:
 * <ul>
 *   <li>tradução de diálogo, corrida de 2026-07-22: <b>393 das 412 falas perdidas (95%)</b> caíram
 *       por marcador ausente, quase todas com texto aproveitável. A resposta na época foi um
 *       remendo, {@code ReparadorMarcadoresLlm}, que repõe o marcador quando dá;</li>
 *   <li>corretor de karaokê, 07/08/2026: <b>269 de 269</b> recusas pelo mesmo motivo. Ali a causa
 *       foi eliminada em vez de remendada ({@code GradienteKaraoke}), e a taxa de sucesso subiu de
 *       <b>34% para 100%</b> nas linhas que passaram a viajar sem marcador.</li>
 * </ul>
 * Medido no Guilty Crown em 07/08/2026: das 5.869 falas de diálogo, <b>488 (8,3%)</b> têm tag só
 * na borda — o recorte desta classe. Outras 234 (4,0%) têm tag no meio e ficam de fora, e 87,6%
 * já viajam limpas e não passam por aqui.
 *
 * <h2>O que esta classe NÃO faz, e por quê</h2>
 * <ul>
 *   <li><b>Quebra {@code \N}</b>: dono é {@code IsoladorQuebraDialogo} desde 2026-07-23, que a
 *       remove antes do mascaramento e a repõe em fronteira de palavra. A primeira versão desta
 *       classe chegou a tratá-la, e a leitura do código mostrou a duplicação a tempo.</li>
 *   <li><b>Vetar karaokê ({@code \k})</b>: nesta fatia a fala musical nunca chega até aqui —
 *       {@code SeletorEventosTraduziveis} a bloqueia antes, consultando
 *       {@code DetectorEfeitoKaraokeService}. Repetir o padrão {@code \k} aqui criaria a QUARTA
 *       cópia dele no projeto, e foi exatamente isso que
 *       {@code CatracaRegraDuplicadaEntreFatiasTest} reprovou em 07/08/2026.</li>
 * </ul>
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só decompõe quando TODA tag está na BORDA (prefixo ou sufixo). Tag no MEIO marca uma
 *       palavra específica ({@code Eu {\i1}nunca{\i0} vou}), e recolocá-la exigiria alinhamento
 *       palavra a palavra entre inglês e português — fora de escopo, por decisão declarada.</li>
 *   <li>Transformação animada ({@code \t(}) veta: ali a tag não é moldura estática.</li>
 *   <li>Prefixo e sufixo saem literais; nada é inventado, reordenado ou alterado.</li>
 *   <li>É o DONO ÚNICO da pergunta "as tags estão todas na borda?" — {@code TradutorLotesService}
 *       delega a {@link #tagsSoNaBorda(String)} em vez de manter a própria cópia.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * {@link #decompor} devolve {@link Optional#empty()} para tudo que não couber no recorte — e o
 * chamador segue pelo caminho antigo, com mascaramento, sem perder nada. {@link #recompor}
 * devolve o {@link #original} quando a tradução vem nula, em branco ou sem texto utilizável.
 * Falha fechada: nunca produz linha meio montada.
 */
public record TextoSemTags(String prefixo, String textoLimpo, String sufixo, String original) {

    private static final Pattern BLOCO_TAGS = Pattern.compile("\\{[^}]*\\}");
    private static final Pattern TAG_TRANSFORMACAO = Pattern.compile("\\\\t\\(");
    /**
     * DUPLICAÇÃO DECLARADA: {@code AchatadorEstilosDecorativosService.OVERRIDE_LIDER}
     * (fatia {@code trocaTipoLegenda}) tem esta mesma regex. Não foram unificadas de propósito —
     * o projeto prefere duplicação consciente a acoplamento, e ligar {@code traducao} a
     * {@code trocaTipoLegenda} só para reusar um padrão criaria aresta entre duas fatias que hoje
     * não se conhecem. As INTENÇÕES também divergem e podem evoluir separadas: lá o bloco líder é
     * DESCARTADO (a frescura visual some); aqui ele é PRESERVADO e recolocado. Contada em
     * {@code CatracaRegraDuplicadaEntreFatiasTest}, que reprovou a cópia não declarada em
     * 07/08/2026 — foi ela que exigiu este parágrafo.
     */
    private static final Pattern BORDA_INICIO = Pattern.compile("^(?:\\{[^}]*\\})+");
    private static final Pattern BORDA_FIM = Pattern.compile("(?:\\{[^}]*\\})+$");

    /**
     * PROPÓSITO DE NEGÓCIO: responde "todas as tags desta fala estão na BORDA?" — a pergunta que
     * decide se ela pode viajar ao LLM como texto puro e, no dedup, se duas camadas com o mesmo
     * texto visível podem compartilhar uma tradução.
     *
     * <p>INVARIANTES DO DOMÍNIO: dono único. Existia como {@code soPrefixo}, privada em
     * {@code TradutorLotesService}; desde 07/08/2026 aquele método delega para cá, para que o
     * critério não tenha duas implementações capazes de divergir.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code null} devolve {@code false}; nunca lança.
     */
    public static boolean tagsSoNaBorda(String texto) {
        return decompor(texto).isPresent();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: separa a moldura de tags do texto que o espectador lê, para que ao LLM
     * vá apenas a frase.
     *
     * <p>INVARIANTES DO DOMÍNIO: só aceita tag na borda; não altera a entrada; texto sem tag
     * nenhuma NÃO entra (o caminho comum já é o correto para ele).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@link Optional#empty()} para entrada nula/vazia, tag no
     * meio, {@code \t(} ou texto visível em branco. Nunca lança.
     */
    public static Optional<TextoSemTags> decompor(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        if (TAG_TRANSFORMACAO.matcher(texto).find()) {
            return Optional.empty();
        }

        String prefixo = casar(BORDA_INICIO, texto);
        String semPrefixo = texto.substring(prefixo.length());
        String sufixo = casar(BORDA_FIM, semPrefixo);
        String miolo = semPrefixo.substring(0, semPrefixo.length() - sufixo.length());

        // Tag sobrando no MIOLO marca palavra: fora do recorte, segue pelo mascarador.
        if (BLOCO_TAGS.matcher(miolo).find()) {
            return Optional.empty();
        }
        // Sem moldura nao ha o que separar — 87,6% do dialogo cai aqui e continua intacto.
        if (prefixo.isEmpty() && sufixo.isEmpty()) {
            return Optional.empty();
        }
        if (miolo.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new TextoSemTags(prefixo, miolo, sufixo, texto));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve a fala pronta para o arquivo — a tradução dentro da moldura
     * original, sem que nenhum marcador tenha precisado sobreviver à viagem.
     *
     * <p>INVARIANTES DO DOMÍNIO: prefixo e sufixo saem literais e na mesma ordem; qualquer tag que
     * o modelo tenha inventado na resposta é descartada, porque a moldura verdadeira é a do
     * original — nunca a que o LLM imaginou.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: tradução nula, em branco ou sem texto após limpeza
     * devolve o {@link #original} intacto.
     */
    public String recompor(String traducao) {
        if (traducao == null || traducao.isBlank()) {
            return original;
        }
        String limpo = BLOCO_TAGS.matcher(traducao).replaceAll("").strip();
        if (limpo.isEmpty()) {
            return original;
        }
        return prefixo + limpo + sufixo;
    }

    private static String casar(Pattern padrao, String texto) {
        Matcher m = padrao.matcher(texto);
        return m.find() ? m.group() : "";
    }
}
