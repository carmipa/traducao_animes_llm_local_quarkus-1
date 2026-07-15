package org.traducao.projeto.config;

/**
 * Contrato para modos de execucao em linha de comando (substituto do CommandLineRunner do Spring Boot).
 */
public interface ExecucaoCli {

    void executar() throws Exception;
}
