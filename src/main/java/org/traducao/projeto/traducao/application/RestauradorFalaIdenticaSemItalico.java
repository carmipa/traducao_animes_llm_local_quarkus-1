package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Component;
import org.traducao.projeto.qualidadeTraducao.application.DetectorTraducaoIdenticaService;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.domain.LoreAtivaPort;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: recupera a fala que é só um nome próprio e que o LLM devolveu correta,
 * mas sem o itálico. {@code {\i1}Eledore!} traduzido é {@code {\i1}Eledore!} — nome não se
 * traduz. Quando o modelo perde o marcador {@code [[TAGn]]} dessa fala, o
 * {@link DescarteItalicoUltimoRecurso} entrega {@code Eledore!} sem itálico, e o portão de
 * {@link AvaliadorTraducaoCache#motivoFalhaFinal} reprova por "tags divergentes" — a fala volta
 * a ser publicada EM INGLÊS e entra na lista de pendências. Este componente reconhece o caso e
 * devolve o ORIGINAL INTACTO: texto certo e itálico preservado.
 *
 * <p>Medido na execução do 08th MS Team em 2026-07-31 (13 arquivos): das 16 falas com itálico
 * descartado, <b>8 terminaram presas exatamente assim</b> e saíram em inglês na tela —
 * {@code {\i1}Eledore!}, {@code {\i1}Michel!}, {@code {\i1}Shiro! Shiro!}, {@code {\i1}Karen!},
 * {@code {\i1}Sanders!!!{\i0}}, {@code {\i1}Sanders...}, {@code {\i1}Eledore...} e
 * {@code {\i1}Eledore! Michel!}. As outras 8 foram recuperadas por retentativa e não passam
 * por aqui, porque o texto visível delas MUDOU.
 *
 * <p>Por que devolver o original em vez da tradução sem itálico: o texto visível é o mesmo nos
 * dois, então o original é estritamente melhor — não perde a ênfase que o fansub escreveu.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O texto VISÍVEL do original e o da tradução têm de ser idênticos. Se o modelo mudou
 *       uma palavra que fosse, este não é o caso e nada é restaurado.</li>
 *   <li>Toda tag ausente na tradução tem de ser <b>exclusivamente de itálico</b>. Perder
 *       {@code \pos}, {@code \an8} ou {@code \N} muda o que se vê na tela e cancela a
 *       restauração.</li>
 *   <li>Tag INVENTADA pelo modelo (presente na tradução e ausente no original) cancela — é
 *       corrupção, não perda.</li>
 *   <li>TODAS as palavras da fala têm de pertencer aos termos protegidos da obra. É esta
 *       condição — e não a capitalização — que separa {@code Eledore!} de {@code Shoot!}.</li>
 *   <li>A identidade precisa ser LEGÍTIMA segundo
 *       {@link DetectorTraducaoIdenticaService#deveManterIdentico}: nome, sigla, número ou
 *       termo de lore. Sem essa condição, um eco do modelo numa fala que DEVIA ser traduzida
 *       entraria como se fosse tradução — o portão viraria porta dos fundos.</li>
 *   <li>Sem estado; só JDK + Spring.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Devolve {@code null} sempre que não puder agir — o chamador mantém a fala pendente, como
 * antes. Nunca lança.
 */
@Component
public class RestauradorFalaIdenticaSemItalico {

    /** Marcador deixado por {@link MascaradorTags} no lugar de cada tag. */
    private static final Pattern MARCADOR = Pattern.compile("\\[\\[TAG\\d+]]");

    /** Separa a fala em palavras, descartando pontuação e reticências. */
    private static final Pattern NAO_LETRA = Pattern.compile("[^\\p{L}\\p{N}]+");

    private final MascaradorTags mascarador;
    private final DetectorTraducaoIdenticaService detectorIdentica;
    private final DescarteItalicoUltimoRecurso descarteItalico;
    private final LoreAtivaPort loreAtiva;

    /**
     * PROPÓSITO DE NEGÓCIO: injeta quem sabe separar tag de texto, quem decide se uma fala
     * idêntica é legítima e quem reconhece uma tag puramente de itálico.
     *
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas; não cria implementação própria.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não valida argumentos; a injeção garante os beans.
     */
    public RestauradorFalaIdenticaSemItalico(
        MascaradorTags mascarador,
        DetectorTraducaoIdenticaService detectorIdentica,
        DescarteItalicoUltimoRecurso descarteItalico,
        LoreAtivaPort loreAtiva
    ) {
        this.mascarador = mascarador;
        this.detectorIdentica = detectorIdentica;
        this.descarteItalico = descarteItalico;
        this.loreAtiva = loreAtiva;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve o original intacto quando a única diferença entre ele e a
     * tradução é itálico perdido sobre um texto que tinha o direito de não mudar.
     *
     * <p>INVARIANTES DO DOMÍNIO: os cinco testes da classe rodam em ordem e qualquer um deles
     * falhando devolve {@code null}. A saída, quando não nula, é o próprio {@code original} —
     * nunca um texto construído.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada nula ou em branco devolve {@code null}.
     *
     * @param original a fala como veio da legenda, com as tags
     * @param traduzido o que o pipeline pretende publicar
     * @return {@code original} quando a restauração é segura, {@code null} caso contrário
     */
    public String restaurarSePossivel(String original, String traduzido) {
        if (original == null || traduzido == null || traduzido.isBlank()) {
            return null;
        }
        MascaradorTags.Mascarado esperado = mascarador.mascarar(original);
        MascaradorTags.Mascarado obtido = mascarador.mascarar(traduzido);
        if (esperado.tags().equals(obtido.tags())) {
            return null;  // estrutura já bate: não é este caso, o portão decide sozinho
        }
        if (!visivel(esperado.texto()).equals(visivel(obtido.texto()))) {
            return null;  // o modelo mudou o texto: perder o itálico aqui teria custo real
        }
        if (!perdeuSomenteItalico(esperado.tags(), obtido.tags())) {
            return null;
        }
        if (!todasAsPalavrasSaoTermoDeLore(visivel(esperado.texto()))) {
            return null;
        }
        return detectorIdentica.deveManterIdentico(original) ? original : null;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: exige que a fala inteira seja feita de palavras que a LORE DA OBRA
     * declarou proteger — é o que separa {@code Eledore!} de {@code Shoot!}.
     *
     * <p>Esta condição existe porque {@link DetectorTraducaoIdenticaService#deveManterIdentico}
     * sozinho NÃO serve de fiador: medido em 2026-07-31, ele devolve {@code true} para
     * {@code Shoot!}, {@code Liar!}, {@code Traitor!}, {@code Visual Contact!} e
     * {@code Emergency Landing!} — inglês corrente que precisa de tradução. A régua dele é
     * capitalização, e capitalização não distingue nome próprio de exclamação. Sem esta
     * checagem, o resgate publicaria essas falas em inglês e ainda as tiraria da lista de
     * pendências, matando a chance de retentativa e de fallback.
     *
     * <p>INVARIANTES DO DOMÍNIO: compara por PALAVRA contra os termos protegidos da obra ativa,
     * decompondo termos compostos — {@code "Eledore Massis"} protege {@code Eledore} sozinho,
     * que é como o personagem é chamado em cena. Fala vazia de palavras devolve {@code false}:
     * sem palavra não há prova de nada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lore sem termos protegidos devolve {@code false} — o
     * resgate simplesmente não age, e a fala segue pendente como antes.
     */
    private boolean todasAsPalavrasSaoTermoDeLore(String textoVisivel) {
        Set<String> protegidas = palavrasProtegidas();
        if (protegidas.isEmpty()) {
            return false;
        }
        String[] palavras = NAO_LETRA.split(textoVisivel);
        boolean achouAlguma = false;
        for (String palavra : palavras) {
            if (palavra.isBlank()) {
                continue;
            }
            achouAlguma = true;
            if (!protegidas.contains(palavra.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return achouAlguma;
    }

    /** Todas as palavras que aparecem em algum termo protegido da obra, em minúsculas. */
    private Set<String> palavrasProtegidas() {
        Set<String> palavras = new HashSet<>();
        for (String termo : loreAtiva.termosProtegidosAtivos()) {
            if (termo == null) {
                continue;
            }
            for (String parte : NAO_LETRA.split(termo)) {
                if (!parte.isBlank()) {
                    palavras.add(parte.toLowerCase(Locale.ROOT));
                }
            }
        }
        return palavras;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: confirma que a tradução só PERDEU tags de itálico, sem inventar
     * nenhuma e sem perder tag de outra natureza.
     *
     * <p>INVARIANTES DO DOMÍNIO: compara por multiconjunto, não por conjunto — uma fala com dois
     * {@code {\i1}} que perde só um continua sendo perda de itálico. Lista vazia de faltantes
     * devolve {@code false}: sem perda não há o que restaurar.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer tag não reconhecida como itálico devolve
     * {@code false}.
     */
    private boolean perdeuSomenteItalico(List<String> tagsOriginal, List<String> tagsTraduzido) {
        List<String> faltantes = new ArrayList<>(tagsOriginal);
        for (String tag : tagsTraduzido) {
            faltantes.remove(tag);
        }
        List<String> inventadas = new ArrayList<>(tagsTraduzido);
        for (String tag : tagsOriginal) {
            inventadas.remove(tag);
        }
        if (faltantes.isEmpty() || !inventadas.isEmpty()) {
            return false;
        }
        for (String tag : faltantes) {
            if (!descarteItalico.ehSomenteItalico(tag)) {
                return false;
            }
        }
        return true;
    }

    /** Texto do mascaramento sem os marcadores, com espaços colapsados. */
    private String visivel(String textoMascarado) {
        return MARCADOR.matcher(textoMascarado).replaceAll("").replaceAll("\\s+", " ").strip();
    }
}
