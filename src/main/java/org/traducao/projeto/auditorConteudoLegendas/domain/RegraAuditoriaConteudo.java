package org.traducao.projeto.auditorConteudoLegendas.domain;

import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import java.util.List;

public interface RegraAuditoriaConteudo {
    String getNome();
    List<AnomaliaConteudo> auditar(DocumentoLegenda original, DocumentoLegenda traduzido);
}
