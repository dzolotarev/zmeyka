package com.dzolotarev.zmeyka;

import java.nio.file.Path;
import java.util.Random;

/**
 * Проверки ядра игры без Swing: только javac + java, без внешних зависимостей.
 * Запуск: ./run-tests.sh
 */
public final class GameLogicTest {

    private static int passed;

    private GameLogicTest() {
    }

    public static void main(String[] args) {
        movesRightByDefault();
        ignoresReversal();
        queuesTwoTurns();
        growsAndScoresOnEat();
        diesOnWall();
        wrapsWhenEnabled();
        diesOnSelfCollision();
        speedsUpWithScore();
        foodNeverSpawnsOnSnake();
        cherryRewards();
        fruitPickerBehaviour();
        familyLook();
        eatenFruitIsReported();
        highScoreRoundTrip();

        System.out.println("OK: пройдено проверок — " + passed);
    }

    private static Game newGame(BoardConfig board) {
        HighScoreStore store = new HighScoreStore(Path.of("/tmp/opencode/snake-test-score.properties"));
        Game game = new Game(board, store, new Random(7));
        game.restart();
        return game;
    }

    private static BoardConfig openBoard() {
        return new BoardConfig(20, 15, 10, false, 100, 50);
    }

    /** Ставит еду ровно перед головой, чтобы следующий tick гарантированно был поеданием. */
    private static void foodAhead(Game game) {
        game.placeFood(game.snake().head().move(game.snake().direction()));
    }

    private static void movesRightByDefault() {
        Game game = newGame(openBoard());
        game.placeFood(new Cell(0, 0));
        game.tick();
        assertEquals("ход вправо", new Cell(11, 7), game.snake().head());
    }

    private static void ignoresReversal() {
        Game game = newGame(openBoard());
        game.placeFood(new Cell(0, 0));
        game.turn(Direction.LEFT);
        game.tick();
        assertEquals("разворот на 180° игнорируется", new Cell(11, 7), game.snake().head());
    }

    private static void queuesTwoTurns() {
        Game game = newGame(openBoard());
        game.placeFood(new Cell(0, 0));
        game.turn(Direction.UP);
        game.turn(Direction.LEFT);
        game.tick();
        assertEquals("первый поворот из очереди", new Cell(10, 6), game.snake().head());
        game.tick();
        assertEquals("второй поворот из очереди", new Cell(9, 6), game.snake().head());
    }

    private static void growsAndScoresOnEat() {
        Game game = newGame(openBoard());
        foodAhead(game);
        game.tick();
        assertEquals("очки за еду", 10, game.score());
        assertEquals("длина сразу после еды", 3, game.snake().length());
        game.placeFood(new Cell(0, 0));
        game.tick();
        assertEquals("рост на следующем тике", 4, game.snake().length());
        if (game.food().position().equals(new Cell(11, 7))) {
            throw new AssertionError("еда должна появиться в новой клетке");
        }
    }

    private static void diesOnWall() {
        Game game = newGame(new BoardConfig(5, 5, 10, false, 100, 50));
        game.placeFood(new Cell(0, 4));
        boolean over = false;
        for (int i = 0; i < 5 && !over; i++) {
            over = game.tick();
        }
        assertTrue("столкновение со стеной завершает игру", game.state() == Game.State.GAME_OVER);
        assertEquals("счёт при гибели", 0, game.score());
    }

    private static void wrapsWhenEnabled() {
        Game game = newGame(new BoardConfig(5, 5, 10, true, 100, 50));
        game.placeFood(new Cell(0, 4));
        for (int i = 0; i < 3; i++) {
            game.tick();
        }
        assertEquals("телепорт сквозь правую стену", new Cell(0, 2), game.snake().head());
        assertTrue("в режиме wrap стена не убивает", game.state() == Game.State.RUNNING);
    }

    private static void diesOnSelfCollision() {
        Game game = newGame(openBoard());
        // Отращиваем змейку, чтобы разворот U-образной дугой уткнулся в собственное тело.
        for (int i = 0; i < 4; i++) {
            foodAhead(game);
            game.tick();
        }
        game.placeFood(new Cell(0, 0));
        game.turn(Direction.UP);
        game.tick();
        game.turn(Direction.LEFT);
        game.tick();
        game.turn(Direction.DOWN);
        game.tick();
        assertTrue("столкновение с собой завершает игру", game.state() == Game.State.GAME_OVER);
    }

    private static void speedsUpWithScore() {
        BoardConfig board = new BoardConfig(20, 15, 10, true, 140, 130);
        Game game = newGame(board);
        assertEquals("стартовая скорость", 140, game.tickDelayMs());
        foodAhead(game);
        game.tick();
        assertEquals("ускорение после еды", 137, game.tickDelayMs());
        for (int i = 0; i < 40; i++) {
            foodAhead(game);
            game.tick();
        }
        assertTrue("скорость ограничена снизу", game.tickDelayMs() == board.minDelayMs());
    }

    /** Инвариант: еда никогда не появляется в клетке, занятой телом. */
    private static void foodNeverSpawnsOnSnake() {
        Game game = newGame(new BoardConfig(6, 5, 10, false, 40, 20));
        for (int i = 0; i < 200 && game.state() == Game.State.RUNNING; i++) {
            Direction next = safeDirection(game);
            game.turn(next);
            game.placeFood(game.board().nextHead(game.snake().head(), next));
            game.tick();
            Food food = game.food();
            if (food != null && game.snake().cells().stream().anyMatch(c -> c.equals(food.position()))) {
                throw new AssertionError("еда появилась внутри змейки: " + food.position());
            }
        }
        assertTrue("еда ни разу не появилась в теле (" + game.snake().length() + " клеток)", true);
    }

