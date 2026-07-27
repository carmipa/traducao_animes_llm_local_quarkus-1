package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.raspagemRevisao.domain.ContextoRevisao;
import org.traducao.projeto.raspagemRevisao.domain.exceptions.RaspagemRevisaoException;
import org.traducao.projeto.traducaoCorrige.application.ContextoManutencaoCacheService;

import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: decide sob QUAL lore um arquivo será revisado, e ativa essa lore — ou
 * recusa o arquivo. É a porta de entrada de toda a revisão: escolhida a obra errada, cada correção
 * seguinte aplica a terminologia de outra série.
 *
 * <h2>Duas testemunhas, não uma</h2>
 * A proveniência gravada no cache é a primeira testemunha e vence a seleção manual da interface —
 * cache versionado sabe de que obra veio. Mas o carimbo pode nascer errado: um cache de Gundam 0083
 * carimbado {@code guilty_crown} passava, a lore errada ficava ativa e VAZAVA para o arquivo
 * seguinte da varredura, reescrevendo o {@code .ass} com a terminologia de outra obra. A pasta em
 * que o cache mora é a segunda testemunha, e as duas precisam concordar.
 *
 * <p>Essa guarda é a MESMA de {@code ContextoManutencaoCacheService} — não uma cópia. Duplicar aqui
 * a política do veredicto criaria duas guardas que divergem com o tempo, que é como o reforço de
 * terminologia acabou com duas implementações desiguais.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A verificação obra×contexto acontece ANTES de {@code definirContextoAtivo}. Uma lore
 *       reprovada não pode chegar a ficar ativa nem por um instante — se ficar, vaza para o próximo
 *       arquivo mesmo com este bloqueado.</li>
 *   <li>Este colaborador IMPRIME, ao contrário dos demais extraídos nesta fase. É deliberado: os
 *       avisos de contexto saem ANTES das checagens que podem lançar, e devolvê-los ao chamador
 *       para impressão posterior os perderia justamente no caminho de falha — o operador veria o
 *       erro sem saber que sua seleção manual havia sido ignorada. Preservar a ordem original das
 *       mensagens vale mais aqui que a pureza do padrão.</li>
 *   <li>A saída deste serviço vai para a CAIXA DE TEXTO DO NAVEGADOR, não só para um terminal:
 *       o {@code ConsoleRedirector} troca o {@code System.out} do processo por um fluxo que
 *       duplica tudo para o SSE, e o canal ("correcao") é um {@code ThreadLocal} definido pela
 *       fila na thread do job. Este serviço é chamado SINCRONAMENTE nessa thread, e é isso que
 *       faz suas mensagens chegarem ao painel certo. Um {@code parallelStream()} aqui dentro
 *       mandaria a saída para threads do pool, que não têm o ThreadLocal: as linhas cairiam no
 *       canal padrão e SUMIRIAM do painel de Correção, sem erro nenhum.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Contexto inexistente OU obra incompatível lançam {@link RaspagemRevisaoException} e interrompem o
 * arquivo antes de qualquer chamada externa ou sobrescrita da legenda.
 */
@Service
public class AtivadorContextoRevisao {

    private final GerenciadorContexto gerenciadorContexto;
    private final ContextoManutencaoCacheService contextoManutencaoCache;

    /**
     * PROPÓSITO DE NEGÓCIO: reúne o registro de contextos e a guarda obra×contexto.
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede a criação do ativador.
     */
    public AtivadorContextoRevisao(
            GerenciadorContexto gerenciadorContexto,
            ContextoManutencaoCacheService contextoManutencaoCache) {
        this.gerenciadorContexto = gerenciadorContexto;
        this.contextoManutencaoCache = contextoManutencaoCache;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: resolve o contexto do arquivo pela proveniência, pela seleção manual ou
     * pelo contexto ativo herdado — nessa ordem de precedência — e o ativa depois de conferir que
     * ele bate com a obra da pasta.
     *
     * <p>INVARIANTES DO DOMÍNIO: proveniência versionada sempre vence a seleção manual; a seleção da
     * interface é fallback apenas para cache legado. O contexto resolvido, venha de onde vier, ainda
     * precisa BATER COM A OBRA DA PASTA antes de ser ativado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link RaspagemRevisaoException} sem ter ativado
     * nada.
     *
     * @param proveniencia carimbo do cache, quando existe
     * @param contextoFallback seleção manual da interface, quando existe
     * @param cachePath caminho do cache, usado para reconhecer a obra pela pasta
     * @return a lore ativa e os termos protegidos da obra
     */
    public ContextoRevisao ativar(
        ProvenienciaCache proveniencia,
        String contextoFallback,
        Path cachePath
    ) {
        String contextoProveniencia = proveniencia != null ? proveniencia.contextoId() : null;
        String contextoEfetivo = contextoProveniencia != null && !contextoProveniencia.isBlank()
            ? contextoProveniencia : contextoFallback;

        if (contextoProveniencia != null && !contextoProveniencia.isBlank()
            && contextoFallback != null && !contextoFallback.isBlank()
            && !contextoProveniencia.equals(contextoFallback)) {
            out(AnsiCores.YELLOW + "  [CONTEXTO] Seleção manual \"" + contextoFallback
                + "\" ignorada: a proveniência do cache exige \"" + contextoProveniencia + "\"."
                + AnsiCores.RESET);
        }
        if (contextoEfetivo == null || contextoEfetivo.isBlank()) {
            contextoEfetivo = gerenciadorContexto.obterIdContextoAtivo();
            out(AnsiCores.YELLOW + "  [CONTEXTO] Cache legado sem proveniência e sem seleção; "
                + "usando contexto ativo \"" + contextoEfetivo + "\"." + AnsiCores.RESET);
        }
        if (!gerenciadorContexto.existeContexto(contextoEfetivo)) {
            throw new RaspagemRevisaoException(
                "Contexto \"" + contextoEfetivo + "\" do cache não existe no projeto: " + cachePath);
        }
        // ANTES de definirContextoAtivo: uma lore reprovada não pode chegar a ficar ativa, senão
        // vaza para o próximo arquivo da varredura mesmo com este bloqueado.
        try {
            contextoManutencaoCache.exigirObraCompativel(
                cachePath, contextoEfetivo, contextoProveniencia != null && !contextoProveniencia.isBlank());
        } catch (IllegalArgumentException e) {
            throw new RaspagemRevisaoException(e.getMessage());
        }

        gerenciadorContexto.definirContextoAtivo(contextoEfetivo);
        out(AnsiCores.CYAN + "  Contexto ativo: " + gerenciadorContexto.obterNomeContextoAtivo()
            + " (fonte: " + (contextoProveniencia != null ? "proveniência do cache" : "seleção/fallback")
            + ")" + AnsiCores.RESET);
        return new ContextoRevisao(
            contextoEfetivo,
            gerenciadorContexto.obterLoreAtiva(),
            gerenciadorContexto.termosProtegidosAtivos());
    }

    private void out(String mensagem) {
        System.out.println(mensagem);
    }
}
