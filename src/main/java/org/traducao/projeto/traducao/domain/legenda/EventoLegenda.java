package org.traducao.projeto.traducao.domain.legenda;

public record EventoLegenda(
    int indice,
    String tipoLinha,
    String estilo,
    String prefixo,
    String texto
) {
    public boolean temTexto() {
        return texto != null;
    }

    public boolean isDialogo() {
        return "Dialogue".equals(tipoLinha);
    }

    public EventoLegenda comTexto(String novoTexto) {
        return new EventoLegenda(indice, tipoLinha, estilo, prefixo, novoTexto);
    }
}
