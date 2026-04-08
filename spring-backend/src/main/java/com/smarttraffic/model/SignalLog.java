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
public class SignalLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String intersectionId;
    private int timing;
    private Date time;
    
    public SignalLog(String intersectionId, int timing, Date time) {
        this.intersectionId = intersectionId;
        this.timing = timing;
        this.time = time;
    }
}
