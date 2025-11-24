package com.gameengine.core;

import com.gameengine.graphics.Renderer;
import com.gameengine.input.InputManager;
import com.gameengine.scene.Scene;
import javax.swing.Timer;

/**
 * 游戏引擎
 */
public class GameEngine {
    private Renderer renderer;
    private InputManager inputManager;
    private Scene currentScene;
    private boolean running;
    private float targetFPS;
    private float deltaTime;
    private long lastTime;
    private String title;
    private Timer gameTimer;
    // 控制 FPS 显示更新频率：每 interval 秒更新一次显示值，防止数字抖动过快
    private double fpsDisplayInterval = 0.3; // 秒（默认 0.3s）
    private double fpsDisplayAccumTime = 0.0; // 累计时间
    private int fpsDisplayAccumFrames = 0;    // 累计帧数
    private double displayedFps = 60.0;       // 当前用于显示的 FPS 值
    private com.gameengine.recording.RecordingService recordingService;
    
    public GameEngine(int width, int height, String title) {
        this.title = title;
        this.renderer = new Renderer(width, height, title);
        this.inputManager = InputManager.getInstance();
        this.running = false;
        this.targetFPS = 60.0f;
        this.deltaTime = 0.0f;
        this.lastTime = System.nanoTime();
    }
    
    /**
     * 初始化游戏引擎
     */
    public boolean initialize() {
        return true; // Swing渲染器不需要特殊初始化
    }
    
    /**
     * 运行游戏引擎
     */
    public void run() {
        if (!initialize()) {
            System.err.println("游戏引擎初始化失败");
            return;
        }
        
        running = true;
        
        // 初始化当前场景
        if (currentScene != null) {
            currentScene.initialize();
        }
        
        // 创建游戏循环定时器
        gameTimer = new Timer((int) (1000 / targetFPS), e -> {
            if (running) {
                update();
                render();
            }
        });
        
        gameTimer.start();
    }
    
    /**
     * 更新游戏逻辑
     */
    private void update() {
        // 计算时间间隔
        long currentTime = System.nanoTime();
        deltaTime = (currentTime - lastTime) / 1_000_000_000.0f; // 转换为秒
        lastTime = currentTime;

        // FPS 显示节流/平均：每 fpsDisplayInterval 秒计算一次平均 FPS 并更新 displayedFps
        fpsDisplayAccumTime += deltaTime;
        fpsDisplayAccumFrames += 1;
        if (fpsDisplayAccumTime >= fpsDisplayInterval) {
            // 计算这段时间的平均 FPS
            displayedFps = fpsDisplayAccumFrames / Math.max(1e-6, fpsDisplayAccumTime);
            // 重置累积器
            fpsDisplayAccumTime = 0.0;
            fpsDisplayAccumFrames = 0;
        }
        
        // 更新场景
        if (currentScene != null) {
            currentScene.update(deltaTime);
        }

        if (recordingService != null && recordingService.isRecording()) {
            recordingService.update(deltaTime, currentScene, inputManager);
        }
        
        // 处理事件
        renderer.pollEvents();
        
        // 检查退出条件
        // if (inputManager.isKeyPressed(27)) { // ESC键
        //     running = false;
        //     gameTimer.stop();
        //     renderer.cleanup();
        // }
        
        // 检查窗口是否关闭
        if (renderer.shouldClose()) {
            stop();
        }

        // 在帧结束时清理一次刚按下的标记，保证 scene.update() 中可以读取到 "just pressed"
        inputManager.update();
    }

    /**
     * 获取用于显示的平滑/节流后的 FPS（更新频率由 fpsDisplayInterval 控制）
     */
    public double getDisplayedFps() {
        return displayedFps;
    }

    /**
     * 设置 FPS 显示更新间隔（秒），例如 0.3 表示每 0.3s 刷新一次显示值
     */
    public void setFpsDisplayInterval(double seconds) {
        if (seconds <= 0) return;
        this.fpsDisplayInterval = seconds;
    }
    
    /**
     * 渲染游戏
     */
    private void render() {
        renderer.beginFrame();
        
        // 渲染场景
        if (currentScene != null) {
            currentScene.render();
        }
        
        renderer.endFrame();
    }
    
    /**
     * 设置当前场景
     */
    public void setScene(Scene scene) {
        this.currentScene = scene;
        if (scene != null && running) {
            scene.initialize();
        }
    }
    
    /**
     * 获取当前场景
     */
    public Scene getCurrentScene() {
        return currentScene;
    }
    
    /**
     * 停止游戏引擎
     */
    public void stop() {
        running = false;
        if (gameTimer != null) {
            gameTimer.stop();
        }
        cleanup();
    }
    
    /**
     * 清理资源
     */
    private void cleanup() {
        if (recordingService != null && recordingService.isRecording()) {
            try { recordingService.stop(); } catch (Exception ignored) {}
        }
        if (currentScene != null) {
            currentScene.clear();
        }
        renderer.cleanup();
    }

    public void enableRecording(com.gameengine.recording.RecordingService service) {
        // Stop existing recording if any
        if (this.recordingService != null && this.recordingService.isRecording()) {
            try { this.recordingService.stop(); } catch (Exception ignored) {}
        }
        
        this.recordingService = service;
        try {
            if (service != null && currentScene != null) {
                service.start(currentScene, renderer.getWidth(), renderer.getHeight());
            }
        } catch (Exception e) {
            System.err.println("录制启动失败: " + e.getMessage());
        }
    }

    public void disableRecording() {
        if (recordingService != null && recordingService.isRecording()) {
            try { recordingService.stop(); } catch (Exception ignored) {}
        }
        recordingService = null;
    }
    
    /**
     * 获取渲染器
     */
    public Renderer getRenderer() {
        return renderer;
    }
    
    /**
     * 获取输入管理器
     */
    public InputManager getInputManager() {
        return inputManager;
    }
    
    /**
     * 获取时间间隔
     */
    public float getDeltaTime() {
        return deltaTime;
    }
    
    /**
     * 设置目标帧率
     */
    public void setTargetFPS(float fps) {
        this.targetFPS = fps;
        if (gameTimer != null) {
            gameTimer.setDelay((int) (1000 / fps));
        }
    }
    
    /**
     * 获取目标帧率
     */
    public float getTargetFPS() {
        return targetFPS;
    }
    
    /**
     * 检查引擎是否正在运行
     */
    public boolean isRunning() {
        return running;
    }

    public com.gameengine.recording.RecordingService getRecordingService() {
        return recordingService;
    }
}
