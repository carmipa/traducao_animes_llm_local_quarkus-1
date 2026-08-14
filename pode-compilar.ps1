<#
.SYNOPSIS
    Responde se e seguro compilar AGORA — ou se ha job do KRONOS que a compilacao mataria.

.DESCRIPTION
    PROPOSITO: em modo dev, o Quarkus recarrega sozinho quando um .class muda. Isso derruba a
    aplicacao em fracao de segundo e MATA qualquer traducao em andamento, sem pedir confirmacao
    e sem salvar o arquivo em curso — o cache so e gravado ao fim de cada arquivo.

    A CICATRIZ, medida em 14/08/2026:
        09:59:06  Restarting quarkus due to changes in TraducaoController.class.
        09:59:06  Traducao do lote 281 interrompida (cancelamento cooperativo)
    O episodio "Memories - S01E02 Stink Bomb" morreu no lote 281 de 368 porque um `gradlew test`
    foi disparado enquanto ele traduzia. Nao houve erro visivel para quem rodou o teste: a suite
    passou verde, e a perda so apareceu quando alguem foi procurar o .ass que nao existia.

    TRES ESTADOS (regra 23 — guarda tem tres, nunca dois):
        0 = PODE compilar (KRONOS parado, ou vivo e ocioso)
        1 = NAO COMPILE (job em andamento — compilar mata)
        2 = NAO VERIFICADO (nao consegui olhar; NAO e aprovacao)

.PARAMETER OciosoSegundos
    Quanto tempo sem escrita no log de execucao conta como ocioso. Padrao 90s: um lote leva
    ~1-2s, entao 90s sem nada e folga larga.

.EXAMPLE
    pwsh -NoProfile -File .\pode-compilar.ps1
    if ($LASTEXITCODE -eq 0) { .\gradlew test }
#>
[CmdletBinding()]
param(
    [int]$OciosoSegundos = 90
)

$ErrorActionPreference = 'Continue'
$raiz = Split-Path -Parent $MyInvocation.MyCommand.Path

function Sair($codigo, $texto, $cor) {
    Write-Host ""
    Write-Host $texto -ForegroundColor $cor
    Write-Host ""
    exit $codigo
}

# --- 1. O KRONOS esta no ar? Parado, nao ha o que matar. -------------------------------------
$conexao = Get-NetTCPConnection -LocalPort 8099 -State Listen -ErrorAction SilentlyContinue
if (-not $conexao) {
    Sair 0 "[0] PODE COMPILAR — KRONOS parado (porta 8099 livre)." 'Green'
}
$pidKronos = $conexao.OwningProcess

# --- 2. Ha log de execucao para consultar? Sem ele, NAO VERIFICADO. ---------------------------
$pastaLogs = Join-Path $raiz 'logs\execucoes'
if (-not (Test-Path $pastaLogs)) {
    Sair 2 ("[2] NAO VERIFICADO — KRONOS vivo (PID $pidKronos) mas $pastaLogs nao existe. " +
        "Nao consigo saber se ha job rodando, e 'nao olhei' nunca e 'pode'.") 'Yellow'
}
$log = Get-ChildItem $pastaLogs -Filter '*.log' -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $log) {
    Sair 2 ("[2] NAO VERIFICADO — KRONOS vivo (PID $pidKronos) e nenhum log de execucao em " +
        "$pastaLogs.") 'Yellow'
}

# --- 3. O log esta sendo escrito AGORA? -------------------------------------------------------
# O log fica aberto pelo processo, entao copia-se antes de ler. Se a copia falhar, e NAO
# VERIFICADO — nunca "pode".
$copia = Join-Path $env:TEMP 'kronos-pode-compilar.log'
try {
    Copy-Item $log.FullName $copia -Force -ErrorAction Stop
} catch {
    Sair 2 ("[2] NAO VERIFICADO — nao consegui ler $($log.Name): $($_.Exception.Message)") 'Yellow'
}

$paradoHa = [int]((Get-Date) - $log.LastWriteTime).TotalSeconds
$ultimas = [System.IO.File]::ReadAllLines($copia) |
    Where-Object { $_ -notmatch 'access-log' } | Select-Object -Last 40

# Marca de pipeline vivo: a thread da fila de execucao so aparece quando ha job.
$sinaisDeJob = $ultimas | Where-Object { $_ -match 'pipeline-fila-execucao|Enviando lote|Lote \d+ traduzido' }

if ($sinaisDeJob -and $paradoHa -lt $OciosoSegundos) {
    $amostra = ($sinaisDeJob | Select-Object -Last 1) -replace '^\S+ \S+ \w+\s+\[[^\]]+\] \([^)]+\) ', ''
    if ($amostra.Length -gt 110) { $amostra = $amostra.Substring(0, 110) + '...' }
    Sair 1 ("[1] NAO COMPILE — ha job do KRONOS em andamento (PID $pidKronos, ultima " +
        "atividade ha ${paradoHa}s):`n      $amostra`n" +
        "`n    Compilar dispara live reload do Quarkus e MATA o arquivo em curso. Em 14/08/2026 " +
        "isso custou o episodio Stink Bomb no lote 281 de 368.`n" +
        "    Espere o job terminar, ou pare o KRONOS de proposito antes de compilar.") 'Red'
}

if ($sinaisDeJob) {
    Sair 0 ("[0] PODE COMPILAR — KRONOS vivo (PID $pidKronos), mas o log nao e escrito ha " +
        "${paradoHa}s (limite ${OciosoSegundos}s). Ultimo job ja terminou.") 'Green'
}

Sair 0 ("[0] PODE COMPILAR — KRONOS vivo (PID $pidKronos) e sem sinal de job nas ultimas " +
    "40 linhas do log.") 'Green'
