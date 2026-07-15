package org.traducao.projeto.legendasExtracao.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: Acumula o resultado de uma execução de extração — tanto
 * os contadores agregados (para o resumo e a telemetria) quanto a lista granular
 * por vídeo ({@link ItemExtracao}, que alimenta a tabela da UI). É o objeto que o
 * use case devolve à camada de apresentação.
 *
 * <p>INVARIANTES DO DOMÍNIO: cada vídeo processado incrementa {@code arquivosDetectados}
 * e adiciona exatamente um item; a soma de extraídas + sem-faixa + já-existentes +
 * falhas + timeouts nunca ultrapassa os vídeos detectados. {@code timeouts} é
 * contado à parte de {@code falhasInesperadas}. A lista de itens é exposta como
 * cópia imutável.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mutador simples, não lança. Contadores
 * começam em zero; a lista de itens começa vazia.
 */
public class RelatorioExtracao {
    private int arquivosDetectados = 0;
    private int faixasEncontradas = 0;
    private int legendasExtraidas = 0;
    private int arquivosSemLegenda = 0;
    private int arquivosJaExistentes = 0;
    private int falhasInesperadas = 0;
    private int timeouts = 0;

    private final List<ItemExtracao> itens = new ArrayList<>();
    private final FormatoLegenda formatoAlvo;

    public RelatorioExtracao(FormatoLegenda formatoAlvo) {
        this.formatoAlvo = formatoAlvo;
    }

    public void registrarDetectado() {
        this.arquivosDetectados++;
    }

    public void registrarFaixasEncontradas(int quantidade) {
        if (quantidade > 0) {
            this.faixasEncontradas += quantidade;
        }
    }

    public void registrarExtraido() {
        this.legendasExtraidas++;
    }

    public void registrarSemLegenda() {
        this.arquivosSemLegenda++;
    }

    public void registrarJaExiste() {
        this.arquivosJaExistentes++;
    }

    public void registrarFalha() {
        this.falhasInesperadas++;
    }

    public void registrarTimeout() {
        this.timeouts++;
    }

    public void adicionarItem(ItemExtracao item) {
        if (item != null) {
            this.itens.add(item);
        }
    }

    public int getArquivosDetectados() { return arquivosDetectados; }
    public int getFaixasEncontradas() { return faixasEncontradas; }
    public int getLegendasExtraidas() { return legendasExtraidas; }
    public int getArquivosSemLegenda() { return arquivosSemLegenda; }
    public int getArquivosJaExistentes() { return arquivosJaExistentes; }
    public int getFalhasInesperadas() { return falhasInesperadas; }
    public int getTimeouts() { return timeouts; }
    public FormatoLegenda getFormatoAlvo() { return formatoAlvo; }

    public List<ItemExtracao> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
