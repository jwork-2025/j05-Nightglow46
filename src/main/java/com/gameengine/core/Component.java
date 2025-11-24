package com.gameengine.core;

/**
 * 组件基类，使用泛型设计
 * @param <T> 组件类型
 */
public abstract class Component<T extends Component<T>> {
    protected GameObject owner;
    protected boolean enabled;
    protected String name;
    
    public Component() {
        this.enabled = true;
        this.name = this.getClass().getSimpleName();
    }
    
    /**
     * 初始化组件
     */
    public abstract void initialize();
    
    /**
     * 更新组件
     * @param deltaTime 时间间隔
     */
    public abstract void update(float deltaTime);
    
    /**
     * 渲染组件
     */
    public abstract void render();
    
    /**
     * 销毁组件
     */
    public void destroy() {
        this.enabled = false;
    }
    
    /**
     * 获取组件类型
     */
    @SuppressWarnings("unchecked")
    public Class<T> getComponentType() {
        return (Class<T>) this.getClass();
    }
    
    // Getters and Setters
    public GameObject getOwner() {
        return owner;
    }
    
    public void setOwner(GameObject owner) {
        this.owner = owner;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
