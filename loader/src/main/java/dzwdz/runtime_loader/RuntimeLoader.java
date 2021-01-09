package dzwdz.runtime_loader;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.loader.metadata.EntrypointMetadata;
import net.fabricmc.loader.metadata.LoaderModMetadata;
import net.fabricmc.loader.metadata.ModMetadataParser;
import net.fabricmc.loader.util.FileSystemUtil;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

import static com.mojang.brigadier.arguments.StringArgumentType.string;

public class RuntimeLoader implements ModInitializer {
    public static Logger LOGGER = LogManager.getLogger();

    public static void loadMod(String s_url, boolean dedicated) throws Exception {
        URL url = new URL("file://" + s_url);

        FileSystemUtil.FileSystemDelegate jarFs = FileSystemUtil.getJarFileSystem(url.toURI(), false);
        Path modJson = jarFs.get().getPath("fabric.mod.json");
        LoaderModMetadata info = ModMetadataParser.parseMetadata(LOGGER, modJson);

        URLClassLoader child = new URLClassLoader(
                new URL[]{url},
                RuntimeLoader.class.getClassLoader()
        );

        for (EntrypointMetadata entry : info.getEntrypoints("main")) {
            Class<?> classToLoad = Class.forName(entry.getValue(), true, child);
            Method method = classToLoad.getDeclaredMethod("onInitialize");
            Object instance = classToLoad.newInstance();
            method.invoke(instance);
        }

        if (dedicated) {
            for (EntrypointMetadata entry : info.getEntrypoints("server")) { //untested
                Class<?> classToLoad = Class.forName(entry.getValue(), true, child);
                Method method = classToLoad.getDeclaredMethod("onInitializeServer");
                Object instance = classToLoad.newInstance();
                method.invoke(instance);
            }
        } else {
            for (EntrypointMetadata entry : info.getEntrypoints("client")) {
                Class<?> classToLoad = Class.forName(entry.getValue(), true, child);
                Method method = classToLoad.getDeclaredMethod("onInitializeClient");
                Object instance = classToLoad.newInstance();
                method.invoke(instance);
            }
        }
    }

    private void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        dispatcher.register(
                CommandManager.literal("loadmod")
                        .then(
                                CommandManager.argument("path", string())
                                        .executes(context -> {
                                            try {
                                                loadMod(context.getArgument("path", String.class), dedicated);
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
