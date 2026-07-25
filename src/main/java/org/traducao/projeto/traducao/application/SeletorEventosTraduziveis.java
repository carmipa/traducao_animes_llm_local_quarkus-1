package org.traducao.projeto.traducao.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.application.ProtecaoCamadasMusicaisService;
import org.traducao.projeto.legenda.application.ProtecaoCamadasMusicaisService.ProtecaoCamadas;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;

import java.util.HashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: decide quais falas de uma legenda devem ir ao LLM, blindando
 * typesetting pesado (karaokê cru, desenho vetorial, letreiros animados quadro a quadro)
 * que a tradução destruiria — isolando essa elegibilidade da orquestração de
 * {@link ProcessarArquivoUseCase} (FASE F, R4).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só falas de diálogo com texto visível são candidatas; estilo musical ignorado só
 *       passa quando o detector de karaokê o reconhece como música traduzível.</li>
 *   <li>Karaokê cru, desenho vetorial ({@code \p1}) e typesetting de alto risco são
 *       bloqueados antes do LLM.</li>
 *   <li>Um letreiro animado é reconhecido pela CONJUNÇÃO tag de efeito pesada + pouco
 *       texto visível + repetição do mesmo texto >= {@value #LIMIAR_REPETICAO_LETREIRO} no
 *       arquivo; a repetição é o sinal decisivo para não descartar por engano uma fala
 *       isolada com efeito visual.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Métodos puros sobre o documento em memória, sem I/O nem exceção: na dúvida a decisão
 * pende para o lado seguro do typesetting — texto sem parte visível ou de alto risco é
 * excluído da tradução, preservando o efeito original.
 */
@Component
public class SeletorEventosTraduziveis {

    private static final Logger log = LoggerFactory.getLogger(SeletorEventosTraduziveis.class);

    // Um letreiro/título animado quadro a quadro reaparece muitas vezes com o
    // mesmo texto visível (só a tag de efeito muda a cada quadro). Abaixo
    // disso é mais provável ser só uma fala com efeito visual pontual (ex.:
    // duas camadas contorno+preenchimento de uma mesma linha de encerramento).
    private static final int LIMIAR_REPETICAO_LETREIRO = 5;

    private final PoliticaEstiloMusical politicaEstiloMusical;
    private final DetectorEfeitoKaraokeService detectorKaraoke;
    private final ProtecaoLegendaAssService protecaoAss;
    private final MascaradorTags mascarador;
    private final ProtecaoCamadasMusicaisService protecaoCamadasMusicais;

    /**
     * PROPÓSITO DE NEGÓCIO: injeta as blindagens que decidem a elegibilidade de uma fala —
     * política de estilo musical, detector de karaokê, proteção ASS e mascarador.
     *
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas; não as substitui nem cria
     * implementação própria.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não valida os argumentos; a injeção CDI garante os beans.
     *
     * @param politicaEstiloMusical define quais estilos musicais são, por padrão, ignorados
     * @param detectorKaraoke reconhece karaokê cru e música traduzível
     * @param protecaoAss extrai texto visível e bloqueia typesetting de alto risco
     * @param mascarador decide, ao final, se há texto realmente traduzível
     */
    public SeletorEventosTraduziveis(
        PoliticaEstiloMusical politicaEstiloMusical,
        DetectorEfeitoKaraokeService detectorKaraoke,
        ProtecaoLegendaAssService protecaoAss,
        MascaradorTags mascarador,
        ProtecaoCamadasMusicaisService protecaoCamadasMusicais
    ) {
        this.politicaEstiloMusical = politicaEstiloMusical;
        this.detectorKaraoke = detectorKaraoke;
        this.protecaoAss = protecaoAss;
        this.mascarador = mascarador;
        this.protecaoCamadasMusicais = protecaoCamadasMusicais;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: calcula, UMA VEZ POR ARQUIVO, quais linhas são a camada original do
     * karaokê — mesmo molde de {@code calcularFrequenciaTextoLimpo}, que já é calculado uma vez e
     * consultado a cada evento. A regra pertence ao peer {@code legenda}; aqui é só consumo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem o serviço injetado (construções manuais em teste)
     * devolve {@link ProtecaoCamadas#VAZIA}, e o pipeline se comporta como antes.
     */
    public ProtecaoCamadas calcularProtecaoCamadas(DocumentoLegenda documento) {
        if (protecaoCamadasMusicais == null) {
            return ProtecaoCamadas.VAZIA;
        }
        return protecaoCamadasMusicais.calcular(documento);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: conta, por texto "limpo" (sem tags de override ASS), quantas
     * vezes ele aparece entre as falas de diálogo — a base para reconhecer letreiro
     * animado, que reaproveita o mesmo texto visível dezenas ou milhares de vezes.
     *
     * <p>INVARIANTES DO DOMÍNIO: só falas de diálogo com texto visível entram na contagem;
     * o texto recebido nunca é modificado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: método puro; documento sem falas de diálogo
     * devolve um mapa vazio.
     */
    public Map<String, Long> calcularFrequenciaTextoLimpo(DocumentoLegenda documento) {
        Map<String, Long> frequencia = new HashMap<>();
        for (EventoLegenda evento : documento.eventos()) {
            if (!evento.isDialogo() || !evento.temTexto()) {
                continue;
            }
            String textoLimpo = protecaoAss.textoVisivel(evento.texto());
            if (!textoLimpo.isEmpty()) {
                frequencia.merge(textoLimpo, 1L, Long::sum);
            }
        }
        return frequencia;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se uma fala deve ser enviada ao LLM ou preservada como
     * está, aplicando em ordem as blindagens de estilo musical, karaokê, desenho vetorial,
     * typesetting de alto risco e letreiro animado.
     *
     * <p>INVARIANTES DO DOMÍNIO: cada blindagem só exclui a fala quando seus sinais
     * conjuntos se confirmam; a repetição do texto visível (via {@code frequenciaTextoLimpo})
     * é o gatilho decisivo do bloqueio de letreiro animado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: método puro; a decisão final recai sobre
     * {@code mascarador.contemTextoTraduzivel}, garantindo que só texto realmente
     * traduzível chegue ao LLM.
     */
    public boolean isTraduzivel(EventoLegenda evento, Map<String, Long> frequenciaTextoLimpo) {
        return isTraduzivel(evento, frequenciaTextoLimpo, ProtecaoCamadas.VAZIA);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mesma decisão, agora podendo consultar a proteção das camadas de
     * karaokê calculada uma vez por arquivo pelo peer {@code legenda}. É o sinal PRIMÁRIO da
     * proteção de romaji: quando duas linhas ocupam o mesmo tempo, a original é reconhecida pelo
     * PAREAMENTO, e não por nome de estilo ou fonte — que Paulo normaliza de propósito.
     *
     * <p>INVARIANTES DO DOMÍNIO: a proteção só BLOQUEIA; nunca libera o que as demais regras
     * negariam. Par indeciso não protege ninguém, então o comportamento anterior é preservado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code protecao} nula equivale a
     * {@link ProtecaoCamadas#VAZIA} — a decisão fica idêntica à da sobrecarga sem proteção.
     */
    public boolean isTraduzivel(EventoLegenda evento, Map<String, Long> frequenciaTextoLimpo, ProtecaoCamadas protecao) {
        if (!evento.isDialogo() || !evento.temTexto()) {
            return false;
        }
        if (protecao != null && protecao.protege(evento.indice())) {
            return false;
        }
        String texto = evento.texto();
        // REGRA DE ESCOPO (Paulo, 2026-07-25): a Tradução Local traduz FALA. Música e karaokê —
        // inclusive em inglês — são responsabilidade da fatia própria (Traduzir Karaokê), que sabe
        // lidar com KFX, camadas e timing por sílaba. Aqui não é "não consigo traduzir", é "não é
        // meu trabalho".
        //
        // Antes, letra em idioma latino passava por esta porta e virava a MAIOR fonte de pendência
        // do projeto: na execução de 2026-07-25 (Guilty Crown completo), MUSICA_LATINA respondeu
        // por 94 das 139 pendências classificadas — sempre a mesma assinatura, uma tag de cor por
        // caractere, com o LLM devolvendo o original ou corrompendo os marcadores. Não era o modelo
        // errando: era linha que não devia estar aqui.
        //
        // Critério LARGO de propósito (o mesmo do pareamento): precisa alcançar ED_S2/OP_S2/OP2,
        // que a fronteira \b do padrão de nome não pega. Errar para o lado de NÃO traduzir música
        // custa uma linha no idioma original; errar para o outro lado é o que produziu a salada.
        if (detectorKaraoke.podeSerCamadaMusical(evento.estilo(), texto)) {
            return false;
        }
        if (politicaEstiloMusical.estiloIgnorado(evento.estilo())) {
            return false;
        }

        // Blindagem contra karaokê cru (\k, \kf, \ko). Só as tags de timing:
        // a detecção agressiva de pós-template (eEfeitoKaraoke) pegaria também
        // letreiros/títulos com \t e texto curto, que aqui DEVEM ser traduzidos
        // — o caso karaokê pós-template é coberto pela heurística de letreiro
        // animado logo abaixo (que exige repetição).
        if (detectorKaraoke.devePreservarKaraokeOriginal(evento.estilo(), texto)) {
            return false;
        }

        // 1. Blindagem Contra Lixo Vetorial Absoluto (modo de desenho \p1, \p2, ... do Aegisub)
        if (protecaoAss.temDesenhoVetorial(texto)) {
            return false;
        }

        String textoLimpo = protecaoAss.textoVisivel(texto);
        if (textoLimpo.isEmpty()) {
            return false;
        }
        long repeticoes = frequenciaTextoLimpo.getOrDefault(textoLimpo, 1L);
        if (protecaoAss.deveBloquearLinhaAntesDoLlm(evento.estilo(), texto, repeticoes)) {
            log.debug("Bloqueando evento de typesetting de alto risco antes do LLM. Repetido {}x. Estilo: {} Texto: {}",
                repeticoes, evento.estilo(), textoLimpo);
            return false;
        }

        // 2. Blindagem Contra Typesetting Dinâmico (letreiros/títulos animados quadro a quadro):
        // tag de efeito pesada + pouco texto visível + o mesmo texto repetido muitas vezes no
        // arquivo. A repetição é o sinal decisivo: sem ela, uma fala isolada com efeito visual
        // (ex.: a camada de contorno de uma legenda de encerramento) seria descartada por engano.
        boolean temTagDeAnimacao = texto.contains("\\clip") || texto.contains("\\move")
            || texto.contains("\\pos") || texto.contains("\\fad") || texto.contains("\\t(");
        if (temTagDeAnimacao && texto.length() > 40 && textoLimpo.length() * 3 < texto.length()) {
            if (repeticoes >= LIMIAR_REPETICAO_LETREIRO) {
                log.debug("Bloqueando evento suspeito de letreiro animado (repetido {}x). Estilo: {} Texto: {}",
                    repeticoes, evento.estilo(), textoLimpo);
                return false;
            }
        }

        return mascarador.contemTextoTraduzivel(texto);
    }
}
