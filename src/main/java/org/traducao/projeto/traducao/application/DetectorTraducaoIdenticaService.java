package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Decide se uma fala pode legitimamente permanecer idêntica ao original (nomes
 * próprios, números, siglas, termos de lore) ou se a igualdade é sinal de que o
 * LLM simplesmente devolveu a fala sem traduzir. Além da lista global fixa,
 * consulta os termos protegidos do lore ATIVO ({@link GerenciadorContexto}),
 * para que um termo novo anexado ao contexto selecionado seja protegido sem
 * precisar editar este detector.
 *
 * <p>PROPÓSITO DE NEGÓCIO: impedir que manutenção ou retomada do cache apague
 * nomes canônicos e, simultaneamente, não aceite frases inglesas como tradução.
 * <p>INVARIANTES DO DOMÍNIO: a lore ativa é a fonte dos termos protegidos;
 * expressões conversacionais comuns continuam exigindo tradução.
 * <p>COMPORTAMENTO EM CASO DE FALHA: texto sem evidência suficiente é preservado
 * para evitar uma decisão destrutiva.
 */
@Service
public class DetectorTraducaoIdenticaService {

    private static final Pattern PADRAO_REMOVE_TAGS_ASS = Pattern.compile("\\{[^}]+}");
    private static final Pattern PADRAO_GAGUEIRA_NOME = Pattern.compile(
        "(?iu)(?<![\\p{L}\\p{N}])([\\p{L}])-(?=\\1[\\p{L}])");

    private final GerenciadorContexto gerenciadorContexto;

    public DetectorTraducaoIdenticaService(GerenciadorContexto gerenciadorContexto) {
        this.gerenciadorContexto = gerenciadorContexto;
    }

    private static final Set<String> PALAVRAS_INGLES_COMUNS = Set.of(
        "hello", "hi", "hey", "goodbye", "bye", "yes", "no", "yeah", "yep", "nope",
        "thanks", "thank", "sorry", "please", "wait", "stop", "go", "come", "run",
        "what", "why", "who", "where", "when", "how", "right", "okay", "ok", "fine",
        "good", "morning", "night", "help", "me", "you", "away", "back", "welcome"
    );

    /**
     * PROPÓSITO DE NEGÓCIO: decide se um conteúdo idêntico pode permanecer no
     * cache por ser nome, sigla, número ou termo canônico da lore.
     *
     * <p>INVARIANTES DO DOMÍNIO: expressões conversacionais em inglês não são
     * protegidas só por estarem em Title Case; termo exato da lore tem prioridade.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo/sem base suficiente é
     * preservado para evitar limpeza destrutiva por heurística fraca.
     */
    public boolean deveManterIdentico(String texto) {
        if (texto == null) {
            return true;
        }
        String textoLimpo = PADRAO_REMOVE_TAGS_ASS.matcher(texto).replaceAll("").strip();
        textoLimpo = removerGagueiraDeNome(textoLimpo);
        textoLimpo = textoLimpo.replaceAll("[^\\w\\s\\d]", "").strip();

        // Um único caractere visível (letra de karaokê por letra, interjeição
        // "A", numeral) não dá base para julgar tradução — manter idêntico.
        if (textoLimpo.length() <= 1) {
            return true;
        }

        String[] palavras = textoLimpo.split("\\s+");
        if (palavras.length == 1) {
            return deveManterPalavraUnicaIdentica(textoLimpo);
        }

        String minusculo = textoLimpo.toLowerCase(Locale.ROOT);
        if (termoDoLoreAtivo(minusculo)) {
            return true;
        }
        if (palavras.length >= 2 && palavras.length <= 4
            && java.util.Arrays.stream(palavras)
                .allMatch(p -> !p.isBlank() && Character.isUpperCase(p.charAt(0)))) {
            return java.util.Arrays.stream(palavras)
                .map(p -> p.toLowerCase(Locale.ROOT))
                .noneMatch(PALAVRAS_INGLES_COMUNS::contains);
        }
        return false;
    }

