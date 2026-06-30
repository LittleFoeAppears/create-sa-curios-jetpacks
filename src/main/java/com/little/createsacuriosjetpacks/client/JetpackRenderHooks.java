package com.little.createsacuriosjetpacks.client;

import com.little.createsacuriosjetpacks.CreateSaCuriosJetpacks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = CreateSaCuriosJetpacks.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class JetpackRenderHooks {
    private static final Map<RenderLayerParent<?, ?>, Map<String, RendererInvoker>> RENDERERS = new WeakHashMap<>();
    private static final Map<String, ArmorModelInvoker> ARMOR_MODELS = new HashMap<>();

    private JetpackRenderHooks() {
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (var skinModel : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skinModel);
            if (renderer != null) {
                renderer.addLayer(new CuriosJetpackLayer(renderer));
            }
        }
    }

    private static final class CuriosJetpackLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        private final RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent;

        private CuriosJetpackLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
            super(parent);
            this.parent = parent;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (player == null || player.isInvisible()) {
                return;
            }

            Optional<ItemStack> optionalJetpack = CreateSaCuriosJetpacks.getFirstRenderedCuriosJetpack(player);
            if (optionalJetpack.isPresent()) {
                renderCuriosJetpack(poseStack, bufferSource, packedLight, player,
                        limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch, optionalJetpack.get());
            }

            renderCuriosBeltTanks(poseStack, bufferSource, packedLight, player);
        }

        private void renderCuriosJetpack(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player,
                                         float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch,
                                         ItemStack curiosJetpack) {
            Optional<String> optionalRendererClass = CreateSaCuriosJetpacks.getCsaRendererClass(curiosJetpack);
            if (optionalRendererClass.isEmpty()) {
                return;
            }

            ArmorModelInvoker armorModel = getOrCreateArmorModel(curiosJetpack);
            if (armorModel != null) {
                armorModel.render(this.getParentModel(), poseStack, bufferSource, packedLight, player,
                        limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            }

            RendererInvoker invoker = getOrCreateRenderer(this.getParentModelParent(), optionalRendererClass.get());
            if (invoker == null) {
                return;
            }

            ItemStack realChestStack = player.getItemBySlot(EquipmentSlot.CHEST);
            try {
                // CSA's jetpack layer only renders when the matching jetpack is in the vanilla chest slot.
                // This layer is already running inside the player's normal render-layer pipeline, so the
                // PoseStack is correctly positioned, rotated, and animated with the body.
                player.setItemSlot(EquipmentSlot.CHEST, curiosJetpack);
                invoker.render(poseStack, bufferSource, packedLight, player, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
            } finally {
                player.setItemSlot(EquipmentSlot.CHEST, realChestStack);
            }
        }

        private void renderCuriosBeltTanks(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player) {
            List<ItemStack> tanks = CreateSaCuriosJetpacks.getRenderedCuriosTanks(player);
            if (tanks.isEmpty()) {
                return;
            }

            int count = Math.min(2, tanks.size());
            for (int i = 0; i < count; i++) {
                ItemStack tank = tanks.get(i);
                if (tank.isEmpty()) {
                    continue;
                }

                poseStack.pushPose();
                this.getParentModel().body.translateAndRotate(poseStack);

                // Approximate back-belt placement: one tank on each side of the lower back.
                double x = count == 1 ? 0.0D : (i == 0 ? -0.24D : 0.24D);
                poseStack.translate(x, 0.72D, 0.24D);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(count == 1 ? 0.0F : (i == 0 ? -10.0F : 10.0F)));
                poseStack.scale(0.38F, 0.38F, 0.38F);

                Minecraft.getInstance().getItemRenderer().renderStatic(
                        tank,
                        ItemDisplayContext.FIXED,
                        packedLight,
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        bufferSource,
                        player.level(),
                        i
                );
                poseStack.popPose();
            }
        }

        private RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> getParentModelParent() {
            return this.parent;
        }
    }


    private static ArmorModelInvoker getOrCreateArmorModel(ItemStack stack) {
        String itemId = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()));
        ArmorModelSpec spec = switch (itemId) {
            case "create_sa:brass_jetpack_chestplate" -> new ArmorModelSpec(
                    "net.mcreator.createstuffadditions.client.model.Modelbrass_jetpack",
                    ResourceLocation.fromNamespaceAndPath("create_sa", "textures/entities/brass_jetpack.png")
            );
            case "create_sa:andesite_jetpack_chestplate" -> new ArmorModelSpec(
                    "net.mcreator.createstuffadditions.client.model.Modelandesite_jetpack",
                    ResourceLocation.fromNamespaceAndPath("create_sa", "textures/entities/andesite_jetpack/andesite_jetpack_1.png")
            );
            case "create_sa:copper_jetpack_chestplate" -> new ArmorModelSpec(
                    "net.mcreator.createstuffadditions.client.model.Modelcopper_jetpack",
                    ResourceLocation.fromNamespaceAndPath("create_sa", "textures/entities/copper_jetpack.png")
            );
            case "create_sa:netherite_jetpack_chestplate" -> new ArmorModelSpec(
                    "net.mcreator.createstuffadditions.client.model.Modelnetherite_jetpack",
                    ResourceLocation.fromNamespaceAndPath("create_sa", "textures/entities/netherite_jetpack.png")
            );
            default -> null;
        };

        if (spec == null) {
            return null;
        }

        try {
            ArmorModelInvoker cached = ARMOR_MODELS.get(spec.modelClassName());
            if (cached != null) {
                return cached;
            }

            Class<?> modelClass = Class.forName(spec.modelClassName());
            Field layerField = modelClass.getField("LAYER_LOCATION");
            ModelLayerLocation layerLocation = (ModelLayerLocation) layerField.get(null);
            ModelPart bakedRoot = Minecraft.getInstance().getEntityModels().bakeLayer(layerLocation);
            Object model = modelClass.getConstructor(ModelPart.class).newInstance(bakedRoot);

            Method setupAnim = modelClass.getMethod("setupAnim", Entity.class, float.class, float.class, float.class, float.class, float.class);
            Method renderToBuffer = modelClass.getMethod("renderToBuffer", PoseStack.class, com.mojang.blaze3d.vertex.VertexConsumer.class,
                    int.class, int.class, int.class);

            ArmorModelInvoker created = new ArmorModelInvoker(
                    model,
                    setupAnim,
                    renderToBuffer,
                    modelClass.getField("body"),
                    modelClass.getField("rightarm"),
                    modelClass.getField("leftarm"),
                    spec.texture()
            );
            ARMOR_MODELS.put(spec.modelClassName(), created);
            return created;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static RendererInvoker getOrCreateRenderer(RenderLayerParent<?, ?> parent, String className) {
        try {
            Map<String, RendererInvoker> rendererMap = RENDERERS.computeIfAbsent(parent, ignored -> new HashMap<>());
            RendererInvoker cached = rendererMap.get(className);
            if (cached != null) {
                return cached;
            }

            Class<?> rendererClass = Class.forName(className);
            Constructor<?> constructor = rendererClass.getConstructor(RenderLayerParent.class);
            Object renderer = constructor.newInstance(parent);
            Method renderMethod = rendererClass.getMethod(
                    "render",
                    PoseStack.class,
                    MultiBufferSource.class,
                    int.class,
                    LivingEntity.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class
            );

            RendererInvoker created = new RendererInvoker(renderer, renderMethod);
            rendererMap.put(className, created);
            return created;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private record RendererInvoker(Object renderer, Method renderMethod) {
        private void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, LivingEntity entity,
                            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            try {
                renderMethod.invoke(renderer, poseStack, bufferSource, packedLight, entity,
                        limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Rendering is cosmetic only; never crash the game because a compatibility renderer failed.
            }
        }
    }

    private record ArmorModelSpec(String modelClassName, ResourceLocation texture) {
    }

    private record ArmorModelInvoker(Object model, Method setupAnim, Method renderToBuffer,
                                     Field bodyField, Field rightArmField, Field leftArmField,
                                     ResourceLocation texture) {
        private void render(PlayerModel<AbstractClientPlayer> parentModel, PoseStack poseStack, MultiBufferSource bufferSource,
                            int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                            float ageInTicks, float netHeadYaw, float headPitch) {
            try {
                setupAnim.invoke(model, player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

                copyPart(parentModel.body, (ModelPart) bodyField.get(model));
                copyPart(parentModel.rightArm, (ModelPart) rightArmField.get(model));
                copyPart(parentModel.leftArm, (ModelPart) leftArmField.get(model));

                var vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
                renderToBuffer.invoke(model, poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Cosmetic only. Do not crash the client if CSA changes its model classes.
            }
        }

        private static void copyPart(ModelPart source, ModelPart target) {
            target.visible = source.visible;
            target.x = source.x;
            target.y = source.y;
            target.z = source.z;
            target.xRot = source.xRot;
            target.yRot = source.yRot;
            target.zRot = source.zRot;
        }
    }

}
