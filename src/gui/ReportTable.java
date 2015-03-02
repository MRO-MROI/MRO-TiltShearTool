package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;

import util.NumberObserver;

import data.AlignmentMath;

public class ReportTable extends JPanel{
	JTable table;
	JScrollPane tableSP;
	
	NumberObserver[] dataObservers;
	boolean scrollPanePaused=false;
	
	DefaultTableCellRenderer headerRenderer;
	
	boolean accessLock = false;
	
	public ReportTable(String[] columnNames){
		buildTable(columnNames);
	}
	
	private void buildTable(String[] columnNames){
		
		//configure the actual JTable
		table = new JTable(new TableModel(columnNames)){
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column){
				Component comp = super.prepareRenderer(renderer, row, column);
				
				if(!isCellSelected(row,column)){
					comp.setBackground(((TableModel)table.getModel()).getRowColor(row));	
				}
				
				return comp;
			}			
		};
		
		headerRenderer = new DefaultTableCellRenderer(){
			@Override
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				JLabel comp = new JLabel();
				Font f = comp.getFont().deriveFont(Font.BOLD);
				comp.setFont(f);
				comp.setText(value.toString());
				comp.setBackground( SystemColor.control );
				comp.setHorizontalAlignment(SwingConstants.CENTER);
				comp.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
				return comp;
			}			
		};
		
		for(int i=0;i<table.getColumnCount();i++){
			table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);	
		}
				
		table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        table.setCellSelectionEnabled(true);
//        table.setDefaultRenderer(Object.class, new Renderer());
        for(int i=0;i<table.getColumnCount();i++){
        	table.getColumnModel().getColumn(i).setCellRenderer(new Renderer());
        }
      
        //Make table cell start the editor when typing started over editable cell
        table.setSurrendersFocusOnKeystroke(true);
        table.setPreferredScrollableViewportSize(new Dimension(800,230));
        table.getTableHeader().setReorderingAllowed(false);
        
        table.getColumnModel().addColumnModelListener( new ColumnChangeListener() );
        //done
        
        //Add the table to a scroll panel
        tableSP = new JScrollPane(table);
        MouseInputAdapter mouseAdapter = new MouseInputAdapter(){
        		@Override
    			public void mousePressed(MouseEvent e){
        			scrollPanePaused = true;
    				return;
        		}

    			@Override 
    			public void mouseClicked(MouseEvent e){
    				int cnt = e.getClickCount();
    				if(cnt==2){
    					scrollPanePaused = false;
    				}
    			}

    			@Override
    			public void mouseWheelMoved(MouseWheelEvent e){
    				mousePressed(e);
    			}
        };
        		
        tableSP.getVerticalScrollBar().addMouseListener(mouseAdapter);
