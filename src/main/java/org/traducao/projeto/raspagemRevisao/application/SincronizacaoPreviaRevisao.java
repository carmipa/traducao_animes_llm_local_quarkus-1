package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.raspagemCorrecao.application.ProtetorTermosLoreService;
import org.traducao.projeto.raspagemRevisao.domain.ContextoRevisao;
import org.traducao.projeto.raspagemRevisao.domain.FrescorCache;
import org.traducao.projeto.raspagemRevisao.domain.ModoReferenciaRevisao;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: traz para a legenda o que a etapa anterior já corrigiu no cache, ANTES de a
 * revisão começar. É a ponte 5→6: sem ela, a revisão gastaria LLM e Google reconsertando falas que
 * o operador já tinha consertado — e poderia chegar a uma resposta diferente da que ele aprovou.
 *
 * <h2>Quando a ponte abre</h2>
 * Normalmente só quando o cache é MAIS NOVO que a legenda: cache antigo não tem autoridade para
 * sobrescrever trabalho posterior. A exceção é a RECUPERAÇÃO — falas que voltaram exatamente ao
 * inglês são restauradas mesmo com cache antigo, porque uma fala em inglês nunca é o estado
 * desejado, e a tradução persistida é melhor que ela por definição.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Falas formadas SÓ por termos canônicos da lore ficam protegidas da sincronização. "Axis"
 *       continua "Axis": elas coincidem com o inglês por estarem certas, não por estarem
 *       pendentes.</li>
 *   <li>No modo Cache, a escrita fica restrita aos índices com vínculo confirmado. No modo Ambos
 *       não há restrição, que é o comportamento histórico.</li>
 *   <li>NÃO imprime. Devolve o desfecho com os índices tocados, e quem narra ao operador é o caso
 *       de uso — aqui as mensagens saem todas juntas depois da sincronização, então coletar não
 *       inverte ordem nenhuma. (No {@code PreparadorReferenciaRevisao} inverteria, e por isso lá o
 *       serviço imprime.)</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Datas ilegíveis desligam a sincronização automática ({@link FrescorCache#INDETERMINADO}) e o
 * chamador avisa — na dúvida, preserva-se o ASS atual.
 */
@Service
public class SincronizacaoPreviaRevisao {

    private final ResolvedorArtefatosRevisao resolvedorArtefatos;
    private final SincronizadorLegendaCacheService sincronizadorCache;
    private final ProtetorTermosLoreService protetorLore;

    /**
     * PROPÓSITO DE NEGÓCIO: reúne quem compara datas, quem sincroniza e quem conhece a lore.
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede a criação do serviço.
     */
    public SincronizacaoPreviaRevisao(
            ResolvedorArtefatosRevisao resolvedorArtefatos,
            SincronizadorLegendaCacheService sincronizadorCache,
            ProtetorTermosLoreService protetorLore) {
        this.resolvedorArtefatos = resolvedorArtefatos;
        this.sincronizadorCache = sincronizadorCache;
        this.protetorLore = protetorLore;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o que a ponte 5→6 fez, com detalhe suficiente para o operador entender.
     *
     * @param documento a legenda depois da sincronização
     * @param frescor o desfecho da comparação de datas, que o chamador avisa quando indeterminado
     * @param sincronizou se a ponte abriu por data
     * @param indicesSincronizados falas trazidas do cache mais novo
     * @param indicesRecuperadosDoOriginal falas que haviam voltado ao inglês e foram restauradas
     */
    public record Resultado(
        DocumentoLegenda documento,
        FrescorCache frescor,
        boolean sincronizou,
        List<Integer> indicesSincronizados,
        List<Integer> indicesRecuperadosDoOriginal
    ) {
        /**
         * PROPÓSITO DE NEGÓCIO: quantas falas a ponte materializou.
         * <p>INVARIANTES DO DOMÍNIO: conta só as sincronizadas por data — as recuperadas entram na
         * mesma contagem do serviço de origem, e este total espelha o que ele devolveu.
         * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
         */
        public int total() {
            return indicesSincronizados.size();
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aplica a ponte 5→6 e devolve o que mudou.
     *
     * <p>INVARIANTES DO DOMÍNIO: a proteção de termos canônicos é calculada ANTES da sincronização
     * e passada a ela — calcular depois olharia um documento já alterado e protegeria a fala
     * errada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: datas ilegíveis apenas desligam a sincronização.
     *
     * @param documentoPt a legenda lida do disco
     * @param entradasCache as entradas do cache pareado
     * @param cachePath caminho do cache, para comparar datas
     * @param arquivoPt caminho da legenda, para comparar datas
     * @param contexto lore ativa da obra
     * @param referencia modo de referência em vigor
     * @param originaisPorIndice índices com vínculo confirmado (restringem a escrita no modo Cache)
     * @return o desfecho da sincronização
     */
    public Resultado sincronizar(
        DocumentoLegenda documentoPt,
        List<EntradaCache> entradasCache,
        Path cachePath,
        Path arquivoPt,
        ContextoRevisao contexto,
        ModoReferenciaRevisao referencia,
        Map<Integer, String> originaisPorIndice
    ) {
        FrescorCache frescor = resolvedorArtefatos.compararFrescor(cachePath, arquivoPt);
        boolean sincronizarCache = frescor == FrescorCache.CACHE_MAIS_NOVO;
        Set<Integer> indicesCanonicosProtegidos = localizarIndicesCanonicosProtegidos(
            documentoPt, entradasCache, contexto);
        // Modo Cache: a sincronização só pode escrever índices com vínculo seguro
        // (as chaves de originaisPorIndice). AMBOS mantém null = comportamento histórico.
        Set<Integer> indicesPermitidosSync = referencia == ModoReferenciaRevisao.CACHE
            ? originaisPorIndice.keySet() : null;
        SincronizadorLegendaCacheService.Resultado sincronizacao = sincronizadorCache.sincronizar(
            documentoPt, entradasCache, sincronizarCache, indicesCanonicosProtegidos, indicesPermitidosSync);
        return new Resultado(sincronizacao.documento(), frescor, sincronizarCache,
            sincronizacao.indicesSincronizados(), sincronizacao.indicesRecuperadosDoOriginal());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: identifica falas válidas que coincidem com o inglês apenas porque são
     * formadas exclusivamente por nomes ou termos canônicos.
     *
     * <p>INVARIANTES DO DOMÍNIO: exige igualdade EXATA com o original do cache, evento dialogado e
     * confirmação pelo protetor da lore ativa. As três condições juntas: sem elas, uma fala
     * pendente parecida com um nome próprio seria protegida e nunca corrigida.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entradas ausentes retornam conjunto vazio; nenhuma fala
     * ambígua recebe proteção automática.
     */
    private Set<Integer> localizarIndicesCanonicosProtegidos(
        DocumentoLegenda documento,
        List<EntradaCache> entradas,
        ContextoRevisao contexto
    ) {
        if (documento == null || entradas == null || entradas.isEmpty() || contexto == null) {
            return Set.of();
        }
        Map<Integer, EntradaCache> porIndice = new HashMap<>();
        for (EntradaCache entrada : entradas) {
            porIndice.putIfAbsent(entrada.indice(), entrada);
        }
        Set<Integer> protegidos = new LinkedHashSet<>();
        for (EventoLegenda evento : documento.eventos()) {
            EntradaCache entrada = porIndice.get(evento.indice());
            if (!evento.isDialogo() || entrada == null || entrada.original() == null
                || !entrada.original().equals(evento.texto())) {
                continue;
            }
            if (protetorLore.contemSomenteTermosCanonicos(
                entrada.original(), contexto.lore(), contexto.termosProtegidos())) {
                protegidos.add(evento.indice());
            }
        }
        return Set.copyOf(protegidos);
    }
}
