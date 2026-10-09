<#
.SYNOPSIS
    Monta build/lore/lore.db a partir da lore (src/main/resources/lore) para consultar com sqlite3.

.DESCRIPTION
    PROPOSITO: a lore do KRONOS e um arquivo SQL por obra. Para AUDITAR — quem protege tal termo, em
    quantas obras esta tal regra, o que difere entre duas obras — e mais facil perguntar a um banco do
    que abrir 70 arquivos. Este script monta esse banco, so para consulta. O KRONOS nao le o lore.db:
    ele carrega os .sql no arranque.

    O .db e GERADO e nao e fonte: fica em build/ (fora do git), somente leitura, com uma tabela
    "aviso" dizendo onde editar. Editar o .db nao muda nada no KRONOS — e e por isso que ele nao
    aceita escrita.

    Grava num arquivo temporario e so depois renomeia: um visualizador com o .db aberto TRAVA o
    arquivo no Windows, e sobrescrever no lugar deixaria um banco pela metade.

    TRES ESTADOS:
        0 = banco montado e conferido (integridade e chave estrangeira ok)
        1 = DEFEITO: a lore nao carrega, ou o .db antigo esta aberto em outro programa
        2 = NAO VERIFICADO: sqlite3 ausente ou antigo demais (precisa 3.37+, tabela STRICT)

    Consultas prontas: ferramentas/lore-consultas.sql
        sqlite3 -readonly build/lore/lore.db ".read ferramentas/lore-consultas.sql"

    Validar UMA obra sem compilar nem montar o banco inteiro (nao reinicia o KRONOS de ninguem):
        sqlite3 :memory: ".read src/main/resources/lore/esquema.sql" ".read src/main/resources/lore/obras/<id>.sql"

.PARAMETER Raiz
    Pasta da lore. Padrao: src/main/resources/lore do repositorio.

.PARAMETER Saida
    Caminho do banco. Padrao: build/lore/lore.db do repositorio.

.PARAMETER Sqlite
    Executavel do sqlite3. Padrao: o do PATH.
