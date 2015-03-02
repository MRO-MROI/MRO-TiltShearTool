package impl;

import gui.ReportTable;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import util.NumberObserver;

import xenimaq.NativeImageImpl;
import data.AlignmentMath;

/**
 * Logger class is used to implement methods for starting threads that
 * print information to stdout.
 * 
 * TODO In the future this class should implement methods for threads that log
 * information to files.
 */
public class Logger {
	NativeImageImpl timg;
	NativeImageImpl simg;
	Point2D.Double tref, toff, sref, soff;
	
	Timer centroidsT, thetaCorrsT, pixOffsT;
	
	ReportTable rtable;
	boolean guiMode = false;
	
	String[] currentColumnNames;
	NumberObserver[] currentDataObservers;
	
	Vector<String> centroidNames = new Vector<String>(Arrays.asList(new String[]{"t(x) [px]","t(y)","s(x)","s(y)"}));
	Vector<NumberObserver> centroids;
	Vector<String> offsetNames = new Vector<String>(Arrays.asList(new String[] {"Hdxt","Hdyt","Hdxs","Hdys","Starting Hdxt","Starting Hdyt","Starting Hdxs","Starting Hdys"}));
	Vector<NumberObserver> offsets;
	Vector<String> thetaNames = new Vector<String>(Arrays.asList(new String[] {"\u03b8x_s1","\u03b8y_s1","\u03b8x_s2","\u03b8y_s2"}));
	Vector<NumberObserver> thetas;
	
	NumberObserver tX, tY, sX, sY;
	
	boolean dispCentroids = false, dispOffsets=false, dispThetas = false;
	double dtMin, dtMax; /*This is used to keep track of the amount of time between updating row when guiMode is true*/
	Thread rowUpdateThread;
	
	public Logger(NativeImageImpl simg, NativeImageImpl timg,ReportTable reportTable) {
		this.timg = timg;
		this.simg = simg;
		this.rtable = reportTable;
		
		if(rtable!=null){
			guiMode=true;
			
			tref=timg.getRefCentroid();
			toff=timg.getCentroid();
			sref=simg.getRefCentroid();
			soff=simg.getCentroid();
			
			tX = new NumberObserver(tref.x);
			tY = new NumberObserver(toff.y);
			sX = new NumberObserver(sref.x);
			sY = new NumberObserver(soff.y);
			
			centroids = new Vector<NumberObserver>( Arrays.asList(new NumberObserver[]{tX,tY,sX,sY}) );
			
			offsets = new Vector<NumberObserver>( Arrays.asList(new NumberObserver[]{
					AlignmentMath.Hxdt,AlignmentMath.Hydt,
					AlignmentMath.Hxds,AlignmentMath.Hyds,
					AlignmentMath.Hxdt_r,AlignmentMath.Hydt_r,
					AlignmentMath.Hxds_r,AlignmentMath.Hyds_r}) );
			
			thetas = new Vector<NumberObserver>( Arrays.asList(new NumberObserver[]{
					AlignmentMath.acm1xObs,AlignmentMath.acm1yObs,
					AlignmentMath.acm2xObs,AlignmentMath.acm2yObs}) );
			
		}
	}
	
	public void handleTableDataSetup(double dT) {
		Vector<String> names = new Vector<String>();
		Vector<NumberObserver> obss = new Vector<NumberObserver>();

		//Stop the update thread; 
		//temporarily if single logger has been enabled
		//permanently if all loggers are disabled
		if(rowUpdateThread!=null){
			rowUpdateThread.interrupt();
			rowUpdateThread = null;
		}
		
		//Check if all data loggers have been disabled, if yes
		//reset dt values,
		//and return without emptying the table
		if( !(dispCentroids || dispOffsets || dispThetas) ){
			dtMin=0;
			dtMax=0;
			return;
		}
		
		dtMin = (dT < dtMin) ? dT:dtMin;
		dtMax = (dT > dtMax) ? dT:dtMax;
		
		names.add("T [ms]");
		if(dispCentroids){
			names.addAll(centroidNames.subList(0,centroidNames.size()));
			obss.addAll(centroids.subList(0, centroids.size()));
		}
		
		if(dispOffsets){
			names.addAll(offsetNames.subList(0, offsetNames.size()));
			obss.addAll(offsets.subList(0, offsets.size()));
		}
		
		if(dispThetas){
			names.addAll(thetaNames.subList(0, thetaNames.size()));
			obss.addAll(thetas.subList(0, thetas.size()));
		}
		
		String[] namesArray=new String[names.size()];
		NumberObserver[] obssArray=new NumberObserver[obss.size()];
		names.toArray(namesArray);
		obss.toArray(obssArray);
		
		rtable.resetTable( namesArray );
		rtable.setDataObservers( obssArray );

	
		rowUpdateThread=new Thread( new Runnable(){			
			public void run(){
				long t0 = System.currentTimeMillis();
				while(true){
					try{
						tX.setNumber(toff.x);
						tY.setNumber(toff.y);
						sX.setNumber(soff.x);
						sY.setNumber(soff.y);
						AlignmentMath.computeThetaCorrection(tref,toff,sref,soff);
						rtable.updateTable( Double.toString( 1.0E3*(System.currentTimeMillis() - t0) ));
						Thread.sleep( (long)(dtMax*1E3) );
					}catch(InterruptedException ex){
						break;
					}
				}
			}
		});
		
		rowUpdateThread.start();

	}
	
