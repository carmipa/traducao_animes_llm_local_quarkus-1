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

    A SEGUNDA CICATRIZ, medida em 17/08/2026 — este portao deu FALSO VERDE:

        21:37  [0] PODE COMPILAR — KRONOS vivo (PID 24328), mas o log nao e escrito ha 605s
               (limite 90s). Ultimo job ja terminou.
        21:37  console-web.log, no mesmo segundo:
               [revisao-lore] [Arquivo 14/22 | Fala 3634/5623] enviada ao LLM

    Havia uma Revisao de Lore de 22 arquivos correndo, e o portao mandou compilar. Duas falhas
    somadas, as duas do mesmo tipo — MEDIR O RELOGIO ERRADO:

      1. Olhava so `logs\execucoes\`. A tela 3.2 tambem escreve em `logs\console-web.log`, que
         nem era candidato.
      2. Usava `LastWriteTime`. No Windows, a data de um arquivo que o processo mantem ABERTO
         nao e atualizada a cada escrita: fica congelada ate um flush. Medido nesta maquina, no
         MESMO arquivo, com 2 minutos de diferenca: as 21:37 dizia "parado ha 605s"; as 21:39,
         "parado ha 0s". O log estava sendo escrito nos dois momentos. Pior que errado: e
         INTERMITENTE, e erra logo depois que um job comeca — o pior instante possivel.

    O relogio confiavel e o carimbo que a propria aplicacao escreve no comeco da linha. Ele nao
    depende de flush, de NTFS nem de cache de metadado.

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

# --- 2. TODOS os logs que uma tela pode estar escrevendo ---------------------------------------
# Nao basta `logs\execucoes\`: a Revisao de Lore (3.2) escreve em `logs\console-web.log`, e foi
# exatamente essa lacuna que deixou o falso verde de 17/08 passar.
$candidatos = @()
$consoleWeb = Join-Path $raiz 'logs\console-web.log'
if (Test-Path $consoleWeb) { $candidatos += Get-Item $consoleWeb }
$pastaLogs = Join-Path $raiz 'logs\execucoes'
if (Test-Path $pastaLogs) {
    $candidatos += Get-ChildItem $pastaLogs -Filter '*.log' -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending | Select-Object -First 4
}
if (-not $candidatos) {
    Sair 2 ("[2] NAO VERIFICADO — KRONOS vivo (PID $pidKronos) e nenhum log para consultar " +
        "(procurei em logs\console-web.log e logs\execucoes\*.log). Nao consigo saber se ha " +
        "job rodando, e 'nao olhei' nunca e 'pode'.") 'Yellow'
}

# --- 3. Qual foi a ULTIMA atividade, pelo carimbo que a aplicacao escreve na linha? ------------
# Nos dois formatos de log os 19 primeiros caracteres sao 'yyyy-MM-dd HH:mm:ss' — o que vem
# depois (`.` ou `,` nos milissegundos) diverge e nao importa aqui.
$agora = Get-Date
$maisRecente = $null      # instante da atividade mais nova achada
$linhasDoAtivo = @()      # ultimas linhas do log que produziu esse instante
$nomeDoAtivo = ''
$lidos = 0

foreach ($arq in $candidatos) {
    # O arquivo fica aberto pelo processo: copia-se antes de ler.
    $copia = Join-Path $env:TEMP ("kronos-pode-compilar-" + $arq.BaseName + ".log")
    try {
        Copy-Item $arq.FullName $copia -Force -ErrorAction Stop
    } catch {
        continue   # este nao deu; se NENHUM der, o contador $lidos trata como estado 2
    }
    $lidos++
    # @() e obrigatorio: com UMA linha sobrando o pipeline devolve string, nao vetor, e
    # $ultimas[0] passaria a valer o primeiro CARACTERE. O portao caia em 2 sem entender por que.
    $ultimas = @([System.IO.File]::ReadAllLines($copia) |
        Where-Object { $_ -notmatch 'access-log' } | Select-Object -Last 60)
    $carimbo = $null
    for ($i = $ultimas.Count - 1; $i -ge 0 -and -not $carimbo; $i--) {
        if ($ultimas[$i] -match '^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})') {
            $carimbo = [datetime]::ParseExact($Matches[1], 'yyyy-MM-dd HH:mm:ss', $null)
        }
    }
    # So o carimbo da aplicacao conta. `LastWriteTime` fica DE FORA do calculo, e nao por
    # preciosismo: ele erra para os DOIS lados e cada lado tem seu estrago.
    #   atrasado  -> falso verde: foi o que mandou compilar sobre um job vivo em 17/08.
    #   adiantado -> falso vermelho: a JVM descarrega o metadado DEPOIS do job acabar, e o
    #                portao passaria a barrar compilacao com o KRONOS parado. Guarda que
    #                reprova o caso correto e pior que guarda nenhuma — alarme falso ensina a
    #                desligar o alarme, e ai o Stink Bomb morre de novo.
    # Log sem carimbo nenhum nao vira aprovacao: cai no estado 2 mais abaixo.
    if ($carimbo -and (-not $maisRecente -or $carimbo -gt $maisRecente)) {
        $maisRecente = $carimbo
        $linhasDoAtivo = $ultimas
        $nomeDoAtivo = $arq.Name
    }
}

if ($lidos -eq 0) {
    Sair 2 ("[2] NAO VERIFICADO — KRONOS vivo (PID $pidKronos) e nenhum dos $($candidatos.Count) " +
        "logs pode ser lido. 'Nao consegui olhar' nunca e 'pode'.") 'Yellow'
}
if (-not $maisRecente) {
    Sair 2 ("[2] NAO VERIFICADO — li $lidos log(s) e nenhum tinha linha com carimbo " +
        "'yyyy-MM-dd HH:mm:ss'. Sem o relogio da aplicacao nao da para saber se ha job; alvo " +
        "vazio sai 2, jamais 0 (regra 23).") 'Yellow'
}

$paradoHa = [int]($agora - $maisRecente).TotalSeconds
if ($paradoHa -lt 0) { $paradoHa = 0 }

if ($paradoHa -ge $OciosoSegundos) {
    Sair 0 ("[0] PODE COMPILAR — KRONOS vivo (PID $pidKronos), e nenhum dos $lidos logs recebeu " +
        "linha ha ${paradoHa}s (limite ${OciosoSegundos}s). Mais recente: $nomeDoAtivo.") 'Green'
}

# --- 4. Ha escrita AGORA. E job, ou so ruido? -------------------------------------------------
# Vocabulario das telas que rodam trabalho longo. `[console]` fica de fora de proposito: e o
# banner de arranque, e escreve sem que exista job.
$marcaDeJob = 'pipeline-fila-execucao|Enviando lote|Lote \d+ traduzido|Arquivo \d+/\d+|' +
    '\[(?:revisao-lore|revisao|revisao-concordancia|traducao|traducao-karaoke|novo-karaoke|' +
    'extracao|remuxer|correcao|auditor-conteudo|troca-tipo-legenda|renomear-arquivos)\]'
$sinaisDeJob = $linhasDoAtivo | Where-Object { $_ -match $marcaDeJob }

if ($sinaisDeJob) {
    $amostra = ($sinaisDeJob | Select-Object -Last 1) -replace '^\S+ \S+ \w+\s+\[[^\]]+\] \([^)]+\) ', ''
    if ($amostra.Length -gt 110) { $amostra = $amostra.Substring(0, 110) + '...' }
    Sair 1 ("[1] NAO COMPILE — ha job do KRONOS em andamento (PID $pidKronos, ultima " +
        "atividade ha ${paradoHa}s em ${nomeDoAtivo}):`n      $amostra`n" +
        "`n    Compilar dispara live reload do Quarkus e MATA o arquivo em curso. Em 14/08/2026 " +
        "isso custou o episodio Stink Bomb no lote 281 de 368.`n" +
        "    Espere o job terminar, ou pare o KRONOS de proposito antes de compilar.") 'Red'
}

# Escrita viva que nao reconheco NAO e permissao. Tela nova, prefixo novo, log novo: o portao
# cai aqui e escala, em vez de aprovar por nao entender — guarda que descarta o que nao entende
# aprova por cegueira (regra 23).
Sair 2 ("[2] NAO VERIFICADO — $nomeDoAtivo recebeu linha ha ${paradoHa}s (limite " +
    "${OciosoSegundos}s), mas nenhuma casa o vocabulario de job conhecido. Pode ser tela nova " +
    "que este portao ainda nao aprendeu. Olhe o log antes de compilar.") 'Yellow'
