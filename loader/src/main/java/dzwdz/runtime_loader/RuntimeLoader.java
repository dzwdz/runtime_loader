package dzwdz.runtime_loader;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import static com.mojang.brigadier.arguments.StringArgumentType.string;

public class RuntimeLoader implements ModInitializer {
    public static void loadMod(String url) throws Exception {
        URLClassLoader child = new URLClassLoader(
                new URL[]{new URL(url)},
                RuntimeLoader.class.getClassLoader()
        );
        Class<?> classToLoad = Class.forName("dzwdz.testmod.Testmod", true, child);
        Method method = classToLoad.getDeclaredMethod("onInitializeClient");
        Object instance = classToLoad.newInstance();
        method.invoke(instance);
    }

    private void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        dispatcher.register(
                CommandManager.literal("loadmod")
                        .then(
                                CommandManager.argument("url", string())
                                        .executes(context -> {
                                            try {
                                                loadMod(context.getArgument("url", String.class));
                                            } catch (Throwable t) {
                                                t.printStackTrace();
                                            }
                                            return 1;
                                        })
                        ).executes(context -> {
                            return 1;
                        })
        );
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(this::registerCommand);
    }
}
