package org.traducao.projeto.auditorConteudoLegendas.application.regras;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.TempoEventoUtil;
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
 * <h2>Onde o par se desfaz — e não onde ele termina</h2>
 * O índice do evento é a POSIÇÃO no arquivo. Até 08/10/2026 a fala ausente era
 * achada por diferença de posições, e uma fala apagada no meio desloca todas as
 * seguintes: o relatório apontava a ÚLTIMA linha do episódio (índice 635 no
 * 0080 E02, cuja 3ª fala tinha sido removida) e a anomalia saía só com o lado
 * original, que a tela classifica como "da fonte" (auditoria de 08/10/2026,
 * A2/A3, reproduzido). Agora o par é alinhado na ORDEM, pelo tempo de cada
 * evento — a tradução não mexe no tempo —, e a anomalia aponta a primeira
 * posição em que os dois lados deixam de coincidir, levando os dois eventos.
 *
 * <p>INVARIANTES DO DOMÍNIO: detecta divergência de contagem de diálogos (com o
 * ponto em que o par se desfaz), índices de diálogo duplicados (pareamento
 * ambíguo) e — só quando os dois arquivos têm o mesmo número de eventos, caso em
 * que a posição é comparável — falas ausentes, extras e mudança de tipo
 * (Dialogue↔Comment). Com número de eventos diferente e falas em igual número,
 * acusa a linha auxiliar a mais/a menos que desloca o pareamento. Qualquer uma
 * dessas anomalias impede o resultado "limpo". Toda anomalia leva os dois
 * eventos quando os dois existem.
 *
 * <p>Limite declarado: duas falas com o mesmo instante são indistinguíveis pelo
 * tempo; se a fala removida tem uma vizinha de tempo idêntico, o ponto apontado
 * é a primeira posição em que o tempo diverge, logo depois dela.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: opera só em memória; documentos válidos e
 * equivalentes não geram anomalia; tempo ilegível compara como tempo ilegível
 * (não lança). Só é executada entre formatos comparáveis (o caso de uso bloqueia
 * ASS↔SRT antes de chegar aqui).
 */
@ApplicationScoped
public class RegraIntegridadePareamento implements RegraAuditoriaConteudo {

    private static final int LIMITE_LISTA = 15;
    private static final int LIMITE_TEXTO = 50;

    @Override
    public String getNome() {
        return "Integridade do Pareamento (falas ausentes/extras/deslocadas)";
    }

