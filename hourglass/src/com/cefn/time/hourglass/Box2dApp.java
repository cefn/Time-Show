package com.cefn.time.hourglass;

import processing.core.PApplet;
import processing.core.PImage;
import processing.serial.*;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jbox2d.collision.AABB;
import org.jbox2d.collision.CircleDef;
import org.jbox2d.collision.PolygonDef;
import org.jbox2d.collision.PolygonShape;
import org.jbox2d.collision.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.World;

import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

import ddf.minim.AudioSnippet;
import ddf.minim.Minim;

import fullscreen.FullScreen;

//TODO check there isn't a memory leak or media issue with reloading stages
//unnecessarily
public class Box2dApp extends PApplet {

	public static void main(String[] args) {
		PApplet.main(new String[] { Box2dApp.class.getName() });
	}

	float WIDTH_PIX = 480;
	float HEIGHT_PIX = 800;

	public static final float G = 100f;
	Vec2 gravity;

	Serial serial = null;
	FullScreen fullScreen = null;
	Minim minim = null;
	AudioSnippet attentionSnippet = null;

	float small = 0.001f, large = 1.0f - small;

	public int targetFps = 60;
	public float timeStep = 1f / (float) targetFps;
	public int iterations = 5; // was 5

	public long lastStageChange = millis();
	public static final long FORCE_RESET_PERIOD = 60000;

	public long lastAttentionGrab = millis();
	public static final long ATTENTION_GRAB_PERIOD = 180000;

	public float MOTION_ANGLE = PI * 0.01f;

	public static final int MAX_BALLS = 24;

	List<Body> balls = new ArrayList<Body>();
	List<Body> lines = new ArrayList<Body>();

	float endAngle; // angle around ellipse which defines hourglass boundary
	float rx; // radius of hourglass ellipses in x dimension
	float ry; // radius of hourglass ellipses in y dimension

	float lastGravityAngle = 0;

	float targetCircleRadius = 0.07f; // the grain size as a share of width
	float targetWaist = 0.35f; // the hourglass waist as a share of width

	float actualWidth = 10f; // approximate width of real screen in m

	float averageAccelerometerNormal = 0.1f; // the normal pythag value which
												// comes from accelerometer

	protected AABB boundingBox;
	protected World box2dWorld;

	protected float zoom = ((float) WIDTH_PIX) / actualWidth; // multiply box2d
																// units by
																// 'zoom' value
																// to get pixel
																// units

	float modelWidth = actualWidth;
	float modelHeight = actualWidth * ((float) HEIGHT_PIX)
			/ ((float) WIDTH_PIX);

	protected float circleRadius = modelWidth * targetCircleRadius;

	protected List<Stage> allStages = new ArrayList<Stage>();
	protected Stage[] liveStages = new Stage[2];

	protected Vec2 v(float x, float y) {
		return new Vec2(x, y);
	}

	protected Vec2 randomV() {
		return new Vec2(random(modelWidth), random(modelHeight));
	}

	public void setGravity(float xG, float yG) {
		setGravity(v(xG, yG));
	}

	public void setGravity(Vec2 gravity) {
		this.gravity = gravity;
		this.box2dWorld.setGravity(gravity);
		// System.out.println("Gravity set to : " + gravity.x + "," +
		// gravity.y);
	}

	public float getGravityAngle() {
		return (float) Math.atan2((double) -gravity.x, (double) gravity.y);
	}

	public void keyPressed(KeyEvent e) {
		// System.out.println("Received key event " + e);
		if (e.getKeyCode() == LEFT) {
			// System.out.println("Turning left ");
			// setGravityAngle(gravityAngle += PI / 16);
		}
		if (e.getKeyCode() == RIGHT) {
			// System.out.println("Turning right ");
			// setGravityAngle(gravityAngle -= PI / 16);
		}
	}

	public static float signum(float f) {
		if (f > 0)
			return 1f;
		if (f < 0)
			return -1f;
		return 0;
	}

