# Fast4DReg


Fast4DReg is a script that can be used for quick drift correction in time-lapse stacks. The script can be used to correct drift in x-, y- and/or z-directions. The correction is based on cross-correlation between projections of time points - making the drift estimation faster than many other correction methods. Fast4DReg uses the NanoJ-Core Fiji plugin.

Drift correction workflow
1. From 3D time series z-projections are created at each time point to create 2D time series. 
2. NanoJ-Core estimates the linear x-y drift between two images by calculating their cross-correlation matrix (CCM). The location of the peak intensity in the CCM determines the linear shift between the two images. 
3. Once the drift is estimated, the dataset can be directly corrected frame by frame according to the amount of measured drift. 
4. Once the images have been corrected the projected images are built back to 3D time series.
5. Each time point of the x-y -corrected 3D time series is projected along the y- or z-axis to create another 2D time series. 
6. NanoJ-Core estimates the linear z drift between two images by calculating their cross-correlation matrix (CCM). The location of the peak intensity in the CCM determines the linear shift between the two images. 
7. Once the z-drift is estimated, the dataset can be directly corrected frame by frame according to the amount of measured drift and built back to the 3D time series.

In images with two channels, the channels need to be split and the drift is first estimated one of the channels. The drift correction can then be applied to the second (or more) channels.

![image](images/methodDescription.png)
*Figure 1: Fast4DReg workflow. 3D time stack images can ce corrected for drift in xy-, z- and/or xyz-directions.*

# Dependencies

Fast4DReg requires the NanoJ-Core plugin and Bioformats, which can both be installed through Fiji update site: open ImageJ and select “Update” in the “Help”-menu.


# Step-by-step walkthrough

**Estimate and apply drift**
1. Create a folder with your image to be corrected in it. If you have multiple channels they can all be in the same folder as separated files.
2. Open the "estimate-drift" script and click run. User interface opens.

![image](images/Fast4DregUI.png)
*Figure 2: Estimte and apply user interface*

3. In the user interface
     - Set the path to the file to be corrected
     - if you want to correct for xy-drift, tick the xy-drift correction box
     - Select projection type used for xy-drift estimation (maximum or average intensity) 
     - This sets the number of frames to average together to make coarser timepoints on which the
cross-correlation analysis will be run to calculate drift. Setting this value to 1 will calculate
straight frame-to-frame cross-correlations and while this should capture drift very accurately, it
will also be very susceptible to noise. Conversely, setting this value high will average out noise
but will also give a lower sample of the drift (which is then interpolated).
     - This refers to the maximum expected drift between the first frame of the dataset and the last
frame of the dataset in units of pixels. Setting this to 0 will allow the algorithm to automatically
determine the drift without any limitations. It is only really worth changing this value from 0 if
running the algorithm gives incorrect results with large jumps in estimated drift.
    - If this is set to ‘first frame (default, better for fixed)’ then every averaged group of frames will be
compared to the first average group of frames to calculate drift. If this is set to ‘previous frame
(better for live)’ then every averaged group of frames will be compared to the previous averaged
group of frames. For static samples, it is best to
compare to the first frame, and for live samples where there may be slow scale drift overlaying
the faster scale sample motion, it is better to compare to the previous frame.
    - Crop output will crop out the black frame created by the image moving. This will be performed on default if continued to z-correction.
    - if you want to correct for z-drift, tick the z-drift correction box.
    - Reslice mode lets you decide if you want to create the projection along the x-axis (top) or y-axis (left).
    - Select projection type used for z-drift estimation (maximum or average intensity) 
    - next three - see above
    - Extend stack to fit will create extra slices to the stack to ensure that the whole stack is saved.
    - Save RAM - if ticked the z-corrected image is built frame by frame instead of building the image in one go. This saves RAM but approximately doubles the time for processing.
  
  1. Click ok. The script will run.
  2. When the script has completed the process, you will have the following files in the same folder as the original image:
       - corrected images
       - drift plots
       - drift tables
       - a settings file, you can use to run the script on another channel with identical parameters. 
   
   If you plan to apply the correction to another channel, make sure not to move these files to another folder.
  
**Apply drift**

1. Open the "apply" script and click run. User interface opens.  

![image](images/applyUI.png)
*Figure 3: Apply user interface*


2. Browse to the file that you want to which you want to apply the correction.
3. Browse to the settings file (called settings.csv)
4. Click ok. The corrected image will be saved to the same folder as everything else.

Done!



# Known issues

**Fiji crashing without notice**

Windows might not have OpenCL installed. If you experience Fiji crashing without notice, [install the OpenCL through the Microsoft store library.](https://www.microsoft.com/en-us/p/opencl-and-opengl-compatibility-pack/9nqpsl29bfff?activetab=pivot:overviewtab)

**Importance of file locations**

Make sure not to move the drift table from the results folder as the path to the drift table is hardcoded to the settings.csv file. 

**Result images are black**

Try disabling time averaging (1).


# Contributors

* [Joanna Pylvänäinen](https://twitter.com/JwPylvanainen)
* [Romain F. Laine](https://twitter.com/LaineBioImaging)
* [Guillaume Jacquemet](https://twitter.com/guijacquemet)



# When using this script, please cite the NanoJ paper

Laine, R. F., Tosheva, K. L., Gustafsson, N., Gray, R., Almada, P., Albrecht, D., Risa, G. T., Hurtig, F., Lindås, A. C., Baum, B., Mercer, J., Leterrier, C., Pereira, P. M., Culley, S., & Henriques, R. (2019). NanoJ: a high-performance open-source super-resolution microscopy toolbox. Journal of physics D: Applied physics, 52(16), 163001. https://doi.org/10.1088/1361-6463/ab0261


