
FROM gradle:8.5-jdk21 AS xray-builder

WORKDIR /opt/xray

# Скачиваем готовый бинарник Xray
RUN apt-get update && \
    apt-get install -y wget unzip && \
    echo "Downloading Xray binary..." && \
    wget -O Xray-linux-64.zip https://github.com/XTLS/Xray-core/releases/download/v24.12.18/Xray-linux-64.zip && \
    echo "Extracting..." && \
    unzip -q Xray-linux-64.zip && \
    chmod +x xray && \
    ./xray version && \
    rm -f Xray-linux-64.zip geoip.dat geosite.dat && \
    rm -rf /var/lib/apt/lists/*


FROM gradle:8.5-jdk21

WORKDIR /app

COPY --from=xray-builder /opt/xray /opt/xray

EXPOSE 8080 443

CMD ["./gradlew", "bootRun", "--no-daemon"]
