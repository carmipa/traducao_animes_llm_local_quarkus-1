package org.traducao.projeto.revisaoConcordancia.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: provar que os padrões curados acertam a fala do acervo e calam onde a
 * palavra sem acento está certa.
 *
 * <h2>Cada caso NEGATIVO aqui é uma fala real que a leitura salvou</h2>
 * A medição de 24/08/2026 rodou sobre 86.147 falas e a amostra foi lida uma a uma. Duas regras
 * foram REFEITAS por causa do que apareceu, e é isso que os testes negativos congelam — sem eles,
 * a próxima "simplificação" reabre o buraco:
 *
 * <pre>
 *   "Entrei em contato com você esta noite"  -> `esta noite` e DEMONSTRATIVO
 *   "Ela desceu para nos salvar"             -> `nos salvar` e pronome obliquo
 *   "Judau, isso e aquilo."                  -> coordenacao, o `e` esta certo
 * </pre>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprova nomeando a fala inteira, para quem ler saber o que mudou.
 */
class CorretorAcentoPorPadraoServiceTest {

    private final CorretorAcentoPorPadraoService corretor = new CorretorAcentoPorPadraoService();

    @Test
    @DisplayName("POSITIVO: o `e` depois de demonstrativo vira verbo")
    void demonstrativoMaisE() {
        assertEquals(Optional.of("Essa é uma ótima notícia."),
            corretor.corrigir("Essa e uma ótima notícia."));
        assertEquals(Optional.of("Isso é rápido."), corretor.corrigir("Isso e rápido."));
        assertEquals(Optional.of("Qual é o seu nome?"), corretor.corrigir("Qual e o seu nome?"));
    }

    @Test
    @DisplayName("NEGATIVO: `isso e aquilo` e coordenacao — fica como esta")
    void coordenacaoNaoEVerbo() {
        assertEquals(Optional.empty(), corretor.corrigir("Judau, isso e aquilo."),
            "trocou por verbo uma coordenacao legitima — sao 3 falas assim no acervo");
        assertEquals(Optional.empty(), corretor.corrigir("Isso e aquilo..."));
    }

    /**
     * O DANO da auditoria da 3.3 (17/09/2026): DEMONSTRATIVO_E assumia que apos isso/isto/aquilo o
     * {@code e} so pode ser o verbo {@code é}. Falso quando o demonstrativo e OBJETO de um verbo
     * (imperativo): {@code "Faca isso e pronto"} (faca isso E pronto) virava {@code "Faca isso é
     * pronto"}. Comum em dialogo.
     */
    @Test
    @DisplayName("NEGATIVO: `isso e` apos verbo (objeto) e CONJUNCAO, nao o verbo ser")
    void demonstrativoObjetoNaoViraVerbo() {
        assertEquals(Optional.empty(), corretor.corrigir("Faça isso e pronto."),
            "'isso' e OBJETO do imperativo 'Faca'; o 'e' e conjuncao, nao o verbo ser");
        assertEquals(Optional.empty(), corretor.corrigir("Pegue isso e vá."));
        assertEquals(Optional.empty(), corretor.corrigir("Deixa isso e vem."));
    }

    /**
     * O CONTRA-TESTE do anterior: o demonstrativo SUJEITO (no inicio da clausula — comeco da fala,
     * apos pontuacao, ou apos subordinador) continua virando o verbo. Sem ele, exigir clausula
     * fecharia a regra inteira.
     */
    @Test
    @DisplayName("CONTROLE: demonstrativo SUJEITO (inicio de clausula) continua virando verbo")
    void demonstrativoSujeitoContinuaVirandoVerbo() {
        assertEquals(Optional.of("Isso é rápido."), corretor.corrigir("Isso e rápido."));
        assertEquals(Optional.of("Acho que isso é verdade."),
            corretor.corrigir("Acho que isso e verdade."));
        assertEquals(Optional.of("Bom dia. Isso é urgente."),
            corretor.corrigir("Bom dia. Isso e urgente."));
    }

