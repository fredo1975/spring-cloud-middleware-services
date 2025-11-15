// ============================================================================
// 10. README.md - Documentation
// ============================================================================
/*
# MCP Server DVDthèque avec Ollama

## Description
Serveur MCP (Model Context Protocol) qui expose une API conversationnelle
pour gérer une collection de Films en utilisant Spring AI + Ollama (LLM local).

## Prérequis
- Java 17+
- Maven 3.8+
- Ollama installé et en cours d'exécution
- Un modèle Ollama (recommandé: llama3.2, mistral, ou llama2)

## Installation d'Ollama

### macOS/Linux
```bash
curl -fsSL https://ollama.com/install.sh | sh
```

### Windows
Téléchargez depuis https://ollama.com/download

### Télécharger un modèle
```bash
# Modèle recommandé (performant et léger)
ollama pull llama3.2

# Alternatives
ollama pull mistral
ollama pull llama2
ollama pull codellama
```

### Vérifier qu'Ollama fonctionne
```bash
ollama list
curl http://localhost:11434/api/tags
```

## Configuration

### 1. Modifier application.yml
Changer le modèle si nécessaire:
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: llama3.2  # ou mistral, llama2, etc.
```

### 2. Configurer l'endpoint DVDthèque
```yaml
dvdtheque:
  api:
    base-url: http://localhost:3000/api
```

## Installation et Démarrage

```bash
# Compiler
mvn clean install

# Lancer
mvn spring-boot:run
```

## Endpoints

### MCP Chat avec Ollama
```bash
POST http://localhost:8080/mcp/chat
Content-Type: application/json

{
  "message": "Montre-moi tous les films de science-fiction",
  "conversationId": "user123"
}
```

### Effacer une conversation
```bash
DELETE http://localhost:8080/mcp/conversation/user123
```

### Health Check
```bash
GET http://localhost:8080/mcp/health
```

### API REST classique
```bash
GET    /api/dvds           - Tous les DVDs
GET    /api/dvds/{id}      - DVD par ID
POST   /api/dvds           - Créer un DVD
PUT    /api/dvds/{id}      - Mettre à jour un DVD
DELETE /api/dvds/{id}      - Supprimer un DVD
GET    /api/dvds/search?q= - Rechercher des DVDs
```

## Exemples d'utilisation du chat

### 1. Lister tous les DVDs
```bash
curl -X POST http://localhost:8080/mcp/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Montre-moi tous mes films"}'
```

### 2. Rechercher un film
```bash
curl -X POST http://localhost:8080/mcp/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Cherche les films de Christopher Nolan"}'
```

### 3. Ajouter un film
```bash
curl -X POST http://localhost:8080/mcp/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Ajoute le film Inception réalisé par Christopher Nolan en 2010, genre science-fiction, note 4.8"}'
```

### 4. Modifier un film
```bash
curl -X POST http://localhost:8080/mcp/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Change la note du film numéro 5 à 4.5"}'
```

### 5. Supprimer un film
```bash
curl -X POST http://localhost:8080/mcp/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Supprime le DVD avec l'ID 3"}'
```

### 6. Questions complexes
```bash
curl -X POST http://localhost:8080/mcp/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Quels sont mes films les mieux notés ?"}'
```

## Architecture
- Spring Boot 3.2
- Spring AI avec Ollama (LLM local, pas de clé API nécessaire!)
- WebClient pour les appels API REST
- MCP pour l'interface conversationnelle
- Mémoire de conversation intégrée

## Avantages d'Ollama
✅ Gratuit et open-source
✅ Exécution locale (confidentialité des données)
✅ Pas de limite de requêtes
✅ Pas besoin de clé API
✅ Fonctionne offline
✅ Plusieurs modèles disponibles

## Modèles recommandés

### llama3.2 (recommandé)
- Rapide et performant
- Bon en français
- Taille: ~2GB

### mistral
- Excellent en français
- Très performant
- Taille: ~4GB

### llama2
- Version stable
- Bon équilibre performance/vitesse
- Taille: ~3.8GB

## Performance
Les réponses peuvent prendre quelques secondes selon:
- Le modèle utilisé
- La puissance de votre machine
- La complexité de la requête

Pour améliorer les performances:
1. Utilisez un modèle plus petit (llama3.2)
2. Augmentez la RAM allouée à Java: `java -Xmx4g`
3. Utilisez un GPU si disponible

## Troubleshooting

### Ollama ne répond pas
```bash
# Vérifier qu'Ollama tourne
curl http://localhost:11434/api/tags

# Redémarrer Ollama
ollama serve
```

### Le modèle est lent
```bash
# Essayer un modèle plus léger
ollama pull llama3.2
```

### Erreur de connexion
Vérifier que l'URL de base est correcte dans application.yml:
```yaml
spring.ai.ollama.base-url: http://localhost:11434
```

## Licence
MIT
*/