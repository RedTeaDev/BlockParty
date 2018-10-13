package de.leonkoth.blockparty.arena;

import de.leonkoth.blockparty.BlockParty;
import de.leonkoth.blockparty.event.PlayerEliminateEvent;
import de.leonkoth.blockparty.event.PlayerJoinArenaEvent;
import de.leonkoth.blockparty.event.PlayerLeaveArenaEvent;
import de.leonkoth.blockparty.floor.Floor;
import de.leonkoth.blockparty.floor.FloorPattern;
import de.leonkoth.blockparty.locale.LocaleString;
import de.leonkoth.blockparty.locale.Messenger;
import de.leonkoth.blockparty.phase.PhaseHandler;
import de.leonkoth.blockparty.player.PlayerInfo;
import de.leonkoth.blockparty.player.PlayerState;
import de.leonkoth.blockparty.song.SongManager;
import de.leonkoth.blockparty.particle.ParticlePlayer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Leon on 14.03.2018.
 * Project Blockparty2
 * © 2016 - Leon Koth
 */
public class Arena {

    public static final double TIME_REDUCTION_PER_LEVEL = 0.5;
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 15;
    public static final int LOBBY_COUNTDOWN = 30;
    public static final int DISTANCE_TO_OUT_AREA = 5;
    public static final int TIME_TO_SEARCH = 8;
    public static final int AMOUNT_OF_LEVELS = 15;
    public static final int BOOST_DURATION = 10;

    private BlockParty blockParty;

    @Setter
    @Getter
    private int distanceToOutArea, timeToSearch, levelAmount, boostDuration, minPlayers, maxPlayers, lobbyCountdown;

    @Setter
    @Getter
    private double timeReductionPerLevel;

    @Setter
    @Getter
    private boolean enabled, enableParticles, enableLightnings, autoRestart, autoKick, enableBoosts, enableFallingBlocks, useAutoGeneratedFloors, usePatternFloors,
            enableActionbarInfo, useNoteblockSongs, useWebSongs, enableFireworksOnWin, timerResetOnPlayerJoin, allowJoinDuringGame;

    @Setter
    @Getter
    private String name;

    @Getter
    private Location lobbySpawn, gameSpawn;

    @Setter
    @Getter
    private SongManager songManager;

    @Setter
    @Getter
    private Floor floor;

    @Setter
    @Getter
    private ArenaDataManager arenaDataManager;

    @Setter
    @Getter
    private ArenaState arenaState;

    @Setter
    @Getter
    private GameState gameState;

    @Setter
    @Getter
    private List<PlayerInfo> playersInArena;

    @Setter
    @Getter
    private PhaseHandler phaseHandler;

    @Setter
    @Getter
    private ParticlePlayer particlePlayer;

    public Arena(String name, BlockParty blockParty, boolean save) {
        this.name = name;
        this.blockParty = blockParty;
        this.arenaState = ArenaState.LOBBY;
        this.gameState = GameState.WAIT;
        this.minPlayers = MIN_PLAYERS;
        this.maxPlayers = MAX_PLAYERS;
        this.lobbyCountdown = LOBBY_COUNTDOWN;
        this.distanceToOutArea = DISTANCE_TO_OUT_AREA;
        this.timeToSearch = TIME_TO_SEARCH;
        this.timeReductionPerLevel = TIME_REDUCTION_PER_LEVEL;
        this.levelAmount = AMOUNT_OF_LEVELS;
        this.boostDuration = BOOST_DURATION;
        this.enabled = false;
        this.enableLightnings = true;
        this.enableParticles = true;
        this.autoRestart = true;
        this.enableBoosts = true;
        this.allowJoinDuringGame = true;
        this.enableFireworksOnWin = true;
        this.useAutoGeneratedFloors = true;
        this.usePatternFloors = true;
        this.enableActionbarInfo = true;
        this.useNoteblockSongs = false;
        this.useWebSongs = true;
        this.timerResetOnPlayerJoin = false;
        this.enableFallingBlocks = false;
        this.autoKick = false;
        this.songManager = new SongManager(this, new ArrayList<>());
        this.arenaDataManager = new ArenaDataManager(this);
        this.phaseHandler = new PhaseHandler(blockParty, this);
        this.particlePlayer = new ParticlePlayer("CLOUD");
        this.playersInArena = new ArrayList<>();

        if (save)
            this.saveData();
    }

    public static boolean create(String name) {
        if (isLoaded(name)) {
            return false;
        }

        BlockParty blockParty = BlockParty.getInstance();
        Arena arena = new Arena(name, blockParty, true);
        blockParty.getArenas().add(arena);

        return true;
    }

    public static Arena getArenaData(String name) {

        BlockParty blockParty = BlockParty.getInstance();
        Arena arena = new Arena(name, blockParty, false);

        if (Arena.isLoaded(name)) {
            arena = Arena.getByName(name);
        }

        if (arena != null) {

            if (arena.getArenaDataManager().getConfig().isConfigurationSection("Floor")) {
                arena.getArenaDataManager().loadData();
                Floor floor = Floor.getFromConfig(arena);
                arena.setFloor(floor);
            }

            if (arena.getArenaDataManager().getConfig().isConfigurationSection("Spawns.Lobby")) {
                Location lobbySpawn = arena.getArenaDataManager().getLocation("Spawns.Lobby");
                arena.setLobbySpawn(lobbySpawn);
            }

            if (arena.getArenaDataManager().getConfig().isConfigurationSection("Spawns.Game")) {
                Location gameSpawn = arena.getArenaDataManager().getLocation("Spawns.Game");
                arena.setGameSpawn(gameSpawn);
            }

        }

        return arena;
    }

