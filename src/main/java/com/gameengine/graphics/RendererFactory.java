package com.gameengine.graphics;

public class RendererFactory {
    public static IRenderer createRenderer(RenderBackend backend, int width, int height, String title) {
        if (backend == RenderBackend.GPU) {
            return new GPURenderer(width, height, title);
        }
        throw new IllegalArgumentException("不支持的渲染后端: " + backend);
    }
}

