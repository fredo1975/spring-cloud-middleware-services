#pour docker builder le projet
docker-compose up --build -d

#pour tagger une image d'un service config-service ou discovery-service ou gateway-service
docker build -t discovery_service_spring_v3:1.0.0 discovery-service/.

#pour tagger une image des autres services
docker build -t dvdtheque_allocine_service_spring_v3:1.0.0 -f dvdtheque-allocine-service/Dockerfile .