//    	tableSP.getVerticalScrollBar().addMouseWheelListener(mouseAdapter);
    	
        //Add components to main JPanel
        setLayout( new BoxLayout(this,BoxLayout.Y_AXIS) );
        add(tableSP);        
	}
	
	public void resetTable(String[] colNames){
		((TableModel)table.getModel()).setDataVector(new Object[0][colNames.length],colNames);
	}
	
	public void setDataObservers(NumberObserver[] dataObservers){
		this.dataObservers = dataObservers;
	}
	
	/**
	 * Add a row to the table.  
	 * This method will add a row with first column value set to 'rowLabel' string
	 * the remaining column data is obtained using the dataObserverers array.
	 * @param rowLabel
	 */
	public void updateTable(String rowLabel){
		Object[] rowData=new Object[1+dataObservers.length];
		
		rowData[0]=rowLabel;
		for(int i=1;i<rowData.length;i++){
			rowData[i] = new Double(dataObservers[i-1].getDouble());
		}
		
		((TableModel)table.getModel()).addRow(rowData);
		if(!scrollPanePaused){
			Runnable updateScrollPane = new Runnable(){
				public void run(){
					table.scrollRectToVisible( 
							table.getCellRect( ((TableModel)table.getModel()).getRowCount()-1,0,false) );
				}
			};
			SwingUtilities.invokeLater( updateScrollPane ); 
		}
	}
	
	/**
	 * Add a row of data to the table.
	 * This method allows you to specify the column data to add to the next row.
	 * @param rowData Column data to use for this row. 
	 */
	public void updateTable(Object[] rowData){
		((TableModel)table.getModel()).addRow(rowData);
		if(!scrollPanePaused){
			Runnable updateScrollPane = new Runnable(){
				public void run(){
					table.scrollRectToVisible( 
							table.getCellRect( ((TableModel)table.getModel()).getRowCount()-1,0,false) );
				}
			};
			SwingUtilities.invokeLater( updateScrollPane ); 
		}
	}
		
	private class Renderer extends DefaultTableCellRenderer{
		NumberFormat f;
		
		public Renderer(){
			super();
			f = NumberFormat.getNumberInstance();
			f.setMaximumFractionDigits(7);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {		
			Component comp = super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
			
			return comp;
		}
		
		@Override
		protected void setValue(Object value){
			if(value instanceof Double){
				setText( f.format(value) );
			}else if(value instanceof NumberObserver){
				setText( f.format(((NumberObserver)value).getDouble()) );
			}else{
				super.setValue(value);
			}			
		}
	}
	
	private class TableModel extends DefaultTableModel{
		private Color oddRowColor=Color.getHSBColor(.0f,.0f,1.0f);
		private Color evenRowColor=Color.getHSBColor(.0f,.0f,.96f);
			
		public TableModel(String[] columnNames){
			super(new Object[0][columnNames.length], columnNames);
		}		
		
		public void resetTable(Object[][] data, Object[][] columnNames){
			synchronized(table){
				super.setDataVector(data,columnNames);
			}
		}
		
		public void setColumnNames(String[] columnNames){
			super.setColumnIdentifiers(columnNames);
		}
		
		public Color getRowColor(int row) {
			if( (row % 2) == 0 ){
				return evenRowColor;
			}else{
				return oddRowColor;
			}
		}

        /* JTable uses this method to determine the default renderer/
         * editor for each cell.
         */
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public boolean isCellEditable(int row, int col) {
        	return false;
        }

        @Override
        public void addRow(Object[] rowData) {
        	synchronized(table){
        		super.addRow(rowData);
        	}
        }
	}
	
	private class ColumnChangeListener implements TableColumnModelListener{
		@Override
		public void columnAdded(TableColumnModelEvent evt) {
			table.getColumnModel().getColumn(evt.getToIndex()).setHeaderRenderer(headerRenderer);
		}

		@Override
		public void columnMarginChanged(ChangeEvent arg0) {
		}

		@Override
		public void columnMoved(TableColumnModelEvent arg0) {
		}

		@Override
		public void columnRemoved(TableColumnModelEvent arg0) {
		}

		@Override
		public void columnSelectionChanged(ListSelectionEvent arg0) {
		}		
	}
	
	public static void main(String[] args){
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		final ReportTable rtable = new ReportTable(new String[] {"",""});
		Container mcp = frame.getContentPane();
		mcp.add(rtable);
		
		frame.pack();
		frame.setVisible(true);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		
		}

		rtable.setDataObservers(new NumberObserver[] {AlignmentMath.Hxdt,AlignmentMath.Hydt,AlignmentMath.Hxds,AlignmentMath.Hyds});

		Thread updater = new Thread(new Runnable(){
			public void run(){
				int i=1;
				
				rtable.resetTable(new String[] {"","Hdxt","Hdyt","Hdxs","Hdys"});
				
				try {
					System.out.println("Sleeping");
					Thread.sleep(2000);
				} catch (InterruptedException e) {				
				}
				
				while(true){
					AlignmentMath.Hxdt.setNumber(i);
					AlignmentMath.Hydt.setNumber(i);
					AlignmentMath.Hxds.setNumber(i);
					AlignmentMath.Hyds.setNumber(i);
					
					rtable.updateTable(new String("row "+i));
					
					i++;
					
					if(i>25){
						rtable.resetTable(new String[] {"","Hdxt2","Hdyt2","Hdxs2","Hdys2"});
						i=0;
					}
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						break;
					}
				}
				
			}
		});
		updater.start();
		
	}
	
}
