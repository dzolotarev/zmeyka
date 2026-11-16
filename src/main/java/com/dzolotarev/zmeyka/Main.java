package com.dzolotarev.zmeyka;

/**
 * Точка входа.
 *
 * Запуск: java -jar zmeyka-1.0.jar [--wrap] [--cell=24] [--cols=30] [--rows=22] [--speed=140]
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        int cols = 30;
        int rows = 22;
        int cell = 24;
        int speed = 140;
        boolean wrap = false;

        for (String arg : args) {
            switch (arg) {
                case "--wrap" -> wrap = true;
                case "--version", "-v" -> {
                    System.out.println(Version.full());
                    return;
                }
                case "--help", "-h" -> {
                    usage();
                    return;
                }
                default -> {
                    Integer value = parseOption(arg);
                    if (value == null) {
                        System.out.println("Неизвестный аргумент: " + arg);
                        usage();
                        return;
                    }
                    switch (arg.substring(2, arg.indexOf('='))) {
                        case "cell" -> cell = value;
                        case "cols" -> cols = value;
                        case "rows" -> rows = value;
                        case "speed" -> speed = value;
                        default -> {
                            usage();
                            return;
                        }
                    }
                }
            }
        }

        if (cols < 8 || rows < 6 || cell < 10 || speed < 20) {
            System.out.println("Слишком маленькое поле или слишком быстрая змейка: cols>=8, rows>=6, cell>=10, speed>=20");
            return;
        }

        BoardConfig board = new BoardConfig(cols, rows, cell, wrap, speed, 55);
        Game game = new Game(board, HighScoreStore.defaultStore(), new java.util.Random());
        GameFrame.show(new GamePanel(game));
    }

    /** Разбирает `--ключ=число`; null — если аргумент не число или ключ неизвестен. */
    private static Integer parseOption(String arg) {
        int separator = arg.indexOf('=');
        if (!arg.startsWith("--") || separator < 0) {
            return null;
        }
        String key = arg.substring(2, separator);
        if (!key.equals("cell") && !key.equals("cols") && !key.equals("rows") && !key.equals("speed")) {
            return null;
        }
        try {
            return Integer.parseInt(arg.substring(separator + 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void usage() {
        System.out.println(Version.full() + " — змейка на Java/Swing");
        System.out.println("Использование: zmeyka [--wrap] [--cell=N] [--cols=N] [--rows=N] [--speed=MS]");
        System.out.println("  --wrap      проходить сквозь стены");
        System.out.println("  --cell=N    размер клетки в пикселях (по умолчанию 24)");
        System.out.println("  --cols=N    ширина поля в клетках (по умолчанию 30)");
        System.out.println("  --rows=N    высота поля в клетках (по умолчанию 22)");
        System.out.println("  --speed=MS  стартовая задержка тика в миллисекундах (по умолчанию 140)");
        System.out.println("  --version   версия; --help — эта справка");
    }
}
