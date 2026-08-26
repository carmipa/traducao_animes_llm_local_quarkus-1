package org.traducao.projeto.core.texto.gramatica;

import jakarta.enterprise.context.ApplicationScoped;
import org.languagetool.JLanguageTool;
import org.languagetool.language.BrazilianPortuguese;
import org.languagetool.rules.RuleMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: liga o KRONOS ao LanguageTool para responder à única pergunta que o
 * dicionário não responde — a classe gramatical da palavra no contexto.
 *
 * <h2>A configuração aqui foi MEDIDA, não escolhida (2026-08-23)</h2>
 * O {@link org.traducao.projeto.core.texto.gramatica.RevisorGramaticalPort} nasceu de um gold set
 * de 60 falas lidas à mão nos episódios 1 e 2 do Macross II. O experimento rodou duas vezes, e a
 * diferença entre elas é a razão de existir cada linha de configuração abaixo:
 *
 * <pre>
 *   COM o corretor ortografico do LT:  158 acusacoes ·  92 alarmes falsos · 83 tocando lore
 *   SEM ele, so gramatica:              41 acusacoes ·  13 alarmes       ·  2 tocando lore
 *   so com as categorias que ficaram:   23 acertos    ·   5 alarmes
 * </pre>
 *
 * <h2>Por que o corretor ORTOGRÁFICO dele fica desligado — e a correção de uma afirmação minha</h2>
 * {@code MORFOLOGIK_RULE_PT_BR} disparou <b>117 das 158</b> acusações da primeira rodada, e o que
 * ele acusava era {@code UN}, {@code Spacy}, {@code Exxegran}, {@code Silvie} — <b>nome de lore</b>.
 * Ortografia o projeto já tem, com hunspell e com os termos protegidos.
 *
 * <p><b>Mas não é o {@code disableRule} que protege a lore, e isto foi descoberto pela mutação:</b>
 * religar a regra NÃO fez o caso-controle de lore reprovar. A sonda explicou — a categoria dela é
 * {@code TYPOS}, que já está fora da lista ligada. <b>Quem protege é o filtro de categoria</b>; o
 * {@code disableRule} é a segunda camada, e o que ele compra de verdade é <b>custo</b>: com a
 * regra ativa a análise custou 6,6 ms por fala, sem ela 3,9 ms — 40% do tempo era gasto para
 * produzir achados que seriam descartados adiante.
 *
 * <h2>Por que categorias, e não regras</h2>
 * São 2.925 regras ativas. Manter uma lista nominal delas seria dívida que ninguém revisa; a
 * categoria é a unidade que a própria ferramenta usa para agrupar, e foi por categoria que a
 * medição separou o que acerta do que só faz barulho:
 *
 * <pre>
 *   LIGADAS     CONFUSED_WORDS 15/3 · GRAMMAR 4/2 · MISSPELLING 2/0 · BR_SPELLING 1/0 · SYNTAX 1/0
 *   DESLIGADAS  STYLE 1/3 · REDUNDANCY 1/2 · PUNCTUATION 0/2 · CASING 1/1 · MISC 1/1
 * </pre>
 *
 * As desligadas não erram por bug: elas acham que <i>"bode expiatório"</i> é frase-feita, que
 * <i>"shampoo"</i> devia ser <i>"xampu"</i> e que <i>"anos de idade"</i> é pleonasmo. Está tudo
 * certo — e nada disso é defeito numa legenda de anime.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li><b>Só acusa; nunca reescreve.</b> Devolve achados e mais nada.</li>
 *   <li><b>Falha FECHADA e DECLARADA:</b> se o motor não carregar, {@link #disponivel()} devolve
 *       {@code false} e {@link #motivoDaIndisponibilidade()} diz por quê. Zero achados com o
 *       motor fora do ar é NÃO VERIFICADO, e quem consome é obrigado a mostrar isso.</li>
 *   <li><b>Não conhece lore, de propósito.</b> Filtrar nome de obra é problema de quem tem a
 *       lore — a fatia. O core não atravessa para o domínio de ninguém.</li>
 *   <li>A carga é preguiçosa e acontece UMA vez (~1,2 s). O {@code check} do LanguageTool não é
 *       seguro para uso concorrente, e por isso é serializado aqui.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nunca lança para fora. Falha de carga vira indisponibilidade declarada; falha numa fala isolada
 * vira lista vazia para aquela fala, com log em nível de aviso.
 */
@ApplicationScoped
public class LanguageToolRevisorAdapter implements RevisorGramaticalPort {

    private static final Logger LOG = LoggerFactory.getLogger(LanguageToolRevisorAdapter.class);

    /**
     * As categorias que a medição contra o gold set aprovou. Ver o Javadoc da classe para o
     * placar de cada uma — nenhuma entrou por parecer razoável.
     */
    static final Set<String> CATEGORIAS_LIGADAS = Set.of(
        "CONFUSED_WORDS", "GRAMMAR", "MISSPELLING", "BR_SPELLING", "SYNTAX");

    /**
     * O corretor ortográfico do LanguageTool. Desligado porque duplicaria o hunspell E acusaria
     * nome de lore — 117 de 158 acusações na medição de 23/08/2026.
     *
     * <p>PÚBLICO desde 26/08/2026 porque a medição que confere a lista de palavras defeituosas
     * precisa LIGAR esta regra num motor próprio, e o id tem de vir daqui: uma segunda cópia da
     * string faria os dois lados divergirem no dia em que o LanguageTool renomear a regra.
     */
    public static final String REGRA_ORTOGRAFICA = "MORFOLOGIK_RULE_PT_BR";

    private JLanguageTool motor;
    private boolean tentouCarregar;
    private String motivo;

    /**
     * PROPÓSITO DE NEGÓCIO: revisa uma fala já visível e devolve só o que interessa ao projeto.
     *
     * <p>INVARIANTES DO DOMÍNIO: filtra pelas categorias ligadas; devolve as posições no texto
     * que recebeu; não altera nada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista vazia, sempre — motor ausente, texto nulo, texto
     * em branco ou exceção do próprio LanguageTool.
     */
    @Override
    public List<AchadoGramatical> revisar(String texto) {
        if (texto == null || texto.isBlank() || !disponivel()) {
            return List.of();
        }
        List<RuleMatch> matches;
        synchronized (this) {
            try {
                matches = motor.check(texto);
            } catch (Exception e) {
                LOG.warn("Revisao gramatical falhou nesta fala; seguindo sem ela: {}", e.toString());
                return List.of();
            }
        }
        List<AchadoGramatical> fora = new ArrayList<>();
        for (RuleMatch m : matches) {
            String categoria = m.getRule().getCategory() == null
                ? "?" : m.getRule().getCategory().getId().toString();
            if (!CATEGORIAS_LIGADAS.contains(categoria)) {
                continue;
            }
            int ini = Math.max(0, m.getFromPos());
            int fim = Math.min(texto.length(), m.getToPos());
            if (ini >= fim) {
                continue;
            }
            fora.add(new AchadoGramatical(
                m.getRule().getId(), categoria, ini, fim,
                texto.substring(ini, fim), m.getMessage(), m.getSuggestedReplacements()));
        }
        return List.copyOf(fora);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: diz se dá para confiar num resultado vazio.
     *
     * <p>INVARIANTES DO DOMÍNIO: carrega uma vez só; uma falha de carga não é retentada a cada
     * fala, senão um episódio inteiro pagaria 1,2 s por linha para falhar igual.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code false} e o motivo preenchido; nunca lança.
     */
    @Override
    public synchronized boolean disponivel() {
        if (tentouCarregar) {
            return motor != null;
        }
        tentouCarregar = true;
        try {
            long t0 = System.currentTimeMillis();
            JLanguageTool novo = new JLanguageTool(new BrazilianPortuguese());
            novo.disableRule(REGRA_ORTOGRAFICA);
            motor = novo;
            LOG.info("Revisor gramatical pt-BR pronto em {} ms ({} regras, ortografico desligado)",
                System.currentTimeMillis() - t0, novo.getAllActiveRules().size());
            return true;
        } catch (Exception | NoClassDefFoundError e) {
            // A causa conhecida e o teto de entidade XML do JDK, que o grammar.xml do portugues
            // estoura. Os dois -D estao no build.gradle (TETOS_XML_DO_LANGUAGETOOL); sem eles a
            // revisao nasce indisponivel — e e por isso que ela DECLARA o motivo em vez de
            // devolver zero achados em silencio.
            motivo = "revisor gramatical indisponivel: " + e;
            LOG.warn("{} — a revisao gramatical fica NAO VERIFICADA, nunca aprovada por omissao",
                motivo);
            return false;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o texto que o operador lê na tela quando a revisão não pôde rodar.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code null} quando está disponível.
     */
    @Override
    public String motivoDaIndisponibilidade() {
        return motivo;
    }
}
