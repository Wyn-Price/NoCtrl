package com.wynprice.noctrl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;

public class GuiSelectList {

    private static Minecraft mc = Minecraft.getMinecraft();

    private static final int CELL_WIDTH = 150;
    private static final int CELL_HEIGHT = 20;

    private static final int CELL_MAX = 5;

    private final int xPos;
    private final int yPos;

    private boolean open;
    private int scroll;

    public GuiSelectList(int xPos, int yPos) {
        this.xPos = xPos;
        this.yPos = yPos;
    }

    public void render(int mouseX, int mouseY) {
        int relX = mouseX - this.xPos;
        int relY = mouseY - this.yPos;

        int listedCells = Math.min(NoCtrl.ALL_LISTS.size(), CELL_MAX);
        int totalCells = listedCells + 1;

        int height = CELL_HEIGHT + (this.open ? listedCells * CELL_HEIGHT : 0);
        int borderSize = 1;
        int borderColor = -1;
        int insideColor = 0xFF000000;
        int insideSelectionColor = 0xFF303030;
        int highlightColor = 0x2299bbff;
        mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderHelper.enableGUIStandardItemLighting();

        Gui.drawRect(this.xPos, this.yPos, this.xPos + CELL_WIDTH, this.yPos + CELL_HEIGHT, insideColor);
        mc.getRenderItem().renderItemIntoGUI(new ItemStack(NoCtrl.ACTIVE.getModel()), this.xPos + 5, this.yPos + 2);
        mc.fontRenderer.drawString(NoCtrl.ACTIVE.getName(), this.xPos + 26, this.yPos + CELL_HEIGHT / 2 - 4, -1);
        if(this.open) {
			//scroll can point beyond what exists
			this.scroll = MathHelper.clamp(this.scroll, 0, Math.max(NoCtrl.ALL_LISTS.size() - CELL_MAX, 0));
            for (int i = 0; i < listedCells; i++) {
                int actual = i + this.scroll;
                int yStart = this.yPos + CELL_HEIGHT * (i + 1);
                Gui.drawRect(this.xPos, yStart, this.xPos + CELL_WIDTH, yStart + CELL_HEIGHT, insideSelectionColor);
                Gui.drawRect(this.xPos, yStart, this.xPos + CELL_WIDTH, yStart + borderSize, borderColor);
                mc.getRenderItem().renderItemIntoGUI(new ItemStack(NoCtrl.ALL_LISTS.get(actual).getModel()), this.xPos + 5, yStart + 2);
                mc.fontRenderer.drawString(NoCtrl.ALL_LISTS.get(actual).getName(), this.xPos + 26, yStart + CELL_HEIGHT / 2 - 4, -1);
            }
        }
        Gui.drawRect(this.xPos, this.yPos, this.xPos + CELL_WIDTH, this.yPos + borderSize, borderColor);
        Gui.drawRect(this.xPos, this.yPos + height, this.xPos + CELL_WIDTH, this.yPos + height - borderSize, borderColor);
        Gui.drawRect(this.xPos, this.yPos, this.xPos + borderSize, this.yPos + height, borderColor);
        Gui.drawRect(this.xPos + CELL_WIDTH, this.yPos, this.xPos + CELL_WIDTH - borderSize, this.yPos + height, borderColor);
        if(relX > 0 && relY > 0) {
            if(relX <= CELL_WIDTH){
                if (relY <= CELL_HEIGHT) {
                    Gui.drawRect(this.xPos, this.yPos, this.xPos + CELL_WIDTH, this.yPos + CELL_HEIGHT, highlightColor);
                } else if(this.open) {
                    for (int i = 0; i < CELL_MAX; i++) {
                        if(relY <= CELL_HEIGHT * (i + 2)) {
                            int yStart = this.yPos + CELL_HEIGHT * (i + 1);
                            Gui.drawRect(this.xPos, yStart, this.xPos + CELL_WIDTH, yStart + CELL_HEIGHT, highlightColor);
                            break;
                        }
                    }
                }
            }
        }
        RenderHelper.disableStandardItemLighting();
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if(mouseButton == 0) {
            int relX = mouseX - this.xPos;
            int relY = mouseY - this.yPos;
            if(relX > 0 && relY > 0) {
                if(relX <= CELL_WIDTH ) {
                    if(relY <= CELL_HEIGHT) {
                        this.open = !this.open;
                        return;
                    } else if(this.open){
                        for (int i = 0; i < NoCtrl.ALL_LISTS.size(); i++) {
                            int i1 = i + this.scroll;
                            if(relY <= CELL_HEIGHT * (i + 2)) {
                                NoCtrl.ALL_LISTS.get(i1).setAsCurrent();
                                break;
                            }
                        }
                    }
                }
            }
        }
        this.open = false;
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        int relX = mouseX - this.xPos;
        int relY = mouseY - this.yPos;
        if(relX > 0 && relY > 0) {
            if(relX <= CELL_WIDTH ) {
                if(relY <= CELL_HEIGHT) {
                    return true;
                } else if(this.open){
                    return relY <= CELL_HEIGHT * (Math.min(NoCtrl.ALL_LISTS.size(), CELL_MAX) + 1);
                }
            }
        }
        return false;
    }

    public void scroll(int amount) {
        this.scroll -= amount;
        this.scroll = MathHelper.clamp(this.scroll, 0, Math.max(NoCtrl.ALL_LISTS.size() - CELL_MAX, 0));
    }
}
