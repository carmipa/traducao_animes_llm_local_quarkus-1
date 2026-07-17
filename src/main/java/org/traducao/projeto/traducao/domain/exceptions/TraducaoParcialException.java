package org.traducao.projeto.traducao.domain.exceptions;

import org.traducao.projeto.llm.domain.TraducaoLote;
import java.util.List;
import java.util.Map;

public class TraducaoParcialException extends TradutorException {
    
    private final List<TraducaoLote> lotesSalvos;
    private final Map<String, String> dicionarioParcial;

    // Construtor usado pela camada do Episódio (nível de Lotes)
    public TraducaoParcialException(String message, List<TraducaoLote> lotesSalvos, Throwable cause) {
        super(message, cause);
        this.lotesSalvos = lotesSalvos;
        this.dicionarioParcial = null;
    }

    // Construtor usado pela camada de Arquivo (nível de Falas Mascaradas)
    public TraducaoParcialException(String message, Map<String, String> dicionarioParcial, Throwable cause) {
        super(message, cause);
        this.lotesSalvos = null;
        this.dicionarioParcial = dicionarioParcial;
    }

    public List<TraducaoLote> getLotesSalvos() {
        return lotesSalvos;
    }

    public Map<String, String> getDicionarioParcial() {
        return dicionarioParcial;
    }
}
