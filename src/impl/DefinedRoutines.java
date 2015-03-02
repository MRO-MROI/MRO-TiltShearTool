package impl;

import java.io.IOException;

import javax.swing.JOptionPane;

import xenimaq.NativeImageException;
import xenimaq.NativeImageImpl;
import agilis.Actuator;
import agilis.ActuatorInterface;
import agilis.AgilisException;
import data.AlignmentMath;
import data.SetupData;

/**<pre>

 * Routines:
 *  - Set reference points for tilt and shear (w/coadd wo/coadd)
 *  - Compute average step sizes.
 *  - Alignment co-added grab.
 *  - Alignment continuous grab. 
 * ____________________________ 
 * Commands:
 * 
 *  setTiltRef
 *  setShearRef
 *  startStepCalc
 *  stopStepCalc
 *  startPILoop
 *  stopPILoop
 * ____________________________
 *</pre>
 */
public class DefinedRoutines {
	ActuatorInterface ai;
	NativeImageImpl tiltImg;
	NativeImageImpl shearImg;
		
	AlignmentMath amath;
	PIControl pictrl;
	MeanStepSizeCalculator sscalc;
	
	boolean coadd=false;
	boolean autoPILoop=false;
	
	Thread motionThread;
	
	public DefinedRoutines(ActuatorInterface ai){
		this.ai = ai;
	}
	
	public DefinedRoutines(ActuatorInterface ai, NativeImageImpl shearImg,
			NativeImageImpl tiltImg){
		this.ai = ai;
		this.shearImg = shearImg;
		this.tiltImg = tiltImg;
				
		pictrl = new PIControl(ai,tiltImg,shearImg,null);
		sscalc = new MeanStepSizeCalculator(ai,tiltImg,shearImg,null); //TODO move this
	}

/* * * * * * * * * * * * * * * */
/* pre defined routines */
	/**
	 * Continuous image grab from both images, no-coaddition, auto alignment 
	 */
	public void runAlignAutoNoCoadd(){
		startImages(true,true);
		startStepCalc();
	
		try{
			while(sscalc.isRunning())
				Thread.sleep(500);
		}catch(InterruptedException ex){
			
		}
		
		//At this point; the alignment beam should be blocked
		//and the reference beam should be shown
		setRef(tiltImg);
		setRef(shearImg);
		
		//Now the reference beam should be blocked and the
		//alignment beam should be shown
		autoPILoop = true;
		startPILoop();
	}
	
	/** Co-add image grab, non-continous, auto alignment */
	public void runAlignAutoCoadd(int coaddN, int coaddT){
		coadd = true;
		
		setupCoadd(coaddN,coaddT);
		startStepCalc();
		
		//At this point; the alignment beam should be blocked
		//and the reference beam should be shown
		setRef(tiltImg);
		setRef(shearImg);
		
		//Now the reference beam should be blocked and the
		//alignment beam should be shown
		autoPILoop = true;
		startPILoop();
	}
	
	/** Continuous image grab, manual alignment */
	public void runAlignManualNoCoadd(){
		coadd = false;
		autoPILoop = false;
		
		startImages(true,true);
		//At this point; the alignment beam should be blocked
		//and the reference beam should be shown
		setRef(tiltImg);
		setRef(shearImg);
		
		//Now the reference beam should be blocked and the
		//alignment beam should be shown
		startPILoop();
	}
	
	/** Coadd image grab, manual alignment */
	public void runAlignManualCoadd(int coaddN, int coaddT){
		coadd = true;
		
		setupCoadd(coaddN,coaddT);
		startStepCalc();
		
		//At this point; the alignment beam should be blocked
		//and the reference beam should be shown
		setRef(tiltImg);
		setRef(shearImg);
		
		//Now the reference beam should be blocked and the
		//alignment beam should be shown
		autoPILoop = true;
		startPILoop();
	}
	
	/** Compute step sizes, no coadd */
	public void getStepSizesNoCoadd(){
		coadd = false;
		
		startStepCalc();
		while(sscalc.isRunning()){
			try{
				Thread.sleep(100);
			}catch(InterruptedException ex){
				break;
			}
		}
			
	}
	
	public void agilisTest(){
		int nsteps=250, ampl=50;
		try{

			if(!ai.isConnected()){
				ai.connect();
			}
			
			ai.setChannel(3);
			
			ai.setEqualStepSize(50);
			ai.getAllStepSizes();
			
			ai.stepAxis1(nsteps);
			while(ai.isMoving(1))
				Thread.sleep(1000);
			
			//move back to start point
			ai.stepAxis1(-nsteps);
			while(ai.isMoving(1))
				Thread.sleep(1000);
			
			ai.stepAxis2(nsteps);
			while(ai.isMoving(2))
				Thread.sleep(1000);
						
			//move back to start point
			ai.stepAxis2(-nsteps);
			while(ai.isMoving(2))
				Thread.sleep(1000);
			
		}catch(Exception ex){
			ai.disconnect();
			ex.printStackTrace();
		}
	}
	
/* * * * * * * * * * * * * * * */
	
	public void setCoaddOn(boolean b){
		coadd = b;
	}
	
