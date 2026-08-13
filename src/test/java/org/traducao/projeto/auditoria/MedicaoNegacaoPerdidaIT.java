package org.traducao.projeto.auditoria;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: mede a segunda forma de erro FLUENTE — a negação que some na tradução.
 * {@code "I can't do it"} virando {@code "Eu posso fazer isso"} é português impecável, passa em
 * eco, resíduo, pendência e no detector de pergunta, e <b>inverte a cena</b>.
 *
 * <h2>Por que é irmã da medição de pergunta</h2>
 * Mesma natureza: uma propriedade ESTRUTURAL do original que o português preserva sempre.
 * Toda negação inglesa tem negação portuguesa correspondente; quando ela desaparece, ou o
 * sentido inverteu (grave), ou a frase foi reescrita numa forma afirmativa equivalente
 * (aceitável, e é o que a leitura humana separa). O instrumento reduz 5.455 falas a uma lista
 * curta; ele nunca afirma "defeito".
 *
 * <h2>O cuidado que este critério exige</h2>
 * Português nega de mais formas que o inglês: {@code nunca}, {@code nada}, {@code ninguém},
 * {@code nenhum}, {@code jamais}, {@code sem}, {@code nem}. Uma lista pobre do lado PT
 * produziria alarme falso em massa — "I never lied" → "Eu jamais menti" é tradução CORRETA e
 * seria acusada se a lista só tivesse "não". Por isso o controle negativo abaixo cobre
 * justamente essas formas, e não só o caminho feliz.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Música, karaokê e estilos da lista nominal saem pelos vetos de PRODUÇÃO.</li>
 *   <li>Fala idêntica ao original não conta: já é eco, e contar o mesmo defeito em duas
 *       métricas infla o diagnóstico.</li>
 *   <li>Cada versão é medida contra A SUA entrada — a versão achatada nasceu de um arquivo com
 *       2.898 eventos a menos, e parear por índice com o original inventaria números.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem acervo, PULA por {@link Assumptions}: "não verifiquei" nunca sai com a cara de "não há
 * nada".
 */
@DisplayName("medição: negação do original que sumiu na tradução")
class MedicaoNegacaoPerdidaIT {

    private static final Path OBRA = Path.of("C:", "animes",
        "Mobile Suit Gundam Unicorn Re0096 (2016) [Season 1] [BD 1080p HEVC OPUS] [Dual-Audio]",
        "Gundam Unicorn Season 1");
    private static final Path ENTRADA_ORIGINAL = Path.of("backups", "troca_tipo_legenda_20260812_113344");
    private static final Pattern TAGS = Pattern.compile("\\{[^}]*\\}");

    /**
     * Negação em inglês. {@code n't} cobre as contrações todas de uma vez; as demais são as
     * formas plenas. {@code cannot} entra separado porque não tem espaço.
     */
    private static final Pattern NEGA_EN = Pattern.compile(
        "(?i)\\bn[o']t\\b|n't\\b|\\bnot\\b|\\bcannot\\b|\\bnever\\b|\\bno one\\b|\\bnobody\\b"
            + "|\\bnothing\\b|\\bnone\\b|\\bneither\\b|\\bnor\\b|\\bwithout\\b");

    /**
     * Negação em português — DELIBERADAMENTE larga. Um instrumento que só conhecesse "não"
     * acusaria "Eu jamais menti" como negação perdida, e alarme falso ensina a desligar o
     * alarme.
     */
    private static final Pattern NEGA_PT = Pattern.compile(
        "(?i)\\bn[ãa]o\\b|\\bnunca\\b|\\bjamais\\b|\\bnada\\b|\\bningu[ée]m\\b|\\bnenhum[ao]?s?\\b"
            + "|\\bnem\\b|\\bsem\\b|\\btampouco\\b|\\bdeix(?:e|ou|ar)\\s+de\\b|\\bimposs[íi]vel\\b"
            // Formas que a primeira versão não conhecia e acusou como defeito na varredura de
            // 12/08: "Not at all." → "De modo algum." é tradução CORRETA e virou falso positivo.
            + "|\\bde\\s+modo\\s+algum\\b|\\bde\\s+jeito\\s+nenhum\\b|\\bem\\s+v[ãa]o\\b"
            + "|\\blonge\\s+disso\\b|\\bnegativ[ao]\\b"
            // Terceira leva, da mesma varredura: "never being able to be rid of it" → "incapaz
            // de se livrar dele" e "Not unless he becomes a vessel" → "A menos que ele se torne"
            // são traduções CORRETAS que a lista anterior acusava.
            + "|\\bincapa(?:z|zes)\\b|\\ba\\s+menos\\s+que\\b|\\bsalvo\\s+se\\b|\\bexceto\\b"
            + "|\\bdesconsider|\\bignor(?:e|a|ar)\\b");

