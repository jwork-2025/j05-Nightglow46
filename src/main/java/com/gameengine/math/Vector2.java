package com.gameengine.math;

/**
 * 2D向量类，用于表示位置、速度等
 */
public class Vector2 {
    public float x;
    public float y;
    
    public Vector2() {
        this(0, 0);
    }
    
    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public Vector2(Vector2 other) {
        this.x = other.x;
        this.y = other.y;
    }
    
    /**
     * 向量加法
     */
    public Vector2 add(Vector2 other) {
        return new Vector2(this.x + other.x, this.y + other.y);
    }
    
    /**
     * 向量减法
     */
    public Vector2 subtract(Vector2 other) {
        return new Vector2(this.x - other.x, this.y - other.y);
    }
    
    /**
     * 标量乘法
     */
    public Vector2 multiply(float scalar) {
        return new Vector2(this.x * scalar, this.y * scalar);
    }
    
    /**
     * 计算向量长度
     */
    public float magnitude() {
        return (float) Math.sqrt(x * x + y * y);
    }
    
    /**
     * 标准化向量
     */
    public Vector2 normalize() {
        float mag = magnitude();
        if (mag == 0) return new Vector2(0, 0);
        return new Vector2(x / mag, y / mag);
    }
    
    /**
     * 计算点积
     */
    public float dot(Vector2 other) {
        return this.x * other.x + this.y * other.y;
    }
    
    /**
     * 计算距离
     */
    public float distance(Vector2 other) {
        return this.subtract(other).magnitude();
    }
    
    @Override
    public String toString() {
        return String.format("Vector2(%.2f, %.2f)", x, y);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vector2 vector2 = (Vector2) obj;
        return Float.compare(vector2.x, x) == 0 && Float.compare(vector2.y, y) == 0;
    }
}

