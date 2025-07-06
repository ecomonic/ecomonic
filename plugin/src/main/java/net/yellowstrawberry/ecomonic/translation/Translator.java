package net.yellowstrawberry.ecomonic.translation;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.yellowstrawberry.ecomonic.config.Configurations;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

public class Translator {
    private static final Map<String, Map<String, Object>> translations = new HashMap<>();

    public static void loadTranslation(File file) {
        try {
            Map<String, Object> o = flatten(new Yaml().load(new FileInputStream(file)));
            translations.put((String) o.get("locale"), o);
            for(String locale : (List<String>) o.get("coverage")) translations.putIfAbsent(locale, o);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void send(CommandContext<CommandSourceStack> context, String key){
        send(context.getSource(), key);
    }

    public static void send(CommandSourceStack stack, String key){
        send(stack.getSender(), key);
    }

    public static void send(CommandSender sender, String key) {
        sender.sendMessage(get(sender, key).build());
    }

    public static void send(CommandContext<CommandSourceStack> context, String key, Map<String, Object> args){
        send(context.getSource(), key, args);
    }

    public static void send(CommandSourceStack stack, String key, Map<String, Object> args){
        send(stack.getSender(), key, args);
    }

    public static void send(CommandSender sender, String key, Map<String, Object> args) {
        sender.sendMessage(get(sender, key).fulfill(args).build());
    }

    public static AfulfilledComponent get(CommandSourceStack stack, String key) {
        return get((stack.getSender() instanceof Player p ? p.locale() : Locale.getDefault()).toLanguageTag().replaceAll("-", "_"), key);
    }

    public static AfulfilledComponent get(CommandSender sender, String key) {
        return get((sender instanceof Player p ? p.locale() : Locale.getDefault()).toLanguageTag().replaceAll("-", "_"), key);
    }


    public static AfulfilledComponent get(String language, String key) {
        return (AfulfilledComponent) translations.getOrDefault(language, translations.get(Configurations.defaultLanguage)).getOrDefault(key, translations.get(Configurations.defaultLanguage).get(key));
    }

    private static Map<String, Object> flatten(Map<String, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        flatten(null, map, result);
        return result;
    }

    private static void flatten(String prefix, Map<String, Object> source, Map<String, Object> target) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String currentKey = (prefix != null) ? prefix + "." + entry.getKey() : entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                flatten(currentKey, (Map<String, Object>) value, target);
            } else {
                if(prefix == null) target.put(currentKey, value);
                else target.put(currentKey, new AfulfilledComponent(value.toString()));
            }
        }
    }
}
