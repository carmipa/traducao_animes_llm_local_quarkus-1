package org.traducao.projeto.traducaoCorrige.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.cachetraducao.infrastructure.CacheManutencaoService;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.contexto.application.ValidadorCompatibilidadeObraContexto;
import org.traducao.projeto.contexto.domain.SnapshotContexto;
import org.traducao.projeto.contexto.domain.VeredictoObraContexto;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;

import java.nio.file.Path;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: garante que cada arquivo da pasta cache seja analisado
 * com a lore da obra que realmente o originou, mesmo quando a raiz contém
 * caches de vários animes.
 *
 * <p>INVARIANTES DO DOMÍNIO: a proveniência versionada tem prioridade; contexto
 * manual serve somente como fallback para cache legado; contexto desconhecido
 * nunca cai silenciosamente no padrão.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link IllegalArgumentException} e o
 * arquivo é contabilizado como falha sem ser modificado.
 *
 * <h2>Por que a guarda obra×contexto também mora aqui</h2>
 * Este é o ÚNICO ponto por onde as três fatias de manutenção de cache
 * ({@code traducaoCorrige}, {@code raspagemCorrecao}, {@code raspagemRevisao}) escolhem
 * sob qual lore vão reinterpretar um cache. A pergunta que a guarda responde aqui é
 * DIFERENTE da que a fatia {@code traducao} responde: lá se desconfia do CLIQUE do
 * operador; aqui se desconfia do PRÓPRIO CARIMBO. O incidente medido nesta árvore provou
 * que a proveniência pode estar fielmente errada — 15 caches de Gundam 0083 gravados com
 * {@code contextoId = "guilty_crown"}. Um corretor que confia no carimbo reativaria a lore
 * errada e "corrigiria" a terminologia de 0083 para a de Guilty Crown, aprofundando o dano
 * em vez de repará-lo. A pasta em que o arquivo mora é a segunda testemunha.
 */
@Service
public class ContextoManutencaoCacheService {

    private static final Logger log = LoggerFactory.getLogger(ContextoManutencaoCacheService.class);

    private final GerenciadorContexto gerenciadorContexto;
    private final ValidadorCompatibilidadeObraContexto validadorCompatibilidade;

