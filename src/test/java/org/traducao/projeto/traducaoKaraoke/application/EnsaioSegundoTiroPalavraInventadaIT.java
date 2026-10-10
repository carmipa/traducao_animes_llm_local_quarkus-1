package org.traducao.projeto.traducaoKaraoke.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;
import org.traducao.projeto.core.texto.dicionarioOrtografia.VeredictoPalavra;
import org.traducao.projeto.lore.infrastructure.GerenciadorContexto;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.llm.domain.Lote;
import org.traducao.projeto.llm.domain.TraducaoLote;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PROPÓSITO DE NEGÓCIO: ENSAIO, no modelo real, de quanto um segundo tiro determinístico conserta
 * a palavra inventada da letra de karaokê — a medida que decide se a guarda refaz ou só registra
 * (regra A8: a ação de cada estado sai da consequência, e o custo do bloqueio é medido antes).
 *
 * <h2>Critério declarado ANTES de rodar</h2>
 * Um segundo tiro só vale se, nos 4 casos conhecidos, a primeira resposta reproduzir a invenção
 * (controle: a temperatura 0 é determinística) e a variante consertar pelo menos 3 de 4 com
 * português sem palavra desconhecida. Abaixo disso a guarda só REGISTRA, e não refaz nada.
 * Limitação: são os únicos 4 casos conhecidos — não há amostra reservada para validar.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A primeira resposta sai pelo CAMINHO DE PRODUÇÃO ({@link TradutorDeLetraKaraoke#traduzirViaLlm})
 *       com o prompt da lore que gravou o cache ({@code GerenciadorContexto#snapshotPorId}).</li>
 *   <li>Não escreve nada: imprime.</li>
 *   <li>Travado por {@code -Dkronos.comparacao=true}: chama o LM Studio.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * LLM fora do ar ou dicionário indisponível imprimem NÃO VERIFICADO e o ensaio não conclui nada.
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.comparacao", matches = "true")
class EnsaioSegundoTiroPalavraInventadaIT {

    private record Caso(String contextoId, String original, String inventada) { }

    private static final List<Caso> CASOS = List.of(
        new Caso("break_blade_1", "I'd act on all of my feelings", "Açãoaria"),
        new Caso("gundam_0083", "Angel come and take me by the hand", "Anjel"),
        new Caso("gundam_0083", "I don't need dreams that have crumbled away.", "desfezera"),
        new Caso("eight_six", "Chanting Alchemilla, Alchemilla", "Alchemia"));

    @Inject
    TradutorDeLetraKaraoke tradutor;

    @Inject
    LlmPort llm;

    @Inject
    GerenciadorContexto contextos;

    @Test
    @DisplayName("ensaio: a primeira resposta reproduz a invencao, e que variante a conserta")
    void ensaiar() {
        CorretorOrtograficoLegenda dicionario = new CorretorOrtograficoLegenda();
        System.out.println("LLM: " + llm.verificarDisponibilidade() + " | modelo: " + llm.modeloAtivo());
        StringBuilder sb = new StringBuilder("\n=== ENSAIO DO SEGUNDO TIRO ===\n");
        for (Caso caso : CASOS) {
            String prompt = contextos.snapshotPorId(caso.contextoId()).promptSistema();
            sb.append("\n[").append(caso.contextoId()).append("] ").append(caso.original()).append('\n');

            String primeira = tradutor.traduzirViaLlm(caso.original(), new ArrayList<>(),
                new AtomicInteger(), prompt);
            sb.append(linha("V0 producao", caso, primeira, dicionario));

            String alternada = caso.original().matches(".*[.!?]$")
                ? caso.original().substring(0, caso.original().length() - 1)
                : caso.original() + ".";
            sb.append(linha("V1 pontuacao", caso,
                tradutor.traduzirViaLlm(alternada, new ArrayList<>(), new AtomicInteger(), prompt), dicionario));

            String reticencias = caso.original().replaceAll("[.!?]$", "") + "...";
            sb.append(linha("V2 reticencias", caso,
                tradutor.traduzirViaLlm(reticencias, new ArrayList<>(), new AtomicInteger(), prompt), dicionario));

            for (int i = 1; i <= 2; i++) {
                TraducaoLote r = llm.traduzir(new Lote(i, List.of(caso.original())), 0.3d, prompt);
                String t = r != null && r.sucesso() && r.linhasTraduzidas() != null && !r.linhasTraduzidas().isEmpty()
                    ? r.linhasTraduzidas().getFirst() : null;
                sb.append(linha("V3 temp0.3#" + i, caso, t, dicionario));
            }
        }
        System.out.println(sb);
    }

    private static String linha(String rotulo, Caso caso, String traduzido, CorretorOrtograficoLegenda dic) {
        if (traduzido == null) {
            return String.format(Locale.ROOT, "  %-16s (sem traducao)%n", rotulo);
        }
        Set<String> doOriginal = new LinkedHashSet<>();
        CorretorOrtograficoLegenda.palavrasDe(caso.original()).forEach(p -> doOriginal.add(p.toLowerCase(Locale.ROOT)));
        List<String> novas = CorretorOrtograficoLegenda.palavrasDe(traduzido).stream()
            .filter(p -> !doOriginal.contains(p.toLowerCase(Locale.ROOT))).toList();
        Map<String, VeredictoPalavra> v = dic.classificarPalavras(novas);
        List<String> desconhecidas = novas.stream().filter(p -> v.get(p) == VeredictoPalavra.DESCONHECIDA).toList();
        String estado = !dic.disponivel() ? "NAO VERIFICADO"
            : desconhecidas.isEmpty() ? "limpa" : "INVENTADA " + desconhecidas;
        return String.format(Locale.ROOT, "  %-16s %-34s %s%n", rotulo, estado, traduzido);
    }
}
