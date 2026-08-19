package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;
import org.traducao.projeto.core.texto.dicionarioOrtografia.VeredictoPalavra;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: responder a pergunta que nenhum instrumento do projeto respondia —
 * <b>quantos erros de concordância de gênero existem de verdade no acervo?</b> — sem depender das
 * listas curadas de 20 substantivos e 30 adjetivos que o corretor e o detector usam hoje.
 *
 * <h2>Por que esta medição existe</h2>
 * Os instrumentos atuais acusam <b>1 fala em 332.545</b>. Isso tem duas leituras opostas, e elas
 * levam a decisões contrárias: ou o acervo está limpo de concordância intra-frase, ou o
 * instrumento é estreito demais para ver o que existe. Escrever mais corretor antes de separar as
 * duas é escrever remédio para doença não diagnosticada.
 *
 * <h2>O oráculo, e o que ele pode dizer</h2>
 * O dicionário pt_BR (hunspell, 312.369 entradas) <b>não sabe gênero</b> — perguntar
 * {@code garota} devolve {@code st:garotar}, um verbo. O que ele sabe é se uma forma EXISTE, e é
 * disso que sai a inferência por <b>par mínimo</b>: se a palavra termina em {@code -a} e a forma
 * em {@code -o} também existe, ela é a flexão feminina de um par; o inverso vale para {@code -o}.
 * Palavra sem par ({@code problema}, {@code dia}, {@code mapa}, {@code artista}) fica de fora —
 * é exatamente esse filtro que impede o instrumento de acusar substantivo invariável.
 *
 * <p>Quem responde "esta forma existe" é a produção ({@link CorretorOrtograficoLegenda#classificar}),
 * em LOTE, e ela distingue {@code DESCONHECIDA} de {@code NAO_VERIFICADO} — dicionário ausente
 * não vira aprovação silenciosa.
 *
 * <h2>O que este número É e o que ele NÃO é</h2>
 * É uma lista de <b>candidatos</b>, não de erros. O par mínimo tem falso positivo conhecido:
 * {@code caso}/{@code casa} e {@code barco}/{@code barca} existem os dois e são lemas
 * diferentes. Por isso o relatório imprime AMOSTRAS — o veredito final é o olho sobre a fala, e
 * a taxa de acerto medida na amostra é o que qualifica o total. Contagem não prova conjunto.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY, mesmo universo dos outros harnesses: sem música, sem {@code .parcial}.</li>
 *   <li><b>Controle positivo e negativo no MESMO experimento:</b> falas plantadas que TÊM de ser
 *       acusadas e falas corretas que NÃO podem ser — o instrumento reprova se errar qualquer
 *       uma, antes de a contagem do acervo valer alguma coisa.</li>
 *   <li>O determinante {@code a}/{@code as} é contado SEPARADO: ele também é preposição, e foi
 *       exatamente por confundir os dois que o corretor estragou 14 falas de "graças a Deus".</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Dicionário indisponível termina declarando isso e sem afirmar número nenhum.
 *
 * <p>Uso: {@code gradlew test --tests "*MedicaoConcordanciaPorDicionarioIT*" "-Dkronos.medicao=true"}
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoConcordanciaPorDicionarioIT {

    private static final Path RAIZ = Path.of(System.getProperty("kronos.acervo", "C:\\animes"));
    private static final int AMOSTRAS = 40;

    @Inject
    LeitorLegendaAss leitor;

    @Inject
    PoliticaEstiloMusical politicaEstiloMusical;

    @Inject
    CorretorOrtograficoLegenda dicionario;

    /** Determinantes de gênero INEQUÍVOCO. O {@code a}/{@code as} entra na lista ambígua. */
    private static final Set<String> DET_MASC = Set.of(
        "o", "os", "um", "uns", "este", "estes", "esse", "esses", "aquele", "aqueles",
        "do", "dos", "no", "nos", "ao", "aos", "pelo", "pelos", "num", "nuns", "meu", "meus",
        "seu", "seus", "nosso", "nossos");
    /**
     * O feminino, SEM {@code esta}/{@code estas}.
     *
     * <p>O caso-controle pegou isto antes de qualquer número sair: em {@code "O dia esta bonito."}
     * o {@code esta} é o VERBO "está" sem acento — e a aya produz texto sem acento, foram 197
     * casos medidos no Unicorn. Tratá-lo como demonstrativo feminino fazia o instrumento acusar
     * {@code esta bonito} como discordância, numa frase perfeita. É o mesmo erro de classe que o
     * {@code a} preposição causou no corretor, e a lição é a mesma: palavra ambígua fica fora.
     *
     * <p><b>Perda declarada:</b> {@code "esta menino"} deixa de ser detectável. O demonstrativo
     * {@code este} (masculino) não tem essa ambiguidade e continua na lista.
     */
    private static final Set<String> DET_FEM = Set.of(
        "uma", "umas", "essa", "essas", "aquela", "aquelas",
        "da", "das", "na", "nas", "pela", "pelas", "numa", "numas", "minha", "minhas",
        "sua", "suas", "nossa", "nossas", "à", "às");
    /** Ambíguos com preposição: medidos à parte, nunca somados ao número principal. */
    private static final Set<String> DET_FEM_AMBIGUO = Set.of("a", "as");

    private static final Pattern PAR = Pattern.compile(
        "(?<![\\p{L}\\p{N}])(\\p{L}+)\\s+(\\p{L}{3,})(?![\\p{L}\\p{N}])",
        Pattern.UNICODE_CHARACTER_CLASS);

    /** Um par determinante+palavra encontrado numa fala. */
    private record Par(String determinante, String palavra, String obra, String fala,
                       boolean seguidoDeHifen, boolean maiusculaNoMeio) {}

    /**
     * Palavras de <b>gênero comum</b> — a mesma forma serve aos dois: {@code o cara}/{@code a
     * cara}, {@code a piloto}, {@code o colega}. O par mínimo não as distingue (existe
     * {@code caro}, existe {@code piloto/pilota}) e por isso as acusa sempre.
     *
     * <p>Esta lista é EXCLUSÃO de medição, não regra de produção: ela serve para o número
     * estimado não ser dominado por construção correta. Foi montada lendo os pares mais
     * frequentes do próprio acervo — nenhuma palavra entrou aqui por intuição.
     */
    private static final Set<String> GENERO_COMUM = Set.of(
        "cara", "piloto", "idiota", "guarda", "colega", "artista", "policia", "chefe",
        "fantasma", "cometa", "sistema", "programa", "problema", "planeta", "clima", "mapa",
        "tema", "poema", "drama", "dilema", "esquema", "trauma", "estrategista", "camarada",
        "companhia", "gente", "recruta");
    // CORRIGIDO em 19/08/2026: "criança", "pessoa", "vitima" e "testemunha" estavam aqui por
    // engano meu — são femininos FIXOS, não de gênero comum, e excluí-las cegava a medição para
    // erros reais. A medição do plural mostrou o custo: "aqueles crianças" só apareceu porque a
    // exclusão comparava o singular e a fala trazia o plural.

    @Test
    @DisplayName("acervo: quantos erros de concordancia existem que as listas curadas NAO veem")
    void medirComDicionario() throws IOException {
        if (!Files.isDirectory(RAIZ)) {
            System.out.println("SEM ACERVO em " + RAIZ + " — nada medido.");
            return;
        }
        // SONDA ANTES DE PERGUNTAR — e ela é o controle do próprio oráculo. O adaptador só sabe
        // se está disponível DEPOIS da primeira consulta ({@code disponivel} nasce null e vira
        // true/false ao rodar o processo); consultar o estado antes de exercitá-lo devolve false
        // sempre, e foi o que esta medição fez na primeira tentativa. Aqui a sonda pergunta por
        // uma palavra que TEM de ser conhecida e uma que NÃO pode ser: se as duas não
        // discriminarem, nenhum número é afirmado.
        Map<String, VeredictoPalavra> sonda = dicionario.classificar(
            new LinkedHashSet<>(List.of("menina", "menino", "xkcdqwzp")));
        boolean oraculoDiscrimina = sonda.get("menina") == VeredictoPalavra.PORTUGUES_OK
            && sonda.get("menino") == VeredictoPalavra.PORTUGUES_OK
            && sonda.get("xkcdqwzp") != VeredictoPalavra.PORTUGUES_OK;
        if (!dicionario.disponivel() || !oraculoDiscrimina) {
            System.out.printf("NAO VERIFICADO: o dicionario pt_BR nao discriminou na sonda "
                + "(menina=%s, menino=%s, xkcdqwzp=%s). Nenhum numero afirmado.%n",
                sonda.get("menina"), sonda.get("menino"), sonda.get("xkcdqwzp"));
            return;
        }

        // ---------- 1. varredura: todos os pares determinante + palavra ----------
        List<Par> pares = new ArrayList<>();
        int falas = 0;
        List<Path> obras;
        try (Stream<Path> s = Files.list(RAIZ)) {
            obras = s.filter(Files::isDirectory).sorted().toList();
        }
        for (Path obra : obras) {
            List<Path> arquivos;
            try (Stream<Path> s = Files.walk(obra)) {
                arquivos = s.filter(Files::isRegularFile).filter(this::assOuSsa)
                    .filter(p -> !p.getFileName().toString().toLowerCase().contains(".parcial."))
                    .sorted().toList();
            }
            for (Path arquivo : arquivos) {
                DocumentoLegenda documento;
                try {
                    documento = leitor.ler(arquivo);
                } catch (RuntimeException e) {
                    continue;
                }
                for (EventoLegenda evento : documento.eventos()) {
                    if (!evento.temTexto()) {
                        continue;
                    }
                    if (evento.estilo() != null && politicaEstiloMusical.estiloIgnorado(evento.estilo())) {
                        continue;
                    }
                    falas++;
                    colher(visivel(evento.texto()), obra.getFileName().toString(), pares);
                }
            }
        }

        // ---------- 2. UMA pergunta em lote ao dicionário ----------
        Set<String> formas = new LinkedHashSet<>();
        for (Par p : pares) {
            formas.add(p.palavra());
            String outra = flexionar(p.palavra());
            if (outra != null) {
                formas.add(outra);
            }
        }
        Map<String, VeredictoPalavra> veredictos = classificarEmBlocos(formas);

        // ---------- 3. controle positivo e negativo, no MESMO experimento ----------
        List<String> falhasDoControle = rodarControle(veredictos);

        // ---------- 4. o veredito por par ----------
        Map<String, Integer> porPar = new TreeMap<>();
        List<String> amostras = new ArrayList<>();
        int comGeneroInferivel = 0;
        int discordantes = 0;
        int discordantesAmbiguos = 0;

        int descartadosPorRuido = 0;
        for (Par p : pares) {
            if (p.seguidoDeHifen() || p.maiusculaNoMeio()
                || GENERO_COMUM.contains(p.palavra().toLowerCase())) {
                descartadosPorRuido++;
                continue;
            }
            Boolean generoDaPalavra = generoFeminino(p.palavra(), veredictos);
            if (generoDaPalavra == null) {
                continue;
            }
            String det = p.determinante().toLowerCase();
            Boolean generoDoDet = DET_FEM.contains(det) ? Boolean.TRUE
                : DET_MASC.contains(det) ? Boolean.FALSE
                : DET_FEM_AMBIGUO.contains(det) ? Boolean.TRUE : null;
            if (generoDoDet == null) {
                continue;
            }
            boolean ambiguo = DET_FEM_AMBIGUO.contains(det);
            if (!ambiguo) {
                comGeneroInferivel++;
            }
            if (generoDoDet.equals(generoDaPalavra)) {
                continue;
            }
            if (ambiguo) {
                discordantesAmbiguos++;
                continue;
            }
            discordantes++;
            porPar.merge(det + " " + p.palavra().toLowerCase(), 1, Integer::sum);
            if (amostras.size() < AMOSTRAS) {
                amostras.add(String.format("  %-22s %s | %s", det + " " + p.palavra(),
                    p.obra(), recortar(p.fala(), 96)));
            }
        }

        // ---------- 5. relatório ----------
        System.out.printf("%n=== CONCORDANCIA MEDIDA PELO DICIONARIO (par minimo) ===%n");
        System.out.printf("falas ao alcance ..................... %d%n", falas);
        System.out.printf("pares determinante+palavra ........... %d%n", pares.size());
        System.out.printf("descartados por ruido estrutural ..... %d   (hifen, nome proprio, genero comum)%n",
            descartadosPorRuido);
        System.out.printf("com genero INFERIVEL pelo dicionario . %d%n", comGeneroInferivel);
        System.out.printf("DISCORDANTES (candidatos a erro) ..... %d%n", discordantes);
        System.out.printf("discordantes com 'a/as' (ambiguo) .... %d   <- contados a parte: 'a' tambem e preposicao%n%n",
            discordantesAmbiguos);

        System.out.println("PARES DISTINTOS (todos, do mais frequente ao menos):");
        porPar.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
            .forEach(e -> System.out.printf("  %5d  %s%n", e.getValue(), e.getKey()));
        System.out.printf("%npares distintos: %d%n", porPar.size());

        System.out.printf("%nAMOSTRAS (o veredito final e o olho sobre a fala):%n");
        amostras.forEach(System.out::println);

        System.out.printf("%n[controle] %s%n", falhasDoControle.isEmpty()
            ? "positivo e negativo OK — o instrumento discrimina"
            : "FALHOU: " + falhasDoControle);

        assertTrue(falhasDoControle.isEmpty(),
            "INSTRUMENTO NAO CALIBRADO: " + falhasDoControle + ". Enquanto ele nao separa o "
                + "caso doente do sao, o numero medido no acervo nao vale nada.");
        assertTrue(falas > 0, "controle positivo: nenhuma fala ao alcance — instrumento cego");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: pergunta ao dicionário em BLOCOS, dentro do orçamento que o próprio
     * adaptador declara.
     *
     * <h2>O prejuízo, medido nesta sessão</h2>
     * A primeira versão mandou o conjunto inteiro — dezenas de milhares de formas — numa chamada
     * só. O {@code HunspellDicionarioAdapter} tem timeout de <b>20 s</b> e a documentação dele diz
     * o custo real: <i>7.432 formas em 12,6 s</i>. Resultado: o processo ficou com <b>1.159 s de
     * CPU</b>, a medição passou de 10 minutos sem terminar, e o timeout do adaptador desistiu da
     * resposta sem matar o processo. Foi preciso derrubar o hunspell à mão.
     *
     * <p>INVARIANTES DO DOMÍNIO: bloco de 3.000 formas, abaixo da metade do que já foi medido
     * como seguro; a ordem de inserção é preservada e os mapas parciais se somam sem sobrescrever
     * veredito anterior.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: bloco que volte vazio não derruba os outros — a palavra
     * fica sem veredito e, por construção, cai fora da conta em vez de virar acusação.
     */
    private Map<String, VeredictoPalavra> classificarEmBlocos(Set<String> formas) {
        final int tamanhoDoBloco = 3_000;
        Map<String, VeredictoPalavra> todos = new java.util.LinkedHashMap<>();
        List<String> lista = new ArrayList<>(formas);
        for (int i = 0; i < lista.size(); i += tamanhoDoBloco) {
            Set<String> bloco = new LinkedHashSet<>(
                lista.subList(i, Math.min(i + tamanhoDoBloco, lista.size())));
            todos.putAll(dicionario.classificar(bloco));
            System.out.printf("  dicionario: %d/%d formas classificadas%n",
                Math.min(i + tamanhoDoBloco, lista.size()), lista.size());
        }
        return todos;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: prova, no mesmo experimento, que o instrumento acusa o erro plantado
     * e deixa em paz a fala correta — inclusive os substantivos invariáveis que o par mínimo
     * precisa filtrar.
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve a lista do que ele errou; vazio significa
     * calibrado.
     */
    private List<String> rodarControle(Map<String, VeredictoPalavra> jaClassificados) {
        record Caso(String fala, boolean deveAcusar, String porque) {}
        List<Caso> casos = List.of(
            new Caso("Vi o menina no parque.", true, "artigo masc + substantivo fem"),
            new Caso("Chamei uma menino.", true, "artigo fem + substantivo masc"),
            new Caso("Aquela garoto poderia ser um Newtype.", true, "demonstrativo trocado"),
            new Caso("A menina esta cansada.", false, "concordancia correta"),
            new Caso("O problema e outro.", false, "invariavel: 'problemo' nao existe"),
            new Caso("O dia esta bonito.", false, "invariavel: 'dio' nao existe"),
            new Caso("O mapa da regiao.", false, "invariavel: 'mapo' nao existe"),
            new Caso("A casa e grande.", false, "par existe (caso/casa) mas concorda"));

        List<String> falhas = new ArrayList<>();
        for (Caso caso : casos) {
            List<Par> pares = new ArrayList<>();
            colher(caso.fala(), "controle", pares);

            Set<String> formas = new LinkedHashSet<>();
            for (Par p : pares) {
                formas.add(p.palavra());
                String outra = flexionar(p.palavra());
                if (outra != null) {
                    formas.add(outra);
                }
            }
            Map<String, VeredictoPalavra> v = new TreeMap<>(jaClassificados);
            v.putAll(dicionario.classificar(formas));

            boolean acusou = false;
            for (Par p : pares) {
                Boolean generoPalavra = generoFeminino(p.palavra(), v);
                if (generoPalavra == null) {
                    continue;
                }
                String det = p.determinante().toLowerCase();
                Boolean generoDet = DET_FEM.contains(det) ? Boolean.TRUE
                    : DET_MASC.contains(det) ? Boolean.FALSE : null;
                if (generoDet != null && !generoDet.equals(generoPalavra)) {
                    acusou = true;
                }
            }
            if (acusou != caso.deveAcusar()) {
                falhas.add(String.format("\"%s\" (%s): esperado %s, obtido %s", caso.fala(),
                    caso.porque(), caso.deveAcusar() ? "ACUSAR" : "calar",
                    acusou ? "ACUSOU" : "calou"));
            }
        }
        return falhas;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o gênero da palavra pelo PAR MÍNIMO — {@code TRUE} feminino,
     * {@code FALSE} masculino, {@code null} quando o dicionário não permite decidir.
     *
     * <p>INVARIANTES DO DOMÍNIO: exige que as DUAS formas existam. Palavra sem contraparte é
     * invariável ou de gênero fixo, e acusá-la seria alarme falso — é este filtro que mantém
     * {@code problema}, {@code dia} e {@code artista} fora da conta.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer veredito que não seja {@code PORTUGUES_OK}
     * conta como "não existe", e a palavra cai fora — na dúvida, o instrumento cala.
     */
    private Boolean generoFeminino(String palavra, Map<String, VeredictoPalavra> veredictos) {
        String w = palavra.toLowerCase();
        String outra = flexionar(w);
        if (outra == null || !conhecida(w, veredictos) || !conhecida(outra, veredictos)) {
            return null;
        }
        return w.endsWith("a") || w.endsWith("as") ? Boolean.TRUE : Boolean.FALSE;
    }

    private boolean conhecida(String forma, Map<String, VeredictoPalavra> veredictos) {
        return veredictos.get(forma) == VeredictoPalavra.PORTUGUES_OK;
    }

    /**
     * A contraparte de gênero pela terminação: {@code menina→menino}, {@code menino→menina} — e
     * desde 19/08/2026 também no PLURAL, {@code meninas→meninos}.
     *
     * <p>O plural entrou porque a primeira medição não o via: {@code reparos} termina em
     * {@code -s}, caía fora do par mínimo, e a fala {@code "nas reparos da casco"} teve o
     * {@code casco} corrigido enquanto o {@code nas reparos} passava. Medir antes de decidir se
     * vale ampliar a correção é o ponto — o número é que responde.
     */
    private String flexionar(String palavra) {
        String w = palavra.toLowerCase();
        if (w.length() < 4) {
            return null;
        }
        if (w.endsWith("as")) {
            return w.substring(0, w.length() - 2) + "os";
        }
        if (w.endsWith("os")) {
            return w.substring(0, w.length() - 2) + "as";
        }
        if (w.endsWith("a")) {
            return w.substring(0, w.length() - 1) + "o";
        }
        if (w.endsWith("o")) {
            return w.substring(0, w.length() - 1) + "a";
        }
        return null;
    }

    private void colher(String texto, String obra, List<Par> destino) {
        Matcher m = PAR.matcher(texto);
        int fim = 0;
        while (m.find(fim)) {
            // Só o par que começa em DETERMINANTE interessa. Guardar todos os pares adjacentes
            // fazia o conjunto de formas explodir para centenas de milhares e o hunspell, que
            // custa 12,6 s a cada 7.432 formas, virava a medição inteira — 1.159 s de CPU num
            // processo só, na primeira tentativa.
            if (!eDeterminante(m.group(1))) {
                fim = m.end(1) + 1;
                continue;
            }
            // Duas marcas ESTRUTURAIS, colhidas aqui porque só o texto original as revela:
            // o hífen do composto ("Para-RAID", "guarda-chuva") e a maiúscula fora do início
            // da fala (nome próprio). As duas produziram lixo puro na primeira medição.
            int depois = m.end(2);
            boolean hifen = depois < texto.length() && texto.charAt(depois) == '-';
            boolean maiuscula = Character.isUpperCase(m.group(2).charAt(0)) && m.start(1) > 0;
            destino.add(new Par(m.group(1), m.group(2), obra, texto, hifen, maiuscula));
            // Avança só até o fim do PRIMEIRO grupo: "o menina cansada" tem dois pares, e
            // saltar o casamento inteiro perderia o segundo.
            fim = m.end(1) + 1;
        }
    }

    /** Determinante de gênero conhecido — inclui o ambíguo {@code a}/{@code as}, medido à parte. */
    private boolean eDeterminante(String palavra) {
        String p = palavra.toLowerCase();
        return DET_MASC.contains(p) || DET_FEM.contains(p) || DET_FEM_AMBIGUO.contains(p);
    }

    private boolean assOuSsa(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        return nome.endsWith(".ass") || nome.endsWith(".ssa");
    }

    /** Texto como o espectador lê: sem tags e com a quebra virando espaço. */
    private static String visivel(String t) {
        return t == null ? "" : t.replaceAll("\\{[^}]*}", "").replace("\\N", " ").strip();
    }

    private static String recortar(String t, int max) {
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }
}
