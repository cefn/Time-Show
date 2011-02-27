package com.cefn.time.hourglass;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import processing.core.PImage;

public class StoryboardApp extends App{
	
	static final int MAX_BOUNCERS = 32;
	
	protected List<Stage> stages = new ArrayList<Stage>();
	protected List<Stage> liveStages = new ArrayList<Stage>();
	
	public void setup() {
	  size(w,h);
	  
	  obstacles.add(new Funnel(new Vector(w/2,0), w/2));
	  obstacles.add(new Floor(0,0.0002f,false));
	  obstacles.add(new Floor(h,0.0002f,true));
	  
	  stages = Arrays.asList(new Stage[]{
			  new Stage("sperm.png","porn.wav"){
				  public void setup(){
					  Bouncer sperm = new Bouncer(getImage(), new Vector(w/2,0));
					  sperm.noise = 0.02f;
					  bouncers.add(sperm);
					  super.setup();
				  }
			  },
			  new Stage("heart.png","porn.wav"){
				  PImage egg = loadImage(filePath + "egg.png");
				  float separation = 10;
				  public void draw(){
					  if(attached.size() == 0){
						  image(egg, w/2 - (egg.width/2), h/4 - (egg.height/2));						  
					  }
					  else if(attached.size() < MAX_BOUNCERS){
						  for(Bouncer todivide:attached){
							  Vector spacing = Vector.vectorWithAngle(random(2 * PI));
							  new Bouncer(getImage(), todivide.pos.plus(spacing.times(separation))); //create new
							  todivide.pos = todivide.pos.minus(spacing.times(separation)); //separate old
						  }
					  }
					  super.draw();
				  }
			  },
			  new Stage("baby.png","birth.wav"){},
			  new Stage("kid.png","kidsplay.wav"){},
			  new Stage("adult.png","adult.wav"){},
			  new Stage("pensioner.png","elderly.wav"){}	  
	  });	  	  
	}

	public void draw() {
	  background(0);
	  //translate(w/2,0);
	  //scale(0.2f);
	  for(Bouncer bouncer: bouncers){
	    bouncer.acc = gravity;
	    for(Obstacle obstacle:obstacles){
	    	bouncer.acc = bouncer.acc.plus(obstacle.calculateRepulsion(bouncer));
	    }
	    for(Bouncer otherbouncer:bouncers){
	    	if(otherbouncer != bouncer){
	    		bouncer.acc = bouncer.acc.plus(bouncer.calculateRepulsion(otherbouncer));	    		
	    	}	
	    }
	    bouncer.update(this);
	    bouncer.draw(this);
	  }
	  
	  Vector unitG = gravity.unitVector();
	  line(w/2,h/2,unitG.x * w * 0.5f, unitG.y * w * 0.5f);
	  
	  if(liveStages.size() == 0){
		  liveStages.add(stages.get(0));
		  liveStages.add(stages.get(1));
	  }

	  for(int i = 0; i < liveStages.size(); i++){
		  Stage stage = liveStages.get(i);
		  int index = stage.getIndex();
		  if((index % 2) != 0){
			  stage.draw();			  
		  }
		  else{
			  translate(w,h);
			  rotate(PI);
			  stage.draw();
			  rotate(-PI);
			  translate(-w,-h);
		  }
	  }

	}
		

	
	abstract class Stage {
		public final String imageFile;
		public final String soundFile;
		public Set<Bouncer> attached = new TreeSet<Bouncer>();
		public Stage(String imageFile, String soundFile){
			this.imageFile = imageFile;
			this.soundFile = soundFile;
		}
		public PImage getImage(){
			return loadImage(filePath + imageFile);
		}
		public void attachBouncer(Bouncer bouncer){
			bouncer.pic = getImage();
		}
		public void removeBouncer(Bouncer bouncer){
			
		}
		public void setup(){
		}
		public int getIndex(){
			return stages.indexOf(this);
		}
		public void draw(){
			int pos = getIndex();
			if(pos != -1){
				for(Bouncer bouncer:bouncers){
					boolean odd = pos % 2 != 0;
					if((odd && bouncer.pos.y > h/2) || (!odd && bouncer.pos.y < h/2)){
						if(attached.add(bouncer)){
							if(attached.size() == 1){
								playWav(soundFile);
							}
							attachBouncer(bouncer);							
						}
					}
					else{
						if(attached.remove(bouncer)){
							removeBouncer(bouncer);							
						}						
					}
				}
			}
			else{
				System.out.println("Could not introspect position");
			}
		}
	}

}
