package bc19;

public class CrusaderHandler extends RobotHandler 
{
    public CrusaderHandler(MyRobot robot) {
        super(robot);
    }

    boolean mapSymmetry;
    int origX, origY;
    Direction[][] enemyCastleMap, myCastleMap;
    Coordinate myCastle;
    Coordinate myTarget;
    Direction myDirection; // pick one of the four directions to head

    public void setup() {
        origX = robot.me.x;
        origY = robot.me.y;
        myCastle = Coordinate.fromRobot(findMyCastle());
        //mapSymmetry = Utils.getSymmetry(robot.map, robot.karboniteMap, robot.fuelMap);
        //myCastleMap = Utils.getDirectionMap(robot.map, myCastle);
        //myTarget = Utils.getReflected(robot.map, myCastle, mapSymmetry);
        //enemyCastleMap = Utils.getDirectionMap(robot.map, myTarget);
        if (origX == myCastle.x){
         myDirection = new Direction(0, (origY - myCastle.y) * 2);   
        }
        else if (origY == myCastle.y){
            myDirection = new Direction((origX - myCastle.x) * 2, 0);
        }
        else{
            if ((int)(Math.random() * 2) == 0){
                myDirection = new Direction((origX - myCastle.x) * 2, 0);
            }
            else{
                myDirection = new Direction(0, (origY - myCastle.y) * 2);
            }
        }
    }

    // lazy and just finds first, but whatever
    public Robot findMyCastle() {
        for (Robot r : robot.getVisibleRobots()) {
            if (robot.isVisible(r) && r.team == robot.me.team && (r.unit == robot.SPECS.CASTLE || r.unit == robot.SPECS.CHURCH)
                && Utils.getDistance(r.x, r.y, robot.me.x, robot.me.y) <= 2) {
                return r;
            }
        }
    }

    int idleTurns = 0;

