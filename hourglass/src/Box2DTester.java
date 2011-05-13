import org.jbox2d.collision.AABB;
import org.jbox2d.collision.CircleDef;
import org.jbox2d.collision.PolygonDef;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.World;

public class Box2DTester {
    public int targetFPS = 40;
    public int timeStep = (1000 / targetFPS);
    public int iterations = 5;

    private Body[] bodies = new Body[100]; 
    private int count = 0;
    
    private AABB worldAABB;
    private World world;
    private BodyDef groundBodyDef;
    private PolygonDef groundShapeDef;
    
    
    public static void main(String... args){
    	Box2DTester tester = new Box2DTester();
    	tester.create();
    	for(int i = 0; i < 100; i++){
    		tester.addBall();
    	}
    	while(true){
        	tester.update();    		
    	}
    }

    public void create() {
        // Step 1: Create Physics World Boundaries
        worldAABB = new AABB();
        worldAABB.lowerBound.set(new Vec2((float) -100.0, (float) -100.0));
        worldAABB.upperBound.set(new Vec2((float) 100.0, (float) 100.0));
        
        // Step 2: Create Physics World with Gravity
        Vec2 gravity = new Vec2((float) 0.0, (float) -10.0);
        boolean doSleep = true;
        world = new World(worldAABB, gravity, doSleep);
        
        // Step 3: Create Ground Box
        groundBodyDef = new BodyDef();
        groundBodyDef.position.set(new Vec2((float) 0.0, (float) -10.0));
        Body groundBody = world.createBody(groundBodyDef);
        groundShapeDef = new PolygonDef();
        groundShapeDef.setAsBox((float) 50.0, (float) 10.0);
        groundBody.createShape(groundShapeDef);
    }

    public void addBall() {
        // Create Dynamic Body
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set((float) 6.0+count, (float) 24.0);
        bodies[count] = world.createBody(bodyDef);
        
        // Create Shape with Properties
        CircleDef circle = new CircleDef();
        circle.radius = (float) 1.8;
        circle.density = (float) 1.0;
        
        // Assign shape to Body
        bodies[count].createShape(circle);
        bodies[count].setMassFromShapes();

        // Increase Counter
        count += 1;        
    }
    
    public void update() {
        // Update Physics World
        world.step(timeStep, iterations);
        
        // Print info of latest body
        if (count > 0) {
            Vec2 position = bodies[count - 1].getPosition();
            float angle = bodies[count - 1].getAngle();
            System.out.println("Physics Test" + "Pos: (" + position.x + ", " + position.y + "), Angle: " + angle);
        }
    }    
}