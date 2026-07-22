package org.traducao.projeto.qualidadeTraducao.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.domain.LoreAtivaPort;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: impede que nomes próprios legítimos sejam enviados ao
 * revisor apenas porque são idênticos no inglês e no PT-BR.
 * <p>INVARIANTES DO DOMÍNIO: hesitação e pontuação não descaracterizam nomes;
 * palavras conversacionais inglesas continuam pendentes.
 * <p>COMPORTAMENTO EM CASO DE FALHA: falso nome ou falso inglês reprova o teste.
 */
class DetectorTraducaoIdenticaServiceTest {

    /**
     * PROPÓSITO DE NEGÓCIO: fake de {@link LoreAtivaPort} fiel ao estado que o detector
     * enxergava antes da E8c.1 com {@code new GerenciadorContexto(List.of())} — nenhum
     * provedor de contexto, logo nenhum termo protegido e a lore neutra do prompt padrão.
     * <p>INVARIANTES DO DOMÍNIO: reproduz exatamente os retornos daquele cenário para que
     * a inversão de dependência não altere o comportamento observado nestes casos.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; retornos fixos e determinísticos.
     */
    private static final class LoreAtivaVazia implements LoreAtivaPort {
        @Override
        public Set<String> termosProtegidosAtivos() {
            return Set.of();
        }

        @Override
        public String obterLoreAtiva() {
            return "Voce e um tradutor especialista. Traduza fielmente.";
        }
    }

    private final DetectorTraducaoIdenticaService detector =
        new DetectorTraducaoIdenticaService(new LoreAtivaVazia());

