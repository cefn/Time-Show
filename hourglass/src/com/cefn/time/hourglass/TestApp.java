package com.cefn.time.hourglass;

import processing.core.PImage;

public class TestApp extends App {

	static final int MAX_BOUNCERS = 32;
	
	Funnel funnel;
	
	public void setup() {
				
		size((int)WIDTH,(int)HEIGHT, P2D);
		
		PImage babyImg = loadImage(filePath + "baby.png");
		float imageScale = 0.5f;
				
		  funnel = new Funnel(new Vector(WIDTH/2f,0), WIDTH/2f);
	  obstacles.add(funnel);
	  //obstacles.add(new Floor(0,0.0002f,false));
	  //obstacles.add(new Floor(HEIGHT,0.0002f,true));

		for(int bouncerIdx = 0; bouncerIdx < MAX_BOUNCERS; bouncerIdx++){
			float deviation = PI * 0.5f;
			bouncers.add(
				new Bouncer(
					babyImg, 
					funnel.centre.plus(Vector.vectorWithAngle( - deviation + random(deviation * 2.0f)).times(random(funnel.radius))), 
					babyImg.width * imageScale, 
					babyImg.height * imageScale
				)
			);
		}

		
	}

	public void draw() {
		
		pushMatrix();
		
		translate(WIDTH * 0.5f, HEIGHT * 0.5f);
		
		scale(0.2f);
			
		background(0);

		for(Bouncer currentBouncer:bouncers){
			currentBouncer.acc = gravity;
			for(Bouncer otherBouncer:bouncers){
				if(! (otherBouncer==currentBouncer)){
					currentBouncer.acc = currentBouncer.acc.plus(otherBouncer.calculateRepulsion(currentBouncer));					
				}
			}
		    for(Obstacle obstacle:obstacles){
		    	currentBouncer.acc = currentBouncer.acc.plus(obstacle.calculateRepulsion(currentBouncer));
		    }
		}
				
		for(Bouncer currentBouncer:bouncers){
			currentBouncer.update(this);
		}

		for(Bouncer currentBouncer:bouncers){
			currentBouncer.draw(this);
		}
		
		popMatrix();
		
	}

	
}
