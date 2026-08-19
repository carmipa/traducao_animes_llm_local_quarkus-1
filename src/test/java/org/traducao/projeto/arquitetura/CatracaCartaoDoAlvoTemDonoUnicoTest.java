package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: o cartão que mostra PARA ONDE a tela vai escrever tem um dono só. Toda
 * tela que exibe o cartão chama {@code js/cartaoAlvoAtivo.js} — nenhuma monta o seu por conta.
 *
 * <h2>O prejuízo que originou (19/08/2026), e ele foi MEDIDO</h2>
 * O mesmo cartão existia em <b>quatro telas</b>, com o JS copiado em cada uma. As cópias já
 * tinham divergido em <b>três comportamentos diferentes</b> para a mesma coisa:
 *
 * <pre>
 *   Correção de Cache ....... &lt;code&gt; por textContent
 *   3.1 Revisão de Legendas . &lt;code class="alvo-pasta"&gt; por textContent
 *   3.2 Revisão de Lore ..... INTERPOLADA no innerHTML   &lt;-- a divergente
 *   3.3 Concordância ........ módulo compartilhado (nasceu depois)
 * </pre>
 *
 * Ninguém decidiu a diferença: ela apareceu sozinha, que é como divergência de cópia sempre
 * aparece. Um caminho com {@code &} no nome da pasta renderizava diferente conforme a tela.
 *
 * <p><b>A gravidade é declarada com honestidade:</b> o cenário de marcação injetada por {@code <}
 * NÃO acontece — caminho do Windows não aceita esse caractere. O que esta catraca protege é a
 * DIVERGÊNCIA, e o custo dela já foi cobrado duas vezes neste projeto no mesmo mês: a limpeza do
 * token de fim de turno existia na fatia {@code traducao} e faltava na {@code revisaoLore} sete
 * dias depois; e a catraca de escrita cobria a 3.1 e era cega para a 3.2. Cópia não avisa quando
 * anda sozinha — por isso a guarda é executável, e não um combinado.
 *
 * <h2>O critério, e por que é este</h2>
 * O alvo é o arquivo {@code .js} que procura o elemento do cartão pelo id
 * ({@code '...-alvo-texto'}). Quem faz isso está montando o cartão; quem não faz, não está.
 * Filtrar por nome de tela ou por pasta seria adivinhação — a busca pelo elemento é o fato.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Quem procura o elemento do cartão importa {@code cartaoAlvoAtivo.js}, ou está na LINHA
 *       DE BASE nominal abaixo, com o motivo escrito.</li>
 *   <li>A linha de base é CATRACA: <b>só desce</b>. Tela nova entra usando o módulo, nunca na
 *       lista.</li>
 *   <li><b>Alvo vazio é NÃO VERIFICADO, nunca aprovação:</b> varredura que não acha nenhum cartão
 *       reprova, porque existem quatro.</li>
 * </ul>
 *
 * <h2>Limite declarado</h2>
 * Prova que a tela CHAMA o módulo, não que o módulo esteja certo. Quem prova o comportamento do
 * cartão é a leitura da tela; esta catraca só impede a quinta cópia de nascer.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprova nomeando o arquivo exato e o que falta. Pasta {@code static} ausente reprova como não
 * verificado — "não achei nada" e "não tinha como achar" não podem dar o mesmo sinal.
 */
class CatracaCartaoDoAlvoTemDonoUnicoTest {

    private static final Path RAIZ_ESTATICOS = Path.of("src", "main", "resources", "static");

    /** Quem procura o elemento do cartão pelo id está montando o cartão. */
    private static final String PROCURA_O_CARTAO = "-alvo-texto'";

    /**
     * Duas marcas, e não uma: o import E a chamada. Só o nome do arquivo aprovaria uma tela que
     * apenas CITA o módulo num comentário — guarda que aceita menção como prova aprova por
     * cegueira, e este projeto já pagou por isso.
     */
    private static final String IMPORTA_O_MODULO = "cartaoAlvoAtivo.js";
    private static final String CHAMA_O_MODULO = "ligarCartaoAlvoAtivo(";

