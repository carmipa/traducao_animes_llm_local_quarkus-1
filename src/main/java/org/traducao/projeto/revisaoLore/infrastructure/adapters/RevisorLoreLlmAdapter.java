package org.traducao.projeto.revisaoLore.infrastructure.adapters;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.StatusRevisaoLoreLlm;
import org.traducao.projeto.revisaoLore.domain.ports.RevisorLoreLlmPort;
import org.traducao.projeto.revisaoLore.infrastructure.config.RevisaoLoreLlmProperties;
import org.traducao.projeto.revisaoLore.infrastructure.dtos.RevisaoLoreLlmDtos.ChatRequest;
import org.traducao.projeto.revisaoLore.infrastructure.dtos.RevisaoLoreLlmDtos.ListaModelos;
import org.traducao.projeto.revisaoLore.infrastructure.dtos.RevisaoLoreLlmDtos.ListaModelosV0;
import org.traducao.projeto.revisaoLore.infrastructure.dtos.RevisaoLoreLlmDtos.Mensagem;
import org.traducao.projeto.revisaoLore.infrastructure.dtos.RevisaoLoreLlmDtos.ModeloDisponivel;
import org.traducao.projeto.revisaoLore.infrastructure.dtos.RevisaoLoreLlmDtos.ModeloDisponivelV0;
import org.traducao.projeto.revisaoLore.infrastructure.dtos.RevisaoLoreLlmDtos.RespostaLlm;
import org.traducao.projeto.revisaoLore.infrastructure.http.RevisaoLoreHttpClient;
import org.traducao.projeto.revisaoLore.infrastructure.http.RevisaoLoreHttpClient.HttpClientException;

import java.util.List;
import java.util.Optional;

/**
 * PROPÓSITO DE NEGÓCIO: adapter LLM próprio da Revisão de Lore. Implementa a
 * única interação com o modelo local de que a fatia precisa — checar
 * disponibilidade e revisar a terminologia de lore de uma fala —, replicando o
 * comportamento efetivo anterior (antes exposto por {@code LlmPort.revisarLore})
 * sem depender da stack LLM da Tradução Local.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A revisão usa o prompt de sistema de lore recebido e o prompt de usuário
 *       de {@link PromptRevisaoLore}, com temperatura fixa de revisão (0.15).</li>
 *   <li>No máximo {@value #MAX_TENTATIVAS_REVISAO} tentativas; erro HTTP permanente
 *       (4xx exceto 408/429) não é repetido.</li>
 *   <li>Só publica uma linha que preserve todos os marcadores {@code [[TAGn]]}.</li>
 *   <li>Nenhuma responsabilidade de tradução de lotes ou correção gramatical vive aqui.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Resposta inválida ou sem linha utilizável devolve {@link Optional#empty()} após
 * esgotar as tentativas. Interrupção durante a pausa entre tentativas restaura o
 * flag de interrupção e encerra o laço. {@link #verificarDisponibilidade} nunca
 * lança: reporta indisponibilidade via {@link StatusRevisaoLoreLlm}.
 */
@Component
public class RevisorLoreLlmAdapter implements RevisorLoreLlmPort {

    private static final Logger log = LoggerFactory.getLogger(RevisorLoreLlmAdapter.class);

    private static final int MAX_TENTATIVAS_REVISAO = 2;
    private static final double TEMPERATURA_REVISAO = 0.15;

    private final RevisaoLoreLlmProperties propriedades;
    private final ObjectMapper objectMapper;
    private final NormalizadorRespostaRevisaoLore normalizador;
    private final RevisaoLoreHttpClient httpClient;
    private final long pausaEntreTentativasMs;

    public RevisorLoreLlmAdapter(
        RevisaoLoreLlmProperties propriedades,
        ObjectMapper objectMapper,
        NormalizadorRespostaRevisaoLore normalizador
    ) {
        this.propriedades = propriedades;
        this.objectMapper = objectMapper;
        this.normalizador = normalizador;
        this.httpClient = new RevisaoLoreHttpClient(
            propriedades.connectTimeout(), propriedades.baseUrl(), propriedades.readTimeout(), objectMapper);
        this.pausaEntreTentativasMs = propriedades.pausaEntreTentativas().toMillis();
    }

