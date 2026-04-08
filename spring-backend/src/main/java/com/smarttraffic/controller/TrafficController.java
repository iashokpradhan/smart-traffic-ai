package com.smarttraffic.controller;

import com.smarttraffic.model.EmergencyEvent;
import com.smarttraffic.model.Intersection;
import com.smarttraffic.model.IntersectionHistory;
import com.smarttraffic.model.SignalLog;
import com.smarttraffic.repository.EmergencyEventRepository;
import com.smarttraffic.repository.IntersectionRepository;
import com.smarttraffic.repository.SignalLogRepository;
import com.smarttraffic.service.GeminiVisionService;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

import java.util.*;

@RestController
@CrossOrigin(origins = "*")
public class TrafficController {

    @Autowired
    private IntersectionRepository intersectionRepo;

    @Autowired
    private SignalLogRepository signalLogRepo;

    @Autowired
    private EmergencyEventRepository emergencyEventRepo;

    @Autowired
    private GeminiVisionService geminiVisionService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostConstruct
    public void initDb() {
        if (intersectionRepo.count() == 0) {
            Arrays.asList("A1", "A2", "B1", "B2", "C1", "C2").forEach(id -> {
                Intersection i = new Intersection(id);
                intersectionRepo.save(i);
            });
        }
    }

    private String calculateDensity(int count) {
        if (count < 10) return "LOW";
        if (count < 30) return "MEDIUM";
        return "HIGH";
    }

    private int calculateSignalTiming(String density) {
        switch (density) {
            case "LOW": return 20;
            case "MEDIUM": return 40;
            case "HIGH": return 60;
            default: return 30;
        }
    }

    private void broadcastUpdate() {
        try {
            List<Intersection> intersections = new ArrayList<>();
            intersectionRepo.findAll().forEach(intersections::add);
            
            messagingTemplate.convertAndSend("/topic/traffic", intersections);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam("intersectionId") String intersectionId) {
        try {
            Intersection intersection = intersectionRepo.findById(intersectionId).orElse(null);
            if (intersection == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid intersectionId"));
            }
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No file uploaded"));
            }

            int count = geminiVisionService.detectVehicles(file.getBytes(), file.getContentType(), 15);
            
            intersection.setVehicleCount(count);
            intersection.setDensity(calculateDensity(count));
            intersection.setSignalTiming(calculateSignalTiming(intersection.getDensity()));
            
            intersection.getHistory().add(new IntersectionHistory(new Date(), count));
            intersectionRepo.save(intersection);
            
            signalLogRepo.save(new SignalLog(intersectionId, intersection.getSignalTiming(), new Date()));
            
            broadcastUpdate();
            
            return ResponseEntity.ok(Map.of("message", "Traffic processed", "data", intersection));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/traffic/{id}")
    public ResponseEntity<?> getTraffic(@PathVariable String id) {
        Intersection intersection = intersectionRepo.findById(id).orElse(null);
        if (intersection == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(intersection);
    }

    @PostMapping("/signal/update")
    public ResponseEntity<?> updateSignal(@RequestBody Map<String, Object> payload) {
        String id = (String) payload.get("intersectionId");
        int timing = (Integer) payload.get("timing");
        
        Intersection intersection = intersectionRepo.findById(id).orElse(null);
        if (intersection == null) return ResponseEntity.notFound().build();
        
        intersection.setSignalTiming(timing);
        intersectionRepo.save(intersection);
        
        signalLogRepo.save(new SignalLog(id, timing, new Date()));
        
        broadcastUpdate();
        
        return ResponseEntity.ok(Map.of("message", "Signal updated", "intersection", intersection));
    }

    @PostMapping("/emergency")
    public ResponseEntity<?> triggerEmergency(@RequestBody Map<String, Object> payload) {
        String id = (String) payload.get("intersectionId");
        
        Intersection intersection = intersectionRepo.findById(id).orElse(null);
        if (intersection == null) return ResponseEntity.notFound().build();
        
        intersection.setSignalTiming(90);
        intersectionRepo.save(intersection);
        
        EmergencyEvent ev = new EmergencyEvent(id, new Date(), "EMERGENCY");
        emergencyEventRepo.save(ev);
        
        broadcastUpdate();
        
        return ResponseEntity.ok(Map.of("message", "Emergency priority activated", "event", ev));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        List<Map<String, Object>> analytics = new ArrayList<>();
        intersectionRepo.findAll().forEach(i -> {
            int total = i.getHistory().stream().mapToInt(IntersectionHistory::getCount).sum();
            Map<String, Object> stat = new HashMap<>();
            stat.put("id", i.getId());
            stat.put("totalVehicles", total);
            stat.put("density", i.getDensity());
            stat.put("currentSignal", i.getSignalTiming());
            analytics.add(stat);
        });
        
        return ResponseEntity.ok(Map.of(
            "intersections", analytics,
            "signalLogs", signalLogRepo.findAll(),
            "emergencyEvents", emergencyEventRepo.findAll()
        ));
    }

    // Interval to simulate traffic
    @Scheduled(fixedRate = 10000)
    public void simulateTraffic() {
        if (intersectionRepo.count() > 0) {
            Random r = new Random();
            intersectionRepo.findAll().forEach(i -> {
                int count = r.nextInt(50);
                i.setVehicleCount(count);
                i.setDensity(calculateDensity(count));
                i.setSignalTiming(calculateSignalTiming(i.getDensity()));
                i.getHistory().add(new IntersectionHistory(new Date(), count));
                
                intersectionRepo.save(i);
                signalLogRepo.save(new SignalLog(i.getId(), i.getSignalTiming(), new Date()));
            });
            broadcastUpdate();
            System.out.println("🔄 Real-time traffic updated...");
        }
    }
}
