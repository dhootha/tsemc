/*
 * JStock - Free Stock Market Software
 * Copyright (C) 2010 Yan Cheng CHEOK <yccheok@yahoo.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.yccheok.jstock.gui;

import java.util.*;
import org.yccheok.jstock.engine.*;
import org.yccheok.jstock.file.CSVHelper;
import org.yccheok.jstock.file.GUIBundleWrapper;
import org.yccheok.jstock.file.GUIBundleWrapper.Language;
import org.yccheok.jstock.internationalization.GUIBundle;

/**
 *
 * @author yccheok
 */
public class StockTableModel extends AbstractTableModelWithMemory implements CSVHelper {
    /** Creates a new instance of StockTableModel */
    public StockTableModel() {
        for (int i = 0; i < columnNames.length; i++) {
            columnNameMapping.put(columnNames[i], i);
        }
                
    }

    @Override
    public Object getOldValueAt(int rowIndex, int columnIndex) {
        List<Object> stockInfo = oldTableModel.get(rowIndex);
        
        if (null == stockInfo) return null;
        
        return stockInfo.get(columnIndex);        
    }

    @Override    
    public void clearOldValueAt(int rowIndex, int columnIndex) {
        List<Object> stockInfo = oldTableModel.get(rowIndex);
        
        if (null == stockInfo) return;
        
        stockInfo.set(columnIndex, null);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        List<Object> stockInfo = tableModel.get(rowIndex);
        return stockInfo.get(columnIndex);
    }
    
    @Override
    public int getColumnCount()
    {
        return columnNames.length;
    }
    
    @Override
    public int getRowCount()
    {
        return tableModel.size();
    }
    
    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public Class getColumnClass(int c) {
        return columnClasses[c];
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return ((col == (columnClasses.length - 1)) || (col == (columnClasses.length - 2)));
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        if (col == (columnClasses.length - 1))
        {
            final Double riseAbove = (Double)value;
            alerts.set(row, alerts.get(row).setRiseAbove(riseAbove));
            List<Object> oldInfos = oldTableModel.get(row);
            if (oldInfos != null) oldInfos.set(col, tableModel.get(row).get(col));
            tableModel.get(row).set(col, riseAbove);
            fireTableCellUpdated(row, col);
            return;
        }
        else if (col == (columnClasses.length - 2))
        {
            final Double fallBelow = (Double)value;
            alerts.set(row, alerts.get(row).setFallBelow(fallBelow));
            List<Object> oldInfos = oldTableModel.get(row);
            if (oldInfos != null) oldInfos.set(col, tableModel.get(row).get(col));
            tableModel.get(row).set(col, fallBelow);
            fireTableCellUpdated(row, col);
            return;
        }
        
        throw new java.lang.UnsupportedOperationException();
    }
    
    public void updateStock(Stock stock) {
        final Integer row = rowStockCodeMapping.get(stock.getCode());

        if (row != null) {
            oldTableModel.set(row, tableModel.get(row));
            final StockAlert alert = alerts.get(row);
            tableModel.set(row, stockToList(stock, alert));
            stocks.set(row, stock);
            this.fireTableRowsUpdated(row, row);            
        }        
    }

    public void addStock(Stock stock, StockAlert alert) {
        Integer row = rowStockCodeMapping.get(stock.getCode());
        if (row == null) {
            tableModel.add(stockToList(stock, alert));
            oldTableModel.add(null);
            stocks.add(stock);
            alerts.add(alert);
            final int rowIndex = tableModel.size() - 1;
            rowStockCodeMapping.put(stock.getCode(), rowIndex);
            fireTableRowsInserted(rowIndex, rowIndex);
        }
    }

    public void addStock(Stock stock) {
        this.addStock(stock, new StockAlert());
    }
    
    public void clearAllStocks() {
        final int size = stocks.size();
        
        if (size == 0) return;
        
        tableModel.clear();
        oldTableModel.clear();
        stocks.clear();
        alerts.clear();
        rowStockCodeMapping.clear();
            
        this.fireTableRowsDeleted(0, size - 1);
    }
    
    /**
     * Returns stock based on the row index.
     *
     * @param row The row index
     * @return Stock based on the row index.
     */
    public Stock getStock(int row) {
        return stocks.get(row);
    }

    public Double getRiseAbove(Stock stock) {
        Integer row = this.rowStockCodeMapping.get(stock.getCode());
        if (row == null) return null;
        return this.alerts.get(row).getRiseAbove();
    }

    public Double getFallBelow(Stock stock) {
        Integer row = this.rowStockCodeMapping.get(stock.getCode());
        if (row == null) return null;
        return this.alerts.get(row).getFallBelow();
    }

    public List<StockAlert> getAlerts() {
        return Collections.unmodifiableList(alerts);
    }

    public List<Stock> getStocks() {
        return Collections.unmodifiableList(stocks);
    }
    
    public void removeRow(int row) {
        oldTableModel.remove(row);
        List<Object> list = tableModel.remove(row);
        stocks.remove(row);
        alerts.remove(row);
        // 0 is stock code.
        rowStockCodeMapping.remove((Code)list.get(0));

        int size = stocks.size();
        for (int i = row; i < size; i++) {
            Stock s = stocks.get(i);
            rowStockCodeMapping.put(s.getCode(), i);
        }
        
        this.fireTableRowsDeleted(row, row);
    }
    
