package org.traducao.projeto.revisaoLore.infrastructure.adapters;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: extrai a fala revisada final das respostas do LLM de
 * lore, que podem vir com raciocínio ({@code <think>}), cerca Markdown ou um
 * rótulo antes do texto. Responsabilidade separada do adapter, própria da
 * Revisão de Lore — não reutiliza o normalizador da Tradução Local.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Quando a tradução possui marcadores {@code [[TAGn]]}, apenas uma linha que
 *       preserve TODOS eles pode ser escolhida; explicações nunca são concatenadas
 *       ao texto da legenda.</li>
 *   <li>A lista de marcadores preserva ordem e elimina duplicações.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem linha utilizável que preserve os marcadores esperados, devolve texto vazio,
 * permitindo nova tentativa sem publicar conteúdo estruturalmente incompleto.
 */
@Component
public class NormalizadorRespostaRevisaoLore {

    private static final Pattern BLOCO_RACIOCINIO = Pattern.compile("(?is)<think>.*?</think>");
    private static final Pattern MARCADOR_TAG = Pattern.compile("\\[\\[TAG\\d+]]");
    private static final Pattern PREFIXO_RESPOSTA = Pattern.compile(
        "(?i)^(?:tradu[cç][aã]o(?: corrigida)?|resposta|pt-br|texto corrigido)\\s*:\\s*");

    /**
     * PROPÓSITO DE NEGÓCIO: escolhe a linha final revisada, limpando raciocínio,
     * cerca Markdown e rótulos, e garantindo a preservação dos marcadores.
     * <p>INVARIANTES DO DOMÍNIO: com marcadores esperados, só uma linha que os
     * contenha todos é aceita; sem marcadores, usa a última linha útil.
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@code ""} quando nenhuma linha
     * preserva os marcadores esperados.
     */
    public String normalizarLinhaUnica(String texto, List<String> marcadoresEsperados) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String normalizado = texto.replace("\r\n", "\n").replace('\r', '\n').strip();
        normalizado = BLOCO_RACIOCINIO.matcher(normalizado).replaceAll("").strip();
        if (normalizado.startsWith("```") && normalizado.endsWith("```")) {
            normalizado = removerCercaMarkdown(normalizado).strip();
        }

        List<String> candidatas = normalizado.lines()
            .map(String::strip)
            .filter(linha -> !linha.isBlank())
            .filter(linha -> !linha.equalsIgnoreCase("<think>") && !linha.equalsIgnoreCase("</think>"))
            .map(linha -> PREFIXO_RESPOSTA.matcher(linha).replaceFirst("").strip())
            .filter(linha -> !linha.isBlank())
            .toList();
        if (candidatas.isEmpty()) {
            return "";
        }

        List<String> esperados = marcadoresEsperados == null
            ? List.of() : marcadoresEsperados.stream().distinct().toList();
        for (int i = candidatas.size() - 1; i >= 0; i--) {
            String candidata = candidatas.get(i);
            if (esperados.stream().allMatch(candidata::contains)) {
                return candidata;
            }
        }
        return esperados.isEmpty() ? candidatas.getLast() : "";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: identifica os marcadores estruturais presentes na
     * tradução mascarada para orientar a seleção segura da linha respondida.
     * <p>INVARIANTES DO DOMÍNIO: preserva ordem e elimina duplicações.
     * <p>COMPORTAMENTO EM CASO DE FALHA: tradução ausente devolve lista vazia.
     */
    public List<String> extrairMarcadores(String textoMascarado) {
        if (textoMascarado == null || textoMascarado.isBlank()) {
            return List.of();
        }
        Set<String> marcadores = new LinkedHashSet<>();
        Matcher matcher = MARCADOR_TAG.matcher(textoMascarado);
        while (matcher.find()) {
            marcadores.add(matcher.group());
        }
        return List.copyOf(marcadores);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: remove o invólucro de cerca Markdown de uma resposta.
     * <p>INVARIANTES DO DOMÍNIO: exige abertura, quebra inicial e fechamento válidos;
     * o conteúdo permanece intacto.
     * <p>COMPORTAMENTO EM CASO DE FALHA: formato incompleto é devolvido sem corte.
     */
    private String removerCercaMarkdown(String texto) {
        int primeiraQuebra = texto.indexOf('\n');
        int ultimaCerca = texto.lastIndexOf("```");
        if (primeiraQuebra < 0 || ultimaCerca <= primeiraQuebra) {
            return texto;
        }
        return texto.substring(primeiraQuebra + 1, ultimaCerca);
    }
}