	public void setup() {
		// configure processing
		size(screenWidth, screenHeight, P2D);

		// note rotated when drawn
		int screenWidth = (int) HEIGHT_PIX; // width=>height
		int screenHeight = (int) WIDTH_PIX; // height=>width

		// configure applet
		// size(screenWidth, screenHeight, P2D);

		// configure fullscreen
		fullScreen = new FullScreen(this);
		fullScreen.setShortcutsEnabled(true);
		fullScreen.setResolution(screenWidth, screenHeight);
		fullScreen.enter();

		// set up minim library for sound
		minim = new Minim(this);

		attentionSnippet = minim.loadSnippet("data/sounds/00_rumpsong.mp3");

		// set up serial link to arduino
		serial = new Serial(this, Serial.list()[0], 9600);

		// set up robot to use to put mouse out of the way
		try{
			Robot robot = new Robot();			
			robot.mouseMove(800, 240);
		}
		catch(AWTException awte){
			//ignore
		}

		// establish target frame rate
		frameRate(targetFps);

		// work out the angle where the ellipse lines should intersect
		// coordinate origins for each ellipse are separated by scaledHeight
		// although coords of cx,cy act as an offset.
		// cy is always interpreted towards the centre of the screen
		// cx is always zero
		float theta = asin(targetWaist); // the angle subtending the waist
		endAngle = (PI * 0.5f) - theta;
		rx = modelWidth * 0.5f;// ellipses fill screen horizontally
		ry = modelHeight * 0.5f / cos(theta); // radius is enough that the curve
												// fills the height

		// create world
		boundingBox = new AABB(v(0, 0), v(modelWidth, modelHeight));
		Vec2 gravity = v(0, 0);
		boolean sleep = false;
		box2dWorld = new World(boundingBox, gravity, sleep);
		setGravity(0, -G);

		// add floor and ceiling
		addLine(modelWidth * small, modelHeight * small, modelWidth * large,
				modelHeight * small); // adding them at the borders makes them
										// invisible
		addLine(modelWidth * small, modelHeight * large, modelWidth * large,
				modelHeight * large);

		/** Create hourglass boundary using edge polygons */
		plotHourglass();

		final Vec2 eggPos = v(modelWidth * 0.5f, modelHeight * 0.25f);
		final PImage eggImage = loadImage(dataPath("images/egg.png"));

		allStages = Arrays.asList(new Stage[] {
				new Stage("sperm.png", "01_alltheworld.mp3") {
					public void setup() {
						followGravity = false;
					}

					@Override
					public void draw() {
						super.draw();
						drawBall(eggPos, eggImage);
					}
				}, new Stage("sperm.png", "02_exits.mp3") {
					boolean dividing = false;

					public void setup() {
						followGravity = false;
					}

					@Override
					public void draw() {
						if (attached.size() > 0) {
							if (attached.size() == 1) {
								// check if sperm has crossed the quartile
								Body triggerBall = attached.get(0);
								Vec2 pos = triggerBall.getPosition();
								if (pos.y < eggPos.y) {
									// change to heart and start dividing
									setImageFileName("heart.png");
									followGravity = true;
									dividing = true;
								}
							}

							if (dividing) {
								// if dividing, add balls through cell division
								if (frameCount % 8 == 0) {
									for (Body ball : attached) {
										Vec2 prevPos = ball.getPosition();
										if (balls.size() < MAX_BALLS) {
											float angle = random(PI * 2.0f);
											Box2dApp.this
													.addBall(
															prevPos.x
																	+ (sin(angle) * circleRadius),
															prevPos.y
																	+ (cos(angle) * circleRadius));
										}
									}
								}
							} else {
								// if not dividing, just draw the egg
								drawBall(eggPos, eggImage);
							}

						}
						super.draw();
					}
				}, new Stage("baby.png", "03_baby.mp3") {
				}, new Stage("schoolboy.png", "04_schoolboy.mp3") {
				}, new Stage("lover.png", "05_teenlover.mp3") {
				}, new Stage("soldier.png", "06_soldier.mp3") {
				}, new Stage("judge.png", "07_judge.mp3") {
				}, new Stage("pantaloon.png", "08_pensioner.mp3") {
				}, new Stage("oblivion.png", "09_elderly.mp3") {
				}, new Stage("dust.png", "10_sanseverything.mp3") {
				}, new Stage("dust.png", null) {
				}, });

		for (Stage stage : allStages) {
			stage.setup();
		}

		resetStages();

	}

