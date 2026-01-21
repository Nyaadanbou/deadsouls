package net.sanctuaryhosting.deadSouls;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.NumberConversions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static net.sanctuaryhosting.deadSouls.Util.distance2;
import static net.sanctuaryhosting.deadSouls.Util.getTotalExperience;
import static net.sanctuaryhosting.deadSouls.Util.isNear;
import static net.sanctuaryhosting.deadSouls.Util.parseColor;
import static net.sanctuaryhosting.deadSouls.Util.parseTimeMs;
import static net.sanctuaryhosting.deadSouls.Util.set;

public final class DeadSoulsPlugin extends JavaPlugin implements Listener, DeadSoulsAPI {

    final FileConfiguration config = getConfig();
    final Logger logger = getLogger();

    private final long soulReleaseTimerMs = parseTimeMs(config.getString("soul-release-timer"), Long.MAX_VALUE, logger);
    private final long soulFadeTimerMs = parseTimeMs(config.getString("soul-fade-timer"), Long.MAX_VALUE, logger);
    private final boolean smartSoulPlacement = config.getBoolean("smart-soul-placement");
    private int savedExperiencePerLevel;
    private float savedExperiencePercentage;
    private PvPBehavior pvpBehavior = PvPBehavior.NORMAL;
    private final ArrayList<Pattern> worldParsePatterns = new ArrayList<>();
    private final HashSet<UUID> enabledWorlds = new HashSet<>();
    private final long autoSaveMs = parseTimeMs(config.getString("auto-save"), 0L, logger);

    private final String soulSoundSpawned = config.getString("soul-sound-spawned");
    private final String soulSoundCalling = config.getString("soul-sound-calling");
    private final float soulSoundCallingDistance = (float) config.getDouble("soul-sound-calling-distance");
    private final String soulSoundCollectedItem = config.getString("soul-sound-collected-item");
    private final String soulSoundCollectedExperience = config.getString("soul-sound-collected-experience");
    private final String soulSoundFade = config.getString("soul-sound-fade");

    private final boolean soulModernParticles = config.getBoolean("soul-modern-particles");
    private final Particle.DustOptions soulLayerBaseOptions = new Particle.DustOptions(parseColor(config.getString("soul-color"), Color.fromRGB(255, 255, 255), logger), 2f);
    private final Particle.DustOptions soulLayerItemsOptions = new Particle.DustOptions(parseColor(config.getString("soul-color-items"), Color.fromRGB(0, 170, 170), logger), 2f);
    private final Particle.DustOptions soulLayerExperienceOptions = new Particle.DustOptions(parseColor(config.getString("soul-color-experience"), Color.fromRGB(197, 147, 214), logger), 2f);
    private final Particle.DustOptions soulLayerFadeOptions = new Particle.DustOptions(parseColor(config.getString("soul-color-fade"), Color.fromRGB(85, 85, 85), logger), 2f);

    private final int soulPageDepth = config.getInt("soul-page-depth");
    private final int soulAgeAncient = config.getInt("soul-age-ancient");

    private final String languageNoPermission = config.getString("language-no-permission");
    private final String languageSoulDoesNotExist = config.getString("language-actionbar-soul-does-not-exist");
    private final String languageSoulUnableToLocate = config.getString("language-actionbar-soul-unable-to-locate");
    private final String languageSoulSpawned = config.getString("language-soul-spawned");
    private final String languageSoulSpawnedCoordinates = config.getString("language-soul-spawned-coordinates");
    private final String languageSoulClaimed = config.getString("language-actionbar-soul-claimed");
    private final String languageSoulListNoSouls = config.getString("language-actionbar-soul-list-no-souls");
    private final String languageSoulListHeader = config.getString("language-soul-list-header");
    private final String languageSoulListNumberStyle = config.getString("language-soul-list-number-style");
    private final String languageSoulListAgeTimeStyle = config.getString("language-soul-list-age-time-style");
    private final String languageListAgeNew = config.getString("language-soul-list-age-new");
    private final String languageListAgeMinutesOld = config.getString("language-soul-list-age-minutes-old");
    private final String languageListAgeHoursOld = config.getString("language-soul-list-age-hours-old");
    private final String languageListAgeDaysOld = config.getString("language-soul-list-age-days-old");
    private final String languageListAgeAncient = config.getString("language-soul-list-age-ancient");
    private final String languageSoulListCoordinatesStyle = config.getString("language-soul-list-coordinates-style");
    private final String languageSoulListDistanceStyle = config.getString("language-soul-list-distance-style");
    private final String languageSoulListReleaseButton = config.getString("language-soul-list-release-button");
    private final String languageSoulListReleaseTooltip = config.getString("language-soul-list-release-button-tooltip");
    private final String languageSoulListTeleportButton = config.getString("language-soul-list-teleport-button");
    private final String languageSoulListTeleportTooltip = config.getString("language-soul-list-teleport-button-tooltip");
    private final String languageSoulTeleportSuccess = config.getString("language-actionbar-soul-teleport-success");
    private final String languageSoulListPageSpacer = config.getString("language-soul-list-page-spacer");
    private final String languageSoulListPage = config.getString("language-soul-list-page");
    private final String languageSoulListPageNextButton = config.getString("language-soul-list-page-next-button");
    private final String languageSoulListPageNextTooltip = config.getString("language-soul-list-page-next-tooltip");
    private final String languageSoulListPagePreviousButton = config.getString("language-soul-list-page-previous-button");
    private final String languageSoulListPagePreviousTooltip = config.getString("language-soul-list-page-previous-tooltip");
    private final String languageSoulListPageTotal = config.getString("language-soul-list-page-total");

    final String languageFreeSoulDoesNotExist = config.getString("language-free-soul-does-not-exist");
    final String languageFreeSoulAlreadyReleased = config.getString("language-free-soul-already-released");
    final String languageFreeSoulCannotFreeOwn = config.getString("language-free-soul-cannot-free-own");
    final String languageFreeSoulDoesNotBelong = config.getString("language-free-soul-does-not-belong");
    final String languageFreeSoulReleased = config.getString("language-free-soul-released");

