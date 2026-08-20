package org.traducao.projeto.telemetria;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducaoKaraoke.domain.DesfechoKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.ResultadoTraducaoKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.TelemetriaKaraoke;
import org.traducao.projeto.traducaoKaraoke.infrastructure.TelemetriaKaraokeDataset;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: garante que TODA medição que a fatia de karaokê calcula chega ao dataset
 * público — no JSONL e na tabela CSV. É a ordem "adiciona tudo à telemetria para gerar dataset"
 * transformada em mecanismo, para deixar de depender de alguém lembrar.
 *
 * <h2>O prejuízo que originou, medido</h2>
 * Em 2026-08-20 o acervo estruturado tinha <b>2.266 execuções e zero de karaokê</b>. A fatia
 * media doze contadores por arquivo, três estados de dicionário, falha nominal por arquivo e o
 * cache descartado por proveniência divergente — e nada disso saía do manifesto em
 * {@code logs/traducao-karaoke/manifestos/}, que o publicador nunca leu. O que chegava ao
 * repositório público era UMA linha genérica de sete campos por execução inteira.
 *
 * <h2>Por que reflexão sobre os records, e não lista escrita à mão</h2>
 * Lista à mão é documentação: envelhece calada. Um contador novo em
 * {@link ResultadoTraducaoKaraoke} entraria no manifesto e sumiria do dataset sem uma linha
 * vermelha em lugar nenhum — que é exatamente a forma do defeito que esta catraca fecha. Com
 * reflexão, o componente novo REPROVA o build até alguém decidir, por escrito, se ele vai ao
 * dataset ou entra na exclusão nominal.
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem nomeia o campo órfão e o lado em que ele falta. Nenhum teste aqui passa por
 * ausência de dado: os conjuntos são carregados de records reais, e record sem componente é
 * impossível em Java.
 */
class CatracaTelemetriaKaraokeCompletaTest {

    /**
     * O único campo do resultado que NÃO vai ao dataset, e o motivo — decisão, não esquecimento.
     *
     * <p>{@code arquivoDestino} é caminho ABSOLUTO da máquina do operador
     * ({@code C:\animes\86\...}). O dataset é público e a sanitização dele proíbe caminho de
     * máquina; o nome do arquivo já viaja em {@code arquivo}, que é o que identifica a legenda.
     */
    private static final Set<String> EXCLUIDOS_POR_DECISAO = Set.of("arquivoDestino");

    /**
     * Como cada campo do DESFECHO da execução aparece na linha do dataset.
     *
     * <p>Os nomes mudam de propósito: na linha existem DOIS status — o do arquivo
     * ({@code desfechoArquivo}) e o da execução ({@code statusExecucao}) —, e chamar os dois de
     * {@code status} tornaria a tabela ambígua justamente na coluna que se lê primeiro.
     * {@code falhas} é a lista da execução inteira e vira duas colunas por LINHA:
     * {@code desfechoArquivo=FALHOU} mais {@code motivoFalha}.
     */
    private static final Map<String, List<String>> DESFECHO_NO_DATASET = Map.of(
        "status", List.of("statusExecucao"),
        "motivo", List.of("motivoExecucao"),
        "falhas", List.of("desfechoArquivo", "motivoFalha"),
        "cacheIgnorado", List.of("cacheIgnorado"),
        "estadoDicionario", List.of("estadoDicionario"));

    /**
     * A lista de avisos não vira coluna: vira CONTAGEM na tabela principal e uma linha por aviso
     * na tabela tidy, exatamente como o diálogo faz com {@code errosOcorridos}. Espremer N avisos
     * numa célula produz campo que planilha nenhuma abre e {@code group by} nenhum agrega.
     */
    private static final Map<String, String> COLUNA_DERIVADA = Map.of("avisos", "quantidadeAvisos");

    @Test
    @DisplayName("todo contador que o karaokê mede por arquivo chega ao dataset")
    void todoContadorDoResultadoChegaAoDataset() {
        Set<String> naLinha = componentes(TelemetriaKaraoke.class);
        List<String> orfaos = new ArrayList<>();
        for (String campo : componentes(ResultadoTraducaoKaraoke.class)) {
            if (EXCLUIDOS_POR_DECISAO.contains(campo) || naLinha.contains(campo)) {
                continue;
            }
            orfaos.add(campo);
        }
        assertTrue(orfaos.isEmpty(),
            "campo(s) de ResultadoTraducaoKaraoke que o dataset publico NAO carrega: " + orfaos
                + " — acrescente em TelemetriaKaraoke ou declare em EXCLUIDOS_POR_DECISAO com o motivo");
    }

