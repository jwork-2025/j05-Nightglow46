package com.gameengine.example;

import com.gameengine.components.*;
import com.gameengine.core.*;
import com.gameengine.graphics.Renderer;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;
import com.gameengine.scene.MenuScene;
import com.gameengine.recording.*;

import java.util.Random;
import java.util.List;
import java.io.File;

public class GameScene extends Scene {
    private GameEngine engine;
    private Renderer renderer;
    private Random random;
    private float time;
    private GameLogic gameLogic;
    private boolean loadFromSave;
    private GameObject player; // Add player field

    private String replayPath;
    private java.util.Queue<String> replayEvents;
    private float replayTime;

    private boolean isRestoring = false; // Flag to prevent duplicate spawns

    public GameScene(GameEngine engine, boolean loadFromSave) {
        this(engine, loadFromSave, null);
    }

    public GameScene(GameEngine engine, boolean loadFromSave, String replayPath) {
        super("GameScene");
        this.engine = engine;
        this.loadFromSave = loadFromSave;
        this.replayPath = replayPath;
    }

    @Override
    public void initialize() {
        super.initialize();
        this.renderer = engine.getRenderer();
        this.random = new Random();
        this.time = 0;
        this.gameLogic = new GameLogic(this);
        
        if (replayPath != null) {
            // Replay Mode Initialization
            loadReplayEvents();
            createPlayer();
            // Do NOT create initial enemies/decorations randomly.
            // They will be created by spawn events.
            engine.getInputManager().clear();
            engine.getInputManager().setIgnoreHardwareInput(true);
        } else {
            // Normal Mode
            engine.getInputManager().setIgnoreHardwareInput(false);
            createPlayer();
            if (loadFromSave) {
                restoreState();
            } else {
                createEnemies();
                createDecorations();
            }
        }

        // Configure recording service if available
        if (engine.getRecordingService() != null) {
            engine.getRecordingService().setExtraDataCallback(() -> 
                "\"score\":" + gameLogic.getScore() + ",\"hp\":" + gameLogic.getPlayerHp()
            );
        }
    }

