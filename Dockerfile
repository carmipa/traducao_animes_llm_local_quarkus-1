# syntax=docker/dockerfile:1
#
# KRONOS CORE — imagem de execução local.
#
# POR QUE ESTA IMAGEM EXISTE
# O KRONOS invoca três binários que não são Java: ffmpeg, ffprobe e mkvextract.
# Até aqui, quem quisesse rodar o projeto tinha de instalar ffmpeg e mkvtoolnix
# à mão e acertar o PATH do Windows. Empacotar isso é o maior ganho da imagem —
# maior que qualquer questão de runtime ou de portabilidade de JVM.
#
# ESTA IMAGEM NÃO É PARA VPS. O KRONOS roda na máquina de quem usa, ao lado de
# um LM Studio local. O mapeamento para o host está no docker-compose.yml.
#
# Uso: docker compose build && docker compose up -d

# --------------------------------------------------------------------------
# Estágio 1 — build. Java 25 é exigência do projeto (build.gradle:52).
# --------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /build

COPY gradle gradle
COPY gradlew gradlew.bat settings.gradle build.gradle gradle.properties ./
# O repositório é editado no Windows: sem remover o CR o shell do container
# responde "bad interpreter: /bin/sh^M" e o build morre antes de compilar.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

COPY src src
RUN ./gradlew build -x test --no-daemon -Dquarkus.package.jar.type=fast-jar

# --------------------------------------------------------------------------
# Estágio 2 — execução. Base Debian/Ubuntu, não UBI.
#
# O projeto irmão (framework-net) roda em ubi9/openjdk-25-runtime, e aqui isso
# seria um erro: ffmpeg não existe nos repositórios do RHEL, e chegar nele pela
# UBI exige RPM Fusion de terceiro. No Ubuntu é um apt-get. A divergência do
# padrão é deliberada e o motivo é o ffmpeg.
# --------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-noble

# ffmpeg + ffprobe : leitura e validação das faixas do contêiner .mkv
# mkvtoolnix       : fornece o mkvextract, usado na extração de legenda
# fontconfig       : libass pede fontes ao validar legenda com fonte declarada;
#                    sem isto a falha vem obscura, no meio de um pipeline longo
# curl             : usado só pelo HEALTHCHECK abaixo
RUN apt-get update \
 && apt-get install -y --no-install-recommends \
        ffmpeg mkvtoolnix fontconfig curl tzdata \
 && rm -rf /var/lib/apt/lists/*

# UTF-8 no processo inteiro. O acervo tem obra com acento e colchete no nome
# ([Sokudo], [Coalgirls], [Joseki]) e legenda em PT-BR; sob locale POSIX esses
# nomes viram mojibake já na leitura de diretório. É o mesmo defeito que o
# build.gradle combate no console do Windows, aparecendo por outra porta.
# Java 18+ já usa UTF-8 por padrão em file.encoding, então não se acrescenta
# JAVA_TOOL_OPTIONS aqui — ele só poluiria o stderr com o banner.
ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    TZ=America/Sao_Paulo

WORKDIR /app

COPY --from=build /build/build/quarkus-app/lib/     lib/
COPY --from=build /build/build/quarkus-app/*.jar    ./
COPY --from=build /build/build/quarkus-app/app/     app/
COPY --from=build /build/build/quarkus-app/quarkus/ quarkus/

# Usuário não-root. As pastas de estado são criadas com o dono certo ANTES de
# receberem volume: se o Docker as criasse por conta própria no primeiro `up`,
# viriam como root e a aplicação falharia ao gravar cache — e falha de escrita
# de cache aparece como "traduziu tudo de novo", não como erro de permissão.
RUN groupadd -r kronos && useradd -r -g kronos -d /app kronos \
 && mkdir -p /app/cache /app/logs /app/relatorios /app/backups /app/entrada /app/saida \
 && chown -R kronos:kronos /app

USER kronos

EXPOSE 8099

# A aplicação não expõe /q/health (smallrye-health não está no build), então o
# alvo é a própria página. start-period generoso: o boot carrega contextos de
# lore e faz a varredura inicial de acervo.
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS http://localhost:8099/ >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]
