package com.cefn.time.hindsight;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import codeanticode.gsvideo.GSMovie;
import fullscreen.FullScreen;

import processing.core.PApplet;
import processing.serial.Serial;

public class HindsightApp extends PApplet{

	File videoRoot = new File("/media/BTEXT3/timeshow/tv");
	File noiseMovieFile = new File(videoRoot,"noise.mp4");

	List<Year> cachedYears = new ArrayList<Year>();
	Map<Integer, GSMovie> movieCache = new HashMap<Integer,GSMovie>();
	int maximumCachedYears = 16;
	
	long lastVideoLoad = 0;
	long videoLoadDelay = 25;

	public static float WIDTH = 640;
	public static float HEIGHT = 480;
	
	public static void main(String[] args) {
        PApplet.main(new String[] {HindsightApp.class.getName()});
    }
	
	long lastRead = 0;
	public void movieEvent(GSMovie movie) {
		while(movie.available()){
			movie.read();		
			lastRead = millis();
		}
	}
	
	protected Serial serial;
	int potValue = 0; //the analog reading from the potentiometer
	int keyValue = 0; //the analog reading from the keyswitch
	
	protected YearRegistry years = new BasicYearRegistry();
	GSMovie noiseMovie = null;
	
	Year playingYear = null;
	GSMovie playingMovie = null;
	boolean playingMovieJumped = false; //has the currently playing movie been set to the right timestamp
	
	FullScreen fullScreen;
	
	public void setup(){
		// configure processing
		size((int) WIDTH, (int) HEIGHT, P3D);
		
		frameRate(30);
		
		fullScreen = new FullScreen(this);
		fullScreen.setShortcutsEnabled(true);
		fullScreen.enter();
		
		serial = new Serial(this, Serial.list()[0], 9600);
		
		try{
			noiseMovie = new GSMovie(this, noiseMovieFile.getCanonicalPath());
			noiseMovie.loop();
			noiseMovie.volume(0);			
		}
		catch(IOException ioe){
			noiseMovie = null;
		}
		
		//load video from year folders
		List<File> yearDirList = Arrays.asList(videoRoot.listFiles());
		for(File yearDir:yearDirList){
			if(yearDir.isDirectory()){
				try{
					int yearName = Integer.parseInt(yearDir.getName());
					List<File> videoFileList = Arrays.asList(yearDir.listFiles(new FileFilter() {
						public boolean accept(File pathname) {return pathname.getName().endsWith(".mp4");}
					}));
					if(videoFileList.size() > 0){
						Year newYear = new BasicYear(yearName,videoFileList);
						newYear.setLoadNext((int)random(videoFileList.size()));
						years.add(newYear);						
					}
				}
				catch(NumberFormatException nfe){ 
					//ignore filestructure which doesn't conform
				}
			}
		}		
				
	}
	
	float noiseStrength = 0;
	float noiseFadeStep = 4f/30f;
	@Override
	public void draw() {
		//read from hardware
		readValuesFromSerial();
		
		float yearTuning = map(constrain(potValue,52,1021), 1021, 52, 0, years.size() - 1);
		int yearIndex = (int)round(yearTuning);

		Year currentYear = years.yearAt(yearIndex);
		setPlayingYear(currentYear);
						
		/*
		noiseStrength = abs((yearTuning - ((float)yearIndex))) * 2f; //should be approx 1
		noiseStrength *= noiseStrength; //use the square of the linear distance for smoothing
		*/
		
		noiseStrength = (playingMovie == null ? noiseStrength + noiseFadeStep : noiseStrength - noiseFadeStep);
		noiseStrength = constrain(noiseStrength, 0, 1f);
		float movieStrength = 1f - noiseStrength;
		//paint noise
		if(noiseMovie != null){
			noTint();
			//tint(255,255);
			noiseMovie.volume(noiseStrength * 0.5f);
			image(noiseMovie, 0, 0,width,height);
		}
		
		//paint movie
		if(playingMovie != null){			
			
			//reposition movie to timestamp
			float duration = playingMovie.duration();
			if(duration != 0){
				if(!playingMovieJumped){
					long now = System.currentTimeMillis();
					long timecode = now % ((long)(duration * 1000f));
					playingMovie.jump(((float)timecode) / 1000f);
					playingMovieJumped = true;
				}
			}
			
			//paint with transparency
			tint(255,(int)(255f * movieStrength));
			playingMovie.volume(movieStrength);
			image(playingMovie, 0, 0, width, height);	
			if(playingMovie.duration() > 0 && (playingMovie.duration() - playingMovie.time() < 0.01f)){
				System.out.println("Resetting");
				removeFromCache(playingYear);
			}
			
		}
		
	}
	
