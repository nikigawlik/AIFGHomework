package s0553449;

import java.awt.Point;

import org.lwjgl.LWJGLUtil;
import org.lwjgl.opengl.GL11;

import lenz.htw.ai4g.ai.AI;
import lenz.htw.ai4g.ai.DriverAction;
import lenz.htw.ai4g.ai.Info;

public class MyCarMain extends AI {

	private static float BRAKE_ANGLE = 6.14f;
	private static float MAX_ANGLE_SPD = 200f;
	private static float APPROACH_POWER = 1.5f;
	private static float BRAKE_TIME = 1f;
	
	private String debugStr = "";

	public MyCarMain(Info info) {
		super(info);
	}

	@Override
	public DriverAction update(boolean arg0) {
		debugStr = "";
		
		float throttle = info.getMaxAcceleration();
		float steering = 0f;
		
		Point ncp = info.getCurrentCheckpoint();
		float x = info.getX();
		float y = info.getY();
		float deltaX = ncp.x - x;
		float deltaY = ncp.y - y;
		
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
			throttle = info.getMaxAcceleration(); // * 0.25f;
		}

		debugStr += "deltaAngle: " + deltaAngle + "\n";
		debugStr += "targetAngleV: " + targetAngleV + "\n";
		debugStr += "current angleV: " + info.getAngularVelocity() + "\n";
		
		return new DriverAction(throttle, steering);
	}
	
	@Override
	public void doDebugStuff() {
		System.out.println(debugStr);
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
