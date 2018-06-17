package s0553449;

import lenz.htw.ai4g.ai.DriverAction;
import lenz.htw.ai4g.ai.Info;

public class /*<n*/TemplateCar/*>*/ extends MyCarMain{
    
    public /*<n*/TemplateCar/*>*/(Info info) {
        super(info);
        // approachPower = /*<f*/1.4607647999999998f/*>*/;
        // turnVelocity = /*<f*/5.558256f/*>*/;
	    // cornerPostOffset = /*<f*/10.933880000000002f/*>*/;
        targetWeight = /*<f*/1.049188f/*>*/;
        obstacleWeight = /*<f*/9.459520000000001f/*>*/;
        feelerDistance = /*<f*/20.516320000000004f/*>*/;
        feelerDistanceSides = /*<f*/20.64272f/*>*/;
        feelerAngle = /*<f*/0.7524399999999999f/*>*/;
        
	    cornerOffset = /*<f*/30.6f/*>*/;
        maxDistanceFromPath = /*<f*/33.704640000000005f/*>*/;
        targetPointShift = /*<f*/97.8576f/*>*/;
    }

    @Override
    protected void init() {
        enlistForInternalDevelopmentPurposesOnlyAndDoNOTConsiderThisAsPartOfTheHandedInSolution();
		calculateGraph();
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