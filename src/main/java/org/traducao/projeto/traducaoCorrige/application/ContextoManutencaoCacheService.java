package org.traducao.projeto.traducaoCorrige.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.traducao.infrastructure.cache.CacheManutencaoService;
import org.traducao.projeto.traducao.infrastructure.cache.ProvenienciaCache;
import org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto;

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
 */
@Service
public class ContextoManutencaoCacheService {

    private final GerenciadorContexto gerenciadorContexto;

    /**
     * PROPÓSITO DE NEGÓCIO: conecta a manutenção ao catálogo local de lores.
     * <p>INVARIANTES DO DOMÍNIO: existe um gerenciador compartilhado pela fila serial.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede o uso do serviço.
     */
    public ContextoManutencaoCacheService(GerenciadorContexto gerenciadorContexto) {
        this.gerenciadorContexto = gerenciadorContexto;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: ativa e devolve o contexto correto para uma unidade
     * de cache antes de qualquer classificação ou chamada de LLM.
     *
     * <p>INVARIANTES DO DOMÍNIO: cache versionado não pode ser reinterpretado
     * com o fallback de outra obra; cache legado exige seleção explícita.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link IllegalArgumentException}
     * para contexto ausente/desconhecido, preservando o arquivo.
     */
    public String ativar(CacheManutencaoService.DocumentoEditavel documento, String contextoFallback) {
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
        gerenciadorContexto.definirContextoAtivo(contextoId);
        return contextoId;
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
