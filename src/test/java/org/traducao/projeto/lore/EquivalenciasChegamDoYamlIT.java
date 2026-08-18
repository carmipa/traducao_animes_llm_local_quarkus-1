package org.traducao.projeto.lore;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.revisaoLore.application.GerenciadorPromptRevisaoLore;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: provar que a equivalência declarada no {@code lore.yaml} CHEGA ao
 * provedor que a tela 3.2 consome — o degrau entre o dado e o uso.
 *
 * <h2>O prejuízo que originou, e ele tem uma hora de idade</h2>
 * Em 17/08/2026 o mecanismo de equivalências entrou com teste unitário verde e <b>não funcionou
 * no acervo</b>: a corrida no 86 Part 2 fechou com as MESMAS 309 pendências de antes, e
 * {@code Federacy} (30) e {@code Republic} (17) continuaram acusados.
 *
 * <p>A causa: o {@code lore.yaml} tem DOIS blocos por obra — {@code obras:} (tradução) e
 * {@code revisao:} — e eu escrevi a declaração no primeiro, enquanto o {@code CatalogoLoreYaml}
 * a lê do segundo. O teste unitário passava porque eu entregava o mapa na mão; ninguém provava
 * que ele vinha do arquivo.
 *
 * <p><b>A lição, e é por isso que este arquivo existe:</b> teste que constrói a entrada à mão
 * prova a REGRA, nunca a LIGAÇÃO. Enquanto o dado atravessa arquivo → leitor → provedor → caso de
 * uso, cada degrau precisa de alguém que o exercite de ponta a ponta.
 *
 * <h2>Invariantes do domínio</h2>
 * Lê pelo CDI, do catálogo REAL — não de fixture. Fixture aqui provaria o parser, não o arquivo
 * que o KRONOS carrega no boot.
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem lembra em qual seção do yaml a declaração precisa estar.
 */
@QuarkusTest
class EquivalenciasChegamDoYamlIT {

    @Inject
    GerenciadorPromptRevisaoLore gerenciador;

    @Test
    @DisplayName("as equivalencias do 86 chegam do lore.yaml ate o provedor da revisao")
    void equivalenciasDo86ChegamDoArquivo() {
        Map<String, List<String>> equivalencias =
            gerenciador.obterPrompt("eight_six").equivalenciasAceitas();

        assertFalse(equivalencias.isEmpty(),
            "o 86 declara equivalenciasAceitas no lore.yaml e elas chegaram VAZIAS. Confira em "
                + "qual secao a declaracao esta: o CatalogoLoreYaml le do bloco 'revisao:', nao "
                + "do bloco 'obras:'. Foi exatamente esse o erro de 17/08/2026, e ele passou por "
                + "um teste unitario verde.");

        for (String termo : List.of("federacy", "republic", "empire", "reaper")) {
            assertTrue(equivalencias.containsKey(termo),
                () -> "falta a equivalencia de '" + termo + "'. Declaradas: " + equivalencias.keySet());
        }

        assertTrue(equivalencias.get("reaper").contains("ceifador"),
            "o epiteto do Shin em portugues e 'Ceifador' e tem de estar declarado como aceito — "
                + "senao a tela volta a acusar traducao correta, e foi por acusa-la que 13 falas "
                + "chegaram a ser reescritas por engano");
    }

    /**
     * O controle usava {@code gundam_zeta} até 18/08/2026, quando o Zeta passou a declarar
     * equivalências de verdade ({@code A.E.U.G}, {@code Zabi Family}, {@code Earth Sphere},
     * {@code Psyco Gundam}). Trocado por uma obra que segue sem declarar nada — das 61 que
     * ainda não declaram, o Break Blade 1 é a mais distante das sete de Gundam, que é o que o
     * controle precisa: se a leitura vazar bloco de obra vizinha, aparece aqui.
     */
    @Test
    @DisplayName("CONTROLE: obra que nao declara nada devolve mapa vazio, nao nulo")
    void obraSemDeclaracaoDevolveVazio() {
        Map<String, List<String>> equivalencias =
            gerenciador.obterPrompt("break_blade_1").equivalenciasAceitas();

        assertTrue(equivalencias.isEmpty(),
            "o Break Blade 1 nao declara equivalencia nenhuma e devolveu " + equivalencias.keySet()
                + " — sinal de que a leitura esta pegando o bloco de outra obra");
    }
}
