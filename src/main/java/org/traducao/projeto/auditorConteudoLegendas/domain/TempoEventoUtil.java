package org.traducao.projeto.auditorConteudoLegendas.domain;

import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PadraoEstiloMusical;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: interpreta e DIAGNOSTICA os instantes de início e fim de
 * um evento de legenda, para que a auditoria distinga um timestamp válido de um
 * corrompido em vez de simplesmente ignorá-lo.
 *
 * <p>INVARIANTES DO DOMÍNIO: o tempo é lido do campo {@code prefixo} preservado
 * pelos leitores — ASS guarda {@code Dialogue: Layer,Início,Fim,...} e SRT guarda
 * a linha {@code hh:mm:ss,mmm --> hh:mm:ss,mmm}. Valores são milissegundos desde
 * 0; minutos e segundos válidos ficam em 0–59.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; um prefixo ilegível, incompleto
 * ou fora do intervalo é reportado com o {@link StatusTempo} correspondente.
 */
public final class TempoEventoUtil {

    // hh:mm:ss seguido de separador decimal (',' no SRT, '.' no ASS) + frações.
    private static final Pattern TEMPO = Pattern.compile("(\\d{1,2}):(\\d{1,2}):(\\d{1,2})[.,](\\d{1,3})");

    public enum StatusTempo {
        OK, AUSENTE, ILEGIVEL, FORA_INTERVALO, SETA_SRT_INVALIDA, INCOMPLETO, FIM_ANTES_INICIO
    }

    public record Diagnostico(StatusTempo status, long inicioMs, long fimMs) {
        public boolean ok() {
            return status == StatusTempo.OK;
        }
    }

    private enum StatusInstante { OK, ILEGIVEL, FORA_INTERVALO }

    private record Instante(StatusInstante status, long ms) {}

