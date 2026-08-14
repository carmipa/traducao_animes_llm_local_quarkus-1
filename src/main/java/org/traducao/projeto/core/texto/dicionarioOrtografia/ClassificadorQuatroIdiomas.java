package org.traducao.projeto.core.texto.dicionarioOrtografia;

import java.text.Normalizer;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: classifica cada palavra de uma legenda consultando os QUATRO idiomas que
 * aparecem no acervo — português (o alvo), inglês (a origem), alemão (a lore) e japonês (a obra).
 *
 * <h2>A ordem das perguntas é a garantia</h2>
 * Só o português DECIDE. Os outros três rotulam o que já não é português, e isso muda a AÇÃO, não
 * o veredito. A cicatriz que fixou essa ordem foi medida: {@code Resonância} está errada (o certo
 * é <i>ressonância</i>), o pt_BR reprovou, e o de_DE aceitou. Se o alemão pudesse aprovar, um erro
 * real teria passado — e quanto mais dicionários, mais chances disso.
 *
 * <h2>Um processo por idioma, por LOTE</h2>
 * São no máximo três chamadas ao hunspell para um episódio inteiro, não uma por palavra: o custo é
 * o arranque do processo. Medido: 7.432 formas em 12,6 s. E o japonês nem consulta nada — kana e
 * kanji são faixas Unicode, resolvidas em memória.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Idioma cujo dicionário não respondeu não rotula nada: fica {@link VeredictoPalavra#DESCONHECIDA}
 *       em vez de virar aprovação silenciosa.</li>
 *   <li>Nenhum dicionário disponível devolve tudo como {@link VeredictoPalavra#NAO_VERIFICADO}.</li>
 *   <li>Não corrige nada: classifica. Quem corrige é {@link CorretorAcentoPorDicionario}, e só o
 *       veredicto de acento autoriza.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nunca lança. Entrada vazia devolve mapa vazio.
 */
public final class ClassificadorQuatroIdiomas {

    // \p{IsHan} para o kanji, e NÃO \p{IsCJKUnifiedIdeographs}: aquela é sintaxe .NET, que
    // funciona no PowerShell da medição e explode em Java com PatternSyntaxException. Descoberto
    // portando a mesma regex do script para cá.
    private static final Pattern JAPONES = Pattern.compile(
        "[\\p{IsHiragana}\\p{IsKatakana}\\p{IsHan}]");

    private final DicionarioOrtograficoPort portugues;
    private final DicionarioOrtograficoPort ingles;
    private final DicionarioOrtograficoPort alemao;
    private final DicionarioOrtograficoPort frances;
    private final DicionarioOrtograficoPort romaji;

    /**
     * PROPÓSITO DE NEGÓCIO: recebe um verificador por idioma ocidental; o japonês não precisa de
     * dicionário porque se reconhece pela escrita.
     *
     * <p>INVARIANTES DO DOMÍNIO: qualquer um pode estar indisponível, e o classificador continua
     * funcionando com o que sobrou — declarando o que não pôde julgar. O francês pode ser
     * {@code null} nas chamadas antigas, e nesse caso simplesmente não rotula.
     */
    public ClassificadorQuatroIdiomas(DicionarioOrtograficoPort portugues,
            DicionarioOrtograficoPort ingles, DicionarioOrtograficoPort alemao) {
        this(portugues, ingles, alemao, null);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mesma coisa, com o francês — necessário desde que o acervo passou a
     * ter obra traduzida A PARTIR do francês.
     *
     * <h2>O prejuízo que obrigou a existir</h2>
     * No primeiro run do {@code Memories} a partir da faixa francesa, o detector de nome próprio
     * acusou seis palavras e CINCO eram francês comum — {@code Dieu}, {@code Octobre},
     * {@code Juillet}, {@code Maman}, {@code Californie}. Sem dicionário francês elas não são
     * palavra de idioma nenhum, que é exatamente a assinatura de nome inventado da obra.
     */
    public ClassificadorQuatroIdiomas(DicionarioOrtograficoPort portugues,
            DicionarioOrtograficoPort ingles, DicionarioOrtograficoPort alemao,
            DicionarioOrtograficoPort frances) {
        this(portugues, ingles, alemao, frances, null);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a forma completa, com o dicionário de ROMAJI — japonês escrito em
     * alfabeto latino, que nenhum dos outros reconhece e que aparece em toda legenda de anime.
     *
     * <p>INVARIANTES DO DOMÍNIO: o romaji é consultado por ÚLTIMO e produz
     * {@link VeredictoPalavra#ROMAJI}, que NÃO isenta a palavra da checagem de nome próprio — o
     * dicionário do IPADIC é cheio de sobrenome japonês e trata-lo como "palavra comum" cegaria o
     * detector no caso mais frequente do acervo.
     */
    public ClassificadorQuatroIdiomas(DicionarioOrtograficoPort portugues,
            DicionarioOrtograficoPort ingles, DicionarioOrtograficoPort alemao,
            DicionarioOrtograficoPort frances, DicionarioOrtograficoPort romaji) {
        this.portugues = portugues;
        this.ingles = ingles;
        this.alemao = alemao;
        this.frances = frances;
        this.romaji = romaji;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o veredicto de cada palavra recebida.
     *
     * <p>INVARIANTES DO DOMÍNIO: uma consulta por idioma para o lote inteiro; a ordem das perguntas
     * é português, depois acento, depois inglês, depois alemão.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem português disponível, tudo vira
     * {@link VeredictoPalavra#NAO_VERIFICADO}.
     */
    public Map<String, VeredictoPalavra> classificar(Collection<String> palavras) {
        Map<String, VeredictoPalavra> fora = new LinkedHashMap<>();
        if (palavras == null || palavras.isEmpty()) {
            return fora;
        }
        Set<String> todas = new LinkedHashSet<>(palavras);

        Set<String> comJapones = separarJapones(todas, fora);
        Set<String> paraConsultar = new LinkedHashSet<>(todas);
        paraConsultar.removeAll(comJapones);

        Map<String, Set<String>> naoPt = portugues.sugestoes(paraConsultar);
        if (!portugues.disponivel()) {
            paraConsultar.forEach(p -> fora.put(p, VeredictoPalavra.NAO_VERIFICADO));
            return fora;
        }

        Set<String> comAcento = CorretorAcentoPorDicionario.apenasAcentuacoes(naoPt).keySet();
        Set<String> restantes = new LinkedHashSet<>(naoPt.keySet());
        restantes.removeAll(comAcento);

        Set<String> naoEn = restantes.isEmpty() ? Set.of() : ingles.desconhecidas(restantes);
        Set<String> naoDe = restantes.isEmpty() ? Set.of() : alemao.desconhecidas(restantes);
        Set<String> naoFr = (restantes.isEmpty() || frances == null)
            ? Set.of() : frances.desconhecidas(restantes);
        Set<String> naoRomaji = (restantes.isEmpty() || romaji == null)
            ? Set.of() : romaji.desconhecidas(restantes);

        for (String p : paraConsultar) {
            fora.put(p, vereditoDe(p, naoPt.keySet(), comAcento, naoEn, naoDe, naoFr, naoRomaji,
                restantes));
        }
        return fora;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: tira do caminho o que se reconhece pela ESCRITA, antes de gastar
     * consulta.
     *
     * <p>INVARIANTES DO DOMÍNIO: kana e kanji não passam por dicionário nenhum — nenhum
     * verificador de lista funciona para japonês, que não separa palavras por espaço.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança.
     */
    private Set<String> separarJapones(Set<String> todas, Map<String, VeredictoPalavra> fora) {
        Set<String> japonesas = new LinkedHashSet<>();
        for (String p : todas) {
            if (JAPONES.matcher(p).find()) {
                japonesas.add(p);
                fora.put(p, VeredictoPalavra.JAPONES);
            }
        }
        return japonesas;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aplica a ordem das perguntas a UMA palavra.
     *
     * <p>INVARIANTES DO DOMÍNIO: inglês e alemão só são consultados depois de o português ter
     * reprovado — e mesmo então apenas rotulam.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: idioma secundário indisponível não rotula, e a palavra
     * cai em {@link VeredictoPalavra#DESCONHECIDA}.
     */
    private VeredictoPalavra vereditoDe(String palavra, Set<String> naoPt, Set<String> comAcento,
            Set<String> naoEn, Set<String> naoDe, Set<String> naoFr, Set<String> naoRomaji,
            Set<String> restantes) {
        if (!naoPt.contains(palavra)) {
            return VeredictoPalavra.PORTUGUES_OK;
        }
        if (comAcento.contains(palavra)) {
            return VeredictoPalavra.ACENTO_FALTANDO;
        }
        if (!restantes.contains(palavra)) {
            return VeredictoPalavra.DESCONHECIDA;
        }
        if (ingles.disponivel() && !naoEn.contains(palavra)) {
            return VeredictoPalavra.RESIDUO_INGLES;
        }
        if (alemao.disponivel() && !naoDe.contains(palavra)) {
            return VeredictoPalavra.TERMO_ALEMAO;
        }
        // O francês vem por ÚLTIMO de propósito: é o dicionário mais recente e o menos usado como
        // origem. Ordem diferente mudaria o rótulo de palavras que existem em mais de um idioma
        // (o clássico "important", que é inglês e francês) sem mudar o que importa — em todos os
        // casos a palavra deixa de ser DESCONHECIDA, e é isso que o detector de nome próprio lê.
        if (frances != null && frances.disponivel() && !naoFr.contains(palavra)) {
            return VeredictoPalavra.TERMO_FRANCES;
        }
        // O romaji fecha a fila, e o rótulo NÃO isenta de nada: quem lê este veredicto para
        // decidir sobre nome próprio precisa tratá-lo como candidato, porque o IPADIC reconhece
        // sobrenome japonês (Aoshima, e o .dic abre em aarajima/aatsukawa).
        if (romaji != null && romaji.disponivel() && !naoRomaji.contains(palavra)) {
            return VeredictoPalavra.ROMAJI;
        }
        return VeredictoPalavra.DESCONHECIDA;
    }

    /** Forma sem diacrítico — usada pelos testes para comparar identidade de palavra. */
    static String semAcento(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
