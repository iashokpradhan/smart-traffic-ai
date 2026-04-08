package com.smarttraffic.repository;

import com.smarttraffic.model.SignalLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignalLogRepository extends JpaRepository<SignalLog, Long> {
}
