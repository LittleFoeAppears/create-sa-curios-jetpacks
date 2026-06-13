package com.little.createsacuriosjetpacks;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

@Mod(CreateSaCuriosJetpacks.MOD_ID)
public final class CreateSaCuriosJetpacks {
    public static final String MOD_ID = "create_sa_curios_jetpacks";
    private static final String CURIOS_API_CLASS = "top.theillusivec4.curios.api.CuriosApi";

    private static final ResourceLocation FUELABLE_TAG_ID = ResourceLocation.parse("create_sa:fuelable");
    private static final ResourceLocation FILLABLE_TAG_ID = ResourceLocation.parse("create_sa:fillable");

    private static final Map<ResourceLocation, JetpackProcedure> JETPACKS = new HashMap<>();

    static {
        JETPACKS.put(ResourceLocation.parse("create_sa:brass_jetpack_chestplate"),
                new JetpackProcedure("net.mcreator.createstuffadditions.procedures.BrassEncasedPropelerBodyTickEventProcedure"));
        JETPACKS.put(ResourceLocation.parse("create_sa:andesite_jetpack_chestplate"),
                new JetpackProcedure("net.mcreator.createstuffadditions.procedures.AndesitePropelerBodyTickEventProcedure"));
        JETPACKS.put(ResourceLocation.parse("create_sa:copper_jetpack_chestplate"),
                new JetpackProcedure("net.mcreator.createstuffadditions.procedures.CopperPropelerBodyTickEventProcedure"));
        JETPACKS.put(ResourceLocation.parse("create_sa:netherite_jetpack_chestplate"),
                new JetpackProcedure("net.mcreator.createstuffadditions.procedures.NetheriteJetpackChestplateTickEventProcedure"));
    }

    public CreateSaCuriosJetpacks(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(CreateSaCuriosJetpacks::registerPayloads);
        NeoForge.EVENT_BUS.addListener(CreateSaCuriosJetpacks::onPlayerTickPost);
        NeoForge.EVENT_BUS.addListener(CreateSaCuriosJetpacks::onRightClickBlock);
    }


    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        InteractionHand hand = event.getHand();
        ItemStack held = player.getItemInHand(hand);

        if (level == null || held.isEmpty() || !isRefillTank(held)) {
            return;
        }

        ResourceLocation tankId = BuiltInRegistries.ITEM.getKey(held.getItem());
        Fluid wantedFluid;
        if (isFillingTank(tankId)) {
            wantedFluid = Fluids.WATER;
        } else if (isFuelingTank(tankId)) {
            wantedFluid = Fluids.LAVA;
        } else {
            // Creative Filling Tank already refills held/armor items by CSA itself.
            // Do not let it drain basins as a storage tank.
            return;
        }

        BlockPos pos = event.getPos();
        if (!isCreateBasin(level, pos)) {
            return;
        }

        if (tryFillCsaTankFromBasin(level, pos, held, tankId, wantedFluid)) {
            player.swing(hand, true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private static boolean isCreateBasin(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return false;
        }

        // Avoid a hard compile-time dependency on Create.
        return blockEntity.getClass().getName().equals("com.simibubi.create.content.processing.basin.BasinBlockEntity")
                || BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock()).equals(ResourceLocation.parse("create:basin"));
    }

    private static boolean tryFillCsaTankFromBasin(Level level, BlockPos pos, ItemStack tank, ResourceLocation tankId, Fluid fluid) {
        double capacity = getTankCapacity(tankId);
        double current = getCustomDouble(tank, "tagStock");
        if (current >= capacity) {
            return false;
        }

        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        if (handler == null) {
            return false;
        }

        // CSA source-block refilling consumes one source and adds +100 stock.
        // A fluid source is 1000 mB, so drain 1000 mB from the basin.
        FluidStack requested = new FluidStack(fluid, 1000);
        FluidStack simulated = handler.drain(requested, FluidAction.SIMULATE);
        if (simulated.isEmpty() || simulated.getAmount() < 1000 || !simulated.getFluid().isSame(fluid)) {
            return false;
        }

        if (!level.isClientSide) {
            FluidStack drained = handler.drain(requested, FluidAction.EXECUTE);
            if (drained.isEmpty() || drained.getAmount() < 1000) {
                return false;
            }
            setCustomDouble(tank, "tagStock", Math.min(capacity, current + 100.0D));
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                blockEntity.setChanged();
            }
        }

