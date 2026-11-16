package com.dzolotarev.zmeyka;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Ядро игры без зависимости от Swing: состояние, правила, ходы.
 * Отрисовка и ввод живут в {@link GamePanel}.
 */
public class Game {

    public enum State { READY, RUNNING, PAUSED, GAME_OVER }

    /** Сколько очков снимаем за каждый ускорившийся тик. */
    private static final int SPEEDUP_PER_FOOD_MS = 3;

    private final BoardConfig board;
    private final Random random;
    private final HighScoreStore highScores;

    private Snake snake;
    private Food food;
    private Food lastEaten;
    private State state = State.READY;

    private int score;
    private int eaten;
    private int pendingGrowth;
    private long bestScore;

    public Game(BoardConfig board, HighScoreStore highScores, Random random) {
        this.board = board;
        this.highScores = highScores;
        this.random = random;
        this.bestScore = highScores.read();
        reset();
    }

    public BoardConfig board() {
        return board;
    }

    public State state() {
        return state;
    }

    public Snake snake() {
        return snake;
    }

    public Food food() {
        return food;
    }

    public int score() {
        return score;
    }

    public long bestScore() {
        return bestScore;
    }

    /** Задержка между тиками: чем длиннее змейка, тем быстрее бег. */
    public int tickDelayMs() {
        int speedup = eaten * SPEEDUP_PER_FOOD_MS;
        return Math.max(board.minDelayMs(), board.baseDelayMs() - speedup);
    }

    public void start() {
        if (state == State.READY || state == State.GAME_OVER) {
            reset();
        }
        state = State.RUNNING;
    }

    public void restart() {
        reset();
        state = State.RUNNING;
    }

    public void togglePause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
        } else if (state == State.PAUSED) {
            state = State.RUNNING;
        }
    }

    /** Возвращает true, если игра закончилась на этом ходе. */
    public boolean tick() {
        if (state != State.RUNNING) {
            return false;
        }
        lastEaten = null;

        boolean grow = pendingGrowth > 0;
        Cell newHead = snake.advance(board, grow);
        if (grow) {
            pendingGrowth--;
        }

        if (!board.isInside(newHead) || snake.hitsBody(newHead)) {
            finish();
            return true;
        }

        if (newHead.equals(food.position())) {
            score += food.points();
            eaten++;
            pendingGrowth += food.growth();
            lastEaten = food;
            food = spawnFood().orElse(null);
            if (food == null) {
                // Поле заполнено целиком — это победа, а не поражение.
                finish();
                return true;
            }
        }
        return false;
    }

    /** Фрукт, съеденный на последнем тике (null если не ели) — нужен для всплывающих очков. */
    public Food lastEaten() {
        return lastEaten;
    }

    public void turn(Direction direction) {
        if (state == State.RUNNING) {
            snake.queueTurn(direction);
        }
    }

    /** Пакетный хук для тестов: ставит еду в конкретную клетку. */
    void placeFood(Cell cell) {
        placeFood(cell, FruitKind.APPLE_RED);
    }

    void placeFood(Cell cell, FruitKind kind) {
        this.food = new Food(cell, kind);
    }

    private void reset() {
        Cell start = new Cell(board.columns() / 2, board.rows() / 2);
        this.snake = new Snake(start, Direction.RIGHT);
        this.score = 0;
        this.eaten = 0;
        this.pendingGrowth = 0;
        this.food = spawnFood().orElse(null);
    }

    private void finish() {
        state = State.GAME_OVER;
        if (score > bestScore) {
            bestScore = score;
            highScores.write(bestScore);
        }
    }

    /** Спавним еду только в свободных клетках — иначе проигрыш из-за случайности. */
    private Optional<Food> spawnFood() {
        List<Cell> free = new ArrayList<>();
        for (int x = 0; x < board.columns(); x++) {
            for (int y = 0; y < board.rows(); y++) {
                Cell cell = new Cell(x, y);
                if (!snake.hitsBody(cell) && !cell.equals(snake.head())) {
                    free.add(cell);
                }
            }
        }
        if (free.isEmpty()) {
            return Optional.empty();
        }
        Cell picked = free.get(random.nextInt(free.size()));
        return Optional.of(new Food(picked, FruitPicker.pick(random, eaten)));
    }
}
