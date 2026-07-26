package org.traducao.projeto.contexto.infrastructure;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.contexto.domain.ContextoNaoEncontradoException;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.contexto.domain.SnapshotContexto;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa as duas capacidades que o peer {@code contexto} passou a
 * oferecer para fechar a janela do incidente medido nesta árvore (15 caches de Gundam 0083
 * carimbados com {@code guilty_crown}): reconhecer a obra pelo nome da pasta e congelar o
 * contexto ativo em um valor imutável.
 *
 * <p>INVARIANTES DO DOMÍNIO: o reconhecimento é por palavras INTEIRAS sobre o nome
 * normalizado (sem acento, sem caixa, sem pontuação) e nunca por palpite; a identidade é
 * DERIVADA de id e nome de exibição, então obra sem apelidos declarados também se reconhece;
 * a entrada mais específica vence a mais genérica do mesmo franchise; empate entre obras
 * distintas é devolvido como tal (vira ambiguidade no validador) e colisão de nome canônico
 * derruba o boot; um snapshot já emitido não observa trocas posteriores do contexto ativo.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: reconhecer por prefixo/similaridade voltaria a permitir
 * traduzir a obra errada — ou, pior, bloquear a certa. Desempatar arbitrariamente um empate
 * real seria adivinhar a obra, que é o erro que esta guarda existe para impedir.
 */
class GerenciadorContextoReconhecimentoObraTest {

    /**
     * PROPÓSITO DE NEGÓCIO: dublê de lore com apelidos de pasta, espelhando o formato real
     * declarado por {@code ContextoGundam0083} e {@code ContextoGuiltyCrown}.
     * <p>INVARIANTES DO DOMÍNIO: id, nome, prompt e apelidos fixos; sem I/O.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    private record LoreFake(String id, String nome, String lore, Set<String> apelidos)
        implements ProvedorContexto {

        @Override public String getId() { return id; }
        @Override public String getNomeExibicao() { return nome; }
        @Override public String obterPromptSistema() { return ContextoPrompt.montar(nome, lore); }
        @Override public Set<String> apelidosPasta() { return apelidos; }
        @Override public Set<String> termosProtegidos() { return Set.of(nome); }
    }

    private static final LoreFake ZERO_083 =
        new LoreFake("gundam_0083", "Gundam 0083", "Lore 0083.", Set.of("Gundam 0083", "Stardust Memory"));
    private static final LoreFake GUILTY =
        new LoreFake("guilty_crown", "Guilty Crown", "Lore GC.", Set.of("Guilty Crown"));
    private static final LoreFake SEM_VOCABULARIO =
        new LoreFake("sem_vocabulario", "Sem Vocabulario", "Lore SV.", Set.of());

    private GerenciadorContexto gerenciador() {
        return new GerenciadorContexto(List.of(ZERO_083, GUILTY, SEM_VOCABULARIO));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a pasta real do incidente — com grupo de fansub, ano e ruído em
     * volta do título — precisa ser reconhecida como 0083 e só como 0083.
     */
    @Test
    void pastaRealDoIncidenteEhReconhecidaApenasPelaObraCerta() {
        Set<String> ids = gerenciador().idsQueReconhecem(
            "[Joseki] Mobile Suit Gundam 0083 Stardust Memory COMPLETE (1991) [BD 1080p]");

        assertEquals(Set.of("gundam_0083"), ids);
    }

