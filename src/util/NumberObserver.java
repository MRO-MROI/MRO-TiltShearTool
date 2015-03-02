package util;

import java.util.ArrayList;

/**
 * This class implements an observer that can register viewers
 * and update them on the state of an internal number value. 
 */
public class NumberObserver {
	private Number value;
	
	public NumberObserver(){
		super();
		value = new Integer(0);
	}
	
	/**
	 * Initialize the NumberObserver with a Number value.
	 * @param value
	 */
	public NumberObserver(Number value){
		super();
		this.value=value;
	}
	
	public void setNumber(int num){
		value = new Integer(num);
	}
	
	public void setNumber(float num){
		value = new Float(num);
	}
	
	public void setNumber(double num){
		value = new Double(num);
	}

	public double getDouble(){
		return value.doubleValue();
	}
	
	public float getFloat(){
		return value.floatValue();
	}
	
	public int getInt(){
		return value.intValue();
	}
	
	public String toString(){
		if(value==null){
			return "null";
		}
		
		if(value instanceof Integer){
			return ((Integer)value).toString();
		}else if(value instanceof Float){
			return ((Float)value).toString();
		}else if(value instanceof Double){
			return ((Double)value).toString();
		}else{
			System.err.println("???");
			return value.toString();
		}	
	}
}
