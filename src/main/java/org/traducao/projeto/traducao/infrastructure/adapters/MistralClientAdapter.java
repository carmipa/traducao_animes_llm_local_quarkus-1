package org.traducao.projeto.traducao.infrastructure.adapters;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.domain.Lote;
import org.traducao.projeto.traducao.domain.StatusLlm;
import org.traducao.projeto.traducao.domain.TraducaoLote;
import org.traducao.projeto.traducao.domain.exceptions.RespostaLlmVaziaException;
import org.traducao.projeto.traducao.domain.ports.MistralPort;
import org.traducao.projeto.contexto.domain.RegrasConcordanciaPtBr;
import org.traducao.projeto.traducao.infrastructure.config.LlmProperties;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.traducao.infrastructure.dtos.RecordsMistral.*;
import org.traducao.projeto.core.infrastructure.http.JsonHttpClient;
import org.traducao.projeto.core.infrastructure.http.JsonHttpClient.HttpClientException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class MistralClientAdapter implements MistralPort {

    private static final Logger log = LoggerFactory.getLogger(MistralClientAdapter.class);

    private static final int MAX_TENTATIVAS = 3;
    private static final int MAX_TENTATIVAS_REVISAO = 2;
    private static final long PAUSA_ENTRE_TENTATIVAS_MS = 2_000;
    private static final double TEMPERATURA_REVISAO = 0.15;
    private static final double TEMPERATURA_CORRECAO_TRADUCAO = 0.3;
    private static final Pattern BLOCO_RACIOCINIO = Pattern.compile("(?is)<think>.*?</think>");
    private static final Pattern MARCADOR_TAG = Pattern.compile("\\[\\[TAG\\d+]]");
    private static final Pattern PREFIXO_RESPOSTA = Pattern.compile(
        "(?i)^(?:tradu[cç][aã]o(?: corrigida)?|resposta|pt-br|texto corrigido)\\s*:\\s*");

    private final JsonHttpClient httpClient;
    private final LlmProperties propriedades;
    private final GerenciadorContexto gerenciadorContexto;
    private final ObjectMapper objectMapper;

    public MistralClientAdapter(LlmProperties propriedades, GerenciadorContexto gerenciadorContexto, ObjectMapper mapper) {
        this.propriedades = propriedades;
        this.gerenciadorContexto = gerenciadorContexto;
        this.objectMapper = mapper;
        this.httpClient = new JsonHttpClient(propriedades.connectTimeout(), propriedades.readTimeout(), propriedades.baseUrl(), mapper);
    }

    @Override
    public StatusLlm verificarDisponibilidade() {
        try {
            // 1. Fonte de verdade: a API estendida da LM Studio (/api/v0/models) informa
            // o campo "state" ("loaded"/"not-loaded"), diferente do endpoint OpenAI-
            // compatible (/v1/models) usado abaixo, que só lista o catálogo baixado sem
            // indicar o que está de fato carregado em memória.
            List<String> carregados = buscarModelosCarregadosViaApiEstendida();
            if (!carregados.isEmpty()) {
                String escolhido = escolherEntreCarregados(carregados);
                propriedades.setModel(escolhido);
                String msg = carregados.size() == 1
                    ? "Servidor LLM online e modelo \"" + escolhido + "\" carregado em memória."
                    : "Servidor LLM online; " + carregados.size() + " modelos carregados, usando \"" + escolhido + "\".";
                return new StatusLlm(true, true, msg);
            }

            // 2. API estendida indisponível (servidor não é LM Studio, ou não suporta a
            // extensão) — cai para o catálogo OpenAI-compatible (/v1/models), que NÃO
            // confirma o que está carregado. NUNCA escolher o primeiro do catálogo às
            // cegas: isso já fez o app pedir um modelo diferente do carregado e o LM
            // Studio subir uma SEGUNDA instância via auto-load (JIT). Só adota o modelo
            // CONFIGURADO se ele aparecer no catálogo; caso contrário, reporta que não
            // dá para confirmar e não inicia.
            ListaModelos resposta = httpClient.get("/models", ListaModelos.class);
            List<ModeloDisponivel> modelos = resposta != null ? resposta.data() : null;
            if (modelos == null || modelos.isEmpty()) {
                return new StatusLlm(true, false,
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
                return new StatusLlm(true, true,
                    "Servidor LLM online. Modelo configurado \"" + modelo + "\" encontrado no catálogo (o servidor não confirma qual está carregado).");
            }

            return new StatusLlm(true, false,
                "Servidor LLM online, mas não foi possível confirmar qual modelo está carregado e o modelo configurado \""
                + modeloConfigurado + "\" não está no catálogo. Carregue o modelo no LM Studio e tente novamente.");
        } catch (Exception e) {
            return new StatusLlm(false, false,
                "Não foi possível conectar ao servidor LLM em " + propriedades.baseUrl() + ": " + e.getMessage());
        }
    }

    /**
     * Consulta a API estendida da LM Studio ({@code /api/v0/models}, fora do
     * prefixo {@code /v1} do base-url configurado) e devolve os ids dos modelos
     * com {@code state == "loaded"}. Lista vazia (sem lançar) se o servidor não
     * suportar essa extensão — o chamador cai para o comportamento de catálogo.
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
     * Entre os modelos efetivamente carregados, prefere o que casa com o
     * configurado; senão o primeiro. Loga qual será usado quando houver mais de
     * um carregado, em vez de escolher em silêncio.
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
    public TraducaoLote traduzir(Lote lote) {
        return traduzir(lote, null);
    }

    @Override
    public TraducaoLote traduzir(Lote lote, Double temperaturaOverride) {
        return traduzir(lote, temperaturaOverride, null);
    }

    @Override
    public TraducaoLote traduzir(Lote lote, Double temperaturaOverride, String promptSistemaCongelado) {
        String prompt = montarPrompt(lote);
        String promptSistema = promptSistemaCongelado != null
            ? promptSistemaCongelado : gerenciadorContexto.obterPromptAtivo();
        ChatRequest request = new ChatRequest(
            propriedades.model(),
            List.of(
                new Mensagem("system", promptSistema),
                new Mensagem("user", prompt)
            ),
            temperaturaOverride != null ? temperaturaOverride : propriedades.temperature(),
            propriedades.maxTokens()
        );

        Exception ultimaFalha = null;
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                log.debug("Enviando lote {} ao LLM ({} linha(s)) — tentativa {}/{}",
                    lote.idLote(), lote.linhasOriginais().size(), tentativa, MAX_TENTATIVAS);
                long inicio = System.currentTimeMillis();

                RespostaLlm resposta = httpClient.post("/chat/completions", request, RespostaLlm.class);

                long duracaoMs = System.currentTimeMillis() - inicio;

                if (resposta == null || resposta.choices() == null || resposta.choices().isEmpty()) {
                    throw new RespostaLlmVaziaException("Resposta vazia do LLM para o lote " + lote.idLote());
                }

                Mensagem mensagem = resposta.choices().getFirst().message();
                String traduzidoText = mensagem != null ? mensagem.content() : null;
                if (traduzidoText == null || traduzidoText.isBlank()) {
                    throw new RespostaLlmVaziaException("Conteudo vazio retornado pelo LLM para o lote " + lote.idLote());
                }

                List<String> linhasTraduzidas = extrairLinhasTraduzidas(traduzidoText);
                linhasTraduzidas = removerNumeracaoAlucinada(linhasTraduzidas, lote.linhasOriginais());
                log.debug("Lote {} traduzido em {} ms ({} -> {} linha(s))",
                    lote.idLote(), duracaoMs, lote.linhasOriginais().size(), linhasTraduzidas.size());

                return new TraducaoLote(lote.idLote(), linhasTraduzidas, true, null);

            } catch (RespostaLlmVaziaException e) {
                // Resposta vazia costuma ser falha transitória do LLM local
                // (modelo ainda aquecendo, timeout interno) — participa das
                // tentativas em vez de encerrar o lote na primeira ocorrência.
                ultimaFalha = e;
                log.warn("Resposta vazia do LLM para o lote {} (tentativa {}/{}): {}",
                    lote.idLote(), tentativa, MAX_TENTATIVAS, e.getMessage());
            } catch (HttpClientException e) {
                ultimaFalha = e;
                log.warn("LLM respondeu com erro HTTP {} para o lote {} (tentativa {}/{}): {}",
                    e.statusCode(), lote.idLote(), tentativa, MAX_TENTATIVAS, e.getMessage());
                if (isErroPermanente(e.statusCode())) {
                    log.warn("Erro HTTP {} é permanente (não é timeout/rate-limit) — abortando retries do lote {}.",
                        e.statusCode(), lote.idLote());
                    break;
                }
            } catch (Exception e) {
                ultimaFalha = e;
                if (JsonHttpClient.isErroRedeOuTimeout(e)) {
                    log.warn("Falha de rede/timeout ao chamar o LLM para o lote {} (tentativa {}/{}): {}",
                        lote.idLote(), tentativa, MAX_TENTATIVAS, e.getMessage());
                } else if (e.getMessage() != null && e.getMessage().contains("interrupt")) {
                    log.warn("Erro ao traduzir o lote {} - Thread abortada (tentativa {}/{})",
                        lote.idLote(), tentativa, MAX_TENTATIVAS);
                } else {
                    log.warn("Erro ao traduzir o lote {} (tentativa {}/{}): {}",
                        lote.idLote(), tentativa, MAX_TENTATIVAS, e.getMessage());
                }
            }

            if (tentativa < MAX_TENTATIVAS) {
                try {
                    Thread.sleep(PAUSA_ENTRE_TENTATIVAS_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        String mensagemFinal = "Erro ao traduzir o lote " + lote.idLote()
            + " após " + MAX_TENTATIVAS + " tentativa(s)";
        if (ultimaFalha != null) {
            mensagemFinal += ": " + ultimaFalha.getMessage();
        }
        log.error(mensagemFinal);
        return new TraducaoLote(lote.idLote(), null, false, mensagemFinal);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: solicita uma revisão pontual de concordância PT-BR
     * usando a lore ativa e preservando os marcadores estruturais da tradução.
     * <p>INVARIANTES DO DOMÍNIO: a resposta final contém uma única fala e todos
     * os marcadores presentes na tradução mascarada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve vazio após tentativas sem conteúdo
     * utilizável ou erro HTTP, mantendo a fala atual intacta.
     */
    @Override
    public Optional<String> revisarConcordancia(
        String originalInglesMascarado,
        String traducaoPtMascarada,
        List<String> problemasDetectados
    ) {
        String promptUsuario = montarPromptRevisao(originalInglesMascarado, traducaoPtMascarada, problemasDetectados);
        String promptSistema = RegrasConcordanciaPtBr.montarPromptRevisao(gerenciadorContexto.obterLoreAtiva());

        ChatRequest request = new ChatRequest(
            propriedades.model(),
            List.of(
                new Mensagem("system", promptSistema),
                new Mensagem("user", promptUsuario)
            ),
            TEMPERATURA_REVISAO,
            propriedades.maxTokens()
        );

        return postarLinhaUnica(request, traducaoPtMascarada);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: retraduz uma fala residual ou incompleta sem perder
     * tags ASS já existentes na tradução atual.
     * <p>INVARIANTES DO DOMÍNIO: marcadores esperados precisam sobreviver à resposta.
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve vazio e delega a decisão de
     * pendência ao caso de uso chamador.
     */
    @Override
    public Optional<String> corrigirTraducao(
        String originalInglesMascarado,
        String traducaoPtMascarada,
        String motivoDetectado
    ) {
        String promptUsuario = montarPromptCorrecaoTraducao(originalInglesMascarado, traducaoPtMascarada, motivoDetectado);

        ChatRequest request = new ChatRequest(
            propriedades.model(),
            List.of(
                new Mensagem("system", gerenciadorContexto.obterPromptAtivo()),
                new Mensagem("user", promptUsuario)
            ),
            TEMPERATURA_CORRECAO_TRADUCAO,
            propriedades.maxTokens()
        );

        return postarLinhaUnica(request, traducaoPtMascarada);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: executa chamadas curtas de revisão e extrai uma fala
     * final compatível com os marcadores da tradução corrente.
     * <p>INVARIANTES DO DOMÍNIO: erro permanente não é repetido; erro transitório
     * respeita o limite; resposta sem tags obrigatórias nunca é publicada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: registra causa técnica e devolve vazio
     * depois de esgotar as tentativas.
     */
    private Optional<String> postarLinhaUnica(ChatRequest request, String traducaoMascarada) {
        List<String> marcadoresEsperados = extrairMarcadores(traducaoMascarada);
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

                String normalizado = normalizarLinhaUnica(texto, marcadoresEsperados);
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
                        Thread.sleep(PAUSA_ENTRE_TENTATIVAS_MS);
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
                        Thread.sleep(PAUSA_ENTRE_TENTATIVAS_MS);
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
     * 4xx que não seja 408 (timeout) ou 429 (rate limit) indica um problema permanente
     * (modelo inválido, payload rejeitado, contexto excedido) — repetir a mesma
     * requisição não muda o resultado, então não vale gastar as tentativas restantes.
     */
    private boolean isErroPermanente(int statusCode) {
        return statusCode >= 400 && statusCode < 500 && statusCode != 408 && statusCode != 429;
    }

    private String montarPromptCorrecaoTraducao(String originalIngles, String traducaoPt, String motivoDetectado) {
        return """
            A traducao abaixo ficou com residuo em ingles, incompleta ou alucinada.
            Retraduza esta fala para PT-BR corretamente, preservando o sentido e os
            marcadores [[TAGn]] literalmente (nao traduza nem remova marcadores).

            Original (ingles):
            %s

            Traducao atual (problema detectado: %s):
            %s

            Responda com uma unica linha: a traducao corrigida em portugues.
            """.formatted(originalIngles, motivoDetectado, traducaoPt);
    }

    private String montarPromptRevisao(
        String originalIngles, String traducaoPt, List<String> problemasDetectados
    ) {
        String listaProblemas = problemasDetectados == null || problemasDetectados.isEmpty()
            ? "(nenhum detalhe heurístico)"
            : String.join("\n- ", problemasDetectados);

        return """
            Corrija APENAS concordancia de genero/pronomes/adjetivos na traducao em portugues.
            Original em ingles (referencia de genero/contexto):
            %s

            Traducao atual em portugues (corrigir se necessario):
            %s

            Problemas detectados automaticamente:
            - %s

            Se o problema mencionar "masculino marcado" e o original nao indicar genero:
            - use feminino quando a lore/personagem indicar falante ou interlocutora mulher;
            - se a lore nao permitir inferir, troque por uma formulacao neutra natural em PT-BR;
            - nao mantenha masculino apenas por costume ou padrao generico.

            Responda com uma unica linha: a traducao corrigida.
            """.formatted(originalIngles, traducaoPt, listaProblemas);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: extrai a tradução final de respostas de revisão que
     * podem conter raciocínio, cerca Markdown ou um rótulo antes da fala.
     *
     * <p>INVARIANTES DO DOMÍNIO: quando a fala possui marcadores {@code [[TAGn]]},
     * somente uma linha que preserve todos eles pode ser escolhida; explicações
     * nunca são concatenadas ao texto da legenda.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve texto vazio quando nenhuma linha
     * utilizável preserva os marcadores esperados, permitindo nova tentativa sem
     * publicar conteúdo estruturalmente incompleto.
     */
    static String normalizarLinhaUnica(String texto, List<String> marcadoresEsperados) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String normalizado = texto.replace("\r\n", "\n").replace('\r', '\n').strip();
        normalizado = BLOCO_RACIOCINIO.matcher(normalizado).replaceAll("").strip();
        if (normalizado.startsWith("```") && normalizado.endsWith("```")) {
            normalizado = removerCercaMarkdownEstatico(normalizado).strip();
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
     * tradução atual mascarada para orientar a seleção segura da linha respondida.
     *
     * <p>INVARIANTES DO DOMÍNIO: a lista preserva ordem e elimina duplicações,
     * preservando a ordem estrutural usada para restaurar o ASS.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: tradução ausente devolve lista vazia e
     * mantém o comportamento de resposta sem tags.
     */
    private List<String> extrairMarcadores(String textoMascarado) {
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
     * PROPÓSITO DE NEGÓCIO: registra uma amostra segura da resposta recusada para
     * distinguir formato inesperado de indisponibilidade do servidor local.
     *
     * <p>INVARIANTES DO DOMÍNIO: não modifica a resposta usada pelo pipeline e
     * limita o log a 500 caracteres, preservando marcadores úteis ao diagnóstico.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conteúdo ausente é exibido como vazio.
     */
    private String resumirRespostaLog(String texto) {
        if (texto == null || texto.isBlank()) return "<vazio>";
        String limpo = texto.replace("\r", "").replace("\n", " ↵ ").strip();
        return limpo.length() <= 500 ? limpo : limpo.substring(0, 497) + "...";
    }

    private String montarPrompt(Lote lote) {
        int totalLinhas = lote.linhasOriginais().size();
        String linhas = String.join("\n", lote.linhasOriginais());
        return "Traduza estas " + totalLinhas + " linha(s), uma por linha. Responda com exatamente "
            + totalLinhas + " linha(s) de saida, na mesma ordem:\n" + linhas;
    }

    private List<String> extrairLinhasTraduzidas(String texto) {
        String normalizado = texto.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (normalizado.startsWith("```") && normalizado.endsWith("```")) {
            normalizado = removerCercaMarkdown(normalizado).strip();
        }

        String[] linhas = normalizado.split("\n", -1);
        List<String> resultado = new ArrayList<>(linhas.length);
        for (String linha : linhas) {
            // Linha em branco nunca é tradução: os lotes só contêm falas com
            // texto. É o modelo separando as respostas com linha vazia — sem
            // este filtro a contagem diverge e o lote é dividido/retentado à toa.
            if (linha.isBlank()) {
                continue;
            }
            resultado.add(linha.stripTrailing());
        }
        return resultado;
    }

    private static final java.util.regex.Pattern PADRAO_LINHA_NUMERADA =
        java.util.regex.Pattern.compile("^\\s*(\\d+)[.)]\\s+(.*)$");

    /**
     * Alguns modelos enumeram a resposta ("1. fala", "2. fala") mesmo
     * instruídos a não fazê-lo. A contagem de linhas bate, então a validação
     * não pega — e o "1." iria parar na legenda final. Remove a numeração
     * apenas quando TODAS as linhas vêm numeradas em sequência 1..N e os
     * originais correspondentes NÃO começam numerados (se começam, o número é
     * conteúdo real da fala, ex.: itens de uma lista, e deve ficar).
     * <p>
     * Vale também para lote de UMA linha (o tamanho-lote padrão do projeto é
     * 1): "1. fala" numa resposta única é numeração alucinada do mesmo jeito,
     * e era exatamente o caso que escapava quando este método exigia 2+ linhas.
     */
    private List<String> removerNumeracaoAlucinada(List<String> traduzidas, List<String> originais) {
        if (traduzidas.size() != originais.size() || traduzidas.isEmpty()) {
            return traduzidas;
        }
        List<String> semNumeracao = new ArrayList<>(traduzidas.size());
        for (int i = 0; i < traduzidas.size(); i++) {
            var matcher = PADRAO_LINHA_NUMERADA.matcher(traduzidas.get(i));
            if (!matcher.matches() || Integer.parseInt(matcher.group(1)) != i + 1) {
                return traduzidas;
            }
            if (PADRAO_LINHA_NUMERADA.matcher(originais.get(i)).matches()) {
                return traduzidas;
            }
            semNumeracao.add(matcher.group(2));
        }
        log.info("Numeração de linhas alucinada pelo LLM detectada e removida ({} linha(s)).", traduzidas.size());
        return semNumeracao;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: remove cercas Markdown de respostas de tradução em lote.
     * <p>INVARIANTES DO DOMÍNIO: apenas o invólucro é removido; conteúdo permanece.
     * <p>COMPORTAMENTO EM CASO DE FALHA: formato incompleto é devolvido sem corte.
     */
    private String removerCercaMarkdown(String texto) {
        return removerCercaMarkdownEstatico(texto);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: disponibiliza a mesma remoção de cerca ao normalizador
     * estático testável das respostas de revisão.
     * <p>INVARIANTES DO DOMÍNIO: exige abertura, quebra inicial e fechamento válidos.
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve o texto original.
     */
    private static String removerCercaMarkdownEstatico(String texto) {
        int primeiraQuebra = texto.indexOf('\n');
        int ultimaCerca = texto.lastIndexOf("```");
        if (primeiraQuebra < 0 || ultimaCerca <= primeiraQuebra) {
            return texto;
        }
        return texto.substring(primeiraQuebra + 1, ultimaCerca);
    }
}
