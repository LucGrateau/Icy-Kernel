/*
 * Copyright 2010-2015 Institut Pasteur.
 * 
 * This file is part of Icy.
 * 
 * Icy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Icy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Icy. If not, see <http://www.gnu.org/licenses/>.
 */
package icy.gui.menu;

import icy.action.FileActions;
import icy.action.FileActions.OpenSequenceRegionAction;
import icy.action.RoiActions;
import icy.gui.component.button.IcyCommandButton;
import icy.gui.component.button.IcyCommandMenuButton;
import icy.gui.component.button.IcyCommandToggleButton;
import icy.gui.plugin.PluginCommandButton;
import icy.gui.util.RibbonUtil;
import icy.main.Icy;
import icy.plugin.PluginDescriptor;
import icy.plugin.PluginLoader;
import icy.plugin.PluginLoader.PluginLoaderEvent;
import icy.plugin.PluginLoader.PluginLoaderListener;
import icy.resource.ResourceUtil;
import icy.resource.icon.IcyIcon;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.roi.ROI3D;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;
import icy.util.StringUtil;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import javax.swing.event.EventListenerList;

import org.pushingpixels.flamingo.api.common.AbstractCommandButton;
import org.pushingpixels.flamingo.api.common.CommandToggleButtonGroup;
import org.pushingpixels.flamingo.api.common.JCommandButton;
import org.pushingpixels.flamingo.api.common.JCommandButton.CommandButtonKind;
import org.pushingpixels.flamingo.api.common.JCommandToggleButton;
import org.pushingpixels.flamingo.api.common.RichTooltip;
import org.pushingpixels.flamingo.api.common.popup.JCommandPopupMenu;
import org.pushingpixels.flamingo.api.common.popup.JPopupPanel;
import org.pushingpixels.flamingo.api.common.popup.PopupPanelCallback;
import org.pushingpixels.flamingo.api.ribbon.JRibbonBand;
import org.pushingpixels.flamingo.api.ribbon.RibbonElementPriority;
import org.pushingpixels.flamingo.api.ribbon.RibbonTask;
import org.pushingpixels.flamingo.api.ribbon.resize.CoreRibbonResizeSequencingPolicies;

import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DShape;
import plugins.kernel.roi.roi2d.plugin.ROI2DAreaPlugin;
import plugins.kernel.roi.roi2d.plugin.ROI2DEllipsePlugin;
import plugins.kernel.roi.roi2d.plugin.ROI2DLinePlugin;
import plugins.kernel.roi.roi2d.plugin.ROI2DPointPlugin;
import plugins.kernel.roi.roi2d.plugin.ROI2DPolyLinePlugin;
import plugins.kernel.roi.roi2d.plugin.ROI2DPolygonPlugin;
import plugins.kernel.roi.roi2d.plugin.ROI2DRectanglePlugin;
import plugins.kernel.roi.roi3d.ROI3DArea;
import plugins.kernel.roi.roi3d.plugin.ROI3DLinePlugin;
import plugins.kernel.roi.roi3d.plugin.ROI3DPointPlugin;
import plugins.kernel.roi.roi3d.plugin.ROI3DPolyLinePlugin;
import plugins.kernel.roi.roi4d.ROI4DArea;
import plugins.kernel.roi.roi5d.ROI5DArea;
import plugins.kernel.roi.tool.plugin.ROILineCutterPlugin;

/**
 * ROI dedicated task
 * 
 * @author Stephane
 */
public class ToolRibbonTask extends RibbonTask implements PluginLoaderListener
{
    public static final String NAME = "File & ROI tools";

    /**
     * @deprecated Use {@link #setSelected(String)} with <code>null</code> parameter instead.
     */
    @Deprecated
    public static final String SELECT = "Selection";
    /**
     * @deprecated Use {@link #setSelected(String)} with <code>null</code> parameter instead.
     */
    @Deprecated
    public static final String MOVE = "Move";

    /**
     * @deprecated Use {@link ToolRibbonTask#isROITool()} instead.
     */
    @Deprecated
    public static boolean isROITool(String command)
    {
        // assume it's a ROI command when it's not null
        return (command != null);
    }

    /**
     * Listener class
     */
    public interface ToolRibbonTaskListener extends EventListener
    {
        public void toolChanged(String command);
    }

