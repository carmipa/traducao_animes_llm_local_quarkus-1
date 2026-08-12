package org.traducao.projeto.auditoria;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.qualidadeTraducao.application.LoreAtivaFake;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: compara, NO ARTEFATO EM DISCO, o que o mistral-nemo e a aya-expanse-8b
 * entregaram para o MESMO episódio do Gundam Unicorn — porque telemetria mede esforço do
 * pipeline, não qualidade do texto. No Guilty Crown as duas medições divergiram: a telemetria
 * dava vantagem larga à aya e os artefatos finais empataram.
 *
 * <h2>Por que existe como teste, e não como script</h2>
 * O critério "esta fala tem resíduo em inglês?" já é implementado pela produção em
 * {@link ValidadorTraducaoService}. Reimplementá-lo num script de varredura é o erro que a
 * regra da medição proíbe — a segunda implementação sempre diverge da primeira. Aqui o
 * critério é CONSULTADO: o teste instancia a classe real e pergunta a ela.
 *
 * <h2>As duas medições, e por que duas</h2>
 * <ul>
 *   <li><b>ECO</b> — a fala PT saiu IDÊNTICA à EN. É o sinal mais objetivo que existe: não
 *       depende de heurística de idioma, só de igualdade de string. Pareado por índice de
 *       evento, então compara a MESMA fala nos dois lados.</li>
 *   <li><b>RESÍDUO</b> — o validador de produção reprova a fala. Cobre o caso em que o modelo
 *       traduziu parcialmente, que o eco não pega.</li>
 * </ul>
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só episódios presentes nos TRÊS lados (entrada, mistral, aya) entram na conta. O E01
 *       está fora porque a rodada da aya foi interrompida nele — comparar 22 contra 21 faria a
 *       diferença do episódio ausente parecer diferença de modelo.</li>
 *   <li>Nada é escrito. O teste lê o acervo e conclui.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem o acervo em disco, o teste é PULADO por {@link Assumptions} — nunca passa em silêncio
 * fingindo ter medido. Alvo vazio é "não verifiquei", não "está tudo certo".
 */
@DisplayName("medição: mistral × aya no artefato final do Gundam Unicorn")
class MedicaoUnicornMistralXAyaIT {

    private static final Path OBRA = Path.of("C:", "animes",
        "Mobile Suit Gundam Unicorn Re0096 (2016) [Season 1] [BD 1080p HEVC OPUS] [Dual-Audio]",
        "Gundam Unicorn Season 1");
    /**
     * A entrada ORIGINAL, de onde saíram mistral e aya. Ela não está mais em
     * {@code legendas_extraidas_ass}: em 12/08 11:33 a troca de fonte e o achatamento
     * reescreveram aquela pasta in-place. O backup que o próprio KRONOS gravou ANTES de tocar
     * no arquivo é o único lugar onde o original ainda existe — e é por isso que "sem backup,
     * não grava" é invariante e não recomendação.
     */
    private static final Path ENTRADA_ORIGINAL = Path.of("backups", "troca_tipo_legenda_20260812_113344");
    /** A entrada depois de fonte trocada e estilos achatados — origem da terceira rodada. */
    private static final Path ENTRADA_ACHATADA = OBRA.resolve("legendas_extraidas_ass");
    private static final Path MISTRAL = OBRA.resolve("traducao_mistral");
    private static final Path AYA = OBRA.resolve("traducao_aya");
    private static final Path AYA_ACHATADA = OBRA.resolve("traducao_ptbr");

    /**
     * A lista nominal do {@code application.yml}, LIDA do arquivo — não digitada aqui. É o
     * segundo veto do pipeline, e ignorá-lo custou uma medição errada: {@code OPL2} (155 falas
     * só no E02, 3.410 nos 22 episódios) é o único estilo do acervo que escapa do veto por
     * forma e só é barrado por nome. Sem consultar esta lista, o instrumento contava a letra
     * inteira do OP como "fala não traduzida" — nos dois modelos, inflando os dois lados.
     */
    private static final org.traducao.projeto.legenda.domain.PoliticaEstiloMusical POLITICA_DO_YML =
        new org.traducao.projeto.legenda.domain.PoliticaEstiloMusical(lerEstilosIgnoradosDoYml());

    /** Exposta para {@link OndeOAchatamentoValeriaIT} — a lista tem um dono só. */
    static List<String> estilosIgnoradosDoYml() {
        return lerEstilosIgnoradosDoYml();
    }

    private static List<String> lerEstilosIgnoradosDoYml() {
        Path yml = Path.of("src", "main", "resources", "application.yml");
        List<String> estilos = new ArrayList<>();
        try {
            boolean dentro = false;
            for (String linha : Files.readAllLines(yml)) {
                if (linha.contains("estilos-ignorados:")) {
                    dentro = true;
                    continue;
                }
                if (!dentro) {
                    continue;
                }
                String t = linha.trim();
                if (t.startsWith("#") || t.isEmpty()) {
                    continue;
                }
                if (!t.startsWith("- ")) {
                    break;
                }
                estilos.add(t.substring(2).trim().replaceAll("^\"|\"$", ""));
            }
        } catch (Exception e) {
            throw new IllegalStateException("nao consegui ler estilos-ignorados de " + yml, e);
        }
        if (estilos.isEmpty()) {
            throw new IllegalStateException("instrumento cego: lista estilos-ignorados vazia");
        }
        return estilos;
    }