    /**
     * PROPÓSITO DE NEGÓCIO: cobre os nomes que o Nemo recebeu indevidamente na execução real.
     * <p>INVARIANTES DO DOMÍNIO: uma a quatro palavras capitalizadas são preservadas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer nome não reconhecido reprova o teste.
     */
    @Test
    void preservaNomesSimplesGaguejadosESequenciais() {
        assertTrue(detector.deveManterIdentico("Maria?"));
        assertTrue(detector.deveManterIdentico("E-Eledore..."));
        assertTrue(detector.deveManterIdentico("Rob... Sally... Mike..."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém saudações inglesas na fila de tradução.
     * <p>INVARIANTES DO DOMÍNIO: capitalização isolada não protege vocabulário comum.
     * <p>COMPORTAMENTO EM CASO DE FALHA: classificação como nome reprova o teste.
     */
    @Test
    void naoConfundePalavraInglesaComNome() {
        assertFalse(detector.deveManterIdentico("Hello!"));
    }

    /** Lore com a terminologia UC de Zeon, como as obras da Guerra de Um Ano declaram. */
    private static final class LoreZeon implements LoreAtivaPort {
        @Override
        public Set<String> termosProtegidosAtivos() {
            return Set.of("Sieg Zeon", "Zeon", "Principality of Zeon", "Earth Federation",
                "Minovsky", "Gundam");
        }

        @Override
        public String obterLoreAtiva() {
            return "Universal Century; Principality of Zeon vs Earth Federation.";
        }
    }

    private final DetectorTraducaoIdenticaService detectorZeon =
        new DetectorTraducaoIdenticaService(new LoreZeon());

    /**
     * PROPÓSITO DE NEGÓCIO: o brado do Principado de Zeon é canônico em toda a linha do
     * tempo UC e deve permanecer no original — traduzi-lo para "Salve Zeon!" perde o
     * paralelo histórico deliberado da obra. Caso REAL: na corrida de 2026-07-22 a fala
     * "Sieg Zeon! Sieg Zeon! Sieg Zeon! Sieg Zeon!" virou pendência por "modelo devolveu o
     * texto original", porque o reconhecimento exigia o texto inteiro igual ao termo e não
     * enxergava a repetição.
     * <p>INVARIANTES DO DOMÍNIO: repetição do termo canônico continua canônica.
     * <p>COMPORTAMENTO EM CASO DE FALHA: se voltar a reprovar, o brado vira pendência a
     * cada execução de qualquer obra UC.
     */
    @Test
    void bradoCanonicoRepetidoPermaneceNoOriginal() {
        assertTrue(detectorZeon.deveManterIdentico("Sieg Zeon! Sieg Zeon! Sieg Zeon! Sieg Zeon!"));
        assertTrue(detectorZeon.deveManterIdentico("{\\i1}Sieg Zeon! Sieg Zeon!{\\i0}"));
        assertTrue(detectorZeon.deveManterIdentico("Sieg Zeon!"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que a regra nova NÃO virou porta dos fundos — basta
     * sobrar uma palavra traduzível para a fala continuar exigindo tradução.
     * <p>INVARIANTES DO DOMÍNIO: só texto INTEIRAMENTE composto de termos declarados passa.
     * <p>COMPORTAMENTO EM CASO DE FALHA: aceitar frase inglesa como tradução.
     */
    @Test
    void fraseComPalavraTraduzivelContinuaPendente() {
        assertFalse(detectorZeon.deveManterIdentico("Zeon attacks the colony"),
            "sobrou \"attacks the colony\": a fala não foi traduzida");
        assertFalse(detectorZeon.deveManterIdentico("The Gundam is here"),
            "sobrou \"is here\": a fala não foi traduzida");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fecha o vazamento MEDIDO de inglês publicado como se fosse tradução.
     * Em 2026-07-22 varri as 55.322 entradas distintas dos 216 caches versionados: 1.569 falas
     * saíram idênticas ao original e 128 dessas ocorrências eram inglês corrente, entregue no
     * {@code _PT-BR.ass} final e gravado no cache como tradução VÁLIDA — congelando em inglês,
     * porque {@code isCacheReaproveitavel} consulta esta mesma régua na execução seguinte.
     *
     * <p>Todas passavam pela última linha de {@code deveManterPalavraUnicaIdentica}: "3+ letras
     * e inicial maiúscula ⇒ deve ser nome próprio". Nenhuma lore, por melhor que fosse curada,
     * fecharia isso — a regra nem consultava a lore antes de aceitar.
     *
     * <p>INVARIANTES DO DOMÍNIO: evidência positiva de inglês traduzível RECUSA a identidade.
     * <p>COMPORTAMENTO EM CASO DE FALHA: se voltar a aceitar, o inglês volta à legenda entregue.
     */
    @Test
    void inglesCorrenteMedidoNosCachesDeixaDeSerAceitoComoTraducao() {
        assertFalse(detectorZeon.deveManterIdentico("Next Episode"), "63 ocorrências medidas");
        assertFalse(detectorZeon.deveManterIdentico("NEXT EPISODE"), "caixa alta não é salvo-conduto");
        assertFalse(detectorZeon.deveManterIdentico("Roger!"), "29 ocorrências medidas");
        assertFalse(detectorZeon.deveManterIdentico("Gotcha!"), "12 ocorrências medidas");
        assertFalse(detectorZeon.deveManterIdentico("Heavy!"), "caso real do episódio 2");
        assertFalse(detectorZeon.deveManterIdentico("Enter."), "caso real do episódio 7");
        assertFalse(detectorZeon.deveManterIdentico("\"Doctor Flanagan\"...?"), "caso real do episódio 12");
        assertFalse(detectorZeon.deveManterIdentico("Big Brother! Big Brother!"));
        assertFalse(detectorZeon.deveManterIdentico("Little Sister!"));
        assertFalse(detectorZeon.deveManterIdentico("Ready, Lieutenant!"));
        assertFalse(detectorZeon.deveManterIdentico("Move!"));
        assertFalse(detectorZeon.deveManterIdentico("Fire!"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a régua por evidência não pode cobrar o preço dos nomes próprios.
     * As mesmas 1.569 falas idênticas incluem 995 de uma palavra só que são personagens reais
     * — e as legendas os chamam pelo PRIMEIRO nome, enquanto a lore declara o nome completo.
     * Uma régua que exigisse cadastro exaustivo reprovaria todos eles.
     *
     * <p>INVARIANTES DO DOMÍNIO: sem evidência de inglês traduzível, a identidade é preservada,
     * esteja o nome declarado na lore ou não.
     * <p>COMPORTAMENTO EM CASO DE FALHA: personagem virando pendência a cada execução, gravado
     * como vazio no cache e retentado para sempre.
     */
    @Test
    void nomesProprioMedidosNosCachesContinuamPreservados() {
        // Nenhum destes está declarado na LoreZeon deste teste: são aceitos por AUSÊNCIA de
        // evidência, que é justamente a propriedade que dispensa curadoria exaustiva.
        assertTrue(detectorZeon.deveManterIdentico("Kamille!"), "43 ocorrências medidas");
        assertTrue(detectorZeon.deveManterIdentico("Judau!"), "39 ocorrências medidas");
        assertTrue(detectorZeon.deveManterIdentico("Michel!"));
        assertTrue(detectorZeon.deveManterIdentico("Karen!"));
        assertTrue(detectorZeon.deveManterIdentico("Dell!"), "não está em lore nenhuma e mesmo assim é nome");
        assertTrue(detectorZeon.deveManterIdentico("Leina!"));
        assertTrue(detectorZeon.deveManterIdentico("E-Eledore..."), "hesitação não descaracteriza o nome");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: quando a própria obra DECLARA uma expressão que contém palavra do
     * léxico, a terminologia canônica vence — senão declarar o termo na lore não teria efeito.
     *
     * <p>INVARIANTES DO DOMÍNIO: o veto age tanto sobre a fala inteira quanto por token.
     * <p>COMPORTAMENTO EM CASO DE FALHA: terminologia oficial da obra sendo traduzida.
     */
    @Test
    void terminologiaDeclaradaVenceOLexicoDeEvidencia() {
        DetectorTraducaoIdenticaService comTitulo = new DetectorTraducaoIdenticaService(new LoreAtivaPort() {
            @Override public Set<String> termosProtegidosAtivos() {
                return Set.of("War in the Pocket", "Doctor Flanagan", "Zeon");
            }
            @Override public String obterLoreAtiva() {
                return "Universal Century.";
            }
        });
        assertTrue(comTitulo.deveManterIdentico("War in the Pocket"),
            "título declarado da obra permanece, mesmo contendo palavra do léxico");
        assertTrue(comTitulo.deveManterIdentico("\"Doctor Flanagan\"...?"),
            "declarado na lore, 'doctor' deixa de ser evidência");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a mudança precisa ser MONÓTONA — a régua por evidência só pode
     * recusar, nunca aceitar algo que as heurísticas históricas já recusavam. Sem isso, um
     * conserto de vazamento poderia abrir outro.
     *
     * <p>INVARIANTES DO DOMÍNIO: o portão é uma conjunção, não uma disjunção.
     * <p>COMPORTAMENTO EM CASO DE FALHA: inglês novo passando a ser aceito.
     */
    @Test
    void reguaPorEvidenciaSoRecusaNuncaPassaAAceitar() {
        assertFalse(detector.deveManterIdentico("Hello!"), "continua recusado como sempre foi");
        assertFalse(detectorZeon.deveManterIdentico("Zeon attacks the colony"),
            "sobra texto traduzível: segue recusado pela régua histórica");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: sem obra selecionada, a regra nova não pode inventar proteção.
     * <p>INVARIANTES DO DOMÍNIO: lore vazia degrada para as heurísticas globais.
     * <p>COMPORTAMENTO EM CASO DE FALHA: aceitação cega sem terminologia declarada.
     */
    @Test
    void semLoreAtivaOBradoNaoEhProtegidoPelaRegraNova() {
        assertFalse(detector.deveManterIdentico("Sieg Zeon! Sieg Zeon! Sieg Zeon! Sieg Zeon!"),
            "sem termos declarados, nada autoriza manter a fala em inglês");
    }
}