    public static class FileRibbonBand extends JRibbonBand
    {
        /**
     *
     */
        private static final long serialVersionUID = -2677243480668715388L;

        public static final String BAND_NAME = "File";

        final IcyCommandButton openButton;
        final IcyCommandButton openAreaButton;
        final IcyCommandButton saveButton;

        public FileRibbonBand()
        {
            super(BAND_NAME, new IcyIcon(ResourceUtil.ICON_DOC));

            openButton = new IcyCommandButton(FileActions.openSequenceAction);
            openAreaButton = new IcyCommandButton("Open region", new IcyIcon(ResourceUtil.ICON_CROP));
            openAreaButton.setCommandButtonKind(CommandButtonKind.POPUP_ONLY);
            openAreaButton.setPopupRichTooltip(new RichTooltip("Open selected region",
                    "Open the selected ROI region from the original image at a specific resolution level"));
            openAreaButton.setPopupCallback(new PopupPanelCallback()
            {
                @Override
                public JPopupPanel getPopupPanel(JCommandButton commandButton)
                {
                    final JCommandPopupMenu result = new JCommandPopupMenu();

                    for (int r = 0; r < 5; r++)
                        result.addMenuButton(new IcyCommandMenuButton(new OpenSequenceRegionAction(r)));

                    return result;
                }
            });
            saveButton = new IcyCommandButton(FileActions.saveAsSequenceAction);

            addCommandButton(openButton, RibbonElementPriority.MEDIUM);
            addCommandButton(openAreaButton, RibbonElementPriority.MEDIUM);
            addCommandButton(saveButton, RibbonElementPriority.MEDIUM);

            RibbonUtil.setRestrictiveResizePolicies(this);
            updateButtonsState();
        }

        void updateButtonsState()
        {
            final Sequence sequence = Icy.getMainInterface().getActiveSequence();

            openAreaButton.setEnabled((sequence != null) && (!StringUtil.isEmpty(sequence.getFilename()))
                    && sequence.hasROI());
            saveButton.setEnabled(Icy.getMainInterface().getActiveSequence() != null);
        }
    }

    static class ROI2DBand extends JRibbonBand
    {
        public static final String BAND_NAME = "2D ROI";

        final List<IcyCommandToggleButton> pluginButtons;

        public ROI2DBand()
        {
            super(BAND_NAME, new IcyIcon(ResourceUtil.ICON_DOC));

            startGroup();

            pluginButtons = new ArrayList<IcyCommandToggleButton>();

            // refresh buttons (don't set button group and listener at this point)
            setVisible(setROIFromPlugins(pluginButtons, this, getROIPlugins(), null, null));
            updateButtonsState();

            RibbonUtil.setRestrictiveResizePolicies(this);
            setToolTipText("2D Region Of Interest");
        }

        public static List<PluginDescriptor> getROIPlugins()
        {
            final List<PluginDescriptor> result = new ArrayList<PluginDescriptor>();

            // 2D ROI
            result.add(PluginLoader.getPlugin(ROI2DAreaPlugin.class.getName()));
            result.add(PluginLoader.getPlugin(ROI2DRectanglePlugin.class.getName()));
            result.add(PluginLoader.getPlugin(ROI2DEllipsePlugin.class.getName()));
            result.add(PluginLoader.getPlugin(ROI2DPolygonPlugin.class.getName()));
            result.add(PluginLoader.getPlugin(ROI2DPointPlugin.class.getName()));
            result.add(PluginLoader.getPlugin(ROI2DLinePlugin.class.getName()));
            result.add(PluginLoader.getPlugin(ROI2DPolyLinePlugin.class.getName()));

            return result;
        }

        void updateButtonsState()
        {
            final Sequence sequence = Icy.getMainInterface().getActiveSequence();

            for (IcyCommandToggleButton button : pluginButtons)
                button.setEnabled(sequence != null);
        }
    }

    static class ROI3DBand extends JRibbonBand
    {
        public static final String BAND_NAME = "3D ROI";

        final List<IcyCommandToggleButton> pluginButtons;

