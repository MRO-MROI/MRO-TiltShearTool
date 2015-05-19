package gui;

import impl.DefinedRoutines;
import impl.Logger;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.LayoutStyle;
import javax.swing.border.LineBorder;
import javax.swing.event.MouseInputAdapter;

import xenimaq.NativeImage;
import xenimaq.NativeImageException;
import xenimaq.NativeImageImpl;
import agilis.ActuatorInterface;
import agilis.ActuatorPanel;
import data.SetupData;

/**
 * Main GUI class, creates window and serves as the parent to all sub panels.
 * 
 * The StatusMonitor thread is used to keep track of the state of the grab button and the
 * check boxes used to open subpanels.
 * 
 * TODO JOptionPane message dialogs need to be used to print warnings/errors, at the moment critical
 * errors are printed to stderr.
 * 
 */
final public class TiltShearUI extends JFrame implements ActionListener {
	private boolean testGUI = false;
	
	ActuatorInterface ai;
	NativeImageImpl tiltImg;
	NativeImageImpl shearImg;

	String tiltCalibFile;
	String shearCalibFile;
	String colorProfile;
	
	JToggleButton grabb;
	JCheckBox tiltCB, shearCB, alignPanelCB, actuatorPanelCB, logPanelCB, corrTestCB, cameraPanelCB;
	
	JTextArea outTextArea;
	JScrollPane outScrollPane;
	
	ActuatorPanel actuatorPanel;
	JFrame actuatorPanelFrame;
	
	AlignPanel alignPanel;
	JFrame alignPanelFrame;
	
	CameraPanel cameraPanel;
	JFrame cameraPanelFrame;
	
	LogPanel logPanel;
	JFrame logPanelFrame;
	Logger logger;
	
	CorrectionTest corrTest;
	JFrame corrTestFrame;
	DefinedRoutines routines;
	
	boolean scrollPaneFocus=false;
	
	ReportTable reportTable;
	JFrame reportTableFrame;
	
	private TiltShearUI(){
		super("TiltShearUI");
		testGUI = true;
		buildUI();
	}
	
	public TiltShearUI(ActuatorInterface ai, NativeImageImpl shearImg, NativeImageImpl tiltImg){
		super("TiltShearUI");
		this.ai = ai;
		this.shearImg = shearImg;
		this.tiltImg = tiltImg;
		
		updateVars();
		routines = new DefinedRoutines(ai,shearImg,tiltImg);
	}
	
	/** Method used to acquire the setup values from SetupData and store into
	 * local variables and update setup values for all other classes that use
	 * setup values in SetupData. 
	 */
	public void updateVars(){
		tiltCalibFile = (String)SetupData.vart.get("tiltCalibFile");
		shearCalibFile = (String)SetupData.vart.get("shearCalibFile");
		colorProfile = (String)SetupData.vart.get("xenicsColorProfile");
	}
	
