package org.traducao.projeto.auditorConteudoLegendas.application.regras;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaConteudo;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * PROPÓSITO DE NEGÓCIO: garante que o par original ↔ traduzido descreve o MESMO
 * conjunto de falas antes de qualquer regra confiar no pareamento por índice.
 * Sem ela, uma fala apagada, uma fala inventada ou um deslocamento por
 * Comentário passavam despercebidos e o arquivo era declarado "limpo".
 *
 * <p>INVARIANTES DO DOMÍNIO: detecta divergência de contagem de diálogos, índices
 * de diálogo ausentes no traduzido, índices extras no traduzido, índices
 * duplicados (pareamento ambíguo) e mudança de tipo (Dialogue↔Comment) no mesmo
 * índice. Qualquer uma dessas anomalias impede o resultado "limpo".
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: opera só em memória; documentos válidos e
 * equivalentes não geram anomalia. Só é executada entre formatos comparáveis
 * (o caso de uso bloqueia ASS↔SRT antes de chegar aqui).
 */
@ApplicationScoped
public class RegraIntegridadePareamento implements RegraAuditoriaConteudo {

    private static final int LIMITE_LISTA = 15;

    @Override
    public String getNome() {
        return "Integridade do Pareamento (falas ausentes/extras/deslocadas)";
    }

    @Override
    public List<AnomaliaConteudo> auditar(DocumentoLegenda original, DocumentoLegenda traduzido) {
        List<AnomaliaConteudo> anomalias = new ArrayList<>();

        List<Integer> dupOriginal = indicesDuplicados(original);
        if (!dupOriginal.isEmpty()) {
            anomalias.add(new AnomaliaConteudo(
                AnomaliaConteudo.TipoSeveridade.CRITICAL, getNome(),
                "Índices de diálogo duplicados no original (pareamento ambíguo): " + amostra(dupOriginal),
                null, null,
                "Renumerar os eventos: cada fala precisa de um índice único."));
        }
        List<Integer> dupTraduzido = indicesDuplicados(traduzido);
        if (!dupTraduzido.isEmpty()) {
            anomalias.add(new AnomaliaConteudo(
                AnomaliaConteudo.TipoSeveridade.CRITICAL, getNome(),
                "Índices de diálogo duplicados no traduzido (pareamento ambíguo): " + amostra(dupTraduzido),
                null, null,
                "Renumerar os eventos: cada fala precisa de um índice único."));
        }

        Map<Integer, EventoLegenda> dialogosOriginal = dialogosPorIndice(original);
        Map<Integer, EventoLegenda> dialogosTraduzido = dialogosPorIndice(traduzido);
        int totalOriginal = contarDialogos(original);
        int totalTraduzido = contarDialogos(traduzido);

        if (totalOriginal != totalTraduzido) {
            anomalias.add(new AnomaliaConteudo(
                AnomaliaConteudo.TipoSeveridade.CRITICAL, getNome(),
                "Quantidade de diálogos difere — original: " + totalOriginal
                    + ", traduzido: " + totalTraduzido + ". Há fala(s) perdida(s) ou inventada(s).",
                null, null,
                "Reextrair/re-traduzir garantindo correspondência 1:1 das falas."));
        }

        List<Integer> ausentes = diferenca(dialogosOriginal.keySet(), dialogosTraduzido.keySet());
        if (!ausentes.isEmpty()) {
            EventoLegenda primeiro = dialogosOriginal.get(ausentes.get(0));
            anomalias.add(new AnomaliaConteudo(
                AnomaliaConteudo.TipoSeveridade.CRITICAL, getNome(),
                ausentes.size() + " fala(s) do original sem correspondente no traduzido — índice(s): "
                    + amostra(ausentes),
                primeiro, null,
                "A fala existe no original e sumiu no traduzido; restaure-a."));
        }

        List<Integer> extras = diferenca(dialogosTraduzido.keySet(), dialogosOriginal.keySet());
        if (!extras.isEmpty()) {
            EventoLegenda primeiro = dialogosTraduzido.get(extras.get(0));
            anomalias.add(new AnomaliaConteudo(
                AnomaliaConteudo.TipoSeveridade.ERROR, getNome(),
                extras.size() + " fala(s) presente(s) só no traduzido — índice(s): " + amostra(extras),
                null, primeiro,
                "Fala inexistente no original; verifique se foi inventada ou deslocada."));
        }

        anomalias.addAll(mudancasDeTipo(original, traduzido));
        return anomalias;
    }

