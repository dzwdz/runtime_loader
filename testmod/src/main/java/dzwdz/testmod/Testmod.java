package dzwdz.testmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;

public class Testmod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((matrices, delta) -> {
            MinecraftClient.getInstance().textRenderer
                    .drawWithShadow(matrices, "oh no", 1, 1, 0xFFFF00FF);
        });
    }
}
