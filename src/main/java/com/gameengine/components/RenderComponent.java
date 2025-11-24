package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.core.GameObject;
import com.gameengine.graphics.Renderer;
import com.gameengine.math.Vector2;

/**
 * 渲染组件，负责对象的渲染
 */
public class RenderComponent extends Component<RenderComponent> {
    private Renderer renderer;
    private RenderType renderType;
    private Vector2 size;
    private Color color;
    private boolean visible;
    
    public enum RenderType {
        RECTANGLE,
        CIRCLE,
        LINE
    }
    
    public static class Color {
        public float r, g, b, a;
        
        public Color(float r, float g, float b, float a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }
        
        public Color(float r, float g, float b) {
            this(r, g, b, 1.0f);
        }
    }
    
    public RenderComponent() {
        this.renderType = RenderType.RECTANGLE;
        this.size = new Vector2(20, 20);
        this.color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
        this.visible = true;
    }
    
    public RenderComponent(RenderType renderType, Vector2 size, Color color) {
        this.renderType = renderType;
        this.size = new Vector2(size);
        this.color = color;
        this.visible = true;
    }
    
    @Override
    public void initialize() {
        // 获取渲染器引用
        if (owner != null) {
            // 这里需要从游戏引擎获取渲染器
            // 在实际实现中，可以通过依赖注入或其他方式获取
        }
    }
    
    @Override
    public void update(float deltaTime) {
        // 渲染组件通常不需要每帧更新
    }
    
    @Override
    public void render() {
        if (!visible || renderer == null) {
            return;
        }
        
        TransformComponent transform = owner.getComponent(TransformComponent.class);
        if (transform == null) {
            return;
        }
        
        Vector2 position = transform.getPosition();
        
        switch (renderType) {
            case RECTANGLE:
                renderer.drawRect(position.x, position.y, size.x, size.y, 
                                color.r, color.g, color.b, color.a);
                break;
            case CIRCLE:
                renderer.drawCircle(position.x + size.x/2, position.y + size.y/2, 
                                  size.x/2, 16, color.r, color.g, color.b, color.a);
                break;
            case LINE:
                renderer.drawLine(position.x, position.y, 
                                position.x + size.x, position.y + size.y,
                                color.r, color.g, color.b, color.a);
                break;
        }
    }
    
    /**
     * 设置渲染器
     */
    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }
    
    /**
     * 设置颜色
     */
    public void setColor(Color color) {
        this.color = color;
    }
    
    /**
     * 设置颜色
     */
    public void setColor(float r, float g, float b, float a) {
        this.color = new Color(r, g, b, a);
    }
    
    /**
     * 设置大小
     */
    public void setSize(Vector2 size) {
        this.size = new Vector2(size);
    }
    
    /**
     * 设置可见性
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    // Getters
    public RenderType getRenderType() {
        return renderType;
    }
    
    public Vector2 getSize() {
        return new Vector2(size);
    }
    
    public Color getColor() {
        return color;
    }
    
    public boolean isVisible() {
        return visible;
    }
}
