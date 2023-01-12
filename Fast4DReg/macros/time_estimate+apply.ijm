/*
Fast4DReg is a Fiji macro for drift correction of 2D or 3D videos or 
channel alignment in 2D or 3D multichannel images. Drift or 
misalignment can be corrected in all x-, y- and/or z-directions. 

This time_estimate+apply script estimates the drift between frames in 
a 2D or 3D video(s) and applies the correction to the same dataset(s).
A new folder will be created in the same directory with the source image
will contain corrected images, a settings file and drift information. 

Fast4DReg is dependent on the Bio-Formats.

If you use this script in your research, please cite:

1. our pre-print:
Pylvänäinen J. P., et al. (2022). Fast4DReg: Fast registration of 
4D microscopy datasets. doi: 10.1101/2022.08.22.504744

2. and the original NanoJ tool publication:
Laine, R. F. et al 2019. NanoJ: a high-performance open-source super-resolution 
microscopy toolbox, doi: 10.1088/1361-6463/ab0261.

Authors: Joanna W Pylvänäinen, Guillaume Jacquemet, Romain F Laine, Bruno Saraiva
Contact: joanna.pylvanainen@abo.fi
Version: 2.1 (the clean 'n fast dimensionalist)
Documentation: https://github.com/guijacquemet/Fast4DReg
Licence: MIT

*/
														
run("Close All");
print("\\Clear");
run("Collect Garbage");


// give experiment number
#@ Integer (label="Experiment number", value=001, style="format:000") exp_nro ;

// select file to be corrected
#@ File[] (label="Select the file(s) to be corrected") files ;

//settings for xy-drift correction
#@ String  (value="-----------------------------------------------------------------------------", visibility="MESSAGE") hint1;
#@ boolean (label = "<html><b>xy-drift correction</b></html>") XY_registration ; 
#@ String(label = "Projection type", choices={"Max Intensity","Average Intensity"}, style="listBox") projection_type_xy ;
#@ Integer (label="Time averaging (default: 100, 1 - disables)", min=1, max=100, style="spinner") time_xy ;
#@ Integer (label="Maximum expected drift (pixels, 0 - auto)", min=0, max=auto, style="spinner") max_xy ;
#@ String (label = "Reference frame", choices={"first frame (default, better for fixed)" , "previous frame (better for live)"}, style="listBox") reference_xy ;
#@ boolean (label = "Crop output") crop_output ; 

//settings for z-drift correction
#@ String  (value="-----------------------------------------------------------------------------", visibility="MESSAGE") hint3;
#@ boolean (label = "<html><b>z-drift correction</b></html>") z_registration ; 
#@ String(label = "Projection type", choices={"Max Intensity","Average Intensity"}, style="listBox") projection_type_z ;
#@ String(label = "Reslice mode", choices={"Top","Left"}, style="listBox") reslice_mode ;
#@ Integer (label="Time averaging (default 100, 1 - disables)", min=1, max=100, style="spinner") time_z ;
#@ Integer (label="Maximum expected drift (pixels, 0 - auto)", min=0, max=auto, style="spinner") max_z ;
#@ String (label = "Reference frame", choices={"first frame (default, better for fixed)" , "previous frame (better for live)"}, style="listBox") reference_z ;
#@ boolean (label = "Extend stack to fit") extend_stack_to_fit ; 
#@ boolean (label = "Save RAM") ram_conservative_mode ; 
#@ String  (value="-----------------------------------------------------------------------------", visibility="MESSAGE") hint4;

// get time stamp
MonthNames = newArray("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec");  
getDateAndTime(year, month, week, day, hour, min, sec, msec);
IJ.log("== Fast4DReg - estimate and apply drift correction - 3D videos =====================");

year = "" + year; //converts year to string
timeStamp = year+"-"+MonthNames[month]+"-"+day+"-"+IJ.pad(exp_nro, 3);

print(timeStamp);

t_start = getTime();
print("Number of files to correct:" + lengthOf(files)); 
	
//======================================================================
// ----- Let's go ! -----


