<#
.SYNOPSIS
    Caso-controle do pode-compilar.ps1 — prova que o portao decide nos TRES estados.

.DESCRIPTION
    PROPOSITO: `pode-compilar.ps1` e a guarda que impede compilar por cima de um job vivo do
    KRONOS. Guarda exercitada so no caso sao pode estar aprovando por nao enxergar nada, entao
    ela precisa ser VISTA reprovando um caso doente montado a mao (regra 9).

    A CICATRIZ QUE ORIGINOU ESTE ARREIO — 17/08/2026:
        21:37  pode-compilar.ps1 -> [0] PODE COMPILAR ... "log nao e escrito ha 605s"
        21:37  console-web.log   -> [revisao-lore] [Arquivo 14/22 | Fala 3634/5623]
    O portao mandou compilar sobre uma Revisao de Lore de 22 arquivos em andamento. Ele existia
    havia tres dias, tinha o texto certo, a cicatriz certa — e nunca fora exercitado contra um
    job vivo. Verde porque nunca tinha sido testado.

    INVARIANTES: cada cenario fixa UM comportamento e o portao precisa acertar os cinco. As
    mutacoes ja conferidas, que este arreio pegou:
      - tirar console-web.log dos candidatos (o defeito original)  -> 3 cenarios reprovam
      - voltar a medir por LastWriteTime em vez do carimbo         -> 1 cenario reprova

    COMPORTAMENTO EM CASO DE FALHA: sai 1 e lista o cenario, o esperado e o obtido. Exige o
    KRONOS NO AR na porta 8099 — com ele parado o portao responde 0 de saida e os cenarios
    perdem o sentido; nesse caso o arreio avisa e sai 2, que nao e aprovacao.

.EXAMPLE
    pwsh -NoProfile -File .\calibrar-pode-compilar.ps1
#>
[CmdletBinding()]
param()

$raiz = Split-Path -Parent $MyInvocation.MyCommand.Path
$origem = Join-Path $raiz 'pode-compilar.ps1'
$base = Join-Path $env:TEMP 'calib-pode-compilar'
$agora = Get-Date
$falhas = 0

if (-not (Get-NetTCPConnection -LocalPort 8099 -State Listen -ErrorAction SilentlyContinue)) {
    Write-Host ""
    Write-Host ("[2] NAO VERIFICADO — KRONOS parado (porta 8099 livre). O portao responde 0 na " +
        "primeira linha e os cenarios nao chegam a ser exercitados. Suba o KRONOS e rode de " +
        "novo; 'nao consegui olhar' nunca e 'passou'.") -ForegroundColor Yellow
    Write-Host ""
    exit 2
}

<#
.SYNOPSIS
    Monta uma raiz falsa com um log sintetico e confere o codigo de saida do portao.
.DESCRIPTION
    O portao le os logs relativos a PROPRIA pasta ($MyInvocation), entao copiar o script para um
    diretorio temporario com logs plantados isola o cenario sem tocar nos logs de verdade.
#>
function Test-Cenario {
    param(
        [string]$Nome,
        [string]$UltimaLinha,
        [int]$IdadeSegundos,
        [bool]$CriarLogs,
        [int]$Esperado
    )
    $pasta = Join-Path $base ($Nome -replace '\W', '_')
    if (Test-Path $pasta) { Remove-Item $pasta -Recurse -Force }
    New-Item -ItemType Directory $pasta -Force | Out-Null
    Copy-Item $origem (Join-Path $pasta 'pode-compilar.ps1')

    if ($CriarLogs) {
        New-Item -ItemType Directory (Join-Path $pasta 'logs') -Force | Out-Null
        $carimbo = $agora.AddSeconds(-$IdadeSegundos).ToString('yyyy-MM-dd HH:mm:ss.fff')
        Set-Content (Join-Path $pasta 'logs\console-web.log') "$carimbo $UltimaLinha" -Encoding UTF8
    }

    & (Join-Path $pasta 'pode-compilar.ps1') *>&1 | Out-Null
    $obtido = $LASTEXITCODE
    $ok = ($obtido -eq $Esperado)
    if (-not $ok) { $script:falhas++ }
    $rotulo = if ($ok) { 'OK    ' } else { 'FALHOU' }
    $cor = if ($ok) { 'Green' } else { 'Red' }
    Write-Host ("{0}  {1,-42} esperado={2} obtido={3}" -f $rotulo, $Nome, $Esperado, $obtido) -ForegroundColor $cor
}

Write-Host ""
Write-Host "CASO-CONTROLE do pode-compilar.ps1" -ForegroundColor Cyan
Write-Host ""

# DOENTE: ha escrita agora e ela e de job. O portao TEM de barrar.
Test-Cenario -Nome 'DOENTE revisao de lore correndo' `
    -UltimaLinha '[revisao-lore] [Arquivo 3/22 | Fala 100/500] enviada ao LLM' `
    -IdadeSegundos 5 -CriarLogs $true -Esperado 1

# DOENTE por outra tela: o vocabulario nao pode conhecer so a Traducao — foi assim que a 3.2
# passou despercebida.
Test-Cenario -Nome 'DOENTE traducao correndo' `
    -UltimaLinha '[traducao] Lote 12 traduzido' `
    -IdadeSegundos 5 -CriarLogs $true -Esperado 1

# SAO: silencio bem alem do limite. Precisa liberar — guarda que reprova o caso correto ensina
# a desligar o alarme. Este cenario tambem prova que LastWriteTime ficou fora da conta: o
# arquivo e criado AGORA, so o carimbo dentro dele e velho.
Test-Cenario -Nome 'SAO job terminou ha muito' `
    -UltimaLinha '[revisao-lore] [Arquivo 22/22] enviada ao LLM' `
    -IdadeSegundos 600 -CriarLogs $true -Esperado 0

# CEGO: escrita viva que o portao nao reconhece. Tela nova, prefixo novo. Nao entender NAO e
# permissao (regra 23).
Test-Cenario -Nome 'CEGO vocabulario desconhecido' `
    -UltimaLinha '[tela-que-ainda-nao-existe] fazendo algo pesado' `
    -IdadeSegundos 5 -CriarLogs $true -Esperado 2

# CEGO: nao ha log nenhum para olhar.
Test-Cenario -Nome 'CEGO nenhum log para consultar' `
    -UltimaLinha '' -IdadeSegundos 0 -CriarLogs $false -Esperado 2

Write-Host ""
if ($falhas -eq 0) {
    Write-Host "PLACAR: 5/5 — o portao decide nos tres estados." -ForegroundColor Green
    Write-Host ""
    exit 0
}
Write-Host "PLACAR: $falhas cenario(s) FALHARAM — o portao NAO esta confiavel." -ForegroundColor Red
Write-Host ""
exit 1
