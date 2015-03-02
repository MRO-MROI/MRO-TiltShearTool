package gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.io.*;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.LayoutStyle;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.*;

import data.AlignmentMath;
import data.SetupData;
import xenimaq.NativeImageException;
import xenimaq.NativeImageImpl;
import java.util.Hashtable;

/*This class creates a panel to change the configuration files for the tilt and shear xenics cameras.
 * It does this by changing the configuration file and the internal program settings.  To change to 
 * the new settings after changing presets, one simply must capture the image again.  
 * */
final public class CameraPanel extends JPanel implements ActionListener, PropertyChangeListener {
	
	NativeImageImpl tiltImg;
	NativeImageImpl shearImg;
	JFileChooser fc;
	JButton openShearButton;
	JButton openTiltButton;
	JLabel ShearLabel;
	JLabel TiltLabel;
	File dirT;
	File dirS;
	
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
		GroupLayout layout=new GroupLayout(this);
		setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		
		layout.setHorizontalGroup(
			layout.createSequentialGroup()
				.addGroup(
					layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(openTiltButton).addComponent(openShearButton))
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
	public void propertyChange(PropertyChangeEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}