	public void readValuesFromSerial(){
		//load in the current settings for the pot and the key
		if(serial != null){
			while(serial.available() > 0){
				String sensorValues = serial.readStringUntil('\n');
				if(sensorValues != null){
					Pattern pattern = Pattern.compile("Pot([0-9]+)Key([0-9]+)");
					Matcher matcher = pattern.matcher(sensorValues);
					if(matcher.find()){
						potValue = Integer.parseInt(matcher.group(1));
						keyValue = Integer.parseInt(matcher.group(2));
					}				
				}				
			}
		}		
	}

	Year lastYearSelection = null;
	int lastYearChanged = 0;
	long settlingFrames = 1;
	public void setPlayingYear(Year year){
		//TODO CH check if year stays put and blend in from noise after
		if(year != lastYearSelection){
			System.out.println("Pot:" + potValue + " Key:" + keyValue);
			System.out.println("New year selected:" + year.getName());
			lastYearSelection = year;
			lastYearChanged = frameCount;
		}

		if(year != playingYear || playingMovie == null){
			if(playingMovie != null){
				if(playingMovie.isPlaying()){
					playingMovie.pause();
				}
				playingMovie = null;
			}
			System.out.println("Selection " + year.getName() + " is not playingYear");
			if((frameCount - lastYearChanged) > settlingFrames){
				System.out.println("Settling period passed for:" + year.getName());
				playingYear = year;
				playingMovie = getCachedMovie(year);
				playingMovieJumped = false;
				System.out.println("Beginning: " + year.getName() + " with " + playingMovie.getFilename());
				System.out.println("Years cached: " + cachedYears.size());
				playingMovie.play(); //trigger playback
				playingMovie.volume(0); //start silent				
			}
		}
	}
			
	public GSMovie getCachedMovie(Year year){
		GSMovie cachedMovie = movieCache.get(year.getName());
		if(cachedMovie == null){
			return addToCache(year);
		}
		else{
			return cachedMovie;
		}
	}
	
	public GSMovie addToCache(Year year){
		//remove oldest movie if too many in cache
		if(cachedYears.size() >= maximumCachedYears){
			removeFromCache(cachedYears.get(0));					
		}
		
		//add the newest movie to the cache
		try{
			cachedYears.add(year);
			List<File> files = year.getVideoFiles();
			File selectedFile = files.get(year.getLoadNext() % files.size()); //visit films in order
			year.incrementLoadNext(); //move pointer to next film 
			GSMovie yearMovie = new GSMovie(this,selectedFile.getCanonicalPath());
			movieCache.put(year.getName(), yearMovie);
			return yearMovie;
		}
		catch(IOException ioe){
			throw new RuntimeException(ioe);
		}
	}

	public void removeFromCache(Year yearToRemove){
		GSMovie movieToRemove = movieCache.remove(yearToRemove.getName());
		if(movieToRemove != null){
			if(movieToRemove == playingMovie){
				playingMovie = null;
			}
			movieToRemove.stop();	
			movieToRemove.delete();
		}
		cachedYears.remove(yearToRemove);
	}

}
