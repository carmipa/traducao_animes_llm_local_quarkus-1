package org.traducao.projeto.traducao.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.traducao.domain.ports.FallbackTraducaoOnlinePort;
import org.traducao.projeto.traducao.infrastructure.config.FallbackOnlineProperties;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: recuperação de ÚLTIMO RECURSO das falas de diálogo que o LLM local
 * não conseguiu traduzir. Só age quando o modo online está explicitamente ligado (opt-in) e
 * apenas sobre as pendências informadas desta execução; para cada uma, tenta o provedor
 * externo (porta própria da fatia) e só aceita a resposta se ela preservar os nomes próprios
 * do original — a validação canônica final (mesma do LLM) fica a cargo do chamador.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Modo desligado ou conjunto vazio ⇒ nenhuma chamada externa e resultado vazio.</li>
 *   <li>Nunca varre cache de execuções anteriores: opera só sobre as falas recebidas.</li>
 *   <li>Guarda de nomes próprios: um token capitalizado MID-SENTENCE (não início de frase,
 *       ≥3 letras) do original deve sobreviver na tradução; se sumir, a resposta é RECUSADA
 *       (a fala continua pendente). Recusar é seguro — equivale a manter o inglês, como sem
 *       o fallback.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Porta vazia (rede/marcador corrompido) ou nome próprio perdido ⇒ a fala é simplesmente
 * omitida do resultado (permanece pendente). Nunca lança para o chamador.
 */
@Service
public class RecuperarPendenciaGoogleService {

    private static final Logger log = LoggerFactory.getLogger(RecuperarPendenciaGoogleService.class);

    private static final Pattern PADRAO_TAG = Pattern.compile("\\{[^{}]*}");
    private static final int TAMANHO_MINIMO_NOME = 3;

    private final FallbackOnlineProperties propriedades;
    private final FallbackTraducaoOnlinePort fallbackPort;

    /**
     * PROPÓSITO DE NEGÓCIO: compõe a recuperação online com a flag opt-in e a porta própria.
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede a criação do serviço.
     */
    public RecuperarPendenciaGoogleService(
        FallbackOnlineProperties propriedades,
        FallbackTraducaoOnlinePort fallbackPort
    ) {
        this.propriedades = propriedades;
        this.fallbackPort = fallbackPort;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: expõe ao orquestrador se a recuperação online está ligada,
     * para que ele possa NARRAR na saída dinâmica que caiu no scraping do Google — sem
     * o use case precisar depender de {@link FallbackOnlineProperties} (infra).
     *
     * <p>INVARIANTES DO DOMÍNIO: reflete fielmente a flag opt-in {@code ativo}; não decide
     * nada por conta própria.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: leitura pura de configuração; não lança.
     */
    public boolean ativo() {
        return propriedades.ativo();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: tenta recuperar online cada fala de diálogo pendente, devolvendo
     * só as candidatas que preservaram os nomes próprios do original.
     *
     * <p>INVARIANTES DO DOMÍNIO: sem efeito quando o modo está desligado; a ordem de iteração
     * é estável; cada candidata ainda passará pela validação canônica do chamador.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: falha de rede, marcador corrompido ou nome próprio
     * perdido apenas omitem a fala do resultado (permanece pendente).
     *
     * @param dialogosPendentes textos originais das falas de diálogo já dadas como pendentes
     * @return mapa original→tradução candidata (subconjunto do informado), possivelmente vazio
     */
    public Map<String, String> recuperar(Set<String> dialogosPendentes) {
        if (!propriedades.ativo() || dialogosPendentes == null || dialogosPendentes.isEmpty()) {
            return Map.of();
        }
        Map<String, String> recuperadas = new LinkedHashMap<>();
        for (String original : dialogosPendentes) {
            Optional<String> candidata = fallbackPort.traduzir(original);
            if (candidata.isEmpty()) {
                continue;
            }
            String traduzido = candidata.get();
            if (!nomesPropriosPreservados(original, traduzido)) {
                log.warn("Fallback Google: nome próprio não preservado — fala mantida pendente: {}", original);
                continue;
            }
            recuperadas.put(original, traduzido);
        }
        return recuperadas;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: guarda heurística de nomes próprios sem depender da lore — evita
     * que a tradução externa apague nomes que aparecem capitalizados no meio da frase.
     *
     * <p>INVARIANTES DO DOMÍNIO: ignora o token inicial de cada frase (capital de início não é
     * nome); só considera tokens com ≥{@value #TAMANHO_MINIMO_NOME} letras iniciados por
     * maiúscula; a checagem de sobrevivência ignora caixa e respeita fronteiras.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer nome próprio candidato ausente na tradução
     * devolve {@code false} (recusa segura — mantém a fala pendente).
     */
    private boolean nomesPropriosPreservados(String original, String traduzido) {
        String semTags = PADRAO_TAG.matcher(original).replaceAll("")
            .replace("\\N", " ").replace("\\n", " ").replace("\\h", " ");
        String[] tokens = semTags.split("\\s+");
        boolean inicioDeFrase = true;
        for (String bruto : tokens) {
            if (bruto.isEmpty()) {
                continue;
            }
            String token = bruto.replaceAll("^[^\\p{L}]+", "").replaceAll("[^\\p{L}]+$", "");
            boolean terminaFrase = bruto.matches(".*[.!?][\"')\\]]*$");
            if (!token.isEmpty()) {
                boolean candidatoNome = token.length() >= TAMANHO_MINIMO_NOME
                    && Character.isUpperCase(token.codePointAt(0))
                    && !inicioDeFrase;
                if (candidatoNome && !sobrevive(token, traduzido)) {
                    return false;
                }
                inicioDeFrase = terminaFrase;
            } else if (terminaFrase) {
                // Token de pontuação pura que encerra a frase (ex.: " . "): a próxima palavra
                // é início de frase e não pode ser confundida com nome próprio obrigatório.
                inicioDeFrase = true;
            }
        }
        return true;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: verifica se um nome próprio do original aparece na tradução.
     * <p>INVARIANTES DO DOMÍNIO: comparação ignora caixa e exige fronteira alfanumérica.
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência devolve {@code false}.
     */
    private boolean sobrevive(String nome, String traduzido) {
        Pattern ocorrencia = Pattern.compile(
            "(?iu)(?<![\\p{L}\\p{N}])" + Pattern.quote(nome) + "(?![\\p{L}\\p{N}])");
        return ocorrencia.matcher(traduzido).find();
    }
}