    private record Placar(int falas, int eco, int residuo) {
        Placar mais(Placar o) {
            return new Placar(falas + o.falas, eco + o.eco, residuo + o.residuo);
        }
    }

    @Test
    @DisplayName("conta eco e resíduo em inglês nos dois lados, pareado por episódio")
    void compararArtefatos() {
        Assumptions.assumeTrue(Files.isDirectory(ENTRADA_ORIGINAL) && Files.isDirectory(MISTRAL)
            && Files.isDirectory(AYA), "acervo do Unicorn ausente nesta máquina — NÃO VERIFICADO");

        LeitorLegendaAss leitor = new LeitorLegendaAss();
        ValidadorTraducaoService validador = new ValidadorTraducaoService(LoreAtivaFake.vazia());

        // CONTROLE POSITIVO: um resíduo=0 só vale depois de o instrumento provar que sabe dar
        // não-zero NO MESMO experimento. Sem isto, validador mudo e legenda limpa produzem
        // exatamente o mesmo número.
        boolean reprovaIngles = false;
        try {
            validador.validarFala("I will protect you, no matter what happens.");
        } catch (RuntimeException e) {
            reprovaIngles = true;
        }
        assertTrue(reprovaIngles,
            "instrumento cego: o validador nao reprovou uma fala inteiramente em ingles, "
                + "entao um residuo=0 sobre o acervo nao prova nada");

        Map<String, Path> eng = porEpisodio(ENTRADA_ORIGINAL);
        Map<String, Path> engAch = porEpisodio(ENTRADA_ACHATADA);
        Map<String, Path> mis = porEpisodio(MISTRAL);
        Map<String, Path> aya = porEpisodio(AYA);
        Map<String, Path> ayaAch = porEpisodio(AYA_ACHATADA);

        List<String> pareados = eng.keySet().stream()
            .filter(mis::containsKey).filter(aya::containsKey).sorted().toList();
        assertTrue(!pareados.isEmpty(), "instrumento cego: nenhum episódio pareado nos três lados");

        Placar totalM = new Placar(0, 0, 0);
        Placar totalA = new Placar(0, 0, 0);
        Placar totalAch = new Placar(0, 0, 0);
        List<String> linhas = new ArrayList<>();

        // CALIBRAGEM OBRIGATÓRIA: um "eco" de 190 falas por episódio seria absurdo, e a primeira
        // execução deu exatamente isso. Antes de qualquer total, o instrumento tem de MOSTRAR o
        // que está contando — se o que ele chama de eco for música preservada de propósito ou
        // desalinhamento de índice, o número não vale nada.
        String primeiro = pareados.getFirst();
        List<EventoLegenda> amostraEn = eventos(leitor, eng.get(primeiro));
        List<EventoLegenda> amostraPt = eventos(leitor, aya.get(primeiro));
        System.out.println("\n=== CALIBRAGEM em " + primeiro + " (en=" + amostraEn.size()
            + " eventos, pt=" + amostraPt.size() + ") ===");
        int mostrados = 0;
        for (int i = 0; i < Math.min(amostraEn.size(), amostraPt.size()) && mostrados < 8; i++) {
            String en = amostraEn.get(i).texto();
            String pt = amostraPt.get(i).texto();
            if (en != null && pt != null && en.trim().equals(pt.trim())) {
                System.out.printf("  IGUAIS  [%s] %s%n", amostraPt.get(i).estilo(),
                    pt.length() > 90 ? pt.substring(0, 90) + "..." : pt);
                mostrados++;
            }
        }

        // Cada saída é medida contra A SUA PRÓPRIA entrada. A terceira rodada nasceu do arquivo
        // já achatado, que tem 2.898 eventos a menos: pareá-la com o original por índice
        // compararia a fala i com a fala j e produziria um número inventado.
        int comAchatado = 0;
        for (String ep : pareados) {
            List<EventoLegenda> o = eventos(leitor, eng.get(ep));
            Placar m = medir(o, eventos(leitor, mis.get(ep)), validador);
            Placar a = medir(o, eventos(leitor, aya.get(ep)), validador);
            totalM = totalM.mais(m);
            totalA = totalA.mais(a);
            String extra = "";
            if (engAch.containsKey(ep) && ayaAch.containsKey(ep)) {
                Placar ac = medir(eventos(leitor, engAch.get(ep)),
                    eventos(leitor, ayaAch.get(ep)), validador);
                totalAch = totalAch.mais(ac);
                comAchatado++;
                extra = String.format("  |  achatado eco=%d res=%d", ac.eco(), ac.residuo());
            }
            if (m.eco() + m.residuo() + a.eco() + a.residuo() > 0) {
                linhas.add(String.format("  %s  mistral eco=%d res=%d  |  aya eco=%d res=%d%s",
                    ep, m.eco(), m.residuo(), a.eco(), a.residuo(), extra));
            }
        }

        // O QUE SAO as falas em eco? 181 de 5.455 e 3,3%, e o numero sozinho nao diz se e
        // defeito ou se nao havia o que traduzir. Interjeicao e nome proprio saem iguais e estao
        // CORRETOS; frase inteira saindo igual e o modelo devolvendo a entrada.
        System.out.println("\n=== AMOSTRA DAS FALAS EM ECO (aya) — defeito ou nada a traduzir? ===");
        int amostra = 0;
        for (String ep : pareados) {
            if (amostra >= 25) {
                break;
            }
            List<EventoLegenda> o = eventos(leitor, eng.get(ep));
            List<EventoLegenda> t = eventos(leitor, aya.get(ep));
            var det = new org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService();
            for (int i = 0; i < Math.min(o.size(), t.size()) && amostra < 25; i++) {
                String en = o.get(i).texto();
                String pt = t.get(i).texto();
                if (en == null || pt == null || en.isBlank()) {
                    continue;
                }
                if (det.podeSerCamadaMusical(o.get(i).estilo(), en) || det.eEfeitoKaraoke(en)
                    || POLITICA_DO_YML.estiloIgnorado(o.get(i).estilo())) {
                    continue;
                }
                if (en.trim().equals(pt.trim())) {
                    String visivel = pt.replaceAll("\\{[^}]*\\}", "").trim();
                    System.out.printf("  %s [%s] %d chars: \"%s\"%n", ep, o.get(i).estilo(),
                        visivel.length(), visivel.length() > 70 ? visivel.substring(0, 70) + "..." : visivel);
                    amostra++;
                }
            }
        }

        System.out.println("\n=== UNICORN: artefato final, " + pareados.size() + " episódios pareados ===");
        linhas.forEach(System.out::println);
        System.out.printf("  %-14s falas=%d  eco=%d  residuo=%d%n", "MISTRAL",
            totalM.falas(), totalM.eco(), totalM.residuo());
        System.out.printf("  %-14s falas=%d  eco=%d  residuo=%d%n", "AYA",
            totalA.falas(), totalA.eco(), totalA.residuo());
        System.out.printf("  %-14s falas=%d  eco=%d  residuo=%d   (%d episódios)%n", "AYA+ACHATADO",
            totalAch.falas(), totalAch.eco(), totalAch.residuo(), comAchatado);
    }

