   package data;

import java.io.*;
import java.util.Calendar;
import java.util.Hashtable;

/**
 * SetupData class is used to keep track of all values that might need to be changed
 * by the user without having to recompile the program. 
 * 
 * All fields/methods in SetupData are static making SetupData a singleton class.  Only one
 * instance of SetupData should exist during the lifetime of the program.
 * 
 * Setup variables are stored in a Hashtable, {@link #vart}.  The Hashtable is designed
 * so that the key to the desired value matches the field name that will be used.
 * 
 * To reduce programming confusion, any code that accesses a value in the Hashtable should
 * make the field name match the hash key name. 
 */
final public class SetupData {
	public final static String pathsep=System.getProperty("file.separator");
	
	public static String wdir=".";
	public static String dataDir=".";
	
	public static Hashtable<String,Object> vart=new Hashtable<String,Object>(0);
	
	public static boolean debug=false;
	
	public static int currentMountStepAmplitude = 0;
	
	static {
		wdir=System.getProperty("user.dir");
		if(pathsep.equals("\\")){ //if running in windows
			dataDir=String.format("%s\\%2$tm%2$td20%2$ty",wdir,Calendar.getInstance());
		}else if(pathsep.equals("/")){ //if running in Linux
			dataDir=String.format("%s/%2$tm%2$td20%2$ty",wdir,Calendar.getInstance());
		}
		
		//build the default configuration variable table
		vart.put("wdir", wdir);
		vart.put("tiltCameraDevice", "BobcatTilt");
		vart.put("shearCameraDevice","BobcatShear");
		vart.put("shearCalibFile", "Calibrations/Shear/Bobcat2486_100us_highgain_2486.xca");
		vart.put("tiltCalibFile", "Calibrations/Tilt/Bobcat2478_100us_highgain_2478.xca");
		vart.put("xenicsColorProfile", "ColorProfiles/Grayscale.png");
		vart.put("agilisControllerDevice", "/dev/ttyUSB0");
		vart.put("chM1", new Double(1));
		vart.put("chM2", new Double(2));
//      -------------------
//      These are the office setup values
		vart.put("Ls2",new Double(390)); //
		vart.put("Ll",new Double(650)); //
		vart.put("Ld",new Double(80)); //
		vart.put("F",new Double(150));
		vart.put("Lbs",new Double(540));//
		vart.put("Lc",new Double(0));//	(No beam compressor attached)
		vart.put("M",new Double(1)); //(No beam compressor attached)
		vart.put("Lr",new Double(60));//
//		-------------------
//		These are the lab setup values 
//		vart.put("Ls2",new Double(696));
//		vart.put("Ll",new Double(4439.79));
//		vart.put("Ld",new Double(155));
//		vart.put("F",new Double(150));
//		vart.put("Lbs",new Double(4324.79));
//		vart.put("Lc",new Double(75));
//		vart.put("M",new Double(3));
//		vart.put("Lr",new Double(150));
//		----------------
//		Use these to compare with Zemax model
//		vart.put("Ls2",new Double(250));
//		vart.put("Ll",new Double(3993.29));
//		vart.put("Ld",new Double(150));
//		vart.put("F",new Double(150));
//		vart.put("Lbs",new Double(3893.29));
//		vart.put("Lc",new Double(50));
//		vart.put("M",new Double(2.5));
//		vart.put("Lr",new Double(50));
//		----------------
		vart.put("xpix",new Double(0.010));
		vart.put("ypix",new Double(0.010));
		vart.put("debug","false");
//		makeDir(dataDir);
	}
	
	public static void updateVars(){
		wdir=(String)vart.get("wdir");
		
		if(pathsep.equals("\\")){
			dataDir=String.format("%s\\%2$tm%2$td20%2$ty",wdir,Calendar.getInstance());
		}else if(pathsep.equals("/")){
			dataDir=String.format("%s/%2$tm%2$td20%2$ty",wdir,Calendar.getInstance());
		}
		
		String dbg = (String)vart.get("debug");
		debug = (dbg.equals("true")) ? true:false;
	}
	
