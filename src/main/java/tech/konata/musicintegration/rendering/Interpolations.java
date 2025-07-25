package tech.konata.musicintegration.rendering;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import tech.konata.musicintegration.MusicIntegration;

/**
 * 一个实用的补间动画类。
 *
 * @author IzumiiKonata
 * @since 2023/12/30
 */
public class Interpolations {

    public static double frameDelta = 0.0;
    static long lastNanoFrame = System.nanoTime();

    public static void init() {
        HudElementRegistry.addLast(ResourceLocation.fromNamespaceAndPath(MusicIntegration.MOD_ID, "music_integration_interpolations"), new HudElement() {
            @Override
            public void render(GuiGraphics context, DeltaTracker tickCounter) {
                Interpolations.calcDelta();
            }
        });
    }

    /**
     * 计算帧时间
     */
    public static void calcDelta() {
        double value = ((double) System.nanoTime() - (double) lastNanoFrame) / 10000000.0;

//        if (value > 0.2) {
//            System.out.println(value);
//            value = 0.01;
//        }

        frameDelta = value;
        lastNanoFrame = System.nanoTime();
    }

    public static double lerp(final double d, final double e, final double f) {
        return e + d * (f - e);
    }

    /**
     * interpolate animation
     *
     * @param startValue start
     * @param endValue   end
     * @param fraction   speed
     * @return animation value
     */
    public static double interpBezier(double startValue, double endValue,
                                      double fraction) {
        boolean increasing = startValue < endValue;

        double result = lerp(getDelta() * fraction, startValue, endValue);

        if (increasing) {
            return Math.min(endValue, result);
        } else {
            return Math.max(endValue, result);
        }

    }

    public static float interpBezier(float startValue, float endValue,
                                     float fraction) {
        boolean increasing = startValue < endValue;

        float result = (float) lerp(getDelta() * fraction, startValue, endValue);

        if (increasing) {
            return Math.min(endValue, result);
        } else {
            return Math.max(endValue, result);
        }
    }

    public static double getDelta() {
        return frameDelta * 0.5;
    }

    public static float interpLinear(float now, float end, float interpolation) {
        float add = (float) (getDelta() * interpolation);
        if (now < end) {
            if (now + add < end)
                now += add;
            else
                now = end;
        } else {
            if (now - add > end)
                now -= add;
            else
                now = end;
        }
        return now;
    }

}
