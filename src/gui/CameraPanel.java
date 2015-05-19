package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import xenimaq.NativeImageImpl;
import data.SetupData;

/*This class creates a panel to change the configuration files for the tilt and shear xenics cameras.
 * It does this by changing the configuration file and the internal program settings.  To change to 
 * the new settings after changing presets, one simply must capture the image again.  
 * */
final public class CameraPanel extends JPanel implements ActionListener, PropertyChangeListener, ChangeListener {
	
	NativeImageImpl tiltImg;
	NativeImageImpl shearImg;
	JFileChooser fc;
	JButton openShearButton;
	JButton openTiltButton;
	JLabel ShearLabel;
	JLabel TiltLabel;
	File dirT;
	File dirS;
	JSlider brightnessSlider;
	JSlider contrastSlider;
	
	// TODO: figure out reasonable values for these
	static final int BRIGHTNESS_MIN = 0, BRIGHTNESS_MAX = 100, BRIGHTNESS_INIT = 50,
			CONTRAST_MIN = 0, CONTRAST_MAX = 100, CONTRAST_INIT = 50;
	
	public CameraPanel(NativeImageImpl tiltImg, NativeImageImpl shearImg){
		dirT = new File("/home/mroi/workspace/Xenics-IMAQ/Calibrations/Bobcat2478_1000us_highgain_2478.xca");
		dirS = new File("/home/mroi/workspace/Xenics-IMAQ/Calibrations/Bobcat2486_1000us_highgain_2486.xca");
		this.tiltImg = tiltImg;
		this.shearImg = shearImg;
		fc = new JFileChooser();
	    TiltLabel = new JLabel(getSetupData("tilt",dirT));
		ShearLabel = new JLabel(getSetupData("shear",dirS));
		openShearButton = new JButton("Change Shear Calibration File");
		openShearButton.addActionListener(this);
		openTiltButton = new JButton("Change Tilt Calibration File");
		openTiltButton.addActionListener(this);
		
		brightnessSlider = new JSlider(BRIGHTNESS_MIN, BRIGHTNESS_MAX, BRIGHTNESS_INIT);
		brightnessSlider.setMajorTickSpacing(BRIGHTNESS_MAX);
		brightnessSlider.setMinorTickSpacing(BRIGHTNESS_INIT);
		brightnessSlider.setPaintTicks(true);
		brightnessSlider.setPaintLabels(true);
		brightnessSlider.addChangeListener(this);
		TitledBorder brightnessBorder = new TitledBorder("Brightness");
		brightnessSlider.setBorder(brightnessBorder);
		
		contrastSlider = new JSlider(CONTRAST_MIN, CONTRAST_MAX, CONTRAST_INIT);
		contrastSlider.setMajorTickSpacing(CONTRAST_MAX);
		contrastSlider.setMinorTickSpacing(CONTRAST_INIT);
		contrastSlider.setPaintTicks(true);
		contrastSlider.setPaintLabels(true);
		contrastSlider.addChangeListener(this);
		TitledBorder contrastBorder = new TitledBorder("Contrast");
		contrastSlider.setBorder(contrastBorder);
		
		GroupLayout layout=new GroupLayout(this);
		setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		
		layout.setHorizontalGroup(
			layout.createSequentialGroup()
				.addGroup(
					layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(openTiltButton).addComponent(openShearButton)
						.addComponent(brightnessSlider).addComponent(contrastSlider))
				.addGroup(
					layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					    .addComponent(TiltLabel).addComponent(ShearLabel))
			);	
		layout.setVerticalGroup(
			layout.createSequentialGroup()
				.addGroup(
					layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					    .addComponent(openTiltButton).addComponent(TiltLabel))
						
				.addGroup(
					layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(openShearButton).addComponent(ShearLabel))
						
				.addComponent(brightnessSlider)
				.addComponent(contrastSlider)
			);
	}
	public String getSetupData(String option, File file){
		 BufferedReader br = null;
		 
		 	String line = "";
            String info = "";
            SetupData.vart.put(option + "CalibFile", file.getPath());
            try {
                FileReader fr = new FileReader("/home/mroi/workspace/TiltShearTool/setup.txt");
                br = new BufferedReader(fr);
                while ((line = br.readLine()) != null) {
                    if (line.contains(option+ "CalibFile")&&!line.contains("%")){
                        info = line;
                    }
                }    
            } 
            catch (Exception e) {
                return info;
            } finally {
                try {
                   if(br != null)
                      br.close();
               } catch (IOException e) {
                   return info;
               }
            }
            return info;
	}
	public void changeSetupData(String option, File file){
		 BufferedReader br = null;
	     BufferedWriter bw = null;
        
             String line = "";
             String totalfile = "";
             SetupData.vart.put(option + "CalibFile", file.getPath());
             try {
                 FileReader fr = new FileReader("/home/mroi/workspace/TiltShearTool/setup.txt");
                 br = new BufferedReader(fr);
                 FileWriter fw = new FileWriter("/home/mroi/workspace/TiltShearTool/setup_tmp.txt");
                 bw = new BufferedWriter(fw);
                 while ((line = br.readLine()) != null) {
                     if (line.contains(option+ "CalibFile")&&!line.contains("%")){
                         line = option + "CalibFile=\""+file.getPath()+"\"";}
                     totalfile = totalfile + line + "\n";
                 }    
                 bw.write(totalfile);
             } 
             catch (Exception e) {
                 return;
             } finally {
                 try {
                    if(br != null)
                       br.close();
                } catch (IOException e) {
                    //
                }
                try {
                    if(bw != null)
                       bw.close();
                } catch (IOException e) {
                    //
                }
             }
             File oldFile = new File("/home/mroi/workspace/TiltShearTool/setup.txt");
             oldFile.delete();

             File newFile = new File("/home/mroi/workspace/TiltShearTool/setup_tmp.txt");
             newFile.renameTo(oldFile);
	}
	@Override
	public void actionPerformed(ActionEvent e) {

        //Handle open button action.
        if (e.getSource() == openShearButton) {
        	 fc.setCurrentDirectory(dirS);
        	 int returnVal = fc.showOpenDialog(CameraPanel.this);
    		 File file = fc.getSelectedFile();
    		 if (returnVal == JFileChooser.APPROVE_OPTION) {
    			 changeSetupData("shear",file);
    		 }
    	     ShearLabel.setText(getSetupData("shear",dirS));
             
        }
        if (e.getSource() == openTiltButton) {
        	fc.setCurrentDirectory(dirT);
       	 	int returnVal = fc.showOpenDialog(CameraPanel.this);
       	 	File file = fc.getSelectedFile();
       	 	if (returnVal == JFileChooser.APPROVE_OPTION) {
       	 		changeSetupData("tilt",file);
       	 	}	
       	    TiltLabel.setText(getSetupData("tilt",dirT));
            
       }
	}
	
	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == brightnessSlider
				&& !brightnessSlider.getValueIsAdjusting()) {
			//TODO:
			System.out.println("brightnessSlider: " + brightnessSlider.getValue());
		}
		if (e.getSource() == contrastSlider
				&& !contrastSlider.getValueIsAdjusting()) {
			//TODO:
			System.out.println("contrastSlider: " + contrastSlider.getValue());
		}
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}