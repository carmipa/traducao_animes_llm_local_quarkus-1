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
import java.util.stream.Collectors;

@ApplicationScoped
public class RegraAlucinacaoQuebraLinha implements RegraAuditoriaConteudo {

    @Override
    public String getNome() {
        return "Alucinação de Quebra de Linha (\\N)";
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

            int quebrasOriginal = contarOcorrencias(eventoOrig.texto(), "\\N");
            int quebrasTraduzido = contarOcorrencias(eventoTrad.texto(), "\\N");

            if (quebrasOriginal <= 1 && quebrasTraduzido >= 3) {
                anomalias.add(new AnomaliaConteudo(
                        AnomaliaConteudo.TipoSeveridade.CRITICAL,
                        getNome(),
                        "A tradução inseriu " + quebrasTraduzido + " quebras de linha onde o original tinha " + quebrasOriginal + ". Isso destrói o posicionamento (\\pos).",
                        eventoOrig,
                        eventoTrad,
                        "Remover quebras de linha ou reverter para a linha original se for karaokê."
                ));
            } else if (quebrasTraduzido > quebrasOriginal + 1) {
                anomalias.add(new AnomaliaConteudo(
                        AnomaliaConteudo.TipoSeveridade.WARNING,
                        getNome(),
                        "Aumento significativo no número de quebras de linha.",
                        eventoOrig,
                        eventoTrad,
                        "Verifique se o texto traduzido não ficou mal formatado."
                ));
            }
        }
        return anomalias;
    }

    private int contarOcorrencias(String texto, String padrao) {
        int count = 0;
        int idx = 0;
        while ((idx = texto.indexOf(padrao, idx)) != -1) {
            count++;
            idx += padrao.length();
        }
        return count;
    }
}
