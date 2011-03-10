package com.cefn.time;

import java.awt.FontMetrics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import codeanticode.gsvideo.GSMovie;

import processing.core.PApplet;
import processing.core.PFont;

public class App extends PApplet{
	
	File videoRoot = new File("/media/BTEXT3/timeshow/tv");
	//File videoRoot = new File("/home/cefn/Videos/ted");

	YearRegistry years = new BasicYearRegistry();
	
	Traveller traveller = new AbstractTraveller();
	
	Tv tv = null;	
	
	YearDisplay display = null;
	
	long loopcount = 0;
		
	public static void main(String[] args) {
        PApplet.main(new String[] {App.class.getName()});
    }
	
	public void setup(){ 
		
		size(640,480,P3D);
		frameRate(30);
		
		traveller.addListener(new Traveller.Listener(){
			public void yearChanged(Traveller traveller) {
				System.out.println("Set year to: " + traveller.getYearName());
			}
		});
		
		//load video from year folders
		List<File> yearDirList = Arrays.asList(videoRoot.listFiles());
		for(File yearDir:yearDirList){
			if(yearDir.isDirectory()){
				try{
					int yearName = Integer.parseInt(yearDir.getName());
					List<File> videoFileList = Arrays.asList(yearDir.listFiles(new FileFilter() {
						public boolean accept(File pathname) {return pathname.getName().endsWith(".mp4");}
					}));
					years.add(new BasicYear(yearName,videoFileList));
				}
				catch(NumberFormatException nfe){ 
					//ignore filestructure which doesn't conform
				}
			}
		}		

		display = new YearDisplay();

		display.registerFont(1890,"Old Town Normal");
		display.registerFont(1900,"DavidaC");
		display.registerFont(1910,"BaphometCondensed");
		display.registerFont(1920,"ArtDecoSSi");
		display.registerFont(1930,"Ukrainian Play");
		//display.registerFont(1940,"Torhok Italic:001.001");
		display.registerFont(1940,"Verdana");
		display.registerFont(1950,"TS Curly");
		display.registerFont(1960,"FlowerPower  Italic");
		display.registerFont(1970,"Digital-7");
		display.registerFont(1980,"el&font gohtic!");
		display.registerFont(1990,"Comic Sans MS");
		display.registerFont(2000,"Verdana");
		display.registerFont(2010,"Liberation Mono");
		
		traveller.addListener(display);
		
		//set a random year
		traveller.setYearName(years.randomYear().getName());
		
		try{
			tv = new Tv(
					years.yearCalled(traveller.getYearName()).getVideoFiles(),
					new File(videoRoot.getCanonicalPath() + "/" + "noise.mp4")
			);			
			traveller.addListener(tv);
		}
		catch(IOException ioe){
			System.out.println("Problem loading file");
			tv = null;
		}
				
	}
	
	public void movieEvent(GSMovie movie) {
		movie.read();
	}		
	
	public void draw(){
		noCursor();
		tv.draw(this);
		display.draw(this);
		loopcount++;
	}
	
	public void mouseMove(MouseEvent e){
		cursor();
	}
			
	public void keyPressed(KeyEvent e){
		super.keyPressed(e);
		if(e.getKeyCode() == UP){
			traveller.setYearName(years.nameBefore(traveller.getYearName()));
		}
		if(e.getKeyCode() == DOWN){
			traveller.setYearName(years.nameAfter(traveller.getYearName()));
		}
		if(e.getKeyCode() == LEFT){
			tv.decrementFrequency();
		}
		if(e.getKeyCode() == RIGHT){
			tv.incrementFrequency();
		}
	}
	
	class Tv implements Traveller.Listener, Tunable, Drawable{
		GSMovie noise = null;
		GSMovie playing = null;
		float freq;
		float freqStep;
		List<GSMovie> movies;
		
		static final float UPPER_FREQ = 1.0f;
		static final float LOWER_FREQ = 0.0f;
		
		public Tv(List<File> videoFiles, File noiseFile) throws IOException{
			setVideoFiles(videoFiles);
			noise = new GSMovie(App.this, noiseFile.getCanonicalPath());
			noise.loop();
			noise.volume(0.0);
		}
		
		public void yearChanged(Traveller traveller) {
			try{
				setVideoFiles(years.yearCalled(traveller.getYearName()).getVideoFiles());				
			}
			catch(IOException ioe){
				System.out.println("Couldn't load new video file paths");
			}
		}
		
		synchronized void setVideoFiles(List<File> videoFiles) throws IOException{
			freq = random(0.99f);
			freqStep = 1.0f/(videoFiles.size() * 20f);
			setPlaying(null);
			if(movies != null){
				for(GSMovie movie:movies){ //dispose of old movies? commented for now
					movie.dispose();
				}				
			}
			movies = new ArrayList<GSMovie>(); //empty list for new movies
			for(File videoFile:videoFiles){
				movies.add(new GSMovie(App.this, videoFile.getCanonicalPath()));
			}
			setPlaying(getMovie());
		}
		
		boolean jumped=true;
		public void setPlaying(GSMovie next){
			if(playing != null){
				playing.pause();
			}
			playing = next;
			if(playing != null){
				jumped = false;
				playing.loop();
				playing.volume(0.9);
			}
		}
		
