package ru.mam.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Оверлей по центру экрана: плохие правила слева от центра, хорошие справа.
 * Показывается 10 секунд после возрождения.
 */
public final class RuleOverlay {
    public record Line(String text, boolean harmful) {
    }

    private static final long SHOW_MILLIS = 10_000;
    private static final int GAP = 16;
    private static final int ROW = 14;
    private static volatile List<Line> lines = List.of();
    private static volatile long shownAt = 0;

    private RuleOverlay() {
    }

    public static void show(List<Line> newLines) {
        lines = newLines;
        shownAt = System.currentTimeMillis();
    }

    public static List<Line> parse(String text) {
        List<Line> result = new ArrayList<>();
        for (String raw : text.split("\n")) {
            if (raw.isEmpty()) {
                continue;
            }
            boolean harmful = raw.charAt(0) == '1';
            result.add(new Line(raw.substring(1), harmful));
        }
        return result;
    }

    public static void render(DrawContext context) {
        List<Line> current = lines;
        if (current.isEmpty() || System.currentTimeMillis() - shownAt > SHOW_MILLIS) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer font = client.textRenderer;
        int centerX = client.getWindow().getScaledWidth() / 2;
        int top = client.getWindow().getScaledHeight() / 3;

        List<String> bad = new ArrayList<>();
        List<String> good = new ArrayList<>();
        for (Line line : current) {
            (line.harmful() ? bad : good).add(line.text());
        }

        int leftWidth = Math.max(font.getWidth("Опасно"), 0);
        for (String text : bad) {
            leftWidth = Math.max(leftWidth, font.getWidth(text));
        }
        int rightWidth = Math.max(font.getWidth("Полезно"), 0);
        for (String text : good) {
            rightWidth = Math.max(rightWidth, font.getWidth(text));
        }

        int rows = Math.max(bad.size(), good.size());
        int bgLeft = centerX - GAP - leftWidth - 8;
        int bgRight = centerX + GAP + rightWidth + 8;
        int bgTop = top - 6;
        int bgBottom = top + 26 + rows * ROW;
        context.fill(bgLeft, bgTop, bgRight, bgBottom, 0x90000000);

        String title = "Новые правила";
        context.drawText(font, title, centerX - font.getWidth(title) / 2, top, 0xFFFFFFFF, true);

        int headerY = top + 14;
        String badHeader = "Опасно";
        context.drawText(font, badHeader, centerX - GAP - font.getWidth(badHeader), headerY, 0xFFFF8888, true);
        context.drawText(font, "Полезно", centerX + GAP, headerY, 0xFF88FF88, true);

        int y = top + 28;
        for (int i = 0; i < rows; i++) {
            if (i < bad.size()) {
                String text = bad.get(i);
                context.drawText(font, text, centerX - GAP - font.getWidth(text), y, 0xFFFF5555, true);
            }
            if (i < good.size()) {
                context.drawText(font, good.get(i), centerX + GAP, y, 0xFF55FF55, true);
            }
            y += ROW;
        }
    }
}
