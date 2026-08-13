package org.traducao.projeto.core.texto.dicionarioOrtografia;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova as duas metades do contrato do dicionário — que ele ACUSA o que não
 * existe em português e, principalmente, que a ausência do verificador NÃO vira aprovação.
 *
 * <h2>A metade que roda sempre</h2>
 * O caminho da indisponibilidade é o mais importante e não depende de nada instalado: é ele que
 * decide se o pipeline vai dizer "ortografia limpa" quando na verdade não olhou. Esse teste roda em
 * qualquer máquina, inclusive no Docker sem hunspell.
 *
 * <h2>A metade que depende do pré-requisito</h2>
 * A verificação real PULA por {@link Assumptions} quando o hunspell não está instalado — declarado
 * como NÃO VERIFICADO, jamais como sucesso. Instalar:
 * {@code choco install hunspell.portable} e o dicionário {@code pt_BR}.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se o adaptador passar a acusar palavra correta, ou a se declarar disponível sem ter respondido,
 * reprova aqui.
 */
@DisplayName("dicionário do sistema: acusa o inexistente e nunca aprova por cegueira")
class HunspellDicionarioAdapterTest {

    /** Caminho de binário que não existe em máquina nenhuma. */
    private static final String INEXISTENTE = "hunspell-que-nao-existe-em-lugar-nenhum";

    @Test
    @DisplayName("FALHA FECHADA: sem o verificador, nada é acusado E nada é dado por verificado")
    void semVerificadorNaoAcusaEnaoAprova() {
        var adapter = new HunspellDicionarioAdapter(INEXISTENTE, "pt_BR");

        Set<String> r = adapter.desconhecidas(List.of("organizacao", "xyzabc", "vamos"));

        assertTrue(r.isEmpty(),
            "sem verificador não se pode acusar ninguém — seria inventar defeito");
        assertFalse(adapter.disponivel(),
            "ESTADO 2 PERDIDO: o adaptador se diz disponível sem ter verificado nada. É assim que "
                + "'não olhei' vira 'está limpo' no relatório, que é o defeito da regra 12.");
    }

    @Test
    @DisplayName("antes de qualquer consulta, o adaptador é INDISPONÍVEL")
    void semNenhumaConsultaAindaEhIndisponivel() {
        assertFalse(new HunspellDicionarioAdapter(INEXISTENTE, "pt_BR").disponivel(),
            "a resposta conservadora é a única honesta antes da primeira consulta");
    }

    @Test
    @DisplayName("entrada vazia não dispara processo nem acusa nada")
    void entradaVaziaNaoFazNada() {
        var adapter = new HunspellDicionarioAdapter(INEXISTENTE, "pt_BR");
        assertTrue(adapter.desconhecidas(List.of()).isEmpty());
        assertTrue(adapter.desconhecidas(null).isEmpty());
    }

    @Test
    @DisplayName("com hunspell instalado: acusa o que não existe e poupa o que existe")
    void comHunspellInstaladoSeparaOqueExisteDoQueNao() {
        var adapter = new HunspellDicionarioAdapter("hunspell", "pt_BR");
        Set<String> r = adapter.desconhecidas(List.of(
            "organizacao", "observacao", "inutil", "xyzabcdef",
            "organização", "observação", "inútil", "vamos", "estamos", "criança"));

        Assumptions.assumeTrue(adapter.disponivel(),
            "hunspell/pt_BR ausente — NÃO VERIFICADO. Instale: choco install hunspell.portable");

        // CONTROLE POSITIVO: as formas sem acento não existem em português.
        assertTrue(r.contains("organizacao"), "forma sem acento tem de ser acusada");
        assertTrue(r.contains("inutil"), "forma sem acento tem de ser acusada");
        assertTrue(r.contains("xyzabcdef"), "palavra inventada tem de ser acusada");
        // CONTROLE NEGATIVO: o que existe não pode ser acusado — é o alarme falso que
        // desmoralizaria a correção inteira.
        assertFalse(r.contains("organização"), "alarme falso: forma correta acusada");
        assertFalse(r.contains("criança"), "alarme falso: forma correta acusada");
        assertFalse(r.contains("vamos"), "alarme falso: 'vamos' não leva acento e é a palavra mais "
            + "frequente do acervo — acusá-la reescreveria 1.787 falas");
        assertFalse(r.contains("estamos"), "alarme falso: forma correta acusada");
    }

    @Test
    @DisplayName("com hunspell instalado: a grafia devolvida é EXATAMENTE a recebida")
    void preservaAgrafiaRecebida() {
        var adapter = new HunspellDicionarioAdapter("hunspell", "pt_BR");
        Set<String> r = adapter.desconhecidas(List.of("Organizacao", "ORGANIZACAO"));
        Assumptions.assumeTrue(adapter.disponivel(), "hunspell ausente — NÃO VERIFICADO");

        assertEquals(2, r.size(), "as duas caixas são formas distintas e as duas são inválidas");
        assertTrue(r.contains("Organizacao") && r.contains("ORGANIZACAO"),
            "devolver a palavra normalizada quebraria quem for casar com o texto original");
    }
}



