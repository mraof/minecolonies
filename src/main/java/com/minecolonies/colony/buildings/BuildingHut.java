package com.minecolonies.colony.buildings;

import com.minecolonies.colony.Colony;
import com.minecolonies.colony.ColonyView;
import net.minecraft.util.ChunkCoordinates;

public abstract class BuildingHut extends Building
{
    public BuildingHut(Colony c, ChunkCoordinates l)
    {
        super(c, l);
    }

    /**
     * Returns the max amount of inhabitants
     *
     * @return  Max inhabitants
     */
    public int getMaxInhabitants()
    {
        return 1;
    }

    public static class View extends Building.View
    {
        protected View(ColonyView c, ChunkCoordinates l)
        {
            super(c, l);
        }
    }
}