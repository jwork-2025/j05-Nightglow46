package com.gameengine.core;

import com.gameengine.components.TransformComponent;
import com.gameengine.components.PhysicsComponent;
import com.gameengine.core.GameObject;
import com.gameengine.components.RenderComponent;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 游戏逻辑类，处理具体的游戏规则
 * 已增加并行化物理更新（基于 ExecutorService）的实现，见 updatePhysics() 中标注
 */
public class GameLogic {
    private Scene scene;
    private InputManager inputManager;

    // 碰撞距离常量
    private static final float PLAYER_ENEMY_COLLISION_DISTANCE = 20f; // 玩家与敌人的碰撞判定距离
    private static final float BULLET_HIT_DISTANCE = 30f; // 子弹击中敌人的判定距离

    // 并行相关
    private final ExecutorService physicsExecutor;

    public GameLogic(Scene scene) {
        this.scene = scene;
        this.inputManager = InputManager.getInstance();
        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        this.physicsExecutor = Executors.newFixedThreadPool(threadCount);
    }

    /**
     * 清理并行执行器，建议在程序退出时调用
     */
    public void cleanup() {
        if (physicsExecutor != null && !physicsExecutor.isShutdown()) {
            physicsExecutor.shutdown();
            try {
                if (!physicsExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    physicsExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                physicsExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 处理玩家输入
     */
    public void handlePlayerInput() {
        List<GameObject> players = scene.findGameObjectsByComponent(TransformComponent.class);
        if (players.isEmpty()) return;

        GameObject player = players.get(0);
        TransformComponent transform = player.getComponent(TransformComponent.class);
        PhysicsComponent physics = player.getComponent(PhysicsComponent.class);

        if (transform == null || physics == null) return;

        Vector2 movement = new Vector2();

        if (inputManager.isKeyPressed(87) || inputManager.isKeyPressed(38)) { // W或上箭头
            movement.y -= 1;
        }
        if (inputManager.isKeyPressed(83) || inputManager.isKeyPressed(40)) { // S或下箭头
            movement.y += 1;
        }
        if (inputManager.isKeyPressed(65) || inputManager.isKeyPressed(37)) { // A或左箭头
            movement.x -= 1;
        }
        if (inputManager.isKeyPressed(68) || inputManager.isKeyPressed(39)) { // D或右箭头
            movement.x += 1;
        }

        if (movement.magnitude() > 0) {
            movement = movement.normalize().multiply(200);
            physics.setVelocity(movement);
        }

        if (inputManager.isMouseButtonJustPressed(1)) { // 鼠标左键
            Vector2 mousePos = inputManager.getMousePosition();
            scene.createBullet(mousePos); // 发射子弹
        }

        // 边界检查（800x600）
        Vector2 pos = transform.getPosition();
        if (pos.x < 0) pos.x = 0;
        if (pos.y < 0) pos.y = 0;
        if (pos.x > 800 - 20) pos.x = 800 - 20;
        if (pos.y > 600 - 20) pos.y = 600 - 20;
        transform.setPosition(pos);
    }

    /**
     * 更新物理系统（并行实现）
     * 实现说明：
     * - 使用 ExecutorService 将物理组件分批提交给线程池并行处理
     * - 每个任务只处理自己批次内的 PhysicsComponent，避免数据竞争
     * - 使用 Future.get() 等待所有任务完成，保证帧同步
     */
    public void updatePhysics() {
        List<PhysicsComponent> physicsComponents = scene.getComponents(PhysicsComponent.class);
        if (physicsComponents == null || physicsComponents.isEmpty()) return;

        // 如果物理组件数量较小，使用串行以避免线程开销
        if (physicsComponents.size() < 10) {
            for (PhysicsComponent physics : physicsComponents) {
                updateSinglePhysics(physics);
            }
            return;
        }

        int threadCount = Runtime.getRuntime().availableProcessors() - 1;
        threadCount = Math.max(2, threadCount);
        int batchSize = Math.max(1, physicsComponents.size() / threadCount + 1);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < physicsComponents.size(); i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, physicsComponents.size());

            Future<?> future = physicsExecutor.submit(() -> {
                for (int j = start; j < end; j++) {
                    PhysicsComponent physics = physicsComponents.get(j);
                    updateSinglePhysics(physics);
                }
            });

            futures.add(future);
        }

        // 等待所有任务完成
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 单个物理组件的更新逻辑（复用于串行和并行路径）
     */
    private void updateSinglePhysics(PhysicsComponent physics) {
        GameObject owner = physics.getOwner();
        if (owner == null) return;

        TransformComponent transform = owner.getComponent(TransformComponent.class);
        if (transform == null) return;
        Vector2 pos = transform.getPosition();

        if ("Bullet".equals(owner.getName())) {
            // 子弹只做边界移除
            if (pos.x < 0 || pos.x > 800 || pos.y < 0 || pos.y > 600) {
                scene.removeGameObject(owner);
            }
            return;
        }

        Vector2 velocity = physics.getVelocity();
        boolean velocityChanged = false;

        if (pos.x <= 0 || pos.x >= 800 - 15) {
            velocity.x = -velocity.x;
            velocityChanged = true;
        }
        if (pos.y <= 0 || pos.y >= 600 - 15) {
            velocity.y = -velocity.y;
            velocityChanged = true;
        }

        // 边界修正
        if (pos.x < 0) pos.x = 0;
        if (pos.y < 0) pos.y = 0;
        if (pos.x > 800 - 15) pos.x = 800 - 15;
        if (pos.y > 600 - 15) pos.y = 600 - 15;
        transform.setPosition(pos);

        if (velocityChanged) {
            physics.setVelocity(velocity);
        }
    }

    private int score = 0;

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    /**
     * 玩家生命与无敌逻辑
     */
    private final int PLAYER_MAX_HP = 5;
    private int playerHp = PLAYER_MAX_HP;
    private long invincibleUntilNanos = 0L;
    private final double INVINCIBLE_DURATION_SECONDS = 3.0; // 3秒无敌
    private boolean gameOver = false;

    public void checkCollisions() {
        if (gameOver) return;

        List<GameObject> players = scene.findGameObjectsByComponent(TransformComponent.class);
        if (players.isEmpty()) return;

        GameObject player = players.get(0);
        TransformComponent playerTransform = player.getComponent(TransformComponent.class);
        if (playerTransform == null) return;

        List<GameObject> toRemove = new ArrayList<>();

        // 玩家与敌人碰撞检测（使用 AABB 碰撞，考虑渲染尺寸与坐标约定）
        if (!isPlayerInvincible()) {
            // 计算玩家包围盒
            float[] playerAABB = getAABBForGameObject(player);
            for (GameObject obj : scene.getGameObjects()) {
                if (obj.getName().equals("Enemy")) {
                    float[] enemyAABB = getAABBForGameObject(obj);
                    if (aabbIntersects(playerAABB, enemyAABB)) {
                        // 受到一次伤害，进入无敌
                        playerHp = Math.max(0, playerHp - 1);
                        invincibleUntilNanos = System.nanoTime() + (long) (INVINCIBLE_DURATION_SECONDS * 1_000_000_000L);
                        // 移除被碰撞的敌人，避免重复伤害
                        toRemove.add(obj);
                        if (playerHp <= 0) {
                            gameOver = true;
                        }
                        break;
                    }
                }
            }
        }

        // 子弹与敌人碰撞（移除并计分）
        for (GameObject bullet : scene.getGameObjects()) {
            if (bullet.getName().equals("Bullet")) {
                TransformComponent bulletTransform = bullet.getComponent(TransformComponent.class);
                if (bulletTransform != null) {
                    for (GameObject enemy : scene.getGameObjects()) {
                        if (enemy.getName().equals("Enemy")) {
                            TransformComponent enemyTransform = enemy.getComponent(TransformComponent.class);
                            if (enemyTransform != null) {
                                float dist = bulletTransform.getPosition().distance(enemyTransform.getPosition());
                                if (dist < BULLET_HIT_DISTANCE) {
                                    toRemove.add(enemy);
                                    toRemove.add(bullet);
                                    score++;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        // 统一移除，避免在遍历时修改集合
        for (GameObject rem : toRemove) {
            scene.removeGameObject(rem);
        }
    }

    /**
     * 返回对象的 AABB：{minX, minY, maxX, maxY}
     * 根据对象是否有 RenderComponent 或者对象类型（Player/Bullet）决定坐标约定
     */
    private float[] getAABBForGameObject(GameObject obj) {
        TransformComponent t = obj.getComponent(TransformComponent.class);
        float px = 0, py = 0;
        if (t != null) {
            Vector2 pos = t.getPosition();
            px = pos.x;
            py = pos.y;
        }

        // 如果对象有 RenderComponent，则根据其类型和 size 计算包围盒
        RenderComponent rc = obj.getComponent(RenderComponent.class);
        if (rc != null) {
            Vector2 size = rc.getSize();
            switch (rc.getRenderType()) {
                case RECTANGLE:
                    // RenderComponent 的 RECTANGLE 使用 position 作为左上角
                    return new float[] { px, py, px + size.x, py + size.y };
                case CIRCLE:
                    // 绘制时圆心是 position + size/2
                    float cx = px + size.x / 2f;
                    float cy = py + size.y / 2f;
                    float r = size.x / 2f;
                    return new float[] { cx - r, cy - r, cx + r, cy + r };
                default:
                    return new float[] { px, py, px + size.x, py + size.y };
            }
        }

        // 对于没有 RenderComponent 的对象，按照常见命名约定处理
        if ("Player".equals(obj.getName())) {
            // 在 GameExample 中，Player 的绘制是以 basePosition 为中心的，身体为 16x20
            float w = 16f, h = 20f;
            return new float[] { px - w/2f, py - h/2f, px + w/2f, py + h/2f };
        }
        if ("Bullet".equals(obj.getName())) {
            // 子弹绘制使用 position -5 为左上角，尺寸 10x10，position 在代码中是子弹中心
            float w = 10f, h = 10f;
            return new float[] { px - w/2f, py - h/2f, px + w/2f, py + h/2f };
        }

        // 默认：把对象当作一个小点（1x1）
        return new float[] { px - 0.5f, py - 0.5f, px + 0.5f, py + 0.5f };
    }

    private boolean aabbIntersects(float[] a, float[] b) {
        return a[0] < b[2] && a[2] > b[0] && a[1] < b[3] && a[3] > b[1];
    }

    private boolean isPlayerInvincible() {
        return System.nanoTime() < invincibleUntilNanos;
    }

    public int getPlayerHp() {
        return playerHp;
    }

    public int getPlayerMaxHp() {
        return PLAYER_MAX_HP;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setPlayerHp(int hp) {
        this.playerHp = hp;
    }
}