	public void setupCoadd(int coaddN, int coaddT){
		tiltImg.setupCoadd(coaddN, coaddT);
		shearImg.setupCoadd(coaddN, coaddT);
	}
	
	public void startImages(boolean tiltOn, boolean shearOn){
		boolean coadd=false;
		
		if(tiltOn){
			try{
				tiltImg.initImage();	
				if(coadd){
					tiltImg.startCoaddedImage();
				}else{
					tiltImg.startImage();
				}
			}catch(NativeImageException ex){
				//TODO
				System.err.println(ex.getMessage());
			}
		}
		
		if(shearOn){
			try{
				shearImg.initImage();
				if(coadd){
					shearImg.startCoaddedImage();
				}else{
					shearImg.startImage();
				}
			}catch(NativeImageException ex){
				//TODO
				System.err.println(ex.getMessage());
			}
		}
		
	}
	
	public void stopImages(){
		try{
			tiltImg.stopImage();
			tiltImg.closeImage();
		}catch(NativeImageException ex){
			//TODO
			System.err.println(ex.getMessage());
		}
		
		try{
			shearImg.stopImage();
			shearImg.closeImage();
		}catch(NativeImageException ex){
			//TODO
			System.err.println(ex.getMessage());
		}
	}
	
	public void setRef(NativeImageImpl img){
		if(img==null){
			return;
		}
		
		try{
			if(coadd){
				img.singleCoaddedImage(true);
			}
			
			img.setRefCentroid();
		}catch(NativeImageException ex){
			//TODO
			System.err.println(ex.getMessage());
		}catch(InterruptedException ex){
			//Do nothing
		}
	}
	
	public void startStepCalc(){
		sscalc.startCalc();
	}
	
	public void stopStepCalc(){
		sscalc.stopCalc();
	}
	
	public void startPILoop(){
		if(!autoPILoop){ //TODO change amplitude values to variables
			try{
				pictrl.startManualLoop(50,false);
			}catch(NativeImageException ex){
				tiltImg.termImage();
				shearImg.termImage();
				System.err.println(ex.getMessage());
			}
		}else{
			pictrl.startAutoLoop(50,coadd);
		}	
	}
	
	public void stepPILoop() {
		try {
			pictrl.singleStep();
		} catch (Exception ex) {
			if( ex instanceof NativeImageException ) {
				tiltImg.termImage();
				shearImg.termImage();
			//TODO
				System.err.println(ex.getMessage());
			}else if( ex instanceof InterruptedException ) {
				//Do nothing
			}
		}
	}
	
	public void stopPILoop() {
		pictrl.stopLoop();
	}
	
	public void abortMotion(){
		if(motionThread!=null){
			motionThread.interrupt();
		}
	}
	
	public void setReferencePoints(){
		tiltImg.setRefCentroid();
		shearImg.setRefCentroid();	
	}
	
	public void moveMounts(int m1ax1, int m1ax2, int m2ax1, int m2ax2){
		if(motionThread==null || !motionThread.isAlive() ){
			motionThread = new MoveMountsThread(m1ax1,m1ax2,m2ax1,m2ax2);
			motionThread.start();
		}else{
			throw new RuntimeException("Motion currently executing by: "+motionThread.getName());
		}
	}
	
	public class MoveMountsThread extends Thread{
		int m1ax1;
		int m1ax2;
		int m2ax1;
		int m2ax2;
		
		public MoveMountsThread(int m1ax1, int m1ax2, int m2ax1, int m2ax2){
			super("MoveMountsThread");
			this.m1ax1 = m1ax1;
			this.m1ax2 = m1ax2;
			this.m2ax1 = m2ax1;
			this.m2ax2 = m2ax2;
		}
		
		@Override
		public void run(){
			try{
				ai.setExclusiveAccessLock(true,this.getName());
				if(ai.getActuatorState()==Actuator.NOT_INITIALIZED){
					ai.connect();
				}	
				
				ai.setChannel( ((Double)SetupData.vart.get("chM1")).intValue() );
				ai.setEqualStepSize( SetupData.currentMountStepAmplitude );
				
				ai.stepAxis1(m1ax1);
				while(ai.isMoving(1)){
					Thread.sleep(500);
				}
								
				ai.stepAxis2(m1ax2);
				while(ai.isMoving(2)){
					Thread.sleep(500);
				}
				
				ai.setChannel( ((Double)SetupData.vart.get("chM2")).intValue() );
				ai.setEqualStepSize( SetupData.currentMountStepAmplitude );
				
				ai.stepAxis1(m2ax1);
				while(ai.isMoving(1)){
					Thread.sleep(500);
				}
				
				ai.stepAxis2(m2ax2);
				while(ai.isMoving(2)){
					Thread.sleep(500);
				}
				
				AlignmentMath.computeThetaCorrection(tiltImg.getRefCentroid(),tiltImg.getCentroid(),
						shearImg.getRefCentroid(),shearImg.getCentroid());
				
				ai.setExclusiveAccessLock(false,this.getName());
			}catch(Exception ex){
				ai.setExclusiveAccessLock(false,this.getName());
				if(ex instanceof IOException || ex instanceof AgilisException) {
					ai.disconnect();
					System.err.println(ex.getMessage());
				}else{
					throw new RuntimeException(ex);
				}
			}
		}
	}

}
