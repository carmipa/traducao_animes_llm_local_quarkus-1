"""CONFERIDOR DO ACERVO — le os BYTES depois de uma gravacao, sem perguntar a ferramenta.

    python ferramentas/conferir-acervo.py antes   [saida.json]
    python ferramentas/conferir-acervo.py depois  [saida.json]

`antes` tira o inventario (caminho -> SHA + contagem de linhas). `depois` compara e diz o que
mudou, LINHA A LINHA, com o ESTILO de cada uma.

## Por que existe

O relatorio de uma ferramenta e afirmacao dela sobre si mesma. Uma passada da tela 3.3 pode
dizer "1.086 falas corrigidas" e estar certa no numero e errada no ALVO — e o dano que este
projeto ja pagou tres vezes e sempre o mesmo: linha de MUSICA alterada. Romaji nao se acentua
(`mae` de 前 virou `mãe` em 103 linhas; `ai` de 愛 virou `aí` em 6), e o unico jeito de saber e
olhar o `.ass` gravado.

## A GUARDA DESTE SCRIPT, e o prejuizo dela

A baseline e escolhida pelo **SHA**, nunca pelo nome do backup.

Em 25/08/2026 a primeira versao pegava o ultimo backup por ordem alfabetica. A pasta guarda
backups de varias passadas, e um deles chamava-se `...ass.antes-do-desfazer-20260824_153742` —
snapshot criado ao DESFAZER um dano. Como "a" vem depois de "2", ele saia como "o mais recente",
e a comparacao acusou 6 linhas de `Romanji` alteradas que eram, na verdade, a diferenca entre o
estado DANIFICADO e o ja consertado. Achado inventado por instrumento errado, e uma investigacao
inteira gasta atras dele.

## Comportamento em caso de falha

Arquivo sem baseline localizavel e DECLARADO, nunca ignorado — "nao consegui comparar" e um
resultado diferente de "nao mudou".
"""
import collections
import hashlib
import io
import json
import os
import sys

ACERVO = os.environ.get("KRONOS_ACERVO", "C:/animes")
PASTA_TRADUZIDA = "traducao_ptbr"
PASTA_BACKUP = "backup_revisao_concordancia"

MUSICAL = ("song", "romaji", "romanji", "karaok", "lyric", "opening", "ending", "op -", "ed -")


def arquivosDoAcervo():
    for dp, _, fns in os.walk(ACERVO):
        if os.path.basename(dp) != PASTA_TRADUZIDA:
            continue
        for fn in fns:
            if fn.endswith(".ass"):
                yield os.path.join(dp, fn)


def fichaDe(caminho):
    bruto = open(caminho, "rb").read()
    texto = bruto.decode("utf-8-sig", errors="replace")
    linhas = texto.split("\n")
    return {"sha": hashlib.sha256(bruto).hexdigest(),
            "dialogue": sum(1 for l in linhas if l.startswith("Dialogue:")),
            "comment": sum(1 for l in linhas if l.startswith("Comment:"))}


def antes(saida):
    inv = {p: fichaDe(p) for p in arquivosDoAcervo()}
    io.open(saida, "w", encoding="utf-8").write(json.dumps(inv))
    print("inventario ANTES: %d arquivos -> %s" % (len(inv), saida))


def baselineDe(caminho, shaEsperado):
    """O backup cujo SHA casa com o estado de ANTES. Ver a GUARDA deste script."""
    pasta = os.path.join(os.path.dirname(caminho), PASTA_BACKUP)
    base = os.path.basename(caminho)
    if not os.path.isdir(pasta):
        return None
    for x in os.listdir(pasta):
        if not x.startswith(base):
            continue
        bruto = open(os.path.join(pasta, x), "rb").read()
        if hashlib.sha256(bruto).hexdigest() == shaEsperado:
            return bruto.decode("utf-8-sig", errors="replace").split("\n")
    return None


