package bc19;

public class MiningPilgrimHandler extends RobotHandler 
{
    Coordinate miningTarget;
    Coordinate homeCastle;

    Direction[][] miningMap;
    Direction[][] homeMap;

    public MiningPilgrimHandler(MyRobot robot, Coordinate miningTarget, Coordinate homeCastle) {
        super(robot);
        this.miningTarget = miningTarget;
        this.homeCastle = homeCastle;
    }

    public void setup() {
        miningMap = Utils.getDirectionMap8(robot.map, miningTarget);
        homeMap   = Utils.getDirectionMap8(robot.map, homeCastle);
        Utils.setStationary(homeMap, homeCastle.x, homeCastle.y, 1, 1);
    }

    int idleTurns = 0;
    boolean headingBack = false;

    int sinceLastHomeUpdate = 15;

    Coordinate myLocation;

    // TODO: reassign to closest church 
    public Action turn() {
        if (robot.fuel < 1) return null;

        myLocation = Coordinate.fromRobot(robot.me);
        int usableFuel = (robot.fuel < 75 ? 2 : 4);

        if (robot.me.turn % 5 == 0 && sinceLastHomeUpdate >= 15) updateHomeCheck();
        if (sinceLastHomeUpdate < 15) sinceLastHomeUpdate++;

        // if we're empty, then we've finished our dropoff, head back out to mine
        if (headingBack && (robot.me.fuel == 0 && robot.me.karbonite == 0)) {
            headingBack = false;
        }

        // if we're mining and full of a resource or in danger, start heading back to dropoff
        if (!headingBack && (robot.me.fuel >= 100 || robot.me.karbonite >= 20 || isInDanger())) {
            headingBack = true;
        }

        if (headingBack) {
            // if we're in dropoff zone, just dropoff
            if (homeMap[robot.me.y][robot.me.x].equals(Utils.STATIONARY)) {
                robot.log("Handing off mining load! [K=" + robot.me.karbonite + ",F=" + robot.me.fuel + "]");
                return robot.give(homeCastle.x - robot.me.x, homeCastle.y - robot.me.y, robot.me.karbonite, robot.me.fuel);
            }

            Direction dir = Utils.followDirectionMap(homeMap, robot.getVisibleRobotMap(), usableFuel, robot.me.x, robot.me.y);

            // after 5 turns of not being able to move closer, we stop flailing
            if (dir.equals(Utils.STATIONARY) && idleTurns > 5) {
                if (idleTurns > 10) {
                    robot.log("WOW IDLE SO LONG I'M DUMPING!!");
                    return robot.give(homeMap[robot.me.y][robot.me.x].dx, homeMap[robot.me.y][robot.me.x].dy, robot.me.karbonite, robot.me.fuel);
                }
                robot.log("I've been idle for " + idleTurns + " turns!");
                idleTurns++;
                return null;
            }

            int ctr = 0;
            while (dir.equals(Utils.STATIONARY) && (ctr++) < 10) {
                dir = Utils.followDirectionMapFuzz(homeMap, robot.getVisibleRobotMap(), usableFuel, robot.me.x, robot.me.y);
            }

            // if we had to fuzz, bump idle counter
            if (ctr > 0) idleTurns++;
            else idleTurns = 0;

            if (dir.equals(Utils.STATIONARY)) return null;

            return robot.move(dir.dx, dir.dy);

        } else {

            if (miningMap[robot.me.y][robot.me.x].equals(Utils.STATIONARY)) {
                return robot.mine();
            }

            Direction dir = Utils.followDirectionMap(miningMap, robot.getVisibleRobotMap(), usableFuel, robot.me.x, robot.me.y);

            int ctr = 0;
            while (dir.equals(Utils.STATIONARY) && (ctr++) < 10) {
                dir = Utils.followDirectionMapFuzz(miningMap, robot.getVisibleRobotMap(), usableFuel, robot.me.x, robot.me.y);
            }

            if (dir.equals(Utils.STATIONARY)) {
                if (robot.karboniteMap[robot.me.y][robot.me.x] || robot.fuelMap[robot.me.y][robot.me.x]) {
                    // as long as we're stuck here...
                    return robot.mine();
                }
            }

            return robot.move(dir.dx, dir.dy);
        }

    }

    public void updateHomeCheck() {
        if (sinceLastHomeUpdate < 15) return;

        if (Utils.getDistance(myLocation, homeCastle) <= 2) return;

        for (Direction d : Utils.dir8) {
            Coordinate n = myLocation.add(d);
            if (Utils.isInRange(robot.map, n) && robot.getVisibleRobotMap()[n.y][n.x] > 0) {
                Robot possHome = robot.getRobot(robot.getVisibleRobotMap()[n.y][n.x]);
                if (possHome.team == robot.me.team && (possHome.unit == robot.SPECS.CHURCH || possHome.unit == robot.SPECS.CASTLE)) {
                    homeCastle = Coordinate.fromRobot(possHome);
                    homeMap = Utils.getDirectionMap8(robot.map, homeCastle);
                    Utils.setStationary(homeMap, homeCastle.x, homeCastle.y, 1, 1);
                    sinceLastHomeUpdate = 0;
                    robot.log("We updated home! -> " + homeCastle);
                    return;
                }
            }
        }
    }

    // TODO: scan for enemies, so we run back to drop off if in danger
    // better than die with our loot
    public boolean isInDanger() {
        return false;
    }
}