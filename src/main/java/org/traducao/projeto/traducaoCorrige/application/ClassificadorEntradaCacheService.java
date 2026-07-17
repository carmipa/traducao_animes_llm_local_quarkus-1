package org.traducao.projeto.traducaoCorrige.application;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducao.application.DetectorTraducaoIdenticaService;
import org.traducao.projeto.traducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.traducao.application.ValidadorTraducaoService;
import org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;

/**
 * PROPÓSITO DE NEGÓCIO: aplica ao menu Correção do Cache a mesma decisão de
 * validade usada pela Tradução Local, distinguindo falha real de nome, sigla,
 * número, termo de lore, karaokê ou efeito que deve permanecer intocado.
 *
 * <p>INVARIANTES DO DOMÍNIO: entrada protegida nunca é enviada ao Google/LLM;
 * tradução idêntica autorizada pela lore é válida; vazio, fallback não
 * autorizado e resposta rejeitada pelo validador são candidatos à correção.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: campos ausentes são classificados como
 * {@code IGNORADA}; exceções do validador viram {@code INVALIDA} com motivo.
 */
@Service
public class ClassificadorEntradaCacheService {

    public enum Status { VALIDA, VAZIA, NAO_TRADUZIDA, INVALIDA, PROTEGIDA, IGNORADA }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve status e justificativa estáveis para
     * auditoria e para os três modos de manutenção.
     *
     * <p>INVARIANTES DO DOMÍNIO: status nunca é nulo; motivo sempre é legível.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: record imutável sem efeitos colaterais.
     */
    public record Classificacao(Status status, String motivo) {
        /**
         * PROPÓSITO DE NEGÓCIO: informa se a entrada deve ser limpa ou preenchida.
         * <p>INVARIANTES DO DOMÍNIO: válida/protegida/ignorada nunca vira candidata.
         * <p>COMPORTAMENTO EM CASO DE FALHA: retorna decisão booleana estável.
         */
        public boolean precisaCorrecao() {
            return status == Status.VAZIA || status == Status.NAO_TRADUZIDA || status == Status.INVALIDA;
        }
    }

    private final DetectorTraducaoIdenticaService detectorIdentica;
    private final ValidadorTraducaoService validador;
    private final PoliticaEstiloMusical politicaEstiloMusical;
    private final DetectorEfeitoKaraokeService detectorKaraoke;
    private final ProtecaoLegendaAssService protecaoAss;

    /**
     * PROPÓSITO DE NEGÓCIO: reúne num classificador as blindagens da Tradução Local.
     * <p>INVARIANTES DO DOMÍNIO: todas as proteções participam da decisão canônica.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede o uso do serviço.
     */
    public ClassificadorEntradaCacheService(
        DetectorTraducaoIdenticaService detectorIdentica,
        ValidadorTraducaoService validador,
        PoliticaEstiloMusical politicaEstiloMusical,
        DetectorEfeitoKaraokeService detectorKaraoke,
        ProtecaoLegendaAssService protecaoAss
    ) {
        this.detectorIdentica = detectorIdentica;
        this.validador = validador;
        this.politicaEstiloMusical = politicaEstiloMusical;
        this.detectorKaraoke = detectorKaraoke;
        this.protecaoAss = protecaoAss;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se uma entrada do banco precisa ser mantida,
     * invalidada ou reparada.
     *
     * <p>INVARIANTES DO DOMÍNIO: proteções visuais são avaliadas antes do
     * conteúdo; a lore ativa participa da decisão de tradução idêntica.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conteúdo inválido retorna uma
     * classificação segura em vez de lançar para o lote inteiro.
     */
    public Classificacao classificar(ObjectNode entrada) {
        String original = texto(entrada, "original");
        String traduzido = texto(entrada, "traduzido");
        String estilo = texto(entrada, "estilo");
        if (original == null || original.isBlank()) {
            return new Classificacao(Status.IGNORADA, "Entrada sem texto original");
        }
        if (estilo != null && politicaEstiloMusical.estiloIgnorado(estilo)
            && !detectorKaraoke.eKaraokeOuMusicaTraduzivel(estilo, original)) {
            return new Classificacao(Status.PROTEGIDA, "Estilo protegido");
        }
        if (detectorKaraoke.eEfeitoKaraoke(original)
            && !detectorKaraoke.eKaraokeOuMusicaTraduzivel(estilo, original)) {
            return new Classificacao(Status.PROTEGIDA, "Karaokê/efeito protegido");
        }
        if (protecaoAss.deveIgnorarIntervencaoIa(estilo, original)) {
            return new Classificacao(Status.PROTEGIDA, "Linha ASS gráfica protegida");
        }
        if (traduzido == null || traduzido.isBlank()) {
            return new Classificacao(Status.VAZIA, "Tradução vazia");
        }
        if (detectorIdentica.pareceNaoTraduzida(original, traduzido)) {
            return new Classificacao(Status.NAO_TRADUZIDA, "Fallback idêntico ao original sem proteção da lore");
        }
        try {
            validador.validarFala(traduzido);
            return new Classificacao(Status.VALIDA, "Tradução válida");
        } catch (AlucinacaoDetectadaException e) {
            return new Classificacao(Status.INVALIDA, e.getMessage());
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lê campos textuais opcionais sem espalhar casts
     * frágeis pelos casos de uso.
     *
     * <p>INVARIANTES DO DOMÍNIO: nulo JSON e campo ausente resultam em nulo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança para tipos JSON escalares;
     * usa a representação textual fornecida pelo Jackson.
     */
    public static String texto(ObjectNode entrada, String campo) {
        return entrada == null || entrada.get(campo) == null || entrada.get(campo).isNull()
            ? null : entrada.get(campo).asText();
    }
}