def depois(entrada):
    inv = json.loads(io.open(entrada, encoding="utf-8").read())
    mudaram, novos = [], []
    for p in arquivosDoAcervo():
        sha = fichaDe(p)["sha"]
        if p not in inv:
            novos.append(p)
        elif inv[p]["sha"] != sha:
            mudaram.append(p)

    porEstilo, porObra, tipos = collections.Counter(), collections.Counter(), collections.Counter()
    alteradas, contagemQuebrada, semBaseline, suspeitas, amostra = 0, [], [], [], []

    for p in mudaram:
        obra = os.path.basename(os.path.dirname(os.path.dirname(p)))
        novo = open(p, "rb").read().decode("utf-8-sig", errors="replace").split("\n")
        ficha = fichaDe(p)
        if ficha["dialogue"] != inv[p]["dialogue"] or ficha["comment"] != inv[p]["comment"]:
            contagemQuebrada.append((os.path.basename(p)[:44], inv[p], ficha))
        velho = baselineDe(p, inv[p]["sha"])
        if velho is None:
            semBaseline.append(os.path.basename(p)[:44])
            continue
        if len(velho) != len(novo):
            contagemQuebrada.append((os.path.basename(p)[:44], "linhas", len(velho), len(novo)))
        for lv, ln in zip(velho, novo):
            if lv == ln:
                continue
            alteradas += 1
            campos = ln.split(",", 9)
            estilo = campos[3] if len(campos) >= 10 else "?"
            tipo = ln.split(":")[0]
            porEstilo[estilo] += 1
            porObra[obra[:32]] += 1
            tipos[tipo] += 1
            if any(k in estilo.lower() for k in MUSICAL) or tipo == "Comment":
                suspeitas.append((obra[:22], tipo, estilo,
                                  lv.split(",", 9)[-1][:70], ln.split(",", 9)[-1][:70]))
            if len(amostra) < 12:
                amostra.append((obra[:20], estilo[:14],
                                lv.split(",", 9)[-1][:72], ln.split(",", 9)[-1][:72]))

    print("arquivos ANTES ............ %d" % len(inv))
    print("arquivos que MUDARAM ...... %d" % len(mudaram))
    print("arquivos NOVOS ............ %d" % len(novos))
    print("linhas ALTERADAS .......... %d" % alteradas)
    print("TIPOS de linha alterada ... %s" % dict(tipos))
    print("SEM BASELINE (declarado) .. %d %s" % (len(semBaseline), semBaseline[:4]))
    print("\nCONTAGEM DE LINHAS: %s"
          % ("OK — nenhum arquivo ganhou ou perdeu linha" if not contagemQuebrada
             else contagemQuebrada[:6]))

    print("\n=== ESTILO DE CADA LINHA ALTERADA ===")
    for e, n in porEstilo.most_common():
        print("   %-26s %5d" % (e[:26], n))
    print("\n=== POR OBRA ===")
    for o, n in porObra.most_common():
        print("   %-34s %5d" % (o, n))

    print("\n=== MUSICA OU COMENTARIO ALTERADO (tem de ser ZERO) ===")
    if not suspeitas:
        print("   ZERO — nenhuma linha de musica e nenhuma linha Comment foi tocada")
    for s in suspeitas[:14]:
        print("   [%s] %s <%s>\n       - %s\n       + %s" % s)

    print("\n=== AMOSTRA (antes -> depois) ===")
    for obra, estilo, v, n in amostra:
        print("   [%-20s] <%-14s>\n       - %s\n       + %s" % (obra, estilo, v, n))

    return 1 if (suspeitas or contagemQuebrada or semBaseline) else 0


if __name__ == "__main__":
    if len(sys.argv) < 2 or sys.argv[1] not in ("antes", "depois"):
        print(__doc__)
        sys.exit(2)
    destino = sys.argv[2] if len(sys.argv) > 2 else "inventario-acervo.json"
    if sys.argv[1] == "antes":
        antes(destino)
        sys.exit(0)
    sys.exit(depois(destino))
