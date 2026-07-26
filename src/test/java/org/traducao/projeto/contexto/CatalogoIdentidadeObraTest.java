package org.traducao.projeto.contexto;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.contexto.domain.IdentidadeObra;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: TESTE DE CATÁLOGO — percorre TODAS as lores registradas contra os
 * NOMES REAIS DE PASTA desta árvore e prova que cada pasta resolve para UMA obra, a certa. É a
 * rede que substitui a inspeção manual: até este passo, só duas obras declaravam vocabulário de
 * pasta, e o incidente medido (15 caches de Gundam 0083 gravados sob {@code guilty_crown},
 * ~4.442 entradas traduzidas com a lore errada) era possível em todas as outras 57.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Nenhuma COLISÃO de nome canônico entre obras — se houvesse, o boot já teria falhado
 *       (o {@code GerenciadorContexto} valida na construção); aqui a colisão é reafirmada com
 *       mensagem legível, para o diagnóstico não depender de um stack trace de CDI.</li>
 *   <li>Nenhuma pasta real é AMBÍGUA: cada uma resolve para no máximo um id.</li>
 *   <li>Os pares mínimos de NÚMERO/ANO/SIGLA ficam presos contra nomes de pasta reais:
 *       {@code 0083}×{@code 0080}, {@code 86}×o ano {@code (1986)} da pasta de ZZ,
 *       {@code Z Gundam}×{@code Gundam ZZ}, e {@code Gundam 00} (obra que este catálogo NÃO
 *       tem) não pode ser confundida com nenhuma que ele tem.</li>
 *   <li>Cobertura: 100% das obras têm identidade não vazia e se reconhecem pelo próprio id e
 *       pelo próprio nome de exibição. As LACUNAS conhecidas (pastas reais que ainda não
 *       resolvem) são declaradas nominalmente e valem como INDETERMINADO — avisam e não
 *       bloqueiam, que é o contrato da fase de migração.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * O inventário de pastas é LITERAL, capturado da árvore real ({@code cache/}, que é
 * gitignorada e não existe em clone limpo), justamente para o teste ser um registro estável do
 * mundo real em vez de depender do disco. Uma pasta que passe a resolver para a obra errada, a
 * empatar entre obras ou a deixar de resolver reprova aqui — antes da primeira chamada ao LLM,
 * não depois de milhares de entradas gravadas.
 */
@QuarkusTest
@DisplayName("catálogo de identidade de obra: todas as lores × nomes REAIS de pasta")
class CatalogoIdentidadeObraTest {

    @Inject
    List<ProvedorContexto> provedores;

    @Inject
    GerenciadorContexto gerenciador;

    /**
     * PROPÓSITO DE NEGÓCIO: inventário LITERAL das pastas de obra desta árvore, com a obra que
     * cada uma deve identificar. Conjunto VAZIO significa lacuna conhecida e aceita nesta fase:
     * a pasta não resolve, a guarda AVISA e a tradução segue.
     *
     * <p>INVARIANTES DO DOMÍNIO: os nomes são exatamente os que o sistema de arquivos entrega —
     * com grupo de fansub, ano, resolução e codec em volta do título. Normalizar ou encurtar
     * qualquer um deles aqui esvaziaria o teste: é justamente o ruído em volta que o
     * reconhecimento precisa atravessar.
     */
    private static final Map<String, Set<String>> PASTAS_REAIS = pastasReais();