    @Test
    @DisplayName("POSITIVO: `nao e` e sempre `nao é`")
    void naoMaisE() {
        assertEquals(Optional.of("Não é uma ideia ruim."),
            corretor.corrigir("Não e uma ideia ruim."));
        assertEquals(Optional.of("Isso não é saudável."),
            corretor.corrigir("Isso não e saudável."));
    }

    /**
     * O DANO da auditoria da 3.3 (17/09/2026): {@code "não e"} vira {@code "não é"} tambem quando
     * {@code não} e SUBSTANTIVO (uma recusa) e o {@code e} coordena: {@code "Levou um não e
     * desistiu"} -> {@code "Levou um não é desistiu"}. Guarda pelo artigo antes ({@code um/o não}).
     * Residual declarado: {@code "sim ou não e acabou"} (não em lista) segue como esta — raro e
     * ambiguo com {@code "ou não é"}.
     */
    @Test
    @DisplayName("NEGATIVO: 'um não'/'o não' (substantivo) + conjuncao 'e' nao vira verbo")
    void naoSubstantivoNaoViraVerbo() {
        assertEquals(Optional.empty(), corretor.corrigir("Levou um não e desistiu."),
            "'um não' e substantivo (uma recusa); o 'e' e conjuncao");
        assertEquals(Optional.empty(), corretor.corrigir("Deu o não e foi embora."));
    }

    @Test
    @DisplayName("POSITIVO: `esta` seguido de gerundio, participio ou preposicao e verbo")
    void estaVerbo() {
        assertEquals(Optional.of("Do que você está falando?"),
            corretor.corrigir("Do que você esta falando?"));
        assertEquals(Optional.of("Ela está de serviço de limpeza hoje."),
            corretor.corrigir("Ela esta de serviço de limpeza hoje."));
        assertEquals(Optional.of("Ela está viva?"), corretor.corrigir("Ela esta viva?"));
    }

    /**
     * A fala que refez esta regra. Antes, o padrão era só {@code <pronome> esta} — e casava
     * {@code você esta noite}, onde {@code esta} é o demonstrativo e está CERTO.
     */
    @Test
    @DisplayName("NEGATIVO: `esta noite` e demonstrativo — a fala do acervo que refez a regra")
    void estaDemonstrativoNaoEVerbo() {
        assertEquals(Optional.empty(),
            corretor.corrigir("Entrei em contato com você esta noite para me apresentar."),
            "acentuou o demonstrativo: 'esta noite' esta correto, e a fala e real do acervo");
        assertEquals(Optional.empty(), corretor.corrigir("Ele esta vez nao veio."));
    }

    @Test
    @DisplayName("POSITIVO: `nao ha` e o verbo haver")
    void naoHa() {
        assertEquals(Optional.of("Então não há vitimas."),
            corretor.corrigir("Então não ha vitimas."));
    }

    @Test
    @DisplayName("POSITIVO: `nos` no fim da oracao e o pronome tonico")
    void nosTonico() {
        // A fala tem DOIS defeitos e os dois sao corrigidos na mesma passada — a expectativa
        // errada foi minha, e o teste so ficou honesto depois de eu ler a saida real.
        assertEquals(Optional.of("Este é o fim da linha para nós."),
            corretor.corrigir("Este e o fim da linha para nos."));
        assertEquals(Optional.of("Eles não tem poder contra nós."),
            corretor.corrigir("Eles não tem poder contra nos."));
    }

    /**
     * A segunda fala que refez uma regra. {@code nos} entre preposição e verbo é objeto —
     * <i>"para nos salvar"</i> significa "para salvar A NÓS", e trocar ali estraga fala correta.
     */
    @Test
    @DisplayName("NEGATIVO: `para nos salvar` e pronome obliquo — a outra fala que refez a regra")
    void nosObliquoNaoEtonico() {
        assertEquals(Optional.empty(),
            corretor.corrigir("Ela desceu ao mundo dos mortais para nos salvar."),
            "trocou o pronome obliquo por tonico: 'para nos salvar' esta correto");
    }

