package org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaArquivoUnico;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;

import java.util.ArrayList;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: encontra eventos de diálogo que ficaram sem texto visível
 * (só tags, quebras ou espaços). Numa tradução, isso costuma indicar uma fala
 * perdida; num original, uma linha inútil que polui o tempo de exibição.
 *
 * <p>INVARIANTES DO DOMÍNIO: só eventos {@code Dialogue} são avaliados; o texto
 * visível é o que sobra após remover blocos {@code {...}}, {@code \N}, {@code \h}
 * e espaços.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: eventos que não são diálogo ou sem campo de
 * texto são ignorados; a regra nunca lança.
 */
@ApplicationScoped
public class RegraEventoDialogoVazio implements RegraAuditoriaArquivoUnico {

    @Override
    public String getNome() {
        return "Evento de Diálogo Vazio";
    }

    @Override
    public List<AnomaliaConteudo> auditar(DocumentoLegenda documento) {
        List<AnomaliaConteudo> anomalias = new ArrayList<>();
        for (EventoLegenda evento : documento.eventos()) {
            if (!evento.isDialogo() || !evento.temTexto()) {
                continue;
            }
            if (textoVisivel(evento.texto()).isEmpty()) {
                anomalias.add(new AnomaliaConteudo(
                    AnomaliaConteudo.TipoSeveridade.WARNING,
                    getNome(),
                    "Evento de diálogo sem texto visível (apenas tags, quebras ou espaços).",
                    evento,
                    null,
                    "Preencher a fala ou remover a linha se ela for realmente vazia."
                ));
            }
        }
        return anomalias;
    }

    private String textoVisivel(String texto) {
        return texto.replaceAll("\\{[^}]*\\}", "")
            .replace("\\N", " ")
            .replace("\\n", " ")
            .replace("\\h", " ")
            .strip();
    }
}
