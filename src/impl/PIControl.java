package impl;

import gui.ReportTable;

import java.awt.geom.Point2D;
import java.io.IOException;

import sun.util.calendar.BaseCalendar.Date;

import data.AlignmentMath;
import data.PIDMath;
import data.SetupData;
import xenimaq.NativeImageException;
import xenimaq.NativeImageImpl;
import agilis.Actuator;
import agilis.ActuatorInterface;
import agilis.AgilisException;


/**<pre>
 * This class implements the PI control of the four angular
 * corrections needed to correct tilt and shear. 
 * 
 * The angular corrections require two images; one image is obtained from a
 * camera set up to measure tilt and the other from a camera set up to measure shear.
 * Pixel offsets relative to the reference position are obtained from the images.
 * PIControl uses NativeImageImpl to get access to reference and alignment
 * beam positions, these positions are passed to {@link AlignmentMath#computeThetaCorrection(java.awt.geom.Point2D.Double, java.awt.geom.Point2D.Double, java.awt.geom.Point2D.Double, java.awt.geom.Point2D.Double)}
 * to compute the corrections.
 * 
 * PIControl is designed to allow alignment using the following procedures:
 * 1. Continuous grab from both tilt and shear images.
 *  - Start tilt and shear grab routines
 *  - Set reference points for tilt and shear images
 *  - Start PI loop (automated or manual)
 *   - If manual, the loop must be stepped using the method call singleStep(false)
 *  
 * 2. Non continuous coadded tilt and shear image.
 *  - Set coadd variables
 *  - Set reference points for tilt and shear images
 *  - Start PI loop (automated or manual)
 *   - If manual, the loop must be stepped using the method call singleStep(true)
 *  
 * PIControl does not change image related settings.  So setup for coaddition should
 * be done prior to starting a coadd alignment routine. 
 * 
 * Step procedure:
 *  - Regardless of the method used (automated or single step) the method stepLoop() is used to position
 *  the alignment beam on top of the reference beam. 'stepLoop()' computes the angular correction angles, 
 *  the angles are input into the corresponding PIMath loops, the output from these loops are used to compute
 *  the number of steps and direction each axis must be moved for mirrors SY1 and SY2.  
 *  - One important note: SY1 and SY2 are alternately adjusted.  The first call to 'stepLoop()' will
 *  apply the correction steps for SY1 and ignore the correction for SY2.  The next call will apply the
 *  correction to SY2 and ignore the correction for SY1.  The cycle is repeated until loop is ended.
 *   
 * </pre>
 */
public class PIControl{
	ActuatorInterface ai;
	NativeImageImpl timg;
	NativeImageImpl simg;
	PIRules errorChecker;
	
	Point2D.Double tref;
	Point2D.Double toff;
	Point2D.Double sref;
	Point2D.Double soff;

	double pgain, igain;
	
	int chM1, chM2;
	
	boolean coadd=false;
	
	Thread autoThread;
	Thread stepMotionThread;
	Thread stepCoaddThread;
	
	int stepCnt;
	
	long loopStartTime;
	
	private static enum AlignState { SY1, SY2 };
	AlignState alignState = AlignState.SY1;
	
	private boolean errorChecking = false;
	private boolean loopLock=false;
	private boolean stepLock=false;
	
	private ReportTable rtable;
	boolean guiMode = false;
	
	double stepInterval = 500;
	
	public PIControl(ActuatorInterface ai, 
			NativeImageImpl simg, NativeImageImpl timg, ReportTable rtable){
		this.ai = ai;
		this.timg = timg;
		this.simg = simg;
		errorChecker =  new PIRules(timg, simg, ai);
		
		chM1 = ((Double)SetupData.vart.get("chM1")).intValue();
		chM2 = ((Double)SetupData.vart.get("chM2")).intValue();
	
		this.rtable = rtable;
		if(rtable!=null){
			guiMode=true;
		}
	}
	
	public void setHome()  throws IOException, AgilisException,InterruptedException{
		int chai = ai.getChannel();
		Integer[] home1 = {0,0,0,0};
		Integer[] home2 = {0,0,0,0};
		ai.setChannel(1);
		home1[0] = ai.getHomeAxis1();
		while(ai.isMoving(1)){
			Thread.sleep(500);
		}
		Thread.sleep(0);
		home2[0] = ai.getHomeAxis2();
		while(ai.isMoving(2)){
			Thread.sleep(500);
		}
		Thread.sleep(0);
		ai.setChannel(2);
		home1[1] = ai.getHomeAxis1();
		while(ai.isMoving(1)){
			Thread.sleep(500);
		}
		Thread.sleep(0);
		home2[1] = ai.getHomeAxis2();
		while(ai.isMoving(2)){
			Thread.sleep(500);
		}
		Thread.sleep(0);
		ai.setChannel(chai);
		ai.setHomePositions(home1, home2);
	}
	
