package spring_rest_api;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class SensorDataConsumer {

    private static final Logger logger = LoggerFactory.getLogger(SensorDataConsumer.class);

    @KafkaListener(topics = "javatechie-demo",groupId = "jt-group")
    public void consumeEvents(SensorData sensor) {
        System.out.println("consumer consume the events : " + sensor.getSensorId());
    }

    /**
     * Écoute les messages du topic "sensor-data"
     * Affiche les informations détaillées sur chaque message reçu
     */
    @KafkaListener(topics = "sensor-data", groupId = "sensor-consumer-group")
    public void consumeSensorData(ConsumerRecord<String, SensorData> record) {
        
        // Processing time = moment où le message est traité
        Instant processingTime = Instant.now();
        
        // Récupération des données du message
        SensorData sensorData = record.value();
        String sensorId = record.key();
        
        // Métadonnées Kafka
        int partition = record.partition();
        long offset = record.offset();
        
        // Calcul de la latence (écart entre event time et processing time)
        Duration latency = Duration.between(sensorData.getTimestamp(), processingTime);
        
        // Affichage détaillé du message reçu
        logger.info("📥 MESSAGE REÇU - " +
                   "Capteur: {} | " +
                   "Valeur: {:.2f}{} | " +
                   "Partition: {} | " +
                   "Offset: {} | " +
                   "Event time: {} | " +
                   "Processing time: {} | " +
                   "Latence: {}ms",
                   sensorId,
                   sensorData.getValue(),
                   sensorData.getUnit(),
                   partition,
                   offset,
                   sensorData.getTimestamp(),
                   processingTime,
                   latency.toMillis());
        
        // Simulation d'un traitement métier (optionnel)
        processBusinessLogic(sensorData);
    }

    /**
     * Simulation d'un traitement métier sur les données du capteur
     */
    private void processBusinessLogic(SensorData sensorData) {
        // Exemple : alertes si tension anormale
        if (sensorData.getValue() < 200 || sensorData.getValue() > 240) {
            logger.warn("⚠️ ALERTE - Tension anormale détectée sur {} : {:.2f}V", 
                       sensorData.getSensorId(), sensorData.getValue());
        }
        
        // Simulation d'un traitement plus long
        try {
            Thread.sleep(100); // 100ms de traitement
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
