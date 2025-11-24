package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.core.GameObject;
import com.gameengine.math.Vector2;

/**
 * 物理组件，处理物理运动
 */
public class PhysicsComponent extends Component<PhysicsComponent> {
    private Vector2 velocity;
    private Vector2 acceleration;
    private float mass;
    private float friction;
    private boolean useGravity;
    private Vector2 gravity;
    
    public PhysicsComponent() {
        this.velocity = new Vector2();
        this.acceleration = new Vector2();
        this.mass = 1.0f;
        this.friction = 0.9f;
        this.useGravity = false;
        this.gravity = new Vector2(0, 9.8f);
    }
    
    public PhysicsComponent(float mass) {
        this();
        this.mass = mass;
    }
    
    @Override
    public void initialize() {
        // 初始化物理组件
    }
    
    @Override
    public void update(float deltaTime) {
        if (!enabled) return;
        
        TransformComponent transform = owner.getComponent(TransformComponent.class);
        if (transform == null) return;
        
        // 应用重力
        if (useGravity) {
            acceleration = acceleration.add(gravity);
        }
        
        // 更新速度
        velocity = velocity.add(acceleration.multiply(deltaTime));
        
        // 应用摩擦力
        velocity = velocity.multiply(friction);
        
        // 更新位置
        Vector2 deltaPosition = velocity.multiply(deltaTime);
        transform.translate(deltaPosition);
        
        // 重置加速度
        acceleration = new Vector2();
    }
    
    @Override
    public void render() {
        // 物理组件不直接渲染
    }
    
    /**
     * 应用力
     */
    public void applyForce(Vector2 force) {
        if (mass > 0) {
            acceleration = acceleration.add(force.multiply(1.0f / mass));
        }
    }
    
    /**
     * 应用冲量
     */
    public void applyImpulse(Vector2 impulse) {
        if (mass > 0) {
            velocity = velocity.add(impulse.multiply(1.0f / mass));
        }
    }
    
    /**
     * 设置速度
     */
    public void setVelocity(Vector2 velocity) {
        this.velocity = new Vector2(velocity);
    }
    
    /**
     * 设置速度
     */
    public void setVelocity(float x, float y) {
        this.velocity = new Vector2(x, y);
    }
    
    /**
     * 添加速度
     */
    public void addVelocity(Vector2 delta) {
        this.velocity = velocity.add(delta);
    }
    
    /**
     * 设置重力
     */
    public void setGravity(Vector2 gravity) {
        this.gravity = new Vector2(gravity);
    }
    
    /**
     * 启用/禁用重力
     */
    public void setUseGravity(boolean useGravity) {
        this.useGravity = useGravity;
    }
    
    /**
     * 设置摩擦力
     */
    public void setFriction(float friction) {
        this.friction = Math.max(0, Math.min(1, friction));
    }
    
    /**
     * 设置质量
     */
    public void setMass(float mass) {
        this.mass = Math.max(0.1f, mass);
    }
    
    // Getters
    public Vector2 getVelocity() {
        return new Vector2(velocity);
    }
    
    public Vector2 getAcceleration() {
        return new Vector2(acceleration);
    }
    
    public float getMass() {
        return mass;
    }
    
    public float getFriction() {
        return friction;
    }
    
    public boolean isUseGravity() {
        return useGravity;
    }
    
    public Vector2 getGravity() {
        return new Vector2(gravity);
    }
}
