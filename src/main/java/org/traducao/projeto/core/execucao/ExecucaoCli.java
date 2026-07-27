package org.traducao.projeto.core.execucao;

/**
 * PROPÓSITO DE NEGÓCIO: contrato para modos de execução em linha de comando — o substituto do
 * {@code CommandLineRunner} do Spring Boot. Cada fatia que oferece um modo CLI implementa esta
 * interface, e o despachante de bootstrap resolve qual rodar a partir de {@code app.modo}.
 *
 * <h2>Por que mora no kernel e não em {@code config}</h2>
 * Morava em {@code config} e era a perna de VOLTA de sete ciclos {@code config ⇄ fatia}: o
 * despachante importava cada CLI (bootstrap, legítimo) e cada CLI importava a interface de volta.
 * Um ciclo por fatia com modo CLI — {@code analisadorMidia}, {@code legendasExtracao},
 * {@code mapaProjeto}, {@code remuxer}, {@code raspagemCorrecao}, {@code raspagemRevisao} e
 * {@code traducaoCorrige} — dos quais três eram alvo declarado da FASE 3 do Plano-Mestre do
 * corretor de cache.
 *
 * <p>A mudança é de LUGAR, não de natureza: são as mesmas nove linhas, sem um caractere de
 * comportamento alterado. E o lugar novo é o correto pelo próprio contrato — o kernel técnico é
 * onde vive o que "só usa JDK e bibliotecas técnicas" e é consumível por qualquer fatia. Uma
 * interface de uma linha, sem framework e sem conhecer nenhuma fatia, é exatamente isso; estava em
 * {@code config} por acidente histórico, porque foi lá que nasceu.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Não conhece fatia alguma e não importa framework. Se um dia precisar, não pertence mais
 *       ao kernel.</li>
 *   <li>Uma implementação por modo de execução; quem escolhe o modo é o bootstrap, não a CLI.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A assinatura declara {@code Exception} de propósito: o modo CLI é o topo da pilha e quem trata é
 * o despachante, que loga e encerra. Uma CLI não deve engolir falha para "não quebrar o boot" —
 * seria um modo que reporta sucesso sem ter feito o trabalho.
 *
 * <h2>O que esta mudança NÃO alcançou</h2>
 * O despachante continua importando as oito CLIs, então {@code config → fatia} segue existindo em
 * um sentido. O ciclo morreu — é o que a FASE 3 pede — mas não é o padrão-ouro: a fase D-Config
 * zerou {@code config ⇄ traducao} nos DOIS sentidos, tirando a {@code TradutorCLI} do despachante
 * e dando à fatia um bootstrap próprio ({@code traducao.presentation.bootstrap.TraducaoStartup}).
 * Fazer o mesmo para as outras sete custaria sete bootstraps e a decomposição do despachante.
 */
public interface ExecucaoCli {

    /**
     * PROPÓSITO DE NEGÓCIO: executa o modo escolhido, do início ao fim.
     * <p>INVARIANTES DO DOMÍNIO: chamado UMA vez por processo, pelo bootstrap.
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga; o despachante loga e encerra.
     *
     * @throws Exception qualquer falha do modo, tratada pelo despachante
     */
    void executar() throws Exception;
}
