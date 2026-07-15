package org.traducao.projeto.raspagemCorrecao.application;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: impede que a contingência Google traduza literalmente
 * nomes e terminologia que a lore manda manter na forma oficial.
 *
 * <p>INVARIANTES DO DOMÍNIO: termos maiores são mascarados antes dos menores;
 * a grafia encontrada no original é restaurada; marcadores nunca podem sobrar.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: restauração incompleta devolve
 * {@code null}, fazendo o chamador manter a entrada pendente.
 */
@Service
public class ProtetorTermosLoreService {

    private static final Pattern LINHA_MANTER = Pattern.compile(
        "(?im)^\\s*-\\s*Manter sempre[^:]*:\\s*(.+)$");
    private static final Pattern LINHA_PERSONAGEM = Pattern.compile(
        "(?im)^\\s*-\\s*([^:\\r\\n]{2,100}?)\\s*\\((?:homem|mulher|masculino|feminino)[^)]*\\)\\s*:");
    private static final Pattern LINHA_CATALOGO_PERSONAGENS = Pattern.compile(
        "(?im)^\\s*-\\s*Personagens?\\s*:\\s*(.+)$");
    private static final Pattern NOMES_OFICIAIS = Pattern.compile(
        "(?iu)\\\"([^\\\"]{2,80})\\\"(?:\\s+e\\s+\\\"([^\\\"]{2,80})\\\")?\\s+s(?:a|ã)o nomes oficiais");

    /**
     * PROPÓSITO DE NEGÓCIO: transporta o texto seguro para tradução e o mapa
     * necessário para recompor os termos oficiais depois da resposta externa.
     *
     * <p>INVARIANTES DO DOMÍNIO: marcadores são únicos por ocorrência textual.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: record imutável, sem efeitos colaterais.
     */
    public record TextoProtegido(String textoMascarado, Map<String, String> termosPorMarcador) {}

