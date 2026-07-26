package net.ape.soulssmp;

import net.ape.soulssmp.ability.AbilityManager;
import net.ape.soulssmp.command.SoulCommand;
import net.ape.soulssmp.command.SoulTestCommand;
import net.ape.soulssmp.hud.HudManager;
import net.ape.soulssmp.hud.HudTask;
import net.ape.soulssmp.listener.HudJoinQuitListener;
import net.ape.soulssmp.listener.NullFieldTriggerListener;
import net.ape.soulssmp.listener.VoidCombatListener;
import net.ape.soulssmp.mana.ManaManager;
import net.ape.soulssmp.mana.ManaTask;
import net.ape.soulssmp.player.PlayerDataManager;
import net.ape.soulssmp.soul.SoulManager;
import net.ape.soulssmp.soul.types.voidsoul.AbyssMark;
import net.ape.soulssmp.soul.types.voidsoul.NullField;
import net.ape.soulssmp.soul.types.voidsoul.NullFieldManager;
import net.ape.soulssmp.soul.types.voidsoul.ShadowCloneManager;
import net.ape.soulssmp.soul.types.voidsoul.VoidMarkManager;
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

        abilityManager.registerAbility(new VoidStep());
        abilityManager.registerAbility(new AbyssMark());
        abilityManager.registerAbility(new NullField());

        getLogger().info("=================================");
        getLogger().info("Souls SMP has awakened!");
        getLogger().info("Plugin enabled successfully.");
        getLogger().info("=================================");

        registerListeners();
        registerCommands();
        startTasks();

        for (Player player : getServer().getOnlinePlayers()) {
            hudManager.createBars(player);
        }
    }

    @Override
    public void onDisable() {
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

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new VoidCombatListener(), this);
        getServer().getPluginManager().registerEvents(new NullFieldTriggerListener(), this);
        getServer().getPluginManager().registerEvents(new HudJoinQuitListener(), this);
    }

    private void registerCommands() {
        getCommand("soul").setExecutor(new SoulCommand());
        getCommand("soultest").setExecutor(new SoulTestCommand());
    }

    private void startTasks() {
        new HudTask().runTaskTimer(this, 0L, 4L);
        new ManaTask().runTaskTimer(this, 100L, 100L);
    }
}