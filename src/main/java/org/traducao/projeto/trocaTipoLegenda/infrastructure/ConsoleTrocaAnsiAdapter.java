package org.traducao.projeto.trocaTipoLegenda.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.trocaTipoLegenda.domain.ports.ConsoleTrocaPort;

/**
 * PROPÓSITO DE NEGÓCIO: pinta as mensagens da fatia para o console do KRONOS,
 * traduzindo a INTENÇÃO declarada pelo caso de uso (sucesso, aviso, erro) no código de
 * escape correspondente.
 *
 * <p>É a única classe da fatia que conhece {@link AnsiCores}. Antes, os dois casos de
 * uso concatenavam cor em ~20 pontos do {@code application}: regra de negócio decidindo
 * escape ANSI, e nenhuma forma de testar a mensagem sem o texto de cor no meio.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O log recebe a mensagem SEM os códigos de cor — arquivo de log com escape ANSI
 *       é ilegível em editor e quebra busca por texto.</li>
 *   <li>Mensagem nula é ignorada silenciosamente; saída nunca derruba a operação.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Não lança: escrita em console é efeito colateral e não pode interromper o trabalho.
 */
@Component
public class ConsoleTrocaAnsiAdapter implements ConsoleTrocaPort {

    private static final Logger log = LoggerFactory.getLogger(ConsoleTrocaAnsiAdapter.class);

    @Override
    public void titulo(String mensagem) {
        emitir(AnsiCores.CYAN, mensagem);
    }

    @Override
    public void info(String mensagem) {
        emitir(null, mensagem);
    }

    @Override
    public void sucesso(String mensagem) {
        emitir(AnsiCores.GREEN, mensagem);
    }

    @Override
    public void aviso(String mensagem) {
        emitir(AnsiCores.YELLOW, mensagem);
    }

    @Override
    public void erro(String mensagem) {
        emitir(AnsiCores.RED, mensagem);
    }

    private void emitir(String cor, String mensagem) {
        if (mensagem == null) {
            return;
        }
        System.out.println(cor == null ? mensagem : cor + mensagem + AnsiCores.RESET);
        // Log sem cor: o console recebe o escape, o arquivo recebe texto legível.
        log.info(mensagem);
    }
}
