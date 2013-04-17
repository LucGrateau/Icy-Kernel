/*
 * Copyright 2010-2013 Institut Pasteur.
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
package icy.painter;

import vtk.vtkProp;

/**
 * Basic VTK painter.<br>
 * Painter implementing this interface are automatically
 * added / removed from Canvas3D.
 * 
 * @author Stephane
 */
public interface VtkPainter
{
    /**
     * Returns the VTK actors for this painter.
     */
    public vtkProp[] getProps();
}