    @Test
    @DisplayName("todo campo do desfecho da execução chega ao dataset, com nome declarado")
    void todoCampoDoDesfechoChegaAoDataset() {
        Set<String> naLinha = componentes(TelemetriaKaraoke.class);
        Set<String> doDesfecho = componentes(DesfechoKaraoke.class);

        assertEquals(doDesfecho, DESFECHO_NO_DATASET.keySet(),
            "DesfechoKaraoke mudou de forma: o mapa DESFECHO_NO_DATASET precisa acompanhar, "
                + "senão um campo novo do desfecho sai do dataset sem ninguem notar");

        List<String> ausentes = new ArrayList<>();
        for (List<String> destinos : DESFECHO_NO_DATASET.values()) {
            for (String destino : destinos) {
                if (!naLinha.contains(destino)) {
                    ausentes.add(destino);
                }
            }
        }
        assertTrue(ausentes.isEmpty(),
            "coluna(s) declarada(s) no mapa que nao existem em TelemetriaKaraoke: " + ausentes);
    }

    @Test
    @DisplayName("todo campo da linha do dataset tem coluna no CSV do karaokê")
    void todoCampoDoDatasetTemColunaNoCsv() {
        Set<String> colunas = new LinkedHashSet<>(TelemetriaDatasetService.COLUNAS_KARAOKE);
        List<String> semColuna = new ArrayList<>();
        for (String campo : componentes(TelemetriaKaraoke.class)) {
            String esperada = COLUNA_DERIVADA.getOrDefault(campo, campo);
            if (!colunas.contains(esperada)) {
                semColuna.add(campo + " (esperava coluna '" + esperada + "')");
            }
        }
        assertTrue(semColuna.isEmpty(),
            "campo(s) do JSONL sem coluna no kronos-karaoke.csv — chegam ao JSON e somem da "
                + "tabela que a maioria abre: " + semColuna);
    }

    @Test
    @DisplayName("nenhuma coluna do CSV é inventada — toda coluna vem de um campo real")
    void nenhumaColunaDoCsvEInventada() {
        Set<String> campos = componentes(TelemetriaKaraoke.class);
        Set<String> derivadas = new LinkedHashSet<>(COLUNA_DERIVADA.values());
        List<String> semCampo = new ArrayList<>();
        for (String coluna : TelemetriaDatasetService.COLUNAS_KARAOKE) {
            if (!campos.contains(coluna) && !derivadas.contains(coluna)) {
                semCampo.add(coluna);
            }
        }
        assertTrue(semCampo.isEmpty(),
            "coluna(s) do CSV sem campo correspondente — sairiam SEMPRE vazias, que e pior que "
                + "nao existir porque parecem dado ausente: " + semCampo);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o nome do acervo é combinado entre dois módulos que, de propósito,
     * não se importam — a fatia escreve, o publicador lê. Combinado só no comentário, ele quebra
     * na primeira renomeação e o karaokê some do dataset EM SILÊNCIO, que é o defeito original.
     */
    @Test
    @DisplayName("escritora e publicador concordam sobre o nome do acervo local")
    void nomeDoAcervoLocalEIgualNosDoisLados() {
        assertEquals(TelemetriaKaraokeDataset.NOME_ARQUIVO,
            TelemetriaDatasetService.NOME_ARQUIVO_KARAOKE_LOCAL,
            "a fatia escreve num arquivo e o publicador le outro: o karaoke sairia do dataset "
                + "sem nenhuma mensagem de erro");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a chave de deduplicação do acervo precisa existir como campo, senão
     * {@code indexar} descarta toda linha do karaokê por "sem campos-chave" — e descartar em
     * silêncio é aprovar por cegueira.
     */
    @Test
    @DisplayName("a chave do acervo (registradoEm + arquivo) existe na linha")
    void chaveDoAcervoExisteNaLinha() {
        Set<String> campos = componentes(TelemetriaKaraoke.class);
        assertTrue(campos.contains("registradoEm") && campos.contains("arquivo"),
            "sem registradoEm+arquivo o publicador descarta cada linha do karaoke calado: " + campos);
    }

    private static Set<String> componentes(Class<?> record) {
        RecordComponent[] cs = record.getRecordComponents();
        assertTrue(cs != null && cs.length > 0,
            record.getSimpleName() + " deixou de ser record: esta catraca ficaria cega");
        return Arrays.stream(cs).map(RecordComponent::getName)
            .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }
}