        public ROI3DBand()
        {
            super(BAND_NAME, new IcyIcon(ResourceUtil.ICON_DOC));

            startGroup();

            pluginButtons = new ArrayList<IcyCommandToggleButton>();

            // refresh buttons (don't set button group and listener at this point)
            setVisible(setROIFromPlugins(pluginButtons, this, getROIPlugins(), null, null));
            updateButtonsState();

            RibbonUtil.setRestrictiveResizePolicies(this);
            setToolTipText("3D Region Of Interest");
        }

        public static List<PluginDescriptor> getROIPlugins()
        {
            final List<PluginDescriptor> result = new ArrayList<PluginDescriptor>();

            // 3D ROI
            result.add(PluginLoader.getPlugin(ROI3DPointPlugin.class.getName()));
            result.add(PluginLoader.getPlugin(ROI3DLinePlugin.class.getName()));
            result.add(PluginLoader.getPlugin(ROI3DPolyLinePlugin.class.getName()));

            return result;
        }

        void updateButtonsState()
        {
            final Sequence sequence = Icy.getMainInterface().getActiveSequence();

            for (IcyCommandToggleButton button : pluginButtons)
                button.setEnabled(sequence != null);
        }
    }

    static class ROIExtBand extends JRibbonBand
    {
        public static final String BAND_NAME = "External ROI";

        final List<IcyCommandToggleButton> pluginButtons;

        public ROIExtBand()
        {
            super(BAND_NAME, new IcyIcon(ResourceUtil.ICON_DOC));

            startGroup();

            pluginButtons = new ArrayList<IcyCommandToggleButton>();

            // refresh buttons (don't set button group and listener at this point)
            setVisible(setROIFromPlugins(pluginButtons, this, getROIPlugins(), null, null));
            updateButtonsState();

            RibbonUtil.setRestrictiveResizePolicies(this);
            setToolTipText("External Region Of Interest");
        }

        public static List<PluginDescriptor> getROIPlugins()
        {
            final List<PluginDescriptor> result = new ArrayList<PluginDescriptor>();

            // remove default 2D & 3D ROI to only keep external ROI
            result.removeAll(ROI2DBand.getROIPlugins());
            result.removeAll(ROI3DBand.getROIPlugins());
            // explicitly remove the ROI cutter from the list
            result.remove(PluginLoader.getPlugin(ROILineCutterPlugin.class.getName()));

            return result;
        }

        void updateButtonsState()
        {
            final Sequence sequence = Icy.getMainInterface().getActiveSequence();

            for (IcyCommandToggleButton button : pluginButtons)
                button.setEnabled(sequence != null);
        }
    }

    static class ROIConversionBand extends JRibbonBand
    {
        public static final String BAND_NAME = "Conversion";

        final IcyCommandButton convertToStackButton;
        final IcyCommandButton convertToMaskButton;
        final IcyCommandButton convertToShapeButton;

        public ROIConversionBand()
        {
            super(BAND_NAME, new IcyIcon(ResourceUtil.ICON_LAYER_V2));

            // conversion
            convertToStackButton = new IcyCommandButton(RoiActions.convertToStackAction);
            convertToMaskButton = new IcyCommandButton(RoiActions.convertToMaskAction);
            convertToShapeButton = new IcyCommandButton(RoiActions.convertToShapeAction);
            addCommandButton(convertToStackButton, RibbonElementPriority.MEDIUM);
            addCommandButton(convertToMaskButton, RibbonElementPriority.MEDIUM);
            addCommandButton(convertToShapeButton, RibbonElementPriority.MEDIUM);

            setToolTipText("Conversion tools for ROI");
            RibbonUtil.setRestrictiveResizePolicies(this);
        }

        public void updateButtonsState()
        {
            boolean convertStackEnable = false;
            boolean convertMaskEnable = false;
            boolean convertShapeEnable = false;
            final Sequence seq = Icy.getMainInterface().getActiveSequence();

            if (seq != null)
            {
                final List<ROI> selectedRois = seq.getSelectedROIs();

                for (ROI roi : selectedRois)
                {
                    if (roi instanceof ROI2D)
                    {
                        convertStackEnable = true;
                        if (!(roi instanceof ROI2DShape))
                            convertShapeEnable = true;
                    }
                    if (!((roi instanceof ROI2DArea) || (roi instanceof ROI3DArea) || (roi instanceof ROI4DArea) || (roi instanceof ROI5DArea)))
                        convertMaskEnable = true;
                    if (roi instanceof ROI3D)
                    {
                        if (roi instanceof ROI3DArea)
                            convertShapeEnable = true;
                    }
                }
            }

            convertToStackButton.setEnabled(convertStackEnable);
            convertToMaskButton.setEnabled(convertMaskEnable);
            convertToShapeButton.setEnabled(convertShapeEnable);
        }
    }

