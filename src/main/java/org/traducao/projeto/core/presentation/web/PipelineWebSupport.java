package org.traducao.projeto.core.presentation.web;

import org.traducao.projeto.core.io.DiretorioBaseKronos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.core.execucao.FilaExecucaoPipeline;
import org.traducao.projeto.core.util.DuracaoUtil;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: concentra os utilitários compartilhados pelos
 * controllers web do pipeline — a normalização de caminhos digitados/colados na
 * interface e o enfileiramento padronizado de jobs pesados na fila única de
 * execução. Existe para que todos os endpoints entrem na MESMA fila e imprimam o
 * MESMO formato de relatório final, sem duplicar essa lógica em cada controller.
 *
 * <p>INVARIANTES DO DOMÍNIO: expõe a única {@link FilaExecucaoPipeline}
 * compartilhada (bean CDI) — jamais deve existir mais de uma instância de fila;
 * todo job pesado passa por {@link #submeterJobComRelatorio}, garantindo canal
 * SSE definido antes da execução e execução sequencial em segundo plano.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: {@link #normalizarCaminho(String)} devolve
 * {@code null} para entrada nula/vazia ou sintaxe de caminho inválida
 * ({@link InvalidPathException}), registrando aviso no log; o corpo submetido em
 * {@link #submeterJobComRelatorio} sempre imprime a linha de relatório final,
 * mesmo quando lança exceção, via bloco {@code finally}.
 */
@Component
public class PipelineWebSupport {

    private static final Logger log = LoggerFactory.getLogger(PipelineWebSupport.class);

    // Fila única compartilhada por todos os módulos (ver FilaExecucaoPipeline):
    // garante execução sequencial em segundo plano e impede que outro endpoint
    // troque o contexto/modelo LLM no meio de um job em andamento.
    private final FilaExecucaoPipeline filaExecucao;
    private final LogStreamService logStreamService;

    public PipelineWebSupport(FilaExecucaoPipeline filaExecucao, LogStreamService logStreamService) {
        this.filaExecucao = filaExecucao;
        this.logStreamService = logStreamService;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: submete um job à fila já com o canal SSE correto e
     * imprime, ao final (sucesso OU falha), a linha padrão de relatório com o
     * tempo total — todos os consoles da UI encerram com o mesmo formato de
     * resumo.
     *
     * <p>INVARIANTES DO DOMÍNIO: o canal SSE é definido como primeiro passo,
     * antes de qualquer saída do corpo; a execução ocorre na thread única da fila
     * compartilhada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: exceções lançadas por {@code corpo} não
     * são engolidas aqui, mas o {@code finally} garante que a linha de relatório
     * final com o tempo total seja sempre impressa.
     */
    public void submeterJobComRelatorio(String canal, String nomeOperacao, Runnable corpo) {
        filaExecucao.submeter(() -> {
            logStreamService.definirCanalAtual(canal);
            long inicioMs = System.currentTimeMillis();
            try {
                corpo.run();
            } finally {
                System.out.println(DuracaoUtil.linhaRelatorioFinal(nomeOperacao, inicioMs));
            }
        });
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte o texto de caminho digitado ou colado na
     * interface em um {@link Path} utilizável, tolerando aspas envolventes que os
     * usuários frequentemente incluem ao copiar caminhos do explorador.
     *
     * <p>INVARIANTES DO DOMÍNIO: entrada nula/em branco não vira caminho; aspas
     * simples/duplas nas extremidades são removidas antes da conversão.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@code null} para entrada
     * inválida ou sintaxe de caminho ilegal ({@link InvalidPathException}),
     * registrando aviso no log — o endpoint chamador decide o HTTP 400.
     */
    public Path normalizarCaminho(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            String limpo = valor.trim();
            if ((limpo.startsWith("\"") && limpo.endsWith("\"")) || (limpo.startsWith("'") && limpo.endsWith("'"))) {
                if (limpo.length() >= 2) {
                    limpo = limpo.substring(1, limpo.length() - 1).trim();
                }
            }
            // Ancorado em DiretorioBaseKronos: um caminho ABSOLUTO (o caso normal vindo da
            // UI) passa intocado; um caminho RELATIVO ("cache", "saida") fica sob a raiz
            // operacional efetiva. Em produção a raiz é o diretório corrente e o resultado
            // é idêntico ao histórico. Na suíte, isto impede que um POST com caminho
            // relativo enfileire um job que varre os diretórios versionados pelo Git —
            // foi assim que testes de contrato HTTP esvaziaram traduções reais de 28 caches.
            return limpo.isBlank() ? null : DiretorioBaseKronos.resolver(limpo);
        } catch (InvalidPathException e) {
            log.warn("Caminho inválido informado: {}", valor);
            return null;
        }
    }
}
