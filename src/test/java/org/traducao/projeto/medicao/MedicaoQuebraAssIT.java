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

    /**
     * Mesma forma, mas separada por {@code \h} — o ESPAÇO DURO do ASS, que o fansub usa
     * justamente para impedir que o nome quebre na virada da linha.
     *
     * <p>Medir isto separado não é preciosismo: {@code FronteiraTermoAss.SEPARADOR_INTERNO}
     * aceita espaço e {@code \N}, e NÃO aceita {@code \h}. Se o acervo usa {@code \h} dentro de
     * nome composto, a mecânica consertada em 04/08 ainda tem um terceiro furo — e ele seria
     * encontrado do mesmo jeito que os outros dois: por acaso, meses depois.
     */
    private static final String ESPACO_DURO = "\\" + "h";
    private static final Pattern NOME_COM_ESPACO_DURO = Pattern.compile(
        "(\\p{Lu}\\p{L}+)" + Pattern.quote(ESPACO_DURO) + "(\\p{Lu}\\p{L}+)");

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE (regra 9) — o instrumento tem de ser visto ACHANDO o
     * caso plantado e CALANDO no caso são, antes de qualquer número valer.
     *
     * <h2>Por que esta medição precisa disto</h2>
     * O número que ela produz decide se a mecânica da fronteira de termo ainda tem furo. Um zero
     * aqui só significa "o acervo está limpo" se a regex tiver sido vista casando — e regex de
     * quebra do ASS é justamente onde este projeto mais errou: {@code \N} ocupa DOIS caracteres,
     * e o {@code N} é letra para {@code \p{L}}, então um padrão descuidado casa coisa errada ou
     * não casa nada, sempre em silêncio.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: imprime o que falhou e devolve {@code false}; quem chama
     * termina sem afirmar número.
     */
    /**
     * O caso-controle como TESTE, e não só como guarda dentro da medição.
     *
     * <p>Guarda que imprime e volta protege o NÚMERO daquela execução; ela não protege o
     * instrumento de ser quebrado por quem editar a regex amanhã. Só uma asserção reprova o
     * build — e só o que reprova o build sobrevive à troca de quem edita.
     */
    @Test
    @DisplayName("calibracao: as regex acham o nome partido e calam no nome inteiro")
    void calibracaoDasRegex() {
        org.junit.jupiter.api.Assertions.assertTrue(instrumentoCalibrado(),
            "o instrumento nao passou no proprio controle — nenhum numero desta medicao vale");
    }

    private static boolean instrumentoCalibrado() {
        boolean achaPartido = NOME_PARTIDO.matcher("Cardeas" + QUEBRA + "Vist").find();
        boolean achaEspacoDuro =
            NOME_COM_ESPACO_DURO.matcher("Cardeas" + ESPACO_DURO + "Vist").find();
        // NEGATIVOS: nome com espaco NORMAL nao e nome partido, e minuscula nao e nome.
        boolean calaComEspaco = !NOME_PARTIDO.matcher("Cardeas Vist").find()
            && !NOME_COM_ESPACO_DURO.matcher("Cardeas Vist").find();
        boolean calaComMinuscula = !NOME_PARTIDO.matcher("cardeas" + QUEBRA + "vist").find();

        if (achaPartido && achaEspacoDuro && calaComEspaco && calaComMinuscula) {
            System.out.println("  controle: acha 'Cardeas\\NVist' e 'Cardeas\\hVist'; "
                + "cala em 'Cardeas Vist' e em minuscula");
            return true;
        }
        System.out.printf("INSTRUMENTO REPROVADO NO CONTROLE — partido=%s espacoDuro=%s "
            + "calaComEspaco=%s calaComMinuscula=%s. Nenhum numero abaixo vale.%n",
            achaPartido, achaEspacoDuro, calaComEspaco, calaComMinuscula);
        return false;
    }

    @Test
    @DisplayName("acervo: alcance da quebra \\N e o que ela ainda esconde")
    void medir() throws IOException {
        if (!instrumentoCalibrado()) {
            return;
        }
        Path raiz = LeitorAcervoCache.raizPadrao();
        Acervo acervo = LeitorAcervoCache.ler(raiz);
        if (acervo.vazio()) {
            System.out.println("NAO VERIFICADO: acervo ausente em " + raiz.toAbsolutePath()
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

        Map<String, String> comEspacoDuro = new LinkedHashMap<>();
        int camposDuro = 0;
        for (FalaDoAcervo fala : acervo.falas()) {
            for (String texto : new String[] {fala.original(), fala.traduzido()}) {
                Matcher m = NOME_COM_ESPACO_DURO.matcher(texto);
                if (m.find()) {
                    camposDuro++;
                    comEspacoDuro.putIfAbsent(
                        m.group(1) + ESPACO_DURO + m.group(2), recortar(texto));
                }
            }
        }
        System.out.printf("%n[2b] NOME COMPOSTO SEPARADO PELO ESPACO DURO \\h%n"
                + "     (SEPARADOR_INTERNO aceita espaco e \\N; NAO aceita \\h)%n"
                + "     campos atingidos... %d%n     formas distintas... %d%n",
            camposDuro, comEspacoDuro.size());
        comEspacoDuro.entrySet().stream().limit(8)
            .forEach(e -> System.out.printf("       %-30s %s%n", e.getKey(), e.getValue()));
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
