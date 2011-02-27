/**
 * 
 */
package com.cefn.time.hourglass;

import processing.core.PApplet;
import processing.core.PImage;

class Bouncer implements Obstacle{

  PImage pic;
  Vector pos = new Vector(0,0), 
         vel = new Vector(0,0), 
         acc = new Vector(0,0);
  float noise = 0;
  long lastnow = -1;
  float width,height;
  
  Bouncer(PImage pic, Vector pos, float width, float height){
    this.pic = pic;
    this.pos = pos;
    this.width = width;
    this.height = height;
  }
  
  void update(PApplet applet){

	//work out step length
	long now = applet.millis();
    float timediff = lastnow != -1 ? (float)(now - lastnow) : 0;
    timediff = timediff * 0.001f; //turn into seconds
    lastnow = now;
    
    //introduce some wiggle
    if(noise != 0){
    	acc = acc.plus(Vector.vectorWithAngle(applet.random(2 * App.PI)).times(noise));
    }
    
    //impose limit on magnitude of acceleration
    float maxacc = App.G * 4f;
    if(acc.hypotenuse()>maxacc){
      acc = acc.unitVector().times(maxacc);
    }

    //propagate location and speed from acceleration
    pos = pos.plus(vel.times(timediff));
    vel = vel.plus(acc.times(timediff));
    vel = vel.times(0.999f);
  }

  void draw(PApplet applet){
    applet.image(this.pic,this.pos.x - (width/2),this.pos.y - (height/2), width, height);      
  }

  public Vector calculateRepulsion(Bouncer other){
	Vector diff = other.pos.minus(this.pos);
	float radius = (width + height) * 0.5f;
    float spacing = diff.hypotenuse() / radius; //normalise for size
    if(spacing < 1.0f){
        Vector repulsion = diff.unitVector().times(App.G/App.sqrt(spacing));
        return repulsion;    	
    }
    else{
    	return App.ZERO;
    }
  }

}