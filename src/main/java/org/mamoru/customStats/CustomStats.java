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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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

        // Регистрация команды
        String commandName = config.getString("command", "");
        if (commandName != null && !commandName.isEmpty()) {
            if (getCommand(commandName) != null) {
                this.getCommand(commandName).setExecutor(new StatsCommandExecutor());
            } else {
                getLogger().severe("Команда " + commandName + " не найдена в plugin.yml");
            }
        } else {
            getLogger().severe("Имя команды не указано в конфигурационном файле!");
        }

        // Проверяем наличие PlaceholderAPI и регистрируем плейсхолдеры
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CustomPlaceholderExpansion(this).register();
        } else {
            getLogger().warning("PlaceholderAPI не найден. Плейсхолдеры не будут работать.");
        }
    }

    private void createConfig() {
        // Загружаем или создаем конфигурационный файл
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try {
                getDataFolder().mkdirs(); // Создаем папку плагина, если она не существует
                configFile.createNewFile();
                config = YamlConfiguration.loadConfiguration(configFile);

                // Сохраняем дефолтный конфиг
                config.set("command", "stats");
                config.set("message", List.of(
                        "%player_name%",
                        "Всего сыграно: %customstats_time_played_total%",
                        "Текущий сеанс: %customstats_time_since_last_played%",
                        "Дата регистрации: %customstats_first_join_date%",
                        "Количество смертей: %customstats_deaths%"));
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
                sender.sendMessage("Эту команду может использовать только игрок.");
                return true;
            }

            Player player = (Player) sender;

            List<String> messages = config.getStringList("message");
            if (messages.isEmpty()) {
                player.sendMessage("Default stats message");
            } else {
                for (String message : messages) {
                    // Замена плейсхолдеров через PlaceholderAPI
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
            return "customstats"; // Идентификатор для использования плейсхолдеров, например %customstats_<placeholder>%
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
            return true; // Плагин будет оставаться зарегистрированным
        }

        @Override
        public boolean canRegister() {
            return true; // Разрешаем регистрацию плейсхолдеров
        }

        @Override
        public String onPlaceholderRequest(Player player, @NotNull String identifier) {
            if (player == null) {
                return "";
            }

            // Плейсхолдер для общего времени игры
            if (identifier.equals("time_played_total")) {
                long totalPlayTime = player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 20; // Время в секундах
                return formatTime(totalPlayTime); // Форматируем в часы, минуты
            }

            // Плейсхолдер для времени текущего сеанса
            if (identifier.equals("time_since_last_played")) {
                long lastPlayed = (System.currentTimeMillis() - player.getLastPlayed()) / 1000; // Время в секундах
                return formatTime(lastPlayed);
            }

            // Плейсхолдер для даты первого входа
            if (identifier.equals("first_join_date")) {
                return new java.text.SimpleDateFormat("dd.MM.yyyy").format(new java.util.Date(player.getFirstPlayed()));
            }

            // Плейсхолдер для количества смертей
            if (identifier.equals("deaths")) {
                int deaths = player.getStatistic(org.bukkit.Statistic.DEATHS); // Получаем количество смертей
                return String.valueOf(deaths);
            }

            // Если плейсхолдер не найден, возвращаем null
            return null;
        }

        // Метод для форматирования времени в часы, минуты
        private String formatTime(long seconds) {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + " ч " + minutes + " мин";
        }
    }

    @Override
    public void onDisable() {
        // Логика при отключении плагина (если нужно)
    }
}
