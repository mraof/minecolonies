package com.minecolonies.coremod.colony.buildings;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.minecolonies.api.colony.requestsystem.requestable.crafting.PublicCrafting;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.constant.TypeConstants;
import com.minecolonies.coremod.colony.CitizenData;
import com.minecolonies.coremod.colony.Colony;
import com.minecolonies.coremod.colony.ColonyView;
import com.minecolonies.coremod.colony.jobs.AbstractJobCrafter;
import com.minecolonies.coremod.colony.requestsystem.management.IStandardRequestManager;
import com.minecolonies.coremod.colony.requestsystem.management.handlers.RequestHandler;
import com.minecolonies.coremod.colony.requestsystem.requests.StandardRequests;
import com.minecolonies.coremod.colony.requestsystem.resolvers.PublicWorkerCraftingProductionResolver;
import com.minecolonies.coremod.colony.requestsystem.resolvers.PublicWorkerCraftingRequestResolver;
import com.minecolonies.coremod.colony.requestsystem.resolvers.core.AbstractCraftingProductionResolver;
import com.minecolonies.coremod.colony.requestsystem.resolvers.core.AbstractCraftingRequestResolver;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.minecolonies.api.util.constant.BuildingConstants.CONST_DEFAULT_MAX_BUILDING_LEVEL;

/**
 * Class of the crafter building.
 */
public abstract class AbstractBuildingCrafter extends AbstractBuildingWorker
{
    /**
     * Extra amount of recipes the crafters can learn.
     */
    private static final int EXTRA_RECIPE_MULTIPLIER = 10;

    /**
     * Instantiates a new crafter building.
     *
     * @param c the colony.
     * @param l the location
     */
    public AbstractBuildingCrafter(final Colony c, final BlockPos l)
    {
        super(c, l);
    }

    @Override
    public int getMaxBuildingLevel()
    {
        return CONST_DEFAULT_MAX_BUILDING_LEVEL;
    }

    @Override
    public ImmutableCollection<IRequestResolver<?>> createResolvers()
    {
        final ImmutableCollection<IRequestResolver<?>> supers = super.createResolvers();
        final ImmutableList.Builder<IRequestResolver<?>> builder = ImmutableList.builder();

        builder.addAll(supers);
        builder.add(new PublicWorkerCraftingRequestResolver(getRequester().getRequesterLocation(),
          getColony().getRequestManager().getFactoryController().getNewInstance(TypeConstants.ITOKEN)));
        builder.add(new PublicWorkerCraftingProductionResolver(getRequester().getRequesterLocation(),
          getColony().getRequestManager().getFactoryController().getNewInstance(TypeConstants.ITOKEN)));

        return builder.build();
    }

    @Override
    public Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> getRequiredItemsAndAmount()
    {
        final Map<ItemStorage, Tuple<Integer, Boolean>> recipeOutputs = new HashMap<>();
        for (final CitizenData citizen : getAssignedCitizen())
        {
            if (citizen.getJob() instanceof AbstractJobCrafter)
            {
                for (final IToken<?> taskToken : ((AbstractJobCrafter) citizen.getJob()).getAssignedTasksFromDataStore())
                {
                    final IRequest<? extends PublicCrafting> request = (IRequest<? extends PublicCrafting>) colony.getRequestManager().getRequestForToken(taskToken);
                    final IRecipeStorage recipeStorage = getFirstRecipe(request.getRequest().getStack());
                    if (recipeStorage != null)
                    {
                        for (final ItemStorage itemStorage : recipeStorage.getCleanedInput())
                        {
                            if (recipeOutputs.containsKey(itemStorage))
                            {
                                itemStorage.setAmount(recipeOutputs.get(itemStorage).getFirst() + itemStorage.getAmount());
                            }
                            recipeOutputs.put(itemStorage, new Tuple<>(itemStorage.getAmount(), true));
                        }
                    }
                }
            }
        }

        final Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> toKeep = new HashMap<>(keepX);
        toKeep.putAll(recipeOutputs.entrySet().stream().collect(Collectors.toMap(key -> (stack -> stack.isItemEqual(key.getKey().getItemStack())), Map.Entry::getValue)));
        return toKeep;
    }

    @Override
    public boolean canCraftComplexRecipes()
    {
        return true;
    }

    @Override
    public boolean canRecipeBeAdded(final IToken token)
    {
        return AbstractBuildingCrafter.canBuildingCanLearnMoreRecipes (getBuildingLevel(), super.getRecipes().size());
    }

    /**
     * Crafter building View.
     */
    public static class View extends AbstractBuildingWorker.View
    {
        /**
         * Instantiate the crafter view.
         *
         * @param c the colonyview to put it in
         * @param l the positon
         */
        public View(final ColonyView c, final BlockPos l)
        {
            super(c, l);
        }

        /**
         * Check if an additional recipe can be added.
         * @return true if so.
         */
        public boolean canRecipeBeAdded()
        {
            return AbstractBuildingCrafter.canBuildingCanLearnMoreRecipes (getBuildingLevel(), super.getRecipes().size());
        }
    }

    /**
     * Check if an additional recipe can be added.
     * @param learnedRecipes the learned recipes.
     * @param buildingLevel the building level.
     * @return true if so.
     */
    public static boolean canBuildingCanLearnMoreRecipes(final int buildingLevel, final int learnedRecipes)
    {
        return (Math.pow(2, buildingLevel) * EXTRA_RECIPE_MULTIPLIER) >= (learnedRecipes + 1);
    }
}
