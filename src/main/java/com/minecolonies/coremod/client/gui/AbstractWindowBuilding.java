package com.minecolonies.coremod.client.gui;

import com.minecolonies.api.util.LanguageHandler;
import com.minecolonies.blockout.controls.Button;
import com.minecolonies.blockout.controls.Label;
import com.minecolonies.blockout.views.SwitchView;
import com.minecolonies.coremod.MineColonies;
import com.minecolonies.coremod.colony.buildings.AbstractBuildingWorker;
import com.minecolonies.coremod.colony.buildings.views.AbstractBuildingView;
import com.minecolonies.coremod.network.messages.BuildRequestMessage;
import com.minecolonies.coremod.network.messages.OpenInventoryMessage;
import org.jetbrains.annotations.NotNull;

import static com.minecolonies.api.util.constant.WindowConstants.*;

/**
 * Manage windows associated with Buildings.
 *
 * @param <B> Class extending {@link AbstractBuildingView}.
 */
public abstract class AbstractWindowBuilding<B extends AbstractBuildingView> extends AbstractWindowSkeleton
{
    /**
     * Type B is a class that extends {@link AbstractBuildingWorker.View}.
     */
    protected final B          building;
    private final   Label      title;
    private final   Button     buttonBuild;
    private final   Button     buttonRepair;
    protected       SwitchView switchView;
    protected       int        switchPagesSize;
    protected final Label      pageNum;
    protected final Button     buttonPrevPage;
    protected final Button     buttonNextPage;

    /**
     * Constructor for the windows that are associated with buildings.
     *
     * @param building Class extending {@link AbstractBuildingView}.
     * @param resource Resource location string.
     */
    public AbstractWindowBuilding(final B building, final String resource)
    {
        super(resource);

        this.building = building;
        registerButton(BUTTON_BUILD, this::buildClicked);
        registerButton(BUTTON_REPAIR, this::repairClicked);
        registerButton(BUTTON_INVENTORY, this::inventoryClicked);
        switchView = findPaneOfTypeByID(VIEW_PAGES, SwitchView.class);
        title = findPaneOfTypeByID(LABEL_BUILDING_NAME, Label.class);
        buttonNextPage = findPaneOfTypeByID(BUTTON_NEXTPAGE, Button.class);
        buttonPrevPage = findPaneOfTypeByID(BUTTON_PREVPAGE, Button.class);
        pageNum = findPaneOfTypeByID(LABEL_PAGE_NUMBER, Label.class);
        buttonBuild = findPaneOfTypeByID(BUTTON_BUILD, Button.class);
        buttonRepair = findPaneOfTypeByID(BUTTON_REPAIR, Button.class);
    }

    /**
     * Action when build button is clicked.
     */
    private void buildClicked()
    {
        if(buttonBuild.getLabel().equalsIgnoreCase(LanguageHandler.format("com.minecolonies.coremod.gui.workerHuts.cancelBuild"))
                || buttonBuild.getLabel().equalsIgnoreCase(LanguageHandler.format("com.minecolonies.coremod.gui.workerHuts.cancelUpgrade")))
        {
            MineColonies.getNetwork().sendToServer(new BuildRequestMessage(building, BuildRequestMessage.BUILD));
        }
        else
        {
            @NotNull final WindowBuildBuilding window = new WindowBuildBuilding(building.getColony(), building.getLocation());
            window.open();
        }
    }

    /**
     * Action when repair button is clicked.
     */
    private void repairClicked()
    {
        MineColonies.getNetwork().sendToServer(new BuildRequestMessage(building, BuildRequestMessage.REPAIR));
    }

