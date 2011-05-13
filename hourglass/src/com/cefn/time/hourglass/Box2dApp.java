package com.cefn.time.hourglass;

import processing.core.PApplet;
import processing.core.PImage;
import processing.serial.*;

import java.io.ObjectInputStream.GetField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jbox2d.collision.AABB;
import org.jbox2d.collision.CircleDef;
import org.jbox2d.collision.MassData;
import org.jbox2d.collision.PolygonDef;
import org.jbox2d.collision.PolygonShape;
import org.jbox2d.collision.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.World;

import com.cefn.time.hourglass.StoryboardApp.Stage;

import fullscreen.FullScreen;

public class Box2dApp extends App {
	
	public static void main(String[] args){
		PApplet.main(new String[]{Box2dApp.class.getName()});
	}
	
	Serial serial = null;
	
	public int targetFps = 60;
	public float timeStep = 1f / (float) targetFps;
	public int iterations = 5;

	public static final int NUM_BALLS = 24;

	List<Body> balls = new ArrayList<Body>();
	List<Body> lines = new ArrayList<Body>();
	
	float endAngle; //angle around ellipse which defines hourglass boundary
	float rx; //radius of hourglass ellipses in x dimension
	float ry; //radius of hourglass ellipses in y dimension

	float targetCircleRadius = 0.07f; // the grain size as a share of width
	float targetWaist = 0.35f; // the hourglass waist as a share of width
	
	protected AABB boundingBox;
	protected World box2dWorld;

	protected BodyDef groundBodyDef;
	protected PolygonDef groundPolygonDef;

	protected float scale = 10f;

	float scaledWidth = WIDTH / scale;
	float scaledHeight = HEIGHT / scale;

	protected float circleRadius = scaledWidth * targetCircleRadius;

	protected List<Stage> allStages = new ArrayList<Stage>();
	protected Stage[] liveStages = new Stage[2];
	
	protected Vec2 v(float x, float y) {
		return new Vec2(x, y);
	}
	
	protected Vec2 randomV() {
		return new Vec2(random(scaledWidth), random(scaledHeight));
	}

	boolean alreadyRunSetup = false;
	
	FullScreen fullScreen;
	
	public void setup() {
		// configure processing
		if(!alreadyRunSetup){
			alreadyRunSetup = true;
			size((int) WIDTH, (int) HEIGHT, OPENGL);			
			fullScreen = new FullScreen(this);
			fullScreen.setShortcutsEnabled(true);
			fullScreen.setResolution(480, 800);
			fullScreen.enter();
		}
		frameRate(targetFps);
		
		
		//set up serial link to arduino
		serial = new Serial(this, Serial.list()[0], 9600);
		
		// work out the angle where the ellipse lines should intersect
		// coordinate origins for each ellipse are separated by scaledHeight
		// although coords of cx,cy act as an offset.
		// cy is always interpreted towards the centre of the screen
		// cx is always zero
		float theta = asin(targetWaist); // the angle subtending the waist
		endAngle = (PI * 0.5f) - theta;
		rx = scaledWidth * 0.5f;// ellipses fill screen horizontally
		ry = scaledHeight * 0.5f / cos(theta); // radius is enough that the curve fills the height
		
		// create world
		boundingBox = new AABB(v(0, 0), v(scaledWidth, scaledHeight));
		Vec2 gravity = v(0, 0);
		boolean sleep = false;
		box2dWorld = new World(boundingBox, gravity, sleep);
		setGravityAngle(PI);

		//add floor and ceiling
		addLine(0.1f,0.1f,47.9f,0.1f); //adding them at the borders makes them invisible
		addLine(0.1f,79.9f,47.9f,79.9f);

		/** Create hourglass boundary using edge polygons*/
		plotHourglass();
				
		allStages = Arrays.asList(new Stage[]{
			new Stage("egg.png","cellpop.wav"){},
			new Stage("heart.png","heartbeat-01.wav"){
				@Override
				public void draw() {
					if(attached.size() > 0){
						while(balls.size() < NUM_BALLS){
							addRandomBall();
						}						
					}
					super.draw();
				}
			},
			new Stage("baby.png","baby_cry.wav"){},
			new Stage("schoolboy.png",null){},
			new Stage("lover.png",null){},
			new Stage("soldier.png",null){},
			new Stage("judge.png",null){},
			new Stage("pantaloon.png",null){},
			new Stage("dust.png",null){},
			new Stage("dust2.png",null){},
		});	  	 
		
		for(Stage stage:allStages){
			stage.setup();
		}
		
		resetStages();

	}

