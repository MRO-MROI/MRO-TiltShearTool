package impl;

import java.awt.geom.Point2D;
import java.io.IOException;

import agilis.Actuator;
import agilis.ActuatorInterface;
import agilis.AgilisException;

import xenimaq.NativeImageImpl;

/*This class will check for special exceptions to the PIControl math. So far it mainly 
 *just runs checkBeam which looks for errors in the beam and encodes the results in an integer.*/
public class PIRules {
//implement homing rule for when light beam gets completely missaligned
	//The prevTimg and prevSimg serve as a buffer holding the previos images in case they are needed.
	private NativeImageImpl prevTimg; 
	private NativeImageImpl prevSimg;
	private ActuatorInterface ai;
	
	public PIRules(NativeImageImpl timg, NativeImageImpl simg, ActuatorInterface ai){
		prevSimg = simg;
		prevTimg = timg;
		this.ai = ai;
	}
	public int checkBeam(NativeImageImpl timg, NativeImageImpl simg) throws IOException, AgilisException{
		int result = 0;
        if (simg.getCentroid().x == 0 && simg.getCentroid().y == 0){
			result = result + 1;
		}
        if (timg.getCentroid().x == 0 && timg.getCentroid().y == 0){
			result = result + 2;
		}

		int ch = ai.getChannel();
		ai.setChannel(1);
		int b = ai.getLimitStatus();
		ai.setChannel(2);
		int c = ai.getLimitStatus();
		ai.setChannel(ch);
		result = result + 4*b + 16*c;
		
		prevSimg = simg;
		prevTimg = timg;
		return result;
	}
}
