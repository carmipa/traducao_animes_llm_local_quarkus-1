package org.traducao.projeto.raspagemRevisao.infrastructure;

import org.springframework.stereotype.Component;
import org.traducao.projeto.raspagemCorrecao.domain.ResultadoRaspagem;
import org.traducao.projeto.raspagemCorrecao.domain.ports.RecuperacaoExternaPort;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoRecuperacaoExterna;
import org.traducao.projeto.raspagemRevisao.domain.StatusRecuperacaoExterna;
import org.traducao.projeto.raspagemRevisao.domain.ports.RecuperacaoExternaRevisaoPort;

/**
 * PROPÓSITO DE NEGÓCIO: liga a revisão de legendas ao tradutor de máquina externo, traduzindo o
 * desfecho para o vocabulário desta fatia.
 *
 * <p>É o ADAPTADOR da {@link RecuperacaoExternaRevisaoPort}. Toda a dependência para
 * {@code raspagemCorrecao} vive AQUI, em {@code infrastructure}, e é para a PORTA daquela fatia —
 * um contrato publicado — e não para o scraper concreto, que era o que a camada de aplicação
 * importava antes.
 *
 * <h2>Por que delegar em vez de duplicar</h2>
 * A alternativa era uma terceira cópia das 239 linhas do {@code GoogleTranslateScraper}. O contrato
 * do projeto prefere duplicação consciente a acoplamento, e com razão — mas o que se duplicaria
 * aqui não é uma regra de negócio desta fatia: é um contorno contra um endpoint não documentado do
 * Google, para um uso IDÊNTICO ao da fatia vizinha (mesma chamada, mesmo desfecho, mesma
 * finalidade de corrigir tradução existente). As duas cópias que já existem no projeto — o scraper
 * e o {@code traducao.infrastructure.adapters.GoogleFallbackAdapter} — divergiram: padrão de
 * marcador residual e política de retry diferentes. Uma terceira cópia sem divergência semântica
 * que a justifique seria drift garantido, e quando o Google mudar o endpoint três lugares quebram
 * e dois são consertados.
 *
 * <p>A dívida que sobra, portanto, é fatia→fatia e está declarada como tal na catraca de
 * arquitetura. O que ela NÃO é: uma violação de camada, que era o alvo da FASE 2 e foi eliminada.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só TRADUZ vocabulário. Nenhuma decisão, nenhum retry, nenhuma pausa — o ritmo continua
 *       sendo do caso de uso, que já pausa depois de cada fala.</li>
 *   <li>O mapeamento de status é um {@code switch} EXAUSTIVO, sem {@code default}: se a fatia
 *       vizinha ganhar um desfecho novo, isto vira erro de compilação em vez de virar
 *       silenciosamente "resposta inválida" no painel do operador.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Não lança: a porta de origem já garante desfecho tipado com o texto original em toda recusa, e
 * este adaptador preserva essa garantia.
 */
@Component
public class GoogleRecuperacaoExternaAdapter implements RecuperacaoExternaRevisaoPort {

    private final RecuperacaoExternaPort recuperacaoExterna;

    /**
     * PROPÓSITO DE NEGÓCIO: injeta o tradutor externo da área de correção.
     * <p>INVARIANTES DO DOMÍNIO: guarda a referência recebida.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede o uso do adaptador.
     */
    public GoogleRecuperacaoExternaAdapter(RecuperacaoExternaPort recuperacaoExterna) {
        this.recuperacaoExterna = recuperacaoExterna;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: delega a tradução e converte o desfecho.
     * <p>INVARIANTES DO DOMÍNIO: o texto atravessa sem modificação; só o status é traduzido.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    @Override
    public ResultadoRecuperacaoExterna traduzir(String textoOriginal) {
        ResultadoRaspagem resultado = recuperacaoExterna.traduzir(textoOriginal);
        return new ResultadoRecuperacaoExterna(traduzirStatus(resultado), resultado.texto());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte o desfecho da fatia vizinha para o desta.
     * <p>INVARIANTES DO DOMÍNIO: exaustivo e sem {@code default} — desfecho novo lá vira erro de
     * compilação aqui, e não uma tradução errada em silêncio.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    private static StatusRecuperacaoExterna traduzirStatus(ResultadoRaspagem resultado) {
        return switch (resultado.status()) {
            case SUCESSO -> StatusRecuperacaoExterna.SUCESSO;
            case SEM_ALTERACAO -> StatusRecuperacaoExterna.SEM_ALTERACAO;
            case FALHA_TRANSITORIA -> StatusRecuperacaoExterna.FALHA_TRANSITORIA;
            case RESPOSTA_INVALIDA -> StatusRecuperacaoExterna.RESPOSTA_INVALIDA;
            case TAG_CORROMPIDA -> StatusRecuperacaoExterna.TAG_CORROMPIDA;
        };
    }
}
