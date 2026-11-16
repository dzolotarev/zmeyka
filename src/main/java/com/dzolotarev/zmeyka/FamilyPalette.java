package com.dzolotarev.zmeyka;

import java.awt.Color;
import java.util.List;

/**
 * «Семейный» окрас змейки: сегменты сгруппированы по членам семьи —
 * впереди Папа, за ним Мама, дальше дети.
 */
public final class FamilyPalette {

    /** Один член семьи занимает столько сегментов. */
    public static final int SEGMENTS_PER_MEMBER = 2;

    /** Имя и два оттенка: основной и светлый (блик на спине). */
    public record Member(String name, Color color, Color shade) {
    }

    private static final List<Member> FAMILY = List.of(
        new Member("Папа", new Color(46, 124, 88), new Color(74, 158, 114)),
        new Member("Мама", new Color(96, 188, 126), new Color(136, 214, 156)),
        new Member("Малыш", new Color(150, 216, 160), new Color(188, 236, 190)),
        new Member("Дочка", new Color(84, 196, 178), new Color(126, 222, 206)),
        new Member("Сынок", new Color(178, 226, 148), new Color(212, 244, 186))
    );

    private static final int ADULTS = 2;

    private FamilyPalette() {
    }

    public static List<Member> family() {
        return FAMILY;
    }

    /** Член семьи по индексу сегмента (0 — голова). Дети после пятого роли повторяются. */
    public static Member memberAt(int segmentIndex) {
        int role = segmentIndex / SEGMENTS_PER_MEMBER;
        if (role < FAMILY.size()) {
            return FAMILY.get(role);
        }
        int kids = FAMILY.size() - ADULTS;
        return FAMILY.get(ADULTS + (role - ADULTS) % kids);
    }

    /** Сколько членов семьи помещается на поле при данной длине змейки. */
    public static int membersOnBoard(int snakeLength) {
        return Math.max(1, (snakeLength + SEGMENTS_PER_MEMBER - 1) / SEGMENTS_PER_MEMBER);
    }
}
