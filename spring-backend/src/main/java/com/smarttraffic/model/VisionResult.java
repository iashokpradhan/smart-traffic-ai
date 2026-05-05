package com.smarttraffic.model;

import lombok.Data;

@Data
public class VisionResult {
    private int vehicleCount;
    private boolean hasEmergency;
    private boolean hasAccident;

    public VisionResult(int vehicleCount, boolean hasEmergency, boolean hasAccident) {
        this.vehicleCount = vehicleCount;
        this.hasEmergency = hasEmergency;
        this.hasAccident = hasAccident;
    }
}