    /**
     * true se o termo (já em minúsculas) constar dos termos protegidos do lore
     * atualmente selecionado — permite proteger nomes/facções do contexto ativo
     * sem depender só da lista global fixa neste detector.
     */
    /**
     * PROPÓSITO DE NEGÓCIO: reconhece nomes e terminologia diretamente na lore
     * ativa, eliminando listas hardcoded específicas de DanMachi/Gundam.
     *
     * <p>INVARIANTES DO DOMÍNIO: comparação exige termo inteiro; termos
     * declarados explicitamente pelo provedor continuam tendo prioridade.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lore vazia ou termo vazio retorna falso.
     */
    private boolean termoDoLoreAtivo(String termoMinusculo) {
        for (String termo : gerenciadorContexto.termosProtegidosAtivos()) {
            if (termo != null && termo.toLowerCase(Locale.ROOT).equals(termoMinusculo)) {
                return true;
            }
        }
        String lore = gerenciadorContexto.obterLoreAtiva();
        if (lore == null || lore.isBlank() || termoMinusculo == null || termoMinusculo.isBlank()) {
            return false;
        }
        Pattern termoInteiro = Pattern.compile(
            "(?iu)(?<![\\p{L}\\p{N}])" + Pattern.quote(termoMinusculo) + "(?![\\p{L}\\p{N}])");
        return termoInteiro.matcher(lore.toLowerCase(Locale.ROOT)).find();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: diferencia um nome próprio isolado de uma palavra
     * inglesa comum que ainda precisa de tradução.
     * <p>INVARIANTES DO DOMÍNIO: números, siglas, lore e nomes capitalizados são
     * preservados; vocabulário conversacional cadastrado nunca vira nome.
     * <p>COMPORTAMENTO EM CASO DE FALHA: evidência insuficiente mantém o termo
     * capitalizado para evitar retradução destrutiva de personagem.
     */
    private boolean deveManterPalavraUnicaIdentica(String textoLimpo) {
        if (textoLimpo.matches("\\d+")) {
            return true;
        }
        if (textoLimpo.length() > 1 && textoLimpo.equals(textoLimpo.toUpperCase())) {
            return true;
        }

        String minusculo = textoLimpo.toLowerCase(Locale.ROOT);
        if (PALAVRAS_INGLES_COMUNS.contains(minusculo)) {
            return false;
        }

        return termoDoLoreAtivo(minusculo)
            || (textoLimpo.length() >= 3 && Character.isUpperCase(textoLimpo.charAt(0)));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: normaliza hesitações escritas antes de reconhecer um
     * nome próprio, permitindo que `E-Eledore` seja comparado como `Eledore`.
     *
     * <p>INVARIANTES DO DOMÍNIO: o prefixo só é removido quando a letra após o
     * hífen repete a letra hesitada; palavras legítimas como `X-ray` permanecem.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo não é esperado pelo chamador;
     * texto sem o padrão é devolvido integralmente.
     */
    private String removerGagueiraDeNome(String texto) {
        return PADRAO_GAGUEIRA_NOME.matcher(texto).replaceAll("");
    }

    /**
     * true quando a "tradução" só repete o original em inglês (ignorando tags ASS
     * e quebras de linha) e isso não é um caso legítimo de nome/número/termo de
     * lore — ou seja, a fala provavelmente nunca foi traduzida de fato.
     */
    public boolean pareceNaoTraduzida(String original, String traduzido) {
        if (original == null || traduzido == null) {
            return false;
        }
        String o = normalizar(original);
        String t = normalizar(traduzido);
        if (o.isEmpty() || !o.equals(t)) {
            return false;
        }
        return !deveManterIdentico(original);
    }

    private String normalizar(String texto) {
        return PADRAO_REMOVE_TAGS_ASS.matcher(texto).replaceAll("")
            .replace("\\N", " ")
            .replace("\\n", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }
}
