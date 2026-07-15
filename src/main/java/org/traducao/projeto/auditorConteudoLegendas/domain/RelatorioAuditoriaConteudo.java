package org.traducao.projeto.auditorConteudoLegendas.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: representa o resultado exibido e exportado pela
 * Análise de Legenda, incluindo a identificação inequívoca dos artefatos
 * comparados e de seus formatos.
 *
 * <p>INVARIANTES DO DOMÍNIO: arquivo e formato original sempre pertencem ao
 * mesmo artefato; arquivo e formato traduzido seguem a mesma regra; anomalias
 * são acumuladas sem alterar os metadados de entrada.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: esta classe não executa I/O; dados
 * inválidos precisam ser rejeitados pelo caso de uso antes de sua criação.
 */
public class RelatorioAuditoriaConteudo {
    private final String arquivoOriginal;
    private final String arquivoTraduzido;
    private final String formatoOriginal;
    private final String formatoTraduzido;
    private final ModoAuditoria modo;
    private final List<AnomaliaConteudo> anomalias = new ArrayList<>();
    private long duracaoMs;
    private String caminhoRelatorioJson;
    private int regrasExecutadas;

    /**
     * PROPÓSITO DE NEGÓCIO: mantém a assinatura histórica da comparação
     * original ↔ traduzido, fixando o modo em {@link ModoAuditoria#AMBAS}.
     * <p>INVARIANTES DO DOMÍNIO: preserva o contrato usado pelos testes e
     * chamadas que auditam os dois arquivos.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não normaliza valores; delega ao
     * construtor completo.
     */
    public RelatorioAuditoriaConteudo(
        String arquivoOriginal,
        String arquivoTraduzido,
        String formatoOriginal,
        String formatoTraduzido
    ) {
        this(arquivoOriginal, arquivoTraduzido, formatoOriginal, formatoTraduzido, ModoAuditoria.AMBAS);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: cria a fotografia inicial da análise (comparativa ou
     * de arquivo único) que será apresentada ao usuário e usada como dataset de
     * telemetria.
     * <p>INVARIANTES DO DOMÍNIO: no modo de arquivo único apenas o lado auditado
     * traz nome e formato; o outro lado é {@code null} por definição.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não normaliza nem tenta adivinhar
     * valores ausentes; a validação pertence ao caso de uso.
     */
    public RelatorioAuditoriaConteudo(
        String arquivoOriginal,
        String arquivoTraduzido,
        String formatoOriginal,
        String formatoTraduzido,
        ModoAuditoria modo
    ) {
        this.arquivoOriginal = arquivoOriginal;
        this.arquivoTraduzido = arquivoTraduzido;
        this.formatoOriginal = formatoOriginal;
        this.formatoTraduzido = formatoTraduzido;
        this.modo = modo;
    }

    public void adicionarAnomalia(AnomaliaConteudo anomalia) {
        anomalias.add(anomalia);
    }

    public List<AnomaliaConteudo> getAnomalias() {
        return anomalias;
    }

    public String getArquivoOriginal() {
        return arquivoOriginal;
    }

    public String getArquivoTraduzido() {
        return arquivoTraduzido;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: informa o formato da legenda usada como referência.
     * <p>INVARIANTES DO DOMÍNIO: valor corresponde ao arquivo original.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não executa detecção tardia.
     */
    public String getFormatoOriginal() {
        return formatoOriginal;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: informa o formato do artefato traduzido auditado.
     * <p>INVARIANTES DO DOMÍNIO: valor corresponde ao arquivo traduzido.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não executa detecção tardia.
     */
    public String getFormatoTraduzido() {
        return formatoTraduzido;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: informa o escopo escolhido (só original, só traduzido
     * ou ambos) para que tela, exportação e telemetria descrevam a mesma análise.
     * <p>INVARIANTES DO DOMÍNIO: o valor é definido na criação e nunca muda.
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca é nulo — o construtor histórico
     * assume {@link ModoAuditoria#AMBAS}.
     */
    @JsonProperty("modo")
    public String getModo() {
        return modo.name();
    }

    @JsonProperty("limpo")
    public boolean isLimpo() {
        return anomalias.isEmpty();
    }

    public long getDuracaoMs() {
        return duracaoMs;
    }

    public String getCaminhoRelatorioJson() {
        return caminhoRelatorioJson;
    }

    public int getRegrasExecutadas() {
        return regrasExecutadas;
    }

    public void definirMetadados(long duracaoMs, String caminhoRelatorioJson, int regrasExecutadas) {
        this.duracaoMs = duracaoMs;
        this.caminhoRelatorioJson = caminhoRelatorioJson;
        this.regrasExecutadas = regrasExecutadas;
    }
}
