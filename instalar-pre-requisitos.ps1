<#
.SYNOPSIS
    Instala e CONFERE os pré-requisitos de sistema do KRONOS.

.DESCRIPTION
    O KRONOS não embarca ferramenta externa: usa o que está instalado no sistema, como já fazia
    com ffmpeg e mkvmerge. Este script deixa isso reproduzível — sem ele, "funciona na máquina do
    Paulo" é a única documentação que existe.

    Idempotente: o que já está instalado é conferido, não reinstalado.

    TRÊS ESTADOS no relatório final, nunca dois (regra 23):
        OK          instalado e RESPONDEU a uma chamada real
        FALTA       ausente, com o comando exato para instalar
        NAO-VERIF   presente mas não respondeu como esperado — que NÃO é aprovação

.NOTES
    Exige PowerShell como Administrador apenas para a parte do Chocolatey.
    Os dicionários vão para C:\Hunspell\, que já está no search path padrão do hunspell.
#>
[CmdletBinding()]
param(
    # Só confere, não instala nada.
    [switch]$SomenteConferir,
    # Idiomas de dicionário. pt_BR é o alvo da tradução; en_US separa resíduo de inglês de erro
    # real; de_DE existe porque anime usa termo alemão à beça (Evangelion: Nerv, Seele, Gehirn).
    [string[]]$Idiomas = @('pt_BR', 'en_US', 'de_DE')
)

$ErrorActionPreference = 'Continue'
$pastaDicionarios = 'C:\Hunspell'

# Mapa idioma -> nome do arquivo no repositório do LibreOffice. O alemão NÃO segue o padrão dos
# outros: o pacote oficial chama-se de_DE_frami, e pedir de_DE devolve 404 — descoberto na marra.
$origemDicionarios = @{
    'pt_BR' = @{ pasta = 'pt_BR'; arquivo = 'pt_BR' }
    'en_US' = @{ pasta = 'en';    arquivo = 'en_US' }
    'de_DE' = @{ pasta = 'de';    arquivo = 'de_DE_frami' }
    'es_ES' = @{ pasta = 'es';    arquivo = 'es_ES' }
    'fr_FR' = @{ pasta = 'fr_FR'; arquivo = 'fr' }
    'it_IT' = @{ pasta = 'it_IT'; arquivo = 'it_IT' }
}

$resultado = [System.Collections.Generic.List[object]]::new()
function Registrar($item, $estado, $detalhe) {
    $resultado.Add([pscustomobject]@{ Item = $item; Estado = $estado; Detalhe = $detalhe })
}

function TemComando($nome) {
    return $null -ne (Get-Command $nome -ErrorAction SilentlyContinue)
}

Write-Host "`n=== PRE-REQUISITOS DO KRONOS ===`n" -ForegroundColor Cyan

# ---------------------------------------------------------------------------------------------
# 1. Chocolatey — como as outras ferramentas chegam
# ---------------------------------------------------------------------------------------------
if (TemComando 'choco') {
    Registrar 'chocolatey' 'OK' (Get-Command choco).Source
} else {
    Registrar 'chocolatey' 'FALTA' 'https://chocolatey.org/install'
}

# ---------------------------------------------------------------------------------------------
# 2. Ferramentas de mídia — já eram pré-requisito, agora estão declaradas
# ---------------------------------------------------------------------------------------------
foreach ($f in @(
    @{ nome = 'ffmpeg';   pacote = 'ffmpeg';     teste = { & ffmpeg -version 2>&1 | Select-Object -First 1 } },
    @{ nome = 'ffprobe';  pacote = 'ffmpeg';     teste = { & ffprobe -version 2>&1 | Select-Object -First 1 } },
    @{ nome = 'mkvmerge'; pacote = 'mkvtoolnix'; teste = { & mkvmerge --version 2>&1 | Select-Object -First 1 } }
)) {
    if (TemComando $f.nome) {
        $saida = try { & $f.teste } catch { $null }
        if ($saida) { Registrar $f.nome 'OK' "$saida" }
        else { Registrar $f.nome 'NAO-VERIF' 'no PATH, mas não respondeu --version' }
    } else {
        Registrar $f.nome 'FALTA' "choco install $($f.pacote)"
    }
}

# ---------------------------------------------------------------------------------------------
# 3. hunspell — o verificador ortográfico
# ---------------------------------------------------------------------------------------------
if (-not (TemComando 'hunspell') -and -not $SomenteConferir) {
    Write-Host "instalando hunspell..." -ForegroundColor Yellow
    & choco install hunspell.portable -y | Out-Null
    $env:Path = [Environment]::GetEnvironmentVariable('Path', 'Machine') + ';' +
                [Environment]::GetEnvironmentVariable('Path', 'User')
}
if (TemComando 'hunspell') {
    Registrar 'hunspell' 'OK' (Get-Command hunspell).Source
} else {
    Registrar 'hunspell' 'FALTA' 'choco install hunspell.portable'
}

