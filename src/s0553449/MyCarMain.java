package s0553449;

import java.awt.Point;
import java.awt.Polygon;
import java.text.DecimalFormat;

import org.lwjgl.opengl.GL11;

import lenz.htw.ai4g.ai.AI;
import lenz.htw.ai4g.ai.DriverAction;
import lenz.htw.ai4g.ai.Info;

public class MyCarMain extends AI {

	private static float BRAKE_ANGLE = 6.14f;
	private static float MAX_ANGLE_SPD = 50f;
	private static float APPROACH_POWER = 1.62f;
	private static float TURN_VELOCITY = 6f;
	
	private static float TARGET_WEIGHT = 1f;
	private static float OBSTACLE_WEIGHT = 10f;
	
	private float feelerDistance = 20f;
	
	private String debugStr = "";
	DecimalFormat debugFormat = new DecimalFormat( "#,###,###,##0.0000" );
	
	// display specific stuff
	private float x;
	private float y; 
	private float[] debugXs;
	private float[] debugYs;

	public MyCarMain(Info info) {
		super(info);
	}

	@Override
	public DriverAction update(boolean arg0) {
		debugStr = "";
		
		float throttle = info.getMaxAcceleration();
		float steering = 0f;
		
		Point ncp = info.getCurrentCheckpoint();
		x = info.getX();
		y = info.getY();
		float deltaX = ncp.x - x;
		float deltaY = ncp.y - y;
		float distanceToTarget = (float) Math.sqrt(deltaX*deltaX + deltaY*deltaY);
		deltaX /= distanceToTarget;
		deltaY /= distanceToTarget;
		
		deltaX *= TARGET_WEIGHT;
		deltaY *= TARGET_WEIGHT;
		
		debugStr += "pre delta: " + deltaX + ", " + deltaY + "\n";
		
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
					deltaX += invert * dY * OBSTACLE_WEIGHT;
					deltaY += -invert * dX * OBSTACLE_WEIGHT;

					throttlemod = i == 0? -1f : 0.4f;
					if (i == -1) {
						firstCollided = true;
					}
				}
			}
			
			debugXs[i+1] = x + dX * feelerDistance;
			debugYs[i+1] = y + dY * feelerDistance;
		}

		debugStr += "post delta: " + deltaX + ", " + deltaY + "\n";
		
		float reverse = 1;
		
		float toAngle = (float) Math.atan2(reverse * deltaY, reverse * deltaX);
		float fromAngle = info.getOrientation();
		
		float deltaAngle = (toAngle - fromAngle);
		deltaAngle = (deltaAngle % 6.283f + 6.283f) % 6.283f;
		if (Math.abs(deltaAngle) > 3.1415f)
			deltaAngle -= 6.283f;
		
		// the angular velocity we want to have (linear function)
		float absDA = Math.abs(deltaAngle);
		float targetAngleV = (float) Math.pow(absDA/BRAKE_ANGLE, APPROACH_POWER) * MAX_ANGLE_SPD;
		targetAngleV *= Math.signum(deltaAngle);
		// clamp
		targetAngleV = Math.min(Math.max(targetAngleV, -MAX_ANGLE_SPD), MAX_ANGLE_SPD);
		
		steering = targetAngleV - info.getAngularVelocity();
		
		if (Math.abs(deltaAngle) < 1.57) {
			// We are on target (more or less)
			throttle = info.getMaxAcceleration();			
		} else {
			// We are turning, so we drive slower
			float acc = Math.max(TURN_VELOCITY - info.getVelocity().length(), 0);
			throttle = acc;
		}

		throttle *= throttlemod;

		debugStr += "deltaAngle: " + debugFormat.format(deltaAngle) + "\n";
		debugStr += "targetAngleV: " + debugFormat.format(targetAngleV) + "\n";
		debugStr += "current angleV: " + debugFormat.format(info.getAngularVelocity()) + "\n";
		debugStr += "current vel: " + debugFormat.format(info.getVelocity().length()) + "\n";

		debugStr += "steering chk: " + debugFormat.format(steering/info.getMaxAngularAcceleration()) + "\n";
		debugStr += "throttle chk: " + debugFormat.format(throttle/info.getMaxAcceleration()) + "\n";
		
		return new DriverAction(throttle, steering);
	}

	protected Point getCurrentCheckpoint() {
		return info.getCurrentCheckpoint();
	}
	
	@Override
	public void doDebugStuff() {
		System.out.println(debugStr);
		
		if (debugXs.length != 0) {
			for (int i = 0; i < debugXs.length; i++) {
				GL11.glColor3f(0.0f, 1.0f, 0.2f);
				GL11.glBegin(GL11.GL_LINE_STRIP);
		
				GL11.glVertex2d(x, y);
				GL11.glVertex2d(debugXs[i], debugYs[i]);
				GL11.glEnd();
			}
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
	
	@Override
	public boolean isEnabledForRacing() {
		return true;
	}
}
