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
package icy.image.lut;

import icy.common.EventHierarchicalChecker;

/**
 * @author stephane
 */
public class LUTEvent implements EventHierarchicalChecker
{
    public enum LUTEventType
    {
        SCALER_CHANGED, COLORMAP_CHANGED
    }

    private final LUT lut;
    private final int component;
    private final LUTEventType type;

    /**
     * @param lut
     * @param component
     */
    public LUTEvent(LUT lut, int component, LUTEventType type)
    {
        super();

        this.lut = lut;
        this.component = component;
        this.type = type;
    }

    /**
     * @return the lut
     */
    public LUT getLut()
    {
        return lut;
    }

    /**
     * @return the component
     */
    public int getComponent()
    {
        return component;
    }

    /**
     * @return the {@link LUTEventType}
     */
    public LUTEventType getType()
    {
        return type;
    }

    @Override
    public boolean isEventRedundantWith(EventHierarchicalChecker event)
    {
        if (event instanceof LUTEvent)
        {
            final LUTEvent e = (LUTEvent) event;

            return (type == e.getType()) && ((component == -1) || (component == e.getComponent()));
        }

        return false;
    }
}
