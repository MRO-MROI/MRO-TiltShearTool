package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.text.NumberFormat;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import util.NumberObserver;

import data.AlignmentMath;

public class ThetasTable extends JPanel{
	private JTable table;
	private JScrollPane tableSP;
	
	public ThetasTable(){
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
        table.setPreferredScrollableViewportSize(new Dimension(260,50));
        
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
				comp.setFont( getFont().deriveFont(Font.BOLD) );
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
		private String[] columnNames = {"", "<html><b>thetaX</b></html>","<html><b>thetaY</b></html>"};
		private Object[][] data = {
				{"SY1",AlignmentMath.acm1xObs,AlignmentMath.acm1yObs},
				{"SY2",AlignmentMath.acm2xObs,AlignmentMath.acm2yObs},	
				};

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
		
		ThetasTable table = new ThetasTable();
		
		Container mcp = frame.getContentPane();
		mcp.add(table);
		
		frame.pack();
		frame.setVisible(true);
	}
}
