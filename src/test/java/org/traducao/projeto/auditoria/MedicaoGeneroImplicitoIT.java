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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fecha o buraco que a medição de gênero explícito declarou. O inglês
 * não marca gênero em adjetivo, então {@code "You're late"} dito a uma personagem só pode ser
 * traduzido certo por quem SABE de quem se fala — e o português obriga a escolher. Este é o
 * erro de gênero que realmente acontece, e nenhum instrumento anterior o via.
 *
 * <h2>De onde vem o gênero — consultado, nunca inventado</h2>
 * O elenco com gênero já existe na ficha do contexto de produção
 * ({@link ContextoGundamUnicorn#obterPromptSistema()}), em prosa:
 * {@code "Banagher Links (m); Mineva Lao Zabi, que se apresenta como Audrey Burne (f); ..."}.
 * Este teste PARSEIA aquele texto. Manter uma segunda lista de personagens aqui seria a
 * duplicação que a regra da medição proíbe — e ela divergiria no dia em que a lore mudasse.
 *
 * <h2>O critério, deliberadamente estreito</h2>
 * Só conta PARTICÍPIO ({@code -ado/-ada/-ido/-ida}), porque particípio concorda com o sujeito
 * sem ambiguidade, e só nos dois padrões em que o sujeito é inequívoco:
 * <ul>
 *   <li>{@code <Nome> está/é/parece/ficou <particípio>}</li>
 *   <li>{@code <Nome>, você está/é/parece <particípio>} — vocativo</li>
 * </ul>
 * Fica de fora, de propósito, o substantivo de gênero fixo: {@code "Audrey é o piloto"} não é
 * acusado, porque "piloto" tem gênero próprio e a frase pode estar correta. É PISO declarado.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem acervo, PULA por {@link Assumptions}. Se a ficha do contexto deixar de trazer marcação
 * {@code (m)}/{@code (f)}, o teste FALHA em vez de reportar zero — alvo vazio é "não verifiquei".
 */
@DisplayName("medição: gênero IMPLÍCITO — particípio que discorda do personagem")
class MedicaoGeneroImplicitoIT {

    private static final Path OBRA = Path.of("C:", "animes",
        "Mobile Suit Gundam Unicorn Re0096 (2016) [Season 1] [BD 1080p HEVC OPUS] [Dual-Audio]",
        "Gundam Unicorn Season 1");
    private static final Pattern TAGS = Pattern.compile("\\{[^}]*\\}");

    /** {@code Nome Sobrenome (m)} / {@code (f)} na prosa da ficha. */
    private static final Pattern FICHA = Pattern.compile(
        "([A-ZÁÉÍÓÚÂÊÔÃÕ][\\p{L}'-]+(?:\\s+[A-ZÁÉÍÓÚÂÊÔÃÕ][\\p{L}'-]+)*)\\s*\\(([mf])\\)");

    /**
     * O {@code é} vai ACENTUADO e sozinho. A primeira versão aceitava {@code [ée]}, e o {@code e}
     * conjunção casou em "o Capitão e Marida" — com "Marida" terminando em {@code -ida}, o
     * instrumento acusou um nome próprio de ser particípio feminino discordante.
     */
    private static final String VERBO = "(?:est[áa]|é|parece|ficou|foi|fica|estava|seria)";

    /**
     * O que pode existir entre o personagem e o verbo: nada, ou o vocativo {@code , você}. A
     * primeira versão aceitava até 40 caracteres quaisquer e acusou "a mente de Banagher
     * finalmente está tomada" — ali o particípio concorda com "a mente", não com Banagher.
     * Sujeito distante não é sujeito.
     */
    private static final String LIGACAO = "\\s*(?:,\\s*(?:você|voce))?\\s+";
    private static final Pattern PARTICIPIO_M = Pattern.compile("(?i)\\b\\p{L}{3,}(?:ado|ido)s?\\b");
    private static final Pattern PARTICIPIO_F = Pattern.compile("(?i)\\b\\p{L}{3,}(?:ada|ida)s?\\b");

    private record Caso(String ep, String personagem, char generoEsperado, String pt) {}

    @Test
    @DisplayName("a ficha do contexto de produção traz o elenco com gênero")
    void fichaTemGenero() {
        Map<String, Character> elenco = elencoDoContexto();
        assertFalse(elenco.isEmpty(),
            "instrumento cego: a ficha do ContextoGundamUnicorn nao trouxe nenhum (m)/(f)");
        assertEquals('f', elenco.get("Audrey Burne"), "Audrey Burne e (f) na ficha");
        assertEquals('m', elenco.get("Banagher Links"), "Banagher Links e (m) na ficha");
        System.out.println("\n=== elenco lido da ficha de produção: " + elenco.size() + " nomes ===");
        elenco.forEach((n, g) -> System.out.printf("   %-28s %s%n", n, g));
    }

    @Test
    @DisplayName("conta particípios que discordam do gênero do personagem citado")
    void generoImplicito() {
        Assumptions.assumeTrue(Files.isDirectory(OBRA), "acervo ausente — NÃO VERIFICADO");
        Map<String, Character> elenco = elencoDoContexto();
        Assumptions.assumeFalse(elenco.isEmpty(), "ficha sem gênero — NÃO VERIFICADO");

        // CONTROLE POSITIVO — o caso que o instrumento existe para achar.
        assertTrue(discorda("Audrey, você está atrasado.", 'f'), "instrumento cego: vocativo feminino");
        assertTrue(discorda("Audrey está preocupado.", 'f'), "instrumento cego: sujeito feminino");
        assertTrue(discorda("Banagher está preocupada.", 'm'), "instrumento cego: sujeito masculino");
        // CONTROLE NEGATIVO — concordancia CORRETA nao pode ser acusada.
        assertFalse(discorda("Audrey, você está atrasada.", 'f'), "alarme falso: concordancia correta");
        assertFalse(discorda("Banagher está preocupado.", 'm'), "alarme falso: concordancia correta");
        assertFalse(discorda("Audrey está feliz.", 'f'), "alarme falso: adjetivo invariavel");
        assertFalse(discorda("Audrey é o piloto.", 'f'),
            "alarme falso: substantivo de genero proprio fica FORA, e o piso declarado");

        LeitorLegendaAss leitor = new LeitorLegendaAss();
        var detector = new DetectorEfeitoKaraokeService();
        var politica = new PoliticaEstiloMusical(MedicaoUnicornMistralXAyaIT.estilosIgnoradosDoYml());

        for (String versao : new String[] {"traducao_mistral", "traducao_aya", "traducao_ptbr"}) {
            Path pasta = OBRA.resolve(versao);
            if (!Files.isDirectory(pasta)) {
                continue;
            }
            List<Caso> casos = new ArrayList<>();
            int comPersonagem = 0;
            List<Path> arquivos;
            try (var s = Files.list(pasta)) {
                arquivos = s.filter(p -> p.toString().endsWith(".ass"))
                    .filter(p -> !p.getFileName().toString().contains(".parcial.")).sorted().toList();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            for (Path arq : arquivos) {
                String ep = arq.getFileName().toString().replaceAll(".*(S\\d{2}E\\d{2}).*", "$1");
                for (EventoLegenda ev : leitor.ler(arq).eventos()) {
                    if (!ev.isDialogo() || ev.texto() == null) {
                        continue;
                    }
                    if (detector.podeSerCamadaMusical(ev.estilo(), ev.texto())
                        || detector.eEfeitoKaraoke(ev.texto())
                        || politica.estiloIgnorado(ev.estilo())) {
                        continue;
                    }
                    String v = visivel(ev.texto());
                    for (var e : elenco.entrySet()) {
                        String primeiro = e.getKey().split("\\s+")[0];
                        if (!v.matches("(?s).*\\b" + Pattern.quote(primeiro) + "\\b.*")) {
                            continue;
                        }
                        comPersonagem++;
                        if (discordaCom(v, primeiro, e.getValue())) {
                            casos.add(new Caso(ep, primeiro, e.getValue(), v));
                        }
                        break;
                    }
                }
            }
            System.out.printf("%n=== %s: %d de %d falas com personagem citado têm particípio discordante ===%n",
                versao, casos.size(), comPersonagem);
            casos.stream().limit(12).forEach(c -> System.out.printf("   %s  %s(%s): \"%s\"%n",
                c.ep(), c.personagem(), c.generoEsperado(), c.pt()));
        }
    }

    /** Lê o elenco da ficha do contexto de PRODUÇÃO. Nenhuma lista de personagens mora aqui. */
    private static Map<String, Character> elencoDoContexto() {
        Map<String, Character> elenco = new LinkedHashMap<>();
        Matcher m = FICHA.matcher(org.traducao.projeto.lore.LoreDeTeste.obra("gundam_unicorn").obterPromptSistema());
        while (m.find()) {
            elenco.put(m.group(1).trim(), m.group(2).charAt(0));
        }
        return elenco;
    }

    /** Atalho dos controles: o nome é a primeira palavra da frase de teste. */
    private static boolean discorda(String texto, char genero) {
        return discordaCom(texto, texto.split("[,\\s]")[0], genero);
    }

    /**
     * O particípio precisa estar LIGADO ao personagem por um verbo de ligação, direto
     * ({@code Audrey está atrasado}) ou pelo vocativo ({@code Audrey, você está atrasado}).
     * Sem essa amarra, qualquer particípio na mesma fala seria acusado — e a fala costuma
     * falar de mais de uma pessoa.
     */
    private static boolean discordaCom(String texto, String nome, char genero) {
        Pattern ligacao = Pattern.compile(
            "(?i)\\b" + Pattern.quote(nome) + "\\b" + LIGACAO + VERBO + "\\s+(\\p{L}+)");
        Matcher m = ligacao.matcher(texto);
        while (m.find()) {
            String palavra = m.group(1);
            if (genero == 'f' && PARTICIPIO_M.matcher(palavra).matches()) {
                return true;
            }
            if (genero == 'm' && PARTICIPIO_F.matcher(palavra).matches()) {
                return true;
            }
        }
        return false;
    }

    private static String visivel(String texto) {
        return TAGS.matcher(texto == null ? "" : texto).replaceAll("")
            .replace("\\N", " ").replace("\\n", " ").trim();
    }
}
