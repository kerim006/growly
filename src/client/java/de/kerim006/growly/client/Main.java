package de.kerim006.growly.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class Main implements ClientModInitializer {

    private static final int MAX_PERCENTAGE = 100;
    private static final int EARLY_GROWTH_MAX_PERCENTAGE = 33;

    private static final double LABEL_HEIGHT = 0.8;

    private static final RenderStateDataKey<GrowthLabel> GROWTH_LABEL_KEY = RenderStateDataKey.create(() -> "growly:growth_label");

    @Override
    public void onInitializeClient() {
        System.out.println("Growly has started successfully and is ready to go!");

        LevelExtractionEvents.AFTER_BLOCK_OUTLINE_EXTRACTION.register(Main::extractGrowthLabel);
        LevelRenderEvents.COLLECT_SUBMITS.register(Main::renderCropGrowth);
    }

    private static void extractGrowthLabel(LevelExtractionContext context, HitResult hitResult) {
        if (!(hitResult instanceof BlockHitResult blockHitResult) || blockHitResult.getType() != HitResult.Type.BLOCK) 
            return;

        Component text = createGrowthLabel(context.level().getBlockState(blockHitResult.getBlockPos()));

        if (text != null) {
            Vec3 position = Vec3.upFromBottomCenterOf(blockHitResult.getBlockPos(), LABEL_HEIGHT);

            context.levelState().setData(GROWTH_LABEL_KEY, new GrowthLabel(position, text));
        }
    }

    private static void renderCropGrowth(LevelRenderContext context) {
        LevelRenderState levelState = context.levelState();

        GrowthLabel growthLabel = levelState.getData(GROWTH_LABEL_KEY);

        if (growthLabel == null)
            return;

        CameraRenderState camera = levelState.cameraRenderState;

        Vec3 labelPosition = growthLabel.position().subtract(camera.pos);

        context.submitNodeCollector().submitNameTag(
            context.poseStack(),
            labelPosition,
            0,
            growthLabel.text(),
            true,
            LightCoordsUtil.FULL_BRIGHT,
            camera
        );
    }

    private static Component createGrowthLabel(BlockState blockState) {
        if (!(blockState.getBlock() instanceof CropBlock cropBlock)) {
            return null;
        }

        int maxAge = cropBlock.getMaxAge();

        if (maxAge <= 0)
            return null;

        int age = cropBlock.getAge(blockState);
        int percentage = (age * MAX_PERCENTAGE + maxAge / 2) / maxAge;

        return Component.literal(percentage + "%").withStyle(getGrowthColor(percentage, age >= maxAge));
    }

    private static ChatFormatting getGrowthColor(int percentage, boolean readyToHarvest) {
        if (readyToHarvest)
            return ChatFormatting.GREEN;

        return percentage <= EARLY_GROWTH_MAX_PERCENTAGE ? ChatFormatting.RED : ChatFormatting.GOLD;
    }

    private record GrowthLabel(Vec3 position, Component text) {
        // Empty.
    }
}