for (p = 0; p < lengthOf(files); p++) {
	
	setBatchMode(true); 
																																																																																																	
	showProgress(p+1, lengthOf(files));
	
	p_start = getTime();
	
	options = "open=[" + files[p] + "] autoscale color_mode=Default stack_order=XYCZT use_virtual_stack "; // here using bioformats
	run("Bio-Formats", options);
	
	// study the image
	getDimensions(width, height, channels, slices, frames);
	
	//set file paths
	filename_no_extension = File.getNameWithoutExtension(files[p]);
	results = File.getDirectory(files[p])+filename_no_extension+"_"+timeStamp+File.separator;
	File.makeDirectory(results);
	
	settings_file_path = results+filename_no_extension+"_settings.csv"; 
	DriftTable_path_XY = results+filename_no_extension+"-"+projection_type_xy+"_xy_";
	DriftTable_path_Z = results+filename_no_extension+"-"+projection_type_z+"-"+reslice_mode+"_z_";
	
	// create a settings table and set columns
	setResult("Setting", 0, "File Name");
	setResult("Value", 0, filename_no_extension);
	
	setResult("Setting", 1, "xy-registration");
	setResult("Value", 1, XY_registration);
	
	setResult("Setting", 2, "xy-projection type");
	setResult("Value", 2, projection_type_xy);
	
	setResult("Setting", 3, "xy-time averaging");
	setResult("Value", 3, time_xy);
	
	setResult("Setting", 4, "xy-maximum expected drift");
	setResult("Value", 4, max_xy);
	
	setResult("Setting", 5, "xy-reference frame");
	setResult("Value", 5, reference_xy);
	
	setResult("Setting", 6, "Crop output");
	setResult("Value", 6, crop_output);
	
	setResult("Setting", 7, "z-registration");
	setResult("Value", 7, z_registration);
	
	setResult("Setting", 8, "z-projection type");
	setResult("Value", 8, projection_type_z);
	
	setResult("Setting", 9, "z-reslice mode");
	setResult("Value", 9, reslice_mode);
	
	setResult("Setting", 10, "z-time averaging");
	setResult("Value", 10, time_z);
	
	setResult("Setting", 11, "z-maximum expected drift");
	setResult("Value", 11, max_z);
	
	setResult("Setting", 12, "z-reference frame");
	setResult("Value", 12, reference_z);
	
	setResult("Setting", 13, "Extend stack to fit");
	setResult("Value", 13, extend_stack_to_fit);
	
	setResult("Setting", 14, "Save RAM");
	setResult("Value", 14, ram_conservative_mode);
	
	setResult("Setting", 15, "xy-drift table path");
	setResult("Value", 15, DriftTable_path_XY +"DriftTable.njt");
	
	setResult("Setting", 16, "z-drift table path");
	setResult("Value", 16, DriftTable_path_Z +"DriftTable.njt");
	
	setResult("Setting", 17, "results path");
	setResult("Value", 17, results);
	
	saveAs("Results", settings_file_path);
	
	close("Results");
	

	
	setBatchMode(true); 
	thisTitle = getTitle();
	
	//the 2D module
	
	IJ.log("===========================");
	IJ.log("===========================");
	
	if (slices == 1) {
		
		IJ.log("Now processing image: " + thisTitle); 
		
		if (XY_registration){

			IJ.log("2D image detected - z-correction disabled");
			IJ.log("Estimating and applying the xy-correction to the 2D image....");
		
			run("F4DR Estimate Drift", "time="+time_xy+" max="+max_xy+" reference=["+reference_xy+"] show_drift_plot apply choose=["+DriftTable_path_XY+"]");	
			rename(filename_no_extension+"_xyCorrected");
		
			//save drift plots
			selectWindow("Drift-X");
			saveAs("Tiff", results + filename_no_extension + "_Drift-plot-X");
		
			selectWindow("Drift-Y");
			saveAs("Tiff", results + filename_no_extension + "_Drift-plot-Y");
			
			// crops image when doing xy-correction
			if (crop_output) {	
				minmaxXYdrift = getMinMaxXYFromDriftTable_xy(DriftTable_path_XY+"DriftTable.njt");
		
				selectWindow(filename_no_extension+"_xyCorrected");
				width = getWidth();
				height = getHeight();
			 
				new_width = width - Math.ceil(minmaxXYdrift[1]) + Math.ceil(minmaxXYdrift[0]);
				new_height = height - Math.ceil(minmaxXYdrift[3]) + Math.ceil(minmaxXYdrift[2]);
			
				makeRectangle(Math.ceil(minmaxXYdrift[1]), Math.ceil(minmaxXYdrift[3]), new_width, new_height);
				run("Crop");
			}
		
			// Save xy-corrected image
			selectWindow(filename_no_extension+"_xyCorrected");
			Corrected_path_xy = results+ File.separator + filename_no_extension + "_xyCorrected";	 
			saveAs("Tiff", Corrected_path_xy);
			close("*");
		}
		
		if (!XY_registration){
			IJ.log("2D image detected - z-correction disabled, no images saved");
		}
	}	
	
	//the 3D module
	if (slices > 1) {
		IJ.log("Now processing image: " + thisTitle); 
		IJ.log("3D image detected");
		
	//======================================================================
	// ----- Estimating the xy-correction from the resliced projection -----
	
		if (XY_registration){
			IJ.log("Estimating the xy-drift....");
			// make projection
			getDimensions(width, height, channels, slices, frames);
			run("Z Project...", "projection=["+projection_type_xy+"] all");
			rename(projection_type_xy+" projection_"+filename_no_extension);
			
			//estimate x-y drift
			run("F4DR Estimate Drift", "time="+time_xy+" max="+max_xy+" reference=["+reference_xy+"] show_drift_plot apply choose=["+DriftTable_path_XY+"]");
			rename("DriftCorrOutput_XY");
		
			//save drift plots
			selectWindow("Drift-X");
			saveAs("Tiff", results+"_Drift-plot-X");
		
			selectWindow("Drift-Y");
			saveAs("Tiff", results+"_Drift-plot-Y");
	
		// ----- Applying the xy-correction from the resliced projection -----
			
			IJ.log("Applying the xy-correction to the stack....");
			
			for (i = 0; i < slices; i++) {
				showProgress(i, slices);
				
				selectWindow(thisTitle);
				run("Duplicate...", "title=DUP duplicate slices="+(i+1));
				
				if(!ram_conservative_mode){
					run("32-bit");
				}
				
				run("F4DR Correct Drift", "choose=["+DriftTable_path_XY+"DriftTable.njt]");
				selectWindow("DUP - drift corrected");
				rename("SLICE");
			
				if (i==0){
					rename("AllStarStack");
				} else {
					// This is potentially what makes it so slow as it needs to dump and recreate the stack every time
					run("Concatenate...", "  image1=AllStarStack image2=SLICE image3=[-- None --]");
					rename("AllStarStack");
				}
		
			close("DUP");

			}

			selectWindow("AllStarStack");
	
			run("Stack to Hyperstack...", "order=xyctz channels=1 slices="+slices+" frames="+frames+" display=Color");
		
			run("Enhance Contrast", "saturated=0.35");
			rename(filename_no_extension+"_xyCorrected");
			Corrected_path_xy = results+ File.separator +filename_no_extension+"_xyCorrected";
	
			// crops image when doing xy-correction
			if (crop_output) {	
				minmaxXYdrift = getMinMaxXYFromDriftTable_xy(DriftTable_path_XY+"DriftTable.njt");
	
				selectWindow(filename_no_extension+"_xyCorrected");
				width = getWidth();
				height = getHeight();
				
				new_width = width - Math.ceil(minmaxXYdrift[1]) + Math.ceil(minmaxXYdrift[0]);
				new_height = height - Math.ceil(minmaxXYdrift[3]) + Math.ceil(minmaxXYdrift[2]);
				
				makeRectangle(Math.ceil(minmaxXYdrift[1]), Math.ceil(minmaxXYdrift[3]), new_width, new_height);
				run("Crop");
			}
	
			// Save intermediate file xy-correct	 
			saveAs("Tiff", Corrected_path_xy);
			close("*");
	
		}
	//======================================================================
	
		if (z_registration) {
			IJ.log("------------------------------");
			IJ.log("Estimating the z-drift....");
			
			// ----- opening the correct file-----	
			if (!XY_registration){
				options = "open=[" + files[p] + "] autoscale color_mode=Default stack_order=XYCZT use_virtual_stack "; // here using bioformats
				run("Bio-Formats", options);
			} else {
				Corrected_image_xy = Corrected_path_xy+".tif";
				options = "open=[" + Corrected_image_xy + "]";
				run("TIFF Virtual Stack...", options);
			}
			
			// ----- Reslicing for z-projection estimation-----	
			getVoxelSize(width, height, depth, unit);
			run("Reslice [/]...", "output="+depth+" start="+reslice_mode+" avoid");
			rename("DataRescliced");
			getDimensions(width, height, channels, slices, frames);
			scale_factor = round(width/height);
			
			setBatchMode("show"); // don't remove
			
			//======================================================================
			// ----- Estimating the z correction  from the resliced projection -----
			run("Z Project...", "projection=["+projection_type_z+"] all");
			rename(projection_type_z+" "+reslice_mode+" projection_"+filename_no_extension);
			
			run("Scale...", "x=1.0 y="+scale_factor+" z=1.0 width="+width+" height="+(scale_factor*width)+" depth="+frames+" interpolation=Bicubic average process create");
		
			run("F4DR Estimate Drift", "time="+time_z+" max="+max_z+" reference=["+reference_z+"] show_drift_plot apply choose=["+DriftTable_path_Z+"]");
			
			setBatchMode(true); 
			
			rename("DriftCorrOutput");
			
			//selectWindow("Drift-X");
			selectWindow("Drift-Y");
			rename("Drift-Z");
			Plot.setXYLabels("time-points", "z-drift (px)");
			saveAs("Tiff", results+"_Drift-plot-Z");
			
			selectWindow("DriftCorrOutput");
			run("Scale...", "x=1.0 y="+(1/scale_factor)+" z=1.0 width="+width+" height="+height+" depth="+frames+" interpolation=Bicubic average process create");
			rename("DriftCorrected_"+projection_type_z+" "+reslice_mode+" projection_"+filename_no_extension);
			
			//edits drift tabel so that only z drift is saved
			run("F4DR Open NanoJ Table (NJT)...", "load=["+DriftTable_path_Z+"DriftTable.njt]");
			TableName = filename_no_extension+"-"+projection_type_z+"-"+reslice_mode+"_z_DriftTable.njt";
			Table.rename(TableName, "Results");
			
			for (i = 0; i < nResults; i++) {
				setResult("X-Drift (pixels)", i, 0);
				setResult("XY-Drift (pixels)", i, 0);
			}
			
			updateResults();
			run("F4DR Save Results-Table as NJT...", "save=["+DriftTable_path_Z+"DriftTable.njt]");
			
			resetDriftTable(DriftTable_path_Z+"DriftTable.njt", scale_factor);
			
			run("Collect Garbage");
			
			
		//------- Applying the z correction -------- 
			
		
			IJ.log("Applying the z-correction to the stack....");
			
			if (extend_stack_to_fit){
				minmaxZdrift = getMinMaxFromDriftTable_z(DriftTable_path_Z+"DriftTable.njt");
				padding = 2*maxOf(-minmaxZdrift[0], minmaxZdrift[1]);
			} else {
				padding = 0;
			}
		
			selectWindow("DataRescliced");
			getDimensions(width, height, channels, slices, frames);
			getVoxelSize(width_realspace, height_realspace, depth_realspace, unit_realspace);
			padded_height = height + padding;
			bit1 = bitDepth();
		
			if (!ram_conservative_mode){
				newImage("DataRescliced_Corrected", "32-bit black", width, padded_height, slices*frames);
				setVoxelSize(width_realspace, height_realspace, depth_realspace, unit_realspace);
			} else {
				newImage("DataRescliced_Corrected", bit1 + "black", width, padded_height, slices*frames);
				setVoxelSize(width_realspace, height_realspace, depth_realspace, unit_realspace);
			}

			for (i = 0; i < slices; i++) {
				showProgress(i, slices);
				selectWindow("DataRescliced");
			
				run("Duplicate...", "title=DUP duplicate slices="+(i+1));
				run("Canvas Size...", "width="+width+" height="+(padded_height)+" position=Center zero");
				
				run("F4DR Correct Drift", "choose=["+DriftTable_path_Z+"DriftTable.njt]");
				rename("SLICE");
				run("Hyperstack to Stack");
			
				for (f = 0; f < frames; f++) {
					selectWindow("SLICE");
					setSlice(f+1);
					run("Select All");
					run("Copy");
					selectWindow("DataRescliced_Corrected");
					setSlice(i*frames + f+1);
					run("Paste");		
				}
				
				close("DUP");
				close("SLICE");

			}
		
			close("DataRescliced");
			selectWindow("DataRescliced_Corrected");
			run("Select None");
			run("Enhance Contrast", "saturated=0.35");

			run("Stack to Hyperstack...", "order=xyctz channels=1 slices="+slices+" frames="+frames+" display=Color");
			getVoxelSize(width, height, depth, unit);
			run("Reslice [/]...", "output="+depth+" start=Top avoid");
		
			if (reslice_mode == "Left"){
				run("Flip Vertically", "stack");
				run("Rotate 90 Degrees Right");
			}
					
		//save files here
			if (!XY_registration) {
				rename(filename_no_extension+"_zCorrected"); 
				Corrected_path_z = results+ File.separator +filename_no_extension+"_zCorrected"; 
				saveAs("Tiff", Corrected_path_z);
			} else {
				rename(filename_no_extension+"_xyzCorrected");
				Corrected_path_xyz = results+ File.separator +filename_no_extension+"_xyzCorrected";
				saveAs("Tiff", Corrected_path_xyz);  
			}   
			
			run("Enhance Contrast", "saturated=0.35");
		}
	}

	run("Close All");
	run("Collect Garbage");

	IJ.log("--- Correction completed --- Time taken: "+round((getTime()-p_start)/1000)+"s ---");

	setBatchMode(false);

//====== THE END =======================================================

//======================================================================
	// ----- Helper functions -----
	function getMinMaxFromDriftTable_z(path_to_table) {
		run("F4DR Open NanoJ Table (NJT)...", "load=["+path_to_table+"]");
		Table.rename(File.getName(path_to_table), "Results");

		minmaxZdrift = newArray(2);
		minmaxZdrift[0] = 0;
		minmaxZdrift[1] = 0;

		for (i = 0; i < nResults; i++) {
			zDrift = getResult("Y-Drift (pixels)", i);
			if (zDrift < minmaxZdrift[0]) minmaxZdrift[0] = zDrift;
			if (zDrift > minmaxZdrift[1]) minmaxZdrift[1] = zDrift;
		}

		minmaxZdrift[0] = floor(minmaxZdrift[0]); 
		minmaxZdrift[1] = Math.ceil(minmaxZdrift[1]);
		close("Results");

		return minmaxZdrift;
	}

//--------------------------------------------- 
	function resetDriftTable(path_to_table, scale_factor) {
		run("F4DR Open NanoJ Table (NJT)...", "load=["+path_to_table+"]");
		Table.rename(File.getName(path_to_table), "Results");

		for (i = 0; i < nResults; i++) {
			zDrift = getResult("Y-Drift (pixels)", i);
			setResult("Y-Drift (pixels)", i, zDrift/scale_factor);
		}
		updateResults();

		run("F4DR Save Results-Table as NJT...", "save=["+path_to_table+"]");
		close("Results");

		return;
	}

//--------------------------------------------- 
	function getMinMaxXYFromDriftTable_xy(path_to_table) {
		run("F4DR Open NanoJ Table (NJT)...", "load=["+path_to_table+"]");
		Table.rename(File.getName(path_to_table), "Results");

		minmaxXYdrift = newArray(4);
		minmaxXYdrift[0] = 0;
		minmaxXYdrift[1] = 0;
		minmaxXYdrift[2] = 0;
		minmaxXYdrift[3] = 0;

		for (i = 0; i < nResults; i++) {
			xDrift = getResult("X-Drift (pixels)", i);
			yDrift = getResult("Y-Drift (pixels)", i);
			if (xDrift < minmaxXYdrift[0]) minmaxXYdrift[0] = xDrift;
			if (xDrift > minmaxXYdrift[1]) minmaxXYdrift[1] = xDrift;

			if (yDrift < minmaxXYdrift[2]) minmaxXYdrift[2] = yDrift;
			if (yDrift > minmaxXYdrift[3]) minmaxXYdrift[3] = yDrift;
		}

		close("Results");
		return minmaxXYdrift;
	}
}

IJ.log("==============================");
IJ.log("==============================");
IJ.log("All DONE! Total time: " +round((getTime()-t_start)/1000)+"s");


