package com.terraformersmc.mod_menu.gui.widget.entries;

import com.mojang.blaze3d.systems.RenderSystem;
import com.terraformersmc.mod_menu.ModMenu;
import com.terraformersmc.mod_menu.gui.widget.ModListWidget;
import com.terraformersmc.mod_menu.util.DrawingUtil;
import com.terraformersmc.mod_menu.util.mod.Mod;
import com.terraformersmc.mod_menu.util.mod.ModBadgeRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;

import java.awt.*;

public class ModListEntry extends ObjectSelectionList.Entry<ModListEntry> {
    public static final ResourceLocation UNKNOWN_ICON = new ResourceLocation("textures/misc/unknown_pack.png");
    private static final ResourceLocation MOD_CONFIGURATION_ICON = new ResourceLocation(ModMenu.MOD_ID, "textures/gui/mod_configuration.png");
    private static final ResourceLocation ERROR_ICON = new ResourceLocation("minecraft", "textures/gui/world_selection.png");

    protected final Minecraft client;
    public final Mod mod;
    protected final ModListWidget list;
    protected Tuple<ResourceLocation, Dimension> iconLocation;
    protected Tuple<ResourceLocation, Dimension> smallIconLocation;
    protected static final int FULL_ICON_SIZE = 32;
    protected static final int COMPACT_ICON_SIZE = 19;
    protected long sinceLastClick;

    public ModListEntry(Mod mod, ModListWidget list) {
        this.mod = mod;
        this.list = list;
        this.client = Minecraft.getInstance();
    }

    @Override
    public Component getNarration() {
        return Component.literal(mod.getTranslatedName());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float delta) {
        x += getXOffset();
        rowWidth -= getXOffset();
        
        // --- FIX: Reset Color State (Crucial for preventing text blackouts) ---
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        String modId = mod.getId();
        
        // --- Icon Rendering (Commented Out) ---
        /*
        int iconSize = ModMenu.getConfig().COMPACT_LIST.get() ? COMPACT_ICON_SIZE : FULL_ICON_SIZE;
        if ("java".equals(modId)) {
            DrawingUtil.drawRandomVersionBackground(mod, guiGraphics, x, y, iconSize, iconSize);
        }
        RenderSystem.enableBlend();
        // ... (Icon drawing logic) ...
        RenderSystem.disableBlend();
        */

        Component name = Component.literal(mod.getTranslatedName());
        FormattedText trimmedName = name;
        
        // Text width calculation (No icon offset)
        int maxNameWidth = rowWidth - 6; 
        
        Font font = this.client.font;
        if (font.width(name) > maxNameWidth) {
            FormattedText ellipsis = FormattedText.of("...");
            trimmedName = FormattedText.composite(font.substrByWidth(name, maxNameWidth - font.width(ellipsis)), ellipsis);
        }
        
        // Draw Mod Name
        guiGraphics.drawString(font, Language.getInstance().getVisualOrder(trimmedName), x + 6, y + 1, 0xFFFFFF, false);
        
        var updateBadgeXOffset = 0;
        if (!ModMenu.getConfig().HIDE_BADGES.get()) {
            new ModBadgeRenderer(x + 6 + font.width(name) + 2 + updateBadgeXOffset, y, x + rowWidth, mod, list.getParent()).draw(guiGraphics);
        }
        
        // Draw Description / Version
        if (!ModMenu.getConfig().COMPACT_LIST.get()) {
            String summary = mod.getSummary();
            DrawingUtil.drawWrappedString(guiGraphics, summary, (x + 6), (y + client.font.lineHeight + 2), rowWidth - 6, 2, 0x808080);
        } else {
            DrawingUtil.drawWrappedString(guiGraphics, mod.getPrefixedVersion(), (x + 6), (y + client.font.lineHeight + 2), rowWidth - 6, 2, 0x808080);
        }

        // --- Gear Overlay (Commented Out) ---
        /*
        if (!(this instanceof ParentEntry) && !(this instanceof ChildParentEntry) ... ) {
             // ...
        }
        */
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int delta) {
        list.select(this);
        if (ModMenu.getConfig().QUICK_CONFIGURE.get() &&
                this.list.getParent().getModHasConfigScreen(this.mod.getContainer())) {
            // Keep only double-click shortcut
            if (Util.getMillis() - this.sinceLastClick < 250) {
                this.openConfig();
            }
        }
        this.sinceLastClick = Util.getMillis();
        return true;
    }

    public void openConfig() {
        mod.getContainer().ifPresent(modContainer ->
                this.list.getParent().safelyOpenConfigScreen(modContainer));
    }

    public Mod getMod() {
        return mod;
    }

    public Tuple<ResourceLocation, Dimension> getIconTexture() {
        if (ModMenu.shouldResetCache) {
            this.smallIconLocation = null;
            this.iconLocation = null;
            ModMenu.shouldResetCache = false;
        }

        if (this.iconLocation == null) {
            this.iconLocation = new Tuple<>(new ResourceLocation(ModMenu.MOD_ID, mod.getId() + "_icon"), new Dimension());
            Tuple<DynamicTexture, Dimension> icon = mod.getIcon(list.getNeoforgeIconHandler(),
                    64 * this.client.options.guiScale().get(), false);


            if (icon != null) {
                float multiplier = 32f / icon.getB().height;
                this.iconLocation.setB(new Dimension(
                        (int) (icon.getB().width * multiplier),
                        (int) (icon.getB().height * multiplier)));

                this.client.getTextureManager().register(this.iconLocation.getA(), icon.getA());
            } else {
                this.iconLocation.setA(UNKNOWN_ICON);
            }
        }
        return iconLocation;
    }

    public Tuple<ResourceLocation, Dimension> getSquaredIconTexture() {
        Tuple<ResourceLocation, Dimension> icon = new Tuple<>(getIconTexture().getA(), iconLocation.getB().getSize()) ;
        float iconSize = ModMenu.getConfig().COMPACT_LIST.get() ? ModListEntry.COMPACT_ICON_SIZE : ModListEntry.FULL_ICON_SIZE;
        float biggerValue = Math.max(icon.getB().width, icon.getB().height);
        icon.getB().setSize(icon.getB().width / biggerValue * iconSize, icon.getB().height / biggerValue * iconSize);
        return icon;
    }


    public Tuple<ResourceLocation, Dimension> getSquareIconTexture() {
        if (this.smallIconLocation == null) {
            this.smallIconLocation = new Tuple<>(new ResourceLocation(ModMenu.MOD_ID, mod.getId() + "_icon_small"), new Dimension());
            Tuple<DynamicTexture, Dimension> icon = mod.getIcon(list.getNeoforgeIconHandler(),
                    64 * this.client.options.guiScale().get(), true);
            if (icon != null) {
                this.smallIconLocation.setB(new Dimension());
                this.client.getTextureManager().register(this.smallIconLocation.getA(), icon.getA());
            } else {
                this.smallIconLocation = this.getSquaredIconTexture();
            }
        }
        return smallIconLocation;
    }

    public int getXOffset() {
        return 0;
    }

    @Override
    public String toString() {
        return "ModListEntry{mod_id=\"" + getMod().getId() + "\"}";
    }
}