		public synchronized void draw(PApplet applet) {
			applet.noTint();
			///*
			if(this.getBandOffset() < 0.25f){
				if(random(1.0f) < 0.1f){
					if(random(1.0f)<0.5f){
						decrementFrequency();
					}
					else{
						incrementFrequency();
					}
				}				
			}
			else{
				if(random(1.0f) < 0.1f){
					incrementFrequency();						
				}				
			}
			if(random(1.0f) < 0.0004f){
				traveller.setYearName(years.randomYear().getName());
			}
			//*/
			//establish correct video to play
			GSMovie current = getMovie();
			if(current != playing){
				setPlaying(current);
			}
			
			//noise
			if(lastNoiseVolume != getNoiseVolume()){
				lastNoiseVolume = getNoiseVolume();
				noise.volume(lastNoiseVolume);
			}
			applet.tint(255,255);
			applet.image(noise, 0, 0,width,height);

			//movie
			if(playing != null){
				//jump to correct point
				float duration = playing.duration();
				if(!jumped && duration > 0){
					jumped = true;
					long now = System.currentTimeMillis();
					long timecode = now % ((long)(duration * 1000f));
					playing.jump(((float)timecode) / 1000f);
				}
				//set correct volume
				if(lastMovieVolume != getMovieVolume()){
					lastMovieVolume = getMovieVolume();
					playing.volume(lastMovieVolume);
				}
				float opacity = getMovieOpacity();
				applet.tint(255,(int)(255f * opacity));
				applet.image(playing,0,0,width,height);				
			}
		}
		double lastMovieVolume,lastNoiseVolume;
		
		
		public int getMovieIndex(){
			float numMovies = movies.size();
			return floor((freq < UPPER_FREQ? freq : LOWER_FREQ ) * numMovies); //frequency wraps			
		}

		
		GSMovie lastMovie = null;
		public synchronized GSMovie getMovie() {
			GSMovie movie = movies.get(getMovieIndex());
			if(movie != lastMovie){
				System.out.println("Playing: " + movie .getFilename());
				lastMovie = movie;
			}
			return movie;	
		}
		
		private float getBandOffset(){
			float numMovies = movies.size();
			float bandwidth = 1.0f / numMovies;
			float midband = (((float)getMovieIndex()) + 0.5f) * bandwidth;
			float bandoffset = min(1.0f, abs((freq - midband)/bandwidth) * 2.0f);
			//System.out.println("BO:" + bandoffset);
			return bandoffset;
		}

		public float getMovieOpacity() {
			return 1.0f - getBandOffset();
		}
		
		public double getMovieVolume() {
			return constrain(1.0f - getBandOffset(), 0.01f, 0.99f);
		}
		
		public double getNoiseVolume() {
			return 0.5 * constrain(getBandOffset(), 0.01f, 0.99f);
		}
	
		public void incrementFrequency() {
			freq += freqStep;
			wrapFrequency();
		}
		public void decrementFrequency() {
			freq -= freqStep;
			wrapFrequency();
		}
		private void wrapFrequency(){
			if(freq < LOWER_FREQ){ freq = UPPER_FREQ;}
			if(freq >= UPPER_FREQ){ freq = LOWER_FREQ;}
		}
		
	}

	class BasicYear implements Year{
		int name;
		List<File> videoFiles;
		BasicYear(int name, List<File> videoFiles){
			this.name = name;
			this.videoFiles = videoFiles;
		}
		public int getName(){ return name; }
		public List<File> getVideoFiles(){return videoFiles; }
	}
		
	class BasicYearRegistry implements YearRegistry{
		List<Year> yearList = new ArrayList<Year>();
		Map<Integer,Year> yearMap = new HashMap<Integer,Year>();		
		
		public Year randomYear(){
			return years.yearAt(floor(random(0,years.size())));
		}
		
		public void add(Year year) {
			yearList.add(year);
			yearMap.put(year.getName(), year);
			Collections.sort(yearList, new Comparator<Year>() {
				public int compare(Year y1, Year y2) {
					Integer i1 = new Integer(y1.getName());
					Integer i2 = new Integer(y2.getName());
					return i1.compareTo(i2);
				}
			});
		}
		public int size() {
			return yearList.size();
		}
		public Year yearAt(int pos){
			return yearList.get(pos);
		}
		public Year yearCalled(int name){
			return yearMap.get(name);
		}
		private int indexOf(Year year){
			return yearList.indexOf(year);
		}
		public int nameAfter(int name) {
			return yearAt((indexOf(yearCalled(name)) + 1) % yearList.size()).getName();
		}
		public int nameBefore(int name) {
			return yearAt((indexOf(yearCalled(name)) - 1 + yearList.size()) % yearList.size()).getName();
		}

	}
			
	class YearDisplay implements Drawable, Traveller.Listener{
		
		List<PFont> fontList = new ArrayList<PFont>();
		List<Integer> fontFromList = new ArrayList<Integer>();
		
		private PFont yearFont;
		private float x, y;
		long lastChange = millis();

		public void registerFont(int from, String name){
			fontList.add(createFont(name, 128));
			fontFromList.add(from);
		}
		
		public PFont getYearFont(int year){
			for(int i = fontFromList.size(); i--> 0;){
				if(fontFromList.get(i) < year){
					return fontList.get(i);
				}
			}
			return fontList.get(0);
		}

		
		public void yearChanged(Traveller traveller) {
			lastChange = millis();
			int yearName = traveller.getYearName();
			yearFont = getYearFont(yearName);
			FontMetrics metrics = App.this.getFontMetrics(yearFont.getFont());
			int textwidth = metrics.stringWidth(Integer.toString(yearName));
			int textheight = metrics.getHeight();
			x = (App.this.width/2) - (textwidth/2);
			y = (App.this.height/2) - (textheight/2);
		}
				
		public void draw(PApplet applet){
			if(millis() - lastChange < 10000){
				textFont(yearFont);
				stroke(color(255));
				applet.text(Integer.toString(traveller.getYearName()), x, y);				
			}
		}

	}
	
}
