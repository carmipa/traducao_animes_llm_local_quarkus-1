package org.traducao.projeto.traducao.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.application.ValidadorCompatibilidadeObraContexto;
import org.traducao.projeto.contexto.domain.SnapshotContexto;
import org.traducao.projeto.contexto.domain.VeredictoObraContexto;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.traducao.domain.exceptions.ObraDivergenteDoContextoException;
import org.traducao.projeto.traducao.presentation.ui.ConsoleUILogger;

import java.nio.file.Path;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: portão de entrada da tradução de um arquivo — TRADUZ O VEREDICTO
 * obra×contexto EM EFEITO dentro da fatia {@code traducao}: bloqueia o arquivo quando a obra
 * escrita no CAMINHO não é a obra cuja lore está selecionada, avisa quando não há base para
 * julgar e segue nos demais casos. Nada da regra de IDENTIDADE DE OBRA mora aqui: quem
 * reconhece a pasta e quem emite o veredicto é o peer {@code contexto} (o catálogo
 * {@code GerenciadorContexto} e o {@code ValidadorCompatibilidadeObraContexto}). Esta classe
 * é o único ponto da fatia que sabe o que FAZER com cada desfecho.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>NÃO decide compatibilidade: não compara ids, não interpreta nomes de pasta e não
 *       redige mensagem própria. Recebe o veredicto pronto do peer {@code contexto} e
 *       apenas o converte em bloqueio, aviso ou seguimento. Reintroduzir aqui qualquer
 *       julgamento de identidade de obra duplicaria a regra fora do seu dono.</li>
 *   <li>Roda ANTES de tudo: antes de ler a legenda, antes da primeira chamada ao LLM e
 *       antes de qualquer escrita de cache ou de saída. Bloquear depois seria tarde — o
 *       incidente que originou esta guarda deixou ~4.442 entradas gravadas.</li>
 *   <li>Determinística: o veredicto depende só do nome da pasta e do vocabulário declarado
 *       nas lores. Sem rede, sem relógio, sem I/O.</li>
 *   <li>{@link VeredictoObraContexto#DIVERGENTE} e {@link VeredictoObraContexto#AMBIGUO}
 *       BLOQUEIAM aquele arquivo; qualquer outro desfecho segue. Os dois usam a MESMA exceção
 *       porque, para o lote, o efeito é idêntico — não foi possível provar que o arquivo é da
 *       obra selecionada. O que muda é a mensagem: divergência nomeia a obra esperada e se
 *       conserta no combo; ambiguidade nomeia as obras empatadas e se conserta declarando um
 *       apelido de pasta mais específico na lore.</li>
 *   <li>Caminho não reconhecido AVISA e segue — e o aviso é só log/console, NUNCA entra na
 *       lista de avisos do episódio: essa lista marca o resultado como PARCIAL, e uma obra
 *       ainda sem vocabulário declarado não é uma tradução incompleta.</li>
 *   <li>A guarda nunca corrige nem troca o contexto ativo por conta própria: adivinhar a
 *       obra certa seria repetir, automatizado, o erro que ela existe para impedir.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Divergência lança {@link ObraDivergenteDoContextoException}, que o lote trata como
 * BLOQUEADO e segue para o próximo arquivo. Todos os demais desfechos retornam
 * normalmente. A guarda falha ABERTA (deixa passar) sempre que não tem prova — obra sem
 * apelidos declarados, pasta irreconhecível ou execução sem contexto ativo —, porque falhar
 * fechada pararia a tradução de toda obra que ainda não declarou vocabulário de pasta.
 */
@Component
public class GuardaContextoObraTraducao {

    private static final Logger log = LoggerFactory.getLogger(GuardaContextoObraTraducao.class);

    private final GerenciadorContexto gerenciadorContexto;
    private final ValidadorCompatibilidadeObraContexto validadorCompatibilidade;
    private final ResolvedorCacheTraducao resolvedorCache;
    private final ConsoleUILogger uiLogger;

    /**
     * PROPÓSITO DE NEGÓCIO: injeta as três fontes do julgamento — o catálogo de obras que
     * sabem se reconhecer, o validador de compatibilidade obra×contexto e o derivador do
     * nome da obra a partir do caminho — mais a saída dinâmica onde o operador lê o bloqueio.
     *
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas; não as substitui nem cria
     * implementação própria. Os dois primeiros colaboradores pertencem ao peer
     * {@code contexto}, dono da identidade de obra.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não valida os argumentos; a injeção CDI garante os beans.
     *
     * @param gerenciadorContexto catálogo dos contextos registrados e de quem reconhece a pasta
     * @param validadorCompatibilidade validador de compatibilidade obra×contexto do peer
     *        {@code contexto}: emite o veredicto e redige as mensagens
     * @param resolvedorCache deriva o nome da obra a partir do caminho do arquivo
     * @param uiLogger saída dinâmica onde o bloqueio e o aviso aparecem para o operador
     */
    public GuardaContextoObraTraducao(
        GerenciadorContexto gerenciadorContexto,
        ValidadorCompatibilidadeObraContexto validadorCompatibilidade,
        ResolvedorCacheTraducao resolvedorCache,
        ConsoleUILogger uiLogger
    ) {
        this.gerenciadorContexto = gerenciadorContexto;
        this.validadorCompatibilidade = validadorCompatibilidade;
        this.resolvedorCache = resolvedorCache;
        this.uiLogger = uiLogger;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: autoriza (ou recusa) a tradução deste arquivo sob este contexto
     * congelado, comparando a obra do caminho com a obra selecionada.
     *
     * <p>INVARIANTES DO DOMÍNIO: usa o {@code id} do SNAPSHOT — não o do gerenciador — para
     * que a checagem julgue exatamente o contexto que será usado no prompt, na validação e
     * na proveniência deste arquivo. Divergência e ambiguidade bloqueiam; indeterminação avisa
     * apenas em log/console e segue; casamento registra a confirmação em log.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link ObraDivergenteDoContextoException}
     * na divergência provada e na ambiguidade de identidade; nenhum outro desfecho lança.
     *
     * @param arquivoEntrada arquivo de legenda prestes a ser traduzido
     * @param contexto snapshot congelado desta execução
     * @throws ObraDivergenteDoContextoException quando a obra do caminho resolve para outro
     *         contexto que não o congelado, ou quando não resolve para uma obra única
     */
    public void verificar(Path arquivoEntrada, SnapshotContexto contexto) {
        String obra = resolvedorCache.animeAPartirDoArquivo(arquivoEntrada);
        Set<String> reconhecedores = gerenciadorContexto.idsQueReconhecem(obra);
        VeredictoObraContexto veredicto = validadorCompatibilidade.avaliar(obra, contexto.id(), reconhecedores);

        switch (veredicto) {
            case DIVERGENTE -> {
                String mensagem = validadorCompatibilidade.mensagemDeBloqueio(
                    arquivoEntrada.toString(), obra, contexto.id(), reconhecedores);
                log.error(mensagem);
                uiLogger.log("[ BLOQUEADO ] " + mensagem);
                throw new ObraDivergenteDoContextoException(mensagem);
            }
            case AMBIGUO -> {
                String mensagem = validadorCompatibilidade.mensagemDeAmbiguidade(
                    arquivoEntrada.toString(), obra, contexto.id(), reconhecedores);
                log.error(mensagem);
                uiLogger.log("[ BLOQUEADO ] " + mensagem);
                throw new ObraDivergenteDoContextoException(mensagem);
            }
            case INDETERMINADO -> {
                // Só log/console: entrar na lista de avisos do episódio o marcaria PARCIAL,
                // e "obra sem vocabulário declarado" não é tradução incompleta.
                String aviso = validadorCompatibilidade.mensagemDeIndeterminacao(obra, contexto.id());
                log.warn(aviso);
                uiLogger.log("[ AVISO ] " + aviso);
            }
            case CASA -> log.info("[ CONTEXTO ] Obra \"{}\" confere com o contexto ativo \"{}\".",
                obra, contexto.id());
        }
    }
}
