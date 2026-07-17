================================================================================
 MAPA ESTRUTURAL DO PROJETO - TRACKER ANIMES
================================================================================
 Raiz do repositorio      : traducao_animes_llm_local_quarkus
 Pastas mapeadas          : 312
 Arquivos (na arvore)     : 590
 Arquivos-fonte indexados : 485  (.java: 485 | .py: 0)
 Memoria viva do projeto  : CEREBRO_IA.md (na raiz do repositorio)

 Objetivo: mapa de contexto para LLMs navegarem os diretorios e
 atualizarem a documentacao oficial. Geracao estatica e automatica.
================================================================================

--------------------------------------------------------------------------------
 1. ARVORE DE DIRETORIOS
--------------------------------------------------------------------------------
traducao_animes_llm_local_quarkus/
├── .codex/
│   └── config.toml
├── .vscode/
│   └── settings.json
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── src/
│   ├── main/
│   │   ├── docker/
│   │   │   ├── Dockerfile.jvm
│   │   │   ├── Dockerfile.legacy-jar
│   │   │   ├── Dockerfile.native
│   │   │   └── Dockerfile.native-micro
│   │   ├── java/
│   │   │   └── org/
│   │   │       └── traducao/
│   │   │           └── projeto/
│   │   │               ├── analisadorMidia/
│   │   │               │   ├── application/
│   │   │               │   │   ├── AnalisarMidiaUseCase.java
│   │   │               │   │   ├── BarraProgressoAnalise.java
│   │   │               │   │   ├── ClassificadorLegendaService.java
│   │   │               │   │   ├── LocalizadorVideosService.java
│   │   │               │   │   ├── RelatorioMidiaTextoFormatter.java
│   │   │               │   │   └── TelemetriaMidiaMapper.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── exceptions/
│   │   │               │   │   │   └── AnaliseStreamException.java
│   │   │               │   │   ├── AnalisadorException.java
│   │   │               │   │   ├── AnexoInfo.java
│   │   │               │   │   ├── AudioInfo.java
│   │   │               │   │   ├── AuditoriaResultado.java
│   │   │               │   │   ├── CapituloInfo.java
│   │   │               │   │   ├── ContainerInfo.java
│   │   │               │   │   ├── FalhaAnalise.java
│   │   │               │   │   ├── LegendaInfo.java
│   │   │               │   │   ├── ResultadoAnaliseLote.java
│   │   │               │   │   └── VideoInfo.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   └── adapters/
│   │   │               │   │       └── FfprobeAdapter.java
│   │   │               │   └── presentation/
│   │   │               │       ├── ui/
│   │   │               │       │   └── ConsoleAnalisadorLogger.java
│   │   │               │       ├── web/
│   │   │               │       │   └── AnaliseMidiaController.java
│   │   │               │       └── AnalisadorMidiaCLI.java
│   │   │               ├── apiDadosAnime/
│   │   │               │   ├── application/
│   │   │               │   │   └── ObterMetadataAnimeUseCase.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── exceptions/
│   │   │               │   │   │   ├── AnimeNaoEncontradoException.java
│   │   │               │   │   │   └── ApiDadosAnimeException.java
│   │   │               │   │   └── model/
│   │   │               │   │       └── AnimeMetadata.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   ├── adapters/
│   │   │               │   │   │   ├── AniListApiClientAdapter.java
│   │   │               │   │   │   ├── JikanApiClientAdapter.java
│   │   │               │   │   │   └── TmdbApiClientAdapter.java
│   │   │               │   │   └── config/
│   │   │               │   │       └── ApiDadosAnimeHttpProperties.java
│   │   │               │   └── presentation/
│   │   │               │       └── web/
│   │   │               │           └── AnimeMetadataController.java
│   │   │               ├── auditorConteudoLegendas/
│   │   │               │   ├── application/
│   │   │               │   │   ├── regras/
│   │   │               │   │   │   ├── arquivounico/
│   │   │               │   │   │   │   ├── RegraEfeitoComTextoLongo.java
│   │   │               │   │   │   │   ├── RegraEventoDialogoVazio.java
│   │   │               │   │   │   │   ├── RegraQuebrasLinhaExcessivas.java
│   │   │               │   │   │   │   ├── RegraSobreposicaoTempo.java
│   │   │               │   │   │   │   ├── RegraTagOverrideNaoFechada.java
│   │   │               │   │   │   │   └── RegraTimestampInvalido.java
│   │   │               │   │   │   ├── RegraAlucinacaoQuebraLinha.java
│   │   │               │   │   │   ├── RegraDanoKaraoke.java
│   │   │               │   │   │   ├── RegraEfeitoVazado.java
│   │   │               │   │   │   ├── RegraIntegridadePareamento.java
│   │   │               │   │   │   ├── RegraMetadadosAss.java
│   │   │               │   │   │   └── RegraSincroniaEstilos.java
│   │   │               │   │   ├── AuditorConteudoUseCase.java
│   │   │               │   │   ├── TelemetriaAuditoriaService.java
│   │   │               │   │   └── ValidadorParsingLegenda.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── AnomaliaConteudo.java
│   │   │               │   │   ├── AuditoriaConteudoRelatorioJson.java
│   │   │               │   │   ├── AuditoriaException.java
│   │   │               │   │   ├── ModoAuditoria.java
│   │   │               │   │   ├── RegraAuditoriaArquivoUnico.java
│   │   │               │   │   ├── RegraAuditoriaConteudo.java
│   │   │               │   │   ├── RelatorioAuditoriaConteudo.java
│   │   │               │   │   └── TempoEventoUtil.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   └── AuditoriaConteudoPersistencia.java
│   │   │               │   └── presentation/
│   │   │               │       └── AuditorConteudoController.java
│   │   │               ├── cachetraducao/
│   │   │               │   ├── domain/
│   │   │               │   │   ├── CacheDocumento.java
│   │   │               │   │   ├── EntradaCache.java
│   │   │               │   │   └── ProvenienciaCache.java
│   │   │               │   └── infrastructure/
│   │   │               │       ├── CacheManutencaoService.java
│   │   │               │       └── CacheTraducaoService.java
│   │   │               ├── config/
│   │   │               │   ├── AppConfig.java
│   │   │               │   ├── ExecucaoCli.java
│   │   │               │   └── ModoExecucaoStartup.java
│   │   │               ├── contexto/
│   │   │               │   ├── domain/
│   │   │               │   │   ├── ContextoNaoEncontradoException.java
│   │   │               │   │   ├── ContextoPrompt.java
│   │   │               │   │   ├── ExcecaoContexto.java
│   │   │               │   │   ├── ProvedorContexto.java
│   │   │               │   │   └── RegrasConcordanciaPtBr.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   ├── config/
│   │   │               │   │   │   └── ContextoBeansConfig.java
│   │   │               │   │   └── GerenciadorContexto.java
│   │   │               │   └── lore/
│   │   │               │       ├── danmachi/
│   │   │               │       │   ├── ContextoDanMachi.java
│   │   │               │       │   ├── ContextoDanMachiOrion.java
│   │   │               │       │   ├── ContextoDanMachiS1.java
│   │   │               │       │   ├── ContextoDanMachiS2.java
│   │   │               │       │   ├── ContextoDanMachiS3.java
│   │   │               │       │   ├── ContextoDanMachiS4.java
│   │   │               │       │   ├── ContextoDanMachiS5.java
│   │   │               │       │   └── ContextoDanMachiSwordOratoria.java
│   │   │               │       ├── eightsix/
│   │   │               │       │   └── Contexto86.java
│   │   │               │       ├── evangelion/
│   │   │               │       │   ├── ContextoEvangelion111.java
│   │   │               │       │   ├── ContextoEvangelion222.java
│   │   │               │       │   ├── ContextoEvangelion3010.java
│   │   │               │       │   ├── ContextoEvangelion333.java
│   │   │               │       │   └── ContextoEvangelionTV.java
│   │   │               │       ├── guiltycrown/
│   │   │               │       │   └── ContextoGuiltyCrown.java
│   │   │               │       ├── gundam/
│   │   │               │       │   ├── chars/
│   │   │               │       │   │   └── ContextoCharsCounterattack.java
│   │   │               │       │   ├── msteam/
│   │   │               │       │   │   └── ContextoGundam08thMSTeam.java
│   │   │               │       │   ├── reconguista/
│   │   │               │       │   │   └── ContextoGundamReconguista.java
│   │   │               │       │   ├── stardust/
│   │   │               │       │   │   └── ContextoGundam0083.java
│   │   │               │       │   ├── warInpocket/
│   │   │               │       │   │   └── ContextoWarInPocket.java
│   │   │               │       │   ├── zeta/
│   │   │               │       │   │   └── ContextoGundamZeta.java
│   │   │               │       │   ├── zz/
│   │   │               │       │   │   └── ContextoGundamZZ.java
│   │   │               │       │   ├── ContextoGundam0079.java
│   │   │               │       │   ├── ContextoGundamF91.java
│   │   │               │       │   ├── ContextoGundamHathaway.java
│   │   │               │       │   ├── ContextoGundamNT.java
│   │   │               │       │   ├── ContextoGundamOrigin.java
│   │   │               │       │   ├── ContextoGundamSEED.java
│   │   │               │       │   ├── ContextoGundamSEEDAstray.java
│   │   │               │       │   ├── ContextoGundamSEEDDestiny.java
│   │   │               │       │   ├── ContextoGundamSEEDFreedom.java
│   │   │               │       │   ├── ContextoGundamSEEDStargazer.java
│   │   │               │       │   ├── ContextoGundamUnicorn.java
│   │   │               │       │   └── ContextoGundamVictory.java
│   │   │               │       ├── macross/
│   │   │               │       │   ├── ContextoMacross2.java
│   │   │               │       │   ├── ContextoMacross7.java
│   │   │               │       │   ├── ContextoMacross7Encore.java
│   │   │               │       │   ├── ContextoMacross7Filme.java
│   │   │               │       │   ├── ContextoMacross7Filmes.java
│   │   │               │       │   ├── ContextoMacrossAnime.java
│   │   │               │       │   ├── ContextoMacrossDelta.java
│   │   │               │       │   ├── ContextoMacrossDeltaFilme1.java
│   │   │               │       │   ├── ContextoMacrossDeltaFilme2.java
│   │   │               │       │   ├── ContextoMacrossDeltaFilmes.java
│   │   │               │       │   ├── ContextoMacrossDynamite7.java
│   │   │               │       │   ├── ContextoMacrossDYRL.java
│   │   │               │       │   ├── ContextoMacrossFilme1.java
│   │   │               │       │   ├── ContextoMacrossFilme2.java
│   │   │               │       │   ├── ContextoMacrossFrontier.java
│   │   │               │       │   ├── ContextoMacrossFrontierFilme1.java
│   │   │               │       │   ├── ContextoMacrossFrontierFilme2.java
│   │   │               │       │   ├── ContextoMacrossFrontierFilmes.java
│   │   │               │       │   ├── ContextoMacrossPlus.java
│   │   │               │       │   └── ContextoMacrossZero.java
│   │   │               │       └── sidonia/
│   │   │               │           ├── ContextoKnightsOfSidonia.java
│   │   │               │           └── ContextoSidoniaFilme.java
│   │   │               ├── core/
│   │   │               │   ├── exception/
│   │   │               │   │   ├── web/
│   │   │               │   │   │   └── BasePipelineExceptionMapper.java
│   │   │               │   │   └── BasePipelineException.java
│   │   │               │   ├── execucao/
│   │   │               │   │   └── FilaExecucaoPipeline.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   └── http/
│   │   │               │   │       └── JsonHttpClient.java
│   │   │               │   ├── io/
│   │   │               │   │   └── DiretorioBaseKronos.java
│   │   │               │   ├── presentation/
│   │   │               │   │   ├── ui/
│   │   │               │   │   │   ├── AnsiCores.java
│   │   │               │   │   │   └── ConsoleEntrada.java
│   │   │               │   │   └── web/
│   │   │               │   │       ├── LogStreamService.java
│   │   │               │   │       ├── OperacaoRequest.java
│   │   │               │   │       ├── PipelineWebSupport.java
│   │   │               │   │       └── RespostaPadrao.java
│   │   │               │   └── util/
│   │   │               │       ├── ArquivoAtomicoUtil.java
│   │   │               │       ├── DuracaoUtil.java
│   │   │               │       └── ProcessoExternoUtil.java
│   │   │               ├── correcaoLegendas/
│   │   │               │   ├── application/
│   │   │               │   │   ├── CorretorTraducaoLlmService.java
│   │   │               │   │   ├── CorrigirLegendasUseCase.java
│   │   │               │   │   └── SanitizadorTagsService.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── CorrecaoLegendasRelatorioJson.java
│   │   │               │   │   ├── LogEventoCorrecaoLegendas.java
│   │   │               │   │   └── ResultadoCorrecaoLegendas.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   └── CorrecaoLegendasLogPersistencia.java
│   │   │               │   └── presentation/
│   │   │               │       └── CorrecaoLegendasController.java
│   │   │               ├── legenda/
│   │   │               │   ├── application/
│   │   │               │   │   └── DetectorEfeitoKaraokeService.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── ArquivoLegendaException.java
│   │   │               │   │   ├── DocumentoLegenda.java
│   │   │               │   │   ├── EventoLegenda.java
│   │   │               │   │   ├── ExcecaoLegenda.java
│   │   │               │   │   └── PoliticaEstiloMusical.java
│   │   │               │   └── infrastructure/
│   │   │               │       ├── config/
│   │   │               │       │   └── PoliticaEstiloMusicalProducer.java
│   │   │               │       ├── EscritorLegendaAss.java
│   │   │               │       ├── EscritorLegendaSrt.java
│   │   │               │       ├── LeitorLegendaAss.java
│   │   │               │       └── LeitorLegendaSrt.java
│   │   │               ├── legendasExtracao/
│   │   │               │   ├── application/
│   │   │               │   │   ├── strategy/
│   │   │               │   │   │   ├── ExtratorAssStrategy.java
│   │   │               │   │   │   ├── ExtratorPgsStrategy.java
│   │   │               │   │   │   ├── ExtratorSrtStrategy.java
│   │   │               │   │   │   └── ExtratorStrategy.java
│   │   │               │   │   ├── ExtrairLegendaUseCase.java
│   │   │               │   │   └── ValidadorSaidaExtracao.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── exceptions/
│   │   │               │   │   │   ├── ExtracaoTimeoutException.java
│   │   │               │   │   │   └── FormatoLegendaInvalidoException.java
│   │   │               │   │   ├── ports/
│   │   │               │   │   │   └── ExtratorVideoPort.java
│   │   │               │   │   ├── ExtratorException.java
│   │   │               │   │   ├── FaixaLegenda.java
│   │   │               │   │   ├── FormatoLegenda.java
│   │   │               │   │   ├── ItemExtracao.java
│   │   │               │   │   ├── RelatorioExtracao.java
│   │   │               │   │   └── StatusExtracao.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   ├── adapters/
│   │   │               │   │   │   ├── FfmpegAdapter.java
│   │   │               │   │   │   └── MkvToolNixAdapter.java
│   │   │               │   │   └── config/
│   │   │               │   │       ├── ExtracaoBeansConfig.java
│   │   │               │   │       └── ExtratorProperties.java
│   │   │               │   └── presentation/
│   │   │               │       ├── ui/
│   │   │               │       │   ├── ConsoleExtratorLogger.java
│   │   │               │       │   └── TabelaExtracaoRenderer.java
│   │   │               │       ├── web/
│   │   │               │       │   ├── ExtracaoLegendaController.java
│   │   │               │       │   └── ExtracaoRequest.java
│   │   │               │       └── ExtratorCLI.java
│   │   │               ├── llm/
│   │   │               │   └── domain/
│   │   │               │       ├── LlmPort.java
│   │   │               │       ├── Lote.java
│   │   │               │       ├── StatusLlm.java
│   │   │               │       └── TraducaoLote.java
│   │   │               ├── mapaProjeto/
│   │   │               │   ├── application/
│   │   │               │   │   ├── GeradorMapaProjetoUseCase.java
│   │   │               │   │   └── MapeadorDiretorioUseCase.java
│   │   │               │   ├── domain/
│   │   │               │   │   └── exceptions/
│   │   │               │   │       └── MapaProjetoException.java
│   │   │               │   └── presentation/
│   │   │               │       ├── web/
│   │   │               │       │   ├── MapaController.java
│   │   │               │       │   └── MapaResponse.java
│   │   │               │       └── MapaProjetoCLI.java
│   │   │               ├── mcp/
│   │   │               │   └── KronosMcpTools.java
│   │   │               ├── novoKaraoke/
│   │   │               │   ├── application/
│   │   │               │   │   └── ConversorKaraokeUseCase.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── EventoAss.java
│   │   │               │   │   ├── LinhaSimplesKaraoke.java
│   │   │               │   │   ├── NovoKaraokeException.java
│   │   │               │   │   └── ResultadoConversaoKaraoke.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   └── NovoKaraokePersistencia.java
│   │   │               │   └── presentation/
│   │   │               │       ├── NovoKaraokeController.java
│   │   │               │       └── NovoKaraokeRequest.java
│   │   │               ├── qualidadeTraducao/
│   │   │               │   ├── application/
│   │   │               │   │   ├── DetectorTraducaoIdenticaService.java
│   │   │               │   │   ├── MascaradorTags.java
│   │   │               │   │   ├── ProtecaoLegendaAssService.java
│   │   │               │   │   └── ValidadorTraducaoService.java
│   │   │               │   └── domain/
│   │   │               │       ├── AlucinacaoDetectadaException.java
│   │   │               │       ├── ExcecaoQualidadeTraducao.java
│   │   │               │       └── LoreAtivaPort.java
│   │   │               ├── raspagemCorrecao/
│   │   │               │   ├── application/
│   │   │               │   │   ├── CorrigirComGoogleUseCase.java
│   │   │               │   │   └── ProtetorTermosLoreService.java
│   │   │               │   ├── domain/
│   │   │               │   │   └── exceptions/
│   │   │               │   │       └── RaspagemCorrecaoException.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   ├── GoogleTranslateScraper.java
│   │   │               │   │   ├── ResultadoRaspagem.java
│   │   │               │   │   └── StatusRaspagem.java
│   │   │               │   └── CorretorRaspagemCLI.java
│   │   │               ├── raspagemRevisao/
│   │   │               │   ├── application/
│   │   │               │   │   ├── AuditorProblemasLegendaService.java
│   │   │               │   │   ├── CorretorDeterministicoConcordanciaService.java
│   │   │               │   │   ├── DetectorConcordanciaService.java
│   │   │               │   │   ├── LeitorCacheReferenciaService.java
│   │   │               │   │   ├── ResultadoRevisaoLegendas.java
│   │   │               │   │   ├── RevisarCacheUseCase.java
│   │   │               │   │   ├── RevisarLegendasUseCase.java
│   │   │               │   │   └── SincronizadorLegendaCacheService.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── exceptions/
│   │   │               │   │   │   └── RaspagemRevisaoException.java
│   │   │               │   │   └── ResultadoDeteccaoConcordancia.java
│   │   │               │   ├── presentation/
│   │   │               │   │   └── web/
│   │   │               │   │       └── RevisaoLegendasController.java
│   │   │               │   ├── RevisorLegendasCLI.java
│   │   │               │   └── RevisorRaspagemCLI.java
│   │   │               ├── remuxer/
│   │   │               │   ├── application/
│   │   │               │   │   ├── MapeadorMidiaService.java
│   │   │               │   │   └── RemuxarLoteUseCase.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── MkvToolNixNaoEncontradoException.java
│   │   │               │   │   ├── PlanoRemux.java
│   │   │               │   │   ├── RelatorioRemux.java
│   │   │               │   │   ├── RemuxerException.java
│   │   │               │   │   ├── RemuxTarefa.java
│   │   │               │   │   └── SaidaRemuxJaExisteException.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   ├── adapters/
│   │   │               │   │   │   └── MkvmergeAdapter.java
│   │   │               │   │   └── config/
│   │   │               │   │       └── RemuxerProperties.java
│   │   │               │   └── presentation/
│   │   │               │       ├── ui/
│   │   │               │       │   └── ConsoleRemuxerLogger.java
│   │   │               │       ├── web/
│   │   │               │       │   ├── RemuxerController.java
│   │   │               │       │   └── RemuxRequest.java
│   │   │               │       └── RemuxerCLI.java
│   │   │               ├── renomearArquivos/
│   │   │               │   ├── application/
│   │   │               │   │   ├── OperacaoRenomeacaoEmAndamentoException.java
│   │   │               │   │   └── RenomeadorUseCase.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── OperacaoRenomeacao.java
│   │   │               │   │   └── ResultadoRenomeacao.java
│   │   │               │   └── presentation/
│   │   │               │       └── web/
│   │   │               │           ├── RenomearArquivosController.java
│   │   │               │           └── RenomearArquivosRequest.java
│   │   │               ├── revisaoLore/
│   │   │               │   ├── application/
│   │   │               │   │   ├── DetectorTermosLoreService.java
│   │   │               │   │   ├── GerenciadorPromptRevisaoLore.java
│   │   │               │   │   ├── PromptRevisaoLore.java
│   │   │               │   │   ├── RevisarLoreUseCase.java
│   │   │               │   │   └── ValidadorCandidatoLoreService.java
│   │   │               │   ├── contexto/
│   │   │               │   │   ├── ContextoRevisaoLore86.java
│   │   │               │   │   ├── ContextoRevisaoLoreDanMachi.java
│   │   │               │   │   ├── ContextoRevisaoLoreDanMachiS4.java
│   │   │               │   │   ├── ContextoRevisaoLoreDanMachiS5.java
│   │   │               │   │   ├── ContextoRevisaoLoreGuiltyCrown.java
│   │   │               │   │   ├── ContextoRevisaoLoreGundam0080.java
│   │   │               │   │   ├── ContextoRevisaoLoreGundam0083.java
│   │   │               │   │   ├── ContextoRevisaoLoreGundam08thMSTeam.java
│   │   │               │   │   ├── ContextoRevisaoLoreGundamCCA.java
│   │   │               │   │   ├── ContextoRevisaoLoreGundamNT.java
│   │   │               │   │   ├── ContextoRevisaoLoreGundamUnicorn.java
│   │   │               │   │   ├── ContextoRevisaoLoreGundamZeta.java
│   │   │               │   │   └── ContextoRevisaoLoreGundamZZ.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── exceptions/
│   │   │               │   │   │   └── RevisaoLoreException.java
│   │   │               │   │   ├── ports/
│   │   │               │   │   │   ├── ProvedorPromptRevisaoLore.java
│   │   │               │   │   │   └── RevisorLoreLlmPort.java
│   │   │               │   │   ├── EntradaAuditoriaRevisaoLore.java
│   │   │               │   │   ├── LogEventoRevisaoLore.java
│   │   │               │   │   ├── ResultadoDeteccaoLore.java
│   │   │               │   │   ├── ResultadoRevisaoLore.java
│   │   │               │   │   ├── RevisaoLoreRelatorioJson.java
│   │   │               │   │   ├── StatusRevisaoLore.java
│   │   │               │   │   └── StatusRevisaoLoreLlm.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   ├── adapters/
│   │   │               │   │   │   ├── NormalizadorRespostaRevisaoLore.java
│   │   │               │   │   │   └── RevisorLoreLlmAdapter.java
│   │   │               │   │   ├── config/
│   │   │               │   │   │   └── RevisaoLoreLlmProperties.java
│   │   │               │   │   ├── dtos/
│   │   │               │   │   │   └── RevisaoLoreLlmDtos.java
│   │   │               │   │   ├── http/
│   │   │               │   │   │   └── RevisaoLoreHttpClient.java
│   │   │               │   │   ├── RevisaoLoreAuditoriaCache.java
│   │   │               │   │   └── RevisaoLoreLogPersistencia.java
│   │   │               │   └── presentation/
│   │   │               │       └── RevisaoLoreController.java
│   │   │               ├── sistema/
│   │   │               │   ├── application/
│   │   │               │   │   └── EncerrarAplicacaoUseCase.java
│   │   │               │   └── presentation/
│   │   │               │       └── SistemaController.java
│   │   │               ├── telemetria/
│   │   │               │   ├── presentation/
│   │   │               │   │   └── web/
│   │   │               │   │       ├── TelemetriaController.java
│   │   │               │   │       └── TelemetriaStreamResource.java
│   │   │               │   ├── AmbienteExecucaoDataset.java
│   │   │               │   ├── AmbienteExecucaoDatasetService.java
│   │   │               │   ├── LlmTelemetria.java
│   │   │               │   ├── MidiaTelemetria.java
│   │   │               │   ├── OperacaoHistorico.java
│   │   │               │   ├── OperacaoTelemetria.java
│   │   │               │   ├── RevisaoLoreTelemetriaResumo.java
│   │   │               │   ├── TelemetriaDatasetProperties.java
│   │   │               │   ├── TelemetriaDatasetService.java
│   │   │               │   ├── TelemetriaResumo.java
│   │   │               │   ├── TelemetriaService.java
│   │   │               │   └── TelemetriaTraducaoLeitura.java
│   │   │               ├── traducao/
│   │   │               │   ├── application/
│   │   │               │   │   ├── ProcessarArquivoUseCase.java
│   │   │               │   │   └── ProcessarEpisodioUseCase.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── exceptions/
│   │   │               │   │   │   ├── DivergenciaLinhasException.java
│   │   │               │   │   │   ├── EntradaJaTraduzidaException.java
│   │   │               │   │   │   ├── LlmFalhaComunicacaoException.java
│   │   │               │   │   │   ├── LmStudioOfflineException.java
│   │   │               │   │   │   ├── RespostaLlmVaziaException.java
│   │   │               │   │   │   ├── TraducaoParcialException.java
│   │   │               │   │   │   └── TradutorException.java
│   │   │               │   │   ├── legenda/
│   │   │               │   │   ├── ports/
│   │   │               │   │   │   └── TelemetriaTraducaoPort.java
│   │   │               │   │   ├── NormalizadorNomeEpisodio.java
│   │   │               │   │   ├── ResultadoTraducaoArquivo.java
│   │   │               │   │   ├── StatusArquivoTraducao.java
│   │   │               │   │   ├── StatusLoteTraducao.java
│   │   │               │   │   ├── TelemetriaTraducao.java
│   │   │               │   │   └── TelemetriaTraducaoDocumento.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   ├── adapters/
│   │   │               │   │   │   ├── LlmClientAdapter.java
│   │   │               │   │   │   └── LoreAtivaContextoAdapter.java
│   │   │               │   │   ├── config/
│   │   │               │   │   │   ├── LlmProperties.java
│   │   │               │   │   │   ├── RestClientConfig.java
│   │   │               │   │   │   └── TradutorProperties.java
│   │   │               │   │   ├── dtos/
│   │   │               │   │   │   └── RecordsLlm.java
│   │   │               │   │   ├── legenda/
│   │   │               │   │   └── telemetria/
│   │   │               │   │       └── TelemetriaTraducaoAdapter.java
│   │   │               │   └── presentation/
│   │   │               │       ├── bootstrap/
│   │   │               │       │   └── TraducaoStartup.java
│   │   │               │       ├── ui/
│   │   │               │       │   ├── ConsoleUILogger.java
│   │   │               │       │   ├── PastasExecucao.java
│   │   │               │       │   └── TabelaTraducaoRenderer.java
│   │   │               │       ├── web/
│   │   │               │       │   ├── BrowserLauncher.java
│   │   │               │       │   ├── ConsoleRedirector.java
│   │   │               │       │   ├── ContextoResponse.java
│   │   │               │       │   ├── DialogoArquivoController.java
│   │   │               │       │   ├── DocumentacaoController.java
│   │   │               │       │   ├── LlmStatusResponse.java
│   │   │               │       │   ├── LogStreamResource.java
│   │   │               │       │   ├── PipelineController.java
│   │   │               │       │   └── TraducaoController.java
│   │   │               │       └── TradutorCLI.java
│   │   │               ├── traducaoCorrige/
│   │   │               │   ├── application/
│   │   │               │   │   ├── ClassificadorEntradaCacheService.java
│   │   │               │   │   ├── ContextoManutencaoCacheService.java
│   │   │               │   │   └── LimparCacheUseCase.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── exceptions/
│   │   │               │   │   │   └── CorretorCacheException.java
│   │   │               │   │   ├── EntradaAuditoriaCorrecaoCache.java
│   │   │               │   │   └── ResultadoManutencaoCache.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   └── CorrecaoCacheAuditoria.java
│   │   │               │   ├── presentation/
│   │   │               │   │   └── web/
│   │   │               │   │       └── CorrecaoCacheController.java
│   │   │               │   └── CorretorCacheCLI.java
│   │   │               ├── traducaoKaraoke/
│   │   │               │   ├── application/
│   │   │               │   │   ├── ClassificadorLetraKaraokeService.java
│   │   │               │   │   └── TraduzirKaraokeUseCase.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── ClasseLinhaKaraoke.java
│   │   │               │   │   ├── ResultadoTraducaoKaraoke.java
│   │   │               │   │   └── TraducaoKaraokeException.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   └── TraducaoKaraokePersistencia.java
│   │   │               │   └── presentation/
│   │   │               │       ├── TraducaoKaraokeController.java
│   │   │               │       └── TraducaoKaraokeRequest.java
│   │   │               └── trocaTipoLegenda/
│   │   │                   ├── application/
│   │   │                   │   ├── AuditoriaFontesService.java
│   │   │                   │   └── TrocaTipoLegendaUseCase.java
│   │   │                   ├── domain/
│   │   │                   │   ├── exceptions/
│   │   │                   │   │   └── TrocaTipoLegendaException.java
│   │   │                   │   ├── AuditoriaFonteInfo.java
│   │   │                   │   ├── AuditoriaLegendaResultado.java
│   │   │                   │   ├── EntradaAuditoriaTrocaFonte.java
│   │   │                   │   ├── ResultadoGeralAuditoria.java
│   │   │                   │   └── ResultadoTrocaFonte.java
│   │   │                   ├── infrastructure/
│   │   │                   │   └── TrocaTipoLegendaAuditoriaCache.java
│   │   │                   └── presentation/
│   │   │                       └── TrocaTipoLegendaController.java
│   │   └── resources/
│   │       ├── static/
│   │       │   ├── analise/
│   │       │   │   ├── analise.css
│   │       │   │   └── analise.js
│   │       │   ├── auditorConteudoLegendas/
│   │       │   │   ├── auditorConteudoLegendas.css
│   │       │   │   ├── auditorConteudoLegendas.html
│   │       │   │   └── auditorConteudoLegendas.js
│   │       │   ├── correcao/
│   │       │   │   ├── correcao.css
│   │       │   │   └── correcao.js
│   │       │   ├── css/
│   │       │   │   └── base.css
│   │       │   ├── cura/
│   │       │   │   ├── cura.css
│   │       │   │   └── cura.js
│   │       │   ├── documentacao/
│   │       │   │   ├── documentacao.css
│   │       │   │   └── documentacao.js
│   │       │   ├── extracao/
│   │       │   │   ├── extracao.css
│   │       │   │   └── extracao.js
│   │       │   ├── i18n/
│   │       │   │   ├── flags/
│   │       │   │   │   ├── br.svg
│   │       │   │   │   ├── es.svg
│   │       │   │   │   └── us.svg
│   │       │   │   ├── i18n.css
│   │       │   │   └── i18n.js
│   │       │   ├── img/
│   │       │   │   ├── screenshots/
│   │       │   │   │   ├── analise-conteudo.png
│   │       │   │   │   ├── analise-midia.webp
│   │       │   │   │   ├── correcao-cache.webp
│   │       │   │   │   ├── cura-legendas.webp
│   │       │   │   │   ├── documentacao.webp
│   │       │   │   │   ├── extracao.webp
│   │       │   │   │   ├── karaoke-simples.png
│   │       │   │   │   ├── mapa-projeto.webp
│   │       │   │   │   ├── metadados-anime.webp
│   │       │   │   │   ├── painel-inicial.webp
│   │       │   │   │   ├── remuxer.webp
│   │       │   │   │   ├── renomear-arquivos.png
│   │       │   │   │   ├── revisao-legendas.webp
│   │       │   │   │   ├── revisao-lore.webp
│   │       │   │   │   ├── telemetria.webp
│   │       │   │   │   ├── traducao-karaoke.png
│   │       │   │   │   ├── traducao-local.webp
│   │       │   │   │   └── troca-tipo-legenda.png
│   │       │   │   ├── antigravity_banner.png
│   │       │   │   ├── antigravity_logo.png
│   │       │   │   ├── kronos_banner.png
│   │       │   │   ├── kronos_logo.png
│   │       │   │   └── kronos_logo.svg
│   │       │   ├── inicio/
│   │       │   │   ├── inicio.css
│   │       │   │   └── inicio.js
│   │       │   ├── js/
│   │       │   │   ├── app.js
│   │       │   │   ├── chart.umd.min.js
│   │       │   │   ├── marked.min.js
│   │       │   │   └── mermaid.min.js
│   │       │   ├── mapa/
│   │       │   │   ├── mapa.css
│   │       │   │   └── mapa.js
│   │       │   ├── novoKaraoke/
│   │       │   │   ├── novoKaraoke.css
│   │       │   │   ├── novoKaraoke.html
│   │       │   │   └── novoKaraoke.js
│   │       │   ├── remuxer/
│   │       │   │   ├── remuxer.css
│   │       │   │   └── remuxer.js
│   │       │   ├── renomearArquivos/
│   │       │   │   ├── renomearArquivos.css
│   │       │   │   ├── renomearArquivos.html
│   │       │   │   └── renomearArquivos.js
│   │       │   ├── revisao/
│   │       │   │   ├── revisao.css
│   │       │   │   └── revisao.js
│   │       │   ├── revisaoLore/
│   │       │   │   ├── revisaoLore.css
│   │       │   │   ├── revisaoLore.html
│   │       │   │   └── revisaoLore.js
│   │       │   ├── sobre/
│   │       │   │   ├── sobre.css
│   │       │   │   ├── sobre.html
│   │       │   │   └── sobre.js
│   │       │   ├── telemetria/
│   │       │   │   ├── telemetria.css
│   │       │   │   └── telemetria.js
│   │       │   ├── traducao/
│   │       │   │   ├── traducao.css
│   │       │   │   └── traducao.js
│   │       │   ├── traducaoKaraoke/
│   │       │   │   ├── traducaoKaraoke.css
│   │       │   │   ├── traducaoKaraoke.html
│   │       │   │   └── traducaoKaraoke.js
│   │       │   ├── trocaTipoLegenda/
│   │       │   │   ├── trocaTipoLegenda.css
│   │       │   │   ├── trocaTipoLegenda.html
│   │       │   │   └── trocaTipoLegenda.js
│   │       │   └── index.html
│   │       ├── application-local.yml
│   │       ├── application-local.yml.example
│   │       ├── application.properties
│   │       └── application.yml
│   └── test/
│       ├── java/
│       │   └── org/
│       │       └── traducao/
│       │           └── projeto/
│       │               ├── analisadorMidia/
│       │               │   ├── application/
│       │               │   │   ├── AnalisarMidiaClassificacaoTest.java
│       │               │   │   ├── AnalisarMidiaTelemetriaTest.java
│       │               │   │   ├── LocalizadorVideosServiceTest.java
│       │               │   │   └── TelemetriaMidiaMapperTest.java
│       │               │   ├── domain/
│       │               │   │   └── ResultadoAnaliseLoteSerializacaoTest.java
│       │               │   ├── infrastructure/
│       │               │   │   └── adapters/
│       │               │   │       └── FfprobeAdapterTest.java
│       │               │   └── presentation/
│       │               │       └── AnalisadorMidiaCLITest.java
│       │               ├── apiDadosAnime/
│       │               │   ├── application/
│       │               │   │   └── ObterMetadataAnimeUseCaseTest.java
│       │               │   └── infrastructure/
│       │               │       ├── adapters/
│       │               │       │   └── AniListApiClientAdapterTest.java
│       │               │       └── config/
│       │               │           └── ApiDadosAnimeHttpPropertiesIT.java
│       │               ├── arquitetura/
│       │               │   ├── ContextoInvalidoC2CaracterizacaoTest.java
│       │               │   └── ContratoJsonRecordsE1Test.java
│       │               ├── auditorConteudoLegendas/
│       │               │   ├── application/
│       │               │   │   ├── regras/
│       │               │   │   │   ├── RegraAlucinacaoQuebraLinhaTest.java
│       │               │   │   │   ├── RegraDanoKaraokeTest.java
│       │               │   │   │   ├── RegraEfeitoVazadoTest.java
│       │               │   │   │   ├── RegraMetadadosAssTest.java
│       │               │   │   │   └── RegraSincroniaEstilosTest.java
│       │               │   │   ├── AuditorConteudoIntegridadeTest.java
│       │               │   │   └── AuditorConteudoUseCaseTest.java
│       │               │   └── support/
│       │               │       └── AssAuditoriaFixtures.java
│       │               ├── cachetraducao/
│       │               │   ├── arquitetura/
│       │               │   │   └── FronteiraCacheTraducaoArchTest.java
│       │               │   ├── domain/
│       │               │   │   └── ProvenienciaCacheTest.java
│       │               │   └── infrastructure/
│       │               │       ├── CacheManutencaoServiceTest.java
│       │               │       ├── CacheTraducaoServiceTest.java
│       │               │       └── CompatibilidadeCacheJsonLegadoTest.java
│       │               ├── config/
│       │               │   └── ModoExecucaoDispatcherTest.java
│       │               ├── contexto/
│       │               │   ├── arquitetura/
│       │               │   │   └── FronteiraContextoArchTest.java
│       │               │   ├── domain/
│       │               │   │   └── HierarquiaExcecaoContextoTest.java
│       │               │   ├── ProtecaoConteudoLoreTest.java
│       │               │   └── RegistroProvedoresContextoIT.java
│       │               ├── core/
│       │               │   ├── exception/
│       │               │   │   └── BasePipelineExceptionTest.java
│       │               │   ├── execucao/
│       │               │   │   └── FilaExecucaoPipelineTest.java
│       │               │   ├── io/
│       │               │   │   └── DiretorioBaseKronosTest.java
│       │               │   └── presentation/
│       │               │       └── ui/
│       │               │           └── ConsoleEntradaCaracterizacaoTest.java
│       │               ├── correcaoLegendas/
│       │               │   └── application/
│       │               │       └── CorrigirLegendasUseCaseTest.java
│       │               ├── legenda/
│       │               │   ├── application/
│       │               │   │   └── DetectorEfeitoKaraokeServiceTest.java
│       │               │   ├── arquitetura/
│       │               │   │   └── FronteiraLegendaArchTest.java
│       │               │   ├── domain/
│       │               │   │   ├── HierarquiaExcecaoLegendaTest.java
│       │               │   │   └── PoliticaEstiloMusicalTest.java
│       │               │   └── infrastructure/
│       │               │       ├── config/
│       │               │       │   └── PoliticaEstiloMusicalProducerIT.java
│       │               │       ├── EscritorLegendaAssTest.java
│       │               │       └── LeitorEscritorSrtTest.java
│       │               ├── legendasExtracao/
│       │               │   ├── application/
│       │               │   │   ├── ExtrairLegendaUseCaseTest.java
│       │               │   │   └── ValidadorSaidaExtracaoTest.java
│       │               │   ├── infrastructure/
│       │               │   │   ├── adapters/
│       │               │   │   │   ├── FfmpegAdapterTest.java
│       │               │   │   │   └── MkvToolNixAdapterTest.java
│       │               │   │   └── config/
│       │               │   │       └── ExtratoresInjecaoIT.java
│       │               │   └── presentation/
│       │               │       └── ExtratorCLITest.java
│       │               ├── llm/
│       │               │   └── arquitetura/
│       │               │       └── FronteiraLlmArchTest.java
│       │               ├── mapaProjeto/
│       │               │   └── application/
│       │               │       └── GeradorMapaProjetoUseCaseTest.java
│       │               ├── mcp/
│       │               │   └── KronosMcpToolsTest.java
│       │               ├── novoKaraoke/
│       │               │   └── application/
│       │               │       └── ConversorKaraokeUseCaseTest.java
│       │               ├── qualidadeTraducao/
│       │               │   ├── application/
│       │               │   │   ├── DetectorTraducaoIdenticaServiceTest.java
│       │               │   │   ├── MascaradorTagsTest.java
│       │               │   │   └── ValidadorTraducaoServiceTest.java
│       │               │   ├── arquitetura/
│       │               │   │   └── FronteiraQualidadeTraducaoArchTest.java
│       │               │   └── domain/
│       │               │       └── HierarquiaExcecaoQualidadeTraducaoTest.java
│       │               ├── raspagemCorrecao/
│       │               │   ├── application/
│       │               │   │   ├── CorrigirComGoogleUseCaseTest.java
│       │               │   │   └── ProtetorTermosLoreServiceTest.java
│       │               │   └── infrastructure/
│       │               │       └── GoogleTranslateScraperTest.java
│       │               ├── raspagemRevisao/
│       │               │   └── application/
│       │               │       ├── CorretorDeterministicoConcordanciaServiceTest.java
│       │               │       ├── DetectorConcordanciaServiceTest.java
│       │               │       ├── LeitorCacheReferenciaServiceTest.java
│       │               │       ├── ResultadoRevisaoLegendasTest.java
│       │               │       ├── RevisarCacheUseCaseTest.java
│       │               │       ├── RevisarLegendasCacheIntegracaoTest.java
│       │               │       ├── RevisarLegendasCacheSeguroTest.java
│       │               │       ├── RevisarLegendasContextoTest.java
│       │               │       ├── RevisarLegendasProtecaoMassaTest.java
│       │               │       └── SincronizadorLegendaCacheServiceTest.java
│       │               ├── remuxer/
│       │               │   ├── application/
│       │               │   │   ├── MapeadorMidiaServiceTest.java
│       │               │   │   └── RemuxarLoteUseCaseTest.java
│       │               │   ├── infrastructure/
│       │               │   │   └── adapters/
│       │               │   │       └── MkvmergeAdapterTest.java
│       │               │   └── presentation/
│       │               │       └── RemuxerCLITest.java
│       │               ├── renomearArquivos/
│       │               │   └── application/
│       │               │       └── RenomeadorUseCaseTest.java
│       │               ├── revisaoLore/
│       │               │   ├── application/
│       │               │   │   ├── DetectorTermosLoreServiceTest.java
│       │               │   │   ├── RevisarLoreUseCaseRevisorFakeIT.java
│       │               │   │   ├── RevisarLoreUseCaseTest.java
│       │               │   │   └── ValidadorCandidatoLoreServiceTest.java
│       │               │   ├── contexto/
│       │               │   │   └── ContextosRevisaoLoreCatalogoTest.java
│       │               │   └── infrastructure/
│       │               │       ├── adapters/
│       │               │       │   ├── NormalizadorRespostaRevisaoLoreTest.java
│       │               │       │   ├── RevisorLoreLlmAdapterCaracterizacaoTest.java
│       │               │       │   ├── RevisorLoreLlmCdiIT.java
│       │               │       │   ├── RevisorLoreLlmDisponibilidadeTest.java
│       │               │       │   └── ServidorLlmDeTeste.java
│       │               │       └── RevisaoLoreAuditoriaCacheTest.java
│       │               ├── telemetria/
│       │               │   ├── IsolamentoArtefatosTest.java
│       │               │   ├── TelemetriaConsolidacaoTest.java
│       │               │   ├── TelemetriaDatasetPropertiesTest.java
│       │               │   ├── TelemetriaDatasetServiceTest.java
│       │               │   ├── TelemetriaServiceCompactacaoTest.java
│       │               │   └── TelemetriaServiceRevisaoLoreTest.java
│       │               ├── traducao/
│       │               │   ├── application/
│       │               │   │   ├── ProcessarArquivoUseCaseCaracterizacaoTest.java
│       │               │   │   ├── ProcessarArquivoUseCaseGuardTest.java
│       │               │   │   └── ProcessarEpisodioUseCaseAlucinacaoCaracterizacaoTest.java
│       │               │   ├── arquitetura/
│       │               │   │   ├── FronteiraInboundArchTest.java
│       │               │   │   ├── FronteiraTraducaoArchTest.java
│       │               │   │   └── GrafoCdiTraducaoIT.java
│       │               │   ├── domain/
│       │               │   │   ├── NormalizadorNomeEpisodioTest.java
│       │               │   │   └── StatusLoteTraducaoTest.java
│       │               │   ├── infrastructure/
│       │               │   │   ├── adapters/
│       │               │   │   │   ├── LlmClientAdapterRespostaRevisaoTest.java
│       │               │   │   │   └── LoreAtivaContextoAdapterTest.java
│       │               │   │   ├── config/
│       │               │   │   │   ├── ConfiguracaoSimplesE3bIT.java
│       │               │   │   │   ├── ParidadeBindingEstilosIT.java
│       │               │   │   │   ├── ParidadeBindingVazioIT.java
│       │               │   │   │   └── ParidadeResolucaoCaminhoE4bTest.java
│       │               │   │   ├── legenda/
│       │               │   │   └── telemetria/
│       │               │   │       └── TelemetriaTraducaoAdapterTest.java
│       │               │   └── presentation/
│       │               │       ├── web/
│       │               │       │   ├── ConsoleRedirectorTest.java
│       │               │       │   └── LogStreamServiceTest.java
│       │               │       └── TradutorCLIAlucinacaoCaracterizacaoTest.java
│       │               ├── traducaoCorrige/
│       │               │   ├── application/
│       │               │   │   ├── ClassificadorEntradaCacheServiceTest.java
│       │               │   │   └── LimparCacheUseCaseTest.java
│       │               │   └── domain/
│       │               │       └── ResultadoManutencaoCacheTest.java
│       │               ├── traducaoKaraoke/
│       │               │   └── application/
│       │               │       ├── ClassificadorLetraKaraokeServiceTest.java
│       │               │       └── TraduzirKaraokeUseCaseTest.java
│       │               ├── trocaTipoLegenda/
│       │               │   └── application/
│       │               │       ├── AuditoriaFontesServiceTest.java
│       │               │       └── TrocaTipoLegendaUseCaseTest.java
│       │               ├── ApiControllerTest.java
│       │               ├── ApiEndpointsTest.java
│       │               └── WebInterfaceTest.java
│       └── resources/
│           ├── cachetraducao/
│           │   └── legado.cache.json
│           └── contexto/
│               └── manifesto-lore.properties
├── .dockerignore
├── .gitignore
├── .mcp.json
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── hero-banner-atual.png
├── iniciar-kronos-dev.cmd
├── mapa_projeto.md
├── README.md
├── relatorio_diretorio_vps.txt
├── settings.gradle
└── transcricao_chat_isolamento_opcao4_2026-07-15.txt

