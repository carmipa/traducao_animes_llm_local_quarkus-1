package org.traducao.projeto.trocaTipoLegenda.infrastructure;

import org.springframework.stereotype.Component;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.trocaTipoLegenda.domain.ports.LegendaIoPort;

import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: liga a porta de I/O da fatia ao par leitor/escritor ASS do peer
 * {@code legenda}. É a única classe da fatia que conhece essas duas implementações.
 *
 * <p>INVARIANTES DO DOMÍNIO: repassa as chamadas sem transformar o documento; não faz
 * backup nem decide política de preservação — isso é do caso de uso, via
 * {@code ArmazenamentoBackupPort}.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: propaga a exceção do leitor/escritor, para o laço
 * do caso de uso tratá-la por arquivo sem abortar o lote.
 */
@Component
public class LegendaIoAssAdapter implements LegendaIoPort {

    private final LeitorLegendaAss leitor;
    private final EscritorLegendaAss escritor;

    public LegendaIoAssAdapter(LeitorLegendaAss leitor, EscritorLegendaAss escritor) {
        this.leitor = leitor;
        this.escritor = escritor;
    }

    @Override
    public DocumentoLegenda ler(Path arquivo) {
        return leitor.ler(arquivo);
    }

    @Override
    public void escrever(Path arquivo, DocumentoLegenda documento) {
        escritor.escrever(arquivo, documento);
    }
}
