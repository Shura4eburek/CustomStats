package org.mamoru.customStats;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CustomStats extends JavaPlugin {

    private File configFile;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        createConfig();

        // Command registration
        String commandName = config.getString("command", "");
        if (commandName != null && !commandName.isEmpty()) {
            if (getCommand(commandName) != null) {
                this.getCommand(commandName).setExecutor(new StatsCommandExecutor());
            } else {
                getLogger().severe("Команда " + commandName + " не найдена в plugin.yml");
            }
        } else {
            getLogger().severe("The command name is not specified in the configuration file!");
        }

        // Checking for PlaceholderAPI and registering placeholders
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CustomPlaceholderExpansion(this).register();
        } else {
            getLogger().warning("PlaceholderAPI is not found. Placeholders won't work.");
        }
    }

    private void createConfig() {
        // Load or create a configuration file
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try {
                getDataFolder().mkdirs(); // Create a plugin folder if it does not exist
                configFile.createNewFile();
                config = YamlConfiguration.loadConfiguration(configFile);

                // Save default config
                config.set("command", "stats");
                config.set("message", List.of(
                        "%player_name%",
                        "Total played: %customstats_time_played_total%",
                        "Current session: %customstats_time_since_last_played%",
                        "Registration date: %customstats_first_join_date%",
                        "Count of deaths: %customstats_deaths%"));
                config.save(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            config = YamlConfiguration.loadConfiguration(configFile);
        }
    }

    private class StatsCommandExecutor implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only a player can use this command.");
                return true;
            }

            Player player = (Player) sender;

            List<String> messages = config.getStringList("message");
            if (messages.isEmpty()) {
                player.sendMessage("Default stats message");
            } else {
                for (String message : messages) {
                    // Placeholder replacement via PlaceholderAPI
                    String replacedMessage = PlaceholderAPI.setPlaceholders(player, message);
                    player.sendMessage(replacedMessage);
                }
            }
            return true;
        }
    }

    public class CustomPlaceholderExpansion extends PlaceholderExpansion {

        private final CustomStats plugin;

        public CustomPlaceholderExpansion(CustomStats plugin) {
            this.plugin = plugin;
        }

        @Override
        public @NotNull String getIdentifier() {
            return "customstats"; // Identifier for placeholders, e.g. %customstats_<placeholder>%
        }

        @Override
        public @NotNull String getAuthor() {
            return plugin.getDescription().getAuthors().toString();
        }

        @Override
        public @NotNull String getVersion() {
            return plugin.getDescription().getVersion();
        }

        @Override
        public boolean persist() {
            return true; // The plugin will remain registered
        }

        @Override
        public boolean canRegister() {
            return true; // Allowing placeholders to register
        }

        @Override
        public String onPlaceholderRequest(Player player, @NotNull String identifier) {
            if (player == null) {
                return "";
            }

            // Placeholder for total game time
            if (identifier.equals("time_played_total")) {
                long totalPlayTime = player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 20; // Время в секундах
                return formatTime(totalPlayTime); // Форматируем в часы, минуты
            }

            // Placeholder for the time of the current session
            if (identifier.equals("time_since_last_played")) {
                long lastPlayed = (System.currentTimeMillis() - player.getLastPlayed()) / 1000; // Время в секундах
                return formatTime(lastPlayed);
            }

            // Placeholder for first entry date
            if (identifier.equals("first_join_date")) {
                return new java.text.SimpleDateFormat("dd.MM.yyyy").format(new java.util.Date(player.getFirstPlayed()));
            }

            // Placeholder for count of deaths
            if (identifier.equals("deaths")) {
                int deaths = player.getStatistic(org.bukkit.Statistic.DEATHS); // Получаем количество смертей
                return String.valueOf(deaths);
            }

            // If the placeholder is not found, return null.
            return null;
        }

        // Method for formatting time into hours, minutes
        private String formatTime(long seconds) {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + " ч " + minutes + " мин";
        }
    }

    @Override
    public void onDisable() {
        // Logic when disabling the plugin (if necessary)
    }
}
