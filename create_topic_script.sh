#!/bin/bash

echo "🚀 Création du topic Kafka 'sensor-data'..."

# Attendre que Kafka soit complètement démarré
echo "⏳ Attente de Kafka (30 secondes)..."
sleep 30

# Créer le topic avec 2 partitions
docker exec kafka kafka-topics --create \
  --topic sensor-data \
  --bootstrap-server localhost:9092 \
  --partitions 2 \
  --replication-factor 1

if [ $? -eq 0 ]; then
    echo "✅ Topic 'sensor-data' créé avec succès (2 partitions)"
else
    echo "❌ Erreur lors de la création du topic"
    exit 1
fi

# Vérifier la création
echo "📋 Liste des topics disponibles :"
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Afficher les détails du topic
echo "🔍 Détails du topic 'sensor-data' :"
docker exec kafka kafka-topics --describe --topic sensor-data --bootstrap-server localhost:9092

echo "🎯 Topic prêt ! Vous pouvez maintenant démarrer les applications."