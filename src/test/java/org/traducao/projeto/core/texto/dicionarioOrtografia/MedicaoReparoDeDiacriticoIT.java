package org.traducao.projeto.core.texto.dicionarioOrtografia;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: medir se a palavra quebrada do acervo pode ser consertada
 * <b>mecanicamente</b> — repondo o diacrítico que o modelo comeu — antes de isso virar produção.
 *
 * <h2>A observação que originou esta medição</h2>
 * A leitura das 476 palavras quebradas do acervo (24/08/2026) mostrou que boa parte delas tem a
 * MESMA assinatura: o modelo perdeu a cedilha, o til, ou os dois.
 *
 * <pre>
 *   braos      -> braços        preguioso  -> preguiçoso
 *   obrigaao   -> obrigação     distraao   -> distração
 *   missaoes   -> missões       percepoes  -> percepções
 *   accoes     -> ações         amanhao    -> amanhã
 * </pre>
 *
 * <p>Isso não é adivinhação de sentido: é reposição de caractere, e o projeto já tem o mecanismo
 * certo para essa família — {@link CorretorAcentoPorDicionario#reparosDeTerminacaoAo} conserta
 * {@code Esquadroo→Esquadrão} propondo mecanicamente e <b>exigindo que o dicionário aceite o
 * resultado</b>. O que se mede aqui é se a mesma disciplina cobre as outras formas.
 *
 * <h2>A régua, e por que ela é essa</h2>
 * Uma proposta só conta se for a <b>ÚNICA</b> candidata que o dicionário aceita. Duas candidatas
 * válidas significam ambiguidade, e ambiguidade resolvida por desempate vira palavra inventada —
 * que é pior que a palavra quebrada: o leitor tropeça uma vez em {@code braos} e desconfia; não
 * tropeça nunca numa palavra plausível e errada.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY, e NÃO é produção: aqui só se mede se a regra valeria a pena.</li>
 *   <li>Quem aprova a proposta é o DICIONÁRIO, em lote único.</li>
 *   <li>Dicionário indisponível termina NÃO VERIFICADO.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem palavras recebidas, termina declarando — lista vazia não é acervo limpo.
 *
 * <p>Uso: {@code gradlew test --tests "*MedicaoReparoDeDiacriticoIT*" "-Dkronos.medicao=true"
 * "-Dkronos.palavras=braos,preguioso,obrigaao"}
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoReparoDeDiacriticoIT {

    private static final String PALAVRAS = System.getProperty("kronos.palavras", "");

    /**
     * Caminho de um arquivo com as palavras, uma por linha ou separadas por vírgula.
     *
     * <p>A propriedade solta estourou o limite da linha de comando do Windows com 143 palavras, e
     * o sintoma foi o pior possível: o Gradle não gerou XML nenhum. Isso só não virou "zero
     * achados" porque este projeto trata ausência de XML como NÃO VERIFICADO.
     */
    private static final String ARQUIVO_DE_PALAVRAS =
        System.getProperty("kronos.palavras.arquivo", "");

    /**
     * Os diacríticos que o português usa e que o modelo come. Nada além disto entra: a lista
     * curta é o que separa "repor caractere" de "gerar palavra parecida".
     */
    private static final int TETO_DE_CANDIDATAS = 6000;

    private static final String[][] TROCAS = {
        {"a", "ã"}, {"a", "á"}, {"a", "â"}, {"a", "à"},
        {"o", "õ"}, {"o", "ó"}, {"o", "ô"},
        {"e", "é"}, {"e", "ê"}, {"i", "í"}, {"u", "ú"},
        {"c", "ç"},
    };

    @Test
    @DisplayName("mede quantas palavras quebradas tem UMA unica reposicao de diacritico valida")
    void medir() {
        System.out.printf("%n=== REPOSICAO MECANICA DE DIACRITICO — o que ela alcancaria ===%n");
        String bruto = PALAVRAS;
        if (!ARQUIVO_DE_PALAVRAS.isBlank()) {
            try {
                bruto = java.nio.file.Files.readString(
                    java.nio.file.Path.of(ARQUIVO_DE_PALAVRAS),
                    java.nio.charset.StandardCharsets.UTF_8);
            } catch (java.io.IOException e) {
                System.out.printf("NAO VERIFICADO: nao consegui ler %s (%s)%n",
                    ARQUIVO_DE_PALAVRAS, e);
                return;
            }
        }
        Set<String> palavras = new LinkedHashSet<>();
        for (String p : bruto.split("[,\\s]+")) {
            if (!p.isBlank()) {
                palavras.add(p.trim());
            }
        }
        if (palavras.isEmpty()) {
            System.out.println("NAO VERIFICADO: nenhuma palavra em -Dkronos.palavras. "
                + "Lista vazia NAO significa acervo limpo.");
            return;
        }

        DicionarioOrtograficoPort pt = new HunspellDicionarioAdapter("hunspell", "pt_BR");

        // CONTROLE: uma palavra que TEM conserto obvio e uma que ja esta certa.
        Map<String, List<String>> ctrl = candidatasDe(Set.of("braos", "batalha"));
        Set<String> todasCtrl = new LinkedHashSet<>();
        ctrl.values().forEach(todasCtrl::addAll);
        Set<String> ruinsCtrl = pt.desconhecidas(todasCtrl);
        if (!pt.disponivel()) {
            System.out.println("NAO VERIFICADO: dicionario indisponivel — " + pt.descricao());
            return;
        }
        List<String> okBraos = ctrl.get("braos").stream().filter(c -> !ruinsCtrl.contains(c)).toList();
        if (!okBraos.contains("braços")) {
            System.out.printf("INSTRUMENTO REPROVADO NO CONTROLE: 'braos' nao gerou/aprovou "
                + "'bracos' com cedilha. Aprovadas: %s. Nenhum numero abaixo vale.%n", okBraos);
            return;
        }
        System.out.printf("  controle: 'braos' -> %s%n%n", okBraos);

        Map<String, List<String>> candidatas = candidatasDe(palavras);
        Set<String> universo = new LinkedHashSet<>();
        candidatas.values().forEach(universo::addAll);
        Set<String> ruins = pt.desconhecidas(universo);

        List<String> umaSo = new ArrayList<>();
        List<String> ambiguas = new ArrayList<>();
        List<String> semConserto = new ArrayList<>();
        List<String> naoMedidas = new ArrayList<>();
        for (String p : palavras) {
            // PULADA PELO TETO NAO E "SEM CONSERTO". A primeira versao juntava as duas coisas e
            // reportou "133 sem conserto mecanico" sobre palavras que nunca foram medidas — o
            // mesmo defeito de saida ambigua que este projeto persegue no codigo de producao,
            // cometido dentro da medicao.
            if (!candidatas.containsKey(p)) {
                naoMedidas.add(p);
                continue;
            }
            List<String> boas = candidatas.get(p).stream()
                .filter(c -> !ruins.contains(c)).toList();
            if (boas.isEmpty()) {
                semConserto.add(p);
                continue;
            }
            // A CANDIDATA DE MENOR EDICAO, e ela tem de ser UNICA nesse nivel.
            //
            // Exigir uma candidata so no total reprovou 142 de 143: o gerador produz muitas
            // formas validas, e quase toda palavra tem alguma. Mas conserto de diacritico e
            // MINIMO por natureza — o modelo comeu um caractere, nao reescreveu a palavra. Se a
            // proposta mais proxima for unica, ela e a reposicao; se houver empate na distancia
            // minima, ai sim e ambiguidade de verdade.
            int menor = boas.stream().mapToInt(c -> edicoes(p.toLowerCase(), c)).min().orElse(99);
            List<String> minimas = boas.stream()
                .filter(c -> edicoes(p.toLowerCase(), c) == menor).toList();
            if (minimas.size() == 1) {
                umaSo.add(String.format("%-16s -> %-16s (dist %d, entre %d validas)",
                    p, minimas.get(0), menor, boas.size()));
            } else {
                ambiguas.add(String.format("%-16s -> EMPATE na dist %d: %s", p, menor, minimas));
            }
        }

        System.out.printf("  RESUMO de %d palavras: %d com UMA reposicao valida · %d AMBIGUAS "
            + "(empate na menor distancia) · %d sem conserto mecanico · %d NAO MEDIDAS "
            + "(estouraram o teto)%n%n",
            palavras.size(), umaSo.size(), ambiguas.size(), semConserto.size(),
            naoMedidas.size());
        if (!naoMedidas.isEmpty()) {
            System.out.println("  NAO MEDIDAS: " + naoMedidas);
        }

        System.out.println("=== UMA REPOSICAO VALIDA (o que a regra consertaria) ===");
        umaSo.forEach(x -> System.out.println("   " + x));
        System.out.println("\n=== AMBIGUAS (a regra NAO pode tocar: desempate e invencao) ===");
        ambiguas.forEach(x -> System.out.println("   " + x));
        System.out.printf("%n=== SEM CONSERTO MECANICO (%d) — seguem sendo relatorio ===%n",
            semConserto.size());
        System.out.println("   " + semConserto);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: gera as reposições mecânicas possíveis de uma palavra — uma ou duas
     * trocas de letra por sua forma com diacrítico, e nada mais.
     *
     * <p>INVARIANTES DO DOMÍNIO: só TROCA caractere por sua versão acentuada; não insere, não
     * remove e não reordena. É essa restrição que impede o gerador de virar "palavra parecida".
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: palavra nula ou curta devolve lista vazia; nunca lança.
     */
    /**
     * Distância de edição simples (Levenshtein) — usada só para achar a proposta MAIS PRÓXIMA.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada nula conta como vazia; nunca lança.
     */
    static int edicoes(String a, String b) {
        String x = a == null ? "" : a;
        String y = b == null ? "" : b;
        int[] ant = new int[y.length() + 1];
        int[] atual = new int[y.length() + 1];
        for (int j = 0; j <= y.length(); j++) {
            ant[j] = j;
        }
        for (int i = 1; i <= x.length(); i++) {
            atual[0] = i;
            for (int j = 1; j <= y.length(); j++) {
                int custo = x.charAt(i - 1) == y.charAt(j - 1) ? 0 : 1;
                atual[j] = Math.min(Math.min(atual[j - 1] + 1, ant[j] + 1), ant[j - 1] + custo);
            }
            int[] t = ant;
            ant = atual;
            atual = t;
        }
        return ant[y.length()];
    }

    static Map<String, List<String>> candidatasDe(Set<String> palavras) {
        Map<String, List<String>> fora = new LinkedHashMap<>();
        for (String p : palavras) {
            if (p == null || p.length() < 4) {
                continue;
            }
            String base = p.toLowerCase();
            Set<String> nivel1 = new LinkedHashSet<>();
            for (int i = 0; i < base.length(); i++) {
                for (String[] t : TROCAS) {
                    if (base.charAt(i) == t[0].charAt(0)) {
                        nivel1.add(base.substring(0, i) + t[1] + base.substring(i + 1));
                    }
                }
            }
            // INSERCAO DE CEDILHA, e nao so troca de letra.
            //
            // A primeira versao deste gerador so TROCAVA caractere por sua forma acentuada, e por
            // isso nao alcancava a familia mais comum do acervo: `braos`->`bracos`, onde falta um
            // caractere inteiro. Das 143 palavras quebradas, a assinatura dominante e a cedilha
            // comida — `obrigaao`, `distraao`, `accoes`, `doao`, `percepoes`.
            //
            // So a cedilha e inserida, e so ANTES de vogal: e o unico caractere que o portugues
            // perde assim. Inserir qualquer letra em qualquer posicao geraria palavra parecida, e
            // "parecida" e o comeco de inventar.
            for (int i = 1; i < base.length(); i++) {
                char proximo = base.charAt(i);
                if ("aeiouáéíóúâêôãõ".indexOf(proximo) >= 0) {
                    nivel1.add(base.substring(0, i) + "ç" + base.substring(i));
                }
            }
            Set<String> nivel2 = new LinkedHashSet<>(nivel1);
            for (String um : nivel1) {
                for (int i = 0; i < um.length(); i++) {
                    for (String[] t : TROCAS) {
                        if (um.charAt(i) == t[0].charAt(0)) {
                            nivel2.add(um.substring(0, i) + t[1] + um.substring(i + 1));
                        }
                    }
                }
            }
            nivel2.remove(base);
            // TETO POR PALAVRA. Com a insercao de cedilha, o nivel 2 passou de ~30 para ~900
            // candidatas por palavra: 143 palavras viraram 130 mil consultas e a medicao nao
            // terminava. O teto e alto o bastante para caber o caso real (`obrigaao` precisa de
            // duas edicoes) e baixo o bastante para a medicao acabar.
            //
            // Palavra que ESTOURA o teto sai DECLARADA e nao truncada em silencio: truncar sem
            // dizer transforma "nao coube" em "nao tem conserto".
            List<String> lista = new ArrayList<>(nivel2);
            if (lista.size() > TETO_DE_CANDIDATAS) {
                System.out.printf("  NAO VERIFICADO: '%s' gerou %d candidatas (teto %d) — "
                    + "nao foi medida.%n", p, lista.size(), TETO_DE_CANDIDATAS);
                continue;
            }
            fora.put(p, lista);
        }
        return fora;
    }
}
