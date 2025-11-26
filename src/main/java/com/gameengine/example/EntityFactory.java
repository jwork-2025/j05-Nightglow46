package com.gameengine.example;

import com.gameengine.components.RenderComponent;
import com.gameengine.components.TransformComponent;
import com.gameengine.core.GameObject;
import com.gameengine.graphics.IRenderer;
import com.gameengine.math.Vector2;

public final class EntityFactory {
    private EntityFactory() {}

    public static GameObject createPlayerVisual(IRenderer renderer) {
        return new GameObject("Player") {
            private Vector2 basePosition;
            @Override
            public void update(float dt) {
                super.update(dt);
                TransformComponent tc = getComponent(TransformComponent.class);
                if (tc != null) basePosition = tc.getPosition();
            }
            @Override
            public void render() {
                if (basePosition == null) return;
                renderer.drawRect(basePosition.x - 8, basePosition.y - 10, 16, 20, 1.0f, 0.0f, 0.0f, 1.0f);
                renderer.drawRect(basePosition.x - 6, basePosition.y - 22, 12, 12, 1.0f, 0.5f, 0.0f, 1.0f);
                renderer.drawRect(basePosition.x - 13, basePosition.y - 5, 6, 12, 1.0f, 0.8f, 0.0f, 1.0f);
                renderer.drawRect(basePosition.x + 7, basePosition.y - 5, 6, 12, 0.0f, 1.0f, 0.0f, 1.0f);
            }
        };
    }

    public static GameObject createAIVisual(IRenderer renderer, float w, float h, float r, float g, float b, float a) {
        GameObject obj = new GameObject("AIPlayer");
        TransformComponent tc = obj.addComponent(new TransformComponent(new Vector2(0, 0)));
        RenderComponent rc = obj.addComponent(new RenderComponent(
            RenderComponent.RenderType.RECTANGLE,
            new Vector2(Math.max(1, w), Math.max(1, h)),
            new RenderComponent.Color(r, g, b, a)
        ));
        rc.setRenderer(renderer);
        return obj;
    }
}


