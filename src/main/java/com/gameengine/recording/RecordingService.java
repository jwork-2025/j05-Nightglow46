package com.gameengine.recording;

import com.gameengine.components.TransformComponent;
import com.gameengine.core.GameObject;
import com.gameengine.input.InputManager;
import com.gameengine.scene.Scene;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class RecordingService {
    private final RecordingConfig config;
    private final BlockingQueue<String> lineQueue;
    private volatile boolean recording;
    private Thread writerThread;
    private RecordingStorage storage = new FileRecordingStorage();
    private double elapsed;
    private double keyframeElapsed;
    private double sampleAccumulator;
    private final double warmupSec = 0.1; 
    private final DecimalFormat qfmt;
    private Scene lastScene;
    private java.util.function.Supplier<String> extraDataCallback;

    public RecordingService(RecordingConfig config) {
        this.config = config;
        this.lineQueue = new ArrayBlockingQueue<>(config.queueCapacity);
        this.recording = false;
        this.elapsed = 0.0;
        this.keyframeElapsed = 0.0;
        this.sampleAccumulator = 0.0;
        this.qfmt = new DecimalFormat("0", java.text.DecimalFormatSymbols.getInstance(java.util.Locale.US));
        this.qfmt.setMaximumFractionDigits(Math.max(0, config.quantizeDecimals));
        this.qfmt.setGroupingUsed(false);
    }

    public void setExtraDataCallback(java.util.function.Supplier<String> callback) {
        this.extraDataCallback = callback;
    }

    public boolean isRecording() {
        return recording;
    }

    public void start(Scene scene, int width, int height) throws IOException {
        if (recording) return;
        storage.openWriter(config.outputPath);
        writerThread = new Thread(() -> {
            try {
                while (recording || !lineQueue.isEmpty()) {
                    String s = lineQueue.poll();
                    if (s == null) {
                        try { Thread.sleep(2); } catch (InterruptedException ignored) {}
                        continue;
                    }
                    storage.writeLine(s);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try { storage.closeWriter(); } catch (Exception ignored) {}
            }
        }, "record-writer");
        recording = true;
        writerThread.start();

        // header
        enqueue("{\"type\":\"header\",\"version\":1,\"w\":" + width + ",\"h\":" + height + "}");
        keyframeElapsed = 0.0;
    }

    public void stop() {
        if (!recording) return;
        try {
            // Record end event
            enqueue("{\"type\":\"end\",\"t\":" + qfmt.format(elapsed) + "}");

            if (lastScene != null) {
                // Force write keyframe immediately to queue
                writeKeyframe(lastScene);
            }
        } catch (Exception ignored) {}
        
        // Signal stop
        recording = false;
        
        // Wait for writer thread to finish flushing queue
        try { 
            if (writerThread != null) {
                writerThread.join(2000); // Wait up to 2 seconds
            }
        } catch (InterruptedException ignored) {}
    }

    private Set<Integer> lastPressedKeys = new java.util.HashSet<>();
    private com.gameengine.math.Vector2 lastMousePos = new com.gameengine.math.Vector2(-9999, -9999);
    private boolean[] lastMouseButtons = new boolean[3];

    public void update(double deltaTime, Scene scene, InputManager input) {
        if (!recording) return;
        elapsed += deltaTime;
        keyframeElapsed += deltaTime;
        sampleAccumulator += deltaTime;
        lastScene = scene;

        boolean inputChanged = false;
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"input\",\"t\":").append(qfmt.format(elapsed));

        // Keys
        Set<Integer> currentPressed = input.getPressedKeysSnapshot();
        if (!currentPressed.equals(lastPressedKeys)) {
            inputChanged = true;
            sb.append(",\"keys\":[");
            boolean first = true;
            for (Integer k : currentPressed) {
                if (!first) sb.append(',');
                sb.append(k);
                first = false;
            }
            sb.append("]");
            lastPressedKeys = new java.util.HashSet<>(currentPressed);
        }

        // Mouse Position
        com.gameengine.math.Vector2 currentMousePos = input.getMousePosition();
        if (Math.abs(currentMousePos.x - lastMousePos.x) > config.positionThreshold || 
            Math.abs(currentMousePos.y - lastMousePos.y) > config.positionThreshold) {
            inputChanged = true;
            sb.append(",\"mx\":").append(qfmt.format(currentMousePos.x))
              .append(",\"my\":").append(qfmt.format(currentMousePos.y));
            lastMousePos = currentMousePos;
        }

        // Mouse Buttons
        boolean[] currentButtons = input.getMouseButtonsSnapshot();
        boolean btnsChanged = false;
        for(int i=0; i<3; i++) {
            if (currentButtons[i] != lastMouseButtons[i]) {
                btnsChanged = true;
                break;
            }
        }
        if (btnsChanged) {
            inputChanged = true;
            sb.append(",\"mb\":[");
            for(int i=0; i<3; i++) {
                if (i > 0) sb.append(',');
                sb.append(currentButtons[i] ? 1 : 0);
                lastMouseButtons[i] = currentButtons[i];
            }
            sb.append("]");
        }

        if (inputChanged) {
            sb.append("}");
            enqueue(sb.toString());
        }

        // periodic keyframe
        if (elapsed >= warmupSec && keyframeElapsed >= config.keyframeIntervalSec) {
            if (writeKeyframe(scene)) {
                keyframeElapsed = 0.0;
            }
        }
    }

    public void recordSpawn(String type, float x, float y, String id, String rt, float w, float h, float r, float g, float b, float a) {
        if (!recording) return;
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"spawn\",\"t\":").append(qfmt.format(elapsed))
          .append(",\"id\":\"").append(id).append("\"")
          .append(",\"x\":").append(qfmt.format(x))
          .append(",\"y\":").append(qfmt.format(y))
          .append(",\"rt\":\"").append(rt).append("\"")
          .append(",\"w\":").append(qfmt.format(w))
          .append(",\"h\":").append(qfmt.format(h))
          .append(",\"color\":[")
          .append(qfmt.format(r)).append(',')
          .append(qfmt.format(g)).append(',')
          .append(qfmt.format(b)).append(',')
          .append(qfmt.format(a)).append(']')
          .append("}");
        enqueue(sb.toString());
    }

    private boolean writeKeyframe(Scene scene) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"keyframe\",\"t\":").append(qfmt.format(elapsed));
        
        if (extraDataCallback != null) {
            String extra = extraDataCallback.get();
            if (extra != null && !extra.isEmpty()) {
                sb.append(",").append(extra);
            }
        }
        
        sb.append(",\"entities\":[");
        List<GameObject> objs = scene.getGameObjects();
        boolean first = true;
        int count = 0;
        for (GameObject obj : objs) {
            TransformComponent tc = obj.getComponent(TransformComponent.class);
            if (tc == null) continue;
            float x = tc.getPosition().x;
            float y = tc.getPosition().y;
            if (!first) sb.append(',');
            sb.append('{')
              .append("\"id\":\"").append(obj.getName()).append("#").append(obj.getUuid()).append("\",")
              .append("\"x\":").append(qfmt.format(x)).append(',')
              .append("\"y\":").append(qfmt.format(y));

            // 可选渲染信息
            com.gameengine.components.RenderComponent rc = obj.getComponent(com.gameengine.components.RenderComponent.class);
            if (rc != null) {
                com.gameengine.components.RenderComponent.RenderType rt = rc.getRenderType();
                com.gameengine.math.Vector2 sz = rc.getSize();
                com.gameengine.components.RenderComponent.Color col = rc.getColor();
                sb.append(',')
                  .append("\"rt\":\"").append(rt.name()).append("\",")
                  .append("\"w\":").append(qfmt.format(sz.x)).append(',')
                  .append("\"h\":").append(qfmt.format(sz.y)).append(',')
                  .append("\"color\":[")
                  .append(qfmt.format(col.r)).append(',')
                  .append(qfmt.format(col.g)).append(',')
                  .append(qfmt.format(col.b)).append(',')
                  .append(qfmt.format(col.a)).append(']');
            } else {
                sb.append(',').append("\"rt\":\"CUSTOM\"");
            }

            sb.append('}');
            first = false;
            count++;
        }
        sb.append("]}");
        if (count == 0) return false;
        enqueue(sb.toString());
        return true;
    }

    private void enqueue(String line) {
        if (!lineQueue.offer(line)) {
            // 队列满时丢弃
        }
    }

    public RecordingConfig getConfig() {
        return config;
    }

    public void continueRecording(java.io.File sourceFile, String lastKeyframeLine) {
        try {
            double time = RecordingJson.parseDouble(RecordingJson.field(lastKeyframeLine, "t"));
            Iterable<String> lines = new FileRecordingStorage().readLines(sourceFile.getAbsolutePath());
            
            for (String line : lines) {
                String tStr = RecordingJson.field(line, "t");
                try {
                    if (tStr != null) {
                        double t = RecordingJson.parseDouble(tStr);
                        if (t <= time) {
                            lineQueue.put(line);
                        }
                    } else {
                        // Header or other non-timed events
                        lineQueue.put(line);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                
                if (line.equals(lastKeyframeLine)) {
                    break;
                }
            }
            
            this.elapsed = time;
            this.keyframeElapsed = 0.0;
            
            // Restore input state from last keyframe context (if possible) or reset
            // Since we don't have input state in keyframe, we should reset last* variables
            // to force a full input update on next frame.
            this.lastPressedKeys.clear();
            this.lastMousePos.x = -9999;
            this.lastMousePos.y = -9999;
            for(int i=0; i<3; i++) this.lastMouseButtons[i] = false;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
