package org.traducao.projeto.medicao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.medicao.LeitorAcervoCache.Acervo;
import org.traducao.projeto.medicao.LeitorAcervoCache.FalaDoAcervo;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * PROPÓSITO DE NEGÓCIO: separar fala que nasce VAZIA no cache de fala que é esvaziada DEPOIS.
 *
 * <h2>A pergunta que este harness responde</h2>
 * A versão final do Guilty Crown tem falas em branco nos episódios 14, 16 e 17. A suspeita é do
 * menu de karaokê, mas suspeita não é diagnóstico — e o pipeline tem várias etapas entre o cache
 * e o {@code .ass} entregue. Este medidor fecha a primeira bifurcação: <b>se a fala já está vazia
 * no cache, o defeito é da tradução; se não está, é de alguma etapa posterior</b>, e aí a busca
 * continua para frente em vez de vasculhar tudo.
 *
 * <p>Fala vazia no cache NÃO é necessariamente defeito: pendência gravada em branco é o
 * comportamento declarado quando o LLM falha e o portão recusa. O que importa é o CONTRASTE entre
 * obra e obra, e entre episódio e episódio da mesma obra.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY.</li>
 *   <li>Conta separadamente {@code traduzido} VAZIO e {@code traduzido} EM BRANCO (só espaços ou
 *       só tags ASS) — o segundo aparece preenchido a olho nu e some na tela do mesmo jeito.</li>
 *   <li>Reporta por ARQUIVO, não só por obra: "4 falas vazias" numa série de 23 episódios é
 *       diagnóstico diferente de "4 falas vazias" concentradas em um episódio.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo ausente termina com aviso; não lança.
 *
 * <p>Uso: {@code gradlew test --tests "*MedicaoFalasVaziasIT*" "-Dkronos.medicao=true"}
 * (opcional: {@code "-Dkronos.medicao.obra=Guilty Crown"} filtra por trecho do nome da pasta)
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoFalasVaziasIT {

    private static final String CHAVE_OBRA = "kronos.medicao.obra";

    /** Sobra depois de tirar tags ASS e quebras: o que o espectador realmente leria. */
    private static String visivel(String texto) {
        return texto.replaceAll("\\{[^}]*}", "")
            .replaceAll("\\\\[Nnh]", " ")
            .strip();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE (regra 9) do único instrumento desta medição — o
     * {@link #visivel(String)}, que decide o que sobra para o espectador ler.
     *
     * <p>INVARIANTES DO DOMÍNIO: tem de achar a fala que é SÓ ornamento (tag e quebra, nada de
     * texto) e tem de calar na fala com texto de verdade. Um {@code visivel()} que devolvesse
     * sempre vazio reportaria o acervo inteiro como perdido; um que nunca devolvesse vazio
     * reportaria zero — e os dois números sairiam com a mesma cara de medição.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: imprime e devolve {@code false}; nenhum número é afirmado.
     */
    private static boolean instrumentoCalibrado() {
        boolean achaOrnamento = visivel("{\\an8\\pos(10,20)}\\N\\h").isEmpty();
        boolean calaNoTexto = !visivel("{\\an8}Capitao, a nave chegou.").isEmpty();
        if (achaOrnamento && calaNoTexto) {
            System.out.println("  controle: ve a fala so de ornamento · cala na fala com texto");
            return true;
        }
        System.out.printf("INSTRUMENTO REPROVADO NO CONTROLE — ornamento=%s texto=%s. "
            + "Nenhum numero e afirmado.%n", achaOrnamento, calaNoTexto);
        return false;
    }

    @Test
    @DisplayName("acervo: falas que ja nascem VAZIAS no cache, por obra e por arquivo")
    void medir() throws IOException {
        if (!instrumentoCalibrado()) {
            return;
        }
        Acervo acervo = LeitorAcervoCache.ler(LeitorAcervoCache.raizPadrao());
        if (acervo.vazio()) {
            System.out.println("NAO VERIFICADO: acervo de cache vazio — zero aqui seria cegueira "
                + "do instrumento, e nao ausencia de fala vazia.");
            return;
        }
        String filtro = System.getProperty(CHAVE_OBRA);

        Map<String, int[]> porObra = new TreeMap<>();
        Map<String, List<Integer>> porArquivo = new LinkedHashMap<>();

        for (FalaDoAcervo f : acervo.falas()) {
            if (filtro != null && !f.obra().toLowerCase().contains(filtro.toLowerCase())) {
                continue;
            }
            int[] c = porObra.computeIfAbsent(f.obra(), k -> new int[3]);
            c[0]++;
            boolean semTexto = f.traduzido().isEmpty();
            boolean soOrnamento = !semTexto && visivel(f.traduzido()).isEmpty();
            if (semTexto) {
                c[1]++;
            }
            if (soOrnamento) {
                c[2]++;
            }
            if ((semTexto || soOrnamento) && !visivel(f.original()).isEmpty()) {
                // Só interessa quando o ORIGINAL tinha texto: fala vazia dos dois lados é
                // marcador de cena, não perda.
                // A chave e o NOME DO ARQUIVO, nao obra+arquivo: a pasta da obra costuma ser o
                // nome de release inteiro ("[Anime Time] ... [BD][Dual Audio][1080p][HEVC...]") e
                // qualquer recorte razoavel comia exatamente o episodio, que e o dado procurado.
                porArquivo.computeIfAbsent(f.arquivo().getFileName().toString(),
                        k -> new java.util.ArrayList<>())
                    .add(f.entrada().indice());
            }
        }

        System.out.printf("%n%-46s %8s %8s %8s%n", "OBRA", "falas", "vazias", "so-tag");
        porObra.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue()[1] + b.getValue()[2],
                a.getValue()[1] + a.getValue()[2]))
            .forEach(e -> System.out.printf("%-46s %8d %8d %8d%n",
                recortar(e.getKey()), e.getValue()[0], e.getValue()[1], e.getValue()[2]));

        System.out.printf("%n=== ARQUIVOS com fala perdida (original TINHA texto) ===%n");
        porArquivo.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
            .limit(20)
            .forEach(e -> System.out.printf("  %-58s %3d fala(s)  indices %s%n",
                recortar(e.getKey()), e.getValue().size(),
                e.getValue().size() <= 8 ? e.getValue() : e.getValue().subList(0, 8) + "…"));
        if (porArquivo.isEmpty()) {
            System.out.println("  (nenhum) — no CACHE nao ha fala perdida; se a tela mostra "
                + "buraco, ele nasce DEPOIS do cache");
        }
        relatarOQueSePerdeu(acervo, filtro);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mostra o INGLÊS das falas que ficaram sem tradução.
     *
     * <p>Saber quantas se perderam localiza o arquivo; saber o QUE se perdeu diz se é fala de
     * diálogo, cartão de tela ou verso de música — e isso muda completamente o diagnóstico e a
     * urgência. Um buraco no meio de uma conversa é perda de conteúdo; um cartão de tela sem
     * tradução o espectador nem nota.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem filtro de obra a lista seria longa demais para ser
     * lida, então só imprime quando há filtro.
     */
    private static void relatarOQueSePerdeu(Acervo acervo, String filtro) {
        if (filtro == null) {
            System.out.printf("%n(passe -Dkronos.medicao.obra=<trecho> para ver o TEXTO perdido)%n");
            return;
        }
        System.out.printf("%n=== O QUE SE PERDEU (ingles das falas sem traducao) ===%n");
        acervo.falas().stream()
            .filter(f -> f.obra().toLowerCase().contains(filtro.toLowerCase()))
            .filter(f -> visivel(f.traduzido()).isEmpty() && !visivel(f.original()).isEmpty())
            .limit(30)
            .forEach(f -> System.out.printf("  %-34s #%-5d %s%n",
                recortar(f.arquivo().getFileName().toString()), f.entrada().indice(),
                recortar(visivel(f.original()))));
    }

    private static String recortar(String t) {
        return t.length() <= 58 ? t : t.substring(0, 58) + "…";
    }
}
