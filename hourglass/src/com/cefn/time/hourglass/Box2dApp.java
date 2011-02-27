package com.cefn.time.hourglass;

import processing.serial.*;

import cc.arduino.*;

import java.util.ArrayList;
import java.util.List;
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

public class Box2dApp extends App {

	
	Serial serial;
	
	public int targetFps = 30;
	public float timeStep = 1f / (float) targetFps;
	public int iterations = 5;

	public static final int NUM_BALLS = 24;

	List<Body> balls = new ArrayList<Body>();
	List<Body> lines = new ArrayList<Body>();
	
	float endAngle; //angle around ellipse which defines hourglass boundary
	float rx; //radius of hourglass ellipses in x dimension
	float ry; //radius of hourglass ellipses in y dimension

	float targetWaist = 0.3f; // the hourglass waist as a share of width
	
	protected AABB boundingBox;
	protected World box2dWorld;

	protected BodyDef groundBodyDef;
	protected PolygonDef groundPolygonDef;

	protected float scale = 10f;
	protected float circleRadius = 2f;

	float scaledWidth = WIDTH / scale;
	float scaledHeight = HEIGHT / scale;

	protected Vec2 v(float x, float y) {
		return new Vec2(x, y);
	}

	protected Vec2 randomV() {
		return new Vec2(random(scaledWidth), random(scaledHeight));
	}

	public void setup() {
		// configure processing
		size((int) WIDTH, (int) HEIGHT, P3D);
		frameRate(targetFps);
		
		serial = new Serial(this, Serial.list()[0], 9600);
		
		// work out the angle where the ellipse lines should intersect
		// coordinate origins for each ellipse are separated by scaledHeight
		// although coords of cx,cy act as an offset.
		// cy is always interpreted towards the centre of the screen
		// cx is always zero
		float theta = asin(targetWaist); // the angle subtending the waist
		endAngle = (PI * 0.5f) - theta;
		rx = scaledWidth * 0.5f;// ellipses fill screen horizontally
		ry = scaledHeight * 0.5f / cos(theta); // radius is enough that
														// the curve fills the
														// height
		
		// create world
		boundingBox = new AABB(v(0, 0), v(scaledWidth, scaledHeight));
		Vec2 gravity = v(0, -10);
		boolean sleep = false;
		box2dWorld = new World(boundingBox, gravity, sleep);

		//add floor and ceiling
		addLine(0.1f,0.1f,29.9f,0.1f);
		addLine(0.1f,59.9f,29.9f,59.9f);

		for (int ballCount = 0; ballCount < NUM_BALLS; ballCount++) {
			addBall();
		}

		/** Draw hourglass shape */
		plotHourglass();

	}

	public void plotHourglass() {
	
		// draw each ellipse's lines as numLines line segments
		int numLines = 8;
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
		//linePolyDef.addVertex(v(0, 0)); //TODO CH how do you close a polygon
		linePolyDef.friction = 0f;
		BodyDef lineBodyDef = new BodyDef();
		lineBodyDef.position = v(ax, ay);
		Body lineBody = box2dWorld.createBody(lineBodyDef); // leave zero weight to make static
		lineBody.createShape(linePolyDef);
		lines.add(lineBody);
	}

	public void addBall() {
		
		float ellipseCentreX = scaledWidth * 0.5f;
		float ellipseCentreY = scaledHeight ;

		// create ball body
		BodyDef ballBodyDef = new BodyDef();
		float ballAngle = PI * (0.25f + random(0.5f));
		float multiplier = 0.1f + random(0.9f);
		ballBodyDef.position.set(ellipseCentreX + (multiplier * cos(ballAngle) * rx), ellipseCentreY - (multiplier * sin(ballAngle) * ry));
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

	@Override
	public void draw() {
		background(0);	

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
				}				
			}
		}
			
		//run the physic simulation for one step
		box2dWorld.step(timeStep, iterations);
		
		//draw where the balls are
		pushMatrix();
	
		translate(0,HEIGHT);
		
		scale(scale,-scale);		

		//draw where the lines are
		//stroke(255,0,0);
		//plotHourglass();
		
		stroke(0,0,255);

		for(Body ball:balls){
			Vec2 ballPos = ball.getPosition();
			ellipseMode(RADIUS);
			ellipse(
				ballPos.x, ballPos.y, circleRadius,circleRadius
			);
		}

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
		
		//draw gravity
		float gX = scaledWidth * 0.5f;
		float gY = scaledHeight * 0.5f;
		float gScale = 10;
		strokeWeight(gScale);
		stroke(0,255,0);
		line(gX,gY,gX - gravity.x ,gY - gravity.y);
		strokeWeight(1);
		
		popMatrix();
		
	}

	@Override
	public void setGravityAngle(float angle) {
		super.setGravityAngle(angle);
		if(box2dWorld != null){
			box2dWorld.setGravity(v(-this.gravity.x,-this.gravity.y));			
		}
	}
	
}
