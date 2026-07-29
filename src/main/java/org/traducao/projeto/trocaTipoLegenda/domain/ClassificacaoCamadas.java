package org.traducao.projeto.trocaTipoLegenda.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: resposta, em dados puros, das perguntas que o achatamento
 * precisa fazer sobre cada linha de uma legenda — "esta linha é a camada ORIGINAL da
 * música, que não pode ser achatada?" e "esta linha é uma SÍLABA de timing de karaokê,
 * que não deve virar legenda?".
 *
 * <p>Existe para que a fatia {@code trocaTipoLegenda} pare de carregar dentro do seu
 * {@code application} os serviços de decisão do peer {@code legenda}. Antes, o achatador
 * chamava {@code ProtecaoCamadasMusicaisService} e {@code DetectorEfeitoKaraokeService}
 * diretamente: qualquer mudança na regra de karaokê daquele peer mudava, sem aviso, o
 * resultado do achatamento. Agora a fatia declara o que precisa saber e a regra fica
 * atrás de {@code ClassificadorCamadaMusicalPort}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Índices são os de {@code EventoLegenda.indice()} do documento classificado —
 *       um conjunto pertence a UM documento e não faz sentido em outro.</li>
 *   <li>Os dois conjuntos são independentes: uma linha pode ser preservada sem ser
 *       sílaba, e uma sílaba de timing nunca é também "preservar" (seria contraditório
 *       — preservar mantém, sílaba descarta).</li>
 *   <li>Cópias defensivas: o estado não muda por mutação externa do conjunto original.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Conjuntos nulos viram vazios. {@link #VAZIA} representa "nada classificado", e com ela
 * o achatamento se comporta exatamente como antes de existir a classificação.
 */
public record ClassificacaoCamadas(Set<Integer> preservarIntacto, Set<Integer> silabasDeTiming) {

    /** Nenhuma linha classificada: o achatador trata o documento como diálogo comum. */
    public static final ClassificacaoCamadas VAZIA = new ClassificacaoCamadas(Set.of(), Set.of());

    public ClassificacaoCamadas {
        preservarIntacto = preservarIntacto == null
            ? Set.of() : Collections.unmodifiableSet(new HashSet<>(preservarIntacto));
        silabasDeTiming = silabasDeTiming == null
            ? Set.of() : Collections.unmodifiableSet(new HashSet<>(silabasDeTiming));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: diz se a linha é camada original de música (romaji/letra
     * japonesa) e portanto NÃO pode ser achatada para o estilo de diálogo.
     *
     * <p>INVARIANTES DO DOMÍNIO: consulta por índice do evento; nunca lança.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: índice desconhecido devolve {@code false}.
     */
    public boolean devePreservar(int indice) {
        return preservarIntacto.contains(indice);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: diz se a linha é uma sílaba/palavra isolada da camada de
     * TIMING do karaokê — o "sa", "cri", "feel", "on" que só existe para acender uma
     * parte da frase no tempo certo.
     *
     * <p>INVARIANTES DO DOMÍNIO: consulta por índice do evento; nunca lança.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: índice desconhecido devolve {@code false}.
     */
    public boolean ehSilabaDeTiming(int indice) {
        return silabasDeTiming.contains(indice);
    }
}