    private final Set<EntityType> entitiesWithSouls = new HashSet<>();
    private final HashMap<Player, PlayerSoulInfo> watchedPlayers = new HashMap<>();
    private boolean refreshNearbySoulCache = false;
    private static final double collectionRange = NumberConversions.square(1);
    private SoulDatabase soulDatabase;
    private static final ItemStack[] noItemStack = new ItemStack[0];
    private final ComparatorSoulDistanceTo processPlayers_comparatorDistanceTo = new ComparatorSoulDistanceTo();
    private final Location processPlayers_playerLocation = new Location(null, 0, 0, 0);
    private final Random processPlayers_random = new Random();
    private final DeadSoulsAPI.SoulPickupEvent soulPickupEvent = new SoulPickupEvent();
    private long processPlayers_nextFadeCheck = 0;
    private long processPlayers_nextAutoSave = 0;

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private void processPlayers() {
        final SoulDatabase soulsDatabase = this.soulDatabase;
        if (soulsDatabase == null) {
            getLogger().log(Level.WARNING, "The DeadSouls database has not loaded yet.");
            return;
        }

        final PluginManager pluginManager = getServer().getPluginManager();
        final long now = System.currentTimeMillis();

        if (now > processPlayers_nextFadeCheck && soulFadeTimerMs < Long.MAX_VALUE) {
            final int fadingSouls = soulsDatabase.removeFadedSouls(soulFadeTimerMs);
            if (fadingSouls > 0) {
                this.refreshNearbySoulCache = true;
                getLogger().log(Level.FINE, "Removed " + fadingSouls + " fading soul(s).");
            }
            // Check for fading souls every 5 minutes.
            processPlayers_nextFadeCheck = now + 1000 * 60 * 5;
        }

        final boolean refreshNearbySoulCache = this.refreshNearbySoulCache;
        this.refreshNearbySoulCache = false;

        final boolean playCallingSounds = !soulSoundCalling.isEmpty() && !soulSoundCalling.equals("intentionally.empty") && soulSoundCallingDistance > 0f && this.processPlayers_random.nextInt(12) == 0;

        boolean databaseChanged = false;

        for (Map.Entry<Player, PlayerSoulInfo> entry : watchedPlayers.entrySet()) {
            final Player player = entry.getKey();
            final GameMode playerGameMode = player.getGameMode();
            final World world = player.getWorld();
            final PlayerSoulInfo info = entry.getValue();

            boolean searchNewSouls = refreshNearbySoulCache;

            // Update location
            final Location playerLocation = player.getLocation(processPlayers_playerLocation);
            if (!isNear(playerLocation, info.lastKnownLocation, 16)) {
                set(info.lastKnownLocation, playerLocation);
                searchNewSouls = true;
            }

            if (playerGameMode != GameMode.SPECTATOR) {
                final Block underPlayer = world.getBlockAt(playerLocation.getBlockX(), playerLocation.getBlockY() - 1, playerLocation.getBlockZ());
                if (underPlayer.getType().isSolid()) {
                    final Block atPlayer = world.getBlockAt(playerLocation.getBlockX(), playerLocation.getBlockY(), playerLocation.getBlockZ());
                    if (atPlayer.getType() != Material.LAVA) {
                        // Do not spawn souls in air or in lava
                        set(info.lastSafeLocation, playerLocation);
                    }
                }
            }

            // Update visible souls
            final ArrayList<SoulDatabase.Soul> visibleSouls = info.visibleSouls;
            if (searchNewSouls) {
                visibleSouls.clear();
                soulsDatabase.findSouls(world.getUID(), playerLocation.getBlockX(), playerLocation.getBlockZ(), 100, visibleSouls);
            }

            if (visibleSouls.isEmpty()) {
                continue;
            }

            { // Sort souls
                final ComparatorSoulDistanceTo comparator = this.processPlayers_comparatorDistanceTo;
                comparator.toX = playerLocation.getX();
                comparator.toY = playerLocation.getY();
                comparator.toZ = playerLocation.getZ();
                visibleSouls.sort(comparator);
            }

            // Send particles
            final int soulCount = visibleSouls.size();
            int remainingSoulsToShow = 16;
            final boolean viewAllSouls = playerGameMode == GameMode.SPECTATOR && player.hasPermission("deadsouls.souls.spectator");
            for (int i = 0; i < soulCount && remainingSoulsToShow > 0; i++) {
                final SoulDatabase.Soul soul = visibleSouls.get(i);
                if (!viewAllSouls && !soul.isAccessibleBy(player, now, soulReleaseTimerMs)) {
                    // Soul belongs to another player, do not show and disallow collection.
                    continue;
                }

                final Location soulLocation = soul.getLocation(player.getWorld());
                if (soulLocation == null) {
                    continue;
                }

                // Show this soul!
                if (soulModernParticles) {
                    if (soul.xp > 0) {
                        player.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, soulLocation.getX(), soulLocation.getY() - 1.5, soulLocation.getZ(), 20, 0.25, 0.1, 0.25, 0.001);
                    }
                    if (soul.items.length != 0) {
                        player.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, soulLocation.getX(), soulLocation.getY() - 1.5, soulLocation.getZ(), 20, 0.25, 0.1, 0.25, 0.001);
                    }

                    // Spawn particles for the ring
                    double radius = .75; // Radius for Soul Fire Flame ring.
                    int numParticles = 20; // Number of particles in ring.
                    for (int j = 0; j < numParticles; j++) {
                        double angle = 2 * Math.PI * j / numParticles;
                        double xOffset = radius * Math.cos(angle);
                        double zOffset = radius * Math.sin(angle);

                        Bukkit.getScheduler().runTaskLater(this, () -> { // Delay the particle spawning based on the index of the loop.
                            player.spawnParticle(Particle.SOUL_FIRE_FLAME, soulLocation.getX() + xOffset, soulLocation.getY() - 0.9, soulLocation.getZ() + zOffset, 1, 0, 0, 0, 0.001);
                        }, j * 2); // Change the delay (in ticks) as needed.
                    }
                }

                if (!soulModernParticles) {
                    player.spawnParticle(Particle.DUST, soulLocation, 10, 0.1, 0.1, 0.1, soulLayerBaseOptions);
                    if (soul.xp > 0) {
                        player.spawnParticle(Particle.DUST, soulLocation, 10, 0.1, 0.1, 0.1, soulLayerExperienceOptions);
                    }
                    if (soul.items.length != 0) {
                        player.spawnParticle(Particle.DUST, soulLocation, 10, 0.1, 0.1, 0.1, soulLayerItemsOptions);
                    }
                }

                remainingSoulsToShow--;
            }

