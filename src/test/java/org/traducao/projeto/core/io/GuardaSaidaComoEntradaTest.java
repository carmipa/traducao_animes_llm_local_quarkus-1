package org.traducao.projeto.core.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que a tradução recusa LER a pasta onde traduções são GRAVADAS.
 * É a revisão de BOA-FÉ virando mecanismo — até 12/08/2026 a cicatriz existia só como anotação.
 *
 * <h2>O prejuízo</h2>
 * 06/08/2026: uma tradução apontou para {@code legenda-simplificada}, que é pasta de SAÍDA, e
 * sobrescreveu <b>17 arquivos limpos</b>. Nenhuma varredura de segurança acharia isso: não há
 * ataque, há uma pessoa escolhendo a pasta errada numa lista de pastas parecidas.
 *
 * <h2>Invariantes que este teste trava</h2>
 * <ul>
 *   <li>Entrada igual à saída é sempre recusa.</li>
 *   <li>Nome de pasta de saída conhecida é recusa, inclusive as do confronto de modelos
 *       ({@code traducao_mistral}, {@code traducao_aya}), que são baseline e doem mais.</li>
 *   <li>Pasta de ENTRADA legítima nunca é recusada — guarda que reprova o uso correto ensina
 *       a desligar o alarme, e aqui desligar significa voltar a perder arquivo.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Testes puros com {@link TempDir}; sem acervo, sem rede.
 */
@DisplayName("boa-fé: traduzir a partir da pasta de saída é recusado na porta")
class GuardaSaidaComoEntradaTest {

    private final GuardaCaminhoEntrada guarda = new GuardaCaminhoEntrada();

    @Test
    @DisplayName("a cicatriz de 06/08: legenda-simplificada como ENTRADA é recusada")
    void recusaAPastaQueCustou17Arquivos(@TempDir Path base) throws Exception {
        Path saida = Files.createDirectories(base.resolve("legenda-simplificada"));

        var r = guarda.conferirEntradaNaoEhSaidaDeTraducao(saida.toString(), null);

        assertTrue(r.isPresent(), "a pasta que sobrescreveu 17 arquivos limpos tem de ser recusada");
        assertEquals(GuardaCaminhoEntrada.Motivo.SAIDA_COMO_ENTRADA, r.get().motivo());
        assertTrue(r.get().mensagem().contains("legendas_extraidas_ass"),
            "a recusa tem de ORIENTAR qual pasta o operador queria: " + r.get().mensagem());
    }

    @Test
    @DisplayName("o risco de HOJE: traducao_mistral e traducao_aya são baseline de comparação")
    void recusaAsPastasDoConfrontoDeModelos(@TempDir Path base) throws Exception {
        for (String nome : new String[] {"traducao_mistral", "traducao_aya", "traducao_ptbr"}) {
            Path p = Files.createDirectories(base.resolve(nome));
            assertTrue(guarda.conferirEntradaNaoEhSaidaDeTraducao(p.toString(), null).isPresent(),
                "apontar " + nome + " como entrada apaga o baseline de um experimento de dias");
        }
    }

    @Test
    @DisplayName("entrada IGUAL à saída: a tradução leria o que acabou de gravar")
    void recusaEntradaIgualASaida(@TempDir Path base) throws Exception {
        Path p = Files.createDirectories(base.resolve("legendas_extraidas_ass"));

        var r = guarda.conferirEntradaNaoEhSaidaDeTraducao(p.toString(), p.toString());

        assertTrue(r.isPresent(), "mesma pasta nos dois campos sobrescreve o original");
        assertTrue(r.get().mensagem().contains("MESMA"), r.get().mensagem());
    }

    @Test
    @DisplayName("CONTRAPROVA: pasta de entrada legítima passa, com e sem saída informada")
    void naoRecusaEntradaLegitima(@TempDir Path base) throws Exception {
        Path entrada = Files.createDirectories(base.resolve("legendas_extraidas_ass"));
        Path saida = Files.createDirectories(base.resolve("traducao_ptbr"));

        assertTrue(guarda.conferirEntradaNaoEhSaidaDeTraducao(entrada.toString(), null).isEmpty(),
            "legendas_extraidas_ass é a entrada canônica e não pode ser recusada");
        assertTrue(guarda.conferirEntradaNaoEhSaidaDeTraducao(entrada.toString(), saida.toString()).isEmpty(),
            "entrada legítima com saída DIFERENTE é o fluxo normal");

        Path eng = Files.createDirectories(base.resolve("legendas_eng"));
        assertTrue(guarda.conferirEntradaNaoEhSaidaDeTraducao(eng.toString(), null).isEmpty(),
            "legendas_eng também é entrada canônica");
        Path piloto = Files.createDirectories(base.resolve("_piloto_aya_eng"));
        assertTrue(guarda.conferirEntradaNaoEhSaidaDeTraducao(piloto.toString(), null).isEmpty(),
            "pasta de experimento com nome livre não pode ser confundida com saída");
    }

    @Test
    @DisplayName("entrada em branco ou caminho impossível não é problema DESTA guarda")
    void naoInvadeOEscopoDaOutraChecagem() {
        assertTrue(guarda.conferirEntradaNaoEhSaidaDeTraducao(null, null).isEmpty());
        assertTrue(guarda.conferirEntradaNaoEhSaidaDeTraducao("   ", null).isEmpty());
    }
}
