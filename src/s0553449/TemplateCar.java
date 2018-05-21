package s0553449;

import lenz.htw.ai4g.ai.DriverAction;
import lenz.htw.ai4g.ai.Info;

public class /*<n*/TemplateCar/*>*/ extends MyCarMain{
    
    public /*<n*/TemplateCar/*>*/(Info info) {
        super(info);
        brakeAngle = /*<f*/6.5f/*>*/;
        maxAngleSpd = /*<f*/45f/*>*/;
        approachPower = /*<f*/1.7f/*>*/;
        turnVelocity = /*<f*/6.1f/*>*/;
        targetWeight = /*<f*/1f/*>*/;
        obstacleWeight = /*<f*/10f/*>*/;
        feelerDistance = /*<f*/20f/*>*/;
    }

    @Override
    public String getName() {
        return /*<n*/"TemplateCar"/*>*/;
    }

    @Override
    public DriverAction update(boolean arg0) {
        return super.update(arg0);
    }
}