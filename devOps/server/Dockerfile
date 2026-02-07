
FROM gradle:8.5-jdk21 AS xray-builder

WORKDIR /opt/xray

RUN apt-get update && \
    apt-get install -y wget unzip curl && \
    echo "Downloading Xray..." && \
    wget --timeout=30 --tries=3 https://github.com/XTLS/Xray-core/releases/latest/download/Xray-linux-64.zip || \
    curl -L -o Xray-linux-64.zip https://github.com/XTLS/Xray-core/releases/latest/download/Xray-linux-64.zip && \
    echo "Extracting Xray..." && \
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