    /**
     * O próprio módulo cita o elemento na documentação. Exclusão NOMINAL, de um arquivo só —
     * excluir a pasta {@code js/} inteira abriria a brecha para a próxima cópia morar lá.
     */
    private static final String O_PROPRIO_MODULO = "cartaoAlvoAtivo.js";

    /**
     * LINHA DE BASE — exceção declarada, não dívida esquecida. Só desce.
     *
     * <p>A Correção de Cache monta o cartão dela e <b>continua assim de propósito</b>: o cartão
     * alterna entre "somente esta pasta" e "o acervo INTEIRO", que é outra decisão, não a mesma
     * com outro texto. Forçar o módulo ali seria encaixar a régua errada — e módulo compartilhado
     * que ganha opção para cada tela volta a ser quatro comportamentos, só que num arquivo só.
     */
    private static final Map<String, String> LINHA_DE_BASE = new LinkedHashMap<>();

    static {
        LINHA_DE_BASE.put("correcao.js",
            "cartao com OUTRA semantica: alterna entre 'somente esta pasta' e 'o acervo INTEIRO'. "
            + "Nao e o mesmo cartao com outro texto — e outra decisao.");
    }

    private static Set<String> telasComCartao(Path raiz) throws IOException {
        Set<String> achadas = new TreeSet<>();
        try (Stream<Path> caminhos = Files.walk(raiz)) {
            for (Path arquivo : caminhos.filter(p -> p.toString().endsWith(".js"))
                .filter(p -> !p.getFileName().toString().equals(O_PROPRIO_MODULO)).toList()) {
                if (Files.readString(arquivo, StandardCharsets.UTF_8).contains(PROCURA_O_CARTAO)) {
                    achadas.add(arquivo.getFileName().toString());
                }
            }
        }
        return achadas;
    }

    @Test
    @DisplayName("toda tela com cartao do alvo chama o modulo compartilhado")
    void cartaoDoAlvoNaoTemCopia() throws IOException {
        assertTrue(Files.isDirectory(RAIZ_ESTATICOS),
            "NAO VERIFICADO: " + RAIZ_ESTATICOS.toAbsolutePath() + " nao existe");

        Set<String> comCartao = telasComCartao(RAIZ_ESTATICOS);
        assertFalse(comCartao.isEmpty(),
            "instrumento CEGO: nao achou NENHUMA tela com cartao do alvo, e existem quatro. "
                + "Se a marcacao mudou, corrija o criterio — nao apague a catraca.");

        List<String> semModulo = new ArrayList<>();
        for (String nome : comCartao) {
            if (LINHA_DE_BASE.containsKey(nome)) {
                continue;
            }
            String fonte = fonteDe(nome);
            if (!fonte.contains(IMPORTA_O_MODULO) || !fonte.contains(CHAMA_O_MODULO)) {
                semModulo.add(nome);
            }
        }

        assertTrue(semModulo.isEmpty(), () -> """
            Tela montando o cartao do alvo por conta propria: %s

            Em 19/08/2026 o mesmo cartao existia em quatro telas com o JS copiado, e as copias ja
            tinham divergido em TRES jeitos de mostrar a pasta — uma delas interpolando o caminho
            no innerHTML. Ninguem decidiu a diferenca; ela apareceu sozinha.

            Importe js/cartaoAlvoAtivo.js e chame ligarCartaoAlvoAtivo({...}). Se o seu cartao tem
            OUTRA semantica (como o da Correcao de Cache, que alterna para o acervo inteiro),
            declare aqui na LINHA_DE_BASE com o motivo escrito.
            """.formatted(semModulo));

        List<String> sumiram = new ArrayList<>(LINHA_DE_BASE.keySet());
        sumiram.removeAll(comCartao);
        assertTrue(sumiram.isEmpty(),
            "excecao declarada sumiu da varredura: " + sumiram + ". Se a tela passou a usar o "
                + "modulo, tire da linha de base — a catraca so desce. Se so mudou de forma, "
                + "corrija o criterio, nao afrouxe a busca.");
    }

