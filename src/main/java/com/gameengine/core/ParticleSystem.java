package com.gameengine.core;

import com.gameengine.graphics.IRenderer;
import com.gameengine.math.Vector2;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Random;

public class ParticleSystem {
    private List<Particle> particles;
    private Random random;
    private IRenderer renderer;
    private float spawnRate;
    private float timeSinceLastSpawn;
    private Vector2 position;
    private boolean active;
    
    private Config config;
    
    private static class Particle {
        private Vector2 position;
        private Vector2 velocity;
        private float life;
        private float maxLife;
        private float size;
        private float r, g, b, a;
        
        public Particle(Vector2 position, Vector2 velocity, float life, float size, float r, float g, float b) {
            this.position = new Vector2(position);
            this.velocity = new Vector2(velocity);
            this.maxLife = life;
            this.life = life;
            this.size = size;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = 1.0f;
        }
        
        public void update(float deltaTime) {
            position = position.add(velocity.multiply(deltaTime));
            life -= deltaTime;
            
            if (life > 0) {
                a = life / maxLife;
                velocity = velocity.multiply(0.98f);
            }
        }
        
        public boolean isAlive() {
            return life > 0;
        }
        
        public Vector2 getPosition() {
            return new Vector2(position);
        }
        
        public float getSize() {
            return size;
        }
        
        public float getR() {
            return r;
        }
        
        public float getG() {
            return g;
        }
        
        public float getB() {
            return b;
        }
        
        public float getA() {
            return a;
        }
    }
    
    public static class Config {
        public float spawnRate = 0.015f;
        public int initialCount = 30;
        public float speedMin = 40f;
        public float speedMax = 100f;
        public float lifeMin = 0.8f;
        public float lifeMax = 2.0f;
        public float sizeMin = 3f;
        public float sizeMax = 7f;
        public float r = 1.0f;
        public float g = 1.0f;
        public float b = 0.0f;
        public float opacityMultiplier = 1.0f;
        public float minRenderSize = 2.0f;
        
        public float burstSpeedMin = 80f;
        public float burstSpeedMax = 200f;
        public float burstLifeMin = 0.6f;
        public float burstLifeMax = 1.6f;
        public float burstSizeMin = 4f;
        public float burstSizeMax = 10f;
        public float burstR = 1.0f;
        public float burstGMin = 0.5f;
        public float burstGMax = 1.0f;
        public float burstB = 0.0f;
        
        public static Config defaultPlayer() {
            Config config = new Config();
            config.spawnRate = 0.015f;
            config.initialCount = 30;
            config.speedMin = 40f;
            config.speedMax = 100f;
            config.lifeMin = 0.8f;
            config.lifeMax = 2.0f;
            config.sizeMin = 3f;
            config.sizeMax = 7f;
            config.r = 1.0f;
            config.g = 1.0f;
            config.b = 0.0f;
            config.opacityMultiplier = 1.0f;
            config.minRenderSize = 2.0f;
            return config;
        }
        
        public static Config light() {
            Config config = new Config();
            config.spawnRate = 0.05f;
            config.initialCount = 10;
            config.speedMin = 20f;
            config.speedMax = 50f;
            config.lifeMin = 0.5f;
            config.lifeMax = 1.3f;
            config.sizeMin = 2f;
            config.sizeMax = 5f;
            config.r = 0.6f + 0.2f;
            config.g = 0.8f + 0.2f;
            config.b = 1.0f;
            config.opacityMultiplier = 0.6f;
            config.minRenderSize = 1.5f;
            return config;
        }
    }
    
    public ParticleSystem(IRenderer renderer, Vector2 position) {
        this(renderer, position, Config.defaultPlayer());
    }
    