    /**
     * Action when a button opening an inventory is clicked.
     */
    private void inventoryClicked()
    {
        MineColonies.getNetwork().sendToServer(new OpenInventoryMessage(building.getID()));
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();

        // Check if there is no page switcher
        // Or that we are on the correct page
        if (switchView == null || switchView.getCurrentView().getID().equals(PAGE_ACTIONS))
        {
            final AbstractBuildingView buildingView = building.getColony().getBuilding(building.getID());

            if (title != null)
            {
                title.setLabelText(LanguageHandler.format(getBuildingName()) + " " + buildingView.getBuildingLevel());
            }

            updateButtonBuild(buildingView);
            updateButtonRepair(buildingView);
        }
    }

    /**
     * Returns the name of a building.
     *
     * @return Name of a building.
     */
    public abstract String getBuildingName();

    /**
     * Update the state and label for the Build button.
     */
    private void updateButtonBuild(final AbstractBuildingView buildingView)
    {
        if (buttonBuild == null)
        {
            return;
        }

        if (buildingView.isBuildingMaxLevel())
        {
            buttonBuild.setLabel(LanguageHandler.format("com.minecolonies.coremod.gui.workerHuts.switchStyle"));
        }
        else if (buildingView.isBuilding())
        {
            if (buildingView.getBuildingLevel() == 0)
            {
                buttonBuild.setLabel(LanguageHandler.format("com.minecolonies.coremod.gui.workerHuts.cancelBuild"));
            }
            else
            {
                buttonBuild.setLabel(LanguageHandler.format("com.minecolonies.coremod.gui.workerHuts.cancelUpgrade"));
            }
        }
        else
        {
            if (buildingView.getBuildingLevel() == 0)
            {
                buttonBuild.setLabel(LanguageHandler.format("com.minecolonies.coremod.gui.workerHuts.build"));
            }
            else
            {
                buttonBuild.setLabel(LanguageHandler.format("com.minecolonies.coremod.gui.workerHuts.upgrade"));
            }
        }
    }

    /**
     * Update the state and label for the Repair button.
     */
    private void updateButtonRepair(final AbstractBuildingView buildingView)
    {
        if (buttonRepair == null)
        {
            return;
        }

        buttonRepair.setVisible(buildingView.getBuildingLevel() != 0 && !buildingView.isBuilding());
        if (buildingView.isRepairing())
        {
            buttonRepair.setLabel(LanguageHandler.format("com.minecolonies.coremod.gui.workerHuts.cancelRepair"));
        }
        else
        {
            buttonRepair.setLabel(LanguageHandler.format("com.minecolonies.coremod.gui.workerHuts.repair"));
        }
    }

    @Override
    public void onOpened() {
        super.onOpened();
        switchPagesSize = switchView.getChildrenSize();
        setPage("");
    }

    @Override
    public void onButtonClicked(@NotNull final Button button)
    {
        switch (button.getID())
        {
            case BUTTON_PREVPAGE:
                setPage(BUTTON_PREVPAGE);
                break;
            case BUTTON_NEXTPAGE:
                setPage(BUTTON_NEXTPAGE);
                break;
            default:
                super.onButtonClicked(button);
                break;
        }
    }

    public void setPage(@NotNull final String button)
    {
        int curPage = pageNum.getLabelText().isEmpty() ? 1 : Integer.parseInt(pageNum.getLabelText().substring(0, pageNum.getLabelText().indexOf("/")));

        switch (button)
        {
            case BUTTON_PREVPAGE:
                switchView.previousView();
                curPage--;
                break;
            case BUTTON_NEXTPAGE:
                switchView.nextView();
                curPage++;
                break;
            default:
                if (switchPagesSize == 1)
                {
                    buttonPrevPage.off();
                    buttonNextPage.off();
                    pageNum.off();
                    return;
                }
                break;
        }

        buttonNextPage.on();
        buttonPrevPage.on();
        if (curPage == 1)
        {
            buttonPrevPage.off();
        }
        if (curPage == switchPagesSize)
        {
            buttonNextPage.off();
        }
        pageNum.setLabelText(curPage + "/" + switchPagesSize);
    }
}
