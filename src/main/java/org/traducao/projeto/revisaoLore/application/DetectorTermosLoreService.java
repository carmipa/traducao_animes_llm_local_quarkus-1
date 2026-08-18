package org.traducao.projeto.revisaoLore.application;

import org.traducao.projeto.core.texto.FronteiraTermoAss;

import org.springframework.stereotype.Service;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: prioriza falas com possível erro terminológico antes
 * de chamar o LLM, respeitando a lore específica da obra selecionada.
 * <p>INVARIANTES DO DOMÍNIO: nomes canônicos, equivalências PT-BR autorizadas
 * e termos oficiais preservados não podem virar falsos resíduos em inglês.
 * <p>COMPORTAMENTO EM CASO DE FALHA: entradas insuficientes retornam resultado
 * limpo; suspeitas são somente sinalizadas e nunca modificam a legenda.
 */
@Service
public class DetectorTermosLoreService {

    private static final String INICIO_DE_TERMO = FronteiraTermoAss.INICIO;

    private static final Pattern NOME_PROPRIO = Pattern.compile(
        "\\b(?:[A-Z][A-Za-z0-9'’.-]{2,}|[A-Z]{2,}(?:-[A-Z0-9]+)?)(?:\\s+(?:[A-Z][A-Za-z0-9'’.-]{2,}|[A-Z]{2,}(?:-[A-Z0-9]+)?))*\\b"
    );
    private static final Map<String, List<String>> TRADUCOES_LITERAIS_SUSPEITAS = criarTraducoesLiteraisSuspeitas();
    private static final Map<String, List<String>> TERMOS_TRADUZIVEIS_ACEITOS = criarTermosTraduziveisAceitos();
    private static final Set<String> TERMOS_LORE_SOLTEIROS_RELEVANTES = Set.of(
        "aeug", "titans", "anaheim", "apsalus", "sahalin", "sakhalin", "char", "amuro",
        "londo", "phenex", "unicorn", "narrative", "banshee", "legion", "handler",
        "processor", "juggernaut", "valkyrie", "macross",
        "zeon", "gundam", "zaku", "gouf", "gelgoog", "dom", "gm", "kampfer", "sazabi",
        "alex", "sinanju", "axis", "jaburo", "odessa", "libot", "albion", "shiro",
        "aina", "karen", "eledore", "michel", "kiki", "ginias", "norris", "packard",
        "kojima", "kou", "gato", "nina", "cima", "bernie", "christina", "chris",
        "bright", "sayla", "lalah", "hathaway", "quess", "gyunei", "nanai", "jona",
        "michelle", "rita", "zoltan",
        "shin", "shinei", "nouzen", "lena", "vladilena", "milize", "raiden", "shuga",
        "anju", "theoto", "theo", "rikka", "kurena", "kukumila", "frederica",
        "rosenfort", "ernst", "zimmerman", "eugene", "rantz", "spearhead",
        "nordlicht", "morpho", "feldress", "para-raid", "san", "magnolia", "giad",
        "alba", "colorata", "undertaker"
    );
    private static final Set<String> PALAVRAS_IGNORADAS = Set.of(
        "the", "and", "you", "your", "that", "this", "with", "from", "what", "when", "where",
        "why", "how", "are", "was", "were", "have", "has", "had", "not", "but", "for", "all",
        "out", "our", "his", "her", "him", "she", "they", "them", "will", "can", "just", "like",
        "get", "got", "one", "two", "now", "yes", "hey", "sir", "miss", "lord", "lady", "man",
        "boy", "girl", "god", "damn", "hell", "okay", "ok", "well", "come", "here", "there",
        "even", "passing", "beginning", "fall", "leave", "these", "if", "let", "yeah",
        "youre", "im", "dont", "cant", "wont", "ill", "ive", "thats", "whats", "base",
        "someone", "anyone", "something", "anything", "forever", "indeed", "maybe", "please",
        "thanks", "thank", "hello", "goodbye", "always", "never", "every", "second", "father",
        "laugh", "heaven", "chairman", "minister", "princess", "commander", "ensign", "adm"
    );

