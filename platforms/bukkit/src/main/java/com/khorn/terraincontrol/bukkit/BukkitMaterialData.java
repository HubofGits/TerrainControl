package com.khorn.terraincontrol.bukkit;

import com.khorn.terraincontrol.LocalMaterialData;
import com.khorn.terraincontrol.TerrainControl;
import com.khorn.terraincontrol.util.helpers.BlockHelper;
import com.khorn.terraincontrol.util.minecraftTypes.DefaultMaterial;
import net.minecraft.server.v1_8_R2.Block;
import net.minecraft.server.v1_8_R2.BlockFalling;
import net.minecraft.server.v1_8_R2.Blocks;
import net.minecraft.server.v1_8_R2.IBlockData;

/**
 * Implementation of LocalMaterial that wraps one of Minecraft's Blocks.
 * 
 */
public class BukkitMaterialData implements LocalMaterialData
{

    /**
     * Gets a {@code BukkitMaterialData} of the given id and data.
     * @param id   The block id.
     * @param data The block data.
     * @return The {@code BukkitMateialData} instance.
     */
    public static BukkitMaterialData ofIds(int id, int data)
    {
        Block block = Block.getById(id);
        IBlockData blockData = block.fromLegacyData(data);
        return ofMinecraftBlockData(blockData);
    }

    /**
     * Gets a {@code BukkitMaterialData} of the given material and data.
     * @param material The material.
     * @param data     The block data.
     * @return The {@code BukkitMateialData} instance.
     */
    public static BukkitMaterialData ofDefaultMaterial(DefaultMaterial material, int data)
    {
        return ofIds(material.id, data);
    }

    /**
     * Gets a {@code BukkitMaterialData} of the given Minecraft block. The
     * default block data (usually 0) will be used.
     * @param block The material.
     * @return The {@code BukkitMateialData} instance.
     */
    public static BukkitMaterialData ofMinecraftBlock(Block block)
    {
        return ofMinecraftBlockData(block.getBlockData());
    }

    /**
     * Gets a {@code BukkitMaterialData} of the given Minecraft blockData.
     * @param blockData The material an data.
     * @return The {@code BukkitMateialData} instance.
     */
    public static BukkitMaterialData ofMinecraftBlockData(IBlockData blockData)
    {
        return new BukkitMaterialData(blockData);
    }

    private final IBlockData blockData;

    private BukkitMaterialData(IBlockData blockData)
    {
        this.blockData = blockData;
    }

    @Override
    public boolean canSnowFallOn()
    {
        return toDefaultMaterial().canSnowFallOn();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof BukkitMaterialData))
        {
            return false;
        }
        BukkitMaterialData other = (BukkitMaterialData) obj;
        if (!blockData.equals(other.blockData))
        {
            return false;
        }
        return true;
    }

    @Override
    public byte getBlockData()
    {
        return (byte) blockData.getBlock().toLegacyData(blockData);
    }

    @Override
    public int getBlockId()
    {
        return Block.getId(blockData.getBlock());
    }

    @Override
    public String getName()
    {
        Block block = blockData.getBlock();
        DefaultMaterial defaultMaterial = toDefaultMaterial();

        byte data = getBlockData();
        boolean nonDefaultData = !block.getBlockData().equals(this.blockData);
        // Note that the above line is not equivalent to data != 0, as for
        // example pumpkins have a default data value of 2

        if (defaultMaterial == DefaultMaterial.UNKNOWN_BLOCK)
        {
            // Use Minecraft's name
            if (nonDefaultData)
            {
                return Block.REGISTRY.c(blockData.getBlock()) + ":" + data;
            }
            return Block.REGISTRY.c(blockData.getBlock()).toString();
        } else
        {
            // Use our name
            if (nonDefaultData)
            {
                return defaultMaterial.name() + ":" + getBlockData();
            }
            return defaultMaterial.name();
        }
    }

    @Override
    public int hashCode()
    {
        // From 4096 to 69632 when there are 4096 block ids
        return TerrainControl.SUPPORTED_BLOCK_IDS + getBlockId() * 16 + getBlockData();
    }

    @Override
    public int hashCodeWithoutBlockData()
    {
        // From 0 to 4095 when there are 4096 block ids
        return getBlockId();
    }

    @Override
    public boolean isLiquid()
    {
        return blockData.getBlock().getMaterial().isLiquid();
    }

    @Override
    public boolean isMaterial(DefaultMaterial material)
    {
        return material.id == getBlockId();
    }

    @Override
    public boolean isSolid()
    {
        // Let us override whether materials are solid
        DefaultMaterial defaultMaterial = toDefaultMaterial();
        if (defaultMaterial != DefaultMaterial.UNKNOWN_BLOCK)
        {
            return defaultMaterial.isSolid();
        }

        return blockData.getBlock().getMaterial().isSolid();
    }

    @Override
    public DefaultMaterial toDefaultMaterial()
    {
        return DefaultMaterial.getMaterial(getBlockId());
    }

    @Override
    public String toString()
    {
        return getName();
    }

    @Override
    public LocalMaterialData withBlockData(int i)
    {
        if (i == getBlockData())
        {
            return this;
        }

        Block block = this.blockData.getBlock();
        return ofMinecraftBlockData(block.fromLegacyData(i));
    }

    public IBlockData internalBlock()
    {
        return blockData;
    }

    @Override
    public LocalMaterialData rotate()
    {
        // Try to rotate
        DefaultMaterial defaultMaterial = toDefaultMaterial();
        if (defaultMaterial != DefaultMaterial.UNKNOWN_BLOCK)
        {
            // We only know how to rotate vanilla blocks
            byte blockDataByte = getBlockData();
            int newData = BlockHelper.rotateData(defaultMaterial, blockDataByte);
            if (newData != blockDataByte)
            {
                return ofMinecraftBlockData(blockData.getBlock().fromLegacyData(newData));
            }
        }

        // No changes, return object itself
        return this;
    }

    @Override
    public boolean isAir() {
        return blockData.getBlock() == Blocks.AIR;
    }

    @Override
    public boolean canFall()
    {
        return blockData.getBlock() instanceof BlockFalling;
    }

}
