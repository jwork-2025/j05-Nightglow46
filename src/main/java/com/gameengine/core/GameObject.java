package com.gameengine.core;

import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

import java.util.*;

/**
 * 游戏对象基类，使用泛型组件系统
 */
public class GameObject {
    protected boolean active;
    protected String name;
    protected final String uuid = UUID.randomUUID().toString();
    protected final List<Component<?>> components;
    
    public GameObject() {
        this.active = true;
        this.name = "GameObject";
        this.components = new ArrayList<>();
    }
    
    public GameObject(String name) {
        this();
        this.name = name;
    }
    
    /**
     * 更新游戏对象逻辑
     */
    public void update(float deltaTime) {
        updateComponents(deltaTime);
    }
    
    /**
     * 渲染游戏对象
     */
    public void render() {
        renderComponents();
    }
    
    /**
     * 初始化游戏对象
     */
    public void initialize() {
        // 子类可以重写此方法进行初始化
    }
    
    /**
     * 销毁游戏对象
     */
    public void destroy() {
        this.active = false;
        // 销毁所有组件
        for (Component<?> component : components) {
            component.destroy();
        }
        components.clear();
    }
    
    /**
     * 添加组件
     */
    public <T extends Component<T>> T addComponent(T component) {
        component.setOwner(this);
        components.add(component);
        component.initialize();
        return component;
    }
    
    /**
     * 获取组件
     */
    @SuppressWarnings("unchecked")
    public <T extends Component<T>> T getComponent(Class<T> componentType) {
        for (Component<?> component : components) {
            if (componentType.isInstance(component)) {
                return (T) component;
            }
        }
        return null;
    }
    
    /**
     * 检查是否有指定类型的组件
     */
    public <T extends Component<T>> boolean hasComponent(Class<T> componentType) {
        for (Component<?> component : components) {
            if (componentType.isInstance(component)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 更新所有组件
     */
    public void updateComponents(float deltaTime) {
        for (Component<?> component : components) {
            if (component.isEnabled()) {
                component.update(deltaTime);
            }
        }
    }
    
    /**
     * 渲染所有组件
     */
    public void renderComponents() {
        for (Component<?> component : components) {
            if (component.isEnabled()) {
                component.render();
            }
        }
    }
    
    // Getters and Setters
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    private Scene scene;

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public Scene getScene() {
        return scene;
    }

    public String getUuid() {
        return uuid;
    }
}