	public void setupLoop(double pgain, double igain){
		this.pgain = pgain;
		this.igain = igain;
	}
	
	public void setErrorCheckingStatus(boolean status){
		errorChecking = status;
	}
	
	public boolean getErrorCheckingStatus(){
		return errorChecking;
	}
	
	private void initLoops(int stepAmpl){
		loopLock = true;
		stepCnt=0;
		
		AlignmentMath.m1Xloop=new PIDMath(pgain,igain);
		AlignmentMath.m1Yloop=new PIDMath(pgain,igain);
		AlignmentMath.m2Xloop=new PIDMath(pgain,igain);
		AlignmentMath.m2Yloop=new PIDMath(pgain,igain);
		
		//Get direct references to Point2D.Double instances.
		//This grants read-write access to instances in NativeImageImpl,
		//however it also makes it so that this call is only needed
		//once in order to have access to the latest centroid value.
		tref = timg.getRefCentroid();
		sref = simg.getRefCentroid();
		toff = timg.getCentroid();
		soff = simg.getCentroid();
		
		try{
			ai.setExclusiveAccessLock(true,"PIControl");
			
			if(ai.getActuatorState()==Actuator.NOT_INITIALIZED){
				ai.connect();
			}	
		
			ai.setEqualStepSize(stepAmpl);
			
		}catch(Exception ex){
			if(ex instanceof IOException || ex instanceof AgilisException) {
				ai.disconnect();
				System.err.println(ex.getMessage());
			}else{
				throw new RuntimeException(ex);
			}
		}
		
		alignState = AlignState.SY1;
	}
	
	public void setStepInterval(double stepInterval){
		this.stepInterval = stepInterval;
	}
	
	public void startManualLoop(int stepAmpl, boolean coadd)
		throws NativeImageException {
		if(loopLock){
			throw new RuntimeException("A PI control loop is already started!");
		}
		
		this.coadd=coadd;
		if(coadd){
			timg.initImage();
			simg.initImage();
		}
		
		initLoops(stepAmpl);
		if(guiMode){
			rtable.resetTable(new String[] {"Iter. #","\u03b8x_s1","\u03b8y_s1","\u03b8x_s2","\u03b8y_s2","N_s1ax1","N_s1ax2","N_s2ax1","N_s2ax2"});
		}
		
		if(SetupData.debug) System.out.println("Manual PI loop started, coadd="+coadd);
	}
	
	public void startAutoLoop(int stepAmpl, boolean coadd){
		System.out.println(System.currentTimeMillis());
		AlignmentMath.Hxdt_r.setNumber(0);
		AlignmentMath.Hydt_r.setNumber(0);
		AlignmentMath.Hxds_r.setNumber(0);
		AlignmentMath.Hyds_r.setNumber(0);
		if(loopLock){
			if(SetupData.debug) System.out.println("PIControl auto loop already started!");
		}
		
		this.coadd = coadd;
		initLoops(stepAmpl);
		if(guiMode){
			rtable.resetTable(new String[] {"Time [sec]","\u03b8x_s1","\u03b8y_s1","\u03b8x_s2","\u03b8y_s2","N_s1ax1","N_s1ax2","N_s2ax1","N_s2ax2"});
		}
		
		autoThread = new Thread(new AutoAlignThread());
		autoThread.start();
	}
	
	/** 
	 * Stop currently executing alignment loop:
	 * 
	 * 4 scenarios:
	 *  - Auto align loop no coaddition
	 *  - Auto align loop with coaddition
	 *  - Manual align loop no coaddition
	 *  - Manual align loop with coaddition
	 */
	public void stopLoop(){
		if(autoThread!=null){
			if(SetupData.debug) System.out.println("Stopping auto alignment loop coadd="+coadd);
			autoThread.interrupt(); //auto align loop, with/without coaddition
			autoThread=null;
		}else{
			if(SetupData.debug) System.out.println("Stopping manual alignment loop coadd="+coadd); 
				
			//Manual loop mode
			if(coadd){
				if(stepCoaddThread!=null){
					stepCoaddThread.interrupt(); //manual align loop with coaddition 
					stepCoaddThread = null;					
				}
				
				timg.termImage();
				simg.termImage();
			}else{ 
				if(stepMotionThread!=null){
					stepMotionThread.interrupt(); //manual align loop without coaddition
				}
			}
		}
		
		loopLock = false;
		//TODO this will throw a runtime exception if 'stopLoop()' is called
		//before initLoops() is called. Which happens if stop button is pressed before start button
		ai.setExclusiveAccessLock(false,"PIControl");
		System.out.println(System.currentTimeMillis());
	}
	
