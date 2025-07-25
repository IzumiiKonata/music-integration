package tech.konata.musicintegration.rendering;

import lombok.Setter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import tech.konata.musicintegration.utils.Timer;

import java.util.Objects;

/**
 * @author IzumiiKonata
 * @since 2024/9/17 22:32
 */
public class ScrollText {

    Timer t = new Timer();
    double scrollOffset = 0;
    String cachedText = "";
    @Setter
    long waitTime = 2500;
    @Setter
    boolean oneShot = false;

    public ScrollText() {

    }

    public void reset() {
        t.reset();
        scrollOffset = 0;
    }

    public void render(GuiGraphics ctx, Font fr, String text, int x, int y, int width, int color) {

        if (!Objects.equals(cachedText, text)) {
            cachedText = text;

            this.reset();
        }

        int exp = 2;
        this.clip(ctx, x, y - exp, width, fr.lineHeight + exp * 2, () -> {
            ctx.drawString(fr, text, (int) (x + scrollOffset), y, color);

            int w = fr.width(text);

            if (w > width) {

                String s = "    ";
                int dest = -(w + fr.width(s));

                if (t.isDelayed(waitTime)) {
                    scrollOffset = Interpolations.interpLinear((float) scrollOffset, (float) dest, .5f);
                }

                ctx.drawString(fr, s + text, (int) (x + w + scrollOffset), y, color);

                if (Math.abs(dest - scrollOffset) == 0) {
                    scrollOffset = 0;
                    t.reset();
                }

            }
        });

    }

    private void clip(GuiGraphics ctx, int x, int y, int width, int height, Runnable renderContent) {
        ctx.enableScissor(x, y, x + width, y + height);

        try {
            renderContent.run();
        } finally {
            ctx.disableScissor();
        }
    }

}
