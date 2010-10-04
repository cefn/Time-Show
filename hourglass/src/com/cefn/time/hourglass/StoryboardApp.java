package com.cefn.time.hourglass;

import java.applet.AudioClip;
import java.awt.event.KeyEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import processing.core.PApplet;
import processing.core.PImage;

public class StoryboardApp extends PApplet{
	
	String filePath = "/media/197D-0C88/timeshow/hourglassfiles/";
	AudioClip clip;
	
	int w = 600;
	int h = 600;

	static final Vector ZERO = new Vector(0,0);
	static final Vector UNIT = new Vector(1,1);

	final float G = 0.005f;
	
	private Vector gravity;
	float gravityAngle;

	static final int MAX_BOUNCERS = 32;
	
	List<Stage> stages = new ArrayList<Stage>();
	List<Bouncer> bouncers = new ArrayList<Bouncer>();
	List<Obstacle> obstacles = new ArrayList<Obstacle>();

	List<Stage> liveStages = new ArrayList<Stage>();

	Funnel funnel;
	
	public void setup() {
	  size(w,h);
	  	  	  
	  setGravityAngle(0);
	  
	  funnel = new Funnel(new Vector(w/2,0), w/2);
	  obstacles.add(funnel);
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
	
	public void stopWav(){
		if(clip != null){
			clip.stop();
			clip = null;
		}
	}
	
	public void playWav(String filename){
		stopWav();
		try{
			java.net.URL url = new URL("file://" + filePath + filename);
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
	    bouncer.update();
	    bouncer.draw();
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
		
	public void keyPressed(KeyEvent e){
		System.out.println("Received key event " + e);
		if(e.getKeyCode() == LEFT){
			System.out.println("Turning left ");
			setGravityAngle(gravityAngle += PI / 16);			
		}
		if(e.getKeyCode() == RIGHT){
			System.out.println("Turning right ");
			setGravityAngle(gravityAngle -= PI / 16);			
		}
	}
	
	public void stop(){
		if(clip != null){
			clip.stop();			
		}
		super.stop();
	}

	static class Vector {
	  final float x;
	  final float y;
	  Vector(float x, float y){
	    this.x = x;
	    this.y = y;
	  }
	  public String toString(){
		  return "[" + x + "," + y + "]";
	  }	  
	  Vector plus(Vector toadd){
	    return new Vector(this.x + toadd.x, this.y + toadd.y);
	  }
	  Vector minus(Vector totake){
	    return new Vector(this.x - totake.x, this.y - totake.y);
	  }
	  Vector times(Vector totimes){
	    return new Vector(this.x * totimes.x, this.y * totimes.y);
	  }
	  Vector times(float by){
	    return new Vector(this.x * by, this.y * by);
	  }
	  float squareofhypotenuse(){
	    return (x * x) + (y * y);
	  }
	  float hypotenuse(){
	    return sqrt(this.squareofhypotenuse());
	  }
	  Vector unitVector(){
	    return this.times(1.0f/this.hypotenuse());
	  }
	  static Vector vectorWithAngle(float angle){ //0 is down the screen
		  return new Vector(sin(angle), cos(angle));
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
	
	class Bouncer implements Obstacle{

	  PImage pic;
	  Vector pos = new Vector(0,0), 
	         vel = new Vector(0,0), 
	         acc = new Vector(0,0);
	  float noise = 0;
	  long lastnow = -1;
	  
	  Bouncer(PImage pic, Vector pos){
	    this.pic = pic;
	    this.pos = pos;
	  }
	  
	  void update(){
	    long now = millis();
	    float time = lastnow != -1 ? (float)(now - lastnow) : 0;
	    lastnow = now;
	    float maxacc = 0.01f;
	    if(acc.hypotenuse()>maxacc){
	      acc = acc.unitVector().times(maxacc);
	    }
	    if(noise != 0){
	    	acc = acc.plus(Vector.vectorWithAngle(random(2 * PI)).times(noise));
	    }
	    pos = pos.plus(vel.times(time));
	    vel = vel.plus(acc.times(time));
	    vel = vel.times(0.5f);
	  }
	  
	  void draw(){
	    image(this.pic,this.pos.x - (this.pic.width/2),this.pos.y - (this.pic.height/2));      
	  }
	  
	  public Vector calculateRepulsion(Bouncer other){
		Vector diff = this.pos.minus(other.pos);
	    float spacing = diff.hypotenuse();
	    float strength = 1.0f / diff.squareofhypotenuse() * 16.0f;
	    float separation = spacing - 50;
	    if(separation > 0){
	      strength = strength / sqrt(sqrt(sqrt(separation)));
	    } 
	    return this.pos.minus(other.pos).unitVector().times(strength);       
	  }
	  
	}

	interface Obstacle{
	  Vector calculateRepulsion(Bouncer bouncer);
	}

	class Bowl implements Obstacle{
	  Vector centre;
	  float radius;
	  float hardness = 0.0002f;
	  Bowl(Vector centre, float radius){
	    this.centre = centre;
	    this.radius = radius;
	  }
	  public Vector calculateRepulsion(Bouncer bouncer){
	    Vector diff = this.centre.minus(bouncer.pos);
	    float distfromcentre = diff.hypotenuse();
	    if(distfromcentre > this.radius){
	      float strength = (distfromcentre - radius) * hardness;
	      return diff.unitVector().times(strength);
	    }
	    else{
	      return ZERO;
	    }
	  } 
	}

	class Funnel extends Bowl{
	  Funnel(Vector centre, float radius){
	    super(centre,radius);
	  }
	  
	  public Vector calculateRepulsion(Bouncer bouncer){
	    Vector diff = bouncer.pos.minus(centre);
	    float hypotenuse = diff.hypotenuse();
	    if(hypotenuse < radius * 2.0){
	    	Vector repulsion = super.calculateRepulsion(bouncer);
	    	if(abs(bouncer.pos.x - centre.x) < 80){
	    		return new Vector(repulsion.x, 0); //things can drop through the middle (no vertical force)
	    	}
	    	else{
	    		return repulsion;
	    	}
	    }
	    else{
	    	return ZERO;
	    }
	  }
	}

	class Floor implements Obstacle{
	  float y;
	  float hardness;
	  boolean up ;
	  Floor(float y, float hardness, boolean up){
	    this.y = y;
	    this.hardness = hardness;
	    this.up = up;
	  }
	  public Vector calculateRepulsion(Bouncer other){
	    float ydiff = other.pos.y - this.y;
	    if((up && ydiff > 0) || (!up && ydiff < 0)){
	      return new Vector(0, hardness * ydiff * (up?-1.0f:1.0f));
	    }
	    else{
	      return new Vector(0,0);
	    }
	  }
	}

}
