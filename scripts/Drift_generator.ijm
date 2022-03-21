//----------
// 2021-11-10 Romain F. Laine, romain.cauk@gmail.com
// Add synthetic 3D drift to a multicolor 3D dataset
// Edited by Joanna Pylvänäinen
//----------

//Macro to generate drifting data:
// you set the number of time points you want to generate
// the drifts over time currently follows a quadratic form but we can easily change that for what you'd like
// you can get the GT drifts from the table generated
// you can add some additional poisson noise as in a real time course (enable/disable) but that's very slow, so I recommend to play with drifts first and only add the noise once you're happy with drifts
// you'll need RandomJ installed for the Poisson noise to work
// you can disable the randomness on the drift curves by setting the drift_noise_amplitude to 0 (edited) 

//Current limitations:
// it's a bit slow, but it seems to work
// it loses the color of the channels
// it does not automatically change to 32-bit (which would be better before applying the translations + interpolations) but it currently keeps the bit depth so that it's easier for memory useage
// randomJ converts to 32-bit though, so it can require a big chunk of memory at the end
// NOT IMPLEMENTED: load drift curves rather than generating them

#@ File (label="File to be simulated", style="open") file_path ;

#@ Integer (label="Number of time points to simulate", style="spinner") time_points ;

#@ String  (value="-----------------------------------------------------------------------------", visibility="MESSAGE") hint1;
#@ Double (label="x2", value=0.01, persist=false, style="format:#.##", style="spinner") x2 ;
#@ Double (label="x1", value=0.01, persist=false, style="format:#.##", style="spinner") x1 ;

#@ String  (value="-----------------------------------------------------------------------------", visibility="MESSAGE") hint1;
#@ Double (label="y2", value=0.01, persist=false, style="format:#.##", style="spinner") y2 ;
#@ Double (label="y1", value=0.01, persist=false, style="format:#.##", style="spinner") y1 ;


#@ String  (value="-----------------------------------------------------------------------------", visibility="MESSAGE") hint1;
#@ Double (label="z2", value=0.01, persist=false, style="format:#.##", style="spinner") z2 ;
#@ Double (label="z1", value=-0.01, persist=false, style="format:#.##", style="spinner") z1 ;

#@ String  (value="-----------------------------------------------------------------------------", visibility="MESSAGE") hint1;

#@ Integer (label="Drift noise amplitude", style="spinner") drift_noise_amplitude ;

#@ Integer (label="Remove drifing frame (average bg signal)", style="spinner") remove_frame ;

#@ boolean (label = "Add poisson noise") additional_poisson_noise ; 


// ---- User input ----
//time_points = 25; // number of time points to simulate

// Simulating the drift curves over time as quadratic functions: ¢
// x_drift(t) = x2*t^2 + x1*t + noise, for instance (but we could use any functions really)

//x2 = 0.1; 
//x1 = 0.2; 

//y2 = -0.3; 
//y1 = 1.5; 

//z2 = 0.0;
//z1 = -0.6;

//drift_noise_amplitude = 0.7;
//drift_noise_amplitude = 0.1; //original
//additional_poisson_noise = true;

//file_path = "/Users/jpylvana/Dropbox/Tutkimus-phd/3_PhD/NanoJ-Core/PaperDatasets/1-synteticData/210407aspc1-noDrift-nobg-2channel.tif";



// ---- ----

run("Close All");
print("\\Clear");

// Build the drift arrays

print("Creating drift arrays...");
t = newArray(time_points);
x_drift = newArray(time_points);
y_drift = newArray(time_points);
z_drift = newArray(time_points);


for (i = 0; i < time_points; i++) {
	t[i] = i;
	x_drift[i] = x2*t[i]*t[i] + x1*t[i] + drift_noise_amplitude*(random("gaussian")-0.5);
	y_drift[i] = y2*t[i]*t[i] + y1*t[i] + drift_noise_amplitude*(random("gaussian")-0.5);
	z_drift[i] = z2*t[i]*t[i] + z1*t[i] + drift_noise_amplitude*(random("gaussian")-0.5);
}

// Plot the drift curves
Plot.create("Drift curves", "Time (# frames)", "Drift (pixels)");
Plot.add("line", t, x_drift, "X-drift");
Plot.setColor("blue");
Plot.add("line", t, y_drift, "Y-drift");
Plot.setColor("red");
Plot.add("line", t, z_drift, "Z-drift");
Plot.setColor("green");
Plot.setLegend("X-drift","top-left");
Plot.show();
Plot.setLimitsToFit();

// Let's get the image

open(file_path);

img_title = getTitle();
getDimensions(width, height, n_channels, n_slices, n_frames);

setBatchMode(true);
selectWindow(img_title);
run("Duplicate...", "title=3DDStack duplicate");

print("Adding drift to the stack...");
for (i = 1; i < time_points; i++) {
	showProgress(i, time_points);
	
	selectWindow(img_title);
	run("Duplicate...", "title=3DD_singleFrame duplicate");
	// Apply XY drift
	run("Translate...", "x="+x_drift[i]+" y="+y_drift[i]+" interpolation=Bicubic");
	run("Split Channels");

	merge_string = "";
	for (c = 0; c < n_channels; c++) {
		selectWindow("C"+(c+1)+"-3DD_singleFrame");
		// Reslice to swap Z and Y
		run("Reslice [/]...", "output=0.400 start=Top avoid");
		close("C"+(c+1)+"-3DD_singleFrame");
		selectWindow("Reslice of "+"C"+(c+1)+"-3DD_singleFrame");
		rename("C"+(c+1)+"-3DD_singleFrame");
		run("Translate...", "x=0 y="+z_drift[i]+" interpolation=Bicubic stack");
		run("Reslice [/]...", "output=0.101 start=Top avoid");
		close("C"+(c+1)+"-3DD_singleFrame");
		selectWindow("Reslice of "+"C"+(c+1)+"-3DD_singleFrame");
		rename("C"+(c+1)+"-3DD_singleFrame");
		merge_string = merge_string+"c"+(c+1)+"=C"+(c+1)+"-3DD_singleFrame ";
	}

	merge_string = merge_string+" create";
//	print(merge_string);
	run("Merge Channels...", merge_string);

	run("Concatenate...", "  title=3DD_FullStack keep image1=3DDStack image2=3DD_singleFrame image3=[-- None --]");
	close("3DDStack");
	close("3DD_singleFrame");
	selectWindow("3DD_FullStack");
	rename("3DDStack");
}


selectWindow("3DDStack");
run("Hyperstack to Stack");

setBatchMode(false);
print("Resetting the hyperstack...");
run("Stack to Hyperstack...", "order=xyczt(default) channels="+n_channels+" slices="+n_slices+" frames="+time_points+" display=Composite");

// homogenize bg

if (remove_frame > 0){
	run("Subtract...", "value=remove_frame");
	run("Add...", "value=remove_frame");
	run("Add Specified Noise...", "standard=100");
}
// Add Poisson noise

if (additional_poisson_noise){
	print("Adding Poisson noise...");
	run("RandomJ Poisson", "mean=1.0 insertion=Modulatory");
}

print("---- All done ----");


