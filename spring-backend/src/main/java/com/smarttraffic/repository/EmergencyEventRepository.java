package com.smarttraffic.repository;

import com.smarttraffic.model.EmergencyEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyEventRepository extends JpaRepository<EmergencyEvent, Long> {
}
