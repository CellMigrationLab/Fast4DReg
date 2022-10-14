/*
Fast4DReg is a Fiji macro for drift correction of 3D videos or 
channel alignment in 3D multichannel image stacks. Drift or 
misalignment can be corrected in all x-, y- and/or z-directions. 

This time_apply script corrects drift between 
frames of a 2D or 3D video(s) based to 
settings defined in the time_estimate+apply script. 

Fast4DReg is dependent on the NanoJ-Core plugin and Bioformats.
If you use this script in your research, please cite our pre-print and 
Laine, R. F. Et al 2019. NanoJ: a high-performance open-source super-resolution 
microscopy toolbox, doi: 10.1088/1361-6463/ab0261.

Authors: Joanna W Pylvänäinen, Guillaume Jacquemet, Romain F Laine, Bruno Saraiva
Contact: joanna.pylvanainen@abo.fi
Cersion: 2.0 (the dimensionalist)
Documentation: https://github.com/guijacquemet/Fast4DReg
Licence: MIT

*/


run("Close All");
print("\\Clear");
run("Collect Garbage");

// select file to be corrected
// select file(s) to be corrected
#@ File[] (label="Select the file(s) to be corrected") files ;

#@ File (label="Select settings file (.csv)", style="open") settings_file_path ;

#@ File (label="Select where to save corrected images", style="directory") results_path ;


// ----- Let's go ! -----

IJ.log("== Fast4DReg - apply drift correction - 3D videos =====================");

print("Number of files to correct: " + lengthOf(files)); 
IJ.log("Destination folder: " + results_path);