    static class ROIBooleanOpBand extends JRibbonBand
    {
        public static final String BAND_NAME = "Boolean Op";

        final IcyCommandButton booleanUnionButton;
        final IcyCommandButton booleanIntersectionButton;
        final IcyCommandButton booleanOthersButton;
        final IcyCommandMenuButton booleanInversionButton;
        final IcyCommandMenuButton booleanExclusiveUnionButton;
        final IcyCommandMenuButton booleanSubtractionButton;

        public ROIBooleanOpBand()
        {
            super(BAND_NAME, new IcyIcon(ResourceUtil.ICON_LAYER_V2));

            booleanUnionButton = new IcyCommandButton(RoiActions.boolOrAction);
            booleanIntersectionButton = new IcyCommandButton(RoiActions.boolAndAction);
            booleanInversionButton = new IcyCommandMenuButton(RoiActions.boolNotAction);
            booleanExclusiveUnionButton = new IcyCommandMenuButton(RoiActions.boolXorAction);
            booleanSubtractionButton = new IcyCommandMenuButton(RoiActions.boolSubtractAction);

            booleanOthersButton = new IcyCommandButton("Other operation");
            booleanOthersButton.setCommandButtonKind(CommandButtonKind.POPUP_ONLY);
            booleanOthersButton.setPopupCallback(new PopupPanelCallback()
            {
                @Override
                public JPopupPanel getPopupPanel(JCommandButton arg0)
                {
                    final JCommandPopupMenu result = new JCommandPopupMenu();

                    result.addMenuButton(booleanInversionButton);
                    result.addMenuButton(booleanExclusiveUnionButton);
                    result.addMenuButton(booleanSubtractionButton);

                    return result;
                }
            });
            booleanOthersButton.setEnabled(false);

            addCommandButton(booleanUnionButton, RibbonElementPriority.MEDIUM);
            addCommandButton(booleanIntersectionButton, RibbonElementPriority.MEDIUM);
            addCommandButton(booleanOthersButton, RibbonElementPriority.MEDIUM);

            setToolTipText("Boolean operation for ROI");
            RibbonUtil.setRestrictiveResizePolicies(this);
        }

        public void updateButtonsState()
        {
            boolean singleOp = false;
            boolean boolOp = false;
            final Sequence seq = Icy.getMainInterface().getActiveSequence();

            if (seq != null)
            {
                final List<ROI> selectedRois = seq.getSelectedROIs();

                singleOp = !selectedRois.isEmpty();
                boolOp = selectedRois.size() > 1;
            }

            booleanUnionButton.setEnabled(boolOp);
            booleanIntersectionButton.setEnabled(boolOp);
            booleanInversionButton.setEnabled(singleOp);
            booleanExclusiveUnionButton.setEnabled(boolOp);
            booleanSubtractionButton.setEnabled(boolOp);
            booleanOthersButton.setEnabled(singleOp);
        }
    }

    static class ROISeparationBand extends JRibbonBand
    {
        public static final String BAND_NAME = "Separation";

        final IcyCommandButton separateObjectsButton;
        final IcyCommandToggleButton cutButton;
        final IcyCommandButton splitButton;

        public ROISeparationBand()
        {
            super(BAND_NAME, new IcyIcon(ResourceUtil.ICON_LAYER_V2));

            // conversion
            separateObjectsButton = new IcyCommandButton(RoiActions.separateObjectsAction);
            cutButton = PluginCommandButton.createToggleButton(
                    PluginLoader.getPlugin(ROILineCutterPlugin.class.getName()), false, true);
            splitButton = new IcyCommandButton(RoiActions.autoSplitAction);
            addCommandButton(separateObjectsButton, RibbonElementPriority.MEDIUM);
            addCommandButton(cutButton, RibbonElementPriority.MEDIUM);
//            addCommandButton(splitButton, RibbonElementPriority.MEDIUM);

            cutButton.setEnabled(false);
            
            setToolTipText("Separation tools for ROI");
            RibbonUtil.setRestrictiveResizePolicies(this);
        }

