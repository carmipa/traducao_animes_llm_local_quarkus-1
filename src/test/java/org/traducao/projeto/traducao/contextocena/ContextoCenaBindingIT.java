package org.traducao.projeto.traducao.contextocena;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducao.infrastructure.contextocena.ContextoCenaProperties;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova DEFINITIVA de que a flag do contexto de cena
 * ({@code tradutor.contexto-cena.ativo}) realmente liga o motor via binding de
 * {@code @ConfigurationProperties}. Nasceu de um caso real: uma retradução do
 * Gundam 08th NÃO engatou o motor D apesar de a intenção ser ligá-lo (o cache saiu
 * sem {@code assinaturaContexto}). Se este teste PASSA, o mecanismo de configuração
 * funciona e a causa é a FONTE da config (chave num arquivo/perfil não carregado,
 * ex.: {@code application-local.yml} fora do perfil {@code dev}); se FALHA, o binding
 * está quebrado e é bug de produção.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Com as chaves presentes numa fonte carregada (perfil de teste), o bean CDI
 *       injetado reflete {@code ativo=true}, {@code tamanho-janela=3} e
 *       {@code relatorio-ab=true} — os três campos bindam por setter.</li>
 *   <li>Ligada, {@link ContextoCenaProperties#marcadorProveniencia()} carrega o
 *       marcador {@code contexto-cena} (invalida o cache do baseline).</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer campo não refletido reprova, sinalizando bug de binding — não config do
 * usuário. Fecha a lacuna de que o binding por config desta classe nunca fora
 * exercitado (os demais testes constroem o objeto à mão).
 */
@QuarkusTest
@TestProfile(ContextoCenaBindingIT.PerfilContextoCenaLigado.class)
class ContextoCenaBindingIT {

    public static class PerfilContextoCenaLigado implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "tradutor.contexto-cena.ativo", "true",
                "tradutor.contexto-cena.tamanho-janela", "3",
                "tradutor.contexto-cena.relatorio-ab", "true"
            );
        }
    }

    @Inject
    ContextoCenaProperties props;

    @Test
    @DisplayName("binding: ativo=true via config realmente popula o bean injetado (o D engataria)")
    void flagLigaOMotor() {
        assertTrue(props.ativo(),
            "tradutor.contexto-cena.ativo=true deve refletir no bean CDI — senão o motor D nunca engata");
        assertEquals(3, props.tamanhoJanela(), "tradutor.contexto-cena.tamanho-janela deve bindar por setter");
        assertTrue(props.relatorioAb(), "tradutor.contexto-cena.relatorio-ab deve bindar por setter");
        assertTrue(props.marcadorProveniencia().contains("contexto-cena"),
            "com ativo, a proveniência carrega o marcador (separa o cache do baseline)");
    }
}
