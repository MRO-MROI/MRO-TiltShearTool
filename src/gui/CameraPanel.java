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
import java.util.Hashtable;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import xenimaq.GammaVisitor;
import xenimaq.NativeImageException;
import xenimaq.NativeImageImpl;
import data.SetupData;

/**
 * This class creates a panel to change the configuration files for the tilt and
 * shear xenics cameras. It does this by changing the configuration file and the
 * internal program settings. To change to the new settings after changing
 * presets, one simply must capture the image again.
 **/
final public class CameraPanel extends JPanel implements ActionListener,
		PropertyChangeListener, ChangeListener {

	static final int GAMMA_MIN = -10, GAMMA_MAX = 10, GAMMA_INIT = 0,
			GAMMA_UNIT = 100;

	NativeImageImpl tiltImg;
	NativeImageImpl shearImg;
	JFileChooser fc;
	JButton openShearButton;
	JButton openTiltButton;
	JLabel ShearLabel;
	JLabel TiltLabel;
	File dirT;
	File dirS;
	JSlider tiltBrightness;
	JSlider tiltContrast;
	JSlider shearBrightness;
	JSlider shearContrast;
	GammaVisitor tiltGamma;
	GammaVisitor shearGamma;

	public CameraPanel(NativeImageImpl tiltImg, NativeImageImpl shearImg) {
		dirT = new File(
				"/home/mroi/workspace/Xenics-IMAQ/Calibrations/Bobcat2478_1000us_highgain_2478.xca");
		dirS = new File(
				"/home/mroi/workspace/Xenics-IMAQ/Calibrations/Bobcat2486_1000us_highgain_2486.xca");
		this.tiltImg = tiltImg;
		this.shearImg = shearImg;
		fc = new JFileChooser();
		TiltLabel = new JLabel(getSetupData("tilt", dirT));
		ShearLabel = new JLabel(getSetupData("shear", dirS));
		openShearButton = new JButton("Change Shear Calibration File");
		openShearButton.addActionListener(this);
		openTiltButton = new JButton("Change Tilt Calibration File");
		openTiltButton.addActionListener(this);

		/* Brightness, Contrast for Tilt and Shear */
		tiltBrightness = buildGammaSlider("Tilt Brightness");
		tiltContrast = buildGammaSlider("Tilt Contrast");
		shearBrightness = buildGammaSlider("Shear Brightness");
		shearContrast = buildGammaSlider("ShearContrast");

		tiltGamma = new GammaVisitor();
		shearGamma = new GammaVisitor();

		initGammaSlider(tiltImg, tiltGamma, tiltBrightness, tiltContrast);
		initGammaSlider(shearImg, shearGamma, shearBrightness, shearContrast);

		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		layout.setHorizontalGroup(layout
				.createSequentialGroup()
				.addGroup(
						layout.createParallelGroup(
								GroupLayout.Alignment.LEADING)
								.addComponent(openTiltButton)
								.addComponent(openShearButton)
								.addComponent(tiltBrightness)
								.addComponent(tiltContrast)
								.addComponent(shearBrightness)
								.addComponent(shearContrast))
				.addGroup(
						layout.createParallelGroup(
								GroupLayout.Alignment.LEADING)
								.addComponent(TiltLabel)
								.addComponent(ShearLabel)));
		layout.setVerticalGroup(layout
				.createSequentialGroup()
				.addGroup(
						layout.createParallelGroup(
								GroupLayout.Alignment.BASELINE)
								.addComponent(openTiltButton)
								.addComponent(TiltLabel))

				.addGroup(
						layout.createParallelGroup(
								GroupLayout.Alignment.BASELINE)
								.addComponent(openShearButton)
								.addComponent(ShearLabel))

				.addComponent(tiltBrightness).addComponent(tiltContrast)
				.addComponent(shearBrightness).addComponent(shearContrast));
	}

	public String getSetupData(String option, File file) {
		BufferedReader br = null;

		String line = "";
		String info = "";
		SetupData.vart.put(option + "CalibFile", file.getPath());
		try {
			FileReader fr = new FileReader(
					"/home/mroi/workspace/TiltShearTool/setup.txt");
			br = new BufferedReader(fr);
			while ((line = br.readLine()) != null) {
				if (line.contains(option + "CalibFile") && !line.contains("%")) {
					info = line;
				}
			}
		} catch (Exception e) {
			return info;
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				return info;
			}
		}
		return info;
	}

	public void changeSetupData(String option, File file) {
		BufferedReader br = null;
		BufferedWriter bw = null;

		String line = "";
		String totalfile = "";
		SetupData.vart.put(option + "CalibFile", file.getPath());
		try {
			FileReader fr = new FileReader(
					"/home/mroi/workspace/TiltShearTool/setup.txt");
			br = new BufferedReader(fr);
			FileWriter fw = new FileWriter(
					"/home/mroi/workspace/TiltShearTool/setup_tmp.txt");
			bw = new BufferedWriter(fw);
			while ((line = br.readLine()) != null) {
				if (line.contains(option + "CalibFile") && !line.contains("%")) {
					line = option + "CalibFile=\"" + file.getPath() + "\"";
				}
				totalfile = totalfile + line + "\n";
			}
			bw.write(totalfile);
		} catch (Exception e) {
			return;
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				//
			}
			try {
				if (bw != null)
					bw.close();
			} catch (IOException e) {
				//
			}
		}
		File oldFile = new File("/home/mroi/workspace/TiltShearTool/setup.txt");
		oldFile.delete();

		File newFile = new File(
				"/home/mroi/workspace/TiltShearTool/setup_tmp.txt");
		newFile.renameTo(oldFile);
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		// Handle open button action.
		if (e.getSource() == openShearButton) {
			fc.setCurrentDirectory(dirS);
			int returnVal = fc.showOpenDialog(CameraPanel.this);
			File file = fc.getSelectedFile();
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				changeSetupData("shear", file);
			}
			ShearLabel.setText(getSetupData("shear", dirS));

		}
		if (e.getSource() == openTiltButton) {
			fc.setCurrentDirectory(dirT);
			int returnVal = fc.showOpenDialog(CameraPanel.this);
			File file = fc.getSelectedFile();
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				changeSetupData("tilt", file);
			}
			TiltLabel.setText(getSetupData("tilt", dirT));

		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		try {

			if (e.getSource() == tiltBrightness
					&& !tiltBrightness.getValueIsAdjusting()) {
				double brightness = tiltBrightness.getValue() / GAMMA_UNIT;
				tiltGamma.setParameter("Brightness", "" + brightness);
				System.out.println("Tilt Brightness: " + brightness);
				tiltImg.exportVisitor(tiltGamma);
			}
			if (e.getSource() == tiltContrast
					&& !tiltContrast.getValueIsAdjusting()) {
				double contrast = tiltContrast.getValue() / GAMMA_UNIT;
				tiltGamma.setParameter("Contrast", "" + contrast);
				System.out.println("Tilt Contrast: " + contrast);
				tiltImg.exportVisitor(tiltGamma);
			}
			if (e.getSource() == shearBrightness
					&& !shearBrightness.getValueIsAdjusting()) {
				double brightness = shearBrightness.getValue() / GAMMA_UNIT;
				shearGamma.setParameter("Brightness", "" + brightness);
				System.out.println("Shear Brightness: " + brightness);
				shearImg.exportVisitor(shearGamma);
			}
			if (e.getSource() == shearContrast
					&& !shearContrast.getValueIsAdjusting()) {
				double contrast = shearContrast.getValue() / GAMMA_UNIT;
				shearGamma.setParameter("Contrast", "" + contrast);
				System.out.println("Shear Contrast: " + contrast);
				shearImg.exportVisitor(shearGamma);
			}
		} catch (NativeImageException err) {
			System.out.println(err);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		// TODO Auto-generated method stub

	}

	/**
	 * Helper method to construct a JSlider for a camera Gamma property
	 * (Brightness or Contrast)
	 * 
	 * @param title
	 *            String label for the slider.
	 * @return Constructed slider.
	 */
	private JSlider buildGammaSlider(String title) {
		JSlider s = new JSlider(GAMMA_MIN * GAMMA_UNIT, GAMMA_MAX * GAMMA_UNIT,
				GAMMA_INIT * GAMMA_UNIT);
		s.setMajorTickSpacing(GAMMA_MAX * GAMMA_UNIT);
		s.setMinorTickSpacing(GAMMA_INIT * GAMMA_UNIT);
		s.setPaintTicks(true);
		s.setPaintLabels(true);
		s.addChangeListener(this);

		TitledBorder border = new TitledBorder(title);
		s.setBorder(border);

		Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		labels.put(GAMMA_MIN * GAMMA_UNIT, new JLabel("" + GAMMA_MIN));
		labels.put(GAMMA_MAX * GAMMA_UNIT, new JLabel("" + GAMMA_MAX));
		labels.put(GAMMA_INIT * GAMMA_UNIT, new JLabel("" + GAMMA_INIT));
		s.setLabelTable(labels);

		return s;
	}

	/**
	 * Helper method to initialize the tilt and shear gamma sliders by polling
	 * the current filter state for each camera.
	 */
	private void initGammaSlider(NativeImageImpl image, GammaVisitor visitor,
			JSlider brightness, JSlider contrast) {
		try {
			if (image != null) {
				image.acceptVisitor(visitor);
			}

			if (visitor.parameterCount() == 2) {
				brightness.setValue((int) (Double.parseDouble(visitor
						.getParameter("Brightness")) * GAMMA_UNIT));
				contrast.setValue((int) (Double.parseDouble(visitor
						.getParameter("Contrast")) * GAMMA_UNIT));
			} else {
				brightness.setEnabled(false);
				contrast.setEnabled(false);
			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}
}