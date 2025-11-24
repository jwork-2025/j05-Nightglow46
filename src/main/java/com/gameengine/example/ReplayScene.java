package com.gameengine.example;

import com.gameengine.core.GameEngine;
import com.gameengine.graphics.Renderer;
import com.gameengine.input.InputManager;
import com.gameengine.scene.Scene;
import com.gameengine.recording.FileRecordingStorage;
import com.gameengine.recording.RecordingJson;
import com.gameengine.recording.RecordingStorage;
import com.gameengine.scene.MenuScene;

import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReplayScene extends Scene {
    private GameEngine engine;
    private String recordingPath;
    private Renderer renderer;
    private InputManager input;
    private float time;
    
    private List<File> recordingFiles;
    private int selectedIndex = 0;

    private static class Keyframe {
        static class EntityInfo {
            float x, y;
            String rt; // RECTANGLE/CIRCLE/LINE/CUSTOM
            float w, h;
            float r=1f,g=1f,b=1f,a=1f;
            String id;
        }
        double t;
        List<EntityInfo> entities = new ArrayList<>();
    }

    private final List<Keyframe> keyframes = new ArrayList<>();

    public ReplayScene(GameEngine engine, String path) {
        super("ReplayScene");
        this.engine = engine;
        this.recordingPath = path;
    }

    @Override
    public void initialize() {
        super.initialize();
        this.renderer = engine.getRenderer();
        this.input = engine.getInputManager();
        this.time = 0;
        this.keyframes.clear();
        
        if (recordingPath != null) {
            loadRecording(recordingPath);
        } else {
            RecordingStorage storage = new FileRecordingStorage();
            recordingFiles = storage.listRecordings();
            if (recordingFiles.isEmpty()) {
                System.out.println("No recordings found.");
            }
        }
    }

    private void loadRecording(String path) {
        RecordingStorage storage = new FileRecordingStorage();
        try {
            for (String line : storage.readLines(path)) {
                String type = RecordingJson.stripQuotes(RecordingJson.field(line, "type"));
                if ("keyframe".equals(type)) {
                    Keyframe kf = new Keyframe();
                    kf.t = RecordingJson.parseDouble(RecordingJson.field(line, "t"));
                    
                    int entKeyIdx = line.indexOf("\"entities\":");
                    String entitiesStr = "";
                    if (entKeyIdx >= 0) {
                        int entArrIdx = line.indexOf("[", entKeyIdx);
                        if (entArrIdx >= 0) {
                            entitiesStr = RecordingJson.extractArray(line, entArrIdx);
                        }
                    }

                    String[] entStrs = RecordingJson.splitTopLevel(entitiesStr);
                    for (String es : entStrs) {
                        Keyframe.EntityInfo ei = new Keyframe.EntityInfo();
                        ei.id = RecordingJson.stripQuotes(RecordingJson.field(es, "id"));
                        ei.x = (float)RecordingJson.parseDouble(RecordingJson.field(es, "x"));
                        ei.y = (float)RecordingJson.parseDouble(RecordingJson.field(es, "y"));
                        ei.rt = RecordingJson.stripQuotes(RecordingJson.field(es, "rt"));
                        ei.w = (float)RecordingJson.parseDouble(RecordingJson.field(es, "w"));
                        ei.h = (float)RecordingJson.parseDouble(RecordingJson.field(es, "h"));
                        
                        // Use the robust field parser for color array
                        String colorStr = RecordingJson.field(es, "color");
                        if (colorStr != null && colorStr.startsWith("[")) {
                            // Remove brackets
                            colorStr = colorStr.substring(1, colorStr.length() - 1);
                            String[] rgba = colorStr.split(",");
                            if (rgba.length >= 3) {
                                try {
                                    ei.r = Float.parseFloat(rgba[0].trim());
                                    ei.g = Float.parseFloat(rgba[1].trim());
                                    ei.b = Float.parseFloat(rgba[2].trim());
                                    if (rgba.length > 3) ei.a = Float.parseFloat(rgba[3].trim());
                                } catch (NumberFormatException e) {
                                    // ignore
                                }
                            }
                        }
                        kf.entities.add(ei);
                    }
                    keyframes.add(kf);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        
        if (input.isKeyJustPressed(KeyEvent.VK_ESCAPE)) {
            engine.setScene(new MenuScene(engine));
            return;
        }

        if (recordingPath == null) {
            // File selection
            if (recordingFiles == null || recordingFiles.isEmpty()) return;
            
            if (input.isKeyJustPressed(KeyEvent.VK_UP)) {
                selectedIndex--;
                if (selectedIndex < 0) selectedIndex = Math.max(0, recordingFiles.size() - 1);
            }
            if (input.isKeyJustPressed(KeyEvent.VK_DOWN)) {
                selectedIndex++;
                if (selectedIndex >= recordingFiles.size()) selectedIndex = 0;
            }
            if (input.isKeyJustPressed(KeyEvent.VK_ENTER)) {
                if (!recordingFiles.isEmpty()) {
                    recordingPath = recordingFiles.get(selectedIndex).getPath();
                    // Launch GameScene in Replay Mode
                    engine.setScene(new GameScene(engine, false, recordingPath));
                }
            }
            return;
        }

        if (keyframes.isEmpty()) return;

        time += deltaTime;
        double lastT = keyframes.get(keyframes.size() - 1).t;
        if (time > lastT) {
            time = (float)lastT; // Stop at end
            // Or loop: time = 0;
        }
    }

    @Override
    public void render() {
        // Draw background
        renderer.drawRect(0, 0, 800, 600, 0.1f, 0.1f, 0.2f, 1.0f);

        super.render();
        
        if (recordingPath == null) {
            renderer.drawText("Select Recording:", 100, 50, 30, 1, 1, 1, 1);
            if (recordingFiles == null || recordingFiles.isEmpty()) {
                renderer.drawText("No recordings found", 100, 100, 20, 1, 0, 0, 1);
                return;
            }
            float y = 100;
            for (int i = 0; i < recordingFiles.size(); i++) {
                float r=1,g=1,b=1;
                if (i == selectedIndex) { r=1; g=1; b=0; }
                renderer.drawText(recordingFiles.get(i).getName(), 100, y + i*30, 20, r, g, b, 1);
            }
            return;
        }

        // Interpolate and render
        if (keyframes.isEmpty()) return;
        
        Keyframe a = keyframes.get(0);
        Keyframe b = keyframes.get(keyframes.size() - 1);
        
        for (int i = 0; i < keyframes.size() - 1; i++) {
            if (time >= keyframes.get(i).t && time <= keyframes.get(i+1).t) {
                a = keyframes.get(i);
                b = keyframes.get(i+1);
                break;
            }
        }
        
        double span = Math.max(1e-6, b.t - a.t);
        float t = (float)((time - a.t) / span);
        
        for (Keyframe.EntityInfo eiA : a.entities) {
            // Find corresponding entity in b
            Keyframe.EntityInfo eiB = null;
            for (Keyframe.EntityInfo e : b.entities) {
                if (e.id.equals(eiA.id)) {
                    eiB = e;
                    break;
                }
            }
            
            float x = eiA.x;
            float y = eiA.y;
            if (eiB != null) {
                x = eiA.x + (eiB.x - eiA.x) * t;
                y = eiA.y + (eiB.y - eiA.y) * t;
            }
            
            // Render
            if ("RECTANGLE".equals(eiA.rt)) {
                renderer.drawRect(x, y, eiA.w, eiA.h, eiA.r, eiA.g, eiA.b, eiA.a);
            } else if ("CIRCLE".equals(eiA.rt)) {
                renderer.drawCircle(x + eiA.w/2, y + eiA.h/2, eiA.w/2, 16, eiA.r, eiA.g, eiA.b, eiA.a);
            } else if ("LINE".equals(eiA.rt)) {
                renderer.drawLine(x, y, x + eiA.w, y + eiA.h, eiA.r, eiA.g, eiA.b, eiA.a);
            } else {
                // Custom or default
                renderer.drawRect(x, y, 20, 20, eiA.r, eiA.g, eiA.b, eiA.a);
            }
        }
        
        renderer.drawText("Replay Time: " + String.format("%.2f", time), 10, 20, 12, 1, 1, 1, 1);
    }
}
