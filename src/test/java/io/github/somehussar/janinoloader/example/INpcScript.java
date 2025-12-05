package io.github.somehussar.janinoloader.example;

public interface INpcScript {
     int getHealth();

    int getHeight();

    String getName();

    void onDamaged(int number);
}
