package com.dzolotarev.zmeyka;

/** Геометрия и правила поля. Неизменяемые настройки игры. */
public record BoardConfig(int columns, int rows, int cellSize, boolean wrapAround, int baseDelayMs, int minDelayMs) {

    public static BoardConfig defaults() {
        return new BoardConfig(30, 22, 24, false, 140, 55);
    }

    public int width() {
        return columns * cellSize;
    }

    public int height() {
        return rows * cellSize;
    }

    /** Голова змейки по новую координату: проворачиваем у стен или считаем столкновением. */
    public Cell nextHead(Cell head, Direction dir) {
        Cell raw = head.move(dir);
        if (!wrapAround) {
            return raw;
        }
        int x = Math.floorMod(raw.x(), columns);
        int y = Math.floorMod(raw.y(), rows);
        return new Cell(x, y);
    }

    public boolean isInside(Cell cell) {
        return cell.x() >= 0 && cell.x() < columns && cell.y() >= 0 && cell.y() < rows;
    }
}
