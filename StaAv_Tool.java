/*
//  Stack Avreging Tool
//  
//  Copyright © 2019 Saleh Altahini. All rights reserved.
*/
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.tool.PlugInTool;
import java.awt.event.*;

import ij.plugin.filter.Analyzer;
import ij.measure.*;
import java.util.ArrayList;
import java.util.List;

public class StaAv_Tool extends PlugInTool implements ImageListener, KeyListener{

    ImagePlus monitored_stack;
    boolean listeners_active = false;
    List<Integer> slices = new ArrayList<Integer> (); 
    int current_frame;
    ImageWindow win;
    ImageCanvas canvas;
    double fpm = 100;
    boolean get_background = true;
    double freq = 30.8; // in Hz
    double decay = 1100; // in ms
    double fprm = 1;
    
    public void run(String arg) {
        Toolbar.addPlugInTool(this);
	}
    
	public void mousePressed(ImagePlus imp, MouseEvent e) {
        markImage(imp);
	}
    
    public void mouseMoved(ImagePlus imp, MouseEvent e){
        if(listeners_active)
            IJ.showStatus("Marked frames: " + slices.size());
        else
            IJ.showStatus("Tool inactive");
    }
    
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == 32){
            markImage(monitored_stack);
        }
        else
            IJ.getInstance().keyPressed(e);
    }
    
    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}
    
    public void imageOpened(ImagePlus imp){
        return;
    }
    
    public void imageClosed(ImagePlus imp){
        if(imp == monitored_stack){
            listeners_active = false;
            slices.clear();
            deregisterListeners();
        }
    }
    
    public void imageUpdated(ImagePlus imp){
        if(imp == monitored_stack){
            if (imp.getSlice() == current_frame)
                return;
            current_frame = imp.getSlice();
            if (slices.contains(current_frame)){
                drawOverlay(imp.getProcessor());
                imp.updateAndDraw();
            }
        }
    }

	public void showOptionsDialog() {
        String w_names[] = new String[(WindowManager.getWindowCount()+1)];
        w_names[w_names.length-1] = "none";
        String cs;
        int ids[] = WindowManager.getIDList();
        for (int i = 0; i < ids.length; i++){
			if (WindowManager.getImage(ids[i]).getImageStackSize() > 1)
			{
				w_names[i] = WindowManager.getImage(ids[i]).getTitle();
			}
		}
        if (WindowManager.getCurrentImage().getImageStackSize() > 1){
            cs = WindowManager.getCurrentImage().getTitle();
        }else{
            cs = "none";
        }
        GenericDialog gd = new GenericDialog("Stack Avreging Tool");
        gd.enableYesNoCancel("Ok", "Get Stack");
        gd.addMessage("Select the Stack you want to mark");
		gd.addChoice("Stack",w_names,cs);
        gd.addMessage("Click on a frame or press the space button to mark/unmark it");
        //gd.addMessage('Press the "M" button to make a new stack');
        gd.addMessage("Currently marked frames: "+slices.size());
        gd.addNumericField("Number of frames for each mark", fpm, 0);
        gd.addCheckbox("Automatically mark background frames?", get_background);
        gd.addNumericField("Imaging frequency in Hz", freq, 1);
        gd.addNumericField("Decay time of Indicator in ms", decay, 0);
        gd.addNumericField("Number of frames for each background frame", fprm, 0);
		gd.showDialog();
      	if (gd.wasCanceled()) return;
        get_background = gd.getNextBoolean();
        fpm = gd.getNextNumber();
        freq = gd.getNextNumber();
        decay = gd.getNextNumber();
        fprm = gd.getNextNumber();
        String stack_name = gd.getNextChoice();
        if (gd.wasOKed()){
            if (stack_name == "none"){
                if (listeners_active){
                    listeners_active = false;
                    deregisterListeners();
                    slices.clear();
                }
                return;
            } 
            if (monitored_stack == WindowManager.getImage(stack_name))
                return;
            if (listeners_active){
                listeners_active = false;
                deregisterListeners();
                slices.clear();
            }
            listeners_active = true;
            monitored_stack = WindowManager.getImage(stack_name);
            registerListeners();
        }else{
            if (!listeners_active){
                listeners_active = true;
                monitored_stack = WindowManager.getImage(stack_name);
                registerListeners();
            }
            getNewStack();
        }
        
	}
    
    private void getNewStack(){
        boolean get_marked = false;
        int total_frames = 0;
        int frame_interval = 0;
        GenericDialog gd = new GenericDialog("Stack Avreging Tool");
        gd.addMessage("Currently marked frames: "+slices.size());
        gd.addMessage("Number of frames for each mark: " + fpm);
        if(slices.size() > 0){
            gd.addMessage("total of marked frames: " + (slices.size()*fpm));
            get_marked = true;
            total_frames = (int) (slices.size()*fpm);
        }
        
        if(get_background){
            gd.addMessage("Imaging frequency in Hz: " + freq);
            gd.addMessage("Decay time of Indicator in ms: " + decay);
            gd.addMessage("Decay time of Indicator in ms: " + Math.round((freq/1000)*(decay/2)));
            frame_interval = (int) Math.round((freq/1000)*(decay/2));
            gd.addMessage("A frame evrey: " + frame_interval + " frames will be taken for backgrounf");
            gd.addMessage("Number of frames for each background frame: " + fprm);
            gd.addMessage("Total of background frames: " + ((monitored_stack.getImageStackSize() / frame_interval) * fprm));
            total_frames += ((monitored_stack.getImageStackSize() / frame_interval) * fprm);
        }
        gd.showDialog();
      	if (gd.wasCanceled()) return;
        
        
        // update the marked frames array
        // using for loops instead of bitstreams for compatibility
        IJ.showStatus("Indexing Images");
        if(get_marked){
            // get slices.size() ref because it changes
            int stackSize = slices.size();
            for(int i = 0; i < stackSize; i++){
                int start = (int) (slices.get(i) - (fpm/2));
                int end = (int) (slices.get(i) + (fpm/2));
                if(start < 0){
                    end += -start;
                    start = 0;
                }
                if(end >= monitored_stack.getImageStackSize()){
                    end = monitored_stack.getImageStackSize();
                    start = (monitored_stack.getImageStackSize()-100);
                }
                
                for(int x = start; x < end; x++){
                    if(slices.contains(x)) continue;
                    slices.add(x);
                }
                
            }
        }
        if(get_background){
            for(int i = 1; i < (monitored_stack.getImageStackSize() / frame_interval); i++){
                if((i*frame_interval) >= monitored_stack.getImageStackSize()) break;
                for(int x = (i*frame_interval); x < ((i*frame_interval)+fprm); x++){
                    if(slices.contains(x)) continue;
                    slices.add(x);
                }
            }
        }
        
        IJ.showStatus("Copying images");
        ImageStack fullStack = monitored_stack.getImageStack();
        int height = WindowManager.getCurrentImage().getHeight();
		int width  = WindowManager.getCurrentImage().getWidth();
        ImageStack newStack = new ImageStack(width,height);
        ResultsTable rt = Analyzer.getResultsTable();
        if (rt == null) {
            rt = new ResultsTable();
            Analyzer.setResultsTable(rt);
        }
        int new_i = 1;
        int framesCount = monitored_stack.getImageStackSize();
        for (int i = 0; i < monitored_stack.getImageStackSize(); i++) {
            IJ.showProgress(i, framesCount);
            if(slices.contains(i)){
                rt.incrementCounter();
                newStack.addSlice(fullStack.getSliceLabel((i+1)), fullStack.getProcessor((i+1)));
                rt.addValue("File Name", fullStack.getSliceLabel((i+1)));
                rt.addValue("Original Order", (i+1));
                rt.addValue("New Order" , new_i);
                new_i++;
            }
        }
        ImagePlus newImages = new ImagePlus("New Stack", newStack);
        StackWindow window = new StackWindow(newImages);
        rt.show("Results");
        IJ.showStatus("Done!");
        listeners_active = false;
        deregisterListeners();
        slices.clear();
        
    }
    
    private void registerListeners(){
        //IJ.log("registered Listeners!");
        ImagePlus.addImageListener(this);
        win = monitored_stack.getWindow();
        canvas = win.getCanvas();
        win.removeKeyListener(IJ.getInstance());
        canvas.removeKeyListener(IJ.getInstance());
        win.addKeyListener(this);
        canvas.addKeyListener(this);
    }
    
    private void deregisterListeners(){
        //IJ.log("deregistered Listeners!");
        ImagePlus.removeImageListener(this);
        if (win!=null)
            win.removeKeyListener(this);
        if (canvas!=null)
            canvas.removeKeyListener(this);
    }
    
    private void markImage(ImagePlus imp) {
        if(imp != monitored_stack) return;
        current_frame = imp.getSlice();
        if (slices.contains(current_frame)){
            slices.removeIf(p -> p == current_frame);
            imp.getProcessor().reset();
            imp.updateAndDraw();
            IJ.log("unmarked " + imp.getSlice());
        }else{
            slices.add(current_frame);
            drawOverlay(imp.getProcessor());
            imp.updateAndDraw();
            IJ.log("marked " + imp.getSlice());
        }
        IJ.showStatus("Marked frames: " + slices.size());
    }
    
    private void drawOverlay(ImageProcessor ip){
        ip.snapshot();
        ip.setFont(new Font("SanSerif", Font.PLAIN, 18));
		ip.setAntialiasedText(true);
        ip.resetRoi();
		ip.setColor(Color.white);
        ip.drawString("Marked", 10, 40);
	}
    
    public String getToolIcon() {
        return "C037T5f16S";
    }

    public String getToolName() {
        return "Stack Avreging Tool";
    }
    

}
