package io.github.somehussar.janinoloader.example;

public class Npc {

    private String name;
    private int health;
    private int height;

    public Npc(String name, int health, int height) {
        this.health = health;
        this.name = name;
        this.height = height;
    }

    public int getHealth() {
        return health;
    }
    public int getHeight() {
        return height;
    }
    public String getName() {
        return name;
    }

    public void setHealth(int i) {
        health = i;
    }
}
