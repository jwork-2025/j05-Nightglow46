package com.gameengine.example;

import com.gameengine.core.GameEngine;
import com.gameengine.graphics.IRenderer;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;
import com.gameengine.recording.RecordingConfig;
import com.gameengine.recording.RecordingService;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MenuScene extends Scene {
    public enum MenuOption {
        START_GAME,
        REPLAY,
        EXIT
    }
    
    private IRenderer renderer;
    private InputManager inputManager;
    private GameEngine engine;
    private int selectedIndex;
    private MenuOption[] options;
    private boolean selectionMade;
    private MenuOption selectedOption;
    private List<String> replayFiles;
    private boolean showReplayInfo;
    private int debugFrames;
    
    public MenuScene(GameEngine engine, String name) {
        super(name);
        this.engine = engine;
        this.renderer = engine.getRenderer();
        this.inputManager = InputManager.getInstance();
        this.selectedIndex = 0;
        this.options = new MenuOption[]{MenuOption.START_GAME, MenuOption.REPLAY, MenuOption.EXIT};
        this.selectionMade = false;
        this.selectedOption = null;
        this.replayFiles = new ArrayList<>();
        this.showReplayInfo = false;
    }
    
    private void loadReplayFiles() {}
    
    @Override
    public void initialize() {
        super.initialize();
        loadReplayFiles();
        selectedIndex = 0;
        selectionMade = false;
        debugFrames = 0;
        
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        
        handleMenuSelection();
        
        if (selectionMade) {
            processSelection();
        }
    }
    
    private void handleMenuSelection() {
        if (inputManager.isKeyJustPressed(38)) {
            selectedIndex = (selectedIndex - 1 + options.length) % options.length;
        } else if (inputManager.isKeyJustPressed(40)) {
            selectedIndex = (selectedIndex + 1) % options.length;
        } else if (inputManager.isKeyJustPressed(10) || inputManager.isKeyJustPressed(32)) {
            selectionMade = true;
            selectedOption = options[selectedIndex];
            
            if (selectedOption == MenuOption.REPLAY) {
                engine.disableRecording();
                Scene replay = new ReplayScene(engine, null);
                engine.setScene(replay);
            } else if (selectedOption == MenuOption.EXIT) {
                engine.stop();
                engine.cleanup();
                System.exit(0);
            }
        }
        
        Vector2 mousePos = inputManager.getMousePosition();
        if (inputManager.isMouseButtonJustPressed(0)) {
            float centerY = renderer.getHeight() / 2.0f;
            float buttonY1 = centerY - 80;
            float buttonY2 = centerY + 0;
            float buttonY3 = centerY + 80;
            
            if (mousePos.y >= buttonY1 - 30 && mousePos.y <= buttonY1 + 30) {
                selectedIndex = 0;
                selectionMade = true;
                selectedOption = MenuOption.START_GAME;
            } else if (mousePos.y >= buttonY2 - 30 && mousePos.y <= buttonY2 + 30) {
                selectedIndex = 1;
                selectedOption = MenuOption.REPLAY;
                engine.disableRecording();
                Scene replay = new ReplayScene(engine, null);
                engine.setScene(replay);
            } else if (mousePos.y >= buttonY3 - 30 && mousePos.y <= buttonY3 + 30) {
                selectedIndex = 2;
                selectionMade = true;
                selectedOption = MenuOption.EXIT;
                engine.stop();
                engine.cleanup();
                System.exit(0);
            }
        }
    }

    private String findLatestRecording() {
        File dir = new File("recordings");
        if (!dir.exists() || !dir.isDirectory()) return null;
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json") || name.endsWith(".jsonl"));
        if (files == null || files.length == 0) return null;
        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return files[0].getAbsolutePath();
    }
    
    private void processSelection() {
        if (selectedOption == MenuOption.START_GAME) {
            switchToGameScene();
        }
    }
    
    private void switchToGameScene() {
        Scene gameScene = new GameScene(engine);
        engine.setScene(gameScene);
        try {
            new File("recordings").mkdirs();
            String path = "recordings/session_" + System.currentTimeMillis() + ".jsonl";
            RecordingConfig cfg = new RecordingConfig(path);
            RecordingService svc = new RecordingService(cfg);
            engine.enableRecording(svc);
        } catch (Exception e) {
            
        }
    }
    
    private void switchToReplayScene() {}
    
    @Override
    public void render() {
        if (renderer == null) return;
        
        int width = renderer.getWidth();
        int height = renderer.getHeight();
        if (debugFrames < 5) {
            
            debugFrames++;
        }
        
        renderer.drawRect(0, 0, width, height, 0.25f, 0.25f, 0.35f, 1.0f);
        
        super.render();
        
        renderMainMenu();
    }
    
    private void renderMainMenu() {
        if (renderer == null) return;
        
        int width = renderer.getWidth();
        int height = renderer.getHeight();
        
        float centerX = width / 2.0f;
        float centerY = height / 2.0f;
        
        String title = "GAME ENGINE";
        float titleWidth = title.length() * 20.0f;
        float titleX = centerX - titleWidth / 2.0f;
        float titleY = 120.0f;
        
        renderer.drawRect(centerX - titleWidth / 2.0f - 20, titleY - 40, titleWidth + 40, 80, 0.4f, 0.4f, 0.5f, 1.0f);
        renderer.drawText(titleX, titleY, title, 1.0f, 1.0f, 1.0f, 1.0f);
        
        for (int i = 0; i < options.length; i++) {
            String text = "";
            if (options[i] == MenuOption.START_GAME) {
                text = "START GAME";
            } else if (options[i] == MenuOption.REPLAY) {
                text = "REPLAY";
            } else if (options[i] == MenuOption.EXIT) {
                text = "EXIT";
            }
            
            float textWidth = text.length() * 20.0f;
            float textX = centerX - textWidth / 2.0f;
            float textY = centerY - 80.0f + i * 80.0f;
            
            float r, g, b;
            
            if (i == selectedIndex) {
                r = 1.0f;
                g = 1.0f;
                b = 0.5f;
                renderer.drawRect(textX - 20, textY - 20, textWidth + 40, 50, 0.6f, 0.5f, 0.2f, 0.9f);
            } else {
                r = 0.95f;
                g = 0.95f;
                b = 0.95f;
                renderer.drawRect(textX - 20, textY - 20, textWidth + 40, 50, 0.2f, 0.2f, 0.3f, 0.5f);
            }
            
            renderer.drawText(textX, textY, text, r, g, b, 1.0f);
        }
        
        String hint1 = "USE ARROWS OR MOUSE TO SELECT, ENTER TO CONFIRM";
        float hint1Width = hint1.length() * 20.0f;
        float hint1X = centerX - hint1Width / 2.0f;
        renderer.drawText(hint1X, height - 100, hint1, 0.6f, 0.6f, 0.6f, 1.0f);
        
        String hint2 = "ESC TO EXIT";
        float hint2Width = hint2.length() * 20.0f;
        float hint2X = centerX - hint2Width / 2.0f;
        renderer.drawText(hint2X, height - 70, hint2, 0.6f, 0.6f, 0.6f, 1.0f);

        if (showReplayInfo) {
            String info = "REPLAY COMING SOON";
            float w = info.length() * 20.0f;
            renderer.drawText(centerX - w / 2.0f, height - 140, info, 0.9f, 0.8f, 0.2f, 1.0f);
        }
    }
    
    
}

