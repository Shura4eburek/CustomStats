package org.mamoru.customStats;


import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

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
            CustomPlaceholderExpansion expansion = new CustomPlaceholderExpansion(this);
            expansion.register(); // Register placeholders
            Bukkit.getPluginManager().registerEvents(expansion, this); // Register events
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

    public class CustomPlaceholderExpansion extends PlaceholderExpansion implements Listener {

        private final CustomStats plugin;
        private final Map<Player, Long> sessionStartTimes = new HashMap<>();

        public CustomPlaceholderExpansion(CustomStats plugin) {
            this.plugin = plugin;
        }

        @Override
        public @NotNull String getIdentifier() {
            return "customstats";
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
            return true;
        }

        @Override
        public boolean canRegister() {
            return true;
        }

        // Event to track player's join time
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            sessionStartTimes.put(player, System.currentTimeMillis()); // Store the session start time
            System.out.println("Player " + player.getName() + " joined. Session start time recorded.");
        }

        @Override
        public String onPlaceholderRequest(Player player, @NotNull String identifier) {
            if (player == null) {
                return "";
            }

            // Placeholder for total game time
            if (identifier.equals("time_played_total")) {
                long totalPlayTime = player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 20; // Time in seconds
                return formatTime(totalPlayTime); // Format into hours, minutes
            }

            // Placeholder for the time of the current session
            if (identifier.equals("time_since_last_played")) {
                Long sessionStartTime = sessionStartTimes.get(player);
                if (sessionStartTime != null) {
                    long sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 1000; // Time in seconds
                    return formatTime(sessionDuration);
                } else {
                    return "Session time not available";
                }
            }

            // Placeholder for first join date
            if (identifier.equals("first_join_date")) {
                return new java.text.SimpleDateFormat("dd.MM.yyyy").format(new java.util.Date(player.getFirstPlayed()));
            }

            // Placeholder for count of deaths
            if (identifier.equals("deaths")) {
                int deaths = player.getStatistic(org.bukkit.Statistic.DEATHS); // Get the number of deaths
                return String.valueOf(deaths);
            }

            // If the placeholder is not found, return null.
            return null;
        }

        // Method for formatting time into hours and minutes
        private String formatTime(long seconds) {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + " h " + minutes + " min";
        }
    }

    @Override
    public void onDisable() {
        // Logic when disabling the plugin (if necessary)
    }
}
