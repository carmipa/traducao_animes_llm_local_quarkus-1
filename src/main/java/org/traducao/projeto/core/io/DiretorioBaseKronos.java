package org.traducao.projeto.core.io;

import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: ponto único de resolução da raiz onde o KRONOS grava
 * seus artefatos operacionais (telemetria em {@code logs/}, relatórios em
 * {@code relatorios/}, cache em {@code cache/}, backups em {@code backups/}).
 * Em produção a raiz é o próprio diretório de trabalho do processo, preservando
 * exatamente o comportamento local histórico do projeto. Durante a suíte de
 * testes a raiz é redirecionada (via system property {@code kronos.dir.base},
 * configurada no {@code build.gradle}) para uma árvore descartável em
 * {@code build/tmp/kronos-tests}, impedindo que os testes contaminem os
 * diretórios operacionais reais versionados pelo Git.
 *
 * <p>INVARIANTES DO DOMÍNIO:
 * <ul>
 *   <li>Quando {@code kronos.dir.base} está ausente ou em branco, a raiz é o
 *       diretório de trabalho corrente ({@code Path.of("")}), de modo que
 *       {@code resolver("cache")} é idêntico a {@code Path.of("cache")} — o
 *       comportamento de produção não muda.</li>
 *   <li>A raiz é lida da system property a cada chamada, não capturada em campo
 *       estático, para que o valor definido no lançamento da JVM de teste valha
 *       inclusive para constantes resolvidas em tempo de carga de classe.</li>
 * </ul>
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: não lança exceção própria. Se a property
 * contiver um caminho sintaticamente inválido, a exceção de {@link Path#of}
 * propaga ao chamador; com property ausente/branca cai no diretório corrente.
 */
public final class DiretorioBaseKronos {

    /** Nome da system property que redireciona a raiz operacional em testes. */
    public static final String PROPRIEDADE_BASE = "kronos.dir.base";

    private DiretorioBaseKronos() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve a raiz operacional efetiva do processo.
     *
     * <p>INVARIANTES DO DOMÍNIO: property ausente/branca ⇒ {@code Path.of("")}
     * (diretório corrente), garantindo paridade com o comportamento anterior.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga
     * {@link java.nio.file.InvalidPathException} se a property tiver valor
     * sintaticamente inválido.
     */
    public static Path base() {
        String valor = System.getProperty(PROPRIEDADE_BASE);
        if (valor == null || valor.isBlank()) {
            return Path.of("");
        }
        return Path.of(valor);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: resolve um caminho de artefato operacional sob a
     * raiz efetiva (ex.: {@code resolver("relatorios", nomePasta)}).
     *
     * <p>INVARIANTES DO DOMÍNIO: com raiz corrente, {@code resolver("logs")}
     * equivale exatamente a {@code Path.of("logs")}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga
     * {@link java.nio.file.InvalidPathException} de segmentos inválidos.
     */
    public static Path resolver(String primeiro, String... resto) {
        Path caminho = base().resolve(primeiro);
        for (String segmento : resto) {
            caminho = caminho.resolve(segmento);
        }
        return caminho;
    }
}
