# StaAv: Stack Avreging Tool
An ImageJ Plugin that can be used to mark and copy frames from an image stack to a new stack to create an average image with enhanced cells visibility from the new stack. 
This Plugin can be used either by manually selecting only the frames with cell activity, by automaticly marking a frame for every half of the calcium indicator decay time or both. 


## Installing the Plugin:
### Method 1:
1. Copy the “StaAv_Tool.class” file to the plugins folder of ImageJ or Fiji. (In Windows it’s mostly
under C:\Program Files\ImageJ\plugins).
2. Restart ImageJ or go to Help>Refresh Menus in ImageJ.
### Method 2:
- In ImageJ, go to Plugins>Install and choose the “StaAv_Tool.class” file.


## How to use:
1. Open the time lapse (as a virtual stack for faster performance) in ImageJ.
2. Double click on the tools icon to open the options menu. If no icon is visible, go to
Plugins>StaAv Tool to select the tool first.
3. Select the stacks name from the drop menu, if only one stack is opened it will be selected automatically.
4. Choose a number of frames for each mark. For example if the value here is 100 and you mark the frame #200, then the frames [150,151,...,200,..., 248, 249] will be copied.
5. If the “automatically mark background frames” is checked, the plugin will copy a frame for every half of the decay time. For a time lapse with a frequency of 30.8Hz and using GCaMP with a decay time of 1100ms, a frame every 550ms i.e. the frames [17, 34, 51, ...] will be copied.
6. Click on ok to start marking frames by clicking on them or by pressing the space bar then reopen the options menu and click on “Get Stack”.  
**Or** immediately click on “Get Stack” to automatically mark and copy frames based on the decay time.
7. A new image stack will be opened with the selected frames and a table containing the original and new frames order. Create an average image as usual and save the new stack and the table if wanted.


## Theory of function:
The Plugin saves the marked frames numbers in an array and then copies the frames to a newly created image stack.
For automatically marking frames, the time interval between the marks is calculated depending on the imaging frequency and indicator decay time from the user input.
```
Frame Interval = (Imaging frequency in Hz / 1000) * (Decay time of Indicator in ms / 2)
```
