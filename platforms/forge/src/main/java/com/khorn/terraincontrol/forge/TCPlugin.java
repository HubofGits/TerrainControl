package com.khorn.terraincontrol.forge;

import com.khorn.terraincontrol.LocalWorld;
import com.khorn.terraincontrol.TerrainControl;
import com.khorn.terraincontrol.TerrainControlEngine;
import com.khorn.terraincontrol.configuration.standard.PluginStandardValues;
import com.khorn.terraincontrol.events.EventPriority;
import com.khorn.terraincontrol.forge.events.EventManager;
import com.khorn.terraincontrol.forge.events.PacketHandler;
import com.khorn.terraincontrol.forge.events.PlayerTracker;
import com.khorn.terraincontrol.forge.events.SaplingListener;
import com.khorn.terraincontrol.forge.generator.structure.RareBuildingStart;
import com.khorn.terraincontrol.forge.generator.structure.VillageStart;
import com.khorn.terraincontrol.util.minecraftTypes.StructureNames;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;

import java.io.File;
import java.lang.reflect.Field;
import java.util.logging.Level;

import net.minecraft.block.Block;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraftforge.common.MinecraftForge;

@Mod(modid = "TerrainControl", name = "TerrainControl")
@NetworkMod(clientSideRequired = false, serverSideRequired = false, versionBounds = "*")
public class TCPlugin implements TerrainControlEngine
{
    
    @Instance("TerrainControl")
    public static TCPlugin instance;

    public File terrainControlDirectory;
    private TCWorldType worldType;
    
    @EventHandler
    public void load(FMLInitializationEvent event)
    {
        // This is the place where the mod starts loading
        
        // Set the directory
        TerrainControl.setEngine(this);
        TerrainControl.setupLogging(FMLCommonHandler.instance().getFMLLogger());

        // Start TerrainControl engine
        TerrainControl.supportedBlockIds = 4095;
        TerrainControl.startEngine();

        // Register localization
        LanguageRegistry.instance().addStringLocalization("generator.TerrainControl", "TerrainControl");

        // Register world type
        worldType = new TCWorldType(this, "TerrainControl");
        
        // Register village and rare building starts
        MapGenStructureIO.func_143034_b(RareBuildingStart.class, StructureNames.RARE_BUILDING);
        MapGenStructureIO.func_143034_b(VillageStart.class, StructureNames.VILLAGE);

        // Register listening channel for listening to received configs.
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
        {
            NetworkRegistry.instance().registerChannel(new PacketHandler(), PluginStandardValues.ChannelName.stringValue());
        }

        // Register player tracker, for sending configs.
        GameRegistry.registerPlayerTracker(new PlayerTracker(this));

        // Register sapling tracker, for custom tree growth.
        SaplingListener saplingListener = new SaplingListener();
        MinecraftForge.TERRAIN_GEN_BUS.register(saplingListener);
        MinecraftForge.EVENT_BUS.register(saplingListener);

        // Register to our own events, so that they can be fired again as Forge
        // events.
        // TODO: make this optional for people who haven't installed other
        // terrain mods, and don't want to lose performance.
        TerrainControl.registerEventHandler(new EventManager(), EventPriority.CANCELABLE);
    }

    @Override
    public LocalWorld getWorld(String name)
    {
        LocalWorld world = worldType.worldTC;
        if (world == null)
        {
            return null;
        }
        if (world.getName().equals(name))
        {
            return world;
        }
        return null;
    }

    /**
     * Gets the world loaded by Terrain Control.
     * <p />
     * Note: this method may be removed in the future, when multiworld
     * support is introduced.
     * <p/>
     * @return The world loaded by Terrain Control, or null if no world is
     *         loaded.
     */
    public LocalWorld getWorld()
    {
        return worldType.worldTC;
    }

    @Override
    public File getTCDataFolder()
    {
        File dataFolder;
        try
        {
            Field minecraftDir = Loader.class.getDeclaredField("minecraftDir");
            minecraftDir.setAccessible(true);
            dataFolder = new File((File) minecraftDir.get(null), "mods" + File.separator + "TerrainControl");
        } catch (Throwable e)
        {
            dataFolder = new File("mods" + File.separator + "TerrainControl");
            System.out.println("Could not reflect the Minecraft directory, save location may be unpredicatble.");
            TerrainControl.printStackTrace(Level.SEVERE, e);
        }
        return dataFolder;
    }

    @Override
    public File getGlobalObjectsDirectory()
    {
        return new File(this.getTCDataFolder(), PluginStandardValues.BO_DirectoryName.stringValue());
    }

    @Override
    public boolean isValidBlockId(int id)
    {
        if (id == 0)
        {
            // Air is a special case
            return true;
        }
        if (id < 0 || id > TerrainControl.supportedBlockIds)
        {
            return false;
        }
        if (Block.blocksList[id] == null)
        {
            return false;
        }
        return true;
    }
    
}
