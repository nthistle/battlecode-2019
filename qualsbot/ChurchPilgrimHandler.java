package bc19;

public class ChurchPilgrimHandler extends RobotHandler 
{
    Coordinate clusterDestination;
    boolean isAggro;

    public ChurchPilgrimHandler(MyRobot robot, Coordinate target, boolean isAggro) {
        super(robot);
        this.clusterDestination = target;
        this.isAggro = isAggro;
    }

    Coordinate originalLocation;

    Direction[][] clusterMap; // map to cluster destination

    boolean builtMyChurch = false;

    public void setup() {

        //establish a coordinate of where I am when first created
        originalLocation = Coordinate.fromRobot(robot.me);

        clusterMap = Utils.getDirectionMap8(robot.map, clusterDestination);
        Utils.setStationary(clusterMap, clusterDestination.x, clusterDestination.y, 1, 1);
    }

    public Action turn() {
        if (builtMyChurch) return null;
        if (robot.fuel < 5) return null;

        Coordinate myLocation = Coordinate.fromRobot(robot.me);

        int usableFuel = (robot.fuel < 75 ? 2 : 4);

        if (Utils.getDistance(myLocation, clusterDestination) <= 2) {
            Direction buildDir = myLocation.directionTo(clusterDestination);
            if (robot.getVisibleRobotMap()[clusterDestination.y][clusterDestination.x] == 0 &&
                   robot.karbonite >= 50 && robot.fuel >= 200) {
                builtMyChurch = true;
                return robot.buildUnit(robot.SPECS.CHURCH, buildDir.dx, buildDir.dy);
            } else {
                return null;
            }
        } else {
            Direction dir = Utils.followDirectionMap(clusterMap, robot.getVisibleRobotMap(), usableFuel, robot.me.x, robot.me.y);
            int ctr = 0;
            while (dir.equals(Utils.STATIONARY) && (ctr++) < 10) {
                dir = Utils.followDirectionMapFuzz(clusterMap, robot.getVisibleRobotMap(), usableFuel, robot.me.x, robot.me.y);
            }
            if (dir.equals(Utils.STATIONARY)) return null;
            return robot.move(dir.dx, dir.dy);
        }
    }
}