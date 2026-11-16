package com.dzolotarev.zmeyka;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Timer;

/** Swing-оболочка: ввод, таймер игрового цикла и отрисовка. */
public class GamePanel extends JPanel {

    private static final Color FIELD_TOP = new Color(30, 33, 42);
    private static final Color FIELD_BOTTOM = new Color(17, 19, 25);
    private static final Color GRID = new Color(255, 255, 255, 12);
    private static final Color HUD_BG = new Color(13, 15, 19);
    private static final Color TEXT = new Color(228, 232, 240);
    private static final Color TEXT_DIM = new Color(150, 158, 172);
    private static final Color ACCENT = new Color(96, 214, 133);
    private static final Color STEM = new Color(104, 76, 52);
    private static final Color LEAF = new Color(104, 182, 104);
    private static final Color TONGUE = new Color(228, 96, 96);

    private static final int HUD_HEIGHT = 36;
    private static final int POPUP_TICKS = 10;
    /** Если фрукт ближе этого числа клеток — змейка высовывает язык. */
    private static final int TONGUE_RANGE = 4;

    private final Game game;
    private final Timer timer;
    private final List<Popup> popups = new ArrayList<>();

    public GamePanel(Game game) {
        this.game = game;
        BoardConfig board = game.board();

        setPreferredSize(new Dimension(board.width(), board.height() + HUD_HEIGHT));
        setBackground(HUD_BG);
        setFocusable(true);

        this.timer = new Timer(game.tickDelayMs(), e -> step());
        addKeyListener(new InputHandler());
    }

    /** Стартуем игру и забираем фокус на панель. */
    public void beginPlay() {
        requestFocusInWindow();
        popups.clear();
        game.start();
        syncTimer();
    }

    private void step() {
        boolean over = game.tick();

        Food eaten = game.lastEaten();
        if (eaten != null) {
            popups.add(new Popup(eaten.position(), "+" + eaten.points(), eaten.kind()));
        }
        popups.removeIf(popup -> --popup.ttl <= 0);

        timer.setDelay(game.tickDelayMs());
        if (over) {
            timer.stop();
        }
        repaint();
    }

