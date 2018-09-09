package com.wynprice.noctrl;

import com.google.common.collect.Lists;
import com.google.gson.*;
import com.wynprice.noctrl.compact.GuiControlsOverrideControlling;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.*;
import java.util.Comparator;
import java.util.List;

@Mod(modid = NoCtrl.MODID, name = NoCtrl.NAME, version = NoCtrl.VERSION, clientSideOnly = true)
@Mod.EventBusSubscriber
public class NoCtrl {
    public static final String MODID = "noctrl";
    public static final String NAME = "NoCtrl";
    public static final String VERSION = "1.2";
    private static Logger logger = LogManager.getLogger(MODID);

    public static final File baseLoc = new File(Minecraft.getMinecraft().mcDataDir, "noctrl");
    private static final File settingsLoc = new File(baseLoc, "noctrl.json");
    public static KeyBindSet ACTIVE = KeyBindSet.DEFAULT;
    public static final List<KeyBindSet> ALL_LISTS = Lists.newArrayList();

    public static KeyBinding SCREEN_BINDING = new KeyBinding("key.noctrl.screen", Keyboard.KEY_LMENU, NAME);

    public static final boolean isControlling = Loader.isModLoaded("controlling");

    private static final int BOX_MIN = 45;
    private static final int BOX_MAX = 60;

    private static final int PAGE_MAX = 7;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        //Ensure that the directory is created and the default keybinds exist
        if(!KeyBindSet.DEFAULT.getLocation().exists()) {
            for (KeyBinding keyBinding : Minecraft.getMinecraft().gameSettings.keyBindings) {
                KeyBindSet.DEFAULT.putOverride(keyBinding, keyBinding.getKeyCode());
            }
            KeyBindSet.DEFAULT.writeToFile();
        }
        //
        File[] files = baseLoc.listFiles((dir, name) -> name.endsWith(".txt"));
        if(files != null) {
            for (File file : files) {
                String name = FilenameUtils.getBaseName(file.getName());
                if(!name.equals("default")) {
                    ALL_LISTS.add(new KeyBindSet(name));
                }
            }
            ALL_LISTS.add(KeyBindSet.DEFAULT);
            ALL_LISTS.sort(Comparator.comparing(KeyBindSet::getName));
        } else {
            logger.error("Unable to load directory, " + baseLoc.getAbsolutePath() + " and therefore unable to load keybindings");
        }
        //Load and apply the settings
        try {
            JsonObject json = JsonUtils.getJsonObject(new JsonParser().parse(new FileReader(settingsLoc)), "root");
            for (KeyBindSet list : ALL_LISTS) {
                if(list.getName().equals(json.get("active").getAsString())) {
                    ACTIVE = list;
                    break;
                }
            }
        } catch (JsonParseException e) {
            logger.error("Unable to parse settings file", e);
        } catch (FileNotFoundException ignored) {

        }

        ClientRegistry.registerKeyBinding(SCREEN_BINDING);

