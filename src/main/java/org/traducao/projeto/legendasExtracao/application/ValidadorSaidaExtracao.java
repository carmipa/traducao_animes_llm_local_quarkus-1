package org.traducao.projeto.legendasExtracao.application;

import org.traducao.projeto.legendasExtracao.domain.ExtratorException;
import org.traducao.projeto.legendasExtracao.domain.FormatoLegenda;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * PROPÓSITO DE NEGÓCIO: Garante que o arquivo recém-extraído é uma legenda de
 * verdade no formato pedido — não um arquivo vazio nem uma faixa de outro tipo
 * gravada por engano. É a blindagem que separa "extração concluída" de "arquivo
 * criado", exigida para nunca entregar lixo ao módulo de tradução.
 *
 * <p>INVARIANTES DO DOMÍNIO: um arquivo só é válido se (1) existe, (2) tem
 * tamanho maior que zero e (3) seu conteúdo bate com a assinatura do formato:
 * ASS contém marcador de seção/{@code Dialogue:}; SRT contém a seta de
 * timestamp {@code -->}; PGS começa com o magic {@code PG} (0x50 0x47). A
 * verificação lê apenas o início do arquivo (amostra), nunca o carrega inteiro.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link ExtratorException} com a razão
 * específica (inexistente / vazio / formato divergente / erro de leitura). Não
 * remove o arquivo — o cleanup do parcial é responsabilidade do use case.
 */
public final class ValidadorSaidaExtracao {

    private static final int TAMANHO_AMOSTRA = 8192;

    private ValidadorSaidaExtracao() {
    }

    public static void validar(Path arquivo, FormatoLegenda formato) {
        if (arquivo == null || !Files.exists(arquivo)) {
            throw new ExtratorException("Extração não gerou o arquivo esperado: " + arquivo);
        }

        long tamanho;
        try {
            tamanho = Files.size(arquivo);
        } catch (IOException e) {
            throw new ExtratorException("Falha ao ler o tamanho do arquivo extraído: " + arquivo, e);
        }
        if (tamanho == 0) {
            throw new ExtratorException("Arquivo extraído está vazio (0 bytes): " + arquivo);
        }

        byte[] amostra = lerAmostra(arquivo);
        if (!correspondeAoFormato(amostra, formato)) {
            throw new ExtratorException("Conteúdo extraído não corresponde ao formato "
                + formato.name() + ": " + arquivo);
        }
    }

    private static byte[] lerAmostra(Path arquivo) {
        try (InputStream in = Files.newInputStream(arquivo)) {
            return in.readNBytes(TAMANHO_AMOSTRA);
        } catch (IOException e) {
            throw new ExtratorException("Falha ao ler o conteúdo do arquivo extraído: " + arquivo, e);
        }
    }

    private static boolean correspondeAoFormato(byte[] amostra, FormatoLegenda formato) {
        return switch (formato) {
            case ASS -> {
                String txt = new String(amostra, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                yield txt.contains("[script info]") || txt.contains("dialogue:") || txt.contains("[v4");
            }
            case SRT -> new String(amostra, StandardCharsets.UTF_8).contains("-->");
            // PGS (.sup): fluxo binário; cada segmento começa com o magic "PG".
            case PGS -> amostra.length >= 2 && amostra[0] == 'P' && amostra[1] == 'G';
        };
    }
}
