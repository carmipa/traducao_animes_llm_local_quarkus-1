package org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaArquivoUnico;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;

import java.util.ArrayList;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: detecta blocos de override ASS ({@code {\...}}) abertos e
 * nunca fechados numa única legenda. Uma chave desbalanceada faz o player exibir
 * as tags como texto ou ignorar a linha inteira — dano estrutural que independe
 * de arquivo de referência.
 *
 * <p>INVARIANTES DO DOMÍNIO: só eventos {@code Dialogue} com texto são avaliados;
 * a contagem considera aninhamento inválido ({@code {} dentro de {}}) como
 * malformação.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo/sem chaves não gera anomalia;
 * cada evento é avaliado isoladamente e nunca lança.
 */
@ApplicationScoped
public class RegraTagOverrideNaoFechada implements RegraAuditoriaArquivoUnico {

    @Override
    public String getNome() {
        return "Bloco de Override ASS Não Fechado";
    }

    @Override
    public List<AnomaliaConteudo> auditar(DocumentoLegenda documento) {
        List<AnomaliaConteudo> anomalias = new ArrayList<>();
        for (EventoLegenda evento : documento.eventos()) {
            if (!evento.isDialogo() || !evento.temTexto()) {
                continue;
            }
            if (chavesDesbalanceadas(evento.texto())) {
                anomalias.add(new AnomaliaConteudo(
                    AnomaliaConteudo.TipoSeveridade.CRITICAL,
                    getNome(),
                    "Bloco de override '{...}' aberto e não fechado corretamente. O player pode exibir as tags como texto ou descartar a linha.",
                    evento,
                    null,
                    "Fechar a chave '}' correspondente ou remover o '{' órfão."
                ));
            }
        }
        return anomalias;
    }

    private boolean chavesDesbalanceadas(String texto) {
        boolean aberto = false;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '{') {
                if (aberto) {
                    return true; // '{' dentro de bloco já aberto
                }
                aberto = true;
            } else if (c == '}') {
                if (!aberto) {
                    return true; // '}' sem abertura
                }
                aberto = false;
            }
        }
        return aberto; // sobrou um '{' sem fechamento
    }
}
