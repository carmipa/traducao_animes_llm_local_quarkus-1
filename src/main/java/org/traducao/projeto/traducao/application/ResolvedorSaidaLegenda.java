package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: resolve os caminhos de saída da tradução de uma legenda —
 * o artefato final {@code _PT-BR}, a variante {@code .parcial} para resultados
 * incompletos e a extensão canônica do formato — isolando o endereçamento de saída
 * da orquestração de {@link ProcessarArquivoUseCase} (FASE F, R1).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A saída final preserva a pasta de destino e a extensão do formato, troca o
 *       sufixo {@code _ENG} por {@code _PT-BR} e nunca converte o formato
 *       (.ass/.ssa/.srt).</li>
 *   <li>O artefato parcial mantém pasta e extensão, acrescentando {@code .parcial}
 *       imediatamente antes da extensão.</li>
 *   <li>Sem pendências, publica sempre o caminho final; com pendências, só publica o
 *       final quando a proteção foi liberada explicitamente.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Funções puras, sem I/O nem exceção: um nome sem extensão reconhecida recebe o
 * padrão {@code .ass} do módulo e o cálculo prossegue deterministicamente.
 */
@Component
public class ResolvedorSaidaLegenda {

    /**
     * PROPÓSITO DE NEGÓCIO: identifica a extensão canônica do formato de legenda a
     * partir do nome do arquivo, para preservar o mesmo formato na saída e no cache.
     *
     * <p>INVARIANTES DO DOMÍNIO: reconhece {@code .srt} e {@code .ssa}; qualquer
     * outro nome é tratado como {@code .ass} (formato padrão do módulo).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: função pura; nome sem extensão reconhecida
     * devolve {@code .ass} em vez de lançar.
     */
    public String extensaoLegenda(String nome) {
        String lower = nome.toLowerCase();
        if (lower.endsWith(".srt")) {
            return ".srt";
        }
        return lower.endsWith(".ssa") ? ".ssa" : ".ass";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: calcula o caminho da saída PT-BR final de um episódio,
     * o artefato que o remux consome.
     *
     * <p>INVARIANTES DO DOMÍNIO: mantém a extensão do formato de entrada, remove o
     * sufixo {@code _ENG} da base e acrescenta {@code _PT-BR}; o arquivo é resolvido
     * dentro do diretório de saída informado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: função pura, sem acesso a disco; não lança.
     */
    public Path resolverSaidaFinal(Path entrada, Path diretorioSaida) {
        String nome = entrada.getFileName().toString();
        String extensao = extensaoLegenda(nome);
        String base = nome.substring(0, nome.length() - extensao.length());
        String baseSemSufixoIngles = base.replaceFirst("(?i)_ENG$", "");
        return diretorioSaida.resolve(baseSemSufixoIngles + "_PT-BR" + extensao);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se uma retomada publica diretamente a saída
     * PT-BR final ou mantém o resultado incompleto isolado como arquivo parcial.
     *
     * <p>INVARIANTES DO DOMÍNIO: sem pendências, sempre publica a saída final; com
     * pendências, só publica a saída final quando a proteção foi liberada
     * explicitamente, mantendo o comportamento seguro como padrão.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não acessa disco nem lança exceção; retorna
     * deterministicamente um caminho final ou com sufixo {@code .parcial}.
     */
    public Path selecionar(Path arquivoFinal, boolean temPendencias, boolean protecaoLiberada) {
        return !temPendencias || protecaoLiberada
            ? arquivoFinal : resolverParcial(arquivoFinal);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: diferencia visualmente uma legenda ainda incompleta do
     * artefato PT-BR final usado no remux.
     *
     * <p>INVARIANTES DO DOMÍNIO: preserva pasta e extensão e acrescenta o sufixo
     * {@code .parcial} antes da extensão.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: caminho sem extensão reconhecida ainda
     * recebe o sufixo calculado a partir da convenção ASS/SRT do módulo.
     */
    private Path resolverParcial(Path arquivoFinal) {
        String nome = arquivoFinal.getFileName().toString();
        String extensao = extensaoLegenda(nome);
        String base = nome.substring(0, nome.length() - extensao.length());
        return arquivoFinal.resolveSibling(base + ".parcial" + extensao);
    }
}
