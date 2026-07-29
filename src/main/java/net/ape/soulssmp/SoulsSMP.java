package net.ape.soulssmp;

import net.ape.soulssmp.managers.core.AbilityManager;
import net.ape.soulssmp.managers.SilenceManager;
import net.ape.soulssmp.commands.SoulCommand;
import net.ape.soulssmp.commands.SoulTabCompleter;
import net.ape.soulssmp.managers.HudManager;
import net.ape.soulssmp.managers.StunManager;
import net.ape.soulssmp.managers.echosoul.ResonanceManager;
import net.ape.soulssmp.abilities.echo.passive.EchoPassiveTask;
import net.ape.soulssmp.listeners.EchoCombatListener;
import net.ape.soulssmp.tasks.HudTask;
import net.ape.soulssmp.tasks.ResonanceTask;
import net.ape.soulssmp.listeners.BloodCombatListener;
import net.ape.soulssmp.listeners.CurseGUIListener;
import net.ape.soulssmp.listeners.HudJoinQuitListener;
import net.ape.soulssmp.listeners.PlayerDataListener;
import net.ape.soulssmp.tasks.RestrictionJumpTask;
import net.ape.soulssmp.listeners.RestrictionSprintListener;
import net.ape.soulssmp.listeners.UltimateTriggerListener;
import net.ape.soulssmp.listeners.VoidCombatListener;
import net.ape.soulssmp.managers.core.ManaManager;
import net.ape.soulssmp.tasks.ManaTask;
import net.ape.soulssmp.managers.player.PlayerDataManager;
import net.ape.soulssmp.managers.core.SoulManager;
import net.ape.soulssmp.abilities.blood.ultimate.BloodHands;
import net.ape.soulssmp.abilities.blood.passive.BloodPassiveTask;
import net.ape.soulssmp.abilities.blood.active.Curse;
import net.ape.soulssmp.abilities.blood.active.Hemorrhage;
import net.ape.soulssmp.tasks.HoldTask;
import net.ape.soulssmp.managers.bloodsoul.RestraintManager;
import net.ape.soulssmp.tasks.RestraintRingTask;
import net.ape.soulssmp.tasks.RestraintTask;
import net.ape.soulssmp.abilities.voidsoul.active.AbyssMark;
import net.ape.soulssmp.abilities.voidsoul.ultimate.NullField;
import net.ape.soulssmp.abilities.voidsoul.passive.VoidPassiveTask;
import net.ape.soulssmp.abilities.voidsoul.active.VoidStep;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SoulsSMP extends JavaPlugin {

    private static SoulsSMP instance;

    private PlayerDataManager playerDataManager;
    private SoulManager soulManager;
    private ManaManager manaManager;
    private AbyssMark abyssMark;
    private NullField nullField;
    private AbilityManager abilityManager;
    private HudManager hudManager;
    private StunManager stunManager;
    private ResonanceManager resonanceManager;
    private VoidStep voidStep;
    private BloodPassiveTask bloodPassiveTask;
    private SilenceManager silenceManager;
    private RestraintManager restraintManager;
    private Curse curse;
    private Hemorrhage hemorrhage;
    private BloodHands bloodHands;
    private BloodCombatListener bloodCombatListener;

    @Override
    public void onEnable() {
        instance = this;

        playerDataManager = new PlayerDataManager();
        soulManager = new SoulManager(playerDataManager);
        manaManager = new ManaManager(playerDataManager);
        abilityManager = new AbilityManager();
        hudManager = new HudManager();
        stunManager = new StunManager();
        resonanceManager = new ResonanceManager();
        bloodPassiveTask = new BloodPassiveTask();
        silenceManager = new SilenceManager();
        restraintManager = new RestraintManager();
        bloodCombatListener = new BloodCombatListener();

        voidStep = new VoidStep();
        abilityManager.registerAbility(voidStep);
        abyssMark = new AbyssMark();
        abilityManager.registerAbility(abyssMark);
        nullField = new NullField();
        abilityManager.registerAbility(nullField);
        curse = new Curse();
        abilityManager.registerAbility(curse);
        hemorrhage = new Hemorrhage();
        abilityManager.registerAbility(hemorrhage);
        bloodHands = new BloodHands();
        abilityManager.registerAbility(bloodHands);

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

        getServer().getScheduler().runTask(this, () -> bloodPassiveTask.setupSenseTeam());
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }

        if (voidStep != null) {
            voidStep.removeAllClones();
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

    public AbyssMark getAbyssMark() {
        return abyssMark;
    }

    public NullField getNullField() {
        return nullField;
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    public HudManager getHudManager() {
        return hudManager;
    }

    public StunManager getStunManager() {
        return stunManager;
    }

    public ResonanceManager getResonanceManager() {
        return resonanceManager;
    }

    public SilenceManager getSilenceManager() {
        return silenceManager;
    }

    public RestraintManager getRestraintManager() {
        return restraintManager;
    }

    public Curse getCurse() {
        return curse;
    }

    public Hemorrhage getHemorrhage() {
        return hemorrhage;
    }

    public BloodHands getBloodHands() {
        return bloodHands;
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
        getServer().getPluginManager().registerEvents(new CurseGUIListener(), this);
        getServer().getPluginManager().registerEvents(stunManager, this);
        getServer().getPluginManager().registerEvents(new EchoCombatListener(), this);
    }

    private void registerCommands() {
        getCommand("soul").setExecutor(new SoulCommand());
        getCommand("soul").setTabCompleter(new SoulTabCompleter());
    }

    private void startTasks() {
        new HudTask().runTaskTimer(this, 0L, 4L);
        new ManaTask().runTaskTimer(this, 100L, 100L);
        new VoidPassiveTask().runTaskTimer(this, 0L, 10L);
        new EchoPassiveTask().runTaskTimer(this, 0L, 5L);
        new ResonanceTask().runTaskTimer(this, 20L, 20L);
        bloodPassiveTask.runTaskTimer(this, 0L, 10L);
        new RestraintTask().runTaskTimer(this, 0L, 5L);
        new HoldTask().runTaskTimer(this, 0L, 5L);
        new RestraintRingTask().runTaskTimer(this, 0L, 4L);
        new RestrictionJumpTask().runTaskTimer(this, 0L, 1L);
    }
}