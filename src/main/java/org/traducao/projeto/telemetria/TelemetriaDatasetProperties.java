package org.traducao.projeto.telemetria;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PROPÓSITO DE NEGÓCIO: configura a publicação do dataset público e a coleta
 * sanitizada do hardware local que contextualiza os benchmarks.
 *
 * <p>INVARIANTES DO DOMÍNIO: hardware publicado é sempre detectado na máquina
 * atual; não existe override manual de CPU, GPU ou RAM capaz de misturar hosts.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: propriedades ausentes usam padrões seguros;
 * a detecção pode cair para dados limitados da JVM, sem inventar componentes.
 */
@ConfigurationProperties(prefix = "telemetria-dataset")
public class TelemetriaDatasetProperties {

    /** Pasta local do repositório do dataset (irmã do projeto por padrão). */
    private String repositorioLocal = "../kronos-anime-translation-telemetry-dataset";

    /** Remoto Git para onde o dataset é publicado. */
    private String repositorioRemoto = "https://github.com/carmipa/kronos-anime-translation-telemetry-dataset.git";

    /** Metadados públicos e sanitizados do ambiente de execução. */
    private Hardware hardware = new Hardware();

    public TelemetriaDatasetProperties() {
    }

    public String repositorioLocal() { return repositorioLocal; }
    public String getRepositorioLocal() { return repositorioLocal; }
    public void setRepositorioLocal(String repositorioLocal) { this.repositorioLocal = repositorioLocal; }

    public String repositorioRemoto() { return repositorioRemoto; }
    public String getRepositorioRemoto() { return repositorioRemoto; }
    public void setRepositorioRemoto(String repositorioRemoto) { this.repositorioRemoto = repositorioRemoto; }

    public Hardware hardware() { return hardware; }
    public Hardware getHardware() { return hardware; }
    public void setHardware(Hardware hardware) { this.hardware = hardware != null ? hardware : new Hardware(); }

    /**
     * PROPÓSITO DE NEGÓCIO: controla apenas se a fotografia sanitizada do
     * hardware local deve ser publicada e detectada automaticamente.
     *
     * <p>INVARIANTES DO DOMÍNIO: não armazena nomes ou capacidades físicas; esses
     * valores pertencem exclusivamente à detecção da máquina atual.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: valores ausentes mantêm os padrões
     * seguros de publicação e detecção habilitadas.
     */
    public static class Hardware {
        /** Inclui o bloco ambienteExecucao no JSON público do dataset. */
        private boolean publicarAmbienteExecucao = true;

        /** Usa detecção local por SO quando disponível. */
        private boolean permitirDeteccaoAutomatica = true;

        public boolean publicarAmbienteExecucao() { return publicarAmbienteExecucao; }
        public boolean isPublicarAmbienteExecucao() { return publicarAmbienteExecucao; }
        public void setPublicarAmbienteExecucao(boolean publicarAmbienteExecucao) {
            this.publicarAmbienteExecucao = publicarAmbienteExecucao;
        }

        public boolean permitirDeteccaoAutomatica() { return permitirDeteccaoAutomatica; }
        public boolean isPermitirDeteccaoAutomatica() { return permitirDeteccaoAutomatica; }
        public void setPermitirDeteccaoAutomatica(boolean permitirDeteccaoAutomatica) {
            this.permitirDeteccaoAutomatica = permitirDeteccaoAutomatica;
        }
    }
}
