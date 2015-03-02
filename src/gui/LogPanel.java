package gui;

import impl.Logger;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPanel;

final public class LogPanel extends JPanel implements ActionListener{
	
	JCheckBox centroids, pixOffsets, thetaCorr, stepSizes, piStatus;
	JFormattedTextField centroidsRate, pixOffsetsRate, thetaCorrRate;
	
	Logger log;
	
	//Only used for testing GUI
	private LogPanel(){
		buildPanel();
	}
	
	public LogPanel(Logger log){
		this.log = log;
		buildPanel();
	}
	
	public void buildPanel(){
		centroids = new JCheckBox("Centroids");
		centroids.addActionListener(this);
		pixOffsets= new JCheckBox("Pixel Offsets");
		pixOffsets.addActionListener(this);
		thetaCorr= new JCheckBox("Theta Corrections");
		thetaCorr.addActionListener(this);
		stepSizes= new JCheckBox("Current step sizes"); 
		stepSizes.addActionListener(this);
		piStatus= new JCheckBox("PI loop status");
		piStatus.addActionListener(this);	
		
		NumberFormat f = NumberFormat.getNumberInstance();
		centroidsRate = new JFormattedTextField(f);
		centroidsRate.setValue(new Double(1));
		centroidsRate.setPreferredSize(new Dimension(40,10));
		
		f = NumberFormat.getNumberInstance();
		pixOffsetsRate = new JFormattedTextField(f);
		pixOffsetsRate.setValue(new Double(1));
		pixOffsetsRate.setPreferredSize(new Dimension(40,10));
		
		f = NumberFormat.getNumberInstance();
		thetaCorrRate = new JFormattedTextField(f);	
		thetaCorrRate.setValue(new Double(1));
		thetaCorrRate.setPreferredSize(new Dimension(40,10));
		
		GroupLayout layout=new GroupLayout(this);
		setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		
		layout.setHorizontalGroup(
			layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(centroids)
					.addComponent(pixOffsets)
					.addComponent(thetaCorr)
					.addComponent(stepSizes)
					.addComponent(piStatus)
				)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(centroidsRate,GroupLayout.PREFERRED_SIZE, 
							GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addComponent(pixOffsetsRate,GroupLayout.PREFERRED_SIZE, 
							GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addComponent(thetaCorrRate,GroupLayout.PREFERRED_SIZE, 
							GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				)
		);
		
		layout.setVerticalGroup(
			layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(centroids).addComponent(centroidsRate))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(pixOffsets).addComponent(pixOffsetsRate))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(thetaCorr).addComponent(thetaCorrRate))
				.addComponent(stepSizes)
				.addComponent(piStatus)
		);
		
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		Object src = ae.getSource();
		double dT;
		
		if(src == centroids){
			if(centroids.isSelected()){
				dT = ((Number)centroidsRate.getValue()).doubleValue();
				log.setupCentroids(dT,true);
			}else{
				log.setupCentroids(0,false);
			}
		}else if(src == pixOffsets){
			if(pixOffsets.isSelected()){
				dT = ((Number)pixOffsetsRate.getValue()).doubleValue();
				log.setupPixOffs(dT,true);
			}else{
				log.setupPixOffs(0,false);
			}
		}else if(src == thetaCorr){
			if(thetaCorr.isSelected()){
				dT = ((Number)thetaCorrRate.getValue()).doubleValue();
				log.setupThetaCorrs(dT,true);
			}else{
				log.setupThetaCorrs(0,false);
			}
		}else if(src == stepSizes){
			log.getStepSizes();
			stepSizes.setSelected(false);
		}else if(src == piStatus){
			log.getPIStatus();
			piStatus.setSelected(false);
		}
		
	}
	
	public void terminate(){
		log.setupCentroids(0,false);
		log.setupPixOffs(0,false);
		log.setupThetaCorrs(0,false);
	}
	
	public static void main(String[] args){
		JFrame f = new JFrame("LogPanel");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		Container c=f.getContentPane();
		c.add(new LogPanel());
		
		f.pack();
		f.setVisible(true);
	}
}
