package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.core.GameObject;
import com.gameengine.math.Vector2;

/**
 * 变换组件，管理位置、旋转、缩放
 */
public class TransformComponent extends Component<TransformComponent> {
    private Vector2 position;
    private Vector2 scale;
    private float rotation;
    
    public TransformComponent() {
        this.position = new Vector2();
        this.scale = new Vector2(1, 1);
        this.rotation = 0;
    }
    
    public TransformComponent(Vector2 position) {
        this();
        this.position = new Vector2(position);
    }
    
    public TransformComponent(Vector2 position, Vector2 scale, float rotation) {
        this.position = new Vector2(position);
        this.scale = new Vector2(scale);
        this.rotation = rotation;
    }
    
    @Override
    public void initialize() {
        // 初始化变换组件
    }
    
    @Override
    public void update(float deltaTime) {
        // 变换组件通常不需要每帧更新
    }
    
    @Override
    public void render() {
        // 变换组件不直接渲染
    }
    
    /**
     * 移动到指定位置
     */
    public void moveTo(Vector2 newPosition) {
        this.position = new Vector2(newPosition);
    }
    
    /**
     * 移动相对距离
     */
    public void translate(Vector2 delta) {
        this.position = position.add(delta);
    }
    
    /**
     * 旋转指定角度
     */
    public void rotate(float angle) {
        this.rotation += angle;
    }
    
    /**
     * 设置旋转角度
     */
    public void setRotation(float angle) {
        this.rotation = angle;
    }
    
    /**
     * 缩放
     */
    public void scale(Vector2 scaleFactor) {
        this.scale = new Vector2(this.scale.x * scaleFactor.x, this.scale.y * scaleFactor.y);
    }
    
    /**
     * 设置缩放
     */
    public void setScale(Vector2 newScale) {
        this.scale = new Vector2(newScale);
    }
    
    // Getters and Setters
    public Vector2 getPosition() {
        return new Vector2(position);
    }
    
    public void setPosition(Vector2 position) {
        this.position = new Vector2(position);
    }
    
    public Vector2 getScale() {
        return new Vector2(scale);
    }
    
    public float getRotation() {
        return rotation;
    }
}
