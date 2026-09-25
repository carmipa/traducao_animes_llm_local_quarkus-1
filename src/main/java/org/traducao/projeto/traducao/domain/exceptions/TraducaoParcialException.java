package org.traducao.projeto.traducao.domain.exceptions;

import org.traducao.projeto.llm.domain.TraducaoLote;
import java.util.List;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: o episódio parou antes de publicar a legenda, mas o que já foi traduzido
 * viaja junto para ser salvo no cache e retomado depois.
 *
 * <p>INVARIANTES DO DOMÍNIO: {@link #interrompidaPeloUsuario()} diz se a parada foi PEDIDA
 * (Sair / parada da fila) e não uma falha. A marca viaja NA EXCEÇÃO de propósito: o flag de
 * interrupção da thread é consumido no caminho — pela barra de progresso e pela espera da escrita
 * atômica —, e medido em 25/09/2026 um "Sair" real chegava ao controller com o flag já limpo e
 * saía como FALHOU.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem a marca, a parada é tratada como falha (o lado
 * conservador).
 */
public class TraducaoParcialException extends TradutorException {

    private final List<TraducaoLote> lotesSalvos;
    private final Map<String, String> dicionarioParcial;
    private boolean interrompidaPeloUsuario;

    /** Marca esta parada como pedida pelo operador; devolve a própria exceção. */
    public TraducaoParcialException marcarInterrompidaPeloUsuario() {
        this.interrompidaPeloUsuario = true;
        return this;
    }

    /** {@code true} quando a parada foi pedida (Sair / parada da fila), não uma falha. */
    public boolean interrompidaPeloUsuario() {
        return interrompidaPeloUsuario;
    }

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