	public static void makeDir(String path){
		File f;
		f=new File(path);
		if(!f.exists()){
			f.mkdir();
		}else if(!f.isDirectory()){
			f.delete();
			f.mkdir();
		}
	}
	
	/** */
	public static boolean readConfigFile(String path){
		boolean ret=true;
		BufferedReader in=null;
		String s;
		
		try{
			in=new BufferedReader(new FileReader(path));
			
			while((s=in.readLine())!=null){
				//s=s.replaceAll("[\\s]","\u0000");
				if(s.length()==0){
					//line is empty ignore
				}else if(s.charAt(0)=='%'){
					//line is a comment ignore
				}else{
					String[] tkns=s.split("[=;]");
					String var=tkns[0].trim();
					String val=tkns[1].trim();
					
					if(val.matches("\".*\"")){
						//System.out.println("string: "+var+"="+val+";");
						vart.put(var,val.substring(1,val.length()-1));
					}else{
						try{
							//System.out.format("double: %s=%f;%n",var,Double.parseDouble(val));
							vart.put(var,Double.parseDouble(val));
						}catch (NumberFormatException e) {
							System.err.format("[SetupData.readConfigFile(%s)]: Error parsing value: %s: %s%n",path,e.getMessage());
							ret=false;
							break;
						}
					}
				}
			}
			
			in.close();
		}catch(IOException e){
			System.err.format("[SetupData.readConfigFile(%s)]: Error reading file: %s%n",path,e.getMessage());
			ret=false;
		}catch(Exception e){
			System.err.format("[SetupData.readConfigFile(%s)]: Error parsing file: %s%n",path,e.getMessage());
			ret=false;
		}
		
		return ret;
	}
	
