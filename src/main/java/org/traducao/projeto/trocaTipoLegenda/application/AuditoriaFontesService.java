package org.traducao.projeto.trocaTipoLegenda.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.trocaTipoLegenda.domain.AuditoriaFonteInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AuditoriaFontesService {

    // Mapeamento de fontes vietnamitas/ANSI problemáticas para Arial como padrão seguro.
    private static final Map<String, String> FONTES_PROBLEMATICAS = Map.of(
        ".VnBook-Antiqua", "Arial",
        ".VnArial", "Arial",
        ".VnTimes", "Arial"
    );

    public record ResultadoSubstituicaoCabecalho(String cabecalho, int substituicoes) {}

    public List<AuditoriaFonteInfo> analisarCabecalho(String cabecalho) {
        if (cabecalho == null || cabecalho.isBlank()) {
            return Collections.emptyList();
        }

        String[] linhas = cabecalho.split("\r\n|\n", -1);
        int indiceStyles = -1;
        for (int i = 0; i < linhas.length; i++) {
            if (ehSecaoEstilos(linhas[i].trim())) {
                indiceStyles = i;
                break;
            }
        }

        // Se não houver a seção [V4+ Styles], não há estilos para analisar
        if (indiceStyles < 0) {
            return Collections.emptyList();
        }

        int indiceFormat = -1;
        for (int i = indiceStyles + 1; i < linhas.length; i++) {
            String linha = linhas[i].trim();
            // Se encontrar outra seção antes de achar o Format, encerra a busca
            if (linha.startsWith("[") && linha.endsWith("]")) {
                break;
            }
            if (linha.startsWith("Format:")) {
                indiceFormat = i;
                break;
            }
        }

        if (indiceFormat < 0) {
            return Collections.emptyList();
        }

        // Parseando colunas de formato
        String linhaFormat = linhas[indiceFormat];
        String dadosFormat = linhaFormat.substring(linhaFormat.indexOf(':') + 1).trim();
        List<String> colunas = Arrays.stream(dadosFormat.split(","))
            .map(String::trim)
            .toList();

        int indexName = indiceColuna(colunas, "Name");
        int indexFontname = indiceColuna(colunas, "Fontname");

        if (indexName < 0 || indexFontname < 0) {
            // Formato inválido ou inesperado, sem as colunas obrigatórias
            return Collections.emptyList();
        }

        List<AuditoriaFonteInfo> resultado = new ArrayList<>();
        int numColunas = colunas.size();

        for (int i = indiceFormat + 1; i < linhas.length; i++) {
            String linha = linhas[i].trim();
            // Se começar outra seção, a seção de estilos acabou
            if (linha.startsWith("[") && linha.endsWith("]")) {
                break;
            }
            if (linha.startsWith("Style:")) {
                String dadosEstilo = linha.substring(linha.indexOf(':') + 1);
                if (dadosEstilo.startsWith(" ")) {
                    dadosEstilo = dadosEstilo.substring(1);
                }
                
                // Dividir por vírgula limitando pelo número de colunas
                String[] partes = dadosEstilo.split(",", numColunas);
                if (partes.length >= numColunas) {
                    String nomeEstilo = partes[indexName].trim();
                    String nomeFonte = partes[indexFontname].trim();
                    
                    String fonteSugerida = fonteSugerida(nomeFonte);
                    boolean problematica = !fonteSugerida.equals(nomeFonte);
                    
                    resultado.add(new AuditoriaFonteInfo(nomeEstilo, nomeFonte, fonteSugerida, problematica));
                }
            }
        }

        return resultado;
    }

    public ResultadoSubstituicaoCabecalho substituirFontesProblematicas(String cabecalho) {
        if (cabecalho == null || cabecalho.isBlank()) {
            return new ResultadoSubstituicaoCabecalho(cabecalho, 0);
        }

        String quebraLinha = cabecalho.contains("\r\n") ? "\r\n" : "\n";
        String[] linhas = cabecalho.split("\r\n|\n", -1);
        int indiceStyles = -1;
        for (int i = 0; i < linhas.length; i++) {
            if (ehSecaoEstilos(linhas[i].trim())) {
                indiceStyles = i;
                break;
            }
        }
        if (indiceStyles < 0) {
            return new ResultadoSubstituicaoCabecalho(cabecalho, 0);
        }

        int indiceFormat = -1;
        for (int i = indiceStyles + 1; i < linhas.length; i++) {
            String linha = linhas[i].trim();
            if (linha.startsWith("[") && linha.endsWith("]")) {
                break;
            }
            if (linha.regionMatches(true, 0, "Format:", 0, "Format:".length())) {
                indiceFormat = i;
                break;
            }
        }
        if (indiceFormat < 0) {
            return new ResultadoSubstituicaoCabecalho(cabecalho, 0);
        }

        String linhaFormat = linhas[indiceFormat];
        String dadosFormat = linhaFormat.substring(linhaFormat.indexOf(':') + 1).trim();
        List<String> colunas = Arrays.stream(dadosFormat.split(","))
            .map(String::trim)
            .toList();
        int indexFontname = indiceColuna(colunas, "Fontname");
        if (indexFontname < 0) {
            return new ResultadoSubstituicaoCabecalho(cabecalho, 0);
        }

        int numColunas = colunas.size();
        int substituicoes = 0;
        for (int i = indiceFormat + 1; i < linhas.length; i++) {
            String linhaOriginal = linhas[i];
            String linha = linhaOriginal.trim();
            if (linha.startsWith("[") && linha.endsWith("]")) {
                break;
            }
            if (!linha.regionMatches(true, 0, "Style:", 0, "Style:".length())) {
                continue;
            }

            int indiceDoisPontos = linhaOriginal.indexOf(':');
            if (indiceDoisPontos < 0) {
                continue;
            }
            String prefixo = linhaOriginal.substring(0, indiceDoisPontos + 1);
            String dadosEstilo = linhaOriginal.substring(indiceDoisPontos + 1);
            String espacoInicial = "";
            if (dadosEstilo.startsWith(" ")) {
                espacoInicial = " ";
                dadosEstilo = dadosEstilo.substring(1);
            }

            String[] partes = dadosEstilo.split(",", numColunas);
            if (partes.length < numColunas) {
                continue;
            }

            String fonteAtual = partes[indexFontname].trim();
            String fonteNova = fonteSugerida(fonteAtual);
            if (!fonteNova.equals(fonteAtual)) {
                partes[indexFontname] = fonteNova;
                linhas[i] = prefixo + espacoInicial + String.join(",", partes);
                substituicoes++;
            }
        }

        return new ResultadoSubstituicaoCabecalho(String.join(quebraLinha, linhas), substituicoes);
    }

    public Map<String, String> getFontesProblematicas() {
        return FONTES_PROBLEMATICAS;
    }

    private boolean ehSecaoEstilos(String linha) {
        return linha.equalsIgnoreCase("[V4+ Styles]") || linha.equalsIgnoreCase("[V4 Styles]");
    }

    private int indiceColuna(List<String> colunas, String nome) {
        for (int i = 0; i < colunas.size(); i++) {
            if (colunas.get(i).equalsIgnoreCase(nome)) {
                return i;
            }
        }
        return -1;
    }

    private String fonteSugerida(String nomeFonte) {
        if (nomeFonte == null) {
            return "";
        }
        return FONTES_PROBLEMATICAS.entrySet().stream()
            .filter(e -> e.getKey().equalsIgnoreCase(nomeFonte.trim()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(nomeFonte);
    }
}
