package org.traducao.projeto.legendasExtracao.application.strategy;

import org.springframework.stereotype.Component;
import org.traducao.projeto.legendasExtracao.domain.FaixaLegenda;
import org.traducao.projeto.legendasExtracao.domain.FormatoLegenda;

import java.util.List;
import java.util.Optional;

@Component
public class ExtratorAssStrategy implements ExtratorStrategy {

    @Override
    public boolean suporta(FormatoLegenda formato) {
        return formato == FormatoLegenda.ASS;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: escolher, entre as faixas ASS do contêiner, a que carrega o
     * DIÁLOGO — e não a faixa reduzida de letreiros e músicas que os lançamentos com áudio
     * dublado trazem junto.
     *
     * <h2>O prejuízo que criou o filtro negativo</h2>
     * Até 06/08/2026 a regra era: tenta palavra-chave e, falhando, <b>pega a última candidata</b>,
     * apoiada no comentário "a primeira é signs". No Break Blade a ordem é a inversa — a faixa
     * completa é {@code [Coalgirls]} (id 3) e a última é {@code Signs/Songs} (id 4). Resultado
     * medido: os <b>6 filmes foram extraídos e traduzidos inteiros da faixa errada</b>, com 373
     * falas quando o diálogo real tem 3.457 — 9,3x menos. E a palavra {@code Signs} estava no
     * nome da faixa o tempo todo, sem ninguém a ler.
     *
     * <h2>INVARIANTES DO DOMÍNIO</h2>
     * <ul>
     *   <li>Faixa cujo nome indica letreiro/música, ou marcada como {@code forced}, é
     *       DESPRIORIZADA — nunca escolhida enquanto existir alternativa.</li>
     *   <li>Ordem de posição deixa de ser critério isolado: só decide entre as que sobraram
     *       depois do filtro negativo.</li>
     *   <li>Se TODAS forem de letreiro, devolve mesmo assim. Legenda reduzida é melhor que
     *       nenhuma, e recusar aqui transformaria um arquivo pobre em erro de operação.</li>
     * </ul>
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem nenhuma faixa ASS, devolve {@link Optional#empty()}.
     * Nome nulo é tratado como vazio; nunca lança.
     */
    @Override
    public Optional<FaixaLegenda> selecionarMelhorFaixa(List<FaixaLegenda> faixasDisponiveis) {
        List<FaixaLegenda> candidatas = faixasDisponiveis.stream()
                .filter(f -> {
                    String c = f.codec().toLowerCase();
                    String cid = f.codecId().toLowerCase();
                    return c.contains("ass") || c.contains("substation") || cid.contains("ass");
                })
                .toList();

        if (candidatas.isEmpty()) {
            return Optional.empty();
        }

        // 1. Descartar o que se ANUNCIA como faixa reduzida. Feito ANTES de qualquer outra
        //    regra: sem isto, a heurística de posição escolhe a faixa de letreiros sempre que
        //    ela vier por último, que é justamente o layout do Break Blade.
        List<FaixaLegenda> principais = candidatas.stream()
                .filter(f -> !ehFaixaDeLetreiro(f))
                .toList();

        // 2. Se sobrou alguma, é entre elas que se decide. Se NÃO sobrou nenhuma, o contêiner
        //    só tem faixa reduzida — devolver a melhor delas é melhor que devolver nada.
        List<FaixaLegenda> alvo = principais.isEmpty() ? candidatas : principais;

        // 3. Palavra-chave que declara faixa completa.
        for (FaixaLegenda f : alvo) {
            String n = nomeNormalizado(f);
            if (n.contains("dialogue") || n.contains("full") || n.contains("complete")
                || n.contains("legendado") || n.contains("gcs8") || n.contains("english")) {
                return Optional.of(f);
            }
        }

        // 4. Sem declaração, a última costuma ser a completa — agora sobre um conjunto já
        //    limpo das faixas de letreiro.
        return Optional.of(alvo.getLast());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reconhece a faixa reduzida que acompanha lançamento dublado —
     * traduz só placa, cartaz e letra de música, e nunca o diálogo.
     *
     * <p>INVARIANTES DO DOMÍNIO: decide pelo que a faixa DECLARA (nome e flag {@code forced}),
     * nunca pelo tamanho ou pela posição. {@code forced} entra porque faixa forçada é, por
     * convenção do formato, exatamente o subconjunto que aparece sobre áudio dublado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nome nulo devolve {@code false} — na dúvida a faixa
     * permanece candidata, porque excluir demais é o defeito pior aqui.
     */
    private boolean ehFaixaDeLetreiro(FaixaLegenda faixa) {
        if (faixa.isForced()) {
            return true;
        }
        String n = nomeNormalizado(faixa);
        return n.contains("sign") || n.contains("song") || n.contains("forced")
            || n.contains("letreiro") || n.contains("s&s");
    }

    private String nomeNormalizado(FaixaLegenda faixa) {
        return faixa.nome() == null ? "" : faixa.nome().toLowerCase();
    }
}