    private List<AnomaliaConteudo> mudancasDeTipo(DocumentoLegenda original, DocumentoLegenda traduzido) {
        Map<Integer, EventoLegenda> todosOriginal = todosPorIndice(original);
        Map<Integer, EventoLegenda> todosTraduzido = todosPorIndice(traduzido);
        Set<Integer> indices = new TreeSet<>(todosOriginal.keySet());
        indices.retainAll(todosTraduzido.keySet());

        List<AnomaliaConteudo> anomalias = new ArrayList<>();
        int reportadas = 0;
        for (Integer indice : indices) {
            EventoLegenda eo = todosOriginal.get(indice);
            EventoLegenda et = todosTraduzido.get(indice);
            String tipoO = eo.tipoLinha() == null ? "" : eo.tipoLinha();
            String tipoT = et.tipoLinha() == null ? "" : et.tipoLinha();
            if (tipoO.equals(tipoT)) {
                continue;
            }
            // Só interessa quando pelo menos um lado é Dialogue (troca Dialogue↔Comment
            // desloca o pareamento das falas seguintes).
            if (!eo.isDialogo() && !et.isDialogo()) {
                continue;
            }
            if (reportadas++ >= LIMITE_LISTA) {
                continue;
            }
            anomalias.add(new AnomaliaConteudo(
                AnomaliaConteudo.TipoSeveridade.WARNING, getNome(),
                "Índice " + indice + " muda de tipo — original: " + rotulo(tipoO)
                    + ", traduzido: " + rotulo(tipoT) + " (deslocamento de pareamento).",
                eo, et,
                "Um Comentário inserido em um dos lados desloca as falas seguintes; alinhe os eventos."));
        }
        return anomalias;
    }

    private Map<Integer, EventoLegenda> dialogosPorIndice(DocumentoLegenda doc) {
        Map<Integer, EventoLegenda> mapa = new LinkedHashMap<>();
        for (EventoLegenda evento : doc.eventos()) {
            if (evento.isDialogo()) {
                mapa.putIfAbsent(evento.indice(), evento);
            }
        }
        return mapa;
    }

    private Map<Integer, EventoLegenda> todosPorIndice(DocumentoLegenda doc) {
        Map<Integer, EventoLegenda> mapa = new LinkedHashMap<>();
        for (EventoLegenda evento : doc.eventos()) {
            mapa.putIfAbsent(evento.indice(), evento);
        }
        return mapa;
    }

    private int contarDialogos(DocumentoLegenda doc) {
        int total = 0;
        for (EventoLegenda evento : doc.eventos()) {
            if (evento.isDialogo()) {
                total++;
            }
        }
        return total;
    }

    private List<Integer> indicesDuplicados(DocumentoLegenda doc) {
        Set<Integer> vistos = new LinkedHashSet<>();
        Set<Integer> duplicados = new LinkedHashSet<>();
        for (EventoLegenda evento : doc.eventos()) {
            if (evento.isDialogo() && !vistos.add(evento.indice())) {
                duplicados.add(evento.indice());
            }
        }
        return new ArrayList<>(duplicados);
    }

    private List<Integer> diferenca(Set<Integer> a, Set<Integer> b) {
        Set<Integer> resultado = new TreeSet<>(a);
        resultado.removeAll(b);
        return new ArrayList<>(resultado);
    }

    private String amostra(List<Integer> indices) {
        if (indices.size() <= LIMITE_LISTA) {
            return indices.toString();
        }
        return indices.subList(0, LIMITE_LISTA) + " ... (+" + (indices.size() - LIMITE_LISTA) + ")";
    }

    private String rotulo(String tipo) {
        return tipo == null || tipo.isBlank() ? "linha malformada/auxiliar" : tipo;
    }
}
