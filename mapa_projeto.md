================================================================================
 MAPA ESTRUTURAL DO PROJETO - TRACKER ANIMES
================================================================================
 Raiz do repositorio      : traducao_animes_llm_local_quarkus
 Pastas mapeadas          : 241
 Arquivos (na arvore)     : 516
 Arquivos-fonte indexados : 413  (.java: 413 | .py: 0)
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
├── downloads/
│   └── plano-mapas-saas.html
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
│   │   │               │   │   └── adapters/
│   │   │               │   │       ├── AniListApiClientAdapter.java
│   │   │               │   │       ├── JikanApiClientAdapter.java
│   │   │               │   │       └── TmdbApiClientAdapter.java
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
│   │   │               ├── config/
│   │   │               │   ├── AppConfig.java
│   │   │               │   ├── ExecucaoCli.java
│   │   │               │   └── ModoExecucaoStartup.java
│   │   │               ├── core/
│   │   │               │   ├── exception/
│   │   │               │   │   ├── web/
│   │   │               │   │   │   └── BasePipelineExceptionMapper.java
│   │   │               │   │   └── BasePipelineException.java
│   │   │               │   ├── execucao/
│   │   │               │   │   └── FilaExecucaoPipeline.java
│   │   │               │   ├── io/
│   │   │               │   │   └── DiretorioBaseKronos.java
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
│   │   │               │   │       └── ExtratorProperties.java
│   │   │               │   └── presentation/
│   │   │               │       ├── ui/
│   │   │               │       │   ├── ConsoleExtratorLogger.java
│   │   │               │       │   └── TabelaExtracaoRenderer.java
│   │   │               │       └── ExtratorCLI.java
│   │   │               ├── mapaProjeto/
│   │   │               │   ├── application/
│   │   │               │   │   ├── GeradorMapaProjetoUseCase.java
│   │   │               │   │   └── MapeadorDiretorioUseCase.java
│   │   │               │   ├── domain/
│   │   │               │   │   └── exceptions/
│   │   │               │   │       └── MapaProjetoException.java
│   │   │               │   └── presentation/
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
│   │   │               │   │   │   └── ProvedorPromptRevisaoLore.java
│   │   │               │   │   ├── EntradaAuditoriaRevisaoLore.java
│   │   │               │   │   ├── LogEventoRevisaoLore.java
│   │   │               │   │   ├── ResultadoDeteccaoLore.java
│   │   │               │   │   ├── ResultadoRevisaoLore.java
│   │   │               │   │   ├── RevisaoLoreRelatorioJson.java
│   │   │               │   │   └── StatusRevisaoLore.java
│   │   │               │   ├── infrastructure/
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
│   │   │               │   └── TelemetriaService.java
│   │   │               ├── traducao/
│   │   │               │   ├── application/
│   │   │               │   │   ├── DetectorEfeitoKaraokeService.java
│   │   │               │   │   ├── DetectorTraducaoIdenticaService.java
│   │   │               │   │   ├── ProcessarArquivoUseCase.java
│   │   │               │   │   ├── ProcessarEpisodioUseCase.java
│   │   │               │   │   ├── ProtecaoLegendaAssService.java
│   │   │               │   │   └── ValidadorTraducaoService.java
│   │   │               │   ├── contexto/
│   │   │               │   │   ├── danmachi/
│   │   │               │   │   │   ├── ContextoDanMachi.java
│   │   │               │   │   │   ├── ContextoDanMachiOrion.java
│   │   │               │   │   │   ├── ContextoDanMachiS1.java
│   │   │               │   │   │   ├── ContextoDanMachiS2.java
│   │   │               │   │   │   ├── ContextoDanMachiS3.java
│   │   │               │   │   │   ├── ContextoDanMachiS4.java
│   │   │               │   │   │   ├── ContextoDanMachiS5.java
│   │   │               │   │   │   └── ContextoDanMachiSwordOratoria.java
│   │   │               │   │   ├── eightsix/
│   │   │               │   │   │   └── Contexto86.java
│   │   │               │   │   ├── evangelion/
│   │   │               │   │   │   ├── ContextoEvangelion111.java
│   │   │               │   │   │   ├── ContextoEvangelion222.java
│   │   │               │   │   │   ├── ContextoEvangelion3010.java
│   │   │               │   │   │   ├── ContextoEvangelion333.java
│   │   │               │   │   │   └── ContextoEvangelionTV.java
│   │   │               │   │   ├── guiltycrown/
│   │   │               │   │   │   └── ContextoGuiltyCrown.java
│   │   │               │   │   ├── gundam/
│   │   │               │   │   │   ├── chars/
│   │   │               │   │   │   │   └── ContextoCharsCounterattack.java
│   │   │               │   │   │   ├── msteam/
│   │   │               │   │   │   │   └── ContextoGundam08thMSTeam.java
│   │   │               │   │   │   ├── reconguista/
│   │   │               │   │   │   │   └── ContextoGundamReconguista.java
│   │   │               │   │   │   ├── stardust/
│   │   │               │   │   │   │   └── ContextoGundam0083.java
│   │   │               │   │   │   ├── warInpocket/
│   │   │               │   │   │   │   └── ContextoWarInPocket.java
│   │   │               │   │   │   ├── zeta/
│   │   │               │   │   │   │   └── ContextoGundamZeta.java
│   │   │               │   │   │   ├── zz/
│   │   │               │   │   │   │   └── ContextoGundamZZ.java
│   │   │               │   │   │   ├── ContextoGundam0079.java
│   │   │               │   │   │   ├── ContextoGundamF91.java
│   │   │               │   │   │   ├── ContextoGundamHathaway.java
│   │   │               │   │   │   ├── ContextoGundamNT.java
│   │   │               │   │   │   ├── ContextoGundamOrigin.java
│   │   │               │   │   │   ├── ContextoGundamSEED.java
│   │   │               │   │   │   ├── ContextoGundamSEEDAstray.java
│   │   │               │   │   │   ├── ContextoGundamSEEDDestiny.java
│   │   │               │   │   │   ├── ContextoGundamSEEDFreedom.java
│   │   │               │   │   │   ├── ContextoGundamSEEDStargazer.java
│   │   │               │   │   │   ├── ContextoGundamUnicorn.java
│   │   │               │   │   │   └── ContextoGundamVictory.java
│   │   │               │   │   ├── macross/
│   │   │               │   │   │   ├── ContextoMacross2.java
│   │   │               │   │   │   ├── ContextoMacross7.java
│   │   │               │   │   │   ├── ContextoMacross7Encore.java
│   │   │               │   │   │   ├── ContextoMacross7Filme.java
│   │   │               │   │   │   ├── ContextoMacross7Filmes.java
│   │   │               │   │   │   ├── ContextoMacrossAnime.java
│   │   │               │   │   │   ├── ContextoMacrossDelta.java
│   │   │               │   │   │   ├── ContextoMacrossDeltaFilme1.java
│   │   │               │   │   │   ├── ContextoMacrossDeltaFilme2.java
│   │   │               │   │   │   ├── ContextoMacrossDeltaFilmes.java
│   │   │               │   │   │   ├── ContextoMacrossDynamite7.java
│   │   │               │   │   │   ├── ContextoMacrossDYRL.java
│   │   │               │   │   │   ├── ContextoMacrossFilme1.java
│   │   │               │   │   │   ├── ContextoMacrossFilme2.java
│   │   │               │   │   │   ├── ContextoMacrossFrontier.java
│   │   │               │   │   │   ├── ContextoMacrossFrontierFilme1.java
│   │   │               │   │   │   ├── ContextoMacrossFrontierFilme2.java
│   │   │               │   │   │   ├── ContextoMacrossFrontierFilmes.java
│   │   │               │   │   │   ├── ContextoMacrossPlus.java
│   │   │               │   │   │   └── ContextoMacrossZero.java
│   │   │               │   │   ├── sidonia/
│   │   │               │   │   │   ├── ContextoKnightsOfSidonia.java
│   │   │               │   │   │   └── ContextoSidoniaFilme.java
│   │   │               │   │   ├── ContextoPrompt.java
│   │   │               │   │   └── RegrasConcordanciaPtBr.java
│   │   │               │   ├── domain/
│   │   │               │   │   ├── exceptions/
│   │   │               │   │   │   ├── AlucinacaoDetectadaException.java
│   │   │               │   │   │   ├── ArquivoLegendaException.java
│   │   │               │   │   │   ├── ContextoNaoEncontradoException.java
│   │   │               │   │   │   ├── DivergenciaLinhasException.java
│   │   │               │   │   │   ├── EntradaJaTraduzidaException.java
│   │   │               │   │   │   ├── LlmFalhaComunicacaoException.java
│   │   │               │   │   │   ├── LmStudioOfflineException.java
│   │   │               │   │   │   ├── RespostaLlmVaziaException.java
│   │   │               │   │   │   ├── TraducaoParcialException.java
│   │   │               │   │   │   └── TradutorException.java
│   │   │               │   │   ├── legenda/
│   │   │               │   │   │   ├── DocumentoLegenda.java
│   │   │               │   │   │   └── EventoLegenda.java
│   │   │               │   │   ├── ports/
│   │   │               │   │   │   ├── MistralPort.java
│   │   │               │   │   │   └── ProvedorContexto.java
│   │   │               │   │   ├── Lote.java
│   │   │               │   │   ├── ResultadoTraducaoArquivo.java
│   │   │               │   │   ├── StatusArquivoTraducao.java
│   │   │               │   │   ├── StatusLlm.java
│   │   │               │   │   ├── StatusLoteTraducao.java
│   │   │               │   │   └── TraducaoLote.java
│   │   │               │   ├── infrastructure/
│   │   │               │   │   ├── adapters/
│   │   │               │   │   │   └── MistralClientAdapter.java
│   │   │               │   │   ├── config/
│   │   │               │   │   │   ├── LlmProperties.java
│   │   │               │   │   │   ├── RestClientConfig.java
│   │   │               │   │   │   └── TradutorProperties.java
│   │   │               │   │   ├── contexto/
│   │   │               │   │   │   └── GerenciadorContexto.java
│   │   │               │   │   ├── dtos/
│   │   │               │   │   │   └── RecordsMistral.java
│   │   │               │   │   ├── http/
│   │   │               │   │   │   └── JsonHttpClient.java
│   │   │               │   │   └── legenda/
│   │   │               │   │       ├── EscritorLegendaAss.java
│   │   │               │   │       ├── EscritorLegendaSrt.java
│   │   │               │   │       ├── LeitorLegendaAss.java
│   │   │               │   │       ├── LeitorLegendaSrt.java
│   │   │               │   │       └── MascaradorTags.java
│   │   │               │   ├── presentation/
│   │   │               │   │   ├── ui/
│   │   │               │   │   │   ├── AnsiCores.java
│   │   │               │   │   │   ├── ConsoleEntrada.java
│   │   │               │   │   │   ├── ConsoleUILogger.java
│   │   │               │   │   │   ├── PastasExecucao.java
│   │   │               │   │   │   └── TabelaTraducaoRenderer.java
│   │   │               │   │   ├── web/
│   │   │               │   │   │   ├── AnaliseMidiaController.java
│   │   │               │   │   │   ├── BrowserLauncher.java
│   │   │               │   │   │   ├── ConsoleRedirector.java
│   │   │               │   │   │   ├── ContextoResponse.java
│   │   │               │   │   │   ├── CorrecaoCacheController.java
│   │   │               │   │   │   ├── DialogoArquivoController.java
│   │   │               │   │   │   ├── DocumentacaoController.java
│   │   │               │   │   │   ├── ExtracaoLegendaController.java
│   │   │               │   │   │   ├── ExtracaoRequest.java
│   │   │               │   │   │   ├── LlmStatusResponse.java
│   │   │               │   │   │   ├── LogStreamResource.java
│   │   │               │   │   │   ├── LogStreamService.java
│   │   │               │   │   │   ├── MapaController.java
│   │   │               │   │   │   ├── MapaResponse.java
│   │   │               │   │   │   ├── OperacaoRequest.java
│   │   │               │   │   │   ├── PipelineController.java
│   │   │               │   │   │   ├── PipelineWebSupport.java
│   │   │               │   │   │   ├── RemuxerController.java
│   │   │               │   │   │   ├── RemuxRequest.java
│   │   │               │   │   │   ├── RespostaPadrao.java
│   │   │               │   │   │   ├── RevisaoLegendasController.java
│   │   │               │   │   │   ├── TelemetriaController.java
│   │   │               │   │   │   ├── TelemetriaStreamResource.java
│   │   │               │   │   │   └── TraducaoController.java
│   │   │               │   │   └── TradutorCLI.java
│   │   │               │   └── Application.java
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
│       └── java/
│           └── org/
│               └── traducao/
│                   └── projeto/
│                       ├── analisadorMidia/
│                       │   ├── application/
│                       │   │   ├── AnalisarMidiaClassificacaoTest.java
│                       │   │   ├── AnalisarMidiaTelemetriaTest.java
│                       │   │   ├── LocalizadorVideosServiceTest.java
│                       │   │   └── TelemetriaMidiaMapperTest.java
│                       │   ├── domain/
│                       │   │   └── ResultadoAnaliseLoteSerializacaoTest.java
│                       │   └── infrastructure/
│                       │       └── adapters/
│                       │           └── FfprobeAdapterTest.java
│                       ├── apiDadosAnime/
│                       │   ├── application/
│                       │   │   └── ObterMetadataAnimeUseCaseTest.java
│                       │   └── infrastructure/
│                       │       └── adapters/
│                       │           └── AniListApiClientAdapterTest.java
│                       ├── auditorConteudoLegendas/
│                       │   ├── application/
│                       │   │   ├── regras/
│                       │   │   │   ├── RegraAlucinacaoQuebraLinhaTest.java
│                       │   │   │   ├── RegraDanoKaraokeTest.java
│                       │   │   │   ├── RegraEfeitoVazadoTest.java
│                       │   │   │   ├── RegraMetadadosAssTest.java
│                       │   │   │   └── RegraSincroniaEstilosTest.java
│                       │   │   ├── AuditorConteudoIntegridadeTest.java
│                       │   │   └── AuditorConteudoUseCaseTest.java
│                       │   └── support/
│                       │       └── AssAuditoriaFixtures.java
│                       ├── core/
│                       │   ├── exception/
│                       │   │   └── BasePipelineExceptionTest.java
│                       │   ├── execucao/
│                       │   │   └── FilaExecucaoPipelineTest.java
│                       │   └── io/
│                       │       └── DiretorioBaseKronosTest.java
│                       ├── correcaoLegendas/
│                       │   └── application/
│                       │       └── CorrigirLegendasUseCaseTest.java
│                       ├── legendasExtracao/
│                       │   ├── application/
│                       │   │   ├── ExtrairLegendaUseCaseTest.java
│                       │   │   └── ValidadorSaidaExtracaoTest.java
│                       │   └── infrastructure/
│                       │       └── adapters/
│                       │           ├── FfmpegAdapterTest.java
│                       │           └── MkvToolNixAdapterTest.java
│                       ├── mapaProjeto/
│                       │   └── application/
│                       │       └── GeradorMapaProjetoUseCaseTest.java
│                       ├── mcp/
│                       │   └── KronosMcpToolsTest.java
│                       ├── novoKaraoke/
│                       │   └── application/
│                       │       └── ConversorKaraokeUseCaseTest.java
│                       ├── raspagemCorrecao/
│                       │   ├── application/
│                       │   │   ├── CorrigirComGoogleUseCaseTest.java
│                       │   │   └── ProtetorTermosLoreServiceTest.java
│                       │   └── infrastructure/
│                       │       └── GoogleTranslateScraperTest.java
│                       ├── raspagemRevisao/
│                       │   └── application/
│                       │       ├── CorretorDeterministicoConcordanciaServiceTest.java
│                       │       ├── DetectorConcordanciaServiceTest.java
│                       │       ├── LeitorCacheReferenciaServiceTest.java
│                       │       ├── ResultadoRevisaoLegendasTest.java
│                       │       ├── RevisarCacheUseCaseTest.java
│                       │       ├── RevisarLegendasCacheIntegracaoTest.java
│                       │       ├── RevisarLegendasCacheSeguroTest.java
│                       │       ├── RevisarLegendasContextoTest.java
│                       │       ├── RevisarLegendasProtecaoMassaTest.java
│                       │       └── SincronizadorLegendaCacheServiceTest.java
│                       ├── remuxer/
│                       │   ├── application/
│                       │   │   ├── MapeadorMidiaServiceTest.java
│                       │   │   └── RemuxarLoteUseCaseTest.java
│                       │   └── infrastructure/
│                       │       └── adapters/
│                       │           └── MkvmergeAdapterTest.java
│                       ├── renomearArquivos/
│                       │   └── application/
│                       │       └── RenomeadorUseCaseTest.java
│                       ├── revisaoLore/
│                       │   ├── application/
│                       │   │   ├── DetectorTermosLoreServiceTest.java
│                       │   │   ├── RevisarLoreUseCaseTest.java
│                       │   │   └── ValidadorCandidatoLoreServiceTest.java
│                       │   ├── contexto/
│                       │   │   └── ContextosRevisaoLoreCatalogoTest.java
│                       │   └── infrastructure/
│                       │       └── RevisaoLoreAuditoriaCacheTest.java
│                       ├── telemetria/
│                       │   ├── IsolamentoArtefatosTest.java
│                       │   ├── TelemetriaDatasetPropertiesTest.java
│                       │   ├── TelemetriaDatasetServiceTest.java
│                       │   ├── TelemetriaServiceCompactacaoTest.java
│                       │   └── TelemetriaServiceRevisaoLoreTest.java
│                       ├── traducao/
│                       │   ├── application/
│                       │   │   ├── DetectorEfeitoKaraokeServiceTest.java
│                       │   │   ├── DetectorTraducaoIdenticaServiceTest.java
│                       │   │   ├── ProcessarArquivoUseCaseCaracterizacaoTest.java
│                       │   │   ├── ProcessarArquivoUseCaseGuardTest.java
│                       │   │   └── ValidadorTraducaoServiceTest.java
│                       │   ├── domain/
│                       │   │   └── StatusLoteTraducaoTest.java
│                       │   ├── infrastructure/
│                       │   │   ├── adapters/
│                       │   │   │   └── MistralClientAdapterRespostaRevisaoTest.java
│                       │   │   └── legenda/
│                       │   │       ├── EscritorLegendaAssTest.java
│                       │   │       ├── LeitorEscritorSrtTest.java
│                       │   │       └── MascaradorTagsTest.java
│                       │   └── presentation/
│                       │       └── web/
│                       │           ├── ConsoleRedirectorTest.java
│                       │           └── LogStreamServiceTest.java
│                       ├── traducaoCorrige/
│                       │   ├── application/
│                       │   │   ├── ClassificadorEntradaCacheServiceTest.java
│                       │   │   └── LimparCacheUseCaseTest.java
│                       │   └── domain/
│                       │       └── ResultadoManutencaoCacheTest.java
│                       ├── traducaoKaraoke/
│                       │   └── application/
│                       │       ├── ClassificadorLetraKaraokeServiceTest.java
│                       │       └── TraduzirKaraokeUseCaseTest.java
│                       ├── trocaTipoLegenda/
│                       │   └── application/
│                       │       ├── AuditoriaFontesServiceTest.java
│                       │       └── TrocaTipoLegendaUseCaseTest.java
│                       ├── ApiControllerTest.java
│                       ├── ApiEndpointsTest.java
│                       └── WebInterfaceTest.java
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
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/analisadorMidia/presentation/ui/
  - ConsoleAnalisadorLogger.java
      (sem cabecalho explicativo)

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

