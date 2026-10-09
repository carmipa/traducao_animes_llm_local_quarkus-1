package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.application.DetectorTraducaoIdenticaService;
import org.traducao.projeto.qualidadeTraducao.application.LoreAtivaFake;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.qualidadeTraducao.domain.LoreAtivaPort;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.traducao.projeto.qualidadeTraducao.application.RemovedorItalico;

/**
 * PROPÓSITO DE NEGÓCIO: o reparo de troca de entidade só entra se passar pelo MESMO portão que
 * acabou de reprovar a fala. Sem essa revalidação, o reparo vira porta dos fundos.
 *
 * <h2>Por que este teste existe</h2>
 * A revalidação foi escrita junto com o reparo e, na primeira rodada de mutação, <b>nenhum teste
 * caiu ao removê-la</b>. Uma salvaguarda sem teste é uma salvaguarda que alguém apaga na próxima
 * refatoração sem que nada avise — foi o mesmo buraco encontrado no dia anterior na régua de
 * resíduo, e ali também só a mutação denunciou.
 *
 * <h2>Invariantes do domínio</h2>
 * O reparo troca UMA palavra. A fala reparada pode continuar falhando por um motivo que o
 * primeiro veredicto escondeu — o portão devolve só a primeira causa. Aqui o segundo defeito é
 * resíduo em inglês, que sobrevive intacto à troca do nome.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se a revalidação sumir, fala com resíduo entra no cache disfarçada de conserto.
 */
class AvaliadorReparoRevalidaTest {

    private static final class LoreVazia implements LoreAtivaPort {
        @Override public Set<String> termosProtegidosAtivos() { return Set.of(); }
        @Override public String obterLoreAtiva() { return ""; }
    }

    private static AvaliadorTraducaoCache avaliador() {
        return new AvaliadorTraducaoCache(
            new MascaradorTags(),
            new DetectorTraducaoIdenticaService(new LoreVazia()),
            new ValidadorTraducaoService(LoreAtivaFake.comPares(List.of("Four", "Quattro"))),
            new VerificadorIdentificadorNumerico(LoreAtivaFake.vazia()),
                new RemovedorItalico(),
            new RestauradorFalaIdenticaSemItalico(new MascaradorTags(),
                new DetectorTraducaoIdenticaService(new LoreVazia()),
                new DescarteItalicoUltimoRecurso(), new LoreVazia()));
    }

    @Test
    @DisplayName("troca de entidade sozinha: o reparo entra")
    void reparoLimpoEntra() {
        assertEquals("Responda-me, Four!",
            avaliador().repararSePossivel("Answer me, Four!", "Responda-me, Quattro!"));
    }

    /**
     * O caso que a mutação expôs. A fala tem DOIS defeitos: a entidade trocada e o resíduo
     * {@code only} — a assinatura do LLM local com o "Just" enfático, medida em 33 falas do
     * acervo. Trocar o nome conserta o primeiro e não toca no segundo.
     */
    @Test
    @DisplayName("reparo que ainda falha por OUTRO motivo é recusado")
    void reparoQueAindaFalhaEhRecusado() {
        var av = avaliador();
        String traduzido = "Responda-me, Quattro!only watch me!";

        // O reparo textual acontece — o nome é corrigido...
        assertEquals("Responda-me, Four!only watch me!",
            new ValidadorTraducaoService(LoreAtivaFake.comPares(List.of("Four", "Quattro")))
                .repararTrocaDeEntidade("Answer me, Four!", traduzido),
            "o reparo troca o nome; quem julga se isso basta é o portão");

        // ...mas o resíduo continua lá, então a fala NÃO pode ser aceita.
        assertNotNull(av.motivoFalhaFinal("Answer me, Four!", "Responda-me, Four!only watch me!"),
            "a fala reparada ainda tem resíduo em inglês: o portão tem de reprovar");
        assertNull(av.repararSePossivel("Answer me, Four!", traduzido),
            "aceitar sem revalidar deixaria o resíduo entrar no cache como se fosse conserto");
    }

    @Test
    @DisplayName("fala sem troca de entidade não produz reparo")
    void semTrocaNaoHaReparo() {
        assertNull(avaliador().repararSePossivel("Answer me, Four!", "Responda-me, Four!"));
    }

    private static AvaliadorTraducaoCache avaliador0083() {
        var lore = LoreAtivaFake.comPares(List.of("Uraki", "Kou"), List.of("Keith", "Chuck"));
        return new AvaliadorTraducaoCache(
            new MascaradorTags(),
            new DetectorTraducaoIdenticaService(new LoreVazia()),
            new ValidadorTraducaoService(lore),
            new VerificadorIdentificadorNumerico(LoreAtivaFake.vazia()),
            new RemovedorItalico(),
            new RestauradorFalaIdenticaSemItalico(new MascaradorTags(),
                new DetectorTraducaoIdenticaService(new LoreVazia()),
                new DescarteItalicoUltimoRecurso(), new LoreVazia()));
    }

    /**
     * Entradas REAIS do cache do 0083 (09/10/2026) e de polaridade do Unicorn: o reaproveitamento as
     * recusa, e o conserto da entrada devolve a fala com só o defeito desfeito — o resto, que estava
     * certo, fica como estava (o modelo, chamado de novo, trocava "Core Fighter" por "Lutador
     * Central").
     */
    @Test
    @DisplayName("entrada de cache com defeito de par consertável é consertada, o resto intacto")
    void entradaDeCacheComDefeitoConsertavelEhConsertada() {
        var av = avaliador0083();
        assertEquals("Desista, Keith.", av.repararCacheSePossivel("Give it up, Keith.", "Desista, Chuck."));
        assertEquals("Keith, veja isso! Este\\Ntem um Core Fighter!",
            av.repararCacheSePossivel("Keith, check this out!\\NThis one's got a Core Fighter!",
                "Chuck, veja isso! Este\\Ntem um Core Fighter!"));
        assertEquals("Uraki! Keith! Formação Três!",
            av.repararCacheSePossivel("Uraki! Keith! Formation Three!", "Kou! Chuck! Formação Três!"));
        assertEquals("Não... é tarde demais.", av.repararCacheSePossivel("No... it's too late.", "Sim... é tarde demais."));
    }

    /**
     * A1: o mesmo sinal (entrada recusada pelo reaproveitamento) sem conserto seguro, ou com OUTRO
     * defeito além do consertado — vai ao modelo como antes. E entrada sã não é "consertada".
     */
    @Test
    @DisplayName("A1: sem conserto, conserto que ainda reprova, ou entrada sã — nada é consertado")
    void entradaSemConsertoSeguroNaoEhConsertada() {
        var av = avaliador0083();
        assertNull(av.repararCacheSePossivel("I'm relieved.", "Estou aliviado(a)."), "alternativa vai ao modelo");
        assertNull(av.repararCacheSePossivel("Give it up, Keith.", "Desista, Chuck(a)."),
            "o nome se conserta, a alternativa continua: o portão reprova o conserto");
        assertNull(av.repararCacheSePossivel("Give it up, Keith.", "Desista, Keith."), "entrada sã");
        assertNull(av.repararCacheSePossivel("Give it up, Keith.", null));
        assertNull(av.repararCacheSePossivel("Give it up, Keith.", "  "));
    }
}
