package s0553449;

import java.awt.Point;
import java.awt.Polygon;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import lenz.htw.ai4g.ai.AI;
import lenz.htw.ai4g.ai.DriverAction;
import lenz.htw.ai4g.ai.Info;

public class MyCarMain extends AI {

	protected float brakeAngle = 6.14f;
	protected float maxAngleSpd = 50f;
	protected float approachPower = 1.62f;
	protected float turnVelocity = 6f;
	
	protected float targetWeight = 1f;
	protected float obstacleWeight = 10f;
	
	protected float feelerDistance = 20f;
	
	private String debugStr = "";
	DecimalFormat debugFormat = new DecimalFormat( "#,###,###,##0.0000" );
	
	// display specific stuff
	private float x;
	private float y; 
	private float[] debugXs;
	private float[] debugYs;
	private Vector2f[] debugPoints = new Vector2f[0];

	public MyCarMain(Info info) {
		super(info);
		init();
	}

	protected void init() {
		enlistForTournament(553449);
		calculateGraph();
	}

	private void calculateGraph() {
		// calculate outer points
		Vector2f[] nodes = findNodes();

		debugPoints = nodes;
	}

	private Vector2f[] findNodes() {
		ArrayList<Vector2f> outputNodes = new ArrayList<>();
		for (Polygon polygon : info.getTrack().getObstacles()) {
			int n = polygon.npoints;
			for(int i = 0; i < n; i++) {
				Vector2f p1 = new Vector2f(polygon.xpoints[i], polygon.ypoints[i]);
				Vector2f p2 = new Vector2f(polygon.xpoints[(i+1)%n], polygon.ypoints[(i+1)%n]);
				Vector2f p3 = new Vector2f(polygon.xpoints[(i+2)%n], polygon.ypoints[(i+2)%n]);
				
				Vector2f v1 = new Vector2f();
				Vector2f.sub(p1, p2, v1);
				Vector2f v2 = new Vector2f();
				Vector2f.sub(p3, p1, v2);
				Vector2f v2o = new Vector2f(-v2.y, v2.x);

				if(Vector2f.dot(v1, v2o) > 0) {
					outputNodes.add(p2);
				}
			}
		}

		Vector2f[] array = new Vector2f[outputNodes.size()];
		return outputNodes.toArray(array);
	}

	@Override
	public DriverAction update(boolean arg0) {
		debugStr = "";
		
		return doSteering();
	}

	private DriverAction doSteering() {
		float throttle = info.getMaxAcceleration();
		float steering = 0f;
		
		Point ncp = getCurrentCheckpoint();
		x = info.getX();
		y = info.getY();
		float deltaX = ncp.x - x;
		float deltaY = ncp.y - y;
		float distanceToTarget = (float) Math.sqrt(deltaX*deltaX + deltaY*deltaY);
		deltaX /= distanceToTarget;
		deltaY /= distanceToTarget;
		
		deltaX *= targetWeight;
		deltaY *= targetWeight;
		
		// debugStr += "pre delta: " + deltaX + ", " + deltaY + "\n";
		
		Polygon[] obstacles = info.getTrack().getObstacles();
		
		debugXs = new float[3];
		debugYs = new float[3];

		float throttlemod = 1f;
		// stupid hack
		boolean firstCollided = false;
		
		for(int i = -1; i <= 1; i++) {
			float angle = i * (float) Math.PI/4;
			float absAngle = info.getOrientation() + angle;
			float dX = (float) Math.cos(absAngle);
			float dY = (float) Math.sin(absAngle);
			float invert = (float) Math.signum(i);
			
			for(Polygon o : obstacles) {
				if(o.contains(x + dX * feelerDistance, y + dY * feelerDistance) && !(i == 1 && firstCollided)) {
					deltaX += invert * dY * obstacleWeight;
					deltaY += -invert * dX * obstacleWeight;

					throttlemod = i == 0? -1f : 0.4f;
					if (i == -1) {
						firstCollided = true;
					}
				}
			}
			
			debugXs[i+1] = x + dX * feelerDistance;
			debugYs[i+1] = y + dY * feelerDistance;
		}

		// debugStr += "post delta: " + deltaX + ", " + deltaY + "\n";
		
		float reverse = 1;
		
		float toAngle = (float) Math.atan2(reverse * deltaY, reverse * deltaX);
		float fromAngle = info.getOrientation();
		
		float deltaAngle = (toAngle - fromAngle);
		deltaAngle = (deltaAngle % 6.283f + 6.283f) % 6.283f;
		if (Math.abs(deltaAngle) > 3.1415f)
			deltaAngle -= 6.283f;
		
		// the angular velocity we want to have (linear function)
		float absDA = Math.abs(deltaAngle);
		float targetAngleV = (float) Math.pow(absDA/brakeAngle, approachPower) * maxAngleSpd;
		targetAngleV *= Math.signum(deltaAngle);
		// clamp
		targetAngleV = Math.min(Math.max(targetAngleV, -maxAngleSpd), maxAngleSpd);
		
		steering = targetAngleV - info.getAngularVelocity();
		
		if (Math.abs(deltaAngle) < 1.57) {
			// We are on target (more or less)
			throttle = info.getMaxAcceleration();			
		} else {
			// We are turning, so we drive slower
			float acc = Math.max(turnVelocity - info.getVelocity().length(), 0);
			throttle = acc;
		}

		throttle *= throttlemod;

		// debugStr += "deltaAngle: " + debugFormat.format(deltaAngle) + "\n";
		// debugStr += "targetAngleV: " + debugFormat.format(targetAngleV) + "\n";
		// debugStr += "current angleV: " + debugFormat.format(info.getAngularVelocity()) + "\n";
		// debugStr += "current vel: " + debugFormat.format(info.getVelocity().length()) + "\n";

		// debugStr += "steering chk: " + debugFormat.format(steering/info.getMaxAngularAcceleration()) + "\n";
		// debugStr += "throttle chk: " + debugFormat.format(throttle/info.getMaxAcceleration()) + "\n";
		
		return new DriverAction(throttle, steering);
	}

	protected Point getCurrentCheckpoint() {
		return info.getCurrentCheckpoint();
	}
	
	@Override
	public void doDebugStuff() {
		if(debugStr.compareTo("") != 0) {
			System.out.println(debugStr);
		}
		
		if (debugXs.length != 0) {
			for (int i = 0; i < debugXs.length; i++) {
				GL11.glColor3f(0.0f, 1.0f, 0.2f);
				GL11.glBegin(GL11.GL_LINE_STRIP);
		
				GL11.glVertex2d(x, y);
				GL11.glVertex2d(debugXs[i], debugYs[i]);
				GL11.glEnd();
			}
		}

		if(debugPoints != null) {
			GL11.glColor3f(1f, 1f, 1f);
			GL11.glBegin(GL11.GL_POINTS);
			for (Vector2f p : debugPoints) {
				GL11.glVertex2d(p.x, p.y);
			}
			GL11.glEnd();
		}
	}


	@Override
	public String getName() {
		return "Combat Wombat";
	}

	@Override
	public String getTextureResourceName() {
		return "/s0553449/car.png";
	}
}