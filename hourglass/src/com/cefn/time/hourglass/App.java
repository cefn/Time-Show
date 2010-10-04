package com.cefn.time.hourglass;

import processing.core.PApplet;
import processing.core.PImage;

public class App extends PApplet{
	
	String filePath = "/media/197D-0C88/timeshow/hourglassfiles/";
	
	int w = 600;
	int h = 600;
	final int NUM_BOUNCERS = 32;

	final Vector G = new Vector(0,0.005f);
	final Vector ZERO = new Vector(0,0);
	final Vector UNIT = new Vector(1,1);

	Bouncer[] bouncers = new Bouncer[NUM_BOUNCERS];
	Obstacle[] obstacles = new Obstacle[]{
	  new Funnel(new Vector(w/2,0), w/2)
	};

	public void setup() {
	  size(w,h);
	  PImage pic = loadImage(filePath + "baby.png");
	  for(int i = 0; i < bouncers.length; i++){
	    bouncers[i] = new Bouncer(pic, new Vector((1 + i)*(((float)w)/((float)NUM_BOUNCERS + 2)),random(4)));
	  }
	}

	public void draw() {
	  background(0);
	  translate(w/2,0);
	  scale(0.2f);
	  for(int i = 0; i < bouncers.length; i++){
	    bouncers[i].acc = G;
	    for(int j = 0; j < obstacles.length; j++){
	      bouncers[i].acc = bouncers[i].acc.plus(obstacles[j].calculateRepulsion(bouncers[i].pos));
	    }
	    for(int j = 0; j < bouncers.length; j++){
	        if(i != j){
	          bouncers[i].acc = bouncers[i].acc.plus(bouncers[i].calculateRepulsion(bouncers[j].pos));
	        }
	    }
	    bouncers[i].update();
	    bouncers[i].draw();
	  }
	}

	class Vector {
	  final float x;
	  final float y;
	  Vector(float x, float y){
	    this.x = x;
	    this.y = y;
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
	}

	class Bouncer implements Obstacle{

	  PImage pic;
	  Vector pos = new Vector(0,0), 
	         vel = new Vector(0,0), 
	         acc = new Vector(0,0);
	  long lastnow = -1;
	  
	  Bouncer(PImage pic, Vector pos){
	    this.pic = pic;
	    this.pos = pos;
	  }
	  
	  void update(){
	    long now = millis();
	    float time = (float)(now - lastnow);
	    lastnow = now;
	    float maxacc = 0.01f;
	    if(acc.hypotenuse()>maxacc){
	      acc = acc.unitVector().times(maxacc);
	    }
	    pos = pos.plus(vel.times(time));
	    vel = vel.plus(acc.times(time));
	    vel = vel.times(0.5f);
	  }
	  
	  void draw(){
	    image(this.pic,this.pos.x - (this.pic.width/2),this.pos.y - (this.pic.height/2));      
	  }
	  
	  public Vector calculateRepulsion(Vector otherpos){
	    Vector diff = this.pos.minus(otherpos);
	    float spacing = diff.hypotenuse();
	    float strength = 1.0f / diff.squareofhypotenuse() * 16.0f;
	    float separation = spacing - 50;
	    if(separation > 0){
	      strength = strength / sqrt(sqrt(sqrt(separation)));
	    } 
	    return this.pos.minus(otherpos).unitVector().times(strength);       
	  }
	  
	}

	interface Obstacle{
	  Vector calculateRepulsion(Vector pos);
	}

	class Bowl implements Obstacle{
	  Vector centre;
	  float radius;
	  Bowl(Vector centre, float radius){
	    this.centre = centre;
	    this.radius = radius;
	  }
	  public Vector calculateRepulsion(Vector pos){
	    Vector diff = this.centre.minus(pos);
	    float distfromcentre = diff.hypotenuse();
	    if(distfromcentre > this.radius){
	      float strength = (distfromcentre - radius) * 0.0001f;
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
	  public Vector calculateRepulsion(Vector pos){
	    Vector diff = pos.minus(centre);
	    if(millis() > 10000 && (diff.hypotenuse() > radius * 1.6 || abs(pos.x - centre.x) < 80)){
	      return ZERO;
	    }
	    else{
	      return super.calculateRepulsion(pos);
	    }
	  }
	}


	class Floor implements Obstacle{
	  float y;
	  float hardness;
	  Floor(float y, float hardness){
	    this.y = y;
	    this.hardness = hardness;
	  }
	  public Vector calculateRepulsion(Vector pos){
	    float ydiff = pos.y - this.y;
	    if(ydiff > 0){
	      return new Vector(0, -hardness * ydiff);
	    }
	    else{
	      return new Vector(0,0);
	    }
	  }
	}

	

}
