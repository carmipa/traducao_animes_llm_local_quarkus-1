package org.traducao.projeto.traducao.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PROPÓSITO DE NEGÓCIO: controla, por configuração, se a Tradução Local pode acionar
 * a recuperação online de último recurso (fallback Google) para as falas que o LLM
 * local não conseguiu traduzir. É OPT-IN e desligada por padrão: quando desligada, o
 * pipeline permanece 100% local, sem nenhuma chamada externa.
 *
 * <p>INVARIANTES DO DOMÍNIO: {@code ativo} default {@code false}; ligar torna o fluxo
 * PARCIALMENTE online, porém somente sobre as pendências desta execução.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: portador de configuração puro; a decisão de rede e
 * a tolerância a falha externa ficam no serviço/adaptador que a consome.
 */
@ConfigurationProperties(prefix = "tradutor.fallback-online")
public class FallbackOnlineProperties {

    private boolean ativo = false;

    public FallbackOnlineProperties() {
    }

    public FallbackOnlineProperties(boolean ativo) {
        this.ativo = ativo;
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
}
