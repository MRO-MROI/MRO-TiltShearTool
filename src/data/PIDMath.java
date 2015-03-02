package data;

/**
 * This class implements the math for a discrete position-integral-derivative controller.
 * 
 * The mathematical equation describing the discrete PID controller:
 *                             
 * error = Kp*(SP - MP)  + Ki*( iState + (SP-MP) ) - Kd*( dState - (SP-MP) )
 *      
 * Note that this is the time independent version of the PID controller, hence
 * iState is simply the sum of all the previous errors and 
 * dState is the difference all previous errors.
 * 
 * Variations:
 * Gain values can be changed using the various set methods.
 * 
 * Kp=1, Ki=0, Kd=0: Simple position controller without integral or derivative compensation.
 * 
 * Kp=1, Ki>0, Kd=0: Position integral controller with integral compensation.  
 *  
 * Kp=1, Ki>0, Kd>0:  Position integral control with integral and differential compensation.
 *  
 * 
 * Credits: 
 *  The code was adapted from the code provided in the article:
 *  "PID Without A PHD" by Tim Wescott
 *  eetindia.com , October 2000
 *  
 */
public class PIDMath {
	double dState;
	double iState;
	
	double iMin=-1E12;
	double iMax=1E12;

	double iGain=0;
	double pGain=1;
	double dGain=0;
	
	double error; //current error

	/** Default constructor, starts the PID loop with default gain values
	 *  Kp = 1, Ki=0, Kd=0.   {@link #setGains(double, double, double)} can be used
	 *  to adjust the gain values. 
	 */
	public PIDMath(){
		
	}
	
	public PIDMath(double pGain, double iGain){
		this.iGain=iGain;
		this.pGain=pGain;
	}
	
	public PIDMath(double pGain, double iGain, double dGain){
		this.iGain=iGain;
		this.pGain=pGain;
		this.dGain=dGain;
	}
	
	/**
	 * This method will iterate the PID loop.  The purpose of the loop
	 * is to calculate the error, or correction amount, needed to minimize
	 * (setPos-measPos). 
	 * @param setPos The desired value. 
	 * @param measPos The measured value.
	 * @return
	 */
	public double updatePID(double setPos, double measPos){ 
		double inError;
		double pTerm, iTerm, dTerm;
		double outError;
		inError = setPos-measPos;
		pTerm=pGain*inError;
		
		iState+=inError;
		if(iState<iMin){
			iState = iMin;
		}else if(iState>iMax){
			iState = iMax;
		}
		
		iTerm=iGain*iState;
		
		dTerm=dGain*(dState-inError);
		dState = inError;
		
		outError = pTerm + iTerm - dTerm;
		
		error = outError;
		
		return outError;
	}
	
	/** Set position gain Kp*/
	public void setPGain(double gain){
		pGain = gain;
	}
	
	/** Set integral gain Ki*/
	public void setIGain(double gain){
		iGain = gain;
	}
	
	/** Set differential gain Kd*/
	public void setDGain(double gain){
		dGain = gain;
	}
	
	/** Set all three gain values.
	 * 
	 * @param Kp
	 * @param Ki
	 * @param Kd
	 */
	public void setGains(double Kp, double Ki, double Kd){
		this.pGain = Kp;
		this.iGain = Ki;
		this.dGain = Kd;
	}
	
	/**
	 * Set the limits for integral summation term.
	 * @param iMin Lower threshold for integral term, 
	 * @param iMax Upper threshold for integral term, 
	 */
	public void setLimits(double iMin, double iMax){
		this.iMin = iMin;
		this.iMax = iMax;
	}
	
	public double getCurrentError(){
		return error;
	}
	
}
