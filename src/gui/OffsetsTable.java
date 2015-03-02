package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.text.NumberFormat;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.NumberFormatter;

import util.NumberObserver;

import data.AlignmentMath;

public class OffsetsTable extends JPanel{
	private JTable table;
	private JScrollPane tableSP;
	
//	private JComboBox unitList;
	
	public OffsetsTable(){
		super();
		buildTable();
		
		Thread t = new Thread(new Runnable(){
			public void run(){
				while(true){
					try{
						Thread.sleep(1000);					
					}catch(InterruptedException ex){
						
					}
					repaint();
				}
			}
		});
		
		t.start();
	}
	
	public void buildTable(){
//		unitList = new JComboBox();
//		unitList.addItem("px");
		table = new JTable(new TableModel()){
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column){
				Component comp = super.prepareRenderer(renderer, row, column);
				
				if(!isCellSelected(row,column)){
					//even index, selected or not selected
					if (row % 2 == 0 ) {
						comp.setBackground(Color.getHSBColor(.5f,.10f,.99f));
					}else{
						comp.setBackground(Color.getHSBColor(.5f,.30f,.99f));
					}
				}
				return comp;
			}
		};
		
		table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        table.setCellSelectionEnabled(true);
        table.setDefaultRenderer(Object.class, new Renderer());
        table.setPreferredScrollableViewportSize(new Dimension(260,150));
        table.getTableHeader().setReorderingAllowed(false);
        
        tableSP = new JScrollPane(table);
        
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        
        add(tableSP);       
	}
	
	private class Renderer extends DefaultTableCellRenderer{
		NumberFormat f;
		
		public Renderer(){
			super();
			f = NumberFormat.getNumberInstance();
			f.setMaximumFractionDigits(4);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			
			Component comp = super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
			
			if(column==0){
				setFont( getFont().deriveFont(Font.BOLD) );
			}
			
			return comp;
		}
		
		@Override
		protected void setValue(Object value){
			if(value instanceof NumberObserver){
				setText( f.format(((NumberObserver)value).getDouble()) );
			}else{
				super.setValue(value);
			}
		}
	}
		
	private class TableModel extends AbstractTableModel{
		private String[] columnNames = {"<html><b>Offset</b></html>", "<html><b>Value [mm]</b></html>"};
		private Object[][] data = {
				{"Hxdt",AlignmentMath.Hxdt},
				{"Hydt",AlignmentMath.Hydt},
				{"Hxds",AlignmentMath.Hxds},
				{"Hyds",AlignmentMath.Hyds},
				{"Starting Hxdt",AlignmentMath.Hxdt_r},
				{"Starting Hydt",AlignmentMath.Hydt_r},
				{"Starting Hxds",AlignmentMath.Hxds_r},
				{"Starting Hyds",AlignmentMath.Hyds_r}
				};
		
//		private String[] columnNames = {"<html><b>Hxdt</b></html>", "<html><b>Hydt</b></html>", 
//				"<html><b>Hxds</b></html>", "<html><b>Hyds</b></html>", 
//				"<html><b>Hxdt_r</b></html>", "<html><b>Hydt_r</b></html>", 
//				"<html><b>Hxds_r</b></html>", "<html><b>Hyds_r</b></html>"};
//		
//		private Object[][] data = 
//				{{AlignmentMath.Hxdt,
//				AlignmentMath.Hydt,
//				AlignmentMath.Hxds,
//				AlignmentMath.Hyds,
//				AlignmentMath.Hxdt_r,
//				AlignmentMath.Hydt_r,
//				AlignmentMath.Hxds_r,
//				AlignmentMath.Hyds_r
//				}};
		
		public TableModel(){
		
		}
		
		public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            return data[row][col];
        }

        /* JTable uses this method to determine the default renderer/
         * editor for each cell.
         */
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        /*
         * Don't need to implement this method unless your table's
         * editable.
         */
        public boolean isCellEditable(int row, int col) {
           return false;
        }

        /*
         * Don't need to implement this method unless your table's
         * data can change.
         */
        public void setValueAt(Object value, int row, int col) {
//            data[row][col] = value;
//            fireTableCellUpdated(row, col);
        }
	}
	
	public static void main(String[] args){
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		OffsetsTable table = new OffsetsTable();
		
		Container mcp = frame.getContentPane();
		mcp.add(table);
		
		frame.pack();
		frame.setVisible(true);
	}
}