--------------------------------------------------------------------------------
 2. TAXONOMIA DOS ARQUIVOS-FONTE (.java / .py)
--------------------------------------------------------------------------------

[PASTA] src/main/java/org/traducao/projeto/analisadorMidia/application/
  - AnalisarMidiaUseCase.java
      PROPÓSITO DE NEGÓCIO: orquestra a auditoria técnica de um lote de vídeos
      (Opção 1) — localiza os arquivos, executa o ffprobe, classifica as legendas,
      coleta sucessos e falhas, alimenta o dataset permanente de telemetria e
      devolve o resultado ESTRUTURADO ({@link ResultadoAnaliseLote}) que é a fonte
      única da interface HTML e da exportação TXT manual.
      
      <p>INVARIANTES DO DOMÍNIO: nenhum relatório é gravado em disco; a telemetria é
      persistida internamente e é um dataset permanente (acumula/deduplica). Uma
      falha em um arquivo não aborta o lote; a barra de progresso é cosmética e
      nunca aborta a análise. As responsabilidades técnicas (localização,
      classificação, formatação textual, mapeamento de telemetria) são delegadas a
      colaboradores dedicados; este use case apenas as coordena.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: lote vazio lança {@link AnalisadorException};
      falha por arquivo vira {@link FalhaAnalise} no resultado; falhas de telemetria
      são registradas sem interromper a análise.
  - BarraProgressoAnalise.java
      PROPÓSITO DE NEGÓCIO: encapsula a barra de progresso do console da análise de
      mídia, isolando a dependência de UI e, sobretudo, a política de que a barra é
      PURAMENTE COSMÉTICA — nunca pode abortar a análise do lote.
      
      <p>INVARIANTES DO DOMÍNIO: qualquer falha ao criar/avançar/fechar a barra
      (ex.: terminal incompatível) é contida; após uma falha a barra se autodesativa
      e a análise continua normalmente.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: nenhum método propaga exceção; falhas são
      apenas registradas em log em nível WARN.
  - ClassificadorLegendaService.java
      PROPÓSITO DE NEGÓCIO: classifica tecnicamente cada faixa de legenda pelo dado
      VITAL da Análise de Mídia — a traduzibilidade: legenda de TEXTO (ASS/SSA/SRT/
      WebVTT/MOV_TEXT) é extraível e traduzível; BITMAP (PGS/VobSub/DVB) exige OCR;
      ausência de faixa é RAW/hardsub. Decide se um episódio segue no pipeline de
      tradução.
      
      <p>INVARIANTES DO DOMÍNIO: PGS e VobSub são bitmap (imagem), NÃO hardsub;
      ausência de faixa softsub NÃO prova hardsub; uma faixa de texto tem prioridade
      sobre bitmap no veredito de traduzibilidade do arquivo.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: entradas nulas de codec/formato caem em
      "Desconhecido"/"DESCONHECIDO" sem exceção.
  - LocalizadorVideosService.java
      PROPÓSITO DE NEGÓCIO: localiza os arquivos de vídeo a auditar a partir de uma
      entrada que pode ser um único arquivo ou uma pasta (varredura recursiva),
      filtrando pelas extensões de contêiner suportadas.
      
      <p>INVARIANTES DO DOMÍNIO: só retorna arquivos regulares com extensão de vídeo
      conhecida; a ordem é estável (alfabética) para tornar a análise determinística.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: erro de I/O ao varrer a pasta lança
      {@link AnalisadorException} didática; entrada válida sem vídeos retorna lista
      vazia (o orquestrador decide como reportar).
  - RelatorioMidiaTextoFormatter.java
      PROPÓSITO DE NEGÓCIO: produz o relatório textual da auditoria de uma mídia a
      partir do resultado JÁ CLASSIFICADO (fonte única de verdade). Serve de base
      para exibição/exportação textual sem reimplementar a classificação — evita
      duas regras de formatação divergentes.
      
      <p>INVARIANTES DO DOMÍNIO: lê apenas o domínio estruturado
      ({@link AuditoriaResultado}); não reexecuta ffprobe nem reclassifica; a
      terminologia segue a do domínio (bitmap ≠ hardsub).
      
      <p>COMPORTAMENTO EM CASO DE FALHA: método puro sem I/O; listas vazias produzem
      as seções vazias correspondentes, sem exceção.
  - TelemetriaMidiaMapper.java
      PROPÓSITO DE NEGÓCIO: converte o resultado técnico de uma mídia auditada em
      um registro de telemetria anonimizado, alimentando o dataset permanente do
      projeto (diagnóstico + melhoria futura).
      
      <p>INVARIANTES DO DOMÍNIO: o caminho é relativizado à entrada para preservar
      privacidade (não grava caminhos pessoais absolutos); a telemetria carrega
      apenas metadados técnicos, nunca falas ou conteúdo da legenda.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: se a relativização falhar, cai para o nome
      simples do arquivo, sem lançar exceção.

[PASTA] src/main/java/org/traducao/projeto/analisadorMidia/domain/
  - AnalisadorException.java
      (sem cabecalho explicativo)
  - AnexoInfo.java
      Anexo do contêiner (ex.: fontes de karaokê em MKV), reportado pelo ffprobe
      como stream {@code codec_type: attachment}.
  - AudioInfo.java
      (sem cabecalho explicativo)
  - AuditoriaResultado.java
      (sem cabecalho explicativo)
  - CapituloInfo.java
      Capítulo (marcador de tempo) do contêiner, como reportado por
      {@code ffprobe -show_chapters}.
  - ContainerInfo.java
      (sem cabecalho explicativo)
  - FalhaAnalise.java
      Falha individual na análise de um arquivo do lote — representada no resultado
      (em vez de apenas logada), para que a UI exiba o que não pôde ser analisado.
  - LegendaInfo.java
      Faixa de legenda detectada, com classificação de traduzibilidade e flags do
      contêiner. Os indicadores temporais ({@code duracaoSegundos},
      {@code diferencaFimSegundos}) são apenas INFORMAÇÃO TÉCNICA — o módulo não
      emite veredito automático de sincronismo.
  - ResultadoAnaliseLote.java
      Resultado de uma execução de auditoria sobre um lote de vídeos: os arquivos
      analisados com sucesso e as falhas individuais. A análise não grava mais
      relatório em disco automaticamente — a exportação é manual (via UI).
  - VideoInfo.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/analisadorMidia/domain/exceptions/
  - AnaliseStreamException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/analisadorMidia/infrastructure/adapters/
  - FfprobeAdapter.java
      Executa ffprobe no vídeo e obtém o JSON com as informações gerais e faixas.
      Parsing do Container. Duração de fallback vinda dos streams: o
      ffprobe às vezes reporta a duração nos streams mas não no format.

[PASTA] src/main/java/org/traducao/projeto/analisadorMidia/presentation/
  - AnalisadorMidiaCLI.java
      PROPÓSITO DE NEGÓCIO: oferece execução local por terminal da esteira de auditoria
      e análise técnica de mídia da fatia {@code analisadorMidia}, resolvendo os caminhos
      de que precisa (entrada obrigatória e saída opcional) a partir da própria
      configuração, sem depender da configuração ou do estado da fatia {@code traducao}.
      
      <p>INVARIANTES DO DOMÍNIO: usa {@code tradutor.diretorio-entrada} e
      {@code tradutor.diretorio-saida}; saída ausente/vazia/blank significa "sem pasta de
      saída" (nunca aplica o fallback {@code traducao_ptbr}); não injeta cache.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: entrada não configurada ou caminho inexistente
      encerram o fluxo sem produzir auditoria; exceções durante o processamento são
      reportadas sem mascarar a causa.

[PASTA] src/main/java/org/traducao/projeto/analisadorMidia/presentation/ui/
  - ConsoleAnalisadorLogger.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/analisadorMidia/presentation/web/
  - AnaliseMidiaController.java
      PROPÓSITO DE NEGÓCIO: expõe a análise de mídia (Opção 1) à interface web,
      enfileirando o processamento pesado em segundo plano e publicando o relatório
      estruturado no canal SSE {@code analise-relatorio} para renderização no
      navegador.
      
      <p>Fronteira arquitetural: este endpoint pertence ao módulo
      {@code analisadorMidia} (Opção 1) e reside na sua camada de apresentação
      própria. Não importa nenhuma regra funcional da Tradução Local (Opção 4): usa
      apenas o use case do próprio módulo. As dependências
      {@link PipelineWebSupport}, {@link LogStreamService}, {@link RespostaPadrao} e
      {@link OperacaoRequest} são <b>glue técnico de apresentação</b> (fila única,
      SSE de logs, contratos de transporte HTTP) hoje localizado em
      {@code traducao.presentation.web}; é dívida técnica temporária cujo saneamento
      está reservado para a FASE E — não representa acoplamento funcional.
      
      <p>INVARIANTES DO DOMÍNIO: usa a MESMA fila compartilhada via
      {@link PipelineWebSupport} e o MESMO canal SSE; caminhos são normalizados antes
      do uso; a rota {@code POST /api/analisar}, o status, os campos de DTO e o canal
      SSE {@code analise-relatorio} são contrato público preservado exatamente como
      antes da movimentação.