	/** Create the hourglass as a series of single, partial arcs . */
	public void plotHourglass() {

		// draw each ellipse's lines as numLines line segments
		int numLines = 6;
		float topCx = modelWidth * 0.5f, topCy = 0;
		float bottomCx = modelWidth * 0.5f, bottomCy = modelHeight;

		// top right
		plotArc(topCx, topCy, rx, ry, numLines, endAngle);
		// top left
		plotArc(topCx, topCy, -rx, ry, numLines, endAngle);
		// bottom right
		plotArc(bottomCx, bottomCy, rx, -ry, numLines, endAngle);
		// bottom left
		plotArc(bottomCx, bottomCy, -rx, -ry, numLines, endAngle);

	}

	/**
	 * Plot a single multi-segment arc according to a unit circle, calculating
	 * the inverse transform (given current processing scaling)
	 */
	public void plotArc(float xOffset, float yOffset, float xR, float yR,
			int numLines, float endAngle) {
		float angleStep = endAngle / ((float) numLines);

		for (int line = 0; line < numLines; line++) {
			float angle = line * angleStep;
			float fromX = xOffset + (cos(angle) * xR);
			float fromY = yOffset + (sin(angle) * yR);
			float toX = xOffset + (cos(angle + angleStep) * xR);
			float toY = yOffset + (sin(angle + angleStep) * yR);

			// draw the line
			// line(fromX,fromY,toX,toY);

			// create the sections according to the current transform
			addLine(fromX, fromY, toX, toY);
		}
	}

	public void addLine(float ax, float ay, float bx, float by) {
		PolygonDef linePolyDef = new PolygonDef();
		linePolyDef.addVertex(v(0, 0));
		linePolyDef.addVertex(v(bx - ax, by - ay));
		linePolyDef.friction = 0f;
		BodyDef lineBodyDef = new BodyDef();
		lineBodyDef.position = v(ax, ay);
		Body lineBody = box2dWorld.createBody(lineBodyDef); // leave zero weight
															// to make static
		lineBody.createShape(linePolyDef);
		lines.add(lineBody);
	}

	public void addRandomBall() {
		float ellipseCentreX = modelWidth * 0.5f;
		float ellipseCentreY = 0;
		float ballAngle = PI * (0.25f + random(0.5f));
		float multiplier = 0.1f + random(0.5f);
		addBall(ellipseCentreX + (multiplier * cos(ballAngle) * rx),
				ellipseCentreY + (multiplier * sin(ballAngle) * ry));
	}

	public void addBall(float centreX, float centreY) {

		// create ball body
		BodyDef ballBodyDef = new BodyDef();
		ballBodyDef.position.set(centreX, centreY);
		Body ballBody = box2dWorld.createBody(ballBodyDef);

		// attach shape to ball
		CircleDef ballCircleDef = new CircleDef();
		ballCircleDef.radius = circleRadius;
		ballCircleDef.density = 1f;
		// ballCircleDef.friction = 0f;
		ballBody.createShape(ballCircleDef);
		ballBody.setMassFromShapes();

		// record
		balls.add(ballBody);

	}

