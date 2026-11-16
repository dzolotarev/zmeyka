package com.dzolotarev.zmeyka;

import java.awt.Color;

/** Фрукт: подпись для всплывающих очков, цвета отрисовки, награда и шанс появления. */
public enum FruitKind {

    APPLE_RED("красное яблоко", new Color(216, 74, 74), new Color(255, 140, 124), 10, 1, 34),
    APPLE_GREEN("зелёное яблоко", new Color(124, 196, 92), new Color(178, 234, 144), 10, 1, 22),
    APPLE_YELLOW("жёлтое яблоко", new Color(240, 200, 74), new Color(255, 234, 146), 15, 1, 16),
    PEAR("груша", new Color(190, 198, 98), new Color(230, 236, 154), 20, 1, 14),
    CHERRY("вишня", new Color(198, 46, 74), new Color(242, 116, 134), 50, 2, 4);

    public final String label;
    public final Color color;
    public final Color highlight;
    public final int points;
    public final int growth;
    public final int weight;

    FruitKind(String label, Color color, Color highlight, int points, int growth, int weight) {
        this.label = label;
        this.color = color;
        this.highlight = highlight;
        this.points = points;
        this.growth = growth;
        this.weight = weight;
    }

    public boolean isApple() {
        return this == APPLE_RED || this == APPLE_GREEN || this == APPLE_YELLOW;
    }
}
