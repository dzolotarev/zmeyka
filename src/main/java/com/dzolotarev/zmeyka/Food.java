package com.dzolotarev.zmeyka;

/** Угощение на поле: клетка + вид фрукта. */
public record Food(Cell position, FruitKind kind) {

    public int points() {
        return kind.points;
    }

    public int growth() {
        return kind.growth;
    }
}
