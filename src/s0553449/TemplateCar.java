package s0553449;

import lenz.htw.ai4g.ai.DriverAction;
import lenz.htw.ai4g.ai.Info;

public class /*<n*/TemplateCar/*>*/ extends MyCarMain{
    
    public /*<n*/TemplateCar/*>*/(Info info) {
        super(info);
        brakeAngle = /*<f*/4.15f/*>*/;
        maxAngleSpd = /*<f*/54.39f/*>*/;
        approachPower = /*<f*/1.44f/*>*/;
        turnVelocity = /*<f*/6.05f/*>*/;
        targetWeight = /*<f*/0.98f/*>*/;
        obstacleWeight = /*<f*/9.17f/*>*/;
        feelerDistance = /*<f*/13.49f/*>*/;
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