	public void readGravityFromSerial() {
		if (serial != null) {
			if (serial.available() > 0) {
				String sensorValues = serial.readStringUntil('\n');
				if (sensorValues != null) {
					Pattern pattern = Pattern
							.compile("X([0-9]+)Y([0-9]+)Z([0-9]+)");
					Matcher matcher = pattern.matcher(sensorValues);
					if (matcher.find()) {
						// read analog values and centre them
						float x = (float) Integer.parseInt(matcher.group(1));
						float y = (float) Integer.parseInt(matcher.group(2));
						float z = (float) Integer.parseInt(matcher.group(3));
						// centre the values
						x -= 512f;
						y -= 512f;
						z -= 512f;
						// normalise against unit range and average
						// accelerometer value for G
						x /= 1024f * averageAccelerometerNormal;
						y /= 1024f * averageAccelerometerNormal;
						z /= 1024f * averageAccelerometerNormal;

						// System.out.println("X:" + x + " Y:" + y + " Z:" + z);

						setGravity(G * x, G * -y);

					}
				}
			}
		}
	}

	@Override
	public void draw() {

		// draw black background
		background(0);

		// get the accelerometer data from arduino
		readGravityFromSerial();

		// run the physics simulation for one step
		box2dWorld.step(timeStep, iterations);

		// handle error where balls step outside boundaries by removing them
		Iterator<Body> ballIterator = balls.iterator();
		while (ballIterator.hasNext()) {
			Body ball = ballIterator.next();
			Vec2 ballPos = ball.getPosition();
			Vec2 validPos = v(constrain(ballPos.x, 0, modelWidth), constrain(
					ballPos.y, 0, modelHeight));
			if (ballPos.x != validPos.x || ballPos.y != validPos.y) {
				System.out.println("Ball has fallen outside boundaries");
				// drop it from global ball list
				ballIterator.remove();
				// drop it from both live stages
				evenLiveStage().removeBall(ball);
				oddLiveStage().removeBall(ball);
				// remove from 2d world
				box2dWorld.destroyBody(ball);
				continue;
			}
		}

		// reassign balls to stages according to their position
		updateStages();

		// remap coordinates to scale to fill screen
		pushMatrix();

		// map model values to pixel values
		scale(zoom, zoom);

		// rotate model to right and slide across so it fits in screen
		rotate(PI * 0.5f);
		translate(0, -modelHeight);

		// draw the currently live stages
		for (Stage liveStage : liveStages) {
			liveStage.draw();
		}

		// draw where the edges are
		/*
		stroke(255, 0, 0);
		for (Body line : lines) {
			Vec2 linePos = line.getPosition();
			Shape shape = line.getShapeList();
			if (shape instanceof PolygonShape) {
				Vec2[] vertices = ((PolygonShape) shape).getVertices();
				for (int vIdx = 0; vIdx < (vertices.length - 1); vIdx++) {
					Vec2 from = vertices[vIdx];
					Vec2 to = vertices[vIdx + 1];
					line(linePos.x + from.x, linePos.y + from.y, linePos.x
							+ to.x, linePos.y + to.y);
				}
			}
		}
		*/

		// draw gravity
		/*
		 * float gX = scaledWidth * 0.5f; float gY = scaledHeight * 0.5f; float
		 * gScale = 10; strokeWeight(gScale); stroke(0,255,0); line(gX,gY,gX -
		 * gravity.x ,gY - gravity.y); strokeWeight(1);
		 */

		// reset back to normal coordinate system
		popMatrix();

		if (millis() - lastStageChange > FORCE_RESET_PERIOD) {
			if (liveStages[0].getIndex() != 0) {
				resetStages();
			}
			if (abs(getGravityAngle() - lastGravityAngle) > MOTION_ANGLE) {
				resetStages();
			}
			if (millis() - lastAttentionGrab > ATTENTION_GRAB_PERIOD) {
				attentionSnippet.rewind();
				attentionSnippet.play();
				lastAttentionGrab = millis();
			}
		}
		lastGravityAngle = getGravityAngle();

	}

	public int oldestLiveStage() {
		return Math.min(liveStages[0].getIndex(), liveStages[1].getIndex());
	}

