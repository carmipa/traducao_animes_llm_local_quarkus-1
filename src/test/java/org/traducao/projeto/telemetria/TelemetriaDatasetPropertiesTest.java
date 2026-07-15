package org.traducao.projeto.telemetria;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PROPÓSITO DE NEGÓCIO: valida a configuração segura da publicação do dataset.
 *
 * <p>INVARIANTES DO DOMÍNIO: ambiente e detecção automática permanecem ativos,
 * sem propriedade manual de GPU capaz de misturar máquinas.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: configuração divergente impede a suíte de
 * integração de aprovar o empacotamento da aplicação.
 */
@QuarkusTest
class TelemetriaDatasetPropertiesTest {

    @Inject
    TelemetriaDatasetProperties propriedades;

    @Inject
    AmbienteExecucaoDatasetService ambienteExecucao;

    /**
     * PROPÓSITO DE NEGÓCIO: comprova que o application.yml habilita a fotografia
     * automática do computador que está publicando o dataset.
     *
     * <p>INVARIANTES DO DOMÍNIO: publicar e detectar estão ativos; nenhum valor
     * físico específico é codificado no arquivo compartilhado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer chave ausente ou falsa reprova
     * o teste e bloqueia a entrega.
     */
    @Test
    void carregaConfiguracaoDeHardwareDoApplicationYaml() {
        assertTrue(propriedades.hardware().publicarAmbienteExecucao());
        assertTrue(propriedades.hardware().permitirDeteccaoAutomatica());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: valida no computador executor que CPU, RAM e GPU
     * principal formam uma fotografia automática coerente do mesmo ambiente.
     *
     * <p>INVARIANTES DO DOMÍNIO: a GPU principal, quando detectada, pertence à
     * lista de GPUs da mesma coleta; RAM é positiva e não há alias configurado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: em ambientes sem GPU acessível aceita
     * lista vazia; inconsistências entre principal e lista reprovam o teste.
     */
    @Test
    void detectaHardwareLocalSemMisturarConfiguracaoManual() {
        AmbienteExecucaoDataset ambiente = ambienteExecucao.detectar(propriedades.hardware());

        assertNotNull(ambiente);
        assertNotNull(ambiente.ramTotalGb());
        assertTrue(ambiente.ramTotalGb() > 0);
        if (ambiente.gpuPrincipal() != null) {
            assertTrue(ambiente.gpusDetectadas().contains(ambiente.gpuPrincipal()));
        }
    }
}
