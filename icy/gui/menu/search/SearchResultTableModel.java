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
package icy.gui.menu.search;

import icy.search.SearchResult;
import icy.search.SearchResultProducer;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class SearchResultTableModel extends AbstractTableModel
{
    /**
     * 
     */
    private static final long serialVersionUID = -6476031165522752303L;

    public static final int COL_RESULT_OBJECT = 1;

    private List<SearchResult> results;

    public SearchResultTableModel(int maxRowCount)
    {
        super();

        results = new ArrayList<SearchResult>();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return false;
    }

    @Override
    public int getRowCount()
    {
        return results.size();
    }

    @Override
    public int getColumnCount()
    {
        return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        if (rowIndex < getRowCount())
        {
            final SearchResult element = results.get(rowIndex);

            switch (columnIndex)
            {
                case 0:
                    final SearchResultProducer producer = element.getProducer();

                    // display producer on first producer result
                    if (rowIndex == 0)
                        return producer;
                    else if (results.get(rowIndex - 1).getProducer() != producer)
                        return producer;

                    return null;

                case 1:
                    return element;
            }
        }

        return null;
    }

    public void setResults(List<SearchResult> results)
    {
        this.results = results;
    }
}
