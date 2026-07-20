package org.traducao.projeto.revisaoConcordancia.domain;

import java.nio.file.Path;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: resultado (value object) da revisão de concordância de uma pasta —
 * quantos arquivos foram vistos/alterados, quantas falas foram corrigidas, os backups criados e
 * se a execução gravou ({@code aplicado}) ou foi dry-run. Fica na camada {@code domain} para ser
 * o contrato estável entre o caso de uso (application) e a apresentação (presentation), sem
 * carregar orquestração nem framework.
 *
 * <p>INVARIANTES DO DOMÍNIO: contagens são {@code >= 0}; {@code backups} é imutável; nada aqui
 * some falas — {@code falasCorrigidas} conta só as que realmente mudaram.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: portador de dados puro; a lista recebida é copiada
 * defensivamente para não vazar referência mutável.
 */
public record ResultadoConcordancia(
    int arquivosAnalisados,
    int arquivosAlterados,
    int falasCorrigidas,
    List<Path> backups,
    boolean aplicado
) {
    public ResultadoConcordancia {
        backups = backups == null ? List.of() : List.copyOf(backups);
    }
}