    /**
     * O DEFEITO DA AUDITORIA DA 3.3 (17/09/2026), lente de corrupcao estrutural: o {@code \\N} do
     * ASS e quebra VISUAL de linha, nao fim de oracao. O lookahead de {@code NOS_TONICO} tratava a
     * quebra como fim de oracao e acentuava o {@code nos} OBJETO no meio da clausula. Como 24,6%
     * das falas do acervo tem {@code \\N}, o wrap cai depois de {@code nos} com frequencia.
     *
     * <pre>
     *   "para nos\Najudar"  (para nos ajudar, pronome atono, CERTO)  virava  "para nós\Najudar"
     * </pre>
     */
    @Test
    @DisplayName("NEGATIVO: `nos` antes da quebra \\N e OBJETO — a quebra nao e fim de oracao")
    void quebraNaoEfimDeOracaoParaNos() {
        assertEquals(Optional.empty(), corretor.corrigir("Você veio para nos\\Najudar?"),
            "tratou a quebra visual \\N como fim de oracao e acentuou o pronome OBJETO 'nos'");
        assertEquals(Optional.empty(), corretor.corrigir("Ela desceu para nos\\Nsalvar a todos."),
            "o verbo na linha seguinte prova que 'para nos salvar' e objeto, nao tonico");
    }

    /**
     * O CONTRA-TESTE: o {@code nos} TONICO de verdade — no fim da oracao, com pontuacao, fim de
     * fala, ou uma quebra que encerra a fala — continua sendo acentuado. Sem ele, apagar a regra
     * inteira passaria no teste acima: guarda que reprova o certo e pior que guarda nenhuma.
     */
    @Test
    @DisplayName("CONTROLE: `nos` tonico no fim da oracao continua sendo acentuado")
    void nosTonicoNoFimContinuaSendoAcentuado() {
        assertEquals(Optional.of("Fica entre nós."), corretor.corrigir("Fica entre nos."));
        assertEquals(Optional.of("O segredo fica entre nós"), corretor.corrigir("O segredo fica entre nos"));
        // A quebra que ENCERRA a fala (nada depois dela) e fim de oracao de verdade — continua acentuando.
        assertEquals(Optional.of("Fica entre nós\\N"), corretor.corrigir("Fica entre nos\\N"));
    }

    @Test
    @DisplayName("POSITIVO: `so` antes de palavra portuguesa e adverbio")
    void soAdverbio() {
        assertEquals(Optional.of("Este veículo só tem dois lugares."),
            corretor.corrigir("Este veículo so tem dois lugares."));
    }

    @Test
    @DisplayName("NEGATIVO: `so` do ingles nao e tocado")
    void soDoInglesFicaIntacto() {
        assertEquals(Optional.empty(), corretor.corrigir("I love you so much, baby"),
            "acentuou o 'so' do ingles — o acervo tem letra de musica e residuo em ingles");
    }

    @Test
    @DisplayName("POSITIVO: futuro sem acento, com a caixa preservada")
    void futuro() {
        assertEquals(Optional.of("Isso terá que servir."), corretor.corrigir("Isso tera que servir."));
        assertEquals(Optional.of("Será que ele vem?"), corretor.corrigir("Sera que ele vem?"),
            "perdeu a maiuscula de inicio de frase");
    }

    @Test
    @DisplayName("duas correcoes na MESMA fala, sem deslocar uma a outra")
    void duasNaMesmaFala() {
        Optional<String> r = corretor.corrigir("Isso e serio, e ela esta indo embora.");
        assertTrue(r.isPresent(), "nao corrigiu nada numa fala com dois defeitos");
        assertTrue(r.get().contains("Isso é") && r.get().contains("está indo"),
            "uma correcao deslocou a outra: " + r.get());
    }