	/** */
	public static boolean writeConfigFile(String path){
		boolean ret=true;
		PrintWriter out=null;
		
		try{
			out=new PrintWriter(new BufferedWriter(new FileWriter(path)));
			out.println("% Default setup file for TiltShearTool ");
			out.println("% Use the percent sign at the start of the line for comments.");
			out.println("% Variables are assumed to be in the following format:");
			out.println("% \tfor numerical values: name=value;");
			out.println("% \tfor string values: name=\"value\";");
			out.println("% Do not change the name of the variable!");
			out.println("% String variables must be enclosed in quotes!");
			out.println();
			out.println("% NOTE: Do not enclose the variable in quotes!");
			out.format("wdir=\"%s\";%n",(String)vart.get("wdir"));
			out.println();
			out.println("%run program in debug mode");
			out.format("debug=\"%s\";%n",(String)vart.get("debug"));
			out.println();
			out.println("% tilt camera identifier");
			out.format("tiltCameraDevice=\"%s\";%n",(String)vart.get("tiltCameraDevice"));
			out.println();
			out.println("% shear camera identifier");
			out.format("shearCameraDevice=\"%s\";%n",(String)vart.get("shearCameraDevice"));
			out.println();
			out.println("%As of 08/2012 there are two Bobcat cameras.  For some reason these");
			out.println("%come in two models Bobcat-2478 and Bobcat-2486. Each model has its");
			out.println("%own specific calibration file.  You must be careful to specify the ");
			out.println("%correct file as it contains the pixel correction data for the ccd.");
			out.println("%shear camera calibration file");
			out.format("shearCalibFile=\"%s\";%n",(String)vart.get("shearCalibFile"));
			out.println("%tilt camera calibration file");
			out.format("tiltCalibFile=\"%s\";%n",(String)vart.get("tiltCalibFile"));
			out.println("%xenics color profile file");
			out.format("xenicsColorProfile=\"%s\";%n",(String)vart.get("xenicsColorProfile"));
			out.println("%Agilis controller device name");
			out.format("agilisControllerDevice=\"%s\";%n",(String)vart.get("agilisControllerDevice"));
			out.println();
			out.println("%Agilis controller channel number of SY mirror 1");
			out.format("chM1=%d;%n",((Double)vart.get("chM1")).intValue());
			out.println();
			out.println("%Agilis controller channel number of SY mirror 2");
			out.format("chM2=%d;%n",((Double)vart.get("chM2")).intValue());
			out.println();
			out.println("%%---------------------------------------");	
			out.println("%%Theta correction calculation constants.");
			out.println("%%---------------------------------------");
			out.println();
			out.println("%%Distance between SY mirrors 1 and 2 ");
			out.format("Ls2=%f;%n",(Double)vart.get("Ls2"));  
			out.println();
			out.println("%%Distance from SY mirror 2 to the lens (included distance traveled to going from BC mirrors 1 and 2) ");
			out.format("Ll=%f;%n",(Double)vart.get("Ll"));
			out.println();
			out.println("%%Distance from the lens to the tilt detector");
			out.format("Ld = %f;%n",(Double)vart.get("Ld"));
			out.println();
			out.println("%%Focal length of the lens in the tilt detector");
			out.format("F = %f;%n",(Double)vart.get("F"));
			out.println();
			out.println("%%Distance from SY mirror 2 to the beam splitter in the shear detector"); 
			out.format("Lbs = %f;%n",(Double)vart.get("Lbs"));
			out.println();
			out.println("%%Distance from the beam splitter to the beam compressor ");
			out.format("Lc = %f;%n",(Double)vart.get("Lc"));
			out.println();
			out.println("%%Magnification of the beam compressor");
			out.format("M = %f;%n",(Double)vart.get("M"));
			out.println();
			out.println("%%Distance from the beam compressor to the shear detector ");
			out.format("Lr = %f;%n",(Double)vart.get("Lr"));
			out.println();
			out.println("%%pixel length in x-direction in mm ");
			out.format("xpix = %f;%n",(Double)vart.get("xpix"));
			out.println();
			out.println("%%pixel length in y-direction in mm ");
			out.format("ypix = %f;%n",(Double)vart.get("ypix"));
			out.println();
//			out.println("%Mount step size in degrees for up arrow for rotated x-axis (mount axis)."); 
//			out.println("%Up arrow corresponds to negative angles about the rotated x-axis.");
//			out.format("stepUpX = %f;%n",(Double)vart.get("stepUpX"));
//			out.println();
//			out.println("%Mount step size in degrees for down arrow for rotated x-axis (mount axis).");
//			out.println("%Down arrow corresponds to positive angles about the rotated x-axis.");
//			out.format("stepDownX = %f;%n", (Double)vart.get("stepDownX"));
//			out.println();
//			out.println("%Mount step size in degrees for up arrow for rotated y-axis (mount axis).");
//			out.println("%Up arrow corresponds to positive angles about the rotated y-axis.");
//			out.format("stepUpY = %f;%n", (Double)vart.get("stepUpY"));
//			out.println();
//			out.println("%Mount step size in degrees for down arrow for rotated y-axis (mount axis).");
//			out.println("%Down arrow corresponds to negative angles about the rotated y-axis.");
//			out.format("stepDownY = %f;%n", (Double)vart.get("stepDownY"));
			out.println();
		}catch(IOException e){
			System.err.format("[SetupData.writeConfigFile(%s)]: Error writing file: %s%n", path, e.getMessage());
			ret=false;
		}
		
		if(out!=null){
			out.close();
		}
		
		return ret;
	}
	
	public static void main(String[] args){
		
		if(SetupData.pathsep.equals("/")){
			SetupData.writeConfigFile("./setup.txt");
			SetupData.readConfigFile("./setup.txt");
			SetupData.writeConfigFile("./setup2.txt");
		}else if(SetupData.pathsep.equals("\\")){
			SetupData.writeConfigFile(".\\setup.txt");
			SetupData.readConfigFile(".\\setup.txt");
			SetupData.writeConfigFile(".\\setup2.txt");
		}
		
		System.exit(0);
	}
}
