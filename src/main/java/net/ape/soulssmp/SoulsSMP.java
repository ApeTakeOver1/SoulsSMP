package net.ape.soulssmp;

import org.bukkit.plugin.java.JavaPlugin;

public class SoulsSMP extends JavaPlugin {

    private static SoulsSMP instance;

    @Override
    public void onEnable() {

        instance = this;

        getLogger().info("Souls SMP has awakened!");

    }


    @Override
    public void onDisable() {

        getLogger().info("Souls SMP has been sealed!");

    }


    public static SoulsSMP getInstance() {
        return instance;
    }

}