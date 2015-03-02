package data;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import java.awt.geom.Point2D;
import java.util.Hashtable;

import util.NumberObserver;

/**<pre>
 * This class implements all mathematical operations that used
 * throughout the TiltShearTool program. 
 * 
 * It also serves as a global storage class for variables that
 * need to be accessed by other classes.  
 * 
 * AlignmentMath defines all methods and variables as static.  This essentially
 * make AlignmentMath a singleton class, there is no constructor and instantiating
 * AlignmentMath has no effect, as all instances will share the same variables.
 *</pre>
 */
final public class AlignmentMath {
	static SetupData setup;
	
	public static Point2D.Double Hdt=new Point2D.Double();
	public static Point2D.Double Hds=new Point2D.Double();
	
	public static double m1ax1p, m1ax1n, m1ax2p, m1ax2n;
	public static double m2ax1p, m2ax1n, m2ax2p, m2ax2n;
	
	//step-size-mirror#-axis#-direction 
	//measured average step size:
	public static double ssm1ax1p=0.005, ssm1ax1n=0.005, ssm1ax2p=0.005, ssm1ax2n=0.005;
	public static double ssm2ax1p=0.005, ssm2ax1n=0.005, ssm2ax2p=0.005, ssm2ax2n=0.005;
	
	//angular-correction-mirror#-axis
	public static double acm1x, acm1y, acm2x, acm2y;
	
	//step-correction-mirror#-axis#
	public static int scm1ax1, scm1ax2, scm2ax1, scm2ax2;
	
	public static PIDMath m1Xloop;
	public static PIDMath m1Yloop;
	public static PIDMath m2Xloop;
	public static PIDMath m2Yloop;
	
	public static NumberObserver Hxdt = new NumberObserver();
	public static NumberObserver Hydt = new NumberObserver();
	public static NumberObserver Hxds = new NumberObserver();
	public static NumberObserver Hyds = new NumberObserver();
	
	public static NumberObserver Hxdt_r = new NumberObserver();
	public static NumberObserver Hydt_r = new NumberObserver();
	public static NumberObserver Hxds_r = new NumberObserver();
	public static NumberObserver Hyds_r = new NumberObserver();
	
	public static NumberObserver acm1xObs = new NumberObserver();
	public static NumberObserver acm1yObs = new NumberObserver();
	public static NumberObserver acm2xObs = new NumberObserver();
	public static NumberObserver acm2yObs = new NumberObserver();
	
	public static NumberObserver scm1ax1Obs = new NumberObserver();
	public static NumberObserver scm1ax2Obs = new NumberObserver();
	public static NumberObserver scm2ax1Obs = new NumberObserver();
	public static NumberObserver scm2ax2Obs = new NumberObserver();
	
	public static NumberObserver ssm1ax1pObs = new NumberObserver();
	public static NumberObserver ssm1ax2pObs = new NumberObserver();
	public static NumberObserver ssm2ax1pObs = new NumberObserver();
	public static NumberObserver ssm2ax2pObs = new NumberObserver();
	public static NumberObserver ssm1ax1nObs = new NumberObserver();
	public static NumberObserver ssm1ax2nObs = new NumberObserver();
	public static NumberObserver ssm2ax1nObs = new NumberObserver();
	public static NumberObserver ssm2ax2nObs = new NumberObserver();
	
	public static NumberObserver thetaObs = new NumberObserver();
	
	/* ******************** */
	
	/** Distance between SY mirrors 1 and 2 */
	private static double Ls2;  
	
	//Tilt Detector
	/** Distance from SY mirror 2 to the lens (included distance traveled to going from BC mirrors 1 and 2)*/
	private static double Ll;
	/**Distance from the lens to the tilt detector*/
	private static double Ld;  
	/**Focal length of the lens in the tilt detector*/
	private static double F;

	//Shear Detector
	/** Distance from SY mirror 2 to the beam splitter in the shear detector */
	private static double Lbs;  
	/** Distance from the beam splitter to the beam compressor */
	private static double Lc;
	/** Magnification of the beam compressor*/
	private static double M;
	/** Distance from the beam compressor to the shear detector */
	private static double Lr;