#>
[CmdletBinding()]
param(
    [string]$Raiz = '',
    [string]$Saida = '',
    [string]$Sqlite = ''
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
if (-not $Raiz)  { $Raiz  = Join-Path $repo 'src\main\resources\lore' }
if (-not $Saida) { $Saida = Join-Path $repo 'build\lore\lore.db' }

# ---- sqlite3: ausente ou antigo e NAO VERIFICADO, nunca "tudo certo"
if (-not $Sqlite) {
    $cmd = Get-Command sqlite3 -ErrorAction SilentlyContinue
    if ($cmd) { $Sqlite = $cmd.Source }
}
if (-not $Sqlite -or -not (Test-Path -LiteralPath $Sqlite)) {
    Write-Host "[2] NAO VERIFICADO: sqlite3 nao encontrado (instale-o ou passe -Sqlite <caminho>)." -ForegroundColor Yellow
    exit 2
}
$versao = (& $Sqlite --version) -split ' ' | Select-Object -First 1
$partes = $versao -split '\.'
if ($partes.Count -lt 2 -or [int]$partes[0] -lt 3 -or ([int]$partes[0] -eq 3 -and [int]$partes[1] -lt 37)) {
    Write-Host "[2] NAO VERIFICADO: sqlite3 $versao nao tem tabela STRICT (precisa 3.37+)." -ForegroundColor Yellow
    exit 2
}

# ---- a lista de obras e a mesma que o KRONOS carrega
$esquema = Join-Path $Raiz 'esquema.sql'
$lista = Join-Path $Raiz 'obras.lst'
foreach ($f in @($esquema, $lista)) {
    if (-not (Test-Path -LiteralPath $f)) {
        Write-Host "[1] DEFEITO: $f nao existe." -ForegroundColor Red
        exit 1
    }
}
$ids = Get-Content -LiteralPath $lista -Encoding utf8 | ForEach-Object { $_.Trim() } |
    Where-Object { $_ -and -not $_.StartsWith('#') }
if (-not $ids) {
    Write-Host "[1] DEFEITO: obras.lst nao lista nenhuma obra." -ForegroundColor Red
    exit 1
}

$pasta = Split-Path -Parent $Saida
New-Item -ItemType Directory -Force -Path $pasta | Out-Null
$temp = "$Saida.tmp-$PID"
$roteiro = "$Saida.roteiro-$PID.sql"
Remove-Item -LiteralPath $temp, $roteiro -Force -ErrorAction SilentlyContinue

$barra = { param($p) ($p -replace '\\', '/') }
$linhas = New-Object System.Collections.Generic.List[string]
$linhas.Add('.bail on')
$linhas.Add('PRAGMA foreign_keys = ON;')
$linhas.Add('BEGIN;')
$linhas.Add(".read '$(& $barra $esquema)'")
foreach ($id in $ids) {
    $arq = Join-Path (Join-Path $Raiz 'obras') "$id.sql"
    if (-not (Test-Path -LiteralPath $arq)) {
        Write-Host "[1] DEFEITO: obras.lst lista '$id' e $arq nao existe." -ForegroundColor Red
        exit 1
    }
    $linhas.Add(".read '$(& $barra $arq)'")
}
$linhas.Add("CREATE TABLE aviso (texto TEXT NOT NULL);")
$linhas.Add("INSERT INTO aviso VALUES ('ARQUIVO GERADO por ferramentas/lore-db.ps1 em $(Get-Date -Format s). A fonte e src/main/resources/lore/obras/*.sql: editar este banco nao muda nada no KRONOS.');")
$linhas.Add('COMMIT;')
[System.IO.File]::WriteAllLines($roteiro, $linhas, (New-Object System.Text.UTF8Encoding($false)))

try {
    $saidaSqlite = & $Sqlite $temp ".read '$(& $barra $roteiro)'" 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[1] DEFEITO: a lore nao carregou no sqlite3:" -ForegroundColor Red
        $saidaSqlite | ForEach-Object { Write-Host "    $_" }
        exit 1
    }
    $integridade = (& $Sqlite -readonly $temp "PRAGMA integrity_check;") -join ' '
    $chaves = (& $Sqlite -readonly $temp "PRAGMA foreign_key_check;") -join ' '
    if ($integridade -ne 'ok' -or $chaves) {
        Write-Host "[1] DEFEITO: integridade='$integridade' chave estrangeira='$chaves'" -ForegroundColor Red
        exit 1
    }

    if (Test-Path -LiteralPath $Saida) {
        Set-ItemProperty -LiteralPath $Saida -Name IsReadOnly -Value $false
    }
    try {
        Move-Item -LiteralPath $temp -Destination $Saida -Force
    } catch {
        # O banco antigo continua no lugar: volta a ser somente leitura, como prometido.
        if (Test-Path -LiteralPath $Saida) {
            Set-ItemProperty -LiteralPath $Saida -Name IsReadOnly -Value $true -ErrorAction SilentlyContinue
        }
        Write-Host "[1] DEFEITO: nao consegui substituir $Saida — ele esta aberto em outro programa? Feche o visualizador e rode de novo. ($($_.Exception.Message))" -ForegroundColor Red
        exit 1
    }
    Set-ItemProperty -LiteralPath $Saida -Name IsReadOnly -Value $true
} finally {
    Remove-Item -LiteralPath $temp, $roteiro -Force -ErrorAction SilentlyContinue
}

$resumo = & $Sqlite -readonly $Saida "SELECT (SELECT count(*) FROM obra), (SELECT count(*) FROM lore_traducao), (SELECT count(*) FROM lore_revisao), (SELECT count(*) FROM termo_protegido), (SELECT count(*) FROM correcao_terminologia);"
$c = $resumo -split '\|'
Write-Host "[0] lore.db montado: $Saida" -ForegroundColor Green
Write-Host "    obras $($c[0]) | traducao $($c[1]) | revisao $($c[2]) | termos protegidos $($c[3]) | correcoes $($c[4]) | sqlite3 $versao"
exit 0
