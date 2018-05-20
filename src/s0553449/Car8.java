package s0553449;

import lenz.htw.ai4g.ai.Info;

public class /*<n*/Car8/*>*/ extends MyCarMain{
    
	protected float brakeAngle = /*<f*/6.5f/*>*/;
	protected float maxAngleSpd = /*<f*/45f/*>*/;
	protected float approachPower = /*<f*/1.7f/*>*/;
	protected float turnVelocity = /*<f*/6.1f/*>*/;
	
	protected float targetWeight = /*<f*/1f/*>*/;
	protected float obstacleWeight = /*<f*/10f/*>*/;
	
    protected float feelerDistance = /*<f*/20f/*>*/;
    
    public /*<n*/Car8/*>*/(Info info) {
        super(info);
    }

    @Override
    public String getName() {
        return/*<n*/"Car8"/*>*/;
    }
}