        public void updateButtonsState()
        {
            boolean separateObjEnable = false;
            boolean cutEnable = false;
            boolean splitEnable = false;
            final Sequence seq = Icy.getMainInterface().getActiveSequence();

            if (seq != null)
            {
                final List<ROI> selectedRois = seq.getSelectedROIs();

                cutEnable = seq.hasROI();
                separateObjEnable = !selectedRois.isEmpty();
                splitEnable = !selectedRois.isEmpty();
            }

            separateObjectsButton.setEnabled(separateObjEnable);
            cutButton.setEnabled(cutEnable);
            splitButton.setEnabled(splitEnable);
        }
    }

//    static class ROIMorphoBand extends JRibbonBand
//    {
//        public static final String BAND_NAME = "Morphology";
//
//        final IcyCommandButton erodeButton;
//        final IcyCommandButton dilateButton;
//        final IcyCommandButton fillHoledilateButton;
//        final IcyCommandButton otherButton;
//
//        public ROIMorphoBand()
//        {
//            super(BAND_NAME, new IcyIcon(ResourceUtil.ICON_LAYER_V2));
//
//            booleanUnionButton = new IcyCommandButton(RoiActions.boolOrAction);
//            booleanIntersectionButton = new IcyCommandButton(RoiActions.boolAndAction);
//            booleanInversionButton = new IcyCommandMenuButton(RoiActions.boolNotAction);
//            booleanExclusiveUnionButton = new IcyCommandMenuButton(RoiActions.boolXorAction);
//            booleanSubtractionButton = new IcyCommandMenuButton(RoiActions.boolSubtractAction);
//
//            booleanOthersButton = new IcyCommandButton("Other operation");
//            booleanOthersButton.setCommandButtonKind(CommandButtonKind.POPUP_ONLY);
//            booleanOthersButton.setPopupCallback(new PopupPanelCallback()
//            {
//                @Override
//                public JPopupPanel getPopupPanel(JCommandButton arg0)
//                {
//                    final JCommandPopupMenu result = new JCommandPopupMenu();
//
//                    result.addMenuButton(booleanInversionButton);
//                    result.addMenuButton(booleanExclusiveUnionButton);
//                    result.addMenuButton(booleanSubtractionButton);
//
//                    return result;
//                }
//            });
//            booleanOthersButton.setEnabled(false);
//
//            addCommandButton(booleanUnionButton, RibbonElementPriority.MEDIUM);
//            addCommandButton(booleanIntersectionButton, RibbonElementPriority.MEDIUM);
//            addCommandButton(booleanOthersButton, RibbonElementPriority.MEDIUM);
//
//            setToolTipText("Morphological operator for ROI");
//            RibbonUtil.setRestrictiveResizePolicies(this);
//        }
//
//        public void updateButtonsState()
//        {
//            boolean separateObjEnable = false;
//            boolean cutEnable = false;
//            boolean splitEnable = false;
//            final Sequence seq = Icy.getMainInterface().getActiveSequence();
//
//            if (seq != null)
//            {
//                final List<ROI> selectedRois = seq.getSelectedROIs();
//
//                cutEnable = seq.hasROI();
//                separateObjEnable = !selectedRois.isEmpty();
//                splitEnable = !selectedRois.isEmpty();
//            }
//
//            separateObjectsButton.setEnabled(separateObjEnable);
//            cutButton.setEnabled(cutEnable);
//            splitButton.setEnabled(splitEnable);
//        }
//    }

    final FileRibbonBand fileBand;
    final ROI2DBand roi2dBand;
    final ROI3DBand roi3dBand;
    final ROIExtBand roiExtBand;
    final ROIConversionBand roiConversionBand;
    final ROIBooleanOpBand roiBooleanOpBand;
    final ROISeparationBand roiSeparationBand;

    final CommandToggleButtonGroup buttonGroup;
    final ActionListener buttonActionListener;

    /**
     * List of listeners
     */
    private final EventListenerList listeners;