    private static String fonteDe(String nomeDoArquivo) throws IOException {
        try (Stream<Path> caminhos = Files.walk(RAIZ_ESTATICOS)) {
            Path achado = caminhos.filter(p -> p.getFileName().toString().equals(nomeDoArquivo))
                .findFirst().orElse(null);
            return achado == null ? "" : Files.readString(achado, StandardCharsets.UTF_8);
        }
    }

    /**
     * CASO-CONTROLE: a varredura só vale se separar os três casos — a tela que usa o módulo, a que
     * monta o cartão inline e a que não tem cartão nenhum. Sem isto, um critério errado deixaria a
     * catraca verde para sempre.
     */
    @Test
    @DisplayName("instrumento calibrado: acha a copia inline e ignora quem nao tem cartao")
    void instrumentoDiscrimina(@TempDir Path temp) throws IOException {
        Files.writeString(temp.resolve("telaBoa.js"), """
            import { ligarCartaoAlvoAtivo } from '../js/cartaoAlvoAtivo.js';
            ligarCartaoAlvoAtivo({ alvoTextoId: 'tela-boa-alvo-texto', selectId: 'x', pastaId: 'y' });
            """);
        Files.writeString(temp.resolve("telaCopiada.js"), """
            const alvo = document.getElementById('tela-copiada-alvo-texto');
            alvo.innerHTML = 'Pasta: <strong>' + pasta + '</strong>';
            """);
        Files.writeString(temp.resolve("semCartao.js"), """
            export function initOutraCoisa() { document.getElementById('console-outro'); }
            """);

        Set<String> comCartao = telasComCartao(temp);
        assertEquals(Set.of("telaBoa.js", "telaCopiada.js"), comCartao,
            "a varredura tinha de achar as DUAS telas com cartao. Achou: " + comCartao);

        String copiada = Files.readString(temp.resolve("telaCopiada.js"), StandardCharsets.UTF_8);
        String boa = Files.readString(temp.resolve("telaBoa.js"), StandardCharsets.UTF_8);
        assertFalse(copiada.contains(IMPORTA_O_MODULO) && copiada.contains(CHAMA_O_MODULO),
            "a copia inline nao pode passar pelo criterio do modulo");
        assertTrue(boa.contains(IMPORTA_O_MODULO) && boa.contains(CHAMA_O_MODULO),
            "a tela que usa o modulo tem de passar");

        // Terceiro caso do controle, e o que motivou as DUAS marcas: uma tela que so CITA o
        // modulo num comentario nao pode ser confundida com uma que o usa.
        Files.writeString(temp.resolve("telaQueSoCita.js"), """
            // Cartao do alvo: o dono e js/cartaoAlvoAtivo.js — migrar esta tela depois.
            const alvo = document.getElementById('tela-que-so-cita-alvo-texto');
            alvo.innerHTML = 'Pasta: <strong>' + pasta + '</strong>';
            """);
        String soCita = Files.readString(temp.resolve("telaQueSoCita.js"), StandardCharsets.UTF_8);
        assertTrue(soCita.contains(IMPORTA_O_MODULO),
            "o caso-controle so vale se a mencao ESTIVER la — senao nao prova nada");
        assertFalse(soCita.contains(CHAMA_O_MODULO),
            "mencao em comentario NAO pode passar como uso do modulo");
    }

