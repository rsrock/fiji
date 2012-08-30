package io;

import java.io.*;
import java.util.Date;

import ij.*;
import ij.gui.GenericDialog;
import ij.io.*;
import ij.plugin.PlugIn;
import ij.plugin.frame.Editor;
import ij.process.ImageProcessor;

/**
 * Open_SIFkai_.java
 * Updated in 02/01/2011
 * @author Yoshiyuki Arai
 **/

public class Open_SIFkai_ implements PlugIn {

	public void run(String arg) {
		// *Open SIF File
		OpenDialog od = new OpenDialog("Open SIF...",arg);
		String file = od.getFileName();
		if (file == null) return;
		String directory = od.getDirectory();
		ImagePlus imp = open(directory,file);
		if (imp != null) {
			imp.show();
		} else {
			IJ.error("Open SIF failed.");
		}
	}

	private static ImagePlus open(String directory, String file) {
		File f = new File(directory,file);
		int i, offset=0, mod;
		int left = 1, right = 512, bottom = 1, top = 512;
		int height, width, stacksize = 1;
		int Xbin = 1, Ybin = 1;
		int readout = 0;
		double temperature1 =0, temperature2 =0;
		Date d = null;
		String cycleTime="", temperature = "", exposureTime = "", EMGain = "",
		verticalShiftSpeed = "", version = "", model = "", originalFilename = "", preAmpGain = "";
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
			int spool = 0;	//If Data is Spooled one, spool value is set to 1
			int ver = 0; //
			for(i=0;i<27+spool+ver;i++) {
				String str = in.readLine();
				if(i==2) { // Extract parameters such as temperature, exposureTime and so on.
					String[] tokens = str.split(" ");
					//at token[4] is Date value
					d = new Date(Long.parseLong(tokens[4])*1000); // Date is recored as seconds counted from 1970.1.1 00:00:00
					//at token[5] is "temperature value"
					temperature1 = Double.parseDouble(tokens[5]);
					//at token[12] is "exposure time"
					exposureTime = tokens[12];
					//at token[14] is "cycle time"
					cycleTime = tokens[14];
					//at token[18] is "readout rate"
					readout =  (int) (1/Double.parseDouble(tokens[18])/1e+6);
					//at token[21] is "EM Gain"
					EMGain = tokens[21];
					//at token[41] is "vertical shift speed"
					verticalShiftSpeed = tokens[41];
					//at token[43] is "pre-amplifier gain"
					preAmpGain = tokens[43];
					//at token[47] is temperature when temperature is unstable
					temperature2 = Double.parseDouble(tokens[47]);
					//at token[54-57] is version of solis
					version = tokens[54]+"."+tokens[55]+"."+tokens[56]+"."+tokens[57];
					//header size is dependent on the sif file format version.
					if(version.equals("4.9.30004.0")) ver = 0;
					if(version.equals("4.13.30000.0") || version.equals("4.15.30000.0")) ver = 4;
					if(temperature1 == -999)
						// If the temperature is unstable, temperature1 value is -999 and unstable temperature value is recored in temperature2
						temperature = String.valueOf(temperature2) + " (Unstable)";
					else
						temperature = String.valueOf(temperature1);
				}
				//at line 3 is model of EMCCD camera
				if(i==3) {
					model = str; // Model of EMCCD camera
				}
				//at line 5 is filename where sif file is saved
				if(i==5) {
					originalFilename = str; // Read original filename
				}
				//
				if(i==7) { // If the Data is spooled one, "spool" value is set to 1
					String[] tokens = str.split(" ");
					if(ver == 0 && tokens[0].equals("Spooled")) {
						spool=1;
					}
				}
				if(i==24+spool+ver) { // Read size of stack (frame length)
					String[] tokens = str.split(" ");
					stacksize = Integer.parseInt(tokens[6]);

				}
				if(i==25+spool+ver) { // Read information about the size and bin
					String[] tokens = str.split(" ");
					left = Integer.parseInt(tokens[1]);
					top = Integer.parseInt(tokens[2]);
					right = Integer.parseInt(tokens[3]);
					bottom = Integer.parseInt(tokens[4]);
					Xbin = Integer.parseInt(tokens[5]);
					Ybin = Integer.parseInt(tokens[6]);
				}
			}
			// Estimate the offset value
			FileInputStream ino = new FileInputStream(f);
			// Version dependency
			if(version.equals("4.15.30000.0")) ver = 5;
			for (i = 0; i < 26+stacksize+ver+spool; i++){
				while(ino.read() != 10) { //"10" means "\a"
					offset++;
				}
				offset++;
			}
			ino.close();

			// Calc the width and the height value
			width = right-left+1;
			mod = width % Xbin;
			width = (width-mod)/Xbin;
			height = top-bottom+1;
			mod = height % Ybin;
			height = (height-mod)/Ybin;
			/*
			Now that the size and the offset of the image is known
			we can open the image/stack.
			*/

			//ShowDialog to get the information about the startFrame, endFrame and whether convert it to 16 bit or not
			GenericDialog gd = new GenericDialog("Open SIF");
			gd.addNumericField("start frame", 1, 0);
			gd.addNumericField("End frame", stacksize, 0);
			gd.addCheckbox("Convert to 16 bit images?", false);
			gd.showDialog();
			if(gd.wasCanceled()) return null;
			int startFrame = (int)gd.getNextNumber();
			int endFrame = (int)gd.getNextNumber();
			boolean convertTo16bit = gd.getNextBoolean();

			//range check
			if(startFrame>endFrame || startFrame>stacksize) {
				startFrame=endFrame;
				IJ.error("startFrame is set to "+startFrame);
			}
			startFrame--;
			startFrame=startFrame<0?0:startFrame;
			endFrame = endFrame>stacksize?stacksize:endFrame;

			ImagePlus imp=new ImagePlus();
			ImageStack stack2 = new ImageStack(width,height);

			FileInfo fi = new FileInfo();
			fi.directory = directory;
			fi.fileFormat = FileInfo.RAW;
			fi.fileName = file;
			fi.fileType = FileInfo.GRAY32_FLOAT; // Data type of SIF file is 32bit_Float
			fi.intelByteOrder = true;
			fi.gapBetweenImages = 0;
			fi.height = height;
			fi.width = width;
			if(!convertTo16bit) {
			fi.nImages = (endFrame-startFrame);
			fi.offset = offset+startFrame*height*width*4;
			FileOpener fo = new FileOpener(fi);
			imp = fo.open(false);
			} else {
				// if "convetTo16bit" was checked, read 1 frame and convert it to 16 bit-data
				// loop from startFrame to endFrame
				fi.nImages = 1;
				ImageProcessor ip1,ip2;
				for(int cnt = 0;cnt<(endFrame-startFrame);cnt++) {
					fi.offset = offset+(startFrame+cnt)*height*width*4;
					FileOpener fo = new FileOpener(fi);
					ImagePlus tmpImp = fo.open(false); // read 1 frame
					ip1=tmpImp.getProcessor();
					ip2=ip1.convertToShort(false);
					stack2.addSlice(String.valueOf((cnt+1)), ip2);
					IJ.showProgress((double)cnt/(endFrame-startFrame));
					IJ.showStatus("Converting to 16-bits: "+(cnt+1)+"/"+(endFrame-startFrame));
				}
				IJ.showProgress(1.0D);

				imp.setStack(null, stack2);
				imp.setTitle(file);
			}

			// *result window
			file = file.substring(0, file.length() - 3) + "txt";
			String text =
				"Original Filename:\t" + originalFilename +
				"\nDate and Time:\t" + d.toString() +
				"\nSoftware Version:\t" + version +
				"\nModel:\t\t" + model +
				"\nTemperature (C):\t"		+ temperature +
				"\nExposure Time (s):\t"	+ exposureTime +
				"\nCycle Time (s):\t" + cycleTime +
				"\nStack size :\t" + stacksize +
				"\nPixel Readout Rate (MHz):\t" + readout +
				"\nWidth:\t"+width+
				"\nHeigth:\t"+height+
				"\nHorizontal Binning:\t"	+ Xbin +
				"\nVertical Binning:\t"		+ Ybin +
				"\nEM Gain level:\t"		+ EMGain +
				"\nVertical Shift Speed (s):\t" + verticalShiftSpeed +
				"\nPre-Amplifier Gain:\t" + preAmpGain;
			if(spool == 1) text = text+"\nSpooled data";
			Editor ed = new Editor();
			ed.setSize(500, 280);
			ed.create(file, text);

			// flip the picture vertically
			if (imp!=null) {
				ImageStack stack = imp.getStack();
				for (i=1; i<=stack.getSize(); i++) {
					ImageProcessor ip = convertTo16bit?stack.getProcessor(i).convertToShort(false):stack.getProcessor(i);
					ip.flipVertical();
				}
				imp.show();
			}
			IJ.showStatus("");
			in.close();
			return imp;

		} catch (IOException e) {
			IJ.error("An error occured reading the file.\n" + e);
			IJ.log(e.toString());
		}
		return null;
	}

}