    /**
     * Construções inglesas cuja tradução natural em português é AFIRMATIVA. Elas carregam
     * palavra de negação sem negar: {@code "nothing but trouble"} é "só problemas", e
     * {@code "for nothing"} é "em vão". Acusá-las enche a lista de tradução correta — e guarda
     * que reprova o certo ensina a desligar o alarme.
     *
     * <p>A tag question final entra aqui pelo mesmo motivo: em {@code "You can hear me, can't
     * you?"} a negação é retórica, e o português a resolve com entonação ou com nada.
     */
    private static final Pattern IDIOMATISMO_EN = Pattern.compile(
        "(?i)\\bnothing\\s+but\\b|\\bfor\\s+nothing\\b|\\bstop\\s+at\\s+nothing\\b"
            + "|\\bnone\\s+other\\b|\\bnot\\s+unlike\\b"
            // Tag question: ", <auxiliar>n't <pronome>". Duas correções que o controle negativo
            // impôs, uma de cada vez:
            //   1. exigir "?" logo depois do pronome perdia "can't you, Banagher?" — há vocativo
            //      no meio. O que define a tag question é o PRONOME, não a pontuação.
            //   2. "ca"/"wo" em vez de "can"/"won": a contração COME o n da raiz. Com "can" a
            //      alternância consumia o n de "can't" e sobrava "'t" para um "n[o']?t" que
            //      exigia outro n — então "doesn't" casava e "can't" não, silenciosamente.
            + "|,\\s*(?:ca|wo|do|does|did|is|are|would|should|could|have|has)n[o']?t\\s+"
            + "(?:you|he|she|it|we|they|i)\\b");

    private record Caso(String ep, String en, String pt) {}

    @Test
    @DisplayName("conta e lista, nas três versões, as negações que sumiram")
    void negacoesQueSumiram() {
        Assumptions.assumeTrue(Files.isDirectory(ENTRADA_ORIGINAL) && Files.isDirectory(OBRA),
            "acervo do Unicorn ausente — NÃO VERIFICADO");

        // CONTROLE POSITIVO — o caso doente, antes de qualquer contagem valer.
        assertTrue(perdeuNegacao("I can't do it.", "Eu posso fazer isso."),
            "instrumento cego: nao viu a negacao sumir");
        assertTrue(perdeuNegacao("Nobody is coming.", "Alguém está vindo."),
            "instrumento cego: nao viu 'nobody' sumir");
        assertTrue(perdeuNegacao("We cannot lose her.", "Podemos perdê-la."),
            "instrumento cego: nao viu 'cannot' sumir");

        // CONTROLE NEGATIVO — traducoes CORRETAS que uma lista pobre acusaria.
        assertFalse(perdeuNegacao("I never lied to you.", "Eu jamais menti para você."),
            "alarme falso: 'jamais' nega tao bem quanto 'nao'");
        assertFalse(perdeuNegacao("There's nothing left.", "Não restou nada."),
            "alarme falso: negacao preservada");
        assertFalse(perdeuNegacao("He left without saying goodbye.", "Ele saiu sem se despedir."),
            "alarme falso: 'sem' e a traducao de 'without'");
        assertFalse(perdeuNegacao("Nobody saw it.", "Ninguém viu."),
            "alarme falso: negacao preservada");
        assertFalse(perdeuNegacao("I will protect you.", "Eu vou proteger você."),
            "alarme falso: fala sem negacao nao entra nesta medicao");
        // Os falsos positivos REAIS colhidos na varredura de 12/08, cada um vindo de tradução
        // correta. Ficam como controle negativo permanente: se voltarem a ser acusados, o
        // refinamento foi desfeito.
        assertFalse(perdeuNegacao("You brought us nothing but trouble.", "Você só nos trouxe problemas."),
            "alarme falso: 'nothing but' e afirmativo em portugues");
        assertFalse(perdeuNegacao("this truly will have been for nothing.", "tudo isso terá sido em vão."),
            "alarme falso: 'for nothing' -> 'em vao' e a traducao idiomatica");
        assertFalse(perdeuNegacao("Not at all. As someone born in space,", "De modo algum. Como alguém nascido no espaço,"),
            "alarme falso: 'de modo algum' E negacao");
        assertFalse(perdeuNegacao("You can hear me, can't you, Banagher?", "Você pode me ouvir, Banagher?"),
            "alarme falso: tag question nao e negacao semantica");

        LeitorLegendaAss leitor = new LeitorLegendaAss();
        var detector = new DetectorEfeitoKaraokeService();
        var politica = new PoliticaEstiloMusical(MedicaoUnicornMistralXAyaIT.estilosIgnoradosDoYml());

        Map<String, Path> engOriginal = porEpisodio(ENTRADA_ORIGINAL);
        Map<String, Path> engAchatada = porEpisodio(OBRA.resolve("legendas_extraidas_ass"));

        for (String versao : new String[] {"traducao_mistral", "traducao_aya", "traducao_ptbr"}) {
            Path pasta = OBRA.resolve(versao);
            if (!Files.isDirectory(pasta)) {
                continue;
            }
            Map<String, Path> eng = "traducao_ptbr".equals(versao) ? engAchatada : engOriginal;
            Map<String, Path> saida = porEpisodio(pasta);
            List<Caso> casos = new ArrayList<>();
            int comNegacao = 0;

            for (String ep : eng.keySet().stream().filter(saida::containsKey).sorted().toList()) {
                List<EventoLegenda> o = eventos(leitor, eng.get(ep));
                List<EventoLegenda> t = eventos(leitor, saida.get(ep));
                for (int i = 0; i < Math.min(o.size(), t.size()); i++) {
                    String en = o.get(i).texto();
                    String pt = t.get(i).texto();
                    if (en == null || pt == null) {
                        continue;
                    }
                    if (detector.podeSerCamadaMusical(o.get(i).estilo(), en)
                        || detector.eEfeitoKaraoke(en)
                        || politica.estiloIgnorado(o.get(i).estilo())) {
                        continue;
                    }
                    if (NEGA_EN.matcher(visivel(en)).find()) {
                        comNegacao++;
                    }
                    if (perdeuNegacao(en, pt)) {
                        casos.add(new Caso(ep, visivel(en), visivel(pt)));
                    }
                }
            }
            System.out.printf("%n=== %s: %d de %d falas com negação a perderam (%.1f%%) ===%n",
                versao, casos.size(), comNegacao,
                comNegacao == 0 ? 0.0 : 100.0 * casos.size() / comNegacao);
            casos.stream().limit(12).forEach(c ->
                System.out.printf("   %s  EN: \"%s\"%n            PT: \"%s\"%n", c.ep(), c.en(), c.pt()));
        }
    }