    /** Chave = trecho SxxEyy do nome, que é o que os três lados têm em comum. */
    private static Map<String, Path> porEpisodio(Path pasta) {
        Map<String, Path> m = new LinkedHashMap<>();
        try (var s = Files.list(pasta)) {
            s.filter(p -> p.toString().endsWith(".ass"))
             .filter(p -> !p.getFileName().toString().contains(".parcial."))
             .forEach(p -> {
                 var mm = java.util.regex.Pattern.compile("(S\\d{2}E\\d{2})")
                     .matcher(p.getFileName().toString());
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
        DocumentoLegenda d = leitor.ler(arquivo);
        return d.eventos().stream().filter(EventoLegenda::isDialogo).toList();
    }

    /**
     * Pareia por ÍNDICE do evento: os dois arquivos vêm do mesmo original, então a i-ésima fala
     * é a mesma fala. Quando as contagens divergem, compara só o prefixo comum e o restante fica
     * de fora da conta — inflar o denominador com falas que um dos lados não tem produziria
     * exatamente o tipo de número errado que esta medição existe para evitar.
     */
    private static Placar medir(List<EventoLegenda> original, List<EventoLegenda> traduzido,
            ValidadorTraducaoService validador) {
        var detector = new org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService();
        var politica = POLITICA_DO_YML;
        int n = Math.min(original.size(), traduzido.size());
        int falas = 0;
        int eco = 0;
        int residuo = 0;
        for (int i = 0; i < n; i++) {
            String en = original.get(i).texto();
            String pt = traduzido.get(i).texto();
            if (en == null || pt == null || en.isBlank()) {
                continue;
            }
            // Música e karaokê saem IDÊNTICOS de propósito — o pipeline não os traduz, por regra
            // de escopo. Contá-los como "eco" é o erro que a primeira execução cometeu: 190 falas
            // por episódio, quase todas romaji do ED. O veto vem do detector de produção, não de
            // uma lista minha de estilos.
            if (detector.podeSerCamadaMusical(original.get(i).estilo(), en)
                || detector.eEfeitoKaraoke(en)
                || politica.estiloIgnorado(original.get(i).estilo())) {
                continue;
            }
            falas++;
            if (en.trim().equals(pt.trim())) {
                eco++;
                continue;
            }
            try {
                validador.validarFala(pt);
            } catch (RuntimeException e) {
                residuo++;
            }
        }
        return new Placar(falas, eco, residuo);
    }
}
