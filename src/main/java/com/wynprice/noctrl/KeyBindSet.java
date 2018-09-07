package com.wynprice.noctrl;

import com.google.common.collect.Maps;
import com.google.common.io.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class KeyBindSet {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static final KeyBindSet DEFAULT = new KeyBindSet("default");

    private final String name;
    private final File location;
    private final Map<KeyBinding, Integer> keyBindMap = Maps.newHashMap();

    private Item model = Items.AIR;

    public KeyBindSet(String name) {
        this.name = name;
        this.location = new File(NoCtrl.baseLoc,name + ".txt");

        try {
            List<String> lines = IOUtils.readLines(new FileInputStream(this.location), StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if(i == 0) {

                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(line)); //Dont call #setModel as it causes this to write to file, when the file hasn't serialized
                    this.model = item == null || item == Items.AIR ? Item.getItemFromBlock(Blocks.STONE) : item;
                    continue;
                }
                for (KeyBinding keyBinding : mc.gameSettings.keyBindings) {
                    int index = line.indexOf(':');
                    if(index == -1) {
                        continue;
                    }
                    String key = line.substring(0, index);
                    String value = line.substring(index + 1);
                    if (key.equals(keyBinding.getKeyDescription())) {
                        this.keyBindMap.put(keyBinding, Integer.parseInt(value));
                        break;
                    }
                }
            }
        } catch (IOException ignored) { //Ignore this, as this will occur when the list is originally created, or hasnt been saved
        }
    }

    private void ensureValidity() {
        for (KeyBinding keybinding : mc.gameSettings.keyBindings) {
            if(!this.keyBindMap.containsKey(keybinding)) {
                NoCtrl.getLogger().error("Found missing keybinding " + keybinding.getKeyDescription() + " in set " + this.name + ". Injecting default codes in");
                this.keyBindMap.put(keybinding, keybinding.getKeyCode());
            }
        }
    }

    public void writeToFile() {
        this.ensureValidity();
        PrintWriter printwriter = null;
        try {
            File parentFile = this.location.getParentFile();
            if(!parentFile.exists() && !parentFile.mkdirs()) {
                NoCtrl.getLogger().error("Unable to create directory");
            } else {
                printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(this.location), StandardCharsets.UTF_8));
                printwriter.println(this.model.getRegistryName());
                for (KeyBinding keybinding : this.keyBindMap.keySet()) {
                    printwriter.println(keybinding.getKeyDescription() + ":" + this.keyBindMap.get(keybinding));
                }
            }
        } catch (Exception e) {
            NoCtrl.getLogger().error("Failed to save keybinds", e);
        } finally {
            IOUtils.closeQuietly(printwriter);
        }
    }

    public void setAsCurrent() {
        this.ensureValidity();
        for (KeyBinding keyBinding : mc.gameSettings.keyBindings) {
            keyBinding.setKeyCode(this.keyBindMap.get(keyBinding));
        }
        KeyBinding.resetKeyBindingArrayAndHash(); //Needed ?

        NoCtrl.ACTIVE = this;
        NoCtrl.saveSettings();
        mc.player.sendStatusMessage(new TextComponentTranslation("noctrl.current_changed", this.name), true);

        this.writeToFile(); //Ensure current is written
    }

    public KeyBindSet copy() {
        String newName = this.name + "_copy"; //Have some type of numbered system ?
        KeyBindSet list = new KeyBindSet(newName);
        list.keyBindMap.putAll(this.keyBindMap);
        list.setModel(this.model);
        try {
            Files.copy(this.location, list.location);
        } catch (IOException e) {
            NoCtrl.getLogger().error("Unable to clone list", e);
        }
        return list;
    }

    public KeyBindSet rename(String newName) {
        KeyBindSet list = new KeyBindSet(newName);
        list.keyBindMap.putAll(this.keyBindMap);
        list.setModel(this.model);
        try {
            Files.move(this.location, list.location);
            NoCtrl.ALL_LISTS.remove(this);
        } catch (IOException e) {
            NoCtrl.getLogger().error("Unable to rename list", e);
        }
        return list;
    }

    public void delete() {
        if(this.location.delete()) {
            NoCtrl.ALL_LISTS.remove(this);
            DEFAULT.setAsCurrent();
        } else {
            NoCtrl.getLogger().error("Unable to delete list");
        }
    }

    public void putOverride(KeyBinding keyBinding, int override) {
        this.keyBindMap.put(keyBinding, override);
    }

    public File getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public void setModel(Item model) {
        this.model = model == null || model == Items.AIR ? Item.getItemFromBlock(Blocks.STONE) : model;
        this.writeToFile();
    }

    public Item getModel() {
        return this.model == null || this.model == Items.AIR ? Item.getItemFromBlock(Blocks.STONE) : this.model;
    }
}