# ---------------------------------------------------------------------------------------------
# 4. Dicionários — o binário do hunspell NÃO traz nenhum
# ---------------------------------------------------------------------------------------------
if (-not $SomenteConferir) {
    New-Item -ItemType Directory -Force -Path $pastaDicionarios | Out-Null
}
$baseUrl = 'https://raw.githubusercontent.com/LibreOffice/dictionaries/master'

foreach ($idioma in $Idiomas) {
    if (-not $origemDicionarios.ContainsKey($idioma)) {
        Registrar "dic:$idioma" 'NAO-VERIF' 'idioma sem origem mapeada neste script'
        continue
    }
    $dic = Join-Path $pastaDicionarios "$idioma.dic"
    $aff = Join-Path $pastaDicionarios "$idioma.aff"

    if (-not (Test-Path $dic) -and -not $SomenteConferir) {
        $o = $origemDicionarios[$idioma]
        foreach ($ext in @('dic', 'aff')) {
            $url = "$baseUrl/$($o.pasta)/$($o.arquivo).$ext"
            $destino = Join-Path $pastaDicionarios "$idioma.$ext"
            try {
                Invoke-WebRequest -Uri $url -OutFile $destino -UseBasicParsing -TimeoutSec 120
            } catch {
                Write-Host "  falhou baixar $idioma.$ext de $url" -ForegroundColor Red
            }
        }
    }

    if (-not (Test-Path $dic) -or -not (Test-Path $aff)) {
        Registrar "dic:$idioma" 'FALTA' "esperado em $dic"
        continue
    }

    # PROVA DE VIDA com caso-controle: uma palavra inventada TEM de ser reprovada. Sem dicionário
    # o hunspell não reclama de nada, e "não reclamou" seria lido como "tudo válido" — o defeito
    # que a regra 12 descreve, e que este script existe para não cometer.
    if (TemComando 'hunspell') {
        $saida = @('^zzzqqqxxxnaoexiste') | & hunspell -a -d $idioma -i UTF-8 2>&1
        $reprovou = $saida | Where-Object { $_ -match '^[&#]' }
        if ($reprovou) {
            $kb = [math]::Round((Get-Item $dic).Length / 1KB)
            Registrar "dic:$idioma" 'OK' "$kb KB, reprovou o caso-controle"
        } else {
            Registrar "dic:$idioma" 'NAO-VERIF' 'arquivo presente mas não reprovou palavra inventada'
        }
    } else {
        Registrar "dic:$idioma" 'NAO-VERIF' 'arquivo presente, hunspell ausente para conferir'
    }
}

# ---------------------------------------------------------------------------------------------
# 5. LM Studio — não se instala por script, mas a ausência precisa aparecer
# ---------------------------------------------------------------------------------------------
try {
    $m = Invoke-RestMethod -Uri 'http://localhost:1234/v1/models' -TimeoutSec 5
    Registrar 'LM Studio' 'OK' "$($m.data.Count) modelo(s) disponivel(is)"
} catch {
    Registrar 'LM Studio' 'NAO-VERIF' 'nao respondeu em :1234 (abra o app e carregue um modelo)'
}

# ---------------------------------------------------------------------------------------------
# Placar
# ---------------------------------------------------------------------------------------------
Write-Host ""
$resultado | ForEach-Object {
    $cor = switch ($_.Estado) { 'OK' { 'Green' } 'FALTA' { 'Red' } default { 'Yellow' } }
    Write-Host ("  {0,-12} {1,-10} {2}" -f $_.Item, $_.Estado, $_.Detalhe) -ForegroundColor $cor
}

$ok    = ($resultado | Where-Object Estado -eq 'OK').Count
$falta = ($resultado | Where-Object Estado -eq 'FALTA').Count
$nv    = ($resultado | Where-Object Estado -eq 'NAO-VERIF').Count

Write-Host "`n  $ok OK · $falta FALTA · $nv NAO-VERIFICADO" -ForegroundColor Cyan
Write-Host "  (NAO-VERIFICADO nao e aprovacao: e o que o script nao conseguiu confirmar)`n" -ForegroundColor DarkGray

# Só FALTA reprova. NAO-VERIF sai com codigo proprio, para nao ser confundido com sucesso.
if ($falta -gt 0) { exit 1 }
if ($nv -gt 0) { exit 2 }
exit 0