	public void getPIStatus() {
		System.out.format("[PI Loop] SY1(X)  SY1(Y)  SY2(X)  SY2(Y) [deg]%n");
		System.out.format("%10.7f %10.7f %10.7f %10.7f%n",
				AlignmentMath.m1Xloop.getCurrentError(), AlignmentMath.m1Yloop.getCurrentError(),
				AlignmentMath.m2Xloop.getCurrentError(), AlignmentMath.m2Yloop.getCurrentError());
	}
	
	public void getStepSizes() {
		System.out.format("     M1AX1-    M1AX1+    M1AX2-    M1AX2+%n");
		System.out.format("%10.7f %10.7f %10.7f %10.7f%n",AlignmentMath.ssm1ax1n,AlignmentMath.ssm1ax1p,AlignmentMath.ssm1ax2n,AlignmentMath.ssm1ax2p);
		
		System.out.format("     M2AX1-    M2AX1+    M2AX2-    M2AX2+%n");
		System.out.format("%10.7f %10.7f %10.7f %10.7f%n",AlignmentMath.ssm2ax1n,AlignmentMath.ssm2ax1p,AlignmentMath.ssm2ax2n,AlignmentMath.ssm2ax2p);		
	}
	
	/** Turn on/off thread for logging latest centroid position values.  
	 * @param dT  Time interval, in seconds, to print data.
	 * @param on on/off switch
	 * */
	public void setupCentroids(double dT, boolean on) {		
		if(guiMode){
			if(on){
				dispCentroids = true;
			}else{
				dispCentroids = false;
			}
			
			handleTableDataSetup(dT);			
			return;
		}
		
	
		if(on){
			if(centroidsT !=null){
				throw new RuntimeException("Centroid logging already on.");
			}
		
			System.out.println("[centroids]    TiltX    TiltY    ShearX    ShearY");
		
		
			TimerTask task = new TimerTask(){
				long startt=System.currentTimeMillis();
				Point2D.Double tcent = new Point2D.Double();
				Point2D.Double scent = new Point2D.Double();
				
				public void run(){
					timg.getCentroid(tcent);
					simg.getCentroid(scent);
					
					if(guiMode){
					
					}else{
						System.out.format("[centroids] %9.4f %9.4f %9.4f %9.4f %9.4f%n",
								(System.currentTimeMillis()-startt)*1E-3,
								tcent.x,tcent.y,scent.x,scent.y);
					}
				}
				
			};
			
			centroidsT = new Timer();
			centroidsT.scheduleAtFixedRate( task,0,(long)(dT*1E3) );
	
		}else{
			if(centroidsT !=null){
				centroidsT.cancel();
				centroidsT=null;
			}
		}		

	}
	
	/** Turn on/off thread for logging latest angular correction values.  
	 * @param dT  Time interval, in seconds, to print data.
	 * @param on on/off switch
	 * */
	public void setupThetaCorrs(double dT, boolean on) {
		if(guiMode){
			if(on){
				dispThetas = true;
			}else{
				dispThetas = false;
			}
			
			handleTableDataSetup(dT);			
			return;
		}
		
		if(on){
			if(thetaCorrsT != null){
				throw new RuntimeException("Angular correction logging already on.");
			}
			
			System.out.println("[thetas] SY1X    SY1Y    SY2X    SY2Y");
			TimerTask task = new TimerTask(){
				long startt=System.currentTimeMillis();
				
				public void run(){
					AlignmentMath.computeThetaCorrection(timg.getRefCentroid(),timg.getCentroid(),
							simg.getRefCentroid(),simg.getCentroid());
					System.out.format("[thetas] %9.4f %9.7f %9.7f %9.7f %9.7f%n",
						(System.currentTimeMillis()-startt)*1E-3,
						AlignmentMath.acm1x,AlignmentMath.acm1y,
						AlignmentMath.acm2x,AlignmentMath.acm2y);
				}
				
			};
			
			thetaCorrsT = new Timer();
			thetaCorrsT.scheduleAtFixedRate(task,0,(long)(dT*1E3) );
		}else{
			if(thetaCorrsT!=null){
				thetaCorrsT.cancel();
				thetaCorrsT=null;
			}
		}
	}
	
	/** Turn on/off thread for logging latest pixel offset values.  
	 * @param dT  Time interval, in seconds, to print data.
	 * @param on on/off switch
	 * */
	public void setupPixOffs(double dT, boolean on) {
		if(guiMode){
			if(on){
				dispOffsets = true;
			}else{
				dispOffsets = false;
			}
			
			handleTableDataSetup(dT);			
			return;
		}
		
		if(on){
			System.out.println("[offsets] Hdxt    Hdyt    Hdxs    Hdys");
			
			TimerTask task = new TimerTask(){
				long startt=System.currentTimeMillis();
				
				public void run(){
					AlignmentMath.computeThetaCorrection( timg.getRefCentroid(),timg.getCentroid(),
							simg.getRefCentroid(),simg.getCentroid() );
					
					System.out.format("[offsets] %9.4f %9.4f %9.4f %9.4f %9.4f%n",
							(System.currentTimeMillis()-startt)*1E-3,
							AlignmentMath.Hdt.x,AlignmentMath.Hdt.y,
							AlignmentMath.Hds.x,AlignmentMath.Hds.y);
				}				
			};
			
			pixOffsT = new Timer();
			pixOffsT.scheduleAtFixedRate(task,0,(long)(dT*1E3) );
		}else{
			if(pixOffsT != null){
				pixOffsT.cancel();
				pixOffsT=null;
			}
		}
	}
	
}
