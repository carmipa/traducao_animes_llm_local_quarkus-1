package org.traducao.projeto.contexto.lore.guiltycrown;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: mapa canônico de formas-ruim → grafia oficial de Guilty Crown.
 *
 * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim PT; valor = canônico; frases longas
 * ANTES de palavras curtas (ex.: "Genoma do Vazio" antes de "Vazio"); o enforcer só
 * aplica se o original EN contém o canônico.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
 */
public final class CorrecoesTerminologiaGuiltyCrown {

    private CorrecoesTerminologiaGuiltyCrown() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve o mapa de restauração determinística da obra.
     *
     * <p>INVARIANTES DO DOMÍNIO: ordem LinkedHashMap — multipalavra primeiro.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sempre o mesmo mapa imutável.
     */
    public static Map<String, String> mapa() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Genoma do Vazio", "Void Genome");
        m.put("Genoma Vazio", "Void Genome");
        m.put("Genoma Void", "Void Genome");
        m.put("Empresa Funerária", "Funeral Parlor");
        m.put("Empresa Funeraria", "Funeral Parlor");
        m.put("Vírus do Apocalipse", "Apocalypse Virus");
        m.put("Virus do Apocalipse", "Apocalypse Virus");
        m.put("Natal Perdido", "Lost Christmas");
        m.put("Coroa Culpada", "Guilty Crown");
        m.put("Poder do Rei", "King's Power");
        m.put("Poder dos Reis", "King's Power");
        m.put("Ressonância Genômica", "Genomic Resonance");
        m.put("Ressonancia Genomica", "Genomic Resonance");
        m.put("Gene Norma", "Norma Gene");
        m.put("Funerária", "Funeral Parlor");
        m.put("Funeraria", "Funeral Parlor");
        m.put("Anticorpos", "Anti Bodies");
        m.put("Corpos Anti", "Anti Bodies");
        m.put("Cristalização", "Crystallization");
        m.put("Cristalizacao", "Crystallization");
        m.put("Segunda Mão", "Second Hand");
        m.put("Segunda Mao", "Second Hand");
        m.put("Forte Roppongi", "Roppongi Fort");
        m.put("Endslave", "Endlave");
        m.put("Escravo Final", "Endlave");
        m.put("Coveiro", "Undertaker");
        m.put("Vazios", "Voids");
        m.put("Vazio", "Void");
        // LinkedHashMap: frases longas antes de "Vazio" (Map.copyOf não garante ordem).
        return Collections.unmodifiableMap(m);
    }
}