    /**
     * PROPÓSITO DE NEGÓCIO: conecta a manutenção ao catálogo local de lores e ao juiz de
     * compatibilidade obra×contexto do peer {@code contexto}.
     * <p>INVARIANTES DO DOMÍNIO: existe um gerenciador compartilhado pela fila serial; a regra
     * de identidade de obra NÃO é reimplementada aqui — este serviço só converte o veredicto
     * em efeito, como manda o dono da regra.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede o uso do serviço.
     */
    public ContextoManutencaoCacheService(
        GerenciadorContexto gerenciadorContexto,
        ValidadorCompatibilidadeObraContexto validadorCompatibilidade
    ) {
        this.gerenciadorContexto = gerenciadorContexto;
        this.validadorCompatibilidade = validadorCompatibilidade;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: ativa e devolve o contexto correto para uma unidade
     * de cache antes de qualquer classificação ou chamada de LLM.
     *
     * <p>INVARIANTES DO DOMÍNIO: cache versionado não pode ser reinterpretado
     * com o fallback de outra obra; cache legado exige seleção explícita; e o contexto
     * resolvido — venha do carimbo ou do fallback — ainda precisa bater com a obra da PASTA
     * antes de ser ativado. A ordem importa: a guarda roda ANTES de
     * {@code definirContextoAtivo}, para que uma lore reprovada nunca chegue a ficar ativa e
     * vazar para o arquivo seguinte da varredura.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link IllegalArgumentException}
     * para contexto ausente/desconhecido e para obra incompatível, preservando o arquivo.
     */
    public String ativar(CacheManutencaoService.DocumentoEditavel documento, String contextoFallback) {
        return ativarSnapshot(documento, contextoFallback).id();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mesma ativação de {@link #ativar}, devolvendo a FOTOGRAFIA do
     * contexto — prompt, termos protegidos e mapa forma-ruim→canônico — em vez de só o id. É o
     * que a correção de terminologia sobre cache precisa: o mapa da obra daquele arquivo.
     *
     * <p>INVARIANTES DO DOMÍNIO: o snapshot vem por {@code snapshotPorId(contextoId)}, NÃO do
     * contexto ativo global. A diferença não é estilística: {@code correcoesTerminologiaAtiva()}
     * foi removida do {@code GerenciadorContexto} justamente porque ler o mapa do estado global
     * num segundo momento era a janela por onde uma troca de lore entrava no meio do trabalho.
     * Ler por id fecha essa janela — o mapa pertence, por construção, ao mesmo contexto que a
     * guarda acabou de aprovar para este arquivo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: as mesmas de {@link #ativar} — lança
     * {@link IllegalArgumentException} para contexto ausente/desconhecido e obra incompatível.
     *
     * @param documento cache aberto para manutenção
     * @param contextoFallback contexto escolhido na UI, usado só para cache legado
     * @return fotografia imutável do contexto deste arquivo; nunca {@code null}
     */
    public SnapshotContexto ativarSnapshot(
            CacheManutencaoService.DocumentoEditavel documento, String contextoFallback) {
        ProvenienciaCache proveniencia = documento.proveniencia();
        String contextoId = proveniencia != null ? proveniencia.contextoId() : contextoFallback;
        if (contextoId == null || contextoId.isBlank()) {
            throw new IllegalArgumentException(
                "Cache legado sem proveniência: selecione a Obra / Contexto para processar "
                    + documento.arquivo().getFileName());
        }
        if (!gerenciadorContexto.existeContexto(contextoId)) {
            throw new IllegalArgumentException(
                "Contexto da proveniência não existe no projeto: \"" + contextoId + "\" em "
                    + documento.arquivo().getFileName());
        }
        exigirObraCompativel(documento.arquivo(), contextoId);
        gerenciadorContexto.definirContextoAtivo(contextoId);
        return gerenciadorContexto.snapshotPorId(contextoId);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: recusa o arquivo quando a obra escrita na PASTA do cache não é a
     * obra do contexto prestes a ser ativado — a blindagem contra reinterpretar um cache sob a
     * lore errada, seja porque o carimbo nasceu errado, seja porque o operador escolheu o
     * fallback errado para um cache legado.
     *
     * <p>INVARIANTES DO DOMÍNIO: não julga identidade de obra — delega ao
     * {@link ValidadorCompatibilidadeObraContexto}, dono da regra no peer {@code contexto}, e
     * só converte o veredicto em efeito. DIVERGENTE e AMBÍGUO bloqueiam; INDETERMINADO avisa
     * em log e segue. Falhar aberto no indeterminado é deliberado: uma pasta que nenhuma lore
     * reconhece não é prova de erro, e fechar aí pararia a manutenção de todo cache de obra
     * ainda sem vocabulário declarado.
     *
     * <p>AMBÍGUO bloqueia mesmo que o contexto resolvido esteja entre os empatados, pelo mesmo
     * motivo que vale na tradução: se a identidade não resolve para uma obra única, aceitar o
     * carimbo como desempate seria tomar por prova exatamente aquilo que se está verificando.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link IllegalArgumentException}, que as três
     * fatias já contabilizam como falha do arquivo sem tocá-lo.
     *
     * @param arquivoCache caminho do arquivo de cache em manutenção
     * @param contextoId contexto resolvido para este arquivo, ainda não ativado
     */
    private void exigirObraCompativel(Path arquivoCache, String contextoId) {
        String obra = obraDoCache(arquivoCache);
        Set<String> reconhecedores = gerenciadorContexto.idsQueReconhecem(obra);
        VeredictoObraContexto veredicto = validadorCompatibilidade.avaliar(obra, contextoId, reconhecedores);

        switch (veredicto) {
            case DIVERGENTE -> throw new IllegalArgumentException(
                validadorCompatibilidade.mensagemDeBloqueio(
                    arquivoCache.toString(), obra, contextoId, reconhecedores));
            case AMBIGUO -> throw new IllegalArgumentException(
                validadorCompatibilidade.mensagemDeAmbiguidade(
                    arquivoCache.toString(), obra, contextoId, reconhecedores));
            case INDETERMINADO -> log.warn(
                validadorCompatibilidade.mensagemDeIndeterminacao(obra, contextoId));
            case CASA -> log.debug("[ CONTEXTO ] Cache da obra \"{}\" confere com \"{}\".", obra, contextoId);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: deriva o nome da obra a partir do caminho do arquivo de CACHE.
     *
     * <p>INVARIANTES DO DOMÍNIO: o cache mora em {@code <raizCache>/<Obra>/<base>.cache.json},
     * então a obra é a pasta-MÃE. Não é a mesma regra da legenda de entrada, que mora em
     * {@code <Obra>/legendas_originais/arquivo.ass} e resolve pela pasta-avó: são duas
     * convenções distintas, e reusar a regra da legenda aqui deslocaria a obra em um nível.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: caminho sem pasta-mãe devolve string vazia, que o
     * validador trata como INDETERMINADO — o desfecho que deixa seguir com aviso.
     *
     * @param arquivoCache caminho do arquivo de cache
     * @return nome da pasta da obra, cru como veio do sistema de arquivos; nunca {@code null}
     */
    private String obraDoCache(Path arquivoCache) {
        Path pasta = arquivoCache.getParent();
        return pasta != null && pasta.getFileName() != null ? pasta.getFileName().toString() : "";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: entrega à manutenção online a lore que corresponde
     * ao cache cuja proveniência acabou de ser ativada.
     *
     * <p>INVARIANTES DO DOMÍNIO: a lore pertence ao mesmo contexto retornado por
     * {@link #ativar(CacheManutencaoService.DocumentoEditavel, String)}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: contexto sem lore devolve o fallback
     * textual do gerenciador, nunca {@code null} por contrato operacional.
     */
    public String loreAtiva() {
        return gerenciadorContexto.obterLoreAtiva();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: expõe os termos formalmente protegidos pelo provedor
     * ativo para mascaramento durante a tradução online.
     *
     * <p>INVARIANTES DO DOMÍNIO: o conjunto pertence ao contexto ativo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência de termos produz conjunto vazio.
     */
    public java.util.Set<String> termosProtegidosAtivos() {
        return gerenciadorContexto.termosProtegidosAtivos();
    }
}
