package org.traducao.projeto.auditorConteudoLegendas.domain;

import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import java.util.List;

public interface RegraAuditoriaConteudo {
    String getNome();
    List<AnomaliaConteudo> auditar(DocumentoLegenda original, DocumentoLegenda traduzido);
}