    private static Map<String, Set<String>> pastasReais() {
        Map<String, Set<String>> mapa = new LinkedHashMap<>();
        mapa.put("86 Part 1", Set.of("eight_six"));
        mapa.put("86 Part 2", Set.of("eight_six"));
        mapa.put("Gundam Narrative NT", Set.of("gundam_nt"));
        mapa.put("[Anime Time] Guilty Crown + OVA [BD][Dual Audio][1080p][HEVC 10bit x265][Opus][Eng Sub]",
            Set.of("guilty_crown"));
        mapa.put("[Anime Time - Mistral Nemo] Guilty Crown + OVA [BD][Dual Audio][1080p][HEVC 10bit x265][Opus][Eng Sub]",
            Set.of("guilty_crown"));
        mapa.put("[Anime Time - Tower Mistral] Guilty Crown + OVA [BD][Dual Audio][1080p][HEVC 10bit x265][Opus][Eng Sub]",
            Set.of("guilty_crown"));
        mapa.put("[Joseki] Mobile Suit Gundam 0080 War in the Pocket COMPLETE (1989)(BD AV1 1080p Opus)[Sub Eng]",
            Set.of("gundam_0080"));
        mapa.put("[Joseki] Mobile Suit Gundam 0083 Stardust Memory COMPLETE (1991)(BD AV1 1080p Opus)[Sub Eng]",
            Set.of("gundam_0083"));
        mapa.put("[Joseki] Mobile Suit Gundam The 08th MS Team COMPLETE (1996)(BD AV1 1080p Opus)[Sub Eng]",
            Set.of("gundam_08ms"));
        mapa.put("[Joseki] Mobile Suit Gundam ZZ COMPLETE (1986)(BD AV1 1080p Opus)[Eng Sub][v2]",
            Set.of("gundam_zz"));
        mapa.put("[Joseki] Mobile Suit Z Gundam COMPLETE (1985)(BD AV1 1080p Opus)[Sub Eng][v2]",
            Set.of("gundam_zeta"));
        // LACUNA CONHECIDA: uma única pasta guarda os SEIS filmes de Break Blade, que são seis
        // entradas distintas do catálogo. Nenhuma regra determinística escolhe qual delas é —
        // e declarar "Break Blade" nas seis seria colisão de identidade, que derruba o boot.
        // Fica INDETERMINADO de propósito: avisa e deixa a tradução seguir.
        mapa.put("[Lulu] Break Blade Movies [BD 1080p Hi10 FLAC][Dual-Audio]", Set.of());
        // LACUNA CONHECIDA: pasta nomeada pelo operador ("filme1"), sem nada do título da obra.
        // Nenhum vocabulário pode alcançá-la — é o caso que prova que ausência vira aviso.
        mapa.put("filme1", Set.of());
        return mapa;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o teste central — toda pasta real do disco tem de resolver para a
     * obra certa, atravessando grupo de fansub, ano, resolução e codec.
     *
     * <p>INVARIANTES DO DOMÍNIO: consulta o MESMO caminho que a guarda usa em produção
     * ({@code idsQueReconhecem}), e não uma reimplementação — auditar uma cópia da regra
     * provaria a cópia, não o sistema.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: acumula TODAS as divergências e as reporta juntas, para
     * um ajuste de vocabulário não ser descoberto uma pasta por vez.
     */
    @Test
    @DisplayName("cada pasta REAL resolve para a obra certa (ou para a lacuna declarada)")
    void pastasReaisResolvemParaAObraCerta() {
        List<String> divergencias = new ArrayList<>();
        PASTAS_REAIS.forEach((pasta, esperado) -> {
            Set<String> resolvido = gerenciador.idsQueReconhecem(pasta);
            if (!resolvido.equals(esperado)) {
                divergencias.add("\"" + pasta + "\"\n      esperado: " + esperado + "\n      obtido:   " + resolvido);
            }
        });

        assertTrue(divergencias.isEmpty(),
            () -> "Pastas reais resolvendo para a obra errada (ou deixando de resolver). Cada linha é um "
                + "arquivo que seria traduzido sob a lore errada, ou uma tradução legítima que seria "
                + "bloqueada:\n  " + String.join("\n  ", divergencias));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: nenhuma pasta real pode empatar entre duas obras. Empate é bloqueio
     * em runtime — uma tradução legítima parada por defeito de catálogo.
     */
    @Test
    @DisplayName("nenhuma pasta REAL é ambígua entre duas obras")
    void nenhumaPastaRealEhAmbigua() {
        List<String> ambiguas = new ArrayList<>();
        PASTAS_REAIS.keySet().forEach(pasta -> {
            Set<String> resolvido = gerenciador.idsQueReconhecem(pasta);
            if (resolvido.size() > 1) {
                ambiguas.add("\"" + pasta + "\" -> " + resolvido);
            }
        });

        assertTrue(ambiguas.isEmpty(),
            () -> "Pastas reais reivindicadas por MAIS DE UMA obra com a mesma especificidade — a guarda "
                + "bloquearia a tradução delas. Desempate: apelido de pasta mais específico em uma das "
                + "lores.\n  " + String.join("\n  ", ambiguas));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reafirma, com mensagem legível, a invariante que o
     * {@code GerenciadorContexto} já impõe no boot — nenhum nome canônico pertence a duas obras.
     * Sem este teste, uma colisão introduzida por um apelido novo apareceria como uma falha de
     * deploy CDI em todo teste do projeto, sem apontar o culpado.
     */
    @Test
    @DisplayName("nenhuma COLISÃO de nome canônico entre obras do catálogo")
    void nenhumaColisaoDeNomeCanonicoEntreObras() {
        Map<String, Set<String>> idsPorNome = new TreeMap<>();
        for (IdentidadeObra identidade : gerenciador.identidades()) {
            for (String nome : identidade.nomesCanonicos()) {
                idsPorNome.computeIfAbsent(nome, chave -> new TreeSet<>()).add(identidade.contextoId());
            }
        }

        List<String> colisoes = idsPorNome.entrySet().stream()
            .filter(entrada -> entrada.getValue().size() > 1)
            .map(entrada -> "\"" + entrada.getKey() + "\" -> " + entrada.getValue())
            .toList();

        assertTrue(colisoes.isEmpty(),
            () -> "Colisão de identidade canônica: o mesmo nome identifica obras diferentes, então nenhuma "
                + "pasta com esse nome resolveria para uma obra única. É erro de CONFIGURAÇÃO do catálogo "
                + "e derruba o startup.\n  " + String.join("\n  ", colisoes));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: cobertura de 100% do catálogo — nenhuma obra fica sem identidade
     * agora que ela é DERIVADA. Uma lore sem identidade seria um ponto cego permanente da
     * guarda, exatamente como as 57 obras eram antes deste passo.
     */
    @Test
    @DisplayName("cobertura: toda obra registrada tem identidade e se reconhece pelo próprio id e nome")
    void todaObraTemIdentidadeESeReconhece() {
        List<String> semIdentidade = new ArrayList<>();
        List<String> naoSeReconhece = new ArrayList<>();

        for (ProvedorContexto provedor : provedores) {
            IdentidadeObra identidade = IdentidadeObra.de(provedor);
            if (identidade.nomesCanonicos().isEmpty()) {
                semIdentidade.add(provedor.getId());
                continue;
            }
            if (!identidade.reconhece(provedor.getId()) || !identidade.reconhece(provedor.getNomeExibicao())) {
                naoSeReconhece.add(provedor.getId());
            }
        }

        assertTrue(semIdentidade.isEmpty(),
            () -> "Obras sem identidade canônica (ponto cego permanente da guarda): " + semIdentidade);
        assertTrue(naoSeReconhece.isEmpty(),
            () -> "Obras que não se reconhecem pelo próprio id/nome de exibição — a derivação não está "
                + "cobrindo o catálogo: " + naoSeReconhece);
        assertEquals(provedores.size(), gerenciador.identidades().size(),
            "o gerenciador deve derivar UMA identidade por obra registrada");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: par mínimo do INCIDENTE, com os dois nomes de pasta reais. É o
     * cenário exato que gravou 15 caches errados: pasta de Gundam 0083, combo em Guilty Crown.
     */
    @Test
    @DisplayName("par mínimo do incidente: pasta de 0083 nunca é Guilty Crown, e vice-versa")
    void parMinimoDoIncidente() {
        String pasta0083 = "[Joseki] Mobile Suit Gundam 0083 Stardust Memory COMPLETE (1991)(BD AV1 1080p Opus)[Sub Eng]";
        String pastaGuilty = "[Anime Time] Guilty Crown + OVA [BD][Dual Audio][1080p][HEVC 10bit x265][Opus][Eng Sub]";

        assertEquals(Set.of("gundam_0083"), gerenciador.idsQueReconhecem(pasta0083));
        assertFalse(gerenciador.idsQueReconhecem(pasta0083).contains("guilty_crown"));
        assertEquals(Set.of("guilty_crown"), gerenciador.idsQueReconhecem(pastaGuilty));
        assertFalse(gerenciador.idsQueReconhecem(pastaGuilty).contains("gundam_0083"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: pares mínimos de NÚMERO, ANO e SIGLA contra pastas reais. Existem
     * para proibir, de forma executável, qualquer "limpeza de ruído de release" que apague
     * dígitos: no catálogo desta árvore o dígito É o nome da obra.
     */
    @Test
    @DisplayName("pares mínimos número/ano/sigla: 0083×0080, 86×(1986), Z Gundam×Gundam ZZ, e Gundam 00 fora do catálogo")
    void paresMinimosDeNumeroAnoESigla() {
        String pasta0080 = "[Joseki] Mobile Suit Gundam 0080 War in the Pocket COMPLETE (1989)(BD AV1 1080p Opus)[Sub Eng]";
        String pasta0083 = "[Joseki] Mobile Suit Gundam 0083 Stardust Memory COMPLETE (1991)(BD AV1 1080p Opus)[Sub Eng]";
        String pastaZZ = "[Joseki] Mobile Suit Gundam ZZ COMPLETE (1986)(BD AV1 1080p Opus)[Eng Sub][v2]";
        String pastaZeta = "[Joseki] Mobile Suit Z Gundam COMPLETE (1985)(BD AV1 1080p Opus)[Sub Eng][v2]";

        // Número: 0080 e 0083 só diferem no último dígito e são obras distintas.
        assertEquals(Set.of("gundam_0080"), gerenciador.idsQueReconhecem(pasta0080));
        assertEquals(Set.of("gundam_0083"), gerenciador.idsQueReconhecem(pasta0083));

        // Ano: o (1986) da pasta de ZZ NÃO pode ser lido como a obra "86".
        assertEquals(Set.of("gundam_zz"), gerenciador.idsQueReconhecem(pastaZZ));
        assertFalse(gerenciador.idsQueReconhecem(pastaZZ).contains("eight_six"),
            "1986 é uma palavra inteira: o ano jamais identifica a obra 86");
        assertEquals(Set.of("eight_six"), gerenciador.idsQueReconhecem("86 Part 1"));

        // Sigla: Z Gundam e Gundam ZZ são obras diferentes; a ordem das palavras separa as duas.
        assertEquals(Set.of("gundam_zeta"), gerenciador.idsQueReconhecem(pastaZeta));
        assertFalse(gerenciador.idsQueReconhecem(pastaZeta).contains("gundam_zz"));
        assertFalse(gerenciador.idsQueReconhecem(pastaZZ).contains("gundam_zeta"));

        // Obra que este catálogo NÃO tem: Gundam 00 não pode cair em 0080/0083 por parecença.
        assertEquals(Set.of(), gerenciador.idsQueReconhecem("Mobile Suit Gundam 00 Season 2"),
            "Gundam 00 é outra obra e não está no catálogo: o desfecho correto é NÃO reconhecer");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fase de migração — pasta que nenhuma obra alcança fica
     * INDETERMINADA e a tradução SEGUE com aviso. Falhar fechado aqui pararia toda obra cujo
     * nome de pasta o catálogo ainda não cobre.
     */
    @Test
    @DisplayName("lacunas de cobertura não bloqueiam: pasta irreconhecível resolve para vazio")
    void lacunasDeCoberturaNaoBloqueiam() {
        assertEquals(Set.of(), gerenciador.idsQueReconhecem("[Lulu] Break Blade Movies [BD 1080p Hi10 FLAC][Dual-Audio]"),
            "os seis filmes de Break Blade dividem uma pasta só; nenhuma regra determinística escolhe um");
        assertEquals(Set.of(), gerenciador.idsQueReconhecem("filme1"));
        assertEquals(Set.of(), gerenciador.idsQueReconhecem("Pasta Que Nao Existe No Catalogo"));
        assertEquals(Set.of(), gerenciador.idsQueReconhecem(""));
    }
}
