package bc19;

public class ChurchHandler extends RobotHandler 
{
    public ChurchHandler(MyRobot robot) {
        super(robot);
    }

    boolean aggro;

    public void setup() {
        aggro = false;
        for (Robot r : robot.getVisibleRobots()) {
            if (r.team == robot.me.team && Utils.getDistance(r.x, r.y, robot.me.x, robot.me.y) <= 2) {
                if (r.signal == 80) {
                    aggro = true;
                    break;
                }
            }
        }
        if (aggro) robot.log("I am aggro church!");
    }

    public Action turn() {
        Coordinate myLocation = Coordinate.fromRobot(robot.me);
        if (aggro) {
            // first priority in this case is to build a prophet for defense
            if (robot.karbonite >= 25 && robot.fuel >= 52) {
                Direction dir = Utils.getRandom8Direction();
                Coordinate poss = myLocation.add(dir);
                int ctr = 0;
                while ((!Utils.isInRange(robot.map, poss) || !Utils.isPassable(robot.map, poss) || Utils.isOccupied(robot.getVisibleRobotMap(), poss)) && ((ctr++) < 10)) {
                    dir = Utils.getRandom8Direction();
                    poss = myLocation.add(dir);
                }
                if (Utils.isInRange(robot.map, poss) && Utils.isPassable(robot.map, poss) && !Utils.isOccupied(robot.getVisibleRobotMap(), poss)) {
                    aggro = false;
                    return robot.buildUnit(robot.SPECS.PROPHET, dir.dx, dir.dy);
                }
            }
        } else {

        }
        return null;
    }
}