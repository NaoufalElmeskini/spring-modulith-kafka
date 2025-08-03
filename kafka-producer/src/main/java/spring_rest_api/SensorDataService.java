package spring_rest_api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Service
public class SensorDataService {

    private static final Logger logger = LoggerFactory.getLogger(SensorDataService.class);
    private static final String TOPIC_NAME = "sensor-data";
    
    private final KafkaTemplate<String, SensorData> kafkaTemplate;
    private final Random random = new Random();
    
    // Simulation de 3 capteurs différents
    private final List<String> sensorIds = List.of("SENSOR_001", "SENSOR_002", "SENSOR_003");

    public SensorDataService(KafkaTemplate<String, SensorData> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Envoie des données de capteur toutes les 2 secondes
     * La clé Kafka = sensorId permet de garantir l'ordre des messages par capteur
     */
    @Scheduled(fixedRate = 2000)
    public void sendSensorData() {
        // Sélection aléatoire d'un capteur
        String sensorId = sensorIds.get(random.nextInt(sensorIds.size()));
        
        // Génération d'une valeur électrique réaliste (en volts)
        double voltage = 220.0 + (random.nextGaussian() * 10); // Voltage autour de 220V
        
        // Création du message avec timestamp actuel
        SensorData sensorData = new SensorData(
            sensorId,
            Instant.now(), // Event time = moment de création
            voltage,
            "V"
        );

        // Envoi asynchrone vers Kafka
        // La clé (sensorId) détermine la partition
        CompletableFuture<SendResult<String, SensorData>> future = 
            kafkaTemplate.send(TOPIC_NAME, sensorId, sensorData);
        
        // Callback pour traiter le succès/échec
        future.whenComplete((result, exception) -> {
            if (exception == null) {
                logger.info("✅ Message envoyé avec succès - Capteur: {}, Valeur: {:.2f}V, " +
                           "Partition: {}, Offset: {}, Event time: {}", 
                           sensorId, voltage, 
                           result.getRecordMetadata().partition(),
                           result.getRecordMetadata().offset(),
                           sensorData.getTimestamp());
            } else {
                logger.error("❌ Échec envoi message - Capteur: {}, Erreur: {}", 
                           sensorId, exception.getMessage());
            }
        });
    }
}