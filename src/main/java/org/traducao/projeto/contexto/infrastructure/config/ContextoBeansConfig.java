package org.traducao.projeto.contexto.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.contexto.infrastructure.CatalogoLoreYaml;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: entrega ao resto do sistema a lista de obras com lore. Desde 2026-08-15
 * ela vem do ARQUIVO ÚNICO ({@code /lore/lore-traducao.yaml}), e não mais de 78 classes Java
 * descobertas por CDI — ordem de Paulo: <i>"todas as lores devem ficar em um único arquivo"</i>.
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
 *   <li>O catálogo é lido UMA vez, na construção do bean. Nenhum I/O por chamada.</li>
 *   <li><b>Falha FECHADA.</b> Arquivo ausente, ilegível, sem obras, com id repetido ou com obra
 *       sem prompt faz o {@link CatalogoLoreYaml} lançar, e a aplicação NÃO SOBE. É deliberado:
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

    private final CatalogoLoreYaml catalogo = new CatalogoLoreYaml();

    /**
     * PROPÓSITO DE NEGÓCIO: expõe as obras do arquivo único ao {@code GerenciadorContexto} e a
     * quem injeta {@code List<ProvedorContexto>}.
     * <p>INVARIANTES DO DOMÍNIO: lista imutável, na ordem do arquivo; nunca vazia.
     * <p>COMPORTAMENTO EM CASO DE FALHA: a leitura já ocorreu na construção; aqui não lança.
     */
    @Bean
    public List<ProvedorContexto> todosProvedoresContexto() {
        return catalogo.obras();
    }
}
