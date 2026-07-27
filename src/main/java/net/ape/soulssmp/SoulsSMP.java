package net.ape.soulssmp;

import net.ape.soulssmp.ability.AbilityManager;
import net.ape.soulssmp.ability.SilenceManager;
import net.ape.soulssmp.command.SoulCommand;
import net.ape.soulssmp.command.SoulTabCompleter;
import net.ape.soulssmp.command.SoulTestCommand;
import net.ape.soulssmp.command.SoulTestTabCompleter;
import net.ape.soulssmp.hud.HudManager;
import net.ape.soulssmp.hud.HudTask;
import net.ape.soulssmp.listener.BloodCombatListener;
import net.ape.soulssmp.listener.HudJoinQuitListener;
import net.ape.soulssmp.listener.PlayerDataListener;
import net.ape.soulssmp.listener.RestrictionSprintListener;
import net.ape.soulssmp.listener.UltimateTriggerListener;
import net.ape.soulssmp.listener.VoidCombatListener;
import net.ape.soulssmp.mana.ManaManager;
import net.ape.soulssmp.mana.ManaTask;
import net.ape.soulssmp.player.PlayerDataManager;
import net.ape.soulssmp.soul.SoulManager;
import net.ape.soulssmp.soul.types.bloodsoul.BloodHands;
import net.ape.soulssmp.soul.types.bloodsoul.BloodPassiveTask;
import net.ape.soulssmp.soul.types.bloodsoul.BloodSenseManager;
import net.ape.soulssmp.soul.types.bloodsoul.Curse;
import net.ape.soulssmp.soul.types.bloodsoul.Hemorrhage;
import net.ape.soulssmp.soul.types.bloodsoul.HemorrhageManager;
import net.ape.soulssmp.soul.types.bloodsoul.HoldManager;
import net.ape.soulssmp.soul.types.bloodsoul.HoldTask;
import net.ape.soulssmp.soul.types.bloodsoul.RestraintManager;
import net.ape.soulssmp.soul.types.bloodsoul.RestraintTask;
import net.ape.soulssmp.soul.types.bloodsoul.RestrictionManager;
import net.ape.soulssmp.soul.types.voidsoul.AbyssMark;
import net.ape.soulssmp.soul.types.voidsoul.NullField;
import net.ape.soulssmp.soul.types.voidsoul.NullFieldManager;
import net.ape.soulssmp.soul.types.voidsoul.ShadowCloneManager;
import net.ape.soulssmp.soul.types.voidsoul.VoidMarkManager;
import net.ape.soulssmp.soul.types.voidsoul.VoidPassiveTask;
import net.ape.soulssmp.soul.types.voidsoul.VoidStep;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SoulsSMP extends JavaPlugin {

    private static SoulsSMP instance;

    private PlayerDataManager playerDataManager;
    private SoulManager soulManager;
    private ManaManager manaManager;
    private VoidMarkManager voidMarkManager;
    private NullFieldManager nullFieldManager;
    private AbilityManager abilityManager;
    private HudManager hudManager;
    private ShadowCloneManager shadowCloneManager;
    private BloodSenseManager bloodSenseManager;
    private SilenceManager silenceManager;
    private RestraintManager restraintManager;
    private RestrictionManager restrictionManager;
    private HemorrhageManager hemorrhageManager;
    private HoldManager holdManager;
    private BloodCombatListener bloodCombatListener;

    @Override
    public void onEnable() {
        instance = this;

        playerDataManager = new PlayerDataManager();
        soulManager = new SoulManager(playerDataManager);
        manaManager = new ManaManager(playerDataManager);
        voidMarkManager = new VoidMarkManager();
        nullFieldManager = new NullFieldManager();
        abilityManager = new AbilityManager();
        hudManager = new HudManager();
        shadowCloneManager = new ShadowCloneManager();
        bloodSenseManager = new BloodSenseManager();
        silenceManager = new SilenceManager();
        restraintManager = new RestraintManager();
        restrictionManager = new RestrictionManager();
        hemorrhageManager = new HemorrhageManager();
        holdManager = new HoldManager();
        bloodCombatListener = new BloodCombatListener();

        abilityManager.registerAbility(new VoidStep());
        abilityManager.registerAbility(new AbyssMark());
        abilityManager.registerAbility(new NullField());
        abilityManager.registerAbility(new Curse());
        abilityManager.registerAbility(new Hemorrhage());
        abilityManager.registerAbility(new BloodHands());

        getLogger().info("=================================");
        getLogger().info("Souls SMP has awakened!");
        getLogger().info("Plugin enabled successfully.");
        getLogger().info("=================================");

        registerListeners();
        registerCommands();
        startTasks();

        for (Player player : getServer().getOnlinePlayers()) {
            playerDataManager.loadPlayerData(player);
            hudManager.createBars(player);
        }

        getServer().getScheduler().runTask(this, () -> bloodSenseManager.setupTeam());
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }

        if (shadowCloneManager != null) {
            shadowCloneManager.removeAll();
        }

        getLogger().info("=================================");
        getLogger().info("Souls SMP has been sealed.");
        getLogger().info("Plugin disabled.");
        getLogger().info("=================================");
    }

    public static SoulsSMP getInstance() {
        return instance;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public SoulManager getSoulManager() {
        return soulManager;
    }

    public ManaManager getManaManager() {
        return manaManager;
    }

    public VoidMarkManager getVoidMarkManager() {
        return voidMarkManager;
    }

    public NullFieldManager getNullFieldManager() {
        return nullFieldManager;
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    public HudManager getHudManager() {
        return hudManager;
    }

    public ShadowCloneManager getShadowCloneManager() {
        return shadowCloneManager;
    }

    public BloodSenseManager getBloodSenseManager() {
        return bloodSenseManager;
    }

    public SilenceManager getSilenceManager() {
        return silenceManager;
    }

    public RestraintManager getRestraintManager() {
        return restraintManager;
    }

    public RestrictionManager getRestrictionManager() {
        return restrictionManager;
    }

    public HemorrhageManager getHemorrhageManager() {
        return hemorrhageManager;
    }

    public HoldManager getHoldManager() {
        return holdManager;
    }

    public BloodCombatListener getBloodCombatListener() {
        return bloodCombatListener;
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new VoidCombatListener(), this);
        getServer().getPluginManager().registerEvents(new UltimateTriggerListener(), this);
        getServer().getPluginManager().registerEvents(new HudJoinQuitListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerDataListener(), this);
        getServer().getPluginManager().registerEvents(bloodCombatListener, this);
        getServer().getPluginManager().registerEvents(new RestrictionSprintListener(), this);
    }

    private void registerCommands() {
        getCommand("soul").setExecutor(new SoulCommand());
        getCommand("soul").setTabCompleter(new SoulTabCompleter());
        getCommand("soultest").setExecutor(new SoulTestCommand());
        getCommand("soultest").setTabCompleter(new SoulTestTabCompleter());
    }

    private void startTasks() {
        new HudTask().runTaskTimer(this, 0L, 4L);
        new ManaTask().runTaskTimer(this, 100L, 100L);
        new VoidPassiveTask().runTaskTimer(this, 0L, 10L);
        new BloodPassiveTask().runTaskTimer(this, 0L, 10L);
        new RestraintTask().runTaskTimer(this, 0L, 5L);
        new HoldTask().runTaskTimer(this, 0L, 5L);
    }
}