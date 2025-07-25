package tech.konata.musicintegration.rendering;

import com.mojang.blaze3d.platform.NativeImage;
import lombok.Cleanup;
import lombok.SneakyThrows;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;
import tech.konata.musicintegration.MusicIntegration;
import tech.konata.musicintegration.PlayingMusicData;
import tech.konata.musicintegration.config.ModConfig;
import tech.konata.musicintegration.integration.IIntegrationCallback;
import tech.konata.musicintegration.integration.IIntegrationProvider;
import tech.konata.musicintegration.integration.Music;
import tech.konata.musicintegration.utils.HttpUtils;
import tech.konata.musicintegration.utils.MultiThreadingUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.Objects;

/**
 * @author IzumiiKonata
 * Date: 2025/7/24 21:54
 */
public final class MusicIntegrationRenderer implements HudElement, IIntegrationCallback {

    public static final MusicIntegrationRenderer INSTANCE = new MusicIntegrationRenderer();

    private static final ResourceLocation MUSIC_INTEGRATION_ELEMENT_ID = ResourceLocation.fromNamespaceAndPath(MusicIntegration.MOD_ID, "music_integration_hud");
    float musicBgAlpha = 0.0f;
    ResourceLocation prevBg = null;
    Music prevMusic = null;
    float alpha = 0.0f;
    ScrollText musicName = new ScrollText();
    ScrollText artists = new ScrollText();
    private boolean isDragging = false;
    private double dragStartX, dragStartY;

    public MusicIntegrationRenderer() {
        MusicIntegration.autoRegisterCallback(this::addCallbacks);
    }

    public static ResourceLocation getMusicCover(Music music) {
        return ResourceLocation.fromNamespaceAndPath(MusicIntegration.MOD_ID, "textures/cover/" + music.id + ".png");
    }

    public static void register() {
        HudElementRegistry.addLast(MUSIC_INTEGRATION_ELEMENT_ID, MusicIntegrationRenderer.INSTANCE);
        MusicIntegration.autoRegisterCallback(() -> {
            MusicIntegration.getProvider().registerCallback(MusicIntegrationRenderer.INSTANCE);
        });
    }

    public void addCallbacks() {
        IIntegrationProvider provider = MusicIntegration.getProvider();
        provider.registerCallback(this);
    }