	/**
	 * Process: use this method when the user is executing manual loop.
	 * 
	 * Two image grab scenarios can be used:
	 * 
	 *  1. Continuous image grab, images are continuosly acquired while
	 *  stepLoop executes.  Any mathematical operation uses the latest available
	 *  centroid values as they are needed.
	 *  
	 *  2. Coadded images, non-continuous image grab.  A coadded image must
	 *  be obtained before and after calling 'stepLoop()'.  The coadd process
	 *  will block until it is done, so it is necessary to obtain the coadd
	 *  image inside a local thread.
	 * 
	 */
	public void singleStep() {
		if(stepLock){
			if(SetupData.debug) System.out.println("Step already in progress!");
			return;
		}
		
		//Note that 'coadd' is set when start*Loop is called, so if for some reason
		//the 'coadd' option becomes disabled outside this class, it would not affect
		//the local variable.
		if(coadd){
			stepCoaddThread = new Thread(new SingleStepCoaddThread());
			stepCoaddThread.start();
		}else{
			stepLoop();
		}		
	}
	
	/** 
	 * Process:
	 * - Compute the angular corrections using the latest centroid value calculated for
	 * the tilt and shear image.  
	 * 
	 */

	public void stepLoop() {
		double em1X, em1Y, em2X, em2Y;

		AlignmentMath.computeThetaCorrection(tref, toff, sref, soff); 
		
		em1X=-1*AlignmentMath.m1Xloop.updatePID(0, AlignmentMath.acm1x);
		em1Y=-1*AlignmentMath.m1Yloop.updatePID(0, AlignmentMath.acm1y);
		em2X=-1*AlignmentMath.m2Xloop.updatePID(0, AlignmentMath.acm2x);
		em2Y=-1*AlignmentMath.m2Yloop.updatePID(0, AlignmentMath.acm2y);
		
		AlignmentMath.computeStepCorrection(em1X,em1Y,em2X,em2Y); 

		if(autoThread!=null){
			if(guiMode){
				rtable.updateTable(new Object[]{
						stepCnt,em1X,em1Y,em2X,em2Y,AlignmentMath.scm1ax1,AlignmentMath.scm1ax2,
						AlignmentMath.scm2ax1,AlignmentMath.scm2ax2 
				});
			}
		}else{
			if(guiMode){
				rtable.updateTable(new Object[]{
						stepCnt,em1X,em1Y,em2X,em2Y,AlignmentMath.scm1ax1,AlignmentMath.scm1ax2,
						AlignmentMath.scm2ax1,AlignmentMath.scm2ax2 
				});
			}
		}
		stepCnt++;
		
		stepMotionThread = new Thread(new StepMotionThread());
		stepMotionThread.start();  
		
	}
	
	/**
	 * This thread serves as a wrapper to the {@link PIControl.#stepLoop()} method.  It is only used
	 * when coaddition mode is selected.  The step loop method is called, then a new single coadded
	 * image is obtained for the tilt and the shear detector. 
	 */
	private class SingleStepCoaddThread implements Runnable{
		public void run(){
			try{
				stepLoop();
				while(stepMotionThread.isAlive()){
					Thread.sleep(500);
				}
				//When stepMotionThread is done, stepLock is set to false.
				//Keep stepLock set to true until coadded images are obtained.
				stepLock=true;

				timg.singleCoaddedImage(false);
				simg.singleCoaddedImage(false);
				
				stepLock = false;
			}catch(Exception ex){
				if(ex instanceof InterruptedException){
					stepMotionThread.interrupt();
					if(SetupData.debug) System.out.println("SingleStepCoaddThread interrupted");
				}else{
					timg.termImage();
					simg.termImage();
					throw new RuntimeException(ex);
				}
			}
		}
	}
	
