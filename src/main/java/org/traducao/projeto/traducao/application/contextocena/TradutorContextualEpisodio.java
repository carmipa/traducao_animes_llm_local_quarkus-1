package org.traducao.projeto.traducao.application.contextocena;

import org.springframework.stereotype.Service;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;
import org.traducao.projeto.traducao.application.AvaliadorTraducaoCache;
import org.traducao.projeto.traducao.application.IsoladorQuebraDialogo;
import org.traducao.projeto.traducao.domain.contextocena.JanelaContextual;
import org.traducao.projeto.traducao.domain.contextocena.LinhaAlvoContextual;
import org.traducao.projeto.traducao.domain.contextocena.RequisicaoTraducaoContextual;
import org.traducao.projeto.traducao.domain.contextocena.ResumoContextoCena;
import org.traducao.projeto.traducao.domain.contextocena.TradutorContextualPort;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.traducao.infrastructure.contextocena.ContextoCenaProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: traduz as falas de DIÁLOGO de um episódio COM contexto de cena,
 * reaproveitando integralmente as máquinas por-fala que já existem — isola o {@code \N}
 * mid-sentence ({@link IsoladorQuebraDialogo}), MASCARA as tags ASS ({@link MascaradorTags})
 * antes de chamar o modelo e as restaura depois, e valida o resultado
 * ({@link AvaliadorTraducaoCache}) — para NÃO corromper a legenda. Só o diálogo passa por
 * aqui; música/letreiro/romaji seguem pela via de tradução de hoje. É o coração do piloto de
 * correção de gênero, atrás da flag; a chave do cache é a assinatura contextual só-fonte
 * ({@link ChaveadorContextual}), que impede o colapso de falas iguais de cenas diferentes.
 *
 * <p>INVARIANTES DO DOMÍNIO: a fala-alvo é MASCARADA (o modelo nunca vê a sintaxe de estilo);
 * as vizinhas entram como referência com as tags REMOVIDAS (não são restauradas); marcador
 * {@code [[TAGn]]} corrompido na resposta ⇒ MANTÉM o original (mesmo fallback do fluxo OFF);
 * tradução que reprova na validação canônica ⇒ pendente (mantém o original na legenda,
 * branco no cache); só entra no cache contextual a fala com assinatura. Só JDK + peers já
 * consumidos pela fatia {@code traducao} (sem acoplamento novo).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: o adaptador da porta já devolve o original em falha de
 * rede; aqui, alucinação de tags e reprova de validação também degradam para "manter
 * original", nunca para legenda corrompida. Nenhum I/O.
 */
@Service
public class TradutorContextualEpisodio {

    /** Versão da política contextual — deve casar com {@code ContextoCenaProperties} (proveniência). */
    public static final String POLITICA_VERSAO = "v1";

    private final MontadorJanelaContextual montadorJanela;
    private final ChaveadorContextual chaveador;
    private final MascaradorTags mascarador;
    private final IsoladorQuebraDialogo isolador;
    private final TradutorContextualPort tradutorContextual;
    private final AvaliadorTraducaoCache avaliador;
    private final ContextoCenaProperties contextoCena;
    private final TradutorProperties propriedades;

