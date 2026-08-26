"""MUTADOR — muta uma condicao do codigo e diz QUAL teste reprova.

    python ferramentas/mutar.py mutacoes.json

Uma guarda que nunca foi vista REPROVANDO um caso doente pode estar aprovando por cegueira. Este
script automatiza a prova: troca um trecho do fonte, roda os testes, e exige que algum reprove.
Mutacao que nao derruba nada denuncia condicao morta ou nao coberta.

## AS TRES GUARDAS DESTE SCRIPT, e o prejuizo de cada uma

Em 25/08/2026 tres versoes deste mutador, escritas na hora, deram resultado errado com cara de
certo. As correcoes viraram guardas aqui para nao serem redescobertas:

1. `--rerun-tasks` SEMPRE.
   Sem ele o Gradle considera a tarefa atualizada e o XML da rodada ANTERIOR fica no lugar. Cinco
   mutacoes rodaram assim e devolveram "nenhum teste reprovou" nas cinco — inclusive a que
   DESLIGA a remocao do caractere invisivel. Cinco de cinco sem reprovar e sinal de instrumento
   cego, nunca de codigo bom.

2. O veredito sai do XML, nunca do CODIGO DE SAIDA.
   `rc=1` significa "teste reprovou" E "o cmd nao achou o executavel". Sao dois estados com o
   mesmo sinal — exatamente o defeito que este projeto persegue no codigo de producao, cometido
   dentro do instrumento que deveria pega-lo.

3. O caminho do wrapper e ABSOLUTO.
   Este shell roda com NoDefaultCurrentDirectoryInExePath: o cmd nao procura na pasta atual, e
   nem `gradlew.bat` nem `./gradlew.bat` resolvem. Falha silenciosa com rc=1.

E a quarta, que nao e deste script mas mora na mesma familia: o XML e APAGADO antes de cada
rodada, para "sem XML" significar "nao rodou" e nunca "rodou e nao reprovou".

## O arquivo de mutacoes

    {
      "alvos": ["*MinhaClasseTest*", "*OutraTest*"],
      "propriedades": {"kronos.medicao": "true"},
      "mutacoes": [
        {"arquivo": "src/main/java/.../Corretor.java",
         "titulo":  "desligar a guarda de maiuscula",
         "velho":   "Set<String> intocaveis = palavrasComMaiuscula(texto);",
         "novo":    "Set<String> intocaveis = Set.of();"}
      ]
    }

`velho` tem de casar EXATAMENTE UMA vez; zero ou duas viram NAO VERIFICADO e nao passam por
mutacao aplicada.

## Comportamento em caso de falha

O fonte e restaurado no `finally`, sempre, e a suite e rodada de novo ao fim para provar que
voltou ao verde. Se o Gradle nao rodar, isso e dito — nunca confundido com "nada reprovou".
"""
import glob
import io
import json
import os
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

XML = "build/test-results/test/*.xml"


def _wrapper():
    """Caminho ABSOLUTO do wrapper. Ver guarda 3."""
    return os.path.join(os.getcwd(), "gradlew.bat" if os.name == "nt" else "gradlew")


def rodar(alvos, propriedades=None):
    """Roda os testes e devolve a lista de casos que REPROVARAM, lida do XML.

    Devolve uma lista com uma linha de erro quando o Gradle nao rodou, ou quando NENHUM caso
    chegou a EXECUTAR — os tres estados sao diferentes e nunca podem sair iguais.
    """
    for x in glob.glob(XML):
        os.remove(x)
    filtro = " ".join('--tests "%s"' % a for a in alvos)
    # GUARDA 5: as propriedades do plano. Sem elas, um caso atras de
    # `@EnabledIfSystemProperty` e PULADO — e pulado nao e aprovado. Em 25/08/2026 uma mutacao
    # deste mesmo mutador devolveu "nenhum teste reprovou" porque o alvo estava atras de
    # `-Dkronos.medicao=true` e nunca chegou a rodar.
    ds = " ".join('"-D%s=%s"' % (k, v) for k, v in (propriedades or {}).items())
    # GUARDA 1: --rerun-tasks sempre.
    cmd = '"%s" test --rerun-tasks %s %s' % (_wrapper(), ds, filtro)
    r = subprocess.run(cmd, shell=True, capture_output=True, text=True, errors="replace")
    # GUARDA 2: o veredito vem do XML; o rc so serve para detectar que nem rodou.
    saida = (r.stdout or "") + (r.stderr or "")
    if "BUILD" not in saida:
        return ["!! O GRADLE NAO RODOU: " + saida.strip()[-300:]]
    arquivos = glob.glob(XML)
    if not arquivos:
        return ["!! SEM XML — nada foi medido"]
    ruins = []
    executados = 0
    for x in arquivos:
        for caso in ET.parse(x).getroot().iter("testcase"):
            if caso.find("skipped") is not None:
                continue
            executados += 1
            if caso.find("failure") is not None or caso.find("error") is not None:
                ruins.append(caso.get("name"))
    # GUARDA 6: PULADO NAO E APROVADO. Se nada executou, isso e NAO VERIFICADO — nunca
    # "a mutacao nao derrubou nada".
    if executados == 0:
        return ["!! NENHUM CASO EXECUTOU (todos pulados) — NAO VERIFICADO, e nao aprovacao"]
    return ruins


def main(caminhoDoPlano):
    plano = json.loads(io.open(caminhoDoPlano, encoding="utf-8").read())
    alvos = plano["alvos"]
    props = plano.get("propriedades", {})
    mortas = []

    print("=== VERDE DE PARTIDA (sem mutacao) ===")
    partida = rodar(alvos, props)
    if partida:
        print("    A SUITE JA ESTA VERMELHA: %s" % partida)
        print("    Mutacao sobre suite vermelha nao mede nada. Abortando.")
        return 2
    print("    ok\n")

    for m in plano["mutacoes"]:
        arquivo, titulo = m["arquivo"], m["titulo"]
        bak = arquivo + ".bak"
        shutil.copy(arquivo, bak)
        try:
            base = io.open(bak, encoding="utf-8").read()
            if base.count(m["velho"]) != 1:
                print("### %s\n    NAO APLICADA: o alvo aparece %d vez(es) — NAO VERIFICADO\n"
                      % (titulo, base.count(m["velho"])))
                continue
            io.open(arquivo, "w", encoding="utf-8", newline="\n").write(
                base.replace(m["velho"], m["novo"]))
            ruins = rodar(alvos, props)
            print("### %s" % titulo)
            if not ruins:
                mortas.append(titulo)
                print("    !!! NENHUM TESTE REPROVOU — condicao morta ou nao coberta")
            for x in ruins:
                print("    reprovou: %s" % x)
            print()
        finally:
            shutil.copy(bak, arquivo)
            os.remove(bak)

    print("=== RESTAURADO — conferindo o verde de volta ===")
    volta = rodar(alvos, props)
    print("    %s" % (volta if volta else "VERDE"))
    if mortas:
        print("\nMUTACOES QUE NAO DERRUBARAM NADA (%d): %s" % (len(mortas), mortas))
        print("Cada uma e uma condicao que pode estar morta, ou um caso de teste que falta.")
        return 1
    return 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(__doc__)
        sys.exit(2)
    sys.exit(main(sys.argv[1]))