    @Override
    public void render(GuiGraphics ctx, DeltaTracker tickCounter) {

        int width = this.getWidth(), height = this.getHeight();

        ModConfig.UIPosition uiPosition = MusicIntegration.CONFIG.uiPosition;
        int posX = uiPosition.posX, posY = uiPosition.posY;

        if (this.isInDraggingMode())
            this.handleDragging();

        Music playingMusic = PlayingMusicData.getPlayingMusic();
        double position = PlayingMusicData.getPosition();
        boolean canRender = playingMusic != null;

        Minecraft mc = Minecraft.getInstance();

        alpha = Interpolations.interpBezier(alpha, canRender ? 1 : 0, canRender ? 0.15f : 0.2f);

        if (playingMusic != null) {

            ResourceLocation cover = getMusicCover(playingMusic);

            boolean textureLoaded = this.isTextureLoaded(cover);
//            AbstractTexture texture = textureLoaded ? mc.getTextureManager().getTexture(cover) : null;

            int imgSpacing = 2;

            int imgX = posX + imgSpacing;

            int y = posY;
            int imgY = y + imgSpacing;

            int imgSize = height - imgSpacing * 2;

            Matrix3x2fStack stack = ctx.pose();

            stack.pushMatrix();

            stack.translate(posX, posY);
            stack.scale((float) MusicIntegration.CONFIG.scale);
            stack.translate(-posX, -posY);

            this.drawRect(ctx, posX, posY, width, height, this.hexColor(0, 0, 0, 80));

            if (textureLoaded) {
                ctx.blit(RenderPipelines.GUI_TEXTURED, cover, imgX, imgY, 0, 0, imgSize, imgSize, imgSize, imgSize);
            }

            String secondaryText = String.join(" / ", playingMusic.getArtists());

            artists.setWaitTime(2000L);
            artists.setOneShot(false);

            int progressBarWidth = (int) (width - (imgSize + imgSpacing * 3.25));

            String name = playingMusic.getName();

            musicName.render(ctx, mc.font, name, imgX + imgSize + imgSpacing, imgY + 2, progressBarWidth, new Color(1f, 1f, 1f, alpha).getRGB());

            double progressBarOffsetY = y + height - imgSpacing - 3 - mc.font.lineHeight - 4;

            artists.render(ctx, mc.font, secondaryText, imgX + imgSize + imgSpacing, (int) (imgY + mc.font.lineHeight + (progressBarOffsetY - (imgY + 2 + mc.font.lineHeight)) * 0.5 - mc.font.lineHeight * 0.5 + 2), progressBarWidth, new Color(1f, 1f, 1f, alpha * 0.8f).getRGB());

            this.drawRect(ctx, imgX + imgSize + imgSpacing, progressBarOffsetY, progressBarWidth, 4, this.hexColor(255, 255, 255, (int) (alpha * 0.3f * 255)));
            this.drawRect(ctx, imgX + imgSize + imgSpacing, progressBarOffsetY, (float) ((progressBarWidth) * (position / playingMusic.duration)), 4, this.hexColor(233, 233, 233, (int) (alpha * 255)));

            double currentTimeSeconds = (int) (position / 1000.0);
            double totalTimeSeconds = (int) (playingMusic.duration / 1000.0);

            int cMin = (int) (currentTimeSeconds / 60);
            int cSec = (int) (currentTimeSeconds - cMin * 60);
            String currentTime = (cMin < 10 ? "0" + cMin : cMin) + ":" + (cSec < 10 ? "0" + cSec : cSec);
            int tMin = (int) (totalTimeSeconds / 60);
            int tSec = (int) (totalTimeSeconds - tMin * 60);
            String totalTime = (tMin < 10 ? "0" + tMin : tMin) + ":" + (tSec < 10 ? "0" + tSec : tSec);

            int textColor = this.hexColor(255, 255, 255, (int) (alpha * 128));
            double playbackTimeY = progressBarOffsetY + 9;

            stack.pushMatrix();
            stack.translate((float) (imgX + imgSize + imgSpacing), (float) playbackTimeY);
            stack.scale(.75f);
            ctx.drawString(mc.font, currentTime, 0, 0, textColor);
            stack.popMatrix();

            stack.pushMatrix();
            stack.translate(imgX + imgSize + imgSpacing + progressBarWidth - mc.font.width(totalTime) * .75f, (float) playbackTimeY);
            stack.scale(.75f);
            ctx.drawString(mc.font, totalTime, 0, 0, textColor);
            stack.popMatrix();


            stack.popMatrix();
        }


    }

    @Override
    public void onMusicChanged(Music music) {
        loadMusicCover(music, false);
    }

    @Override
    public void onPreloadNext(String musicId) {
        loadMusicCover(Music.builder().id(musicId).coverType(Music.CoverType.URL).build(), false);
    }

    @SneakyThrows
    private void loadCover(BufferedImage img, Music music, int imgSize) {
        ResourceLocation musicCover = getMusicCover(music);

        imgSize *= 4;
        if (img.getWidth() != imgSize || img.getHeight() != imgSize) {
            BufferedImage resizedImg = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB);

            Graphics2D graphics = (Graphics2D) resizedImg.getGraphics();

            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);


            graphics.drawImage(img, 0, 0, imgSize, imgSize, null);
            graphics.dispose();
            img = resizedImg;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        NativeImage nativeImage = NativeImage.read(baos.toByteArray());

