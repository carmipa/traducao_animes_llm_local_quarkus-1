package org.traducao.projeto.core.io;

import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: recusa, na porta de entrada, o trabalho que não tem como
 * dar certo — pasta que não existe, texto que não forma caminho, arquivo passado
 * onde se espera diretório. É o que impede a interface responder "iniciado" para
 * uma operação impossível e deixar a pessoa esperando um resultado que nunca vem.
 *
 * <h2>O prejuízo que originou a guarda</h2>
 * Medido em 11/08/2026, sondando as bordas com uma pasta que não existe em
 * ambiente nenhum. Das seis rotas testadas, <b>cinco responderam HTTP 200/202
 * "iniciada"</b>: revisão de lore, revisão de concordância, correção de legendas
 * e as duas de karaokê. Só o renomeador recusou com 400, e só porque é síncrono.
 *
 * <p>A correção de legendas foi até o fim: criou
 * {@code relatorios/PASTA-INEXISTENTE.../}, gravou o relatório JSON e registrou
 * na telemetria canônica {@code {"arquivosProcessados": 1, "itensCorrigidos": 0}}
 * — para uma pasta inexistente. Ali "0 corrigidos porque a pasta não existe"
 * virou idêntico a "0 corrigidos porque estava tudo certo", e o
 * {@code ConsolidadorTelemetriaPorFatia} lê esse arquivo.
 *
 * <p>Não é defeito de contêiner: um caminho digitado errado no Windows produz
 * exatamente o mesmo silêncio.
 *
 * <h2>INVARIANTES DO DOMÍNIO</h2>
 * <ul>
 *   <li><b>Falha fechada.</b> Caminho em branco, inválido ou inexistente é
 *       recusa, nunca "seguir e ver no que dá".</li>
 *   <li><b>Recusa é ANTES do enfileiramento.</b> Depois que a operação entra na
 *       fila, a resposta HTTP já saiu e o único canal que resta é o log — que
 *       ninguém está lendo no instante do clique.</li>
 *   <li><b>Recusa ORIENTA, não adivinha.</b> Caminho do Windows recebido num
 *       sistema que não é Windows devolve o equivalente sob a raiz montada
 *       <i>como sugestão de texto</i>. Converter em silêncio seria pior que
 *       recusar: um palpite errado aponta para conteúdo errado, e aí o dano é
 *       gravado no acervo em vez de barrado na porta.</li>
 *   <li><b>Nada é lido nem escrito aqui.</b> A guarda só decide.</li>
 * </ul>
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@link Optional} com a
 * {@link Recusa} e o {@link Motivo}; nunca lança, nunca devolve {@code null}.
 * {@link Optional#empty()} significa — e só significa — que o caminho existe e é
 * um diretório neste sistema de arquivos, agora.
 */
@Component
public class GuardaCaminhoEntrada {

    /**
     * Onde o acervo é montado dentro do contêiner. Usado apenas para SUGERIR o
     * caminho equivalente na mensagem de recusa; nada é convertido por conta
     * própria.
     */
    private static final String RAIZ_ACERVO_CONTAINER = "/acervo";

    /** {@code C:\...}, {@code d:/...} — letra de unidade seguida de barra. */
    private static final Pattern CAMINHO_WINDOWS = Pattern.compile("^[A-Za-z]:[\\\\/].*");

    /** Por que o caminho foi recusado. Cada valor tem uma orientação diferente. */
    public enum Motivo {
        /** Campo vazio ou só espaços. */
        NAO_INFORMADO,
        /** Texto que sequer forma um caminho para este sistema de arquivos. */
        CAMINHO_INVALIDO,
        /** O caminho não existe aqui. */
        NAO_ENCONTRADO,
        /** Existe, mas é arquivo onde se esperava pasta. */
        NAO_E_DIRETORIO
    }

    /** Recusa com motivo e mensagem pronta para a tela. */
    public record Recusa(Motivo motivo, String mensagem) {}

    /**
     * PROPÓSITO DE NEGÓCIO: confere um único campo de pasta vindo da interface.
     *
     * <p>INVARIANTES DO DOMÍNIO: o rótulo entra na mensagem para a pessoa saber
     * QUAL dos campos da tela está errado — "a pasta não existe" numa tela com
     * dois campos não diz nada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@link Optional} com a recusa. Vazio
     * significa que o diretório existe agora.
     *
     * @param rotulo  como o campo se chama na tela ("Pasta traduzida (PT-BR)")
     * @param caminho o texto exatamente como veio da interface
     */
    public Optional<Recusa> conferirDiretorio(String rotulo, String caminho) {
        if (caminho == null || caminho.isBlank()) {
            return Optional.of(new Recusa(Motivo.NAO_INFORMADO,
                rotulo + " não foi informada."));
        }

        String limpo = caminho.trim();
        Path alvo;
        try {
            alvo = Path.of(limpo);
        } catch (InvalidPathException e) {
            return Optional.of(new Recusa(Motivo.CAMINHO_INVALIDO,
                rotulo + ": o texto informado não forma um caminho válido neste sistema (" + limpo + ")."));
        }

        if (Files.isDirectory(alvo)) {
            return Optional.empty();
        }
        if (Files.exists(alvo)) {
            return Optional.of(new Recusa(Motivo.NAO_E_DIRETORIO,
                rotulo + ": " + limpo + " existe, mas é um arquivo. Informe a PASTA que o contém."));
        }
        return Optional.of(new Recusa(Motivo.NAO_ENCONTRADO,
            rotulo + ": a pasta " + limpo + " não existe" + orientacao(limpo) + "."));
    }

    // NÃO existe um conferirDiretorios(Map). A primeira versão tinha, e a tela com
    // dois campos a chamava com Map.of(...) — que NÃO garante ordem de iteração.
    // A mensagem apontaria ora o campo de cima, ora o de baixo, sem nada mudar no
    // código. Quem tem mais de um campo encadeia com Optional.or(), que é ordenado
    // e ainda por cima só avalia o segundo se o primeiro passar.

    /**
     * Acrescenta a orientação que transforma "não existe" em algo acionável.
     *
     * <p>Caminho do Windows num sistema que não é Windows é o caso do contêiner:
     * {@code Path.of("C:/animes/86")} no Linux vira um caminho RELATIVO e acaba
     * resolvido como {@code /app/C:/animes/86}. A mensagem crua ("não existe")
     * está certa e não ajuda ninguém; a sugestão diz onde o acervo realmente
     * está montado — e continua sendo sugestão, porque quem escolhe é quem sabe.
     */
    private String orientacao(String caminho) {
        boolean sistemaWindows = File.separatorChar == '\\';
        if (sistemaWindows || !CAMINHO_WINDOWS.matcher(caminho).matches()) {
            return "";
        }
        // NÃO se monta aqui o caminho equivalente. Seria um palpite: a segunda
        // parte de "C:/animes/86" só é o acervo porque a montagem de HOJE diz
        // isso, e "D:/PROJETOS/x" viraria "/acervo/x", que aponta para lugar
        // nenhum com cara de resposta. Diz-se onde o acervo está e devolve-se a
        // escolha a quem sabe — é a diferença entre orientar e adivinhar.
        return " — este é um caminho do Windows e o KRONOS está rodando em Linux (contêiner),"
            + " onde o acervo do host é montado em " + RAIZ_ACERVO_CONTAINER
            + ". Use o botão Procurar para escolher a pasta a partir de " + RAIZ_ACERVO_CONTAINER;
    }
}
