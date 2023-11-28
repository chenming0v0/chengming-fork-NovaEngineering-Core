package github.kasuminova.mmce.client.gui.widget.container;

import github.kasuminova.mmce.client.gui.util.MousePos;
import github.kasuminova.mmce.client.gui.util.RenderPos;
import github.kasuminova.mmce.client.gui.util.RenderSize;
import github.kasuminova.mmce.client.gui.widget.base.DynamicWidget;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;

public abstract class WidgetContainer extends DynamicWidget {
    protected int absX;
    protected int absY;

    public static void enableScissor(final GuiContainer gui, final RenderSize renderSize, final RenderPos renderPos, final int width, final int height) {
        int offsetX = renderPos.posX();
        int offsetY = renderPos.posY();

        if (renderSize.isLimited()) {
            ScaledResolution res = new ScaledResolution(gui.mc);
            Rectangle scissorFrame = new Rectangle(
                    offsetX * res.getScaleFactor(),
                    offsetY * res.getScaleFactor(),
                    (renderSize.isWidthLimited() ? renderSize.width() : width) * res.getScaleFactor(),
                    (renderSize.isHeightLimited() ? renderSize.height() : height) * res.getScaleFactor()
            );
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(scissorFrame.x, scissorFrame.y, scissorFrame.width, scissorFrame.height);
        }
    }

    public static void disableScissor(final RenderSize renderSize) {
        if (renderSize.isLimited()) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    @Override
    public final void preRender(final GuiContainer gui, final RenderSize renderSize, final RenderPos renderPos, final MousePos mousePos) {
//        enableScissor(gui, renderSize, renderPos, getWidth(), getHeight());

        preRenderInternal(gui, renderSize, renderPos, mousePos);

//        disableScissor(renderSize);
    }

    @Override
    public final void postRender(final GuiContainer gui, final RenderSize renderSize, final RenderPos renderPos, final MousePos mousePos) {
//        enableScissor(gui, renderSize, renderPos, getWidth(), getHeight());

        postRenderInternal(gui, renderSize, renderPos, mousePos);

//        disableScissor(renderSize);
    }

    protected abstract void preRenderInternal(final GuiContainer gui, final RenderSize renderSize, final RenderPos renderPos, final MousePos mousePos);

    protected abstract void postRenderInternal(final GuiContainer gui, final RenderSize renderSize, final RenderPos renderPos, final MousePos mousePos);

    public abstract List<DynamicWidget> getWidgets();

    public abstract WidgetContainer addWidget(DynamicWidget widget);

    // GUI EventHandlers

    @Override
    public abstract void update(final GuiContainer gui);

    @Override
    public abstract boolean onMouseClicked(final MousePos mousePos, final RenderPos renderPos, final int mouseButton);

    @Override
    public abstract boolean onMouseReleased(final MousePos mousePos, final RenderPos renderPos);

    @Override
    public abstract boolean onMouseDWheel(final MousePos mousePos, final RenderPos renderPos, final int wheel);

    @Override
    public abstract boolean onKeyTyped(final char typedChar, final int keyCode);

    // Tooltips

    @Override
    public List<String> getHoverTooltips() {
        return super.getHoverTooltips();
    }

    // Getter Setters

    public int getAbsX() {
        return absX;
    }

    public WidgetContainer setAbsX(final int absX) {
        this.absX = absX;
        return this;
    }

    public int getAbsY() {
        return absY;
    }

    public WidgetContainer setAbsY(final int absY) {
        this.absY = absY;
        return this;
    }

}