    public ResultadoDeteccaoLore auditar(String originalIngles, String traducaoPt) {
        return auditar(originalIngles, traducaoPt, null);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: audita a fala usando somente as regras pertinentes
     * à lore da obra ativa, reduzindo falsos positivos entre franquias.
     * <p>INVARIANTES DO DOMÍNIO: termos canônicos preservados e traduções PT-BR
     * explicitamente aceitas não são tratados como resíduos; sem lore mantém o
     * comportamento global compatível com chamadas antigas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: entradas vazias produzem resultado
     * limpo e nenhuma alteração é autorizada diretamente por este detector.
     */
    public ResultadoDeteccaoLore auditar(String originalIngles, String traducaoPt, String loreObraAtiva) {
        return auditar(originalIngles, traducaoPt, loreObraAtiva, Map.of());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mesma auditoria, agora sabendo quais traduções PT-BR a OBRA declara
     * como aceitas — o que impede a tela de acusar tradução correta.
     *
     * <h2>Por que este parâmetro existe — medido em 17/08/2026</h2>
     * O 86 fechou com 543 pendências e quase nenhuma era defeito: {@code Federacy}→{@code
     * Federação}, {@code Republic}→{@code República}, {@code Empire}→{@code Império},
     * {@code Reaper}→{@code Ceifador}. Todas corretas, todas acusadas, porque o único catálogo de
     * equivalências era o {@link #TERMOS_TRADUZIVEIS_ACEITOS} HARDCODED aqui — que mistura
     * Gundam, 86 e Macross e é uma segunda cópia da lore dentro do código.
     *
     * <p>INVARIANTES DO DOMÍNIO: as equivalências da obra têm precedência e SOMAM ao mapa global;
     * mapa vazio reproduz exatamente o comportamento anterior. Declarar equivalência NÃO escreve
     * na legenda — só cala a acusação.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa nulo é tratado como vazio.
     */
    public ResultadoDeteccaoLore auditar(String originalIngles, String traducaoPt, String loreObraAtiva,
                                         Map<String, List<String>> equivalenciasDaObra) {
        if (originalIngles == null || originalIngles.isBlank()
            || traducaoPt == null || traducaoPt.isBlank()) {
            return ResultadoDeteccaoLore.limpo();
        }

        List<String> motivos = new ArrayList<>();
        String en = achatarQuebras(originalIngles);
        String pt = achatarQuebras(traducaoPt);
        String loreLower = loreObraAtiva == null || loreObraAtiva.isBlank()
            ? null
            : loreObraAtiva.toLowerCase(Locale.ROOT);

        // As TRES regras de lore, e so elas. Escopo fechado por Paulo em 17/08/2026: a 3.2 acusa
        // nome, local e termo canonico da obra ativa — nada mais.
        //
        // Sairam daqui, com o volume medido no acervo (MedicaoEscopoDaRevisaoLoreIT, 22 obras,
        // 75.419 falas, 10.080 motivos):
        //   detectarNomesInglesRemanescentes  1.606 motivos (15,9%) — acusava QUALQUER palavra
        //       inglesa >=4 letras que sobrasse no PT. Isso e FALTA DE TRADUCAO, e falta de
        //       traducao e a tela 3.1. Acusar aqui fazia a 3.2 ficar amarela por trabalho alheio.
        //   detectarTermosMaiusculosSuspeitos   816 motivos (8,1%) — acusava qualquer palavra em
        //       CAIXA ALTA >=3 letras como "pode indicar lore fora do padrao". Um grito "PARE!"
        //       virava motivo. Palpite sobre formatacao, nao nome nem local.
        //
        // Juntas eram 2.422 motivos: 24,0% de todo o ruido que a tela produzia.
        detectarTraducoesLiteraisSuspeitas(en, pt, motivos, loreLower);
        detectarTermosTraduziveisEmIngles(en, pt, motivos);
        detectarNomesPropriosDivergentes(en, pt, motivos, loreLower, equivalenciasDaObra);

        if (motivos.isEmpty()) {
            return ResultadoDeteccaoLore.limpo();
        }
        return new ResultadoDeteccaoLore(true, List.copyOf(motivos));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: entrega às regras de auditoria o texto como uma FRASE, e não como
     * as duas ou três linhas em que o ASS a desenhou na tela.
     *
     * <h2>O que estava acontecendo — MEDIDO, não suposto</h2>
     * Este detector usa {@code \b} e {@code \s+} em cinco lugares, e {@code \N} derruba os dois:
     * o {@code N} é caractere de palavra (não abre borda) e a quebra não é espaço (não separa as
     * partes de um nome composto). A consequência dominante, porém, <b>não</b> é o detector
     * perder nome — é ele ACUSAR o que está certo.
     *
     * <p>O caso típico: o inglês traz {@code "the Zeta Gundam"} numa linha só e a tradução
     * quebra em {@code "o Zeta\NGundam"}. O nome é achado inteiro no EN, e o
     * {@code pt.contains(nome)} não o encontra no PT — porque lá tem uma quebra no meio. Sai
     * "nome próprio inconsistente" para uma tradução perfeita.
     *
     * <p><b>Medido sobre o acervo (67.923 falas, 16.023 com quebra) em 2026-08-05</b>, rodando o
     * serviço pelo CDI antes e depois desta normalização:
     * <pre>
     *                     falas com pendencia          motivos
     *   ANTES   com quebra   13.099 de 16.023 (81,8%)   16.956
     *   DEPOIS  com quebra    4.163 de 16.023 (26,0%)    5.576
     *   CONTROLE sem quebra   6.929 de 52.138 (13,3%)   inalterado nos dois
     * </pre>
     * 81,8% contra uma base de 13,3% era ruído, não achado: <b>8.936 falas</b> deixam de ser
     * pendência, e só o motivo "nome próprio inconsistente" cai de 13.362 para 2.372. O grupo sem
     * quebra não se move — é o que prova que a mudança não vazou para fora do seu escopo.
     *
     * <p>Consertar as cinco regexes uma a uma resolveria a DESCOBERTA e deixaria a COMPARAÇÃO
     * pior: o nome casado sairia com a quebra dentro ({@code "Psyco\NGundam"}) e seria procurado
     * assim no PT. Normalizar na entrada põe os dois lados na mesma régua, e de quebra o motivo
     * que chega ao humano sai legível. Ver {@code MedicaoLoreQuebraIT}.
     *
     * <p>INVARIANTES DO DOMÍNIO: só o que a regra LÊ muda; o detector é read-only e não
     * reescreve legenda nenhuma. Espaços consecutivos são colapsados para que os dois lados da
     * comparação fiquem canônicos — quebra colada e quebra com espaço em volta produzem o mesmo
     * texto.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada nula nunca chega aqui (barrada em
     * {@link #auditar(String, String, String)}); texto sem quebra atravessa inalterado a menos
     * do {@code trim}.
     */
    private static String achatarQuebras(String texto) {
        return texto.replace("\\N", " ").replaceAll("\\s+", " ").trim();
    }

    /** Termo canônico vale para a obra ativa? Sem lore informado, vale globalmente. */
    private boolean loreMenciona(String loreLower, String termo) {
        return loreLower == null || contemExpressaoInteira(loreLower, termo);
    }

    private void detectarTermosTraduziveisEmIngles(String en, String pt, List<String> motivos) {
        String enLower = en.toLowerCase(Locale.ROOT);
        String ptLower = pt.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, List<String>> entrada : TERMOS_TRADUZIVEIS_ACEITOS.entrySet()) {
            String termoIngles = entrada.getKey();
            if (!contemExpressaoInteira(enLower, termoIngles) || contemAlgumaExpressao(ptLower, entrada.getValue())) {
                continue;
            }
            if (contemExpressaoInteira(ptLower, termoIngles)) {
                motivos.add("Termo de faccao/organizacao traduzivel permaneceu em ingles: \""
                    + termoIngles + "\"");
            }
        }
    }

    private void detectarTraducoesLiteraisSuspeitas(String en, String pt, List<String> motivos, String loreLower) {
        String enLower = en.toLowerCase(Locale.ROOT);
        String ptLower = pt.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, List<String>> entrada : TRADUCOES_LITERAIS_SUSPEITAS.entrySet()) {
            String termoCanonico = entrada.getKey();
            if (!enLower.contains(termoCanonico) || !loreMenciona(loreLower, termoCanonico)) {
                continue;
            }
            for (String traducaoSuspeita : entrada.getValue()) {
                if (ptLower.contains(traducaoSuspeita)) {
                    motivos.add("Possivel traducao literal de termo/nome canonico: \""
                        + traducaoSuspeita + "\" deveria preservar \"" + termoCanonico + "\" quando for lore");
                    break;
                }
            }
        }
    }

    private void detectarNomesPropriosDivergentes(String en, String pt, List<String> motivos, String loreLower,
            Map<String, List<String>> equivalenciasDaObra) {
        Matcher matcherEn = NOME_PROPRIO.matcher(en);
        while (matcherEn.find()) {
            String grupo = matcherEn.group();
            // Divide o grupo por quebras de frase reais (. ! ? seguido de espaço),
            // ignorando abreviações comuns (Dr., Lt., U.C., etc.)
            String[] subNomes = grupo.split("(?<!\\b(?:Dr|Lt|Col|Capt|Gen|Mr|Mrs|Ms|St|U\\.C)\\.)(?<=[.!?])\\s+");
            for (String subNomeRaw : subNomes) {
                String nome = limparCandidatoNomeProprio(subNomeRaw);
                int indexNoOriginal = en.indexOf(subNomeRaw);
                // O indice tem de apontar para o NOME, nao para o artigo/patente que veio antes:
                // em "Ensign Keith, ..." quem esta na posicao inicial e "Ensign".
                int inicioDoNome = (indexNoOriginal >= 0 ? indexNoOriginal : matcherEn.start())
                    + deslocamentoDoPrefixoDeFrente(subNomeRaw);
                if (deveIgnorarNomeProprio(nome, inicioEfetivoDaFala(en, inicioDoNome), loreLower)
                    || traducaoAceitaParaTermo(nome, pt, equivalenciasDaObra)) {
                    continue;
                }
                if (pt.contains(nome)) {
                    continue;
                }
                if (contemNomeCompostoParcial(pt, nome)) {
                    motivos.add("Nome proprio composto foi preservado apenas em parte; conferir se algum trecho foi traduzido: \"" + nome + "\"");
                } else if (!contemVarianteAproximada(pt, nome)) {
                    motivos.add("Nome proprio do original pode estar inconsistente na traducao: \"" + nome + "\"");
                }
            }
        }
    }

    private boolean contemVarianteAproximada(String pt, String nome) {
        String[] partes = nome.split("\\s+");
        if (partes.length == 1) {
            return false;
        }
        int encontrados = 0;
        for (String parte : partes) {
            if (parte.length() >= 3 && pt.toLowerCase(Locale.ROOT).contains(parte.toLowerCase(Locale.ROOT))) {
                encontrados++;
            }
        }
        return encontrados >= Math.max(1, partes.length - 1);
    }

    private String removerArtigoInicialIngles(String nome) {
        return nome.replaceFirst("(?i)^(the|a|an)\\s+", "");
    }

    private String removerPossessivoIngles(String nome) {
        return nome.replaceAll("(?i)['’]s\\b", "");
    }

    /** Abreviacoes cujo ponto NAO fecha frase — a mesma lista que separa sub-nomes acima. */
    private static final Pattern ABREVIACAO_NO_FIM = Pattern.compile(
        "(?i)\\b(?:dr|lt|col|capt|gen|mr|mrs|ms|st|sgt|adm|cmdr|prof|u\\.c)\\.$");

    private String limparCandidatoNomeProprio(String nome) {
        return removerPossessivoIngles(removerPrefixosDeFrente(nome)).strip();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reúne tudo que o inglês põe NA FRENTE de um nome sem fazer parte
     * dele — artigo, numeral, possessivo posicional e patente/tratamento.
     *
     * <p>INVARIANTES DO DOMÍNIO: só mexe no começo, e o que sobra é o nome. Está separado de
     * {@link #limparCandidatoNomeProprio} porque o chamador precisa saber QUANTOS caracteres
     * saíram da frente — ver {@link #deslocamentoDoPrefixoDeFrente}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nome sem prefixo atravessa inalterado.
     */
    private String removerPrefixosDeFrente(String nome) {
        return removerPatenteOuTratamentoIngles(
            removerPrefixoComumIngles(removerArtigoInicialIngles(nome)));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: diz quantos caracteres a limpeza tirou da FRENTE, para que o
     * chamador pergunte "este nome está no início da fala?" apontando para o nome de verdade,
     * e não para a patente que veio antes dele.
     *
     * <h2>Por que isto existe, e por que nasceu junto com a patente</h2>
     * A regra do início da fala existe porque o inglês capitaliza a primeira palavra da frase:
     * uma palavra maiúscula ali pode ser maiúscula por POSIÇÃO, não por ser nome. Só que em
     * {@code "Ensign Keith, have you been studying?"} quem está na posição inicial é
     * {@code Ensign} — {@code Keith} está maiúsculo porque É nome. Perguntar pelo índice do
     * grupo bruto faria o detector descartar {@code Keith} como se fosse maiúscula de frase, e
     * aí tirar a patente CEGARIA a tela para o caso que ela precisa pegar: nome trocado logo no
     * começo da fala ({@code "Captain Nouzen"} → {@code "Capitao Cha"}).
     *
     * <p>Vale igual para artigo e possessivo, que já eram removidos antes: em
     * {@code "The Reaper is here"}, {@code Reaper} nunca esteve na posição inicial.
     *
     * <p>INVARIANTES DO DOMÍNIO: conta só o que saiu da frente; o possessivo {@code 's}, que é
     * removido do MEIO, fica de fora da conta de propósito.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem prefixo o deslocamento é {@code 0} e o
     * comportamento é o de antes.
     */
    private int deslocamentoDoPrefixoDeFrente(String bruto) {
        return bruto.length() - removerPrefixosDeFrente(bruto).length();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: tira da frente do candidato a patente militar ou o tratamento
     * ({@code Ensign}, {@code Lieutenant}, {@code Miss}, {@code Lady}…), que é CARGO, não nome.
     *
     * <h2>O prejuízo, medido em 17/08/2026 nas quatro obras da Universal Century</h2>
     * A regra de nome composto exige que todas as partes sobrevivam no português. Como
     * {@code Ensign} vira {@code Tenente} — que é a tradução certa —, {@code "Ensign Keith"}
     * era acusado para sempre. Nas corridas de 0083, Unicorn, Zeta e ZZ: <b>34%</b> dos termos
     * acusados começavam por patente ou tratamento. Cruzando o par EN/PT e exigindo que a
     * patente em português estivesse COLADA ao mesmo nome:
     * <pre>
     *   FALSA  patente traduzida certo, nome preservado ..  617  62,8%
     *   REAL   patente virou OUTRA patente ...............   10   1,0%
     *   REAL?  o nome sumiu do PT ........................  126  12,8%
     *   ?      PT sem patente nenhuma ....................  229  23,3%
     * </pre>
     * É a mesma classe do possessivo ({@link #removerPrefixoComumIngles}), na mesma linha.
     *
     * <h2>A lacuna que isto abre, declarada</h2>
     * As 10 divergências de patente ({@code Ensign} → {@code Sargento}, {@code Major} →
     * {@code Coronel}) deixam de ser vistas por esta regra. São erros reais, mas de PATENTE, e
     * patente não é nome nem lugar — fica fora do escopo fechado da 3.2. As 126 em que o nome
     * sumiu do PT continuam acusadas: só a patente sai, o nome segue sob conferência.
     *
     * <p>INVARIANTES DO DOMÍNIO: remove só do início e só uma vez. As patentes de duas palavras
     * ({@code Lt. Commander}, {@code Master Chief}) vêm PRIMEIRO na alternância, porque o regex
     * casa da esquerda para a direita e {@code Lt.} sozinho deixaria {@code "Commander Gato"}
     * para trás.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nome sem patente atravessa inalterado.
     */
    private String removerPatenteOuTratamentoIngles(String nome) {
        return PATENTE_NO_INICIO.matcher(nome).replaceFirst("");
    }

    /**
     * Alternância única de patente e tratamento. As de DUAS palavras vêm primeiro porque o regex
     * casa da esquerda para a direita: {@code Lt.} sozinho deixaria {@code "Commander Gato"} para
     * trás. Fica em uma constante só porque serve a duas perguntas diferentes — "isto vem ANTES
     * do nome?" e "isto É o candidato inteiro?" — e listas gêmeas divergem no dia em que alguém
     * acrescenta uma patente em apenas uma delas.
     */
    private static final String ALTERNANCIA_PATENTE =
        "lt\\.?\\s+commander|lieutenant\\s+commander|master\\s+chief|chief\\s+petty\\s+officer"
        + "|petty\\s+officer|warrant\\s+officer|vice\\s+admiral|rear\\s+admiral|sub\\s*lieutenant"
        + "|lieutenant|lt\\.?|ensign|captain|capt\\.?|commander|cmdr\\.?|major|colonel|col\\.?"
        + "|sergeant|sgt\\.?|corporal|admiral|adm\\.?|general|gen\\.?|chief"
        + "|miss|mistress|mister|mr\\.?|mrs\\.?|ms\\.?|master|lady|lord|sir|madam|madame|dame"
        + "|doctor|dr\\.?|professor|prof\\.?|president|chairman";

    private static final Pattern PATENTE_NO_INICIO =
        Pattern.compile("(?i)^(?:" + ALTERNANCIA_PATENTE + ")\\s+");

    /**
     * A patente SOZINHA, sem nome atrás — o vocativo {@code "When do we get to see them,
     * Lieutenant?!"}, que o português resolve com {@code "Tenente"}.
     *
     * <p>Sobraram 21 acusações assim na corrida do 0083 depois que a patente com nome saiu: o
     * removedor de prefixo exige {@code \s+} e algo depois, então não alcança a palavra solta.
     * Patente sozinha é cargo, nunca nome próprio.
     */
    private static final Pattern PATENTE_SOZINHA =
        Pattern.compile("(?i)^(?:" + ALTERNANCIA_PATENTE + ")\\.?$");

    /**
     * PROPÓSITO DE NEGÓCIO: tira da frente do candidato as palavras que o inglês capitaliza por
     * posição, e que NÃO fazem parte do nome — numeral, demonstrativo e <b>possessivo</b>.
     *
     * <h2>Os possessivos entraram em 17/08/2026, e o prejuízo foi medido</h2>
     * Sem eles, {@code "Our Reaper"} era tratado como nome próprio COMPOSTO, e a regra de composto
     * exige que todas as partes apareçam no português. Como {@code Our} vira {@code Nosso} — que é
     * a tradução certa —, a fala era acusada para sempre. Duas consequências opostas, as duas
     * vistas na mesma corrida da tela 3.2 no 86:
     * <ul>
     *   <li><b>bloqueava conserto real:</b> no ep10 ev.149 o corretor determinístico trocava
     *       {@code "Nosso Juggernaut"} por {@code "Nosso Reaper"} — o apelido do piloto estava no
     *       lugar do nome do MECHA — e a re-auditoria acusava de novo, então a correção era
     *       DESCARTADA e a legenda ficava errada;</li>
     *   <li><b>criava falso positivo:</b> {@code "His Juggernaut"} → {@code "Seu Juggernaut"} está
     *       correto e era acusado pelo mesmo motivo.</li>
     * </ul>
     * Medido no 86 (11 arquivos, 3.476 falas): 82 acusações de composto, das quais <b>2</b> por
     * possessivo — uma de cada tipo acima. Volume baixo, efeito dos dois lados.
     *
     * <p>INVARIANTES DO DOMÍNIO: remove só do INÍCIO e só uma vez, como já fazia com o artigo
     * ({@code the/a/an}); o resto do nome fica intacto.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nome sem prefixo atravessa inalterado.
     */
    private String removerPrefixoComumIngles(String nome) {
        return nome.replaceFirst("(?i)^(one|two|three|four|five|six|seven|eight|nine|ten|all|these|those|this|that|some|any"
            + "|our|my|his|her|their|your|its)\\s+", "");
    }

    private boolean deveIgnorarNomeProprio(String nome, boolean inicioEfetivoDaFala, String loreLower) {
        if (nome.length() < 4 || nome.matches("(?i)TAG\\d+")) {
            return true;
        }

        String[] partes = nome.split("\\s+");
        if (partes.length == 1) {
            String normalizada = normalizarTokenNome(partes[0]);
            if (PALAVRAS_IGNORADAS.contains(normalizada) || PATENTE_SOZINHA.matcher(nome).matches()) {
                return true;
            }
            return inicioEfetivoDaFala && !temIndicadorLoreSolteiro(partes[0], normalizada, loreLower);
        }
        return false;
    }

    private boolean inicioEfetivoDaFala(String texto, int inicioCandidato) {
        if (inicioCandidato <= 0) {
            return true;
        }
        String prefixo = texto.substring(0, Math.max(0, inicioCandidato))
            .replaceAll("(?i)\\[\\[TAG\\d+\\]\\]", "")
            .trim();
        if (prefixo.isEmpty()) {
            return true;
        }
        // Ponto de ABREVIACAO nao fecha frase. Isto passou a importar em 17/08/2026, quando a
        // patente/tratamento saiu do candidato e o indice deslocou para depois dela: em
        // "We were at Dr. Flanagan's institute", o prefixo de "Flanagan" virou "We were at Dr." e
        // o ultimo caractere e um ponto — a fala inteira era lida como frase nova e "Flanagan"
        // escapava como maiuscula de posicao. A lista e a mesma que ja separa sub-nomes acima.
        if (ABREVIACAO_NO_FIM.matcher(prefixo).find()) {
            return false;
        }
        char ultimo = prefixo.charAt(prefixo.length() - 1);
        return ultimo == '.' || ultimo == '!' || ultimo == '?' || ultimo == '"' || ultimo == '”' || ultimo == '\'' || ultimo == '’';
    }

    private boolean temIndicadorLoreSolteiro(String original, String normalizada, String loreLower) {
        // O termo global só conta se pertencer à obra ativa: "Dom" no início de
        // uma fala do 86 não é o mobile suit do Gundam.
        return (TERMOS_LORE_SOLTEIROS_RELEVANTES.contains(normalizada) && loreMenciona(loreLower, normalizada))
            || original.matches(".*\\d.*")
            || original.matches("[A-Z0-9.-]{2,}");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reconhece nomes e títulos cuja forma PT-BR está
     * explicitamente autorizada pelo catálogo de equivalências.
     * <p>INVARIANTES DO DOMÍNIO: nomes compostos só são aceitos quando cada
     * parte permanece canônica ou possui uma variante traduzida cadastrada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: parte não comprovada retorna falso e
     * mantém a fala sinalizada para análise segura.
     */
    private boolean traducaoAceitaParaTermo(String nome, String pt, Map<String, List<String>> daObra) {
        String ptLower = pt.toLowerCase(Locale.ROOT);
        // A obra vem PRIMEIRO: ela conhece a propria terminologia melhor que o mapa global.
        List<String> declaradas = daObra == null ? null : daObra.get(nome.toLowerCase(Locale.ROOT));
        if (declaradas != null && contemAlgumaExpressao(ptLower, declaradas)) {
            return true;
        }
        List<String> aceitas = TERMOS_TRADUZIVEIS_ACEITOS.get(nome.toLowerCase(Locale.ROOT));
        if (aceitas != null && contemAlgumaExpressao(ptLower, aceitas)) {
            return true;
        }

        String[] partes = nome.split("\\s+");
        if (partes.length < 2) {
            return false;
        }
        for (String parte : partes) {
            String normalizada = normalizarTokenNome(parte);
            if (normalizada.isBlank()) {
                continue;
            }
            List<String> variantes = daObra != null && daObra.containsKey(normalizada)
                ? daObra.get(normalizada)
                : TERMOS_TRADUZIVEIS_ACEITOS.get(normalizada);
            boolean presenteOriginal = contemExpressaoInteira(ptLower, normalizada);
            boolean presenteTraduzida = variantes != null && contemAlgumaExpressao(ptLower, variantes);
            if (!presenteOriginal && !presenteTraduzida) {
                return false;
            }
        }
        return true;
    }

    private String normalizarTokenNome(String token) {
        return removerPossessivoIngles(token)
            .replaceAll("[^A-Za-z0-9]", "")
            .toLowerCase(Locale.ROOT);
    }

    private boolean contemNomeCompostoParcial(String pt, String nome) {
        String[] partes = nome.split("\\s+");
        if (partes.length < 2) {
            return false;
        }

        String ptLower = pt.toLowerCase(Locale.ROOT);
        int encontrados = 0;
        int relevantes = 0;
        for (String parte : partes) {
            String normalizada = parte.replaceAll("[^A-Za-z0-9]", "");
            if (normalizada.length() < 3) {
                continue;
            }
            relevantes++;
            if (ptLower.contains(normalizada.toLowerCase(Locale.ROOT))) {
                encontrados++;
            }
        }
        return relevantes >= 2 && encontrados > 0 && encontrados < relevantes;
    }

    private boolean contemAlgumaExpressao(String textoLower, List<String> expressoes) {
        return expressoes.stream().anyMatch(expressao -> contemExpressaoInteira(textoLower, expressao));
    }

    private boolean contemPalavraInteira(String textoLower, String palavraLower) {
        return contemExpressaoInteira(textoLower, palavraLower);
    }

    private boolean contemExpressaoInteira(String textoLower, String expressaoLower) {
        // Expressao de lore e multi-palavra ("capital tower"); partida pela quebra, deixava de
        // ser detectada. Ver FronteiraTermoAss.
        return FronteiraTermoAss.padraoIgnorandoCaixa(expressaoLower)
            .matcher(textoLower)
            .find();
    }

    private static Map<String, List<String>> criarTraducoesLiteraisSuspeitas() {
        Map<String, List<String>> termos = new LinkedHashMap<>();
        termos.put("narrative", List.of("narrativo", "narrativa"));
        termos.put("unicorn", List.of("unicórnio", "unicornio"));
        termos.put("phenex", List.of("fênix", "fenix"));
        termos.put("freedom", List.of("liberdade"));
        termos.put("justice", List.of("justiça", "justica"));
        termos.put("destiny", List.of("destino"));
        termos.put("stargazer", List.of("observador de estrelas", "observadora de estrelas"));
        termos.put("war in the pocket", List.of("guerra no bolso", "guerra de bolso"));
        termos.put("mobile suit", List.of("traje móvel", "traje movel", "roupa móvel", "roupa movel"));
        termos.put("mobile armor", List.of("armadura móvel", "armadura movel"));
        termos.put("newtype", List.of("novo tipo", "nova tipo"));
        termos.put("handler", List.of("manipulador", "manipuladora"));
        termos.put("processor", List.of("processador", "processadora"));
        termos.put("juggernaut", List.of("rolo compressor"));
        termos.put("shin", List.of("canela"));
        termos.put("dud rounds", List.of(
            "rodadas aleatorias",
            "rodadas aleatórias",
            "rodadas fracassadas",
            "rodadas dud",
            "rodadas falsas"
        ));
        return Map.copyOf(termos);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: cataloga equivalências PT-BR legítimas de títulos,
     * organizações e conceitos que não precisam permanecer em inglês.
     * <p>INVARIANTES DO DOMÍNIO: chaves ficam em inglês minúsculo e variantes
     * incluem grafias com e sem diacríticos quando necessário.
     * <p>COMPORTAMENTO EM CASO DE FALHA: o mapa final é imutável.
     */
    private static Map<String, List<String>> criarTermosTraduziveisAceitos() {
        Map<String, List<String>> termos = new LinkedHashMap<>();
        termos.put("earth federation", List.of(
            "federacao terrestre",
            "federação terrestre",
            "federacao da terra",
            "federação da terra"
        ));
        termos.put("federation", List.of("federacao", "federação"));
        termos.put("principality of zeon", List.of("principado de zeon"));
        termos.put("republic of zeon", List.of("republica de zeon", "república de zeon"));
        termos.put("universal century", List.of("seculo universal", "século universal"));
        termos.put("laplace declaration", List.of("declaracao de laplace", "declaração de laplace"));
        termos.put("earth", List.of("terra"));
        termos.put("minister", List.of("ministro", "ministra"));
        termos.put("princess", List.of("princesa"));
        termos.put("commander", List.of("comandante"));
        termos.put("ensign", List.of("alferes"));
        return Map.copyOf(termos);
    }
}
