# Guide de dépannage et exercices avancés

## 🔧 Dépannage courant

### Problème : Kafka ne démarre pas

**Symptômes :**
- Erreur "Connection refused" dans les logs
- Les applications ne peuvent pas se connecter

**Solutions :**
```bash
# Vérifier que Zookeeper est démarré
docker logs zookeeper

# Vérifier les ports
netstat -tulpn | grep :2181  # Zookeeper
netstat -tulpn | grep :9092  # Kafka

# Redémarrer l'infrastructure
docker-compose down && docker-compose up -d zookeeper kafka
```

### Problème : Topic non créé

**Symptômes :**
- Erreur "Topic sensor-data does not exist"

**Solutions :**
```bash
# Vérifier si le topic existe
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Créer manuellement le topic
docker exec kafka kafka-topics --create \
  --topic sensor-data \
  --bootstrap-server localhost:9092 \
  --partitions 2 \
  --replication-factor 1
```

### Problème : Consumer ne reçoit pas de messages

**Symptômes :**
- Producer envoie mais Consumer silent

**Solutions :**
```bash
# Vérifier les consumer groups
docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list

# Vérifier les offsets
docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group sensor-consumer-group

# Réinitialiser les offsets (pour tests)
docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 \
  --reset-offsets --group sensor-consumer-group --topic sensor-data --to-earliest --execute
```

### Problème : Sérialisation/Désérialisation

**Symptômes :**
- JsonDeserializationException dans les logs

**Solutions :**
- Vérifier que les classes `SensorData` sont identiques
- Vérifier la configuration `TRUSTED_PACKAGES`
- Utiliser des logs détaillés pour diagnostiquer

---

## 🎯 Exercices d'approfondissement

### Exercice 1 : Monitoring et métriques

**Objectif :** Ajouter des métriques de monitoring

**Instructions :**
1. Ajouter des compteurs pour messages envoyés/reçus
2. Mesurer les temps de traitement
3. Créer des endpoints actuator pour exposer les métriques

**Code à ajouter au Producer :**
```java
@Component
public class MetricsService {
    private final AtomicLong messagesSent = new AtomicLong();
    private final AtomicLong messagesFailures = new AtomicLong();
    
    public void incrementSent() { messagesSent.incrementAndGet(); }
    public void incrementFailure() { messagesFailures.incrementAndGet(); }
    
    @EventListener
    public void printStats(ContextRefreshedEvent event) {
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            logger.info("📊 Producer Stats - Envoyés: {}, Échecs: {}", 
                       messagesSent.get(), messagesFailures.get());
        }, 10, 10, TimeUnit.SECONDS);
    }
}
```

### Exercice 2 : Gestion des erreurs avancée

**Objectif :** Implémenter la gestion d'erreurs robuste

**Instructions :**
1. Ajouter un topic `sensor-data-errors` pour les messages en erreur
2. Implémenter un Dead Letter Queue pattern
3. Gérer les retry automatiques avec backoff

**Configuration Consumer avec retry :**
```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, SensorData> 
    kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, SensorData> factory = 
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    
    // Configuration retry
    factory.setCommonErrorHandler(new DefaultErrorHandler(
        new FixedBackOff(1000L, 3) // 3 retry avec 1s d'intervalle
    ));
    
    return factory;
}
```

### Exercice 3 : Partitioning avancé

**Objectif :** Comprendre l'impact du partitioning

**Instructions :**
1. Modifier le Producer pour utiliser un partitioner custom
2. Créer un topic avec 4 partitions
3. Observer la distribution des messages

**Partitioner custom :**
```java
@Component
public class SensorPartitioner implements Partitioner {
    @Override
    public int partition(String topic, Object key, byte[] keyBytes, 
                        Object value, byte[] valueBytes, Cluster cluster) {
        // Partitioning basé sur le hash du sensorId
        String sensorId = (String) key;
        return Math.abs(sensorId.hashCode()) % cluster.partitionCountForTopic(topic);
    }
}
```

### Exercice 4 : Consumer Groups multiples

**Objectif :** Démontrer la scalabilité avec plusieurs consumers

