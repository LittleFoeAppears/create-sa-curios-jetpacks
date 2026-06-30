package com.little.createsacuriosjetpacks.client;

import com.little.createsacuriosjetpacks.CreateSaCuriosJetpacks;
import com.little.createsacuriosjetpacks.CreateSaCuriosJetpacks.ArmorStatsStackingMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class ClientConfigHooks {
    private ClientConfigHooks() {
    }

    public static void registerConfigScreen(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> new CuriosJetpackConfigScreen(parent));
    }

    private static final class CuriosJetpackConfigScreen extends Screen {
        private static final int OPTION_BUTTON_WIDTH = 150;
        private static final int OPTION_BUTTON_HEIGHT = 20;
        private static final int BOTTOM_BUTTON_WIDTH = 130;
        private static final int BOTTOM_BUTTON_HEIGHT = 20;
        private static final int LABEL_BUTTON_GAP = 12;

        private final Screen parent;
        private final boolean initialArmorStatsEnabled;
        private final ArmorStatsStackingMode initialStackingMode;

        private Button armorStatsButton;
        private Button stackingButton;
        private Button undoButton;
        private Button resetButton;

        private CuriosJetpackConfigScreen(Screen parent) {
            super(Component.translatable("create_sa_curios_jetpacks.config.title"));
            this.parent = parent;
            this.initialArmorStatsEnabled = CreateSaCuriosJetpacks.CONFIG.enableCuriosJetpackArmorStats.get();
            this.initialStackingMode = CreateSaCuriosJetpacks.CONFIG.curiosJetpackArmorStatsStackingMode.get();
        }

        @Override
        protected void init() {
            int contentWidth = contentWidth();
            int contentLeft = (this.width - contentWidth) / 2;
            int buttonX = contentLeft + contentWidth - OPTION_BUTTON_WIDTH;
            int row1Y = firstRowY();
            int row2Y = row1Y + 24;

            this.armorStatsButton = this.addRenderableWidget(Button.builder(armorStatsButtonText(), button -> {
                        boolean newValue = !CreateSaCuriosJetpacks.CONFIG.enableCuriosJetpackArmorStats.get();
                        CreateSaCuriosJetpacks.CONFIG.enableCuriosJetpackArmorStats.set(newValue);
                        saveAndUpdateButtons();
                    })
                    .bounds(buttonX, row1Y, OPTION_BUTTON_WIDTH, OPTION_BUTTON_HEIGHT)
                    .build());

            this.stackingButton = this.addRenderableWidget(Button.builder(stackingButtonText(), button -> {
                        ArmorStatsStackingMode current = CreateSaCuriosJetpacks.CONFIG.curiosJetpackArmorStatsStackingMode.get();
                        ArmorStatsStackingMode next = current == ArmorStatsStackingMode.CHEST_EMPTY_ONLY
                                ? ArmorStatsStackingMode.STACK_WITH_CHEST
                                : ArmorStatsStackingMode.CHEST_EMPTY_ONLY;
                        CreateSaCuriosJetpacks.CONFIG.curiosJetpackArmorStatsStackingMode.set(next);
                        saveAndUpdateButtons();
                    })
                    .bounds(buttonX, row2Y, OPTION_BUTTON_WIDTH, OPTION_BUTTON_HEIGHT)
                    .build());

            int bottomY = this.height - 25;
            int centerX = this.width / 2;
            this.undoButton = this.addRenderableWidget(Button.builder(Component.translatable("create_sa_curios_jetpacks.config.button.undo"), button -> {
                        CreateSaCuriosJetpacks.CONFIG.enableCuriosJetpackArmorStats.set(this.initialArmorStatsEnabled);
                        CreateSaCuriosJetpacks.CONFIG.curiosJetpackArmorStatsStackingMode.set(this.initialStackingMode);
                        saveAndUpdateButtons();
                    })
                    .bounds(centerX - 202, bottomY, BOTTOM_BUTTON_WIDTH, BOTTOM_BUTTON_HEIGHT)
                    .build());

            this.resetButton = this.addRenderableWidget(Button.builder(Component.translatable("create_sa_curios_jetpacks.config.button.reset"), button -> {
                        CreateSaCuriosJetpacks.CONFIG.enableCuriosJetpackArmorStats.set(true);
                        CreateSaCuriosJetpacks.CONFIG.curiosJetpackArmorStatsStackingMode.set(ArmorStatsStackingMode.CHEST_EMPTY_ONLY);
                        saveAndUpdateButtons();
                    })
                    .bounds(centerX - 65, bottomY, BOTTOM_BUTTON_WIDTH, BOTTOM_BUTTON_HEIGHT)
                    .build());

            this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                    .bounds(centerX + 72, bottomY, BOTTOM_BUTTON_WIDTH, BOTTOM_BUTTON_HEIGHT)
                    .build());

            updateButtonState();
        }

        private static Component armorStatsButtonText() {
            return CreateSaCuriosJetpacks.CONFIG.enableCuriosJetpackArmorStats.get()
                    ? Component.translatable("create_sa_curios_jetpacks.config.button.enabled")
                    : Component.translatable("create_sa_curios_jetpacks.config.button.disabled");
        }

        private static Component stackingButtonText() {
            ArmorStatsStackingMode mode = CreateSaCuriosJetpacks.CONFIG.curiosJetpackArmorStatsStackingMode.get();
            return switch (mode) {
                case CHEST_EMPTY_ONLY -> Component.translatable("create_sa_curios_jetpacks.config.stacking.chest_empty_only.short");
                case STACK_WITH_CHEST -> Component.translatable("create_sa_curios_jetpacks.config.stacking.stack_with_chest.short");
            };
        }

        private void saveAndUpdateButtons() {
            CreateSaCuriosJetpacks.CONFIG_SPEC.save();
            updateButtonState();
        }

        private void updateButtonState() {
            if (this.armorStatsButton != null) {
                this.armorStatsButton.setMessage(armorStatsButtonText());
            }
            if (this.stackingButton != null) {
                this.stackingButton.setMessage(stackingButtonText());
                this.stackingButton.active = CreateSaCuriosJetpacks.CONFIG.enableCuriosJetpackArmorStats.get();
            }
            if (this.undoButton != null) {
                this.undoButton.active = isChangedFromInitial();
            }
            if (this.resetButton != null) {
                this.resetButton.active = !isDefaultState();
            }
        }

        private boolean isChangedFromInitial() {
            return CreateSaCuriosJetpacks.CONFIG.enableCuriosJetpackArmorStats.get() != this.initialArmorStatsEnabled
                    || CreateSaCuriosJetpacks.CONFIG.curiosJetpackArmorStatsStackingMode.get() != this.initialStackingMode;
        }

        private boolean isDefaultState() {
            return CreateSaCuriosJetpacks.CONFIG.enableCuriosJetpackArmorStats.get()
                    && CreateSaCuriosJetpacks.CONFIG.curiosJetpackArmorStatsStackingMode.get() == ArmorStatsStackingMode.CHEST_EMPTY_ONLY;
        }

        private int panelTop() {
            return 32;
        }

        private int panelBottom() {
            return this.height - 31;
        }

        private int firstRowY() {
            return panelTop() + 5;
        }

        private int contentWidth() {
            int widestLabel = Math.max(
                    this.font.width(Component.translatable("create_sa_curios_jetpacks.config.enable_armor_stats.label")),
                    this.font.width(Component.translatable("create_sa_curios_jetpacks.config.armor_stats_stacking.label"))
            );
            return Math.min(widestLabel + LABEL_BUTTON_GAP + OPTION_BUTTON_WIDTH, this.width - 120);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // Use Minecraft's normal background handling first. This keeps the menu/world
            // background visible and blurred like other mod config screens instead of
            // falling back to a flat black background when opened outside a world.
            super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
            renderPanel(guiGraphics);
            renderRows(guiGraphics);
            for (Renderable renderable : this.renderables) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
            renderHoveredTooltips(guiGraphics, mouseX, mouseY);
        }

        private void renderPanel(GuiGraphics guiGraphics) {
            int panelTop = panelTop();
            int panelBottom = panelBottom();

            // Title above the panel, matching the cleaner full-width config style.
            guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 13, 0xFFFFFF);

            // Very subtle full-screen dim, then a more transparent full-width panel.
            guiGraphics.fill(0, 0, this.width, this.height, 0x22000000);
            guiGraphics.fill(0, panelTop, this.width, panelBottom, 0x38000000);

            // Thin Easy Anvils-like border: subtle white outer line, darker black line inside.
            guiGraphics.fill(0, panelTop, this.width, panelTop + 1, 0x44FFFFFF);
            guiGraphics.fill(0, panelTop + 1, this.width, panelTop + 2, 0xCC000000);
            guiGraphics.fill(0, panelBottom - 2, this.width, panelBottom - 1, 0xCC000000);
            guiGraphics.fill(0, panelBottom - 1, this.width, panelBottom, 0x44FFFFFF);
        }

        private void renderRows(GuiGraphics guiGraphics) {
            int contentWidth = contentWidth();
            int contentLeft = (this.width - contentWidth) / 2;
            int row1Y = firstRowY();
            int row2Y = row1Y + 24;

            guiGraphics.drawString(this.font,
                    Component.translatable("create_sa_curios_jetpacks.config.enable_armor_stats.label"),
                    contentLeft, row1Y + 6, 0xFFFFFF, false);

            guiGraphics.drawString(this.font,
                    Component.translatable("create_sa_curios_jetpacks.config.armor_stats_stacking.label"),
                    contentLeft, row2Y + 6, 0xFFFFFF, false);
        }

        private void renderHoveredTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            int contentWidth = contentWidth();
            int contentLeft = (this.width - contentWidth) / 2;
            int labelWidth = contentWidth - OPTION_BUTTON_WIDTH - LABEL_BUTTON_GAP;
            int row1Y = firstRowY();
            int row2Y = row1Y + 24;

            if (isMouseIn(mouseX, mouseY, contentLeft, row1Y, labelWidth, OPTION_BUTTON_HEIGHT)) {
                guiGraphics.renderTooltip(this.font,
                        this.font.split(Component.translatable("create_sa_curios_jetpacks.config.enable_armor_stats.description"), 260),
                        mouseX, mouseY);
            } else if (isMouseIn(mouseX, mouseY, contentLeft, row2Y, labelWidth, OPTION_BUTTON_HEIGHT)) {
                guiGraphics.renderTooltip(this.font,
                        this.font.split(Component.translatable("create_sa_curios_jetpacks.config.armor_stats_stacking.description"), 260),
                        mouseX, mouseY);
            }
        }

        private static boolean isMouseIn(int mouseX, int mouseY, int x, int y, int width, int height) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }

        @Override
        public void onClose() {
            this.minecraft.setScreen(this.parent);
        }
    }
}
