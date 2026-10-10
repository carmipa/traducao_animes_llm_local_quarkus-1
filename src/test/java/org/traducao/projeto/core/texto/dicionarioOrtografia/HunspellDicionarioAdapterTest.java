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

    /**
     * PROPÓSITO DE NEGÓCIO: o binário existe mas o DICIONÁRIO não — a máquina sem o {@code de_DE},
     * por exemplo. O hunspell sai com 1 e escreve só "Can't open affix or dictionary files"; até
     * 09/10/2026 o adaptador se declarava disponível e não acusava nada, então o idioma inteiro
     * passava por conhecido. O controle positivo ({@code pt_BR} instalado acusa a palavra inventada)
     * fica no mesmo método: sem ele, "indisponível" aqui poderia ser só hunspell ausente.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: hunspell ausente PULA — NÃO VERIFICADO.
     */
    @Test
    @DisplayName("dicionario ausente com o binario presente: INDISPONIVEL, nunca 'tudo conhecido'")
    void dicionarioAusenteNaoAprovaNada() {
        var controle = new HunspellDicionarioAdapter("hunspell", "pt_BR");
        Set<String> doControle = controle.desconhecidas(List.of("xyzabcdef"));
        Assumptions.assumeTrue(controle.disponivel() && doControle.contains("xyzabcdef"),
            "hunspell/pt_BR ausente — NÃO VERIFICADO");

        var semDicionario = new HunspellDicionarioAdapter("hunspell", "xx_INEXISTENTE");
        Set<String> r = semDicionario.desconhecidas(List.of("xyzabcdef", "casa"));
        assertFalse(semDicionario.disponivel(),
            "dicionario que nao carregou se declarou disponivel — todo o idioma passaria por conhecido");
        assertTrue(r.isEmpty(), "sem dicionario nao se acusa nada (e nada se aprova: disponivel=false)");
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
            "organizacao", "observacao", "inutil", "xyzabcdef", "Resonância",
            "organização", "observação", "inútil", "vamos", "estamos", "criança"));

        Assumptions.assumeTrue(adapter.disponivel(),
            "hunspell/pt_BR ausente — NÃO VERIFICADO. Instale: choco install hunspell.portable");

        // CONTROLE POSITIVO: as formas sem acento não existem em português.
        assertTrue(r.contains("organizacao"), "forma sem acento tem de ser acusada");
        assertTrue(r.contains("inutil"), "forma sem acento tem de ser acusada");
        assertTrue(r.contains("xyzabcdef"), "palavra inventada tem de ser acusada");
        // O MESMO sinal de 'criança' (letra fora do ASCII), do lado errado: se o dialogo com o
        // processo estiver na codificacao errada, a palavra volta partida e passa por conhecida.
        assertTrue(r.contains("Resonância"),
            "grafia errada com acento passou: o dialogo com o pt_BR esta na codificacao errada");
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

    /**
     * PROPÓSITO DE NEGÓCIO: a codificação vem da linha {@code SET} do {@code .aff}, e o padrão do
     * hunspell quando ela falta é ISO8859-1 — é isso que faz o adaptador conversar com o
     * {@code de_DE} na língua dele.
     */
    @Test
    @DisplayName("a codificacao do dicionario vem da linha SET do .aff; sem SET, ISO8859-1")
    void codificacaoVemDoAff(@org.junit.jupiter.api.io.TempDir Path pasta) throws Exception {
        Path latino = Files.writeString(pasta.resolve("xx_LA.aff"), "# cabecalho\nSET ISO8859-1\nTRY esian\n",
            StandardCharsets.ISO_8859_1);
        Path utf = Files.writeString(pasta.resolve("xx_UT.aff"), "SET UTF-8\n", StandardCharsets.UTF_8);
        Path semSet = Files.writeString(pasta.resolve("xx_SS.aff"), "TRY abc\n", StandardCharsets.UTF_8);
        Path comBom = Files.writeString(pasta.resolve("xx_BO.aff"), "﻿SET UTF-8\nFLAG long\n", StandardCharsets.UTF_8);

        assertEquals("ISO8859-1", HunspellDicionarioAdapter.codificacaoDoAff(latino));
        assertEquals("UTF-8", HunspellDicionarioAdapter.codificacaoDoAff(utf));
        assertEquals("UTF-8", HunspellDicionarioAdapter.codificacaoDoAff(comBom),
            "o pt_BR.aff real abre com BOM colado no SET: sem descarta-lo o portugues vira Latin-1 "
                + "e 'fatidico' perde a sugestao 'fatídico'");
        assertEquals("ISO8859-1", HunspellDicionarioAdapter.codificacaoDoAff(semSet),
            "sem SET o hunspell usa ISO8859-1; supor UTF-8 aqui repetiria o defeito do de_DE");
        assertEquals(StandardCharsets.ISO_8859_1, HunspellDicionarioAdapter.charsetJava("ISO8859-1"));
        assertEquals(StandardCharsets.UTF_8, HunspellDicionarioAdapter.charsetJava("UTF-8"));
        assertEquals(java.nio.charset.Charset.forName("windows-1251"),
            HunspellDicionarioAdapter.charsetJava("microsoft-cp1251"));
        assertEquals(null, HunspellDicionarioAdapter.charsetJava("CODIFICACAO-INVENTADA"),
            "nome desconhecido nao vira codificacao inventada: quem chama cai na suposicao declarada");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o {@code .aff} achado é o da PRIMEIRA pasta da ordem de busca — a
     * mesma ordem do hunspell —, para a codificação lida ser a do arquivo que ele carregaria.
     */
    @Test
    @DisplayName("o .aff localizado e o da primeira pasta da ordem de busca")
    void localizaNaOrdemDeBusca(@org.junit.jupiter.api.io.TempDir Path raiz) throws Exception {
        Path primeira = Files.createDirectories(raiz.resolve("a"));
        Path segunda = Files.createDirectories(raiz.resolve("b"));
        Files.writeString(segunda.resolve("zz_ZZ.aff"), "SET UTF-8\n");
        assertEquals(segunda.resolve("zz_ZZ.aff").toAbsolutePath(),
            HunspellDicionarioAdapter.localizarAff("zz_ZZ", List.of(primeira, segunda)).orElseThrow());
        Files.writeString(primeira.resolve("zz_ZZ.aff"), "SET ISO8859-1\n");
        assertEquals(primeira.resolve("zz_ZZ.aff").toAbsolutePath(),
            HunspellDicionarioAdapter.localizarAff("zz_ZZ", List.of(primeira, segunda)).orElseThrow());
        assertTrue(HunspellDicionarioAdapter.localizarAff("nn_NN", List.of(primeira, segunda)).isEmpty());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a fronteira do defeito de 09/10/2026, no hunspell real. O sinal que
     * enganava era LETRA FORA DO ASCII: com o diálogo em UTF-8, o {@code de_DE} (ISO8859-1)
     * partia a palavra e o adaptador a dava por conhecida. Os dois lados carregam o mesmo sinal —
     * a invenção do modelo ({@code Açãoaria}) e o erro de grafia ({@code Resonância}) têm de sair
     * desconhecidos, e o alemão de verdade com {@code ß} e trema ({@code Straße},
     * {@code Walküre}) tem de sair conhecido.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: hunspell ou {@code de_DE} ausentes PULAM por
     * {@link Assumptions} — NÃO VERIFICADO.
     */
    @Test
    @DisplayName("de_DE em ISO8859-1: invencao com acento e desconhecida, alemao com trema e conhecido")
    void dicionarioLatinoNaoAprovaPalavraPartida() {
        var adapter = new HunspellDicionarioAdapter("hunspell", "de_DE");
        Set<String> r = adapter.desconhecidas(List.of("Açãoaria", "Resonância", "opçāo", "Straße", "Walküre", "Haus"));
        Assumptions.assumeTrue(adapter.disponivel(), "hunspell/de_DE ausente — NÃO VERIFICADO");
        Assumptions.assumeTrue(!r.contains("Haus"), "de_DE nao reconhece nem 'Haus' — dicionario errado, NÃO VERIFICADO");

        assertTrue(r.contains("Açãoaria"),
            "a invencao do modelo na letra do Break Blade passou como alema conhecida");
        assertTrue(r.contains("Resonância"), "grafia errada de ressonancia passou como alema conhecida");
        assertTrue(r.contains("opçāo"),
            "o macron nao existe em ISO8859-1: a palavra nao pode estar no dicionario");
        assertFalse(r.contains("Straße"), "alarme falso: alemao legitimo com ß acusado");
        assertFalse(r.contains("Walküre"), "alarme falso: alemao legitimo com trema acusado");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a REDE para quando o {@code .aff} não é achado (outra máquina, outra
     * pasta) e a codificação suposta (UTF-8) está errada. O hunspell carrega o {@code de_DE} pelo
     * nome e conversa em ISO8859-1; a resposta volta com byte ilegível. Aí toda palavra com acento
     * do lote fica desconhecida — o lado conservador: o alemão legítimo perde o rótulo, mas a
     * invenção do modelo não ganha rótulo de "preservar".
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: hunspell ou {@code de_DE} ausentes PULAM — NÃO VERIFICADO.
     */
    @Test
    @DisplayName("sem o .aff, resposta ilegivel deixa a palavra com acento desconhecida, nunca conhecida")
    void semAffRespostaIlegivelNaoAprova(@org.junit.jupiter.api.io.TempDir Path vazia) {
        var adapter = new HunspellDicionarioAdapter("hunspell", "de_DE", List.of(vazia));
        Set<String> r = adapter.desconhecidas(List.of("Açãoaria", "Straße", "Haus"));
        Assumptions.assumeTrue(adapter.disponivel(), "hunspell/de_DE ausente — NÃO VERIFICADO");
        Assumptions.assumeTrue(!r.contains("Haus"), "de_DE nao reconhece 'Haus' — NÃO VERIFICADO");

        assertTrue(r.contains("Açãoaria"),
            "codificacao suposta errada e a palavra partida passou por CONHECIDA — a rede nao pegou");
        assertTrue(r.contains("Straße"),
            "lado conservador: sem poder ler a resposta, nem o alemao com ß sai como conhecido");
    }
}