        return true;
    }

    private static double getTankCapacity(ResourceLocation id) {
        if (id.equals(ResourceLocation.parse("create_sa:large_filling_tank"))
                || id.equals(ResourceLocation.parse("create_sa:large_fueling_tank"))) {
            return getCsaConfigDouble("LARGETANKCAPACITY", 4000.0D);
        }
        if (id.equals(ResourceLocation.parse("create_sa:medium_filling_tank"))
                || id.equals(ResourceLocation.parse("create_sa:medium_fueling_tank"))) {
            return getCsaConfigDouble("MEDIUMTANKCAPACITY", 2000.0D);
        }
        if (id.equals(ResourceLocation.parse("create_sa:small_filling_tank"))
                || id.equals(ResourceLocation.parse("create_sa:small_fueling_tank"))) {
            return getCsaConfigDouble("SMALLTANKCAPACITY", 1000.0D);
        }
        return 0.0D;
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(FlyingStatePayload.TYPE, FlyingStatePayload.STREAM_CODEC, CreateSaCuriosJetpacks::handleFlyingStatePayload);
    }

    private static void handleFlyingStatePayload(FlyingStatePayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player == null) {
            return;
        }

        // Do not interfere with Create Stuff & Additions' normal chestplate-slot handling.
        // If a CSA jetpack is worn as armor, CSA owns the CsaFlying flag.
        if (hasVanillaChestJetpack(player)) {
            return;
        }

        if (hasCuriosJetpack(player)) {
            player.getPersistentData().putBoolean("CsaFlying", payload.flying());
        } else if (!payload.flying()) {
            // Only clear our own stale state when no Curios jetpack is present.
            player.getPersistentData().putBoolean("CsaFlying", false);
        }
    }

    private static void onPlayerTickPost(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();

        // If the jetpack is worn normally, CSA already handles inventory tanks.
        // We only add support for tanks that are stored in the Curios belt slot.
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (isJetpack(chestStack)) {
            refuelJetpackFromCuriosTanks(player, chestStack);
        }

        forEachStackInCuriosSlot(player, "body", stack -> {
            if (isJetpack(stack)) {
                refuelJetpackFromInventoryAndCuriosTanks(player, stack);
                tickJetpackFromCurios(level, player, stack);
            }
        });
    }

    public static boolean hasCuriosJetpack(Player player) {
        final boolean[] found = {false};
        forEachStackInCuriosSlot(player, "body", stack -> {
            if (isJetpack(stack)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    public static boolean hasVanillaChestJetpack(Player player) {
        return player != null && isJetpack(player.getItemBySlot(EquipmentSlot.CHEST));
    }

    public static boolean isJetpack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return JETPACKS.containsKey(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static boolean isRefillTank(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return isFuelingTank(id) || isFillingTank(id) || id.equals(ResourceLocation.parse("create_sa:creative_filling_tank"));
    }


    public static Optional<ItemStack> getFirstRenderedCuriosJetpack(Player player) {
        return getFirstRenderedCuriosJetpack(player, "body");
    }

    private static Optional<ItemStack> getFirstRenderedCuriosJetpack(Player player, String slotId) {
        try {
            Class<?> curiosApi = Class.forName(CURIOS_API_CLASS);
            Method getCuriosInventory = curiosApi.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class);
            Object optionalInventory = getCuriosInventory.invoke(null, player);

            if (!(optionalInventory instanceof Optional<?> inventoryOptional) || inventoryOptional.isEmpty()) {
                return Optional.empty();
            }

            Object curiosInventory = inventoryOptional.get();
            Method getStacksHandler = curiosInventory.getClass().getMethod("getStacksHandler", String.class);
            Object optionalStacksHandler = getStacksHandler.invoke(curiosInventory, slotId);

            if (!(optionalStacksHandler instanceof Optional<?> stacksHandlerOptional) || stacksHandlerOptional.isEmpty()) {
                return Optional.empty();
            }

            Object stacksHandler = stacksHandlerOptional.get();

            boolean handlerVisible = true;
            try {
                Object visible = stacksHandler.getClass().getMethod("isVisible").invoke(stacksHandler);
                if (visible instanceof Boolean visibleBoolean) {
                    handlerVisible = visibleBoolean;
                }
            } catch (ReflectiveOperationException ignored) {
                // Older/newer Curios builds may not expose this method in the same way.
            }

            if (!handlerVisible) {
                return Optional.empty();
            }

            Object stacks = stacksHandler.getClass().getMethod("getStacks").invoke(stacksHandler);
            Object renders = stacksHandler.getClass().getMethod("getRenders").invoke(stacksHandler);
            Method getSlots = stacks.getClass().getMethod("getSlots");
            Method getStackInSlot = stacks.getClass().getMethod("getStackInSlot", int.class);

            int slots = (Integer) getSlots.invoke(stacks);
            for (int i = 0; i < slots; i++) {
                Object result = getStackInSlot.invoke(stacks, i);
                if (!(result instanceof ItemStack stack) || !isJetpack(stack)) {
                    continue;
                }

                boolean shouldRender = true;
                if (renders instanceof List<?> renderList && i < renderList.size()) {
                    shouldRender = Boolean.TRUE.equals(renderList.get(i));
                }

                if (shouldRender) {
                    return Optional.of(stack);
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // If Curios changes its internals, fail closed instead of crashing the renderer.
        }
        return Optional.empty();
    }

    public static List<ItemStack> getRenderedCuriosTanks(Player player) {
        return getRenderedCuriosTanks(player, "belt");
    }

    private static List<ItemStack> getRenderedCuriosTanks(Player player, String slotId) {
        List<ItemStack> result = new ArrayList<>();
        try {
            Class<?> curiosApi = Class.forName(CURIOS_API_CLASS);
            Method getCuriosInventory = curiosApi.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class);
            Object optionalInventory = getCuriosInventory.invoke(null, player);

            if (!(optionalInventory instanceof Optional<?> inventoryOptional) || inventoryOptional.isEmpty()) {
                return result;
            }

            Object curiosInventory = inventoryOptional.get();
            Method getStacksHandler = curiosInventory.getClass().getMethod("getStacksHandler", String.class);
            Object optionalStacksHandler = getStacksHandler.invoke(curiosInventory, slotId);

            if (!(optionalStacksHandler instanceof Optional<?> stacksHandlerOptional) || stacksHandlerOptional.isEmpty()) {
                return result;
            }

            Object stacksHandler = stacksHandlerOptional.get();

            boolean handlerVisible = true;
            try {
                Object visible = stacksHandler.getClass().getMethod("isVisible").invoke(stacksHandler);
                if (visible instanceof Boolean visibleBoolean) {
                    handlerVisible = visibleBoolean;
                }
            } catch (ReflectiveOperationException ignored) {
            }

            if (!handlerVisible) {
                return result;
            }

            Object stacks = stacksHandler.getClass().getMethod("getStacks").invoke(stacksHandler);
            Object renders = stacksHandler.getClass().getMethod("getRenders").invoke(stacksHandler);
            Method getSlots = stacks.getClass().getMethod("getSlots");
            Method getStackInSlot = stacks.getClass().getMethod("getStackInSlot", int.class);

            int slots = (Integer) getSlots.invoke(stacks);
            for (int i = 0; i < slots; i++) {
                Object stackResult = getStackInSlot.invoke(stacks, i);
                if (!(stackResult instanceof ItemStack stack) || !isRefillTank(stack)) {
                    continue;
                }

                boolean shouldRender = true;
                if (renders instanceof List<?> renderList && i < renderList.size()) {
                    shouldRender = Boolean.TRUE.equals(renderList.get(i));
                }

                if (shouldRender) {
                    result.add(stack);
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        return result;
    }

    public static Optional<String> getCsaRendererClass(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id.equals(ResourceLocation.parse("create_sa:brass_jetpack_chestplate"))) {
            return Optional.of("net.mcreator.createstuffadditions.client.renderer.jetpack.BrassJetpackArmorRenderer");
        }
        if (id.equals(ResourceLocation.parse("create_sa:andesite_jetpack_chestplate"))) {
            return Optional.of("net.mcreator.createstuffadditions.client.renderer.jetpack.AndesiteJetpackArmorRenderer");
        }
        if (id.equals(ResourceLocation.parse("create_sa:copper_jetpack_chestplate"))) {
            return Optional.of("net.mcreator.createstuffadditions.client.renderer.jetpack.CopperJetpackArmorRenderer");
        }
        if (id.equals(ResourceLocation.parse("create_sa:netherite_jetpack_chestplate"))) {
            return Optional.of("net.mcreator.createstuffadditions.client.renderer.jetpack.NetheriteJetpackArmorRenderer");
        }
        return Optional.empty();
    }

    private static void tickJetpackFromCurios(Level level, Player player, ItemStack stack) {
        JetpackProcedure procedure = JETPACKS.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (procedure != null) {
            procedure.execute(level, player.getX(), player.getY(), player.getZ(), player, stack);
        }
    }

    private static void refuelJetpackFromInventoryAndCuriosTanks(Player player, ItemStack jetpack) {
        refuelJetpackFromSources(player, jetpack, true, true);
    }

    private static void refuelJetpackFromCuriosTanks(Player player, ItemStack jetpack) {
        refuelJetpackFromSources(player, jetpack, false, true);
    }

    private static void refuelJetpackFromSources(Player player, ItemStack jetpack, boolean includeInventory, boolean includeCuriosBelt) {
        double capacity = getCsaConfigDouble("GADGETCAPACITY", 1600.0D);

        boolean canReceiveFuel = jetpack.is(ItemTags.create(FUELABLE_TAG_ID));
        boolean canReceiveWater = jetpack.is(ItemTags.create(FILLABLE_TAG_ID));

        if (includeInventory) {
            for (ItemStack tank : player.getInventory().items) {
                transferFromTank(tank, jetpack, capacity, canReceiveFuel, canReceiveWater);
            }
        }

        if (includeCuriosBelt) {
            forEachStackInCuriosSlot(player, "belt", tank ->
                    transferFromTank(tank, jetpack, capacity, canReceiveFuel, canReceiveWater)
            );
        }
    }

    private static void transferFromTank(ItemStack tank, ItemStack jetpack, double capacity, boolean canReceiveFuel, boolean canReceiveWater) {
        if (tank.isEmpty()) {
            return;
        }

        ResourceLocation tankId = BuiltInRegistries.ITEM.getKey(tank.getItem());

        if (canReceiveFuel && isFuelingTank(tankId)) {
            transferOne(tank, jetpack, "tagStock", "tagFuel", capacity, false);
        }

        if (canReceiveWater && isFillingTank(tankId)) {
            transferOne(tank, jetpack, "tagStock", "tagWater", capacity, false);
        }

        if (tankId.equals(ResourceLocation.parse("create_sa:creative_filling_tank"))) {
            // CSA's creative filling tank is special: despite its name, its own inventory
            // tick procedure refills both fuel and water for valid items in hand/armor.
            // Mirror that behavior for jetpacks equipped in Curios or refilled from a Curios belt.
            if (canReceiveFuel) {
                addAmountToTarget(jetpack, "tagFuel", capacity, 50.0D);
            }
            if (canReceiveWater) {
                addAmountToTarget(jetpack, "tagWater", capacity, 50.0D);
            }
        }
    }

    private static boolean isFuelingTank(ResourceLocation id) {
        return id.equals(ResourceLocation.parse("create_sa:small_fueling_tank"))
                || id.equals(ResourceLocation.parse("create_sa:medium_fueling_tank"))
                || id.equals(ResourceLocation.parse("create_sa:large_fueling_tank"));
    }

    private static boolean isFillingTank(ResourceLocation id) {
        return id.equals(ResourceLocation.parse("create_sa:small_filling_tank"))
                || id.equals(ResourceLocation.parse("create_sa:medium_filling_tank"))
                || id.equals(ResourceLocation.parse("create_sa:large_filling_tank"));
    }

    private static void transferOne(ItemStack source, ItemStack target, String sourceKey, String targetKey, double targetCapacity, boolean infiniteSource) {
        double sourceAmount = getCustomDouble(source, sourceKey);
        double targetAmount = getCustomDouble(target, targetKey);

        if ((infiniteSource || sourceAmount > 0.0D) && targetAmount < targetCapacity) {
            setCustomDouble(target, targetKey, Math.min(targetCapacity, targetAmount + 1.0D));
            if (!infiniteSource) {
                setCustomDouble(source, sourceKey, Math.max(0.0D, sourceAmount - 1.0D));
            }
        }
    }

    private static void addOneToTarget(ItemStack target, String targetKey, double targetCapacity) {
        addAmountToTarget(target, targetKey, targetCapacity, 1.0D);
    }

    private static void addAmountToTarget(ItemStack target, String targetKey, double targetCapacity, double amount) {
        double targetAmount = getCustomDouble(target, targetKey);
        if (targetAmount < targetCapacity) {
            setCustomDouble(target, targetKey, Math.min(targetCapacity, targetAmount + amount));
        }
    }

    private static double getCustomDouble(ItemStack stack, String key) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble(key);
    }

    private static void setCustomDouble(ItemStack stack, String key, double value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putDouble(key, value));
    }

    private static double getCsaConfigDouble(String fieldName, double fallback) {
        try {
            Class<?> configClass = Class.forName("net.mcreator.createstuffadditions.configuration.CreateSaConfigConfiguration");
            Field field = configClass.getField(fieldName);
            Object configValue = field.get(null);
            Object value = configValue.getClass().getMethod("get").invoke(configValue);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        return fallback;
    }

    public static void setClientFlyingState(Player player, boolean flying) {
        if (player == null || hasVanillaChestJetpack(player)) {
            return;
        }

        if (hasCuriosJetpack(player)) {
            player.getPersistentData().putBoolean("CsaFlying", flying);
        } else if (!flying) {
            player.getPersistentData().putBoolean("CsaFlying", false);
        }
    }

    private static void forEachStackInCuriosSlot(Player player, String slotId, StackConsumer consumer) {
        try {
            Class<?> curiosApi = Class.forName(CURIOS_API_CLASS);
            Method getCuriosInventory = curiosApi.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class);
            Object optionalInventory = getCuriosInventory.invoke(null, player);

            if (!(optionalInventory instanceof Optional<?> inventoryOptional) || inventoryOptional.isEmpty()) {
                return;
            }

            Object curiosInventory = inventoryOptional.get();
            Method getStacksHandler = curiosInventory.getClass().getMethod("getStacksHandler", String.class);
            Object optionalStacksHandler = getStacksHandler.invoke(curiosInventory, slotId);

            if (!(optionalStacksHandler instanceof Optional<?> stacksHandlerOptional) || stacksHandlerOptional.isEmpty()) {
                return;
            }

            Object stacksHandler = stacksHandlerOptional.get();
            Object stacks = stacksHandler.getClass().getMethod("getStacks").invoke(stacksHandler);
            Method getSlots = stacks.getClass().getMethod("getSlots");
            Method getStackInSlot = stacks.getClass().getMethod("getStackInSlot", int.class);

            int slots = (Integer) getSlots.invoke(stacks);
            for (int i = 0; i < slots; i++) {
                Object result = getStackInSlot.invoke(stacks, i);
                if (result instanceof ItemStack stack) {
                    consumer.accept(stack);
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // If Curios changes its internals, fail closed instead of crashing the world.
        }
    }

    @FunctionalInterface
    private interface StackConsumer {
        void accept(ItemStack stack);
    }

    private static final class JetpackProcedure {
        private final String className;
        private Method executeMethod;

        private JetpackProcedure(String className) {
            this.className = className;
        }

        private void execute(Level level, double x, double y, double z, Player player, ItemStack stack) {
            try {
                if (executeMethod == null) {
                    Class<?> procedureClass = Class.forName(className);
                    executeMethod = procedureClass.getMethod(
                            "execute",
                            net.minecraft.world.level.LevelAccessor.class,
                            double.class,
                            double.class,
                            double.class,
                            net.minecraft.world.entity.Entity.class,
                            ItemStack.class
                    );
                }

                executeMethod.invoke(null, level, x, y, z, player, stack);
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Fail closed. A missing CSA procedure should not crash the player tick.
            }
        }
    }

    public record FlyingStatePayload(boolean flying) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<FlyingStatePayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "flying_state"));
        public static final StreamCodec<ByteBuf, FlyingStatePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL,
                FlyingStatePayload::flying,
                FlyingStatePayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
