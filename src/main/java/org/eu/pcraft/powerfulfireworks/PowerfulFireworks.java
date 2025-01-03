package org.eu.pcraft.powerfulfireworks;

import cn.afternode.commons.bukkit.BukkitPluginContext;
import cn.afternode.commons.bukkit.ConfigurationLocalizations;
import cn.afternode.commons.bukkit.IAdventureLocalizations;
import cn.afternode.commons.bukkit.message.MessageBuilder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;
import org.eu.pcraft.powerfulfireworks.commands.MainCommand;
import org.eu.pcraft.powerfulfireworks.commands.TestCommand;
import org.eu.pcraft.powerfulfireworks.config.ConfigManager;
import org.eu.pcraft.powerfulfireworks.config.MessagesConfigModule;
import org.eu.pcraft.powerfulfireworks.config.PepperConfigModule;
import org.eu.pcraft.powerfulfireworks.nms.NMSSelector;
import org.eu.pcraft.powerfulfireworks.nms.common.NMSProvider;
import org.eu.pcraft.powerfulfireworks.utils.BitmapFont;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PowerfulFireworks extends JavaPlugin {
    @Getter
    private static PowerfulFireworks instance;

    @Getter
    private BukkitPluginContext context;
    @Getter
    private NMSProvider nms;

    @Getter
    private ConfigManager configManager;
    @Getter
    private ConfigManager messagesManager;

    @Getter
    private MessagesConfigModule messageConfig = new MessagesConfigModule();
    @Getter
    public PepperConfigModule mainConfig = new PepperConfigModule();
    @Getter private Map<String, BitmapFont> fonts;

    private MainCommand mainCommand;

    FireworksTimer timer;

    @Override
    public void onLoad() {
        //instance
        PowerfulFireworks.instance = this;
        this.context = new BukkitPluginContext(this);

        //config
        loadConfigurations();
        if(mainConfig.debug){
            getLogger().warning("***WARNING***");
            getLogger().warning("You are using the DEBUGING mode!");
            getLogger().warning("To make it disabled, change 'debug' in config.yml into false!");
        }

        //nms
        this.nms = NMSSelector.getImplementation(Bukkit.getMinecraftVersion());
        if (this.nms == null) {
            throw new UnsupportedOperationException("Unsupported version " + Bukkit.getMinecraftVersion());
        } else {
            getSLF4JLogger().info("Using NMS version {}", this.nms.getVersion());
        }
    }

    @Override
    public void onEnable() {
        if (this.nms == null)
            throw new IllegalStateException("NMS not initialized");

        //bStats
        int pluginId = 24294;
        Metrics metrics = new Metrics(this, pluginId);
        //Permissions
        Permissions.register();

        // commands
        CommandMap map = Bukkit.getCommandMap();
        map.register("fireworks", new TestCommand());
        this.mainCommand = new MainCommand();
        map.register("fireworks", this.mainCommand);

        //Listener
        Bukkit.getPluginManager().registerEvents(new EventListener(), instance);
        //Timer
        timer=new FireworksTimer(
                mainConfig.randomFirework.min_delay,
                mainConfig.randomFirework.max_delay, instance);
        timer.start();
    }

    @Override
    public void onDisable() {
        timer.stop();
        // Plugin shutdown logic
    }

    public void runAfter(int ticks, Runnable runnable) {
        Bukkit.getScheduler().runTaskLater(this, runnable, ticks);
    }

    public void nextTick(Runnable runnable) {
        Bukkit.getScheduler().runTask(this, runnable);
    }

    public void loadConfigurations() {
        Path dataPath = getDataFolder().toPath();

        //load
        configManager=new ConfigManager(dataPath.resolve("config.yml"), mainConfig);
        messagesManager=new ConfigManager(dataPath.resolve("messages.yml"), messageConfig);
        configManager.loadConfig();
        messagesManager.loadConfig();
        //message
        try {
            this.context.setLocalizations(new ConfigurationLocalizations(this.context.loadConfiguration("messages.yml")));
            this.context.setDefaultLocalizeMode(IAdventureLocalizations.LocalizeMode.MM);
            this.context.setMessageLinePrefix(new MessageBuilder()
                    .localizations(this.context.getLocalizations())
                    .localizeMode(IAdventureLocalizations.LocalizeMode.MM)
                    .localize("prefix")
                    .build());
        } catch (Throwable t) {
            throw new RuntimeException("Unable to load messages", t);
        }

        // fonts
        Path folder = dataPath.resolve("fonts");
        try {
            this.fonts = new HashMap<>();
            if (!Files.exists(folder)) {
                Files.createDirectories(folder);
            }
            Map<String, String> conf = this.mainConfig.fonts;
            for (String id : conf.keySet()) {
                Path path = folder.resolve(conf.get(id));
                try {
                    if (Files.isRegularFile(path)) {
                        this.fonts.put(id.toLowerCase(Locale.ROOT), BitmapFont.parseBDF(Files.readString(path, StandardCharsets.UTF_8)));
                    } else {
                        this.getSLF4JLogger().warn("Invalid or missing font file {}: {}", id, conf.get(id));
                    }
                    this.getSLF4JLogger().info("Loaded font {} from {}", id, path);
                } catch (Throwable t) {
                    this.getSLF4JLogger().warn("Error loading font file {} for {}", conf.get(id), id, t);
                }
            }
            if (this.mainCommand != null)
                this.mainCommand.setFontIdComp(this.fonts.keySet().toArray(new String[0])); // Add to font ID completions
        } catch (Throwable t) {
            throw new RuntimeException("Unable to load fonts", t);
        }
    }
}
