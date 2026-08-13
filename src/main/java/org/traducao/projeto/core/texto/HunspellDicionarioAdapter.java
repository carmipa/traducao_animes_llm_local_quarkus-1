package org.traducao.projeto.core.texto;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * PROPÓSITO DE NEGÓCIO: implementa o dicionário do idioma chamando o {@code hunspell} instalado no
 * sistema — o mesmo verificador que LibreOffice, Firefox e Chrome usam, com centenas de milhares de
 * formas do português.
 *
 * <h2>Por que ferramenta do sistema e não biblioteca no build</h2>
 * O projeto já trata verificadores externos assim: ffmpeg e mkvmerge são pré-requisitos declarados,
 * instalados por Chocolatey, e o código só os invoca. Um dicionário embarcado no repositório
 * significaria versionar 4 MB de dados de idioma e mantê-los; um binding JNI significaria
 * dependência nativa no build, que quebra o Docker de outra forma. Chamar o binário mantém o build
 * limpo e o mesmo código funciona em Linux, onde o hunspell também existe.
 *
 * <p>Pré-requisito: {@code choco install hunspell.portable} mais o dicionário do idioma
 * ({@code pt_BR.dic}/{@code pt_BR.aff}). Ausente, este adaptador se declara indisponível e nada é
 * verificado — nunca "está tudo certo".
 *
 * <h2>Um processo por LOTE, e o porquê</h2>
 * O custo é o spawn do processo, não a palavra: medido em 13/08/2026 com o verificador do sistema,
 * 7.432 formas distintas do Zeta saíram em 12,6 s — 1,7 ms cada, e a maior parte disso é o
 * arranque. Uma chamada por fala multiplicaria o arranque por centenas; por isso a porta pede
 * {@link Collection} e o modo {@code -a} do hunspell lê a lista inteira pelo stdin.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Cada palavra vai numa linha própria, prefixada por {@code ^} — o modo {@code -a} trata a
 *       linha como texto e sem o prefixo uma palavra com espaço acidental viraria duas.</li>
 *   <li>Só responde o que o hunspell marcou com {@code &} (desconhecida com sugestão) ou {@code #}
 *       (desconhecida sem sugestão). Linha em branco e {@code *}/{@code +}/{@code -} são aceites.</li>
 *   <li>Timeout curto: um verificador travado não pode segurar a tradução de um episódio.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Binário ausente, dicionário ausente, timeout ou saída inesperada devolvem conjunto VAZIO e
 * {@link #disponivel()} falso. Nunca lança, nunca acusa por engano.
 */
@ApplicationScoped
public class HunspellDicionarioAdapter implements DicionarioOrtograficoPort {

    private static final Logger log = LoggerFactory.getLogger(HunspellDicionarioAdapter.class);

    /** Um episódio grande tem centenas de formas distintas; 20 s é folga larga sobre os 12,6 s medidos. */
    private static final long TIMEOUT_SEGUNDOS = 20;

    private final String executavel;
    private final String idioma;
    private volatile Boolean disponivel;

    public HunspellDicionarioAdapter() {
        this("hunspell", "pt_BR");
    }

    HunspellDicionarioAdapter(String executavel, String idioma) {
        this.executavel = executavel;
        this.idioma = idioma;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: pergunta ao hunspell, de uma vez, quais das formas ele não conhece.
     *
     * <p>INVARIANTES DO DOMÍNIO: preserva a grafia recebida; entrada vazia não dispara processo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto vazio e o adaptador passa a se declarar
     * indisponível, para o relatório poder dizer "não verificado".
     */
    @Override
    public Set<String> desconhecidas(Collection<String> palavras) {
        if (palavras == null || palavras.isEmpty()) {
            return Set.of();
        }
        Set<String> candidatas = new LinkedHashSet<>(palavras);
        try {
            ProcessBuilder pb = new ProcessBuilder(executavel, "-a", "-d", idioma, "-i", "UTF-8");
            pb.redirectErrorStream(false);
            Process p = pb.start();

            try (BufferedWriter escrita = new BufferedWriter(
                    new OutputStreamWriter(p.getOutputStream(), StandardCharsets.UTF_8))) {
                for (String palavra : candidatas) {
                    // '^' força o modo -a a tratar a linha inteira como uma entrada só.
                    escrita.write("^" + palavra);
                    escrita.newLine();
                }
            }

            Set<String> desconhecidas = new LinkedHashSet<>();
            try (BufferedReader leitura = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String linha;
                while ((linha = leitura.readLine()) != null) {
                    String palavra = palavraDaLinha(linha);
                    if (palavra != null && candidatas.contains(palavra)) {
                        desconhecidas.add(palavra);
                    }
                }
            }

            if (!p.waitFor(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                log.warn("hunspell não respondeu em {}s; nada foi verificado.", TIMEOUT_SEGUNDOS);
                disponivel = false;
                return Set.of();
            }
            disponivel = true;
            return desconhecidas;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Set.of();
        } catch (Exception e) {
            // Só o PRIMEIRO fracasso vira aviso: repetir por episódio encheria o console de ruído
            // sobre um pré-requisito que o operador já sabe que falta.
            if (disponivel == null) {
                log.warn("Dicionário do sistema indisponível ({}). A ortografia NÃO será verificada. "
                        + "Instale com: choco install hunspell.portable", e.getMessage());
            }
            disponivel = false;
            return Set.of();
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: extrai a forma desconhecida de uma linha do modo {@code -a}.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code &} traz palavra, contagem, offset e sugestões;
     * {@code #} traz palavra e offset. Qualquer outro prefixo é aceite ou ruído.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: linha malformada devolve {@code null} — e isso é
     * DESCARTE CONSCIENTE de linha que não casa o contrato, não de linha que não entendi.
     */
    private static String palavraDaLinha(String linha) {
        if (linha == null || linha.length() < 3) {
            return null;
        }
        char tipo = linha.charAt(0);
        if (tipo != '&' && tipo != '#') {
            return null;
        }
        String[] partes = linha.substring(2).split(" ");
        return partes.length > 0 && !partes[0].isBlank() ? partes[0] : null;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: se o verificador respondeu ao menos uma vez nesta execução.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code null} (nunca chamado) conta como INDISPONÍVEL — quem
     * pergunta antes de usar recebe a resposta conservadora, e "não verificado" jamais é
     * apresentado como "verificado e limpo".
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code false}.
     */
    @Override
    public boolean disponivel() {
        return Boolean.TRUE.equals(disponivel);
    }

    @Override
    public String descricao() {
        return "hunspell (" + idioma + ")";
    }
}

