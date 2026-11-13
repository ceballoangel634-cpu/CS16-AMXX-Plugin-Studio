FROM node:20-bookworm-slim

# Librerías de 32 bits requeridas por el binario oficial amxxpc (Linux x86)
RUN dpkg --add-architecture i386 \
    && apt-get update \
    && apt-get install -y --no-install-recommends \
       ca-certificates \
       libc6:i386 \
       libstdc++6:i386 \
    && rm -rf /var/lib/apt/lists/*

# Usuario sin privilegios para aislamiento de compilación
RUN useradd -m -u 10001 amxxbuilder \
    && mkdir -p /app /opt/amxx

# Copiar compiladores oficiales AMX Mod X (1.9.0 y 1.10.0) e includes
COPY --chown=amxxbuilder:amxxbuilder amxx /opt/amxx
RUN chmod +x /opt/amxx/1.9.0/addons/amxmodx/scripting/amxxpc \
             /opt/amxx/1.10.0/addons/amxmodx/scripting/amxxpc

WORKDIR /app
COPY --chown=amxxbuilder:amxxbuilder package.json server.mjs /app/

USER amxxbuilder

ENV PORT=8080
ENV AMXX_BASE_DIR=/opt/amxx
ENV MAX_SOURCE_BYTES=524288
ENV COMPILE_TIMEOUT_MS=15000

EXPOSE 8080

CMD ["node", "/app/server.mjs"]
