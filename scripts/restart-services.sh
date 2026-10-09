#!/usr/bin/env bash
set -e

# 1. messaging
sudo systemctl start activemq.service

# 2. config server, wait until ready
sudo systemctl start dvdtheque-server-config.service
until curl -sf http://localhost:8888/actuator/health >/dev/null; do sleep 3; done

# 3. discovery/eureka, wait until ready
sudo systemctl start dvdtheque-discovery-server.service
until curl -sf http://localhost:8761/actuator/health >/dev/null; do sleep 3; done

# 4. backends
sudo systemctl start dvdtheque-tmdb.service
sudo systemctl start dvdtheque-rest.service
sudo systemctl start dvdtheque-batch.service
sudo systemctl start dvdtheque-allocine.service

# 5. let backends register in eureka, then start gateway LAST
sleep 15
sudo systemctl start dvdtheque-api-gateway-server.service
