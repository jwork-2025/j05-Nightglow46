package com.gameengine.input;

import com.gameengine.math.Vector2;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 输入管理器，处理键盘和鼠标输入
 */
public class InputManager {
    private static InputManager instance;
    private Set<Integer> pressedKeys;
    private Set<Integer> justPressedKeys;
    private Map<Integer, Boolean> keyStates;
    private Vector2 mousePosition;
    private boolean[] mouseButtons;
    private boolean[] mouseButtonsJustPressed;
    private boolean ignoreHardwareInput = false;

    private InputManager() {
        pressedKeys = new HashSet<>();
        justPressedKeys = new HashSet<>();
        keyStates = new HashMap<>();
        mousePosition = new Vector2();
        mouseButtons = new boolean[3]; // 左键、右键、中键
        mouseButtonsJustPressed = new boolean[3];
    }

    public static InputManager getInstance() {
        if (instance == null) {
            instance = new InputManager();
        }
        return instance;
    }

    public void setIgnoreHardwareInput(boolean ignore) {
        this.ignoreHardwareInput = ignore;
    }

    /**
     * 更新输入状态
     */
    public void update() {
        justPressedKeys.clear();
        for (int i = 0; i < mouseButtonsJustPressed.length; i++) {
            mouseButtonsJustPressed[i] = false;
        }
    }

    /**
     * 处理键盘按下事件
     */
    public void onKeyPressed(int keyCode) {
        if (ignoreHardwareInput && keyCode != 27) return; // Allow ESC (27)
        if (!pressedKeys.contains(keyCode)) {
            justPressedKeys.add(keyCode);
        }
        pressedKeys.add(keyCode);
        keyStates.put(keyCode, true);
    }
    
    /**
     * 处理键盘释放事件
     */
    public void onKeyReleased(int keyCode) {
        if (ignoreHardwareInput && keyCode != 27) return;
        pressedKeys.remove(keyCode);
        keyStates.put(keyCode, false);
    }
    
    /**
     * 处理鼠标移动事件
     */
    public void onMouseMoved(float x, float y) {
        if (ignoreHardwareInput) return;
        mousePosition.x = x;
        mousePosition.y = y;
    }
    
    /**
     * 处理鼠标按下事件
     */
    public void onMousePressed(int button) {
        if (ignoreHardwareInput) return;
        if (button >= 0 && button < mouseButtons.length) {
            if (!mouseButtons[button]) {
                mouseButtonsJustPressed[button] = true;
            }
            mouseButtons[button] = true;
        }
    }
    
    /**
     * 处理鼠标释放事件
     */
    public void onMouseReleased(int button) {
        if (ignoreHardwareInput) return;
        if (button >= 0 && button < mouseButtons.length) {
            mouseButtons[button] = false;
        }
    }    /**
     * 检查按键是否被按下
     */
    public boolean isKeyPressed(int keyCode) {
        return pressedKeys.contains(keyCode);
    }

    /**
     * 检查按键是否刚刚被按下（只在这一帧为true）
     */
    public boolean isKeyJustPressed(int keyCode) {
        return justPressedKeys.contains(keyCode);
    }

    /**
     * 检查鼠标按键是否被按下
     */
    public boolean isMouseButtonPressed(int button) {
        if (button >= 0 && button < mouseButtons.length) {
            return mouseButtons[button];
        }
        return false;
    }

    /**
     * 检查鼠标按键是否刚刚被按下
     */
    public boolean isMouseButtonJustPressed(int button) {
        if (button >= 0 && button < mouseButtons.length) {
            return mouseButtonsJustPressed[button];
        }
        return false;
    }

    /**
     * 获取鼠标位置
     */
    public Vector2 getMousePosition() {
        return new Vector2(mousePosition);
    }

    /**
     * 获取鼠标X坐标
     */
    public float getMouseX() {
        return mousePosition.x;
    }

    /**
     * 获取鼠标Y坐标
     */
    public float getMouseY() {
        return mousePosition.y;
    }

    /**
     * 获取当前按下的所有按键的副本
     */
    public Set<Integer> getPressedKeysSnapshot() {
        return new HashSet<>(pressedKeys);
    }

    /**
     * 获取刚刚按下的所有按键的副本
     */
    public Set<Integer> getJustPressedKeysSnapshot() {
        return new HashSet<>(justPressedKeys);
    }

    /**
     * 强制设置按键状态（用于回放）
     */
    public void setPressedKeys(Set<Integer> keys) {
        // Calculate just pressed
        justPressedKeys.clear();
        for (Integer k : keys) {
            if (!pressedKeys.contains(k)) {
                justPressedKeys.add(k);
            }
        }
        pressedKeys.clear();
        pressedKeys.addAll(keys);
        keyStates.clear();
        for (Integer k : keys) {
            keyStates.put(k, true);
        }
    }

    /**
     * 注入鼠标位置（忽略硬件屏蔽标记）
     */
    public void injectMousePosition(float x, float y) {
        mousePosition.x = x;
        mousePosition.y = y;
    }

    /**
     * 注入鼠标按键状态（忽略硬件屏蔽标记）
     */
    public void injectMouseButton(int button, boolean pressed) {
        if (button >= 0 && button < mouseButtons.length) {
            if (pressed) {
                if (!mouseButtons[button]) {
                    mouseButtonsJustPressed[button] = true;
                }
                mouseButtons[button] = true;
            } else {
                mouseButtons[button] = false;
            }
        }
    }

    public boolean[] getMouseButtonsSnapshot() {
        return mouseButtons.clone();
    }

    public void clear() {
        pressedKeys.clear();
        justPressedKeys.clear();
        keyStates.clear();
        for (int i = 0; i < mouseButtons.length; i++) {
            mouseButtons[i] = false;
            mouseButtonsJustPressed[i] = false;
        }
        mousePosition.x = 0;
        mousePosition.y = 0;
    }
}
