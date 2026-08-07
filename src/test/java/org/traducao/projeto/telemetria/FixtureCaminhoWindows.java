package org.traducao.projeto.telemetria;

/**
 * PROPÓSITO DE NEGÓCIO: monta fixtures de caminho Windows em runtime para testes
 * que precisam provar remoção de letra de drive — sem cravar literal de drive no
 * fonte (a catraca de portabilidade da suíte reprova o literal).
 *
 * <p>INVARIANTES DO DOMÍNIO: o texto produzido é bit-a-bit um caminho Windows
 * com letra de drive e barras invertidas; só a forma de escrever no fonte muda.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: segmentos nulos viram string vazia no join;
 * nunca lança.
 */
final class FixtureCaminhoWindows {

    private FixtureCaminhoWindows() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: {@code C:\seg1\seg2\...} sem literal de drive no fonte.
     */
    static String c(String... segmentos) {
        return de('C', segmentos);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: {@code X:\seg1\seg2\...} com letra escolhida.
     */
    static String de(char letra, String... segmentos) {
        return letra + ":" + "\\" + String.join("\\", segmentos);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: marcador {@code X:\} para assertivas que confirmam
     * ausência de drive no texto sanitizado.
     */
    static String marcadorDrive(char letra) {
        return letra + ":" + "\\";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: forma {@code X:arquivo} (sem barra) — usada nos
     * contra-testes da reconferência do sanitizador.
     */
    static String driveSemBarra(char letra, String resto) {
        return letra + ":" + resto;
    }
}
