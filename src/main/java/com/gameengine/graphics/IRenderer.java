package com.gameengine.graphics;

public interface IRenderer {
    void beginFrame();
    void endFrame();
    
    void drawRect(float x, float y, float width, float height, float r, float g, float b, float a);
    void drawCircle(float x, float y, float radius, int segments, float r, float g, float b, float a);
    void drawLine(float x1, float y1, float x2, float y2, float r, float g, float b, float a);
    void drawText(float x, float y, String text, float r, float g, float b, float a);
    
    boolean shouldClose();
    void pollEvents();
    void cleanup();
    
    int getWidth();
    int getHeight();
    String getTitle();
}

