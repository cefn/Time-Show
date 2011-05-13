package com.cefn.time.hourglass;

import java.applet.AudioClip;
import java.awt.event.KeyEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import processing.core.PApplet;

public abstract class App extends PApplet{
	
	AudioClip clip;
		
	float WIDTH = 480;
	float HEIGHT = 800;

	static final Vector ZERO = new Vector(0,0);
	static final Vector UNIT = new Vector(1,1);

	public static final float G = 50f;	
	float gravityAngle;
	Vector gravity;
	
	protected List<Bouncer> bouncers = new ArrayList<Bouncer>();
	protected List<Obstacle> obstacles = new ArrayList<Obstacle>();
	
	public App() {
		setGravityAngle(0);
	}

	public void stopWav(){
		if(clip != null){
			clip.stop();
			clip = null;
		}
	}
	
	public void playWav(String filename){
		stopWav();
		try{
			java.net.URL url = new URL("file://" + filename);
			java.io.File file = new java.io.File(url.toURI());
			System.out.println("Attempting to play " + file.getAbsolutePath());
			clip = this.getAudioClip(url);
			clip.play();		  
		}
		catch(MalformedURLException mue){
			mue.printStackTrace();
		}
		catch (java.net.URISyntaxException ex) {
			ex.printStackTrace();
		}	  
	}
	
	public void setGravityAngle(float angle){
		this.gravityAngle = angle;
		this.gravity = Vector.vectorWithAngle(gravityAngle).times(G);
		System.out.println("New gravity unit vector is " + gravity.unitVector());
	}	
	
	public float getGravityAngle(){
		return (float)Math.atan2((double)gravity.x,(double)-gravity.y);
	}
	
	public void keyPressed(KeyEvent e){
		//System.out.println("Received key event " + e);
		if(e.getKeyCode() == LEFT){
			//System.out.println("Turning left ");
			setGravityAngle(gravityAngle += PI / 16);			
		}
		if(e.getKeyCode() == RIGHT){
			//System.out.println("Turning right ");
			setGravityAngle(gravityAngle -= PI / 16);			
		}
	}
	
	public static float signum(float f) {
		  if (f > 0) return 1f;
		  if (f < 0) return -1f;
		  return 0;
	} 


}