    private Runnable buttonUpdater;

    public ToolRibbonTask()
    {
        super(NAME, new FileRibbonBand(), new ROI2DBand(), new ROI3DBand(), new ROIExtBand(), new ROIConversionBand(),
                new ROISeparationBand(), new ROIBooleanOpBand());
        // super(NAME,, new ROIRibbonBand());

        setResizeSequencingPolicy(new CoreRibbonResizeSequencingPolicies.CollapseFromLast(this));

        // get band
        fileBand = (FileRibbonBand) RibbonUtil.getBand(this, FileRibbonBand.BAND_NAME);
        roi2dBand = (ROI2DBand) RibbonUtil.getBand(this, ROI2DBand.BAND_NAME);
        roi3dBand = (ROI3DBand) RibbonUtil.getBand(this, ROI3DBand.BAND_NAME);
        roiExtBand = (ROIExtBand) RibbonUtil.getBand(this, ROIExtBand.BAND_NAME);
        roiConversionBand = (ROIConversionBand) RibbonUtil.getBand(this, ROIConversionBand.BAND_NAME);
        roiSeparationBand = (ROISeparationBand) RibbonUtil.getBand(this, ROISeparationBand.BAND_NAME);
        roiBooleanOpBand = (ROIBooleanOpBand) RibbonUtil.getBand(this, ROIBooleanOpBand.BAND_NAME);

        // create button state updater
        buttonUpdater = new Runnable()
        {
            @Override
            public void run()
            {
                // sleep a bit
                ThreadUtil.sleep(1);

                ThreadUtil.invokeNow(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        fileBand.updateButtonsState();
                        roi2dBand.updateButtonsState();
                        roi3dBand.updateButtonsState();
                        roiExtBand.updateButtonsState();
                        roiConversionBand.updateButtonsState();
                        roiBooleanOpBand.updateButtonsState();
                        roiSeparationBand.updateButtonsState();
                    }
                });
            }
        };

        listeners = new EventListenerList();

        buttonActionListener = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setSelectedButton((IcyCommandToggleButton) e.getSource(), true);
            }
        };

        // create button group
        buttonGroup = new CommandToggleButtonGroup();
        buttonGroup.setAllowsClearingSelection(true);

        // add action listener here
        for (AbstractCommandButton button : RibbonUtil.getButtons(roi2dBand))
            button.addActionListener(buttonActionListener);
        for (AbstractCommandButton button : RibbonUtil.getButtons(roi3dBand))
            button.addActionListener(buttonActionListener);
        for (AbstractCommandButton button : RibbonUtil.getButtons(roiExtBand))
            button.addActionListener(buttonActionListener);
        // cut button act as a selector
        roiSeparationBand.cutButton.addActionListener(buttonActionListener);

        for (AbstractCommandButton button : RibbonUtil.getButtons(roi2dBand))
            buttonGroup.add((IcyCommandToggleButton) button);
        for (AbstractCommandButton button : RibbonUtil.getButtons(roi3dBand))
            buttonGroup.add((IcyCommandToggleButton) button);
        for (AbstractCommandButton button : RibbonUtil.getButtons(roiExtBand))
            buttonGroup.add((IcyCommandToggleButton) button);
        // cut button which act as a selector
        buttonGroup.add(roiSeparationBand.cutButton);

        PluginLoader.addListener(this);
    }

    protected void refreshROIButtons()
    {
        // refresh 2D ROI buttons
        roi2dBand.setVisible(setROIFromPlugins(roi2dBand.pluginButtons, roi2dBand, ROI2DBand.getROIPlugins(),
                buttonGroup, buttonActionListener));
        // refresh 3D ROI buttons
        roi3dBand.setVisible(setROIFromPlugins(roi3dBand.pluginButtons, roi3dBand, ROI3DBand.getROIPlugins(),
                buttonGroup, buttonActionListener));
        // refresh external ROI buttons
        roiExtBand.setVisible(setROIFromPlugins(roiExtBand.pluginButtons, roiExtBand, ROIExtBand.getROIPlugins(),
                buttonGroup, buttonActionListener));

        roi2dBand.updateButtonsState();
        roi3dBand.updateButtonsState();
        roiExtBand.updateButtonsState();
    }

    protected static boolean setROIFromPlugins(List<IcyCommandToggleButton> pluginButtons, JRibbonBand band,
            List<PluginDescriptor> roiPlugins, CommandToggleButtonGroup buttonGroup, ActionListener al)
    {
        boolean notEmpty = false;

        // remove previous plugin buttons
        for (IcyCommandToggleButton button : pluginButtons)
        {
            if (al != null)
                button.removeActionListener(al);
            band.removeCommandButton(button);
            if (buttonGroup != null)
                buttonGroup.remove(button);
        }
        pluginButtons.clear();

        for (PluginDescriptor plugin : roiPlugins)
        {
            final IcyCommandToggleButton button = PluginCommandButton.createToggleButton(plugin, false, plugin.isKernelPlugin());

            if (plugin.getClassName().equals(ROI2DAreaPlugin.class.getName()))
                band.addCommandButton(button, RibbonElementPriority.TOP);
            else
                band.addCommandButton(button, RibbonElementPriority.MEDIUM);

            if (al != null)
                button.addActionListener(al);
            if (buttonGroup != null)
                buttonGroup.add(button);

            pluginButtons.add(button);
            notEmpty = true;
        }

        return notEmpty;
    }

    protected IcyCommandToggleButton getButtonFromName(String name)
    {
        if (StringUtil.isEmpty(name))
            return null;

        for (AbstractCommandButton button : RibbonUtil.getButtons(roi2dBand))
            if (name.equals(button.getName()))
                return (IcyCommandToggleButton) button;
        for (AbstractCommandButton button : RibbonUtil.getButtons(roi3dBand))
            if (name.equals(button.getName()))
                return (IcyCommandToggleButton) button;

        return null;
    }

    /**
     * Returns true if current selected tool is ROI type tool.
     */
    public boolean isROITool()
    {
        // currently we only have ROI tool
        // so as soon selected is not null it is ROI tool
        return getSelected() != null;
    }

    /**
     * Returns the current selected button (can be <code>null</code>).
     */
    protected JCommandToggleButton getSelectedButton()
    {
        return buttonGroup.getSelected();
    }

    /**
     * Sets the current selected button (can be <code>null</code>).
     */
    protected void setSelectedButton(JCommandToggleButton button, boolean force)
    {
        if (force || (getSelectedButton() != button))
        {
            // select the button
            if (button != null)
                buttonGroup.setSelected(button, true);
            else
                buttonGroup.clearSelection();

            // notify tool change
            toolChanged(getSelected());
        }
    }

    /**
     * Returns the current selected tool.<br>
     * It can be null if no tool is currently selected.
     */
    public String getSelected()
    {
        final JCommandToggleButton button = getSelectedButton();

        if (button != null)
            return button.getName();

        return null;
    }

    /**
     * Sets the current selected tool.<br>
     * If <i>toolName</i> is a invalid tool name or <code>null</code> then no tool is selected.
     */
    public void setSelected(String value)
    {
        setSelectedButton(getButtonFromName(value), false);
    }

    public void toolChanged(String toolName)
    {
        fireChangedEvent(toolName);
    }

    /**
     * Add a listener
     * 
     * @param listener
     */
    public void addListener(ToolRibbonTaskListener listener)
    {
        listeners.add(ToolRibbonTaskListener.class, listener);
    }

    /**
     * Remove a listener
     * 
     * @param listener
     */
    public void removeListener(ToolRibbonTaskListener listener)
    {
        listeners.remove(ToolRibbonTaskListener.class, listener);
    }

    /**
     * @param toolName
     */
    void fireChangedEvent(String toolName)
    {
        for (ToolRibbonTaskListener listener : listeners.getListeners(ToolRibbonTaskListener.class))
            listener.toolChanged(toolName);
    }

    /**
     * call this method on sequence activation change
     */
    public void onSequenceActivationChange()
    {
        ThreadUtil.runSingle(buttonUpdater);
    }

    /**
     * call this method on sequence ROI change
     */
    public void onSequenceChange()
    {
        ThreadUtil.runSingle(buttonUpdater);
    }

    @Override
    public void pluginLoaderChanged(PluginLoaderEvent e)
    {
        ThreadUtil.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                refreshROIButtons();
            }
        });
    }
}