    /**
     * PROPÓSITO: id trocado deixa o cartão MORTO EM SILÊNCIO. O módulo devolve {@code null} quando
     * não acha o elemento — comportamento certo, para a tela nunca deixar de carregar por causa do
     * cartão — mas isso significa que um typo no id não quebra nada visível: a tela abre, o cartão
     * simplesmente não aparece, e o operador perde o único lugar onde a pasta de destino está à
     * vista antes do clique.
     *
     * <p>É a invariante 12 do projeto: <b>saída vazia ambígua é bug</b>. "Não achei o elemento" e
     * "não tem cartão nesta tela" não podem dar o mesmo sinal. Como não dá para provar a
     * renderização sem subir o navegador, prova-se o que a antecede: todo id passado ao módulo
     * EXISTE na marcação.
     */
    @Test
    @DisplayName("todo id passado ao modulo existe na marcacao da tela")
    void idsDoCartaoExistemNoHtml() throws IOException {
        assertTrue(Files.isDirectory(RAIZ_ESTATICOS),
            "NAO VERIFICADO: " + RAIZ_ESTATICOS.toAbsolutePath() + " nao existe");

        String marcacao = todaAMarcacao(RAIZ_ESTATICOS);
        assertFalse(marcacao.isBlank(), "instrumento CEGO: nenhum .html lido em " + RAIZ_ESTATICOS);

        Map<String, String> ausentes = new LinkedHashMap<>();
        int conferidos = 0;
        try (Stream<Path> caminhos = Files.walk(RAIZ_ESTATICOS)) {
            for (Path arquivo : caminhos.filter(p -> p.toString().endsWith(".js"))
                .filter(p -> !p.getFileName().toString().equals(O_PROPRIO_MODULO)).toList()) {
                String fonte = Files.readString(arquivo, StandardCharsets.UTF_8);
                if (!fonte.contains(CHAMA_O_MODULO)) {
                    continue;
                }
                for (String id : idsPassadosAoModulo(fonte)) {
                    conferidos++;
                    if (!marcacao.contains("id=\"" + id + "\"")) {
                        ausentes.put(arquivo.getFileName() + " -> " + id, "sem id= correspondente");
                    }
                }
            }
        }

        assertTrue(conferidos > 0,
            "instrumento CEGO: nenhuma chamada ao modulo teve id extraido, e existem tres telas. "
                + "Se a forma da chamada mudou, corrija a extracao — nao apague a catraca.");
        assertTrue(ausentes.isEmpty(),
            "id passado ao cartao que NAO existe na marcacao: " + ausentes.keySet()
                + ". O modulo devolve null em silencio nesse caso: a tela abre e o cartao "
                + "simplesmente nao aparece.");
    }

    /** Os ids literais passados nas opções do módulo, na ordem em que aparecem. */
    private static List<String> idsPassadosAoModulo(String fonte) {
        List<String> ids = new ArrayList<>();
        var m = java.util.regex.Pattern
            .compile("(alvoTextoId|selectId|pastaId|caixaId)\\s*:\\s*'([^']+)'")
            .matcher(fonte);
        while (m.find()) {
            ids.add(m.group(2));
        }
        return ids;
    }

    private static String todaAMarcacao(Path raiz) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Stream<Path> caminhos = Files.walk(raiz)) {
            for (Path html : caminhos.filter(p -> p.toString().endsWith(".html")).toList()) {
                sb.append(Files.readString(html, StandardCharsets.UTF_8)).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * CASO-CONTROLE da extração: sem isto, uma regex que não casa nada devolveria lista vazia e a
     * conferência ficaria verde por não ter olhado.
     */
    @Test
    @DisplayName("instrumento calibrado: extrai os ids reais e pega o id inventado")
    void extracaoDeIdsCalibrada() {
        String chamada = """
            ligarCartaoAlvoAtivo({
                alvoTextoId: 'x-alvo-texto',
                selectId: 'x-contexto',
                pastaId: 'x-entrada',
                caixaId: 'x-alvo',
                rotuloObra: 'Obra'
            });
            """;
        assertEquals(List.of("x-alvo-texto", "x-contexto", "x-entrada", "x-alvo"),
            idsPassadosAoModulo(chamada), "a extracao tinha de pegar os quatro ids, e so eles");
        assertTrue(idsPassadosAoModulo("nenhuma chamada aqui").isEmpty(),
            "extracao nao pode inventar id onde nao ha chamada");
    }
}