    private void loadReplayEvents() {
        replayEvents = new java.util.LinkedList<>();
        FileRecordingStorage storage = new FileRecordingStorage();
        try {
            for (String line : storage.readLines(replayPath)) {
                String type = RecordingJson.stripQuotes(RecordingJson.field(line, "type"));
                if ("input".equals(type) || "spawn".equals(type) || "end".equals(type)) {
                    replayEvents.add(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void restoreState() {
        FileRecordingStorage storage = new FileRecordingStorage();
        List<File> files = storage.listRecordings();
        
        if (files.isEmpty()) {
            System.out.println("No recordings found.");
            createEnemies();
            createDecorations();
            return;
        }

        // Sort by filename (timestamp) descending to ensure strict chronological order
        // Filename format: session_123456789.jsonl
        files.sort((f1, f2) -> f2.getName().compareTo(f1.getName()));

        // Identify the current recording file to avoid reading it
        String currentPath = null;
        if (engine.getRecordingService() != null) {
            currentPath = new File(engine.getRecordingService().getConfig().outputPath).getAbsolutePath();
        }

        File loadedFile = null;
        String lastKeyframeLine = null;

        for (File file : files) {
            // Skip the file currently being written
            if (currentPath != null && file.getAbsolutePath().equals(currentPath)) {
                continue;
            }
            
            // Skip empty/small files
            if (file.length() < 100) continue;

            try {
                // Read backwards to find the last keyframe
                List<String> lines = (List<String>) storage.readLines(file.getPath());
                for (int j = lines.size() - 1; j >= 0; j--) {
                    String line = lines.get(j);
                    if (line.contains("\"type\":\"keyframe\"") && line.contains("Player")) {
                        lastKeyframeLine = line;
                        loadedFile = file;
                        break;
                    }
                }
                
                if (lastKeyframeLine != null) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (lastKeyframeLine != null) {
            System.out.println("Restoring from: " + loadedFile.getName());
            try {
                // 1. Restore GameLogic state
                String scoreStr = RecordingJson.field(lastKeyframeLine, "score");
                String hpStr = RecordingJson.field(lastKeyframeLine, "hp");
                if (scoreStr != null) gameLogic.setScore((int)Double.parseDouble(scoreStr));
                if (hpStr != null) gameLogic.setPlayerHp((int)Double.parseDouble(hpStr));

                // 2. Restore Entities
                int entStart = lastKeyframeLine.indexOf("\"entities\":");
                if (entStart != -1) {
                    int arrStart = lastKeyframeLine.indexOf("[", entStart);
                    if (arrStart != -1) {
                        String entitiesJson = RecordingJson.extractArray(lastKeyframeLine, arrStart);
                        String[] entityItems = RecordingJson.splitTopLevel(entitiesJson);
                        
                        // Clear default/existing objects
                        for (GameObject obj : getGameObjects()) {
                            if (!obj.getName().startsWith("Player")) {
                                removeGameObject(obj);
                            }
                        }

                        isRestoring = true; // Set flag to true before restoring entities
                        try {
                            for (String item : entityItems) {
                                String id = RecordingJson.stripQuotes(RecordingJson.field(item, "id"));
                                double x = RecordingJson.parseDouble(RecordingJson.field(item, "x"));
                                double y = RecordingJson.parseDouble(RecordingJson.field(item, "y"));
                                
                                if (id.startsWith("Player")) {
                                    if (player != null) {
                                        TransformComponent tc = player.getComponent(TransformComponent.class);
                                        if (tc != null) tc.setPosition(new Vector2((float)x, (float)y));
                                    }
                                } else if (id.startsWith("Enemy")) {
                                    createEnemy(new Vector2((float)x, (float)y));
                                } else if (id.startsWith("Decoration")) {
                                    createDecoration(new Vector2((float)x, (float)y));
                                }
                            }
                        } finally {
                            isRestoring = false; // Reset flag after restoring entities
                        }
                    }
                }

                // Continue recording from this point
                if (engine.getRecordingService() != null) {
                    engine.getRecordingService().continueRecording(loadedFile, lastKeyframeLine);
                }
            } catch (Exception e) {
                System.err.println("Failed to restore state");
                e.printStackTrace();
            }
        } else {
            System.out.println("No valid save state found.");
            createEnemies();
            createDecorations();
        }
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        time += deltaTime;
        
        if (replayPath != null) {
            // Replay Logic
            replayTime += deltaTime;
            while (!replayEvents.isEmpty()) {
                String line = replayEvents.peek();
                double t = RecordingJson.parseDouble(RecordingJson.field(line, "t"));
                // System.out.println("Replay check: t=" + t + " replayTime=" + replayTime);
                if (t <= replayTime) {
                    replayEvents.poll(); // Consume event
                    if (processReplayEvent(line)) return;
                } else {
                    break; // Wait for time
                }
            }
            
            if (replayEvents.isEmpty()) {
                 // End of replay (fallback for old recordings or missing end event)
                 engine.getInputManager().setIgnoreHardwareInput(false);
                 engine.setScene(new MenuScene(engine));
                 return;
            }
        }

        // Allow ESC to exit replay
        if (engine.getInputManager().isKeyJustPressed(java.awt.event.KeyEvent.VK_ESCAPE)) {
            engine.disableRecording();
            engine.getInputManager().setIgnoreHardwareInput(false);
            engine.setScene(new MenuScene(engine));
            return;
        }


        // Game Logic
        gameLogic.handlePlayerInput();
        gameLogic.updatePhysics();
        gameLogic.checkCollisions();
        
        // Spawn enemies (Only if NOT replaying)
        if (replayPath == null && time > 2.0f) {
            createEnemy();
            time = 0;
        }
    }

    private boolean processReplayEvent(String line) {
        String type = RecordingJson.stripQuotes(RecordingJson.field(line, "type"));
        if ("end".equals(type)) {
            engine.getInputManager().setIgnoreHardwareInput(false);
            engine.setScene(new MenuScene(engine));
            return true;
        } else if ("input".equals(type)) {
            // Keys
            String keysStr = RecordingJson.field(line, "keys");
            if (keysStr != null) {
                // Remove brackets
                if (keysStr.startsWith("[")) keysStr = keysStr.substring(1, keysStr.length()-1);
                String[] keys = RecordingJson.splitTopLevel(keysStr);
                java.util.Set<Integer> keySet = new java.util.HashSet<>();
                for (String k : keys) {
                    try { keySet.add(Integer.parseInt(k)); } catch (Exception ignored) {}
                }
                engine.getInputManager().setPressedKeys(keySet);
            }

            // Mouse Position
            String mxStr = RecordingJson.field(line, "mx");
            String myStr = RecordingJson.field(line, "my");
            if (mxStr != null && myStr != null) {
                float mx = (float)RecordingJson.parseDouble(mxStr);
                float my = (float)RecordingJson.parseDouble(myStr);
                engine.getInputManager().injectMousePosition(mx, my);
            }

            // Mouse Buttons
            String mbStr = RecordingJson.field(line, "mb");
            if (mbStr != null) {
                if (mbStr.startsWith("[")) mbStr = mbStr.substring(1, mbStr.length()-1);
                String[] btns = mbStr.split(",");
                for (int i = 0; i < btns.length; i++) {
                    try {
                        boolean pressed = Integer.parseInt(btns[i].trim()) == 1;
                        engine.getInputManager().injectMouseButton(i, pressed);
                    } catch (Exception ignored) {}
                }
            }

        } else if ("spawn".equals(type)) {
            String id = RecordingJson.stripQuotes(RecordingJson.field(line, "id"));
            float x = (float)RecordingJson.parseDouble(RecordingJson.field(line, "x"));
            float y = (float)RecordingJson.parseDouble(RecordingJson.field(line, "y"));
            
            if (id != null) {
                if (id.startsWith("Enemy")) {
                    createEnemy(new Vector2(x, y));
                } else if (id.startsWith("Decoration")) {
                    createDecoration(new Vector2(x, y));
                }
            }
        }
        return false;
    }

    @Override
    public void render() {
        // Draw background
        renderer.drawRect(0, 0, 800, 600, 0.1f, 0.1f, 0.2f, 1.0f);
        
        super.render();

        // Draw UI
        GameObject playerObj = null;
        for (GameObject obj : getGameObjects()) {
            if (obj.getName().startsWith("Player")) { playerObj = obj; break; }
        }
        if (playerObj != null) {
            TransformComponent pt = playerObj.getComponent(TransformComponent.class);
            if (pt != null) {
                Vector2 ppos = pt.getPosition();
                float barWidth = 48f;
                float barHeight = 6f;
                float x = ppos.x - barWidth / 2f;
                float y = ppos.y - 30f;

                renderer.drawRect(x, y, barWidth, barHeight, 0.2f, 0.2f, 0.2f, 1.0f);
                float hpRatio = 1.0f;
                try {
                    hpRatio = (float) gameLogic.getPlayerHp() / (float) gameLogic.getPlayerMaxHp();
                } catch (Exception ignored) {}
                renderer.drawRect(x, y, barWidth * hpRatio, barHeight, 1.0f, 0.0f, 0.0f, 1.0f);
            }
        }

        renderer.drawText("Score: " + gameLogic.getScore(), 620, 40, 32, 1.0f, 1.0f, 0.0f, 1.0f);

        int enemyCount = 0;
        for (GameObject obj : getGameObjects()) {
            if (obj.getName().startsWith("Enemy")) enemyCount++;
        }
        renderer.drawText("num:" + enemyCount, 620, 80, 24, 1.0f, 1.0f, 1.0f, 1.0f);

        double fps = engine.getDisplayedFps();
        String fpsText = String.format("fps:%.1f FPS", fps);
        renderer.drawText(fpsText, 620, 104, 20, 0.8f, 0.8f, 0.8f, 1.0f);

        if (gameLogic.isGameOver()) {
            javax.swing.JOptionPane.showMessageDialog(null, "游戏失败");
            engine.disableRecording();
            engine.getInputManager().setIgnoreHardwareInput(false);
            engine.setScene(new MenuScene(engine));
        }
    }
    
    private void createPlayer() {
        player = new GameObject("Player") {
            private Vector2 basePosition;
            
            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);
                updateBodyParts();
            }
            
            @Override
            public void render() {
                renderBodyParts();
            }
            
            private void updateBodyParts() {
                TransformComponent transform = getComponent(TransformComponent.class);
                if (transform != null) {
                    basePosition = transform.getPosition();
                }
            }
            
            private void renderBodyParts() {
                if (basePosition == null) return;
                renderer.drawRect(basePosition.x - 8, basePosition.y - 10, 16, 20, 1.0f, 0.0f, 0.0f, 1.0f);
                renderer.drawRect(basePosition.x - 6, basePosition.y - 22, 12, 12, 1.0f, 0.5f, 0.0f, 1.0f);
                renderer.drawRect(basePosition.x - 13, basePosition.y - 5, 6, 12, 1.0f, 0.8f, 0.0f, 1.0f);
                renderer.drawRect(basePosition.x + 7, basePosition.y - 5, 6, 12, 0.0f, 1.0f, 0.0f, 1.0f);
            }
        };
        
        player.addComponent(new TransformComponent(new Vector2(400, 300)));
        PhysicsComponent physics = player.addComponent(new PhysicsComponent(1.0f));
        physics.setFriction(0.95f);
        
        addGameObject(player);
    }

    public void createBullet(Vector2 target) {
        GameObject player = null;
        for (GameObject obj : getGameObjects()) {
            if (obj.getName().startsWith("Player")) {
                player = obj;
                break;
            }
        }
        if (player == null) return;
        TransformComponent playerTransform = player.getComponent(TransformComponent.class);
        if (playerTransform == null) return;
        Vector2 start = playerTransform.getPosition();

        Vector2 direction = target.subtract(start).normalize();

        GameObject bullet = new GameObject("Bullet") {
            private float lifeTime = 0f;
            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);
                lifeTime += deltaTime;
            }
            @Override
            public void render() {
                renderer.drawRect(
                    getComponent(TransformComponent.class).getPosition().x - 5,
                    getComponent(TransformComponent.class).getPosition().y - 5,
                    10, 10,
                    0.2f, 0.8f, 1.0f, 1.0f
                );
            }
        };
        bullet.addComponent(new TransformComponent(start));
        PhysicsComponent physics = bullet.addComponent(new PhysicsComponent(1.0f));
        physics.setVelocity(direction.multiply(400));
        physics.setFriction(1.0f);
        addGameObject(bullet);
    }
    
    private void createEnemies() {
        for (int i = 0; i < 10; i++) { // Reduced initial count for performance
            createEnemy();
        }
    }
    
    private void createEnemy() {
        createEnemy(null);
    }

    private void createEnemy(Vector2 spawnPos) {
        GameObject enemy = new GameObject("Enemy") {
            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);

                GameObject player = null;
                List<GameObject> objects = getGameObjects();
                for (GameObject obj : objects) {
                    if (obj.getName().startsWith("Player")) {
                        player = obj;
                        break;
                    }
                }

                if (player != null) {
                    TransformComponent enemyTransform = getComponent(TransformComponent.class);
                    TransformComponent playerTransform = player.getComponent(TransformComponent.class);
                    PhysicsComponent physics = getComponent(PhysicsComponent.class);

                    if (enemyTransform != null && playerTransform != null && physics != null) {
                        Vector2 toPlayer = playerTransform.getPosition().subtract(enemyTransform.getPosition());
                        if (toPlayer.magnitude() > 1e-2) {
                            Vector2 direction = toPlayer.normalize();
                            float speed = 15f;
                            physics.setVelocity(direction.multiply(speed));
                        }
                    }
                }
            }

            @Override
            public void render() {
                renderComponents();
            }
        };
        
        Vector2 position = spawnPos;
        if (position == null) {
            final float MIN_SPAWN_DISTANCE = 150f;
            final float marginX = 10f;
            final float marginY = 10f;
            Vector2 playerPos = new Vector2(400, 300);
            for (GameObject obj : getGameObjects()) {
                if (obj.getName().startsWith("Player")) {
                    TransformComponent pt = obj.getComponent(TransformComponent.class);
                    if (pt != null) {
                        playerPos = pt.getPosition();
                    }
                    break;
                }
            }

            int maxAttempts = 20;
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                float px = marginX + random.nextFloat() * (800f - 2f * marginX);
                float py = marginY + random.nextFloat() * (600f - 2f * marginY);
                Vector2 cand = new Vector2(px, py);
                if (cand.distance(playerPos) >= MIN_SPAWN_DISTANCE) {
                    position = cand;
                    break;
                }
            }
            if (position == null) {
                float fallbackX = (playerPos.x < 400) ? 800f - marginX : marginX;
                float fallbackY = (playerPos.y < 300) ? 600f - marginY : marginY;
                position = new Vector2(fallbackX, fallbackY);
            }
        }
        
        enemy.addComponent(new TransformComponent(position));
        
        RenderComponent render = enemy.addComponent(new RenderComponent(
            RenderComponent.RenderType.RECTANGLE,
            new Vector2(20, 20),
            new RenderComponent.Color(1.0f, 0.5f, 0.0f, 1.0f)
        ));
        render.setRenderer(renderer);
        
        PhysicsComponent physics = enemy.addComponent(new PhysicsComponent(0.5f));
        physics.setVelocity(new Vector2(
            (random.nextFloat() - 0.5f) * 100,
            (random.nextFloat() - 0.5f) * 100
        ));
        physics.setFriction(0.98f);
        
        addGameObject(enemy);

        // Record spawn event if recording
        if (!isRestoring && engine.getRecordingService() != null && engine.getRecordingService().isRecording()) {
            engine.getRecordingService().recordSpawn("spawn", position.x, position.y, 
                enemy.getName() + "#" + enemy.getUuid(), 
                "RECTANGLE", 20, 20, 1.0f, 0.5f, 0.0f, 1.0f);
        }
    }
    
