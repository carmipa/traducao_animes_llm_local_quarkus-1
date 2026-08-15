package org.traducao.projeto.traducaoKaraoke.domain;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: carrega para o manifesto tudo o que a execução de karaokê sabe sobre o
 * PRÓPRIO desfecho — como terminou, o que quebrou por arquivo, e os estados de cegueira que antes
 * só existiam no console e sumiam com ele.
 *
 * <h2>Por que um record e não mais quatro parâmetros</h2>
 * {@code salvarManifesto} já recebia seis argumentos. Somar status, motivo, lista e flags soltas
 * deixaria valores do mesmo tipo lado a lado — troca silenciosa que compila e mente. Aqui cada
 * campo tem nome.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code falhas} nunca é nula: execução sem falha carrega lista VAZIA, e lista vazia é
 *       informação — significa "olhei e não houve", não "não olhei".</li>
 *   <li>{@code cacheIgnorado} marca a execução em que contexto ou modelo mudaram e TUDO foi
 *       retraduzido. Sem isso, um pico de tempo e de custo fica sem explicação no histórico.</li>
 *   <li>O dicionário é {@link EstadoDicionario}, <b>não um booleano</b> — ver a nota lá.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * O construtor normaliza {@code null} em lista vazia e blinda a lista contra alteração posterior.
 *
 * @param status          como a execução terminou
 * @param motivo          texto do aborto/interrupção; {@code null} quando terminou completa
 * @param falhas          arquivos que estouraram, com o motivo de cada um
 * @param cacheIgnorado   o cache foi descartado por proveniência divergente
 * @param estadoDicionario se a ortografia pôde ser verificada nesta execução
 */
public record DesfechoKaraoke(
    StatusExecucaoKaraoke status,
    String motivo,
    List<FalhaArquivoKaraoke> falhas,
    boolean cacheIgnorado,
    EstadoDicionario estadoDicionario
) {

    /**
     * PROPÓSITO DE NEGÓCIO: distingue as TRÊS situações da ortografia que um booleano fundiria em
     * duas — e a fusão é justamente a que engana.
     *
     * <p>{@code 0 acentos repostos} pode significar "não havia o que repor" (ótimo) ou "o hunspell
     * não está instalado e eu não verifiquei nada" (grave). Com um {@code boolean} os dois casos
     * imprimem o mesmo sinal; foi por esse tipo de fusão que uma guarda deste projeto passou meses
     * verde e cega.
     */
    public enum EstadoDicionario {
        /** Respondeu ao menos uma vez nesta execução: o número de acentos vale. */
        DISPONIVEL,
        /** Foi consultado e não respondeu — hunspell ausente. O número de acentos NÃO vale. */
        AUSENTE,
        /** Nenhuma linha chegou a precisar de correção; nada foi perguntado. */
        NAO_CONSULTADO
    }

    public DesfechoKaraoke {
        falhas = falhas == null ? List.of() : List.copyOf(falhas);
    }
}
