package gui;

import impl.Logger;
import impl.MeanStepSizeCalculator;
import impl.PIControl;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.NumberFormat;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.LayoutStyle;

import data.AlignmentMath;
import data.SetupData;
import xenimaq.NativeImageException;
import xenimaq.NativeImageImpl;
import agilis.ActuatorInterface;
import agilis.ActuatorPanel;
import agilis.AgilisException;

/**
 * AlignPanel is a JPanel component that implements Alignment specific routines and variables.
 * 
 * This includes:
 * - Buttons for setting Tilt/Shear reference centroids
 * - Variables for setting up coaddition
 * - Buttons for starting/stopping automated alignment
 * - Fields for setting PI loop used to calculate alignment corrections
 *
 */
final public class AlignPanel extends JPanel implements ActionListener, PropertyChangeListener {
	
	ActuatorInterface ai;
	NativeImageImpl tiltImg;
	NativeImageImpl shearImg;
	
	JButton getstep;
	JButton stopstep;
	JButton steploop;
	JButton startpi;
	JButton stoppi;
	JButton settref;
	JButton setsref;
	JButton setHome;
	
	JCheckBox autoErrorCorrectcb;
	JCheckBox singlecb;
	JCheckBox coaddcb; 
	JCheckBox setHomecb; 
	
	JFormattedTextField igain, pgain, nsteps, ampl, errthresh, stepInterval; 
	JFormattedTextField coaddSecs, coaddN;

	PIControl pictrl;
	MeanStepSizeCalculator sscalc;
		
	boolean coadd=false;
	boolean autoPILoop=false;
	
	//Only used for testing GUI layout
	private AlignPanel(){
		buildPanel();
	}
	
	public AlignPanel(ActuatorInterface ai, NativeImageImpl tiltImg, NativeImageImpl shearImg, ReportTable reportTable){
		this.ai = ai;
		this.tiltImg = tiltImg;
		this.shearImg = shearImg;
	
		buildPanel();
		pictrl = new PIControl(ai, shearImg, tiltImg, reportTable);
		sscalc = new MeanStepSizeCalculator(ai, shearImg, tiltImg, reportTable);
	}
	