	//Pixel Size
	/** pixel length in x-direction in mm */
	private static double xpix;  
	/** pixel length in y-direction in mm */
	private static double ypix;
	
	//
	static int chM1;
	static int chM2;

	/** Retrieve latest setup values from SetupData class and store
	 * to corresponding local variables.  
	 */
	public static void updateVars() {
		Hashtable<String,Object> vart = SetupData.vart;
		Ls2 =(Double) vart.get("Ls2");
		Ll=(Double) vart.get("Ll");
		Ld=(Double) vart.get("Ld");
		F = (Double) vart.get("F");
		Lbs = (Double) vart.get("Lbs");
		Lc = (Double) vart.get("Lc");
		M = (Double) vart.get("M");
		Lr = (Double) vart.get("Lr");
		xpix = (Double) vart.get("xpix");
		ypix = (Double) vart.get("ypix");
		
		chM1 = ((Double) vart.get("chM1")).intValue();
		chM2 = ((Double) vart.get("chM2")).intValue();
	}
	
	/**<pre>
	 * This method serves no functional purpose.  It computes the theta correction equations
	 * in terms of the dependent variables: the offsets measured by the tilt and shear detectors.
	 * It then prints the equations to stdout.
	 * </pre>
	 * */
	public static void printThetaCorrectionEqns(){
		double cf = 180.0/Math.PI;  //%Conversion factor to convert the angles from radians to degrees
	
		double den = Ls2*(Ld*(Lr*pow(M,2) - Ll + F + Lbs + Lc) + F*(Ll - Lr*pow(M,2) - Lc - Lbs));

		double xnum_s1_a = F*(Lc + Lbs + Lr*pow(M,2));
		double xnum_s1_b = M*(Ld*Ll - Ld*F - Ll*F);
		
		double xnum_s2_a = F*(Lc + Lbs + Lr*pow(M,2) + Ls2);
		double xnum_s2_b = M*(Ld*Ll - Ld*F - Ll*F - Ls2*F + Ls2*Ld);
		
		double xs1_a = -0.5*cf*( xnum_s1_a / den );  
		double xs1_b = -0.5*cf*( xnum_s1_b / den ); 
		
		double xs2_a = -0.5*cf*( xnum_s2_a / den );
		double xs2_b = -0.5*cf*( xnum_s2_b / den );
		
		double ynum_s1_a = F*(Lc + Lbs + Lr*pow(M,2));
		double ynum_s1_b = M*(Ld*Ll - Ld*F - Ll*F);
		
		double ynum_s2_a = F*(Lc + Lbs + Lr*pow(M,2) + Ls2);
		double ynum_s2_b = M*(Ld*Ll - Ld*F - Ll*F - Ls2*F + Ls2*Ld);
		
		double ys1_a = 0.5*cf*(ynum_s1_a/den);
		double ys1_b = 0.5*cf*(ynum_s1_b/den);
		
		double ys2_a = 0.5*cf*(ynum_s2_a/den);
		double ys2_b = 0.5*cf*(ynum_s2_b/den);
		
		System.out.format("theta_xs1 = %.4f * Hydt_r + %.4f * Hyds_r%n",xs1_a,xs1_b);
		System.out.format("theta_ys1 = %.4f * Hxdt_r + %.4f * Hxds_r%n",ys1_a,ys1_b);
		
		System.out.format("theta_xs2 = %.4f * Hydt_r + %.4f * Hyds_r%n",xs2_a,xs2_b);
		System.out.format("theta_ys2 = %.4f * Hxdt_r + %.4f * Hxds_r%n",ys2_a,ys2_b);
	}
	
	/**<pre>  
	 * This method computes the angular corrections for the switchyard mirror mounts
	 * using the pixel offsets measured by the tilt and shear detectors. There are four
	 * equations: 
	 * 
	 *  theta_xs1 : angular rotation about x axis SY1 
	 *  theta_ys1 : angular rotation about y axis SY1
	 *  theta_xs2 : angular rotation about x axis SY2
	 *  theta_ys2 : angular rotation about y axis SY2
	 * 
	 * See report: "Re-Evaluation of the Automated Alignment System" by Kristina Nyland
	 * </pre>
	 */

