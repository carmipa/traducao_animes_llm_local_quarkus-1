package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.raspagemRevisao.domain.exceptions.RaspagemRevisaoException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * PROPÓSITO DE NEGÓCIO: grava a legenda revisada, preservando antes a versão anterior. É o único
 * ponto do fluxo em que o trabalho do operador pode ser destruído, e por isso o backup e a escrita
 * ficam juntos: separá-los permitiria escrever sem ter preservado.
 *
 * <h2>Os dois momentos em que a revisão grava</h2>
 * Não é só no fim. Quando o diagnóstico de retradução em massa BLOQUEIA o arquivo, o que o cache já
 * havia recuperado é gravado assim mesmo, antes da recusa — "bloqueou" não é sinônimo de "não
 * escreveu". Os dois caminhos usavam código idêntico em lugares diferentes; agora usam este.
 * Unificá-los é o ganho desta extração: uma regra de backup, não duas que podem divergir.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Backup só é necessário quando origem e destino são o MESMO arquivo. Com pasta de saída
 *       separada, o original continua onde está e não há o que preservar.</li>
 *   <li>A primeira fotografia da sessão nunca é substituída: se já existe backup daquele arquivo,
 *       ele é mantido. Um segundo backup sobrescreveria a única cópia do estado inicial pelo estado
 *       intermediário.</li>
 *   <li>Não imprime. Devolve destino e backup; quem conta a história ao operador é o caso de uso,
 *       que sabe qual dos dois momentos está vivendo.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Caminho de backup fora da pasta designada ou falha de I/O lançam
 * {@link RaspagemRevisaoException} e BLOQUEIAM a escrita da nova legenda — nunca se grava por cima
 * do que não se conseguiu preservar.
 */
@Service
public class PersistenciaLegendaRevisada {

    private final EscritorLegendaAss escritor;

    /**
     * PROPÓSITO DE NEGÓCIO: injeta o escritor de ASS.
     * <p>INVARIANTES DO DOMÍNIO: guarda a referência recebida.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede a criação do serviço.
     */
    public PersistenciaLegendaRevisada(EscritorLegendaAss escritor) {
        this.escritor = escritor;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: onde a legenda foi gravada e o que foi preservado antes.
     *
     * @param destino arquivo escrito
     * @param backup cópia do estado anterior, ou {@code null} quando não era necessária
     */
    public record Gravacao(Path destino, Path backup) {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: preserva a versão anterior e grava a nova, nessa ordem.
     *
     * <p>INVARIANTES DO DOMÍNIO: o backup acontece ANTES da escrita. Inverter a ordem e falhar no
     * meio deixaria o arquivo novo no lugar sem nenhuma cópia do antigo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança sem ter escrito.
     *
     * @param documento a legenda a gravar
     * @param arquivoPt o arquivo de origem, usado para nomear o destino e decidir o backup
     * @param saidaDir pasta de destino; igual à de origem significa sobrescrita
     * @param pastaBackup onde a versão anterior é preservada
     * @return o destino escrito e o backup criado, quando houve
     */
    public Gravacao gravar(DocumentoLegenda documento, Path arquivoPt, Path saidaDir, Path pastaBackup) {
        Path destino = saidaDir.resolve(arquivoPt.getFileName());
        Path backup = criarBackupSeSobrescrever(arquivoPt, destino, pastaBackup);
        escritor.escrever(destino, documento);
        return new Gravacao(destino, backup);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: preserva a legenda anterior antes de a Opção 6 sobrescrever o arquivo
     * de trabalho.
     *
     * <p>INVARIANTES DO DOMÍNIO: backup só é necessário quando origem e destino são o mesmo
     * arquivo; a primeira fotografia da sessão nunca é substituída.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança exceção de domínio e bloqueia a escrita da nova
     * legenda.
     */
    Path criarBackupSeSobrescrever(Path origem, Path destino, Path pastaBackup) {
        Path origemAbs = origem.toAbsolutePath().normalize();
        Path destinoAbs = destino.toAbsolutePath().normalize();
        if (!origemAbs.equals(destinoAbs)) {
            return null;
        }
        Path backup = pastaBackup.resolve(origem.getFileName()).normalize();
        if (!backup.startsWith(pastaBackup)) {
            throw new RaspagemRevisaoException("Caminho de backup inválido para: " + origem);
        }
        try {
            Files.createDirectories(backup.getParent());
            if (Files.notExists(backup)) {
                Files.copy(origemAbs, backup, StandardCopyOption.COPY_ATTRIBUTES);
            }
            return backup;
        } catch (IOException e) {
            throw new RaspagemRevisaoException("Falha ao criar backup da legenda: " + origem, e);
        }
    }
}
