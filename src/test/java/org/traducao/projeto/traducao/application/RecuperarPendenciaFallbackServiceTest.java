package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducao.domain.ports.FallbackTraducaoMaquinaPort;
import org.traducao.projeto.traducao.infrastructure.config.FallbackOnlineProperties;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa o contrato do {@link RecuperarPendenciaFallbackService} — opt-in,
 * escopo restrito às pendências informadas e guarda de nomes próprios — sem rede.
 *
 * <p>INVARIANTES DO DOMÍNIO: desligado não chama a porta; ligado só aceita respostas que
 * preservam nomes próprios; recusa é segura (fala omitida do resultado).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer desvio de gate, de guarda ou de escopo reprova.
 */
class RecuperarPendenciaFallbackServiceTest {

    private static RecuperarPendenciaFallbackService servico(boolean ativo, FallbackTraducaoMaquinaPort porta) {
        return new RecuperarPendenciaFallbackService(new FallbackOnlineProperties(ativo), porta);
    }

    private static LinkedHashSet<String> conjunto(String... itens) {
        return new LinkedHashSet<>(Set.of(itens));
    }

    @Test
    @DisplayName("desligado: não chama a porta e devolve mapa vazio")
    void desligadoNaoChamaPorta() {
        boolean[] chamou = {false};
        FallbackTraducaoMaquinaPort porta = o -> { chamou[0] = true; return Optional.of("x"); };

        Map<String, String> r = servico(false, porta).recuperar(conjunto("Hello"));

        assertTrue(r.isEmpty());
        assertTrue(!chamou[0], "porta não pode ser chamada com o modo desligado");
    }

    @Test
    @DisplayName("ligado: devolve a tradução quando a porta responde e os nomes são preservados")
    void ligadoRecuperaComNomePreservado() {
        FallbackTraducaoMaquinaPort porta = o -> Optional.of("Eu vi o Lena ontem");

        Map<String, String> r = servico(true, porta).recuperar(conjunto("I saw Lena yesterday"));

        assertEquals("Eu vi o Lena ontem", r.get("I saw Lena yesterday"));
    }

    @Test
    @DisplayName("ligado: recusa (mantém pendente) quando o nome próprio mid-sentence some")
    void ligadoRecusaQuandoNomePropioSome() {
        // "Lena" é mid-sentence e capitalizado; a tradução a perdeu -> recusa segura.
        FallbackTraducaoMaquinaPort porta = o -> Optional.of("Eu vi ela ontem");

        Map<String, String> r = servico(true, porta).recuperar(conjunto("I saw Lena yesterday"));

        assertTrue(r.isEmpty(), "nome próprio perdido deve manter a fala pendente");
    }

    @Test
    @DisplayName("ligado: capital de início de frase não é tratado como nome próprio")
    void capitalDeInicioNaoEhNomeProprio() {
        // "Why" abre a frase; a tradução PT não o contém e mesmo assim é aceita.
        FallbackTraducaoMaquinaPort porta = o -> Optional.of("Por que temos que aguentar isso");

        Map<String, String> r = servico(true, porta).recuperar(conjunto("Why do we have to put up with this"));

        assertEquals("Por que temos que aguentar isso", r.get("Why do we have to put up with this"));
    }

    @Test
    @DisplayName("ligado: porta vazia (rede/recusa) omite a fala do resultado")
    void portaVaziaOmiteFala() {
        FallbackTraducaoMaquinaPort porta = o -> Optional.empty();

        Map<String, String> r = servico(true, porta).recuperar(conjunto("Hello there"));

        assertTrue(r.isEmpty());
    }

