package org.traducao.projeto.contexto;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * PROPÓSITO DE NEGÓCIO: gate de conteúdo da E7a. Prova que a extração do peer
 * {@code contexto} NÃO alterou nenhum prompt, nome de exibição, id ou termo protegido
 * das 59 lores descobertas por CDI — comparando o estado vivo pós-move com o manifesto
 * determinístico capturado ANTES do move ({@code /contexto/manifesto-lore.properties}).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O manifesto é pequeno/legível: {@code count}, {@code ids} (ordenados por id),
 *       {@code aggregate} e uma linha {@code id.<id>=<hash>} por provedor.</li>
 *   <li>A entrada do hash por provedor, em ordem estável e UTF-8, é
 *       {@code id + " " + nomeExibicao + " " + promptSistema + " " + termosProtegidos ordenados},
 *       com os {@code \r} removidos (normalização de line-ending). Os prompts são montados
 *       em COMPILE-TIME a partir de text blocks; conforme a fonte seja consultada com CRLF
 *       (Windows/{@code autocrlf}) ou LF, a compilação pode reter ou não o {@code \r}. Como
 *       {@code \r} não é conteúdo de lore, removê-lo torna o gate determinístico entre
 *       checkouts — sem afetar a detecção de qualquer mudança textual real.</li>
 *   <li>O agregado é o SHA-256 da concatenação {@code id\thash\n} na ordem por id.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer id, nome, prompt ou termo protegido que mude altera o hash correspondente e
 * o agregado, reprovando o teste — sinal de que a E7a tocou conteúdo de lore.
 */
@QuarkusTest
@DisplayName("E7a: proteção de conteúdo das lores (hash pré-move vs pós-move)")
class ProtecaoConteudoLoreTest {

    private static final String MANIFESTO = "/contexto/manifesto-lore.properties";

    @Inject
    List<ProvedorContexto> provedores;

    @Test
    @DisplayName("manifesto vivo pós-move é idêntico ao capturado pré-move (count/ids/hashes/aggregate)")
    void conteudoDasLoresPreservado() throws Exception {
        Properties esperado = new Properties();
        try (InputStream in = getClass().getResourceAsStream(MANIFESTO)) {
            assertNotNull(in, "Manifesto pré-move não encontrado: " + MANIFESTO);
            esperado.load(in);
        }

        List<ProvedorContexto> ord = provedores.stream()
            .sorted(Comparator.comparing(ProvedorContexto::getId))
            .toList();

        assertEquals(Integer.parseInt(esperado.getProperty("count")), ord.size(),
            "quantidade de provedores mudou");

        String idsVivos = ord.stream().map(ProvedorContexto::getId).collect(Collectors.joining(","));
        assertEquals(esperado.getProperty("ids"), idsVivos, "lista ordenada de ids mudou");

        StringBuilder agg = new StringBuilder();
        for (ProvedorContexto p : ord) {
            String termos = p.termosProtegidos().stream().sorted().collect(Collectors.joining(""));
            String entrada = p.getId() + " " + p.getNomeExibicao() + " " + p.obterPromptSistema() + " " + termos;
            String hashVivo = sha256(entrada.replace("\r", ""));
            assertEquals(esperado.getProperty("id." + p.getId()), hashVivo,
                "conteúdo (nome/prompt/termos) mudou para o provedor: " + p.getId());
            agg.append(p.getId()).append("\t").append(hashVivo).append("\n");
        }

        assertEquals(esperado.getProperty("aggregate"), sha256(agg.toString()),
            "hash agregado do conjunto de lores mudou");
    }

    private static String sha256(String s) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(d.length * 2);
        for (byte b : d) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
