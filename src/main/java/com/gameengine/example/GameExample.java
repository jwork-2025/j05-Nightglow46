package com.gameengine.example;

import com.gameengine.core.GameEngine;
import com.gameengine.scene.MenuScene;

/**
 * 游戏示例
 */
public class GameExample {
    public static void main(String[] args) {
        System.out.println("启动游戏引擎...");
        
        try {
            // 创建游戏引擎
            GameEngine engine = new GameEngine(800, 600, "游戏引擎");
            
            // 设置场景
            engine.setScene(new MenuScene(engine));
            
            // 运行游戏
            engine.run();
            
        } catch (Exception e) {
            System.err.println("游戏运行出错: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("游戏结束");
    }
}