	/** Scaled Working Algorithm -Sam **/
	public static void computeThetaCorrectionPowerScaled(Point2D.Double tref, Point2D.Double talign, 
			Point2D.Double sref, Point2D.Double salign) {
		updateVars();
		//Ls2*=-1; 
		double Ref_xt = tref.x;  //%the x location of the tilt reference beam in pixels
		double Ref_yt = tref.y;  //%the y location of the tilt reference beam in pixels
		double Ref_xs = sref.x;  //%the x location of the shear reference beam in pixels
		double Ref_ys = sref.y;  //%the y location of the shear reference beam in pixels

		double Align_xt = talign.x;  //%the x location of the tilt detector beam needing alignment in pixels
		double Align_yt = talign.y;  //%the y location of the tilt detector beam needing alignment in pixels
		double Align_xs = salign.x;  //%the x location of the shear detector beam needing alignment in pixels
		double Align_ys = salign.y;  //%the y location of the shear detector beam needing alignment in pixels
		
		//compute offsets in Pixels
		double Hxdt_pix = (Align_xt - Ref_xt);  //%the x offset from the reference beam in the tilt detector in pixels
		double Hydt_pix = (Align_yt - Ref_yt);  //%the y offset from the reference beam in the tilt detector in pixels
		double Hxds_pix = (Align_xs - Ref_xs);  //%the x offset from the reference beam in the shear detector in pixels
		double Hyds_pix = (Align_ys - Ref_ys);  //%the y offset from the reference beam in the shear detector in pixels
		
		//compute offset in mm
		double Hxdt = Hxdt_pix * xpix;  //%the x offset from the reference beam in the tilt detector in mm
		double Hydt = Hydt_pix * ypix;  //%the y offset from the reference beam in the tilt detector in mm
		double Hxds = Hxds_pix * xpix;  //%the x offset from the reference beam in the shear detector in mm
		double Hyds = Hyds_pix * ypix;  //%the y offset from the reference beam in the shear detector in mm
		
		double cf = 180.0/Math.PI;  //%Conversion factor to convert the angles from radians to degrees

		double den = Ls2*( Ld*(Lr*pow(M,2) - Ll + F + Lbs + Lc) + F*(Ll - Lr*pow(M,2) - Lc - Lbs));

		double xnum_s1_a = F*(Lc + Lbs + Lr*pow(M,2));
		double xnum_s1_b = M*(Ld*Ll - Ld*F - Ll*F);
		
		double xnum_s2_a = F*(Lc + Lbs + Lr*pow(M,2) + Ls2);
		double xnum_s2_b = M*(Ld*Ll - Ld*F - Ll*F - Ls2*F + Ls2*Ld);
		 
		double ynum_s1_a = F*(Lc + Lbs + Lr*pow(M,2));
		double ynum_s1_b = M*(Ld*Ll - Ld*F - Ll*F);
		
		double ynum_s2_a = F*(Lc + Lbs + Lr*pow(M,2) + Ls2);
		double ynum_s2_b = M*(Ld*Ll - Ld*F - Ll*F - Ls2*F + Ls2*Ld);
		
		//Sets to correct orientation for calculating corrections for mirror 1
		Hxdt *= -1; 
		Hydt *= 1;  
		Hxds *= -1; 
		Hyds *= -1; 
		
			//Calculates corrections
		double theta_xs1 = -0.5*( xnum_s1_a*Hydt + xnum_s1_b*Hyds)/den; 
		double theta_ys1 = 0.5*( ynum_s1_a*Hxdt + ynum_s1_b*Hxds )/den;
		
		//Changes orientation to calculated corrections for mirror 2 
		Hxdt *= -1; 
		Hydt *=  1; 
		Hxds *= -1; 
		Hyds *=  1; 
		
			//Calculates corrections
		double theta_xs2 = -0.5*( xnum_s2_a*Hydt + xnum_s2_b*Hyds )/den;
		double theta_ys2 = 0.5*( ynum_s2_a*Hxdt + ynum_s2_b*Hxds )/den;
		
		double POW = 0.75; 
		
		theta_xs1 = Math.pow(Math.abs(theta_xs1),POW)*theta_xs1/Math.abs(theta_xs1);
		theta_ys1 = Math.pow(Math.abs(theta_ys1),POW)*theta_ys1/Math.abs(theta_ys1);
		theta_xs2 = Math.pow(Math.abs(theta_xs2),POW)*theta_xs2/Math.abs(theta_xs2);
		theta_ys2 = Math.pow(Math.abs(theta_ys2),POW)*theta_ys2/Math.abs(theta_ys2);
		
		//Performs rotations to mirror system
		double theta_xs1_r = (1*sqrt(2)/2)*(theta_xs1 - theta_ys1);
		double theta_ys1_r = (1*sqrt(2)/2)*(theta_xs1 + theta_ys1);
		double theta_xs2_r = (1*sqrt(2)/2)*(theta_xs2 - theta_ys2);
		double theta_ys2_r = (1*sqrt(2)/2)*(theta_xs2 + theta_ys2);
		
		//%%% Report Back angular corrections in degrees in the structure 'corrections_rot' in the mount frame %%%
		double theta_xs1_rot_deg = theta_xs1_r*cf;  
		double theta_xs2_rot_deg = theta_xs2_r*cf;  
		double theta_ys1_rot_deg = theta_ys1_r*cf;  
		double theta_ys2_rot_deg = theta_ys2_r*cf;  
	
		Hdt.x = Hxdt_pix;
		Hdt.y = Hydt_pix;
		Hds.x = Hxds_pix;
		Hds.y = Hyds_pix;	
		
		//This stores the calculated values to the global fields
		acm1x=-1*theta_xs1_rot_deg; 
		acm1y=-1*theta_ys1_rot_deg; 
		acm2x=-1*theta_xs2_rot_deg;
		acm2y=-1*theta_ys2_rot_deg;
		
		AlignmentMath.Hxdt.setNumber(Hxdt);
		AlignmentMath.Hydt.setNumber(Hydt);
		AlignmentMath.Hxds.setNumber(Hxds);
		AlignmentMath.Hyds.setNumber(Hyds);
		
		if (AlignmentMath.Hxdt_r.getDouble() == 0 && AlignmentMath.Hydt_r.getDouble()==0 &&
				AlignmentMath.Hxds_r.getDouble() == 0 && AlignmentMath.Hyds_r.getDouble() == 0 ){
			AlignmentMath.Hxdt_r.setNumber(Hxdt);
			AlignmentMath.Hydt_r.setNumber(Hydt);
			AlignmentMath.Hxds_r.setNumber(Hxds);
			AlignmentMath.Hyds_r.setNumber(Hyds);
		}

		acm1xObs.setNumber(acm1x);
		acm1yObs.setNumber(acm1y);
		acm2xObs.setNumber(acm2x);
		acm2yObs.setNumber(acm2y);
	}
	
