package org.eu.pcraft.powerfulfireworks.config;

import org.eu.pcraft.powerfulfireworks.PowerfulFireworks;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.objectmapping.meta.Processor;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.spongepowered.configurate.objectmapping.meta.Processor.comments;

public class ConfigManager {
    CommentedConfigurationNode node;

    YamlConfigurationLoader loader;

    Class configType;
    Object configModule;

    public ConfigManager(Path src,Object cm){
        loader = YamlConfigurationLoader.builder()
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .path(src) // Set where we will load and save to
                .build();
        node=loader.createNode();
        configType=cm.getClass();
        configModule=cm;
    }
    public void loadConfig(){
        try {
            node=loader.load();
            configModule=node.get(configType);
            saveConfig();
        }catch (ConfigurateException e){
            throw new RuntimeException(e);
        }
    }
    public void saveConfig(){
        try {
            node.set(configModule);
            loader.save(node);
        }catch (ConfigurateException e){
            throw new RuntimeException(e);
        }
    }
}
