package org.traducao.projeto.auditorConteudoLegendas.application.regras;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaConteudo;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ApplicationScoped
public class RegraEfeitoVazado implements RegraAuditoriaConteudo {

    private static final Pattern TAG_ANIMACAO_PESADA = Pattern.compile("\\\\(t\\(|pos\\(|clip\\(|move\\(|fad\\()");
    
    @Override
    public String getNome() {
        return "Efeito Visual Vazado para Tradução";
    }

    @Override
    public List<AnomaliaConteudo> auditar(DocumentoLegenda original, DocumentoLegenda traduzido) {
        List<AnomaliaConteudo> anomalias = new ArrayList<>();
        Map<Integer, EventoLegenda> mapOriginal = original.eventos().stream()
                .collect(Collectors.toMap(EventoLegenda::indice, Function.identity(), (a, b) -> a));

        for (EventoLegenda eventoTrad : traduzido.eventos()) {
            if (!eventoTrad.isDialogo() || !eventoTrad.temTexto()) continue;

            EventoLegenda eventoOrig = mapOriginal.get(eventoTrad.indice());
            if (eventoOrig == null || !eventoOrig.temTexto()) continue;

            boolean isOriginalEfeito = TAG_ANIMACAO_PESADA.matcher(eventoOrig.texto()).find();
            
            if (isOriginalEfeito && !eventoOrig.texto().equals(eventoTrad.texto())) {
                String limpoOriginal = limparTags(eventoOrig.texto());
                String limpoTraduzido = limparTags(eventoTrad.texto());
                
                if (limpoOriginal.length() < 15 && limpoTraduzido.length() > limpoOriginal.length() + 10) {
                    anomalias.add(new AnomaliaConteudo(
                            AnomaliaConteudo.TipoSeveridade.CRITICAL,
                            getNome(),
                            "Fragmento de efeito visual ('" + limpoOriginal + "') gerou sentença completa ('" + limpoTraduzido + "').",
                            eventoOrig,
                            eventoTrad,
                            "Reverter para a linha original."
                    ));
                } else {
                    anomalias.add(new AnomaliaConteudo(
                            AnomaliaConteudo.TipoSeveridade.WARNING,
                            getNome(),
                            "Linha com efeitos pesados foi modificada.",
                            eventoOrig,
                            eventoTrad,
                            "Revisar se as tags de efeito não foram corrompidas."
                    ));
                }
            }
        }
        return anomalias;
    }

    private String limparTags(String texto) {
        return texto.replaceAll("\\{[^}]*\\}", "").trim();
    }
}