    public ParticleSystem(IRenderer renderer, Vector2 position, Config config) {
        this.particles = new ArrayList<>();
        this.random = new Random();
        this.renderer = renderer;
        this.position = new Vector2(position);
        this.config = config;
        this.spawnRate = config.spawnRate;
        this.timeSinceLastSpawn = 0f;
        this.active = true;
        
        for (int i = 0; i < config.initialCount; i++) {
            spawnParticle();
        }
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public void setPosition(Vector2 position) {
        if (position != null) {
            this.position = new Vector2(position);
        }
    }
    
    public void update(float deltaTime) {
        if (active) {
            timeSinceLastSpawn += deltaTime;
            if (timeSinceLastSpawn >= spawnRate) {
                spawnParticle();
                timeSinceLastSpawn = 0f;
            }
        }
        
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            particle.update(deltaTime);
            if (!particle.isAlive()) {
                iterator.remove();
            }
        }
    }
    
    private void spawnParticle() {
        if (position == null) return;
        
        float angle = (float) (random.nextFloat() * 2.0 * Math.PI);
        float speed = config.speedMin + random.nextFloat() * (config.speedMax - config.speedMin);
        Vector2 velocity = new Vector2(
            (float) (Math.cos(angle) * speed),
            (float) (Math.sin(angle) * speed)
        );
        
        float life = config.lifeMin + random.nextFloat() * (config.lifeMax - config.lifeMin);
        float size = config.sizeMin + random.nextFloat() * (config.sizeMax - config.sizeMin);
        
        float r = config.r;
        float g = config.g;
        float b = config.b;
        
        if (config.r < 1.0f) {
            r = config.r + random.nextFloat() * 0.2f;
        }
        if (config.g < 1.0f) {
            g = config.g + random.nextFloat() * 0.2f;
        }
        
        Particle particle = new Particle(new Vector2(position), velocity, life, size, r, g, b);
        particles.add(particle);
    }
    
    public void setSpawnRate(float rate) {
        this.spawnRate = rate;
    }
    
    public void render() {
        if (renderer == null) return;
        
        for (Particle particle : particles) {
            Vector2 pos = particle.getPosition();
            float size = particle.getSize();
            
            float r = Math.min(1.0f, Math.max(0.0f, particle.getR()));
            float g = Math.min(1.0f, Math.max(0.0f, particle.getG()));
            float b = Math.min(1.0f, Math.max(0.0f, particle.getB()));
            float a = Math.min(1.0f, Math.max(0.0f, particle.getA())) * config.opacityMultiplier;
            
            float maxW = renderer != null ? renderer.getWidth() : 1920;
            float maxH = renderer != null ? renderer.getHeight() : 1080;
            if (a > 0.01f && pos.x >= -50 && pos.x <= maxW + 50 && pos.y >= -50 && pos.y <= maxH + 50) {
                float renderSize = Math.max(config.minRenderSize, size * a);
                try {
                    renderer.drawRect(
                        pos.x - renderSize * 0.5f, pos.y - renderSize * 0.5f, 
                        renderSize, renderSize,
                        r, g, b, a
                    );
                } catch (Exception e) {
                }
            }
        }
    }
    
    public void burst(int count) {
        for (int i = 0; i < count; i++) {
            float angle = (float) (random.nextFloat() * 2.0 * Math.PI);
            float speed = config.burstSpeedMin + random.nextFloat() * (config.burstSpeedMax - config.burstSpeedMin);
            Vector2 velocity = new Vector2(
                (float) (Math.cos(angle) * speed),
                (float) (Math.sin(angle) * speed)
            );
            
            float life = config.burstLifeMin + random.nextFloat() * (config.burstLifeMax - config.burstLifeMin);
            float size = config.burstSizeMin + random.nextFloat() * (config.burstSizeMax - config.burstSizeMin);
            
            float r = config.burstR;
            float g = config.burstGMin + random.nextFloat() * (config.burstGMax - config.burstGMin);
            float b = config.burstB;
            
            Particle particle = new Particle(new Vector2(position), velocity, life, size, r, g, b);
            particles.add(particle);
        }
    }
    
    public int getParticleCount() {
        return particles.size();
    }
    
    public void clear() {
        particles.clear();
    }
}
