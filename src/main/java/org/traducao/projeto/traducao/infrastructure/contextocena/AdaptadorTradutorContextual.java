package org.traducao.projeto.traducao.infrastructure.contextocena;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.core.infrastructure.http.JsonHttpClient;
import org.traducao.projeto.traducao.application.contextocena.MontadorMensagemContextual;
import org.traducao.projeto.traducao.domain.contextocena.JanelaContextual;
import org.traducao.projeto.traducao.domain.contextocena.LinhaAlvoContextual;
import org.traducao.projeto.traducao.domain.contextocena.RequisicaoTraducaoContextual;
import org.traducao.projeto.traducao.domain.contextocena.TradutorContextualPort;
import org.traducao.projeto.traducao.infrastructure.config.LlmProperties;
import org.traducao.projeto.traducao.infrastructure.dtos.RecordsLlm.ChatRequest;
import org.traducao.projeto.traducao.infrastructure.dtos.RecordsLlm.Mensagem;
import org.traducao.projeto.traducao.infrastructure.dtos.RecordsLlm.RespostaLlm;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: implementação da {@link TradutorContextualPort} — traduz UMA fala
 * usando o contexto de cena, reusando a MESMA infraestrutura HTTP neutra do LM Studio
 * ({@link JsonHttpClient} + {@code ChatRequest} OpenAI-compatible) SEM tocar o peer
 * {@code llm} ({@code Lote}/{@code LlmPort}). Faz uma chamada única (lote=1 no alvo), limpa a
 * resposta e blinda contra o modelo vazar o contexto na tradução. Ainda NÃO está ligado ao
 * pipeline (a fiação e o cache vêm na subfase seguinte, sob a flag desligada).
 *
 * <p>INVARIANTES DO DOMÍNIO: só a fala-alvo é traduzida; a resposta é reduzida a UMA linha;
 * uma linha que apenas ECOA verbatim o contexto ou o próprio original (sem traduzir) é
 * REJEITADA; em qualquer falha (rede, resposta inutilizável, vazamento total) a política é
 * MANTER a fala-alvo original — nunca inventar nem publicar contexto. O passo HTTP fica atrás
 * do método {@link #enviarChat} para permitir teste sem rede.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: {@code requisicao}/janela nulos lançam
 * {@link NullPointerException}; erro de rede/HTTP/parse é capturado e devolve a fala-alvo
 * original (fallback seguro), registrando a causa em log.
 */
@Component
public class AdaptadorTradutorContextual implements TradutorContextualPort {

    private static final Logger log = LoggerFactory.getLogger(AdaptadorTradutorContextual.class);

    private static final Pattern BLOCO_RACIOCINIO = Pattern.compile("(?is)<think>.*?</think>");
    private static final Pattern PREFIXO_RESPOSTA = Pattern.compile(
        "(?i)^(?:tradu[cç][aã]o(?: corrigida)?|resposta|pt-br|texto corrigido)\\s*:\\s*");

    private final LlmProperties propriedades;
    private final MontadorMensagemContextual montadorMensagem;
    private final JsonHttpClient httpClient;

    public AdaptadorTradutorContextual(
            LlmProperties propriedades, MontadorMensagemContextual montadorMensagem, ObjectMapper objectMapper) {
        this.propriedades = propriedades;
        this.montadorMensagem = montadorMensagem;
        this.httpClient = new JsonHttpClient(
            propriedades.connectTimeout(), propriedades.readTimeout(), propriedades.baseUrl(), objectMapper);
    }

    @Override
    public String traduzirComContexto(RequisicaoTraducaoContextual requisicao) {
        Objects.requireNonNull(requisicao, "requisicao");
        JanelaContextual janela = Objects.requireNonNull(requisicao.janela(), "requisicao.janela");
        String original = janela.alvo() != null ? janela.alvo().texto() : null;

        String mensagemUsuario = montadorMensagem.montarMensagemUsuario(janela);
        ChatRequest request = new ChatRequest(
            propriedades.model(),
            List.of(
                new Mensagem("system", requisicao.promptSistema() == null ? "" : requisicao.promptSistema()),
                new Mensagem("user", mensagemUsuario)),
            propriedades.temperature(),
            propriedades.maxTokens());

        try {
            RespostaLlm resposta = enviarChat(request);
            String bruto = extrairConteudo(resposta);
            String linha = escolherLinhaAlvo(bruto, janela);
            return (linha == null || linha.isBlank()) ? original : linha;
        } catch (Exception e) {
            log.warn("Tradução contextual falhou; mantendo a fala-alvo original. Causa: {}", e.getMessage());
            return original;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: executa a chamada HTTP única ao endpoint de chat do LM Studio.
     * É o SEAM da classe — protegido para que o teste substitua a rede por uma resposta
     * canned, sem tocar a lógica de montagem/limpeza.
     * <p>INVARIANTES DO DOMÍNIO: uma requisição, um endpoint {@code /chat/completions}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga {@link IOException}/{@link InterruptedException}
     * e demais erros para o {@code catch} de {@link #traduzirComContexto}.
     */
    protected RespostaLlm enviarChat(ChatRequest request) throws IOException, InterruptedException {
        return httpClient.post("/chat/completions", request, RespostaLlm.class);
    }

    private static String extrairConteudo(RespostaLlm resposta) {
        if (resposta == null || resposta.choices() == null || resposta.choices().isEmpty()) {
            return null;
        }
        Mensagem mensagem = resposta.choices().getFirst().message();
        return mensagem != null ? mensagem.content() : null;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reduz a resposta bruta a UMA linha de tradução — remove o bloco de
     * raciocínio {@code <think>}, um rótulo inicial ("Traducao:" etc.) e escolhe a primeira
     * linha não vazia que NÃO seja eco verbatim do contexto/alvo de entrada (anti-vazamento).
     * <p>INVARIANTES DO DOMÍNIO: linha que ecoa contexto/alvo é pulada; se sobra alguma
     * tradução, é ela; senão devolve {@code null} (o chamador mantém o original).
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada nula/branca devolve {@code null}.
     */
    private static String escolherLinhaAlvo(String bruto, JanelaContextual janela) {
        if (bruto == null || bruto.isBlank()) {
            return null;
        }
        String limpo = bruto.replace("\r\n", "\n").replace('\r', '\n').strip();
        limpo = BLOCO_RACIOCINIO.matcher(limpo).replaceAll("").strip();

        Set<String> entradas = new HashSet<>();
        adicionar(entradas, janela.alvo());
        janela.antes().forEach(l -> adicionar(entradas, l));
        janela.depois().forEach(l -> adicionar(entradas, l));

        for (String bruta : limpo.split("\n")) {
            String linha = PREFIXO_RESPOSTA.matcher(bruta.strip()).replaceFirst("").strip();
            if (linha.isBlank()) {
                continue;
            }
            if (entradas.contains(chave(linha))) {
                continue; // eco verbatim do contexto/alvo: vazamento, descarta
            }
            return linha;
        }
        return null;
    }

    private static void adicionar(Set<String> set, LinhaAlvoContextual linha) {
        if (linha != null && linha.texto() != null) {
            set.add(chave(linha.texto()));
        }
    }

    private static String chave(String texto) {
        return texto.strip().toLowerCase(Locale.ROOT);
    }
}
