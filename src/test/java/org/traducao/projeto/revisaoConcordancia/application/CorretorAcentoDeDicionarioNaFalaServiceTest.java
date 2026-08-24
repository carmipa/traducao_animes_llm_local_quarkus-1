package org.traducao.projeto.revisaoConcordancia.application;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: provar que a reposição de acento por dicionário corrige a palavra comum e
 * <b>não toca no nome próprio</b> — sem depender de catálogo de lore.
 *
 * <h2>Os três nomes deste teste são os três danos reais de 18/08/2026</h2>
 * <pre>
 *   Apsaras -> Apsarás   23 falas   (o mobile armor do 08th MS Team)
 *   Bosnia  -> Bósnia     7 falas   (uma NAVE do Zeta, nao o pais)
 *   Cardeas -> Cárdeas    2 falas   (Cardeas Vist, do Unicorn)
 * </pre>
 * O {@code Bosnia} é o pior deles: o acento transforma uma nave num país, e o resultado é
 * português impecável — defeito invisível para qualquer revisão que não confira contra o inglês.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem dicionário os casos declaram NÃO VERIFICADO em vez de passar por ausência.
 */
class CorretorAcentoDeDicionarioNaFalaServiceTest {

    private static CorretorAcentoDeDicionarioNaFalaService corretor;
    private static boolean dicionarioVivo;

    @BeforeAll
    static void montar() {
        CorretorOrtograficoLegenda dic = new CorretorOrtograficoLegenda();
        corretor = new CorretorAcentoDeDicionarioNaFalaService(dic);
        // SONDA antes de perguntar: o verificador nasce com estado indefinido e so se declara
        // disponivel depois de responder a primeira consulta. Perguntar `disponivel()` de cara
        // devolve false sem que nada esteja errado — e foi assim que estes testes reprovaram na
        // primeira execucao, acusando "dicionario ausente" com o dicionario instalado.
        corretor.corrigir("uma fala qualquer para acordar o verificador");
        dicionarioVivo = corretor.disponivel();
    }

    @Test
    @DisplayName("POSITIVO: a palavra comum que o dicionario rejeita ganha o acento")
    void acentuaPalavraComum() {
        assumirDicionario();
        Optional<String> r = corretor.corrigir("Estamos chegando à borda do territorio inimigo.");
        assertTrue(r.isPresent() && r.get().contains("território"),
            "nao corrigiu 'territorio', que o dicionario rejeita: " + r);
    }

    @Test
    @DisplayName("NEGATIVO: nome proprio no MEIO da fala fica intacto — os tres danos de 18/08")
    void naoTocaNomeProprioNoMeio() {
        assumirDicionario();
        for (String fala : new String[] {
            "Aquele e o mobile armor Apsaras, senhor.",
            "Envie um sinalizador para o Bosnia agora.",
            "As ordens vieram de Cardeas Vist em pessoa."
        }) {
            Optional<String> r = corretor.corrigir(fala);
            String resultado = r.orElse(fala);
            assertFalse(resultado.contains("Apsarás") || resultado.contains("Bósnia")
                    || resultado.contains("Cárdeas"),
                "acentuou nome proprio — e o dano real de 18/08/2026: " + resultado);
        }
    }

    /**
     * O contraponto que impede a guarda de virar demolição: palavra em INÍCIO de frase é
     * capitalizada por regra de escrita, não por ser nome, e continua precisando da correção.
     */
    @Test
    @DisplayName("palavra capitalizada que ABRE frase continua sendo corrigida")
    void inicioDeFraseNaoEnomeProprio() {
        assertTrue(CorretorAcentoDeDicionarioNaFalaService
                .nomesPropriosNoMeioDaFala("Necessario que venha.").isEmpty(),
            "tratou como nome proprio a primeira palavra da fala");
        assertTrue(CorretorAcentoDeDicionarioNaFalaService
                .nomesPropriosNoMeioDaFala("Chega. Necessario que venha.").isEmpty(),
            "tratou como nome proprio a palavra que abre a segunda frase");
    }

    @Test
    @DisplayName("a lista de intocaveis pega o nome no meio e ignora o do inicio")
    void listaDeIntocaveisDiscrimina() {
        Set<String> fora = CorretorAcentoDeDicionarioNaFalaService
            .nomesPropriosNoMeioDaFala("Aquele e o mobile armor Apsaras, senhor.");
        assertTrue(fora.contains("Apsaras"), "o nome do meio da fala tinha de entrar: " + fora);

        Set<String> tag = CorretorAcentoDeDicionarioNaFalaService
            .nomesPropriosNoMeioDaFala("{\\i1}Apsaras{\\i0} avanca sobre nos.");
        assertTrue(tag.isEmpty(),
            "a tag antes do nome escondeu que ele ABRE a fala: " + tag);
    }

    @Test
    @DisplayName("tag e quebra do ASS voltam byte a byte")
    void preservaMarcacao() {
        assumirDicionario();
        String entrada = "{\\i1}Estamos na borda do territorio{\\i0}\\Ninimigo agora.";
        Optional<String> r = corretor.corrigir(entrada);
        if (r.isEmpty()) {
            return;
        }
        assertTrue(r.get().startsWith("{\\i1}") && r.get().contains("{\\i0}")
                && r.get().contains("\\N"),
            "a marcacao do ASS foi corrompida: " + r.get());
    }

    @Test
    @DisplayName("entrada degenerada nao lanca")
    void degenerada() {
        assertEquals(Optional.empty(), corretor.corrigir(null));
        assertEquals(Optional.empty(), corretor.corrigir(""));
        assertEquals(Optional.empty(), corretor.corrigir("   "));
        assertEquals(Set.of(), CorretorAcentoDeDicionarioNaFalaService.nomesPropriosNoMeioDaFala(null));
    }

    private static void assumirDicionario() {
        assertTrue(dicionarioVivo,
            "NAO VERIFICADO: dicionario pt_BR ausente — nenhum caso abaixo prova nada");
    }
}
