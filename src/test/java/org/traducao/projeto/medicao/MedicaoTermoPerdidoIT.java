package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.traducao.projeto.lore.infrastructure.GerenciadorContexto;
import org.traducao.projeto.core.texto.FronteiraTermoAss;
import org.traducao.projeto.medicao.LeitorAcervoCache.Acervo;
import org.traducao.projeto.medicao.LeitorAcervoCache.FalaDoAcervo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * PROPÓSITO DE NEGÓCIO: para cada termo canônico declarado na lore, achar as falas em que ele
 * <b>estava no inglês e sumiu do português</b> — e mostrar o que apareceu no lugar.
 *
 * <h2>A lacuna que este harness fecha</h2>
 * {@code correcoesTerminologia} é um mapa forma-ruim → canônico, e só corrige as formas que
 * alguém escreveu nele. O modelo, porém, inventa formas novas. Medido no 86 em 05/08/2026: o mapa
 * cobria {@code "Cavaleiro da Morte"} e {@code "Coveiro"} para {@code "Undertaker"}, e o modelo
 * produziu <b>{@code "Fúnebre"}</b> e <b>{@code "Carrasco"}</b> — que passaram inteiras.
 *
 * <p>Isso já aconteceu três vezes com nomes diferentes ({@code Fa}, {@code Mobile Suit},
 * {@code Undertaker}) e sempre foi descoberto por acaso, olhando uma fala. Aqui a busca é
 * exaustiva: percorre TODOS os termos de TODAS as obras e devolve o inventário do que se perdeu,
 * ordenado por volume. Cada linha vira candidata a entrada no mapa — <b>com o número do lado</b>,
 * que é a exigência do projeto para entrada nova.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>READ-ONLY. Não decide nada: propõe com a contagem, para aprovação humana.</li>
 *   <li>A presença do termo é apurada por {@link FronteiraTermoAss} — a MESMA mecânica do
 *       enforcer, com quebra {@code \N} tratada dentro e fora do termo. Medir com outra régua
 *       daria números que não valem para quem vai consumir.</li>
 *   <li>Só conta quando o inglês TEM o termo: fala que nunca o mencionou não é perda.</li>
 *   <li>O contexto vem do {@code contextoId} CARIMBADO em cada arquivo, nunca do ativo global.</li>
 *   <li>Termo de UMA letra ou vazio é pulado: ruído garantido.</li>
 * </ul>
 *
 * <h2>FALSO POSITIVO CONHECIDO: plural</h2>
 * A busca usa fronteira de termo nas duas pontas, então {@code "Processor"} não casa dentro de
 * {@code "Processors"}. Uma fala cujo PT preservou o termo <b>no plural inglês</b> é reportada
 * como perda. Medido no 86 em 05/08/2026: {@code Morpho} (1) e {@code Processor} (2) apareceram
 * na lista sendo que o PT trazia {@code "Morphos"} e {@code "Processors"} — corretos.
 *
 * <p>Não foi "consertado" alargando a fronteira: aceitar sufixo faria {@code "Zeon"} casar dentro
 * de {@code "Zeonic"}, que é o defeito oposto e pior. A lista é de CANDIDATOS para leitura humana,
 * e ler a coluna de exemplo resolve o caso em um segundo. Quem usar o número bruto sem olhar os
 * exemplos vai superestimar a perda.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Contexto não resolvido faz o arquivo ser pulado e contado; nunca vira "sem perdas".
 *
 * <p>Uso: {@code gradlew test --tests "*MedicaoTermoPerdidoIT*" "-Dkronos.medicao=true"}
 * (opcional: {@code "-Dkronos.medicao.obra=86"})
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoTermoPerdidoIT {

    private static final int TETO_TERMOS = 25;
    private static final int TETO_EXEMPLOS = 3;

    @Inject
    GerenciadorContexto contextos;

    /** Um termo canônico que o inglês trazia e a tradução não devolveu. */
    private record Perda(String obra, String termo, int vezes, List<String> exemplos) {
    }

    @Test
    @DisplayName("acervo: termo canonico que estava no ingles e sumiu do portugues")
    void medir() throws IOException {
        Acervo acervo = LeitorAcervoCache.ler(LeitorAcervoCache.raizPadrao());
        if (acervo.vazio()) {
            System.out.println("SEM ACERVO — nada medido.");
            return;
        }
        String filtro = System.getProperty("kronos.medicao.obra");

        Map<String, Set<String>> termosPorContexto = new LinkedHashMap<>();
        for (ProvedorContexto p : contextos.getProvedores()) {
            termosPorContexto.put(p.getId(), p.termosProtegidos());
        }

        Map<String, Perda> perdas = new TreeMap<>();
        int pulados = 0;
        for (FalaDoAcervo f : acervo.falas()) {
            if (filtro != null && !f.obra().toLowerCase(Locale.ROOT)
                .contains(filtro.toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (f.proveniencia() == null || f.original().isBlank() || f.traduzido().isBlank()) {
                continue;
            }
            Set<String> termos = termosPorContexto.get(f.proveniencia().contextoId());
            if (termos == null) {
                pulados++;
                continue;
            }
            for (String termo : termos) {
                if (termo == null || termo.strip().length() < 2) {
                    continue;
                }
                boolean noIngles = FronteiraTermoAss.padrao(termo).matcher(f.original()).find();
                if (!noIngles) {
                    continue;
                }
                boolean noPortugues = FronteiraTermoAss.padraoIgnorandoCaixa(termo)
                    .matcher(f.traduzido()).find();
                if (noPortugues) {
                    continue;
                }
                String chave = f.obra() + "/" + termo;
                Perda atual = perdas.get(chave);
                List<String> ex = atual == null ? new ArrayList<>() : atual.exemplos();
                if (ex.size() < TETO_EXEMPLOS) {
                    ex.add("EN " + recortar(visivel(f.original()))
                        + "  ->  PT " + recortar(visivel(f.traduzido())));
                }
                perdas.put(chave, new Perda(f.obra(), termo,
                    atual == null ? 1 : atual.vezes() + 1, ex));
            }
        }

        System.out.printf("%nfalas analisadas: %d | arquivos com contexto nao resolvido: %d%n",
            acervo.falas().size(), pulados);
        System.out.printf("termos canonicos que SUMIRAM em alguma fala: %d%n%n", perdas.size());

        perdas.values().stream()
            .sorted(Comparator.comparingInt(Perda::vezes).reversed())
            .limit(TETO_TERMOS)
            .forEach(p -> {
                System.out.printf("  %-22s %-30s %4d fala(s)%n",
                    recortarCurto(p.obra()), p.termo(), p.vezes());
                p.exemplos().forEach(e -> System.out.printf("        %s%n", e));
            });
        System.out.printf("%nTeto de impressao: %d termos, %d exemplos cada. Cada linha e "
            + "CANDIDATA a entrada em correcoesTerminologia — com o numero do lado, que e o que "
            + "o projeto exige. Nenhuma e decisao.%n", TETO_TERMOS, TETO_EXEMPLOS);
    }

    private static String visivel(String t) {
        return t.replaceAll("\\{[^}]*}", "").replace("\\N", " ").strip();
    }

    private static String recortar(String t) {
        return t.length() <= 46 ? t : t.substring(0, 46) + "…";
    }

    private static String recortarCurto(String t) {
        return t.length() <= 22 ? t : t.substring(0, 22) + "…";
    }
}