            // Process collisions
            if (!player.isDead()) {
                final boolean playerCanCollectByDefault = (playerGameMode == GameMode.SURVIVAL || playerGameMode == GameMode.ADVENTURE);

                //noinspection ForLoopReplaceableByForEach
                for (int soulI = 0; soulI < visibleSouls.size(); soulI++) {
                    final SoulDatabase.Soul closestSoul = visibleSouls.get(soulI);
                    if (!closestSoul.isAccessibleBy(player, now, soulReleaseTimerMs)) {
                        // Soul of somebody else, do not collect
                        continue;
                    }

                    final double dst2 = distance2(closestSoul, playerLocation, 0.4);
                    final Location closestSoulLocation = closestSoul.getLocation(player.getWorld());

                    if (dst2 < collectionRange) {
                        soulPickupEvent.cancelled = !playerCanCollectByDefault;
                        soulPickupEvent.player = player;
                        soulPickupEvent.soul = closestSoul;
                        pluginManager.callEvent(soulPickupEvent);

                        if (!soulPickupEvent.cancelled) {
                            // Collect it!
                            if (closestSoul.xp > 0) {
                                player.giveExp(closestSoul.xp);
                                closestSoul.xp = 0;
                                if (!soulSoundCollectedExperience.isEmpty() && !soulSoundCollectedExperience.equals("intentionally.empty") && closestSoulLocation != null) {
                                    player.playSound(closestSoulLocation, soulSoundCollectedExperience, 1f, 1f);
                                }
                                databaseChanged = true;
                            }

                            final @NotNull ItemStack[] items = closestSoul.items;
                            if (items.length > 0) {
                                final HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(items);
                                if (overflow.isEmpty()) {
                                    closestSoul.items = noItemStack;
                                } else {
                                    closestSoul.items = overflow.values().toArray(noItemStack);
                                }

                                boolean someCollected = false;
                                if (overflow.size() < items.length) {
                                    someCollected = true;
                                    databaseChanged = true;
                                } else {
                                    for (Map.Entry<Integer, ItemStack> overflowEntry : overflow.entrySet()) {
                                        if (!items[overflowEntry.getKey()].equals(overflowEntry.getValue())) {
                                            someCollected = true;
                                            databaseChanged = true;
                                            break;
                                        }
                                    }
                                }

                                if (someCollected && !soulSoundCollectedItem.isEmpty() && !soulSoundCollectedItem.equals("intentionally.empty") && closestSoulLocation != null) {
                                    player.playSound(closestSoulLocation, soulSoundCollectedItem, 1f, 0.5f);
                                }
                            }

                            if (closestSoul.xp == 0 && closestSoul.items.length == 0) {
                                // Soul is depleted
                                soulsDatabase.removeSoul(closestSoul);
                                this.refreshNearbySoulCache = true;
                                if (!languageSoulClaimed.equals("intentionally.empty")) {
                                    Component soulClaimedMessage = miniMessage.deserialize(languageSoulClaimed);
                                    player.sendActionBar(soulClaimedMessage);
                                }

                                // Do some fancy effect
                                if (closestSoulLocation != null) {
                                    if (!soulSoundFade.isEmpty() && !soulSoundFade.equals("intentionally.empty")) {
                                        player.playSound(closestSoulLocation, soulSoundFade, 0.1f, 0.5f);
                                    }
                                    if (soulModernParticles) {
                                        player.spawnParticle(Particle.RAID_OMEN, closestSoulLocation, 12, 0.25, 0.1, 0.25, 0.001);
                                    } else {
                                        player.spawnParticle(Particle.DUST, closestSoulLocation, 20, 0.2, 0.2, 0.2, soulLayerFadeOptions);
                                    }
                                }
                            }
                        }
                    } else if (playCallingSounds && closestSoulLocation != null) {
                        player.playSound(closestSoulLocation, soulSoundCalling, soulSoundCallingDistance, 0.75f);
                    }
                    break;
                }
            }
        }

        if (databaseChanged) {
            soulsDatabase.markDirty();
        }

        final long autoSaveMs = this.autoSaveMs;
        if (now > processPlayers_nextAutoSave) {
            processPlayers_nextAutoSave = now + autoSaveMs;
            soulsDatabase.autoSave();
        }
    }

    @Override
    public void onEnable() {

        {
            this.savedExperiencePerLevel = 7;
            this.savedExperiencePercentage = 50;
            final String experienceToRetain = config.getString("saved-experience");
            if (experienceToRetain != null) {
                String parsedExperienceToRetain = experienceToRetain.replaceAll("\\s", "");
                boolean isSavedExperiencePercentage = false;
                if (parsedExperienceToRetain.endsWith("%")) {
                    isSavedExperiencePercentage = true;
                    parsedExperienceToRetain = parsedExperienceToRetain.substring(0, parsedExperienceToRetain.length() - 1);
                }
                try {
                    final int safeExperience = Integer.parseInt(parsedExperienceToRetain);
                    if (isSavedExperiencePercentage) {
                        if (safeExperience < 0 || safeExperience > 100) {
                            logger.log(Level.WARNING, "Invalid configuration: saved-experience percentage must be between 0% and 100%.");
                        } else {
                            savedExperiencePercentage = safeExperience / 100f;
                            savedExperiencePerLevel = 0;
                        }
                    } else {
                        if (safeExperience < 0) {
                            logger.log(Level.WARNING, "Invalid configuration: saved-experience per level must be a positive integer.");
                        } else {
                            savedExperiencePercentage = -1f;
                            savedExperiencePerLevel = safeExperience;
                        }
                    }
                } catch (NumberFormatException nfe) {
                    logger.log(Level.WARNING, "Invalid configuration: saved-experience is using an invalid format.");
                }
            }
        }

        {
            String pvpBehaviorString = config.getString("pvp-behavior");
            if (pvpBehaviorString == null) {
                pvpBehavior = PvPBehavior.NORMAL;
            } else {
                try {
                    pvpBehavior = PvPBehavior.valueOf(pvpBehaviorString.trim().toUpperCase());
                } catch (IllegalArgumentException ex) {
                    pvpBehavior = PvPBehavior.NORMAL;
                    final StringBuilder sb = new StringBuilder(128);
                    sb.append("Invalid configuration: '").append(pvpBehaviorString).append("' is not an acceptable value for pvp-behavior.");
                    sb.append("Acceptable values are: ");
                    for (PvPBehavior value : PvPBehavior.values()) {
                        sb.append(value.name().toLowerCase()).append(", ");
                    }
                    sb.setLength(sb.length() - 2);
                    logger.log(Level.WARNING, sb.toString());
                }
            }
        }

        entitiesWithSouls.clear();
        for (String entityName : config.getStringList("entities-soul-spawn")) {
            final EntityType entityType;
            try {
                entityType = EntityType.valueOf(entityName);
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, "Invalid configuration: Ignoring '" + entityName + "' as an entity type, no such entity name exists.");
                continue;
            }
            entitiesWithSouls.add(entityType);
        }

        worldParsePatterns.clear();
        for (String worlds : config.getStringList("enabled-worlds")) {
            worldParsePatterns.add(Util.compileSimpleGlob(worlds));
        }
        if (worldParsePatterns.isEmpty()) {
            logger.warning("Invalid configuration: No worlds could be found using the specified patterns. Existing souls will still be collectible, but no more souls will be spawned.");
        }

        refreshEnabledWorlds();

        saveDefaultConfig();

        final Server server = getServer();
        server.getPluginManager().registerEvents(this, this);

        {
            final Path dataFolder = getDataFolder().toPath();
            final Path soulsDB = dataFolder.resolve("souls-database.bin");
            soulDatabase = new SoulDatabase(this, soulsDB);
        }

        refreshNearbySoulCache = true;

        for (Player onlinePlayer : server.getOnlinePlayers()) {
            watchedPlayers.put(onlinePlayer, new PlayerSoulInfo());
        }

        server.getScheduler().runTaskTimer(this, this::processPlayers, 40, 40);
    }

    private void refreshEnabledWorlds() {
        final HashSet<UUID> worlds = this.enabledWorlds;
        worlds.clear();

        for (World world : getServer().getWorlds()) {
            final String name = world.getName();
            final UUID uuid = world.getUID();
            final String uuidString = uuid.toString();
            for (Pattern pattern : this.worldParsePatterns) {
                if (pattern.matcher(name).matches() || pattern.matcher(uuidString).matches()) {
                    worlds.add(uuid);
                }
            }
        }

        if (!worldParsePatterns.isEmpty() && worlds.isEmpty()) {
            getLogger().warning("Invalid configuration: No worlds could be found using the specified patterns. Existing souls will still be collectible, but no more souls will be spawned.");
        }
    }

    @Override
    public void onDisable() {
        final SoulDatabase soulDatabase = this.soulDatabase;
        if (soulDatabase != null) {
            try {
                final int faded = soulDatabase.removeFadedSouls(soulFadeTimerMs);
                if (faded > 0) {
                    getLogger().log(Level.FINE, "Removed " + faded + " fading soul(s).");
                }
                soulDatabase.save();
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to save the DeadSouls database.", e);
            }
            this.soulDatabase = null;
        }

        watchedPlayers.clear();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        final SoulDatabase soulDatabase = this.soulDatabase;
        if (soulDatabase == null) {
            getLogger().log(Level.WARNING, "The DeadSouls database not loaded yet.");
            return false;
        }

        if (!"souls".equalsIgnoreCase(command.getName())) {
            return false;
        }

        final String word = args.length >= 1 ? args[0]: "";
        int number;
        try {
            number = args.length >= 2 ? Integer.parseInt(args[1]): -1;
        } catch (NumberFormatException nfe) {
            number = -1;
        }

        if ("free".equalsIgnoreCase(word)) {
            soulDatabase.freeSoul(sender, number, soulReleaseTimerMs, sender.hasPermission("deadsouls.souls.release"), sender.hasPermission("deadsouls.souls.release.all"));
            return true;
        }

        if ("goto".equalsIgnoreCase(word)) {
            if (!(sender instanceof Player)) {
                String commandAvailableInGameMessage = "<red>This command is only available in-game.";
                sender.sendMessage(MiniMessage.miniMessage().deserialize(commandAvailableInGameMessage));
                return true;
            }

            final SoulDatabase.Soul soul = soulDatabase.getSoulById(number);
            if (soul == null) {
                Component soulDoesNotExistMessage = miniMessage.deserialize(languageSoulDoesNotExist);
                sender.sendActionBar(soulDoesNotExistMessage);
                return true;
            }

            if (!sender.hasPermission("deadsouls.souls.teleport.all")) {
                if (soul.isOwnedBy(sender)) {
                    if (!sender.hasPermission("deadsouls.souls.teleport")) {
                        Component noPermissionMessage = miniMessage.deserialize(languageNoPermission);
                        sender.sendMessage(noPermissionMessage);
                        return true;
                    }
                } else {
                    Component noPermissionMessage = miniMessage.deserialize(languageNoPermission);
                    sender.sendMessage(noPermissionMessage);
                    return true;
                }
            }

            final World world = getServer().getWorld(soul.locationWorld);
            if (world == null) {
                Component unableToLocateMessage = miniMessage.deserialize(languageSoulUnableToLocate);
                sender.sendActionBar(unableToLocateMessage);
                return true;
            }

            ((Player) sender).teleport(new Location(world, soul.locationX, soul.locationY, soul.locationZ), PlayerTeleportEvent.TeleportCause.COMMAND);
            Component soulTeleportSuccessMessage = miniMessage.deserialize(languageSoulTeleportSuccess);
            sender.sendActionBar(soulTeleportSuccessMessage);
            return true;
        }

        boolean listOwnSouls = sender.hasPermission("deadsouls.souls.list");
        boolean listAllSouls = sender.hasPermission("deadsouls.souls.list.all");

        if (!listOwnSouls && !listAllSouls) {
            Component noPermissionMessage = miniMessage.deserialize(languageNoPermission);
            sender.sendMessage(noPermissionMessage);
            return true;
        }

        if (word.isEmpty()) {
            if (number < 0) {
                number = 0;
            }
        } else if (!"page".equalsIgnoreCase(word)) {
            return false;
        }

        final UUID senderUUID = (sender instanceof OfflinePlayer) ? ((OfflinePlayer) sender).getUniqueId(): null;

        if (!(sender instanceof Player)) {
            // Console output
            final List<SoulDatabase.@Nullable Soul> soulsById = soulDatabase.getSoulsById();
            int shownSouls = 0;
            synchronized (soulsById) {
                for (int id = 0; id < soulsById.size(); id++) {
                    final SoulDatabase.Soul soul = soulsById.get(id);
                    if (soul == null) {
                        continue;
                    }
                    shownSouls++;

                    final World world = getServer().getWorld(soul.locationWorld);
                    final String worldStr = world == null ? soul.locationWorld.toString(): world.getName();

                    final String ownerStr;
                    if (soul.owner == null) {
                        ownerStr = "<free>";
                    } else {
                        final OfflinePlayer ownerPlayer = getServer().getOfflinePlayer(soul.owner);
                        final String ownerPlayerName = ownerPlayer.getName();
                        if (ownerPlayerName == null) {
                            ownerStr = soul.owner.toString();
                        } else {
                            ownerStr = ownerPlayerName;
                        }
                    }

                    sender.sendMessage(String.format("%d) %s %.1f %.1f %.1f   %s", id, worldStr, soul.locationX, soul.locationY, soul.locationZ, ownerStr));
                }
            }
            sender.sendMessage(shownSouls + " souls");
        } else {
            // Normal player output
            final List<SoulDatabase.@NotNull Soul> souls = soulDatabase.getSoulsByOwnerAndWorld(listAllSouls ? null: senderUUID, ((Player) sender).getWorld().getUID());
            final Location location = ((Player) sender).getLocation();
            souls.sort(Comparator.comparingLong(soulAndId -> -soulAndId.timestamp));

            final boolean canFree = sender.hasPermission("deadsouls.souls.release");
            final boolean canFreeAll = sender.hasPermission("deadsouls.souls.release.all");
            final boolean canGoto = sender.hasPermission("deadsouls.souls.teleport");
            final boolean canGotoAll = sender.hasPermission("deadsouls.souls.teleport.all");

            final int soulsPerPage = getConfig().getInt("soul-page-depth", soulPageDepth);
            final long now = System.currentTimeMillis();

            if (souls.isEmpty()) {
                Component soulListNoSoulsMessage = miniMessage.deserialize(languageSoulListNoSouls);
                sender.sendActionBar(soulListNoSoulsMessage);
            } else {
                Component soulListMessage = Component.empty();

                if (languageSoulListHeader != null && !languageSoulListHeader.isEmpty()) {
                    Component soulListHeaderMessage = miniMessage.deserialize(languageSoulListHeader,
                            Placeholder.component("player-name", Component.text(sender.getName())));
                    soulListMessage = soulListMessage.append(soulListHeaderMessage);
                }
                for (int i = Math.max(soulsPerPage * number, 0), end = Math.min(i + soulsPerPage, souls.size()); i < end; i++) {
                    final SoulDatabase.Soul soul = souls.get(i);
                    final float distance = (float) Math.sqrt(distance2(soul, location, 1));

                    soulListMessage = soulListMessage.append(Component.newline());
                    Component soulListNumberStyleMessage = miniMessage.deserialize(languageSoulListNumberStyle,
                            Placeholder.component("soul-number", Component.text(i + 1)));
                    soulListMessage = soulListMessage.append(soulListNumberStyleMessage);

                    long minutesOld = TimeUnit.MILLISECONDS.toMinutes(now - soul.timestamp);
                    String age;
                    if (minutesOld >= 0) {
                        if (minutesOld <= 1) {
                            age = String.format(languageListAgeNew);
                        } else if (minutesOld < 60 * 2) {
                            age = minutesOld + " " + String.format(languageListAgeMinutesOld);
                        } else if (minutesOld < 60 * 48) {
                            age = (minutesOld / 60) + " " + String.format(languageListAgeHoursOld);
                        } else if (minutesOld < 60 * 24L * soulAgeAncient) {
                            age = (minutesOld / (60 * 24)) + " " + String.format(languageListAgeDaysOld);
                        } else {
                            age = String.format(languageListAgeAncient);
                        }

                        Component soulListAgeTimeStyleMessage = miniMessage.deserialize(languageSoulListAgeTimeStyle,
                                Placeholder.component("soul-age", Component.text(age)));
                        soulListMessage = soulListMessage.append(soulListAgeTimeStyleMessage);
                    }

                    if (sender.hasPermission("deadsouls.souls.coordinates")) {
                        Component soulListCoordinatesStyleMessage = miniMessage.deserialize(languageSoulListCoordinatesStyle,
                                Placeholder.component("soul-x-coordinate", Component.text(Math.round(soul.locationX))),
                                Placeholder.component("soul-y-coordinate", Component.text(Math.round(soul.locationY))),
                                Placeholder.component("soul-z-coordinate", Component.text(Math.round(soul.locationZ))));
                        soulListMessage = soulListMessage.append(soulListCoordinatesStyleMessage);
                    }

                    if (sender.hasPermission("deadsouls.souls.distance")) {
                        Component soulListDistanceStyleMessage = miniMessage.deserialize(languageSoulListDistanceStyle,
                                Placeholder.component("soul-distance", Component.text(Math.round(distance))));
                        soulListMessage = soulListMessage.append(soulListDistanceStyleMessage);
                    }

                    soulListMessage = soulListMessage.append(Component.newline());

                    final boolean ownSoul = soul.isOwnedBy(sender);

                    if (soul.owner != null && (canFreeAll || (ownSoul && canFree)) && ((now - soul.timestamp) < soulReleaseTimerMs)) {
                        String releaseButton = "    <click:run_command:/souls free " + soul.id + ">" + "<hover:show_text:'" + languageSoulListReleaseTooltip + "'>" + languageSoulListReleaseButton + "</hover></click>";
                        Component releaseButtonMessage = miniMessage.deserialize(releaseButton);
                        soulListMessage = soulListMessage.append(releaseButtonMessage);
                    }

                    if (canGotoAll || (ownSoul && canGoto)) {
                        String teleportButton = "    <click:run_command:/souls goto " + soul.id + ">" + "<hover:show_text:'" + languageSoulListTeleportTooltip + "'>" + languageSoulListTeleportButton + "</hover></click>";
                        Component teleportButtonMessage = miniMessage.deserialize(teleportButton);
                        soulListMessage = soulListMessage.append(teleportButtonMessage);
                    }

                    soulListMessage = soulListMessage.append(Component.newline());
                }

                if (souls.size() > soulsPerPage) {
                    final boolean leftArrow = number > 0;
                    final int pages = (souls.size() + soulsPerPage - 1) / soulsPerPage;
                    final boolean rightArrow = number + 1 < pages;

                    soulListMessage = soulListMessage.append(Component.newline());
                    Component soulListPageSpacerMessage = miniMessage.deserialize(languageSoulListPageSpacer);
                    soulListMessage = soulListMessage.append(soulListPageSpacerMessage);

                    if (leftArrow) {
                        String previousPageButton = "<click:run_command:/souls page " + (number - 1) + ">" + "<hover:show_text:'" + languageSoulListPagePreviousTooltip + "'>" + languageSoulListPagePreviousButton + "</hover></click> ";
                        Component previousPageButtonMessage = miniMessage.deserialize(previousPageButton);
                        soulListMessage = soulListMessage.append(previousPageButtonMessage);
                    } else {
                        Component noPreviousPageButtonMessage = miniMessage.deserialize("<bold>  <reset>");
                        soulListMessage = soulListMessage.append(noPreviousPageButtonMessage);
                    }

                    Component soulListPageMessage = miniMessage.deserialize(languageSoulListPage,
                            Placeholder.component("soul-page-number", Component.text(number + 1)),
                            Placeholder.component("soul-page-total", Component.text(pages)));
                    soulListMessage = soulListMessage.append(soulListPageMessage);

                    if (rightArrow) {
                        String nextPageButton = " <click:run_command:/souls page " + (number + 1) + ">" + "<hover:show_text:'" + languageSoulListPageNextTooltip + "'>" + languageSoulListPageNextButton + "</hover></click>";
                        Component nextPageButtonMessage = miniMessage.deserialize(nextPageButton);
                        soulListMessage = soulListMessage.append(nextPageButtonMessage);
                    } else {
                        Component noNextPageButtonMessage = miniMessage.deserialize("<bold>  <reset>");
                        soulListMessage = soulListMessage.append(noNextPageButtonMessage);
                    }

                    if (languageSoulListPageTotal != null && !languageSoulListPageTotal.isEmpty()) {
                        Component soulListPageTotalMessage = miniMessage.deserialize("    " + languageSoulListPageTotal, Placeholder.component("soul-count", Component.text(souls.size())));
                        soulListMessage = soulListMessage.append(soulListPageTotalMessage);

                    }
                }
                sender.sendMessage(soulListMessage);
            }
        }

        return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onWorldLoaded(WorldLoadEvent event) {
        refreshEnabledWorlds();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Player player = event.getEntity();
        if (!player.hasPermission("deadsouls.souls.spawn")) {
            return;
        }

        final World world = player.getWorld();
        if (!enabledWorlds.contains(world.getUID())) {
            return;
        }

        final boolean pvp = player.getKiller() != null && !player.equals(player.getKiller());
        if (pvp && pvpBehavior == PvPBehavior.DISABLED) {
            return;
        }

        final SoulDatabase soulDatabase = this.soulDatabase;
        if (soulDatabase == null) {
            getLogger().log(Level.WARNING, "The DeadSouls database not loaded yet.");
            return;
        }

        // Actually clearing the drops is deferred to the end of the method:
        // in case of any bug that causes this method to crash, we don't want to just delete the items
        boolean clearItemDrops = false;
        boolean clearXPDrops = false;

        final ItemStack[] soulItems;
        if (event.getKeepInventory() || !player.hasPermission("deadsouls.souls.save.items")) {
            // We don't modify drops for this death at all
            soulItems = noItemStack;
        } else {
            final List<ItemStack> drops = event.getDrops();
            soulItems = drops.toArray(noItemStack);
            clearItemDrops = true;
        }

        int soulXp;
        if (event.getKeepLevel() || !player.hasPermission("deadsouls.souls.save.experience")
                // Required because getKeepLevel is not set when world's KEEP_INVENTORY is set, but it has the same effect
                // See https://hub.spigotmc.org/jira/browse/SPIGOT-2222
                || Boolean.TRUE.equals(world.getGameRuleValue(GameRule.KEEP_INVENTORY))) {
            // We don't modify XP for this death at all
            soulXp = 0;
        } else {
            final int totalExperience = getTotalExperience(player);
            if (savedExperiencePercentage >= 0) {
                soulXp = Math.round(totalExperience * savedExperiencePercentage);
            } else {
                soulXp = savedExperiencePerLevel * player.getLevel();
            }
            soulXp = Util.clamp(soulXp, 0, totalExperience);
            clearXPDrops = true;
        }

        if (soulXp == 0 && soulItems.length == 0) {
            // Soul would be empty
            return;
        }

        Location soulLocation = null;
        try {
            if (smartSoulPlacement) {
                PlayerSoulInfo info = watchedPlayers.get(player);
                if (info == null) {
                    getLogger().log(Level.WARNING, player + " was not watched, and does not have a safe location to spawn a soul.");
                    info = new PlayerSoulInfo();
                    watchedPlayers.put(player, info);
                }
                soulLocation = info.findSafeSoulSpawnLocation(player);
                info.lastSafeLocation.setWorld(null); // Reset it, so it isn't used twice
            } else {
                soulLocation = PlayerSoulInfo.findFallbackSoulSpawnLocation(player, player.getLocation(), false);
            }
        } catch (Exception bugException) {
            // Should never happen, but just in case!
            getLogger().log(Level.SEVERE, "Failed to find soul location, defaulting to player location!", bugException);
        }
        if (soulLocation == null) {
            soulLocation = player.getLocation();
        }

        final UUID owner;
        if ((pvp && pvpBehavior == PvPBehavior.RELEASED) || soulReleaseTimerMs <= 0) {
            owner = null;
        } else {
            owner = player.getUniqueId();
        }

        final int soulId = soulDatabase.addSoul(owner, world.getUID(), soulLocation.getX(), soulLocation.getY(), soulLocation.getZ(), soulItems, soulXp).id;
        refreshNearbySoulCache = true;

        // Do not offer to free the soul if it will be free sooner than the player can click the button.
        if (owner != null && soulReleaseTimerMs > 1000 && languageSoulSpawned != null && !languageSoulSpawned.isEmpty()) {
            Component soulSpawnMessage = miniMessage.deserialize(languageSoulSpawned);
            if (player.hasPermission("deadsouls.souls.release") || player.hasPermission("deadsouls.souls.release.all")) {
                String releaseButton = "    <click:run_command:/souls free " + soulId + ">" + "<hover:show_text:'" + languageSoulListReleaseTooltip + "'>" + languageSoulListReleaseButton + "</hover></click>";
                Component releaseButtonMessage = miniMessage.deserialize(releaseButton);
                soulSpawnMessage = soulSpawnMessage.append(releaseButtonMessage);
            }

            if (player.hasPermission("deadsouls.souls.teleport") || player.hasPermission("deadsouls.souls.teleport.all")) {
                String teleportButton = "    <click:run_command:/souls goto " + soulId + ">" + "<hover:show_text:'" + languageSoulListTeleportTooltip + "'>" + languageSoulListTeleportButton + "</hover></click>";
                Component teleportButtonMessage = miniMessage.deserialize(teleportButton);
                soulSpawnMessage = soulSpawnMessage.append(teleportButtonMessage);
            }

            // Show coordinates if the player has the proper permission.
            if (player.hasPermission("deadsouls.souls.coordinates")) {
                Component soulListCoordinatesStyleMessage = miniMessage.deserialize(languageSoulSpawnedCoordinates,
                        Placeholder.component("soul-x-coordinate", Component.text(Math.round(soulLocation.getX()))),
                        Placeholder.component("soul-y-coordinate", Component.text(Math.round(soulLocation.getY()))),
                        Placeholder.component("soul-z-coordinate", Component.text(Math.round(soulLocation.getZ()))));
                soulSpawnMessage = soulSpawnMessage.append(soulListCoordinatesStyleMessage);
            }

            soulSpawnMessage = soulSpawnMessage.append(Component.newline());
            player.sendMessage(soulSpawnMessage);
        }

        if (!soulSoundSpawned.isEmpty() && !soulSoundSpawned.equals("intentionally.empty")) {
            world.playSound(soulLocation, soulSoundSpawned, SoundCategory.MASTER, 1.1f, 1.7f);
        }

        // No need to set setKeepInventory/Level to false, because if we got here, it already is false
        if (clearItemDrops) {
            event.getDrops().clear();
        }
        if (clearXPDrops) {
            event.setNewExp(0);
            event.setNewLevel(0);
            event.setNewTotalExp(0);
            event.setDroppedExp(0);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();

        if (entity instanceof Player || !entitiesWithSouls.contains(entity.getType())) {
            return;
        }

        final ItemStack[] soulItems = event.getDrops().toArray(noItemStack);
        final int soulXp = event.getDroppedExp();

        if (soulXp == 0 && soulItems.length == 0) {
            // Soul would be empty
            return;
        }

        final SoulDatabase soulDatabase = this.soulDatabase;
        if (soulDatabase == null) {
            getLogger().log(Level.WARNING, "The DeadSouls database not loaded yet.");
            return;
        }

        final Location soulLocation = entity.getLocation();

        final World world = entity.getWorld();
        soulDatabase.addSoul(null, world.getUID(), soulLocation.getX(), soulLocation.getY(), soulLocation.getZ(), soulItems, soulXp);
        refreshNearbySoulCache = true;

        if (!soulSoundSpawned.isEmpty() && !soulSoundSpawned.equals("intentionally.empty")) {
            world.playSound(soulLocation, soulSoundSpawned, SoundCategory.MASTER, 1.1f, 1.7f);
        }

        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        watchedPlayers.put(event.getPlayer(), new PlayerSoulInfo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLeave(PlayerQuitEvent event) {
        watchedPlayers.remove(event.getPlayer());
    }

    private static final class PlayerSoulInfo {
        static final double SOUL_HOVER_OFFSET = 1.2;

        @NotNull
        final Location lastKnownLocation = new Location(null, 0, 0, 0);

        @NotNull
        final Location lastSafeLocation = new Location(null, 0, 0, 0);

        final ArrayList<SoulDatabase.Soul> visibleSouls = new ArrayList<>();

        @NotNull
        Location findSafeSoulSpawnLocation(@NotNull Player player) {
            final Location playerLocation = player.getLocation();
            if (isNear(lastSafeLocation, playerLocation, 20)) {
                set(playerLocation, lastSafeLocation);
                playerLocation.setY(playerLocation.getY() + SOUL_HOVER_OFFSET);
                return playerLocation;
            }

            // Too far, now we have to find a better location
            return findFallbackSoulSpawnLocation(player, playerLocation, true);
        }

        @NotNull
        static Location findFallbackSoulSpawnLocation(@NotNull Player player, @NotNull Location playerLocation, boolean improve) {
            final World world = player.getWorld();

            final int x = playerLocation.getBlockX();
            int y = Util.clamp(playerLocation.getBlockY(), world.getMinHeight(), world.getMaxHeight());
            final int z = playerLocation.getBlockZ();

            if (improve) {
                int yOff = 0;
                while (true) {
                    final Material type = world.getBlockAt(x, y + yOff, z).getType();
                    if (type.isSolid()) {
                        // Soul either started in a block or ended in it, do not want
                        yOff = 0;
                        break;
                    } else if (type == Material.LAVA) {
                        yOff++;

                        if (yOff > 8) {
                            // Probably dead in a lava column, we don't want to push it up
                            yOff = 0;
                            break;
                        }
                        // continue
                    } else {
                        // This place looks good
                        break;
                    }
                }

                y += yOff;
            }

            playerLocation.setY(y + SOUL_HOVER_OFFSET);
            return playerLocation;
        }
    }

    private static final class ComparatorSoulDistanceTo implements Comparator<SoulDatabase.Soul> {

        double toX, toY, toZ;

        private double distanceTo(@NotNull SoulDatabase.Soul s) {
            final double x = toX - s.locationX;
            final double y = toY - s.locationY;
            final double z = toZ - s.locationZ;
            return x * x + y * y + z * z;
        }

        @Override
        public int compare(@NotNull SoulDatabase.Soul o1, @NotNull SoulDatabase.Soul o2) {
            return Double.compare(distanceTo(o1), distanceTo(o2));
        }
    }

    private enum PvPBehavior {
        /**
         * no change
         */
        NORMAL,
        /**
         * souls are not created in PvP, items and XP drops like in vanilla Minecraft
         */
        DISABLED,
        /**
         * created souls are immediately free and can be collected by anyone
         */
        RELEASED
    }

    //region API Methods

    @Override
    public void getSouls(@NotNull Collection<@NotNull Soul> out) {
        out.clear();
        final SoulDatabase soulDatabase = this.soulDatabase;
        if (soulDatabase == null) {
            return;
        }
        final List<SoulDatabase.@Nullable Soul> souls = soulDatabase.getSoulsById();
        synchronized (souls) {
            final int size = souls.size();
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < size; i++) {
                final SoulDatabase.Soul soul = souls.get(i);
                if (soul == null)
                    continue;
                out.add(soul);
            }
        }
    }

    @Override
    public void getSoulsByPlayer(@NotNull Collection<@NotNull Soul> out, @Nullable UUID playerUUID) {
        out.clear();
        final SoulDatabase soulDatabase = this.soulDatabase;
        if (soulDatabase == null) {
            return;
        }
        final List<SoulDatabase.@Nullable Soul> souls = soulDatabase.getSoulsById();
        synchronized (souls) {
            final int size = souls.size();
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < size; i++) {
                final SoulDatabase.Soul soul = souls.get(i);
                if (soul == null || !Objects.equals(playerUUID, soul.owner))
                    continue;
                out.add(soul);
            }
        }
    }

    @Override
    public void getSoulsByWorld(@NotNull Collection<@NotNull Soul> out, @NotNull UUID worldUUID) {
        out.clear();
        final SoulDatabase soulDatabase = this.soulDatabase;
        if (soulDatabase == null) {
            return;
        }
        final List<SoulDatabase.@Nullable Soul> souls = soulDatabase.getSoulsById();
        synchronized (souls) {
            final int size = souls.size();
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < size; i++) {
                final SoulDatabase.Soul soul = souls.get(i);
                if (soul == null || !soul.locationWorld.equals(worldUUID))
                    continue;
                out.add(soul);
            }
        }
    }

    @Override
    public void getSoulsByPlayerAndWorld(@NotNull Collection<@NotNull Soul> out, @Nullable UUID playerUUID, @NotNull UUID worldUUID) {
        out.clear();
        final SoulDatabase soulDatabase = this.soulDatabase;
        if (soulDatabase == null) {
            return;
        }
        final List<SoulDatabase.@Nullable Soul> souls = soulDatabase.getSoulsById();
        synchronized (souls) {
            final int size = souls.size();
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < size; i++) {
                final SoulDatabase.Soul soul = souls.get(i);
                if (soul == null || !soul.locationWorld.equals(worldUUID) || !Objects.equals(playerUUID, soul.owner))
                    continue;
                out.add(soul);
            }
        }
    }

    @Override
    public void getSoulsByLocation(@NotNull Collection<@NotNull Soul> out, @NotNull UUID worldUUID, int x, int z, int radius) {
        out.clear();
        final SoulDatabase soulDatabase = this.soulDatabase;
        if (soulDatabase == null) {
            return;
        }
        //noinspection unchecked
        soulDatabase.findSouls(worldUUID, x, z, radius, (Collection<SoulDatabase.Soul>) (Object) out);
    }

    @Override
    public void freeSoul(@NotNull Soul soul) {
        if (((SoulDatabase.Soul) soul).freeSoul(System.currentTimeMillis(), soulReleaseTimerMs)) {
            final SoulDatabase soulDatabase = this.soulDatabase;
            if (soulDatabase == null) {
                return;
            }
            soulDatabase.markDirty();
        }
    }

    @Override
    public void setSoulItems(@NotNull Soul soul, @NotNull ItemStack @NotNull [] items) {
        ((SoulDatabase.Soul) soul).items = items;
        final SoulDatabase soulDatabase = this.soulDatabase;
        if (soulDatabase == null) {
            return;
        }
        soulDatabase.markDirty();
    }

    @Override
    public void setSoulExperiencePoints(@NotNull Soul soul, int xp) {
        ((SoulDatabase.Soul) soul).xp = xp;
        final SoulDatabase soulDatabase = this.soulDatabase;
        if (soulDatabase == null) {
            return;
        }
        soulDatabase.markDirty();
    }

    @Override
    public void removeSoul(@NotNull Soul soul) {
        final SoulDatabase soulDatabase = this.soulDatabase;
        if (soulDatabase == null) {
            return;
        }
        soulDatabase.removeSoul((SoulDatabase.Soul) soul);
        refreshNearbySoulCache = true;
    }

    @Override
    public boolean soulExists(@NotNull Soul soul) {
        final SoulDatabase.Soul ssoul = (SoulDatabase.Soul) soul;
        if (ssoul.id < 0) {
            return false;
        }
        final SoulDatabase soulDatabase = this.soulDatabase;
        if (soulDatabase == null) {
            return false;
        }

        final ArrayList<SoulDatabase.@Nullable Soul> souls = soulDatabase.getSoulsById();
        synchronized (souls) {
            return ssoul.id < souls.size() && souls.get(ssoul.id) == soul;
        }
    }

    @Override
    public @NotNull Soul createSoul(@Nullable UUID owner, @NotNull UUID world, double x, double y, double z, @Nullable ItemStack[] contents, int xp) {
        ItemStack[] nnContents = contents == null ? noItemStack: contents;
        final SoulDatabase soulDatabase = this.soulDatabase;
        if (soulDatabase == null) {
            // Sad, but better than returning null which would probably cause crash. This situation can be tested through soulExists.
            return new SoulDatabase.Soul(owner, world, x, y, z, System.currentTimeMillis(), nnContents, xp);
        }
        refreshNearbySoulCache = true;
        return soulDatabase.addSoul(owner, world, x, y, z, nnContents, xp);
    }
    //endregion
}