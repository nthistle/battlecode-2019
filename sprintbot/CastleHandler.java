package bc19;

public class CastleHandler extends RobotHandler 
{
    public CastleHandler(MyRobot robot) {
        super(robot);
    }

    int builtPilgrims = 0;

    public void setup() {
    }

    public Action turn() {
        if (builtPilgrims < 2) {
            if (robot.karbonite >= 10 && robot.fuel >= 50) {
                return buildRandom(robot.SPECS.PILGRIM);
            }
        } else {
            return buildRandom(robot.SPECS.CRUSADER);
        }
    }

    public Action buildRandom(int unitType) {
        Coordinate myLoc = Coordinate.fromRobot(robot.me);
        // for (int i = 0; i < 50; i++) {
            Direction buildDir = Utils.getRandom8Direction();
            return robot.buildUnit(unitType, buildDir.dx, buildDir.dy);
        // }
        // return null;
    }
}