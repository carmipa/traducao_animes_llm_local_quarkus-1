package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.raspagemCorrecao.application.ProtetorTermosLoreService;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;
import org.traducao.projeto.traducao.infrastructure.cache.ProvenienciaCache;
import org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: prova que a Opção 6 não revisa uma obra usando a lore
 * selecionada por engano na interface quando o cache conhece sua proveniência.
 *
 * <p>INVARIANTES DO DOMÍNIO: contexto versionado vence fallback manual.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: ativação de DanMachi para cache Gundam
 * reprova o teste antes que uma legenda real seja modificada.
 */
class RevisarLegendasContextoTest {

    /**
     * PROPÓSITO DE NEGÓCIO: reproduz o incidente real Gundam selecionado como DanMachi.
     * <p>INVARIANTES DO DOMÍNIO: `gundam_nt` permanece ativo e fornece sua lore.
     * <p>COMPORTAMENTO EM CASO DE FALHA: contexto divergente reprova o teste.
     */
    @Test
    void provenienciaDoCacheVenceSelecaoManualIncompativel() {
        GerenciadorContexto gerenciador = new GerenciadorContexto(List.of(
            new ContextoTeste("danmachi", "DanMachi"),
            new ContextoTeste("gundam_nt", "Gundam Narrative")));
        RevisarLegendasUseCase useCase = new RevisarLegendasUseCase(
            null, null, null, null, null, null, null, null, null, gerenciador,
            null, null, null, null, null, new ProtetorTermosLoreService(), null);
        ProvenienciaCache proveniencia = new ProvenienciaCache(
            1, "gundam_nt", "hash", "modelo", "en", "pt-br");

        RevisarLegendasUseCase.ContextoRevisao contexto = useCase.ativarContextoDoArquivo(
            proveniencia, "danmachi", Path.of("gundam.cache.json"));

        assertEquals("gundam_nt", contexto.id());
        assertEquals("gundam_nt", gerenciador.obterIdContextoAtivo());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fornece catálogo mínimo de lores para o teste isolado.
     * <p>INVARIANTES DO DOMÍNIO: ID e nome permanecem estáveis.
     * <p>COMPORTAMENTO EM CASO DE FALHA: objeto não executa I/O.
     */
    private record ContextoTeste(String id, String nome) implements ProvedorContexto {
        /**
         * PROPÓSITO DE NEGÓCIO: identifica a lore simulada no catálogo do teste.
         * <p>INVARIANTES DO DOMÍNIO: devolve exatamente o ID recebido no record.
         * <p>COMPORTAMENTO EM CASO DE FALHA: record não produz valor nulo artificial.
         */
        @Override
        public String getId() {
            return id;
        }

        /**
         * PROPÓSITO DE NEGÓCIO: apresenta o nome legível do contexto simulado.
         * <p>INVARIANTES DO DOMÍNIO: não altera a identidade técnica do contexto.
         * <p>COMPORTAMENTO EM CASO DE FALHA: devolve o valor imutável do record.
         */
        @Override
        public String getNomeExibicao() {
            return nome;
        }

        /**
         * PROPÓSITO DE NEGÓCIO: fornece uma lore mínima com termo canônico ao teste.
         * <p>INVARIANTES DO DOMÍNIO: Narrative permanece marcado para preservação.
         * <p>COMPORTAMENTO EM CASO DE FALHA: montagem inválida reprova o teste chamador.
         */
        @Override
        public String obterPromptSistema() {
            return ContextoPrompt.montar(nome, "- Manter sempre em inglês ou forma oficial: Narrative.");
        }
    }
}