        if(isControlling) {
            registerControllingSupport();
        }
    }

    @Optional.Method(modid = "controlling")
    public static void registerControllingSupport() {
        MinecraftForge.EVENT_BUS.register(GuiControlsOverrideControlling.class);
    }

    public static Logger getLogger() {
        return logger;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGuiOpen(GuiOpenEvent event) {
        if(event.getGui() != null && event.getGui() instanceof GuiControls && !isControlling) {
            event.setGui(new GuiControlsOverride(((GuiControls) event.getGui()).parentScreen, Minecraft.getMinecraft().gameSettings));
        }
    }

    private static boolean previousPressed;
    private static boolean needsRelease;
    private static boolean prevMouseReslease;

    private static int page;

    private static int currentItem;

    private static int preMouseX;
    private static int preMouseY;

    @SubscribeEvent
    public static void onRenderTick(RenderGameOverlayEvent.Post event) {
        if(event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();

        boolean keyDown = SCREEN_BINDING.isKeyDown();
        boolean current =  keyDown && mc.currentScreen == null;
        if(needsRelease && keyDown) {
            return;
        }
        needsRelease = false;
        if(current != previousPressed) {
            if(current) {
                mc.setIngameNotInFocus();
                currentItem = mc.player.inventory.currentItem;
            } else {
                mc.setIngameFocus();
            }
        }


        //Due to race conditions, somtimes the in game gui can be forced back into focus, making some pretty odd results
        if(current && previousPressed && mc.inGameHasFocus) {
            mc.setIngameNotInFocus();
            Mouse.setCursorPosition(preMouseX, preMouseY);
        }

        int remeinder = ALL_LISTS.size() % PAGE_MAX;
        int max = ALL_LISTS.size() / PAGE_MAX;

        if(current) {
            mc.player.inventory.currentItem = currentItem;
            int mouseD = Mouse.getDWheel();
            if(mouseD != 0) {
                int amount = mouseD < 0 ? 1 : -1;
                page = MathHelper.clamp(page + amount, 0, max);
            }

            preMouseX = Mouse.getEventX();
            preMouseY = Mouse.getEventY();

            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003921569F);

            KeyBindSet mouse = getUnderMouse();
            ScaledResolution res = new ScaledResolution(mc);

            int lisSize = page == max ? remeinder : PAGE_MAX;
            double theta = ((Math.PI*2D) / lisSize);
            for (int i = 0; i < lisSize; i++) {
                double angle = theta * i;
                double xPos = Math.sin(angle) * 75 + res.getScaledWidth_double() / 2D;
                double yPos = -Math.cos(angle) * 75 + res.getScaledHeight_double() / 2D;

                KeyBindSet list = ALL_LISTS.get(i + page * PAGE_MAX);

                mc.fontRenderer.drawStringWithShadow(list.getName(), (int) (xPos - mc.fontRenderer.getStringWidth(list.getName()) / 2D), (int) yPos, -1);
                GlStateManager.pushMatrix();
                GlStateManager.translate(xPos, yPos, 0);
                GlStateManager.scale(2,2,2);
                GlStateManager.translate(-8, -16, 0);
                mc.getRenderItem().renderItemIntoGUI(new ItemStack(list.getModel()), 0, 0);
                GlStateManager.popMatrix();
                if(list == mouse || ACTIVE == list) {
                    double width = mc.fontRenderer.getStringWidth(list.getName()) + 4;
                    double size = MathHelper.clamp(width, BOX_MIN, BOX_MAX);
                    if(list == ACTIVE) {
                        drawBox((int)(xPos - size / 2D), (int)(yPos - (size - 9)), (int)(xPos + size / 2D), (int)(yPos + 9), 1, 0x20219100, -1);
                    }
                    if(list == mouse) {
                        drawBox((int)(xPos - size / 2D), (int)(yPos - (size - 9)), (int)(xPos + size / 2D), (int)(yPos + 9), 1, 0x1199bbff, -1);
                    }
                }
            }
            if(ALL_LISTS.size() > PAGE_MAX) {
                int activeColor = -1;
                int unactiveColor = 0xFF5B5B5B;

                drawBox(res.getScaledWidth() / 2 - 75, res.getScaledHeight() - 50, res.getScaledWidth() / 2 - 50, res.getScaledHeight() - 25, 1, page != 0 ? activeColor : unactiveColor,0xFF000000);
                drawBox(res.getScaledWidth() / 2 + 50, res.getScaledHeight() - 50, res.getScaledWidth() / 2 + 75, res.getScaledHeight() - 25, 1, page != max ? activeColor : unactiveColor,0xFF000000);
                mc.fontRenderer.drawString("<-",res.getScaledWidth() / 2 - 62 - mc.fontRenderer.getStringWidth("<-") / 2, res.getScaledHeight() - 41, -0xFF000000);
                mc.fontRenderer.drawString("->",res.getScaledWidth() / 2 + 62 - mc.fontRenderer.getStringWidth("->") / 2, res.getScaledHeight() - 41, -0xFF000000);

                double mouseX = (double)Mouse.getEventX() / mc.displayWidth * res.getScaledWidth_double();
                double mouseY = (double)(mc.displayHeight - Mouse.getEventY()) / mc.displayHeight * res.getScaledHeight_double();

                if(mouseY > res.getScaledHeight() - 50 && mouseY < res.getScaledHeight() - 25) {
                    if(mouseX > res.getScaledWidth() / 2 - 75 && mouseX < res.getScaledWidth() / 2 - 50 && page != 0) {
                        Gui.drawRect(res.getScaledWidth() / 2 - 75, res.getScaledHeight() - 50, res.getScaledWidth() / 2 - 50, res.getScaledHeight() - 25, 0x5199bbff);
                    } else if(mouseX > res.getScaledWidth() / 2 + 50 && mouseX < res.getScaledWidth() / 2 + 75 && page != max) {
                        Gui.drawRect(res.getScaledWidth() / 2 + 50, res.getScaledHeight() - 50, res.getScaledWidth() / 2 + 75, res.getScaledHeight() - 25, 0x5199bbff);
                    }
                }
            }
            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            GlStateManager.disableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.color(1f, 1f, 1f, 1f);
            RenderHelper.disableStandardItemLighting();
        }
        previousPressed = current;
    }

    private static void drawBox(int left, int top, int right, int bottom, int borderSize, int insideColor, int borderColor) {
        Gui.drawRect(left, top, right, bottom, insideColor);
        GlStateManager.color(1f, 1f, 1f, 1f);

        Gui.drawRect(left, top, right, top + borderSize, borderColor);
        Gui.drawRect(left, bottom, right, bottom - borderSize, borderColor);
        Gui.drawRect(left, top, left + borderSize, bottom, borderColor);
        Gui.drawRect(right, top, right - borderSize, bottom, borderColor);
        GlStateManager.color(1f, 1f, 1f, 1f);

    }

    @SubscribeEvent
    public static void onMouseIn(InputEvent.MouseInputEvent event) {
        boolean isMouseDown = Mouse.isButtonDown(0);
        if(previousPressed && prevMouseReslease != isMouseDown) {
            if(isMouseDown) {
                previousPressed = false;
                needsRelease = true;
                KeyBindSet list = getUnderMouse();
                if(list != null) {
                    list.setAsCurrent();
                } else {
                    Minecraft mc = Minecraft.getMinecraft();
                    ScaledResolution res = new ScaledResolution(mc);
                    int max = ALL_LISTS.size() / PAGE_MAX;

                    double mouseX = (double)Mouse.getEventX() / mc.displayWidth * res.getScaledWidth_double();
                    double mouseY = (double)(mc.displayHeight - Mouse.getEventY()) / mc.displayHeight * res.getScaledHeight_double();

                    if(mouseY > res.getScaledHeight() - 50 && mouseY < res.getScaledHeight() - 25) {
                        if(mouseX > res.getScaledWidth() / 2 - 75 && mouseX < res.getScaledWidth() / 2 - 50 && page != 0) {
                            page--;
                        } else if(mouseX > res.getScaledWidth() / 2 + 50 && mouseX < res.getScaledWidth() / 2 + 75 && page != max) {
                            page++;
                        }
                    }
                    page = MathHelper.clamp(page, 0, max);

                    mc.setIngameNotInFocus();
                    needsRelease = false;
                    previousPressed = true;
                    Mouse.setCursorPosition(preMouseX, preMouseY);

                }
            }
        }
        prevMouseReslease = isMouseDown;
    }

    public static void addAndSetCurrent(KeyBindSet list) {
        ALL_LISTS.add(list);
        ALL_LISTS.sort(Comparator.comparing(KeyBindSet::getName));
        list.setAsCurrent();
    }

    private static KeyBindSet getUnderMouse() {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(mc);
        int remeinder = ALL_LISTS.size() % PAGE_MAX;
        int max = ALL_LISTS.size() / PAGE_MAX;

        double mouseX = (double)Mouse.getEventX() / mc.displayWidth * resolution.getScaledWidth_double();
        double mouseY = (double)(mc.displayHeight - Mouse.getEventY()) / mc.displayHeight * resolution.getScaledHeight_double();
        int lisSize = page == max ? remeinder : PAGE_MAX;
        double theta = ((Math.PI*2D) / lisSize);
        for (int i = 0; i < lisSize; i++) {
            double angle = theta * i;
            double xPos = Math.sin(angle) * 75 + resolution.getScaledWidth_double() / 2D;
            double yPos = -Math.cos(angle) * 75 + resolution.getScaledHeight_double() / 2D;

            KeyBindSet list = ALL_LISTS.get(i + page * PAGE_MAX);

            double width = mc.fontRenderer.getStringWidth(list.getName()) + 4;
            double size = MathHelper.clamp(width, BOX_MIN, BOX_MAX);
            if(Math.abs(mouseX - xPos) <= size / 2D && mouseY <= yPos + 9 && mouseY >= yPos - (size - 9)) {
                return list;
            }
        }
        return null;
    }

    public static void saveSettings() {
        JsonObject json = new JsonObject();
        json.addProperty("active", ACTIVE.getName());

        try (Writer writer = new FileWriter(settingsLoc)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(json, writer);
        } catch (IOException e) {
            logger.error("Unable to save settings", e);
        }
    }
}
