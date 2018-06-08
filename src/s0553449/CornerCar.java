package s0553449;

import java.awt.Point;

import lenz.htw.ai4g.ai.DriverAction;
import lenz.htw.ai4g.ai.Info;

public class /*<n*/CornerCar/*>*/ extends MyCarMain{
    protected Point corner = new Point(-4000, 0);

    public /*<n*/CornerCar/*>*/(Info info) {
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
    protected void init() {
        enlistForInternalDevelopmentPurposesOnlyAndDoNOTConsiderThisAsPartOfTheHandedInSolution();
    }

    @Override
    public String getName() {
        return /*<n*/"CornerCar"/*>*/;
    }

    @Override
    public DriverAction update(boolean arg0) {
        return super.update(arg0);
    }

    @Override
    protected Point getCurrentCheckpoint() {
        return corner;
    }
}