	public void plotHourglass() {
	
		// draw each ellipse's lines as numLines line segments
		int numLines = 6;
		float topCx = scaledWidth * 0.5f, topCy = 0;
		float bottomCx = scaledWidth * 0.5f, bottomCy = scaledHeight;

		// top right
		pushMatrix();
		translate(topCx, topCy);
		scale(rx, ry);
		plotArc(numLines, endAngle);
		popMatrix();

		// top left
		pushMatrix();
		translate(topCx, topCy);
		scale(-rx, ry);
		plotArc(numLines, endAngle);
		popMatrix();

		// bottom right
		pushMatrix();
		translate(bottomCx, bottomCy);
		scale(rx, -ry);
		plotArc(numLines, endAngle);
		popMatrix();

		// bottom left
		pushMatrix();
		translate(bottomCx, bottomCy);
		scale(-rx, -ry);
		plotArc(numLines, endAngle);
		popMatrix();
		
	}

	public void plotArc(int numLines, float endAngle) {
		float angleStep = endAngle / ((float) numLines);
		for (int line = 0; line < numLines; line++) {
			float angle = line * angleStep;
			float fromX = cos(angle);
			float fromY = sin(angle);
			float toX = cos(angle + angleStep);
			float toY = sin(angle + angleStep);
			line(fromX,fromY,toX,toY);

			//absolutely position the polygons according to the current transform
			float fromModelX = modelX(fromX,fromY,0);
			float fromModelY = modelY(fromX,fromY,0);
			float toModelX = modelX(toX,toY,0);
			float toModelY = modelY(toX,toY,0);
			//line(fromModelX, fromModelY, toModelX,toModelY);
			addLine(fromModelX, fromModelY, toModelX,toModelY);
		}
	}

	public void addLine(float ax, float ay, float bx, float by) {
		PolygonDef linePolyDef = new PolygonDef();
		linePolyDef.addVertex(v(0, 0));
		linePolyDef.addVertex(v(bx - ax, by - ay));
		linePolyDef.friction = 0f;
		BodyDef lineBodyDef = new BodyDef();
		lineBodyDef.position = v(ax, ay);
		Body lineBody = box2dWorld.createBody(lineBodyDef); // leave zero weight to make static
		lineBody.createShape(linePolyDef);
		lines.add(lineBody);
	}

	public void addRandomBall(){
		float ellipseCentreX = scaledWidth * 0.5f;
		float ellipseCentreY = 0;
		float ballAngle = PI * (0.25f + random(0.5f));
		float multiplier = 0.1f + random(0.5f);
		addBall(ellipseCentreX + (multiplier * cos(ballAngle) * rx), ellipseCentreY + (multiplier * sin(ballAngle) * ry));
	}
	
	public void addBall(float centreX,float centreY) {
		
		// create ball body
		BodyDef ballBodyDef = new BodyDef();
		ballBodyDef.position.set(centreX,centreY);
		Body ballBody = box2dWorld.createBody(ballBodyDef);

		// attach shape to ball
		CircleDef ballCircleDef = new CircleDef();
		ballCircleDef.radius = circleRadius;
		ballCircleDef.density = 1f;
		//ballCircleDef.friction = 0f;
		ballBody.createShape(ballCircleDef);
		ballBody.setMassFromShapes();

		// record
		balls.add(ballBody);

	}
	
	public void readGravityFromSerial(){
		if(serial != null){
			if(serial.available() > 0){
				String sensorValues = serial.readStringUntil('\n');
				if(sensorValues != null){
					Pattern pattern = Pattern.compile("X([0-9]+)Y([0-9]+)Z([0-9]+)");
					Matcher matcher = pattern.matcher(sensorValues);
					if(matcher.find()){
						int x = Integer.parseInt(matcher.group(1)) - 512;
						int y = Integer.parseInt(matcher.group(2)) - 512;
						int z = Integer.parseInt(matcher.group(3)) - 512;
						System.out.println("X:" + x + " Y:" + y + " Z:" + z);
						box2dWorld.setGravity(v(-x,y));
						gravity = new Vector(-x,-y);
					}				
				}				
			}
		}
	}
	
	@Override
	public void draw() {
		
		//draw black background
		background(0); 
		
		//get the accelerometer data from arduino
		readGravityFromSerial();
			
		//run the physics simulation for one step
		box2dWorld.step(timeStep, iterations);
		
		//reassign balls to stages according to their position
		updateStages();
		
		//remap coordinates to scale to fill screen
		pushMatrix();
		translate(0,0);		
		scale(scale,scale);
		
		//draw the currently live stages 
		for(Stage liveStage:liveStages){
			liveStage.draw();
		}
		
		//draw where the edges are
		/*
		stroke(255,0,0);
		for(Body line:lines){
			Vec2 linePos = line.getPosition();
			Shape shape = line.getShapeList();
			if(shape instanceof PolygonShape){
				Vec2[] vertices = ((PolygonShape)shape).getVertices();
				for(int vIdx = 0; vIdx < (vertices.length - 1);vIdx++){
					Vec2 from = vertices[vIdx];
					Vec2 to = vertices[vIdx + 1];
					line(linePos.x + from.x, linePos.y + from.y, linePos.x + to.x,linePos.y + to.y);
				}
			}
		}
		*/
		
		//draw gravity
		/*
		float gX = scaledWidth * 0.5f;
		float gY = scaledHeight * 0.5f;
		float gScale = 10;
		strokeWeight(gScale);
		stroke(0,255,0);
		line(gX,gY,gX - gravity.x ,gY - gravity.y);
		strokeWeight(1);
		*/
		
		//reset back to normal coordinate system
		popMatrix();
				
	}
	