    public TradutorContextualEpisodio(
            MontadorJanelaContextual montadorJanela, ChaveadorContextual chaveador,
            MascaradorTags mascarador, IsoladorQuebraDialogo isolador,
            TradutorContextualPort tradutorContextual, AvaliadorTraducaoCache avaliador,
            ContextoCenaProperties contextoCena, TradutorProperties propriedades) {
        this.montadorJanela = montadorJanela;
        this.chaveador = chaveador;
        this.mascarador = mascarador;
        this.isolador = isolador;
        this.tradutorContextual = tradutorContextual;
        this.avaliador = avaliador;
        this.contextoCena = contextoCena;
        this.propriedades = propriedades;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: resultado da tradução contextual de um episódio — a tradução por
     * índice de evento (para a reconstrução não colapsar falas iguais), as entradas de cache
     * com assinatura e os contadores do A/B.
     * <p>INVARIANTES DO DOMÍNIO: {@code traducaoPorIndice} tem uma entrada por fala de diálogo
     * processada; entradas com tradução em branco marcam pendência.
     * <p>COMPORTAMENTO EM CASO DE FALHA: portador de dados puro.
     */
    public record ResultadoContextual(
        Map<Integer, String> traducaoPorIndice,
        List<EntradaCache> entradas,
        ResumoContextoCena resumo
    ) {}

    /**
     * PROPÓSITO DE NEGÓCIO: traduz, com contexto de cena, as falas de diálogo indicadas.
     * <p>INVARIANTES DO DOMÍNIO: cada índice em {@code indicesDialogo} vira uma tradução (ou o
     * original mantido em pendência/alucinação); reaproveita o cache contextual por assinatura.
     * <p>COMPORTAMENTO EM CASO DE FALHA: ver contrato da classe (degrada para manter original).
     *
     * @param eventos lista ordenada de eventos do documento
     * @param indicesDialogo posições dos eventos de diálogo a traduzir
     * @param promptCongelado prompt de sistema congelado do job
     * @param cacheContextual mapa assinatura → tradução já existente (reuso)
     * @return {@link ResultadoContextual}
     */
    public ResultadoContextual traduzir(List<EventoLegenda> eventos, Set<Integer> indicesDialogo,
            String promptCongelado, Map<String, String> cacheContextual) {
        Map<Integer, String> traducaoPorIndice = new HashMap<>();
        List<EntradaCache> entradas = new ArrayList<>();
        int contextualizadas = 0;
        int reaproveitadas = 0;
        int pendentes = 0;
        long caracteresContexto = 0;
        int janelaTam = contextoCena.tamanhoJanela();
        Map<String, String> cache = cacheContextual != null ? cacheContextual : Map.of();

        for (int i = 0; i < eventos.size(); i++) {
            if (!indicesDialogo.contains(i)) {
                continue;
            }
            EventoLegenda ev = eventos.get(i);
            String original = ev.texto();
            if (original == null) {
                continue;
            }

            JanelaContextual janela = montadorJanela.montar(eventos, i, janelaTam);
            List<String> vizinhas = vizinhasOriginais(janela);
            String assinatura = chaveador.assinatura(i, original, ev.estilo(), vizinhas, POLITICA_VERSAO);

            String cacheado = cache.get(assinatura);
            if (cacheado != null && avaliador.isCacheReaproveitavel(original, cacheado)) {
                traducaoPorIndice.put(i, cacheado);
                entradas.add(entrada(ev, cacheado, assinatura));
                reaproveitadas++;
                continue;
            }

            IsoladorQuebraDialogo.FalaIsolada isolada = isolador.isolar(original);
            MascaradorTags.Mascarado masc = mascarador.mascarar(isolada.textoSemQuebra());
            JanelaContextual janelaMascarada = mascararJanela(janela, masc.texto());
            caracteresContexto += somaCaracteres(janela);

            String maskedTrad = tradutorContextual.traduzirComContexto(
                new RequisicaoTraducaoContextual(promptCongelado, janelaMascarada));

            String traduzido;
            try {
                traduzido = mascarador.desmascarar(maskedTrad, masc.tags());
                traduzido = isolador.reaplicar(traduzido, isolada.quebras());
            } catch (AlucinacaoDetectadaException e) {
                traduzido = original; // tags corrompidas: mantém original (mesmo fallback do OFF)
            }

            String motivo = avaliador.motivoFalhaFinal(original, traduzido);
            if (motivo != null) {
                traducaoPorIndice.put(i, original);       // pendente: mantém original na legenda
                entradas.add(entrada(ev, "", assinatura)); // branco no cache
                pendentes++;
            } else {
                traducaoPorIndice.put(i, traduzido);
                entradas.add(entrada(ev, traduzido, assinatura));
                contextualizadas++;
            }
        }
        return new ResultadoContextual(traducaoPorIndice, entradas,
            new ResumoContextoCena(contextualizadas, reaproveitadas, pendentes, caracteresContexto));
    }

    private static List<String> vizinhasOriginais(JanelaContextual janela) {
        List<String> v = new ArrayList<>();
        janela.antes().forEach(l -> v.add(l.texto()));
        janela.depois().forEach(l -> v.add(l.texto()));
        return v;
    }

    private JanelaContextual mascararJanela(JanelaContextual janela, String alvoMascarado) {
        LinhaAlvoContextual alvo = new LinhaAlvoContextual(
            janela.alvo().indice(), janela.alvo().estilo(), alvoMascarado);
        return new JanelaContextual(alvo, semTags(janela.antes()), semTags(janela.depois()));
    }

    private static List<LinhaAlvoContextual> semTags(List<LinhaAlvoContextual> linhas) {
        return linhas.stream()
            .map(l -> new LinhaAlvoContextual(l.indice(), l.estilo(), limparTags(l.texto())))
            .toList();
    }

    private static String limparTags(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replaceAll("\\{[^}]*\\}", "").replaceAll("\\\\[Nnh]", " ").trim();
    }

    private static long somaCaracteres(JanelaContextual janela) {
        long n = 0;
        for (LinhaAlvoContextual l : janela.antes()) {
            n += l.texto() == null ? 0 : l.texto().length();
        }
        for (LinhaAlvoContextual l : janela.depois()) {
            n += l.texto() == null ? 0 : l.texto().length();
        }
        return n;
    }

    private EntradaCache entrada(EventoLegenda ev, String traduzido, String assinatura) {
        return new EntradaCache(ev.indice(), ev.estilo(), ev.texto(), traduzido,
            propriedades.idiomaOriginal(), propriedades.idiomaTraduzido(), assinatura);
    }
}