    private List<Object> stockToList(Stock stock, StockAlert alert) {
        List<Object> list = new ArrayList<Object>();
        list.add(stock.getCode());
        list.add(stock.getSymbol());
        list.add(stock.getPrevPrice());
        list.add(stock.getOpenPrice());
        list.add(stock.getLastPrice());
        list.add(stock.getHighPrice());
        list.add(stock.getLowPrice());
        list.add(stock.getVolume());
        list.add(stock.getChangePrice());
        list.add(stock.getChangePricePercentage());
        list.add(stock.getLastVolume());
        list.add(stock.getBuyPrice());
        list.add(stock.getBuyQuantity());
        list.add(stock.getSellPrice());
        list.add(stock.getSellQuantity());
        list.add(alert.getFallBelow());
        list.add(alert.getRiseAbove());
        return list;
    }
    
    @Override
    public int findColumn(String columnName) {
        return columnNameMapping.get(columnName);
    }

    /**
     * Returns row index for the stock. Comparison will be done based on its
     * stock code.
     *
     * @param stock The stock
     * @return Row index for the stock. -1 will be returned if stock cannot be
     * found.
     */
    public int findRow(Stock stock) {
        Integer row = rowStockCodeMapping.get(stock.getCode());
        if (row != null) {
            return row;
        }
        return -1;
    }
    
    private List<List<Object>> tableModel = new ArrayList<List<Object>>();
    private List<List<Object>> oldTableModel = new ArrayList<List<Object>>();
    private List<Stock> stocks = new ArrayList<Stock>();
    private List<StockAlert> alerts = new ArrayList<StockAlert>();
    // Used to get column by Name in fast way.
    private Map<String, Integer> columnNameMapping = new HashMap<String, Integer>();
    // Used to get row by Stock in fast way.
    private Map<Code, Integer> rowStockCodeMapping = new HashMap<Code, Integer>();
    private static final String[] columnNames;
    private static final String[] languageIndependentColumnNames;
    private static final Class[] columnClasses = {
        Code.class,
        Symbol.class,
        Double.class,
        Double.class,
        Double.class,
        Double.class,
        Double.class,
        // TODO: CRITICAL LONG BUG REVISED NEEDED.
        Long.class,
        Double.class,
        Double.class,
        Integer.class,
        Double.class,
        Integer.class,
        Double.class,
        Integer.class,
        Double.class,
        Double.class
    };

    static {
        final String[] tmp = {
            GUIBundle.getString("MainFrame_Code"),
            GUIBundle.getString("MainFrame_Symbol"),
            GUIBundle.getString("MainFrame_Prev"),
            GUIBundle.getString("MainFrame_Open"),
            GUIBundle.getString("MainFrame_Last"),
            GUIBundle.getString("MainFrame_High"),
            GUIBundle.getString("MainFrame_Low"),
            GUIBundle.getString("MainFrame_Vol"),
            GUIBundle.getString("MainFrame_Chg"),
            GUIBundle.getString("MainFrame_ChgPercentage"),
            GUIBundle.getString("MainFrame_LVol"),
            GUIBundle.getString("MainFrame_Buy"),
            GUIBundle.getString("MainFrame_BQty"),
            GUIBundle.getString("MainFrame_Sell"),
            GUIBundle.getString("MainFrame_SQty"),
            GUIBundle.getString("MainFrame_FallBelow"),
            GUIBundle.getString("MainFrame_RiseAbove")
        };
        final GUIBundleWrapper guiBundleWrapper = GUIBundleWrapper.newInstance(Language.INDEPENDENT);        
        final String[] tmp2 = {
            guiBundleWrapper.getString("MainFrame_Code"),
            guiBundleWrapper.getString("MainFrame_Symbol"),
            guiBundleWrapper.getString("MainFrame_Prev"),
            guiBundleWrapper.getString("MainFrame_Open"),
            guiBundleWrapper.getString("MainFrame_Last"),
            guiBundleWrapper.getString("MainFrame_High"),
            guiBundleWrapper.getString("MainFrame_Low"),
            guiBundleWrapper.getString("MainFrame_Vol"),
            guiBundleWrapper.getString("MainFrame_Chg"),
            guiBundleWrapper.getString("MainFrame_ChgPercentage"),
            guiBundleWrapper.getString("MainFrame_LVol"),
            guiBundleWrapper.getString("MainFrame_Buy"),
            guiBundleWrapper.getString("MainFrame_BQty"),
            guiBundleWrapper.getString("MainFrame_Sell"),
            guiBundleWrapper.getString("MainFrame_SQty"),
            guiBundleWrapper.getString("MainFrame_FallBelow"),
            guiBundleWrapper.getString("MainFrame_RiseAbove")
        };
        
        columnNames = tmp;
        languageIndependentColumnNames = tmp2;
    }

    @Override
    public String getLanguageIndependentColumnName(int columnIndex) {
        return languageIndependentColumnNames[columnIndex];
    }
}