	public static void computeThetaCorrection(Point2D.Double tref, Point2D.Double talign, 
			Point2D.Double sref, Point2D.Double salign) {
		updateVars();
		//Ls2*=-1; 
		double Ref_xt = tref.x;  //%the x location of the tilt reference beam in pixels
		double Ref_yt = tref.y;  //%the y location of the tilt reference beam in pixels
		double Ref_xs = sref.x;  //%the x location of the shear reference beam in pixels
		double Ref_ys = sref.y;  //%the y location of the shear reference beam in pixels

		double Align_xt = talign.x;  //%the x location of the tilt detector beam needing alignment in pixels
		double Align_yt = talign.y;  //%the y location of the tilt detector beam needing alignment in pixels
		double Align_xs = salign.x;  //%the x location of the shear detector beam needing alignment in pixels
		double Align_ys = salign.y;  //%the y location of the shear detector beam needing alignment in pixels
		
		//compute offsets in Pixels
		double Hxdt_pix = (Align_xt - Ref_xt);  //%the x offset from the reference beam in the tilt detector in pixels
		double Hydt_pix = (Align_yt - Ref_yt);  //%the y offset from the reference beam in the tilt detector in pixels
		double Hxds_pix = (Align_xs - Ref_xs);  //%the x offset from the reference beam in the shear detector in pixels
		double Hyds_pix = (Align_ys - Ref_ys);  //%the y offset from the reference beam in the shear detector in pixels
		
		//compute offset in mm
		double Hxdt = Hxdt_pix * xpix;  //%the x offset from the reference beam in the tilt detector in mm
		double Hydt = Hydt_pix * ypix;  //%the y offset from the reference beam in the tilt detector in mm
		double Hxds = Hxds_pix * xpix;  //%the x offset from the reference beam in the shear detector in mm
		double Hyds = Hyds_pix * ypix;  //%the y offset from the reference beam in the shear detector in mm
		
		double cf = 180.0/Math.PI;  //%Conversion factor to convert the angles from radians to degrees

		double den = Ls2*( Ld*(Lr*pow(M,2) - Ll + F + Lbs + Lc) + F*(Ll - Lr*pow(M,2) - Lc - Lbs));

		double xnum_s1_a = F*(Lc + Lbs + Lr*pow(M,2));
		double xnum_s1_b = M*(Ld*Ll - Ld*F - Ll*F);
		
		double xnum_s2_a = F*(Lc + Lbs + Lr*pow(M,2) + Ls2);
		double xnum_s2_b = M*(Ld*Ll - Ld*F - Ll*F - Ls2*F + Ls2*Ld);
		 
		double ynum_s1_a = F*(Lc + Lbs + Lr*pow(M,2));
		double ynum_s1_b = M*(Ld*Ll - Ld*F - Ll*F);
		
		double ynum_s2_a = F*(Lc + Lbs + Lr*pow(M,2) + Ls2);
		double ynum_s2_b = M*(Ld*Ll - Ld*F - Ll*F - Ls2*F + Ls2*Ld);
		
		//Sets to correct orientation for calculating corrections for mirror 1
		Hxdt *= -1; 
		Hydt *= 1;  
		Hxds *= -1; 
		Hyds *= -1; 
		
			//Calculates corrections
		double theta_xs1 = -0.5*( xnum_s1_a*Hydt + xnum_s1_b*Hyds)/den; 
		double theta_ys1 = 0.5*( ynum_s1_a*Hxdt + ynum_s1_b*Hxds )/den;
		
		//Changes orientation to calculated corrections for mirror 2 
		Hxdt *= -1; 
		Hydt *=  1; 
		Hxds *= -1; 
		Hyds *=  1; 
		
			//Calculates corrections
		double theta_xs2 = -0.5*( xnum_s2_a*Hydt + xnum_s2_b*Hyds )/den;
		double theta_ys2 = 0.5*( ynum_s2_a*Hxdt + ynum_s2_b*Hxds )/den;
		
		//Performs rotations to mirror system after other calculations
		double theta_xs1_r = (1*sqrt(2)/2)*(theta_xs1 - theta_ys1);
		double theta_ys1_r = (1*sqrt(2)/2)*(theta_xs1 + theta_ys1);
		double theta_xs2_r = (1*sqrt(2)/2)*(theta_xs2 - theta_ys2);
		double theta_ys2_r = (1*sqrt(2)/2)*(theta_xs2 + theta_ys2);
		
		//%%% Report Back angular corrections in degrees in the structure 'corrections_rot' in the mount frame %%%
		double theta_xs1_rot_deg = theta_xs1_r*cf;  
		double theta_xs2_rot_deg = theta_xs2_r*cf;  
		double theta_ys1_rot_deg = theta_ys1_r*cf;  
		double theta_ys2_rot_deg = theta_ys2_r*cf;  
	
		Hdt.x = Hxdt_pix;
		Hdt.y = Hydt_pix;
		Hds.x = Hxds_pix;
		Hds.y = Hyds_pix;	
		
		//This stores the calculated values to the global fields
		acm1x=-1*theta_xs1_rot_deg; 
		acm1y=-1*theta_ys1_rot_deg; 
		acm2x=-1*theta_xs2_rot_deg;
		acm2y=-1*theta_ys2_rot_deg;
		
		AlignmentMath.Hxdt.setNumber(Hxdt);
		AlignmentMath.Hydt.setNumber(Hydt);
		AlignmentMath.Hxds.setNumber(Hxds);
		AlignmentMath.Hyds.setNumber(Hyds);
		
		if (AlignmentMath.Hxdt_r.getDouble() == 0 && AlignmentMath.Hydt_r.getDouble()==0 &&
				AlignmentMath.Hxds_r.getDouble() == 0 && AlignmentMath.Hyds_r.getDouble() == 0 ){
			AlignmentMath.Hxdt_r.setNumber(Hxdt);
			AlignmentMath.Hydt_r.setNumber(Hydt);
			AlignmentMath.Hxds_r.setNumber(Hxds);
			AlignmentMath.Hyds_r.setNumber(Hyds);
		}

		acm1xObs.setNumber(acm1x);
		acm1yObs.setNumber(acm1y);
		acm2xObs.setNumber(acm2x);
		acm2yObs.setNumber(acm2y);
	}
	
