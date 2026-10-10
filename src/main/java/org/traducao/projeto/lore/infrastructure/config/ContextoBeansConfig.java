package org.traducao.projeto.lore.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.traducao.projeto.lore.infrastructure.CatalogoLoreSqlite;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: entrega ao resto do sistema a lista de obras com lore. Desde 2026-10-09
 * ela vem dos arquivos SQL por obra ({@code /lore/esquema.sql}, {@code /lore/obras.lst} e
 * {@code /lore/obras/<id>.sql}), carregados num SQLite em memória no arranque pelo
 * {@link CatalogoLoreSqlite}. De 2026-08-15 a 2026-10-09 vinha do arquivo único
 * {@code /lore/lore.yaml}, e antes disso de 78 classes Java descobertas por CDI.
 *
 * <h2>A troca de 2026-10-09</h2>
 * Mudou só a fonte, de novo. O {@code lore.yaml} tinha 1 MB, e anexar ou corrigir a lore de uma obra
 * exigia abri-lo inteiro; agora cada obra é um arquivo de ~13 KB. A equivalência foi provada antes da
 * troca com os dois leitores vivos ({@code EquivalenciaCatalogoSqliteYamlTest}: zero divergência em
 * 69 + 69 obras; aposentado junto com o YAML), e o {@code ManifestoCompletoLoreIT} congela obra a obra, lado a lado e campo a
 * campo o que este bean entrega. O hash do prompt é o mesmo {@code contextoHash} do cache, então o
 * acervo já traduzido continua sendo aproveitado.
 *
 * <h2>O que mudou, e o que NÃO mudou</h2>
 * Mudou só a FONTE. O contrato é o mesmo {@link ProvedorContexto}, e os 26 consumidores em 8
 * fatias não souberam da troca — nenhum deles conhecia classe concreta de lore. Era essa
 * indireção que tornava a migração possível sem tocar em quem usa.
 *
 * <p>A equivalência foi provada ANTES da troca, com as duas fontes vivas ao mesmo tempo:
 * {@code EquivalenciaLoreYamlIT} comparou as 69 obras campo a campo contra as classes Java, e
 * o gerador só escreve o arquivo depois de provar ida e volta. Depois da troca essa comparação
 * vira tautologia — quem continua provando o conteúdo são o
 * {@code manifesto-lore.properties} (hash de prompt/nome/termos por obra) e os dois baselines
 * de terminologia e de campos, que comparam o vivo contra fotografia CONGELADA e por isso
 * seguem valendo.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O catálogo é lido UMA vez, no primeiro {@code @Bean} pedido (no boot), e o banco é fechado em seguida. Nenhum
 *       I/O nem chamada nativa por chamada.</li>
 *   <li><b>Falha FECHADA.</b> Arquivo ausente, ilegível, sem obras, com id repetido, com obra sem
 *       prompt, com instrução que não seja inserção de literais ou que escreva outra obra faz o
 *       {@link CatalogoLoreSqlite} lançar, e a aplicação NÃO SOBE. É deliberado:
 *       catálogo de lore silenciosamente vazio faria o pipeline traduzir sem lore nenhuma e
 *       gravar o resultado — o dano apareceria na legenda, semanas depois, e não no boot.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Propaga a exceção do leitor. Nunca devolve lista vazia: lista vazia aqui e "não consegui ler"
 * teriam o mesmo sinal, que é exatamente o modo de falha que este projeto já pagou.
 */
@Configuration
public class ContextoBeansConfig {

    /**
     * Sob demanda, e não no inicializador do campo. O proxy que o CDI cria para esta classe chama
     * o construtor dela, e o inicializador rodava junto: o boot montava o catálogo DUAS vezes
     * (medido em 09/10/2026 no jar: duas linhas "Lore carregada de SQL", 267 ms + 85 ms). Os
     * métodos {@code @Bean} só executam na instância real, então aqui ele é montado uma vez, no
     * mesmo momento de antes — a falha fechada no boot não muda.
     */
    private CatalogoLoreSqlite catalogo;

    private synchronized CatalogoLoreSqlite catalogo() {
        if (catalogo == null) {
            catalogo = new CatalogoLoreSqlite();
        }
        return catalogo;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: expõe as obras da lore ao {@code GerenciadorContexto} e a
     * quem injeta {@code List<ProvedorContexto>}.
     * <p>INVARIANTES DO DOMÍNIO: lista imutável, por id; nunca vazia.
     * <p>COMPORTAMENTO EM CASO DE FALHA: a leitura já ocorreu na construção; aqui não lança.
     */
    @Bean
    public List<ProvedorContexto> todosProvedoresContexto() {
        return catalogo().obras();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: expõe o lado da REVISÃO de lore, lido do MESMO arquivo. É o que
     * fecha a FASE E: a lore de uma obra deixa de ser a UNIÃO de dois pacotes e passa a existir
     * inteira num lugar só.
     *
     * <p>INVARIANTES DO DOMÍNIO: lista imutável. Pode ser vazia sem derrubar a aplicação —
     * diferente do lado da tradução, cuja ausência é fatal. Sem lore de tradução o pipeline
     * traduziria sem lore nenhuma e gravaria o resultado; sem lore de revisão a Opção 7
     * simplesmente não tem obra a oferecer, e quem consome já trata catálogo vazio.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: a leitura já ocorreu na construção; aqui não lança.
     */
    @Bean
    public List<org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore> todosProvedoresRevisaoLore() {
        return catalogo().obrasRevisao();
    }
}