    // package-private: MedicaoZetaMistralXAyaIT aplica o MESMO critério a outra obra.
    static boolean perdeuNegacao(String en, String pt) {
        String a = visivel(en);
        String b = visivel(pt);
        if (a.isEmpty() || b.isEmpty() || a.equals(b)) {
            return false;
        }
        if (IDIOMATISMO_EN.matcher(a).find()) {
            return false;
        }
        return NEGA_EN.matcher(a).find() && !NEGA_PT.matcher(b).find();
    }

    private static String visivel(String texto) {
        return TAGS.matcher(texto == null ? "" : texto).replaceAll("")
            .replace("\\N", " ").replace("\\n", " ").trim();
    }

    private static Map<String, Path> porEpisodio(Path pasta) {
        // Pasta ausente devolve VAZIO em vez de lancar: o acervo e material de trabalho e muda de
        // lugar. Harness de medicao que REPROVA por acervo ausente confunde 'nao verifiquei' com
        // 'esta errado' — e foi o que aconteceu em 13/08, quando as pastas do Unicorn sairam do
        // lugar e dois ITs derrubaram a suite inteira.
        if (pasta == null || !Files.isDirectory(pasta)) {
            return new LinkedHashMap<>();
        }
        Map<String, Path> m = new LinkedHashMap<>();
        try (var s = Files.list(pasta)) {
            s.filter(p -> p.toString().endsWith(".ass"))
             .filter(p -> !p.getFileName().toString().contains(".parcial."))
             .forEach(p -> {
                 var mm = Pattern.compile("(S\\d{2}E\\d{2})").matcher(p.getFileName().toString());
                 if (mm.find()) {
                     m.put(mm.group(1), p);
                 }
             });
        } catch (Exception e) {
            throw new IllegalStateException("falha ao listar " + pasta, e);
        }
        return m;
    }

    private static List<EventoLegenda> eventos(LeitorLegendaAss leitor, Path arquivo) {
        return leitor.ler(arquivo).eventos().stream().filter(EventoLegenda::isDialogo).toList();
    }
}

