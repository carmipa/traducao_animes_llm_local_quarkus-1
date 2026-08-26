package org.traducao.projeto.core.texto.dicionarioOrtografia;

import org.junit.jupiter.api.Assumptions;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    /**
     * PROPÓSITO DE NEGÓCIO: acha o {@code pt_BR.dic} sem cravar caminho de máquina nenhuma.
     *
     * <p>INVARIANTES DO DOMÍNIO: nenhum literal de drive. A primeira versão cravava a pasta do
     * Windows e a catraca {@code CatracaSuiteSemDriveWindowsTest} reprovou — com razão: no
     * contêiner Linux o dicionário mora sob {@code usr/share/hunspell}. (E reprovou duas vezes: a
     * segunda porque o literal tinha ficado neste próprio comentário — a catraca lê o texto do
     * arquivo, não só o código, e está certa: exemplo copiado vira código.)
     * As raízes vêm do próprio sistema de arquivos, e {@code DICPATH} tem precedência porque é a
     * variável que o hunspell respeita.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@code null}, e quem chama declara NÃO VERIFICADO
     * em vez de inventar um resultado.
     */
    private static Path localizarDicionario() {
        List<Path> candidatas = new java.util.ArrayList<>();
        String dicpath = System.getenv("DICPATH");
        if (dicpath != null && !dicpath.isBlank()) {
            for (String parte : dicpath.split(java.io.File.pathSeparator)) {
                if (!parte.isBlank()) {
                    candidatas.add(Path.of(parte.trim()));
                }
            }
        }
        for (Path raiz : java.nio.file.FileSystems.getDefault().getRootDirectories()) {
            candidatas.add(raiz.resolve("Hunspell"));
            candidatas.add(raiz.resolve(Path.of("usr", "share", "hunspell")));
            candidatas.add(raiz.resolve(Path.of("usr", "share", "myspell")));
        }
        for (Path pasta : candidatas) {
            Path arquivo = pasta.resolve("pt_BR.dic");
            if (Files.isRegularFile(arquivo)) {
                return arquivo;
            }
        }
        return null;
    }

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

    /**
     * PROPÓSITO DE NEGÓCIO: um arquivo grande NÃO pode fazer o dicionário devolver nada para o
     * arquivo inteiro.
     *
     * <h2>O prejuízo, medido em 26/08/2026</h2>
     * A passada da tela 3.3 sobre {@code ANIMES-TESTES} ficou <b>15 minutos parada</b> no
     * {@code Mobile.Suit.Gundam.CCA_Track2_PT-BR.ass}, com um único aviso:
     * {@code "hunspell não respondeu em 20s; nada foi verificado"}. O comentário do timeout supunha
     * "centenas de formas distintas"; o CCA tem <b>2.743</b> e o F91 <b>2.719</b> — a mediana do
     * acervo é 940. Numa chamada só, os dois filmes estouram o timeout, e o estouro não custava
     * aquelas palavras: custava TODAS as do arquivo, porque o timeout devolve mapa vazio.
     *
     * <h2>Duas versões deste teste foram descartadas, e o motivo importa</h2>
     * A primeira mandou 2.400 formas INVENTADAS e exigiu 2.400 respostas. Ficou <b>verde nos dois
     * mundos</b> — com lote de 800 e com lote de um milhão. A segunda, com o {@code assumeTrue}
     * depois da consulta grande, transformou a falha em <b>PULO</b>: o placar dizia "abortado" e o
     * build passava.
     *
     * <p>A medição desfez a suposição que eu tinha escrito aqui. Palavra inventada não é barata: é
     * o caso CARO, porque o hunspell gasta tempo gerando sugestão para ela.
     *
     * <pre>
     *    800 inventadas ... 27,6 s        800 reais ... 1,0 s
     *   2400 inventadas ... 80,8 s       2400 reais ... 1,4 s
     * </pre>
     *
     * <p>34 ms contra 0,6 ms — 50×. Um arquivo de legenda é quase todo palavra conhecida, então
     * 2.400 inventadas não medem arquivo nenhum: medem um cenário que não existe, e estouram o
     * timeout nos dois mundos.
     *
     * <p>O que se sela aqui é a ESTRUTURA, via {@code consultasAoProcesso()}: acima do tamanho do
     * lote, a consulta vira mais de uma chamada. Determinístico, e a mutação derruba na hora.
     *
     * <h2>Comportamento em caso de falha</h2>
     * hunspell ausente PULA por {@link Assumptions}: NÃO VERIFICADO, e pular não é aprovar.
     */
    @Test
    @DisplayName("volume grande vai ao processo em LOTES — uma chamada so perde o arquivo inteiro")
    void volumeGrandeNaoDerrubaOarquivoInteiro() throws Exception {
        // AS PALAVRAS SAO REAIS, e isto foi medido, nao suposto. As duas versoes anteriores deste
        // caso mandaram 2.400 formas INVENTADAS, e as duas falharam de jeitos diferentes:
        //
        //   800 formas inventadas .... 27,6 s   (o hunspell gera sugestao para cada uma)
        //  2400 formas inventadas .... 80,8 s
        //   800 formas reais .........  1,0 s   (conhecida sai sem trabalho nenhum)
        //  2400 formas reais .........  1,4 s
        //
        // Palavra inventada e o caso CARO, 34 ms contra 0,6 ms — 50x. Um arquivo de legenda e
        // quase todo palavra conhecida, entao 2.400 inventadas nao mede arquivo nenhum: mede um
        // cenario que nao existe, e estoura o timeout nos DOIS mundos.
        Path dicionario = localizarDicionario();
        Assumptions.assumeTrue(dicionario != null,
            "pt_BR.dic nao encontrado (DICPATH nem as pastas padrao) — NÃO VERIFICADO");
        List<String> reais = Files.readAllLines(dicionario, StandardCharsets.UTF_8).stream()
            .skip(1)
            .map(l -> l.split("/")[0].trim())
            .filter(w -> w.length() > 3 && w.chars().allMatch(Character::isLetter))
            .distinct()
            .limit(2400)
            .toList();
        Assumptions.assumeTrue(reais.size() == 2400,
            "o dicionario tem " + reais.size() + " formas usaveis — NÃO VERIFICADO");

        var adapter = new HunspellDicionarioAdapter("hunspell", "pt_BR");

        // A SONDA VEM PRIMEIRO, e a ORDEM e a propria guarda. Na versao anterior o assumeTrue
        // vinha DEPOIS da consulta grande: quando o processo estourava o timeout,
        // `disponivel()` virava false, e o meu proprio Assumptions transformava a FALHA em PULO.
        // O placar dizia "abortado" e o build passava — pular nao e aprovar, e o teste caiu
        // exatamente na armadilha que ele existe para vigiar.
        adapter.desconhecidas(List.of("casa"));
        Assumptions.assumeTrue(adapter.disponivel(), "hunspell ausente — NÃO VERIFICADO");
        int antesDaCargaGrande = adapter.consultasAoProcesso();

        adapter.desconhecidas(reais);
        int chamadas = adapter.consultasAoProcesso() - antesDaCargaGrande;

        assertTrue(adapter.disponivel(),
            "o dicionario ficou indisponivel com 2.400 formas REAIS — isso e menos do que um "
                + "episodio grande tem, e significa que o loteamento nao esta funcionando");
        assertTrue(chamadas > 1,
            "2.400 formas foram ao hunspell em " + chamadas + " chamada(s). Sem lote, um arquivo "
                + "grande leva junto TODAS as palavras dele: foi o que travou a passada da 3.3 "
                + "por 15 minutos no CCA (2.743 formas), com um unico aviso no log.");
    }
}



