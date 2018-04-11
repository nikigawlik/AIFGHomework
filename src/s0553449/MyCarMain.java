package s0553449;

import java.awt.Point;

import org.lwjgl.LWJGLUtil;

import lenz.htw.ai4g.ai.AI;
import lenz.htw.ai4g.ai.DriverAction;
import lenz.htw.ai4g.ai.Info;

public class MyCarMain extends AI {

	private static float BRAKE_ANGLE = 0.3f;
	private static float BRAKE_TIME = 1f;

	public MyCarMain(Info info) {
		super(info);
	}

	@Override
	public String getName() {
		return "Combat Wombat";
	}

	@Override
	public DriverAction update(boolean arg0) {
		float throttle = info.getMaxAcceleration();
		float steering = 0f;
		
		Point ncp = info.getCurrentCheckpoint();
		float x = info.getX();
		float y = info.getY();
		float deltaX = ncp.x - x;
		float deltaY = ncp.y - y;
		
		float reverse = -1;
		
		float toAngle = (float) Math.atan2(reverse * deltaY, reverse * deltaX);
		float fromAngle = info.getOrientation();
		
		float deltaAngle = (toAngle - fromAngle);
		deltaAngle = (deltaAngle % 6.283f + 6.283f) % 6.283f;
		if (Math.abs(deltaAngle) > 3.1415f)
			deltaAngle -= 6.283f;
		
		if(Math.abs(deltaAngle) < BRAKE_ANGLE) {
			steering = -info.getAngularVelocity() / BRAKE_TIME;
		} else {
			steering = deltaAngle;
		}
		
		throttle = reverse * (float) Math.sqrt(deltaX*deltaX + deltaY*deltaY) / 20;
		
		return new DriverAction(throttle, steering);
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
