package spring_rest_api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Modèle identique au producer pour désérialiser les messages JSON
 */
public class SensorData {
    
    @JsonProperty("sensorId")
    private String sensorId;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("value")
    private double value;
    
    @JsonProperty("unit")
    private String unit;

    // Constructeur par défaut requis pour Jackson
    public SensorData() {}

    public SensorData(String sensorId, Instant timestamp, double value, String unit) {
        this.sensorId = sensorId;
        this.timestamp = timestamp;
        this.value = value;
        this.unit = unit;
    }

    // Getters et Setters
    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    @Override
    public String toString() {
        return "SensorData{" +
                "sensorId='" + sensorId + '\'' +
                ", timestamp=" + timestamp +
                ", value=" + value +
                ", unit='" + unit + '\'' +
                '}';
    }
}