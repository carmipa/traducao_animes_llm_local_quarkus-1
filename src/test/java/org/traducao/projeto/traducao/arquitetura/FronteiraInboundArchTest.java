package org.traducao.projeto.traducao.arquitetura;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPOSITO DE NEGOCIO: congela a fronteira funcional INBOUND da fatia vertical
 * Traducao Local ({@code org.traducao.projeto.traducao}) — todas as dependencias
 * {@code outra-fatia -> traducao..}. E a contraparte do fitness OUTBOUND
 * (FronteiraTraducaoArchTest), com o MESMO rigor: baseline estrita por aresta
 * exata (FQN de origem -> FQN de destino, normalizados a classe de topo), lida do
 * bytecode, NUNCA por nome de fatia. As regras da C2 (OUTBOUND, config, core,
 * telemetria, revisaoLore) permanecem separadas na sua propria classe.
 *
 * <h2>Invariantes do dominio</h2>
 * <ul>
 *   <li>O conjunto real de arestas INBOUND deve ser EXATAMENTE
 *       {@link #INBOUND_ESPERADAS}. Baseline auditada da FASE E: 150 arestas;
 *       apos E1: <b>148</b>.</li>
 *   <li>A FASE E encolhe esta allowlist subfase a subfase. E1 removeu exatamente 2
 *       arestas (RemuxerController -> RemuxRequest e ExtracaoLegendaController ->
 *       ExtracaoRequest), pois esses records foram movidos para as fatias
 *       proprietarias (remuxer / legendasExtracao .presentation.web), virando
 *       dependencias intra-fatia.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer aresta inesperada (nova entrada) ou esperada ausente (removida fora do
 * escopo da subfase corrente) reprova o teste, listando exatamente o desvio.
 */
class FronteiraInboundArchTest {

    private static final String RAIZ = "org.traducao.projeto";
    private static final String PREFIXO = RAIZ + ".";
    private static final String FATIA_TRADUCAO = "traducao";
    private static final String PKG_TRADUCAO = RAIZ + ".traducao";

    /** Baseline estrita INBOUND (outras fatias -> traducao), por aresta exata. */
    private static final Set<String> INBOUND_ESPERADAS = Set.of(
        aresta("org.traducao.projeto.analisadorMidia.presentation.AnalisadorMidiaCLI", "org.traducao.projeto.traducao.infrastructure.config.TradutorProperties"),
        aresta("org.traducao.projeto.analisadorMidia.presentation.AnalisadorMidiaCLI", "org.traducao.projeto.traducao.presentation.ui.ConsoleEntrada"),
        aresta("org.traducao.projeto.analisadorMidia.presentation.AnalisadorMidiaCLI", "org.traducao.projeto.traducao.presentation.ui.PastasExecucao"),
        aresta("org.traducao.projeto.apiDadosAnime.infrastructure.adapters.AniListApiClientAdapter", "org.traducao.projeto.traducao.infrastructure.config.LlmProperties"),
        aresta("org.traducao.projeto.apiDadosAnime.infrastructure.adapters.AniListApiClientAdapter", "org.traducao.projeto.traducao.infrastructure.http.JsonHttpClient"),
        aresta("org.traducao.projeto.apiDadosAnime.infrastructure.adapters.JikanApiClientAdapter", "org.traducao.projeto.traducao.infrastructure.config.LlmProperties"),
        aresta("org.traducao.projeto.apiDadosAnime.infrastructure.adapters.JikanApiClientAdapter", "org.traducao.projeto.traducao.infrastructure.http.JsonHttpClient"),
        aresta("org.traducao.projeto.apiDadosAnime.infrastructure.adapters.TmdbApiClientAdapter", "org.traducao.projeto.traducao.infrastructure.config.LlmProperties"),
        aresta("org.traducao.projeto.apiDadosAnime.infrastructure.adapters.TmdbApiClientAdapter", "org.traducao.projeto.traducao.infrastructure.http.JsonHttpClient"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.AuditorConteudoUseCase", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.AuditorConteudoUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.AuditorConteudoUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaSrt"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.RegraAlucinacaoQuebraLinha", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.RegraAlucinacaoQuebraLinha", "org.traducao.projeto.traducao.domain.legenda.EventoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.RegraDanoKaraoke", "org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.RegraDanoKaraoke", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.RegraDanoKaraoke", "org.traducao.projeto.traducao.domain.legenda.EventoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.RegraEfeitoVazado", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.RegraEfeitoVazado", "org.traducao.projeto.traducao.domain.legenda.EventoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.RegraIntegridadePareamento", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.RegraIntegridadePareamento", "org.traducao.projeto.traducao.domain.legenda.EventoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.RegraMetadadosAss", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.RegraSincroniaEstilos", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico.RegraEfeitoComTextoLongo", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico.RegraEfeitoComTextoLongo", "org.traducao.projeto.traducao.domain.legenda.EventoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico.RegraEventoDialogoVazio", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico.RegraEventoDialogoVazio", "org.traducao.projeto.traducao.domain.legenda.EventoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico.RegraQuebrasLinhaExcessivas", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico.RegraQuebrasLinhaExcessivas", "org.traducao.projeto.traducao.domain.legenda.EventoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico.RegraSobreposicaoTempo", "org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico.RegraSobreposicaoTempo", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico.RegraSobreposicaoTempo", "org.traducao.projeto.traducao.domain.legenda.EventoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico.RegraTagOverrideNaoFechada", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico.RegraTagOverrideNaoFechada", "org.traducao.projeto.traducao.domain.legenda.EventoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico.RegraTimestampInvalido", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico.RegraTimestampInvalido", "org.traducao.projeto.traducao.domain.legenda.EventoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo", "org.traducao.projeto.traducao.domain.legenda.EventoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaArquivoUnico", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaConteudo", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.auditorConteudoLegendas.domain.TempoEventoUtil", "org.traducao.projeto.traducao.domain.legenda.EventoLegenda"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorretorTraducaoLlmService", "org.traducao.projeto.traducao.application.ProtecaoLegendaAssService"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorretorTraducaoLlmService", "org.traducao.projeto.traducao.application.ValidadorTraducaoService"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorretorTraducaoLlmService", "org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorretorTraducaoLlmService", "org.traducao.projeto.traducao.domain.ports.MistralPort"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorretorTraducaoLlmService", "org.traducao.projeto.traducao.infrastructure.legenda.MascaradorTags"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorrigirLegendasUseCase", "org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorrigirLegendasUseCase", "org.traducao.projeto.traducao.application.ProtecaoLegendaAssService"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorrigirLegendasUseCase", "org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorrigirLegendasUseCase", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorrigirLegendasUseCase", "org.traducao.projeto.traducao.domain.legenda.EventoLegenda"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorrigirLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.config.TradutorProperties"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorrigirLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorrigirLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaAss"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorrigirLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss"),
        aresta("org.traducao.projeto.correcaoLegendas.application.CorrigirLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.MascaradorTags"),
        aresta("org.traducao.projeto.correcaoLegendas.presentation.CorrecaoLegendasController", "org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto"),
        aresta("org.traducao.projeto.legendasExtracao.presentation.ExtratorCLI", "org.traducao.projeto.traducao.infrastructure.config.TradutorProperties"),
        aresta("org.traducao.projeto.legendasExtracao.presentation.ExtratorCLI", "org.traducao.projeto.traducao.presentation.ui.ConsoleEntrada"),
        aresta("org.traducao.projeto.legendasExtracao.presentation.ExtratorCLI", "org.traducao.projeto.traducao.presentation.ui.PastasExecucao"),
        aresta("org.traducao.projeto.mapaProjeto.presentation.MapaProjetoCLI", "org.traducao.projeto.traducao.infrastructure.config.TradutorProperties"),
        aresta("org.traducao.projeto.novoKaraoke.application.ConversorKaraokeUseCase", "org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService"),
        aresta("org.traducao.projeto.raspagemCorrecao.CorretorRaspagemCLI", "org.traducao.projeto.traducao.infrastructure.config.TradutorProperties"),
        aresta("org.traducao.projeto.raspagemCorrecao.application.CorrigirComGoogleUseCase", "org.traducao.projeto.traducao.infrastructure.cache.CacheManutencaoService"),
        aresta("org.traducao.projeto.raspagemCorrecao.application.CorrigirComGoogleUseCase", "org.traducao.projeto.traducao.infrastructure.cache.ProvenienciaCache"),
        aresta("org.traducao.projeto.raspagemRevisao.RevisorLegendasCLI", "org.traducao.projeto.traducao.infrastructure.config.TradutorProperties"),
        aresta("org.traducao.projeto.raspagemRevisao.RevisorRaspagemCLI", "org.traducao.projeto.traducao.domain.StatusLlm"),
        aresta("org.traducao.projeto.raspagemRevisao.RevisorRaspagemCLI", "org.traducao.projeto.traducao.domain.ports.MistralPort"),
        aresta("org.traducao.projeto.raspagemRevisao.RevisorRaspagemCLI", "org.traducao.projeto.traducao.infrastructure.config.TradutorProperties"),
        aresta("org.traducao.projeto.raspagemRevisao.application.AuditorProblemasLegendaService", "org.traducao.projeto.traducao.application.DetectorTraducaoIdenticaService"),
        aresta("org.traducao.projeto.raspagemRevisao.application.AuditorProblemasLegendaService", "org.traducao.projeto.traducao.application.ValidadorTraducaoService"),
        aresta("org.traducao.projeto.raspagemRevisao.application.AuditorProblemasLegendaService", "org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException"),
        aresta("org.traducao.projeto.raspagemRevisao.application.LeitorCacheReferenciaService", "org.traducao.projeto.traducao.infrastructure.cache.CacheManutencaoService"),
        aresta("org.traducao.projeto.raspagemRevisao.application.LeitorCacheReferenciaService", "org.traducao.projeto.traducao.infrastructure.cache.EntradaCache"),
        aresta("org.traducao.projeto.raspagemRevisao.application.LeitorCacheReferenciaService", "org.traducao.projeto.traducao.infrastructure.cache.ProvenienciaCache"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarCacheUseCase", "org.traducao.projeto.traducao.application.ProtecaoLegendaAssService"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarCacheUseCase", "org.traducao.projeto.traducao.application.ValidadorTraducaoService"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarCacheUseCase", "org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarCacheUseCase", "org.traducao.projeto.traducao.domain.ports.MistralPort"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarCacheUseCase", "org.traducao.projeto.traducao.infrastructure.cache.CacheManutencaoService"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarCacheUseCase", "org.traducao.projeto.traducao.infrastructure.cache.ProvenienciaCache"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarCacheUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.MascaradorTags"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.application.ProtecaoLegendaAssService"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.application.ValidadorTraducaoService"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.domain.legenda.EventoLegenda"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.domain.ports.MistralPort"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.cache.EntradaCache"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.cache.ProvenienciaCache"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.config.TradutorProperties"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaAss"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss"),
        aresta("org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.MascaradorTags"),
        aresta("org.traducao.projeto.raspagemRevisao.application.SincronizadorLegendaCacheService", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.raspagemRevisao.application.SincronizadorLegendaCacheService", "org.traducao.projeto.traducao.domain.legenda.EventoLegenda"),
        aresta("org.traducao.projeto.raspagemRevisao.application.SincronizadorLegendaCacheService", "org.traducao.projeto.traducao.infrastructure.cache.EntradaCache"),
        aresta("org.traducao.projeto.raspagemRevisao.presentation.web.RevisaoLegendasController", "org.traducao.projeto.traducao.domain.StatusLlm"),
        aresta("org.traducao.projeto.raspagemRevisao.presentation.web.RevisaoLegendasController", "org.traducao.projeto.traducao.domain.ports.MistralPort"),
        aresta("org.traducao.projeto.raspagemRevisao.presentation.web.RevisaoLegendasController", "org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto"),
        aresta("org.traducao.projeto.remuxer.presentation.RemuxerCLI", "org.traducao.projeto.traducao.infrastructure.config.TradutorProperties"),
        aresta("org.traducao.projeto.remuxer.presentation.RemuxerCLI", "org.traducao.projeto.traducao.presentation.ui.ConsoleEntrada"),
        aresta("org.traducao.projeto.remuxer.presentation.RemuxerCLI", "org.traducao.projeto.traducao.presentation.ui.PastasExecucao"),
        aresta("org.traducao.projeto.revisaoLore.application.RevisarLoreUseCase", "org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService"),
        aresta("org.traducao.projeto.revisaoLore.application.RevisarLoreUseCase", "org.traducao.projeto.traducao.application.ProtecaoLegendaAssService"),
        aresta("org.traducao.projeto.revisaoLore.application.RevisarLoreUseCase", "org.traducao.projeto.traducao.application.ValidadorTraducaoService"),
        aresta("org.traducao.projeto.revisaoLore.application.RevisarLoreUseCase", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.revisaoLore.application.RevisarLoreUseCase", "org.traducao.projeto.traducao.domain.legenda.EventoLegenda"),
        aresta("org.traducao.projeto.revisaoLore.application.RevisarLoreUseCase", "org.traducao.projeto.traducao.infrastructure.config.TradutorProperties"),
        aresta("org.traducao.projeto.revisaoLore.application.RevisarLoreUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaAss"),
        aresta("org.traducao.projeto.revisaoLore.application.RevisarLoreUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss"),
        aresta("org.traducao.projeto.revisaoLore.application.RevisarLoreUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.MascaradorTags"),
        aresta("org.traducao.projeto.traducaoCorrige.CorretorCacheCLI", "org.traducao.projeto.traducao.infrastructure.config.TradutorProperties"),
        aresta("org.traducao.projeto.traducaoCorrige.application.ClassificadorEntradaCacheService", "org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService"),
        aresta("org.traducao.projeto.traducaoCorrige.application.ClassificadorEntradaCacheService", "org.traducao.projeto.traducao.application.DetectorTraducaoIdenticaService"),
        aresta("org.traducao.projeto.traducaoCorrige.application.ClassificadorEntradaCacheService", "org.traducao.projeto.traducao.application.ProtecaoLegendaAssService"),
        aresta("org.traducao.projeto.traducaoCorrige.application.ClassificadorEntradaCacheService", "org.traducao.projeto.traducao.application.ValidadorTraducaoService"),
        aresta("org.traducao.projeto.traducaoCorrige.application.ClassificadorEntradaCacheService", "org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException"),
        aresta("org.traducao.projeto.traducaoCorrige.application.ClassificadorEntradaCacheService", "org.traducao.projeto.traducao.infrastructure.config.TradutorProperties"),
        aresta("org.traducao.projeto.traducaoCorrige.application.ContextoManutencaoCacheService", "org.traducao.projeto.traducao.infrastructure.cache.CacheManutencaoService"),
        aresta("org.traducao.projeto.traducaoCorrige.application.ContextoManutencaoCacheService", "org.traducao.projeto.traducao.infrastructure.cache.ProvenienciaCache"),
        aresta("org.traducao.projeto.traducaoCorrige.application.ContextoManutencaoCacheService", "org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto"),
        aresta("org.traducao.projeto.traducaoCorrige.application.LimparCacheUseCase", "org.traducao.projeto.traducao.infrastructure.cache.CacheManutencaoService"),
        aresta("org.traducao.projeto.traducaoCorrige.application.LimparCacheUseCase", "org.traducao.projeto.traducao.infrastructure.cache.ProvenienciaCache"),
        aresta("org.traducao.projeto.traducaoCorrige.presentation.web.CorrecaoCacheController", "org.traducao.projeto.traducao.domain.StatusLlm"),
        aresta("org.traducao.projeto.traducaoCorrige.presentation.web.CorrecaoCacheController", "org.traducao.projeto.traducao.domain.ports.MistralPort"),
        aresta("org.traducao.projeto.traducaoCorrige.presentation.web.CorrecaoCacheController", "org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.ClassificadorLetraKaraokeService", "org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.application.ValidadorTraducaoService"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.domain.Lote"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.domain.StatusLlm"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.domain.TraducaoLote"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.domain.legenda.EventoLegenda"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.domain.ports.MistralPort"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.infrastructure.cache.CacheTraducaoService"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.infrastructure.cache.EntradaCache"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.infrastructure.config.TradutorProperties"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaAss"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss"),
        aresta("org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.MascaradorTags"),
        aresta("org.traducao.projeto.traducaoKaraoke.presentation.TraducaoKaraokeController", "org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto"),
        aresta("org.traducao.projeto.trocaTipoLegenda.application.TrocaTipoLegendaUseCase", "org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda"),
        aresta("org.traducao.projeto.trocaTipoLegenda.application.TrocaTipoLegendaUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaAss"),
        aresta("org.traducao.projeto.trocaTipoLegenda.application.TrocaTipoLegendaUseCase", "org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss"),
        aresta("__SENTINELA_NUNCA_EXISTE__", "__SENTINELA__") // marcador; removido logo abaixo
    );

    private static JavaClasses classesProducao;

    @BeforeAll
    static void importar() {
        classesProducao = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RAIZ);
    }

    @Test
    @DisplayName("Entradas funcionais (outras fatias -> Traducao Local) == baseline estrita da FASE E")
    void inboundBateComBaselineExata() {
        Set<String> reais = new TreeSet<>();
        for (JavaClass classe : classesProducao) {
            if (ehDaTraducao(classe)) {
                continue; // origem deve estar FORA de traducao
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                if (FATIA_TRADUCAO.equals(fatiaDe(dependencia.getTargetClass().getPackageName()))) {
                    reais.add(aresta(origem, topo(dependencia.getTargetClass().getName())));
                }
            }
        }

        Set<String> esperadas = new TreeSet<>(INBOUND_ESPERADAS);
        esperadas.remove(aresta("__SENTINELA_NUNCA_EXISTE__", "__SENTINELA__"));

        Set<String> inesperadas = new TreeSet<>(reais);
        inesperadas.removeAll(esperadas);
        Set<String> ausentes = new TreeSet<>(esperadas);
        ausentes.removeAll(reais);

        assertTrue(inesperadas.isEmpty() && ausentes.isEmpty(),
            () -> "Divergencia na baseline INBOUND da FASE E. Esperado=" + esperadas.size()
                + " Real=" + reais.size() + "\n"
                + "Arestas INESPERADAS (novas): " + inesperadas + "\n"
                + "Arestas ESPERADAS AUSENTES: " + ausentes);
    }

    private static String aresta(String origemFqn, String destinoFqn) {
        return origemFqn + " -> " + destinoFqn;
    }

    private static boolean ehDaTraducao(JavaClass classe) {
        String pkg = classe.getPackageName();
        return pkg.equals(PKG_TRADUCAO) || pkg.startsWith(PKG_TRADUCAO + ".");
    }

    private static String topo(String nomeCompleto) {
        int cifrao = nomeCompleto.indexOf('$');
        return cifrao < 0 ? nomeCompleto : nomeCompleto.substring(0, cifrao);
    }

    private static String fatiaDe(String pkg) {
        if (pkg == null || !pkg.startsWith(PREFIXO)) {
            return null;
        }
        String resto = pkg.substring(PREFIXO.length());
        int ponto = resto.indexOf('.');
        return ponto < 0 ? resto : resto.substring(0, ponto);
    }
}
