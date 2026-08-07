package org.traducao.projeto.telemetria;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: prepara texto de telemetria para PUBLICAÇÃO, removendo o
 * que identifica a máquina ou a pessoa e preservando o que serve a quem for usar
 * o dataset.
 *
 * <h2>O prejuízo que originou</h2>
 * Medido no acervo em 06/08/2026, sobre 6.601 operações registradas:
 * <ul>
 *   <li><b>4.532 (69%)</b> carregavam caminho absoluto de disco no campo livre;</li>
 *   <li><b>2.843 (43%)</b> carregavam nome de usuário ou pasta pessoal.</li>
 * </ul>
 *
 * <p>A publicação atual escapa disso por OMISSÃO: o campo livre simplesmente não
 * é copiado para o dataset. É seguro e é pobre — junto com
 * {@code C:\animes\...} vai embora o {@code "ASS | faixas: 50, extraidas: 50"},
 * que é exatamente o dado com valor. E das 6.601 operações, só 85 chegaram ao
 * repositório público, porque 6.488 vivem em pastas de relatório que o
 * publicador nunca varreu.
 *
 * <p>Este sanitizador troca omissão por TRANSFORMAÇÃO: o caminho perde a parte
 * que identifica a máquina e mantém a que identifica a OBRA — e o nome da obra,
 * com grupo de release, é justamente o que permite a outra pessoa comparar a
 * medição dela com a nossa.
 *
 * <h2>INVARIANTES DO DOMÍNIO</h2>
 * <ul>
 *   <li><b>Falha fechada com verificação POSTERIOR.</b> Depois de transformar, o
 *       resultado é reconferido; se ainda casar um padrão proibido, o texto é
 *       descartado inteiro em vez de publicado pela metade. Transformação que se
 *       auto-aprova é como guarda que confere o próprio trabalho.</li>
 *   <li>Só sai o que identifica máquina ou pessoa: letra de drive, caminho
 *       absoluto, pasta de usuário, host. <b>Nome de obra, grupo de release,
 *       contagens e tempos ficam</b> — é o valor do dataset.</li>
 *   <li>De um caminho, sobrevivem no máximo os DOIS últimos segmentos. Dois
 *       bastam para "obra/pasta" e não recompõem a árvore de ninguém.</li>
 * </ul>
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: entrada nula devolve nula; entrada que não
 * pôde ser limpa devolve {@link #REDIGIDO}, nunca o original. Nunca lança.
 */
public final class SanitizadorTelemetria {

    /** Marca o que não pôde ser limpo. Visível de propósito: campo sumido em silêncio esconde que havia dado. */
    public static final String REDIGIDO = "[redigido]";

    /**
     * Caminho absoluto de Windows ({@code C:\...}) ou raiz POSIX conhecida. A
     * lista POSIX é fechada: um {@code /} solto casaria "e/ou" e datas dentro de
     * texto comum, e o remédio ficaria pior que a doença.
     */
    private static final Pattern CAMINHO = Pattern.compile(
        "(?:[A-Za-z]:[\\\\/]|/(?:acervo|home|Users|mnt|opt|var|srv|root)/)[^|\\r\\n;,]*");

    /**
     * Marcadores de pasta PESSOAL, conferidos na ENTRADA — antes de qualquer
     * transformação.
     *
     * <p>Aprendido reprovando: conferir só na saída não funciona, porque a
     * própria limpeza apaga a evidência. {@code C:\Users\Paulo\Documents\x.ass}
     * aparado nos dois últimos segmentos vira {@code Documents/x.ass}, que já não
     * contém "Users" e passa por qualquer guarda de saída. O texto tinha de ser
     * recusado <b>pelo que era</b>, não pelo que sobrou dele.
     */
    private static final Pattern PESSOAL_NA_ENTRADA = Pattern.compile(
        "\\bUsers\\b|/home/|\\bDocuments and Settings\\b|\\bAppData\\b",
        Pattern.CASE_INSENSITIVE);

    /**
     * Marcador de disco sobrevivente, conferido na SAÍDA.
     *
     * <p>O trecho inicial que exige caractere não-alfabético antes da letra não é
     * enfeite: sem ele, uma única letra seguida de dois-pontos casa o "s:" de
     * "faixas: 50", e o sanitizador redigiria justamente a medição que ele existe
     * para preservar. O caractere de caminho logo depois pega a forma relativa a
     * drive ({@code D:relatorio.txt}), que não tem barra nenhuma e por isso escapa
     * da regra de caminho.
     *
     * <p>Escrito com grupo consumidor em vez de lookbehind de propósito: a
     * catraca de fronteira do ASS varre o fonte por forma, não por domínio, e
     * cobraria aqui o tratamento da quebra {@code \\N} — que nada tem a ver com
     * caminho de disco. Conformar é mais barato que abrir exceção numa guarda.
     */
    private static final Pattern DISCO_NA_SAIDA =
        Pattern.compile("(?:^|[^A-Za-z])[A-Za-z]:[\\\\/\\w]");

    private SanitizadorTelemetria() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve o texto pronto para publicação.
     *
     * <p>INVARIANTES DO DOMÍNIO: o nome do usuário e o do host desta máquina são
     * removidos ONDE QUER que apareçam, inclusive fora de caminho — aparecem em
     * nome de arquivo e em mensagem de erro, não só em pasta.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto que continue casando um padrão
     * proibido depois de tudo vira {@link #REDIGIDO}.
     */
    public static String sanitizar(String texto) {
        if (texto == null) {
            return null;
        }
        if (texto.isBlank()) {
            return texto;
        }

        // Confere o ORIGINAL primeiro: aparar o caminho apagaria a evidência.
        if (PESSOAL_NA_ENTRADA.matcher(texto).find()) {
            return REDIGIDO;
        }

        StringBuilder saida = new StringBuilder();
        Matcher m = CAMINHO.matcher(texto);
        int fim = 0;
        while (m.find()) {
            saida.append(texto, fim, m.start()).append(caudaSegura(m.group()));
            fim = m.end();
        }
        saida.append(texto.substring(fim));

        String limpo = removerIdentidadeLocal(saida.toString()).trim();

        // Segunda rede: marcador de disco que a transformação não conheceu.
        return DISCO_NA_SAIDA.matcher(limpo).find() ? REDIGIDO : limpo;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: de um caminho, guarda só o suficiente para
     * identificar a OBRA — tipicamente "nome da obra / subpasta".
     *
     * <p>INVARIANTES DO DOMÍNIO: no máximo dois segmentos, e a separação sai
     * normalizada em {@code /} para o dataset não denunciar o sistema
     * operacional de origem em cada linha.
     */
    private static String caudaSegura(String caminho) {
        String[] partes = caminho.replace('\\', '/').split("/");
        StringBuilder cauda = new StringBuilder();
        int inicio = Math.max(0, partes.length - 2);
        for (int i = inicio; i < partes.length; i++) {
            if (partes[i].isBlank()) {
                continue;
            }
            if (cauda.length() > 0) {
                cauda.append('/');
            }
            cauda.append(partes[i]);
        }
        return cauda.toString();
    }

    /**
     * Remove nome de usuário e de host desta máquina onde quer que apareçam.
     * Segmentos muito curtos são ignorados: um usuário chamado "ana" faria a
     * limpeza comer pedaço de qualquer palavra que a contivesse.
     */
    private static String removerIdentidadeLocal(String texto) {
        String saida = texto;
        for (String bruto : new String[]{System.getProperty("user.name"), nomeDoHost()}) {
            if (bruto == null || bruto.length() < 4) {
                continue;
            }
            saida = Pattern.compile(Pattern.quote(bruto), Pattern.CASE_INSENSITIVE)
                .matcher(saida).replaceAll(REDIGIDO);
        }
        return saida;
    }

    private static String nomeDoHost() {
        String host = System.getenv("COMPUTERNAME");
        if (host == null || host.isBlank()) {
            host = System.getenv("HOSTNAME");
        }
        return host == null ? null : host.toLowerCase(Locale.ROOT);
    }
}