[PASTA] src/main/java/org/traducao/projeto/config/
  - AppConfig.java
      (sem cabecalho explicativo)
  - ExecucaoCli.java
      Contrato para modos de execucao em linha de comando (substituto do CommandLineRunner do Spring Boot).
  - ModoExecucaoStartup.java
      Dispara o modo CLI configurado em {@code app.modo}. No modo WEB nenhuma CLI e executada.

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
  - ExtratorProperties.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/legendasExtracao/presentation/
  - ExtratorCLI.java
      (sem cabecalho explicativo)

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
      Determina a raiz a ser mapeada

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

[PASTA] src/main/java/org/traducao/projeto/revisaoLore/domain/exceptions/
  - RevisaoLoreException.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/revisaoLore/domain/ports/
  - ProvedorPromptRevisaoLore.java
      (sem cabecalho explicativo)

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

[PASTA] src/main/java/org/traducao/projeto/traducao/
  - Application.java
      Utilitarios de inicializacao compartilhados entre modos CLI.
      O Quarkus e o container principal; nao ha {@code SpringApplication.run} aqui.

[PASTA] src/main/java/org/traducao/projeto/traducao/application/
  - DetectorEfeitoKaraokeService.java
      Reconhece eventos que são efeito de karaokê/música, e não fala de diálogo.
      Cobre as duas formas em que o karaokê aparece nos arquivos .ass:
      <ul>
      <li>Karaokê "cru": tags de timing {@code \k}, {@code \kf}, {@code \ko}
      por sílaba, como sai do fansub antes de aplicar template.</li>
      <li>Saída do Kara Templater do Aegisub: as tags {@code \k} são consumidas
      na aplicação do template e viram uma linha por sílaba/letra com
      transformações animadas ({@code \t(...)}, {@code \frx}, {@code \fad},
      {@code \pos}) e quase nenhum texto visível.</li>
      </ul>
      Regra única compartilhada pelos módulos de tradução, revisão e correção —
      nenhum deles deve mexer em música; isso é responsabilidade do módulo de
      karaokê.
  - DetectorTraducaoIdenticaService.java
      Decide se uma fala pode legitimamente permanecer idêntica ao original (nomes
      próprios, números, siglas, termos de lore) ou se a igualdade é sinal de que o
      LLM simplesmente devolveu a fala sem traduzir. Além da lista global fixa,
      consulta os termos protegidos do lore ATIVO ({@link GerenciadorContexto}),
      para que um termo novo anexado ao contexto selecionado seja protegido sem
      precisar editar este detector.
      
      <p>PROPÓSITO DE NEGÓCIO: impedir que manutenção ou retomada do cache apague
      nomes canônicos e, simultaneamente, não aceite frases inglesas como tradução.
      <p>INVARIANTES DO DOMÍNIO: a lore ativa é a fonte dos termos protegidos;
      expressões conversacionais comuns continuam exigindo tradução.
      <p>COMPORTAMENTO EM CASO DE FALHA: texto sem evidência suficiente é preservado
      para evitar uma decisão destrutiva.
  - ProcessarArquivoUseCase.java
      (sem cabecalho explicativo)
  - ProcessarEpisodioUseCase.java
      Quantas tentativas extras (alem da primeira) sao feitas numa fala isolada
      (lote de tamanho 1) antes de desistir e manter o texto original sem traducao.
      Temperatura por tentativa numa fala isolada: null = a configurada.
      Repetir a mesma requisicao com a mesma temperatura tende a reproduzir a
      mesma alucinacao; subir a temperatura muda a amostragem e da chance real
      de recuperacao antes de desistir da fala.
  - ProtecaoLegendaAssService.java
      Blindagens compartilhadas para linhas ASS/SSA antes e depois de chamadas a
      IA/serviços externos. Centraliza os casos perigosos encontrados em fansubs:
      clips vetoriais longos, letras soltas pós-template e preâmbulos alucinados.
  - ValidadorTraducaoService.java
      PROPÓSITO DE NEGÓCIO: impede que textos parcialmente traduzidos, respostas
      rotuladas ou conteúdo em idioma indevido cheguem às legendas e ao cache.
      <p>INVARIANTES DO DOMÍNIO: comentários ASS não visíveis são ignorados, nomes
      próprios conhecidos não viram falso positivo e resíduos visíveis inequívocos
      sempre bloqueiam a proposta.
      <p>COMPORTAMENTO EM CASO DE FALHA: lança
      {@link AlucinacaoDetectadaException} com diagnóstico e o chamador preserva a
      tradução anterior.

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/
  - ContextoPrompt.java
      Cada ContextoXxx monta seu PROMPT uma unica vez (campo static final), na
      inicializacao da classe; este mapa guarda a lore "crua" por traz de cada
      prompt completo para que outros usos (ex.: revisao de concordancia) nao
      precisem reenviar o prompt de traducao inteiro - que ja inclui lore +
      RegrasConcordanciaPtBr.BLOCO_TRADUCAO + regras de saida - como se fosse
      so a lore, o que estourava o contexto do LLM (ver MistralClientAdapter).
  - RegrasConcordanciaPtBr.java
      Regras de concordância de gênero, pronomes, tratamentos e verbos aplicáveis a
      qualquer obra — o inglês não marca gênero em adjetivos/participios e usa
      "you" genérico, o que leva o LLM a masculinizar tudo.

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/danmachi/
  - ContextoDanMachi.java
      (sem cabecalho explicativo)
  - ContextoDanMachiOrion.java
      (sem cabecalho explicativo)
  - ContextoDanMachiS1.java
      (sem cabecalho explicativo)
  - ContextoDanMachiS2.java
      (sem cabecalho explicativo)
  - ContextoDanMachiS3.java
      (sem cabecalho explicativo)
  - ContextoDanMachiS4.java
      (sem cabecalho explicativo)
  - ContextoDanMachiS5.java
      (sem cabecalho explicativo)
  - ContextoDanMachiSwordOratoria.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/eightsix/
  - Contexto86.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/evangelion/
  - ContextoEvangelion111.java
      (sem cabecalho explicativo)
  - ContextoEvangelion222.java
      (sem cabecalho explicativo)
  - ContextoEvangelion3010.java
      (sem cabecalho explicativo)
  - ContextoEvangelion333.java
      (sem cabecalho explicativo)
  - ContextoEvangelionTV.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/guiltycrown/
  - ContextoGuiltyCrown.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/gundam/chars/
  - ContextoCharsCounterattack.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/gundam/
  - ContextoGundam0079.java
      (sem cabecalho explicativo)
  - ContextoGundamF91.java
      (sem cabecalho explicativo)
  - ContextoGundamHathaway.java
      (sem cabecalho explicativo)
  - ContextoGundamNT.java
      (sem cabecalho explicativo)
  - ContextoGundamOrigin.java
      (sem cabecalho explicativo)
  - ContextoGundamSEED.java
      (sem cabecalho explicativo)
  - ContextoGundamSEEDAstray.java
      (sem cabecalho explicativo)
  - ContextoGundamSEEDDestiny.java
      (sem cabecalho explicativo)
  - ContextoGundamSEEDFreedom.java
      (sem cabecalho explicativo)
  - ContextoGundamSEEDStargazer.java
      (sem cabecalho explicativo)
  - ContextoGundamUnicorn.java
      (sem cabecalho explicativo)
  - ContextoGundamVictory.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/gundam/msteam/
  - ContextoGundam08thMSTeam.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/gundam/reconguista/
  - ContextoGundamReconguista.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/gundam/stardust/
  - ContextoGundam0083.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/gundam/warInpocket/
  - ContextoWarInPocket.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/gundam/zeta/
  - ContextoGundamZeta.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/gundam/zz/
  - ContextoGundamZZ.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/macross/
  - ContextoMacross2.java
      (sem cabecalho explicativo)
  - ContextoMacross7.java
      (sem cabecalho explicativo)
  - ContextoMacross7Encore.java
      (sem cabecalho explicativo)
  - ContextoMacross7Filme.java
      (sem cabecalho explicativo)
  - ContextoMacross7Filmes.java
      (sem cabecalho explicativo)
  - ContextoMacrossAnime.java
      (sem cabecalho explicativo)
  - ContextoMacrossDelta.java
      (sem cabecalho explicativo)
  - ContextoMacrossDeltaFilme1.java
      (sem cabecalho explicativo)
  - ContextoMacrossDeltaFilme2.java
      (sem cabecalho explicativo)
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
      (sem cabecalho explicativo)
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