	/**
	 * Thread used to apply the step corrections to switchyard mirror 1 and 2.
	 * This routine is implemented in a thread to prevent the GUI from freezing up
	 * while calls to the Actuator are being made. 
	 */
	private class StepMotionThread implements Runnable{
		public void run(){
			stepLock=true;
			
			try {
				
				ai.setChannel(chM1);
				ai.stepAxis1(AlignmentMath.scm1ax1);
				while(ai.isMoving(1)){
					Thread.sleep(500);
				}
				
				Thread.sleep(0);

				ai.stepAxis2(AlignmentMath.scm1ax2);
				while(ai.isMoving(2)){
					Thread.sleep(500);
				}

				Thread.sleep(0);
				 
				ai.setChannel(chM2);
				ai.stepAxis1(AlignmentMath.scm2ax1);
				while(ai.isMoving(1)){
					Thread.sleep(500);
				}

				Thread.sleep(0);

				ai.stepAxis2(AlignmentMath.scm2ax2);
				while(ai.isMoving(2)){
					Thread.sleep(500);
				}

				Thread.sleep(0);
				if (errorChecking){
					if(errorChecker.checkBeam(timg, simg)!=0){
						homeAll();
					}
				}

			} catch(Exception ex) {
	
				if(ex instanceof IOException) {
					ai.disconnect();
					System.err.println(ex.getMessage());
				} else if(ex instanceof AgilisException) {
					ai.disconnect();
					System.err.println(ex.getMessage());
				} else if(ex instanceof InterruptedException) {
//					if(SetupData.debug) System.out.println("StepMotionThread interrupted");		
				} else {
					throw new RuntimeException(ex);
				}
				
			}
			stepLock=false;
		}
		public void homeAll()  throws IOException, AgilisException,InterruptedException{
			int chai = ai.getChannel();
			ai.setChannel(1);
			ai.homeAxis1();
			while(ai.isMoving(1)){
				Thread.sleep(500);
			}
			Thread.sleep(0);
			ai.homeAxis2();
			while(ai.isMoving(2)){
				Thread.sleep(500);
			}
			Thread.sleep(0);
			ai.setChannel(2);
			ai.homeAxis1();
			while(ai.isMoving(1)){
				Thread.sleep(500);
			}
			Thread.sleep(0);
			ai.homeAxis2();
			while(ai.isMoving(2)){
				Thread.sleep(500);
			}
			Thread.sleep(0);
			ai.setChannel(chai);
		}
		
	}
	
	/**
	 * Thread implements automated alignment procedure.  
	 * The procedure uses the same routines used in manual mode except
	 * it uses an infinite loop to automatically step the loop. 
	 */
	private class AutoAlignThread implements Runnable{
		public void run(){
			long t0, dt;
			
			if(SetupData.debug) System.out.println("AutoAlignThread entering.");
			
			loopStartTime = System.currentTimeMillis();
			
			/* If coaddition will be used, the images must first be initialized.
			 * Doing this guarantees that images are not currently being used elsewhere,
			 * and configures the image for this method's purpose */
			if(coadd){
				try{
					timg.initImage();
					simg.initImage();
				}catch(NativeImageException ex){
					timg.termImage();
					simg.termImage();
				}
			}
			
			Main:
			while(true){
				t0 = System.currentTimeMillis();
				
				/* If coaddition turned on, obtain a single coadded frame from the
				 * tilt and shear detectors before executing alignment procedure.	
				 */
				if(coadd){
					try{
						timg.singleCoaddedImage(false);
						simg.singleCoaddedImage(false);
					}catch(NativeImageException ex){
						timg.termImage();
						simg.termImage();
						throw new RuntimeException(ex);
					}catch(InterruptedException ex){
						if(SetupData.debug) System.out.println("AutoAlignThread interrupted.");
						stepMotionThread.interrupt();
						break Main;
					}
				}
						
				stepLoop();
			
				while(stepMotionThread.isAlive()){
					try{
						Thread.sleep(500);
					}catch(InterruptedException ex){
						if(SetupData.debug) System.out.println("AutoAlignThread interrupted.");
						stepMotionThread.interrupt();	
						break Main;
					}
				}
				
				dt = System.currentTimeMillis() - t0;
				if(dt<stepInterval){
					try {
						Thread.sleep( (long)(stepInterval*1000)-dt );
					} catch (InterruptedException e) {
						break Main;
					}
				}
			}	
				
			if(coadd){
				timg.termImage();
				simg.termImage();
			}
						
			if(SetupData.debug) System.out.println("AutoAlignThread exiting.");
		}
	}
	
}