        Minecraft.getInstance().execute(() -> {
            DynamicTexture dt = new DynamicTexture(() -> musicCover.toString(), nativeImage);
            Minecraft.getInstance().getTextureManager().register(musicCover, dt);
        });
    }

    @Override
    public void onCoverChanged(String cover, Music.CoverType type) {

        Music playingMusic = PlayingMusicData.getPlayingMusic();

        if (playingMusic == null)
            return;

        ResourceLocation musicCover = getMusicCover(playingMusic);
        boolean textureLoaded = this.isTextureLoaded(musicCover);

        if (type == Music.CoverType.Base64 && cover != null && !textureLoaded) {
            MultiThreadingUtil.runAsync(new Runnable() {
                @Override
                @SneakyThrows
                public void run() {
                    byte[] bytes = Base64.getDecoder().decode(cover);
                    BufferedImage read = ImageIO.read(new ByteArrayInputStream(bytes));

                    loadCover(read, playingMusic, 96);
                }
            });
        }

    }

    @SneakyThrows
    private String getCover(String id) {
        String string = HttpUtils.getString("https://music.163.com/song?id=" + id, null);

        String[] split = string.split("\n");

        for (String s : split) {
            if (s.startsWith("<meta property=\"og:image\" content=\"")) {
                return s.substring("<meta property=\"og:image\" content=\"".length(), s.lastIndexOf("\""));
            }
        }

        return null;
    }

    public void loadMusicCover(Music music, boolean forceReload) {

        ResourceLocation musicCover = getMusicCover(music);
        boolean textureLoaded = this.isTextureLoaded(musicCover);

        if (textureLoaded && !forceReload)
            return;

        MultiThreadingUtil.runAsync(new Runnable() {
            @Override
            @SneakyThrows
            public void run() {

                String cover = music.cover;

                boolean ncm = MusicIntegration.isProvider(ModConfig.ProviderType.NCM);
                if (cover == null && ncm) {
                    cover = getCover(music.id);

                    if (cover == null)
                        return;
                }

                final BufferedImage read;

                if (Objects.requireNonNull(music.coverType) == Music.CoverType.URL) {
                    @Cleanup
                    InputStream is = HttpUtils.downloadStream(cover + (ncm ? "?param=160y160" : ""), 5);
                    read = ImageIO.read(is);
                } else if (music.coverType == Music.CoverType.Base64) {

                    if (music.cover != null) {
                        byte[] bytes = Base64.getDecoder().decode(music.cover);
                        read = ImageIO.read(new ByteArrayInputStream(bytes));
                    } else {
                        return;
                    }

                } else {
                    throw new IllegalArgumentException("Unknown cover type: " + music.coverType);
                }

                loadCover(read, music, 96);

            }
        });
    }

    private boolean isTextureLoaded(ResourceLocation location) {
        return Minecraft.getInstance().getTextureManager().byPath.get(location) != null;
    }

    private int getWidth() {
        return 180;
    }

    private int getHeight() {
        return 48;
    }

    private void drawRect(GuiGraphics ctx, double x, double y, double width, double height, int color) {
        ctx.fill((int) x, (int) y, (int) (x + width), (int) (y + height), color);
    }

    private boolean isHovering(double mouseX, double mouseY, double x, double y, double width, double height) {
        if (width < 0) {
            width = -width;
            x -= width;
        }

        if (height < 0) {
            height = -height;
            y -= height;
        }

        return mouseX >= x && mouseY >= y && mouseX <= x + width && mouseY <= y + height;
    }

    private void handleDragging() {

        Minecraft client = Minecraft.getInstance();

        ModConfig config = MusicIntegration.CONFIG;

        int width = (int) (this.getWidth() * config.scale), height = (int) (this.getHeight() * config.scale);

        ModConfig.UIPosition uiPosition = config.uiPosition;
        int posX = uiPosition.posX, posY = uiPosition.posY;

        double mouseX = client.mouseHandler.xpos() * client.getWindow().getGuiScaledWidth() / client.getWindow().getWidth();
        double mouseY = client.mouseHandler.ypos() * client.getWindow().getGuiScaledHeight() / client.getWindow().getHeight();

        boolean isMousePressed = GLFW.glfwGetMouseButton(client.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (isMousePressed && !isDragging) {
            if (this.isHovering(mouseX, mouseY, posX, posY, width, height)) {
                isDragging = true;
                dragStartX = mouseX - posX;
                dragStartY = mouseY - posY;
            }
        } else if (!isMousePressed && isDragging) {
            isDragging = false;
            dragStartX = dragStartY = 0;
        } else if (isDragging) {
            uiPosition.posX = (int) (mouseX - dragStartX);
            uiPosition.posY = (int) (mouseY - dragStartY);
        }

    }

    private boolean isInDraggingMode() {
        return Minecraft.getInstance().screen instanceof ChatScreen;
    }

    public int hexColor(int red, int green, int blue) {
        return hexColor(red, green, blue, 255);
    }

    public int hexColor(int red, int green, int blue, int alpha) {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    // clipping utility
    private void clip(GuiGraphics ctx, int x, int y, int width, int height, Runnable renderContent) {
        ctx.enableScissor(x, y, x + width, y + height);

        try {
            renderContent.run();
        } finally {
            ctx.disableScissor();
        }
    }

}
