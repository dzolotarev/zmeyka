package com.dzolotarev.zmeyka;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Змейка: тело + буфер поворотов, чтобы быстрые нажатия не разворачивали её на 180°. */
public class Snake {

    private static final int MAX_QUEUED_TURNS = 2;

    private final Deque<Cell> body = new ArrayDeque<>();
    private final Deque<Direction> pendingTurns = new ArrayDeque<>();

    private Direction direction;
    private boolean grewLastTick;

    public Snake(Cell start, Direction initial) {
        this.direction = initial;
        body.addFirst(start);
        // Хвост тянется назад по направлению движения.
        for (int i = 1; i < 3; i++) {
            body.addLast(new Cell(start.x() - initial.dx * i, start.y() - initial.dy * i));
        }
    }

    public Direction direction() {
        return direction;
    }

    public Cell head() {
        return body.peekFirst();
    }

    /** Тело от головы к хвосту — только для отрисовки. */
    public List<Cell> cells() {
        return new ArrayList<>(body);
    }

    public int length() {
        return body.size();
    }

    public boolean grewLastTick() {
        return grewLastTick;
    }

    /**
     * Ставит поворот в очередь. Поворот валиден относительно последнего
     * принятого направления, а не текущего — иначе двойное нажатие за один тик
     * позволяет развернуться через собственное тело.
     */
    public void queueTurn(Direction next) {
        Direction last = pendingTurns.isEmpty() ? direction : pendingTurns.peekLast();
        if (next == last || next.isOppositeOf(last)) {
            return;
        }
        if (pendingTurns.size() >= MAX_QUEUED_TURNS) {
            return;
        }
        pendingTurns.addLast(next);
    }

    /**
     * Сдвигает змейку на одну клетку.
     *
     * @return новая голова
     */
    public Cell advance(BoardConfig board, boolean grow) {
        Direction queued = pendingTurns.pollFirst();
        if (queued != null) {
            this.direction = queued;
        }

        Cell newHead = board.nextHead(head(), direction);
        body.addFirst(newHead);
        if (grow) {
            grewLastTick = true;
        } else {
            grewLastTick = false;
            body.removeLast();
        }
        return newHead;
    }

    /**
     * Столкновение с собственным телом. Голова (первый элемент) пропускается:
     * к моменту проверки она уже содержит новую клетку, а хвост к этому моменту
     * уже сдвинут, поэтому клетка старого хвоста свободна.
     */
    public boolean hitsBody(Cell cell) {
        boolean first = true;
        for (Cell c : body) {
            if (first) {
                first = false;
                continue;
            }
            if (c.equals(cell)) {
                return true;
            }
        }
        return false;
    }
}
