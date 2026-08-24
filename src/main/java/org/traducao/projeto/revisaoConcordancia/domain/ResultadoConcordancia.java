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
 * <p><b>{@code arquivosAnalisados} conta o que foi REVISADO, não o que existe na pasta.</b> O
 * que a tela deixou de fora de propósito vai em {@code arquivosForaDoAlcance}, separado: somar os
 * dois faria o relatório dizer "47 analisados" quando parte deles sequer foi aberta — e um número
 * que mistura "revisei" com "nem olhei" é pior que número nenhum, porque parece prova.
 *
 * <h2>Por que a contagem é separada por CORRETOR (23/08/2026)</h2>
 * A tela passou a ter dois: o de gênero, que decide por determinante, e o de acento, que resolve
 * a inversão verbo/substantivo. Um número só diria "78 falas corrigidas" sem dizer por quê — e a
 * ordem permanente de telemetria neste projeto é contador do que AGIU <b>e</b> contador do que se
 * ABSTEVE, para que a próxima medição não precise adivinhar de onde veio o ganho.
 *
 * <p><b>{@code revisorGramaticalDisponivel} não é enfeite:</b> sem ele, zero correções de acento
 * teria a mesma cara quando o texto está limpo e quando o motor não subiu. É a invariante 12 do
 * projeto — saída vazia ambígua é bug —, e o motivo viaja junto para o operador ler na tela.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: portador de dados puro; a lista recebida é copiada
 * defensivamente para não vazar referência mutável.
 */
public record ResultadoConcordancia(
    int arquivosAnalisados,
    int arquivosAlterados,
    int falasCorrigidas,
    List<Path> backups,
    boolean aplicado,
    int arquivosForaDoAlcance,
    int falasPorGenero,
    int falasPorAcento,
    boolean revisorGramaticalDisponivel,
    String motivoRevisorIndisponivel,
    List<ContagemCorretor> porCorretor
) {
    /** Compatibilidade com os chamadores que existiam antes do campo "fora do alcance". */
    public ResultadoConcordancia(int arquivosAnalisados, int arquivosAlterados, int falasCorrigidas,
                                 List<Path> backups, boolean aplicado) {
        this(arquivosAnalisados, arquivosAlterados, falasCorrigidas, backups, aplicado, 0);
    }

    /** Compatibilidade com os chamadores anteriores ao segundo corretor (acento), em 23/08/2026. */
    public ResultadoConcordancia(int arquivosAnalisados, int arquivosAlterados, int falasCorrigidas,
                                 List<Path> backups, boolean aplicado, int arquivosForaDoAlcance) {
        this(arquivosAnalisados, arquivosAlterados, falasCorrigidas, backups, aplicado,
            arquivosForaDoAlcance, falasCorrigidas, 0, true, null, List.of());
    }

    public ResultadoConcordancia {
        backups = backups == null ? List.of() : List.copyOf(backups);
        porCorretor = porCorretor == null ? List.of() : List.copyOf(porCorretor);
    }
}
