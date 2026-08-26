package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.lore.domain.SnapshotContexto;
import org.traducao.projeto.lore.infrastructure.GerenciadorContexto;
import org.traducao.projeto.medicao.LeitorAcervoCache.Acervo;
import org.traducao.projeto.medicao.LeitorAcervoCache.FalaDoAcervo;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * PROPÓSITO DE NEGÓCIO: responder, ANTES de mandar traduzir, <b>"o cache desta obra ainda vale?"</b>
 *
 * <h2>Por que esta pergunta é cara de errar</h2>
 * O {@code contextoHash} da proveniência é o SHA-256 do PROMPT DE SISTEMA. Mudar uma vírgula da
 * lore muda o hash, {@code mesmaProveniencia()} passa a dar falso, e a próxima Tradução Local
 * <b>descarta o acervo inteiro daquela obra e retraduz do zero</b> — horas de LLM para quem só
 * queria regerar o {@code .ass}.
 *
 * <p>O inverso também importa: mexer em {@code correcoesTerminologia()} ou em
 * {@code termosProtegidos()} <b>não</b> toca o prompt, então o hash NÃO muda e o cache segue
 * valendo. Os dois casos parecem "mexi na lore" para quem olha o diff, e têm consequências
 * opostas. Só o hash decide.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY. Compara o hash CARIMBADO em cada arquivo com o hash do prompt ATUAL.</li>
 *   <li>Compara também modelo e idiomas: são outros quatro dos seis campos de
 *       {@code mesmaProveniencia()}, e qualquer um deles sozinho invalida.</li>
 *   <li>Reporta por OBRA, com o veredito explícito — "REUSA" ou "RETRADUZ".</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Contexto não resolvido é reportado como INDETERMINADO, nunca como "reusa".
 *
 * <p>Uso: {@code gradlew test --tests "*ProvenienciaAindaValeIT*" "-Dkronos.medicao=true"}
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class ProvenienciaAindaValeIT {

    @Inject
    GerenciadorContexto contextos;

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE (regra 9) do instrumento desta verificação — a
     * comparação de hash do prompt, que é o que decide entre REUSAR o cache e RETRADUZIR do zero.
     *
     * <p>INVARIANTES DO DOMÍNIO: prompts diferentes têm de dar hashes diferentes, e o mesmo
     * prompt tem de dar o mesmo hash. Um {@code hashDe} que devolvesse constante reportaria
     * "todos reusam" sobre um acervo inteiro que na verdade seria retraduzido — e a diferença
     * entre os dois é uma noite de LLM.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: imprime e devolve {@code false}; nenhum veredito de
     * reuso é afirmado.
     */
    private static boolean instrumentoCalibrado() {
        String a = ProvenienciaCache.hashDe("Voce e um tradutor. Lore: Zeon.");
        String b = ProvenienciaCache.hashDe("Voce e um tradutor. Lore: AEUG.");
        String aDeNovo = ProvenienciaCache.hashDe("Voce e um tradutor. Lore: Zeon.");
        boolean separaOqueDifere = !a.equals(b);
        boolean juntaOqueEigual = a.equals(aDeNovo);
        if (separaOqueDifere && juntaOqueEigual) {
            System.out.println("  controle: prompts diferentes dao hashes diferentes · o mesmo "
                + "prompt da o mesmo hash");
            return true;
        }
        System.out.printf("INSTRUMENTO REPROVADO NO CONTROLE — separa=%s junta=%s. Nenhum "
            + "veredito de reuso e afirmado.%n", separaOqueDifere, juntaOqueEigual);
        return false;
    }

    @Test
    @DisplayName("acervo: a proxima Traducao Local REUSA o cache ou RETRADUZ do zero?")
    void verificar() throws IOException {
        if (!instrumentoCalibrado()) {
            return;
        }
        Acervo acervo = LeitorAcervoCache.ler(LeitorAcervoCache.raizPadrao());
        if (acervo.vazio()) {
            System.out.println("NAO VERIFICADO: acervo de cache vazio — 'nenhum arquivo retraduz' "
                + "aqui seria cegueira, e nao garantia de reuso.");
            return;
        }

        // obra -> [arquivos, reusam, hash divergente, indeterminados]
        Map<String, int[]> porObra = new TreeMap<>();
        Map<String, String> exemploDivergencia = new LinkedHashMap<>();
        java.util.Set<String> vistos = new java.util.HashSet<>();

        for (FalaDoAcervo f : acervo.falas()) {
            String chave = f.arquivo().toString();
            if (!vistos.add(chave)) {
                continue;   // um veredito por ARQUIVO, não por fala
            }
            int[] c = porObra.computeIfAbsent(f.obra(), k -> new int[4]);
            c[0]++;
            ProvenienciaCache gravada = f.proveniencia();
            if (gravada == null || gravada.contextoId() == null) {
                c[3]++;
                continue;
            }
            SnapshotContexto atual;
            try {
                atual = contextos.snapshotPorId(gravada.contextoId());
            } catch (RuntimeException e) {
                c[3]++;
                continue;
            }
            String hashAtual = ProvenienciaCache.hashDe(atual.promptSistema());
            if (hashAtual.equals(gravada.contextoHash())) {
                c[1]++;
            } else {
                c[2]++;
                exemploDivergencia.putIfAbsent(f.obra(),
                    "gravado " + curto(gravada.contextoHash()) + " != atual " + curto(hashAtual));
            }
        }

        System.out.printf("%n%-46s %7s %7s %9s %7s  %s%n",
            "OBRA", "arqs", "REUSA", "RETRADUZ", "indet.", "veredito");
        porObra.forEach((obra, c) -> System.out.printf("%-46s %7d %7d %9d %7d  %s%n",
            recortar(obra), c[0], c[1], c[2], c[3],
            c[2] > 0 ? "RETRADUZ >> " + exemploDivergencia.getOrDefault(obra, "")
                : c[3] > 0 ? "parcial (indeterminado)" : "reusa o cache"));

        int retraduzem = porObra.values().stream().mapToInt(c -> c[2]).sum();
        System.out.printf("%nTOTAL de arquivos que RETRADUZIRIAM do zero: %d%n", retraduzem);
        if (retraduzem == 0) {
            System.out.println("Seguro rodar a Traducao Local: nenhuma chamada ao LLM, so reemite.");
        } else {
            System.out.println("ATENCAO: rodar a Traducao Local nestas obras chama o LLM do zero.");
        }
    }

    private static String curto(String hash) {
        return hash == null ? "(nulo)" : hash.length() <= 8 ? hash : hash.substring(0, 8);
    }

    private static String recortar(String t) {
        return t.length() <= 46 ? t : t.substring(0, 46) + "…";
    }
}
