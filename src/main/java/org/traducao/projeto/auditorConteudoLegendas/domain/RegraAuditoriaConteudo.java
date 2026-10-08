package org.traducao.projeto.auditorConteudoLegendas.domain;

import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import java.util.List;

public interface RegraAuditoriaConteudo {
    String getNome();
    List<AnomaliaConteudo> auditar(DocumentoLegenda original, DocumentoLegenda traduzido);

    /**
     * PROPÓSITO DE NEGÓCIO: diz se a regra compara o par evento a evento PELA POSIÇÃO — e,
     * portanto, se só pode rodar quando original e traduzido têm a mesma estrutura. Com uma
     * linha a mais ou a menos, a posição k de um lado não é a fala k do outro, e a regra
     * acusa centenas de "falas trocadas" que não existem (auditoria de 08/10/2026, A2: 379
     * anomalias de efeito vazado num par cujo único defeito era uma fala apagada).
     *
     * <p>INVARIANTES DO DOMÍNIO: o padrão é {@code true}, o lado seguro — regra nova nasce
     * pulada quando o pareamento não é confiável. Só a regra que MEDE o pareamento declara
     * {@code false}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: método puro; não lança.
     */
    default boolean dependeDoPareamentoPorPosicao() {
        return true;
    }
}