for (p = 0; p < lengthOf(files); p++) {
																																																	
	setBatchMode(true); 
	
	showProgress(p+1, lengthOf(files));
	
	// read settins from csv
	Table.open(settings_file_path);
	
	// get variables from settings file
	
	File_Name = Table.getString("Value", 0);
	XY_registration = Table.get("Value", 1);
	crop_output = Table.getString("Value",6);
	z_registration = Table.getString("Value",7);
	reslice_mode = Table.getString("Value",9);
	extend_stack_to_fit = Table.getString("Value",13);
	ram_conservative_mode = Table.getString("Value",14);
	DriftTable_path_XY = Table.getString("Value",15);
	DriftTable_path_Z = Table.getString("Value",16);
	
	run("Close");
	
	t_start = getTime();
	
	options = "open=[" + files[p] + "] autoscale color_mode=Default stack_order=XYCZT use_virtual_stack "; // here using bioformats
	run("Bio-Formats", options);
	
	getDimensions(width, height, channels, slices, frames);
	thisTitle = getTitle();
	filename_no_extension = File.getNameWithoutExtension(files[p]);
	
	// 2D module
	
	if (slices == 1) {
		IJ.log("--------------------------------");
		IJ.log("Now correcting image: " + thisTitle); 
		IJ.log("2D image detected - no need for 3D correction");
		IJ.log("Applying the xy-correction to the stack....");
		
		run("F4DR Correct Drift", "choose=["+DriftTable_path_XY+"]");
		
		if (crop_output){
			minmaxXYdrift = getMinMaxXYFromDriftTable(DriftTable_path_XY);
		
			new_width = width - Math.ceil(minmaxXYdrift[1]) + Math.ceil(minmaxXYdrift[0]);
			new_height = height - Math.ceil(minmaxXYdrift[3]) + Math.ceil(minmaxXYdrift[2]);
			makeRectangle(Math.ceil(minmaxXYdrift[1]), Math.ceil(minmaxXYdrift[3]), new_width, new_height);
			run("Crop");
		}
	
		// Save as tif
		Corrected_path_xy = results_path + File.separator + filename_no_extension+"_xyCorrected";   	 
		saveAs("Tiff", Corrected_path_xy);
		close("*");
	
	}
		
	// 3D module
			
	if (slices > 1) 	{
	
	// =============== XY ====================
		if (XY_registration){
		
		IJ.log("--------------------------------");
		IJ.log("Now correcting image: " + filename_no_extension);
		IJ.log("Applying the xy-correction to the stack....");

		setBatchMode(true); 
		
		for (i = 0; i < slices; i++) {
			showProgress(i, slices);
		
			selectWindow(thisTitle);
			run("Duplicate...", "title=DUP duplicate slices="+(i+1));
			run("32-bit");
			run("F4DR Correct Drift", "choose=["+DriftTable_path_XY+"]");
			selectWindow("DUP - drift corrected");
			rename("SLICE");
			
			if (i==0){
				rename("AllStarStack");}
			else {
				// This is potentially what makes it so slow as it needs to dump and recreate the stack every time
				run("Concatenate...", "  image1=AllStarStack image2=SLICE image3=[-- None --]");
				rename("AllStarStack");}
		
			close("DUP");	
		}
		
		selectWindow("AllStarStack");
		run("Stack to Hyperstack...", "order=xyctz channels=1 slices="+slices+" frames="+frames+" display=Color");
		
		run("Enhance Contrast", "saturated=0.35");
		rename(filename_no_extension+"_xyCorrected");
		Corrected_path_xy = results_path + File.separator + filename_no_extension+"_xyCorrected"; 
		
		if (crop_output){
			minmaxXYdrift = getMinMaxXYFromDriftTable(DriftTable_path_XY);
		
			new_width = width - Math.ceil(minmaxXYdrift[1]) + Math.ceil(minmaxXYdrift[0]);
			new_height = height - Math.ceil(minmaxXYdrift[3]) + Math.ceil(minmaxXYdrift[2]);
			makeRectangle(Math.ceil(minmaxXYdrift[1]), Math.ceil(minmaxXYdrift[3]), new_width, new_height);
			run("Crop");
			
		}
		
		setBatchMode("show"); // don't remove
		//setBatchMode(false);
			
		// Save intermediate file xy-correct //JP 	 
		saveAs("Tiff", Corrected_path_xy);
		close("*");
		
		}
		
		
		// =============== Z ====================
		
		if (z_registration){
		
		t_start = getTime();
		filename_no_extension = File.getNameWithoutExtension(files[p]);
		
		// ----- opening the correct file-----	
			if (!XY_registration){
				options = "open=[" + files[p] + "] autoscale color_mode=Default stack_order=XYCZT use_virtual_stack "; // here using bioformats
				run("Bio-Formats", options);
			} else {
				Corrected_image_xy = Corrected_path_xy+".tif";
				options = "open=[" + Corrected_image_xy + "]";
				run("TIFF Virtual Stack...", options);
		
			}
		
		setBatchMode(true); 
		
		thisTitle = getTitle();
		
		getVoxelSize(width, height, depth, unit);
		run("Reslice [/]...", "output="+depth+" start="+reslice_mode+" avoid");
		rename("DataRescliced");
		
		//------- Applying the correction -------- 
		
		if (!XY_registration) {
			IJ.log("--------------------------------");
			IJ.log("Now correcting image: " + filename_no_extension);

		}
		
		IJ.log("Applying the z-correction to the stack....");
		
		if (extend_stack_to_fit){
			minmaxZdrift = getMinMaxFromDriftTable(DriftTable_path_Z);
			padding = 2*maxOf(-minmaxZdrift[0], minmaxZdrift[1]);
		}
		else {
			padding = 0;
		}
		
		selectWindow("DataRescliced");
		getDimensions(width, height, channels, slices, frames);
		getVoxelSize(width_realspace, height_realspace, depth_realspace, unit_realspace);
		padded_height = height + padding;
		
		if (!ram_conservative_mode){
			newImage("DataRescliced_Corrected", "32-bit black", width, padded_height, slices*frames);
			setVoxelSize(width_realspace, height_realspace, depth_realspace, unit_realspace);
		}
		
		for (i = 0; i < slices; i++) {
			showProgress(i, slices);
			
			selectWindow("DataRescliced");
		
			if (ram_conservative_mode){
				setSlice(1);
				run("Duplicate...", "title=DUP duplicate slices=1");
			}
			else{
				run("Duplicate...", "title=DUP duplicate slices="+(i+1));
			}
		
			run("Canvas Size...", "width="+width+" height="+(padded_height)+" position=Center zero");
			run("F4DR Correct Drift", "choose=["+DriftTable_path_Z+"]");
			rename("SLICE");
			run("Hyperstack to Stack");
		
			if (ram_conservative_mode){
				if (i==0){
					rename("AllStarStack");
					} else {
					// This is potentially what makes it so slow as it needs to dump and recreate the stack every time
					run("Concatenate...", "  image1=AllStarStack image2=SLICE image3=[-- None --]");
					rename("AllStarStack");}
			} else {
			for (f = 0; f < frames; f++) {
				selectWindow("SLICE");
				setSlice(f+1);
				run("Select All");
				run("Copy");
				selectWindow("DataRescliced_Corrected");
				setSlice(i*frames + f+1);
				run("Paste");		
				}
			}
				
			close("DUP");
		
			if (ram_conservative_mode){
				selectWindow("DataRescliced");
				run("Delete Slice", "delete=slice");
			}
			else {
				close("SLICE");
			}
		}
		
		if (!ram_conservative_mode){
			close("DataRescliced");
			selectWindow("DataRescliced_Corrected");
			run("Select None");
			run("Enhance Contrast", "saturated=0.35");
		}
		else {
			selectWindow("AllStarStack");
		}
		
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
			Corrected_path_z = results_path + File.separator + filename_no_extension+"_zCorrected"; 
			saveAs("Tiff", Corrected_path_z);
			} else {
			rename(filename_no_extension+"_xyzCorrected");
			Corrected_path_xyz = results_path + File.separator + filename_no_extension+"_xyzCorrected";
			saveAs("Tiff", Corrected_path_xyz);
			}   
		
		close("\\Others");
		run("Enhance Contrast", "saturated=0.35");
		
		}
	}
}

close("*");
IJ.log("============");
IJ.log("All done.");
IJ.log("Time taken: "+round((getTime()-t_start)/1000)+"s");
showMessage("All DONE!");

setBatchMode(false); 

// ----- Helper functions -----
function getMinMaxFromDriftTable(DriftTable_path_Z) {
	run("F4DR Open NanoJ Table (NJT)...", "load=["+DriftTable_path_Z+"]");
	Table.rename(File.getName(DriftTable_path_Z), "Results");

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





// ----- Helper functions -----
function getMinMaxXYFromDriftTable(DriftTable_path_XY) {
	run("F4DR Open NanoJ Table (NJT)...", "load=["+DriftTable_path_XY+"]");
	Table.rename(File.getName(DriftTable_path_XY), "Results");

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
