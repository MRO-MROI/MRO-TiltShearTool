package gui;

import impl.DefinedRoutines;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.NumberFormatter;
import javax.swing.text.JTextComponent;

import agilis.ActuatorInterface;

import util.NumberObserver;

import data.AlignmentMath;

public class CorrectionTest extends JPanel implements ActionListener{
	private JTable table;
	private JScrollPane tableSP;
	
	Double thetaXs1=new Double(0);
	Double thetaYs1=new Double(0);
	Double thetaXs2=new Double(0);
	Double thetaYs2=new Double(0);
	
	JButton startMotionb;
	
	DefinedRoutines routines;
	
	public CorrectionTest(DefinedRoutines routines){
		super();
		this.routines = routines;
		
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
		//configure the actual JTable
		table = new JTable(new TableModel()){
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column){
				Component comp = super.prepareRenderer(renderer, row, column);
				
				if(!isCellSelected(row,column)){
					comp.setBackground(((TableModel)table.getModel()).getRowColor(row));	
				}
				
				return comp;
			}
			
			@Override
			public boolean editCellAt(int row, int column, EventObject e){
				Boolean b = super.editCellAt(row,column,e);
				Component editor = getEditorComponent();
				
				if (editor == null || !(editor instanceof JTextComponent)){
				
				}else{
					((JTextComponent)editor).selectAll();
				}
					
				return b;
			}
			
		};
		
		table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        table.setCellSelectionEnabled(true);
        table.setDefaultRenderer(Object.class, new Renderer());
        for(int i=1; i<=2;i++){
        	table.getColumnModel().getColumn(i).setCellEditor(new DoubleEditor());
        }
        //Make table cell start the editor when typing started over editable cell
        table.setSurrendersFocusOnKeystroke(true);
        table.setPreferredScrollableViewportSize(new Dimension(320,230));
        table.getTableHeader().setReorderingAllowed(false);
        //done
        
        //Add the data change listener to the table
        new DataChangeListener(table);
        //Add the table to a scroll panel
        tableSP = new JScrollPane(table);
        
        JLabel smlabel = new JLabel("Apply Corrections:");
        startMotionb = new JButton("Go");
        startMotionb.addActionListener(this);
        
        JPanel subp = new JPanel();
        subp.add(smlabel);
        subp.add(startMotionb);
        
        //Add components to main JPanel
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        add(tableSP);   
        add(subp);
	}
	
	@Override
	public void actionPerformed(ActionEvent evt) {
		Object src = evt.getSource();
		
		if(src == startMotionb){
			routines.setReferencePoints();
			routines.moveMounts(AlignmentMath.scm1ax1,AlignmentMath.scm1ax2,
					AlignmentMath.scm2ax1,AlignmentMath.scm2ax2);
		}
	}
	
	public void terminate(){
		if(routines!=null){
			routines.abortMotion();
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
	
	//TODO: Actually implement the calculation for step sizes, figure out way to
	//validate input values and then call calculation method.
	//Figure out how to use this to physically move mounts. 
	private class TableModel extends AbstractTableModel{
		private String[] columnNames = {"", "<html><b>SY1</b></html>","<html><b>SY2</b></html>"};
		private Object[][] data = {
				{"Step Sizes","",""},
				{"1-",AlignmentMath.ssm1ax1nObs,AlignmentMath.ssm2ax1nObs},	
				{"1+",AlignmentMath.ssm1ax1pObs,AlignmentMath.ssm2ax1pObs},	
				{"2-",AlignmentMath.ssm1ax2nObs,AlignmentMath.ssm2ax2nObs},	
				{"2+",AlignmentMath.ssm1ax2pObs,AlignmentMath.ssm2ax2pObs},
				{"","",""},
				{"Corrections Input","",""},
				{"thetaX",thetaXs1,thetaXs2}, //row 7, col 1,2,
				{"thetaY",thetaYs1,thetaYs2}, //row 8 col 1,2
				{"","",""},
				{"Steps to Move","",""},
				{"Axis 1",AlignmentMath.scm1ax1Obs,AlignmentMath.scm2ax1Obs},	
				{"Axis 2",AlignmentMath.scm1ax2Obs,AlignmentMath.scm2ax2Obs},	
				};

		private Color[] rowColors={
				Color.white,
				Color.white,
				Color.white,
				Color.white,
				Color.white,
				Color.white,
				Color.getHSBColor(.5f,.10f,.99f),
				Color.getHSBColor(.5f,.10f,.99f),
				Color.getHSBColor(.5f,.10f,.99f),
				Color.white,
				Color.white,
				Color.white,
				Color.white
		};
		
		public TableModel(){
		
		}
		
		public Color getRowColor(int row){
			return rowColors[row];
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
        	if(col==0){
        		return false;
        	}
        	
        	String s = (String)getValueAt(row,0);       	
        	if(s.equals("thetaX") || s.equals("thetaY")){
        		return true;
        	}else{
        		return false;
        	}
        }

        /*
         * Don't need to implement this method unless your table's
         * data can change.
         */
        public void setValueAt(Object value, int row, int col) {
            data[row][col] = value;
            fireTableCellUpdated(row, col);
        }
	}
	
	private class DoubleEditor extends AbstractCellEditor implements TableCellEditor {
		NumberFormat f;
		Component component;
		final int editClicks=2;
		
		public DoubleEditor(){
			f=NumberFormat.getNumberInstance();
			f.setMaximumFractionDigits(7);
			f.setMaximumIntegerDigits(1);
			component = new JFormattedTextField(f);
		}

		@Override
		public boolean isCellEditable(EventObject evt){
			boolean b;
			
			b = super.isCellEditable(evt);
			
			if(evt instanceof MouseEvent){
				return ((MouseEvent)evt).getClickCount() >= editClicks;
			}
			
			return b;
		}
		
		@Override
		public Object getCellEditorValue() {
			double dvalue = ( (Number)( ((JFormattedTextField)component).getValue() ) ).doubleValue();
			
			return new Double(dvalue);
		}

		@Override
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column) {
//			System.out.println("getTableCellEditor* "+value.toString());
			((JFormattedTextField)component).setValue(value);
			
			return component;
		}
	}
	
	private class DataChangeListener implements TableModelListener{

		public DataChangeListener(JTable table){
			table.getModel().addTableModelListener(this);
		}
		
		@Override
		public void tableChanged(TableModelEvent evt) {
			thetaXs1=(Double)table.getValueAt(7,1);
			thetaYs1=(Double)table.getValueAt(8,1);
			thetaXs2=(Double)table.getValueAt(7,2);
			thetaYs2=(Double)table.getValueAt(8,2);
			
			AlignmentMath.computeStepCorrection(thetaXs1,thetaYs1,thetaXs2,thetaYs2);
		}
		
	}
	
	public static void main(String[] args){
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		CorrectionTest table = new CorrectionTest(null);
		
		Container mcp = frame.getContentPane();
		mcp.add(table);
		
		frame.pack();
		frame.setVisible(true);
	}

}