    private void createDecorations() {
        if (replayPath != null) return; // Don't create initial decorations in replay mode, wait for events
        for (int i = 0; i < 5; i++) {
            createDecoration(null);
        }
    }
    
    private void createDecoration(Vector2 spawnPos) {
        GameObject decoration = new GameObject("Decoration") {
            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);
            }
            
            @Override
            public void render() {
                renderComponents();
            }
        };
        
        Vector2 position = spawnPos;
        if (position == null) {
            position = new Vector2(
                random.nextFloat() * 800,
                random.nextFloat() * 600
            );
        }
        
        decoration.addComponent(new TransformComponent(position));
        
        RenderComponent render = decoration.addComponent(new RenderComponent(
            RenderComponent.RenderType.CIRCLE,
            new Vector2(5, 5),
            new RenderComponent.Color(0.5f, 0.5f, 1.0f, 0.8f)
        ));
        render.setRenderer(renderer);
        
        addGameObject(decoration);

        // Record spawn event if recording
        if (!isRestoring && engine.getRecordingService() != null && engine.getRecordingService().isRecording()) {
            engine.getRecordingService().recordSpawn("spawn", position.x, position.y, 
                decoration.getName() + "#" + decoration.getUuid(), 
                "CIRCLE", 5, 5, 0.5f, 0.5f, 1.0f, 0.8f);
        }
    }
}