    public Action turn() {
        // robot.log ("LOCATION: " + robot.me.x + " " + robot.me.y);
        // first look for closest enemy
        int usableFuel = (robot.fuel > 100 ? 4 : 2);
        Coordinate myLoc = Coordinate.fromRobot(robot.me);

        Robot nearestEnemy = null;
        int nearestDist = (int)1e9;

        for (Robot r : robot.getVisibleRobots()) {
            if (r.team != robot.me.team) {
                int dist = Utils.getDistance(myLoc, Coordinate.fromRobot(r));
                if (nearestEnemy == null || dist < nearestDist) { // ignore enemies within this range, hope someone else kills them for us
                    nearestEnemy = r;
                    nearestDist = dist;
                }
            }
        }
        /*
        if (Utils.getDistance(myLoc, myTarget) <= 64) {
            int tid = robot.getVisibleRobotMap()[myTarget.y][myTarget.x];
            if (tid != -1) {
                if (tid == 0 || robot.getRobot(tid).team == robot.me.team) {
                    // reassign new location
                    // for now, rando
                    int ctr = 0;
                    do {
                        myTarget = new Coordinate((int)(Math.random() * robot.map[0].length), (int)(Math.random() * robot.map.length));
                    } while (!robot.map[myTarget.y][myTarget.x] && (ctr++)<20);
                    if (!robot.map[myTarget.y][myTarget.x]) {
                        myTarget = myCastle; // just go home at this point tbh
                    }
                    enemyCastleMap = Utils.getDirectionMap(robot.map, myTarget);
                }
            }
        }
        */
        if (nearestEnemy != null && nearestDist <= 16) {
            // attack the nearest enemy. 
            // TODO: Instead of attacking nearest enemy, attack non pilgrim enemies first.
            return robot.attack(nearestEnemy.x - robot.me.x, nearestEnemy.y - robot.me.y);
        }
        
        // make sure our location right now is valid
        if ((robot.me.x + robot.me.y) % 2 == 0 && canMove(robot.me.x, robot.me.y)){ // sum of coordinates is even
            return null; // we're already where we need to be. 
        }

        Direction towardsHome = myLoc.directionTo(myCastle);

        Utils.shuffleArray(Utils.dir8volatile);

        Coordinate n;

        for (Direction d : Utils.dir8volatile) {
            if (d.dot(towardsHome) <= 0) {
                n = myLoc.add(d);
                if (Utils.isInRange(robot.map, n) && robot.map[n.y][n.x] && robot.getVisibleRobotMap()[n.y][n.x] == 0 && ((n.x + n.y) % 2 == 0)) {
                    idleTurns = 0;
                    return robot.move(d.dx, d.dy);
                }
            }
        }

        for (Direction d : Utils.dir8volatile) {
            if (d.dot(towardsHome) <= 0) {
                n = myLoc.add(d);
                if (Utils.isInRange(robot.map, n) && robot.map[n.y][n.x] && robot.getVisibleRobotMap()[n.y][n.x] == 0) {
                    idleTurns = 0;
                    return robot.move(d.dx, d.dy);
                }
            }
        }

        idleTurns++;
        Utils.shuffleArray(Utils.dir12volatile);

        int bestDot = 10000;
        Direction bestDir = Utils.STATIONARY;

        for (Direction d : Utils.dir12volatile) {
            n = myLoc.add(d);
            if (Utils.isInRange(robot.map, n) && robot.map[n.y][n.x] && robot.getVisibleRobotMap()[n.y][n.x] == 0) {
                if (d.dot(towardsHome) < bestDot) {
                    bestDot = d.dot(towardsHome);
                    bestDir = d;
                }
            }
        }

        if (bestDir.equals(Utils.STATIONARY)) return null;

        return robot.move(bestDir.dx, bestDir.dy);

        /*


        for (int i=0; i<4; i++){ // check 4 neighboring squares and see if we can move on to them
            int new_x = Utils.directions[i].dx + robot.me.x; 
            int new_y = Utils.directions[i].dy + robot.me.y; 
            if (canMove(new_x, new_y)){
                return robot.move(Utils.directions[i].dx, Utils.directions[i].dy); 
            }
        }

        Direction [] openDirections = new Direction [3]; 
        openDirections[0] = myDirection; 
        
        if (myDirection.dx == 0){ // get directions +- 45 degrees of this direction
            openDirections[1] = new Direction(myDirection.dx + 1, myDirection.dy / 2); 
            openDirections[2] = new Direction(myDirection.dx - 1, myDirection.dy / 2); 
        }
        else{
            openDirections[1] = new Direction(myDirection.dx / 2, myDirection.dy + 1); 
            openDirections[2] = new Direction(myDirection.dx / 2, myDirection.dy - 1); 
        }

        ArrayList <Direction> direcs = new ArrayList<> (); 
        for (int i=0; i<3; i++){ // add all of the 3 directions that are possible
            if (canMove(robot.me.x + openDirections[i].dx, robot.me.y + openDirections[i].dy)) direcs.add(openDirections[i]); 
        }
        if (direcs.size() == 0){ // none of the possible directions worked .. oops
            return null; // just stay here at this point tbh 
        }
        int choice = (int)(Math.random() * direcs.size()); // pick one of the open direcs
        Direction toGo = direcs.get(choice);
        return robot.move(toGo.dx, toGo.dy); */
    }

    public boolean canMove (int new_x, int new_y) {
        if (!Utils.isInRange(robot.map, new_x, new_y)) return false;
        if (!Utils.isPassable(robot.map, new_x, new_y)) return false;
        if (robot.karboniteMap[new_y][new_x] || robot.fuelMap[new_y][new_x]) return false;
        //;if (Utils.isOccupied(robot.getVisibleRobotMap(), new_x, new_y)) return false;

        Coordinate c = new Coordinate(new_x, new_y);
        for (Direction d : Utils.dir8) {
            Coordinate n = c.add(d);
            if (Utils.isInRange(robot.map, n)) {
                int tid = robot.getVisibleRobotMap()[n.y][n.x];
                if (tid > 0) {
                    if (robot.getRobot(tid).team == robot.me.team && (robot.getRobot(tid).unit == robot.SPECS.CHURCH || robot.getRobot(tid).unit == robot.SPECS.CASTLE)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}