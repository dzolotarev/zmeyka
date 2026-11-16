package com.dzolotarev.zmeyka;

import java.util.Random;

/** Выбор фрукта: яблоки часто, груши реже, вишня — редко и гарантированно каждой 5-й. */
public final class FruitPicker {

    /** Каждое 5-е съеденное угощение — вишня, чтобы на поле всегда был редкий приз. */
    public static final int CHERRY_EVERY = 5;

    private static final int TOTAL_WEIGHT;

    static {
        int sum = 0;
        for (FruitKind kind : FruitKind.values()) {
            sum += kind.weight;
        }
        TOTAL_WEIGHT = sum;
    }

    private FruitPicker() {
    }

    /**
     * @param eatenCount сколько фруктов уже съедено (после последнего поедания)
     */
    public static FruitKind pick(Random random, int eatenCount) {
        if (eatenCount > 0 && eatenCount % CHERRY_EVERY == 0) {
            return FruitKind.CHERRY;
        }
        int roll = random.nextInt(TOTAL_WEIGHT);
        for (FruitKind kind : FruitKind.values()) {
            roll -= kind.weight;
            if (roll < 0) {
                return kind;
            }
        }
        return FruitKind.APPLE_RED;
    }
}