    @Test
    @DisplayName("#7: nome próprio após token numérico no início de frase é verificado (não escapa a guarda)")
    void nomePropioAposTokenNumericoEhVerificado() {
        // "Zaku" vem após "42" (token sem letras que não encerra frase); a guarda não pode
        // tratá-lo como início de frase e deixá-lo escapar. A tradução perdeu "Zaku" -> recusa.
        FallbackTraducaoMaquinaPort porta = o -> Optional.of("Pare! 42 caiu.");

        Map<String, String> r = servico(true, porta).recuperar(conjunto("Stop! 42 Zaku fell."));

        assertTrue(r.isEmpty(), "nome próprio perdido após token numérico deve manter a fala pendente");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: caracteriza (subfase F1) o DEFEITO hoje vigente na guarda de nomes
     * próprios — tratar QUALQUER palavra capitalizada no meio da frase como nome próprio
     * obrigatório. Consequência real: um título em Title Case jamais pode ser traduzido, porque
     * a tradução correta necessariamente substitui as palavras capitalizadas. Nenhum provedor
     * de fallback contorna isso: a recusa acontece DEPOIS da resposta, sobre ela.
     *
     * <p>INVARIANTES DO DOMÍNIO: este teste fixa o comportamento ATUAL (recusa) para que a
     * correção da subfase F3 apareça como inversão explícita no diff. Ele NÃO descreve o
     * comportamento desejado — descreve o bug.
     *
     * <p>Medição da subfase F0 sobre as 560 falas pendentes reais dos caches versionados:
     * <b>323 delas (57,7%)</b> são recusadas exclusivamente por palavra capitalizada comum que
     * não é termo de lore, sigla nem identificador. Outras 35 (6,2%) dependem de termo de lore
     * legítimo e devem continuar protegidas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se este teste passar a falhar sem que F3 tenha sido
     * aplicada, a guarda mudou por acidente — investigar antes de ajustar a expectativa.
     */
    @Test
    @DisplayName("F1 (defeito atual): título em Title Case é recusado — 'Battle' tratado como nome próprio")
    void caracterizacaoTituloTitleCaseEhRecusadoHoje() {
        // Título real do Gundam 0083. A tradução está CORRETA, mas "Battle", "Three" e
        // "Dimensions" somem (como devem sumir) e a guarda recusa a fala inteira.
        FallbackTraducaoMaquinaPort porta = o -> Optional.of("A Batalha em Três Dimensões");

        Map<String, String> r = servico(true, porta).recuperar(conjunto("The Battle in Three Dimensions"));

        assertTrue(r.isEmpty(),
            "CARACTERIZAÇÃO DO DEFEITO: hoje a guarda recusa um título corretamente traduzido, "
                + "porque exige que 'Battle'/'Three'/'Dimensions' sobrevivam em português");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: caracteriza o mesmo defeito no padrão de DATA/legenda de época
     * ("May 12th, Stellar Year 2148"), que aparece com alta frequência nas pendências reais
     * (Stellar 42x, Year 43x na medição F0). "Stellar" e "Year" não são nomes próprios, mas a
     * guarda os exige.
     * <p>INVARIANTES DO DOMÍNIO: fixa o comportamento atual; F3 inverte.
     * <p>COMPORTAMENTO EM CASO DE FALHA: ver o teste irmão acima.
     */
    @Test
    @DisplayName("F1 (defeito atual): data de época é recusada — 'Stellar'/'Year' tratados como nome próprio")
    void caracterizacaoDataDeEpocaEhRecusadaHoje() {
        FallbackTraducaoMaquinaPort porta = o -> Optional.of("12 de maio, Ano Estelar 2148");

        Map<String, String> r = servico(true, porta).recuperar(conjunto("May 12th, Stellar Year 2148"));

        assertTrue(r.isEmpty(),
            "CARACTERIZAÇÃO DO DEFEITO: 'Stellar' e 'Year' são exigidos literalmente em português");
    }

    @Test
    @DisplayName("ligado: pontuação isolada encerra a frase; a 1ª palavra da frase seguinte não é nome próprio")
    void pontuacaoIsoladaEncerraFrase() {
        // "London" abre a 2ª frase (após o ponto isolado) e foi legitimamente traduzido para
        // "Londres"; não pode ser tratado como nome próprio obrigatório e reprovar a recuperação.
        FallbackTraducaoMaquinaPort porta = o -> Optional.of("Ele saiu . Londres chama");

        Map<String, String> r = servico(true, porta).recuperar(conjunto("He left . London calls"));

        assertEquals("Ele saiu . Londres chama", r.get("He left . London calls"),
            "palavra inicial da frase após pontuação isolada não é nome próprio obrigatório");
    }
}
