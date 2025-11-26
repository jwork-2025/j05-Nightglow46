package com.gameengine.example;

import com.gameengine.core.GameEngine;
import com.gameengine.graphics.RenderBackend;

import java.io.File;
import java.util.Arrays;

public class ReplayLauncher {
    public static void main(String[] args) {
        String path = null;
        if (args != null && args.length > 0) {
            path = args[0];
        } else {
            File dir = new File("recordings");
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) -> name.endsWith(".json") || name.endsWith(".jsonl"));
                if (files != null && files.length > 0) {
                    Arrays.sort(files, (a,b) -> Long.compare(b.lastModified(), a.lastModified()));
                    path = files[0].getAbsolutePath();
                    
                }
            }
        }

        if (path == null) {
            
            return;
        }

        GameEngine engine = new GameEngine(1024, 768, "Replay", RenderBackend.GPU);
        ReplayScene replay = new ReplayScene(engine, path);
        engine.setScene(replay);
        engine.run();
        engine.cleanup();
    }
}


