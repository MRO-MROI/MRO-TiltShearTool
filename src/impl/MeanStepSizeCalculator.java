package impl;

import gui.ReportTable;

import java.awt.geom.Point2D;
import java.io.IOException;

import util.NumberObserver;

import agilis.ActuatorInterface;
import agilis.AgilisException;
import data.AlignmentMath;
import data.SetupData;
import xenimaq.NativeImageException;
import xenimaq.NativeImageImpl;

/**
 * <pre>
 * 
 * </pre>
 */
final public class MeanStepSizeCalculator implements Runnable {
	static boolean theLock;
	
	ActuatorInterface ai;
	NativeImageImpl timg, simg;
		
	boolean coaddRoutine=false;
	
	int nsteps=100, stepAmpl=50;
	Thread thisThread;
	
	ReportTable reportTable;
	boolean guiMode = false;
	
	public MeanStepSizeCalculator(ActuatorInterface ai,
			NativeImageImpl simg, NativeImageImpl timg, ReportTable reportTable){
		this.ai = ai;
		this.timg = timg;
		this.simg = simg;
		
		this.reportTable = reportTable;
		if(reportTable!=null){
			guiMode = true;
		}
	}
	
	public void setSetup(int nsteps, int stepAmpl, boolean useCoadd){
		this.coaddRoutine = useCoadd;
		this.nsteps = nsteps;
		this.stepAmpl = stepAmpl;
	}
	
	public void startCalc(){
		if(!theLock){
			thisThread = new Thread(this,"MeanStepSizeCalculator");
			thisThread.start();
		}
	}
	
	public void stopCalc(){
		if(thisThread!=null){
			thisThread.interrupt();
		}
	}
	
	public boolean isRunning(){
		if(theLock){
			return true;
		}else{
			return false;
		}
	}
	
	@Override 
	public void run(){
		int chM1;
		int chM2;
		
		ai.setExclusiveAccessLock(true,thisThread.getName());
		
		if(theLock){
			throw new RuntimeException("MeanStepSizeCalculator already running!");
		}
		
		theLock = true;
		
		chM1=((Double)SetupData.vart.get("chM1")).intValue();
		chM2=((Double)SetupData.vart.get("chM2")).intValue();
		
		if(guiMode){
			reportTable.resetTable(new String[] 
			        {"","Hdxt","Hdyt","Hdxs","Hdys",
					"Hdxt_r","Hdy_r","Hdxs_r","Hdys_r",
					"\u03b8x_s1","\u03b8y_s1","\u03b8x_s2","\u03b8y_s2",
			        "\u03b8 tilt"});
			reportTable.setDataObservers(new NumberObserver[] {
					AlignmentMath.Hxdt,AlignmentMath.Hydt,
					AlignmentMath.Hxds,AlignmentMath.Hyds,
					AlignmentMath.Hxdt_r,AlignmentMath.Hydt_r,
					AlignmentMath.Hxds_r,AlignmentMath.Hyds_r,
					AlignmentMath.acm1xObs,AlignmentMath.acm1yObs,
					AlignmentMath.acm2xObs,AlignmentMath.acm2yObs,
					AlignmentMath.thetaObs
			});
		}
		
		System.out.println("%%%%");
		System.out.println("MeanStepSizeCalculator started.");
		
		try{
			System.out.format("steps=%d, ampl=%d%n",nsteps,stepAmpl);
			
			if(coaddRoutine){
//				runCoadd(chM1);
			}else{
				runNoCoadd(chM1);
			}
			
			System.out.format("SY1:    1-,       1+,       2-,        2+ [deg/step]%n");
			System.out.format("%8.7f, %8.7f, %8.7f, %8.7f%n",
					AlignmentMath.ssm1ax1n,AlignmentMath.ssm1ax1p,
					AlignmentMath.ssm1ax2n,AlignmentMath.ssm1ax2p);
			
			if(coaddRoutine){
//				runCoadd(chM2);
			}else{
				runNoCoadd(chM2);
			}
			
			System.out.format("SY2:    1-,       1+,       2-,        2+ [deg/step]%n");
			System.out.format("%8.7f, %8.7f, %8.7f, %8.7f%n",
					AlignmentMath.ssm2ax1n,AlignmentMath.ssm2ax1p,
					AlignmentMath.ssm2ax2n,AlignmentMath.ssm2ax2p);

		}catch(Exception ex){
			if(ex instanceof IOException){
				ai.disconnect();
				System.err.println(ex.getMessage());
			}else if(ex instanceof AgilisException){
				ai.disconnect();
				System.err.println(ex.getMessage());
			}else if(ex instanceof NativeImageException){
				timg.termImage();
				simg.termImage();
				System.err.println(ex.getMessage());
			}else{
				throw new RuntimeException(ex);
			}							
		}
		
		System.out.println("%%%%");
		theLock = false;
		ai.setExclusiveAccessLock(false,thisThread.getName());
	}
	
