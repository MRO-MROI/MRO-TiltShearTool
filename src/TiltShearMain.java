import gui.TiltShearUI;

import impl.DefinedRoutines;

import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import util.MemoryWarningSystem;

import data.AlignmentMath;
import data.SetupData;
import xenimaq.NativeImage;
import xenimaq.NativeImageImpl;
import agilis.Actuator;
import agilis.ActuatorInterface;
import agilis.ActuatorPanel;
import agilis.AgilisException;

/** Main class used to start the TiltShearTool program.
 * 
 * TiltShearTool can be run in two modes, CommandServer mode
 * or TiltShearUI mode.
 * 
 * CommandServer mode is supposed to serve as a model of how TiltShearTool could be
 * implemented remotely over a terminal or client program using TCP sockets.  This mode
 * is designed to allow a limited functionality and only pre-defined routines can be 
 * executed.
 * 
 * TiltShearUI mode opens up a GUI designed to be run from the computer the frame grabbers and
 * Agilis controller are connected to. This mode offers more control of the mounts and would
 * be a useful tool a technician could use to manually align the mount system.  It is also useful 
 * for initial alignment purposes and testing since the GUI will display both the tilt and shear images
 * on the screen so the user can see what the alignment loop is doing in real-time.
 * 
 * This class is in charge of setting up all the low level hardware communication eclipse-javadoc:%E2%98%82=TiltShearTool/src%3C%7BTiltShearMain.java%E2%98%83TiltShearMainobjects.
 * 
 * */
final public class TiltShearMain {
	/* Java Hardware control handles */
	Actuator actl;
	NativeImage tiltImgCtl;
	NativeImage shearImgCtl;
	
	/* Implementation */
	ActuatorInterface ai;
	NativeImageImpl tiltImg;
	NativeImageImpl shearImg;
	
	String agilisControllerDevice;
	String tiltCameraDevice;
	String shearCameraDevice;

	
	/**Default constructor, no setup file is used, default setup values 
	 * defined in SetupData class are used.
	 */
	public TiltShearMain(){
		updateVars();
		System.out.println("Did NOT Read Setup File!");
	}
	
	/** Alternate constructor, setup data is loaded from the setup file
	 * at path 'setupFile'
	 * @param setupFile path to setup file
	 */
	public TiltShearMain(String setupFile){
		SetupData.readConfigFile(setupFile);
		updateVars();
		System.out.println("Read Setup File!");
	}
	
	/** Method used to acquire the setup values from SetupData and store into
	 * local variables and update setup values for all other classes that use
	 * setup values in SetupData. 
	 */
	public void updateVars(){
		agilisControllerDevice = (String)SetupData.vart.get("agilisControllerDevice");
		tiltCameraDevice = (String)SetupData.vart.get("tiltCameraDevice");
		shearCameraDevice = (String)SetupData.vart.get("shearCameraDevice");

		AlignmentMath.updateVars();
	}
	
	/** Instantiate all low-level hardware communication objects for
	 * controlling Agilis mounts and obtaining image from the pci frame
	 * grabber. 
	 *   
	 * @param paintImages Flag used to specify whether the images should be displayed to the GUI, 
	 * this should be 'false' when CommandServer mode is used.
	 */
	public void initDevices(boolean paintImages){

		actl = Actuator.getActuatorInstance(agilisControllerDevice);
		ai = new ActuatorInterface(actl);
		
		try{
			tiltImgCtl = NativeImage.getNativeImageInstance(tiltCameraDevice);
		}catch(RuntimeException ex){
			tiltImgCtl = null;
			System.err.println(ex.getMessage());
		}
		
		try{
			shearImgCtl = NativeImage.getNativeImageInstance(shearCameraDevice);
		}catch(RuntimeException ex){
			shearImgCtl = null;
			System.err.println(ex.getMessage());
		}
		
		//TODO add calls load calibration and color profile
		//these should be defined as variables inside the setup file
		// shearCalibFile, tiltCalibFile, colorProfile
		//Also need to add code for controlling the frame rate
		tiltImg = new NativeImageImpl(tiltImgCtl,"Tilt");
		tiltImg.setPaintComponent(paintImages);
		shearImg = new NativeImageImpl(shearImgCtl,"Shear");
		shearImg.setPaintComponent(paintImages);
		
	}
	
	/** Starts TiltShearUI 
	 */
	public void startGUI(){
		try{
			final TiltShearUI frame=new TiltShearUI(ai,shearImg,tiltImg);
			
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	
			frame.addWindowListener(
				new WindowAdapter(){
					@Override
					public void windowClosing(WindowEvent e){
						System.out.println("Window Closing");
						terminate();
					}
				}
			);
						
			frame.buildUI();
			frame.pack();
			frame.setVisible(true);
			
//			System.out.println("Test: \u2191   \u03B8");
		}catch(Exception ex){
			System.err.println("[startGUI()]: Fatal system error occured.");
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
	/** Method must be called before exiting TiltShearMain, it will stop
	 * image acquisition and clean up low-level device objects properly before
	 * returning.   
	 */
	public void terminate(){
		if(tiltImg!=null){
			tiltImg.termImage();
		}
		
		if(shearImg!=null){
			shearImg.termImage();
		}
		
		if(ai!=null){
			ai.disconnect();
		}
	}
	
	/** Start CommandServer mode.
	 */
	public void startCmdServer(){
		DefinedRoutines serv = new DefinedRoutines(ai,shearImg,tiltImg);
//		serv.agilisTest();
		serv.getStepSizesNoCoadd();
	}
	
	public static void main(String[] args){
		/* User option signals whether to start a command server
		 * that takes requests for execution or to start
		 * the GUI.
		 */
		boolean useGUI=true;
		
		final TiltShearMain main = new TiltShearMain("./setup.txt");
		
		MemoryWarningSystem.setPercentageUsageThreshold(0.8);

	    MemoryWarningSystem mws = new MemoryWarningSystem();
	    mws.addListener(new MemoryWarningSystem.Listener() {
	      public void memoryUsageLow(long usedMemory, long maxMemory) {
	        System.err.println("Memory usage low!!!");
	        double percentageUsed = ((double) usedMemory) / maxMemory;
	        System.err.println("percentageUsed = " + percentageUsed);
	        MemoryWarningSystem.setPercentageUsageThreshold(0.8);
	      }
	    });

		if(useGUI){
			main.initDevices(true);
			
			Runnable thread = new Runnable(){
			    public void run(){
	                main.startGUI();
	            }
			};
			
			SwingUtilities.invokeLater(thread);
		}else{
			main.initDevices(false);
			main.startCmdServer();
		}
	}

}