**Instructions :**
1. Créer 2 consumer groups différents
2. Un pour l'alerting, un pour l'archivage
3. Observer que chaque groupe reçoit tous les messages

**Consumer d'alerting :**
```java
@KafkaListener(topics = "sensor-data", groupId = "alert-consumer-group")
public void processAlerts(ConsumerRecord<String, SensorData> record) {
    SensorData data = record.value();
    if (data.getValue() < 200 || data.getValue() > 240) {
        logger.warn("🚨 ALERTE - Capteur {} : tension critique {:.2f}V", 
                   data.getSensorId(), data.getValue());
    }
}
```

### Exercice 5 : Streaming temps réel avec Kafka Streams

**Objectif :** Introduire le traitement en streaming

**Instructions :**
1. Ajouter Kafka Streams au projet
2. Calculer la moyenne mobile sur 5 messages
3. Créer un topic de sortie pour les statistiques

**Dépendance à ajouter :**
```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-streams</artifactId>
</dependency>
```

**Configuration Kafka Streams :**
```java
@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {
    
    @Bean
    public KafkaStreams kafkaStreams() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "sensor-stats-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        
        StreamsBuilder builder = new StreamsBuilder();
        
        KStream<String, SensorData> sensorStream = builder.stream("sensor-data");
        
        // Calcul de moyenne mobile par capteur
        sensorStream
            .groupByKey()
            .windowedBy(TimeWindows.of(Duration.ofMinutes(1)))
            .aggregate(
                () -> new SensorStats(),
                (key, value, aggregate) -> aggregate.add(value),
                Materialized.as("sensor-stats-store")
            )
            .toStream()
            .to("sensor-stats");
            
        return new KafkaStreams(builder.build(), props);
    }
}
```

---

## 📚 Questions d'entretien techniques

### Questions de base

1. **Quelle est la différence entre une queue et un topic ?**
   - Queue : 1 message → 1 consumer (load balancing)
   - Topic : 1 message → N consumers (publish-subscribe)

2. **Pourquoi utiliser Kafka plutôt que RabbitMQ ?**
   - Kafka : haute performance, persistance, replay des messages
   - RabbitMQ : routing complexe, transactions ACID

3. **Qu'est-ce qu'un offset et pourquoi est-il important ?**
   - Position unique d'un message dans une partition
   - Permet le replay, la reprise après crash, l'idempotence

### Questions avancées

4. **Comment gérer le back-pressure dans Kafka ?**
   - Configuration des buffers producer
   - Limitation du throughput consumer
   - Monitoring de la lag

5. **Que se passe-t-il si une partition leader tombe ?**
   - Élection automatique d'un nouveau leader
   - Les répliques prennent le relais
   - Importance du replication factor

6. **Comment assurer l'exactly-once processing ?**
   - Idempotent producer
   - Transactional semantics
   - Exactly-once streams processing

### Questions de production

7. **Comment monitorer un cluster Kafka ?**
   - JMX metrics (lag, throughput, errors)
   - Kafka Manager / Confluent Control Center
   - Alerting sur consumer lag

8. **Stratégies de sizing des partitions ?**
   - Basé sur le throughput cible
   - Nombre de consumers parallèles
   - Taille des messages et retention

---

## 🚀 Prochaines étapes

### Pour aller plus loin

1. **Schema Registry** : Évolution des schémas de données
2. **Kafka Connect** : Intégration avec bases de données
3. **KSQL** : Requêtes SQL sur les streams
4. **Sécurité** : SSL, SASL, ACLs
5. **Multi-cluster** : Replication entre datacenters

### Projets pratiques suggérés

1. **IoT Dashboard** : Visualisation temps réel des capteurs
2. **Event Sourcing** : Système de commandes e-commerce
3. **CDC (Change Data Capture)** : Synchronisation BDD → Kafka
4. **Microservices** : Communication asynchrone entre services

Ce TP vous donne les bases solides pour maîtriser Kafka et être confiant lors d'entretiens techniques. La progression du simple au complexe permet d'assimiler progressivement les concepts clés du streaming de données.

**Temps estimé :** 1-2 jours selon votre niveau d'approfondissement des exercices avancés.