	public void buildUI(){
		OffsetsTable offsetsTable = new OffsetsTable();
		ThetasTable thetasTable = new ThetasTable();
		
		grabb = new JToggleButton("Grab");
		grabb.addActionListener(this);
		
		tiltCB = new JCheckBox("Tilt");
		tiltCB.addActionListener(this);
		tiltCB.setSelected(true);
		
		shearCB = new JCheckBox("Shear");
		shearCB.addActionListener(this);
		shearCB.setSelected(true);
		
		alignPanelCB = new JCheckBox("Align Panel");
		alignPanelCB.addActionListener(this);
		
		actuatorPanelCB = new JCheckBox("Actuator Panel");
		actuatorPanelCB.addActionListener(this);
		
		logPanelCB = new JCheckBox("Log Panel");
		logPanelCB.addActionListener(this);
		
		cameraPanelCB = new JCheckBox("Camera Panel");
		cameraPanelCB.addActionListener(this);
		
		corrTestCB = new JCheckBox("Correction Test");
		corrTestCB.addActionListener(this);
		corrTestCB.setSelected(false);
		
		outTextArea = new JTextArea(){
			@Override
			public void paintComponent(Graphics G){
				if(!scrollPaneFocus){
					setCaretPosition(this.getDocument().getLength());
				}
				super.paintComponent(G);
			}
		};
		
		outTextArea.setEditable(false);
		outTextArea.setFont(new Font("Times New Roman",Font.PLAIN,14));
		
		outScrollPane = new JScrollPane(outTextArea);
		outScrollPane.getVerticalScrollBar().addMouseListener(
			new MouseInputAdapter(){
				@Override
				public void mousePressed(MouseEvent e){
					JScrollBar vbar=outScrollPane.getVerticalScrollBar();
					
					if(vbar.getValueIsAdjusting() || vbar.getValue() < vbar.getMaximum()){
						scrollPaneFocus = true;
					}else if(vbar.getValue() == vbar.getMaximum()){
						scrollPaneFocus = false;
					}
					
					return;
				}
				
				@Override 
				public void mouseClicked(MouseEvent e){
					int cnt = e.getClickCount();
					if(cnt==2){
						scrollPaneFocus = false;
					}
				}
				
				@Override
				public void mouseWheelMoved(MouseWheelEvent e){
					mousePressed(null);
				}
			}
		);
		
		outScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		outScrollPane.setPreferredSize(new Dimension(600,200));
		
		//Here we redirect stdout to the JTextArea
		PrintStream out;
		try {
			out = new PrintStream( new TextAreaOutputStream(outTextArea), true, "UTF-8" );
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		System.setOut(out);
		
		//----------------------------
		//local component panel
		JPanel lpanel=new JPanel();
		GroupLayout lplayout=new GroupLayout(lpanel);
		lpanel.setLayout(lplayout);
		lplayout.setAutoCreateGaps(true);
		lplayout.setAutoCreateContainerGaps(true);
		
		lplayout.setHorizontalGroup( lplayout.createSequentialGroup()
			.addGroup( lplayout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(grabb)
					.addComponent(tiltCB)
					.addComponent(shearCB)
					.addComponent(alignPanelCB)
					.addComponent(actuatorPanelCB)
					.addComponent(logPanelCB)
					.addComponent(corrTestCB)
					.addComponent(cameraPanelCB))
		);
		
		lplayout.setVerticalGroup( lplayout.createSequentialGroup()
			.addComponent(grabb)
			.addComponent(tiltCB)
			.addComponent(shearCB)
			.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 20, 20)
			.addComponent(alignPanelCB)
			.addComponent(actuatorPanelCB)
			.addComponent(logPanelCB)
			.addComponent(corrTestCB)
			.addComponent(cameraPanelCB)
		);
		
		lpanel.setBorder( new LineBorder(Color.gray) );
		//---------------------------
	
		Container mcp = getContentPane();
		GroupLayout layout=new GroupLayout(mcp);
		mcp.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		
		GroupLayout.SequentialGroup hgroup=layout.createSequentialGroup();
		GroupLayout.SequentialGroup vgroup=layout.createSequentialGroup();
		
		//GUI Test case
		JPanel imgPanel = new JPanel();
		GridLayout g = new GridLayout(1,2);
		g.setVgap(1); g.setHgap(3);
		imgPanel.setLayout(g);
		
		if(testGUI){
			JLabel timg=new JLabel("Tilt"), simg=new JLabel("Shear");
			timg.setPreferredSize(new Dimension(640,480));
			simg.setPreferredSize(new Dimension(640,480));
			
			imgPanel.add(timg);
			imgPanel.add(simg);
		}else{
			tiltImg.setToolTipText("Tilt");
			shearImg.setToolTipText("Shear");
			
			tiltImg.setPreferredSize(new Dimension(640,480));
			shearImg.setPreferredSize(new Dimension(640,480));
			
			imgPanel.add(tiltImg);
			imgPanel.add(shearImg);
		}
		
		hgroup.addGroup( layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(imgPanel)
				.addGroup( layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(lpanel) )
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(outScrollPane))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addComponent(offsetsTable)
							.addComponent(thetasTable))
				)
			);
				
		vgroup.addComponent(imgPanel)
		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
				.addComponent(lpanel)
				.addComponent(outScrollPane)
				.addGroup(layout.createSequentialGroup()
					.addComponent(offsetsTable)
					.addComponent(thetasTable))
		);
		
		layout.setHorizontalGroup(hgroup);
		layout.setVerticalGroup(vgroup);	
		
		Thread monitor = new Thread(new StateMonitor());
		monitor.start();		
		
		reportTableFrame = new JFrame("Report Table");
		reportTable = new ReportTable(new String[] {"",""});
		reportTableFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		Container cont = reportTableFrame.getContentPane();
		cont.setLayout(new BoxLayout(cont,BoxLayout.Y_AXIS));
		cont.add(reportTable);
		reportTableFrame.pack();
		reportTableFrame.setVisible(true);
		
	}

	public void actionPerformed(ActionEvent ae) {
		Object src = ae.getSource();
		
		if(src == grabb){
			updateVars();
			boolean res;
			
			synchronized(grabb){
				if(grabb.isSelected()){
					res = startImages();
					grabb.setSelected(res);
				}else{
					stopImages();
				}
			}
			
		}else if(src == alignPanelCB){	

			if( !alignPanelCB.isSelected() ){
				if( alignPanelFrame!=null ){
					alignPanelFrame.dispose();
				}
				return;
			}
			
			//Open PIPanel in its own frame
			alignPanel =  new AlignPanel(ai,tiltImg,shearImg,reportTable);
			alignPanelFrame=new JFrame("Align Panel");
			alignPanelFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			alignPanelFrame.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(WindowEvent e){
					alignPanel.terminate();
					alignPanelCB.setSelected(false);
				}
			});
			
			Container c = alignPanelFrame.getContentPane();
			c.add(alignPanel);			
			alignPanelFrame.pack();
			alignPanelFrame.setVisible(true);
			
		}else if(src == cameraPanelCB){	

			if( !cameraPanelCB.isSelected() ){
				if( cameraPanelFrame!=null ){
					cameraPanelFrame.dispose();
				}
				return;
			}
			
			//Open PIPanel in its own frame
			cameraPanel =  new CameraPanel(tiltImg,shearImg);
			cameraPanelFrame=new JFrame("Camera Panel");
			cameraPanelFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			cameraPanelFrame.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(WindowEvent e){
					cameraPanelCB.setSelected(false);
				}
			});
			
			Container cameraPanelContainer = cameraPanelFrame.getContentPane();
			cameraPanelContainer.add(cameraPanel);			
			cameraPanelFrame.pack();
			cameraPanelFrame.setVisible(true);	
			
		}else if(src == actuatorPanelCB){
			if(!actuatorPanelCB.isSelected()){
				if(actuatorPanelFrame!=null){
					actuatorPanelFrame.dispose();
				}
				return;
			}
		
			//Open PIPanel in its own frame
			actuatorPanelFrame=new JFrame("Actuator Panel");
			actuatorPanelFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			
			Container c = actuatorPanelFrame.getContentPane();
			actuatorPanel = new ActuatorPanel(ai);			
			c.add(actuatorPanel);
			
			actuatorPanelFrame.pack();
			actuatorPanelFrame.setVisible(true);
		}else if(src == logPanelCB){
			if(!logPanelCB.isSelected()){
				if(logPanelFrame!=null){
					logPanel.terminate();
					logPanelFrame.dispose();
				}
				return;
			}
			
			logger = new Logger(shearImg,tiltImg,reportTable);
			logPanel = new LogPanel(logger);
			logPanelFrame = new JFrame("Log Panel");
			logPanelFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			logPanelFrame.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(WindowEvent e){
					logPanel.terminate();
				}
			});
			
			Container c = logPanelFrame.getContentPane();
			c.add(logPanel);
			
			logPanelFrame.pack();
			logPanelFrame.setVisible(true);
		}else if(src == corrTestCB){
			if(!corrTestCB.isSelected()){
				if(corrTestFrame!=null){
					corrTest.terminate();
					corrTestFrame.dispose();
				}
				return;
			}
			
			corrTest = new CorrectionTest(routines);
			corrTestFrame = new JFrame("Correction Test");
			corrTestFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			corrTestFrame.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(WindowEvent e){
					corrTest.terminate();
				}
			});
			
			corrTestFrame.getContentPane().add(corrTest);
			corrTestFrame.pack();
			corrTestFrame.setVisible(true);
		}
		
	}
	
	public boolean startImages(){
		boolean coadd=false;
		int gcnt = 0;
		
		if(alignPanel!=null){
			coadd = alignPanel.isCoaddOn();
		}
		
		if(tiltCB.isSelected()){
			try{
				tiltImg.initImage();
				tiltImg.loadCalibrationFile(tiltCalibFile);
				tiltImg.loadColorProfile(colorProfile);
				if(coadd){
					tiltImg.startCoaddedImage();
				}else{
					tiltImg.startImage();
				}
				
				gcnt++;
			}catch(Exception ex){
				JOptionPane.showMessageDialog(null, "Error starting tilt image: "+ex.getMessage());
			}
		}
		
		if(shearCB.isSelected()){
			try{
				shearImg.initImage();
				shearImg.loadCalibrationFile(shearCalibFile);
				shearImg.loadColorProfile(colorProfile);
				if(coadd){
					shearImg.startCoaddedImage();
				}else{
					shearImg.startImage();
				}
				gcnt++;
			}catch(Exception ex){
				JOptionPane.showMessageDialog(null, "Error starting shear image: "+ex.getMessage());
			}
		}
		
		//check that at least one image was started successfully 
		if(gcnt>0){
			return true;
		}else{
			return false;
		}
	}
	
	public void stopImages(){
		try{
			tiltImg.stopImage();
			tiltImg.closeImage();
		}catch(NativeImageException ex){
			JOptionPane.showMessageDialog(null, "Error stopping tilt image: "+ex.getMessage());
		}
		
		try{
			shearImg.stopImage();
			shearImg.closeImage();
		}catch(NativeImageException ex){
			JOptionPane.showMessageDialog(null, "Error stopping shear image: "+ex.getMessage());
		}
	}
	
	private class StateMonitor implements Runnable{
		
		public void run(){
			while(true){
				try{
					
					synchronized(grabb){
						if( tiltImg != null
								&& (tiltImg.getNativeImageState() != NativeImage.INITIALIZED_RUNNING) 
								&& (shearImg.getNativeImageState() != NativeImage.INITIALIZED_RUNNING) ){
							grabb.setSelected(false);
						}
					}
					
					if(alignPanelFrame!=null){
						alignPanelCB.setSelected(alignPanelFrame.isVisible());
					}else{
						alignPanelCB.setSelected(false);
					}
					
					if(actuatorPanelFrame != null){
						actuatorPanelCB.setSelected(actuatorPanelFrame.isVisible());
					}else{
						actuatorPanelCB.setSelected(false);
					}
					
					if(cameraPanelFrame != null){
						cameraPanelCB.setSelected(cameraPanelFrame.isVisible());
					}else{
						cameraPanelCB.setSelected(false);
					}
					
					if(logPanelFrame != null){
						logPanelCB.setSelected(logPanelFrame.isVisible());
					}else{
						logPanelCB.setSelected(false);
					}
					
					if(corrTestFrame != null){
						corrTestCB.setSelected(corrTestFrame.isVisible());
					}else{
						corrTestCB.setSelected(false);
					}
					
					Thread.sleep(500);
					
				}catch(InterruptedException ex){
					break;
				}
			}
		}
		
	}
	
	public static void main(String[] args){
		TiltShearUI frame = new TiltShearUI();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		
		System.out.println("TiltShearUI test");
	}
	
}
