package org.traducao.projeto.novoKaraoke.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.novoKaraoke.domain.MedicaoEstiloKaraoke;
import org.traducao.projeto.novoKaraoke.domain.ports.TelemetriaKaraokePort;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: mede a decisão "par bilíngue?" do achatador (Passo 2 do Karaokê) pelo
 * caminho de PRODUÇÃO ({@code converterArquivo}) e com o dicionário Hunspell REAL, sobre os pares
 * reais do cache do karaokê. É o instrumento que decide se uma mudança na heurística melhora ou
 * piora — o dublê dos testes de unidade não mostra que o pt_BR aceita "Can't", "escape" e "fate".
 *
 * <h2>Universo e casos (A4)</h2>
 * Cada letra DISTINTA de um verso só do cache, com a sua tradução: {@code EN\NPT} TEM de virar duas
 * linhas (positivo — errar cola inglês e português numa linha, o dano real); {@code EN\NEN} e
 * {@code PT\NPT} de letras vizinhas NÃO podem (negativos — errar deixa um verso monolíngue em duas
 * linhas, dano cosmético).
 *
 * <h2>Linha de base medida em 24/09/2026 (217 pares, dicionário disponível)</h2>
 * <pre>
 *   regra só por contagem:        positivos errados 8  | EN\NEN errados 22 | PT\NPT errados 26
 *   + assimetria de palavra EN:   positivos errados 2  | EN\NEN errados 22 | PT\NPT errados 29
 * </pre>
 * Critério declarado antes do ajuste: reduzir os positivos errados sem subir os cosméticos mais de
 * 5 pontos percentuais. Os 2 restantes não têm palavra só-inglesa em cima ("Dying to see you...")
 * ou são lixo de versão antiga ("hol").
 *
 * <h2>Invariantes do domínio</h2>
 * READ-ONLY sobre o cache; escreve só num diretório temporário e em {@code build/}. Travada por
 * {@code -Dkronos.medicao=true}: lê dado de execução, ausente no CI.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Três estados (regra 23): cache ausente, menos de 50 pares ou dicionário inglês fora do ar ABORTAM
 * como NÃO VERIFICADO (não é aprovação nem reprovação); nenhuma linha da saída casar com o esperado
 * é instrumento cego e reprova — "0 erros" sobre nada não vale.
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoParBilingueAchatadorIT {

    private static final String N = "\\N";

    @Test
    void mede() throws Exception {
        ObjectMapper om = new ObjectMapper();
        LinkedHashMap<String, String> pares = new LinkedHashMap<>();
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isDirectory(Path.of("cache/karaoke")),
            "NAO VERIFICADO: cache/karaoke ausente — sem pares reais nao ha o que medir");
        try (Stream<Path> s = Files.list(Path.of("cache/karaoke"))) {
            for (Path p : s.filter(x -> x.toString().endsWith(".cache.json")).toList()) {
                JsonNode raiz = om.readTree(Files.readString(p, StandardCharsets.UTF_8));
                for (JsonNode e : raiz.path("entradas")) {
                    String o = vis(e.path("original").asText());
                    String t = vis(e.path("traduzido").asText());
                    if (o.isBlank() || t.isBlank() || o.contains(N) || t.contains(N) || o.equalsIgnoreCase(t)) continue;
                    pares.putIfAbsent(o, t);
                }
            }
        }
        List<String[]> lista = new ArrayList<>();
        pares.forEach((o, t) -> lista.add(new String[] {o, t}));
        StringBuilder ass = new StringBuilder("[Script Info]\nPlayResY: 1080\n\n[V4+ Styles]\n"
            + "Format: Name,Fontname,Fontsize,PrimaryColour,SecondaryColour,OutlineColour,BackColour,Bold,Italic,Underline,StrikeOut,ScaleX,ScaleY,Spacing,Angle,BorderStyle,Outline,Shadow,Alignment,MarginL,MarginR,MarginV,Encoding\n"
            + "Style: Default,Arial,48,&H00FFFFFF,&H000000FF,&H00000000,&H96000000,0,0,0,0,100,100,0,0,1,2,1,2,30,30,30,1\n\n"
            + "[Events]\nFormat: Layer,Start,End,Style,Name,MarginL,MarginR,MarginV,Effect,Text\n");
        Map<String, String> esperado = new LinkedHashMap<>();
        long t = 0;
        for (int i = 0; i < lista.size(); i++) {
            String[] p = lista.get(i);
            String pos = p[0] + N + p[1];
            esperado.put(tempo(t), "POS\t" + pos);
            ass.append(evento(t, pos));
            t += 1000;
            if (i + 1 < lista.size()) {
                String[] q = lista.get(i + 1);
                String enen = p[0] + N + q[0];
                esperado.put(tempo(t), "NEG-ENEN\t" + enen);
                ass.append(evento(t, enen));
                t += 1000;
                String ptpt = p[1] + N + q[1];
                esperado.put(tempo(t), "NEG-PTPT\t" + ptpt);
                ass.append(evento(t, ptpt));
                t += 1000;
            }
        }
        Path dir = Files.createTempDirectory("medpar");
        Path origem = dir.resolve("sintetico.ass");
        Files.writeString(origem, ass.toString(), StandardCharsets.UTF_8);
        Path destino = Files.createDirectories(dir.resolve("out"));

        ConversorKaraokeUseCase conv = new ConversorKaraokeUseCase();
        conv.detectorKaraoke = new DetectorEfeitoKaraokeService();
        conv.corretorOrtografico = new CorretorOrtograficoLegenda();
        conv.logStream = new LogStreamService() { @Override public void publicarLog(String c, String m) { } };
        conv.telemetriaKaraoke = new TelemetriaKaraokePort() {
            @Override public void publicarArquivo(String a, int b, int c, int d, int e, List<MedicaoEstiloKaraoke> f) { }
            @Override public void publicarOperacao(String a, Path b, Path c, long d, int e) { }
        };
        org.junit.jupiter.api.Assumptions.assumeTrue(lista.size() > 50,
            "NAO VERIFICADO: so " + lista.size() + " pares distintos no cache — amostra pequena demais");
        conv.converterArquivo(origem, destino, true);
        // Sem o dicionario ingles o achatador cai no fallback do diacritico: o numero sairia, mas
        // seria o do FALLBACK, nao o da regra que esta medicao existe para julgar (A5/A8).
        org.junit.jupiter.api.Assumptions.assumeTrue(conv.corretorOrtografico.inglesDisponivel(),
            "NAO VERIFICADO: dicionario ingles indisponivel — mediria o fallback, nao a regra");

        Map<String, Integer> contagem = new TreeMap<>();
        List<String> erros = new ArrayList<>();
        for (String l : Files.readAllLines(destino.resolve("sintetico.ass"), StandardCharsets.UTF_8)) {
            if (!l.startsWith("Dialogue:")) continue;
            String[] c = l.substring(9).strip().split(",", 10);
            String exp = esperado.get(c[1]);
            if (exp == null) continue;
            String tipo = exp.split("\t")[0];
            boolean duas = c[9].contains(N);
            boolean certo = tipo.equals("POS") == duas;
            contagem.merge(tipo + (certo ? " certo" : " ERRADO"), 1, Integer::sum);
            if (!certo && erros.size() < 60) erros.add(tipo + " | " + c[9]);
        }
        String rel = "ingles=" + conv.corretorOrtografico.inglesDisponivel() + "\npares distintos=" + lista.size()
            + "\n" + contagem + "\n" + String.join("\n", erros) + "\n";
        Files.writeString(Path.of("build/medicao-par-bilingue-achatador.txt"), rel);
        int medidos = contagem.values().stream().mapToInt(Integer::intValue).sum();
        assertTrue(medidos > 0, "instrumento cego: nenhuma linha da saida casou com o esperado");
    }

    private static String vis(String s) {
        return s.replaceAll("\\{[^}]*\\}", "").strip();
    }

    private static String evento(long cs, String texto) {
        return "Dialogue: 0," + tempo(cs) + "," + tempo(cs + 400) + ",ED,,0,0,0,fx,{\\fad(200,200)}" + texto + "\n";
    }

    private static String tempo(long cs) {
        return String.format(Locale.ROOT, "%d:%02d:%02d.%02d", cs / 360000, (cs / 6000) % 60, (cs / 100) % 60, cs % 100);
    }
}