    private void syncTimer() {
        timer.setDelay(game.tickDelayMs());
        switch (game.state()) {
            case RUNNING -> timer.start();
            case PAUSED, READY, GAME_OVER -> timer.stop();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int cell = game.board().cellSize();
        int top = HUD_HEIGHT;

        paintField(g2, cell, top);
        paintHud(g2);
        paintFood(g2, cell, top);
        paintSnake(g2, cell, top);
        paintPopups(g2, cell, top);

        if (game.state() != Game.State.RUNNING) {
            paintOverlay(g2);
        }
        g2.dispose();
    }

    /** Мягкий вертикальный градиент поля, рамка и едва заметная сетка. */
    private void paintField(Graphics2D g2, int cell, int top) {
        BoardConfig board = game.board();
        g2.setPaint(new GradientPaint(0, top, FIELD_TOP, 0, top + board.height(), FIELD_BOTTOM));
        g2.fillRect(0, top, board.width(), board.height());

        g2.setColor(GRID);
        for (int x = 1; x < board.columns(); x++) {
            g2.drawLine(x * cell, top, x * cell, top + board.height());
        }
        for (int y = 1; y < board.rows(); y++) {
            g2.drawLine(0, top + y * cell, board.width(), top + y * cell);
        }

        g2.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 42));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(1, top + 1, board.width() - 3, board.height() - 3, 10, 10);
    }

    private void paintHud(Graphics2D g2) {
        g2.setColor(HUD_BG);
        g2.fillRect(0, 0, getWidth(), HUD_HEIGHT);
        g2.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 60));
        g2.drawLine(0, HUD_HEIGHT - 1, getWidth(), HUD_HEIGHT - 1);

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        int y = (HUD_HEIGHT + fm.getAscent()) / 2 - 2;

        int members = FamilyPalette.membersOnBoard(game.snake().length());
        g2.setColor(TEXT);
        g2.drawString("Счёт: " + game.score(), 12, y);
        g2.setColor(ACCENT);
        g2.drawString("Семейка: " + members, 12 + fm.stringWidth("Счёт: " + game.score() + "   "), y);

        g2.setColor(TEXT_DIM);
        String right = "Рекорд: " + game.bestScore() + "    Скорость: " + game.tickDelayMs() + " мс";
        g2.drawString(right, getWidth() - fm.stringWidth(right) - 12, y);
    }

    private void paintFood(Graphics2D g2, int cell, int top) {
        Food food = game.food();
        if (food == null) {
            return;
        }
        double cx = food.position().x() * cell + cell / 2.0;
        double cy = top + food.position().y() * cell + cell / 2.0;

        g2.setColor(withAlpha(food.kind().color, 46));
        double glow = cell * 0.62;
        g2.fill(new Ellipse2D.Double(cx - glow, cy - glow, glow * 2, glow * 2));

        drawFruit(g2, food.kind(), cx, cy, cell / 2.0 - 1);
    }

    private void drawFruit(Graphics2D g2, FruitKind kind, double cx, double cy, double r) {
        switch (kind) {
            case PEAR -> drawPear(g2, kind, cx, cy, r);
            case CHERRY -> drawCherry(g2, kind, cx, cy, r);
            default -> drawApple(g2, kind, cx, cy, r);
        }
    }

    private void drawApple(Graphics2D g2, FruitKind kind, double cx, double cy, double r) {
        double body = r * 0.86;
        double bodyCy = cy + r * 0.12;
        g2.setColor(kind.color);
        g2.fill(new Ellipse2D.Double(cx - body, bodyCy - body, body * 2, body * 1.9));
        g2.setColor(withAlpha(kind.highlight, 190));
        g2.fill(new Ellipse2D.Double(cx - body * 0.6, bodyCy - body * 0.66, body * 0.62, body * 0.5));
        drawStemAndLeaf(g2, cx, bodyCy - body, r);
    }

    private void drawPear(Graphics2D g2, FruitKind kind, double cx, double cy, double r) {
        double bottom = r * 0.78;
        double bottomCy = cy + r * 0.4;
        double neck = r * 0.5;
        double neckCy = cy - r * 0.3;
        g2.setColor(kind.color);
        g2.fill(new Ellipse2D.Double(cx - bottom, bottomCy - bottom, bottom * 2, bottom * 2));
        g2.fill(new Ellipse2D.Double(cx - neck, neckCy - neck, neck * 2, neck * 2));
        g2.setColor(withAlpha(kind.highlight, 180));
        g2.fill(new Ellipse2D.Double(cx - bottom * 0.55, bottomCy - bottom * 0.5, bottom * 0.5, bottom * 0.45));
        drawStemAndLeaf(g2, cx, neckCy - neck, r);
    }

    private void drawCherry(Graphics2D g2, FruitKind kind, double cx, double cy, double r) {
        double berry = r * 0.52;
        double leftCx = cx - r * 0.5;
        double rightCx = cx + r * 0.45;
        double berryCy = cy + r * 0.42;
        double knotX = cx + r * 0.05;
        double knotY = cy - r * 0.82;

        Stroke previous = g2.getStroke();
        g2.setColor(STEM);
        g2.setStroke(new BasicStroke((float) Math.max(1.2, r * 0.11), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D stems = new Path2D.Double();
        stems.moveTo(leftCx, berryCy - berry * 0.7);
        stems.quadTo(cx - r * 0.2, cy - r * 0.2, knotX, knotY);
        stems.moveTo(rightCx, berryCy - berry * 0.7);
        stems.quadTo(cx + r * 0.3, cy - r * 0.25, knotX, knotY);
        g2.draw(stems);
        g2.setStroke(previous);

        g2.setColor(kind.color);
        g2.fill(new Ellipse2D.Double(leftCx - berry, berryCy - berry, berry * 2, berry * 2));
        g2.fill(new Ellipse2D.Double(rightCx - berry, berryCy - berry, berry * 2, berry * 2));
        g2.setColor(withAlpha(kind.highlight, 210));
        g2.fill(new Ellipse2D.Double(leftCx - berry * 0.5, berryCy - berry * 0.6, berry * 0.5, berry * 0.42));
        g2.fill(new Ellipse2D.Double(rightCx - berry * 0.45, berryCy - berry * 0.55, berry * 0.45, berry * 0.38));

        g2.setColor(LEAF);
        g2.fill(new Ellipse2D.Double(knotX, knotY - r * 0.22, r * 0.62, r * 0.3));
    }

    private void drawStemAndLeaf(Graphics2D g2, double x, double y, double r) {
        Stroke previous = g2.getStroke();
        g2.setColor(STEM);
        g2.setStroke(new BasicStroke((float) Math.max(1.2, r * 0.12), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D stem = new Path2D.Double();
        stem.moveTo(x, y + r * 0.1);
        stem.quadTo(x + r * 0.05, y - r * 0.2, x + r * 0.18, y - r * 0.42);
        g2.draw(stem);
        g2.setStroke(previous);

        g2.setColor(LEAF);
        g2.fill(new Ellipse2D.Double(x + r * 0.16, y - r * 0.5, r * 0.58, r * 0.28));
    }

    private void paintSnake(Graphics2D g2, int cell, int top) {
        List<Cell> cells = game.snake().cells();
        for (int i = cells.size() - 1; i >= 1; i--) {
            Cell c = cells.get(i);
            if (!game.board().isInside(c)) {
                continue;
            }
            boolean tapered = i == cells.size() - 1 && cells.size() > 4;
            drawSegment(g2, c, i, cell, top, tapered);
        }
        Cell head = cells.get(0);
        if (game.board().isInside(head)) {
            drawTongue(g2, cell, top);
            drawSegment(g2, head, 0, cell, top, false);
            paintEyes(g2, cell, top);
        }
    }

    private void drawSegment(Graphics2D g2, Cell c, int index, int cell, int top, boolean tapered) {
        FamilyPalette.Member member = FamilyPalette.memberAt(index);
        int inset = tapered ? cell / 3 : 2;
        int x = c.x() * cell + inset;
        int y = top + c.y() * cell + inset;
        int size = cell - inset * 2;
        int arc = Math.max(4, size / 3);

        g2.setColor(member.color());
        g2.fillRoundRect(x, y, size, size, arc, arc);

        g2.setColor(withAlpha(member.shade(), 200));
        int spineInset = Math.max(2, size / 4);
        g2.fillRoundRect(x + spineInset / 2, y + spineInset / 2, size - spineInset, size - spineInset, arc, arc);

        // Спина каждого члена семьи помечена веснушкой — так видны границы «папа/мама/дети».
        if (index > 0 && index % FamilyPalette.SEGMENTS_PER_MEMBER == 0) {
            g2.setColor(withAlpha(Color.WHITE, 120));
            int dot = Math.max(3, size / 5);
            g2.fillOval(x + (size - dot) / 2, y + (size - dot) / 2, dot, dot);
        }
    }

    /** Язык наружу, когда фрукт рядом — змейка его чует. */
    private void drawTongue(Graphics2D g2, int cell, int top) {
        Food food = game.food();
        if (food == null) {
            return;
        }
        Cell head = game.snake().head();
        int distance = Math.abs(head.x() - food.position().x()) + Math.abs(head.y() - food.position().y());
        if (distance > TONGUE_RANGE) {
            return;
        }
        Direction d = game.snake().direction();
        double startX = head.x() * cell + cell / 2.0 + d.dx * cell * 0.45;
        double startY = top + head.y() * cell + cell / 2.0 + d.dy * cell * 0.45;
        double tipX = startX + d.dx * cell * 0.32;
        double tipY = startY + d.dy * cell * 0.32;
        double fork = cell * 0.12;
        double sideX = d.dx != 0 ? 0 : fork;
        double sideY = d.dx != 0 ? fork : 0;

        Stroke previous = g2.getStroke();
        g2.setColor(TONGUE);
        g2.setStroke(new BasicStroke(Math.max(1.5f, cell * 0.08f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D tongue = new Path2D.Double();
        tongue.moveTo(startX, startY);
        tongue.lineTo(tipX, tipY);
        tongue.moveTo(tipX, tipY);
        tongue.lineTo(tipX + d.dx * fork - sideX, tipY + d.dy * fork - sideY);
        tongue.moveTo(tipX, tipY);
        tongue.lineTo(tipX + d.dx * fork + sideX, tipY + d.dy * fork + sideY);
        g2.draw(tongue);
        g2.setStroke(previous);
    }

    private void paintEyes(Graphics2D g2, int cell, int top) {
        Cell head = game.snake().head();
        Direction d = game.snake().direction();
        double cx = head.x() * cell + cell / 2.0;
        double cy = top + head.y() * cell + cell / 2.0;
        double forward = cell * 0.22;
        double apart = cell * 0.19;
        double eye = Math.max(2.5, cell * 0.1);
        double pupil = eye * 0.55;

        double offsetX = d.dx * forward;
        double offsetY = d.dy * forward;
        double sideX = d.dy != 0 ? apart : 0;
        double sideY = d.dx != 0 ? apart : 0;

        for (int sign : new int[]{-1, 1}) {
            double ex = cx + offsetX + sideX * sign;
            double ey = cy + offsetY + sideY * sign;
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Double(ex - eye, ey - eye, eye * 2, eye * 2));
            g2.setColor(new Color(22, 24, 30));
            g2.fill(new Ellipse2D.Double(ex + d.dx * eye * 0.35 - pupil, ey + d.dy * eye * 0.35 - pupil,
                                         pupil * 2, pupil * 2));
        }
    }

    private void paintPopups(Graphics2D g2, int cell, int top) {
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        int boardWidth = game.board().width();
        for (Popup popup : popups) {
            float alpha = (float) popup.ttl / POPUP_TICKS;
            Graphics2D faded = (Graphics2D) g2.create();
            faded.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            String text = popup.text + " " + popup.kind.label;
            double half = fm.stringWidth(text) / 2.0;
            double rise = (POPUP_TICKS - popup.ttl) * 2.0;
            double x = Math.min(boardWidth - half - 4, Math.max(half + 4, popup.at.x() * cell + cell / 2.0));
            double y = Math.max(top + 14, top + popup.at.y() * cell - rise);
            faded.setColor(new Color(0, 0, 0, (int) (140 * alpha)));
            faded.drawString(text, (float) (x - half + 1), (float) (y + 1));
            faded.setColor(popup.kind.highlight);
            faded.drawString(text, (float) (x - half), (float) y);
            faded.dispose();
        }
    }

    private void paintOverlay(Graphics2D g2) {
        BoardConfig board = game.board();
        g2.setColor(new Color(0, 0, 0, 175));
        g2.fillRect(0, HUD_HEIGHT, board.width(), board.height());

        int centerY = HUD_HEIGHT + board.height() / 2;
        String title;
        Color titleColor;
        switch (game.state()) {
            case READY -> {
                title = "ЗМЕЙКА";
                titleColor = ACCENT;
            }
            case PAUSED -> {
                title = "ПАУЗА";
                titleColor = TEXT;
            }
            default -> {
                title = "ИГРА ОКОНЧЕНА";
                titleColor = new Color(232, 100, 100);
            }
        }

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 36));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(titleColor);
        g2.drawString(title, (board.width() - fm.stringWidth(title)) / 2, centerY - 40);

        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        fm = g2.getFontMetrics();
        String hint = switch (game.state()) {
            case READY -> "Старт — пробел или стрелки. WASD тоже. P — пауза, R — заново";
            case PAUSED -> "P или пробел — продолжить, R — начать заново";
            default -> "Пробел или R — сыграть ещё раз";
        };
        g2.setColor(TEXT_DIM);
        g2.drawString(hint, (board.width() - fm.stringWidth(hint)) / 2, centerY - 14);

        if (game.state() == Game.State.READY) {
            paintFruitRow(g2, centerY + 16, List.of(FruitKind.APPLE_RED, FruitKind.APPLE_GREEN, FruitKind.APPLE_YELLOW));
            paintFruitRow(g2, centerY + 48, List.of(FruitKind.PEAR, FruitKind.CHERRY));
            paintFamilyLegend(g2, centerY + 86);
        } else {
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            fm = g2.getFontMetrics();
            g2.setColor(TEXT);
            String score = "Счёт: " + game.score() + "    Рекорд: " + game.bestScore();
            g2.drawString(score, (board.width() - fm.stringWidth(score)) / 2, centerY + 20);
        }
    }

    /** Строка легенды угощений: центрируется, при узком поле подписи сокращаются до очков. */
    private void paintFruitRow(Graphics2D g2, int y, List<FruitKind> kinds) {
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        FontMetrics fm = g2.getFontMetrics();
        int icon = 22;
        int gap = 16;

        java.util.function.Function<FruitKind, String> caption =
            kind -> kind.label + " +" + kind.points;
        int total = rowWidth(kinds, caption, fm, icon, gap);
        if (total > game.board().width()) {
            caption = kind -> "+" + kind.points;
            total = rowWidth(kinds, caption, fm, icon, gap);
        }

        int x = (game.board().width() - total) / 2;
        for (FruitKind kind : kinds) {
            String label = caption.apply(kind);
            drawFruit(g2, kind, x + icon / 2.0, y + icon / 2.0, icon / 2.0 - 2);
            g2.setColor(TEXT);
            g2.drawString(label, x + icon + 6, y + icon / 2 + fm.getAscent() / 2 - 1);
            x += icon + 6 + fm.stringWidth(label) + gap;
        }
    }

    private int rowWidth(List<FruitKind> kinds, java.util.function.Function<FruitKind, String> caption,
                         FontMetrics fm, int icon, int gap) {
        int total = 0;
        for (FruitKind kind : kinds) {
            total += icon + 6 + fm.stringWidth(caption.apply(kind)) + gap;
        }
        return total - gap;
    }

    /** Кто ползает по полю: имя и цвет сегментов. */
    private void paintFamilyLegend(Graphics2D g2, int y) {
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        FontMetrics fm = g2.getFontMetrics();
        List<FamilyPalette.Member> family = FamilyPalette.family();
        int chip = 14;
        int gap = 18;

        int total = 0;
        for (FamilyPalette.Member member : family) {
            total += chip + 6 + fm.stringWidth(member.name()) + gap;
        }
        total -= gap;

        int x = (game.board().width() - total) / 2;
        for (FamilyPalette.Member member : family) {
            g2.setColor(member.color());
            g2.fillRoundRect(x, y, chip, chip, 5, 5);
            g2.setColor(member.shade());
            g2.fillRoundRect(x + 3, y + 3, chip - 6, chip - 6, 4, 4);
            g2.setColor(TEXT);
            g2.drawString(member.name(), x + chip + 6, y + chip / 2 + fm.getAscent() / 2 - 1);
            x += chip + 6 + fm.stringWidth(member.name()) + gap;
        }
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, alpha));
    }

    /** Всплывающие очки над съеденным фруктом. */
    private static final class Popup {
        private final Cell at;
        private final String text;
        private final FruitKind kind;
        private int ttl = POPUP_TICKS;

        private Popup(Cell at, String text, FruitKind kind) {
            this.at = at;
            this.text = text;
            this.kind = kind;
        }
    }

    /** Вся логика ввода выполняется на EDT вместе с таймером — гонок нет. */
    private class InputHandler extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP, KeyEvent.VK_W -> steer(Direction.UP);
                case KeyEvent.VK_DOWN, KeyEvent.VK_S -> steer(Direction.DOWN);
                case KeyEvent.VK_LEFT, KeyEvent.VK_A -> steer(Direction.LEFT);
                case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> steer(Direction.RIGHT);
                case KeyEvent.VK_P, KeyEvent.VK_SPACE, KeyEvent.VK_ENTER -> toggleAction();
                case KeyEvent.VK_R -> {
                    game.restart();
                    beginPlay();
                }
                case KeyEvent.VK_ESCAPE -> {
                    timer.stop();
                    System.exit(0);
                }
                default -> {
                    return;
                }
            }
            repaint();
        }

        /** Стрелка с READY сразу начинает игру — не нужно жать пробел. */
        private void steer(Direction direction) {
            if (game.state() == Game.State.READY) {
                beginPlay();
            }
            game.turn(direction);
        }

        /** Пробел/P: пауза во время игры, старт — на READY и GAME_OVER. */
        private void toggleAction() {
            switch (game.state()) {
                case READY -> beginPlay();
                case GAME_OVER -> beginPlay();
                default -> {
                    game.togglePause();
                    syncTimer();
                }
            }
        }
    }
}
