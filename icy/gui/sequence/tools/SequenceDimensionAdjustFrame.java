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
package icy.gui.sequence.tools;

import icy.gui.dialog.ActionDialog;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.util.ComponentUtil;
import icy.sequence.AbstractSequenceModel;
import icy.sequence.DimensionId;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;

/**
 * @author Stephane
 */
public class SequenceDimensionAdjustFrame extends ActionDialog
{
    /**
     * 
     */
    private static final long serialVersionUID = -383226926743211242L;

    private class SequenceDimensionAdjustFrameModel extends AbstractSequenceModel
    {
        public SequenceDimensionAdjustFrameModel()
        {
            super();
        }

        @Override
        public int getSizeX()
        {
            if (sequence != null)
                return sequence.getSizeX();

            return 0;
        }

        @Override
        public int getSizeY()
        {
            if (sequence != null)
                return sequence.getSizeY();

            return 0;
        }

        @Override
        public int getSizeZ()
        {
            if (sequence != null)
                return sequence.getSizeZ();

            return 0;
        }

        @Override
        public int getSizeT()
        {
            if (sequence != null)
                return sequence.getSizeT();

            return 0;
        }

        @Override
        public int getSizeC()
        {
            if (sequence != null)
                return sequence.getSizeC();

            return 0;
        }

        @Override
        public BufferedImage getImage(int t, int z)
        {
            if (sequence != null)
                return sequence.getImage(t, z);

            return null;
        }

        @Override
        public BufferedImage getImage(int t, int z, int c)
        {
            if (sequence != null)
                return sequence.getImage(t, z, c);

            return null;
        }
    }

    final Sequence sequence;
    final SequenceDimensionAdjustPanel rangePanel;
    final DimensionId dim;

    public SequenceDimensionAdjustFrame(Sequence sequence, DimensionId dim)
    {
        super("Adjust " + dim.toString() + " dimension");

        this.sequence = sequence;
        this.dim = dim;

        rangePanel = new SequenceDimensionAdjustPanel(dim);
        rangePanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));

        mainPanel.add(rangePanel, BorderLayout.CENTER);
        validate();

        rangePanel.setModel(new SequenceDimensionAdjustFrameModel());

        setOkAction(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                ThreadUtil.bgRun(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        final DimensionId dim = SequenceDimensionAdjustFrame.this.dim;
                        final ProgressFrame pf;

                        if (dim == DimensionId.Z)
                            pf = new ProgressFrame("Removing slices...");
                        else
                            pf = new ProgressFrame("Removing frames...");

                        final Sequence seq = SequenceDimensionAdjustFrame.this.sequence;
                        final Sequence tmp = new Sequence();

                        int sizeT = seq.getSizeT();
                        int sizeZ = seq.getSizeZ();

                        tmp.beginUpdate();
                        seq.beginUpdate();
                        try
                        {
                            int i = 0;

                            // create a temporary sequence containing resulting images
                            for (int t = 0; t < sizeT; t++)
                            {
                                if (dim == DimensionId.Z)
                                    i = 0;

                                for (int z = 0; z < sizeZ; z++)
                                {
                                    if (dim == DimensionId.Z)
                                    {
                                        if (rangePanel.isIndexSelected(z))
                                            tmp.setImage(t, i++, seq.getImage(t, z));
                                    }
                                    else
                                    {
                                        if (rangePanel.isIndexSelected(t))
                                            tmp.setImage(i, z, seq.getImage(t, z));
                                    }
                                }

                                if ((dim == DimensionId.T) && (rangePanel.isIndexSelected(t)))
                                    i++;
                            }

                            sizeT = tmp.getSizeT();
                            sizeZ = tmp.getSizeZ();

                            // then copy back tmp images in seq
                            seq.removeAllImages();
                            for (int t = 0; t < sizeT; t++)
                                for (int z = 0; z < sizeZ; z++)
                                    seq.setImage(t, z, tmp.getImage(t, z));
                        }
                        finally
                        {
                            seq.endUpdate();
                            tmp.endUpdate();
                            pf.close();
                        }
                    }
                });
            }
        });

        setSize(320, 360);
        ComponentUtil.center(this);

        setVisible(true);
    }

    /**
     * @wbp.parser.constructor
     */
    SequenceDimensionAdjustFrame()
    {
        this(new Sequence(), DimensionId.Z);
    }
}