    @Override
    public StatusRevisaoLoreLlm verificarDisponibilidade() {
        try {
            // 1. Fonte de verdade: /api/v0/models da LM Studio informa "state"
            // ("loaded"/"not-loaded"), diferente de /v1/models que só lista o catálogo.
            List<String> carregados = buscarModelosCarregadosViaApiEstendida();
            if (!carregados.isEmpty()) {
                String escolhido = escolherEntreCarregados(carregados);
                propriedades.setModel(escolhido);
                String msg = carregados.size() == 1
                    ? "Servidor LLM online e modelo \"" + escolhido + "\" carregado em memória."
                    : "Servidor LLM online; " + carregados.size() + " modelos carregados, usando \"" + escolhido + "\".";
                return new StatusRevisaoLoreLlm(true, true, msg);
            }

            // 2. Sem API estendida: cai para /v1/models, que NÃO confirma o carregado.
            // Só adota o modelo CONFIGURADO se ele aparecer no catálogo.
            ListaModelos resposta = httpClient.get("/models", ListaModelos.class);
            List<ModeloDisponivel> modelos = resposta != null ? resposta.data() : null;
            if (modelos == null || modelos.isEmpty()) {
                return new StatusRevisaoLoreLlm(true, false,
                    "Servidor LLM em " + propriedades.baseUrl() + " respondeu, mas nenhum modelo está carregado em memória.");
            }

            String modeloConfigurado = propriedades.model();
            Optional<String> configuradoNoCatalogo = modelos.stream()
                .map(ModeloDisponivel::id)
                .filter(id -> id != null)
                .filter(id -> combinaComConfigurado(id, modeloConfigurado))
                .findFirst();

            if (configuradoNoCatalogo.isPresent()) {
                String modelo = configuradoNoCatalogo.get();
                propriedades.setModel(modelo);
                log.warn("Load-state não confirmado por este servidor; usando o modelo configurado \"{}\" achado no catálogo /v1/models.", modelo);
                return new StatusRevisaoLoreLlm(true, true,
                    "Servidor LLM online. Modelo configurado \"" + modelo + "\" encontrado no catálogo (o servidor não confirma qual está carregado).");
            }

            return new StatusRevisaoLoreLlm(true, false,
                "Servidor LLM online, mas não foi possível confirmar qual modelo está carregado e o modelo configurado \""
                + modeloConfigurado + "\" não está no catálogo. Carregue o modelo no LM Studio e tente novamente.");
        } catch (Exception e) {
            return new StatusRevisaoLoreLlm(false, false,
                "Não foi possível conectar ao servidor LLM em " + propriedades.baseUrl() + ": " + e.getMessage());
        }
    }

