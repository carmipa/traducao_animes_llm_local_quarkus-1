package org.traducao.projeto.traducao.infrastructure.contextocena;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PROPÓSITO DE NEGÓCIO: controla, por configuração, a correção de gênero por contexto de
 * cena da Tradução Local. É OPT-IN e DESLIGADA por padrão (molde do
 * {@code FallbackOnlineProperties}): enquanto {@code ativo=false}, o pipeline de tradução
 * segue idêntico ao atual — nenhuma janela de vizinhança é montada, nenhum token extra é
 * enviado ao LM Studio e a chave/proveniência do cache não muda. É a chave do A/B do
 * piloto: liga/desliga o motor sobre o MESMO episódio para comparar.
 *
 * <p>INVARIANTES DO DOMÍNIO: {@code ativo} default {@code false}; {@code tamanhoJanela} é o
 * número de vizinhas de cada lado (default 2), nunca negativo — valor não positivo é
 * normalizado para 0 (janela só com a fala-alvo). Portador de configuração puro.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: nenhuma decisão de runtime vive aqui; o serviço que
 * consumir esta config decide o que fazer quando ligada. Ausência de config no ambiente
 * resolve para os defaults (desligada, janela 2).
 */
@ConfigurationProperties(prefix = "tradutor.contexto-cena")
public class ContextoCenaProperties {

    private boolean ativo = false;
    private int tamanhoJanela = 2;

    public ContextoCenaProperties() {
    }

    public ContextoCenaProperties(boolean ativo, int tamanhoJanela) {
        this.ativo = ativo;
        this.tamanhoJanela = Math.max(0, tamanhoJanela);
    }

    public boolean ativo() {
        return ativo;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public int tamanhoJanela() {
        return tamanhoJanela;
    }

    public int getTamanhoJanela() {
        return tamanhoJanela;
    }

    public void setTamanhoJanela(int tamanhoJanela) {
        this.tamanhoJanela = Math.max(0, tamanhoJanela);
    }
}
