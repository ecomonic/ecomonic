package net.yellowstrawberry.ecomonic.translation;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Map;

public class AfulfilledComponent {
    private String message;

    public AfulfilledComponent(String message) {
        this.message = message;
    }

    public AfulfilledComponent fulfill(Map<String, Object> args) {
        for (Map.Entry<String, Object> entry : args.entrySet()) message = message.replaceAll("\\$" + entry.getKey(), entry.getValue().toString());
        return this;
    }

    public Component build() {
        return MiniMessage.miniMessage().deserialize(message);
    }
}
