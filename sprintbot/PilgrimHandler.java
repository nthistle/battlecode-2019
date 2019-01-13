package bc19;

public class PilgrimHandler extends RobotHandler 
{
    public PilgrimHandler(MyRobot robot) {
        super(robot);
    }

    boolean isKarbonitePilgrim; // TODO

    Direction[][] targetMap;
    Direction[][] castleMap;

    //the location of the site I am mining (right now we consider pilgrims miners)
    Coordinate targetLocation;

    //the location of the castle site I am connected to (later this could be a church)
    Coordinate castleLocation;

    boolean requiresCastleSignal = true;

    public void setup() {

        //establish a coordinate of where I am when first created
        Coordinate myLoc = Coordinate.fromRobot(robot.me);

        //below established my home castle based on what is the closest to me on my creation
        int closestCastleDistance = -1;

        //store the castle I am assigned to (this is just for the one turn)
        Robot myCastle; 

        // TODO: need to handle case where pilgrim is adjacent to 2 castles
        // SOLVED

        for (Robot r : robot.getVisibleRobots()) {

            //check if this robot is a castle on my team
            if (r.team == robot.me.team && r.unit == robot.SPECS.CASTLE) {

                //if the castle is the closer than the current closest, update it
                if ((castleLocation == null || Utils.getDistance(myLoc, Coordinate.fromRobot(r)) < closestCastleDistance)
                      && (!requiresCastleSignal || r.signal != -1)) {

                    //assign the castlelocation to robot r's coordinate, similarly assign distance
                    castleLocation = Coordinate.fromRobot(r);
                    closestCastleDistance = Utils.getDistance(myLoc, castleLocation);
                    myCastle = r;
                }
            }
        }

        if (myCastle.signal == -1) {
            robot.log("ERROR! COULD NOT FIND A SIGNAL FROM MY HOME CASTLE");
        }

        // this will probably work really badly if we don't receive a signal, 
        // TODO is to put some placeholder values in here so we don't get anything
        // fatal in an edge case

        int targetX = myCastle.signal >> 10; // parsed target x
        int targetY = (myCastle.signal >> 4) & (63); // parsed target y

        targetLocation = new Coordinate(targetX, targetY);
        robot.log("Identified signaled location as " + targetLocation);

        targetMap = Utils.getDirectionMap(robot.map, targetLocation.x, targetLocation.y);
        castleMap = Utils.getDirectionMap(robot.map, castleLocation.x, castleLocation.y);
    }

    public Action turn() {
        //robot.log("Pilgrim turn called!");

        int mspeed = Utils.min(4, robot.fuel);

        if (robot.me.karbonite >= 20 || robot.me.fuel >= 100) {
            // navigate back to base

            // if base nearby, handoff
            for (Robot r : robot.getVisibleRobots()) {
                if (r.team == robot.me.team && r.unit == robot.SPECS.CASTLE) {
                    if (Utils.getDistance(robot.me.x, robot.me.y, r.x, r.y) <= 2) {
                        robot.log("Handing off Karbonite/Fuel!");
                        return robot.give(r.x - robot.me.x, r.y - robot.me.y, robot.me.karbonite, robot.me.fuel);
                    }
                }
            }

            // just walk towards base
            Direction dir = Utils.followDirectionMap(castleMap, robot.getVisibleRobotMap(), mspeed, robot.me.x, robot.me.y);

            int ctr = 0;
            while (dir.equals(Utils.STATIONARY) && (ctr++) < 10) {
                dir = Utils.followDirectionMapFuzz(castleMap, robot.getVisibleRobotMap(), mspeed, robot.me.x, robot.me.y);
            }

            if (dir.equals(Utils.STATIONARY)) {
                robot.log("Can't move!");
                return null; // TODO: make it mine if it can
            }

            return robot.move(dir.dx, dir.dy);
        } else {
            // navigate towards karb/fuel and mine if applicable

            // TODO: check to make sure this square is actually mine-able
            if (Coordinate.fromRobot(robot.me).equals(targetLocation)) {
//            if (robot.getFuelMap()[robot.me.y][robot.me.x] || robot.getKarboniteMap()[robot.me.y][robot.me.x]) {
                return robot.mine();
            } 

            Direction dir = Utils.followDirectionMap(targetMap, robot.getVisibleRobotMap(), mspeed, robot.me.x, robot.me.y);

            int ctr = 0;
            while (dir.equals(Utils.STATIONARY) && (ctr++) < 10) {
                dir = Utils.followDirectionMapFuzz(targetMap, robot.getVisibleRobotMap(), mspeed, robot.me.x, robot.me.y);
            }

            if (dir.equals(Utils.STATIONARY)) {
                robot.log("Can't move!");
                return null; // TODO: make it mine if it can
            }

            return robot.move(dir.dx, dir.dy);
        }
    }


}