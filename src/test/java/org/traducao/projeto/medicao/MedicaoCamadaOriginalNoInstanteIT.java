package org.traducao.projeto.medicao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.traducaoKaraoke.application.ClassificadorLetraKaraokeService;
import org.traducao.projeto.traducaoKaraoke.application.PlanoDeClassificacao;
import org.traducao.projeto.traducaoKaraoke.domain.ClasseLinhaKaraoke;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fotografa, pelo plano de PRODUÇÃO, a decisão "já existe a letra original
 * na tela neste instante?" para cada linha que a 4.1 traduz — a decisão que manda EMPILHAR
 * {@code inglês\Nportuguês} (regra F8) ou trocar no lugar. Duas fotos, antes e depois de mexer no
 * critério, comparadas por identidade do evento (A4), dizem exatamente quais linhas do acervo
 * mudam de decisão e permitem conferir uma a uma.
 *
 * <h2>O prejuízo que originou</h2>
 * Guilty Crown, OP_S2, 09/10/2026: a linha "that your eyes were given to you to acknowledge
 * others," foi fatiada pelo fansub em CINCO eventos curtos (o efeito de pulsar) dentro de UM evento
 * de romaji de 5,4 s. O pareamento por pontas (início e fim a menos de 0,5 s) não casa nenhuma
 * fatia, e as cinco empilhariam o inglês sobre o português com o romaji já na tela — três
 * camadas, o defeito do F6 de 24/09 em outra forma.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só leitura: nada no acervo é escrito; a foto vai para {@code build/medicao/}.</li>
 *   <li>Critério CONSULTADO, nunca reimplementado: a coluna de decisão é
 *       {@link PlanoDeClassificacao#temOriginalPreservadaNoInstante} montado como o caso de uso
 *       monta, com o corretor ortográfico real (ele decide as linhas F8).</li>
 *   <li>Chave estável do evento: pasta + arquivo + posição no documento + instante.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo ausente ou zero linha traduzível FALHA — foto vazia por instrumento cego não é "nada
 * muda" (regra 8). O controle positivo exige as duas decisões presentes na mesma foto.
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoCamadaOriginalNoInstanteIT {

    private static final Pattern REMOVE_TAGS = Pattern.compile("\\{[^}]*\\}");

    @Test
    @DisplayName("fotografa a decisao de empilhar de cada linha traduzivel do acervo")
    void fotografaADecisaoDeEmpilhar() throws IOException {
        List<Path> pastas = AlcanceDaMedicao.pastasDeTraducao();
        assertFalse(pastas.isEmpty(), "nenhuma pasta traducao_ptbr no alcance: passe -Dkronos.acervo");

        ClassificadorLetraKaraokeService classificador =
            new ClassificadorLetraKaraokeService(new DetectorEfeitoKaraokeService());
        CorretorOrtograficoLegenda corretor = new CorretorOrtograficoLegenda();
        LeitorLegendaAss leitor = new LeitorLegendaAss();

        List<String> linhas = new ArrayList<>();
        linhas.add("pasta\tarquivo\tposicao\tinicio\tfim\testilo\ttem_original\ttexto");
        int comOriginal = 0;
        int semOriginal = 0;
        for (Path pasta : pastas) {
            List<Path> arquivos;
            try (Stream<Path> s = Files.list(pasta)) {
                arquivos = s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ass"))
                    .sorted()
                    .toList();
            }
            for (Path arquivo : arquivos) {
                DocumentoLegenda doc = leitor.ler(arquivo);
                PlanoDeClassificacao plano = PlanoDeClassificacao.montar(doc, classificador, corretor);
                List<EventoLegenda> eventos = doc.eventos();
                for (int i = 0; i < eventos.size(); i++) {
                    if (plano.classeNaPosicao(i) != ClasseLinhaKaraoke.TRADUZIVEL_INGLES) {
                        continue;
                    }
                    EventoLegenda ev = eventos.get(i);
                    boolean tem = plano.temOriginalPreservadaNoInstante(ev);
                    if (tem) {
                        comOriginal++;
                    } else {
                        semOriginal++;
                    }
                    String[] campos = ev.prefixo() == null ? new String[0] : ev.prefixo().split(",");
                    linhas.add(String.join("\t",
                        pasta.getParent().getFileName().toString(),
                        arquivo.getFileName().toString(),
                        String.valueOf(i),
                        campos.length > 1 ? campos[1] : "?",
                        campos.length > 2 ? campos[2] : "?",
                        ev.estilo() == null ? "" : ev.estilo(),
                        String.valueOf(tem),
                        REMOVE_TAGS.matcher(ev.texto()).replaceAll("").replace('\t', ' ')));
                }
            }
        }
        String placar = "com original " + comOriginal + ", sem " + semOriginal;
        assertTrue(comOriginal > 0 && semOriginal > 0, () -> "controle positivo: a foto tem de ter as DUAS "
            + "decisoes (" + placar + "), senao o instrumento esta cego");

        Path saida = Path.of("build", "medicao", "camada_original_no_instante_"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".tsv");
        Files.createDirectories(saida.getParent());
        Files.write(saida, linhas, StandardCharsets.UTF_8);
        System.out.printf("FOTO %s | linhas traduziveis %d | com original no instante %d | sem (empilham) %d%n",
            saida.toAbsolutePath(), linhas.size() - 1, comOriginal, semOriginal);
    }
}