	/**<pre>
	 * This method computes the number of steps each axis needs to be moved based on the angular
	 * correction value mXaxY and its corresponding step size ssmXaxY where 'X' is the mirror number
	 * and 'Y' is the axis number. 
	 * 
	 * The step correction value is saved to AlignmentMath.scmXaxY .
	 * 
	 * Axes definitions assumed by the angular correction calculation:
	 * 
	 * SY1:
	 *  1- : positive rotation about Y,
	 *  1+ : negative rotation about Y,
	 *  2- : negative rotation about X,
	 *  2+ : positive rotation about X,
	 *  
	 *  SY2
	 *  2- : pos about X
	 *  2+ : neg about X
	 *  1+ : pos about Y
	 *  1- : neg about Y
	 * </pre> 
	 * @param acm1X
	 * @param acm1Y
	 * @param acm2X
	 * @param acm2Y
	 */

	public static void computeStepCorrection(double acm1X, double acm1Y, double acm2X, double acm2Y){
			if(acm1X>0){
				//SY1 1-
				scm1ax1 = -1*(int)Math.round( abs(acm1X)/ssm1ax1n );
			}else{
				//SY1 1+
				scm1ax1 = 1*(int)Math.round( abs(acm1X)/ssm1ax1p );
			}
			
			if(acm1Y>0){
				//SY1 2-
				scm1ax2 = -1*(int)Math.round( abs(acm1Y)/ssm1ax2n );
			}else{
				//SY1 2+
				scm1ax2 = 1*(int)Math.round( abs(acm1Y)/ssm1ax2p );
			}
			
			if(acm2Y>0){
				//SY2 1-
				scm2ax1 = -1*(int)Math.round( abs(acm2Y)/ssm2ax1n );
			}else{
				//SY2 1+
				scm2ax1 = 1*(int)Math.round( abs(acm2Y)/ssm2ax1p );
			}
			
			if(acm2X>0){
				//SY2 2-
				scm2ax2 = -1*(int)Math.round( abs(acm2X)/ssm2ax2n );
			}else{
				//SY2 2+
				scm2ax2 = 1*(int)Math.round( abs(acm2X)/ssm2ax2p );
				}
	}
	public static double theta;	
	public static void computeAngularExtent(Point2D.Double tref, Point2D.Double toff){		
		double dx = (toff.x - tref.x)*xpix; //offset in mm
		double dy = (toff.y - tref.y)*ypix; //offset in mm
		
		dx*=1;
		dy*=-1;
		
		double d = Math.sqrt(dx*dx + dy*dy);
		
//		System.out.format("[angle]  dx=%.2f  dy=%.2f  d=%f [mm] : ",dx,dy,d);
//		System.out.format("[angle]  d=%f [mm] : ",d);
		theta = Math.atan( d/(2*F) )*2;
		
		theta*=(180/Math.PI);
		
		if(dx>0 && dy>0 ){
			//theta positive stays the same
		}else if(dx<0 && dy>0){
			//theta positive stays the same
		}else if(dx<0 && dy<0){
			theta*=-1;
		}else if(dx>0 && dy<0){
			theta*=-1;
		}
		
		thetaObs.setNumber(theta);
	}
	
