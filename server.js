/**
 * SMART TRAFFIC MANAGEMENT SYSTEM (Prototype Backend)
 * Tech: Node.js + Express
 * Database: In-memory (for simplicity)
 */

require("dotenv").config();
const express = require("express");
const multer = require("multer");
const cors = require("cors");
const path = require("path");
const { GoogleGenAI } = require("@google/genai");
const http = require("http");
const { Server } = require("socket.io");

const app = express();
const server = http.createServer(app);
const io = new Server(server, { cors: { origin: "*" } });

const ai = new GoogleGenAI({});

app.use(cors());
app.use(express.json());
app.use(express.static(__dirname));


// ============================
// In-Memory Database
// ============================
const db = {
  intersections: {},
  signalLogs: [],
  emergencyEvents: []
};

// Sample Intersections
["A1", "A2", "B1", "B2", "C1", "C2"].forEach(id => {
  db.intersections[id] = {
    id,
    vehicleCount: 0,
    density: "LOW",
    signalTiming: 30,
    history: []
  };
});
// ============================
// File Upload Setup
// ============================
const storage = multer.memoryStorage();
const upload = multer({ storage });

// ============================
// Utility Functions
// ============================

/**
 * Mock Vehicle Detection (Simulates AI)
 */
async function detectVehicles(file, fallback = 15) {
  try {
    const response = await ai.models.generateContent({
      model: "gemini-2.5-flash",
      contents: [
        {
          role: "user",
          parts: [
            {
              inlineData: {
                data: file.buffer.toString("base64"),
                mimeType: file.mimetype || "image/jpeg"
              }
            },
            {
              text: "Estimate the number of vehicles in this image. Return only a number."
            }
          ]
        }
      ]
    });
    const num = parseInt(response.text.trim(), 10);
    return isNaN(num) ? fallback : num;
  } catch (error) {
    console.error("AI Detection failed:", error);
    return fallback;
  }
}

/**
 * Calculate Traffic Density
 */
function calculateDensity(count) {
  if (count < 10) return "LOW";
  if (count < 30) return "MEDIUM";
  return "HIGH";
}

/**
 * Dynamic Signal Timing Logic
 */
function calculateSignalTiming(density) {
  switch (density) {
    case "LOW": return 20;
    case "MEDIUM": return 40;
    case "HIGH": return 60;
    default: return 30;
  }
}

/**
 * Update Intersection Data
 */
function updateIntersection(id, count) {
  const intersection = db.intersections[id];
  if (!intersection) return null;

  intersection.vehicleCount = count;
  intersection.density = calculateDensity(count);
  intersection.signalTiming = calculateSignalTiming(intersection.density);

  intersection.history.push({
    time: new Date(),
    count
  });

  // Log signal update
  db.signalLogs.push({
    intersectionId: id,
    timing: intersection.signalTiming,
    time: new Date()
  });

  io.emit("trafficUpdate", db.intersections);

  return intersection;
}

// ============================
// APIs
// ============================

/**
 * POST /upload
 * Upload image/video & process traffic
 */
app.post("/upload", upload.single("file"), async (req, res) => {
  try {
    const { intersectionId } = req.body;

    if (!intersectionId || !db.intersections[intersectionId]) {
      return res.status(400).json({ error: "Invalid intersectionId" });
    }

    if (!req.file) {
      return res.status(400).json({ error: "No file uploaded" });
    }

    const vehicleCount = await detectVehicles(req.file);
    const updated = updateIntersection(intersectionId, vehicleCount);

    res.json({
      message: "Traffic processed",
      data: updated
    });

  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

/**
 * GET /traffic/:intersectionId
 * Get traffic data
 */
app.get("/traffic/:intersectionId", (req, res) => {
  const intersection = db.intersections[req.params.intersectionId];

  if (!intersection) {
    return res.status(404).json({ error: "Intersection not found" });
  }

  res.json(intersection);
});

/**
 * POST /signal/update
 * Manually update signal timing
 */
app.post("/signal/update", (req, res) => {
  const { intersectionId, timing } = req.body;

  const intersection = db.intersections[intersectionId];
  if (!intersection) {
    return res.status(404).json({ error: "Intersection not found" });
  }

  intersection.signalTiming = timing;

  db.signalLogs.push({
    intersectionId,
    timing,
    time: new Date()
  });

  io.emit("trafficUpdate", db.intersections);

  res.json({ message: "Signal updated", intersection });
});

/**
 * POST /emergency
 * Trigger emergency vehicle priority
 */
app.post("/emergency", (req, res) => {
  const { intersectionId } = req.body;

  const intersection = db.intersections[intersectionId];
  if (!intersection) {
    return res.status(404).json({ error: "Intersection not found" });
  }

  // Override signal to max green
  intersection.signalTiming = 90;

  const event = {
    intersectionId,
    time: new Date(),
    priority: "EMERGENCY"
  };

  db.emergencyEvents.push(event);

  io.emit("trafficUpdate", db.intersections);

  res.json({
    message: "Emergency priority activated",
    event
  });
});

/**
 * GET /dashboard
 * Analytics Data
 */
app.get("/dashboard", (req, res) => {
  const analytics = Object.values(db.intersections).map(i => ({
    id: i.id,
    totalVehicles: i.history.reduce((sum, h) => sum + h.count, 0),
    density: i.density,
    currentSignal: i.signalTiming
  }));

  res.json({
    intersections: analytics,
    signalLogs: db.signalLogs,
    emergencyEvents: db.emergencyEvents
  });
});

// ============================
// Real-Time Simulation
// ============================
setInterval(() => {
  Object.keys(db.intersections).forEach(id => {
    const randomCount = Math.floor(Math.random() * 50);
    updateIntersection(id, randomCount);
  });

  console.log("🔄 Real-time traffic updated...");
}, 10000); // every 10 sec

// ============================
// Start Server
// ============================
const PORT = 3000;
server.listen(PORT, () => {
  console.log(`🚀 Server running on http://localhost:${PORT}`);
});