    /** Жадный «автопилот»: едёт в первую свободную клетку, ускоряя заполнение поля. */
    private static Direction safeDirection(Game game) {
        Cell head = game.snake().head();
        Direction[] candidates = { game.snake().direction(), Direction.DOWN, Direction.LEFT, Direction.UP, Direction.RIGHT };
        for (Direction d : candidates) {
            if (d.isOppositeOf(game.snake().direction())) {
                continue;
            }
            Cell next = game.board().nextHead(head, d);
            if (game.board().isInside(next) && !game.snake().hitsBody(next) && !next.equals(head)) {
                return d;
            }
        }
        return game.snake().direction();
    }

    private static void cherryRewards() {
        Game game = newGame(new BoardConfig(20, 15, 10, true, 100, 50));
        for (int i = 0; i < FruitPicker.CHERRY_EVERY; i++) {
            foodAhead(game);
            game.tick();
        }
        assertEquals("счёт после 5 яблок", 50, game.score());
        assertEquals("каждая 5-я порция — вишня", FruitKind.CHERRY, game.food().kind());

        Cell ahead = game.snake().head().move(game.snake().direction());
        game.placeFood(ahead, FruitKind.CHERRY);
        game.tick(); // съедаем вишню
        assertEquals("счёт после вишни", 100, game.score());
        assertEquals("очки за вишню", 50, FruitKind.CHERRY.points);
        assertEquals("рост от вишни", 2, FruitKind.CHERRY.growth);

        game.placeFood(new Cell(0, 14));
        for (int i = 0; i < 3; i++) {
            game.tick();
        }
        int expectedLength = 3 + FruitPicker.CHERRY_EVERY * 1 + 2;
        assertEquals("вишня растит на 2", expectedLength, game.snake().length());
    }

    private static void fruitPickerBehaviour() {
        Random random = new Random(11);
        assertEquals("вишня гарантирована каждой 5-й", FruitKind.CHERRY, FruitPicker.pick(random, 5));
        assertEquals("вишня гарантирована каждой 10-й", FruitKind.CHERRY, FruitPicker.pick(random, 10));

        int apples = 0;
        int pears = 0;
        int cherries = 0;
        int draws = 4000;
        for (int i = 0; i < draws; i++) {
            FruitKind kind = FruitPicker.pick(random, 1); // eatenCount=1 — только взвешенная часть
            if (kind.isApple()) {
                apples++;
            } else if (kind == FruitKind.PEAR) {
                pears++;
            } else if (kind == FruitKind.CHERRY) {
                cherries++;
            }
        }
        assertTrue("яблок больше всего (" + apples + ")", apples > pears && apples > cherries);
        assertTrue("груши появляются чаще вишни (" + pears + " > " + cherries + ")", pears > cherries);
        assertTrue("вишня редкая, но бывает (" + cherries + ")", cherries > 0);
        assertTrue("все три яблока встречаются", seenAppleCount(random) == 3);
    }

    private static int seenAppleCount(Random random) {
        java.util.Set<FruitKind> seen = new java.util.HashSet<>();
        for (int i = 0; i < 600; i++) {
            FruitKind kind = FruitPicker.pick(random, 1);
            if (kind.isApple()) {
                seen.add(kind);
            }
        }
        return seen.size();
    }

    private static void familyLook() {
        assertEquals("голова — Папа", "Папа", FamilyPalette.memberAt(0).name());
        assertEquals("второй сегмент тоже Папа", "Папа", FamilyPalette.memberAt(1).name());
        assertEquals("третий сегмент — Мама", "Мама", FamilyPalette.memberAt(2).name());
        assertEquals("пятый сегмент — Малыш", "Малыш", FamilyPalette.memberAt(4).name());
        assertTrue("дальше дети повторяются по кругу",
            FamilyPalette.memberAt(10).name().equals(FamilyPalette.memberAt(4).name()));
        assertEquals("семьей правят двое взрослых", 2, FamilyPalette.family().size() - 3);
        assertEquals("членов семьи на поле", 4, FamilyPalette.membersOnBoard(7));
        assertEquals("минимум один член семьи", 1, FamilyPalette.membersOnBoard(1));
    }

    /** Вьювер рисует всплывающие очки из lastEaten — проверка, что данные есть. */
    private static void eatenFruitIsReported() {
        Game game = newGame(openBoard());
        game.placeFood(new Cell(0, 0));
        game.tick();
        assertTrue("без еды lastEaten пуст", game.lastEaten() == null);

        Cell ahead = game.snake().head().move(Direction.RIGHT);
        game.placeFood(ahead, FruitKind.PEAR);
        game.tick();
        assertEquals("очки за грушу", 20, game.score());
        assertEquals("всплывающая подпись — фрукт", FruitKind.PEAR, game.lastEaten().kind());
        assertEquals("подпись совпадает с клеткой поедания", ahead, game.lastEaten().position());
    }

    private static void highScoreRoundTrip() {
        Path file = Path.of("/tmp/opencode/snake-test-highscore.properties");
        HighScoreStore store = new HighScoreStore(file);
        store.write(1234L);
        assertEquals("рекорд переживает перезапись", 1234L, store.read());
    }

    private static void assertEquals(String what, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(what + ": ожидалось " + expected + ", получено " + actual);
        }
        passed++;
    }

    private static void assertTrue(String what, boolean condition) {
        if (!condition) {
            throw new AssertionError(what);
        }
        passed++;
    }
}
