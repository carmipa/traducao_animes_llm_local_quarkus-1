package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: pedir a um SEGUNDO dicionário se as palavras que declarei defeituosas são
 * mesmo defeito — antes que uma delas mande retraduzir uma fala correta.
 *
 * <h2>O prejuízo, medido em 26/08/2026</h2>
 * A lista foi montada perguntando ao hunspell "quem você não conhece?" e classificando o resultado
 * à mão. {@code irracionais} entrou como <b>palavra quebrada</b>. É português impecável — o plural
 * de {@code irracional}; o hunspell é que não tem essa flexão. No ensaio, essa entrada mandou o LLM
 * reescrever uma fala <b>correta</b>, e a nova versão trocou {@code irracionais} por
 * {@code ilógicos}, mudando o sentido do original em inglês ({@code illogical} está na mesma frase
 * como palavra separada).
 *
 * <h2>Por que LanguageTool, e não um segundo julgamento meu</h2>
 * Regra 20: quem revisa precisa de LENTE DIFERENTE. Reler a lista com o mesmo critério que a
 * produziu acha os mesmos erros — nenhum. O LanguageTool traz outro dicionário, montado por outra
 * gente, e discorda do hunspell em exatamente as flexões que faltam a ele.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Este harness NÃO altera a lista. Ele imprime o que precisa sair, e a remoção é uma
 *       decisão registrada — lista alterada por máquina sem ninguém ler é como o erro entrou.</li>
 *   <li>CASO-CONTROLE obrigatório: o LanguageTool tem de ACUSAR {@code braos} e ficar CALADO em
 *       {@code casa}. Sem isso, "ninguém foi reprovado" é cegueira, não aprovação.</li>
 * </ul>
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: LanguageTool fora do ar termina NÃO VERIFICADO, sem veredicto
 * sobre palavra nenhuma.
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class SegundaOpiniaoSobreListaIT {

    private static final String LISTA = "medicao/palavras-defeituosas.txt";

    /**
     * O id da regra vem da PRODUÇÃO ({@code LanguageToolRevisorAdapter.REGRA_ORTOGRAFICA}), e não
     * de uma string repetida aqui: se ele mudar de lado, os dois lados mudam juntos. É a mesma
     * regra que a produção DESLIGA — e é justamente ela que faz falta como segunda opinião.
     */
    private static final String REGRA_ORTOGRAFICA =
        org.traducao.projeto.core.texto.gramatica.LanguageToolRevisorAdapter.REGRA_ORTOGRAFICA;

    /**
     * O motor é montado AQUI, e não injetado, porque a produção desliga
     * {@code MORFOLOGIK_RULE_PT_BR} de propósito — a regra ortográfica dava 92 alarmes falsos e
     * tocava a lore em 83 falas. Desligada, ela não pode servir de segunda opinião: o primeiro
     * controle deste harness pegou exatamente isso, acusando {@code braos}={@code false}.
     *
     * <p>Aqui a ortografia fica LIGADA, e a lore não corre risco: nada é escrito em legenda
     * nenhuma. O que se mede é a LISTA, não o acervo.
     */
    private org.languagetool.JLanguageTool motor;

    /**
     * PROPÓSITO DE NEGÓCIO: a palavra vai ao revisor dentro de uma frase, porque corretor
     * gramatical calado diante de uma palavra solta não prova nada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro do motor devolve {@code false}, e o caso-controle
     * transforma esse {@code false} em instrumento reprovado antes de qualquer veredicto.
     */
    private boolean acusada(String palavra) {
        String frase = "Eu vi " + palavra + " ontem de manha.";
        try {
            for (org.languagetool.rules.RuleMatch m : motor.check(frase)) {
                // SO a regra ORTOGRAFICA conta. A primeira versao contava qualquer achado, e a
                // frase-veiculo faz o LanguageTool reclamar de GRAMATICA em palavra bem escrita:
                // "Eu vi odiam ontem" tem um verbo onde cabia um substantivo. Assim `odiam`,
                // `atacem` e `odiem` — portugues impecavel — sairam como "o LT tambem recusa", e
                // eu quase os mantive na lista de defeito por causa disso.
                if (!REGRA_ORTOGRAFICA.equals(m.getRule().getId())) {
                    continue;
                }
                String trecho = frase.substring(m.getFromPos(), m.getToPos());
                if (trecho.equals(palavra)) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    @Test
    @DisplayName("LanguageTool confere, palavra a palavra, a lista que autoriza retraducao")
    void conferir() throws Exception {
        System.out.println("\n=== SEGUNDA OPINIAO SOBRE A LISTA DECLARADA ===");
        try {
            motor = new org.languagetool.JLanguageTool(
                new org.languagetool.language.BrazilianPortuguese());
        } catch (RuntimeException e) {
            System.out.printf("NAO VERIFICADO: LanguageTool nao subiu (%s)%n", e);
            return;
        }
        System.out.printf("  motor proprio, ortografia LIGADA (%d regras)%n",
            motor.getAllActiveRules().size());

        // CASO-CONTROLE antes de qualquer veredicto.
        boolean acusaDoente = acusada("braos");
        boolean calaNoSao = !acusada("casa");
        System.out.printf("  controle: acusa 'braos'=%s · cala em 'casa'=%s%n",
            acusaDoente, calaNoSao);
        if (!(acusaDoente && calaNoSao)) {
            System.out.println("INSTRUMENTO REPROVADO NO CONTROLE — nenhum veredicto e emitido.");
            return;
        }

        Set<String> palavras = new LinkedHashSet<>();
        try (InputStream entrada = getClass().getClassLoader().getResourceAsStream(LISTA)) {
            if (entrada == null) {
                System.out.printf("NAO VERIFICADO: lista '%s' ausente do classpath%n", LISTA);
                return;
            }
            try (BufferedReader leitura =
                     new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
                String linha;
                while ((linha = leitura.readLine()) != null) {
                    String limpa = linha.trim();
                    if (!limpa.isEmpty() && !limpa.startsWith("#")) {
                        palavras.add(limpa);
                    }
                }
            }
        }

        List<String> discordancias = new ArrayList<>();
        for (String p : palavras) {
            if (!acusada(p)) {
                discordancias.add(p);
            }
        }
        System.out.printf("%n  palavras conferidas: %d%n", palavras.size());
        System.out.printf("  o LanguageTool ACEITA (o hunspell recusava): %d%n%n",
            discordancias.size());
        for (String p : discordancias) {
            System.out.printf("   ACEITA  %s%n", p);
        }
        if (discordancias.isEmpty()) {
            System.out.println("   (nenhuma — os dois dicionarios concordam palavra a palavra)");
        }

        Path saida = Path.of("relatorios", "lista-segunda-opiniao.txt");
        Files.createDirectories(saida.getParent());
        Files.writeString(saida, String.join("\n", discordancias) + "\n", StandardCharsets.UTF_8);
        System.out.printf("%n  gravado: %s%n", saida.toAbsolutePath());
        System.out.println("  Este harness NAO edita a lista. A remocao e decisao registrada.");
    }
}
