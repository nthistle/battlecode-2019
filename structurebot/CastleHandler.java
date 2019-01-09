package bc19;

public class CastleHandler extends RobotHandler 
{
    public CastleHandler(MyRobot robot) {
        super(robot);
    }

    int crusadersBuilt;

    public void setup() {
        robot.log("Castle setup called!");
        crusadersBuilt = 0; // not necessary, used as an example
    }

    public Action turn() {
        robot.log("Castle turn called!");
        if (crusadersBuilt == 0) {
            return robot.buildUnit(robot.SPECS.CRUSADER, 0, 1);
        }
        return null;
    }
}