    /**
     * Consulta {@code /api/v0/models} (fora do prefixo {@code /v1}) e devolve os
     * ids com {@code state == "loaded"}. Lista vazia se o servidor não suportar a
     * extensão — o chamador cai para o comportamento de catálogo.
     */
    private List<String> buscarModelosCarregadosViaApiEstendida() {
        try {
            String raiz = propriedades.baseUrl().endsWith("/v1")
                ? propriedades.baseUrl().substring(0, propriedades.baseUrl().length() - "/v1".length())
                : propriedades.baseUrl();
            String json = httpClient.getAbsolute(raiz + "/api/v0/models");
            ListaModelosV0 resposta = objectMapper.readValue(json, ListaModelosV0.class);
            if (resposta == null || resposta.data() == null) {
                return List.of();
            }
            return resposta.data().stream()
                .filter(m -> "loaded".equalsIgnoreCase(m.state()))
                .map(ModeloDisponivelV0::id)
                .filter(id -> id != null)
                .toList();
        } catch (Exception e) {
            log.debug("Não foi possível consultar /api/v0/models (extensão da LM Studio): {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Entre os modelos carregados, prefere o que casa com o configurado; senão o
     * primeiro. Loga qual será usado quando houver mais de um carregado.
     */
    private String escolherEntreCarregados(List<String> carregados) {
        String configurado = propriedades.model();
        String escolhido = carregados.stream()
            .filter(id -> combinaComConfigurado(id, configurado))
            .findFirst()
            .orElse(carregados.get(0));
        if (carregados.size() > 1) {
            log.warn("Mais de um modelo carregado no LM Studio ({}). Usando \"{}\".", carregados, escolhido);
        }
        return escolhido;
    }

    private boolean combinaComConfigurado(String id, String configurado) {
        if (id == null || configurado == null || configurado.isBlank()) {
            return false;
        }
        String a = id.toLowerCase();
        String b = configurado.toLowerCase();
        return a.equals(b) || a.contains(b) || b.contains(a);
    }

    @Override
    public Optional<String> revisar(
        String promptSistemaRevisaoLore,
        String originalInglesMascarado,
        String traducaoPtMascarada,
        List<String> problemasDetectados
    ) {
        String promptUsuario = PromptRevisaoLore.montarPromptUsuario(
            originalInglesMascarado, traducaoPtMascarada, problemasDetectados);

        ChatRequest request = new ChatRequest(
            propriedades.model(),
            List.of(
                new Mensagem("system", promptSistemaRevisaoLore),
                new Mensagem("user", promptUsuario)
            ),
            TEMPERATURA_REVISAO,
            propriedades.maxTokens()
        );

        return postarLinhaUnica(request, traducaoPtMascarada);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: executa a chamada curta de revisão e extrai uma fala
     * final compatível com os marcadores da tradução corrente.
     * <p>INVARIANTES DO DOMÍNIO: erro permanente não é repetido; erro transitório
     * respeita o limite; resposta sem tags obrigatórias nunca é publicada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: registra a causa técnica e devolve vazio
     * depois de esgotar as tentativas; interrupção na pausa restaura o flag e encerra.
     */
    private Optional<String> postarLinhaUnica(ChatRequest request, String traducaoMascarada) {
        List<String> marcadoresEsperados = normalizador.extrairMarcadores(traducaoMascarada);
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS_REVISAO; tentativa++) {
            try {
                RespostaLlm resposta = httpClient.post("/chat/completions", request, RespostaLlm.class);

                if (resposta == null || resposta.choices() == null || resposta.choices().isEmpty()) {
                    log.warn("Resposta LLM sem choices (tentativa {}/{}; modelo={}).",
                        tentativa, MAX_TENTATIVAS_REVISAO, request.model());
                    continue;
                }

                Mensagem mensagem = resposta.choices().getFirst().message();
                String texto = mensagem != null ? mensagem.content() : null;
                if (texto == null || texto.isBlank()) {
                    log.warn("Resposta LLM com message.content vazio (tentativa {}/{}; modelo={}).",
                        tentativa, MAX_TENTATIVAS_REVISAO, request.model());
                    continue;
                }

                String normalizado = normalizador.normalizarLinhaUnica(texto, marcadoresEsperados);
                if (!normalizado.isBlank()) {
                    return Optional.of(normalizado);
                }
                log.warn("Resposta LLM recebida, mas sem linha final utilizável (tentativa {}/{}; "
                        + "modelo={}; marcadores esperados={}; resposta={}).",
                    tentativa, MAX_TENTATIVAS_REVISAO, request.model(), marcadoresEsperados,
                    resumirRespostaLog(texto));
            } catch (HttpClientException e) {
                log.warn("Falha na chamada LLM (tentativa {}/{}): HTTP {} - {}",
                    tentativa, MAX_TENTATIVAS_REVISAO, e.statusCode(), e.getMessage());
                if (isErroPermanente(e.statusCode())) {
                    break;
                }
                if (tentativa < MAX_TENTATIVAS_REVISAO) {
                    try {
                        Thread.sleep(pausaEntreTentativasMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("Falha na chamada LLM (tentativa {}/{}): {}",
                    tentativa, MAX_TENTATIVAS_REVISAO, e.getMessage());
                if (tentativa < MAX_TENTATIVAS_REVISAO) {
                    try {
                        Thread.sleep(pausaEntreTentativasMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 4xx que não seja 408 (timeout) ou 429 (rate limit) indica problema permanente
     * (modelo inválido, payload rejeitado, contexto excedido) — repetir a mesma
     * requisição não muda o resultado.
     */
    private boolean isErroPermanente(int statusCode) {
        return statusCode >= 400 && statusCode < 500 && statusCode != 408 && statusCode != 429;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: registra uma amostra segura da resposta recusada para
     * distinguir formato inesperado de indisponibilidade do servidor local.
     * <p>INVARIANTES DO DOMÍNIO: não modifica a resposta e limita o log a 500 chars.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conteúdo ausente é exibido como vazio.
     */
    private String resumirRespostaLog(String texto) {
        if (texto == null || texto.isBlank()) return "<vazio>";
        String limpo = texto.replace("\r", "").replace("\n", " ↵ ").strip();
        return limpo.length() <= 500 ? limpo : limpo.substring(0, 497) + "...";
    }
}