	public void buildPanel(){
		setHome = new JButton("Press");
		setHome.addActionListener(this);
		setHome.setVisible(false);
		
		autoErrorCorrectcb = new JCheckBox("Error Correcting");
		autoErrorCorrectcb.addActionListener(this);
		autoErrorCorrectcb.setSelected(false); 
		
		setHomecb = new JCheckBox("Set Home");
		setHomecb.addActionListener(this);
		setHomecb.setSelected(false); 
		setHomecb.setVisible(false);//change to true once there are limit switches 
		
		getstep = new JButton("Avg. Step");
		getstep.addActionListener(this);
//		getstep.setPreferredSize(new Dimension(80,15));
		
		stopstep = new JButton("Stop");
		stopstep.addActionListener(this);
		
		steploop = new JButton("Step");
		steploop.addActionListener(this);
		steploop.setEnabled(true);
//		steploop.setPreferredSize(new Dimension(80,15));
		
		singlecb = new JCheckBox("Single Step");
		singlecb.addActionListener(this);
		singlecb.setSelected(true);
		
		coaddcb = new JCheckBox("Coadd");
		coaddcb.addActionListener(this);
		coaddcb.setSelected(false);
		
		startpi = new JButton("Start PI");
		startpi.addActionListener(this);
//		startpi.setPreferredSize(new Dimension(80,15));
		
		stoppi = new JButton("Stop PI");
		stoppi.addActionListener(this);
		
		settref = new JButton("Tilt");
		settref.addActionListener(this);
//		settref.setEnabled(coaddcb.isSelected());
		settref.setToolTipText("");
		
		setsref = new JButton("Shear");
		setsref.addActionListener(this);
//		setsref.setEnabled(coaddcb.isSelected());
		setsref.setToolTipText("");
		
        NumberFormat f=NumberFormat.getNumberInstance();
        igain = new JFormattedTextField(f);
        igain.setValue(new Double(0.0));
        igain.addPropertyChangeListener("value",this);
        igain.setPreferredSize(new Dimension(50,15));
        
        f=NumberFormat.getNumberInstance();
        pgain = new JFormattedTextField(f);
        pgain.setValue(new Double(1.0));
        pgain.addPropertyChangeListener("value",this);  
        pgain.setPreferredSize(new Dimension(50,15));
        
        f=NumberFormat.getNumberInstance();
        nsteps = new JFormattedTextField(f);
        nsteps.setValue(new Integer(40));
        nsteps.addPropertyChangeListener("value",this);
        nsteps.setPreferredSize(new Dimension(50,15));
        nsteps.setToolTipText("Number of steps to average.");
        
        f=NumberFormat.getNumberInstance();
        ampl = new JFormattedTextField(f);
        ampl.setValue(new Integer(35));
        ampl.addPropertyChangeListener("value",this);
        ampl.setPreferredSize(new Dimension(50,15));
        ampl.setToolTipText("Step amplitude");
        
        f=NumberFormat.getNumberInstance();
        f.setMaximumFractionDigits(8);
        errthresh = new JFormattedTextField(f);
        errthresh.setValue(new Double(0.001));
        errthresh.addPropertyChangeListener("value",this);
        errthresh.setPreferredSize(new Dimension(50,15));
        
        f=NumberFormat.getNumberInstance();
        stepInterval = new JFormattedTextField(f);
        stepInterval.setValue(new Integer(150));
        stepInterval.addPropertyChangeListener("value",this);
        stepInterval.setPreferredSize(new Dimension(50,15));
        stepInterval.setToolTipText("Time between steps, milliseconds");
        
        f = NumberFormat.getNumberInstance();
		coaddSecs = new JFormattedTextField(f);
		coaddSecs.setPreferredSize(new Dimension(40,20));
		
		f = NumberFormat.getNumberInstance();
		coaddN = new JFormattedTextField(f);
		coaddN.setPreferredSize(new Dimension(40,20));
		coaddN.setValue(new Integer(30));
		coaddSecs.setValue(new Integer(30));
		coaddSecs.addPropertyChangeListener("value",this);
		coaddN.addPropertyChangeListener("value",this);
		
		JLabel setrefl = new JLabel("Set Refs");
		JLabel coaddNl = new JLabel("Coadd #");
		JLabel coaddSecsl = new JLabel("Interval [secs]");
		
        JLabel pgainl = new JLabel("P Gain");
        JLabel igainl = new JLabel("I Gain");
        JLabel nstepsl = new JLabel("# of Steps");
        JLabel ampll = new JLabel("Step Ampl.");
        JLabel errthreshl = new JLabel("Error Thresh.");
        JLabel stepIntervall = new JLabel("Step Interval");
        
        GroupLayout layout=new GroupLayout(this);
		setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		
		layout.setHorizontalGroup(
			layout.createSequentialGroup()
				.addGroup(
					layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(coaddcb)
						.addComponent(coaddNl)
						.addComponent(coaddSecsl)
						.addComponent(setrefl)
						.addComponent(getstep, GroupLayout.PREFERRED_SIZE, 
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(nstepsl)
						.addComponent(ampll)
						.addComponent(startpi, GroupLayout.PREFERRED_SIZE, 
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(singlecb, GroupLayout.PREFERRED_SIZE, 
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(pgainl)
						.addComponent(igainl)
						.addComponent(errthreshl)
						.addComponent(stepIntervall)
						.addComponent(setHomecb)
				)
				.addGroup(
					layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(autoErrorCorrectcb,GroupLayout.PREFERRED_SIZE, 
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(coaddN,GroupLayout.PREFERRED_SIZE, 
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(coaddSecs,GroupLayout.PREFERRED_SIZE, 
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)		
						.addComponent(settref,GroupLayout.PREFERRED_SIZE, 
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(setsref,GroupLayout.PREFERRED_SIZE, 
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(nsteps, GroupLayout.PREFERRED_SIZE, 
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(stoppi, GroupLayout.PREFERRED_SIZE, 
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(ampl, GroupLayout.PREFERRED_SIZE, 
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(steploop, GroupLayout.PREFERRED_SIZE, 
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(pgain, GroupLayout.PREFERRED_SIZE, 
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(igain, GroupLayout.PREFERRED_SIZE, 
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(errthresh, GroupLayout.PREFERRED_SIZE, 
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(stepInterval, GroupLayout.PREFERRED_SIZE, 
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(setHome)
				)	
		);
		
		layout.setVerticalGroup(
				layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(coaddcb).addComponent(autoErrorCorrectcb))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(coaddNl).addComponent(coaddN))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(coaddSecsl).addComponent(coaddSecs))
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
	                 20, 20)		
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(setrefl).addComponent(settref))
				.addComponent(setsref)
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,20, 20)
				.addComponent(getstep)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(nstepsl).addComponent(nsteps))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(ampll).addComponent(ampl))
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,20, 20)
	            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
	            		.addComponent(startpi).addComponent(stoppi))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(singlecb).addComponent(steploop))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(pgainl).addComponent(pgain))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(igainl).addComponent(igain))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(errthreshl).addComponent(errthresh))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(stepIntervall).addComponent(stepInterval))
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(setHomecb).addComponent(setHome))
		);
		
	}
	
	public void actionPerformed(ActionEvent ae){
		Object src = ae.getSource();
		
		if(src == getstep){
			startStepCalc();
		}else if(src == stopstep){
			stopStepCalc();
		}else if (src == autoErrorCorrectcb){
			pictrl.setErrorCheckingStatus(autoErrorCorrectcb.isSelected());
		}else if(src == startpi){
			startPILoop();
		}else if(src==stoppi){
			stopPILoop();
		}else if(src == steploop){
			stepPILoop();
		}else if(src == singlecb){
			if(singlecb.isSelected()){
				autoPILoop = false;
				steploop.setEnabled(true);
			}else{
				autoPILoop = true;
				steploop.setEnabled(false);
			}
		}else if(src == coaddcb){
			
			if(coaddcb.isSelected()){
				try {
					//Make sure to close the image if it was
					//started by the user using 'Grab' button, 
					//co-add process requires software
					//control of image.
					tiltImg.closeImage();
					shearImg.closeImage();
				} catch (NativeImageException e) {
					coaddcb.setSelected(false);
					JOptionPane.showMessageDialog(null,e.getMessage());
				}
			}else{
				
			}
			
			coadd = coaddcb.isSelected();
//			setref.setEnabled(coaddcb.isSelected());
			
		}else if(src == settref){
			setRef(tiltImg);
		}else if(src == setsref){
			setRef(shearImg);
		}
		else if(src == setHomecb){
			setHome.setVisible(setHomecb.isSelected());
		}else if(src == setHome){
			setHome.setVisible(false);
			setHomecb.setSelected(false);
			try {
				pictrl.setHome();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (AgilisException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		Object src=evt.getSource();
		
		if (evt.getPropertyName().equals("value")) {
			if (src == errthresh){
					//TODO implement
			}else if(src==coaddSecs || src==coaddN){
				tiltImg.setupCoadd( ((Number)coaddN.getValue()).intValue(),
						((Number)coaddSecs.getValue()).intValue() );
				shearImg.setupCoadd( ((Number)coaddN.getValue()).intValue(),
						((Number)coaddSecs.getValue()).intValue() );
			}else if(src==pgain || src==igain){
				setupPILoop(((Number)pgain.getValue()).doubleValue(),
						((Number)igain.getValue()).doubleValue());
			}else if(src==stepInterval){
				pictrl.setStepInterval(((Number)stepInterval.getValue()).doubleValue());
			}
		}
		
	}
	
	/* Implementation methods go down here */
	public void setRef(NativeImageImpl img) {
		if(img==null){
			return;
		}
		
		try{
			if(coadd){
				img.singleCoaddedImage(true);
			}
			
			img.setRefCentroid();
		}catch(NativeImageException ex){
			JOptionPane.showMessageDialog(null, ex.getMessage());
		}catch(InterruptedException ex){
			//Do nothing
		}
	}
	
	public void startStepCalc(){
		SetupData.currentMountStepAmplitude = ((Number)ampl.getValue()).intValue();
		sscalc.setSetup(((Number)nsteps.getValue()).intValue(),
				((Number)ampl.getValue()).intValue(), coadd);
		sscalc.startCalc();	
	}
	
	public void stopStepCalc(){
		sscalc.stopCalc();
	}
	
	public void setupPILoop(double Kp, double Ki){
		pictrl.setupLoop(Kp,Ki);
	}
	
	public void startPILoop(){
		pictrl.setupLoop( ((Number)pgain.getValue()).doubleValue(),
				((Number)igain.getValue()).doubleValue() );
		if(!autoPILoop){
			try{
				pictrl.startManualLoop( ((Number)ampl.getValue()).intValue(), coadd );
			}catch(NativeImageException ex){
				JOptionPane.showMessageDialog(null, ex.getMessage());
			}
		}else{
			pictrl.startAutoLoop( ((Number)ampl.getValue()).intValue(), coadd );
		}	
	}
	
	public void stepPILoop(){
		try {
			pictrl.singleStep();
		} catch (Exception ex) {
			if( ex instanceof NativeImageException ) {
				tiltImg.termImage();
				shearImg.termImage();
				JOptionPane.showMessageDialog(null, ex.getMessage());
			} else if( ex instanceof InterruptedException ) {
				//Do nothing
			}
		}
	}
	
	public void stopPILoop(){
		pictrl.stopLoop();
	}
	
	public boolean isCoaddOn(){
		return coadd;
	}
	
	public void terminate(){
		stopStepCalc();
		stopPILoop();
	}
	
	public static void main(String[] args){
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		AlignPanel ap = new AlignPanel();
		
		Container mcp = frame.getContentPane();
		mcp.add(ap);
		
		frame.pack();
		frame.setVisible(true);
	}
	
}
