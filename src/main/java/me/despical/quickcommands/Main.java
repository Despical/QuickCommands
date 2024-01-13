package me.despical.quickcommands;

import me.despical.commandframework.Command;
import me.despical.commandframework.CommandArguments;
import me.despical.commandframework.CommandFramework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author Despical
 * <p>
 * Created at 13.01.2024
 */
public class Main extends JavaPlugin implements Listener {

    private Set<CommandData> commands;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        commands = new HashSet<>();
        CommandFramework commandFramework = new CommandFramework(this);
        commandFramework.registerCommands(this);

        getServer().getPluginManager().registerEvents(this, this);

        registerCommands();
    }

    private void registerCommands() {
        var section = getConfig().getConfigurationSection("commands");

        if (section == null) {
            getLogger().warning("''commands'' section is not found!");
            return;
        }

        commands.clear();

        for (final var key : section.getKeys(false)) {
            String name = section.getString(key + ".name");
            String execute = section.getString(key + ".execute");
            String permission = section.getString(key + ".permission", "");
            Consumer<Player> consumer = (player) -> player.performCommand(execute);

            commands.add(new CommandData(name, permission, consumer));
        }
    }

    @Command(name = "quickcommands")
    public void mainCommand(CommandArguments arguments) {
    }

    @Command(name = "quickcommands.reload", onlyOp = true)
    public void reloadCommand(CommandArguments arguments) {
        reloadConfig();
        registerCommands();

        arguments.sendMessage("Commands reloaded.");
    }

    @EventHandler
    public void onCommandUsage(PlayerCommandPreprocessEvent event) {
        var command = matchCommand(event.getMessage());
        var player = event.getPlayer();

        command.ifPresent(data -> {
            if (!data.test(player))
                return;

            event.setCancelled(true);
            data.execute.accept(player);
        });
    }

    private Optional<CommandData> matchCommand(String name) {
        return commands.stream().filter(data -> name.equals(data.name)).findFirst();
    }

    private record CommandData(String name, String permission, Consumer<Player> execute) {

        boolean test(Player player) {
            return permission.isEmpty() || player.hasPermission(permission);
        }
    }
}