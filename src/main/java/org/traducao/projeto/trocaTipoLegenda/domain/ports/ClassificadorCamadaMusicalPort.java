package org.traducao.projeto.trocaTipoLegenda.domain.ports;

import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.trocaTipoLegenda.domain.ClassificacaoCamadas;

/**
 * PROPÓSITO DE NEGÓCIO: porta pela qual o achatamento pergunta quais linhas de uma
 * legenda são camada de música — e, dentro dela, quais são apenas sílabas de timing de
 * karaokê. A fatia {@code trocaTipoLegenda} declara aqui O QUE precisa saber; COMO se
 * decide fica no adaptador, fora do {@code application}.
 *
 * <h2>Por que existe</h2>
 * Até 2026-07-29 o {@code AchatadorEstilosDecorativosService} injetava
 * {@code DetectorEfeitoKaraokeService} e {@code ProtecaoCamadasMusicaisService} do peer
 * {@code legenda} direto no seu {@code application}. Era regra de negócio de outro
 * serviço rodando dentro deste: mexer no detector de karaokê mudava silenciosamente o
 * resultado do achatamento, e não havia como testar o achatador sem levantar o peer
 * inteiro junto.
 *
 * <p>{@link DocumentoLegenda} continua atravessando a fronteira de propósito — é o
 * formato ASS em si, o dado que trafega, não regra de decisão. Duplicá-lo criaria
 * conversão em toda borda sem isolar problema nenhum.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Chamada PURA de leitura: não altera o documento recebido.</li>
 *   <li>Uma classificação vale só para o documento que a originou — os índices são os
 *       dele.</li>
 *   <li>Implementação indisponível ou incapaz de decidir devolve
 *       {@link ClassificacaoCamadas#VAZIA}, e o achatamento age como antes de existir a
 *       classificação (viés de preservação).</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Documento nulo devolve {@link ClassificacaoCamadas#VAZIA}. A porta não lança.
 */
public interface ClassificadorCamadaMusicalPort {

    /**
     * PROPÓSITO DE NEGÓCIO: classifica, de uma vez por documento, quais linhas devem
     * ficar intactas e quais são sílabas de timing descartáveis.
     *
     * <p>INVARIANTES DO DOMÍNIO: leitura pura; índices referentes ao documento dado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@link ClassificacaoCamadas#VAZIA},
     * nunca {@code null}.
     */
    ClassificacaoCamadas classificar(DocumentoLegenda documento);
}
