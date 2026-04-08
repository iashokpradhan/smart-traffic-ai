package com.smarttraffic.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@Entity
public class EmergencyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String intersectionId;
    private Date time;
    private String priority;
    
    public EmergencyEvent(String intersectionId, Date time, String priority) {
        this.intersectionId = intersectionId;
        this.time = time;
        this.priority = priority;
    }
}
