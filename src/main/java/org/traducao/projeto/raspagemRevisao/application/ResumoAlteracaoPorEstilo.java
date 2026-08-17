package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: dizer, ao fim de cada arquivo, <b>quantas linhas mudaram e em que
 * estilos</b> — e destacar as musicais, que não podiam ter mudado nenhuma.
 *
 * <h2>O prejuízo que originou</h2>
 * Em 17/08/2026 a ponte do cache reescreveu <b>687 linhas de {@code Song ENG}</b> no Gundam 08th
 * MS Team e <b>283</b> no Guilty Crown. O console anunciava {@code [CACHE/RECUPERADO]} por evento,
 * que soa como coisa boa, e o resumo final dizia {@code corrigidas=0} — porque a ponte escreve
 * FORA do laço de correção. <b>Nenhuma das duas mensagens mentia, e junto as duas escondiam o
 * dano.</b> Só se descobriu comparando o backup com o arquivo final, à mão, num script fora do
 * produto.
 *
 * <p>Este resumo traz aquela comparação para dentro da corrida: o veto de música passa a se
 * provar sozinho, toda vez, sem depender de alguém estar olhando. É a regra 23 — documento e
 * hash provam a ENTRADA; só guarda executável prova a SAÍDA.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Compara o documento como veio DO DISCO com o que será gravado, então enxerga tanto o
 *       laço de correção quanto a ponte do cache, que roda antes dele.</li>
 *   <li>Três estados, nunca dois: comparado · nada mudou · <b>NÃO COMPARÁVEL</b>. Contagem de
 *       eventos diferente devolve {@link Resumo.NaoComparavel} — "não consegui comparar" e
 *       "nada mudou" não podem produzir o mesmo sinal.</li>
 *   <li>Não decide o que é música por conta própria: pergunta à {@link PoliticaEstiloMusical},
 *       que é a dona da regra e a mesma das outras telas.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Listas nulas devolvem {@link Resumo.NaoComparavel}. Nunca lança.
 */
@Service
public class ResumoAlteracaoPorEstilo {

    /** Quantas linhas mudaram num estilo, e se aquele estilo é musical. */
    public record LinhaEstilo(String estilo, int quantidade, boolean musical) {}

    /**
     * PROPÓSITO DE NEGÓCIO: o veredito da comparação, com o terceiro estado explícito.
     * <p>INVARIANTES DO DOMÍNIO: {@code NaoComparavel} SEMPRE carrega o motivo — guarda que
     * descarta o que não entende aprova por cegueira.
     * <p>COMPORTAMENTO EM CASO DE FALHA: portadores de dados puros.
     */
    public sealed interface Resumo permits Resumo.Comparado, Resumo.NaoComparavel {

        /** Comparação feita. {@code total} zero significa "nada mudou", e isso é um resultado. */
        record Comparado(List<LinhaEstilo> porEstilo, int total, int totalMusical)
            implements Resumo {}

        /** Não deu para comparar. NÃO é zero, e o motivo vai junto. */
        record NaoComparavel(String motivo) implements Resumo {}
    }

    private final PoliticaEstiloMusical politicaEstiloMusical;

    public ResumoAlteracaoPorEstilo(PoliticaEstiloMusical politicaEstiloMusical) {
        this.politicaEstiloMusical = politicaEstiloMusical;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: compara as duas versões do mesmo arquivo e agrupa o que mudou por
     * estilo, do maior para o menor.
     *
     * <p>INVARIANTES DO DOMÍNIO: compara posição a posição, que é o mesmo pareamento que a
     * gravação usa; tamanhos diferentes são {@link Resumo.NaoComparavel}, nunca zero.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nulo devolve {@link Resumo.NaoComparavel}; nunca lança.
     *
     * @param antes eventos como vieram do disco
     * @param depois eventos que serão gravados
     */
    public Resumo comparar(List<EventoLegenda> antes, List<EventoLegenda> depois) {
        if (antes == null || depois == null) {
            return new Resumo.NaoComparavel("uma das versões do arquivo não foi fornecida");
        }
        if (antes.size() != depois.size()) {
            return new Resumo.NaoComparavel("o arquivo mudou de tamanho ("
                + antes.size() + " -> " + depois.size() + " eventos): o pareamento por posição "
                + "deixou de valer e comparar aqui daria número errado com cara de certo");
        }

        Map<String, Integer> contagem = new LinkedHashMap<>();
        int total = 0;
        for (int i = 0; i < antes.size(); i++) {
            EventoLegenda a = antes.get(i);
            EventoLegenda d = depois.get(i);
            if (textoDe(a).equals(textoDe(d))) {
                continue;
            }
            total++;
            contagem.merge(nomeEstilo(a), 1, Integer::sum);
        }

        List<LinhaEstilo> linhas = new ArrayList<>();
        int musicais = 0;
        for (Map.Entry<String, Integer> e : contagem.entrySet()) {
            boolean musical = politicaEstiloMusical.estiloIgnorado(e.getKey());
            if (musical) {
                musicais += e.getValue();
            }
            linhas.add(new LinhaEstilo(e.getKey(), e.getValue(), musical));
        }
        linhas.sort(Comparator.comparingInt(LinhaEstilo::quantidade).reversed()
            .thenComparing(LinhaEstilo::estilo));
        return new Resumo.Comparado(List.copyOf(linhas), total, musicais);
    }

    private static String textoDe(EventoLegenda evento) {
        return evento == null || evento.texto() == null ? "" : evento.texto();
    }

    private static String nomeEstilo(EventoLegenda evento) {
        String estilo = evento == null ? null : evento.estilo();
        return estilo == null || estilo.isBlank() ? "(sem estilo)" : estilo;
    }
}
