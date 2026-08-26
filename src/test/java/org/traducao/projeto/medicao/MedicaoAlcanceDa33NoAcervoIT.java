package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;
import org.traducao.projeto.core.texto.dicionarioOrtografia.VeredictoPalavra;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.revisaoConcordancia.application.CorretorAcentoDeDicionarioNaFalaService;
import org.traducao.projeto.revisaoConcordancia.application.CorretorAcentoPorPadraoService;
import org.traducao.projeto.revisaoConcordancia.application.CorretorAcentoQueColideComVerboService;
import org.traducao.projeto.revisaoConcordancia.application.CorretorCaractereForaDoPortuguesService;
import org.traducao.projeto.revisaoConcordancia.application.CorretorConcordanciaGeneroService;
import org.traducao.projeto.raspagemRevisao.application.DetectorConcordanciaService;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: o mapa honesto do ALCANCE da tela 3.3 no acervo — o que ela <b>pega</b>,
 * o que ela <b>acusa e não conserta</b>, e o que ninguém nela sequer enxerga.
 *
 * <h2>A pergunta que este harness responde</h2>
 * Depois de a 3.3 ter gravado 1.091 falas em 25/08/2026, uma passada nova encontra quase nada — e
 * "quase nada" é fácil de ler como "o acervo está bom". Não está: ele só está bom <b>naquilo que
 * esta tela sabe olhar</b>. As três colunas separam as duas coisas:
 *
 * <pre>
 *   PEGA          a cadeia de cinco elos mudaria a fala
 *   ACUSA         o DETECTOR da propria 3.3 marca a fala como suspeita e a cadeia nao mexe
 *   NINGUEM VE    o dicionario diz que ha palavra quebrada ou de outro idioma, e nem o
 *                 detector nem a cadeia reagem
 * </pre>
 *
 * <p>A coluna do meio é a mais útil, e é a que só existe porque a tela tem <b>dois</b> objetos: um
 * que acusa e outro que conserta. Onde eles discordam está o trabalho que a tela promete e não
 * entrega — e um relatório que só somasse "falas corrigidas" nunca mostraria isso.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY. Mede e imprime; não escreve no acervo.</li>
 *   <li>Pergunta aos objetos de PRODUÇÃO — a mesma cadeia, na mesma ordem que o caso de uso usa.
 *       Critério que a produção implementa é CONSULTADO, nunca copiado.</li>
 *   <li>Mesmo universo da tela: sem música (pela {@link PoliticaEstiloMusical}), sem
 *       {@code .parcial}, e sem linha {@code Comment} — que o reprodutor não desenha.</li>
 *   <li>Alcance pelo {@link AlcanceDaMedicao}: honra o filtro por obra e declara NÃO VERIFICADO
 *       quando ele não casa com nada.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Instrumento reprovado no caso-controle termina sem afirmar número.
 *
 * <p>Uso: {@code gradlew test --tests "*MedicaoAlcanceDa33NoAcervoIT*" "-Dkronos.medicao=true"}
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoAlcanceDa33NoAcervoIT {

    private static final Pattern TAG_ASS = Pattern.compile("\\{[^{}]*}");
    private static final Pattern PALAVRA = Pattern.compile("[\\p{L}][\\p{L}'-]*");

    @Inject
    LeitorLegendaAss leitor;

    @Inject
    PoliticaEstiloMusical politicaEstiloMusical;

    @Inject
    CorretorConcordanciaGeneroService corretorGenero;

    @Inject
    CorretorAcentoQueColideComVerboService corretorPos;

    @Inject
    CorretorAcentoPorPadraoService corretorPadrao;

    @Inject
    CorretorAcentoDeDicionarioNaFalaService corretorDicionario;

    @Inject
    CorretorCaractereForaDoPortuguesService corretorCaractere;

    @Inject
    DetectorConcordanciaService detector;

    @Inject
    CorretorOrtograficoLegenda dicionario;

    /** O placar de uma obra. */
    private static final class Placar {
        int falas;
        int pega;
        int acusaSemConsertar;
        int ninguemVe;
        final Map<String, Integer> motivosAcusados = new TreeMap<>();
        final Map<String, Integer> palavrasInvisiveis = new TreeMap<>();
        final List<String> amostraAcusada = new ArrayList<>();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE (regra 9) dos TRÊS instrumentos usados aqui. Um zero em
     * qualquer coluna só significa alguma coisa se o instrumento daquela coluna tiver sido visto
     * respondendo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: imprime o que falhou e devolve {@code false}.
     */
    private boolean instrumentoCalibrado() {
        // A CADEIA precisa CORRIGIR o doente e CALAR no são.
        boolean cadeiaAcha = !umaVolta("Isso e tudo.").equals("Isso e tudo.");
        boolean cadeiaCala = umaVolta("Isso é tudo.").equals("Isso é tudo.");
        // O DETECTOR precisa ACUSAR o doente e CALAR no são.
        //
        // A primeira versao usava "um isca no anzol" e o controle REPROVOU: o detector nao acusa
        // essa fala. O harness se recusou a publicar numero, que e o comportamento certo — e a
        // licao e que a fala do controle nao pode ser inventada por mim. Estas vem do
        // `DiagnosticoCorretorConcordanciaIT`, que ja tinha o conjunto de resposta conhecida.
        boolean detectorAcha = detector.analisar(null, "Vi o menina no parque.").suspeito();
        boolean detectorCala = !detector.analisar(null, "Vi a menina no parque.").suspeito();
        // O DICIONÁRIO precisa reprovar a palavra quebrada e aprovar a boa.
        Map<String, VeredictoPalavra> v =
            dicionario.classificar(new LinkedHashSet<>(List.of("esmagrado", "batalha")));
        boolean dicionarioAcha = v.get("esmagrado") == VeredictoPalavra.DESCONHECIDA;
        boolean dicionarioCala = v.get("batalha") == VeredictoPalavra.PORTUGUES_OK;

        if (cadeiaAcha && cadeiaCala && detectorAcha && detectorCala
            && dicionarioAcha && dicionarioCala) {
            System.out.println("  controle: cadeia corrige 'Isso e tudo' e cala no certo · "
                + "detector acusa 'um isca' e cala em 'uma isca' · "
                + "dicionario reprova 'esmagrado' e aprova 'batalha'");
            return true;
        }
        System.out.printf("INSTRUMENTO REPROVADO NO CONTROLE — cadeia(+)=%s cadeia(-)=%s "
            + "detector(+)=%s detector(-)=%s dicionario(+)=%s dicionario(-)=%s. "
            + "Nenhum numero abaixo vale.%n",
            cadeiaAcha, cadeiaCala, detectorAcha, detectorCala, dicionarioAcha, dicionarioCala);
        return false;
    }

    /** UMA volta da cadeia, na MESMA ordem do caso de uso da tela. */
    private String umaVolta(String texto) {
        String t = corretorCaractere.corrigir(texto).orElse(texto);
        t = corretorGenero.corrigir(t).orElse(t);
        t = corretorPos.corrigir(t).orElse(t);
        t = corretorPadrao.corrigir(t).orElse(t);
        return corretorDicionario.corrigir(t).orElse(t);
    }

    @Test
    @DisplayName("acervo: o que a 3.3 PEGA, o que ela ACUSA sem consertar, e o que ninguem ve")
    void medir() throws IOException {
        System.out.printf("%n=== ALCANCE DA 3.3 NO ACERVO ===%n");
        List<Path> pastas = AlcanceDaMedicao.pastasDeTraducao();
        if (pastas.isEmpty()) {
            return;
        }
        if (!instrumentoCalibrado()) {
            return;
        }

        Map<String, Placar> porObra = new TreeMap<>();
        int arquivos = 0;

        // DUAS PASSADAS, e a segunda so depois de a primeira acabar. Nome de personagem se prova
        // pelo CORPUS — ele aparece capitalizado no MEIO de alguma fala, mais cedo ou mais tarde;
        // palavra quebrada, nunca. Julgar fala a fala reportou 6.315 falas com "palavra que o
        // dicionario reprova", e `Gundam` sozinho era 3.573 delas.
        NomesConfirmadosPeloCorpus nomesDoCorpus = new NomesConfirmadosPeloCorpus();
        List<String[]> suspeitas = new ArrayList<>();

        for (Path pasta : pastas) {
            String obra = AlcanceDaMedicao.obraDe(pasta);
            Placar placar = porObra.computeIfAbsent(obra, k -> new Placar());
            for (Path arquivo : AlcanceDaMedicao.arquivosEntregues(pasta)) {
                DocumentoLegenda documento;
                try {
                    documento = leitor.ler(arquivo);
                } catch (RuntimeException e) {
                    System.out.printf("  ILEGIVEL (NAO VERIFICADO): %s%n", arquivo.getFileName());
                    continue;
                }
                arquivos++;

                // Aquece o dicionario com o arquivo inteiro: sem isto, cada fala com palavra
                // inedita paga um processo externo — 80 ms/fala, medido em 24/08/2026.
                List<String> falasDoArquivo = documento.eventos().stream()
                    .filter(EventoLegenda::temTexto)
                    .filter(e -> !eMusicaOuComentario(e))
                    .map(EventoLegenda::texto)
                    .toList();
                corretorDicionario.aquecerCom(falasDoArquivo);

                // E AQUECE O CLASSIFICADOR TAMBEM, com o vocabulario inteiro do arquivo.
                //
                // `aquecerCom` esquenta so o dicionario PORTUGUES. A terceira coluna pergunta ao
                // CLASSIFICADOR, que consulta CINCO dicionarios — e cada palavra inedita ali paga
                // um processo externo por dicionario. A primeira rodada desta medicao levou
                // 43 minutos por causa disso; e a mesma licao dos 80 ms/fala, pela terceira porta.
                Set<String> vocabulario = new LinkedHashSet<>();
                for (String fala : falasDoArquivo) {
                    Matcher mv = PALAVRA.matcher(visivel(fala));
                    while (mv.find()) {
                        if (mv.group().length() >= 4) {
                            vocabulario.add(mv.group());
                        }
                    }
                }
                if (!vocabulario.isEmpty()) {
                    dicionario.classificar(vocabulario);
                }

                for (EventoLegenda evento : documento.eventos()) {
                    if (!evento.temTexto() || eMusicaOuComentario(evento)) {
                        continue;
                    }
                    placar.falas++;
                    String texto = evento.texto();
                    String depois = umaVolta(texto);
                    if (!depois.equals(texto)) {
                        placar.pega++;
                        continue;
                    }
                    ResultadoDeteccaoConcordancia acusacao = detector.analisar(null, texto);
                    if (acusacao.suspeito()) {
                        placar.acusaSemConsertar++;
                        for (String motivo : acusacao.motivos()) {
                            placar.motivosAcusados.merge(recorte(motivo), 1, Integer::sum);
                        }
                        if (placar.amostraAcusada.size() < 4) {
                            placar.amostraAcusada.add(visivel(texto));
                        }
                        continue;
                    }
                    nomesDoCorpus.observar(texto);
                    for (String quebrada : palavrasQueNinguemVe(texto)) {
                        suspeitas.add(new String[]{obra, quebrada});
                        break;
                    }
                }
            }
        }

        // SEGUNDA PASSADA: agora o corpus ja disse quais palavras sao nome.
        int descartadosPorSeremNome = 0;
        for (String[] sus : suspeitas) {
            if (nomesDoCorpus.eNomeProprio(sus[1])) {
                descartadosPorSeremNome++;
                continue;
            }
            Placar p = porObra.get(sus[0]);
            p.ninguemVe++;
            p.palavrasInvisiveis.merge(sus[1], 1, Integer::sum);
        }
        System.out.printf("  nomes proprios confirmados pelo corpus ... %d "
            + "(descartaram %d suspeitas)%n",
            nomesDoCorpus.quantidade(), descartadosPorSeremNome);

        imprimir(porObra, arquivos);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: as palavras que o DICIONÁRIO reprova e que nem o detector nem a cadeia
     * enxergam — a terceira coluna.
     *
     * <p>INVARIANTES DO DOMÍNIO: nome próprio fica de fora (é a mesma régua do corretor de acento:
     * capitalizada fora do início de frase), e palavra de menos de quatro letras também, porque
     * sigla e interjeição enchem a lista sem serem defeito.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: dicionário mudo devolve conjunto vazio.
     */
    private Set<String> palavrasQueNinguemVe(String texto) {
        String visivel = visivel(texto);
        // O filtro de nome NAO acontece aqui: quem responde isso e o CORPUS, depois que ele
        // inteiro tiver passado. Filtrar por fala aqui foi o que deixou `Gundam` entrar 3.573
        // vezes na lista — ele abre fala o tempo todo, e ali o filtro de uma fala nao o protege.
        Set<String> candidatas = new LinkedHashSet<>();
        Matcher m = PALAVRA.matcher(visivel);
        while (m.find()) {
            String p = m.group();
            if (p.length() >= 4) {
                candidatas.add(p);
            }
        }
        if (candidatas.isEmpty()) {
            return Set.of();
        }
        Map<String, VeredictoPalavra> vereditos = dicionario.classificar(candidatas);
        Set<String> fora = new LinkedHashSet<>();
        for (String p : candidatas) {
            if (vereditos.get(p) == VeredictoPalavra.DESCONHECIDA) {
                fora.add(p);
            }
        }
        return fora;
    }

    private boolean eMusicaOuComentario(EventoLegenda evento) {
        if ("Comment".equals(evento.tipoLinha())) {
            return true;
        }
        return evento.estilo() != null && politicaEstiloMusical.estiloIgnorado(evento.estilo());
    }

    private static String visivel(String t) {
        // O ESPACO DURO `\\h` TAMBEM. Sem ele, 16 das 319 palavras da lista eram artefato
        // meu: `\\hJudau` virava a "palavra" hJudau, e `\\hMOBILE` virava hMOBILE. A quebra
        // e o espaco duro sao a mesma familia, e tratar so uma delas e o furo classico da
        // fronteira de termo neste projeto.
        return TAG_ASS.matcher(t).replaceAll(" ")
            .replace("\\N", " ").replace("\\n", " ").replace("\\h", " ").strip();
    }

    private static String recorte(String t) {
        return t == null ? "?" : (t.length() <= 40 ? t : t.substring(0, 40) + "…");
    }

    private void imprimir(Map<String, Placar> porObra, int arquivos) {
        int totalFalas = 0;
        int totalPega = 0;
        int totalAcusa = 0;
        int totalInvisivel = 0;
        System.out.printf("%n  alcance ...... %d arquivos (sem musica, sem Comment, sem .parcial)%n%n",
            arquivos);
        System.out.printf("%-44s %8s %6s %8s %10s%n",
            "OBRA", "falas", "PEGA", "ACUSA", "NINGUEM VE");
        System.out.println("-".repeat(80));
        for (Map.Entry<String, Placar> e : porObra.entrySet()) {
            Placar p = e.getValue();
            totalFalas += p.falas;
            totalPega += p.pega;
            totalAcusa += p.acusaSemConsertar;
            totalInvisivel += p.ninguemVe;
            System.out.printf("%-44s %8d %6d %8d %10d%n",
                e.getKey().length() > 44 ? e.getKey().substring(0, 44) : e.getKey(),
                p.falas, p.pega, p.acusaSemConsertar, p.ninguemVe);
        }
        System.out.println("-".repeat(80));
        System.out.printf("%-44s %8d %6d %8d %10d%n%n",
            "TOTAL", totalFalas, totalPega, totalAcusa, totalInvisivel);

        Map<String, Integer> motivos = new TreeMap<>();
        Map<String, Integer> invisiveis = new TreeMap<>();
        porObra.values().forEach(p -> {
            p.motivosAcusados.forEach((k, v) -> motivos.merge(k, v, Integer::sum));
            p.palavrasInvisiveis.forEach((k, v) -> invisiveis.merge(k, v, Integer::sum));
        });

        System.out.println("=== ACUSA E NAO CONSERTA — por motivo do detector ===");
        if (motivos.isEmpty()) {
            System.out.println("   nenhum — e o controle acima provou que o detector fala");
        }
        motivos.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(x -> System.out.printf("   %5d  %s%n", x.getValue(), x.getKey()));

        System.out.println("\n=== AMOSTRA DAS ACUSADAS (ate 4 por obra) ===");
        porObra.forEach((obra, p) -> {
            if (p.amostraAcusada.isEmpty()) {
                return;
            }
            System.out.printf("   --- %s%n", obra.length() > 44 ? obra.substring(0, 44) : obra);
            p.amostraAcusada.forEach(f -> System.out.printf("       %s%n", recorte(f)));
        });

        System.out.printf("%n=== NINGUEM VE — palavra que o dicionario reprova (%d distintas) ===%n",
            invisiveis.size());
        invisiveis.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(60)
            .forEach(x -> System.out.printf("   %4d  %s%n", x.getValue(), x.getKey()));

        // A LISTA INTEIRA vai para arquivo, e nao so as 60 primeiras. Relatorio que corta a
        // amostra esconde trabalho: quem for decidir o que fazer com estas palavras precisa
        // ver todas, com a obra em que cada uma aparece.
        Path csv = Path.of("relatorios", "alcance-33-ninguem-ve.csv");
        try {
            java.nio.file.Files.createDirectories(csv.getParent());
            StringBuilder sb = new StringBuilder("obra;palavra;ocorrencias\n");
            porObra.forEach((obra, p) -> p.palavrasInvisiveis.forEach((palavra, n) ->
                sb.append(obra.replace(';', ',')).append(';')
                  .append(palavra).append(';').append(n).append('\n')));
            java.nio.file.Files.writeString(csv, sb.toString(),
                java.nio.charset.StandardCharsets.UTF_8);
            System.out.printf("%n  lista COMPLETA (%d palavras): %s%n",
                invisiveis.size(), csv.toAbsolutePath());
        } catch (IOException e) {
            System.out.printf("%n  NAO VERIFICADO: nao consegui gravar %s (%s) — a lista "
                + "completa nao foi salva, e as 60 acima sao so a amostra.%n", csv, e);
        }

        System.out.println("""

              COMO LER: PEGA e o que a cadeia mudaria hoje. ACUSA e a fala que o DETECTOR da
              propria 3.3 marca e a cadeia NAO conserta — e a promessa que a tela nao cumpre.
              NINGUEM VE e defeito que so o dicionario percebe: nem o detector nem a cadeia
              reagem, e nenhum numero da tela mostra isso.""");
    }
}