    /** É esta regra que mede o pareamento: ela roda justamente quando ele não é confiável. */
    @Override
    public boolean dependeDoPareamentoPorPosicao() {
        return false;
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

        List<EventoLegenda> falasOriginal = dialogos(original);
        List<EventoLegenda> falasTraduzido = dialogos(traduzido);
        boolean mesmaEstrutura = original.eventos().size() == traduzido.eventos().size();

        if (falasOriginal.size() != falasTraduzido.size()) {
            anomalias.add(contagemDeFalasDifere(falasOriginal, falasTraduzido));
        }

        if (mesmaEstrutura) {
            anomalias.addAll(divergenciasPorPosicao(original, traduzido));
        } else if (falasOriginal.size() == falasTraduzido.size()) {
            anomalias.add(linhaAuxiliarDesloca(original.eventos(), traduzido.eventos()));
        }
        return anomalias;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: diz quantas falas faltam ou sobram e ONDE o par se desfaz,
     * mostrando as duas falas daquela posição — é o que o operador precisa para achar a
     * fala apagada sem abrir os dois arquivos.
     * <p>INVARIANTES DO DOMÍNIO: alinha as falas na ordem, pelo tempo; leva os dois eventos.
     * Quando um lado acaba antes, leva a última fala desse lado (onde ele termina).
     * <p>COMPORTAMENTO EM CASO DE FALHA: listas vazias produzem a posição 1 sem evento.
     */
    private AnomaliaConteudo contagemDeFalasDifere(List<EventoLegenda> falasOriginal,
                                                   List<EventoLegenda> falasTraduzido) {
        int k = primeiraDivergencia(falasOriginal, falasTraduzido);
        EventoLegenda eo = k < falasOriginal.size() ? falasOriginal.get(k) : null;
        EventoLegenda et = k < falasTraduzido.size() ? falasTraduzido.get(k) : null;
        String onde = "O par se desfaz na fala nº " + (k + 1) + " — "
            + lado("original", eo, falasOriginal) + " × " + lado("traduzido", et, falasTraduzido) + ".";
        return new AnomaliaConteudo(
            AnomaliaConteudo.TipoSeveridade.CRITICAL, getNome(),
            "Quantidade de diálogos difere — original: " + falasOriginal.size()
                + ", traduzido: " + falasTraduzido.size() + ". Há fala(s) perdida(s) ou inventada(s). " + onde,
            eo != null ? eo : ultimo(falasOriginal),
            et != null ? et : ultimo(falasTraduzido),
            "Restaure (ou remova) a fala nesse ponto; daí em diante o pareamento por posição está deslocado.");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: acusa o Comentário (ou linha auxiliar) que existe só de um lado —
     * as falas batem, mas a posição de cada uma muda e toda regra que compara por posição
     * passa a comparar falas diferentes.
     * <p>INVARIANTES DO DOMÍNIO: alinha todos os eventos pelo tipo e tempo; leva os dois
     * eventos da posição em que o par se desfaz.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    private AnomaliaConteudo linhaAuxiliarDesloca(List<EventoLegenda> todosOriginal,
                                                  List<EventoLegenda> todosTraduzido) {
        int k = primeiraDivergencia(todosOriginal, todosTraduzido);
        EventoLegenda eo = k < todosOriginal.size() ? todosOriginal.get(k) : ultimo(todosOriginal);
        EventoLegenda et = k < todosTraduzido.size() ? todosTraduzido.get(k) : ultimo(todosTraduzido);
        return new AnomaliaConteudo(
            AnomaliaConteudo.TipoSeveridade.ERROR, getNome(),
            "Quantidade de eventos difere — original: " + todosOriginal.size() + ", traduzido: "
                + todosTraduzido.size() + " — com as mesmas falas: há Comentário ou linha auxiliar só de um lado. "
                + "A partir do evento nº " + (k + 1) + " a posição de cada fala muda — "
                + lado("original", k < todosOriginal.size() ? eo : null, todosOriginal) + " × "
                + lado("traduzido", k < todosTraduzido.size() ? et : null, todosTraduzido) + ".",
            eo, et,
            "Alinhe os eventos (mesmo Comentário nos dois lados) para as regras fala a fala valerem.");
    }

    private List<AnomaliaConteudo> divergenciasPorPosicao(DocumentoLegenda original, DocumentoLegenda traduzido) {
        List<AnomaliaConteudo> anomalias = new ArrayList<>();
        Map<Integer, EventoLegenda> dialogosOriginal = dialogosPorIndice(original);
        Map<Integer, EventoLegenda> dialogosTraduzido = dialogosPorIndice(traduzido);
        Map<Integer, EventoLegenda> todosOriginal = todosPorIndice(original);
        Map<Integer, EventoLegenda> todosTraduzido = todosPorIndice(traduzido);

        List<Integer> ausentes = diferenca(dialogosOriginal.keySet(), dialogosTraduzido.keySet());
        if (!ausentes.isEmpty()) {
            Integer indice = ausentes.get(0);
            anomalias.add(new AnomaliaConteudo(
                AnomaliaConteudo.TipoSeveridade.CRITICAL, getNome(),
                ausentes.size() + " fala(s) do original sem correspondente no traduzido — índice(s): "
                    + amostra(ausentes),
                dialogosOriginal.get(indice), todosTraduzido.get(indice),
                "A fala existe no original e sumiu no traduzido; restaure-a."));
        }

        List<Integer> extras = diferenca(dialogosTraduzido.keySet(), dialogosOriginal.keySet());
        if (!extras.isEmpty()) {
            Integer indice = extras.get(0);
            anomalias.add(new AnomaliaConteudo(
                AnomaliaConteudo.TipoSeveridade.ERROR, getNome(),
                extras.size() + " fala(s) presente(s) só no traduzido — índice(s): " + amostra(extras),
                todosOriginal.get(indice), dialogosTraduzido.get(indice),
                "Fala inexistente no original; verifique se foi inventada ou deslocada."));
        }

        anomalias.addAll(mudancasDeTipo(todosOriginal, todosTraduzido));
        return anomalias;
    }

    private List<AnomaliaConteudo> mudancasDeTipo(Map<Integer, EventoLegenda> todosOriginal,
                                                  Map<Integer, EventoLegenda> todosTraduzido) {
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

    /**
     * PROPÓSITO DE NEGÓCIO: a primeira posição em que as duas sequências deixam de ser o
     * mesmo evento, julgado pelo que a tradução não altera — tipo de linha e tempo.
     * <p>INVARIANTES DO DOMÍNIO: sequências iguais até o fim do lado menor devolvem o
     * tamanho do lado menor (o par se desfaz onde um lado acaba).
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; tempo ilegível vira parte da chave.
     */
    static int primeiraDivergencia(List<EventoLegenda> a, List<EventoLegenda> b) {
        int limite = Math.min(a.size(), b.size());
        for (int i = 0; i < limite; i++) {
            if (!chaveDeAlinhamento(a.get(i)).equals(chaveDeAlinhamento(b.get(i)))) {
                return i;
            }
        }
        return limite;
    }

    private static String chaveDeAlinhamento(EventoLegenda e) {
        TempoEventoUtil.Diagnostico d = TempoEventoUtil.diagnosticar(e);
        String tipo = e.tipoLinha() == null ? "" : e.tipoLinha();
        return tipo + "|" + d.status() + "|" + d.inicioMs() + "|" + d.fimMs();
    }

    private static String lado(String rotulo, EventoLegenda evento, List<EventoLegenda> sequencia) {
        if (evento == null) {
            return rotulo + " já terminou" + (sequencia.isEmpty() ? " (vazio)" : " (última: " + instante(ultimo(sequencia)) + ")");
        }
        return rotulo + " " + instante(evento) + " \"" + textoCurto(evento) + "\"";
    }

    private static String instante(EventoLegenda e) {
        TempoEventoUtil.Diagnostico d = TempoEventoUtil.diagnosticar(e);
        if (d.status() != TempoEventoUtil.StatusTempo.OK && d.status() != TempoEventoUtil.StatusTempo.FIM_ANTES_INICIO) {
            return "[tempo ilegível]";
        }
        long ms = d.inicioMs();
        long horas = ms / 3_600_000;
        long minutos = (ms / 60_000) % 60;
        long segundos = (ms / 1000) % 60;
        long centesimos = (ms % 1000) / 10;
        return String.format("[%d:%02d:%02d.%02d]", horas, minutos, segundos, centesimos);
    }

    private static String textoCurto(EventoLegenda e) {
        String visivel = e.texto() == null ? "" : e.texto()
            .replaceAll("\\{[^}]*}", "")
            .replace("\\N", " ")
            .replace("\\n", " ")
            .strip();
        return visivel.length() <= LIMITE_TEXTO ? visivel : visivel.substring(0, LIMITE_TEXTO) + "…";
    }

    private static EventoLegenda ultimo(List<EventoLegenda> lista) {
        return lista.isEmpty() ? null : lista.get(lista.size() - 1);
    }

    private List<EventoLegenda> dialogos(DocumentoLegenda doc) {
        List<EventoLegenda> falas = new ArrayList<>();
        for (EventoLegenda evento : doc.eventos()) {
            if (evento.isDialogo()) {
                falas.add(evento);
            }
        }
        return falas;
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