	public int oldestLiveStage(){
		return Math.min(liveStages[0].getIndex(), liveStages[1].getIndex());
	}

	public Stage evenLiveStage(){
		return liveStages[0].getIndex() % 2 == 0 ? liveStages[0] : liveStages[1];
	}

	public Stage oddLiveStage(){
		return liveStages[0].getIndex() % 2 == 0 ? liveStages[1] : liveStages[0];
	}

	public void resetStages(){
				
		//remove all balls from stages
		for(Stage stage:allStages){ 
			stage.attached.clear();
		}
		
		//remove all balls from world
		for(Body ball:balls){ 
			box2dWorld.destroyBody(ball);
		}
		balls.clear();

		//insert first ball
		addBall(scaledWidth * 0.5f, scaledHeight * 0.75f);
		
		//assign first two stages
		liveStages[0] = allStages.get(0);
		liveStages[1] = allStages.get(1);

	}
	
	public void updateStages(){
		for(Body ball:balls){
			if(ball.getPosition().y < scaledHeight * 0.5f){ //should be an odd stage ball
				evenLiveStage().removeBall(ball);
				oddLiveStage().addBall(ball);
			}
			else{ //should be an even stage ball
				oddLiveStage().removeBall(ball);
				evenLiveStage().addBall(ball);				
			}
		}
		if(evenLiveStage().getIndex() < oddLiveStage().getIndex()){ //even stage is older
			if(evenLiveStage().attached.size() == 0){ //even stage is empty - switch it
				int targetStageIdx = evenLiveStage().getIndex() + 2;
				if(targetStageIdx < allStages.size()){
					liveStages[0] = allStages.get(targetStageIdx);			
				}
				else{
					resetStages();
				}
			}
		}
		else{ //odd stage is older
			if(oddLiveStage().attached.size() == 0){ //odd stage is empty - switch it
				int targetStageIdx = oddLiveStage().getIndex() + 2;
				if(targetStageIdx < allStages.size()){
					liveStages[1] = allStages.get(targetStageIdx);
				}
				else{
					resetStages();
				}
			}			
		}
	}

	@Override
	public void setGravityAngle(float angle) {
		super.setGravityAngle(angle);
		if(box2dWorld != null){
			box2dWorld.setGravity(v(-this.gravity.x,-this.gravity.y));			
		}
	}
	
	public class Stage {
		public final String imageFileName;
		public final String soundFileName;
		public PImage image;
		public List<Body> attached = new ArrayList<Body>();
		public Stage(String imageFileName, String soundFileName){
			this.imageFileName = imageFileName;
			this.soundFileName = soundFileName;
		}
		public PImage getImage(){
			if(image == null){
				image = loadImage( dataPath("images/" + imageFileName));
				float localCircleRadius = targetCircleRadius * width;
				if(imageFileName=="dust.png"){
					localCircleRadius = 0.1f * localCircleRadius;
				}
				image.resize((int)localCircleRadius, (int)localCircleRadius);
			}
			return image;
		}
		public boolean addBall(Body ball){
			if(!this.attached.contains(ball)){
				this.attached.add(ball);
				if(this.attached.size() == 1 && soundFileName != null){
					//playWav( dataPath("wavs/" + soundFileName));
				}
				return true;
			}
			else{
				return false;
			}
		}
		public boolean removeBall(Body ball){
			if(this.attached.contains(ball)){
				this.attached.remove(ball);
				return true;
			}
			else{
				return false;
			}
		}
		public int getIndex(){
			return allStages.indexOf(this);
		}

		public void setup(){
			
		}
		
		public void draw(){
			//draw where the balls are
			stroke(0,0,255);
			for(Body ball:attached){
				Vec2 ballPos = ball.getPosition();
				/*
				ellipseMode(RADIUS);
				ellipse(
					ballPos.x, ballPos.y, circleRadius,circleRadius
				);
				*/
				imageMode(CENTER);
				pushMatrix();
				translate(ballPos.x,ballPos.y);
				rotate(getGravityAngle());
				pushMatrix();
				//scale(1f/4f);
				if(imageFileName.equals("dust.png")){
					image(getImage(), 0, 0, circleRadius * 0.25f, circleRadius * 0.25f);					
				}
				else if(imageFileName.equals("dust2.png")){
				}
				else{
					image(getImage(), 0, 0, circleRadius * 2, circleRadius * 2);
					//image(getImage(), 0, 0);
				}
				popMatrix();
				popMatrix();
			}
		}
	}
	
}
