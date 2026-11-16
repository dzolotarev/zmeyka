package com.dzolotarev.zmeyka;

/** Ячейка игрового поля в координатах сетки. */
public record Cell(int x, int y) {

    Cell move(Direction d) {
        return new Cell(x + d.dx, y + d.dy);
    }
}
