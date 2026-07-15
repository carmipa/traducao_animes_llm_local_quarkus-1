package org.traducao.projeto.traducao;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utilitarios de inicializacao compartilhados entre modos CLI.
 * O Quarkus e o container principal; nao ha {@code SpringApplication.run} aqui.
 */
public final class Application {

    private static final String ARG_ENTRADA = "--tradutor.diretorio-entrada=";

    private Application() {
    }

    /**
     * Reconfigura System.out/System.err para UTF-8 explicito no Windows.
     */
    public static void forcarSaidaUtf8() {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
    }

    /**
     * Se a pasta de entrada nao veio na linha de comando, prepara defaults para modo WEB.
     */
    static String[] prepararArgumentosComPastasDoConsole(String[] args) {
        boolean temEntradaArg = false;
        for (String arg : args) {
            if (arg.startsWith(ARG_ENTRADA)) {
                temEntradaArg = true;
                break;
            }
        }

        if (temEntradaArg) {
            if (temValorNaoVazio(args, ARG_ENTRADA)) {
                return args;
            }
            return null;
        }

        List<String> lista = new ArrayList<>(Arrays.asList(args));
        lista.add("--app.modo=WEB");
        lista.add(ARG_ENTRADA + "cache");
        return lista.toArray(String[]::new);
    }

    private static boolean temValorNaoVazio(String[] args, String prefixo) {
        for (String arg : args) {
            if (arg.startsWith(prefixo) && arg.length() > prefixo.length()) {
                return true;
            }
        }
        return false;
    }
}