[PASTA] src/main/java/org/traducao/projeto/apiDadosAnime/application/
  - ObterMetadataAnimeUseCase.java
      PROPÓSITO DE NEGÓCIO: fornece capa e dados oficiais da obra selecionada aos
      formulários, reutilizando cache e fontes externas redundantes.
      <p>
      INVARIANTES DO DOMÍNIO: cache válido tem prioridade; TMDB autenticado é a fonte
      preferencial, AniList é o fallback público primário e Jikan o último fallback.
      <p>
      COMPORTAMENTO EM CASO DE FALHA: fontes indisponíveis resultam em tentativa da
      próxima integração; sem resultado em todas elas, devolve {@link Optional#empty()}.

[PASTA] src/main/java/org/traducao/projeto/apiDadosAnime/domain/exceptions/
  - AnimeNaoEncontradoException.java
      (sem cabecalho explicativo)
  - ApiDadosAnimeException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/apiDadosAnime/domain/model/
  - AnimeMetadata.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/apiDadosAnime/infrastructure/adapters/
  - AniListApiClientAdapter.java
      PROPÓSITO DE NEGÓCIO: consulta a API pública GraphQL da AniList para manter
      capas e dados das obras disponíveis quando a fonte principal estiver fora.
      <p>
      INVARIANTES DO DOMÍNIO: pesquisa somente mídia do tipo ANIME; não exige chave
      ou autenticação; converte a nota percentual da AniList para a escala de 0 a 10.
      <p>
      COMPORTAMENTO EM CASO DE FALHA: registra a causa e devolve
      {@link Optional#empty()}, permitindo que o use case tente a próxima fonte.
  - JikanApiClientAdapter.java
      (sem cabecalho explicativo)
  - TmdbApiClientAdapter.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/apiDadosAnime/infrastructure/config/
  - ApiDadosAnimeHttpProperties.java
      PROPÓSITO DE NEGÓCIO: configuração HTTP própria da fatia {@code apiDadosAnime} —
      timeouts do cliente para as APIs públicas de metadados de anime (AniList/Jikan/TMDB).
      Isola a fatia da stack de LLM da Tradução Local ({@code tradutor.llm}), preservando
      exatamente os valores efetivos herdados (subfase E4a).
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Defaults efetivos: connect {@code 5s}, read {@code 180s}.</li>
      <li>Sem dependência de {@code LlmProperties} nem de qualquer tipo de {@code traducao}.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Valores nulos no binding são ignorados pelos setters, mantendo os defaults {@code 5s/180s}.

[PASTA] src/main/java/org/traducao/projeto/apiDadosAnime/presentation/web/
  - AnimeMetadataController.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/auditorConteudoLegendas/application/
  - AuditorConteudoUseCase.java
      PROPÓSITO DE NEGÓCIO: audita legendas em três escopos — só o original (EN), só
      o traduzido (PT-BR) ou os dois em comparação — produzindo um relatório didático
      com formato, integridade e anomalias.
      <p>INVARIANTES DO DOMÍNIO: somente arquivos regulares ASS, SSA ou SRT entram na
      auditoria; o modo comparativo executa as regras de par (original ↔ traduzido) e
      os modos de arquivo único executam as regras estruturais/temporais isoladas.
      <p>COMPORTAMENTO EM CASO DE FALHA: arquivo ausente, formato não suportado ou
      erro de leitura gera {@link AuditoriaException} sem relatório parcial.
  - TelemetriaAuditoriaService.java
      PROPÓSITO DE NEGÓCIO: transforma cada Análise de Legenda em telemetria e
      dataset JSON pesquisável, incluindo os formatos efetivamente processados.
      <p>INVARIANTES DO DOMÍNIO: métricas e relatório persistido descrevem a mesma
      execução e os mesmos arquivos.
      <p>COMPORTAMENTO EM CASO DE FALHA: falha de persistência é registrada, mas
      não invalida o resultado em memória da auditoria.
  - ValidadorParsingLegenda.java
      PROPÓSITO DE NEGÓCIO: audita o arquivo BRUTO para expor corrupções que os
      leitores tolerantes escondem — bloco SRT truncado, índice SRT não numérico e
      linha Dialogue/Comment ASS malformada. Sem isso, uma linha que deveria ser
      auditada era silenciosamente descartada e o arquivo saía "limpo".
      
      <p>INVARIANTES DO DOMÍNIO: é 100% leitura e nunca altera os leitores de legenda
      compartilhados pelo pipeline; reporta apenas o que só é visível no texto cru.
      A validação de sintaxe de tempo fica com {@code RegraTimestampInvalido}.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: arquivo ilegível gera uma anomalia crítica em
      vez de exceção; formato desconhecido devolve lista vazia.

[PASTA] src/main/java/org/traducao/projeto/auditorConteudoLegendas/application/regras/arquivounico/
  - RegraEfeitoComTextoLongo.java
      PROPÓSITO DE NEGÓCIO: versão de arquivo único da caça a "efeito vazado". Uma
      linha com tags de animação pesada (\t, \move, \clip, \fad) normalmente é um
      efeito visual curto; se ela carrega texto visível longo, é forte indício de
      que uma sentença completa vazou para dentro de um evento de efeito.
      
      <p>INVARIANTES DO DOMÍNIO: só eventos {@code Dialogue} com texto e com tag de
      animação pesada são avaliados; o alerta exige texto visível acima de
      {@value #LIMITE_TEXTO_VISIVEL} caracteres para evitar ruído.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: eventos sem tags de animação ou sem texto
      são ignorados; a regra nunca lança.
  - RegraEventoDialogoVazio.java
      PROPÓSITO DE NEGÓCIO: encontra eventos de diálogo que ficaram sem texto visível
      (só tags, quebras ou espaços). Numa tradução, isso costuma indicar uma fala
      perdida; num original, uma linha inútil que polui o tempo de exibição.
      
      <p>INVARIANTES DO DOMÍNIO: só eventos {@code Dialogue} são avaliados; o texto
      visível é o que sobra após remover blocos {@code {...}}, {@code \N}, {@code \h}
      e espaços.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: eventos que não são diálogo ou sem campo de
      texto são ignorados; a regra nunca lança.
  - RegraQuebrasLinhaExcessivas.java
      PROPÓSITO DE NEGÓCIO: aponta linhas com número anormal de quebras {@code \N}
      numa mesma fala. Sem arquivo de referência não dá para comparar com o original,
      então esta é a heurística de "formatação quebrada / alucinação" para arquivo
      único — muitas quebras costumam destruir posicionamento e legibilidade.
      
      <p>INVARIANTES DO DOMÍNIO: só eventos {@code Dialogue} com texto entram; o
      limite mínimo para alerta é {@value #LIMITE_QUEBRAS} quebras na mesma linha.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo é ignorado; a regra nunca lança.
  - RegraSobreposicaoTempo.java
      PROPÓSITO DE NEGÓCIO: detecta diálogos que se sobrepõem no tempo — uma fala que
      começa antes de a anterior terminar — apontando só sobreposições que realmente
      colidem na tela. Karaokê, placas, efeitos, estilos diferentes e camadas
      diferentes se sobrepõem por design e são ignorados para evitar milhares de
      falsos positivos.
      
      <p>INVARIANTES DO DOMÍNIO: só entram eventos {@code Dialogue} de "diálogo comum"
      (sem tags de karaokê, sem estilo de música, sem tags de posicionamento/efeito e
      sem campo Effect preenchido); a colisão só é reportada entre eventos do MESMO
      estilo e da MESMA camada (Layer), pois eles compartilham a mesma posição visual.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: eventos sem tempo interpretável ou de duração
      inválida são ignorados (a duração inválida é tratada pela
      {@code RegraTimestampInvalido}); a régua de karaokê/música é a mesma do resto do
      pipeline ({@link DetectorEfeitoKaraokeService}).
  - RegraTagOverrideNaoFechada.java
      PROPÓSITO DE NEGÓCIO: detecta blocos de override ASS ({@code {\...}}) abertos e
      nunca fechados numa única legenda. Uma chave desbalanceada faz o player exibir
      as tags como texto ou ignorar a linha inteira — dano estrutural que independe
      de arquivo de referência.
      
      <p>INVARIANTES DO DOMÍNIO: só eventos {@code Dialogue} com texto são avaliados;
      a contagem considera aninhamento inválido ({@code {} dentro de {}}) como
      malformação.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo/sem chaves não gera anomalia;
      cada evento é avaliado isoladamente e nunca lança.
  - RegraTimestampInvalido.java
      PROPÓSITO DE NEGÓCIO: sinaliza eventos cujo instante de fim é anterior ou igual
      ao de início. Uma linha com duração zero ou negativa não aparece na tela e
      costuma indicar corrupção de timestamps na legenda.
      
      <p>INVARIANTES DO DOMÍNIO: só eventos {@code Dialogue} com tempo legível são
      avaliados; a comparação usa milissegundos absolutos.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: evento sem tempo interpretável é ignorado
      (a regra {@link RegraTagOverrideNaoFechada} e as demais cobrem outros danos).

[PASTA] src/main/java/org/traducao/projeto/auditorConteudoLegendas/application/regras/
  - RegraAlucinacaoQuebraLinha.java
      (sem cabecalho explicativo)
  - RegraDanoKaraoke.java
      Detecta dano de tradução em karaokê/música comparando cada evento traduzido
      com o original. Usa o {@link DetectorEfeitoKaraokeService} como fonte única
      de verdade, a mesma régua da tradução, correção e revisão.
  - RegraEfeitoVazado.java
      (sem cabecalho explicativo)
  - RegraIntegridadePareamento.java
      PROPÓSITO DE NEGÓCIO: garante que o par original ↔ traduzido descreve o MESMO
      conjunto de falas antes de qualquer regra confiar no pareamento por índice.
      Sem ela, uma fala apagada, uma fala inventada ou um deslocamento por
      Comentário passavam despercebidos e o arquivo era declarado "limpo".
      
      <p>INVARIANTES DO DOMÍNIO: detecta divergência de contagem de diálogos, índices
      de diálogo ausentes no traduzido, índices extras no traduzido, índices
      duplicados (pareamento ambíguo) e mudança de tipo (Dialogue↔Comment) no mesmo
      índice. Qualquer uma dessas anomalias impede o resultado "limpo".
      
      <p>COMPORTAMENTO EM CASO DE FALHA: opera só em memória; documentos válidos e
      equivalentes não geram anomalia. Só é executada entre formatos comparáveis
      (o caso de uso bloqueia ASS↔SRT antes de chegar aqui).
  - RegraMetadadosAss.java
      (sem cabecalho explicativo)
  - RegraSincroniaEstilos.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/auditorConteudoLegendas/domain/
  - AnomaliaConteudo.java
      (sem cabecalho explicativo)
  - AuditoriaConteudoRelatorioJson.java
      PROPÓSITO DE NEGÓCIO: persiste a auditoria como dataset estruturado para
      diagnóstico, evolução das regras e reprodução de falhas.
      <p>INVARIANTES DO DOMÍNIO: nomes, formatos, métricas e anomalias pertencem à
      mesma execução.
      <p>COMPORTAMENTO EM CASO DE FALHA: o record é imutável; falhas de gravação
      são tratadas pela camada de persistência.
  - AuditoriaException.java
      (sem cabecalho explicativo)
  - ModoAuditoria.java
      PROPÓSITO DE NEGÓCIO: identifica qual escopo de análise de legenda o usuário
      escolheu nas abas do painel — auditar só o arquivo original (EN), só o
      traduzido (PT-BR) ou comparar os dois.
      
      <p>INVARIANTES DO DOMÍNIO: {@link #AMBAS} exige os dois arquivos e executa as
      regras comparativas; {@link #ORIGINAL} e {@link #TRADUZIDO} exigem apenas um
      arquivo e executam as regras de arquivo único.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: {@link #porNome(String)} devolve
      {@link #AMBAS} para valor ausente ou desconhecido, preservando o comportamento
      histórico do endpoint (compatível com chamadas que não enviam o campo).
      PROPÓSITO DE NEGÓCIO: converte o rótulo vindo da requisição em modo válido.
      <p>INVARIANTES DO DOMÍNIO: a comparação ignora caixa e espaços.
      <p>COMPORTAMENTO EM CASO DE FALHA: entrada nula, em branco ou não mapeada
      resulta em {@link #AMBAS} (default seguro e retrocompatível).
  - RegraAuditoriaArquivoUnico.java
      PROPÓSITO DE NEGÓCIO: contrato das regras que auditam UM único arquivo de
      legenda (só original ou só traduzido), sem depender de um par de comparação.
      Sustenta as abas "Só Original" e "Só Traduzida" do painel de Análise de
      Conteúdo, onde não existe artefato de referência.
      
      <p>INVARIANTES DO DOMÍNIO: implementações são de responsabilidade única e não
      alteram o documento recebido; a auditoria é 100% leitura. Estas regras vivem
      numa hierarquia separada da comparativa {@link RegraAuditoriaConteudo} para
      que os dois conjuntos sejam injetados e contados de forma independente.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: uma regra que não consiga avaliar um evento
      (ex.: timestamp ilegível) deve ignorá-lo silenciosamente e nunca lançar; a
      ausência de anomalias é representada por lista vazia.
  - RegraAuditoriaConteudo.java
      (sem cabecalho explicativo)
  - RelatorioAuditoriaConteudo.java
      PROPÓSITO DE NEGÓCIO: representa o resultado exibido e exportado pela
      Análise de Legenda, incluindo a identificação inequívoca dos artefatos
      comparados e de seus formatos.
      
      <p>INVARIANTES DO DOMÍNIO: arquivo e formato original sempre pertencem ao
      mesmo artefato; arquivo e formato traduzido seguem a mesma regra; anomalias
      são acumuladas sem alterar os metadados de entrada.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: esta classe não executa I/O; dados
      inválidos precisam ser rejeitados pelo caso de uso antes de sua criação.
  - TempoEventoUtil.java
      PROPÓSITO DE NEGÓCIO: interpreta e DIAGNOSTICA os instantes de início e fim de
      um evento de legenda, para que a auditoria distinga um timestamp válido de um
      corrompido em vez de simplesmente ignorá-lo.
      
      <p>INVARIANTES DO DOMÍNIO: o tempo é lido do campo {@code prefixo} preservado
      pelos leitores — ASS guarda {@code Dialogue: Layer,Início,Fim,...} e SRT guarda
      a linha {@code hh:mm:ss,mmm --> hh:mm:ss,mmm}. Valores são milissegundos desde
      0; minutos e segundos válidos ficam em 0–59.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; um prefixo ilegível, incompleto
      ou fora do intervalo é reportado com o {@link StatusTempo} correspondente.

[PASTA] src/main/java/org/traducao/projeto/auditorConteudoLegendas/infrastructure/
  - AuditoriaConteudoPersistencia.java
      PROPÓSITO DE NEGÓCIO: grava cada relatório de auditoria como um arquivo JSON
      imutável e único, para que execuções não sobrescrevam umas às outras.
      
      <p>INVARIANTES DO DOMÍNIO: o nome combina timestamp em milissegundos com um
      contador atômico; a gravação usa {@code CREATE_NEW} para nunca substituir um
      relatório existente; a pasta de destino é decidida pelo chamador.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: colisão de nome tenta o próximo contador;
      esgotadas as tentativas, lança {@link IOException} sem sobrescrever nada.

[PASTA] src/main/java/org/traducao/projeto/auditorConteudoLegendas/presentation/
  - AuditorConteudoController.java
      PROPÓSITO DE NEGÓCIO: expõe a Análise de Conteúdo nos três escopos das abas
      do painel (só original, só traduzido, ambos) sobre o mesmo endpoint.
      <p>INVARIANTES DO DOMÍNIO: o modo determina quais caminhos são obrigatórios;
      modo ausente equivale a AMBAS (retrocompatível).
      <p>COMPORTAMENTO EM CASO DE FALHA: caminho exigido em branco → 400 didático;
      {@link AuditoriaException} → 400 com a mensagem de domínio; erro inesperado
      → 500.

[PASTA] src/main/java/org/traducao/projeto/cachetraducao/domain/
  - CacheDocumento.java
      PROPÓSITO DE NEGÓCIO: Formato persistido do cache de tradução versionado — um
      cabeçalho de {@link ProvenienciaCache} seguido das entradas. Substitui a lista
      pura de {@link EntradaCache} para que cada arquivo de cache carregue a origem
      (lore/modelo) que o gerou.
      
      <p>INVARIANTES DO DOMÍNIO: {@code proveniencia} descreve TODAS as entradas do
      documento (um arquivo de cache = uma geração/proveniência). Entradas de
      proveniências diferentes nunca convivem no mesmo documento.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: record imutável; a leitura de um JSON que
      não seja este objeto (ex.: lista pura do formato antigo) falha na
      desserialização e é tratada pelo {@code CacheTraducaoService} como formato
      legado ou corrompido.
      
      @param proveniencia origem (lore/hash/modelo/idiomas) que gerou TODAS as entradas
      @param entradas linhas do cache de tradução deste documento
  - EntradaCache.java
      PROPÓSITO DE NEGÓCIO: modelo persistido de uma linha do cache de tradução — o
      texto original e sua tradução, com o índice, o estilo e o par de idiomas a que
      pertencem. É a unidade que o cache de tradução grava e relê para evitar retraduzir
      a mesma fala. NÃO é um modelo genérico de legenda.
      
      <p>INVARIANTES DO DOMÍNIO: {@code record} imutável; a ordem e os nomes dos
      componentes ({@code indice, estilo, original, traduzido, idiomaOriginal,
      idiomaTraduzido}) compõem o schema JSON persistido e não podem mudar sem quebrar a
      compatibilidade dos arquivos {@code .cache.json} existentes.
      
      <p>COMPORTAMENTO EM CASO DE LIMITE: não há validação — qualquer valor, inclusive
      {@code null} nos campos de texto, é aceito; a coerência é responsabilidade de quem
      grava/lê o cache.
      
      @param indice posição ordinal da fala na legenda
      @param estilo nome do estilo ASS da fala
      @param original texto original (idioma de origem)
      @param traduzido texto traduzido (idioma de destino)
      @param idiomaOriginal código do idioma de origem
      @param idiomaTraduzido código do idioma de destino
  - ProvenienciaCache.java
      PROPÓSITO DE NEGÓCIO: Carimba cada cache de tradução com a origem que o
      produziu — qual lore/contexto, qual hash do prompt de sistema, qual modelo e
      qual par de idiomas. É o que permite provar com o que uma tradução em cache
      foi feita e impedir que uma melhoria de lore reuse silenciosamente traduções
      antigas.
      
      <p>INVARIANTES DO DOMÍNIO: duas proveniências só são "a mesma" se os SEIS campos
      baterem por igualdade exata — schemaVersion, contextoId, contextoHash, modeloLlm,
      idiomaOrigem e idiomaDestino. O hash é derivado do prompt de sistema ativo
      (SHA-256), então qualquer mudança de lore/regra muda o hash. A versão de schema
      NÃO é normalizada: quando comparada à proveniência atual do pipeline, carimbada
      com {@code SCHEMA_ATUAL}, uma versão ausente/{@code 0} ou divergente reprova a
      compatibilidade e nunca é reutilizada.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: {@link #hashDe(String)} nunca lança — se o
      algoritmo SHA-256 faltar (não deve, é padrão da JVM), cai para o hashCode em
      hexadecimal como último recurso. {@link #mesmaProveniencia} trata nulo como
      "diferente"; no fluxo automático, versão ausente/{@code 0} materializada no cache
      diverge de {@code SCHEMA_ATUAL} e leva ao arquivamento da geração anterior.
      
      @param schemaVersion versão do schema do documento de cache persistido
      @param contextoId identificador do lore/contexto usado na geração
      @param contextoHash hash SHA-256 do prompt de sistema ativo
      @param modeloLlm identificador do modelo LLM que gerou as traduções
      @param idiomaOrigem código do idioma de origem
      @param idiomaDestino código do idioma de destino

[PASTA] src/main/java/org/traducao/projeto/cachetraducao/infrastructure/
  - CacheManutencaoService.java
      PROPÓSITO DE NEGÓCIO: fornece uma porta única e segura para os módulos que
      corrigem os arquivos persistentes da pasta {@code cache}, aceitando tanto a
      lista JSON histórica quanto o documento versionado com proveniência.
      
      <p>INVARIANTES DO DOMÍNIO: campos desconhecidos e o formato original são
      preservados; um cache só é substituído depois de backup, serialização em
      temporário e validação estrutural; a proveniência nunca é removida.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link IOException} sem substituir o
      arquivo original. O temporário é removido e o backup permanece disponível em
      {@code backups/correcao-cache/}.
  - CacheTraducaoService.java
      PROPÓSITO DE NEGÓCIO: persiste, por arquivo de legenda, o par (texto original →
      texto traduzido) em JSON, no formato versionado {@link CacheDocumento}. Serve para
      (1) permitir revisão/correção manual do cache editando o JSON e (2) evitar chamar o
      LLM de novo para falas já traduzidas numa execução anterior.
      
      <p>INVARIANTES DO DOMÍNIO: cada arquivo carrega a {@link ProvenienciaCache}
      (lore/hash/modelo/idiomas) que o gerou; uma proveniência divergente NÃO é reutilizada
      — a geração anterior é arquivada e o episódio é retraduzido com o lore atual. A
      escrita é atômica (temporário + {@code ArquivoAtomicoUtil}); a leitura aceita tanto o
      documento versionado quanto a lista JSON histórica.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: um JSON ilegível é preservado (renomeado
      {@code .corrompido_<ts>.json}) em vez de ignorado/sobrescrito; falha de gravação
      propaga {@link ArquivoLegendaException} sem deixar o destino truncado.

[PASTA] src/main/java/org/traducao/projeto/config/
  - AppConfig.java
      (sem cabecalho explicativo)
  - ExecucaoCli.java
      Contrato para modos de execucao em linha de comando (substituto do CommandLineRunner do Spring Boot).
  - ModoExecucaoStartup.java
      Dispara o modo CLI configurado em {@code app.modo}. No modo WEB nenhuma CLI e
      executada. O modo {@code TRADUZIR} possui ciclo de vida proprio na fatia Traducao
      Local ({@code traducao.presentation.bootstrap.TraducaoStartup}) e nao e roteado
      aqui — por isso este dispatcher nao conhece nenhuma classe de {@code traducao}.

[PASTA] src/main/java/org/traducao/projeto/contexto/domain/
  - ContextoNaoEncontradoException.java
      PROPÓSITO DE NEGÓCIO: sinaliza que um id de contexto/lore selecionado na UI não
      corresponde a nenhum provedor registrado. Impede que um anime seja traduzido com
      a lore errada silenciosamente — cair no contexto padrão sem aviso esconderia o
      erro de seleção do operador.
      
      <p>INVARIANTES DO DOMÍNIO: pertence ao módulo compartilhado {@code contexto} e
      estende {@link ExcecaoContexto} (deixou de ser {@code TradutorException} na E7a),
      portanto continua sendo {@code BasePipelineException}; mensagem preservada
      (lista os contextos disponíveis). Só é lançada por quem resolve o contexto ativo
      a partir de um id explícito não vazio.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: propaga como {@code RuntimeException} não
      verificada; é convertida em resposta HTTP estruturada pelo
      {@code BasePipelineExceptionMapper} (comum a toda a família) e pode ser capturada
      por qualquer bloco que trate {@link ExcecaoContexto} ou {@code BasePipelineException}.
  - ContextoPrompt.java
      PROPÓSITO DE NEGÓCIO: monta o prompt de sistema completo de tradução a partir da
      lore de cada obra (juntando prioridades, {@link RegrasConcordanciaPtBr} e regras de
      saída) e mantém, por trás de cada prompt, a lore "crua" correspondente — para que
      usos pontuais (ex.: revisão de concordância) recuperem só a lore sem reenviar o
      prompt inteiro ao LLM.
      
      <p>INVARIANTES DO DOMÍNIO: utilitário de domínio autocontido — sem I/O, sem
      configuração e sem dependência funcional externa —, porém com estado estático
      interno ({@code LORE_POR_PROMPT}, um cache prompt→lore). A ordem/estrutura do
      template e o mapeamento prompt→lore fazem parte do contrato e não podem mudar sem
      quebrar a recuperação da lore. Classe final, construtor privado.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: {@link #obterLore(String)} devolve o próprio
      argumento quando o prompt não foi registrado (nunca lança); {@link #montar(String,
      String)} propaga {@code NullPointerException} se {@code lore} for nulo (via
      {@code strip()}).
  - ExcecaoContexto.java
      PROPÓSITO DE NEGÓCIO: raiz da hierarquia de exceções pertencentes ao módulo
      compartilhado {@code contexto} — falhas ligadas à seleção e ao uso de um
      contexto/lore de tradução. NÃO representa falhas gerais de tradução nem do LLM
      (essas vivem sob {@code TradutorException}, na fatia {@code traducao}) nem falhas
      de legenda (sob {@code ExcecaoLegenda}); é a base específica das falhas do
      domínio de contexto, consumível por qualquer fatia.
      
      <p>INVARIANTES DO DOMÍNIO: estende {@code BasePipelineException} (core), herdando
      {@code errorId} e {@code timestamp}; é concreta e oferece apenas os dois construtores
      canônicos (mensagem; mensagem+causa). Não declara estado próprio, código de
      infraestrutura nem status HTTP — o mapeamento HTTP é responsabilidade única do
      {@code BasePipelineExceptionMapper}, comum a toda a família.
      
      <p>COMPORTAMENTO EM CASO DE PROPAGAÇÃO: propaga como {@code RuntimeException} não
      verificada; por ser {@code BasePipelineException}, é convertida em resposta HTTP
      estruturada pelo mapper e pode ser capturada por qualquer bloco que trate
      {@code ExcecaoContexto} ou uma de suas subclasses.
  - ProvedorContexto.java
      PROPÓSITO DE NEGÓCIO: contrato de um provedor de contexto/lore de tradução — cada
      obra (Gundam, Macross, Danmachi...) implementa esta interface para fornecer o
      prompt de sistema do LLM, o rótulo de UI e os termos que não devem ser traduzidos.
      É o ponto de extensão do módulo compartilhado {@code contexto}: novas obras entram
      apenas adicionando implementações {@code @Component}, sem tocar em quem consome.
      
      <p>INVARIANTES DO DOMÍNIO: interface pura (só depende do JDK); {@link #getId()} é o
      identificador único e estável usado para seleção e para carimbar a proveniência do
      cache; {@link #obterPromptSistema()} devolve o prompt completo já montado; termos
      protegidos são um conjunto imutável (por padrão vazio). Nenhum método realiza I/O.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: os métodos não lançam por contrato; um provedor
      mal formado (id nulo/duplicado) é rejeitado por quem agrega os provedores
      ({@code GerenciadorContexto}), não por esta interface.
  - RegrasConcordanciaPtBr.java
      PROPÓSITO DE NEGÓCIO: bloco fixo de regras de concordância de gênero, pronomes,
      tratamentos e verbos, aplicável a qualquer obra — o inglês não marca gênero em
      adjetivos/particípios e usa "you" genérico, o que leva o LLM a masculinizar tudo.
      É injetado no prompt de tradução ({@link ContextoPrompt#montar}) e reaproveitado
      no prompt de revisão de concordância.
      
      <p>INVARIANTES DO DOMÍNIO: constantes/textos imutáveis; {@code BLOCO_TRADUCAO} e o
      template de {@link #montarPromptRevisao(String)} são conteúdo de negócio congelado
      — espaçamento, quebras de linha e pontuação fazem parte do contrato do prompt e não
      podem ser reformatados. Classe final, construtor privado, sem estado mutável.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: {@link #montarPromptRevisao(String)} trata
      {@code null}/branco como "(sem lore adicional)" e nunca lança.

[PASTA] src/main/java/org/traducao/projeto/contexto/infrastructure/config/
  - ContextoBeansConfig.java
      PROPÓSITO DE NEGÓCIO: reúne, para o módulo compartilhado {@code contexto}, todas as
      implementações CDI de {@link ProvedorContexto} numa única lista consumível pelo
      {@code GerenciadorContexto}. Extraído de {@code traducao.infrastructure.config.RestClientConfig}
      na E7b para que a agregação dos provedores pertença ao próprio peer, e não à fatia
      de tradução (mesmo padrão de {@code legendasExtracao.infrastructure.config.ExtracaoBeansConfig}).
      
      <p>INVARIANTES DO DOMÍNIO: agrega TODAS as implementações descobertas via
      {@link Instance}, na ordem de iteração fornecida pelo container, sem impor ordenação
      própria (a ordenação por nome de exibição é responsabilidade do {@code GerenciadorContexto}).
      Não conhece classes de nenhuma fatia funcional.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: quando não há nenhum provedor registrado,
      {@link #todosProvedoresContexto(Instance)} devolve uma lista vazia (nunca nula).

[PASTA] src/main/java/org/traducao/projeto/contexto/infrastructure/
  - GerenciadorContexto.java
      PROPÓSITO DE NEGÓCIO: agrega todos os provedores de contexto/lore descobertos por
      CDI e mantém qual está ATIVO, servindo o prompt de sistema, a lore crua, o id de
      proveniência e os termos protegidos para a tradução em curso. É o ponto único pelo
      qual as fatias funcionais (tradução, correção, revisão, karaokê) selecionam e
      consultam a obra ativa — agora residente no módulo compartilhado {@code contexto}
      (peer), consumível por qualquer fatia sem acoplamento reverso.
      
      <p>INVARIANTES DO DOMÍNIO: os provedores são ordenados por nome de exibição
      (case-insensitive) e seus ids são únicos (falha na construção se houver duplicata);
      o contexto padrão é {@code danmachi} (ou o primeiro, se ausente); {@code provedorAtivo}
      nunca cai silenciosamente no padrão quando um id explícito não existe. O campo
      {@code provedorAtivo} é {@code volatile} para visibilidade entre a thread do executor
      de background e a leitura ao montar o prompt — não é uma alegação de isolamento por job.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: {@link #definirContextoAtivo(String)} lança
      {@link ContextoNaoEncontradoException} para um id não vazio desconhecido (impede
      traduzir com a lore errada silenciosamente); ids nulos/vazios mantêm o ativo atual;
      ids duplicados no registro lançam {@link IllegalStateException} na construção;
      {@link #obterPromptAtivo()} devolve um prompt genérico quando não há ativo.

[PASTA] src/main/java/org/traducao/projeto/contexto/lore/danmachi/
  - ContextoDanMachi.java
      PROPÓSITO DE NEGÓCIO: lore geral de DanMachi (contexto padrão) cobrindo termos
      de mundo, nomes principais e regras de tradução para qualquer arco.
      
      <p>INVARIANTES DO DOMÍNIO: Liliruca Arde; Syr Flova; Mikoto Yamato; Familia/Falna/Dungeon.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoDanMachiOrion.java
      PROPÓSITO DE NEGÓCIO: lore do filme Arrow of the Orion — correção de grafia
      Liliruca e elenco do filme.
      
      <p>INVARIANTES DO DOMÍNIO: Liliruca Arde (nunca Liriruca/Lilisuka); Artemis;
      nomes oficiais do filme.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoDanMachiS1.java
      PROPÓSITO DE NEGÓCIO: lore da 1ª temporada de DanMachi (arco inicial em Orario /
      Minotaur / formação da Hestia Familia) para o LLM e o detector de tradução idêntica.
      
      <p>INVARIANTES DO DOMÍNIO: nomes oficiais EN/JP-romanizados; Liliruca Arde (não
      "Lilisuka"); ordem ocidental Mikoto Yamato; termos Familia/Falna/Dungeon protegidos.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoDanMachiS2.java
      PROPÓSITO DE NEGÓCIO: lore da 2ª temporada de DanMachi (arco Ishtar / War Game /
      Haruhime) para tradução fiel de nomes e termos.
      
      <p>INVARIANTES DO DOMÍNIO: Liliruca Arde; Haruhime Sanjouno; Ishtar Familia;
      Pleasure Quarter; não traduzir Bell como "sino".
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoDanMachiS3.java
      PROPÓSITO DE NEGÓCIO: lore da 3ª temporada de DanMachi (arco Xenos) para
      preservar nomes de monstros inteligentes e facções.
      
      <p>INVARIANTES DO DOMÍNIO: Xenos, Wiene, Fels, Dix Perdix, Asterius; Liliruca Arde.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoDanMachiS4.java
      PROPÓSITO DE NEGÓCIO: lore da 4ª temporada de DanMachi (Deep Floors / Labyrinth).
      
      <p>INVARIANTES DO DOMÍNIO: Rivira, Juggernaut, Ryuu Lion; Liliruca Arde;
      Mikoto Yamato (ordem ocidental).
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoDanMachiS5.java
      PROPÓSITO DE NEGÓCIO: lore da 5ª temporada de DanMachi (arco Freya / Goddess of
      Fertility) com grafia canônica Syr Flova.
      
      <p>INVARIANTES DO DOMÍNIO: Syr Flova (nunca "Flover"); Folkvangr; Charm divino;
      nomes da Freya Familia.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoDanMachiSwordOratoria.java
      PROPÓSITO DE NEGÓCIO: lore de Sword Oratoria (spin-off Loki Familia / Ais).
      
      <p>INVARIANTES DO DOMÍNIO: nomes oficiais da Loki Familia; Ais = Sword Princess;
      Lefiya, Finn, Riveria, Gareth, Tiona, Tione, Bete.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.

[PASTA] src/main/java/org/traducao/projeto/contexto/lore/eightsix/
  - Contexto86.java
      PROPÓSITO DE NEGÓCIO: lore de 86 — Eighty-Six (segregação estatal, guerra psicológica).
      
      <p>INVARIANTES DO DOMÍNIO: Shin ≠ canela; Alba/Colorata/Pig; Handler/Processor;
      Para-RAID; Legion e unidades em latim/alemão.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.

[PASTA] src/main/java/org/traducao/projeto/contexto/lore/evangelion/
  - ContextoEvangelion111.java
      PROPÓSITO DE NEGÓCIO: lore de Evangelion 1.11 (Rebuild 1.0).
      
      <p>INVARIANTES DO DOMÍNIO: continuidade Rebuild; Sachiel/Shamshel/Ramiel; NERV.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoEvangelion222.java
      PROPÓSITO DE NEGÓCIO: lore de Evangelion 2.22 (Rebuild 2.0) com Asuka Shikinami.
      
      <p>INVARIANTES DO DOMÍNIO: Asuka Shikinami Langley; Mari Illustrious Makinami;
      Near Third Impact; Unit-03 / Bardiel.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoEvangelion3010.java
      PROPÓSITO DE NEGÓCIO: lore de Evangelion 3.0+1.0 — Village-3 / Additional Impact.
      
      <p>INVARIANTES DO DOMÍNIO: Asuka Shikinami Langley; Mari Illustrious Makinami;
      Village-3; Golgotha Object; não usar "Asuka Langley" incompleto.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoEvangelion333.java
      PROPÓSITO DE NEGÓCIO: lore de Evangelion 3.33 (Rebuild 3.0) — WILLE / Wunder.
      
      <p>INVARIANTES DO DOMÍNIO: Asuka Shikinami Langley; Mari Illustrious Makinami;
      WILLE; AAA Wunder; Unit-13.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoEvangelionTV.java
      PROPÓSITO DE NEGÓCIO: lore da série TV clássica Neon Genesis Evangelion.
      
      <p>INVARIANTES DO DOMÍNIO: Asuka Langley Soryu (TV, não Shikinami); NERV; SEELE;
      Angels; Human Instrumentality Project.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.

[PASTA] src/main/java/org/traducao/projeto/contexto/lore/guiltycrown/
  - ContextoGuiltyCrown.java
      PROPÓSITO DE NEGÓCIO: lore de Guilty Crown (biologia distópica + Voids).
      
      <p>INVARIANTES DO DOMÍNIO: Funeral Parlor ≠ Undertaker; Void Genome; Lost Christmas;
      Apocalypse Virus; Endlave; Crystallization.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.

[PASTA] src/main/java/org/traducao/projeto/contexto/lore/gundam/chars/
  - ContextoCharsCounterattack.java
      PROPÓSITO DE NEGÓCIO: lore de Char's Counterattack (UC 0093 / Axis Shock).
      
      <p>INVARIANTES DO DOMÍNIO: Amuro Ray; Char Aznable; Nu Gundam; Sazabi;
      Londo Bell; Axis; psycho-frame.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.

[PASTA] src/main/java/org/traducao/projeto/contexto/lore/gundam/
  - ContextoGundam0079.java
      PROPÓSITO DE NEGÓCIO: lore UC 0079 com núcleo conceitual militar (Minovsky, MS/MA, Newtype).
      
      <p>INVARIANTES DO DOMÍNIO: Newtype ≠ "Novo Tipo"; Mobile Suit ≠ Mobile Armor;
      Spacenoid/Earthnoid; Minovsky Particles; Principality of Zeon vs Earth Federation.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoGundamF91.java
      (sem cabecalho explicativo)
  - ContextoGundamHathaway.java
      (sem cabecalho explicativo)
  - ContextoGundamNT.java
      PROPÓSITO DE NEGÓCIO: lore de Gundam NT (Narrative) calibrada no artefato real
      (legendas EN/PT do BD em C:\\TRACKER-ANIMES\\animes\\Gundam Narrative NT).
      
      <p>INVARIANTES DO DOMÍNIO: Newtype/Cyber-Newtype/Oldtype; Spacenoid; Mobile Suit
      vs Mobile Armor; Phenex; Shezarr; Metis/Banchi 18/Fransson; Minovsky vs psycho-waves.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoGundamOrigin.java
      (sem cabecalho explicativo)
  - ContextoGundamSEED.java
      PROPÓSITO DE NEGÓCIO: lore de Gundam SEED (série CE 71) — sem misturar Destiny.
      
      <p>INVARIANTES DO DOMÍNIO: elenco SEED apenas; Shinn Asuka pertence a Destiny;
      Coordinator/Natural; Freedom/Justice/Strike.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoGundamSEEDAstray.java
      (sem cabecalho explicativo)
  - ContextoGundamSEEDDestiny.java
      PROPÓSITO DE NEGÓCIO: lore de Gundam SEED Destiny — elenco e mecha da sequela.
      
      <p>INVARIANTES DO DOMÍNIO: Shinn Asuka; Minerva; Destiny/Impulse/Strike Freedom;
      Gilbert Durandal; LOGOS.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoGundamSEEDFreedom.java
      PROPÓSITO DE NEGÓCIO: lore do filme Gundam SEED FREEDOM.
      
      <p>INVARIANTES DO DOMÍNIO: Rising Freedom; Immortal Justice; Mighty Strike Freedom;
      Kingdom of Foundation; Orphee Lam Tao; Agnes Giebenrath.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoGundamSEEDStargazer.java
      (sem cabecalho explicativo)
  - ContextoGundamUnicorn.java
      PROPÓSITO DE NEGÓCIO: lore de Gundam Unicorn (UC 0096) com núcleo UC completo.
      
      <p>INVARIANTES DO DOMÍNIO: Laplace's Box; Unicorn/Banshee; Newtype/Spacenoid;
      Mobile Suit vs Armor; Psycho-Frame; Sleeves.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoGundamVictory.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/contexto/lore/gundam/msteam/
  - ContextoGundam08thMSTeam.java
      PROPÓSITO DE NEGÓCIO: lore de The 08th MS Team (OVA UC 0079 — guerra terrestre).
      
      <p>INVARIANTES DO DOMÍNIO: Shiro Amada; Aina Sahalin; Ez-8; Apsalus; Eledore;
      realismo anti-guerra.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.

[PASTA] src/main/java/org/traducao/projeto/contexto/lore/gundam/reconguista/
  - ContextoGundamReconguista.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/contexto/lore/gundam/stardust/
  - ContextoGundam0083.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/contexto/lore/gundam/warInpocket/
  - ContextoWarInPocket.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/contexto/lore/gundam/zeta/
  - ContextoGundamZeta.java
      PROPÓSITO DE NEGÓCIO: lore de Zeta Gundam (Gryps Conflict / UC 0087).
      
      <p>INVARIANTES DO DOMÍNIO: Kamille Bidan (masculino); AEUG; Titans; Quattro Bajeena.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.

[PASTA] src/main/java/org/traducao/projeto/contexto/lore/gundam/zz/
  - ContextoGundamZZ.java
      PROPÓSITO DE NEGÓCIO: lore de Mobile Suit Gundam ZZ (UC 0088 / Neo Zeon).
      
      <p>INVARIANTES DO DOMÍNIO: Judau Ashta; Haman Karn; Glemy Toto; ZZ Gundam;
      Axis/Neo Zeon; Shangri-La.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.

[PASTA] src/main/java/org/traducao/projeto/contexto/lore/macross/
  - ContextoMacross2.java
      PROPÓSITO DE NEGÓCIO: lore de Macross II: Lovers Again para tradução fiel de
      nomes, Marduk/Emulator e mecha da continuidade alternativa.
      
      <p>INVARIANTES DO DOMÍNIO: Hibiki Kanzaki; Ishtar; Silvie Gena; Marduk;
      Emulator; Minmay Attack; VF-2SS.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoMacross7.java
      PROPÓSITO DE NEGÓCIO: lore enriquecida de Macross 7 (série TV) / Fire Bomber.
      
      <p>INVARIANTES DO DOMÍNIO: Basara Nekki; Fire Bomber; Protodeviln; Sound Force;
      VF-19 Custom Fire Valkyrie.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoMacross7Encore.java
      (sem cabecalho explicativo)
  - ContextoMacross7Filme.java
      (sem cabecalho explicativo)
  - ContextoMacross7Filmes.java
      (sem cabecalho explicativo)
  - ContextoMacrossAnime.java
      PROPÓSITO DE NEGÓCIO: lore clássica de Macross (cânone JP — tríade música/mecha/geopolítica).
      
      <p>INVARIANTES DO DOMÍNIO: Valkyrie/VF; Fighter/GERWALK/Battroid; Overtechnology;
      Deculture; Zentradi; proibir léxico Robotech (Veritech).
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoMacrossDelta.java
      PROPÓSITO DE NEGÓCIO: lore de Macross Delta (série TV) com termos protegidos.
      
      <p>INVARIANTES DO DOMÍNIO: Walküre; Windermere; Var Syndrome; elenco oficial.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoMacrossDeltaFilme1.java
      PROPÓSITO DE NEGÓCIO: lore do filme Macross Delta — Passionate Walküre
      (Gekijou no Walküre), alinhada à fila ativa de tradução.
      
      <p>INVARIANTES DO DOMÍNIO: Walküre; Windermere; Var Syndrome; mesmos nomes
      canônicos da série Delta.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoMacrossDeltaFilme2.java
      PROPÓSITO DE NEGÓCIO: lore do filme Macross Delta 2 — Absolute Live!!!!!!
      
      <p>INVARIANTES DO DOMÍNIO: Heimdall; Yami_Q_Ray; VF-31AX Kairos-Plus; Max Jenius;
      continuidade pós-série (não confundir com Passionate Walküre).
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoMacrossDeltaFilmes.java
      (sem cabecalho explicativo)
  - ContextoMacrossDynamite7.java
      (sem cabecalho explicativo)
  - ContextoMacrossDYRL.java
      (sem cabecalho explicativo)
  - ContextoMacrossFilme1.java
      (sem cabecalho explicativo)
  - ContextoMacrossFilme2.java
      (sem cabecalho explicativo)
  - ContextoMacrossFrontier.java
      PROPÓSITO DE NEGÓCIO: lore enriquecida de Macross Frontier (série TV).
      
      <p>INVARIANTES DO DOMÍNIO: Klan Klang; SMS; Vajra; Sheryl Nome; Ranka Lee;
      Macross Frontier fleet.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoMacrossFrontierFilme1.java
      (sem cabecalho explicativo)
  - ContextoMacrossFrontierFilme2.java
      (sem cabecalho explicativo)
  - ContextoMacrossFrontierFilmes.java
      (sem cabecalho explicativo)
  - ContextoMacrossPlus.java
      (sem cabecalho explicativo)
  - ContextoMacrossZero.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/contexto/lore/sidonia/
  - ContextoKnightsOfSidonia.java
      PROPÓSITO DE NEGÓCIO: lore da série Knights of Sidonia.
      
      <p>INVARIANTES DO DOMÍNIO: Izana Shinatose; Gauna; Garde; Ena; Heigus.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.
  - ContextoSidoniaFilme.java
      PROPÓSITO DE NEGÓCIO: lore do filme Sidonia — corrige Izana Shinatose.
      
      <p>INVARIANTES DO DOMÍNIO: Izana Shinatose (nunca Shinoshinari); Nagate; Tsumugi;
      Gauna/Garde.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos protegidos imutáveis.

[PASTA] src/main/java/org/traducao/projeto/core/exception/
  - BasePipelineException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/core/exception/web/
  - BasePipelineExceptionMapper.java
      Converte qualquer exceção de domínio do pipeline (uma por pacote, todas
      estendendo {@link BasePipelineException}) em uma resposta JSON estruturada
      e rastreável, em vez de cada endpoint precisar capturar e formatar erro
      manualmente. O {@code errorId} permite cruzar a resposta HTTP com a
      entrada correspondente no log do servidor.

[PASTA] src/main/java/org/traducao/projeto/core/execucao/
  - FilaExecucaoPipeline.java
      Fila única (single-thread) para todos os jobs pesados do pipeline —
      tradução, correção, revisões (concordância/lore), análise, extração, remux.
      <p>
      Ter UMA fila compartilhada é requisito de corretude, não só de desempenho:
      o contexto de tradução ativo ({@code GerenciadorContexto}) e o modelo LLM
      configurado são estado global mutado no início de cada job. Quando cada
      controller tinha seu próprio executor (ou rodava na thread HTTP), dois jobs
      podiam rodar em paralelo e um trocava a lore/modelo no meio do outro — além
      de disputarem a GPU do LM Studio, que atende uma inferência por vez.

[PASTA] src/main/java/org/traducao/projeto/core/infrastructure/http/
  - JsonHttpClient.java
      PROPÓSITO DE NEGÓCIO: cliente HTTP JSON genérico baseado no {@link HttpClient} do JDK
      (sem Spring RestClient), técnico e neutro — reutilizável por qualquer fatia. Recebe os
      timeouts como {@link Duration} explícitos e a base URL por parâmetro; não conhece LLM,
      anime, autenticação nem fatia funcional (kernel {@code core.infrastructure.http}).
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Connect timeout no builder do {@code HttpClient}; read timeout por requisição.</li>
      <li>{@code baseUrl} normalizada (sem barra final) e usada em {@code get/getString/post};
      {@code getAbsolute} usa a URL completa recebida.</li>
      <li>Nenhuma dependência de fatia funcional; só JDK + Jackson (técnico).</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Status HTTP &ge; 400 lança {@link HttpClientException} com o código e o corpo.
      {@link #isErroRedeOuTimeout(Throwable)} classifica timeout/conexão/host desconhecido.

[PASTA] src/main/java/org/traducao/projeto/core/io/
  - DiretorioBaseKronos.java
      PROPÓSITO DE NEGÓCIO: ponto único de resolução da raiz onde o KRONOS grava
      seus artefatos operacionais (telemetria em {@code logs/}, relatórios em
      {@code relatorios/}, cache em {@code cache/}, backups em {@code backups/}).
      Em produção a raiz é o próprio diretório de trabalho do processo, preservando
      exatamente o comportamento local histórico do projeto. Durante a suíte de
      testes a raiz é redirecionada (via system property {@code kronos.dir.base},
      configurada no {@code build.gradle}) para uma árvore descartável em
      {@code build/tmp/kronos-tests}, impedindo que os testes contaminem os
      diretórios operacionais reais versionados pelo Git.
      
      <p>INVARIANTES DO DOMÍNIO:
      <ul>
      <li>Quando {@code kronos.dir.base} está ausente ou em branco, a raiz é o
      diretório de trabalho corrente ({@code Path.of("")}), de modo que
      {@code resolver("cache")} é idêntico a {@code Path.of("cache")} — o
      comportamento de produção não muda.</li>
      <li>A raiz é lida da system property a cada chamada, não capturada em campo
      estático, para que o valor definido no lançamento da JVM de teste valha
      inclusive para constantes resolvidas em tempo de carga de classe.</li>
      </ul>
      
      <p>COMPORTAMENTO EM CASO DE FALHA: não lança exceção própria. Se a property
      contiver um caminho sintaticamente inválido, a exceção de {@link Path#of}
      propaga ao chamador; com property ausente/branca cai no diretório corrente.

[PASTA] src/main/java/org/traducao/projeto/core/presentation/ui/
  - AnsiCores.java
      Cores ANSI compartilhadas entre o prompt interativo e os loggers de console do
      projeto. Usar apenas caracteres ASCII nos textos do prompt evita problemas de
      encoding no console do Windows (cp1252 vs UTF-8).
  - ConsoleEntrada.java
      PROPÓSITO DE NEGÓCIO: prompt interativo de console do pipeline KRONOS. Coleta,
      pelo terminal, o modo de operação e as pastas de entrada/saída e imprime a dica
      de recuperação quando o console falha. É um utilitário técnico de apresentação
      neutro (I/O de console), sem qualquer conceito de tradução/legenda/LLM — por isso
      reside no {@code core.presentation.ui} compartilhado, ao lado do {@code AnsiCores}.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>NUNCA fecha {@code System.in}: o leitor estático é envolvido uma única vez e
      mantido aberto durante todo o ciclo de vida do processo.</li>
      <li>Charset fixo UTF-8 no {@code InputStreamReader}; os textos do prompt usam
      apenas ASCII para blindar o console do Windows (cp1252 vs UTF-8).</li>
      <li>Zero dependência de fatia funcional: só depende do JDK e do {@code AnsiCores}
      do próprio core.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      EOF / {@code stdin} fechado, ou caminho obrigatório vazio, resultam em
      {@link Optional#empty()}. Uma {@link IOException} de leitura é capturada, sinalizada
      em vermelho e também devolve {@link Optional#empty()}. Opção de modo inválida recai
      no fluxo padrão WEB.

[PASTA] src/main/java/org/traducao/projeto/core/presentation/web/
  - LogStreamService.java
      Gerencia conexoes SSE (JAX-RS) e despacha logs em tempo real para clientes web.
  - OperacaoRequest.java
      PROPÓSITO DE NEGÓCIO: transporta os parâmetros comuns das operações do
      pipeline (análise, tradução, correção e revisão) enviados pela SPA — pastas de
      entrada/saída, contexto de lore selecionado e opções de sincronismo/revisão.
      
      <p>INVARIANTES DO DOMÍNIO: os nomes dos campos são contrato JSON público
      consumido pelo front-end; caminhos são normalizados e o contexto é validado
      pelos endpoints antes de qualquer job entrar na fila compartilhada.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: campos ausentes chegam como {@code null} e
      cada endpoint decide o fallback seguro ou responde HTTP 400 antes de enfileirar.
  - PipelineWebSupport.java
      PROPÓSITO DE NEGÓCIO: concentra os utilitários compartilhados pelos
      controllers web do pipeline — a normalização de caminhos digitados/colados na
      interface e o enfileiramento padronizado de jobs pesados na fila única de
      execução. Existe para que todos os endpoints entrem na MESMA fila e imprimam o
      MESMO formato de relatório final, sem duplicar essa lógica em cada controller.
      
      <p>INVARIANTES DO DOMÍNIO: expõe a única {@link FilaExecucaoPipeline}
      compartilhada (bean CDI) — jamais deve existir mais de uma instância de fila;
      todo job pesado passa por {@link #submeterJobComRelatorio}, garantindo canal
      SSE definido antes da execução e execução sequencial em segundo plano.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: {@link #normalizarCaminho(String)} devolve
      {@code null} para entrada nula/vazia ou sintaxe de caminho inválida
      ({@link InvalidPathException}), registrando aviso no log; o corpo submetido em
      {@link #submeterJobComRelatorio} sempre imprime a linha de relatório final,
      mesmo quando lança exceção, via bloco {@code finally}.
  - RespostaPadrao.java
      PROPÓSITO DE NEGÓCIO: envelope de resposta textual padrão da API web, usado
      por praticamente todos os endpoints do pipeline para devolver ao navegador uma
      mensagem legível (aceitação na fila, validação recusada ou heartbeat).
      
      <p>INVARIANTES DO DOMÍNIO: o nome do campo {@code mensagem} é contrato JSON
      público consumido pela SPA; não pode ser renomeado sem quebrar o front-end.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sendo um record imutável, não há falha de
      construção; {@code mensagem} pode ser vazia, mas nunca deve carregar dados
      sensíveis, pois é ecoada diretamente na interface.

[PASTA] src/main/java/org/traducao/projeto/core/util/
  - ArquivoAtomicoUtil.java
      Substituição atômica de arquivo (temporário -&gt; destino) tolerante ao Windows.
      
      <p>No Windows, o move atômico ({@code MoveFileEx}) falha com
      {@link AccessDeniedException} quando o arquivo de destino está momentaneamente
      aberto por outro processo sem compartilhamento de exclusão — tipicamente
      antivírus ou indexador varrendo o arquivo recém-gravado. O travamento dura
      milissegundos, então algumas tentativas com espera crescente resolvem sem
      perder a garantia de "nunca deixa o destino truncado".</p>
  - DuracaoUtil.java
      Formata durações de jobs para o relatório final dos consoles da UI
      (ex.: "1h 04min 12s", "3min 08s", "45s", "0,8s"). Todos os módulos usam o
      mesmo formato para o usuário comparar execuções entre etapas do pipeline.
  - ProcessoExternoUtil.java
      Executa processos externos (ffmpeg, ffprobe, mkvmerge, mkvextract) de forma segura:
      drena stdout e stderr em threads separadas (evita o deadlock classico de ProcessBuilder,
      em que o processo filho trava escrevendo em um pipe cujo buffer do SO enche enquanto o
      pai ainda le o outro stream) e aplica um timeout que mata o processo (destroyForcibly)
      caso ele nao termine a tempo, em vez de travar o pipeline indefinidamente.

[PASTA] src/main/java/org/traducao/projeto/correcaoLegendas/application/
  - CorretorTraducaoLlmService.java
      Retorna a tradução corrigida via LLM apenas se a tradução atual estiver com
      resíduo em inglês/preâmbulo (ValidadorTraducaoService) — evita chamar o LLM
      para falas que já estão corretas.
  - CorrigirLegendasUseCase.java
      (sem cabecalho explicativo)
  - SanitizadorTagsService.java
      LLM costuma alucinar chaves {texto} como marcação de pensamento, o que quebra a linha no Aegisub.
      Início válido de bloco ASS: "\" (override), "=" (marcador do Kara Templater)
      ou "*" (loop do Kara Templater, ex.: {*\c&H24249D&} — visto no Gundam 0083).
      Tags de timing de karaoke ASS: \k, \K, \kf, \ko seguidas de duração (centissegundos).
      Legado: LLM (ou versões antigas deste código) corrompiam a tag do Kara Templater
      "{=X}" para "\N=X".
      Formatação de tela (pos, cor, an8 etc.) sempre fica no prefixo {...} do início da linha.
      Forçamos a tradução a ter exatamente o mesmo prefixo do original — inclusive quando o
      original não tem prefixo nenhum, caso em que qualquer {...} que apareça na tradução é
      alucinação do LLM e precisa ser descartado, não preservado.

[PASTA] src/main/java/org/traducao/projeto/correcaoLegendas/domain/
  - CorrecaoLegendasRelatorioJson.java
      (sem cabecalho explicativo)
  - LogEventoCorrecaoLegendas.java
      (sem cabecalho explicativo)
  - ResultadoCorrecaoLegendas.java
      Resultado da correção: {@code curados} conta ARQUIVOS modificados;
      {@code falasCuradas} e {@code corrigidosLlm} contam FALAS (linhas) — a
      telemetria usa apenas contagens de falas para não misturar unidades.

[PASTA] src/main/java/org/traducao/projeto/correcaoLegendas/infrastructure/
  - CorrecaoLegendasLogPersistencia.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/correcaoLegendas/presentation/
  - CorrecaoLegendasController.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/legendasExtracao/application/
  - ExtrairLegendaUseCase.java
      PROPÓSITO DE NEGÓCIO: Orquestra a extração de softsubs de vídeos — recebe um
      arquivo ou pasta, o formato desejado (ASS/SRT/PGS) e a pasta de saída,
      localiza a faixa daquele formato e a extrai sem conversão, preservando
      timestamps, estilos e conteúdo. Delega a leitura do contêiner aos adaptadores
      ({@link ExtratorVideoPort}) e a escolha da faixa às strategies
      ({@link ExtratorStrategy}).
      
      <p>INVARIANTES DO DOMÍNIO: extrai exatamente o formato pedido, sem fallback
      para outro; nunca sobrescreve arquivo de saída existente; só publica resultado
      validado (existe, não-vazio, formato correto); cada vídeo gera um item no
      relatório e é contabilizado na telemetria.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: falhas por vídeo são isoladas — o item vira
  - ValidadorSaidaExtracao.java
      PROPÓSITO DE NEGÓCIO: Garante que o arquivo recém-extraído é uma legenda de
      verdade no formato pedido — não um arquivo vazio nem uma faixa de outro tipo
      gravada por engano. É a blindagem que separa "extração concluída" de "arquivo
      criado", exigida para nunca entregar lixo ao módulo de tradução.
      
      <p>INVARIANTES DO DOMÍNIO: um arquivo só é válido se (1) existe, (2) tem
      tamanho maior que zero e (3) seu conteúdo bate com a assinatura do formato:
      ASS contém marcador de seção/{@code Dialogue:}; SRT contém a seta de
      timestamp {@code -->}; PGS começa com o magic {@code PG} (0x50 0x47). A
      verificação lê apenas o início do arquivo (amostra), nunca o carrega inteiro.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link ExtratorException} com a razão
      específica (inexistente / vazio / formato divergente / erro de leitura). Não
      remove o arquivo — o cleanup do parcial é responsabilidade do use case.

[PASTA] src/main/java/org/traducao/projeto/legendasExtracao/application/strategy/
  - ExtratorAssStrategy.java
      1. Tentar por palavras-chave
      2. Tentar a última candidata (geralmente a faixa completa em ASS, a primeira é signs)
  - ExtratorPgsStrategy.java
      Para PGS, geralmente pega a primeira encontrada ou a marcada como default
  - ExtratorSrtStrategy.java
      (sem cabecalho explicativo)
  - ExtratorStrategy.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/legendasExtracao/domain/exceptions/
  - ExtracaoTimeoutException.java
      PROPÓSITO DE NEGÓCIO: Sinaliza que a ferramenta externa (mkvextract/ffmpeg)
      estourou o tempo limite durante a extração, para o use case contabilizar
      timeouts separadamente das demais falhas na telemetria e na tabela de resultado.
      
      <p>INVARIANTES DO DOMÍNIO: só deve ser lançada em caso de {@code TimeoutException}
      real do processo externo — nunca reaproveitada para erros genéricos.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: é ela própria a falha; herda de
      {@link ExtratorException} para continuar sendo capturada por quem trata a
      hierarquia genérica, mas permite {@code catch} específico antes.
  - FormatoLegendaInvalidoException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/legendasExtracao/domain/
  - ExtratorException.java
      (sem cabecalho explicativo)
  - FaixaLegenda.java
      (sem cabecalho explicativo)
  - FormatoLegenda.java
      (sem cabecalho explicativo)
  - ItemExtracao.java
      PROPÓSITO DE NEGÓCIO: Linha da tabela de resultado da extração — o que Paulo vê
      por vídeo (Vídeo | Formato | Track | Arquivo gerado | Status). É o registro
      granular que o relatório agregado ({@link RelatorioExtracao}) não expunha antes.
      
      <p>INVARIANTES DO DOMÍNIO: {@code video}, {@code formato} e {@code status}
      nunca são nulos. {@code trackId} e {@code arquivoGerado} são nulos justamente
      quando não houve faixa/arquivo (ex.: {@link StatusExtracao#FAIXA_NAO_ENCONTRADA}),
      e a UI os renderiza como "—".
      
      <p>COMPORTAMENTO EM CASO DE FALHA: record imutável; as fábricas não validam e
      não lançam — o chamador é responsável por passar dados coerentes com o status.
  - RelatorioExtracao.java
      PROPÓSITO DE NEGÓCIO: Acumula o resultado de uma execução de extração — tanto
      os contadores agregados (para o resumo e a telemetria) quanto a lista granular
      por vídeo ({@link ItemExtracao}, que alimenta a tabela da UI). É o objeto que o
      use case devolve à camada de apresentação.
      
      <p>INVARIANTES DO DOMÍNIO: cada vídeo processado incrementa {@code arquivosDetectados}
      e adiciona exatamente um item; a soma de extraídas + sem-faixa + já-existentes +
      falhas + timeouts nunca ultrapassa os vídeos detectados. {@code timeouts} é
      contado à parte de {@code falhasInesperadas}. A lista de itens é exposta como
      cópia imutável.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: mutador simples, não lança. Contadores
      começam em zero; a lista de itens começa vazia.
  - StatusExtracao.java
      PROPÓSITO DE NEGÓCIO: Classifica o desfecho da tentativa de extrair a legenda
      de um único vídeo, para a UI e a telemetria distinguirem "não tinha a faixa"
      de "falhou de verdade" de "já existia" — informação que Paulo usa para decidir
      se reprocessa, troca de formato ou ignora o item.
      
      <p>INVARIANTES DO DOMÍNIO: cada vídeo processado termina em exatamente um
      status. {@link #JA_EXISTE} nunca sobrescreve arquivo; {@link #TIMEOUT} é
      sempre separado de {@link #FALHA} para a telemetria contabilizá-los à parte.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: enum puro, não lança. O rótulo é sempre
      não-nulo (definido no construtor).

[PASTA] src/main/java/org/traducao/projeto/legendasExtracao/domain/ports/
  - ExtratorVideoPort.java
      Abstrai a ferramenta usada para identificar e extrair faixas de legenda de um
      vídeo. Cada implementação é responsável por um conjunto de contêineres
      (ex.: MKVToolNix para Matroska, ffmpeg para os demais formatos).

[PASTA] src/main/java/org/traducao/projeto/legendasExtracao/infrastructure/adapters/
  - FfmpegAdapter.java
      Extrai legendas de contêineres que o MKVToolNix não lê (mkvextract só opera
      sobre Matroska/WebM). Cobre MP4, MOV, AVI e afins via ffmpeg/ffprobe.
  - MkvToolNixAdapter.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/legendasExtracao/infrastructure/config/
  - ExtracaoBeansConfig.java
      PROPÓSITO DE NEGÓCIO: reúne, dentro da própria fatia de Extração de Legendas,
      a composição CDI dos adaptadores de vídeo e das strategies de formato. Entrega
      a {@code ExtrairLegendaUseCase} a coleção completa de implementações
      registradas, para que a escolha do extrator (por contêiner) e da strategy (por
      formato) seja feita em tempo de execução sem a fatia conhecer as classes
      concretas. A composição pertence à fatia dona da extração — não à Tradução Local.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Cada coleção agrega TODAS as implementações CDI disponíveis do respectivo
      tipo, via {@link Instance}, preservando a ordem natural de descoberta do
      container — nenhuma ordenação, prioridade ou sorting é imposto.</li>
      <li>As listas são novas instâncias mutáveis desacopladas do {@link Instance}.</li>
      <li>A semântica é idêntica à anterior (Spring DI integrado ao Quarkus): apenas
      o LOCAL da composição mudou para a fatia proprietária.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Se nenhuma implementação de um dos tipos estiver registrada, a coleção
      correspondente é retornada vazia (nunca nula); o consumidor decide como tratar
      a ausência de extrator/strategy compatível.
  - ExtratorProperties.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/legendasExtracao/presentation/
  - ExtratorCLI.java
      PROPÓSITO DE NEGÓCIO: oferece execução local por terminal da esteira de extração
      de softsubs (vídeo ➔ legenda) da fatia {@code legendasExtracao}, resolvendo o
      único caminho de que precisa — a pasta de vídeos de entrada — a partir da própria
      configuração, sem depender da configuração ou do estado da fatia {@code traducao}.
      
      <p>INVARIANTES DO DOMÍNIO: usa exclusivamente {@code tradutor.diretorio-entrada};
      não injeta diretório de saída nem de cache; entrada ausente, vazia ou só com
      espaços é rejeitada antes de qualquer processamento.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: entrada não configurada encerra sem processar
      (imprime instrução de saída); pasta inexistente encerra sem produzir extração.

[PASTA] src/main/java/org/traducao/projeto/legendasExtracao/presentation/ui/
  - ConsoleExtratorLogger.java
      Tag colorida em negrito (chama atenção), corpo da mensagem em peso normal
      (mais fácil de ler em blocos de texto maiores) — INFO fica sem cor nenhuma.
  - TabelaExtracaoRenderer.java
      PROPÓSITO DE NEGÓCIO: Monta a tabela simples de resultado da extração
      (Vídeo | Formato | Track | Arquivo gerado | Status) que aparece nos consoles
      da UI web e do CLI, dando a Paulo a visão por vídeo — inclusive qual Track ID
      foi extraído — que os contadores agregados não mostravam.
      
      <p>INVARIANTES DO DOMÍNIO: colunas com largura ajustada ao maior valor;
      campos ausentes ({@code trackId}/{@code arquivoGerado} nulos) viram "—". Só de
      apresentação — não decide nada sobre a extração.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem itens, devolve string vazia (o chamador
      simplesmente não imprime). Não lança.

[PASTA] src/main/java/org/traducao/projeto/legendasExtracao/presentation/web/
  - ExtracaoLegendaController.java
      PROPÓSITO DE NEGÓCIO: expõe a extração de legendas (Opção 2) à interface web,
      validando o formato-alvo e enfileirando o processamento pesado que percorre a
      pasta de vídeos e extrai as faixas de legenda no formato escolhido.
      
      <p>Fronteira arquitetural: este endpoint pertence ao módulo
      {@code legendasExtracao} (Opção 2) e reside na sua camada de apresentação
      própria. Não importa nenhuma regra funcional da Tradução Local (Opção 4): usa
      apenas o use case e os tipos do próprio módulo. As dependências
      {@link PipelineWebSupport}, {@link RespostaPadrao} e {@link ExtracaoRequest}
      são <b>glue técnico de apresentação</b> (fila única e contratos de transporte
      HTTP) hoje em {@code traducao.presentation.web}; é dívida técnica temporária
      reservada para saneamento na FASE E — não é acoplamento funcional.
      
      <p>INVARIANTES DO DOMÍNIO: usa a MESMA fila compartilhada via
      {@link PipelineWebSupport}; o formato é validado antes de enfileirar; caminhos
      são normalizados; a rota {@code POST /api/extrair}, o status e os campos de DTO
      são contrato público preservado exatamente como antes da movimentação.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: entrada em branco ou formato inválido
      retorna HTTP 400; falhas do job de background são registradas no log e no
  - ExtracaoRequest.java
      PROPÓSITO DE NEGÓCIO: transporta os parâmetros da extração de legendas —
      pasta de vídeos, pasta de saída e o formato-alvo escolhido na interface.
      
      <p>INVARIANTES DO DOMÍNIO: os nomes dos campos são contrato JSON público; o
      formato é validado contra {@code FormatoLegenda} antes do job entrar na fila.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: entrada em branco ou formato inválido faz o
      endpoint responder HTTP 400 antes de qualquer processamento.

[PASTA] src/main/java/org/traducao/projeto/legenda/application/
  - DetectorEfeitoKaraokeService.java
      PROPÓSITO DE NEGÓCIO: reconhece se um evento .ass/.ssa é efeito de
      karaokê/música (e não fala de diálogo), para que nenhuma fatia funcional
      (tradução, revisão, correção) mexa em música — responsabilidade exclusiva do
      fluxo de karaokê. É a regra única compartilhada, agora residente no peer
      {@code legenda}, consumível por qualquer fatia sem acoplamento reverso.
      
      <p>Cobre as duas formas em que o karaokê aparece nos arquivos .ass:
      <ul>
      <li>Karaokê "cru": tags de timing {@code \k}, {@code \kf}, {@code \ko}
      por sílaba, como sai do fansub antes de aplicar template.</li>
      <li>Saída do Kara Templater do Aegisub: as tags {@code \k} são consumidas
      na aplicação do template e viram uma linha por sílaba/letra com
      transformações animadas ({@code \t(...)}, {@code \frx}, {@code \fad},
      {@code \pos}) e quase nenhum texto visível.</li>
      </ul>
      
      <p>INVARIANTES DO DOMÍNIO: distingue música de diálogo pela assinatura de tags
      e pela densidade de texto visível; preserva letra em japonês/romaji (kana/kanji
      ou estilo marcado como japonês) para nunca destruí-la, enquanto karaokê/música
      em idiomas latinos com texto traduzível pode seguir para tradução. Em caso de
      dúvida o viés é preservar: deixar uma linha de música sem traduzir custa menos
      que corromper romaji. A classe é sem estado (stateless) e depende apenas de
      JDK e Spring.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: entradas nulas ou em branco devolvem
      {@code false} (não classificam o evento como música/karaokê) e nenhum método
      lança — cada consulta é uma decisão booleana determinística sobre o
      texto/estilo fornecido.

[PASTA] src/main/java/org/traducao/projeto/legenda/domain/
  - ArquivoLegendaException.java
      PROPÓSITO DE NEGÓCIO: sinaliza falha ao ler ou escrever um arquivo de legenda
      (I/O, formato inválido, seção ausente) dentro do módulo compartilhado
      {@code legenda}. É a exceção de I/O de legenda que leitores e escritores lançam
      e que os fluxos consumidores tratam como falha de arquivo.
      
      <p>INVARIANTES DO DOMÍNIO: estende {@link ExcecaoLegenda} (raiz do módulo legenda),
      portanto é {@code BasePipelineException} e NÃO é {@code TradutorException}. Preserva
      os dois construtores canônicos (mensagem; mensagem+causa); não adiciona estado nem
      lógica.
      
      <p>COMPORTAMENTO EM CASO DE PROPAGAÇÃO: propaga como {@code RuntimeException} não
      verificada; é mapeada para resposta HTTP pelo {@code BasePipelineExceptionMapper} e
      pode ser capturada por blocos que tratem {@link ExcecaoLegenda} (ou este tipo).
  - DocumentoLegenda.java
      PROPÓSITO DE NEGÓCIO: representa, como dado imutável do módulo compartilhado
      {@code legenda}, uma legenda inteira já parseada — o cabeçalho bruto, a sequência
      ordenada de eventos ({@link EventoLegenda}) e os metadados de serialização
      (marca de quebra de linha e presença de BOM) necessários para reescrever o arquivo
      fielmente.
      
      <p>INVARIANTES DO DOMÍNIO: é um {@code record} — os quatro componentes são fixados
      na construção e expostos pelos acessores; igualdade e hash derivam de todos eles.
      A imutabilidade é rasa: a referência da lista {@code eventos} é armazenada como
      recebida (não há cópia defensiva). Não há validação: qualquer valor, inclusive
      {@code null} em qualquer componente, é aceito.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: o próprio record não lança nem sanitiza nada;
      entradas inválidas (ex.: {@code eventos} nulo) só se manifestam quando um consumidor
      as percorre. Nenhuma responsabilidade de I/O, parsing ou tradução vive aqui.
      
      @param cabecalho bloco bruto de cabeçalho da legenda, preservado para reescrita fiel
      @param eventos sequência ordenada de eventos (linhas) da legenda
      @param quebraDeLinha marca de quebra de linha original do arquivo, usada na serialização
      @param comBom indica se o arquivo original tinha BOM, a ser reproduzido na escrita
  - EventoLegenda.java
      PROPÓSITO DE NEGÓCIO: representa, como dado imutável do módulo compartilhado
      {@code legenda}, um único evento (linha) de uma legenda — seu índice de ordem, o
      tipo de linha (ex.: {@code Dialogue}), o estilo, o prefixo estrutural e o texto
      visível — servindo de unidade que leitores, escritores e regras percorrem.
      
      <p>INVARIANTES DO DOMÍNIO: é um {@code record} — os cinco componentes são fixados na
      construção; igualdade e hash derivam de todos eles. Não há validação: qualquer valor,
      inclusive {@code null} em {@code texto} ou nos demais campos de texto, é aceito.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: o record não lança nem sanitiza; consultas sobre
      campos nulos são tratadas pelos próprios métodos ({@link #temTexto()} devolve
      {@code false} para {@code texto} nulo). Nenhuma responsabilidade de I/O ou tradução
      vive aqui.
      
      @param indice posição ordinal do evento dentro da legenda
      @param tipoLinha tipo da linha (ex.: {@code Dialogue}), base de {@link #isDialogo()}
      @param estilo nome do estilo associado ao evento
      @param prefixo prefixo estrutural da linha, preservado na reescrita
      @param texto texto visível do evento; pode ser {@code null}
  - ExcecaoLegenda.java
      PROPÓSITO DE NEGÓCIO: raiz da hierarquia de exceções pertencentes ao módulo
      compartilhado {@code legenda} — falhas ligadas a arquivos e conteúdo de legenda.
      NÃO representa falhas gerais de tradução nem do LLM (essas vivem sob
      {@code TradutorException}, na fatia {@code traducao}); é a base específica das
      falhas do domínio de legenda, consumível por qualquer fatia.
      
      <p>INVARIANTES DO DOMÍNIO: estende {@code BasePipelineException} (core), herdando
      {@code errorId} e {@code timestamp}; é concreta e oferece apenas os dois construtores
      canônicos (mensagem; mensagem+causa). Não declara estado próprio, código de
      infraestrutura nem status HTTP — o mapeamento HTTP é responsabilidade única do
      {@code BasePipelineExceptionMapper}, comum a toda a família.
      
      <p>COMPORTAMENTO EM CASO DE PROPAGAÇÃO: propaga como {@code RuntimeException} não
      verificada; por ser {@code BasePipelineException}, é convertida em resposta HTTP
      estruturada pelo mapper e pode ser capturada por qualquer bloco que trate
      {@code ExcecaoLegenda} ou uma de suas subclasses.
  - PoliticaEstiloMusical.java
      PROPÓSITO DE NEGÓCIO: política de IDENTIFICAÇÃO de estilos ASS potencialmente
      MUSICAIS/preserváveis (letra japonesa, romaji, karaokê, aberturas/encerramentos),
      usada EM CONJUNTO com o detector de conteúdo do pipeline. A subfase E3c apenas
      transfere o PROPRIETÁRIO desta regra — antes em
      {@code TradutorProperties.estiloIgnorado} — para o módulo compartilhado
      {@code legenda}, SEM alterar o comportamento funcional do fluxo.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Sinaliza um estilo quando ele está na lista configurada
      ({@code tradutor.estilos-ignorados}, comparação case-insensitive) OU quando é
      reconhecido pelas heurísticas/regex de palavras musicais já existentes.</li>
      <li>Esta política, SOZINHA, NÃO decide se uma linha será enviada ao LLM. A decisão
      final de preservação considera TAMBÉM o conteúdo da linha, avaliado pelo
      {@code DetectorEfeitoKaraokeService} (ex.: {@code eKaraokeOuMusicaTraduzivel},
      {@code devePreservarKaraokeOriginal}), que permanece o proprietário dessa parte.</li>
      <li>Letras japonesas e romaji protegidos devem permanecer INTACTOS; a versão em
      INGLÊS que acompanha o karaokê PODE continuar traduzível quando o detector
      assim determinar.</li>
      <li>Guarda uma cópia IMUTÁVEL da lista recebida, preservando ordem, case,
      duplicatas e elementos vazios — sem trim, dedup ou normalização.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Estilo {@code null} ou em branco retorna {@code false} (não identificado como musical).
      Um retorno {@code false} significa apenas "não identificado como estilo musical por
      esta política" — NÃO significa, por si só, "diálogo padrão" nem "linha traduzível".

[PASTA] src/main/java/org/traducao/projeto/legenda/infrastructure/config/
  - PoliticaEstiloMusicalProducer.java
      PROPÓSITO DE NEGÓCIO: ponte técnica (CDI) que lê a lista configurada de estilos
      musicais preserváveis e produz a política de domínio pura {@link PoliticaEstiloMusical}.
      Isola o Quarkus/MicroProfile Config na borda de infraestrutura do módulo {@code legenda},
      mantendo o domínio livre de framework.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Chave lida: {@code tradutor.estilos-ignorados} (mantida nesta subfase — sem rename).</li>
      <li>Produz um bean de PSEUDO-ESCOPO {@code @Singleton}: {@link PoliticaEstiloMusical}
      é {@code final} e não admite proxy de escopo normal.</li>
      <li>Fallback {@code ["Song JP"]} quando a chave está ausente (Optional.empty).</li>
      </ul>
      
      <h2>Comportamento em caso de falha / caracterização de binding (E3c)</h2>
      Chave ausente → política construída com o fallback {@code ["Song JP"]}, idêntico ao
      binding histórico do {@code TradutorProperties}. Caracterização comprovada: sob string
      vazia ({@code ""}), Spring e SmallRye COLAPSAM em ausente (ambos → {@code ["Song JP"]}) —
      paridade preservada. A sequência YAML vazia ({@code []}) NÃO foi caracterizada e não
      deve ser assumida como estado distinto.

[PASTA] src/main/java/org/traducao/projeto/legenda/infrastructure/
  - EscritorLegendaAss.java
      PROPÓSITO DE NEGÓCIO: reconstrói o arquivo {@code .ass} a partir de um
      {@link DocumentoLegenda}, repetindo o cabeçalho original e as linhas não
      traduzíveis byte a byte, e trocando apenas o campo Text dos eventos
      {@code Dialogue} pela versão traduzida. É o par de saída do {@link LeitorLegendaAss}.
      
      <p>INVARIANTES DO DOMÍNIO: o EOL e o BOM do documento original são reproduzidos na
      saída; a escrita é atômica — grava em arquivo temporário e substitui o destino via
      {@link org.traducao.projeto.core.util.ArquivoAtomicoUtil#substituirAtomico}.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: erro de I/O de escrita lança
      {@link ArquivoLegendaException}, sem deixar o arquivo de destino truncado (a
      substituição atômica só ocorre após a gravação completa do temporário).
  - EscritorLegendaSrt.java
      PROPÓSITO DE NEGÓCIO: Reescreve um .srt a partir do {@link DocumentoLegenda},
      preservando numeração e timestamps (guardados no índice e no {@code prefixo}
      do evento) e trocando apenas o texto pela versão traduzida. É o par de saída
      do {@link LeitorLegendaSrt}.
      
      <p>INVARIANTES DO DOMÍNIO: cada evento vira um bloco SRT válido (índice, linha
      de tempo, texto, linha em branco de separação); as marcas {@code \N} de quebra
      interna voltam a ser quebras reais no EOL do documento; escrita atômica.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: erro de IO → {@link ArquivoLegendaException},
      sem deixar arquivo truncado (grava em temporário e move atomicamente).
  - LeitorLegendaAss.java
      PROPÓSITO DE NEGÓCIO: lê arquivos {@code .ass}/{@code .ssa} para um
      {@link DocumentoLegenda}, preservando byte a byte tudo que não for o campo Text
      dos eventos {@code Dialogue} (estilos, timestamps, seções de metadados). Só o campo
      Text é exposto para tradução; o resto é reconstruído idêntico pelo
      {@link EscritorLegendaAss}.
      
      <p>INVARIANTES DO DOMÍNIO: o cabeçalho, o EOL e o BOM originais são guardados no
      documento para reescrita fiel; cada evento é mapeado para um {@link EventoLegenda}
      conforme a ordem das colunas declarada na linha {@code Format:} da seção
      {@code [Events]}.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: erro de I/O de leitura, arquivo ilegível ou
      seção {@code [Events]} sem linha {@code Format:} lançam {@link ArquivoLegendaException}.
  - LeitorLegendaSrt.java
      PROPÓSITO DE NEGÓCIO: Lê legendas SubRip (.srt) para o mesmo
      {@link DocumentoLegenda} usado pelo ASS, para que o pipeline de tradução
      (cache, máscara de tags, validação) opere sobre SRT sem convertê-lo para ASS.
      Numeração e timestamps ficam no {@code prefixo} do evento (a linha de tempo) e
      no índice; só o texto é traduzido. Quebras internas viram {@code \N} (convenção
      ASS), que o {@link EscritorLegendaSrt} devolve para quebras reais.
      
      <p>INVARIANTES DO DOMÍNIO: cada bloco SRT (índice + "start --> end" + texto)
      vira um {@link EventoLegenda} {@code Dialogue} de estilo "Default"; o EOL e o
      BOM originais são preservados no documento.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: erro de leitura do arquivo →
      {@link ArquivoLegendaException}. Blocos malformados (índice não numérico) são
      tolerados: o índice cai para a posição sequencial.

[PASTA] src/main/java/org/traducao/projeto/llm/domain/
  - LlmPort.java
      PROPÓSITO DE NEGÓCIO: contrato genérico de saída para o modelo de linguagem local
      (servido, por exemplo, via LM Studio). É a porta pela qual qualquer fatia funcional
      pede tradução de um lote, revisão de concordância, correção de uma fala ou a checagem
      de disponibilidade do servidor — sem conhecer o cliente HTTP concreto nem o modelo.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Contrato puro de domínio: depende apenas de JDK e dos tipos do próprio peer
      {@code llm} ({@link Lote}, {@link TraducaoLote}, {@link StatusLlm}); não conhece
      framework, HTTP, contexto nem qualquer fatia funcional.</li>
      <li>A tradução opera sobre um {@link Lote} e devolve um {@link TraducaoLote} que
      preserva o {@code idLote}; as variantes de temperatura e de prompt congelado
      apenas refinam a mesma operação.</li>
      <li>As revisões pontuais retornam {@link Optional}, distinguindo "sem correção
      aplicável" de uma string vazia.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      A implementação concreta ({@code traducao.infrastructure.adapters.LlmClientAdapter})
      define o tratamento de rede/timeout. No contrato, {@code revisarConcordancia} e
      {@code corrigirTraducao} devolvem {@link Optional#empty()} quando o LLM falha ou a
      resposta é inválida, de modo que o chamador preserve a tradução anterior.
  - Lote.java
      PROPÓSITO DE NEGÓCIO: unidade de trabalho enviada ao LLM — um conjunto de linhas
      originais a traduzir de uma vez, identificado para que a resposta possa ser
      correlacionada de volta ao pedido.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>{@code idLote} identifica o lote e é preservado no {@link TraducaoLote} de resposta.</li>
      <li>{@code linhasOriginais} é a sequência a traduzir, na ordem em que deve ser devolvida.</li>
      <li>Record imutável de domínio: só JDK, sem dependência de framework ou fatia.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Não valida os argumentos; é um portador de dados. A ausência ou o formato inválido de
      linhas é tratado pela implementação da porta, não por este tipo.
      
      @param idLote identificador do lote, ecoado na resposta
      @param linhasOriginais linhas originais a traduzir, na ordem de saída esperada
  - StatusLlm.java
      PROPÓSITO DE NEGÓCIO: resultado da checagem de disponibilidade do servidor LLM local
      (ex.: LM Studio) feita no início da execução, antes de traduzir qualquer episódio — para
      falhar cedo, com mensagem clara, em vez de descobrir a indisponibilidade após vários
      timeouts no meio do trabalho.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>{@code servidorOnline} e {@code modeloCarregado} são sinais independentes: o
      servidor pode estar de pé sem o modelo configurado carregado.</li>
      <li>{@code mensagem} descreve o estado para exibição ao operador.</li>
      <li>Record imutável de domínio: só JDK, sem dependência de framework ou fatia.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      É o próprio veículo do estado de falha: servidor offline ou modelo ausente são
      representados pelos flags e pela {@code mensagem}, não por exceção.
      
      @param servidorOnline {@code true} se o servidor LLM respondeu
      @param modeloCarregado {@code true} se o modelo configurado está carregado em memória
      @param mensagem descrição do estado para o operador
  - TraducaoLote.java
      PROPÓSITO DE NEGÓCIO: resultado da tradução de um {@link Lote} pelo LLM — as linhas
      traduzidas mais o desfecho (sucesso ou falha com diagnóstico), para que o pipeline
      decida entre publicar, retentar ou preservar o original.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>{@code idLote} espelha o do {@link Lote} de origem, correlacionando pedido e resposta.</li>
      <li>{@code linhasTraduzidas} corresponde às linhas originais na mesma ordem.</li>
      <li>{@code sucesso} indica se a tradução é utilizável; {@code mensagemErro} traz o
      diagnóstico quando não é.</li>
      <li>Record imutável de domínio: só JDK, sem dependência de framework ou fatia.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Em falha, {@code sucesso} é {@code false} e {@code mensagemErro} descreve a causa; o
      chamador é quem decide preservar a tradução anterior. Este tipo não lança.
      
      @param idLote identificador do lote, herdado do {@link Lote} de origem
      @param linhasTraduzidas linhas traduzidas, na ordem das originais
      @param sucesso {@code true} se a tradução é utilizável
      @param mensagemErro diagnóstico quando {@code sucesso} é {@code false}

[PASTA] src/main/java/org/traducao/projeto/mapaProjeto/application/
  - GeradorMapaProjetoUseCase.java
      Pastas que não representam arquitetura/código-fonte e por isso são podadas
      do mapa: controle de versão/IDE, build/gradle/bin, e artefatos OPERACIONAIS
      volumosos (cache, logs, relatorios, backups) que, se incluídos, dominavam o
      mapa com centenas de entradas de saída de execução em vez de estrutura.
      Prefixos de nomes de diretório/arquivo criados por testes (ex.: os
      relatorios/junit-<n> gerados por @TempDir do JUnit) — podados onde quer
  - MapeadorDiretorioUseCase.java
      Cabeçalho Técnico
      PARTE 1: CAMINHO ABSOLUTO COMPLETO NO SISTEMA LOCAL

[PASTA] src/main/java/org/traducao/projeto/mapaProjeto/domain/exceptions/
  - MapaProjetoException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/mapaProjeto/presentation/
  - MapaProjetoCLI.java
      E3b: chave crua; ausência/branco tratados pelo fallback de domínio local (user.dir).
      Determina a raiz a ser mapeada

[PASTA] src/main/java/org/traducao/projeto/mapaProjeto/presentation/web/
  - MapaController.java
      PROPÓSITO DE NEGÓCIO: expõe a geração do mapa do projeto (Opção 7) à interface
      web, produzindo o relatório em markdown e a árvore no formato GitHub a partir
      da raiz do projeto.
      
      <p>Fronteira arquitetural: este endpoint pertence ao módulo {@code mapaProjeto},
      sua funcionalidade proprietária, e por isso reside na camada de apresentação do
      próprio módulo. Não depende funcionalmente da Tradução Local (Opção 4); usa
      apenas o use case do próprio módulo e a raiz técnica neutra {@code core}.
      
      <p>INVARIANTES DO DOMÍNIO: a raiz mapeada vem de
      {@link DiretorioBaseKronos#base()} — em produção é o diretório de trabalho e,
      sob a suíte de testes, a árvore descartável, evitando reescrever o mapa real;
      a rota {@code POST /api/mapa}, o status e os campos JSON de {@link MapaResponse}
      são contrato público preservado exatamente como antes da movimentação.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: a geração é síncrona; qualquer falha do use
      case propaga como erro do endpoint, sem estado parcial retornado ao navegador.
  - MapaResponse.java
      PROPÓSITO DE NEGÓCIO: entrega ao painel "Mapa do Projeto" o relatório em
      markdown, a árvore no formato GitHub e o nome do projeto gerados pelo módulo
      de mapeamento.
      
      <p>INVARIANTES DO DOMÍNIO: os nomes dos campos são contrato JSON público
      consumido pela SPA; representam o resultado já pronto para renderização.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sendo um record imutável, não há falha de
      construção; a ausência de conteúdo é responsabilidade do use case que o produz.

[PASTA] src/main/java/org/traducao/projeto/mcp/
  - KronosMcpTools.java
      Ferramentas MCP (Model Context Protocol) expostas pelo KRONOS via transporte
      SSE em {@code /mcp/sse}. Clientes MCP (ex.: Claude Code) acionam o pipeline
      enquanto o servidor web ja esta rodando em modo dev.
      <p>
      Toda operação pesada passa pela mesma {@link FilaExecucaoPipeline} da UI: o
      MCP não é uma porta paralela. Isso garante execução sequencial (MCP e UI não
      disputam GPU/estado global), torna o job visível a {@code ocupada()} e o deixa
      cancelável pelo "Parar".

[PASTA] src/main/java/org/traducao/projeto/novoKaraoke/application/
  - ConversorKaraokeUseCase.java
      Converte legendas .ass com karaokê KFX (milhares de eventos por sílaba/frame)
      em legendas simples: uma linha limpa por frase da música, no MESMO tempo do
      efeito original (início = menor início do bloco, fim = maior fim).
      <p>
      Garantias de segurança:
      <ul>
      <li>O arquivo original NUNCA é alterado — a saída vai para a pasta que o
      usuário escolher.</li>
      <li>Diálogo, placas e Comment são reemitidos byte a byte (linha crua).</li>

[PASTA] src/main/java/org/traducao/projeto/novoKaraoke/domain/
  - EventoAss.java
      Um evento {@code Dialogue:} de um arquivo .ass, com a linha crua preservada
      byte a byte. A conversão de karaokê NUNCA reescreve eventos que decide
      manter — ela reemite {@link #linhaCrua()} — para garantir que diálogo,
      placas e blocos preservados saiam idênticos ao arquivo de origem.
      
      @param linhaCrua  linha original completa, exatamente como lida do arquivo
      @param camada     campo Layer
      @param inicio     campo Start (mantido como texto para não perder precisão)
      @param fim        campo End
      @param estilo     campo Style
      @param texto      campo Text (último campo, pode conter vírgulas)
  - LinhaSimplesKaraoke.java
      Uma linha de letra de música reconstruída a partir do bloco de eventos KFX.
      O tempo é herdado literalmente dos eventos de origem: {@code inicioCs} é o
      menor início e {@code fimCs} o maior fim do grupo — nenhum deslocamento é
      introduzido, a legenda simples ocupa exatamente a janela do efeito original.
      
      @param texto            texto visível da linha (sem tags)
      @param inicioCs         menor início do grupo, em centésimos
      @param fimCs            maior fim do grupo, em centésimos
      @param eventosOrigem    quantos eventos KFX foram colapsados nesta linha
      @param variantesTexto   variantes divergentes encontradas na mesma janela (>1 indica voto majoritário)
  - NovoKaraokeException.java
      Falha de negócio na conversão de karaokê para legenda simples. /
  - ResultadoConversaoKaraoke.java
      Resultado da conversão de um arquivo .ass: contadores para o resumo do
      console/telemetria e o material do manifesto de auditoria.

[PASTA] src/main/java/org/traducao/projeto/novoKaraoke/infrastructure/
  - NovoKaraokePersistencia.java
      Manifesto de auditoria da conversão de karaokê: registra, por execução, o
      que foi removido/criado em cada arquivo. Fica em
      {@code logs/novo-karaoke/} dentro do projeto — junto com os originais
      intocados na pasta de origem, é a trilha completa para auditar (ou refazer)
      qualquer conversão.

[PASTA] src/main/java/org/traducao/projeto/novoKaraoke/presentation/
  - NovoKaraokeController.java
      Endpoints do módulo Karaokê Simples. Operação puramente local (sem LLM,
      sem estado global do pipeline), por isso roda async fora da fila — mesmo
      padrão do módulo de Renomear Arquivos.
  - NovoKaraokeRequest.java
      Requisição da conversão de karaokê: pasta das legendas .ass de origem e a
      pasta de destino (obrigatoriamente diferente — o original é preservado).

[PASTA] src/main/java/org/traducao/projeto/qualidadeTraducao/application/
  - DetectorTraducaoIdenticaService.java
      PROPÓSITO DE NEGÓCIO: decide se uma fala pode legitimamente permanecer idêntica ao
      original (nomes próprios, números, siglas, termos de lore) ou se a igualdade é sinal
      de que o LLM simplesmente devolveu a fala sem traduzir. Impede que manutenção ou
      retomada do cache apague nomes canônicos e, simultaneamente, não aceite frases
      inglesas como tradução. Além da lista global fixa, consulta os termos protegidos e a
      lore do contexto ATIVO através da porta {@link LoreAtivaPort}, para que um termo novo
      anexado ao contexto selecionado seja protegido sem editar este detector — e sem que o
      peer {@code qualidadeTraducao} dependa da fatia {@code contexto}.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>A lore ativa (via porta) é a fonte dos termos protegidos; expressões
      conversacionais comuns continuam exigindo tradução.</li>
      <li>A precedência das verificações é preservada: limpeza de tags, gagueira e
      pontuação; caso de caractere único; palavra única; então lore ativa e, por fim,
      heurística de capitalização.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Texto sem evidência suficiente é preservado para evitar uma decisão destrutiva; a
      porta não lança, então lore/termos ausentes apenas recaem nas heurísticas globais.
  - MascaradorTags.java
      PROPÓSITO DE NEGÓCIO: protege a INTEGRIDADE da formatação da legenda ao redor da
      tradução. Isola tags ASS/SSA (ex: {@code {\i1}}, {@code {\pos(...)}}) e códigos de
      quebra ({@code \N}, {@code \n}, {@code \h}) do texto, trocando-os por marcadores
      {@code [[TAGn]]} que o LLM é instruído a preservar literalmente — sem isso o modelo
      tende a "traduzir" ou descartar as tags, corrompendo a legenda renderizada. Regra
      de qualidade compartilhada, residente no peer {@code qualidadeTraducao}.
      
      <p>INVARIANTES DO DOMÍNIO: quantidade, conteúdo e ordem das tags do original são
      preservados na desmascaração; cada tag vira exatamente um marcador {@code [[TAGn]]}
      sequencial; a classe é sem estado (stateless), só JDK + Spring.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: se o texto retornado pelo LLM perdeu, duplicou
      ou inventou marcadores, {@link #desmascarar(String, List)} lança
      {@link AlucinacaoDetectadaException} em vez de gravar formatação corrompida;
      verificações estruturais nulas/divergentes devolvem {@code false}.
  - ProtecaoLegendaAssService.java
      PROPÓSITO DE NEGÓCIO: blindagens compartilhadas para linhas ASS/SSA antes e depois
      de chamadas a IA/serviços externos. Centraliza os casos perigosos encontrados em
      fansubs — clips vetoriais longos, letras soltas pós-template e preâmbulos alucinados
      — para que typesetting pesado não seja enviado ao LLM nem sobrescrito por uma
      resposta que destruiria o efeito visual original.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Só o texto VISÍVEL decide: blocos {@code {...}} de override/comentário ASS são
      removidos para a inspeção e o texto recebido nunca é modificado.</li>
      <li>Bloqueio antes do LLM exige conjunção de sinais (tags pesadas + alta densidade
      de tags + texto curto): uma fala normal com duas camadas de estilo nunca é
      bloqueada só por ter tags.</li>
      <li>Serviço stateless — só constantes {@code static final}. Qualquer instância é
      intercambiável, o que sustenta tanto a injeção Spring quanto o reuso estático
      pelos chamadores.</li>
      <li>Contrato público do peer são os métodos de INSTÂNCIA; os estáticos permanecem
      package-private como detalhe interno de implementação.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Nenhum método lança: entrada {@code null} degrada para o lado seguro — {@code false}
      nas heurísticas de suspeita/bloqueio (não intervir) e {@code ""} na extração de texto
      visível. Texto sem conteúdo visível é bloqueado antes do LLM, evitando gastar chamada
      em linha puramente decorativa.
  - ValidadorTraducaoService.java
      PROPÓSITO DE NEGÓCIO: impede que textos parcialmente traduzidos, respostas
      rotuladas ou conteúdo em idioma indevido cheguem às legendas e ao cache.
      <p>INVARIANTES DO DOMÍNIO: comentários ASS não visíveis são ignorados, nomes
      próprios conhecidos não viram falso positivo e resíduos visíveis inequívocos
      sempre bloqueiam a proposta.
      <p>COMPORTAMENTO EM CASO DE FALHA: lança
      {@link AlucinacaoDetectadaException} com diagnóstico e o chamador preserva a
      tradução anterior.

[PASTA] src/main/java/org/traducao/projeto/qualidadeTraducao/domain/
  - AlucinacaoDetectadaException.java
      PROPÓSITO DE NEGÓCIO: sinaliza que o LLM alucinou de forma que compromete a
      INTEGRIDADE da legenda — corrupção/perda dos marcadores de formatação
      ({@code [[TAGn]]}) ou fala rejeitada pelo validador de qualidade. Impede que uma
      saída corrompida seja publicada como se fosse tradução válida. Pertence ao peer
      compartilhado {@code qualidadeTraducao}, consumível por qualquer fatia.
      
      <p>INVARIANTES DO DOMÍNIO: é subclasse de {@link ExcecaoQualidadeTraducao} (logo de
      {@code BasePipelineException}) — a partir da E8b NÃO é mais {@code TradutorException};
      carrega apenas a mensagem descritiva da alucinação, sem estado próprio.
      
      <p>COMPORTAMENTO EM CASO DE PROPAGAÇÃO: propaga como {@code RuntimeException} não
      verificada; nos fluxos de tradução é normalmente absorvida pela divisão/retry/fallback,
      e os sítios que antes a capturavam via {@code TradutorException} preservam o
      tratamento por captura explícita ({@code catch (... | AlucinacaoDetectadaException)}).
  - ExcecaoQualidadeTraducao.java
      PROPÓSITO DE NEGÓCIO: raiz da hierarquia de exceções do módulo compartilhado
      {@code qualidadeTraducao} — falhas ligadas à QUALIDADE do texto traduzido
      (alucinação, corrupção de marcadores de formatação) detectadas por regras
      consumíveis por qualquer fatia. NÃO representa falhas gerais de tradução/LLM
      (essas vivem sob {@code TradutorException}, na fatia {@code traducao}), de legenda
      (sob {@code ExcecaoLegenda}) nem de contexto (sob {@code ExcecaoContexto}).
      
      <p>INVARIANTES DO DOMÍNIO: estende {@code BasePipelineException} (core), herdando
      {@code errorId} e {@code timestamp}; é concreta e oferece apenas os dois construtores
      canônicos (mensagem; mensagem+causa). Não declara estado próprio, código de
      infraestrutura nem status HTTP — o mapeamento HTTP é responsabilidade única do
      {@code BasePipelineExceptionMapper}, comum a toda a família.
      
      <p>COMPORTAMENTO EM CASO DE PROPAGAÇÃO: propaga como {@code RuntimeException} não
      verificada; por ser {@code BasePipelineException}, é convertida em resposta HTTP
      estruturada pelo mapper e pode ser capturada por qualquer bloco que trate
      {@code ExcecaoQualidadeTraducao} ou uma de suas subclasses.
  - LoreAtivaPort.java
      PROPÓSITO DE NEGÓCIO: porta de saída pela qual o peer {@code qualidadeTraducao}
      obtém a terminologia e a lore da obra atualmente selecionada, para decidir se uma
      fala idêntica ao original é um termo canônico legítimo (nome, facção, patente) ou
      uma tradução que o LLM simplesmente não fez. Inverte a dependência que antes ligava
      o detector diretamente ao {@code contexto}: o peer declara o contrato de que precisa
      e a fatia que possui o contexto fornece a implementação.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Contrato mínimo: exatamente as duas leituras que o detector consome — nada de
      escrita, seleção de contexto ou exposição do prompt de tradução completo.</li>
      <li>É a lore do contexto ATIVO no momento da consulta; trocar o contexto ativo muda
      o que estes métodos retornam, sem que o consumidor precise ser reconfigurado.</li>
      <li>Pertence ao domínio do peer: depende apenas de JDK, para não reintroduzir
      acoplamento a {@code contexto} nem a qualquer outra fatia.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Nenhum método lança: a ausência de contexto ativo é um estado normal, não um erro.
      {@link #termosProtegidosAtivos()} degrada para conjunto vazio e {@link #obterLoreAtiva()}
      para a lore neutra que a implementação adotar — o detector, diante de qualquer um dos
      dois, apenas deixa de reconhecer termos de lore e recai nas heurísticas globais.

[PASTA] src/main/java/org/traducao/projeto/raspagemCorrecao/application/
  - CorrigirComGoogleUseCase.java
      PROPÓSITO DE NEGÓCIO: preenche por contingência online as lacunas e falhas do
      banco de tradução que a Tradução Local não pode reutilizar.
      
      <p>INVARIANTES DO DOMÍNIO: somente candidatos do classificador canônico são
      enviados ao Google; nomes/termos protegidos vêm da lore do próprio cache;
      tags e efeitos protegidos não são tocados; toda gravação tem backup e troca
      atômica.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: falhas de rede permanecem pendentes no
      cache, são auditadas e não impedem salvar correções válidas já obtidas.
  - ProtetorTermosLoreService.java
      PROPÓSITO DE NEGÓCIO: impede que a contingência Google traduza literalmente
      nomes e terminologia que a lore manda manter na forma oficial.
      
      <p>INVARIANTES DO DOMÍNIO: termos maiores são mascarados antes dos menores;
      a grafia encontrada no original é restaurada; marcadores nunca podem sobrar.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: restauração incompleta devolve
      {@code null}, fazendo o chamador manter a entrada pendente.

[PASTA] src/main/java/org/traducao/projeto/raspagemCorrecao/
  - CorretorRaspagemCLI.java
      CommandLineRunner que realiza a tradução das falas residuais pendentes em inglês
      utilizando raspagem na API gratuita e sem chaves do Google Translate.
      Ativado quando a propriedade app.modo é configurada como "RASPAGEM_CORRECAO".

[PASTA] src/main/java/org/traducao/projeto/raspagemCorrecao/domain/exceptions/
  - RaspagemCorrecaoException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/raspagemCorrecao/infrastructure/
  - GoogleTranslateScraper.java
      Traduz texto via API pública do Google Translate, preservando tags ASS
      mascaradas e quebras {@code \N}.
      <p>
      O retorno é tipado ({@link ResultadoRaspagem}): cada desfecho — sucesso, sem
      alteração, falha transitória, resposta inválida ou tag corrompida — vira um
      {@link StatusRaspagem} explícito, em vez de o chamador ter que adivinhar a
      partir de "o texto voltou igual". O transporte HTTP fica isolado em
      {@link #executarGet(String)} para poder ser substituído em testes.
  - ResultadoRaspagem.java
      Resultado tipado de {@link GoogleTranslateScraper#traduzir(String)}: o
      {@link StatusRaspagem} do desfecho e o texto associado.
      <p>
      Em {@link StatusRaspagem#SUCESSO}, {@code texto} é a tradução; em qualquer
      outro caso é o <b>texto original</b> (o chamador mantém a fala intacta), agora
      sabendo o MOTIVO em vez de inferir por igualdade de strings.
  - StatusRaspagem.java
      Desfecho semântico de uma tentativa de tradução via Google Translate.
      <p>
      Substitui a convenção antiga de "texto de saída == original" — que era
      ambígua e interpretada de formas <b>inconsistentes</b> pelos consumidores (um
      tratava como falha, outro como 'sem alteração'). Também é a base para um retry
      seletivo: só {@link #FALHA_TRANSITORIA} vale repetir; resposta estruturalmente
      inválida ou tag corrompida não deve ser retentada.
      Tradução válida e diferente do original. /
      Google devolveu texto idêntico ao original — nada a corrigir. /
      HTTP transitório (408/429/5xx), timeout ou falha de rede — pode valer retry. /
      HTTP não transitório, JSON inesperado ou resposta sem segmentos traduzíveis. /
      Marcador de tag/quebra mutilado ou tag ASS perdida na volta da tradução. /

[PASTA] src/main/java/org/traducao/projeto/raspagemRevisao/application/
  - AuditorProblemasLegendaService.java
      Agrega detecção de resíduo em inglês, falas não traduzidas e erros de
      concordância PT-BR.
  - CorretorDeterministicoConcordanciaService.java
      PROPÓSITO DE NEGÓCIO: corrige localmente contradições linguísticas inequívocas
      antes de consultar um LLM, preservando tom, lore e restante da fala.
      
      <p>INVARIANTES DO DOMÍNIO: somente relações explícitas no original, expressões
      canônicas e incidentes já comprovados recebem substituição determinística;
      contexto ambíguo nunca é reescrito.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: entrada ausente ou regra não comprovada
      devolve {@link Optional#empty()} e mantém a tradução atual.
  - DetectorConcordanciaService.java
      PROPÓSITO DE NEGÓCIO: detecta erros objetivos de gênero e concordância que
      tornam uma legenda em português incoerente com a fala original.
      
      <p>INVARIANTES DO DOMÍNIO: somente evidências presentes na própria entrada
      podem gerar suspeita; primeira e segunda pessoas sem identificação do falante
      não autorizam inferência de gênero; tags ASS não interferem na análise.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: texto traduzido ausente é tratado como
      limpo por este detector e permanece sob responsabilidade dos validadores de
      tradução pendente.
  - LeitorCacheReferenciaService.java
      PROPÓSITO DE NEGÓCIO: entrega à Revisão de Legendas as referências EN/PT do
      cache produzido pela Tradução Local e atualizado pela Correção de Cache.
      
      <p>INVARIANTES DO DOMÍNIO: aceita o formato legado e o envelope versionado;
      a leitura é somente consulta e não remove proveniência nem campos futuros.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: arquivo inexistente devolve lista vazia;
      JSON inválido ou entrada incompatível lança {@link IOException} ao chamador.
  - ResultadoRevisaoLegendas.java
      PROPÓSITO DE NEGÓCIO: comunica ao painel o desfecho real da Opção 6, separando
      correções aplicadas de problemas que ainda exigem atenção.
      
      <p>INVARIANTES DO DOMÍNIO: pendências nunca produzem status de conclusão
      integral; contadores negativos são normalizados para zero.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: ausência de arquivos gera status
      {@code SEM_ARQUIVOS}; o record não lança exceções por contagem inválida.
  - RevisarCacheUseCase.java
      PROPÓSITO DE NEGÓCIO: revisa concordância, gênero e resíduos em traduções
      válidas já persistidas, usando a lore vinculada a cada arquivo da pasta cache.
      
      <p>INVARIANTES DO DOMÍNIO: entradas vazias/inválidas ficam para tradução ou
      contingência, não para revisão; uma pasta com vários animes nunca compartilha
      a mesma lore por engano; tags, karaokê e linhas gráficas são preservados;
      toda alteração possui backup, escrita atômica e auditoria.
  - RevisarLegendasUseCase.java
      (sem cabecalho explicativo)
  - SincronizadorLegendaCacheService.java
      PROPÓSITO DE NEGÓCIO: materializa no ASS/SSA as correções confirmadas pela
      Opção 5 antes de a Opção 6 iniciar sua auditoria linguística.
      
      <p>INVARIANTES DO DOMÍNIO: sincroniza somente por índice existente, somente
      tradução não vazia e nunca modifica cabeçalho, tempos, estilos ou linhas não
      dialogadas. Uma fala que regrediu exatamente ao original EN pode ser
      recuperada mesmo quando o timestamp do cache é anterior ao ASS.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: cache vazio devolve o documento original;
      sem autorização temporal, somente regressões exatas ao original são reparadas.

[PASTA] src/main/java/org/traducao/projeto/raspagemRevisao/domain/exceptions/
  - RaspagemRevisaoException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/raspagemRevisao/domain/
  - ResultadoDeteccaoConcordancia.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/raspagemRevisao/presentation/web/
  - RevisaoLegendasController.java
      PROPÓSITO DE NEGÓCIO: expõe à interface web a revisão das legendas traduzidas
      (.ass) — via Google Translate com auditoria e via LLM local para concordância
      PT-BR — usando cache e/ou legendas originais como referência.
      
      <p>INVARIANTES DO DOMÍNIO: usa a MESMA fila compartilhada via
      {@link PipelineWebSupport}; a pasta de entrada é obrigatória e validada; o modo
      de referência e a pasta de cache são resolvidos e validados antes de
      enfileirar; a revisão de concordância só prossegue com o LLM disponível;
      nenhuma URL, código HTTP ou nome de campo de DTO é alterado em relação ao
      controller monolítico original.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: entrada/cache/contexto inválido retorna
      HTTP 400; indisponibilidade do LLM e falhas do job aparecem no console SSE, sem
      derrubar a fila.

[PASTA] src/main/java/org/traducao/projeto/raspagemRevisao/
  - RevisorLegendasCLI.java
      Revisa arquivos .ass/.ssa já traduzidos, detecta resíduos em inglês e erros
      de concordância, e corrige via Google Translate.
  - RevisorRaspagemCLI.java
      Revisa falas já traduzidas no cache, corrigindo concordância de gênero,
      pronomes e adjetivos — erros comuns quando o LLM traduz literalmente do inglês.
      Ativado quando {@code app.modo=RASPAGEM_REVISAO}.

[PASTA] src/main/java/org/traducao/projeto/remuxer/application/
  - MapeadorMidiaService.java
      PROPÓSITO DE NEGÓCIO: pareia vídeos MKV e legendas finais de forma
      determinística, gerando nomes de saída limpos para a etapa de remux.
      
      <p>INVARIANTES DO DOMÍNIO: uma legenda não atende dois vídeos; episódio 01
      nunca casa por prefixo com 010; empates de mesma prioridade são reportados
      como ambíguos; destinos não colidem.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: pastas ilegíveis lançam
      {@link RemuxerException}; ausência ou ambiguidade vira aviso sem tarefa.
  - RemuxarLoteUseCase.java
      PROPÓSITO DE NEGÓCIO: orquestra o remux em lote, da validação das entradas à
      telemetria final, sem reencodar vídeo/áudio.
      
      <p>INVARIANTES DO DOMÍNIO: somente legenda textual válida chega ao mkvmerge;
      cada sucesso representa temporário validado e publicado; cancelamento é
      observado entre arquivos.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: o lote preserva sucessos anteriores,
      classifica falhas/pendências e sempre tenta registrar status final no dataset.

[PASTA] src/main/java/org/traducao/projeto/remuxer/domain/
  - MkvToolNixNaoEncontradoException.java
      (sem cabecalho explicativo)
  - PlanoRemux.java
      PROPÓSITO DE NEGÓCIO: representa o pareamento auditável entre vídeos e
      legendas antes de qualquer chamada ao mkvmerge.
      
      <p>INVARIANTES DO DOMÍNIO: cada legenda participa de no máximo uma tarefa;
      cada destino é único; ambiguidades e ausências nunca viram pareamentos
      silenciosos.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: problemas de conteúdo são devolvidos como
      avisos e contadores; falhas de leitura da pasta lançam {@link RemuxerException}.
  - RelatorioRemux.java
      PROPÓSITO DE NEGÓCIO: consolida o resultado real de um lote de remux para a
      interface, CLI e dataset de telemetria.
      
      <p>INVARIANTES DO DOMÍNIO: sucesso conta somente MKV validado e promovido ao
      nome final; ausência, ambiguidade e destino existente são pendências; falhas
      técnicas nunca resultam em status de sucesso.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: contadores preservam progresso parcial e o
      status final distingue falha, pendência, cancelamento e lote vazio.
  - RemuxerException.java
      (sem cabecalho explicativo)
  - RemuxTarefa.java
      (sem cabecalho explicativo)
  - SaidaRemuxJaExisteException.java
      PROPÓSITO DE NEGÓCIO: sinaliza que um MKV final já existe e deve ser
      preservado, impedindo sobrescrita ou remoção acidental.
      
      <p>INVARIANTES DO DOMÍNIO: é lançada antes de criar processo ou arquivo
      temporário para o remux atual.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: o caso de uso registra o item como pendente
      seguro e mantém o destino existente intacto.

[PASTA] src/main/java/org/traducao/projeto/remuxer/infrastructure/adapters/
  - MkvmergeAdapter.java
      PROPÓSITO DE NEGÓCIO: executa o mkvmerge sem reencodar, valida o container
      produzido e publica o MKV final sem arriscar um destino já existente.
      
      <p>INVARIANTES DO DOMÍNIO: mkvmerge escreve somente em temporário; o nome final
      nasce por move sem {@code REPLACE_EXISTING}; falha/cancelamento remove
      apenas o temporário desta execução.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: destino existente gera exceção específica;
      timeout, interrupção, saída inválida ou I/O geram {@link RemuxerException} e
      preservam qualquer MKV final anterior.

[PASTA] src/main/java/org/traducao/projeto/remuxer/infrastructure/config/
  - RemuxerProperties.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/remuxer/presentation/
  - RemuxerCLI.java
      PROPÓSITO DE NEGÓCIO: oferece execução local por terminal da mesma etapa de
      remux usada na interface web.
      
      <p>INVARIANTES DO DOMÍNIO: valida pastas antes do lote e imprime o status real
      consolidado, sem anunciar sucesso quando existem pendências ou falhas.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: configuração/pasta inválida encerra sem
      criar saída; falhas do lote permanecem no relatório final.

[PASTA] src/main/java/org/traducao/projeto/remuxer/presentation/ui/
  - ConsoleRemuxerLogger.java
      Tag colorida em negrito (chama atenção), corpo da mensagem em peso normal
      (mais fácil de ler em blocos de texto maiores) — INFO/DEBUG ficam sem cor.
      Exemplo: [10:20:30] [INFO   ] Mensagem...

[PASTA] src/main/java/org/traducao/projeto/remuxer/presentation/web/
  - RemuxerController.java
      PROPÓSITO DE NEGÓCIO: expõe o Remuxer (mkvmerge) à interface web, agendando um
      único lote de remux que combina vídeos e legendas traduzidas com política
      explícita para as legendas originais.
      
      <p>Fronteira arquitetural: este endpoint pertence ao módulo {@code remuxer}
      (Opção 12) e reside na sua camada de apresentação própria. Não importa nenhuma
      regra funcional da Tradução Local (Opção 4): usa o use case do próprio módulo e
      a fila técnica neutra {@code core}. As dependências {@link PipelineWebSupport},
      {@link RespostaPadrao}, {@link RemuxRequest} e {@link AnsiCores} são <b>glue
      técnico de apresentação</b> (fila única, transporte HTTP, cores de console)
      hoje em {@code traducao.presentation}; é dívida técnica temporária reservada
      para saneamento na FASE E — não é acoplamento funcional.
      
      <p>INVARIANTES DO DOMÍNIO: usa a MESMA fila compartilhada via
      {@link PipelineWebSupport} e consulta a MESMA {@link FilaExecucaoPipeline} para
      recusar concorrência; as pastas existem antes da aceitação; o offset fica na
      faixa operacional de ±86.400.000 ms; a rota {@code POST /api/remuxar}, os
  - RemuxRequest.java
      PROPÓSITO DE NEGÓCIO: transporta as opções exclusivas do Remuxer.
      INVARIANTES DO DOMÍNIO: pasta de vídeo é obrigatória; offset e política de
      faixas são validados pelo endpoint.
      COMPORTAMENTO EM CASO DE FALHA: campos ausentes recebem fallback seguro ou
      geram HTTP 400 antes de entrar na fila.

[PASTA] src/main/java/org/traducao/projeto/renomearArquivos/application/
  - OperacaoRenomeacaoEmAndamentoException.java
      PROPÓSITO DE NEGÓCIO: impede duas operações de renomeação concorrentes na
      mesma pasta de mídia, evitando corridas e manifestos inconsistentes.
      
      <p>INVARIANTES DO DOMÍNIO: uma pasta normalizada admite no máximo uma
      simulação, aplicação ou reversão por vez.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: é lançada antes de qualquer alteração em
      disco e convertida pelo controller em HTTP 409.
  - RenomeadorUseCase.java
      PROPÓSITO DE NEGÓCIO: padroniza nomes de vídeos e legendas de uma pasta local,

[PASTA] src/main/java/org/traducao/projeto/renomearArquivos/domain/
  - OperacaoRenomeacao.java
      (sem cabecalho explicativo)
  - ResultadoRenomeacao.java
      PROPÓSITO DE NEGÓCIO: representa o resultado verificável de uma simulação,
      aplicação ou reversão de nomes para que a interface exiba o estado real.
      
      <p>INVARIANTES DO DOMÍNIO: contadores nunca são negativos; {@code itens}
      contém somente mapeamentos pertencentes à pasta processada; o status não
      pode anunciar sucesso quando existem falhas ou pendências.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: operações recusadas antes da execução são
      respondidas pelo controller como erro HTTP; falhas durante um lote retornam
      status {@code CONCLUIDO_COM_FALHAS} e preservam o manifesto de reversão.

[PASTA] src/main/java/org/traducao/projeto/renomearArquivos/presentation/web/
  - RenomearArquivosController.java
      PROPÓSITO DE NEGÓCIO: expõe simulação, aplicação e reversão da opção 13 com
      resposta somente depois que o status real da operação estiver disponível.
      
      <p>INVARIANTES DO DOMÍNIO: entradas inválidas retornam 400, concorrência na
      mesma pasta retorna 409 e nenhuma resposta antecipada anuncia falso sucesso.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: erros esperados viram JSON didático; falhas
      inesperadas são registradas e retornam HTTP 500 sem expor stack trace.
  - RenomearArquivosRequest.java
      PROPÓSITO DE NEGÓCIO: transporta pasta, nome base e temporada escolhidos no
      painel da opção 13.
      
      <p>INVARIANTES DO DOMÍNIO: validação efetiva permanece no backend; temporada
      nula permite inferência pelo nome e compatibilidade com clientes antigos.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: campos ausentes são recusados ou recebem
      fallback seguro pelo caso de uso, nunca usados diretamente em movimentação.

[PASTA] src/main/java/org/traducao/projeto/revisaoLore/application/
  - DetectorTermosLoreService.java
      PROPÓSITO DE NEGÓCIO: prioriza falas com possível erro terminológico antes
      de chamar o LLM, respeitando a lore específica da obra selecionada.
      <p>INVARIANTES DO DOMÍNIO: nomes canônicos, equivalências PT-BR autorizadas
      e termos oficiais preservados não podem virar falsos resíduos em inglês.
      <p>COMPORTAMENTO EM CASO DE FALHA: entradas insuficientes retornam resultado
      limpo; suspeitas são somente sinalizadas e nunca modificam a legenda.
  - GerenciadorPromptRevisaoLore.java
      (sem cabecalho explicativo)
  - PromptRevisaoLore.java
      PROPÓSITO DE NEGÓCIO: monta os prompts de revisão terminológica e mantém a
      lore da obra separável das instruções operacionais.
      <p>INVARIANTES DO DOMÍNIO: a fonte canônica recebida integra o prompt sem
      alteração e pode ser recuperada pelos delimitadores estáveis da classe.
      <p>COMPORTAMENTO EM CASO DE FALHA: lore ausente usa marcador explícito e a
      extração de prompt inválido devolve texto vazio.
  - RevisarLoreUseCase.java
      (sem cabecalho explicativo)
  - ValidadorCandidatoLoreService.java
      PROPÓSITO DE NEGÓCIO: impede que a revisão de lore use uma suspeita
      terminológica como autorização para retraduzir ou reescrever toda a fala.
      
      <p>INVARIANTES DO DOMÍNIO: uma alteração automática deve ser pequena e o
      trecho canônico introduzido precisa existir tanto no original inglês quanto
      na lore ativa; texto comum fora desse recorte permanece intocado.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: devolve o motivo da rejeição e o chamador
      mantém integralmente a legenda PT-BR anterior.

[PASTA] src/main/java/org/traducao/projeto/revisaoLore/contexto/
  - ContextoRevisaoLore86.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreDanMachi.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreDanMachiS4.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreDanMachiS5.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreGuiltyCrown.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreGundam0080.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreGundam0083.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreGundam08thMSTeam.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreGundamCCA.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreGundamNT.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreGundamUnicorn.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreGundamZeta.java
      (sem cabecalho explicativo)
  - ContextoRevisaoLoreGundamZZ.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/revisaoLore/domain/
  - EntradaAuditoriaRevisaoLore.java
      Registro granular, append-only, de cada fala enviada ao LLM na revisão de lore.
  - LogEventoRevisaoLore.java
      Entrada estruturada do log de sessao da revisao de lore (serializavel em JSON).
  - ResultadoDeteccaoLore.java
      (sem cabecalho explicativo)
  - ResultadoRevisaoLore.java
      PROPÓSITO DE NEGÓCIO: entrega ao controller o desfecho completo de uma
      revisão de lore para banner, logs e decisões operacionais.
      <p>INVARIANTES DO DOMÍNIO: status e contadores descrevem a mesma sessão;
      pendentes incluem respostas ausentes, propostas descartadas e falas que
      precisam voltar à revisão linguística da Opção 6.
      <p>COMPORTAMENTO EM CASO DE FALHA: o record é imutável; falhas totais são
      comunicadas por exceção antes de sua criação.
  - RevisaoLoreRelatorioJson.java
      PROPÓSITO DE NEGÓCIO: persiste o dataset completo da revisão de lore com
      contexto, métricas, erros e eventos granulares.
      <p>INVARIANTES DO DOMÍNIO: todos os blocos pertencem à mesma sessão e o
      status resume os contadores persistidos.
      <p>COMPORTAMENTO EM CASO DE FALHA: é imutável; a infraestrutura decide como
      registrar impossibilidade de escrita.
  - StatusRevisaoLore.java
      PROPÓSITO DE NEGÓCIO: distingue o desfecho real de uma execução de revisão de
      lore, substituindo o antigo "[SUCESSO]" incondicional. Permite ao operador
      saber, num relance no console/relatório, se o job realmente concluiu, se
      concluiu deixando pendências, se foi cancelado, se falhou ou se nem havia
      arquivos para processar.
      
      <p>INVARIANTES DO DOMÍNIO: exatamente um status descreve cada execução. Só
      {@link #FALHOU} pode acompanhar uma exceção propagada; os demais representam
      retornos normais do use case. {@link #CONCLUIDO} exige zero erros e zero falas
      pendentes.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: é um enum imutável; não dispara exceções.
      O rótulo textual nunca é nulo.
      PROPÓSITO DE NEGÓCIO: associa cada estado técnico a um rótulo humano.
      <p>INVARIANTES DO DOMÍNIO: todo status possui rótulo não nulo.
      <p>COMPORTAMENTO EM CASO DE FALHA: construção ocorre apenas pelas
      constantes declaradas no enum.
      PROPÓSITO DE NEGÓCIO: fornece o texto exibido nos banners e relatórios.
      <p>INVARIANTES DO DOMÍNIO: retorna sempre o rótulo da própria constante.
      <p>COMPORTAMENTO EM CASO DE FALHA: nunca retorna nulo.
  - StatusRevisaoLoreLlm.java
      PROPÓSITO DE NEGÓCIO: representa o resultado da verificação de disponibilidade
      do servidor LLM local usado exclusivamente pela Revisão de Lore, antes de
      iniciar uma sessão. Permite abortar cedo, com mensagem clara, quando o modelo
      não está carregado — em vez de descobrir isso só no meio da revisão.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>{@code modeloCarregado == true} implica {@code servidorOnline == true}.</li>
      <li>{@code mensagem} descreve o estado de forma legível ao operador.</li>
      <li>Tipo próprio da fatia Revisão de Lore — não reutiliza o status da Tradução Local.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Servidor inacessível é representado por {@code servidorOnline == false} e
      {@code modeloCarregado == false}, com a causa técnica na {@code mensagem}.

[PASTA] src/main/java/org/traducao/projeto/revisaoLore/domain/exceptions/
  - RevisaoLoreException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/revisaoLore/domain/ports/
  - ProvedorPromptRevisaoLore.java
      (sem cabecalho explicativo)
  - RevisorLoreLlmPort.java
      PROPÓSITO DE NEGÓCIO: porta LLM própria da Revisão de Lore. Abstrai a única
      interação com o modelo local de que a fatia precisa — verificar disponibilidade
      e revisar terminologia/nomes de lore de uma fala já traduzida —, mantendo a
      Revisão de Lore independente da stack LLM da Tradução Local.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>{@link #revisar} preserva integralmente os marcadores estruturais
      {@code [[TAGn]]} presentes na tradução; resposta que perca algum marcador
      nunca é publicada.</li>
      <li>A revisão usa o prompt de sistema de lore fornecido pelo chamador e o
      prompt de usuário próprio da fatia — nenhuma responsabilidade de tradução
      de lotes ou correção gramatical pertence a esta porta.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      {@link #revisar} devolve {@link Optional#empty()} quando o LLM falha, a resposta
      é inválida ou nenhuma linha preserva os marcadores — cabendo ao caso de uso
      manter a tradução atual. {@link #verificarDisponibilidade} nunca lança: reporta
      indisponibilidade via {@link StatusRevisaoLoreLlm}.

[PASTA] src/main/java/org/traducao/projeto/revisaoLore/infrastructure/adapters/
  - NormalizadorRespostaRevisaoLore.java
      PROPÓSITO DE NEGÓCIO: extrai a fala revisada final das respostas do LLM de
      lore, que podem vir com raciocínio ({@code <think>}), cerca Markdown ou um
      rótulo antes do texto. Responsabilidade separada do adapter, própria da
      Revisão de Lore — não reutiliza o normalizador da Tradução Local.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Quando a tradução possui marcadores {@code [[TAGn]]}, apenas uma linha que
      preserve TODOS eles pode ser escolhida; explicações nunca são concatenadas
      ao texto da legenda.</li>
      <li>A lista de marcadores preserva ordem e elimina duplicações.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Sem linha utilizável que preserve os marcadores esperados, devolve texto vazio,
      permitindo nova tentativa sem publicar conteúdo estruturalmente incompleto.
  - RevisorLoreLlmAdapter.java
      PROPÓSITO DE NEGÓCIO: adapter LLM próprio da Revisão de Lore. Implementa a
      única interação com o modelo local de que a fatia precisa — checar
      disponibilidade e revisar a terminologia de lore de uma fala —, replicando o
      comportamento efetivo anterior (antes exposto por {@code LlmPort.revisarLore})
      sem depender da stack LLM da Tradução Local.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>A revisão usa o prompt de sistema de lore recebido e o prompt de usuário
      de {@link PromptRevisaoLore}, com temperatura fixa de revisão (0.15).</li>
      <li>No máximo {@value #MAX_TENTATIVAS_REVISAO} tentativas; erro HTTP permanente
      (4xx exceto 408/429) não é repetido.</li>
      <li>Só publica uma linha que preserve todos os marcadores {@code [[TAGn]]}.</li>
      <li>Nenhuma responsabilidade de tradução de lotes ou correção gramatical vive aqui.</li>
      </ul>

[PASTA] src/main/java/org/traducao/projeto/revisaoLore/infrastructure/config/
  - RevisaoLoreLlmProperties.java
      PROPÓSITO DE NEGÓCIO: configuração própria do cliente LLM da Revisão de Lore,
      sob o namespace {@code revisao-lore.llm}, independente do namespace
      {@code tradutor.llm} da Tradução Local. Os defaults reproduzem o comportamento
      efetivo atual (mesmos base-url, model "current", max-tokens, timeouts).
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Namespace exclusivo {@code revisao-lore.llm}; nunca reutiliza {@code tradutor.llm}.</li>
      <li>{@code model} pode ser resolvido em runtime para o modelo efetivamente
      carregado (ver {@code verificarDisponibilidade} do adapter), como no fluxo atual.</li>
      <li>{@code pausaEntreTentativas} preserva o equivalente operacional de 2s entre
      tentativas de revisão.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Valores ausentes ou inválidos caem para os defaults equivalentes ao efetivo
      atual, garantindo timeouts e modelo estáveis mesmo sem configuração explícita.

[PASTA] src/main/java/org/traducao/projeto/revisaoLore/infrastructure/dtos/
  - RevisaoLoreLlmDtos.java
      PROPÓSITO DE NEGÓCIO: DTOs próprios da Revisão de Lore para o protocolo
      OpenAI-compatible do LLM local (chat/completions e catálogo de modelos).
      Duplicação consciente dos records equivalentes da Tradução Local, para manter
      a fatia autônoma — nenhuma dependência de {@code RecordsLlm}.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Campos desconhecidos são ignorados na desserialização ({@code ignoreUnknown = true}).</li>
      <li>{@code ModeloDisponivelV0} carrega o {@code state} da API estendida da LM Studio.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Estruturas ausentes desserializam como {@code null}, tratado pelo adapter como
      resposta inválida.

[PASTA] src/main/java/org/traducao/projeto/revisaoLore/infrastructure/http/
  - RevisaoLoreHttpClient.java
      PROPÓSITO DE NEGÓCIO: cliente HTTP JSON próprio da Revisão de Lore, baseado no
      {@link HttpClient} do JDK. Cobre exatamente o necessário para falar com o LLM
      local: {@code GET} relativo, {@code GET} absoluto (API estendida da LM Studio)
      e {@code POST} JSON. Duplicação técnica consciente — não depende do cliente
      HTTP da Tradução Local.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>{@code connect-timeout} e {@code read-timeout} próprios da fatia são
      aplicados a cada requisição.</li>
      <li>Respostas com status {@code >= 400} viram {@link HttpClientException}
      preservando o código HTTP para a política de retry do adapter.</li>
      <li>A interrupção da thread é propagada: {@code send} do JDK lança
      {@link InterruptedException}, repassada ao chamador sem ser engolida aqui.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Erros de rede/timeout propagam a exceção original de {@link HttpClient}; erros
      HTTP viram {@link HttpClientException}. Nenhum estado é mantido entre chamadas.

[PASTA] src/main/java/org/traducao/projeto/revisaoLore/infrastructure/
  - RevisaoLoreAuditoriaCache.java
      Cache append-only para mineração posterior das decisões da revisão de lore.
  - RevisaoLoreLogPersistencia.java
      Persiste relatorio e log de sessao da revisao de lore exclusivamente em JSON.

[PASTA] src/main/java/org/traducao/projeto/revisaoLore/presentation/
  - RevisaoLoreController.java
      PROPÓSITO DE NEGÓCIO: expõe a Revisão de Lore à interface local, enfileira o
      trabalho com segurança e apresenta o desfecho real no console.
      <p>INVARIANTES DO DOMÍNIO: uma revisão sempre usa contexto conhecido e a fila
      única do pipeline; o banner reflete o status retornado pelo caso de uso.
      <p>COMPORTAMENTO EM CASO DE FALHA: entrada inválida retorna HTTP 400; falha
      assíncrona é registrada com banner vermelho e preserva a fila.

[PASTA] src/main/java/org/traducao/projeto/sistema/application/
  - EncerrarAplicacaoUseCase.java
      Encerra a aplicação de forma ordenada a partir do botão "Sair" da UI.
      <p>
      Sequência: sinaliza parada cooperativa da fila do pipeline (o job em
      execução encerra no próximo ponto seguro, preservando cache e arquivos já
      concluídos), espera um curto período para a resposta HTTP chegar ao
      navegador e então derruba o Quarkus. Se o shutdown normal não terminar o
      processo (ex.: modo dev segura a JVM viva), um fallback força a saída.

[PASTA] src/main/java/org/traducao/projeto/sistema/presentation/
  - SistemaController.java
      Endpoints de controle do processo da aplicação (menu "Sair" da UI).
      Operações de trabalho do pipeline ficam nos controllers de cada módulo;
      aqui entra apenas o ciclo de vida do servidor em si.

[PASTA] src/main/java/org/traducao/projeto/telemetria/
  - AmbienteExecucaoDataset.java
      PROPÓSITO DE NEGÓCIO: representa uma fotografia sanitizada e coerente do
      hardware da máquina que gerou o snapshot público de telemetria.
      
      <p>INVARIANTES DO DOMÍNIO: todos os componentes pertencem à mesma máquina e
      são detectados automaticamente; não inclui usuário, hostname, IP, serial,
      MAC, caminhos ou identificadores de hardware.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: campos indisponíveis ficam nulos e a lista
      de GPUs fica vazia, sem recorrer a valores manuais de outra máquina.
  - AmbienteExecucaoDatasetService.java
      PROPÓSITO DE NEGÓCIO: detecta metadados publicáveis do computador que está
      gerando o dataset para que benchmarks não misturem hardware de máquinas.
      
      <p>INVARIANTES DO DOMÍNIO: CPU, GPUs e RAM vêm da mesma coleta local; valores
      manuais nunca substituem a detecção; em sistemas híbridos, uma GPU dedicada
      é priorizada como principal e todas as GPUs são preservadas na lista.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: tenta um fallback seguro da JVM e deixa
      campos não detectáveis vazios, sem reutilizar configuração de outro host.
  - LlmTelemetria.java
      Compat: construtor antigo (sem lore/status) para chamadas legadas — assume
      lore desconhecido e status CONCLUIDO. Novos registros usam o construtor
      completo para carregar a proveniência (lore) e o desfecho na telemetria.
  - MidiaTelemetria.java
      (sem cabecalho explicativo)
  - OperacaoHistorico.java
      Uma linha da tabela de histórico de operações exibida no painel de Telemetria.
  - OperacaoTelemetria.java
      Registro persistido de operações do pipeline que não passam pelo LLM de tradução
      (revisão de legendas, correção Google, limpeza de cache, etc.).
  - RevisaoLoreTelemetriaResumo.java
      Métricas agregadas das sessões de Revisão de Lore para o painel de Telemetria.
  - TelemetriaDatasetProperties.java
      PROPÓSITO DE NEGÓCIO: configura a publicação do dataset público e a coleta
      sanitizada do hardware local que contextualiza os benchmarks.
      
      <p>INVARIANTES DO DOMÍNIO: hardware publicado é sempre detectado na máquina
      atual; não existe override manual de CPU, GPU ou RAM capaz de misturar hosts.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: propriedades ausentes usam padrões seguros;
      a detecção pode cair para dados limitados da JVM, sem inventar componentes.
  - TelemetriaDatasetService.java
      PROPÓSITO DE NEGÓCIO: publica a telemetria acumulada como dataset público num repositório Git
      DEDICADO ({@code kronos-anime-translation-telemetry-dataset}, seguindo a
      convenção {@code [NomeDoSistema]-telemetry-dataset} para dados de pesquisa/ML).
      <p>
      O serviço é auto-suficiente: se o repositório local não existir, ele clona o
      remoto configurado (ou inicializa um novo e associa o remoto); na primeira
      publicação gera README com declaração de anonimização (LGPD/GDPR), LICENSE e
      a estrutura {@code metrics/}. Cada publicação = 1 commit + push, e o
      histórico Git é o versionamento natural dos snapshots.
      <p>
      <p>INVARIANTES DO DOMÍNIO: a sanitização deliberada mantém
      carrega apenas MÉTRICAS: nada de textos de legenda (os avisos viram
      contagem), nada de caminhos de máquina (o campo {@code detalhe} das
      operações é descartado e nomes de episódio perdem qualquer diretório); o
      ambiente de hardware pertence integralmente à máquina publicadora.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: erros de geração, Git ou rede interrompem a
      publicação com {@link IOException}, preservando o snapshot anterior.
  - TelemetriaResumo.java
      Resumo serializável da telemetria acumulada na sessão atual do servidor,
      consumido pelo painel "Telemetria" da interface web.
  - TelemetriaService.java
      (sem cabecalho explicativo)
  - TelemetriaTraducaoLeitura.java
      PROPÓSITO DE NEGÓCIO: DTO de LEITURA próprio do módulo de telemetria para
      interpretar o arquivo canônico da Tradução Local ({@code telemetria_traducao.json}).
      Permite ao Painel Unificado consolidar a telemetria da tradução como agregador
      CQRS read-only, SEM importar as classes de domínio do pacote {@code traducao} — o
      contrato entre os módulos é exclusivamente o JSON no filesystem.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Espelha o schema do arquivo por estrutura, ignorando campos desconhecidos,
      para tolerar evolução do {@code schemaVersion} sem quebrar a leitura.</li>
      <li>É estritamente de leitura: o módulo de telemetria nunca escreve este arquivo.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Estruturas ausentes desserializam como {@code null}/vazias; o agregador trata um
      documento ausente, vazio ou ilegível como conjunto vazio, sem destruir o arquivo.

[PASTA] src/main/java/org/traducao/projeto/telemetria/presentation/web/
  - TelemetriaController.java
      PROPÓSITO DE NEGÓCIO: expõe à interface web a telemetria acumulada do pipeline
      — resumo consolidado para o painel, exportação segura do arquivo para download
      e a publicação do dataset público sanitizado no repositório Git dedicado.
      
      <p>INVARIANTES DO DOMÍNIO: nenhuma URL, código HTTP ou nome de campo de DTO é
      alterado em relação ao controller monolítico original; a pasta de cache é lida
      diretamente da configuração {@code tradutor.diretorio-cache} (mesma chave e
      default {@code cache} usados antes por {@code TradutorProperties.diretorioCache()},
      preservando o fallback local para valor nulo/em branco); a exportação usa o
      arquivo canônico e a publicação delega ao serviço de dataset já sanitizado.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: exportação sem arquivo retorna 404 e falha
      de leitura retorna 500; falha na publicação do dataset retorna 500 com a
      mensagem do erro no corpo padrão.
  - TelemetriaStreamResource.java
      PROPÓSITO DE NEGÓCIO: canal Server-Sent Events (SSE) reativo que transmite a
      telemetria acumulada da KRONOS ao painel web em tempo real, conforme os
      episódios são processados.
      
      <p>Fronteira arquitetural: pertence ao módulo {@code telemetria}, dono da
      funcionalidade, e reside na sua camada de apresentação própria. Depende apenas
      do {@link TelemetriaService} do próprio módulo — sem qualquer dependência
      funcional da Tradução Local (Opção 4).
      
      <p>INVARIANTES DO DOMÍNIO: a rota {@code GET /api/telemetria/stream} e o tipo
      {@code text/event-stream} são contrato público preservado exatamente como antes
      da movimentação; a rota é distinta das rotas Spring MVC para evitar colisão de
      endpoints (JAX-RS/SSE nativo do Quarkus).
      
      <p>COMPORTAMENTO EM CASO DE FALHA: o registro do {@code SseEventSink} é delegado
      ao serviço de telemetria; o encerramento/erro da conexão é gerido pelo runtime
      SSE, sem afetar o processamento em andamento.

[PASTA] src/main/java/org/traducao/projeto/traducaoCorrige/application/
  - ClassificadorEntradaCacheService.java
      PROPÓSITO DE NEGÓCIO: aplica ao menu Correção do Cache a mesma decisão de
      validade usada pela Tradução Local, distinguindo falha real de nome, sigla,
      número, termo de lore, karaokê ou efeito que deve permanecer intocado.
      
      <p>INVARIANTES DO DOMÍNIO: entrada protegida nunca é enviada ao Google/LLM;
      tradução idêntica autorizada pela lore é válida; vazio, fallback não
      autorizado e resposta rejeitada pelo validador são candidatos à correção.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: campos ausentes são classificados como
      {@code IGNORADA}; exceções do validador viram {@code INVALIDA} com motivo.
  - ContextoManutencaoCacheService.java
      PROPÓSITO DE NEGÓCIO: garante que cada arquivo da pasta cache seja analisado
      com a lore da obra que realmente o originou, mesmo quando a raiz contém
      caches de vários animes.
      
      <p>INVARIANTES DO DOMÍNIO: a proveniência versionada tem prioridade; contexto
      manual serve somente como fallback para cache legado; contexto desconhecido
      nunca cai silenciosamente no padrão.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link IllegalArgumentException} e o
      arquivo é contabilizado como falha sem ser modificado.
  - LimparCacheUseCase.java
      PROPÓSITO DE NEGÓCIO: limpa do banco persistente apenas traduções comprovadas
      como fallback ou inválidas, deixando-as vazias para serem refeitas pela
      Tradução Local sem apagar nomes e termos legitimamente preservados pela lore.
      
      <p>INVARIANTES DO DOMÍNIO: cache versionado/legado é preservado; linhas
      protegidas não mudam; cada arquivo alterado recebe backup e escrita atômica;
      cache vazio já representa trabalho pendente e não é regravado.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: a falha é contabilizada e auditada por
      arquivo, o original permanece no disco e o lote termina com status
      {@code CONCLUIDO_COM_FALHAS}.

[PASTA] src/main/java/org/traducao/projeto/traducaoCorrige/
  - CorretorCacheCLI.java
      CommandLineRunner que realiza a limpeza do cache de tradução integrado ao fluxo do Spring.
      Ativado quando a propriedade app.modo é configurada como "CORRIGIR_CACHE".

[PASTA] src/main/java/org/traducao/projeto/traducaoCorrige/domain/
  - EntradaAuditoriaCorrecaoCache.java
      PROPÓSITO DE NEGÓCIO: registra cada decisão que alterou ou tentou reparar uma
      tradução persistida, formando dataset auditável para descobrir falhas e
      aperfeiçoar o pipeline.
      
      <p>INVARIANTES DO DOMÍNIO: o registro é append-only e contém antes/depois,
      operação, resultado, motivo, lore e arquivo de origem.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: record imutável; a infraestrutura de
      persistência registra warning sem interromper a correção principal.
  - ResultadoManutencaoCache.java
      PROPÓSITO DE NEGÓCIO: resume de forma verificável o resultado de uma operação
      sobre a pasta de cache para que console, API, relatório e telemetria não
      anunciem sucesso quando arquivos falharam.
      
      <p>INVARIANTES DO DOMÍNIO: contadores nunca são negativos; uma execução com
      falhas ou pendências não possuem status {@code CONCLUIDO}; cancelamento tem
      precedência sobre os demais estados.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: record imutável; entradas negativas são
      normalizadas para zero pelo construtor compacto.

[PASTA] src/main/java/org/traducao/projeto/traducaoCorrige/domain/exceptions/
  - CorretorCacheException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducaoCorrige/infrastructure/
  - CorrecaoCacheAuditoria.java
      PROPÓSITO DE NEGÓCIO: persiste em JSONL o histórico granular do menu Correção
      do Cache para auditoria, recuperação e uso como dataset de melhoria.
      
      <p>INVARIANTES DO DOMÍNIO: arquivo canônico fica no projeto, em
      {@code cache/auditoria}; registros existentes nunca são reescritos.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: emite warning e não derruba a operação que
      já preserva o cache por backup e escrita atômica.

[PASTA] src/main/java/org/traducao/projeto/traducaoCorrige/presentation/web/
  - CorrecaoCacheController.java
      PROPÓSITO DE NEGÓCIO: expõe à interface web os três modos de manutenção do
      banco de cache de tradução — limpeza/auditoria local, preenchimento online de
      lacunas via Google Translate e revisão gramatical via LLM local.
      
      <p>INVARIANTES DO DOMÍNIO: usa a MESMA fila compartilhada via
      {@link PipelineWebSupport}; o contexto informado, quando presente, é validado
      antes de enfileirar; a revisão via LLM só prossegue com modelo carregado;
      nenhuma URL, código HTTP ou nome de campo de DTO é alterado em relação ao
      controller monolítico original.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: caminho de cache ou contexto inválido
      retorna HTTP 400; indisponibilidade do LLM e falhas do job aparecem no console
      SSE, sem derrubar a fila.

[PASTA] src/main/java/org/traducao/projeto/traducaoKaraoke/application/
  - ClassificadorLetraKaraokeService.java
      Decide o destino de cada evento de música: preservar (letra original),
      traduzir (camada em inglês) ou não tocar (efeito KFX / já em PT-BR).
      <p>
      O problema central que este classificador resolve: cantores japoneses
      misturam inglês no meio da letra ("kimi no heart ni fly away"). A heurística
      estrita de romaji do {@link DetectorEfeitoKaraokeService} exige que TODAS as
      palavras sejam silabáveis em japonês — uma única palavra inglesa derruba a
      detecção e a letra original iria ao LLM. Aqui a decisão é por EVIDÊNCIA:
      partículas/palavras japonesas romanizadas inequívocas votam em "original",
      palavras gramaticais inequívocas de inglês votam em "tradução", e o estilo
      do evento (Romaji/JP vs English) decide antes de qualquer análise de texto.
      Em caso de dúvida o viés é PRESERVAR — o mesmo princípio de todo o projeto:
      deixar uma linha sem traduzir custa menos que destruir a letra original.
  - TraduzirKaraokeUseCase.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducaoKaraoke/domain/
  - ClasseLinhaKaraoke.java
      Classificação de cada evento da legenda sob a ótica da tradução de letras
      de música. O ponto delicado é a letra japonesa romanizada com palavras em
      inglês misturadas (comum em música japonesa: "kimi no heart ni fly away") —
      ela é ORIGINAL_JAPONES e nunca pode ir ao LLM, enquanto a camada de
      TRADUÇÃO em inglês da mesma música é TRADUZIVEL_INGLES.
      Não é música: diálogo, placa, Comment. Copiada byte a byte. /
      Efeito KFX (sílaba/frame, alta densidade de tags). Preservado intacto. /
      Letra original (kana/kanji/romaji, mesmo com inglês misturado). Nunca traduz. /
      Letra que já está em português. Nada a fazer. /
      Camada de tradução em inglês da música: é o que este módulo traduz. /
  - ResultadoTraducaoKaraoke.java
      Resumo, por arquivo .ass, do que a tradução de karaokê classificou e fez.
      Alimenta o console da UI, o manifesto de auditoria e a telemetria.
  - TraducaoKaraokeException.java
      Erro de negócio do módulo Tradução de Karaokê (validação de pastas,
      LLM indisponível, falha de leitura/escrita das legendas).

[PASTA] src/main/java/org/traducao/projeto/traducaoKaraoke/infrastructure/
  - TraducaoKaraokePersistencia.java
      Manifesto de auditoria da tradução de karaokê: registra, por execução, o
      que foi preservado/traduzido em cada arquivo. Fica em
      {@code logs/traducao-karaoke/manifestos} — junto com os originais intocados
      na pasta de origem e o cache JSON editável, é a trilha completa para
      auditar (ou refazer) qualquer tradução de letra.

[PASTA] src/main/java/org/traducao/projeto/traducaoKaraoke/presentation/
  - TraducaoKaraokeController.java
      Endpoints do módulo Tradução de Karaokê. A simulação só lê arquivos e roda
      async fora da fila (mesmo padrão do Karaokê Simples); a APLICAÇÃO chama o
      LLM e muda o contexto de lore ativo — estado global —, então
      obrigatoriamente entra na {@link FilaExecucaoPipeline}.
  - TraducaoKaraokeRequest.java
      Corpo das requisições do painel Tradução de Karaokê: a pasta com as
      legendas .ass e a obra (contexto de lore) selecionada na UI.

[PASTA] src/main/java/org/traducao/projeto/traducao/application/
  - ProcessarArquivoUseCase.java
      (sem cabecalho explicativo)
  - ProcessarEpisodioUseCase.java
      Quantas tentativas extras (alem da primeira) sao feitas numa fala isolada
      (lote de tamanho 1) antes de desistir e manter o texto original sem traducao.
      Temperatura por tentativa numa fala isolada: null = a configurada.
      Repetir a mesma requisicao com a mesma temperatura tende a reproduzir a
      mesma alucinacao; subir a temperatura muda a amostragem e da chance real
      de recuperacao antes de desistir da fala.

[PASTA] src/main/java/org/traducao/projeto/traducao/domain/exceptions/
  - DivergenciaLinhasException.java
      (sem cabecalho explicativo)
  - EntradaJaTraduzidaException.java
      PROPÓSITO DE NEGÓCIO: Sinaliza que a entrada aparenta já estar em PT-BR e a
      retradução não foi confirmada — o arquivo é bloqueado para não retraduzir e
      sobrescrever trabalho bom. É regra específica do fluxo de tradução e por isso
      permanece na fatia {@code traducao}.
      
      <p>INVARIANTES DO DOMÍNIO: só lançada quando a heurística de caminho já
      traduzido dispara e {@code permitirRetraducao} é falso.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: é a própria sinalização; herda de
      {@link ArquivoLegendaException} (módulo {@code legenda}) para o lote registrar o
      arquivo como BLOQUEADO e seguir para o próximo; a captura específica em
      {@code TraducaoController} permanece válida.
  - LlmFalhaComunicacaoException.java
      (sem cabecalho explicativo)
  - LmStudioOfflineException.java
      (sem cabecalho explicativo)
  - RespostaLlmVaziaException.java
      (sem cabecalho explicativo)
  - TraducaoParcialException.java
      Construtor usado pela camada do Episódio (nível de Lotes)
      Construtor usado pela camada de Arquivo (nível de Falas Mascaradas)
  - TradutorException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/domain/
  - NormalizadorNomeEpisodio.java
      PROPÓSITO DE NEGÓCIO: proprietário ÚNICO, dentro da Tradução Local, da
      normalização do nome de episódio usada como chave de deduplicação da telemetria
      própria. Garante que o mesmo episódio — apesar de variações inócuas de caixa,
      espaços, forma Unicode ou diretório — projete uma única entrada consolidada.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Considera apenas o nome do arquivo (descarta diretórios), aparando e
      colapsando espaços, normalizando Unicode para NFC e reduzindo a caixa.</li>
      <li>É CONSERVADORA: mantém a extensão e os números intactos — {@code ep1} e
      {@code ep11}, ou {@code .ass} e {@code .srt}, permanecem distintos.</li>
      <li>Determinística e idempotente: {@code normalizar(normalizar(x)) == normalizar(x)}.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Entrada {@code null} ou em branco devolve string vazia, chave estável para
      registros sem nome.
  - ResultadoTraducaoArquivo.java
      PROPÓSITO DE NEGÓCIO: Resultado por arquivo da tradução — o que a tabela da UI
      mostra (Arquivo | Lore | Falas | Cache | Traduzidas | Avisos | Status) e o que
      consolida o status do lote. Substitui o retorno "só o Path", que escondia se o
      arquivo concluiu, falhou ou foi bloqueado.
      
      <p>INVARIANTES DO DOMÍNIO: {@code arquivo} e {@code status} nunca nulos;
      {@code arquivoSaida} é nulo quando o arquivo não gerou saída (falha/bloqueio);
      as contagens são zero nesses casos.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: record imutável; as fábricas não lançam.
  - StatusArquivoTraducao.java
      PROPÓSITO DE NEGÓCIO: Desfecho da tradução de um único arquivo de legenda,
      para a tabela por arquivo e a telemetria distinguirem sucesso limpo, sucesso
      com ressalvas, falha e bloqueio (entrada já traduzida).
      
      <p>INVARIANTES DO DOMÍNIO: cada arquivo processado recebe exatamente um status.
      {@code PARCIAL} = traduziu mas houve avisos (falas mantidas sem tradução para
      revisão); {@code BLOQUEADO} = entrada aparentava já estar em PT-BR e a
      retradução não foi confirmada.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: enum puro; rótulo sempre não-nulo.
  - StatusLoteTraducao.java
      PROPÓSITO DE NEGÓCIO: Desfecho do LOTE de tradução (vários arquivos), para a
      UI/telemetria pararem de mostrar "sucesso" quando houve arquivos com falha.
      
      <p>INVARIANTES DO DOMÍNIO: derivado dos status por arquivo — todos concluídos
      (com ou sem ressalvas) → {@code CONCLUIDO}; nenhum concluído → {@code FALHOU};
      mistura → {@code CONCLUIDO_COM_FALHAS}. {@code CANCELADO} é reservado para
      interrupção explícita do usuário.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: {@link #consolidar(List)} nunca lança; lote
      vazio devolve {@code FALHOU}.
  - TelemetriaTraducao.java
      PROPÓSITO DE NEGÓCIO: registro individual de telemetria da Tradução Local por
      episódio — a unidade que a fatia grava no seu arquivo canônico próprio
      ({@code logs/telemetria_traducao.json}), preservando proveniência (lore),
      modelo, volume, origem das falas, desfecho, avisos e timestamp.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Modelo de dados próprio da Tradução Local — não reutiliza tipos do módulo
      de telemetria; o contrato entre os módulos é apenas o JSON no filesystem.</li>
      <li>{@code nomeEpisodio} identifica o episódio; a chave de deduplicação é a
      forma normalizada por {@link NormalizadorNomeEpisodio}.</li>
      <li>{@code registradoEm} é o timestamp UTC ISO-8601 da atualização, usado como
      critério de precedência dentro da mesma fonte.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Campos ausentes são serializados como {@code null}; a ausência de nome resolve
      para a chave vazia na deduplicação.
  - TelemetriaTraducaoDocumento.java
      PROPÓSITO DE NEGÓCIO: raiz do arquivo canônico próprio da telemetria da
      Tradução Local. Projeta o ESTADO FINAL consolidado por episódio (não é
      append-only) mais os quatro contadores persistentes da fatia, com um
      {@code schemaVersion} explícito para evolução do contrato de arquivo.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>{@code registros} contém no máximo uma entrada por episódio (chave
      normalizada por {@link NormalizadorNomeEpisodio}); o mais recente vence.</li>
      <li>Os quatro contadores representam SOMENTE os incrementos da Tradução Local
      após a adoção deste arquivo (iniciam em zero) — nunca copiam o legado —,
      para que a agregação com {@code telemetria_compartilhada.json} não
      sobreponha eventos já contados.</li>
      <li>{@code schemaVersion} identifica a versão do contrato de arquivo (ex.: "1.0").</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Documento ausente ou ilegível é tratado pelos leitores como vazio (versão nula,
      lista vazia, contadores zero), sem destruir o arquivo físico.

[PASTA] src/main/java/org/traducao/projeto/traducao/domain/ports/
  - TelemetriaTraducaoPort.java
      PROPÓSITO DE NEGÓCIO: porta de telemetria própria da Tradução Local. Substitui
      o acoplamento anterior ao {@code telemetria.TelemetriaService}, permitindo que
      o pipeline registre traduções e incrementos de qualidade sem importar o módulo
      de telemetria — a integração passa a ser apenas o arquivo canônico próprio.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Cada registro/incremento é persistido de forma atômica e sincronizada
      (dentro da JVM), como uma única alteração lógica coerente.</li>
      <li>Os contadores são acumuladores da Tradução Local a partir da adoção do
      arquivo próprio (iniciam em zero).</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Falha de I/O ao persistir é registrada; o estado em memória permanece coerente
      e a próxima escrita bem-sucedida projeta o estado consolidado.

[PASTA] src/main/java/org/traducao/projeto/traducao/infrastructure/adapters/
  - LlmClientAdapter.java
      (sem cabecalho explicativo)
  - LoreAtivaContextoAdapter.java
      PROPÓSITO DE NEGÓCIO: liga o contrato {@link LoreAtivaPort}, exigido pelo peer
      {@code qualidadeTraducao}, à fonte real de contexto do sistema, o
      {@link GerenciadorContexto}. É o único ponto de composição dessa inversão: o peer
      permanece ignorante do {@code contexto} e a fatia {@code traducao} assume a ligação.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Adapter puro de delegação: repassa cada chamada ao {@link GerenciadorContexto}
      sem normalizar, filtrar, corrigir ou reinterpretar o retorno.</li>
      <li>Único adapter da porta; não há implementação concorrente em {@code contexto},
      em {@code qualidadeTraducao.infrastructure} nem por fatia.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Não adiciona tratamento próprio: o comportamento observável é exatamente o do
      {@link GerenciadorContexto} — que não lança e degrada para conjunto vazio / lore
      neutra quando não há contexto ativo.

[PASTA] src/main/java/org/traducao/projeto/traducao/infrastructure/config/
  - LlmProperties.java
      (sem cabecalho explicativo)
  - RestClientConfig.java
      PROPÓSITO DE NEGÓCIO: fornece o bean técnico {@link ObjectMapper} de serialização
      usado internamente pela Tradução Local. A agregação dos provedores de contexto
      ({@code todosProvedoresContexto}) foi movida na E7b para o peer proprietário
      ({@code contexto.infrastructure.config.ContextoBeansConfig}); a composição dos
      extratores de vídeo/strategies pertence a {@code legendasExtracao.infrastructure.config.ExtracaoBeansConfig}.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>O {@link ObjectMapper} é criado com configuração default (sem módulos
      ou features customizadas).</li>
      <li>Esta config não conhece classes de outras fatias funcionais nem do peer contexto.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      A serialização default do {@link ObjectMapper} propaga as exceções de Jackson ao chamador.
  - TradutorProperties.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/infrastructure/dtos/
  - RecordsLlm.java
      Shape da API estendida da LM Studio ({@code /api/v0/models}, fora do
      prefixo {@code /v1}), que — diferente do endpoint OpenAI-compatible
      {@code /v1/models} — informa o campo {@code state} ("loaded" /
      "not-loaded"), permitindo saber com certeza qual modelo está de fato
      carregado em memória.

[PASTA] src/main/java/org/traducao/projeto/traducao/infrastructure/telemetria/
  - TelemetriaTraducaoAdapter.java
      PROPÓSITO DE NEGÓCIO: única escritora do arquivo canônico próprio da telemetria
      da Tradução Local ({@code logs/telemetria_traducao.json}). Projeta, por episódio,
      o estado final consolidado das traduções e mantém os quatro contadores da fatia,
      isolando a Tradução Local do módulo de telemetria (o painel apenas lê este arquivo).
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Deduplicação por nome de episódio normalizado ({@link NormalizadorNomeEpisodio});
      o registro mais recente substitui o anterior — nunca append-only.</li>
      <li>Os quatro contadores iniciam em zero e acumulam SOMENTE eventos da Tradução
      Local após a adoção deste arquivo; jamais copiam valores do legado.</li>
      <li>Cada mutação persiste o documento inteiro (registros + contadores) como uma
      ÚNICA alteração lógica, via escrita atômica (temporário no mesmo diretório +
      movimentação segura).</li>
      <li>Sincronização de escopo JVM: as mutações são {@code synchronized}. Não há
      coordenação entre processos — assume-se uma única instância escrevendo o arquivo.</li>

[PASTA] src/main/java/org/traducao/projeto/traducao/presentation/bootstrap/
  - TraducaoStartup.java
      PROPÓSITO DE NEGÓCIO: é o ponto de partida (bootstrap) próprio da fatia vertical
      Tradução Local. Observa a subida do Quarkus e, quando o operador seleciona o modo
      {@code TRADUZIR}, dispara a CLI de tradução ({@link TradutorCLI}). Substitui o
      roteamento que antes vinha do dispatcher compartilhado {@code config.ModoExecucaoStartup},
      de modo que o ciclo de vida do modo TRADUZIR pertença exclusivamente à Tradução
      Local — sem que a fatia {@code config} conheça qualquer classe de {@code traducao}.
      Segue o mesmo molde dos demais observadores de {@code StartupEvent} já existentes
      no slice ({@code BrowserLauncher}, {@code ConsoleRedirector}).
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Age única e exclusivamente quando {@code app.modo == TRADUZIR}
      (case-insensitive); em qualquer outro modo (WEB ou demais CLIs) retorna
      sem efeito, preservando a exclusividade mútua com o dispatcher compartilhado.</li>
      <li>Não é o bootstrap global da aplicação: o container é iniciado implicitamente
      pelo Quarkus/CDI. Nenhum {@code Application.main()}, {@code @QuarkusMain} ou
      Composition Root artificial é introduzido.</li>
      <li>Delega integralmente a lógica de tradução a {@link TradutorCLI}; não duplica
      nem antecipa qualquer regra de negócio da tradução.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      <ul>
      <li>Se o modo for TRADUZIR mas {@link TradutorCLI} não estiver disponível no
      contexto CDI, lança {@link IllegalStateException} — falha explícita de
      inicialização, jamais silenciosa.</li>
      <li>Qualquer exceção lançada por {@link TradutorCLI#executar()} é registrada e

[PASTA] src/main/java/org/traducao/projeto/traducao/presentation/
  - TradutorCLI.java
      Entrada de linha de comando da Tradução Local (Opção 4). Varre a pasta de
      entrada por arquivos {@code .ass}/{@code .ssa} e traduz cada um sequencialmente.
      
      <h2>Propósito de negócio</h2>
      Representa a interface CLI da Tradução Local: recebe e valida a configuração
      necessária para iniciar o processamento e coordena somente a apresentação CLI
      da Opção 4. Não é o bootstrap global da aplicação — apenas orquestra a leitura
      da pasta de entrada e a delegação de cada arquivo ao caso de uso de tradução.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>A pasta de entrada deve estar configurada e válida antes de iniciar.</li>

[PASTA] src/main/java/org/traducao/projeto/traducao/presentation/ui/
  - ConsoleUILogger.java
      Wrapper thread-safe em torno da barra de progresso (estilo tqdm). Todo
      acesso a {@code pb} e sincronizado porque mensagens podem chegar
      durante a tradução de um episódio.
      <p>
      No modo web, {@code System.out} já é espelhado pelo ConsoleRedirector para
      SSE e {@code logs/console-web.log}. As mensagens visuais não são repetidas no
      SLF4J, evitando que a mesma linha apareça duas vezes no terminal e no painel.
  - PastasExecucao.java
      Pastas efetivas da execução atual. Preenchidas pelo {@code TradutorCLI} a
      partir do diálogo Swing ou das propriedades/linha de comando.
  - TabelaTraducaoRenderer.java
      PROPÓSITO DE NEGÓCIO: Monta a tabela por arquivo do lote de tradução
      (Arquivo | Lore | Falas | Cache | Traduzidas | Avisos | Status) para o console
      da UI, dando a Paulo a visão granular que o "sucesso" agregado escondia.
      
      <p>INVARIANTES DO DOMÍNIO: larguras ajustadas ao maior valor; só de
      apresentação — não decide nada sobre a tradução.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sem resultados, devolve string vazia; não lança.

[PASTA] src/main/java/org/traducao/projeto/traducao/presentation/web/
  - BrowserLauncher.java
      Abre o navegador apos a inicializacao do Quarkus quando {@code app.modo=WEB}.
  - ConsoleRedirector.java
      Interceptador global de System.out.
      Redireciona tudo que é impresso no console padrão para o LogStreamService (SSE)
      sem deixar de imprimir no console físico (terminal do CMD/PowerShell original).
      <p>
      No Spring Boot este bean era instanciado eagerly (singleton), e o redirecionamento
      acontecia no construtor. No Quarkus/CDI (ARC) beans normais são lazy: como nada
      injeta {@code ConsoleRedirector}, o bean nunca era construído e o redirecionamento
      nunca era ativado (o console web parava de receber logs). O fix é o mesmo padrão
      já usado por {@link BrowserLauncher} no mesmo pacote: reagir a {@link StartupEvent}
      força a criação do bean na subida do Quarkus e também o protege da remoção de
      beans não-usados em build-time (beans com método {@code @Observes} nunca são
      removidos).
  - ContextoResponse.java
      PROPÓSITO DE NEGÓCIO: representa um contexto de tradução (obra/anime)
      disponível para seleção na interface, com id técnico, nome de exibição e a
      marcação de qual é o padrão.
      
      <p>INVARIANTES DO DOMÍNIO: os nomes dos campos são contrato JSON público
      consumido pela SPA; {@code padrao} identifica de forma exclusiva o contexto
      pré-selecionado.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sendo um record imutável, não há falha de
      construção; a lista vazia é responsabilidade do gerenciador de contexto.
  - DialogoArquivoController.java
      PROPÓSITO DE NEGÓCIO: disponibiliza aos formulários web do KRONOS um seletor
      nativo e responsivo para arquivos e pastas existentes no computador local.
      <p>
      INVARIANTES DO DOMÍNIO: existe no máximo um diálogo aberto por vez; o helper
      gráfico deve executar em STA; caminhos trafegam em UTF-8/Base64 para preservar
      acentos; o processo PowerShell é reutilizado e nunca recriado a cada clique.
      <p>
      COMPORTAMENTO EM CASO DE FALHA: reinicia o helper uma vez quando ele morre ou
      perde o protocolo; após nova falha ou timeout, encerra o helper e devolve caminho
      vazio, mantendo a interface utilizável e permitindo nova tentativa.
  - DocumentacaoController.java
      Serve o conteúdo bruto das páginas de documentação (pasta {@code docs/} na
      raiz do projeto) para o painel "Documentação" da SPA, que renderiza o
      markdown no navegador (ver static/documentacao/documentacao.js). O README
      raiz é o índice canônico no GitHub; este endpoint espelha a mesma pasta
      docs/ dentro do próprio app, sem precisar sair dele.
  - LlmStatusResponse.java
      PROPÓSITO DE NEGÓCIO: informa ao card do painel inicial o estado ao vivo do
      servidor LLM local (online, modelo carregado, nome do modelo e mensagem).
      
      <p>INVARIANTES DO DOMÍNIO: os nomes dos campos são contrato JSON público
      consumido pela SPA; o nome do modelo só é preenchido quando há modelo em
      memória.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: quando a consulta ao servidor falha, o
      endpoint constrói uma instância com {@code online=false} e a mensagem do erro.
  - LogStreamResource.java
      Endpoint SSE nativo do Quarkus (substitui SseEmitter do Spring MVC).
  - PipelineController.java
      PROPÓSITO DE NEGÓCIO: expõe os endpoints de estado e controle do pipeline
      local à interface web — heartbeat, parada cooperativa da fila, estado da fila,
      status ao vivo do servidor LLM e a lista de contextos de tradução disponíveis.
      
      <p>INVARIANTES DO DOMÍNIO: compartilha a MESMA {@link FilaExecucaoPipeline}
      (bean CDI) dos demais controllers; a parada é cooperativa e preserva o
      progresso já salvo; nenhuma URL, código HTTP ou nome de campo de DTO é alterado
      em relação ao controller monolítico original.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: a consulta de status do LLM nunca propaga
      exceção — falhas viram uma resposta {@code online=false} com a mensagem do
      erro; os demais endpoints são consultas simples sem caminho de falha explícito.
  - TraducaoController.java
      PROPÓSITO DE NEGÓCIO: expõe a tradução local via LLM (Opção 3) à interface
      web, verificando a disponibilidade do servidor LLM, configurando as pastas de
      execução, aplicando o contexto de lore selecionado e traduzindo em lote os
      arquivos de legenda encontrados.
      
      <p>INVARIANTES DO DOMÍNIO: usa a MESMA fila compartilhada via
      {@link PipelineWebSupport}; contexto de lore é obrigatório e validado (sem
      fallback silencioso); apenas extensões suportadas ({@code .ass/.ssa/.srt})
      são traduzidas; nenhuma URL, código HTTP ou nome de campo de DTO é alterado em
      relação ao controller monolítico original.

[PASTA] src/main/java/org/traducao/projeto/trocaTipoLegenda/application/
  - AuditoriaFontesService.java
      Mapeamento de fontes vietnamitas/ANSI problemáticas para Arial como padrão seguro.
  - TrocaTipoLegendaUseCase.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/trocaTipoLegenda/domain/
  - AuditoriaFonteInfo.java
      (sem cabecalho explicativo)
  - AuditoriaLegendaResultado.java
      (sem cabecalho explicativo)
  - EntradaAuditoriaTrocaFonte.java
      (sem cabecalho explicativo)
  - ResultadoGeralAuditoria.java
      (sem cabecalho explicativo)
  - ResultadoTrocaFonte.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/trocaTipoLegenda/domain/exceptions/
  - TrocaTipoLegendaException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/trocaTipoLegenda/infrastructure/
  - TrocaTipoLegendaAuditoriaCache.java
      Cache append-only para gravação de auditoria histórica e granular de cada alteração de fonte aplicada.

[PASTA] src/main/java/org/traducao/projeto/trocaTipoLegenda/presentation/
  - TrocaTipoLegendaController.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/analisadorMidia/application/
  - AnalisarMidiaClassificacaoTest.java
      Cobre o dado VITAL da análise: a classificação do tipo de legenda (codec →
      tipo) e o veredicto de traduzibilidade (texto = traduzível; bitmap = OCR;
      nenhuma = RAW/hardsub). Decide se um episódio segue no pipeline de tradução.
      A lógica vive em {@link ClassificadorLegendaService} (extraído do use case).
  - AnalisarMidiaTelemetriaTest.java
      PROPÓSITO DE NEGÓCIO: garante que a Análise de Mídia (Opção 1) trata a
      telemetria como dataset PERMANENTE — mídias de lotes anteriores não são
      apagadas ao analisar um novo lote, reanalisar a mesma mídia deduplica em vez
      de duplicar, e nenhuma pasta {@code relatorios/} é criada junto dos vídeos.
      
      <p>INVARIANTES DO DOMÍNIO: usa um {@link FfprobeAdapter} falso (sem ffprobe
      real) e um {@link TelemetriaService} próprio; a suíte roda com
      {@code kronos.dir.base} redirecionado, então a telemetria vai para a árvore
      descartável e a leitura do JSON canônico reflete apenas este teste.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: qualquer perda de histórico, duplicação
      indevida ou criação de pasta junto da mídia dispara asserção JUnit.
  - LocalizadorVideosServiceTest.java
      Cobre o localizador de vídeos: varredura recursiva por extensão, arquivo
      único e falha de I/O. Componente extraído do AnalisarMidiaUseCase (Etapa 8).
  - TelemetriaMidiaMapperTest.java
      Cobre o mapeador de telemetria de mídia: extração de metadados técnicos e a
      INVARIANTE de privacidade (caminho relativizado, sem raiz absoluta pessoal).
      Componente extraído do AnalisarMidiaUseCase (Etapa 8).

[PASTA] src/test/java/org/traducao/projeto/analisadorMidia/domain/
  - ResultadoAnaliseLoteSerializacaoTest.java
      Verifica o contrato JSON publicado no SSE da Análise de Mídia (o que o front
      renderiza em cartões/tabelas): campos estruturados presentes e SEM vazar o
      caminho local nem os logs internos (via {@code @JsonIgnore}).

[PASTA] src/test/java/org/traducao/projeto/analisadorMidia/infrastructure/adapters/
  - FfprobeAdapterTest.java
      Cobre o parsing ffprobe-JSON → domínio sem executar ffprobe real: substitui o
      seam de processo externo ({@code executarFfprobeJson}) por JSON canônico e
      verifica container, faixas de vídeo/áudio/legenda e casos-limite.

[PASTA] src/test/java/org/traducao/projeto/analisadorMidia/presentation/
  - AnalisadorMidiaCLITest.java
      PROPÓSITO DE NEGÓCIO: prova que o {@link AnalisadorMidiaCLI}, após a E4b, resolve
      entrada e saída exclusivamente a partir de {@code tradutor.diretorio-entrada} e
      {@code tradutor.diretorio-saida}, preservando o comportamento legado — inclusive a
      saída OPCIONAL que resulta em {@code null} quando não informada (sem o fallback
      {@code traducao_ptbr}, exclusivo do remux) — sem depender de {@code TradutorProperties}
      ou {@code PastasExecucao}.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Entrada ausente/vazia/blank ⇒ {@code null} (inválida); útil ⇒ {@code Path.of(trim)}.</li>
      <li>Saída ausente/vazia/blank ⇒ {@code null} (sem pasta de saída); útil ⇒ {@code Path.of(trim)}.</li>
      <li>A saída NUNCA cai em {@code traducao_ptbr}.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Qualquer divergência na normalização ou o surgimento indevido de fallback reprova o teste.

[PASTA] src/test/java/org/traducao/projeto/
  - ApiControllerTest.java
      (sem cabecalho explicativo)
  - ApiEndpointsTest.java
      (sem cabecalho explicativo)
  - WebInterfaceTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/apiDadosAnime/application/
  - ObterMetadataAnimeUseCaseTest.java
      PROPÓSITO DE NEGÓCIO: garante que o contexto Gundam Narrative mantenha o
      alias usado pelas APIs de capa tanto na tradução quanto na revisão de lore.
      <p>

[PASTA] src/test/java/org/traducao/projeto/apiDadosAnime/infrastructure/adapters/
  - AniListApiClientAdapterTest.java
      PROPÓSITO DE NEGÓCIO: protege o contrato entre a resposta pública da AniList
      e o banner de capa exibido em todos os formulários do KRONOS.
      <p>
      INVARIANTES DO DOMÍNIO: título, capa, escala da nota, episódios e descrição
      limpa devem permanecer compatíveis com {@link AnimeMetadata}.
      <p>
      COMPORTAMENTO EM CASO DE FALHA: qualquer mudança incompatível no mapeamento
      reprova a suíte antes de produzir banners vazios em execução.

[PASTA] src/test/java/org/traducao/projeto/apiDadosAnime/infrastructure/config/
  - ApiDadosAnimeHttpPropertiesIT.java
      PROPÓSITO DE NEGÓCIO: gate/caracterização da subfase E4a — prova, no binding REAL
      do {@code application.yml}, que os timeouts efetivos hoje usados pelos adapters de
      {@code apiDadosAnime} (via {@code LlmProperties}) são {@code 5s/180s}, e que a nova
      config própria ({@code ApiDadosAnimeHttpProperties}) resolve os MESMOS valores —
      comprovando paridade antes de neutralizar/mover o cliente HTTP.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Valores efetivos ATUAIS de {@code LlmProperties}: connect {@code 5s}, read {@code 180s}.</li>
      <li>Nova config {@code ApiDadosAnimeHttpProperties}: os MESMOS pares.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Qualquer divergência reprova o teste — sinal de gate (não prosseguir com a migração).

[PASTA] src/test/java/org/traducao/projeto/arquitetura/
  - ContextoInvalidoC2CaracterizacaoTest.java
      PROPÓSITO DE NEGÓCIO: caracteriza que a FASE C2 (mover os controllers de correção
      e revisão para suas fatias proprietárias) preservou integralmente a validação
      síncrona de contexto — um {@code contextoId} inexistente continua retornando
      HTTP 400, sem enfileirar trabalho e sem iniciar processamento assíncrono.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Um {@code GerenciadorContexto} sem provedores reprova qualquer id
      ({@code existeContexto} → false), simulando um contexto desconhecido.</li>
      <li>O {@code PipelineWebSupport} é espionado: se {@code submeterJobComRelatorio}
      for chamado, houve enfileiramento/processamento — o que reprova o teste.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Qualquer resposta diferente de 400 ou qualquer enfileiramento reprova a suíte,
      sinalizando regressão da validação síncrona.
  - ContratoJsonRecordsE1Test.java
      PROPÓSITO DE NEGÓCIO: congela o contrato JSON público dos dois DTOs de request
      movidos na Subfase E1 ({@code RemuxRequest} e {@code ExtracaoRequest}). A mudança
      de pacote (traducao → fatias proprietárias) NÃO pode alterar a serialização/
      desserialização consumida pela SPA: nomes de campos, tipos e ausência de campos
      extras permanecem idênticos.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>{@code RemuxRequest} expõe exatamente {@code entrada, saida, syncOffsetMs,
      preservarLegendasOriginais}.</li>
      <li>{@code ExtracaoRequest} expõe exatamente {@code entrada, saida, formato}.</li>
      <li>Round-trip (objeto → JSON → objeto) preserva todos os valores.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Qualquer campo renomeado, removido, extra ou com tipo divergente reprova a suíte.

[PASTA] src/test/java/org/traducao/projeto/auditorConteudoLegendas/application/
  - AuditorConteudoIntegridadeTest.java
      PROPÓSITO DE NEGÓCIO: cobre os problemas estruturais da Opção 3 que o conjunto
      de testes anterior não pegava — falas ausentes/extras, deslocamento por
      Comentário, comparação ASS↔SRT, corrupção de parsing, índices duplicados,
      timestamps ilegíveis, imutabilidade e isolamento dos relatórios.
      <p>INVARIANTES DO DOMÍNIO: o modo AMBAS nunca declara "limpo" quando há eventos
      sem par; o modo de arquivo único também audita a integridade de parsing.
      <p>COMPORTAMENTO EM CASO DE FALHA: qualquer "limpo" indevido ou exceção reprova.
  - AuditorConteudoUseCaseTest.java
      PROPÓSITO DE NEGÓCIO: confirma que uma auditoria ASS limpa expõe formato
      e gera o dataset JSON esperado.
      <p>INVARIANTES DO DOMÍNIO: os dois arquivos são ASS válidos e equivalentes.
      <p>COMPORTAMENTO EM CASO DE FALHA: metadado ausente ou persistência
      inexistente reprova o teste.

[PASTA] src/test/java/org/traducao/projeto/auditorConteudoLegendas/application/regras/
  - RegraAlucinacaoQuebraLinhaTest.java
      Simulando o erro do anime 86 Ep 2
  - RegraDanoKaraokeTest.java
      Caso real do 86 T1: romaji em estilo "Opening" com tags leves virou
      alucinação em PT — expansão de só 1.7x, que a checagem de tamanho
      não pegaria; a régua tem de ser a proteção de romaji do detector.
  - RegraEfeitoVazadoTest.java
      Simulando o problema de 86 Ep 2, onde um typesetting (como "{\\pos(100,100)}na")
      acaba sendo traduzido de forma louca pela IA.
  - RegraMetadadosAssTest.java
      (sem cabecalho explicativo)
  - RegraSincroniaEstilosTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/auditorConteudoLegendas/support/
  - AssAuditoriaFixtures.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/cachetraducao/arquitetura/
  - FronteiraCacheTraducaoArchTest.java
      PROPÓSITO DE NEGÓCIO: congela a INDEPENDÊNCIA do peer compartilhado
      {@code cachetraducao}, nascido na subfase E6 com o bloco de cache de tradução
      (modelos {@code EntradaCache}/{@code ProvenienciaCache}/{@code CacheDocumento} e
      serviços {@code CacheTraducaoService}/{@code CacheManutencaoService}). Garante que o
      peer é consumível por qualquer fatia funcional sem criar acoplamento reverso.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>{@code cachetraducao} NÃO depende de {@code traducao} nem de qualquer fatia
      funcional (contexto, LLM, apresentação inclusos).</li>
      <li>{@code cachetraducao} só pode depender de JDK/bibliotecas técnicas, do
      {@code core}, do módulo {@code legenda} e do próprio {@code cachetraducao}.</li>
      <li>{@code cachetraducao.domain} é puro: não depende de {@code cachetraducao.infrastructure}
      nem de framework (Quarkus/CDI/MicroProfile/Spring/Jackson).</li>
      <li>Os três modelos permanecem em {@code domain}; os dois serviços em {@code infrastructure}.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Qualquer dependência proibida ou tipo fora do pacote correto reprova o teste,
      listando a aresta/desvio exato.

[PASTA] src/test/java/org/traducao/projeto/cachetraducao/domain/
  - ProvenienciaCacheTest.java
      PROPÓSITO DE NEGÓCIO: caracteriza a regra de compatibilidade de proveniência do
      cache de tradução, garantindo que uma tradução só é reutilizada quando os SEIS
      campos canônicos batem exatamente — incluindo {@code schemaVersion}, cuja omissão
      histórica permitia reutilizar cache de schema desconhecido.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Igualdade exata dos seis campos (schemaVersion, contextoId, contextoHash,
      modeloLlm, idiomaOrigem, idiomaDestino) autoriza a reutilização.</li>
      <li>Diferença isolada em qualquer um dos seis campos torna incompatível.</li>
      <li>{@code schemaVersion} 0 (valor materializado quando o campo está ausente no
      JSON) nunca é considerado igual ao {@code SCHEMA_ATUAL}: sem normalização.</li>
      <li>Comparar com {@code null} é sempre "diferente".</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Qualquer regressão que volte a ignorar {@code schemaVersion} ou a normalizar 0 para
      a versão atual reprova estes testes.

[PASTA] src/test/java/org/traducao/projeto/cachetraducao/infrastructure/
  - CacheManutencaoServiceTest.java
      PROPÓSITO DE NEGÓCIO: prova que a manutenção da pasta cache preserva formato,
      proveniência, extensões futuras e uma cópia restaurável antes de salvar.
      
      <p>INVARIANTES DO DOMÍNIO: cobre lista legada e documento versionado; nenhuma
      estrutura inválida é aceita para escrita.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: o teste exige {@link IOException} e
      confirma que o original não foi alterado.
  - CacheTraducaoServiceTest.java
      Cobre o cache versionado por proveniência: reuso só quando lore/modelo batem,
      invalidação + arquivamento quando divergem, migração do formato antigo e
      preservação (não sobrescrita) de cache corrompido.
  - CompatibilidadeCacheJsonLegadoTest.java
      PROPÓSITO DE NEGÓCIO: gate de compatibilidade retroativa da E6. Prova que um
      arquivo {@code .cache.json} produzido ANTES da extração do peer {@code cachetraducao}
      continua legível pelos tipos pós-move, sem depender de nenhum FQN antigo — garantindo
      que a migração de pacote NÃO quebra os caches já persistidos no disco dos usuários.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>A fixture é textual e estável ({@code src/test/resources/cachetraducao/legado.cache.json}),
      NÃO gerada pelas classes pós-move — caracteriza o schema histórico.</li>
      <li>Desserialização por campos (sem tipagem polimórfica): o JSON não carrega
      {@code @class}/discriminador, logo o nome do pacote é irrelevante para a leitura.</li>
      <li>Regravação mantém o mesmo schema (chaves/valores), comparado estruturalmente
      (não por igualdade textual de espaços/ordem).</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Qualquer campo ausente/divergente ou schema alterado na regravação reprova o teste —
      sinal de que a E6 quebrou a compatibilidade do cache.

[PASTA] src/test/java/org/traducao/projeto/config/
  - ModoExecucaoDispatcherTest.java
      PROPÓSITO DE NEGÓCIO: caracteriza o contrato do dispatcher compartilhado
      {@link ModoExecucaoStartup} após a extração do modo TRADUZIR (D-Config). Fixa que
      o modo TRADUZIR deixou de ser roteado aqui — passando a ser tratado como um
      short-circuit, e nunca como "modo desconhecido" — sem afetar o roteamento dos
      demais modos nem a rejeição de modos inválidos.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>{@code WEB} e {@code TRADUZIR} retornam sem efeito (nenhuma CLI roteada por
      este dispatcher; TRADUZIR tem ciclo de vida próprio em {@code traducao}).</li>
      <li>Um modo inválido continua sendo rejeitado com {@link IllegalStateException}.</li>
      <li>Estes três caminhos (WEB, TRADUZIR, inválido) nunca chamam {@code .get()} nos
      beans injetados — por isso a caracterização dispensa cabeamento CDI.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Qualquer regressão (TRADUZIR voltando a ser tratado como desconhecido, ou modo
      inválido deixando de lançar) reprova a suíte.

[PASTA] src/test/java/org/traducao/projeto/contexto/arquitetura/
  - FronteiraContextoArchTest.java
      PROPÓSITO DE NEGÓCIO: congela a INDEPENDÊNCIA do peer compartilhado
      {@code contexto} (E7a domínio/lore + E7b infrastructure). Garante que o peer é
      consumível por qualquer fatia funcional sem acoplamento reverso.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>{@code contexto} NÃO depende de {@code traducao} nem de outra fatia funcional:
      só JDK/libs técnicas, {@code core} e o próprio {@code contexto}.</li>
      <li>{@code contexto.domain} é puro: sem {@code contexto.infrastructure} nem framework.</li>
      <li>{@code contexto.lore} depende somente de {@code contexto.domain}, JDK e Spring
      {@code @Component} — nunca de {@code core}, {@code infrastructure} ou outra fatia.</li>
      <li>{@code contexto.infrastructure} é congelado nominalmente: exatamente
      {@code GerenciadorContexto} e {@code ContextoBeansConfig}.</li>
      <li>{@code contexto.domain} contém os cinco tipos homologados;
      {@code contexto.lore} agrega 56 classes.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Qualquer dependência proibida, tipo fora do pacote correto ou terceira classe em
      infrastructure reprova o teste, listando a aresta/desvio exato.

[PASTA] src/test/java/org/traducao/projeto/contexto/domain/
  - HierarquiaExcecaoContextoTest.java
      PROPÓSITO DE NEGÓCIO: congela a hierarquia de exceções extraída na E7a —
      {@code ExcecaoContexto} (raiz do módulo {@code contexto}) com
      {@code ContextoNaoEncontradoException} sob ela, ambas movidas de {@code traducao}
      e reparentadas para deixarem de ser {@code TradutorException}.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Construtores preservam mensagem e causa.</li>
      <li>{@code ExcecaoContexto} IS-A {@code BasePipelineException}.</li>
      <li>{@code ContextoNaoEncontradoException} IS-A {@code ExcecaoContexto} (logo IS-A
      {@code BasePipelineException}).</li>
      <li>Prova NEGATIVA: {@code ContextoNaoEncontradoException} não é mais
      {@code TradutorException}. Como o fluxo desta exceção NÃO alcança o
      {@code catch (TradutorException)} do {@code TradutorCLI} (só é lançada por
      {@code GerenciadorContexto.definirContextoAtivo}, fora do caminho do CLI), a
      E7a não precisou de {@code catch (ExcecaoContexto)} no CLI.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Qualquer regressão da hierarquia (positiva ou negativa) reprova o teste — também é a
      garantia de que o {@code BasePipelineExceptionMapper} (genérico sobre
      {@code BasePipelineException}) continua cobrindo toda a família.

[PASTA] src/test/java/org/traducao/projeto/contexto/
  - ProtecaoConteudoLoreTest.java
      PROPÓSITO DE NEGÓCIO: gate de conteúdo da E7a. Prova que a extração do peer
      {@code contexto} NÃO alterou nenhum prompt, nome de exibição, id ou termo protegido
      das 53 lores descobertas por CDI — comparando o estado vivo pós-move com o manifesto
      determinístico capturado ANTES do move ({@code /contexto/manifesto-lore.properties}).
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>O manifesto é pequeno/legível: {@code count}, {@code ids} (ordenados por id),
      {@code aggregate} e uma linha {@code id.<id>=<hash>} por provedor.</li>
      <li>A entrada do hash por provedor, em ordem estável e UTF-8, é
      {@code id + " " + nomeExibicao + " " + promptSistema + " " + termosProtegidos ordenados},
      com os {@code \r} removidos (normalização de line-ending). Os prompts são montados
      em COMPILE-TIME a partir de text blocks; conforme a fonte seja consultada com CRLF
      (Windows/{@code autocrlf}) ou LF, a compilação pode reter ou não o {@code \r}. Como
      {@code \r} não é conteúdo de lore, removê-lo torna o gate determinístico entre
      checkouts — sem afetar a detecção de qualquer mudança textual real.</li>
      <li>O agregado é o SHA-256 da concatenação {@code id\thash\n} na ordem por id.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
  - RegistroProvedoresContextoIT.java
      PROPÓSITO DE NEGÓCIO: caracteriza a descoberta e resolução CDI dos provedores de
      contexto após a E7b, provando que mover o {@code GerenciadorContexto} e o producer
      {@code todosProvedoresContexto} para o peer {@code contexto} NÃO alterou o conjunto
      injetado, a resolução do manager, a ordenação nem a seleção. O manager agora reside em
      {@code contexto.infrastructure} e a lista é produzida por
      {@code contexto.infrastructure.config.ContextoBeansConfig}. As 3 classes agregadoras
      Macross sem {@code @Component} continuam fora do registro, mantendo exatamente 53 provedores.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Exatamente 53 provedores CDI; nenhum id nulo/vazio; nenhum id duplicado.</li>
      <li>{@code GerenciadorContexto} resolve sem ambiguidade e a {@code List<ProvedorContexto>}
      resolve pelo producer sem duplicação (o manager e a injeção direta veem os mesmos 53).</li>
      <li>A lista ordenada de ids é idêntica ao baseline
      ({@code /contexto/manifesto-lore.properties}).</li>

[PASTA] src/test/java/org/traducao/projeto/core/exception/
  - BasePipelineExceptionTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/core/execucao/
  - FilaExecucaoPipelineTest.java
      Cobre o contrato de execução da fila única do pipeline: submissão, execução
      síncrona com propagação de exceção, sinal de ocupação e cancelamento. É a
      invariante que garante que UI, MCP e CLI compartilhem a MESMA política de
      execução sequencial (um job pesado por vez).

[PASTA] src/test/java/org/traducao/projeto/core/io/
  - DiretorioBaseKronosTest.java
      PROPÓSITO DE NEGÓCIO: garante que o resolver central de diretórios preserva o
      comportamento de produção (raiz = diretório corrente) e redireciona quando a
      system property {@code kronos.dir.base} está definida — o mecanismo que
      impede a suíte de contaminar os diretórios operacionais reais.
      
      <p>INVARIANTES DO DOMÍNIO: salva e restaura o valor original da property para
      não afetar os demais testes do mesmo JVM.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: asserções JUnit falham se a resolução
      divergir do contrato.

[PASTA] src/test/java/org/traducao/projeto/core/presentation/ui/
  - ConsoleEntradaCaracterizacaoTest.java
      PROPÓSITO DE NEGÓCIO: caracteriza a saída atual de
      {@link ConsoleEntrada#imprimirErroSaida()} — o único método efetivamente
      consumido pelas fatias CLI — para blindar a subfase E2 (movimentação de
      {@code ConsoleEntrada} para o {@code core.presentation.ui}). Prova que o move
      é puramente de pacote: mensagens, cores ANSI e enquadramento por linhas em
      branco permanecem byte a byte idênticos.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>A saída é exatamente: linha em branco, a mensagem de erro em VERMELHO/negrito,
      a dica de recuperação em AMARELO e uma linha em branco final — cada uma via
      {@code println}, com {@link System#lineSeparator()}.</li>
      <li>O esperado é reconstruído a partir do próprio {@link AnsiCores}, garantindo
      que qualquer troca de mensagem, cor ou ordem reprove o teste.</li>
      <li>O {@code System.out} original é SEMPRE restaurado (via {@link AfterEach}),
      nunca deixando o stream substituído vazar para outros testes.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Qualquer divergência na saída capturada reprova com {@code assertEquals},
      exibindo o esperado versus o real.

[PASTA] src/test/java/org/traducao/projeto/correcaoLegendas/application/
  - CorrigirLegendasUseCaseTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/legendasExtracao/application/
  - ExtrairLegendaUseCaseTest.java
      Cobre a orquestração do extrator sem ferramentas externas: seleção de faixa
      pela strategy real, extração para arquivo temporário, validação de saída,
      guarda anti-sobrescrita, cleanup de parcial, classificação de timeout e o
      mapeamento da telemetria.
  - ValidadorSaidaExtracaoTest.java
      Cobre a blindagem de saída: existência, tamanho > 0 e correspondência de
      formato (ASS/SRT/PGS) do arquivo recém-extraído.

[PASTA] src/test/java/org/traducao/projeto/legendasExtracao/infrastructure/adapters/
  - FfmpegAdapterTest.java
      Cobre a identificação de faixas de legenda em contêineres não-MKV (mp4/mov/…)
      a partir do JSON do {@code ffprobe -show_streams}, sem ffprobe real: substitui
      o seam de processo externo ({@code executarIdentificacao}).
  - MkvToolNixAdapterTest.java
      Cobre a identificação de faixas de legenda a partir do JSON do
      {@code mkvmerge --identify}, sem MKVToolNix real: substitui o seam de processo
      externo ({@code executarIdentificacao}) por saída canônica.

[PASTA] src/test/java/org/traducao/projeto/legendasExtracao/infrastructure/config/
  - ExtratoresInjecaoIT.java
      PROPÓSITO DE NEGÓCIO: caracteriza — antes e depois da subfase D-Ext — a
      composição CDI dos extratores de vídeo e das strategies de formato, que hoje é
      montada por producers de coleção. Congela o contrato de agregação consumido por
      {@code ExtrairLegendaUseCase} para que a mudança do LOCAL dos producers (de
      {@code traducao.RestClientConfig} para a config própria de {@code legendasExtracao})
      não altere quem é injetado nem a resolução por extensão/formato.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>{@code List<ExtratorVideoPort>} contém exatamente {@link MkvToolNixAdapter}
      e {@link FfmpegAdapter}, sem duplicatas nem implementações inesperadas.</li>
      <li>{@code List<ExtratorStrategy>} contém exatamente {@link ExtratorAssStrategy},
      {@link ExtratorSrtStrategy} e {@link ExtratorPgsStrategy}, idem.</li>
      <li>Cada formato (ASS/SRT/PGS) tem exatamente uma strategy compatível; cada
      extensão de contêiner conhecida (.mkv/.mp4) tem exatamente um adapter
      compatível; extensão desconhecida não tem adapter compatível.</li>
      <li>Comparações por CONJUNTO/tipo, nunca por ordem de lista.</li>
      </ul>

[PASTA] src/test/java/org/traducao/projeto/legendasExtracao/presentation/
  - ExtratorCLITest.java
      PROPÓSITO DE NEGÓCIO: prova que o {@link ExtratorCLI}, após a E4b, resolve a pasta
      de vídeos exclusivamente a partir de {@code tradutor.diretorio-entrada}, com a mesma
      normalização por {@code trim} do fluxo legado, sem qualquer dependência de
      {@code TradutorProperties} ou {@code PastasExecucao}.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Entrada ausente, vazia ou só com espaços ⇒ {@code null} (inválida).</li>
      <li>Entrada útil ⇒ {@code Path.of(valor.trim())}.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Qualquer divergência na normalização reprova o teste.

[PASTA] src/test/java/org/traducao/projeto/legenda/application/
  - DetectorEfeitoKaraokeServiceTest.java
      Linha real que escapou da revisão: letra "I" afogada em transformações.
      Linha com \pos e fscx/fscy onde o texto visível é curto em relação às tags.

[PASTA] src/test/java/org/traducao/projeto/legenda/arquitetura/
  - FronteiraLegendaArchTest.java
      PROPÓSITO DE NEGÓCIO: congela a INDEPENDÊNCIA do módulo peer compartilhado
      {@code legenda}. Garante que o módulo é consumível por qualquer fatia funcional sem
      criar acoplamento reverso. Evolução da superfície do módulo:
      <ul>
      <li>E3c: {@code PoliticaEstiloMusical}.</li>
      <li>E5a: {@code DocumentoLegenda} e {@code EventoLegenda} (modelo puro, movido de
      {@code traducao.domain.legenda}).</li>
      <li>E5b: {@code ExcecaoLegenda} (raiz das falhas do módulo) e
      {@code ArquivoLegendaException}, ambos dependendo apenas de
      {@code core.exception.BasePipelineException} — aresta {@code legenda -> core}
      legítima, sem {@code legenda -> traducao}.</li>
      <li>E5c: {@code LeitorLegendaAss/Srt} e {@code EscritorLegendaAss/Srt} em
      {@code legenda.infrastructure}, dependendo apenas de {@code legenda.domain},
      JDK/libs técnicas e (escritores) {@code core.util.ArquivoAtomicoUtil} —
      {@code legenda -> core} legítima.</li>
      <li>E8a: {@code DetectorEfeitoKaraokeService} (regra única música/karaokê) em
      {@code legenda.application}, movido de {@code traducao.application}. Nova
      fronteira da camada {@code application}: não depende de
      {@code legenda.infrastructure} nem de fatia funcional.</li>
      </ul>
      
      <h2>Invariantes do domínio</h2>

[PASTA] src/test/java/org/traducao/projeto/legenda/domain/
  - HierarquiaExcecaoLegendaTest.java
      PROPÓSITO DE NEGÓCIO: congela a hierarquia de exceções extraída na E5b —
      {@code ExcecaoLegenda} (raiz do módulo {@code legenda}) e
      {@code ArquivoLegendaException} sob ela, com {@code EntradaJaTraduzidaException}
      permanecendo em {@code traducao} mas reparentada para a hierarquia de legenda.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Construtores preservam mensagem e causa.</li>
      <li>{@code ArquivoLegendaException} IS-A {@code ExcecaoLegenda} IS-A {@code BasePipelineException}.</li>
      <li>{@code EntradaJaTraduzidaException} IS-A {@code ArquivoLegendaException} (logo IS-A
      {@code ExcecaoLegenda} e {@code BasePipelineException}).</li>
      <li>Provas NEGATIVAS: nenhuma das duas é mais {@code TradutorException} — é exatamente
      por isso que o {@code TradutorCLI} precisou de um {@code catch (ExcecaoLegenda)}
      equivalente ao ramo {@code TradutorException}.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Qualquer regressão da hierarquia (positiva ou negativa) reprova o teste — também é a
      garantia de que o {@code BasePipelineExceptionMapper} (genérico sobre
      {@code BasePipelineException}) continua cobrindo toda a família.
  - PoliticaEstiloMusicalTest.java
      PROPÓSITO DE NEGÓCIO: caracteriza a regra pura {@link PoliticaEstiloMusical#estiloIgnorado(String)}
      herdada de {@code TradutorProperties.estiloIgnorado} — lista configurada + heurísticas
      + regex de fronteira de palavra — travando o comportamento HISTÓRICO exato após o move
      para o módulo {@code legenda}.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Match de lista é case-insensitive; heurística e regex idênticas ao comportamento anterior.</li>
      <li>{@code null}/blank → {@code false}; a política não decide sozinha o envio ao LLM.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Divergência de qualquer caso reprova, sinalizando quebra da regra migrada.

[PASTA] src/test/java/org/traducao/projeto/legenda/infrastructure/config/
  - PoliticaEstiloMusicalProducerIT.java
      PROPÓSITO DE NEGÓCIO: prova o WIRING do {@link PoliticaEstiloMusicalProducer} — o bean
      {@code @Singleton} {@link PoliticaEstiloMusical} é produzido e injeta a lista real do
      {@code application.yml} ({@code tradutor.estilos-ignorados}), não apenas o fallback.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Um estilo presente APENAS na lista do yml (ex.: "Mobile Suit Gundam", sem palavra-chave
      musical) é reconhecido — prova de que a lista completa foi injetada, não o fallback.</li>
      <li>A heurística/regex continuam valendo; um estilo comum não é ignorado.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Falha de produção/injeção do bean, ou lista incorreta, reprova o teste.

[PASTA] src/test/java/org/traducao/projeto/legenda/infrastructure/
  - EscritorLegendaAssTest.java
      (sem cabecalho explicativo)
  - LeitorEscritorSrtTest.java
      Cobre a leitura/escrita nativa de SRT: preservação de índice e timestamps,
      quebra interna via \N, round-trip e troca apenas do texto (o pipeline traduz
      só o texto, mantendo tempos).

[PASTA] src/test/java/org/traducao/projeto/llm/arquitetura/
  - FronteiraLlmArchTest.java
      PROPÓSITO DE NEGÓCIO: congela a INDEPENDÊNCIA do peer de topo {@code llm} extraído na
      E8d — o contrato genérico do modelo de linguagem ({@code LlmPort}) e seus records
      ({@code Lote}, {@code TraducaoLote}, {@code StatusLlm}), todos em {@code llm.domain}.
      Garante que o peer é consumível por qualquer fatia funcional sem acoplamento reverso e
      sem arrastar framework, cliente HTTP ou qualquer biblioteca externa.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Allowlist POSITIVA de destinos: {@code llm} só pode depender de pacotes do JDK
      ({@code java.}), do próprio {@code org.traducao.projeto.llm} e de
      {@code org.traducao.projeto.core} (se houver consumo real). Qualquer outro pacote
      — outra fatia, outro peer, framework (Spring/Quarkus/Jackson), cliente HTTP ou
      qualquer biblioteca externa — é violação. NÃO se admite um destino só porque não
      pertence a uma fatia conhecida.</li>
      <li>Inventário nominal EXATO por FQN COMPLETO: exatamente os quatro proprietários
      top-level ({@code llm.domain.LlmPort}, {@code llm.domain.Lote},
      {@code llm.domain.TraducaoLote}, {@code llm.domain.StatusLlm}). Congelar por FQN
      (não por simple name) impede que uma classe mude de pacote/camada mantendo o mesmo
      nome sem reprovar.</li>
      <li>Estrutura: todos os quatro tipos ficam em {@code llm.domain}; NÃO existe
      {@code llm.infrastructure} nem {@code llm.application}; não há adapter nem

[PASTA] src/test/java/org/traducao/projeto/mapaProjeto/application/
  - GeradorMapaProjetoUseCaseTest.java
      {@code Files.list} (usado por {@code executar}, diferente de
      {@code Files.walk} usado em outros use cases deste projeto) lança
      {@code NotDirectoryException} quando o caminho informado não é um
      diretório — e, ao contrário dos demais use cases, {@code executar} aqui
      não tem nenhuma checagem prévia que intercepte esse caso. Isso o torna o
      único, entre as lacunas de exceção corrigidas nesta auditoria, em que a
      falha real é reproduzível de forma determinística e portátil num teste.

[PASTA] src/test/java/org/traducao/projeto/mcp/
  - KronosMcpToolsTest.java
      Garante que a porta MCP siga a MESMA política de execução da UI: a análise
      roda pela {@link FilaExecucaoPipeline} (não direto), e uma solicitação com a
      fila ocupada é recusada de forma estruturada em vez de rodar em paralelo com
      outro job. Usa um fake do use case — sem ffprobe real.

[PASTA] src/test/java/org/traducao/projeto/novoKaraoke/application/
  - ConversorKaraokeUseCaseTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/qualidadeTraducao/application/
  - DetectorTraducaoIdenticaServiceTest.java
      PROPÓSITO DE NEGÓCIO: impede que nomes próprios legítimos sejam enviados ao
      revisor apenas porque são idênticos no inglês e no PT-BR.
      <p>INVARIANTES DO DOMÍNIO: hesitação e pontuação não descaracterizam nomes;
      palavras conversacionais inglesas continuam pendentes.
      <p>COMPORTAMENTO EM CASO DE FALHA: falso nome ou falso inglês reprova o teste.
  - MascaradorTagsTest.java
      PROPÓSITO DE NEGÓCIO: garante que cache reutilizado não danifique estilo,
      posicionamento nem quebras estruturais das legendas ASS/SSA.
      
      <p>INVARIANTES DO DOMÍNIO: somente o texto visível pode mudar; perda, criação,
      alteração ou reordenação de tags invalida a tradução armazenada.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: cada divergência produz uma asserção falsa
      explícita, impedindo regressões que aceitariam cache estruturalmente corrompido.
  - ValidadorTraducaoServiceTest.java
      Caso real (Gundam Narrative): LLM rotulou a resposta em vez de só traduzir.
      Caso real (G-Reconguista): marcador do pipeline Python antigo na legenda final.

[PASTA] src/test/java/org/traducao/projeto/qualidadeTraducao/arquitetura/
  - FronteiraQualidadeTraducaoArchTest.java
      PROPÓSITO DE NEGÓCIO: congela a INDEPENDÊNCIA do peer compartilhado
      {@code qualidadeTraducao} — extraído na E8b ({@code MascaradorTags} em application,
      {@code ExcecaoQualidadeTraducao} + {@code AlucinacaoDetectadaException} em domain),
      ampliado na E8c ({@code ValidadorTraducaoService} + {@code ProtecaoLegendaAssService}
      em application) e na E8c.1 ({@code DetectorTraducaoIdenticaService} em application e a
      porta {@code LoreAtivaPort} em domain, que inverte o antigo acoplamento a
      {@code contexto}). Garante que o peer é consumível por qualquer fatia funcional sem
      acoplamento reverso.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>{@code qualidadeTraducao} só depende de JDK/libs técnicas, {@code core} e do
      próprio {@code qualidadeTraducao} — nunca de {@code traducao} nem de outra fatia
      funcional, nem de outro peer ({@code legenda}, {@code cachetraducao},
      {@code contexto}, {@code llm}). Em particular o detector, que antes dependia de
      {@code contexto.infrastructure.GerenciadorContexto}, agora depende só da porta
      {@code LoreAtivaPort} do próprio peer.</li>
      <li>Inventário nominal EXATO por FQN COMPLETO: exatamente os sete proprietários
      top-level ({@code qualidadeTraducao.application.DetectorTraducaoIdenticaService},
      {@code qualidadeTraducao.application.MascaradorTags},
      {@code qualidadeTraducao.application.ProtecaoLegendaAssService},

[PASTA] src/test/java/org/traducao/projeto/qualidadeTraducao/domain/
  - HierarquiaExcecaoQualidadeTraducaoTest.java
      PROPÓSITO DE NEGÓCIO: congela a hierarquia de exceções extraída na E8b —
      {@code ExcecaoQualidadeTraducao} (raiz do peer {@code qualidadeTraducao}) com
      {@code AlucinacaoDetectadaException} sob ela, movidas de {@code traducao} e
      reparentadas para deixarem de ser {@code TradutorException}.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>{@code ExcecaoQualidadeTraducao} IS-A {@code BasePipelineException}.</li>
      <li>{@code AlucinacaoDetectadaException} IS-A {@code ExcecaoQualidadeTraducao} (logo
      IS-A {@code BasePipelineException}).</li>
      <li>Prova NEGATIVA: {@code AlucinacaoDetectadaException} NÃO é mais
      {@code TradutorException} — por isso os sítios que a capturavam por herança
      (ProcessarEpisodioUseCase, TradutorCLI) passaram a multi-catch explícito na E8b.</li>
      <li>Construtores preservam mensagem (e causa, na base).</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Qualquer regressão da hierarquia (positiva ou negativa) reprova o teste — também é a
      garantia de que o {@code BasePipelineExceptionMapper} (genérico sobre
      {@code BasePipelineException}) continua cobrindo toda a família.

[PASTA] src/test/java/org/traducao/projeto/raspagemCorrecao/application/
  - CorrigirComGoogleUseCaseTest.java
      PROPÓSITO DE NEGÓCIO: prova a regressão central do menu — uma entrada vazia
      produzida pela limpeza precisa ser preenchida pela contingência Google.
      
      <p>INVARIANTES DO DOMÍNIO: teste não acessa a internet nem grava telemetria no
      projeto; cache versionado e proveniência permanecem intactos.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: qualquer ausência de tradução aplicada ou
  - ProtetorTermosLoreServiceTest.java
      PROPÓSITO DE NEGÓCIO: prova que a contingência online preserva terminologia
      oficial declarada na lore em vez de produzir traduções literais destrutivas.
      
      <p>INVARIANTES DO DOMÍNIO: termos explícitos e regra “Manter sempre” são
      protegidos; marcador perdido invalida a resposta.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: qualquer termo alterado ou marcador aceito
      indevidamente reprova o teste.

[PASTA] src/test/java/org/traducao/projeto/raspagemCorrecao/infrastructure/
  - GoogleTranslateScraperTest.java
      Cobre o contrato tipado e o retry curado sem tocar na rede: substitui o
      transporte HTTP ({@code executarGet}) por respostas canônicas e anula a espera
      ({@code dormir}). Verifica o mapeamento de cada desfecho para
      {@link StatusRaspagem} e que só a falha transitória é retentada.

[PASTA] src/test/java/org/traducao/projeto/raspagemRevisao/application/
  - CorretorDeterministicoConcordanciaServiceTest.java
      PROPÓSITO DE NEGÓCIO: comprova as correções locais que devem preceder o Nemo.
      <p>INVARIANTES DO DOMÍNIO: somente o trecho objetivo muda; restante da fala e
      pontuação permanecem intactos.
      <p>COMPORTAMENTO EM CASO DE FALHA: proposta ausente ou ampla reprova o teste.
  - DetectorConcordanciaServiceTest.java
      PROPÓSITO DE NEGÓCIO: comprova que a revisão automática encontra divergências
      objetivas de gênero sem reescrever falas corretas por inferência do falante.
      
      <p>INVARIANTES DO DOMÍNIO: evidência explícita continua detectável; `I/you`
      e palavras polissêmicas como `cara` não produzem falso positivo.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: qualquer regressão reprova o teste antes
      que uma proposta indevida alcance o cache operacional.
  - LeitorCacheReferenciaServiceTest.java
      PROPÓSITO DE NEGÓCIO: prova que a Opção 6 consome tanto caches históricos
      quanto o formato versionado atualmente produzido pelas Opções 4 e 5.
      
      <p>INVARIANTES DO DOMÍNIO: índice, original e tradução permanecem idênticos
      ao JSON e a proveniência não interfere na leitura das entradas.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: incompatibilidade de schema reprova o teste.
  - ResultadoRevisaoLegendasTest.java
      PROPÓSITO DE NEGÓCIO: garante que o painel da Opção 6 diferencie conclusão
      integral de uma execução estável que ainda deixou falas sem solução.
      
      <p>INVARIANTES DO DOMÍNIO: qualquer pendência impede banner verde.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: status divergente reprova o teste.
  - RevisarCacheUseCaseTest.java
      (sem cabecalho explicativo)
  - RevisarLegendasCacheIntegracaoTest.java
      PROPÓSITO DE NEGÓCIO: cobre o fluxo completo da Opção 6 no modo Cache
      (endpoint → sincronização → gravação), garantindo que cache seguro corrige o
      ASS e cache ausente/insegurо nunca produz sucesso silencioso.
      <p>INVARIANTES DO DOMÍNIO: o vídeo/legenda EN nunca é obrigatório; a
      proveniência e o vínculo por índice/estilo/texto governam qualquer escrita.
      <p>COMPORTAMENTO EM CASO DE FALHA: sem cache correspondente o arquivo fica
      pendente; qualquer alteração indevida do ASS reprova o teste.
  - RevisarLegendasCacheSeguroTest.java
      PROPÓSITO DE NEGÓCIO: comprova a blindagem do modo "Cache" da Opção 6 — uma
      entrada só vira referência quando casa com segurança (índice + estilo +
      proveniência + texto); o resto fica SEM_REFERÊNCIA_SEGURA.
      <p>INVARIANTES DO DOMÍNIO: placas/karaokê não exigem referência e não são
      marcadas; cache sem proveniência não vincula nada.
      <p>COMPORTAMENTO EM CASO DE FALHA: qualquer vínculo indevido ou marcação
      incorreta reprova o teste.
  - RevisarLegendasContextoTest.java
      PROPÓSITO DE NEGÓCIO: prova que a Opção 6 não revisa uma obra usando a lore
      selecionada por engano na interface quando o cache conhece sua proveniência.
      
      <p>INVARIANTES DO DOMÍNIO: contexto versionado vence fallback manual.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: ativação de DanMachi para cache Gundam
      reprova o teste antes que uma legenda real seja modificada.
  - RevisarLegendasProtecaoMassaTest.java
      PROPÓSITO DE NEGÓCIO: garante que a Revisão de Legendas não seja usada como
      retradutor acidental de um ASS restaurado parcialmente em inglês.
      <p>INVARIANTES DO DOMÍNIO: pequenos resíduos continuam revisáveis; regressão
      ampla é bloqueada antes de chamadas em massa ao LLM ou Google.
      <p>COMPORTAMENTO EM CASO DE FALHA: mudança indevida do limiar reprova os testes.
  - SincronizadorLegendaCacheServiceTest.java
      PROPÓSITO DE NEGÓCIO: prova que as correções da Opção 5 chegam à Opção 6 sem
      apagar pendências que o Google não conseguiu resolver.
      
      <p>INVARIANTES DO DOMÍNIO: índice liga cache e diálogo; vazio é sempre
      preservação, nunca comando de exclusão.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: mudança indevida no texto reprova o teste.

[PASTA] src/test/java/org/traducao/projeto/remuxer/application/
  - MapeadorMidiaServiceTest.java
      Criar arquivos de vídeo MKV com padrão "EpsXX" (como nos arquivos de 86 do usuário)
      Criar arquivos de legenda ASS com padrão "_-_XX" e colchetes
  - RemuxarLoteUseCaseTest.java
      PROPÓSITO DE NEGÓCIO: garante que ASS não vazio porém estruturalmente
      inválido seja bloqueado antes do adaptador externo.
      INVARIANTES DO DOMÍNIO: nenhum MKV final é criado.
      COMPORTAMENTO EM CASO DE FALHA: relatório registra legenda inválida.

[PASTA] src/test/java/org/traducao/projeto/remuxer/infrastructure/adapters/
  - MkvmergeAdapterTest.java
      PROPÓSITO DE NEGÓCIO: comprova que destino anterior é barreira absoluta e
      nunca é apagado nem substituído.
      INVARIANTES DO DOMÍNIO: runner externo não chega a ser chamado.
      COMPORTAMENTO EM CASO DE FALHA: conteúdo original deve permanecer idêntico.

[PASTA] src/test/java/org/traducao/projeto/remuxer/presentation/
  - RemuxerCLITest.java
      PROPÓSITO DE NEGÓCIO: prova que o {@link RemuxerCLI}, após a E4b, resolve a pasta de
      legendas PTBR reproduzindo fielmente — como duplicação consciente e autorizada — a
      política legada {@code TradutorProperties.resolverDiretorioSaida()}: saída explícita
      quando informada, senão o fallback {@code entrada/traducao_ptbr}, com {@code trim}
      preservado e sem depender de {@code TradutorProperties} ou {@code PastasExecucao}.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Saída ausente/vazia/blank ⇒ {@code pastaVideos.resolve("traducao_ptbr")}.</li>
      <li>Saída útil ⇒ {@code Path.of(valor.trim())}.</li>
      <li>A resolução da saída nunca devolve {@code null}.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Qualquer divergência do fallback ou da normalização reprova o teste.

[PASTA] src/test/java/org/traducao/projeto/renomearArquivos/application/
  - RenomeadorUseCaseTest.java
      PROPÓSITO DE NEGÓCIO: impede que testes temporários contaminem o dataset
      persistente do projeto.

[PASTA] src/test/java/org/traducao/projeto/revisaoLore/application/
  - DetectorTermosLoreServiceTest.java
      PROPÓSITO DE NEGÓCIO: preserva tecnologias oficiais declaradas pela lore.
      <p>INVARIANTES DO DOMÍNIO: psycho-frame não é resíduo inglês nesta obra.
      <p>COMPORTAMENTO EM CASO DE FALHA: falso positivo reprova o teste.
      PROPÓSITO DE NEGÓCIO: aceita títulos e conceitos oficialmente localizados.
      <p>INVARIANTES DO DOMÍNIO: Terra, Século Universal e Princesa são PT-BR.
      <p>COMPORTAMENTO EM CASO DE FALHA: falso positivo reprova o teste.
  - RevisarLoreUseCaseRevisorFakeIT.java
      PROPÓSITO DE NEGÓCIO: prova que o {@code RevisarLoreUseCase} está integrado à
      porta LLM própria da Revisão de Lore — usando um fake da {@link RevisorLoreLlmPort} —,
      validando o portão de disponibilidade sem depender do LM Studio real.
      <p>INVARIANTES DO DOMÍNIO: o {@code GerenciadorPromptRevisaoLore} real (com os
      contextos registrados) valida o contexto; a porta fake decide a disponibilidade.
      <p>COMPORTAMENTO EM CASO DE FALHA: LLM indisponível deve abortar a sessão com
      {@link RevisaoLoreException} antes de qualquer processamento de arquivo.
  - RevisarLoreUseCaseTest.java
      PROPÓSITO DE NEGÓCIO: protege as fronteiras de segurança e os desfechos da
      opção 7 contra regressões.
      <p>INVARIANTES DO DOMÍNIO: testes não acessam LLM ou arquivos reais do usuário.
      <p>COMPORTAMENTO EM CASO DE FALHA: qualquer quebra de contrato reprova a suíte.
  - ValidadorCandidatoLoreServiceTest.java
      PROPÓSITO DE NEGÓCIO: reproduz as propostas reais que a opção 7 deve aceitar
      ou bloquear antes de sobrescrever uma legenda.
      <p>INVARIANTES DO DOMÍNIO: somente termo presente no EN e na lore pode mudar.
      <p>COMPORTAMENTO EM CASO DE FALHA: qualquer regressão reprova a suíte.

[PASTA] src/test/java/org/traducao/projeto/revisaoLore/contexto/
  - ContextosRevisaoLoreCatalogoTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/revisaoLore/infrastructure/adapters/
  - NormalizadorRespostaRevisaoLoreTest.java
      PROPÓSITO DE NEGÓCIO: caracteriza a normalização das respostas do LLM de lore,
      garantindo a paridade com o comportamento efetivo anterior — remoção de
      raciocínio, cerca Markdown e rótulos, seleção de uma única linha e preservação
      dos marcadores {@code [[TAGn]]}.
      <p>INVARIANTES DO DOMÍNIO: nenhuma rede; apenas lógica de string pura.
      <p>COMPORTAMENTO EM CASO DE FALHA: divergência de normalização reprova a suíte.
  - RevisorLoreLlmAdapterCaracterizacaoTest.java
      PROPÓSITO DE NEGÓCIO: caracteriza a stack LLM própria da Revisão de Lore contra
      um servidor HTTP local, garantindo paridade com o comportamento efetivo anterior
      (payload, normalização, respostas inválidas e política de retry) sem depender do
      LM Studio real.
      <p>INVARIANTES DO DOMÍNIO: nenhuma rede externa; pausas de retry reduzidas para
      determinismo.
      <p>COMPORTAMENTO EM CASO DE FALHA: desvio de payload/normalização/retry reprova a suíte.
  - RevisorLoreLlmCdiIT.java
      PROPÓSITO DE NEGÓCIO: prova, com o container Arc real, que a stack LLM própria
      da Revisão de Lore está cabeada por CDI e que o {@code RevisarLoreUseCase} passou
      a depender da porta própria — não mais da {@code LlmPort} da Tradução Local.
      <p>INVARIANTES DO DOMÍNIO: {@link RevisorLoreLlmPort} resolve para
      {@link RevisorLoreLlmAdapter}; as propriedades próprias refletem os defaults efetivos.
      <p>COMPORTAMENTO EM CASO DE FALHA: bean ausente ou default divergente reprova a suíte.
  - RevisorLoreLlmDisponibilidadeTest.java
      PROPÓSITO DE NEGÓCIO: caracteriza a verificação de disponibilidade do LLM da
      Revisão de Lore — API estendida {@code /api/v0/models}, preferência pelo modelo
      configurado, fallback para {@code /v1/models}, catálogo vazio, modelo ausente e
      servidor inacessível — sem depender do LM Studio real.
      <p>INVARIANTES DO DOMÍNIO: nenhuma rede externa; servidor HTTP local determinístico.
      <p>COMPORTAMENTO EM CASO DE FALHA: divergência de status reprova a suíte.
  - ServidorLlmDeTeste.java
      PROPÓSITO DE NEGÓCIO: servidor HTTP local determinístico que emula o endpoint
      OpenAI-compatible do LLM (LM Studio) para caracterizar a stack de Revisão de
      Lore sem depender de rede externa ou do LM Studio real.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Roteia {@code /v1/chat/completions}, {@code /v1/models} e {@code /api/v0/models}.</li>
      <li>Conta chamadas ao chat e captura cada corpo recebido, para asserção de payload e retry.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Sem resposta de chat enfileirada, repete a última configurada; encerra a porta ao fechar.

[PASTA] src/test/java/org/traducao/projeto/revisaoLore/infrastructure/
  - RevisaoLoreAuditoriaCacheTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/telemetria/
  - IsolamentoArtefatosTest.java
      PROPÓSITO DE NEGÓCIO: prova, exercitando o caminho real de persistência de
      relatório e telemetria de operação (o mesmo usado por revisão, correção,
      lore etc.), que uma execução sob o perfil de teste NÃO grava nos diretórios
      operacionais versionados ({@code relatorios/}, {@code logs/}) e sim na árvore
      descartável redirecionada por {@link DiretorioBaseKronos}. É o guard que
      impede a reaparição dos resíduos {@code relatorios/junit-*}.
      
      <p>INVARIANTES DO DOMÍNIO: a suíte roda com {@code kronos.dir.base} apontando
      para {@code build/tmp/kronos-tests} (ver build.gradle), portanto os caminhos
      relativos crus ({@code Path.of("relatorios")}, {@code Path.of("logs")})
      continuam apontando para os diretórios reais e servem de referência do que
      NÃO pode ser tocado.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: qualquer escrita real dispara asserção
      JUnit, sinalizando regressão do isolamento.
  - TelemetriaConsolidacaoTest.java
      PROPÓSITO DE NEGÓCIO: caracteriza o agregador CQRS read-only do Painel Unificado,
      consolidando o histórico legado ({@code telemetria_compartilhada.json}) com a
      telemetria própria da Tradução Local ({@code telemetria_traducao.json}) de forma
      determinística e sem sobrepor contadores.
      <p>INVARIANTES DO DOMÍNIO: raiz isolada em {@code @TempDir}; o novo arquivo vence
      o legado por chave; contadores somados sem overlap; sem importar o pacote traducao.
      <p>COMPORTAMENTO EM CASO DE FALHA: divergência ou exceção reprova a suíte.
  - TelemetriaDatasetPropertiesTest.java
      PROPÓSITO DE NEGÓCIO: valida a configuração segura da publicação do dataset.
      
      <p>INVARIANTES DO DOMÍNIO: ambiente e detecção automática permanecem ativos,
      sem propriedade manual de GPU capaz de misturar máquinas.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: configuração divergente impede a suíte de
      integração de aprovar o empacotamento da aplicação.
  - TelemetriaDatasetServiceTest.java
      O dataset público carrega SOMENTE métricas: sem textos de legenda (avisos
      viram contagem), sem caminhos de máquina (detalhe descartado, episódio
      reduzido ao nome do arquivo). Estes testes são o contrato de anonimização
      declarado no README do repositório do dataset.
  - TelemetriaServiceCompactacaoTest.java
      Teto de avisos por episódio no JSON canônico: sem ele, os textos de aviso
      dominavam o arquivo de telemetria (21,9 mil avisos ≈ 85% dos 3,5MB medidos
      em 2026-07-09) e eram regravados a cada registro.
  - TelemetriaServiceRevisaoLoreTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/traducaoCorrige/application/
  - ClassificadorEntradaCacheServiceTest.java
      PROPÓSITO DE NEGÓCIO: cobre a fronteira que impede o menu de apagar termos
      legítimos da lore e garante que lacunas/fallbacks reais sejam reparáveis.
      
      <p>INVARIANTES DO DOMÍNIO: a decisão deriva da lore ativa e não de uma lista
      fixa de um anime; expressões inglesas em Title Case continuam sendo falhas.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: cada cenário retorna status explícito, sem
      depender de exceção ou igualdade ambígua.
  - LimparCacheUseCaseTest.java
      PROPÓSITO DE NEGÓCIO: testa o fluxo completo de limpeza sobre a pasta cache,
      incluindo proveniência, lore, backup, auditoria e formato versionado.
      
      <p>INVARIANTES DO DOMÍNIO: fallback inglês é invalidado, termo de lore é
      preservado e cache legado sem seleção não sofre alteração destrutiva.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: o resultado acusa falha e o arquivo
      original permanece byte a byte igual.

[PASTA] src/test/java/org/traducao/projeto/traducaoCorrige/domain/
  - ResultadoManutencaoCacheTest.java
      PROPÓSITO DE NEGÓCIO: garante que o painel diferencie conclusão integral de
      uma execução tecnicamente estável que ainda deixou itens sem correção.
      
      <p>INVARIANTES DO DOMÍNIO: itens detectados e não corrigidos são pendências,
      não sucesso completo nem falha técnica.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: status ou contagem divergente reprova o teste.

[PASTA] src/test/java/org/traducao/projeto/traducaoKaraoke/application/
  - ClassificadorLetraKaraokeServiceTest.java
      O caso central do módulo: cantor japonês mistura inglês na letra.
      Estilo real do 86: "ED-ROM".
  - TraduzirKaraokeUseCaseTest.java
      (sem cabecalho explicativo)

[PASTA] src/test/java/org/traducao/projeto/traducao/application/
  - ProcessarArquivoUseCaseCaracterizacaoTest.java
      (sem cabecalho explicativo)
  - ProcessarArquivoUseCaseGuardTest.java
      PROPÓSITO DE NEGÓCIO: protege por regressão as decisões que impedem o tradutor
      de publicar linhas ASS suspeitas ou substituir uma legenda sem autorização.
      
      <p>INVARIANTES DO DOMÍNIO: entradas protegidas permanecem bloqueadas, a chave
      de liberação escolhe o destino correto e toda sobrescrita preserva um backup.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: qualquer desvio interrompe a suíte antes de
      o comportamento inseguro alcançar arquivos reais.
  - ProcessarEpisodioUseCaseAlucinacaoCaracterizacaoTest.java
      PROPÓSITO DE NEGÓCIO: caracteriza o comportamento REALMENTE observável do
      {@link ProcessarEpisodioUseCase} quando o LLM devolve uma fala isolada que o
      validador rejeita como alucinação — travando o contrato antes da E8b para que a
      extração de {@code AlucinacaoDetectadaException} para o peer {@code qualidadeTraducao}
      não altere o fluxo em silêncio.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>A resposta rejeitada dispara nova tentativa da fala isolada (retry com
      temperatura variável), não a propagação imediata da exceção.</li>
      <li>Cada rejeição é contabilizada em {@code registrarRespostaTraducaoRejeitada}.</li>
      <li>Esgotadas as tentativas, o fallback mantém a fala ORIGINAL — sem sucesso
      falso: {@code registrarFalhaTraducaoRecuperada} nunca é chamado.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Este teste NÃO afirma que a exceção alcança o catch externo de

[PASTA] src/test/java/org/traducao/projeto/traducao/arquitetura/
  - FronteiraInboundArchTest.java
      PROPOSITO DE NEGOCIO: congela a fronteira funcional INBOUND da fatia vertical
      Traducao Local (org.traducao.projeto.traducao) — dependencias outra-fatia ->
      traducao. Contraparte do fitness OUTBOUND (FronteiraTraducaoArchTest); as regras
      da C2 permanecem separadas la.
      
      <p>DUAS MEDIDAS COMPLEMENTARES (baseline auditada da FASE E):
      <ul>
      <li>Fitness principal (ArchUnit/bytecode): pre-E1 = 149, pos-E1 = 147, pos-E2 = 144, pos-E3b = 138, pos-E3c = 134, pos-E4a = 128, pos-E4b = 122, pos-E5a = 83, pos-E5c = 71, pos-E6 = 55, pos-E7b = 47, pos-E8a = 39, pos-E8b = 28, pos-E8c = 15, pos-E8c1 = 13, pos-E8d = 0. Mesmo
      rigor do OUTBOUND; fonte de verdade da fronteira.</li>
      <li>Inventario textual complementar (imports do fonte): pre-E1 = 150, pos-E1 = 148, pos-E2 = 145, pos-E3b = 139, pos-E3c = 135, pos-E4a =
      129, pos-E4b = 123, pos-E5a = 85, pos-E5c = 73, pos-E6 = 57, pos-E7b = 49, pos-E8a = 41, pos-E8b = 28, pos-E8c = 15, pos-E8c1 = 13, pos-E8d = 0. Impede o surgimento silencioso de novos imports outra-fatia -> traducao,
      inclusive tipos usados apenas em clausulas catch (que o ArchUnit 1.4.2 nao
      registra no grafo).</li>
      </ul>
      
      <p>Nota E7b: as oito arestas outras-fatias -> {@code GerenciadorContexto} sairam do
      INBOUND (55->47 bytecode / 57->49 texto) porque o manager migrou para o peer de topo
  - FronteiraTraducaoArchTest.java
      PROPÓSITO DE NEGÓCIO: congela a fronteira funcional da fatia vertical Tradução
      Local ({@code org.traducao.projeto.traducao}). É a Camada A (estática, por
      bytecode) do harness de fitness da FASE D: prova, a cada build, que a Tradução
      Local só depende de outras fatias por meio de uma allowlist **estrita por
      aresta exata** (FQN de origem → FQN de destino), que encolhe subfase a subfase
      até restar somente o débito dos três controllers bloqueados para a C2. Analisa
      dependências no bytecode, alcançando o que o import textual não mostra
      (usos totalmente qualificados no corpo, campos, construtores, herança, genéricos).
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li><b>Baseline dupla (histórico D0)</b>: a auditoria por import textual
      encontrou <b>15</b> arestas funcionais; o bytecode revelava <b>17</b> — as
      2 extras eram usos por FQN no corpo ({@code TraducaoController → LlmTelemetria}
      e {@code TelemetriaController → TelemetriaDatasetService}).</li>
      <li><b>Após D-Ext</b>: eliminadas as 2 arestas {@code RestClientConfig →
      ExtratorVideoPort/ExtratorStrategy} (producers movidos para
      {@code legendasExtracao.ExtracaoBeansConfig}).</li>
      <li><b>Após D-Lore</b>: eliminada a aresta {@code LlmClientAdapter →
  - GrafoCdiTraducaoIT.java
      PROPÓSITO DE NEGÓCIO: Camada B (runtime CDI) do harness de fitness da FASE D.
      Sobe o container Arc com {@code @QuarkusTest} e caracteriza — sem alterar
      produção — o grafo de injeção que a análise estática não alcança: o
      {@link ObjectMapper} efetivamente resolvido, as coleções agregadas de extração
      e o dispatcher do modo CLI. Fixa o baseline homologado antes das subfases D.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Existe exatamente um {@link ObjectMapper} injetável (sem ambiguidade
      impeditiva no baseline).</li>
      <li>Os producers de {@code List<ExtratorVideoPort>} e {@code List<ExtratorStrategy>}
      resolvem coleções não vazias (mesmo contrato consumido por
      {@code ExtrairLegendaUseCase}).</li>

[PASTA] src/test/java/org/traducao/projeto/traducao/domain/
  - NormalizadorNomeEpisodioTest.java
      PROPÓSITO DE NEGÓCIO: fixa a semântica da normalização proprietária da chave de
      episódio (proprietário único da Tradução Local), cobrindo espaços, caixa,
      Unicode, extensão, números e nomes semelhantes.
      <p>INVARIANTES DO DOMÍNIO: normalização conservadora e determinística; não funde
      episódios distintos.
      <p>COMPORTAMENTO EM CASO DE FALHA: divergência reprova a suíte.
  - StatusLoteTraducaoTest.java
      Cobre a consolidação do status do lote a partir dos status por arquivo —
      o núcleo do fix "não mostrar sucesso quando houve falhas".

[PASTA] src/test/java/org/traducao/projeto/traducao/infrastructure/adapters/
  - LlmClientAdapterRespostaRevisaoTest.java
      PROPÓSITO DE NEGÓCIO: garante que respostas do Tower/Mistral com raciocínio
      ou formatação auxiliar entreguem somente a fala final à revisão de legendas.
      
      <p>INVARIANTES DO DOMÍNIO: todos os marcadores ASS esperados permanecem na
      saída e explicações do modelo nunca entram na legenda.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: resposta incompatível produz texto vazio,
      obrigando o cliente a tentar novamente em vez de publicar estrutura quebrada.
  - LoreAtivaContextoAdapterTest.java
      PROPÓSITO DE NEGÓCIO: prova que {@link LoreAtivaContextoAdapter} é uma delegação pura
      ao {@link GerenciadorContexto} — o único ponto de composição que liga a porta
      {@code LoreAtivaPort} do peer de qualidade à fonte real de contexto, sem alterar o que
      o gerenciador entrega.
      
      <p>INVARIANTES DO DOMÍNIO: para qualquer estado do gerenciador (sem contexto ou com
      contexto ativo) a saída do adapter é IGUAL à do gerenciador — mesmos termos, mesma
      lore, sem normalização, filtragem ou substituição.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: qualquer divergência entre adapter e gerenciador
      reprova o teste. A comparação é sempre contra a saída do próprio gerenciador, de modo
      que o teste não congela o fallback interno do {@link ContextoPrompt} como regra.

[PASTA] src/test/java/org/traducao/projeto/traducao/infrastructure/config/
  - ConfiguracaoSimplesE3bIT.java
      PROPÓSITO DE NEGÓCIO: caracteriza a resolução das quatro chaves de valor simples
      migradas na subfase E3b (tradutor.diretorio-entrada, tradutor.idioma-original,
      tradutor.idioma-traduzido, tradutor.diretorio-cache) via {@code @ConfigProperty}
      {@code Optional<String>}, blindando o novo acoplamento antes de remover
      {@code TradutorProperties} dos seis consumidores.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Chave presente → {@code Optional.of(valor)}; a sobrescrita de perfil vence o
      {@code application.yml} (prova de override).</li>
      <li>Chave ausente ou vazia ("") → {@code Optional.empty()} (SmallRye colapsa valor
      vazio), e o fallback de domínio local (.orElse) aplica o default.</li>
      <li>Valor só com espaços → o filtro {@code !isBlank()} força o default de domínio
      (idioma/entrada), sem depender de trimming do SmallRye.</li>
      <li>Nenhum {@code defaultValue} é usado na injeção; o default é sempre do consumidor.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Qualquer divergência na resolução ou no fallback reprova o teste, sinalizando a
      quebra de paridade com o comportamento pré-E3b.
  - ParidadeBindingEstilosIT.java
      PASSO 1 da E3c — caracterizacao de paridade de binding (cenarios PREENCHIDO e AUSENTE).
      Observa o comportamento REAL de dois bindings sobre a MESMA chave
      {@code tradutor.estilos-ignorados}:
      <ul>
      <li>Spring {@code @ConfigurationProperties} (TradutorProperties.estilosIgnorados());</li>
      <li>SmallRye {@code @ConfigProperty Optional<List<String>>} + fallback do futuro produtor.</li>
      </ul>
      O cenario LISTA VAZIA EXPLICITA fica em {@code ParidadeBindingVazioIT} (perfil dedicado).
  - ParidadeBindingVazioIT.java
      PASSO 1 da E3c — cenario LISTA VAZIA EXPLICITA. Perfil de teste sobrescreve
      {@code tradutor.estilos-ignorados} para vazio e OBSERVA o que cada binding produz,
      para o gate de divergencia (decisao 6: ausente != vazia).
      
      <p>Este teste NAO afirma um resultado esperado rigido: ele IMPRIME e registra o
      estado real de ambos os bindings para ratificacao. As assercoes apenas travam a
      comparacao Spring-vs-SmallRye (se divergirem, o gate dispara com evidencia).
  - ParidadeResolucaoCaminhoE4bTest.java
      PROPÓSITO DE NEGÓCIO: gate de paridade da subfase E4b. Congela, ANTES de destacar
      os três CLIs externos (Extrator/Analisador/Remuxer) de {@code PastasExecucao} e
      {@code TradutorProperties}, o comportamento legado de resolução do diretório de
      saída que esses CLIs herdam hoje — de modo que a lógica inline que passará a viver
      em cada CLI possa ser provada equivalente.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>A normalização por {@code trim} da entrada e da saída ocorre em
      {@link PastasExecucao#configurar(String, String, String, TradutorProperties)},
      NÃO em {@link TradutorProperties#resolverDiretorioSaida()} (que apenas decide
      passthrough vs. fallback sobre valores já aparados).</li>
      <li>Composto legado (o que o CLI enxerga): saída ausente/vazia/blank ⇒
      {@code Path.of(entrada.trim()).resolve("traducao_ptbr")}; saída válida ⇒
      {@code Path.of(saida.trim())}.</li>
      <li>Nenhum dos três CLIs lê o diretório de cache de volta; a política de cache
      não entra nesta paridade.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Qualquer divergência entre o composto legado e a fórmula esperada reprova o teste —
      sinal de gate: NÃO prosseguir com a migração dos CLIs.

[PASTA] src/test/java/org/traducao/projeto/traducao/infrastructure/telemetria/
  - TelemetriaTraducaoAdapterTest.java
      PROPÓSITO DE NEGÓCIO: caracteriza o adapter que grava a telemetria própria da
      Tradução Local — deduplicação por episódio normalizado (mais recente vence),
      persistência dos quatro contadores e preservação de arquivo corrompido.
      <p>INVARIANTES DO DOMÍNIO: raiz operacional isolada em {@code @TempDir}; nenhuma
      rede; escrita atômica.
      <p>COMPORTAMENTO EM CASO DE FALHA: divergência reprova a suíte.

[PASTA] src/test/java/org/traducao/projeto/traducao/presentation/
  - TradutorCLIAlucinacaoCaracterizacaoTest.java
      PROPÓSITO DE NEGÓCIO: caracteriza o tratamento que o {@link TradutorCLI} dá a uma
      {@code AlucinacaoDetectadaException} lançada ao processar um arquivo — hoje herdado
      do {@code catch (TradutorException)}. Trava o contrato antes da E8b: mensagem de
      falha, contagem, lista de arquivos com falha e continuidade do lote.
      
      <h2>Invariantes do domínio</h2>
      <ul>
      <li>Uma falha por alucinação num arquivo é contabilizada como falha e não aborta
      o processamento dos demais (continuidade do lote).</li>
      <li>A mensagem exibida usa o formato do ramo crítico ("[ FAIL ] Falha em X: msg"),
      preservando a mensagem original da exceção.</li>
      <li>O relatório final reflete 0 sucessos, N falhas e lista os arquivos com falha.</li>
      </ul>
      
      <h2>Comportamento em caso de falha</h2>
      Após o reparenting da E8b, {@code AlucinacaoDetectadaException} deixa de ser
      {@code TradutorException}; o multi-catch {@code (TradutorException | AlucinacaoDetectadaException)}

[PASTA] src/test/java/org/traducao/projeto/traducao/presentation/web/
  - ConsoleRedirectorTest.java
      Teste de regressão para o bug pós-migração Spring Boot -> Quarkus: o
      console web parou de exibir logs de sucesso/alerta porque
      {@link ConsoleRedirector} (um bean cujo construtor chamava
      {@code System.setOut}) nunca era instanciado pelo CDI/ARC, já que nada o
      injetava em lugar nenhum. Sem o redirecionamento ativo, nada que os use
      cases imprimem com {@code System.out.println} chega ao
      {@code LogStreamService} (SSE) nem ao espelho em arquivo.
      <p>
      Este teste falha sem o fix (em {@code @Observes StartupEvent}) e passa com
      ele, pois depende exclusivamente do bean ter sido ativado automaticamente
      na subida do Quarkus — nenhuma injeção explícita de
      {@code ConsoleRedirector} é feita aqui.
  - LogStreamServiceTest.java
      Sem nenhum SSE client conectado, {@code publicarLog} ainda deve persistir a
      linha em {@code logs/console-web.log} (espelho em disco usado por
      {@link ConsoleRedirector} e pelos consoles web). Isso é o que prova que o
      pipeline de publicação/persistência em arquivo funciona independente de
      haver navegador conectado via SSE.

[PASTA] src/test/java/org/traducao/projeto/trocaTipoLegenda/application/
  - AuditoriaFontesServiceTest.java
      Default: Arial (Unicode Seguro)
  - TrocaTipoLegendaUseCaseTest.java
      PROPÓSITO DE NEGÓCIO: valida auditoria e substituição de fontes sem acessar
      backups ou relatórios reais do projeto.
      <p>INVARIANTES DO DOMÍNIO: todos os artefatos ficam sob diretórios temporários.
      <p>COMPORTAMENTO EM CASO DE FALHA: qualquer acesso à raiz real reprova os testes.


================================================================================
 FIM DO MAPA
================================================================================