    private TempoEventoUtil() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve {início, fim} em ms do evento, ou {@code null}
     * quando o tempo não é válido — usado por regras que só operam sobre tempos
     * confiáveis (ex.: sobreposição).
     * <p>INVARIANTES DO DOMÍNIO: equivale a {@link #diagnosticar(EventoLegenda)}
     * bem-sucedido.
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorna {@code null} para qualquer status
     * diferente de OK.
     */
    public static long[] extrairInicioFimMs(EventoLegenda evento) {
        Diagnostico d = diagnosticar(evento);
        return d.ok() ? new long[]{d.inicioMs(), d.fimMs()} : null;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: classifica o tempo do evento em um {@link StatusTempo}
     * para que a auditoria transforme cada tipo de corrupção em anomalia própria.
     * <p>INVARIANTES DO DOMÍNIO: ASS é reconhecido pelo prefixo {@code Dialogue:}/
     * {@code Comment:}; qualquer outro prefixo é tratado como linha de tempo SRT.
     * <p>COMPORTAMENTO EM CASO DE FALHA: prefixo nulo/vazio → {@code AUSENTE};
     * nunca lança.
     */
    public static Diagnostico diagnosticar(EventoLegenda evento) {
        if (evento == null || evento.prefixo() == null || evento.prefixo().isBlank()) {
            return new Diagnostico(StatusTempo.AUSENTE, 0, 0);
        }
        String prefixo = evento.prefixo();
        boolean ass = prefixo.startsWith("Dialogue:") || prefixo.startsWith("Comment:");

        if (ass) {
            int idxColon = prefixo.indexOf(':');
            String corpo = prefixo.substring(idxColon + 1);
            String[] campos = corpo.split(",");
            if (campos.length < 3) {
                return new Diagnostico(StatusTempo.INCOMPLETO, 0, 0);
            }
            return combinar(parsearInstante(campos[1]), parsearInstante(campos[2]));
        }

        // Linha de tempo SRT: exige a seta '-->' e dois instantes válidos.
        if (!prefixo.contains("-->")) {
            return new Diagnostico(StatusTempo.SETA_SRT_INVALIDA, 0, 0);
        }
        String[] lados = prefixo.split("-->", -1);
        if (lados.length != 2 || lados[0].isBlank() || lados[1].isBlank()) {
            return new Diagnostico(StatusTempo.SETA_SRT_INVALIDA, 0, 0);
        }
        return combinar(parsearInstante(lados[0]), parsearInstante(lados[1]));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reconhece o marcador de capítulo que fansub grava como
     * {@code Dialogue} — {@code {OP Start}}, {@code {Episode}}, {@code {ED}}, às vezes com
     * Name {@code chptr} —, de duração zero de propósito: não é fala, é índice de capítulo
     * para o player. Até 08/10/2026 cada um virava ERROR "Timestamp Inválido" e WARNING
     * "Diálogo Vazio", em todo release real (5 por arquivo no 0080 E02), e "limpo" ficava
     * inalcançável (auditoria de 08/10/2026, A8).
     * <p>Medido no acervo em 08/10/2026 (738 .ass, 2.124.707 falas, 32.296 de duração zero):
     * o predicado aceita 4.512 — 72 marcadores e notas do fansub ({@code {ED}}, {@code {OP
     * Start}}, {@code {name check}}) e 4.440 quadros de efeito só de tags; todos invisíveis
     * por construção. Recusa as 27.726 com texto visível: 27.720 sílabas de KFX (todas em
     * estilo de música: 26.742 no DanMachi e 978 no 86) — essas são do
     * {@link #ehQuadroMusicalDeDuracaoZero} — e 6 falas comuns de duração zero, que são o
     * defeito que a regra existe para pegar.
     * <p>INVARIANTES DO DOMÍNIO: exige os DOIS sinais juntos — início igual ao fim (nem
     * negativo, nem ilegível) E texto feito só de blocos {@code {...}}, sem nada visível.
     * Fala com texto visível e duração zero, ou linha vazia com duração, continuam defeito.
     * <p>COMPORTAMENTO EM CASO DE FALHA: evento nulo, sem texto ou com tempo ilegível não é
     * marcador; nunca lança.
     */
    public static boolean ehMarcadorSemFala(EventoLegenda evento) {
        if (evento == null || evento.texto() == null || evento.texto().isBlank()) {
            return false;
        }
        Diagnostico d = diagnosticar(evento);
        boolean duracaoZero = d.status() == StatusTempo.FIM_ANTES_INICIO && d.inicioMs() == d.fimMs();
        if (!duracaoZero) {
            return false;
        }
        // Texto não vazio que some inteiro ao tirar os blocos {...}: só havia blocos.
        return evento.texto().replaceAll("\\{[^}]*}", "").isBlank();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reconhece o quadro de efeito de karaokê (KFX) que o template do
     * fansub grava com duração zero — sílaba que aparece e some na animação, invisível de
     * propósito. Cada um virava ERROR "Timestamp Inválido": 978 no 86 e 26.742 no DanMachi, e
     * a abertura de cada episódio tornava "limpo" inalcançável (auditoria de 08/10/2026, A8,
     * medido no acervo).
     * <p>INVARIANTES DO DOMÍNIO: duração exatamente zero E estilo que declara música, perguntado
     * ao dono único ({@link PadraoEstiloMusical#nomeDeclaraMusica}). Medido: as 27.720 linhas de
     * duração zero com texto em estilo de música são todas KFX; as 6 de duração zero fora de
     * música são fala comum e continuam acusadas. Risco declarado: letra de música cujo tempo
     * se corrompesse para duração zero exata deixaria de ser acusada (0 no acervo).
     * <p>COMPORTAMENTO EM CASO DE FALHA: evento nulo ou tempo ilegível devolve {@code false};
     * nunca lança.
     */
    public static boolean ehQuadroMusicalDeDuracaoZero(EventoLegenda evento) {
        if (evento == null || !PadraoEstiloMusical.nomeDeclaraMusica(evento.estilo())) {
            return false;
        }
        Diagnostico d = diagnosticar(evento);
        return d.status() == StatusTempo.FIM_ANTES_INICIO && d.inicioMs() == d.fimMs();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reconhece a linha sem fala que NÃO é fala perdida, qualquer que seja
     * a duração: a nota do fansub feita só de comentário ({@code {Intro}}, {@code {OP}}, sem
     * nenhuma tag de override) e o portador de efeito de karaokê (só tags, em estilo de música —
     * o brilho, o borrão, a moldura da sílaba). Medido no acervo em 08/10/2026: 41.146 linhas
     * assim viravam WARNING "Diálogo Vazio" — 12.188 só na abertura do 86 —, afogando o
     * relatório (auditoria de 08/10/2026, A8).
     * <p>INVARIANTES DO DOMÍNIO: texto não vazio feito só de blocos {@code {...}}; e (nenhum bloco
     * com barra invertida) ou (estilo que declara música, perguntado ao dono único). Fala de
     * diálogo que perdeu o texto e ficou só com tags continua acusada. A letra que perde o
     * texto na tradução é pega pela comparação (RegraDanoKaraoke) no modo Ambas; no modo de
     * arquivo único ela fica indistinguível do portador — limite declarado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: evento ou texto nulo devolve {@code false}; nunca lança.
     */
    public static boolean ehLinhaSemFalaDeProposito(EventoLegenda evento) {
        if (evento == null || evento.texto() == null || evento.texto().isBlank()) {
            return false;
        }
        String texto = evento.texto();
        if (!texto.replaceAll("\\{[^}]*}", "").isBlank()) {
            return false;
        }
        boolean soComentario = !texto.contains("\\");
        return soComentario || PadraoEstiloMusical.nomeDeclaraMusica(evento.estilo());
    }

    private static Diagnostico combinar(Instante inicio, Instante fim) {
        if (inicio.status() == StatusInstante.ILEGIVEL || fim.status() == StatusInstante.ILEGIVEL) {
            return new Diagnostico(StatusTempo.ILEGIVEL, 0, 0);
        }
        if (inicio.status() == StatusInstante.FORA_INTERVALO || fim.status() == StatusInstante.FORA_INTERVALO) {
            return new Diagnostico(StatusTempo.FORA_INTERVALO, inicio.ms(), fim.ms());
        }
        if (fim.ms() <= inicio.ms()) {
            return new Diagnostico(StatusTempo.FIM_ANTES_INICIO, inicio.ms(), fim.ms());
        }
        return new Diagnostico(StatusTempo.OK, inicio.ms(), fim.ms());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte um carimbo hh:mm:ss(.|,)frac em ms, validando
     * a sintaxe e o intervalo de minutos/segundos.
     * <p>INVARIANTES DO DOMÍNIO: minutos e segundos válidos ficam em 0–59; frações
     * de 2 dígitos são centésimos (ASS), de 3 são milésimos (SRT).
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem carimbo → ILEGIVEL; mm/ss ≥ 60 →
     * FORA_INTERVALO.
     */
    private static Instante parsearInstante(String trecho) {
        if (trecho == null) {
            return new Instante(StatusInstante.ILEGIVEL, 0);
        }
        Matcher m = TEMPO.matcher(trecho);
        if (!m.find()) {
            return new Instante(StatusInstante.ILEGIVEL, 0);
        }
        long horas = Long.parseLong(m.group(1));
        long minutos = Long.parseLong(m.group(2));
        long segundos = Long.parseLong(m.group(3));
        if (minutos >= 60 || segundos >= 60) {
            return new Instante(StatusInstante.FORA_INTERVALO, 0);
        }
        String fracao = m.group(4);
        long fracaoMs = fracao.length() == 2
            ? Long.parseLong(fracao) * 10L                                   // centésimos (ASS)
            : Long.parseLong(fracao.length() == 1 ? fracao + "00" : fracao); // milésimos (SRT)
        long ms = ((horas * 60 + minutos) * 60 + segundos) * 1000L + fracaoMs;
        return new Instante(StatusInstante.OK, ms);
    }
}
