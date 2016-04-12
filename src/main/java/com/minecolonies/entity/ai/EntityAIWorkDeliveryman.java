package com.minecolonies.entity.ai;

import com.minecolonies.colony.CitizenData;
import com.minecolonies.colony.buildings.BuildingWorker;
import com.minecolonies.colony.jobs.JobDeliveryman;
import com.minecolonies.configuration.Configurations;
import com.minecolonies.tileentities.TileEntityColonyBuilding;
import com.minecolonies.util.ChunkCoordUtils;
import com.minecolonies.util.InventoryUtils;
import net.minecraft.item.ItemStack;

import static com.minecolonies.entity.EntityCitizen.Status.WORKING;

/**
 * Performs deliveryman work
 * Created: July 18, 2014
 *
 * @author MrIbby
 */
public class EntityAIWorkDeliveryman extends AbstractEntityAIWork<JobDeliveryman>
{
    public EntityAIWorkDeliveryman(JobDeliveryman deliveryman)
    {
        super(deliveryman);
    }

    @Override
    public boolean shouldExecute()
    {
        return super.shouldExecute() && (job.hasDestination() || job.isNeeded());
    }

    @Override
    public void startExecuting()
    {
        if(!job.hasDestination())
        {
            job.setDestination(worker.getColony().getDeliverymanRequired().get(0));
        }
        ChunkCoordUtils.tryMoveLivingToXYZ(worker, job.getDestination());
    }

    @Override
    public void updateTask()
    {
        if(!ChunkCoordUtils.isWorkerAtSiteWithMove(worker, job.getDestination()))
        {
            return;
        }

        worker.setStatus(WORKING);

        //  TODO - Actually know the Building, not the ID of it
        BuildingWorker destinationBuilding = worker.getColony().getBuilding(job.getDestination(), BuildingWorker.class);
        if(destinationBuilding == null)
        {
            return;
        }

        CitizenData targetCitizen = destinationBuilding.getWorker();
        if(targetCitizen == null || targetCitizen.getJob() == null)
        {
            return;
        }

        TileEntityColonyBuilding destinationTileEntity = destinationBuilding.getTileEntity();
        if(destinationTileEntity == null)
        {
            //  The recipient or their building's TE aren't loaded currently.  Maybe do something else?
            return;
        }

        for(int i = 0; i < targetCitizen.getJob().getItemsNeeded().size(); i++)
        {
            ItemStack itemstack = targetCitizen.getJob().getItemsNeeded().get(i);
            int amount = itemstack.stackSize;
            for(int j = 0; j < destinationTileEntity.getSizeInventory(); j++)
            {
                ItemStack hutItem = destinationTileEntity.getStackInSlot(j);
                if(hutItem != null && hutItem.isItemEqual(itemstack))
                {
                    amount -= hutItem.stackSize;
                    if(amount <= 0) break;
                }
            }
            if(amount > 0)
            {
                if(!Configurations.deliverymanInfiniteResources)
                {
                    //TODO: resource handling
                }
                InventoryUtils.setStack(destinationTileEntity, new ItemStack(itemstack.getItem(), amount, itemstack.getItemDamage()));
            }
            targetCitizen.getJob().removeItemNeeded(itemstack);
            i--;
        }

        job.setDestination(null);
        resetTask();
    }

    /**
     * {@inheritDoc}
     * This method will be overridden by AI implementations
     */
    @Override
    protected void workOnTask()
    {
        //TODO: rework the deliveryman to use workOnTask eventually
    }

    @Override
    public boolean continueExecuting()
    {
        return super.continueExecuting() && job.hasDestination();
    }
}