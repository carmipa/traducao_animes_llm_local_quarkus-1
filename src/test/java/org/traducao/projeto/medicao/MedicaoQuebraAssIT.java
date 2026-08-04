package org.traducao.projeto.medicao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.medicao.LeitorAcervoCache.Acervo;
import org.traducao.projeto.medicao.LeitorAcervoCache.FalaDoAcervo;
import org.traducao.projeto.qualidadeTraducao.application.NormalizadorAcentosComuns;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: medir, no acervo REAL, o alcance da quebra {@code \N} do ASS — quantas
 * falas a têm, quantos nomes compostos ela parte, e quantas correções de acento ainda estão
 * presas nela.
 *
 * <h2>Por que é medição e NÃO catraca</h2>
 * O acervo muda a cada tradução; congelar estes números faria a suíte reprovar por trabalho novo,
 * e o time aprenderia a ignorar. Aqui não há {@code assert}: o valor é o número REPRODUZÍVEL, com
 * o código de leitura revisado, no lugar do script de rascunho que já reportou 0 onde havia 435.
 * Quem quiser travar comportamento usa {@code CatracaFronteiraQuebraAssTest} e
 * {@code QuebraAssPropriedadeTest}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>NÃO roda na suíte normal: exige {@code -Dkronos.medicao=true}. Lê {@code cache/}, que é
 *       gitignorado e não existe igual em duas máquinas.</li>
 *   <li>READ-ONLY. Nenhuma medição escreve no acervo.</li>
 *   <li>A pendência de acento é apurada CHAMANDO {@link NormalizadorAcentosComuns}, não com uma
 *       cópia do dicionário — cópia de regra em dois lugares foi a causa de quatro defeitos
 *       medidos entre 03 e 04/08/2026.</li>
 *   <li>Acervo ausente TERMINA com aviso visível, nunca com silêncio: "0 falas" e "0 arquivos"
 *       têm a mesma aparência.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem {@code cache/}, imprime o caminho procurado e retorna. Arquivo ilegível entra na contagem
 * de ilegíveis, impressa junto.
 *
 * <p>Uso: {@code gradlew test --tests "*MedicaoQuebraAssIT*" "-Dkronos.medicao=true"}
 *
 * <p>As ASPAS em torno do {@code -D} não são enfeite no PowerShell: sem elas o argumento é partido
 * no ponto e o Gradle responde {@code Task '.medicao=true' not found}.
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoQuebraAssIT {

    private static final String QUEBRA = "\\" + "N";

    /** Palavra capitalizada, quebra, palavra capitalizada — a forma de nome composto partido. */
    private static final Pattern NOME_PARTIDO = Pattern.compile(
        "(\\p{Lu}\\p{L}+)" + Pattern.quote(QUEBRA) + "(\\p{Lu}\\p{L}+)");

    @Test
    @DisplayName("acervo: alcance da quebra \\N e o que ela ainda esconde")
    void medir() throws IOException {
        Path raiz = LeitorAcervoCache.raizPadrao();
        Acervo acervo = LeitorAcervoCache.ler(raiz);
        if (acervo.vazio()) {
            System.out.println("SEM ACERVO em " + raiz.toAbsolutePath()
                + " — nada medido. Aponte outro com -D" + LeitorAcervoCache.CHAVE_RAIZ + "=...");
            return;
        }

        System.out.printf("acervo: %d arquivos lidos, %d ilegiveis, %d falas%n",
            acervo.arquivosLidos(), acervo.arquivosIlegiveis(), acervo.falas().size());

        medirCobertura(acervo);
        medirNomesPartidos(acervo);
        medirAcentosPresosNaQuebra(acervo);
    }

    /** Quantas falas trazem a quebra — o denominador de tudo que vem depois. */
    private static void medirCobertura(Acervo acervo) {
        long comTraducao = acervo.falas().stream().filter(f -> !f.traduzido().isEmpty()).count();
        long comQuebra = acervo.falas().stream()
            .filter(f -> f.traduzido().contains(QUEBRA)).count();
        System.out.printf("%n[1] COBERTURA%n    falas traduzidas... %d%n"
                + "    com a quebra...... %d (%.1f%%)%n",
            comTraducao, comQuebra, 100.0 * comQuebra / Math.max(comTraducao, 1));
    }

    /**
     * Nomes compostos partidos pela quebra: a forma que fez o reparo de entidade corromper
     * tradução correta até {@code e02c1b86}.
     */
    private static void medirNomesPartidos(Acervo acervo) {
        Map<String, String> formas = new LinkedHashMap<>();
        int campos = 0;
        for (FalaDoAcervo fala : acervo.falas()) {
            for (String texto : new String[] {fala.original(), fala.traduzido()}) {
                Matcher m = NOME_PARTIDO.matcher(texto);
                if (m.find()) {
                    campos++;
                    formas.putIfAbsent(m.group(1) + QUEBRA + m.group(2), recortar(texto));
                }
            }
        }
        System.out.printf("%n[2] NOME COMPOSTO PARTIDO PELA QUEBRA%n"
            + "    campos atingidos... %d%n    formas distintas... %d%n", campos, formas.size());
        formas.entrySet().stream().limit(8)
            .forEach(e -> System.out.printf("      %-30s %s%n", e.getKey(), e.getValue()));
    }

    /**
     * Correções de acento ainda pendentes, separadas por estarem ou não coladas à quebra.
     *
     * <p>A pendência é apurada chamando o normalizador de produção: se ele ainda MUDA a fala que
     * já está gravada, aquela correção escapou na hora de traduzir. Foi assim que se mediu, em
     * 2026-08-04, que 0 formas sobreviveram soltas e 11 sobreviveram coladas na quebra — cache
     * como experimento natural, porque o normalizador roda antes de gravar.
     */
    private static void medirAcentosPresosNaQuebra(Acervo acervo) {
        var normalizador = new NormalizadorAcentosComuns();
        int coladas = 0;
        int soltas = 0;
        StringBuilder exemplos = new StringBuilder();
        for (FalaDoAcervo fala : acervo.falas()) {
            String texto = fala.traduzido();
            String normalizado = normalizador.normalizar(texto);
            if (texto.isEmpty() || texto.equals(normalizado)) {
                continue;
            }
            if (coladaNaQuebra(texto, normalizado)) {
                coladas++;
                if (coladas <= 6) {
                    exemplos.append("      ").append(recortar(texto)).append(System.lineSeparator());
                }
            } else {
                soltas++;
            }
        }
        System.out.printf("%n[3] ACENTO AINDA PENDENTE NO CACHE (normalizador de producao)%n"
            + "    solto............. %d%n    colado na quebra.. %d%n", soltas, coladas);
        System.out.print(exemplos);
    }

    /**
     * A primeira divergência começa numa palavra imediatamente após a quebra?
     *
     * <p>O {@code N} da quebra É LETRA para {@link Character#isLetter}, então o laço que recua até
     * o começo da palavra o engole e para na contrabarra — apontando para DENTRO da quebra. A
     * primeira versão deste método caiu exatamente no defeito que este harness mede: reportou 0
     * coladas onde há 11. Por isso o retorno confere a contrabarra na posição anterior e o
     * {@code N} na posição do "começo", em vez de assumir que o recuo parou na palavra.
     */
    private static boolean coladaNaQuebra(String texto, String normalizado) {
        int i = 0;
        int limite = Math.min(texto.length(), normalizado.length());
        while (i < limite && texto.charAt(i) == normalizado.charAt(i)) {
            i++;
        }
        int inicio = i;
        while (inicio > 0 && Character.isLetter(texto.charAt(inicio - 1))) {
            inicio--;
        }
        return inicio > 0 && texto.charAt(inicio - 1) == '\\' && texto.charAt(inicio) == 'N';
    }

    private static String recortar(String texto) {
        return texto.length() <= 92 ? texto : texto.substring(0, 92) + "…";
    }
}
