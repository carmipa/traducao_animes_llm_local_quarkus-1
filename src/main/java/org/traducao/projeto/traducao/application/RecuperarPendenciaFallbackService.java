package org.traducao.projeto.traducao.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.qualidadeTraducao.domain.LoreAtivaPort;
import org.traducao.projeto.traducao.domain.fallback.ProvedorFallback;
import org.traducao.projeto.traducao.domain.fallback.ResultadoFallback;
import org.traducao.projeto.traducao.domain.fallback.ResultadoRecuperacao;
import org.traducao.projeto.traducao.domain.fallback.StatusFallback;
import org.traducao.projeto.traducao.domain.ports.FallbackTraducaoMaquinaPort;
import org.traducao.projeto.traducao.infrastructure.config.FallbackOnlineProperties;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: recuperação de ÚLTIMO RECURSO das falas de diálogo que o LLM local não
 * conseguiu traduzir. Só age quando o modo está explicitamente ligado (opt-in) e apenas sobre as
 * pendências informadas desta execução; para cada uma consulta o tradutor de máquina (porta
 * própria da fatia) e só aceita a resposta se ela preservar os nomes próprios do original — a
 * validação canônica final (a mesma do LLM) fica a cargo do chamador.
 *
 * <p>Sucedeu o {@code RecuperarPendenciaGoogleService}: o nome citava um provedor específico,
 * mas o papel é o de orquestrar QUALQUER tradutor de máquina da cadeia (LibreTranslate local,
 * Google externo).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Modo desligado ou conjunto vazio ⇒ nenhuma chamada externa e resultado vazio.</li>
 *   <li>Nunca varre cache de execuções anteriores: opera só sobre as falas recebidas.</li>
 *   <li>Guarda de nomes próprios: um token capitalizado MID-SENTENCE (não início de frase,
 *       ≥3 letras) do original deve sobreviver na tradução; se sumir, a resposta é RECUSADA
 *       (a fala continua pendente). Recusar é seguro — equivale a manter o inglês, como sem o
 *       fallback.</li>
 *   <li>NENHUMA recusa é silenciosa: toda tentativa que não recupera é registrada em log com o
 *       provedor e a causa canônica ({@link StatusFallback}).</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Recusa da porta (rede/marcador corrompido) ou nome próprio perdido ⇒ a fala é simplesmente
 * omitida do resultado (permanece pendente). Nunca lança para o chamador.
 */
@Service
public class RecuperarPendenciaFallbackService {

    private static final Logger log = LoggerFactory.getLogger(RecuperarPendenciaFallbackService.class);

    private static final Pattern PADRAO_TAG = Pattern.compile("\\{[^{}]*}");
    private static final int TAMANHO_MINIMO_NOME = 3;

    /**
     * Ordinal inglês puro ({@code 12th}, {@code 1st}, {@code 08th}). Em português o sufixo cai e
     * sobra o número ("May 12th" → "12 de maio"), então exigir o token inteiro reprovaria uma
     * tradução correta; exige-se apenas a parte numérica.
     */
    private static final Pattern ORDINAL_INGLES = Pattern.compile("(?i)\\d+(st|nd|rd|th)");

    private final FallbackOnlineProperties propriedades;
    private final FallbackTraducaoMaquinaPort fallbackPort;
    private final LoreAtivaPort loreAtiva;