	public Stage evenLiveStage() {
		return liveStages[0].getIndex() % 2 == 0 ? liveStages[0]
				: liveStages[1];
	}

	public Stage oddLiveStage() {
		return liveStages[0].getIndex() % 2 == 0 ? liveStages[1]
				: liveStages[0];
	}

	public void resetStages() {
		lastStageChange = millis();

		// remove all balls from stages
		for (Stage stage : allStages) {
			stage.attached.clear();
		}

		// remove all balls from world
		for (Body ball : balls) {
			box2dWorld.destroyBody(ball);
		}
		balls.clear();

		// insert first ball
		addBall(modelWidth * 0.5f, modelHeight * 0.75f);

		// assign first two stages
		liveStages[0] = allStages.get(0);
		liveStages[1] = allStages.get(1);

	}

	public void updateStages() {
		for (Body ball : balls) {
			if (ball.getPosition().y < modelHeight * 0.5f) { // should be an odd
																// stage ball
				evenLiveStage().removeBall(ball);
				oddLiveStage().addBall(ball);
			} else { // should be an even stage ball
				oddLiveStage().removeBall(ball);
				evenLiveStage().addBall(ball);
			}
		}
		if (evenLiveStage().getIndex() < oddLiveStage().getIndex()) { // even
																		// stage
																		// is
																		// older
			if (evenLiveStage().attached.size() == 0) { // even stage is empty -
														// switch it
				lastStageChange = millis();
				int targetStageIdx = evenLiveStage().getIndex() + 2;
				if (targetStageIdx < allStages.size()) {
					liveStages[0] = allStages.get(targetStageIdx);
				} else {
					resetStages();
				}
			}
		} else { // odd stage is older
			if (oddLiveStage().attached.size() == 0) { // odd stage is empty -
														// switch it
				lastStageChange = millis();
				int targetStageIdx = oddLiveStage().getIndex() + 2;
				if (targetStageIdx < allStages.size()) {
					liveStages[1] = allStages.get(targetStageIdx);
				} else {
					resetStages();
				}
			}
		}
	}

	public class Stage {
		public boolean followGravity = true;
		public String imageFileName;
		public final AudioSnippet audioSnippet;
		public PImage image;
		public List<Body> attached = new ArrayList<Body>();

		public Stage(String imageFileName, String soundFileName) {
			setImageFileName(imageFileName);
			if (soundFileName != null) {
				this.audioSnippet = minim.loadSnippet("data/sounds/"
						+ soundFileName);
			} else {
				this.audioSnippet = null;
			}
		}

		public void setImageFileName(String imageFileName) {
			this.imageFileName = imageFileName;
			this.image = null;
		}

		public PImage getImage() {
			if (image == null) {
				image = loadImage(dataPath("images/" + imageFileName));
			}
			return image;
		}

		public boolean addBall(Body ball) {
			if (!this.attached.contains(ball)) {
				this.attached.add(ball);
				if (this.attached.size() == 1 && audioSnippet != null) {
					audioSnippet.rewind();
					audioSnippet.play();
				}
				return true;
			} else {
				return false;
			}
		}

		public boolean removeBall(Body ball) {
			if (this.attached.contains(ball)) {
				this.attached.remove(ball);
				return true;
			} else {
				return false;
			}
		}

		public int getIndex() {
			return allStages.indexOf(this);
		}

		public void setup() {

		}

		public void draw() {
			// draw where the balls are
			stroke(0, 0, 255);
			for (Body ball : attached) {
				Vec2 ballPos = ball.getPosition();
				drawBall(ballPos, getImage());
			}
		}

		public void drawBall(Vec2 position, PImage image) {
			imageMode(CENTER);
			pushMatrix();
			translate(position.x, position.y);
			if (followGravity) {
				rotate(getGravityAngle());
			}
			image(image, 0, 0, image.width * circleRadius * 2.0f / 128f,
					image.height * circleRadius * 2.0f / 128f);
			popMatrix();
		}

	}

}
