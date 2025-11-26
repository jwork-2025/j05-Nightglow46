package com.gameengine.scene;

import com.gameengine.core.GameEngine;
import com.gameengine.graphics.Renderer;
import com.gameengine.input.InputManager;
import com.gameengine.recording.RecordingConfig;
import com.gameengine.recording.RecordingService;
import java.awt.event.KeyEvent;
import java.io.File;

public class MenuScene extends Scene {
    private GameEngine engine;
    private String[] options = {"Start Game", "Load Game", "Replay", "Exit"};
    private int selectedIndex = 0;
    private Renderer renderer;
    private InputManager input;

    public MenuScene(GameEngine engine) {
        super("MenuScene");
        this.engine = engine;
    }

    @Override
    public void initialize() {
        super.initialize();
        this.renderer = engine.getRenderer();
        this.input = engine.getInputManager();
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        
        if (input.isKeyJustPressed(KeyEvent.VK_UP)) {
            selectedIndex--;
            if (selectedIndex < 0) selectedIndex = options.length - 1;
        }
        if (input.isKeyJustPressed(KeyEvent.VK_DOWN)) {
            selectedIndex++;
            if (selectedIndex >= options.length) selectedIndex = 0;
        }
        
        if (input.isKeyJustPressed(KeyEvent.VK_ENTER)) {
            selectOption();
        }
    }

    private void selectOption() {
        switch (selectedIndex) {
            case 0: // Start Game
                startNewGame(false);
                break;
            case 1: // Load Game
                startNewGame(true);
                break;
            case 2: // Replay
                engine.disableRecording();
                engine.setScene(new com.gameengine.example.ReplayScene(engine, null));
                break;
            case 3: // Exit
                engine.stop();
                System.exit(0);
                break;
        }
    }

    private void startNewGame(boolean loadFromSave) {
        try {
            new File("recordings").mkdirs();
            String path = "recordings/session_" + System.currentTimeMillis() + ".jsonl";
            RecordingConfig cfg = new RecordingConfig(path);
            RecordingService svc = new RecordingService(cfg);
            // Note: We can't set extra data callback here easily because GameScene isn't created yet
            // But j05 doesn't seem to use extra data callback in MenuScene either.
            // If we need score/hp in recording, we might need to pass the service to GameScene or have GameScene configure it.
            // For now, let's just enable it.
            engine.enableRecording(svc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        engine.setScene(new com.gameengine.example.GameScene(engine, loadFromSave));
    }

    @Override
    public void render() {
        super.render();
        // Draw menu
        float startY = 200;
        for (int i = 0; i < options.length; i++) {
            float r = 1, g = 1, b = 1;
            if (i == selectedIndex) {
                r = 1; g = 1; b = 0; // Yellow for selected
            }
            renderer.drawText(options[i], 300, startY + i * 50, 30, r, g, b, 1);
        }
        
        renderer.drawText("J03 Game - Nightglow46", 250, 100, 40, 0, 1, 1, 1);
    }
}
