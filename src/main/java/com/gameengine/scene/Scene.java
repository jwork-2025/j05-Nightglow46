package com.gameengine.scene;

import com.gameengine.core.GameObject;
import com.gameengine.math.Vector2;
import com.gameengine.core.Component;
// 移除具体游戏逻辑的import
import java.util.*;
import java.util.stream.Collectors;

/**
 * 场景类，管理游戏对象和组件
 */
public class Scene {
    private String name;
    private List<GameObject> gameObjects;
    private List<GameObject> objectsToAdd;
    private List<GameObject> objectsToRemove;
    private boolean initialized;
    // 移除未使用的组件索引
    
    public Scene(String name) {
        this.name = name;
        this.gameObjects = new ArrayList<>();
        this.objectsToAdd = new ArrayList<>();
        this.objectsToRemove = new ArrayList<>();
        this.initialized = false;
        // 移除组件索引初始化
    }
    
    /**
     * 初始化场景
     */
    public void initialize() {
        for (GameObject obj : gameObjects) {
            obj.initialize();
        }
        initialized = true;
    }
    
    /**
     * 更新场景
     */
    public void update(float deltaTime) {
        // 添加新对象
        for (GameObject obj : objectsToAdd) {
            gameObjects.add(obj);
            if (initialized) {
                obj.initialize();
            }
        }
        objectsToAdd.clear();
        
        // 移除标记的对象
        for (GameObject obj : objectsToRemove) {
            gameObjects.remove(obj);
        }
        objectsToRemove.clear();
        
        // 更新所有活跃的游戏对象
        Iterator<GameObject> iterator = gameObjects.iterator();
        while (iterator.hasNext()) {
            GameObject obj = iterator.next();
            if (obj.isActive()) {
                obj.update(deltaTime);
            } else {
                iterator.remove();
            }
        }
    }
    
    /**
     * 渲染场景
     */
    public void render() {
        for (GameObject obj : gameObjects) {
            if (obj.isActive()) {
                obj.render();
            }
        }
    }
    
    /**
     * 添加游戏对象到场景
     */
    public void addGameObject(GameObject gameObject) {
        objectsToAdd.add(gameObject);
    }
    
    /**
     * 根据组件类型查找游戏对象
     */
    public <T extends Component<T>> List<GameObject> findGameObjectsByComponent(Class<T> componentType) {
        return gameObjects.stream()
            .filter(obj -> obj.hasComponent(componentType))
            .collect(Collectors.toList());
    }
    
    /**
     * 获取所有具有指定组件的游戏对象
     */
    public <T extends Component<T>> List<T> getComponents(Class<T> componentType) {
        return findGameObjectsByComponent(componentType).stream()
            .map(obj -> obj.getComponent(componentType))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * 清空场景
     */
    public void clear() {
        gameObjects.clear();
        objectsToAdd.clear();
        objectsToRemove.clear();
    }
    
    /**
     * 获取场景名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取所有游戏对象
     */
    public List<GameObject> getGameObjects() {
        return new ArrayList<>(gameObjects);
    }
    
    // 移除具体游戏逻辑，让子类实现

    public void createBullet(Vector2 position) {
        // 空实现，或抛异常，或由子类/匿名类重写
    }

    public void removeGameObject(GameObject gameObject) {
        objectsToRemove.add(gameObject);
    }
}