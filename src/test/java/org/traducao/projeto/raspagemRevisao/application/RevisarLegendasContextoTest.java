package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.raspagemRevisao.domain.ContextoRevisao;
import org.traducao.projeto.raspagemCorrecao.application.ProtetorTermosLoreService;
import org.traducao.projeto.contexto.application.ValidadorCompatibilidadeObraContexto;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.raspagemRevisao.domain.exceptions.RaspagemRevisaoException;
import org.traducao.projeto.traducaoCorrige.application.ContextoManutencaoCacheService;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        GerenciadorContexto gerenciador = catalogo();
        RevisarLegendasUseCase useCase = useCaseCom(gerenciador);
        ProvenienciaCache proveniencia = new ProvenienciaCache(
            1, "gundam_nt", "hash", "modelo", "en", "pt-br");

        ContextoRevisao contexto = useCase.ativarContextoDoArquivo(
            proveniencia, "danmachi", Path.of("gundam.cache.json"));

        assertEquals("gundam_nt", contexto.id());
        assertEquals("gundam_nt", gerenciador.obterIdContextoAtivo());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: prende o furo medido em 2026-07-27. Esta fatia resolvia a lore pelo
     * carimbo do cache SEM passar pela guarda obra×contexto — o único caminho de entrada de
     * {@code raspagemRevisao} que escapava dela. O carimbo é testemunha, não prova: o incidente
     * que originou a guarda gravou 15 caches de Gundam 0083 carimbados como {@code guilty_crown}.
     *
     * <p>Sem a guarda, esses arquivos eram revisados sob a lore de Guilty Crown e o {@code .ass}
     * era reescrito com a terminologia da obra errada. Pior: a lore reprovada ficava ATIVA e
     * vazava para o arquivo seguinte da varredura.
     *
     * <p>O teste antigo desta classe usa {@code Path.of("gundam.cache.json")} — sem pasta de obra
     * —, então era estruturalmente cego a este defeito. Aqui o caminho tem a pasta real.
     */
    @Test
    @DisplayName("carimbo de outra obra é BLOQUEADO e a lore reprovada não fica ativa")
    void carimboDeOutraObraNaoRevisaSobLoreErrada() {
        GerenciadorContexto gerenciador = catalogo();
        RevisarLegendasUseCase useCase = useCaseCom(gerenciador);
        gerenciador.definirContextoAtivo("gundam_nt");
        ProvenienciaCache carimboErrado = new ProvenienciaCache(
            1, "danmachi", "hash", "modelo", "en", "pt-br");
        Path cacheDeGundam = Path.of("cache", "Gundam Narrative", "ep01.cache.json");

        var erro = assertThrows(RaspagemRevisaoException.class,
            () -> useCase.ativarContextoDoArquivo(carimboErrado, null, cacheDeGundam));

        assertTrue(erro.getMessage().contains("gundam_nt"), erro::getMessage);
        assertEquals("gundam_nt", gerenciador.obterIdContextoAtivo(),
            "a lore recusada foi ativada mesmo assim — o próximo arquivo da varredura herdaria ela");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a guarda não pode bloquear o caso legítimo — pasta e carimbo
     * concordando seguem normalmente, senão trocaríamos um furo por uma parada de produção.
     */
    @Test
    @DisplayName("pasta e carimbo concordando: a revisão segue")
    void pastaEcarimboConcordandoSegue() {
        GerenciadorContexto gerenciador = catalogo();
        RevisarLegendasUseCase useCase = useCaseCom(gerenciador);

        var contexto = useCase.ativarContextoDoArquivo(
            new ProvenienciaCache(1, "gundam_nt", "hash", "modelo", "en", "pt-br"),
            null, Path.of("cache", "Gundam Narrative", "ep01.cache.json"));

        assertEquals("gundam_nt", contexto.id());
    }

    private static GerenciadorContexto catalogo() {
        return new GerenciadorContexto(List.of(
            new ContextoTeste("danmachi", "DanMachi"),
            new ContextoTeste("gundam_nt", "Gundam Narrative")));
    }

    private static RevisarLegendasUseCase useCaseCom(GerenciadorContexto gerenciador) {
        // 18 dependencias; so as quatro que a guarda obra x contexto usa sao reais.
        return new RevisarLegendasUseCase(
            null, null, null, null, null, null, null, null, null,
            gerenciador,                    // gerenciadorContexto
            null, null, null,
            new ProtetorTermosLoreService(), // protetorLore
            null,
            new ContextoManutencaoCacheService(gerenciador, new ValidadorCompatibilidadeObraContexto()),
            new ResolvedorArtefatosRevisao(),
            null,                           // filtroAuditoria
            null);                          // detectorRetraducaoEmMassa
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