[PASTA] src/main/java/org/traducao/projeto/traducao/contexto/sidonia/
  - ContextoKnightsOfSidonia.java
      (sem cabecalho explicativo)
  - ContextoSidoniaFilme.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/domain/exceptions/
  - AlucinacaoDetectadaException.java
      (sem cabecalho explicativo)
  - ArquivoLegendaException.java
      (sem cabecalho explicativo)
  - ContextoNaoEncontradoException.java
      (sem cabecalho explicativo)
  - DivergenciaLinhasException.java
      (sem cabecalho explicativo)
  - EntradaJaTraduzidaException.java
      PROPÓSITO DE NEGÓCIO: Sinaliza que a entrada aparenta já estar em PT-BR e a
      retradução não foi confirmada — o arquivo é bloqueado para não retraduzir e
      sobrescrever trabalho bom.
      
      <p>INVARIANTES DO DOMÍNIO: só lançada quando a heurística de caminho já
      traduzido dispara e {@code permitirRetraducao} é falso.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: é a própria sinalização; herda de
      {@link ArquivoLegendaException} para o lote registrar o arquivo como
      BLOQUEADO e seguir para o próximo.
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

[PASTA] src/main/java/org/traducao/projeto/traducao/domain/legenda/
  - DocumentoLegenda.java
      (sem cabecalho explicativo)
  - EventoLegenda.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/domain/
  - Lote.java
      (sem cabecalho explicativo)
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
  - StatusLlm.java
      Resultado da checagem de disponibilidade do servidor LLM local (ex: LM Studio)
      feita no início da execução, antes de começar a traduzir qualquer episódio.
  - StatusLoteTraducao.java
      PROPÓSITO DE NEGÓCIO: Desfecho do LOTE de tradução (vários arquivos), para a
      UI/telemetria pararem de mostrar "sucesso" quando houve arquivos com falha.
      
      <p>INVARIANTES DO DOMÍNIO: derivado dos status por arquivo — todos concluídos
      (com ou sem ressalvas) → {@code CONCLUIDO}; nenhum concluído → {@code FALHOU};
      mistura → {@code CONCLUIDO_COM_FALHAS}. {@code CANCELADO} é reservado para
      interrupção explícita do usuário.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: {@link #consolidar(List)} nunca lança; lote
      vazio devolve {@code FALHOU}.
  - TraducaoLote.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/domain/ports/
  - MistralPort.java
      Variante com temperatura explícita, usada nas retentativas de uma fala
      isolada: repetir a MESMA requisição com a mesma temperatura tende a
      reproduzir a mesma alucinação; subir a temperatura muda a amostragem e
      dá chance real de recuperação. {@code null} usa a temperatura configurada.
      Variante que recebe o prompt de sistema CONGELADO no início do job. Assim,
      uma troca de contexto (lore) no estado global não pode vazar para o meio da
      tradução de um episódio. {@code null} usa o prompt do contexto ativo.
      Verifica, antes de iniciar a tradução, se o servidor LLM local está
      online e se o modelo configurado está efetivamente carregado em
      memória — evita descobrir isso só depois de várias tentativas/timeouts
      já no meio da tradução do primeiro episódio.
  - ProvedorContexto.java
      Retorna o ID único para seleção via UI.
      Retorna o nome amigável para exibição no combo box da UI.
      Retorna o prompt de sistema completo para o LLM, com regras gerais e lore especifico da midia.
      Termos desta obra que NÃO devem ser traduzidos (nomes próprios, facções,
      patentes, lugares, mecha). Por padrão vazio; cada contexto pode
      sobrescrever para que o detector de "tradução idêntica" proteja o lore
      selecionado, em vez de depender só da lista global fixa no detector.

[PASTA] src/main/java/org/traducao/projeto/traducao/infrastructure/adapters/
  - MistralClientAdapter.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/infrastructure/config/
  - LlmProperties.java
      (sem cabecalho explicativo)
  - RestClientConfig.java
      Beans de agregacao e suporte para injecao CDI/Quarkus.
  - TradutorProperties.java
      (sem cabecalho explicativo)

[PASTA] src/main/java/org/traducao/projeto/traducao/infrastructure/contexto/
  - GerenciadorContexto.java
      Mutado pela thread única do executor de background (ApiController) e lido
      pela mesma thread ao montar o prompt do LLM (MistralClientAdapter). O
      volatile aqui é uma garantia defensiva de visibilidade, não uma alegação
      de que múltiplas threads concorrem por este campo.

[PASTA] src/main/java/org/traducao/projeto/traducao/infrastructure/dtos/
  - RecordsMistral.java
      Shape da API estendida da LM Studio ({@code /api/v0/models}, fora do
      prefixo {@code /v1}), que — diferente do endpoint OpenAI-compatible
      {@code /v1/models} — informa o campo {@code state} ("loaded" /
      "not-loaded"), permitindo saber com certeza qual modelo está de fato
      carregado em memória.

[PASTA] src/main/java/org/traducao/projeto/traducao/infrastructure/http/
  - JsonHttpClient.java
      Cliente HTTP JSON baseado em {@link HttpClient} do JDK (sem Spring RestClient).

[PASTA] src/main/java/org/traducao/projeto/traducao/infrastructure/legenda/
  - EscritorLegendaAss.java
      Reconstroi o arquivo .ass a partir do {@link DocumentoLegenda}, repetindo o
      cabecalho original e as linhas nao traduziveis byte a byte, e so trocando o
      campo Text dos eventos Dialogue pela versao traduzida.
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
      Le arquivos .ass/.ssa preservando byte a byte tudo que nao for o campo Text
      dos eventos Dialogue (estilos, timestamps, secoes de metadados). So o campo
      Text e exposto para traducao; o resto e reconstruido identico pelo
      {@link EscritorLegendaAss}.
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
  - MascaradorTags.java
      Isola tags de formatação ASS/SSA (ex: {\i1}, {\pos(...)}) e códigos de quebra
      (\N, \n, \h) do texto antes de enviar ao LLM, trocando-os por marcadores
      [[TAGn]] que o modelo é instruído a preservar literalmente. Sem isso o LLM
      tende a "traduzir" ou descartar as tags, corrompendo a legenda renderizada.

[PASTA] src/main/java/org/traducao/projeto/traducao/presentation/
  - TradutorCLI.java
      Ponto de entrada da CLI: varre a pasta de entrada por arquivos .ass/.ssa
      e traduz cada um sequencialmente.
      <p>
      Se {@code tradutor.diretorio-entrada} estiver vazio, o {@link Application#main}
      pede os caminhos via {@link ConsoleEntrada}
      antes do Spring subir.
      <p>
      Arquivos são processados um por vez de propósito: todos compartilham o
      mesmo LLM local (GPU única). Lotes dentro de cada episódio também são
      sequenciais (ver {@code ProcessarEpisodioUseCase}).

[PASTA] src/main/java/org/traducao/projeto/traducao/presentation/ui/
  - AnsiCores.java
      Cores ANSI compartilhadas entre o prompt interativo e o {@link ConsoleUILogger}.
      Usar apenas caracteres ASCII nos textos do prompt evita problemas de encoding
      no console do Windows (cp1252 vs UTF-8).
  - ConsoleEntrada.java
      Numeração segue a ordem natural do pipeline: primeiro auditar a
      mídia, depois extrair/traduzir/corrigir a legenda e por fim remuxar.
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
  - AnaliseMidiaController.java
      PROPÓSITO DE NEGÓCIO: expõe a análise de mídia (Opção 1) à interface web,
      enfileirando o processamento pesado em segundo plano e publicando o relatório
      estruturado no canal SSE {@code analise-relatorio} para renderização no
      navegador.
      
      <p>INVARIANTES DO DOMÍNIO: usa a MESMA fila compartilhada via
      {@link PipelineWebSupport}; caminhos são normalizados antes do uso; nenhuma
      URL, código HTTP, nome de campo de DTO ou canal SSE é alterado em relação ao
      controller monolítico original.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: entrada em branco retorna HTTP 400; falhas
      do job de background são registradas no log e no console SSE, sem derrubar a
      fila.
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
  - ExtracaoLegendaController.java
      PROPÓSITO DE NEGÓCIO: expõe a extração de legendas (Opção 2) à interface web,
      validando o formato-alvo e enfileirando o processamento pesado que percorre a
      pasta de vídeos e extrai as faixas de legenda no formato escolhido.
      
      <p>INVARIANTES DO DOMÍNIO: usa a MESMA fila compartilhada via
      {@link PipelineWebSupport}; o formato é validado antes de enfileirar; caminhos
      são normalizados; nenhuma URL, código HTTP ou nome de campo de DTO é alterado
      em relação ao controller monolítico original.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: entrada em branco ou formato inválido
      retorna HTTP 400; falhas do job de background são registradas no log e no
      console SSE, sem derrubar a fila.
  - ExtracaoRequest.java
      PROPÓSITO DE NEGÓCIO: transporta os parâmetros da extração de legendas —
      pasta de vídeos, pasta de saída e o formato-alvo escolhido na interface.
      
      <p>INVARIANTES DO DOMÍNIO: os nomes dos campos são contrato JSON público; o
      formato é validado contra {@code FormatoLegenda} antes do job entrar na fila.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: entrada em branco ou formato inválido faz o
      endpoint responder HTTP 400 antes de qualquer processamento.
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
  - LogStreamService.java
      Gerencia conexoes SSE (JAX-RS) e despacha logs em tempo real para clientes web.
  - MapaController.java
      PROPÓSITO DE NEGÓCIO: expõe a geração do mapa do projeto (Opção 7) à interface
      web, produzindo o relatório em markdown e a árvore no formato GitHub a partir
      da raiz do projeto.
      
      <p>INVARIANTES DO DOMÍNIO: a raiz mapeada vem de
      {@link DiretorioBaseKronos#base()} — em produção é o diretório de trabalho e,
      sob a suíte de testes, a árvore descartável, evitando reescrever o mapa real;
      nenhuma URL, código HTTP ou nome de campo de DTO é alterado em relação ao
      controller monolítico original.
      
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
  - OperacaoRequest.java
      PROPÓSITO DE NEGÓCIO: transporta os parâmetros comuns das operações do
      pipeline (análise, tradução, correção e revisão) enviados pela SPA — pastas de
      entrada/saída, contexto de lore selecionado e opções de sincronismo/revisão.
      
      <p>INVARIANTES DO DOMÍNIO: os nomes dos campos são contrato JSON público
      consumido pelo front-end; caminhos são normalizados e o contexto é validado
      pelos endpoints antes de qualquer job entrar na fila compartilhada.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: campos ausentes chegam como {@code null} e
      cada endpoint decide o fallback seguro ou responde HTTP 400 antes de enfileirar.
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
  - RemuxerController.java
      PROPÓSITO DE NEGÓCIO: expõe o Remuxer (mkvmerge) à interface web, agendando um
      único lote de remux que combina vídeos e legendas traduzidas com política
      explícita para as legendas originais.
      
      <p>INVARIANTES DO DOMÍNIO: usa a MESMA fila compartilhada via
      {@link PipelineWebSupport} e consulta a MESMA {@link FilaExecucaoPipeline} para
      recusar concorrência; as pastas existem antes da aceitação; o offset fica na
      faixa operacional de ±86.400.000 ms; nenhuma URL, código HTTP ou nome de campo
      de DTO é alterado em relação ao controller monolítico original.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: entrada inválida retorna HTTP 400, fila
      ocupada retorna HTTP 409 e falha do lote aparece no status final do console.
  - RemuxRequest.java
      PROPÓSITO DE NEGÓCIO: transporta as opções exclusivas do Remuxer.
      INVARIANTES DO DOMÍNIO: pasta de vídeo é obrigatória; offset e política de
      faixas são validados pelo endpoint.
      COMPORTAMENTO EM CASO DE FALHA: campos ausentes recebem fallback seguro ou
      geram HTTP 400 antes de entrar na fila.
  - RespostaPadrao.java
      PROPÓSITO DE NEGÓCIO: envelope de resposta textual padrão da API web, usado
      por praticamente todos os endpoints do pipeline para devolver ao navegador uma
      mensagem legível (aceitação na fila, validação recusada ou heartbeat).
      
      <p>INVARIANTES DO DOMÍNIO: o nome do campo {@code mensagem} é contrato JSON
      público consumido pela SPA; não pode ser renomeado sem quebrar o front-end.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: sendo um record imutável, não há falha de
      construção; {@code mensagem} pode ser vazia, mas nunca deve carregar dados
      sensíveis, pois é ecoada diretamente na interface.
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
  - TelemetriaController.java
      PROPÓSITO DE NEGÓCIO: expõe à interface web a telemetria acumulada do pipeline
      — resumo consolidado para o painel, exportação segura do arquivo para download
      e a publicação do dataset público sanitizado no repositório Git dedicado.
      
      <p>INVARIANTES DO DOMÍNIO: nenhuma URL, código HTTP ou nome de campo de DTO é
      alterado em relação ao controller monolítico original; a exportação usa o
      arquivo canônico e a publicação delega ao serviço de dataset já sanitizado.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: exportação sem arquivo retorna 404 e falha
      de leitura retorna 500; falha na publicação do dataset retorna 500 com a
      mensagem do erro no corpo padrão.
  - TelemetriaStreamResource.java
      Endpoint Server-Sent Events (SSE) reativo para streaming da telemetria da KRONOS em tempo real.
      Rota mapeada especificamente para evitar colisões com o Controller Spring.
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
      
      <p>COMPORTAMENTO EM CASO DE FALHA: entrada em branco ou contexto ausente/
      inválido retorna HTTP 400; falhas por arquivo são contabilizadas, registradas
      na telemetria e reportadas no console SSE, sem derrubar a fila.

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

[PASTA] src/test/java/org/traducao/projeto/raspagemCorrecao/application/
  - CorrigirComGoogleUseCaseTest.java
      PROPÓSITO DE NEGÓCIO: prova a regressão central do menu — uma entrada vazia
      produzida pela limpeza precisa ser preenchida pela contingência Google.
      
      <p>INVARIANTES DO DOMÍNIO: teste não acessa a internet nem grava telemetria no
      projeto; cache versionado e proveniência permanecem intactos.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: qualquer ausência de tradução aplicada ou
      alteração do envelope falha o teste.
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
  - DetectorEfeitoKaraokeServiceTest.java
      Linha real que escapou da revisão: letra "I" afogada em transformações.
      Linha com \pos e fscx/fscy onde o texto visível é curto em relação às tags.
  - DetectorTraducaoIdenticaServiceTest.java
      PROPÓSITO DE NEGÓCIO: impede que nomes próprios legítimos sejam enviados ao
      revisor apenas porque são idênticos no inglês e no PT-BR.
      <p>INVARIANTES DO DOMÍNIO: hesitação e pontuação não descaracterizam nomes;
      palavras conversacionais inglesas continuam pendentes.
      <p>COMPORTAMENTO EM CASO DE FALHA: falso nome ou falso inglês reprova o teste.
  - ProcessarArquivoUseCaseCaracterizacaoTest.java
      (sem cabecalho explicativo)
  - ProcessarArquivoUseCaseGuardTest.java
      PROPÓSITO DE NEGÓCIO: protege por regressão as decisões que impedem o tradutor
      de publicar linhas ASS suspeitas ou substituir uma legenda sem autorização.
      
      <p>INVARIANTES DO DOMÍNIO: entradas protegidas permanecem bloqueadas, a chave
      de liberação escolhe o destino correto e toda sobrescrita preserva um backup.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: qualquer desvio interrompe a suíte antes de
      o comportamento inseguro alcançar arquivos reais.
  - ValidadorTraducaoServiceTest.java
      Caso real (Gundam Narrative): LLM rotulou a resposta em vez de só traduzir.
      Caso real (G-Reconguista): marcador do pipeline Python antigo na legenda final.

[PASTA] src/test/java/org/traducao/projeto/traducao/domain/
  - StatusLoteTraducaoTest.java
      Cobre a consolidação do status do lote a partir dos status por arquivo —
      o núcleo do fix "não mostrar sucesso quando houve falhas".

[PASTA] src/test/java/org/traducao/projeto/traducao/infrastructure/adapters/
  - MistralClientAdapterRespostaRevisaoTest.java
      PROPÓSITO DE NEGÓCIO: garante que respostas do Tower/Mistral com raciocínio
      ou formatação auxiliar entreguem somente a fala final à revisão de legendas.
      
      <p>INVARIANTES DO DOMÍNIO: todos os marcadores ASS esperados permanecem na
      saída e explicações do modelo nunca entram na legenda.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: resposta incompatível produz texto vazio,
      obrigando o cliente a tentar novamente em vez de publicar estrutura quebrada.

[PASTA] src/test/java/org/traducao/projeto/traducao/infrastructure/legenda/
  - EscritorLegendaAssTest.java
      (sem cabecalho explicativo)
  - LeitorEscritorSrtTest.java
      Cobre a leitura/escrita nativa de SRT: preservação de índice e timestamps,
      quebra interna via \N, round-trip e troca apenas do texto (o pipeline traduz
      só o texto, mantendo tempos).
  - MascaradorTagsTest.java
      PROPÓSITO DE NEGÓCIO: garante que cache reutilizado não danifique estilo,
      posicionamento nem quebras estruturais das legendas ASS/SSA.
      
      <p>INVARIANTES DO DOMÍNIO: somente o texto visível pode mudar; perda, criação,
      alteração ou reordenação de tags invalida a tradução armazenada.
      
      <p>COMPORTAMENTO EM CASO DE FALHA: cada divergência produz uma asserção falsa
      explícita, impedindo regressões que aceitariam cache estruturalmente corrompido.

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