    /**
     * PROPÓSITO DE NEGÓCIO: compõe a recuperação com a flag opt-in, a porta própria da fatia e a
     * terminologia da obra ativa — que passou a ser a FONTE DE VERDADE da guarda, no lugar da
     * heurística de capitalização.
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede a criação do serviço.
     */
    public RecuperarPendenciaFallbackService(
        FallbackOnlineProperties propriedades,
        FallbackTraducaoMaquinaPort fallbackPort,
        LoreAtivaPort loreAtiva
    ) {
        this.propriedades = propriedades;
        this.fallbackPort = fallbackPort;
        this.loreAtiva = loreAtiva;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: expõe ao orquestrador se a recuperação está ligada, para que ele
     * possa NARRAR na saída dinâmica que caiu no tradutor de máquina — sem o use case precisar
     * depender de {@link FallbackOnlineProperties} (infra).
     *
     * <p>INVARIANTES DO DOMÍNIO: reflete fielmente a flag opt-in {@code ativo}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: leitura pura de configuração; não lança.
     */
    public boolean ativo() {
        return propriedades.ativo();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: tenta recuperar cada fala de diálogo pendente, devolvendo só as
     * candidatas que preservaram os nomes próprios do original.
     *
     * <p>INVARIANTES DO DOMÍNIO: sem efeito quando o modo está desligado; a ordem de iteração é
     * estável; cada candidata ainda passará pela validação canônica do chamador; toda recusa é
     * logada com provedor e causa.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: recusa da porta ou nome próprio perdido apenas omitem a
     * fala do resultado (permanece pendente).
     *
     * @param dialogosPendentes textos originais das falas de diálogo já dadas como pendentes
     * @return mapa original→tradução candidata (subconjunto do informado), possivelmente vazio
     */
    public ResultadoRecuperacao recuperar(Set<String> dialogosPendentes) {
        if (!propriedades.ativo() || dialogosPendentes == null || dialogosPendentes.isEmpty()) {
            return ResultadoRecuperacao.vazio();
        }
        Map<String, String> recuperadas = new LinkedHashMap<>();
        Map<StatusFallback, Integer> porCausa = new EnumMap<>(StatusFallback.class);
        Set<String> tokensDeLore = tokensProtegidos();

        for (String original : dialogosPendentes) {
            ResultadoFallback resultado = fallbackPort.traduzir(original);
            if (!resultado.recuperou()) {
                // A causa NUNCA é descartada: sem ela, uma pendência vira um número sem diagnóstico.
                porCausa.merge(resultado.status(), 1, Integer::sum);
                log.warn("Fallback [{}] {}: {} — fala mantida pendente: {}",
                    resultado.provedor(), resultado.status(), resultado.motivo(), original);
                continue;
            }
            String traduzido = resultado.traducao();
            String termoPerdido = termoProtegidoPerdido(original, traduzido, tokensDeLore);
            if (termoPerdido != null) {
                porCausa.merge(StatusFallback.GUARDA_LORE, 1, Integer::sum);
                log.warn("Fallback [{}] {}: termo protegido \"{}\" não sobreviveu — fala mantida pendente: {}",
                    resultado.provedor(), StatusFallback.GUARDA_LORE, termoPerdido, original);
                continue;
            }
            porCausa.merge(StatusFallback.RECUPERADA, 1, Integer::sum);
            recuperadas.put(original, traduzido);
        }
        return new ResultadoRecuperacao(recuperadas, porCausa);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: projeta os termos protegidos da obra ativa (que podem ser compostos,
     * como "Rygart Arrow" ou "Kingdom of Krisna") no conjunto de PALAVRAS que a guarda compara
     * token a token — a verificação é por token, então um termo composto precisa ser decomposto
     * para que cada parte relevante seja exigível.
     *
     * <p>INVARIANTES DO DOMÍNIO: comparação sem distinção de caixa (chaves em minúsculas);
     * partículas curtas (&lt;{@value #TAMANHO_MINIMO_NOME} letras, ex.: "of", "de") ficam de fora,
     * pois nunca são o que identifica um nome e ainda causariam falso-positivo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem contexto ativo, {@code termosProtegidosAtivos()}
     * devolve conjunto vazio e a guarda passa a exigir apenas siglas/identificadores — degrada
     * para menos restritiva, nunca para bloqueio total.
     */
    private Set<String> tokensProtegidos() {
        Set<String> tokens = new HashSet<>();
        for (String termo : loreAtiva.termosProtegidosAtivos()) {
            if (termo == null || termo.isBlank()) {
                continue;
            }
            for (String parte : termo.split("[^\\p{L}\\p{N}'-]+")) {
                if (parte.length() >= TAMANHO_MINIMO_NOME) {
                    tokens.add(parte.toLowerCase(Locale.ROOT));
                }
            }
        }
        return tokens;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: guarda de TERMINOLOGIA — impede que a tradução de máquina apague um
     * termo que a obra exige preservar (nome, facção, codinome), uma sigla ou um identificador
     * técnico. Devolve o primeiro termo perdido, para que o log diga QUAL termo reprovou em vez
     * de apenas "recusada".
     *
     * <p>Substituiu a heurística anterior, que tratava QUALQUER palavra capitalizada no meio da
     * frase como nome próprio obrigatório. Aquela regra tornava impossível traduzir um título em
     * Title Case — "The Battle in Three Dimensions" exigia que "Battle", "Three" e "Dimensions"
     * sobrevivessem em português — e recusava a resposta de qualquer provedor, o que nenhuma
     * troca de tradutor resolveria. Medição sobre as 560 falas pendentes reais dos caches
     * versionados: <b>323 (57,7%)</b> eram recusadas exclusivamente por esse falso-positivo,
     * contra apenas 35 (6,2%) que dependem de termo legítimo e continuam protegidas aqui.
     *
     * <p>INVARIANTES DO DOMÍNIO: só é exigido o token que (a) pertence à terminologia da obra
     * ativa, (b) é sigla/acrônimo em CAIXA ALTA com ≥2 caracteres, ou (c) é identificador
     * alfanumérico (contém dígito, como {@code RX-78} ou {@code 08th}). Capitalização comum
     * deixa de reprovar. A checagem de sobrevivência ignora caixa e respeita fronteiras de
     * palavra. Posição na frase deixou de importar: um termo de lore é exigido mesmo abrindo a
     * frase, e uma palavra comum não é exigida nem no meio dela.
     *
     * <p>TRADE-OFF DECLARADO: um nome próprio que NÃO esteja na lore da obra deixa de ser
     * protegido por esta guarda. É aceitável porque (1) as lores do projeto trazem rosters
     * completos e (2) a validação canônica a jusante — a mesma aplicada à saída do LLM —
     * continua rodando sobre a candidata e barra resíduo em inglês.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve o termo perdido (recusa segura, mantém a fala
     * pendente) ou {@code null} quando tudo que era exigido sobreviveu. Não lança.
     */
    private String termoProtegidoPerdido(String original, String traduzido, Set<String> tokensDeLore) {
        String semTags = PADRAO_TAG.matcher(original).replaceAll("")
            .replace("\\N", " ").replace("\\n", " ").replace("\\h", " ");
        for (String bruto : semTags.split("\\s+")) {
            if (bruto.isEmpty()) {
                continue;
            }
            String token = bruto.replaceAll("^[^\\p{L}\\p{N}]+", "").replaceAll("[^\\p{L}\\p{N}]+$", "");
            if (token.isEmpty() || !exigidoNaTraducao(token, tokensDeLore)) {
                continue;
            }
            // Ordinal em inglês ("12th", "1st") vira o número puro em português ("12 de maio"):
            // exigir o token inteiro reprovaria uma tradução correta. Exige-se só os dígitos.
            String exigido = ORDINAL_INGLES.matcher(token).matches()
                ? token.replaceAll("(?i)(st|nd|rd|th)$", "")
                : token;
            if (!sobrevive(exigido, traduzido)) {
                return token;
            }
        }
        return null;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se um token do original PRECISA aparecer na tradução — o
     * coração da guarda nova. Só três categorias obrigam: terminologia da obra, sigla e
     * identificador técnico.
     *
     * <p>INVARIANTES DO DOMÍNIO: (a) termo de lore, comparado sem distinção de caixa; (b) sigla
     * em CAIXA ALTA com ≥2 caracteres (evita exigir "A"/"I", que são artigo e pronome em inglês);
     * (c) token com dígito (identificador de mecha/data/unidade). Fora disso devolve
     * {@code false}, e a palavra pode ser legitimamente traduzida.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: token nulo/vazio devolve {@code false}; não lança.
     */
    private static boolean exigidoNaTraducao(String token, Set<String> tokensDeLore) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        if (tokensDeLore.contains(token.toLowerCase(Locale.ROOT))) {
            return true;
        }
        if (token.length() >= 2 && token.equals(token.toUpperCase(Locale.ROOT))
                && token.chars().anyMatch(Character::isLetter)) {
            return true; // sigla/acrônimo: MS, EFF, GM
        }
        return token.chars().anyMatch(Character::isDigit); // identificador: RX-78, 08th, 2148
    }

    /**
     * PROPÓSITO DE NEGÓCIO: verifica se um nome próprio do original aparece na tradução.
     * <p>INVARIANTES DO DOMÍNIO: comparação ignora caixa e exige fronteira alfanumérica.
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência devolve {@code false}.
     */
    private boolean sobrevive(String nome, String traduzido) {
        // Token PURAMENTE numérico usa fronteira só de dígito. O ordinal português ("1º",
        // "1ª") coloca um caractere que o Unicode classifica como LETRA logo após o número,
        // então a fronteira \p{L} rejeitaria "1" dentro de "1º" — e "September 1st" traduzido
        // como "1º de setembro" era recusado, mesmo estando correto.
        String fronteiraDireita = nome.chars().allMatch(Character::isDigit) ? "(?!\\p{N})" : "(?![\\p{L}\\p{N}])";
        Pattern ocorrencia = Pattern.compile(
            "(?iu)(?<![\\p{L}\\p{N}])" + Pattern.quote(nome) + fronteiraDireita);
        return ocorrencia.matcher(traduzido).find();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: identifica o provedor da cadeia em uso, para o orquestrador rotular
     * a narração e os contadores sem conhecer as classes concretas de infraestrutura.
     * <p>INVARIANTES DO DOMÍNIO: delega à porta; nunca {@code null}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public ProvedorFallback provedorAtivo() {
        return fallbackPort.provedor();
    }
}