    @Test
    void reconhecimentoIgnoraCaixaAcentoEPontuacao() {
        GerenciadorContexto g = gerenciador();

        assertEquals(Set.of("guilty_crown"), g.idsQueReconhecem("guilty.crown"));
        assertEquals(Set.of("guilty_crown"), g.idsQueReconhecem("[SubsPlease] GUILTY_CROWN [1080p]"));
        assertEquals(Set.of("guilty_crown"), g.idsQueReconhecem("Guílty Crówn"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a comparação é por palavras INTEIRAS. Reconhecer por substring
     * faria "Crown" de outra obra casar com Guilty Crown e bloquear uma tradução legítima.
     */
    @Test
    void apelidoSoCasaComoSequenciaDePalavrasInteiras() {
        GerenciadorContexto g = gerenciador();

        assertTrue(g.idsQueReconhecem("The Crown Season 1").isEmpty(),
            "'Crown' sozinho não é Guilty Crown");
        assertTrue(g.idsQueReconhecem("Guiltycrown").isEmpty(),
            "sem separador não há duas palavras: não casa");
        assertTrue(g.idsQueReconhecem("Gundam 00 Season 2").isEmpty(),
            "Gundam 00 não é Gundam 0083");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: desde a identidade CANÔNICA DERIVADA, "sem apelidos declarados" não
     * significa mais "sem identidade": toda obra do catálogo se reconhece pelo próprio id e pelo
     * próprio nome de exibição, sem escrever uma linha de vocabulário. É o que levou a cobertura
     * de duas obras para todas as 59 de uma vez.
     *
     * <p>INVARIANTES DO DOMÍNIO: os apelidos passam a ser COMPLEMENTO — cobrem grafias
     * alternativas e abreviações que id e rótulo de UI não alcançam. O que continua não sendo
     * reconhecido é a pasta cujo nome não contém NENHUM nome canônico, e esse caso segue sendo
     * aviso, nunca divergência.
     */
    @Test
    void identidadeDerivadaCobreObraSemApelidosDeclarados() {
        GerenciadorContexto g = gerenciador();

        assertTrue(SEM_VOCABULARIO.reconhecePasta("Sem Vocabulario"),
            "sem apelidos, a obra ainda se reconhece pelo nome de exibição derivado");
        assertEquals(Set.of("sem_vocabulario"), g.idsQueReconhecem("[Grupo] Sem Vocabulario [1080p]"));
        assertEquals(Set.of("sem_vocabulario"), g.idsQueReconhecem("sem_vocabulario"),
            "o id também é identidade derivada");
        assertTrue(g.idsQueReconhecem("Obra Totalmente Outra").isEmpty(),
            "o que continua irreconhecível é a pasta sem nenhum nome canônico dentro");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: entrada mais específica vence a mais genérica do mesmo franchise.
     * Sem isso, {@code "Gundam 0083 Stardust Memory"} empataria entre a obra e um hipotético
     * guarda-chuva, e uma tradução legítima seria bloqueada por ambiguidade inventada.
     *
     * <p>INVARIANTES DO DOMÍNIO: a resolução devolve APENAS os ids de especificidade máxima; o
     * genérico não aparece no resultado quando o específico casou.
     */
    @Test
    void entradaMaisEspecificaVenceAMaisGenericaDoMesmoFranchise() {
        LoreFake franquia = new LoreFake("gundam_uc", "Gundam UC", "Lore UC.", Set.of("Gundam"));
        GerenciadorContexto g = new GerenciadorContexto(List.of(franquia, ZERO_083));

        assertEquals(Set.of("gundam_0083"),
            g.idsQueReconhecem("[Joseki] Mobile Suit Gundam 0083 Stardust Memory COMPLETE (1991)"),
            "'Gundam 0083' (2 palavras) é mais específico que 'Gundam' (1) e ganha sozinho");
        assertEquals(Set.of("gundam_uc"), g.idsQueReconhecem("Mobile Suit Gundam Unicorn"),
            "onde o específico não casa, o genérico continua identificando a obra");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: quando duas obras DISTINTAS reivindicam a pasta com a MESMA
     * precisão, a identidade não resolve. O gerenciador devolve as duas — é assim que a
     * ambiguidade chega ao validador, que a transforma em bloqueio. Devolver uma escolhida
     * arbitrariamente seria adivinhar a obra, que é o erro que esta guarda existe para impedir.
     */
    @Test
    void empateDeEspecificidadeEntreObrasDistintasDevolveAsDuas() {
        LoreFake primeira = new LoreFake("obra_a", "Obra A", "Lore A.", Set.of("Filme Duplo"));
        LoreFake segunda = new LoreFake("obra_b", "Obra B", "Lore B.", Set.of("Duplo Filme"));
        GerenciadorContexto g = new GerenciadorContexto(List.of(primeira, segunda));

        assertEquals(Set.of("obra_a", "obra_b"),
            g.idsQueReconhecem("[Grupo] Filme Duplo Filme [BD]"),
            "os dois apelidos têm 2 palavras e ambos casam: empate real, sem desempate arbitrário");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: duas obras declarando o MESMO nome canônico é erro de CONFIGURAÇÃO
     * do catálogo de lores — nenhuma pasta com esse nome jamais resolveria para uma obra única,
     * e toda tradução dela viraria bloqueio por ambiguidade, arquivo a arquivo. Erro de
     * configuração se paga no BOOT, alto e cedo, e não em uma mensagem por episódio depois que o
     * operador já mandou o lote rodar.
     *
     * <p>INVARIANTES DO DOMÍNIO: a colisão é por nome EXATO entre provedores DIFERENTES; nomes
     * que apenas se contêm são hierarquia legítima e NÃO derrubam o boot.
     */
    @Test
    void colisaoDeNomeCanonicoEntreObrasImpedeOStartup() {
        LoreFake filme1 = new LoreFake("break_blade_1", "Break Blade Filme 1", "Lore 1.", Set.of("Break Blade"));
        LoreFake filme2 = new LoreFake("break_blade_2", "Break Blade Filme 2", "Lore 2.", Set.of("Break Blade"));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
            () -> new GerenciadorContexto(List.of(filme1, filme2)));

        assertTrue(erro.getMessage().contains("break blade"), erro.getMessage());
        assertTrue(erro.getMessage().contains("break_blade_1"), erro.getMessage());
        assertTrue(erro.getMessage().contains("break_blade_2"), erro.getMessage());
        assertTrue(erro.getMessage().contains("CONFIGURAÇÃO"),
            "a mensagem precisa dizer que é defeito de catálogo, não de dados: " + erro.getMessage());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: nome canônico que CONTÉM outro não é colisão — é a hierarquia normal
     * de franquia/temporada, resolvida em runtime por especificidade. Tratar contenção como
     * colisão tornaria impossível ter "Macross 7" e "Macross 7 Encore" no mesmo catálogo.
     */
    @Test
    void nomeCanonicoQueApenasConttemOutroNaoEhColisao() {
        LoreFake serie = new LoreFake("macross_7", "Macross 7", "Lore 7.", Set.of());
        LoreFake encore = new LoreFake("macross_7_encore", "Macross 7 Encore", "Lore Encore.", Set.of());

        GerenciadorContexto g = new GerenciadorContexto(List.of(serie, encore));

        assertEquals(Set.of("macross_7_encore"), g.idsQueReconhecem("Macross 7 Encore [BD]"));
        assertEquals(Set.of("macross_7"), g.idsQueReconhecem("Macross 7 [BD]"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: repetir o mesmo nome DENTRO da mesma obra (um apelido igual ao id,
     * por exemplo) é redundância inofensiva, não colisão — a identidade é um conjunto.
     */
    @Test
    void apelidoIgualAoProprioIdNaoEhColisao() {
        LoreFake redundante = new LoreFake("guilty_crown", "Guilty Crown", "Lore GC.",
            Set.of("Guilty Crown", "guilty_crown"));

        GerenciadorContexto g = new GerenciadorContexto(List.of(redundante));

        assertEquals(Set.of("guilty_crown"), g.idsQueReconhecem("[Anime Time] Guilty Crown + OVA [BD]"));
    }

    @Test
    void nomeNuloOuEmBrancoNaoReconheceNada() {
        GerenciadorContexto g = gerenciador();

        assertEquals(Set.of(), g.idsQueReconhecem(null));
        assertEquals(Set.of(), g.idsQueReconhecem("   "));
        assertFalse(ZERO_083.reconhecePasta(null));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o snapshot é o antídoto ao contexto global mutável. Uma troca de
     * obra depois da fotografia não pode alterar nenhum campo dela.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se o snapshot voltasse a ler o provedor ativo, o
     * arquivo em curso passaria a ser traduzido, validado e carimbado com a obra nova no meio
     * do caminho — o modo de falha que produziu os caches contaminados.
     */
    @Test
    void snapshotNaoMudaQuandoOContextoGlobalMudaDepois() {
        GerenciadorContexto g = gerenciador();
        g.definirContextoAtivo("gundam_0083");
        SnapshotContexto antes = g.snapshotAtivo();

        g.definirContextoAtivo("guilty_crown");
        SnapshotContexto depois = g.snapshotAtivo();

        assertEquals("gundam_0083", antes.id(), "o snapshot capturado continua sendo o de 0083");
        assertEquals("Gundam 0083", antes.nomeExibicao());
        assertEquals(Set.of("Gundam 0083"), antes.termosProtegidos());
        assertEquals(ZERO_083.obterPromptSistema(), antes.promptSistema());

        assertEquals("guilty_crown", depois.id(), "um snapshot NOVO reflete o contexto novo");
        assertNotEquals(antes.promptSistema(), depois.promptSistema());
    }

    @Test
    void semContextoAtivoOSnapshotEhNeutro() {
        assertEquals(SnapshotContexto.NEUTRO, new GerenciadorContexto(List.of()).snapshotAtivo());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: um job pede o contexto pelo id que o operador escolheu — e o
     * recebe congelado, INDEPENDENTE de qual obra esteja ativa no gerenciador. É o que
     * permite ao lote inteiro rodar sob a obra pedida mesmo que outra rota (correção,
     * revisão, karaokê) troque o ativo global no meio.
     *
     * <p>INVARIANTES DO DOMÍNIO: o snapshot vem do provedor pedido, não do ativo; e a
     * chamada não altera o ativo — quem quer trocar chama {@code definirContextoAtivo}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se {@code snapshotPorId} lesse ou escrevesse o
     * ativo, o lote voltaria a depender de estado global compartilhado.
     */
    @Test
    void snapshotPorIdIgnoraOAtivoENaoOAltera() {
        GerenciadorContexto g = gerenciador();
        g.definirContextoAtivo("guilty_crown");

        SnapshotContexto pedido = g.snapshotPorId("gundam_0083");

        assertEquals("gundam_0083", pedido.id(), "o snapshot é do contexto PEDIDO, não do ativo");
        assertEquals(ZERO_083.obterPromptSistema(), pedido.promptSistema());
        assertEquals("guilty_crown", g.obterIdContextoAtivo(),
            "congelar o contexto de um job não pode ter efeito colateral sobre o ativo global");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: um id ausente ou desconhecido é erro, nunca fallback. Cair no
     * contexto padrão (ou no ativo anterior) traduziria o lote inteiro com a lore errada em
     * silêncio — exatamente o modo de falha do incidente que originou o congelamento.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer degradação para NEUTRO ou para o padrão
     * reprova aqui.
     */
    @Test
    void snapshotPorIdRecusaIdAusenteOuDesconhecidoSemFallbackSilencioso() {
        GerenciadorContexto g = gerenciador();

        assertThrows(ContextoNaoEncontradoException.class, () -> g.snapshotPorId(null));
        assertThrows(ContextoNaoEncontradoException.class, () -> g.snapshotPorId("  "));
        assertThrows(ContextoNaoEncontradoException.class, () -> g.snapshotPorId("obra_inexistente"));
    }
}