    /**
     * {@code ira} e o unico do padrao do futuro com homografo: substantivo (colera) e verbo. O
     * acervo tem 0 do substantivo e 3 do verbo hoje, e o escudo existe para a traducao de amanha.
     */
    @Test
    @DisplayName("`ira` verbo e acentuado; `a ira` substantivo NAO")
    void iraVerboEsubstantivo() {
        assertEquals(Optional.of("Você só irá cometer mais crimes!"),
            corretor.corrigir("Você so ira cometer mais crimes!"));
        assertEquals(Optional.empty(), corretor.corrigir("A ira dele era visível."),
            "acentuou 'ira' substantivo — 'a ira dele' esta correto sem acento");
        assertEquals(Optional.empty(), corretor.corrigir("Ele falou com ira."),
            "acentuou 'ira' depois de preposicao — continua sendo o substantivo");
    }

    /**
     * O DANO NOVO da auditoria da 3.3 (17/09/2026): {@code ira} e homografo TRIPLO. A versao
     * anterior RENOMEAVA o personagem {@code Ira} (ex.: Ira Gamagoori) e acentuava a colera
     * ({@code sem ira}, {@code tua ira}). Agora o alvo casa so a forma minuscula e so antes de
     * infinitivo.
     */
    @Test
    @DisplayName("NEGATIVO: nome 'Ira' e colera 'ira' nao viram verbo — homografo triplo")
    void iraNomeEColeraNaoViramVerbo() {
        // Nome proprio (maiuscula): nunca tocado, nem no comeco nem no meio da fala.
        assertEquals(Optional.empty(), corretor.corrigir("Ira, espere!"),
            "renomeou o personagem 'Ira' para 'Ira' com acento");
        assertEquals(Optional.empty(), corretor.corrigir("Obrigado, Ira."));
        assertEquals(Optional.empty(), corretor.corrigir("Vi Ira cometer o crime."),
            "nome no meio da fala, seguido de infinitivo, foi renomeado");
        // Substantivo colera (minuscula), nao seguido de infinitivo.
        assertEquals(Optional.empty(), corretor.corrigir("Ele falou sem ira."),
            "acentuou a colera 'ira' depois de 'sem'");
        assertEquals(Optional.empty(), corretor.corrigir("Tua ira me consome."),
            "acentuou a colera 'ira' depois de 'tua'");
    }

    /**
     * A catraca {@code CatracaFronteiraQuebraAssTest} reprovou a primeira versão destes padrões,
     * e com razão: <b>24,6% das falas do acervo têm a quebra {@code \\N}</b>, e ela cai no meio
     * da frase. Com {@code \\s+} entre as palavras, {@code "Isso\\Ne tudo"} não casava — o
     * defeito ficava invisível justamente onde a legenda é mais longa.
     */
    @Test
    @DisplayName("a quebra do ASS no meio do padrao nao esconde o defeito")
    void quebraDoAssNaoEscondeODefeito() {
        assertEquals(Optional.of("Isso\\Né tudo."), corretor.corrigir("Isso\\Ne tudo."),
            "a quebra entre o demonstrativo e o verbo escondeu o defeito");
        assertEquals(Optional.of("Você\\Nestá falando demais."),
            corretor.corrigir("Você\\Nesta falando demais."),
            "a quebra antes de 'esta' escondeu o defeito");
        assertEquals(Optional.of("Não\\Nhá tempo."), corretor.corrigir("Não\\Nha tempo."));
    }

    @Test
    @DisplayName("entrada degenerada nao lanca")
    void degenerada() {
        assertEquals(Optional.empty(), corretor.corrigir(null));
        assertEquals(Optional.empty(), corretor.corrigir(""));
        assertEquals(Optional.empty(), corretor.corrigir("   "));
        assertEquals(Optional.empty(), corretor.corrigir("Uma fala perfeitamente correta."));
    }
}
