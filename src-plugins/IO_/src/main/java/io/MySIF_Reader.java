package io;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import ij.*;
import ij.io.*;
import ij.plugin.PlugIn;
import ij.plugin.frame.Editor;

public class MySIF_Reader implements PlugIn {

	public void run(String arg) {
		OpenDialog od = new OpenDialog("Open SIF...", arg);
		String file = od.getFileName();
		if (file == null) return;
		String directory = od.getDirectory();
		ImagePlus imp = open(directory, file);
		if (imp != null ) {
			//imp.TypeConverter.convertToGray16();
			//IJ.convert("16-bit");  // how the heck is this supposed to work?
			imp.show();
		} else {
			IJ.showMessage("Open SIF...", "Failed.");
		}
	}

	public static ImagePlus open(String directory, String file) {
	    int i, offset = 0;
	    boolean showInfoMessage = true;

		File f = new File(directory, file);
		try {
			offset = 0;
			BufferedReader in = new BufferedReader(new FileReader(f));

			// line 1
			String line = in.readLine();
			offset += line.length() + 1;
			if (!line.equals("Andor Technology Multi-Channel File")) {
				IJ.error("File is not an Andor SIF");
			}

			// line 2
			line = in.readLine();
			if (!line.equals("65538 1")) {								// "65538 1"  Andor File version num, and SIGNAL image is present)
				IJ.error("unknown Andor file version number at offset " + offset);
			}
			offset += line.length() + 1;

			// line 3: TInstaImage thru "head model"
			line = in.readLine();
			offset += line.length() + 1;
			java.util.StringTokenizer st = new java.util.StringTokenizer(line);
			String flush = st.nextToken();								// 65547 for iXon, 65558 for Luca
			if (!flush.equals("65547") && !flush.equals("65558")) {
				IJ.error("unknown TInstaImage version number before offset " + offset);
			}
			flush = st.nextToken();										// type
			flush = st.nextToken();										// active
			flush = st.nextToken();										// structure version (= 1)
			flush = st.nextToken();										// timedate, in unknown format
			int temperature = Integer.parseInt(st.nextToken());			// temperature (= 0?)
			flush = st.nextToken();										// head
			flush = st.nextToken();										// store_type
			flush = st.nextToken();										// data_type
			flush = st.nextToken();										// mode
			flush = st.nextToken();										// trigger_source
			flush = st.nextToken();										// trigger_level
			float exposure = Float.parseFloat(st.nextToken());			// exposure_time
			float framedelay = Float.parseFloat(st.nextToken());		// delay
			flush = st.nextToken();										// integration_cycle_time
			int numinteg = Integer.parseInt(st.nextToken());			// no_integrations
			flush = st.nextToken();										// sync
			float kincycletime = Float.parseFloat(st.nextToken());		// kinetic_cycle_time
			flush = st.nextToken();										// pixel_readout_time
			flush = st.nextToken();										// no_points
			flush = st.nextToken();										// fast_track_height
			int gain = Integer.parseInt(st.nextToken());				// gain
			flush = st.nextToken();										// gate_delay
			flush = st.nextToken();										// gate_width
			flush = st.nextToken();										// gate_step
			flush = st.nextToken();										// track_height
			flush = st.nextToken();										// series_length

			flush = st.nextToken();										// read_pattern
			flush = st.nextToken();										// shutter_delay
			flush = st.nextToken();										// st_centre_row
			flush = st.nextToken();										// mt_offset
			flush = st.nextToken();										// operation_mode
			flush = st.nextToken();										// FlipX
			flush = st.nextToken();										// FlipY
			flush = st.nextToken();										// Clock
			flush = st.nextToken();										// AClock
			flush = st.nextToken();										// MCP
			flush = st.nextToken();										// Prop
			flush = st.nextToken();										// IOC
			flush = st.nextToken();										// Freq
			flush = st.nextToken();										// VertClockAmp
			flush = st.nextToken();										// data_v_shift_speed
			flush = st.nextToken();										// OutputAmp
			flush = st.nextToken();										// PreAmpGain
			flush = st.nextToken();										// Serial
			// the following do not seem to appear in the header, but some are here for the Luca??  Not sure what's different here
			// but probably not needed for our purposes
			//flush = st.nextToken();										// NumPulses
			//flush = st.nextToken();										// mFrameTransferAcqMode
			//flush = st.nextToken();										// unstabilizedTemperature
			//flush = st.nextToken();										// mBaselineClamp
			//flush = st.nextToken();										// mPreScan
			//flush = st.nextToken();										// mEMRealGain
			//flush = st.nextToken();										// mBaselineOffset
			//flush = st.nextToken();										// mSWVersion

			// Now at "head_model" in TInstaImage
			flush = st.nextToken();										// number of bytes in head model string, next readline should be ok)
			line = in.readLine();										// head model string
			offset += line.length() + 1;
			// Now at detector_format_x in TInstaImage
			line = in.readLine();										// not interesting...
			offset += line.length() + 1;
			// Now at filename, terminated with space, newline
			line = in.readLine();
			offset += line.length() + 1;
			//IJ.showMessage(line);

			// Now at TUserText
			line = in.readLine();										// 65538, space, number of characters
			offset += line.length() + 1;
			st = new java.util.StringTokenizer(line);
			flush = st.nextToken();
			if (!flush.equals("65538")) {
				IJ.error("unknown TUserText version number before offset " + offset);
			}
			int usertextlen = Integer.parseInt(st.nextToken()) + 1;		// don't forget the end-of-line!
			char[] utext = new char[usertextlen];
			in.read(utext, 0, usertextlen);								// the actual user text
			String usertext = new String(utext);
			offset += usertextlen;

			// Now at TShutter
			line = in.readLine();										// Note, if you actually want to parse this, the spacing is weird!
			offset += line.length() + 1;

			// TShamrockSave section does not seem to exist!!!  Skip it (but Luca has it?!)

			// Now at TCalibImage
			line = in.readLine();
			offset += line.length() + 1;
			st = new java.util.StringTokenizer(line);
			flush = st.nextToken();										// TCalibImage version number
			if (flush.equals("65538")) {								// Seems to only happen for the Luca, a few extra lines thrown in here
				line = in.readLine();									// Unknown / undocumented values in here
				offset += line.length() + 1;
				line = in.readLine();
				offset += line.length() + 1;
				line = in.readLine();
				offset += line.length() + 1;
				line = in.readLine();
				offset += line.length() + 1;
				line = in.readLine();
				offset += line.length() + 1;
			} else if (!flush.equals("65539")) {
				IJ.error("unknown TCalibImage version number before offset " + offset);
			}
			// dump the rest of the first line of TCalibImage, and move on...
			line = in.readLine();										// x_cal
			offset += line.length() + 1;
			line = in.readLine();										// y_cal
			offset += line.length() + 1;
			line = in.readLine();										// z_cal
			offset += line.length() + 1;
			line = in.readLine();										// rayleigh_wavelength
			offset += line.length() + 1;
			line = in.readLine();										// pixel length
			offset += line.length() + 1;
			line = in.readLine();										// pixel height
			offset += line.length() + 1;

			// Please, do not design file formats like this.  It makes no sense at all.  Here, length of the next string is
			// mashed up against the previous string.  Strings have delimeters elsewhere in this file!
			line = in.readLine();										// length of the next line...
			offset += line.length() + 1;
			int skipchars = Integer.parseInt(line);
			in.skip(skipchars);											// x_text
			offset += skipchars;
			line = in.readLine();										// the length of the next line!
			offset += line.length() + 1;
			skipchars = Integer.parseInt(line);
			in.skip(skipchars);											// y_text
			offset += skipchars;
			line = in.readLine();										// the length of the next line!
			offset += line.length() + 1;
			skipchars = Integer.parseInt(line);
			in.skip(skipchars);											// z_text
			offset += skipchars;

			// Now at TImage!  Phew!
			line = in.readLine();
			offset += line.length() + 1;

			st = new java.util.StringTokenizer(line);
			flush = st.nextToken();
			if (!flush.equals("65538")) {
				IJ.error("unknown TImage version number after offset " + offset);
			}
			// Caution-- the following four items seem to flip around, if you have inverted x or y in your acquisition!
			flush = st.nextToken();										// image_format.left
			flush = st.nextToken();										// image_format.top
			flush = st.nextToken();										// image_format.right
			flush = st.nextToken();										// image_format.bottom
			int Zdim = Integer.parseInt(st.nextToken());				// no_images
			flush = st.nextToken();										// no_subimages -- we assume just one for now!!
			int totallength = Integer.parseInt(st.nextToken());			// length of the entire dataset, in pixels over all frames?

			// Now at first (and, for now, only) subimage, TSubImage
			line = in.readLine();
			offset += line.length() + 1;
			st = new java.util.StringTokenizer(line);
			flush = st.nextToken();
			if (!flush.equals("65538")) {
				IJ.error("unknown TSubImage version number after offset " + offset);
			}
			int left = Integer.parseInt(st.nextToken());
			int top = Integer.parseInt(st.nextToken());
			int right = Integer.parseInt(st.nextToken());
			int bottom = Integer.parseInt(st.nextToken());
			int vertical_bin = Integer.parseInt(st.nextToken());
			int horizontal_bin = Integer.parseInt(st.nextToken());
			int subimage_offset = Integer.parseInt(st.nextToken());

			// calculate frame width, height
			int width = right - left + 1;
			int mod = width%horizontal_bin;
			width = (width - mod)/vertical_bin;
			int height = top - bottom + 1;
			mod = height%vertical_bin;
			height = (height - mod)/horizontal_bin;

			//The rest of the file is a time stamp for the frame followed by a new line. Skip
			for (int iii = 0; iii < Zdim; iii++) {
				line = in.readLine();
				offset += line.length() + 1;
			}
			// offset = offset + 2*Zdim;								// Can't do it this way with the Luca...

			if (showInfoMessage){
				//IJ.showMessage("Image height is "+height+".\nImage width is "+width+".\nStacksize is "+Zdim+".\nOffset is "+offset+".");

				// result window
				String logtext =
													  file +
					"\n\nTemperature (deg C):\t"	+ temperature +
					"\nExposure Time (sec):\t"		+ exposure +
					"\nKin Cycle Time (sec):\t"		+ kincycletime +
					"\nEM Gain level:\t\t"		    + gain +
					"\n\nLeft:\t\t\t"			    + left +
					"\nRight:\t\t\t"			    + right +
					"\nTop:\t\t\t"				    + top +
					"\nBottom:\t\t\t"			    + bottom +
					"\nHorizontal Binning:\t"		+ horizontal_bin +
					"\nVertical Binning:\t"		    + vertical_bin +
 					"\n" + usertext + "\n";

                IJ.log(logtext);
			}

			/*
			Now that the size and the offset of the image is known
			we can open the image/stack.
			*/

			FileInfo fi = new FileInfo();
			fi.directory = directory;
			fi.fileFormat = fi.RAW;
			fi.fileName = file;
			// GRAY32 is the file type for .SIF's
			fi.fileType = 4;
			fi.intelByteOrder = true;
			fi.gapBetweenImages = 0;
			fi.offset = offset;
			fi.width = width;
			fi.height = height;
			fi.nImages = Zdim;

			FileOpener fo = new FileOpener(fi);
			ImagePlus imp = fo.open(false);
			IJ.showStatus("");
			return imp;

		} catch (IOException e) {
			IJ.error("An error occured reading the file.\n \n" + e);
			IJ.showStatus("");
			return null;
		}
	}
}