    /**
     * PROPÓSITO DE NEGÓCIO: mascara no texto os termos explícitos do provedor e
     * os declarados na regra “Manter sempre” da lore ativa.
     *
     * <p>INVARIANTES DO DOMÍNIO: comparação ignora caixa, respeita fronteiras
     * alfanuméricas e preserva exatamente a forma encontrada na fala.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto/lore vazios resultam no texto
     * original com mapa vazio.
     */
    public TextoProtegido mascarar(String texto, String lore, Set<String> termosExplicitos) {
        if (texto == null || texto.isBlank()) {
            return new TextoProtegido(texto, Map.of());
        }
        List<String> termos = extrairTermos(lore, termosExplicitos);
        String resultado = texto;
        Map<String, String> mapa = new LinkedHashMap<>();
        int sequencia = 0;
        for (String termo : termos) {
            Pattern ocorrencia = Pattern.compile(
                "(?iu)(?<![\\p{L}\\p{N}])" + Pattern.quote(termo) + "(?![\\p{L}\\p{N}])");
            Matcher matcher = ocorrencia.matcher(resultado);
            StringBuffer substituido = new StringBuffer();
            boolean encontrou = false;
            while (matcher.find()) {
                encontrou = true;
                String marcador = "ZXQLORE" + sequencia++ + "QXZ";
                mapa.put(marcador, matcher.group());
                matcher.appendReplacement(substituido, Matcher.quoteReplacement(marcador));
            }
            if (encontrou) {
                matcher.appendTail(substituido);
                resultado = substituido.toString();
            }
        }
        return new TextoProtegido(resultado, Map.copyOf(mapa));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: restaura os termos canônicos na tradução externa.
     *
     * <p>INVARIANTES DO DOMÍNIO: todos os marcadores precisam sobreviver; nenhum
     * marcador técnico pode aparecer na legenda final.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@code null} quando o Google
     * remove ou mutila qualquer marcador.
     */
    public String restaurar(String traduzido, TextoProtegido protegido) {
        if (traduzido == null) return null;
        String restaurado = traduzido;
        for (Map.Entry<String, String> item : protegido.termosPorMarcador().entrySet()) {
            if (!restaurado.contains(item.getKey())) {
                return null;
            }
            restaurado = restaurado.replace(item.getKey(), item.getValue());
        }
        return restaurado.matches("(?i).*ZXQLORE\\d+QXZ.*") ? null : restaurado;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: identifica nomes e termos oficiais presentes no
     * original que uma proposta externa removeu, traduziu ou alterou.
     *
     * <p>INVARIANTES DO DOMÍNIO: a comparação ignora caixa, mas exige a grafia
     * canônica completa e respeita fronteiras alfanuméricas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: proposta nula é considerada violação
     * de todos os termos canônicos encontrados; ausência de termos retorna lista vazia.
     */
    public List<String> termosCanonicosAlterados(
        String original,
        String proposta,
        String lore,
        Set<String> termosExplicitos
    ) {
        TextoProtegido protegido = mascarar(original, lore, termosExplicitos);
        Set<String> termosOriginais = new LinkedHashSet<>(protegido.termosPorMarcador().values());
        if (termosOriginais.isEmpty()) return List.of();
        if (proposta == null) return List.copyOf(termosOriginais);

        List<String> alterados = new ArrayList<>();
        for (String termo : termosOriginais) {
            Pattern ocorrencia = Pattern.compile(
                "(?iu)(?<![\\p{L}\\p{N}])" + Pattern.quote(termo) + "(?![\\p{L}\\p{N}])");
            if (!ocorrencia.matcher(proposta).find()) alterados.add(termo);
        }
        return List.copyOf(alterados);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reconhece vocativos e referências formados apenas
     * por nomes oficiais, evitando enviar `Jona!`, `Rita...` ou `Narrative?` à IA.
     *
     * <p>INVARIANTES DO DOMÍNIO: exige ao menos um termo da lore; tags ASS,
     * espaços e pontuação podem cercar o nome, mas nenhuma outra palavra é aceita.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto vazio ou sem termo protegido
     * retorna {@code false} e segue para a auditoria normal.
     */
    public boolean contemSomenteTermosCanonicos(
        String texto,
        String lore,
        Set<String> termosExplicitos
    ) {
        TextoProtegido protegido = mascarar(texto, lore, termosExplicitos);
        if (protegido.termosPorMarcador().isEmpty()) return false;
        String restante = protegido.textoMascarado()
            .replaceAll("(?i)ZXQLORE\\d+QXZ", "")
            .replaceAll("\\{[^{}]*}", "")
            .replaceAll("[\\p{P}\\p{S}\\s]+", "");
        return restante.isEmpty();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte regras textuais, catálogo de personagens
     * e declarações de nomes oficiais da lore em glossário operacional sem
     * exigir listas hardcoded por anime.
     *
     * <p>INVARIANTES DO DOMÍNIO: remove duplicatas sem diferenciar caixa e ordena
     * por tamanho decrescente para evitar mascaramento parcial.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: linhas desconhecidas são ignoradas e a
     * proteção continua com os termos que puderam ser extraídos com segurança.
     */
    private List<String> extrairTermos(String lore, Set<String> termosExplicitos) {
        Map<String, String> unicos = new LinkedHashMap<>();
        if (termosExplicitos != null) {
            termosExplicitos.stream().filter(t -> t != null && !t.isBlank())
                .forEach(t -> unicos.putIfAbsent(t.toLowerCase(Locale.ROOT), t.strip()));
        }
        if (lore != null) {
            Matcher linhas = LINHA_MANTER.matcher(lore);
            while (linhas.find()) {
                adicionarLista(unicos, linhas.group(1));
            }

            Matcher personagens = LINHA_PERSONAGEM.matcher(lore);
            while (personagens.find()) {
                adicionarPersonagens(unicos, personagens.group(1));
            }

            Matcher catalogo = LINHA_CATALOGO_PERSONAGENS.matcher(lore);
            while (catalogo.find()) {
                adicionarListaDePersonagens(unicos, catalogo.group(1));
            }

            Matcher oficiais = NOMES_OFICIAIS.matcher(lore);
            while (oficiais.find()) {
                adicionarTermo(unicos, oficiais.group(1));
                adicionarTermo(unicos, oficiais.group(2));
            }
        }
        List<String> termos = new ArrayList<>(new LinkedHashSet<>(unicos.values()));
        termos.sort(Comparator.comparingInt(String::length).reversed());
        return termos;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: adiciona uma lista de termos canônicos separada por
     * vírgulas ao glossário de proteção.
     *
     * <p>INVARIANTES DO DOMÍNIO: pontuação terminal não integra o termo e itens
     * acima de 80 caracteres não são considerados entidades canônicas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista nula ou vazia não altera o mapa.
     */
    private void adicionarLista(Map<String, String> unicos, String lista) {
        if (lista == null || lista.isBlank()) return;
        for (String bruto : lista.split(",")) {
            adicionarTermo(unicos, bruto);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege nomes completos e o nome curto usado nos
     * diálogos, como “Jona Basta” e “Jona”.
     *
     * <p>INVARIANTES DO DOMÍNIO: aliases separados por barra são independentes;
     * somente primeiro token iniciado por maiúscula vira nome curto.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: descrição sem nome válido é ignorada.
     */
    private void adicionarPersonagens(Map<String, String> unicos, String descricao) {
        if (descricao == null || descricao.isBlank()) return;
        for (String alias : descricao.split("/")) {
            String nome = alias.strip();
            adicionarTermo(unicos, nome);
            String[] partes = nome.split("\\s+");
            if (partes.length > 0 && partes[0].length() > 2
                && Character.isUpperCase(partes[0].codePointAt(0))) {
                adicionarTermo(unicos, partes[0]);
            }
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: interpreta o catálogo compacto “Personagens:” das
     * lores de revisão e reaproveita seus nomes no fallback Google.
     *
     * <p>INVARIANTES DO DOMÍNIO: observações entre parênteses são removidas e
     * cada item separado por vírgula é tratado como personagem.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: catálogo vazio não altera o glossário.
     */
    private void adicionarListaDePersonagens(Map<String, String> unicos, String lista) {
        if (lista == null || lista.isBlank()) return;
        for (String bruto : lista.split(",")) {
            adicionarPersonagens(unicos, bruto.replaceAll("\\s*\\([^)]*\\)\\s*$", ""));
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: normaliza e inclui uma entidade canônica sem perder
     * a grafia definida pela lore.
     *
     * <p>INVARIANTES DO DOMÍNIO: deduplicação ignora caixa; termos vazios,
     * excessivamente longos ou pontuação isolada são rejeitados.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada inválida é simplesmente ignorada.
     */
    private void adicionarTermo(Map<String, String> unicos, String bruto) {
        if (bruto == null) return;
        String termo = bruto.strip().replaceFirst("[.;]$", "").strip();
        if (!termo.isBlank() && termo.length() <= 80) {
            unicos.putIfAbsent(termo.toLowerCase(Locale.ROOT), termo);
        }
    }
}
