package org.traducao.projeto.revisaoConcordancia.application;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.core.texto.FronteiraTermoAss;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;
import org.traducao.projeto.core.texto.dicionarioOrtografia.VeredictoPalavra;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: tira da legenda o caractere que <b>não pertence ao português</b> e que o
 * modelo deixou para trás — o espaço invisível, o macron no lugar do til, a pontuação espanhola.
 *
 * <h2>O inventário que originou cada regra (24/08/2026)</h2>
 * Nada aqui foi imaginado. Um inventário cru dos 222 arquivos do acervo listou <b>15 caracteres</b>
 * fora do alfabeto esperado, e cada um foi lido fala a fala antes de virar regra ou ser recusado:
 *
 * <pre>
 *   U+200B  espaco de largura zero   5 falas   INVISIVEL — "responsáveis[__]pela"   -> APAGA
 *   U+0101  a com macron             6 falas   "opçāo", "coraçāo", "instalaçāo"     -> VIRA TIL
 *   U+00A1  exclamacao invertida     2 falas   "algo? ¡Na minha cabeça não!"        -> APAGA
 *   U+00B4  acento agudo solto       8 falas   {\pos(1093,1027)}´ — TIPOGRAFIA      -> nao toca
 *   U+01B0  u com chifre             1 fala    "Seis mươi" (vietnamita)             -> nao toca
 *   U+00B0  grau como ordinal        2 falas   "n° 7", "18° andar"                  -> nao toca
 *   U+00AA/BA ordinal feminino/masc  59 falas  "24ª Divisão", "16º Distrito"        -> CORRETO
 *   U+00F0  eth                      3 falas   "Læraðr" — nome nordico do 86        -> CORRETO
 * </pre>
 *
 * <h2>Por que três ficaram de fora, e isso não é preguiça</h2>
 * <ul>
 *   <li><b>O acento solto</b> vem em oito falas com {@code \\pos} de coordenada fracionária e
 *       {@code \\i1}: é um letreiro desenhado glifo a glifo, não prosa. Apagar quebraria a
 *       tipografia.</li>
 *   <li><b>O vietnamita</b> — {@code "Seis mươi, setenta"} — precisa de TRADUÇÃO, não de troca de
 *       caractere. Vira relatório; conserto automático aqui inventaria palavra.</li>
 *   <li><b>O grau como ordinal</b> são duas falas e ninguém tropeça lendo {@code "18° andar"}.
 *       A régua deste projeto é <i>atrapalha ler?</i>, e uma regra nova que arrisca casar
 *       {@code "20°C"} custa mais do que o defeito que conserta.</li>
 * </ul>
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O macron só vira til se o <b>DICIONÁRIO de produção</b> aceitar a forma com til e rejeitar
 *       a com macron. O dicionário é JUIZ, nunca proponente: pedir sugestão a ele é o que produziu
 *       {@code terra→terrá} em 23/08/2026. Assim o romaji legítimo ({@code Ōsaka}) fica intacto
 *       sozinho, porque {@code Õsaka} não é palavra portuguesa.</li>
 *   <li>A pontuação espanhola só sai de fala <b>provadamente portuguesa</b>
 *       ({@link CorretorAcentoPorPadraoService#FALA_E_PORTUGUESA}).</li>
 *   <li>O macron só é tocado <b>fora</b> de {@code {...}}: nome de fonte é conteúdo de tag e não
 *       se corrige ortografia de tipografia.</li>
 *   <li>Dicionário indisponível NÃO troca macron — falha fechada. O espaço invisível e a
 *       pontuação espanhola não dependem dele.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Texto nulo ou em branco devolve {@link Optional#empty()}. Nunca lança.
 */
@ApplicationScoped
public class CorretorCaractereForaDoPortuguesService {

    /**
     * A família dos invisíveis. O acervo só tem U+200B hoje, mas os irmãos entram junto porque são
     * o mesmo defeito com outro código: nenhum deles tem significado numa legenda, e todos quebram
     * comparação de palavra em qualquer ferramenta que leia o arquivo depois — inclusive os outros
     * corretores desta mesma cadeia. Varrer a classe inteira é mais barato que voltar aqui.
     */
    private static final Pattern INVISIVEL =
        Pattern.compile("[\u200B\u200C\u200D\uFEFF\u00AD]");

    /** Pontuação de abertura do espanhol. Não existe em português, em nenhum contexto. */
    private static final Pattern ABERTURA_ESPANHOLA = Pattern.compile("[¡¿]\\s*");

    /** Blocos de tag do ASS — o macron dentro deles é nome de fonte e não se toca. */
    private static final Pattern TAG_ASS = Pattern.compile("\\{[^{}]*}");

    /**
     * Macron onde o português teria til. Só {@code a} e {@code o} entram: {@code ē}, {@code ī} e
     * {@code ū} não têm forma com til no idioma, então trocá-los não teria para onde ir.
     */
    private static final Map<Character, Character> MACRON_PARA_TIL = Map.of(
        'ā', 'ã', 'ō', 'õ', 'Ā', 'Ã', 'Ō', 'Õ');

    /** Palavra que contém macron, com a fronteira canônica do ASS (a quebra {@code \\N} conta). */
    private static final Pattern PALAVRA_COM_MACRON = Pattern.compile(
        FronteiraTermoAss.INICIO + "([\\p{L}]*[āōĀŌ][\\p{L}]*)" + FronteiraTermoAss.FIM);

    private final CorretorOrtograficoLegenda dicionario;

    public CorretorCaractereForaDoPortuguesService(CorretorOrtograficoLegenda dicionario) {
        this.dicionario = dicionario;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve a fala sem os caracteres que não são do português, ou vazio se
     * não havia nenhum.
     *
     * <p>INVARIANTES DO DOMÍNIO: tags e quebras {@code \\N} voltam byte a byte; o macron dentro de
     * tag não é tocado; troca de macron exige aval do dicionário.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@link Optional#empty()} para texto nulo, em branco ou
     * já limpo. Nunca lança.
     */
    public Optional<String> corrigir(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String novo = INVISIVEL.matcher(texto).replaceAll("");
        if (CorretorAcentoPorPadraoService.FALA_E_PORTUGUESA.matcher(novo).find()) {
            novo = ABERTURA_ESPANHOLA.matcher(novo).replaceAll("");
        }
        novo = trocarMacronAprovado(novo);
        return novo.equals(texto) ? Optional.empty() : Optional.of(novo);
    }

    /** Diz se o dicionário está de pé — para o relatório separar "limpo" de "não verifiquei". */
    public boolean disponivel() {
        return dicionario.disponivel();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: troca macron por til apenas nas palavras em que o dicionário confirma
     * que a forma com til é portuguesa e a com macron não é.
     *
     * <h2>Por que o dicionário entra como juiz e não como proponente</h2>
     * A tentação é pedir a sugestão dele para {@code opçāo}. Foi assim que uma medição de
     * 23/08/2026 produziu {@code terra→terrá}, {@code batalha→batalhá} e {@code garota→garotá}:
     * saber que existe uma variante não diz qual das duas cabe na frase. Aqui a proposta é
     * determinística — o macron vira til, e nada mais — e ao dicionário se faz uma pergunta
     * fechada, de sim ou não, sobre as duas formas.
     *
     * <p>INVARIANTES DO DOMÍNIO: só toca o texto fora de {@code {...}}; posições preservadas por
     * substituição de mesmo comprimento.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: dicionário indisponível ou veredicto
     * {@link VeredictoPalavra#NAO_VERIFICADO} devolve o texto como veio.
     */
    private String trocarMacronAprovado(String texto) {
        Set<String> candidatas = new LinkedHashSet<>();
        Matcher varredura = PALAVRA_COM_MACRON.matcher(semTags(texto));
        while (varredura.find()) {
            candidatas.add(varredura.group(1));
        }
        if (candidatas.isEmpty()) {
            return texto;
        }
        Set<String> aprovadas = aprovadasPeloDicionario(candidatas);
        if (aprovadas.isEmpty()) {
            return texto;
        }
        // Só as aprovadas são trocadas, e uma a uma pelo texto inteiro: a palavra tem a MESMA
        // forma dentro e fora de tag, então casar de novo com a fronteira do ASS é o que impede
        // trocar um nome de fonte que por acaso repita a palavra.
        StringBuilder sb = new StringBuilder();
        int fim = 0;
        Matcher m = PALAVRA_COM_MACRON.matcher(texto);
        Matcher tags = TAG_ASS.matcher(texto);
        while (m.find()) {
            if (!aprovadas.contains(m.group(1)) || dentroDeTag(tags, m.start(1))) {
                continue;
            }
            sb.append(texto, fim, m.start(1)).append(comTil(m.group(1)));
            fim = m.end(1);
        }
        return sb.append(texto.substring(fim)).toString();
    }

    /**
     * Pergunta fechada ao dicionário: a forma com til é portuguesa e a com macron não é?
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: dicionário mudo devolve conjunto vazio, e nada é trocado.
     */
    private Set<String> aprovadasPeloDicionario(Set<String> candidatas) {
        Set<String> perguntas = new LinkedHashSet<>();
        for (String c : candidatas) {
            perguntas.add(c);
            perguntas.add(comTil(c));
        }
        Map<String, VeredictoPalavra> vereditos = dicionario.classificar(perguntas);
        Set<String> aprovadas = new LinkedHashSet<>();
        for (String c : candidatas) {
            VeredictoPalavra comMacron = vereditos.get(c);
            VeredictoPalavra comTil = vereditos.get(comTil(c));
            boolean julgou = comMacron != null && comMacron != VeredictoPalavra.NAO_VERIFICADO
                && comTil != null && comTil != VeredictoPalavra.NAO_VERIFICADO;
            if (julgou && comTil == VeredictoPalavra.PORTUGUES_OK
                && comMacron != VeredictoPalavra.PORTUGUES_OK) {
                aprovadas.add(c);
            }
        }
        return aprovadas;
    }

    /** A mesma palavra com o macron virado til; mesmo comprimento, para as posições não andarem. */
    private static String comTil(String palavra) {
        StringBuilder sb = new StringBuilder(palavra.length());
        for (char c : palavra.toCharArray()) {
            sb.append(MACRON_PARA_TIL.getOrDefault(c, c));
        }
        return sb.toString();
    }

    /** O texto com os blocos de tag virados espaço — mesmo comprimento, para varrer só a prosa. */
    private static String semTags(String texto) {
        return TAG_ASS.matcher(texto).replaceAll(m -> " ".repeat(m.group().length()));
    }

    /** Diz se a posição cai dentro de algum bloco {@code {...}}. */
    private static boolean dentroDeTag(Matcher tags, int posicao) {
        tags.reset();
        while (tags.find()) {
            if (posicao >= tags.start() && posicao < tags.end()) {
                return true;
            }
        }
        return false;
    }
}
