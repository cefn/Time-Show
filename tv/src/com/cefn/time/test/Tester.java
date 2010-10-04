package com.cefn.time.test;

import javax.swing.JOptionPane;

import processing.core.PApplet;

import codeanticode.gsvideo.*;

public class Tester extends PApplet{

	GSMovie[] yearMovies;
	GSMovie noiseMovie;
	long period = 2000;
	
	interface FrameHandler {
		public void movieEvent(GSMovie movie);
	}
	
	class AlwaysReadFrameHandler{
		public void movieEvent(GSMovie movie) {
			movie.read();
		}		
	}
	
	public void setup() {
	  size(640, 480, P3D);
	  background(0);
	  yearMovies = new GSMovie[]{
		         new GSMovie(this, "/home/cefn/Documents/curiosity/timeshift_tv/1950_CBS_ID_512kb.mp4"),
		         new GSMovie(this, "/home/cefn/Documents/curiosity/timeshift_tv/1960_exodus_512kb.mp4")	  
	  };
      noiseMovie = new GSMovie(this, "/home/cefn/Documents/curiosity/timeshift_tv/StaticWhiteNoise_512kb.mp4");
	  	  
	  for(GSMovie movie: yearMovies){
		  movie.loop();		  
		  movie.volume(0.01);
	  }
	  noiseMovie.loop();
	  noiseMovie.volume(0.01);
	  
	}

	public void draw() {
	  float phase = ((float)(millis() % period))/ ((float)period);
	  phase = abs(phase - 0.5f) * 2;

	  image(noiseMovie, 0, 0, width, height);
	  //noiseMovie.volume(max(0,abs(phase - 0.5f)));
	  
	  tint(255,(int)(255 * phase ));
	  image(yearMovies[0], 0, 0, width, height);
	  //yearMovies[0].volume(max(0,abs(phase - 1.0f)));
	  
	  tint(255,(int)(255 * (1.0f -phase)));
	  image(yearMovies[1], 0, 0, width, height);
	  //yearMovies[1].volume(max(0,abs(1.0f - phase)));
	  
	  
	}

}
