package org.traducao.projeto.traducaoCorrige.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PROPÓSITO DE NEGÓCIO: configuração da fatia de manutenção de cache. Existe hoje por uma razão
 * só — a trava de segurança do reforço de terminologia, que ESCREVE sobre trabalho já pronto.
 *
 * <p>É também o primeiro {@code infrastructure/config} das quatro fatias da correção, que o
 * diagnóstico do Plano-Mestre mediu com zero.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A escrita nasce DESLIGADA. É a receita de fatia nova do projeto: capacidade nova entra
 *       com flag OFF e log de estado no início, para que nada passe a agir sobre o acervo sem
 *       alguém ter decidido ligar. Com {@code false} o comportamento é byte-idêntico ao de antes
 *       de a operação existir.</li>
 *   <li>O ENSAIO não é travado por esta flag, e isso é deliberado: ele é READ-ONLY (medido — 228
 *       arquivos byte-idênticos depois de uma varredura completa) e é justamente o instrumento
 *       com que se decide se vale ligar a escrita. Travar os dois deixaria o operador sem como
 *       formar a decisão que a flag existe para exigir dele.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Propriedade ausente no YAML mantém o padrão seguro ({@code false}); nunca lança.
 */
@ConfigurationProperties(prefix = "correcao-cache")
public class CorrecaoCacheProperties {

    /**
     * Autoriza o reforço de terminologia a ESCREVER no cache já gravado. DESLIGADO por padrão:
     * com {@code false} o pedido de aplicação é recusado antes de abrir qualquer arquivo, e só o
     * ensaio roda. Ligar é uma decisão consciente sobre reescrever trabalho pronto em lote.
     */
    private boolean reforcoTerminologiaAplicarHabilitado = false;

    public boolean reforcoTerminologiaAplicarHabilitado() {
        return reforcoTerminologiaAplicarHabilitado;
    }

    public boolean isReforcoTerminologiaAplicarHabilitado() {
        return reforcoTerminologiaAplicarHabilitado;
    }

    public void setReforcoTerminologiaAplicarHabilitado(boolean reforcoTerminologiaAplicarHabilitado) {
        this.reforcoTerminologiaAplicarHabilitado = reforcoTerminologiaAplicarHabilitado;
    }
}
