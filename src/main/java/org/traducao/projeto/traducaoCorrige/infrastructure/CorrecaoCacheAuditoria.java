package org.traducao.projeto.traducaoCorrige.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.traducaoCorrige.domain.EntradaAuditoriaCorrecaoCache;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * PROPÓSITO DE NEGÓCIO: persiste em JSONL o histórico granular do menu Correção
 * do Cache para auditoria, recuperação e uso como dataset de melhoria.
 *
 * <p>INVARIANTES DO DOMÍNIO: arquivo canônico fica no projeto, em
 * {@code cache/auditoria}; registros existentes nunca são reescritos.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: emite warning e não derruba a operação que
 * já preserva o cache por backup e escrita atômica.
 */
@Component
public class CorrecaoCacheAuditoria {

    private static final Logger log = LoggerFactory.getLogger(CorrecaoCacheAuditoria.class);
    private static final Path ARQUIVO = DiretorioBaseKronos.resolver("cache", "auditoria", "correcao_cache.jsonl");

    private final ObjectMapper mapper;

    /**
     * PROPÓSITO DE NEGÓCIO: usa o serializador comum do projeto no dataset JSONL.
     * <p>INVARIANTES DO DOMÍNIO: cada evento vira um objeto JSON em uma linha.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede o uso do serviço.
     */
    public CorrecaoCacheAuditoria(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: acrescenta uma decisão de manutenção ao dataset sem
     * alterar decisões anteriores.
     *
     * <p>INVARIANTES DO DOMÍNIO: uma entrada ocupa exatamente uma linha JSON e a
     * escrita é serializada entre operações.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: registra warning e retorna normalmente.
     */
    public synchronized void registrar(EntradaAuditoriaCorrecaoCache entrada) {
        if (entrada == null) return;
        try {
            Files.createDirectories(ARQUIVO.toAbsolutePath().getParent());
            Files.writeString(
                ARQUIVO,
                mapper.writeValueAsString(entrada) + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            log.warn("Falha ao registrar auditoria da correção de cache em {}: {}", ARQUIVO, e.getMessage());
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: expõe o caminho canônico para relatórios e testes.
     *
     * <p>INVARIANTES DO DOMÍNIO: sempre aponta para dentro da pasta cache do
     * projeto.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não acessa o disco e não lança.
     */
    public Path caminhoCanonico() {
        return ARQUIVO.toAbsolutePath();
    }
}
