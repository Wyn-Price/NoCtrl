package com.wynprice.noctrl.compact;

import com.google.common.collect.Lists;
import com.wynprice.noctrl.GuiSelectList;
import com.wynprice.noctrl.KeyBindSet;
import com.wynprice.noctrl.NoCtrl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import us.getfluxed.controlsearch.client.gui.GuiNewControls;
import us.getfluxed.controlsearch.client.gui.GuiNewKeyBindingList;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class GuiControlsOverrideControlling extends GuiNewControls {

    private GuiSelectList dropDown;
    private GuiTextField inputField;

    private GuiButton addFolder;
    private GuiButton removeFolder;
    private GuiButton renameFolder;
    private GuiButton editIcon;

    private GuiButton yesAction;
    private GuiButton noAction;

    private boolean showInputField;
    private boolean yesNoScreen;

    @Nullable private Consumer<String> output;
    private String currentTask = "";

    public GuiControlsOverrideControlling(GuiScreen screen, GameSettings settings) {
        super(screen, settings);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.keyBindingList.top+=27;
        this.dropDown = new GuiSelectList(this.width / 2 - 155, 66);

        this.inputField = new GuiTextField(5000, mc.fontRenderer, this.width / 2 - 155, 66, 150, 20);
        this.inputField.setCanLoseFocus(false);
        this.inputField.setFocused(true);

        this.addFolder = this.addButton(new GuiTypeButton(5001, this.width / 2 + 5, 66, 20, 20, TextFormatting.GREEN + "+", "add", s -> NoCtrl.addAndSetCurrent(NoCtrl.ACTIVE.copy().rename(s)), I18n.format("noctrl.gui.add.desc1"), I18n.format("noctrl.gui.add.desc2")));
        this.removeFolder = this.addButton(new GuiTypeButton(5002, this.width / 2 + 30, 66, 20, 20, TextFormatting.RED + "-", "", s -> NoCtrl.ACTIVE.delete(), I18n.format("noctrl.gui.delete.desc")));
        this.renameFolder = this.addButton(new GuiTypeButton(5003, this.width / 2 + 55, 66, 20, 20, TextFormatting.YELLOW + I18n.format("noctrl.gui.rename"), "rename", s -> NoCtrl.addAndSetCurrent(NoCtrl.ACTIVE.rename(s)), I18n.format("noctrl.gui.rename.desc")));
        this.editIcon = this.addButton(new GuiTypeButton(5003, this.width / 2 + 80, 66, 20, 20, TextFormatting.BLUE + I18n.format("noctrl.gui.icon"), "icon", s -> NoCtrl.ACTIVE.setModel(ForgeRegistries.ITEMS.getValue(new ResourceLocation(s))), I18n.format("noctrl.gui.icon.desc")));

        this.yesAction = this.addButton(new GuiTooltipButton(5101, this.width / 2 + 5, 66, 20, 20, TextFormatting.GREEN + I18n.format("noctrl.gui.yes"), I18n.format("noctrl.gui.yes.desc")));
        this.noAction = this.addButton(new GuiTooltipButton(5102, this.width / 2 + 30, 66, 20, 20, TextFormatting.RED + I18n.format("noctrl.gui.no"), I18n.format("noctrl.gui.no.desc")));

        this.yesAction.visible = this.yesAction.enabled = false;
        this.noAction.visible = this.noAction.enabled = false;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        if(button.id > 5100) {
            if(button.id == 5101 && this.output != null && this.yesNoScreen && (!this.showInputField || !this.inputField.getText().trim().isEmpty())) {
                this.output.accept(this.inputField.getText().trim());
                this.inputField.setText("");
            }
            this.currentTask = "";
            this.output = null;
            this.showInputField = false;
            this.yesNoScreen = false;

        } else if(button.id > 5000 && button instanceof GuiTypeButton) {
            this.showInputField = button.id != 5002;
            this.yesNoScreen = true;
            this.output = ((GuiTypeButton)button).out;
            this.currentTask = ((GuiTypeButton)button).taskKey;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if(this.showInputField) {
            this.inputField.drawTextBox();
        } else {
            this.dropDown.render(mouseX, mouseY);
        }
        if(this.showInputField && mouseX >= this.inputField.x && mouseY >= this.inputField.y && mouseX < this.inputField.x + this.inputField.width && mouseY < this.inputField.y + this.inputField.height && !this.currentTask.isEmpty()) {
            this.drawHoveringText(I18n.format("noctrl.gui.task." + this.currentTask), mouseX, mouseY);

        } else {
            for (GuiButton guiButton : this.buttonList) {
                if(guiButton.enabled && guiButton instanceof GuiTooltipButton && guiButton.isMouseOver()) {
                    this.drawHoveringText(((GuiTooltipButton)guiButton).tooltips, mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public void updateScreen() {
        this.yesAction.visible = this.yesAction.enabled = this.yesNoScreen;
        this.noAction.visible = this.noAction.enabled = this.yesNoScreen;

        this.addFolder.visible = this.addFolder.enabled = !this.yesNoScreen;
        this.removeFolder.visible = this.removeFolder.enabled = !this.yesNoScreen;
        this.renameFolder.visible = this.renameFolder.enabled = !this.yesNoScreen;
        this.editIcon.visible = this.editIcon.enabled = !this.yesNoScreen;

        if(NoCtrl.ACTIVE == KeyBindSet.DEFAULT) {
            this.removeFolder.enabled = this.renameFolder.enabled = false;
        }
        super.updateScreen();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        boolean flag = this.buttonId != null;
        if(!flag) {
            LinkedList<GuiListExtended.IGuiListEntry> listEntries = ReflectionHelper.getPrivateValue(GuiNewKeyBindingList.class, (GuiNewKeyBindingList) this.keyBindingList, "listEntries");
            for (int i = 0; i < listEntries.size(); i++) {
                GuiListExtended.IGuiListEntry entry = this.keyBindingList.getListEntry(i);
                if(entry instanceof GuiNewKeyBindingList.KeyEntry && ((GuiButton)ReflectionHelper.getPrivateValue(GuiNewKeyBindingList.KeyEntry.class, (GuiNewKeyBindingList.KeyEntry) entry, "btnReset")).mousePressed(this.mc, mouseX, mouseY)) {
                    flag = true;
                    break;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if(this.showInputField) {
            this.inputField.mouseClicked(mouseX, mouseY, mouseButton);
        } else {
            this.dropDown.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if(flag) {
            this.saveKeyBinds();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        int x = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        GuiKeyBindingList old = this.keyBindingList;
        if(this.dropDown.isMouseOver(x, y)) {
            //Create a dummy list with the #isMouseYWithinSlotBounds set to false, meaning that the mouse inputs never get activated.
            this.keyBindingList = new GuiNewKeyBindingList(this, this.mc) {
                @Override
                public boolean isMouseYWithinSlotBounds(int p_148141_1_) {
                    return false;
                }
            };
            int mouseInput = Mouse.getEventDWheel();
            if(mouseInput != 0) {
                this.dropDown.scroll(mouseInput < 0 ? -1 : 1);
            }
        }
        super.handleMouseInput();
        this.keyBindingList = old;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if(keyCode == Keyboard.KEY_RETURN && this.output != null) {
            this.output.accept(this.inputField.getText().trim());
            this.inputField.setText("");
            this.showInputField = false;
            this.yesNoScreen = false;
        } else {
            boolean flag = this.buttonId != null;
            super.keyTyped(typedChar, keyCode);
            if(this.showInputField) {
                this.inputField.textboxKeyTyped(typedChar, keyCode);
            }
            if(flag) {
                this.saveKeyBinds();
            }
        }
    }

    private void saveKeyBinds() {
        for (KeyBinding keyBinding : mc.gameSettings.keyBindings) {
            NoCtrl.ACTIVE.putOverride(keyBinding, keyBinding.getKeyCode());
        }
    }

    private class GuiTooltipButton extends GuiButtonExt {
        private final List<String> tooltips;
        public GuiTooltipButton(int id, int xPos, int yPos, int width, int height, String displayString, String... tooltips) {
            super(id, xPos, yPos, width, height, displayString);
            this.tooltips = Lists.newArrayList(tooltips);
        }
    }

    private class GuiTypeButton extends GuiTooltipButton {
        private final String taskKey;
        private final Consumer<String> out;
        public GuiTypeButton(int id, int xPos, int yPos, int width, int height, String displayString, String taskKey, Consumer<String> out, String... tooltips) {
            super(id, xPos, yPos, width, height, displayString, tooltips);
            this.taskKey = taskKey;
            this.out = out;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void applyControlling(GuiOpenEvent event) {
        if(event.getGui() != null && event.getGui() instanceof GuiControls && NoCtrl.isControlling) {//isControlling should always be true, but check anyway
            event.setGui(new GuiControlsOverrideControlling(((GuiControls)event.getGui()).parentScreen, Minecraft.getMinecraft().gameSettings));
        }
    }

}
