package org.traducao.projeto.lore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * PROPÓSITO DE NEGÓCIO: impede que a CICATRIZ do arquivo de lore — a medição real escrita em
 * comentário — seja apagada em silêncio por alguém que regenere o YAML e copie por cima.
 *
 * <h2>O risco concreto que ela cobre</h2>
 * O `lore.yaml` é <b>gerado</b> por {@code GeradorLoreYamlIT}, e o gerado tem
 * <b>ZERO comentário</b>. As 377 linhas de cicatriz foram migradas À MÃO das classes Java em
 * 2026-08-15: são as contagens medidas ({@code # 67}, {@code # ja existia; 0 ocorrencias}), o
 * histórico do Spearhead e as decisões de "FICA DE FORA" com o motivo. Um único
 * {@code Copy-Item} de {@code build/tmp/} por cima apaga tudo — e <b>nenhuma outra guarda
 * perceberia</b>, porque todas as outras comparam DADO, e comentário não é dado. O arquivo
 * continuaria passando na equivalência, nos dois baselines e no manifesto, verde e vazio de
 * história.
 *
 * <p>É a mesma família da guarda que ficou meses verde e cega varrendo diretório inexistente:
 * o que não é olhado não é protegido.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li><b>Catraca: o número só SOBE.</b> Migrar mais cicatriz é livre; perder reprova. Quando
 *       subir de verdade, atualize {@link #PISO} no mesmo commit — é registro de progresso,
 *       igual às outras linhas de base deste projeto.</li>
 *   <li>Conta linha cujo primeiro caractere não branco é {@code #}. <b>Limite declarado:</b> se
 *       algum dia um prompt trouxer uma linha começando por {@code #} dentro do bloco literal,
 *       ela entra na conta. Foi medido em 2026-08-15 que <b>nenhum dos 69 prompts</b> tem isso,
 *       e o piso nasceu do arquivo real — então a discriminação da catraca é sobre a cicatriz,
 *       que é o que ela existe para proteger.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Três estados, e o terceiro não é aprovação: arquivo ausente reprova como
 * {@code NÃO VERIFICADO} — "não achei comentário" e "não achei arquivo" não podem dar o mesmo
 * sinal. Contagem abaixo do piso reprova dizendo quantas linhas sumiram e como recuperá-las.
 */
@DisplayName("CATRACA: a cicatriz do arquivo de lore só pode CRESCER")
class CatracaCicatrizNoLoreYamlTest {

    private static final String RECURSO = "/lore/lore.yaml";

    /**
     * Linhas de comentário no arquivo em 2026-08-15, depois da migração das 8 classes que
     * concentravam 89% da cicatriz. Sobe quando mais for migrada; nunca desce.
     */
    private static final int PISO = 377;

    @Test
    @DisplayName("o arquivo de lore não perdeu linha de cicatriz")
    void cicatrizNaoEncolheu() throws IOException {
        String conteudo;
        try (InputStream in = getClass().getResourceAsStream(RECURSO)) {
            if (in == null) {
                fail("NÃO VERIFICADO: " + RECURSO + " não encontrado — a catraca não pôde medir, "
                    + "e isso não é aprovação.");
            }
            conteudo = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        long comentarios = conteudo.lines().filter(l -> l.stripLeading().startsWith("#")).count();

        assertTrue(comentarios >= PISO,
            () -> "A CICATRIZ do arquivo de lore encolheu: " + comentarios + " linhas de "
                + "comentário, piso é " + PISO + " (sumiram " + (PISO - comentarios) + ").\n"
                + "A causa quase certa é alguém ter copiado build/tmp/lore.yaml por "
                + "cima: o gerado tem ZERO comentário.\n"
                + "Recuperar: `git checkout -- src/main/resources/lore/lore.yaml` e, se "
                + "os DADOS precisavam mesmo ser regenerados, aplicar a regeneração preservando "
                + "os comentários em vez de sobrescrever o arquivo inteiro.");
    }
}