	public static void computeStepSize(int mirror, int nax1, int nax2){
		
		if(mirror==chM1){
			//Only compute when the axis was actually moved,
			//i.e., nonzero n value
			if(nax1!=0){
				if(nax1>0){
					ssm1ax1p = abs(theta/nax1);
				}else{ 
					ssm1ax1n = abs(theta/nax1);
				}
			}
			
			if(nax2!=0){
				if(nax2>0){
					ssm1ax2p = abs(theta/nax2);
				}else{ 
					ssm1ax2n = abs(theta/nax2);
				}
			}
		}else if(mirror == chM2){
			if(nax1!=0){
				if(nax1>0){
					ssm2ax1p = abs(theta/nax1);
				}else{ 
					ssm2ax1n = abs(theta/nax1);
				}
			}
			
			if(nax2!=0){
				if(nax2>0){
					ssm2ax2p = abs(theta/nax2);
				}else{ 
					ssm2ax2n = abs(theta/nax2);
				}
			}
		}	

		ssm1ax1pObs.setNumber(ssm1ax1p);
		ssm1ax1nObs.setNumber(ssm1ax1n);
		ssm1ax2pObs.setNumber(ssm1ax2p);
		ssm1ax2nObs.setNumber(ssm1ax2n);
		ssm2ax1pObs.setNumber(ssm2ax1p);
		ssm2ax1nObs.setNumber(ssm2ax1n);
		ssm2ax2pObs.setNumber(ssm2ax2p);
		ssm2ax2nObs.setNumber(ssm2ax2n);
	}
	/** Make all step sizes equal to the largest value computed
	 * for a given mirror. 
	 */
	public static void setSingleStepSize(){
		double ssm1Max=0, ssm2Max=0;
		
		if(ssm1ax1p>ssm1Max){
			ssm1Max=ssm1ax1p;
		}else if(ssm1ax1n>ssm1Max){
			ssm1Max=ssm1ax1n;
		}else if(ssm1ax2p>ssm1Max){
			ssm1Max=ssm1ax2p;
		}else if(ssm1ax2n>ssm1Max){
			ssm1Max=ssm1ax2n;
		}
		
		if(ssm2ax1p>ssm2Max){
			ssm2Max=ssm2ax1p;
		}else if(ssm2ax1n>ssm2Max){
			ssm2Max=ssm2ax1n;
		}else if(ssm2ax2p>ssm2Max){
			ssm2Max=ssm2ax2p;
		}else if(ssm2ax2n>ssm2Max){
			ssm2Max=ssm2ax2n;
		}
		
		ssm1ax1p=ssm1Max;
		ssm1ax1n=ssm1Max;
		ssm1ax2p=ssm1Max;
		ssm1ax2n=ssm1Max;
		
		ssm2ax1p=ssm2Max;
		ssm2ax1n=ssm2Max;
		ssm2ax2p=ssm2Max;
		ssm2ax2n=ssm2Max;
	
	}
	
	public static void main(String[] args){
		double mmperpix = 0.01;
		updateVars();
		
		Point2D.Double tref = new Point2D.Double(0,0);
		Point2D.Double sref = new Point2D.Double(0,0);
		
		double dxt=0.003702;
		double dyt=-0.003702;
		double dxs=0.043;
		double dys=-0.043;
		
		Point2D.Double toff = new Point2D.Double(dxt/mmperpix, -1*dyt/mmperpix);
		Point2D.Double soff = new Point2D.Double(dxs/mmperpix, -1*dys/mmperpix);
		
		computeThetaCorrection(tref,toff,sref,soff);
		
		System.out.println("Working Algorithm");
		System.out.format("thetaXs1 = %f%n",acm1x);
		System.out.format("thetaYs1 = %f%n",acm1y);
		System.out.format("thetaXs2 = %f%n",acm2x);
		System.out.format("thetaYs2 = %f%n",acm2y);
		
	}
}