    public static Arena getByName(String name) {
        for (Arena arena : BlockParty.getInstance().getArenas()) {
            if (arena.getName().equals(name)) {
                return arena;
            }
        }

        return null;
    }

    public static boolean isLoaded(String name) {

        if (BlockParty.getInstance().getArenas() == null)
            return false;

        for (Arena arena : BlockParty.getInstance().getArenas()) {
            if (arena.getName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    public static void saveAll() {
        for (Arena arena : BlockParty.getInstance().getArenas()) {
            arena.save();
        }
    }

    public void save() {

        arenaDataManager.saveLocation("Spawns.Lobby", lobbySpawn, false);
        arenaDataManager.saveLocation("Spawns.Game", gameSpawn, false);
        arenaDataManager.saveLocation("Floors.A", floor.getBounds().getA(), false);
        arenaDataManager.saveLocation("Floors.B", floor.getBounds().getB(), false);
        saveData(false);

        try {
            arenaDataManager.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveData() {
        this.saveData(true);
    }

    public void saveData(boolean save) {
        ArenaDataManager.ArenaDataSet dataSet = new ArenaDataManager.ArenaDataSet(distanceToOutArea, timeToSearch,
                levelAmount, boostDuration, minPlayers, maxPlayers, lobbyCountdown, timeReductionPerLevel, enabled, enableParticles, enableLightnings, autoRestart,
                autoKick, enableBoosts, enableFallingBlocks, useAutoGeneratedFloors, usePatternFloors, enableActionbarInfo,
                useNoteblockSongs, useWebSongs, enableFireworksOnWin, timerResetOnPlayerJoin, allowJoinDuringGame, name,
                songManager, floor);

        arenaDataManager.save(dataSet, save);
    }

    public boolean addPlayer(Player player) {

        PlayerInfo playerInfo = PlayerInfo.getFromPlayer(player);

        if (playerInfo == null) {
            blockParty.getPlayers().add(new PlayerInfo(player.getName(), player.getUniqueId(), 0, 0));
            playerInfo = PlayerInfo.getFromPlayer(player);
        }

        PlayerJoinArenaEvent event = new PlayerJoinArenaEvent(this, player, playerInfo);
        Bukkit.getPluginManager().callEvent(event);

        return !event.isCancelled();
    }

    public boolean removePlayer(Player player) {

        PlayerInfo playerInfo = PlayerInfo.getFromPlayer(player);
        PlayerLeaveArenaEvent event = new PlayerLeaveArenaEvent(this, player, playerInfo);
        Bukkit.getPluginManager().callEvent(event);

        return event.isCancelled();
    }

    public boolean removePattern(String name) {

        if (floor.getPatternNames().contains(name)) {
            floor.getPatternNames().remove(name);

            Iterator<FloorPattern> iterator = floor.getFloorPatterns().iterator();
            while (iterator.hasNext()) {
                FloorPattern pattern = iterator.next();

                if (pattern.getName().equals(name)) {
                    iterator.remove();
                }
            }

            arenaDataManager.getConfig().set("configuration.Floor.EnabledFloors", floor.getPatternNames());
            try {
                arenaDataManager.save();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }

        return false;
    }

    public boolean addPattern(FloorPattern pattern) {

        floor.getPatternNames().add(pattern.getName());
        floor.getFloorPatterns().add(pattern);

        arenaDataManager.getConfig().set("Configuration.Floor.EnabledFloors", floor.getPatternNames());
        try {
            arenaDataManager.save();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void setLobbySpawn(Location lobbySpawn) {
        this.lobbySpawn = lobbySpawn;
        arenaDataManager.saveLocation("Spawns.Lobby", lobbySpawn);
    }

    public void setGameSpawn(Location gameSpawn) {
        this.gameSpawn = gameSpawn;
        arenaDataManager.saveLocation("Spawns.Game", gameSpawn);
    }

    public void broadcast(boolean usePrefix, LocaleString message, boolean onlyIngame, PlayerInfo except, String... placeholders) {
        broadcast(usePrefix, message, onlyIngame, new PlayerInfo[]{except}, placeholders);
    }

    public void broadcast(boolean usePrefix, LocaleString message, boolean onlyIngame, PlayerInfo[] exceptions, String... placeholders) {

        playerLoop:
        for (PlayerInfo playerInfo : playersInArena) {

            for (PlayerInfo exeption : exceptions) {
                if (playerInfo.equals(exeption)) {
                    continue playerLoop;
                }
            }

            if (onlyIngame && playerInfo.getPlayerState() != PlayerState.INGAME) {
                continue;
            }

            Messenger.message(usePrefix, playerInfo.asPlayer(), message, placeholders);
        }
    }

    public void kickAllPlayers() {
        Iterator iterator = playersInArena.iterator();

        while (iterator.hasNext()) {
            PlayerInfo info = (PlayerInfo) iterator.next();
            removePlayer(info.asPlayer());
        }
    }

    public void delete() {
        for (PlayerInfo playerInfo : playersInArena) {
            removePlayer(playerInfo.asPlayer());
        }

        blockParty.getArenas().remove(this);
        arenaDataManager.delete();
    }

    public void eliminate(PlayerInfo playerInfo) {
        PlayerEliminateEvent event = new PlayerEliminateEvent(this, playerInfo.asPlayer(), playerInfo);
        Bukkit.getPluginManager().callEvent(event);
    }

    public int getIngamePlayers() {
        int ingamePlayers = 0;
        for (PlayerInfo info : playersInArena) {
            if (info.getPlayerState() == PlayerState.INGAME || info.getPlayerState() == PlayerState.WINNER)
                ingamePlayers++;
        }

        return ingamePlayers;
    }

    @Override
    public String toString() {
        return name;
    }

}