	public void runCoadd(int M) {
		//TODO
	}
	
	public void runNoCoadd(int M) throws IOException, AgilisException, InterruptedException{
		Point2D.Double tref;
		Point2D.Double toff;
		Point2D.Double sref;
		Point2D.Double soff;

		tref = timg.getRefCentroid();
		toff = timg.getCentroid();
		sref = simg.getRefCentroid();
		soff = simg.getCentroid();
			
		if(!ai.isConnected()){
			ai.connect();
		}
		
		ai.setChannel(M);
		ai.setEqualStepSize(stepAmpl);
		
		timg.setRefCentroid();
		simg.setRefCentroid();
		Thread.sleep(500);		
		
		//Measure 1- direction
		ai.stepAxis1(-nsteps);
		while(ai.isMoving(1))
			Thread.sleep(1000);
		AlignmentMath.computeThetaCorrection(tref, toff, sref, soff);
		AlignmentMath.computeAngularExtent(tref, toff);
		AlignmentMath.computeStepSize(M, -nsteps, 0);
		if(guiMode) reportTable.updateTable("1-");
		timg.setRefCentroid();
		simg.setRefCentroid();
		
		//Measure 1+ direction
		ai.stepAxis1(nsteps);
		while(ai.isMoving(1))
			Thread.sleep(1000);
		AlignmentMath.computeThetaCorrection(tref, toff, sref, soff);
		AlignmentMath.computeAngularExtent(tref, toff);
		AlignmentMath.computeStepSize(M, nsteps, 0);
		if(guiMode) reportTable.updateTable("1+");
		timg.setRefCentroid();
		simg.setRefCentroid();
		
		//Measure 2- direction
		ai.stepAxis2(-nsteps);
		while(ai.isMoving(2))
			Thread.sleep(1000);
		AlignmentMath.computeThetaCorrection(tref, toff, sref, soff);
		AlignmentMath.computeAngularExtent(tref, toff);
		AlignmentMath.computeStepSize(M, 0, -nsteps);
		if(guiMode) reportTable.updateTable("2-");
		timg.setRefCentroid();
		simg.setRefCentroid();
					
		//Measure 2+ direction
		ai.stepAxis2(nsteps);
		while(ai.isMoving(2))
			Thread.sleep(1000);
		AlignmentMath.computeThetaCorrection(tref, toff, sref, soff);
		AlignmentMath.computeAngularExtent(tref, toff);
		AlignmentMath.computeStepSize(M, 0, nsteps);
		if(guiMode) reportTable.updateTable("2+");
		
		//Move back to starting position, this attempts to correct
		//for one direction being more sensitive than another,
		ai.stepAxis2(-nsteps);
		while(ai.isMoving(2))
			Thread.sleep(1000);
		
	}

//	public void runNoCoadd(int M) throws IOException, AgilisException, InterruptedException{
//		Point2D.Double tref;
//		Point2D.Double toff;
//		Point2D.Double sref;
//		Point2D.Double soff;
//
//		double dthetax, dthetay; 
//		
//		tref = timg.getRefCentroid();
//		toff = timg.getCentroid();
//		sref = simg.getRefCentroid();
//		soff = simg.getCentroid();
//			
//		if(!ai.isConnected()){
//			ai.connect();
//		}
//		
//		ai.setChannel(M);
//		ai.setEqualStepSize(stepAmpl);
//		
//		timg.setRefCentroid();
//		simg.setRefCentroid();
//		Thread.sleep(500);		
//		
//		ai.stepAxis1(-nsteps);
//		while(ai.isMoving(1))
//			Thread.sleep(1000);
//		AlignmentMath.computeThetaCorrection(tref, toff, sref, soff);
//		AlignmentMath.computeAngularExtent(tref, toff);
//		AlignmentMath.computeStepSize2(M, -nsteps, 0);
//		System.out.format("1- :  Hxdt = %.4f    Hydt = %.4f    Hxds = %.4f    Hyds = %.4f%n",
//				AlignmentMath.Hdt.x,AlignmentMath.Hdt.y,AlignmentMath.Hds.x,AlignmentMath.Hds.y);
//		System.out.format("1- :   dthetaX_s1=%.7f    dthetaY_s1=%.7f    dthetaX_s2=%.7f    dthetaY_s2=%.7f%n",
//				-1*AlignmentMath.acm1x,-1*AlignmentMath.acm1y,-1*AlignmentMath.acm2x,-1*AlignmentMath.acm2y);
//		dthetay = -1*(AlignmentMath.acm1y+AlignmentMath.acm2x);
//		dthetax = -1*(AlignmentMath.acm1x+AlignmentMath.acm2y);
//		System.out.format("1- : dthetaX = %.7f   dthetaY = %.7f   dtheta_tilt = %.7f%n",dthetax,dthetay,AlignmentMath.theta);
//		if(guiMode) reportTable.updateTable("1-");
//		timg.setRefCentroid();
//		simg.setRefCentroid();
//		
//		//move back to start point
//		ai.stepAxis1(nsteps);
//		while(ai.isMoving(1))
//			Thread.sleep(1000);
//		AlignmentMath.computeThetaCorrection(tref, toff, sref, soff);
//		AlignmentMath.computeAngularExtent(tref, toff);
//		AlignmentMath.computeStepSize2(M, nsteps, 0);
//		System.out.format("1+ :  Hxdt = %.4f    Hydt = %.4f    Hxds = %.4f    Hyds = %.4f%n",
//				AlignmentMath.Hdt.x,AlignmentMath.Hdt.y,AlignmentMath.Hds.x,AlignmentMath.Hds.y);
//		System.out.format("1+ :   dthetaX_s1=%.7f    dthetaY_s1=%.7f    dthetaX_s2=%.7f    dthetaY_s2=%.7f%n",
//				-1*AlignmentMath.acm1x,-1*AlignmentMath.acm1y,-1*AlignmentMath.acm2x,-1*AlignmentMath.acm2y);
//		dthetay = -1*(AlignmentMath.acm1y+AlignmentMath.acm2x);
//		dthetax = -1*(AlignmentMath.acm1x+AlignmentMath.acm2y);
//		System.out.format("1+ : dthetaX = %.7f   dthetaY = %.7f   dtheta_tilt = %.7f%n",dthetax,dthetay,AlignmentMath.theta);
//		if(guiMode) reportTable.updateTable("1+");
//		timg.setRefCentroid();
//		simg.setRefCentroid();
//		
//		ai.stepAxis2(-nsteps);
//		while(ai.isMoving(2))
//			Thread.sleep(1000);
//		AlignmentMath.computeThetaCorrection(tref, toff, sref, soff);
//		AlignmentMath.computeAngularExtent(tref, toff);
//		AlignmentMath.computeStepSize2(M, 0, -nsteps);
//		System.out.format("2- :  Hxdt = %.4f    Hydt = %.4f    Hxds = %.4f    Hyds = %.4f%n",
//				AlignmentMath.Hdt.x,AlignmentMath.Hdt.y,AlignmentMath.Hds.x,AlignmentMath.Hds.y);
//		System.out.format("2- :   dthetaX_s1=%.7f    dthetaY_s1=%.7f    dthetaX_s2=%.7f    dthetaY_s2=%.7f%n",
//				-1*AlignmentMath.acm1x,-1*AlignmentMath.acm1y,-1*AlignmentMath.acm2x,-1*AlignmentMath.acm2y);
//		dthetay = -1*(AlignmentMath.acm1y+AlignmentMath.acm2x);
//		dthetax = -1*(AlignmentMath.acm1x+AlignmentMath.acm2y);
//		System.out.format("2- : dthetaX = %.7f   dthetaY = %.7f   dtheta_tilt = %.7f%n",dthetax,dthetay,AlignmentMath.theta);
//		if(guiMode) reportTable.updateTable("2-");
//		timg.setRefCentroid();
//		simg.setRefCentroid();
//					
//		//move back to start point
//		ai.stepAxis2(nsteps);
//		while(ai.isMoving(2))
//			Thread.sleep(1000);
//		AlignmentMath.computeThetaCorrection(tref, toff, sref, soff);
//		AlignmentMath.computeAngularExtent(tref, toff);
//		AlignmentMath.computeStepSize2(M, 0, nsteps);
//		System.out.format("2+ :  Hxdt = %.4f    Hydt = %.4f    Hxds = %.4f    Hyds = %.4f%n",
//				AlignmentMath.Hdt.x,AlignmentMath.Hdt.y,AlignmentMath.Hds.x,AlignmentMath.Hds.y);
//		System.out.format("2+ :   dthetaX_s1=%.7f    dthetaY_s1=%.7f    dthetaX_s2=%.7f    dthetaY_s2=%.7f%n",
//				-1*AlignmentMath.acm1x,-1*AlignmentMath.acm1y,-1*AlignmentMath.acm2x,-1*AlignmentMath.acm2y);
//		dthetay = -1*(AlignmentMath.acm1y+AlignmentMath.acm2x);
//		dthetax = -1*(AlignmentMath.acm1x+AlignmentMath.acm2y);
//		System.out.format("2+ : dthetaX = %.7f   dthetaY = %.7f   dtheta_tilt = %.7f%n",dthetax,dthetay,AlignmentMath.theta);
//		if(guiMode) reportTable.updateTable("2+");
//		
//		//Move back to starting position
//		ai.stepAxis2(-nsteps);
//		while(ai.isMoving(2))
//			Thread.sleep(1000);
//		
////		AlignmentMath.setSingleStepSize();
//	}

}
 