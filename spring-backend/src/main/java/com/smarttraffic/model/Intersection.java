package com.smarttraffic.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
public class Intersection {
    
    @Id
    private String id;
    
    private int vehicleCount;
    private String density;
    private int signalTiming;
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "intersection_id")
    private List<IntersectionHistory> history = new ArrayList<>();
    
    public Intersection(String id) {
        this.id = id;
        this.vehicleCount = 0;
        this.density = "LOW";
        this.signalTiming = 30;
    }
}
