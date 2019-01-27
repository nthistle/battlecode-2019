package bc19;

import java.util.ArrayList;

public class CastleHandler extends RobotHandler 
{
    public CastleHandler(MyRobot robot) {
        super(robot);
    }

    int ourCastleNum; // IS GUARANTEED to be our order in the turn queue
    int numCastles;
    int[] castleX;
    int[] castleY;
    int[] castleIDs; // NOTE: not guaranteed to be in same order for all 3 castles
    // Also not guaranteed to be fully set until after identification process (if we're 2nd castle)

    boolean receivingCastleInfo;
    boolean hasSymmetricAssigned;

    boolean mapSymmetry;

    ArrayList<Cluster> clusters;
    ArrayList<Cluster> combativeClusters; // pretty close to enemy/us

    ArrayList<Coordinate> myAssignedKarbonite;
    ArrayList<Coordinate> myAssignedFuel;
    int numAssignedKarbonite = 0;
    int numAssignedFuel = 0;

    int numPilgrim = 0; 

    int numProphets = 0; 
    int numCrusaders = 0;

    int[][][] distanceMaps;

    boolean[] clusterAssignments = {true, false, true}; // then, numAssignedTotal % 2 == 1
    int numAssignedTotal = 0;
    int numTransmitted = 0;

    boolean DEBUG = false; // more time intensive when printing

    public void setup() {
        receivingCastleInfo = true;
        hasSymmetricAssigned = false;

        // first, we have to detect which number castle we are
        // for this, we just count the number of broadcasting castleTalks with LSB set
        int otherSpawnedUnits = 0;
        ourCastleNum = 0;
        for (Robot r : robot.getVisibleRobots()) {
            if (r.team == robot.me.team && (r.castle_talk & 1) == 1) {
                ourCastleNum += 1;
                if ((r.castle_talk & 2) == 2) { // this bit is only set if a castle that went before
                    // spawned something, so we know to not count it as a castle
                    if (DEBUG) {
                        robot.log("Detected a friendly Castle as having spawned a unit turn 1 before this!");
                    }
                    otherSpawnedUnits++;
                }
            }
        }

        if (DEBUG) {
            robot.log("Detected self as Castle #" + ourCastleNum);
            robot.log("My coordinates are " + robot.me.x + "," + robot.me.y);
        }

        numCastles = -otherSpawnedUnits;
        for (Robot r : robot.getVisibleRobots()) {
            if (r.team == robot.me.team) {
                numCastles += 1;
            }
        }

        if (DEBUG) {
            robot.log("Detected " + numCastles + " castles!");
        }

        castleX = new int[numCastles];
        castleY = new int[numCastles];
        castleIDs = new int[numCastles];
        distanceMaps = new int[numCastles][][];

        for (int i = 0; i < numCastles; i++) {
            castleX[i] = -1;
            castleY[i] = -1;
        }

        castleX[ourCastleNum] = robot.me.x;
        castleY[ourCastleNum] = robot.me.y;
        castleIDs[ourCastleNum] = robot.me.id;

        // well this is pretty nasty, but devs probably won't increase starting castle count
        if (ourCastleNum == 0) {
            // NOTE: we actually have no idea which is which in the turn queue,
            // but we also don't actually care
            int idx = 1;
            for (Robot r : robot.getVisibleRobots()) {
                if (r.team == robot.me.team && r.id != robot.me.id) {
                    castleIDs[idx++] = r.id;
                }
            }
        } else if (ourCastleNum == 1) {
            // Oof... We won't necessarily know the 3rd castle's ID until we parse castleTalk later in
            // the identification process
            for (Robot r : robot.getVisibleRobots()) {
                if ((r.castle_talk & 1) == 1) {
                    castleIDs[0] = r.id;
                    break;
                }
            }
            if (numCastles > 2) {
                if (otherSpawnedUnits == 0) { // okay, we lucked out, first guy didn't want to spawn anything
                    for (Robot r : robot.getVisibleRobots()) {
                        if (r.id != castleIDs[0] && r.id != castleIDs[1]) {
                            castleIDs[2] = r.id;
                            break;
                        }
                    }
                } else {
                    castleIDs[2] = -1; // we'll have to fill this later
                }
            }
        } else {
            int idx = 0;
            for (Robot r : robot.getVisibleRobots()) {
                if (r.team == robot.me.team && r.id != robot.me.id && (r.castle_talk & 1) == 1) {
                    castleIDs[idx++] = r.id;
                }
            }
        }

        if (DEBUG) {
            robot.log("Detected other castle IDs as:");
            robot.log("Castles[0] = " + castleIDs[0]);
            if (numCastles > 1)
                robot.log("Castles[1] = " + castleIDs[1]);
            if (numCastles > 2)
                robot.log("Castles[2] = " + castleIDs[2]);
        }

        mapSymmetry = Utils.getSymmetry(robot.map, robot.karboniteMap, robot.fuelMap);
    }

    Coordinate myLoc;


    public Action turn() {
        myLoc = Coordinate.fromRobot(robot.me);
        if (DEBUG) {
            robot.log("Starting turn #" + robot.me.turn);
        }
        if (receivingCastleInfo) {  
            receiveCastleInfo();
        }
        else if (!hasSymmetricAssigned) { // we could technically do this a turn earlier but I'm more comfortable
            // with an extra 20ms to do this and we have to be careful that the building process doesn't get triggered
            // before we can send those signals (will cause other castles to hang)
            doSymmetricAssignmentScheme();
            sortTargets();
            hasSymmetricAssigned = true;
            // robot.log("T:" + System.currentTimeMillis());
            if (DEBUG) {
                robot.log("I have been assigned " + myAssignedKarbonite.size() + " karb and " + myAssignedFuel.size() + " fuel");
            }
        }

        if (robot.me.turn <= 2) {
            //int markerBits = builtUnitThisTurn ? 3 : 1;
            int markerBits = 1;
            if (robot.me.turn == 1) {
                robot.castleTalk((robot.me.x << 2) | markerBits);
            } else if (robot.me.turn == 2) {
                robot.castleTalk((robot.me.y << 2) | markerBits);
            }
        }

        if (robot.me.turn <= 10) {
            Action a = doEarlyGamePilgrimSpawn();
            if (a != null) return a;
        }

        /*

        // E C O
        // this has a bias towards castle earlier in turnq but ignore it for now
        // 52 because we need 2 fuel to broadcast its destination
        if (hasSymmetricAssigned && robot.karbonite >= 10 && robot.fuel >= 52 &&
            ((numPilgrim < 2) || (numPilgrim < 4 && robot.me.turn > 40) || (numPilgrim < 6 && robot.me.turn > 100)) &&
            (numAssignedKarbonite < myAssignedKarbonite.size() || numAssignedFuel < myAssignedFuel.size())) {
            Action act = attemptSpawnPilgrim();
            if (act != null) {
                return act;
            }
        }

        // only castle #0 sends this out
        if (hasSymmetricAssigned && ourCastleNum == 0 && numTransmitted < numCastles) { // it'd be nice if we didn't have to transmit ourselves, but I'm lazy

            robot.log("Trying to transmit castle locs...");

            int tReq = getFullTransmitStrength();
            int reqFuel = (int)(0.1+Math.ceil(Math.sqrt(tReq)));
            // here we transmit all castle locs
            if (robot.fuel > 10 + reqFuel) {
                Coordinate toSendLoc = new Coordinate(castleX[numTransmitted], castleY[numTransmitted]);
                toSendLoc = Utils.getReflected(robot.map, toSendLoc, mapSymmetry);
                robot.log("Transmitted a location!");
                robot.signal((toSendLoc.y << 10) | (toSendLoc.x << 4) | 1, tReq);
                numTransmitted += 1;
            }
        }


        if (robot.me.turn >= 10) {
            Direction dr; 
            for (int i=0; i<20; i++){
                dr = Utils.getRandom8Direction(); 
                Coordinate n = new Coordinate(robot.me.x + dr.dx, robot.me.y + dr.dy); 
                if (Utils.isInRange(robot.map, n) && Utils.isPassable(robot.map, n) && !Utils.isOccupied(robot.getVisibleRobotMap(), n)){
                    break; 
                }
            }
            if (numProphets < numCrusaders){
                if (robot.karbonite >= 20 && robot.fuel >= 50){
                    numProphets++;
                    return robot.buildUnit(robot.SPECS.PROPHET, dr.dx, dr.dy);
                }
            }
            else{
                if (robot.karbonite >= 25 && robot.fuel >= 50){
                    numCrusaders++;
                    //return robot.buildUnit(robot.SPECS.CRUSADER, dr.dx, dr.dy);
                    return robot.buildUnit(robot.SPECS.PROPHET, dr.dx, dr.dy);
                }
            }
        }*/

        return doCastleAttack();
    }

    int numLocalSpawned = 0;
    boolean hasSpawnedCombatCP = false;

    public Action doEarlyGamePilgrimSpawn() {
        Action a;
        if (numCastles == 1) {
            if (combativeClusters.size() > 0) {

                if (numLocalSpawned < 1) {
                    a = attemptSpawnPilgrim(true);
                    if (a != null) {
                        numLocalSpawned += 1;
                    }
                    return a;
                }

                if (!hasSpawnedCombatCP) {
                    Cluster closestCCluster = null;
                    int closestDist = 5000;
                    for (Cluster c : combativeClusters) {
                        if (distanceMaps[ourCastleNum][c.y][c.x] < closestDist) {
                            closestDist = distanceMaps[ourCastleNum][c.y][c.x];
                            closestCCluster = c;
                        }
                    }
                    a = attemptSpawnChurchPilgrim(closestCCluster, true);
                    if (a != null) {
                        hasSpawnedCombatCP = true;
                    }
                    return a;
                }

            } else {

            }
        } else {

        }

        return null;
    }

    public Action doCastleAttack() {

        if (robot.fuel >= 10) {
            Direction closestEnemy = null;
            int closestDistance = -1;

            for (Robot r : robot.getVisibleRobots()) {
                if (robot.isVisible(r) && r.team != robot.me.team) {
                    int tdist = Utils.getDistance(Coordinate.fromRobot(r), myLoc);
                    if (closestEnemy == null || tdist < closestDistance) {
                        closestEnemy = myLoc.directionTo(Coordinate.fromRobot(r));
                        closestDistance = tdist;
                    }
                }
            }
            if (closestEnemy != null) {
                return robot.attack(closestEnemy.dx, closestEnemy.dy);
            }
        }

        return null;
    }

    // something transmitted at this r^2 will broadcast to every unit on the map
    public int getFullTransmitStrength() {
        Coordinate c1 = new Coordinate(0,0);
        Coordinate c2 = new Coordinate(0,robot.map.length);
        Coordinate c3 = new Coordinate(robot.map.length,0);
        Coordinate c4 = new Coordinate(robot.map.length,robot.map.length);
        Coordinate m = Coordinate.fromRobot(robot.me);
        return Utils.max(Utils.max(Utils.getDistance(c1, m), Utils.getDistance(c2, m)), Utils.max(Utils.getDistance(c3, m), Utils.getDistance(c4, m)));
    }

    public Action attemptSpawnChurchPilgrim(Cluster c, boolean aggro) {
        if (!(robot.karbonite >= 10 && robot.fuel >= 50)) return null;

        Coordinate dest = chooseChurchHome(c);
        int[][] tDistMap = Utils.getDistanceMap(robot.map, dest);
        int minDist = 5000;
        Direction bestDir = null;

        for (Direction dir : Utils.dir8) {
            Coordinate n = myLoc.add(dir);
            if (Utils.isInRange(robot.map, n) && Utils.isPassable(robot.map, n) && !Utils.isOccupied(robot.getVisibleRobotMap(), n)) {
                if (tDistMap[n.y][n.x] < minDist) {
                    minDist = tDistMap[n.y][n.x];
                    bestDir = dir;
                }
            }
        }

        if (bestDir != null) {
            robot.signal((dest.x << 10) | (dest.y << 4) | (aggro ? 13 : 5), 2);
            return robot.buildUnit(robot.SPECS.PILGRIM, bestDir.dx, bestDir.dy);
        }
        return null; // OOF
    }

    public Coordinate chooseChurchHome(Cluster c) {
        Utils.shuffleArray(Utils.dir9volatile);
        for (Direction d : Utils.dir9volatile) {
            Coordinate co = new Coordinate(c.x + d.dx, c.y + d.dy);
            if (Utils.isInRange(robot.map, co) && robot.map[co.y][co.x] && !robot.fuelMap[co.y][co.x] && !robot.karboniteMap[co.y][co.x]) {
                return co;
            }
        }
        Utils.shuffleArray(Utils.dir21volatile);
        for (Direction d : Utils.dir21volatile) {
            Coordinate co = new Coordinate(c.x + d.dx, c.y + d.dy);
            if (Utils.isInRange(robot.map, co) && robot.map[co.y][co.x] && !robot.fuelMap[co.y][co.x] && !robot.karboniteMap[co.y][co.x]) {
                return co;
            }
        }
        return null;
    }

    // runs up to 1 BFS
    // REQUIRES SIGNALING!
    public Action attemptSpawnPilgrim() {
        // fetch next location to assign
        boolean isKarb = numAssignedTotal < clusterAssignments.length ? clusterAssignments[numAssignedTotal] : (numAssignedTotal % 2 == 1);
        if (numAssignedKarbonite >= myAssignedKarbonite.size()) isKarb = false;
        if (numAssignedFuel >= myAssignedFuel.size()) isKarb = true; 

        return attemptSpawnPilgrim(isKarb);
    }

    public Action attemptSpawnPilgrim(boolean isKarb) {
        if (!(robot.karbonite >= 10 && robot.fuel >= 50)) return null;

        Coordinate assignedTarget = (isKarb ? myAssignedKarbonite : myAssignedFuel).get(isKarb ? numAssignedKarbonite : numAssignedFuel);

        if (DEBUG) {
            robot.log("Doing assignment of " + (isKarb ? "KARBONITE" : "FUEL") + " at location " + assignedTarget);
        }

        int[][] tDistMap = Utils.getDistanceMap(robot.map, assignedTarget);
        int minDist = 5000;
        Direction bestDir = null;


        for (Direction dir : Utils.dir8) {
            Coordinate n = myLoc.add(dir);
            if (Utils.isInRange(robot.map, n) && Utils.isPassable(robot.map, n) && !Utils.isOccupied(robot.getVisibleRobotMap(), n)) {
                if (tDistMap[n.y][n.x] < minDist) {
                    minDist = tDistMap[n.y][n.x];
                    bestDir = dir;
                }
            }
        }

        // for some reason, we're completely blocked, so no build this turn
        if (bestDir != null) {
            // this is the best direction to build in

            if (isKarb) numAssignedKarbonite++;
            else numAssignedFuel++;

            numPilgrim++;
            numAssignedTotal++;

            robot.signal((assignedTarget.x << 10) | (assignedTarget.y << 4) | 7, 2);
            return robot.buildUnit(robot.SPECS.PILGRIM, bestDir.dx, bestDir.dy);
        }

        return null;
    }


    public void doSymmetricAssignmentScheme() {
        // first draw up the distance maps for all the castles
        for (int i = 0; i < numCastles; i++) {
            distanceMaps[i] = Utils.getDistanceMap(robot.map, castleX[i], castleY[i]);
        }

        // TODO: take into account which clusters are closer to enemy
        // TODO: correctly handle tie behavior (it would work, but the ordering of castles that
        // aren't ourselves isn't guaranteed). use smallest castle ID for tiebreak
        myAssignedKarbonite = new ArrayList<Coordinate>();
        myAssignedFuel = new ArrayList<Coordinate>();

        if (DEBUG) {
            robot.log("Doing symmetric assignment!");
        }

        Coordinate orig; // for reflection purposes
        Coordinate refl;

        for (int y = 0; y < robot.karboniteMap.length; y++) {
            for (int x = 0; x < robot.karboniteMap[y].length; x++) {
                if (!robot.fuelMap[y][x] && !robot.karboniteMap[y][x]) continue;
                int assignedTo = 0;
                for (int c = 1; c < numCastles; c++) { // technically, ties aren't handled correctly
                    if (distanceMaps[c][y][x] < distanceMaps[assignedTo][y][x]) {
                        assignedTo = c;
                    }
                }

                orig = new Coordinate(x, y);
                refl = Utils.getReflected(robot.map, orig, mapSymmetry);
                //robot.log("Coordinate is " + x + "," + y + " reflected is " + refl);
                if (distanceMaps[assignedTo][y][x] > distanceMaps[assignedTo][refl.y][refl.x]) 
                    continue;
                // skip any points that are further than their reflected location

                if (assignedTo == ourCastleNum) {
                    if (robot.karboniteMap[y][x]) {
                        myAssignedKarbonite.add(orig);
                    } else {
                        myAssignedFuel.add(orig);
                    }
                }
            }
        }

        // now we clusterize things
        boolean[][] marked = new boolean[robot.map.length][robot.map[0].length];
        for (int y = 0; y < marked.length; y++) {
            for (int x = 0; x < marked[y].length; x++) {
                marked[y][x] = false;
            }
        }

        clusters = new ArrayList<Cluster>();

        for (int y = 0; y < robot.map.length; y++) {
            for (int x = 0; x < robot.map[y].length; x++) {
                if (!marked[y][x] && (robot.karboniteMap[y][x] || robot.fuelMap[y][x])) {

                    marked[y][x] = true;
                    Cluster cluster = new Cluster(x, y);
                    clusterize(cluster, marked, robot.karboniteMap, robot.fuelMap, x, y, 0);

                    cluster.x = (int)(0.5 + ((float)cluster.x)/cluster.count);
                    cluster.y = (int)(0.5 + ((float)cluster.y)/cluster.count);

                    if (robot.map[cluster.y][cluster.x]) {
                        clusters.add(cluster);
                    } else {
                        robot.log("UNEXPECTED BEHAVIOR CENTROID: " + cluster);
                    }
                }
            }
        }

        robot.log("Found these clusters:");

        combativeClusters = new ArrayList<Cluster>();
        for (Cluster c : clusters) {
            robot.log("Cluster: " + c);
            boolean isCombative = true;
            for (int i = 0; i < numCastles; i++) {
                refl = Utils.getReflected(robot.map, new Coordinate(c.x, c.y), mapSymmetry);
                if (Utils.abs(distanceMaps[i][c.y][c.x] - distanceMaps[i][refl.y][refl.x]) > 4) {
                    isCombative = false;
                    break;
                }
            }

            if (isCombative) {
                combativeClusters.add(c);
                robot.log(" ^ IS COMBATIVE");
            }
        }
    }

    public int clusterize(Cluster running, boolean[][] markedMap, boolean[][] karbMap, boolean[][] fuelMap, int x, int y, int depth) {
        if (depth > 15) return 0;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if ((y+i > 0 && y+i < markedMap.length && x+j > 0 && x+j < markedMap[0].length) && !markedMap[y+i][x+j] && (karbMap[y+i][x+j] || fuelMap[y+i][x+j])) {
                    markedMap[y+i][x+j] = true;
                    running.x += x + j;
                    running.y += y + i;
                    running.count += 1;
                    clusterize(running, markedMap, karbMap, fuelMap, x + j, y + i, depth + 1);
                }
            }
        }
    }


    public void receiveCastleInfo() {
        for (Robot r : robot.getVisibleRobots()) {
            if (r.team == robot.me.team && r.id != robot.me.id) {
                if ((r.castle_talk & 1) == 1) {
                    // okay, he's sending out SOMETHING
                    int thisCastleNum = -1;
                    for (int i = 0; i < numCastles; i++) {
                        if (r.id == castleIDs[i]) {
                            thisCastleNum = i;
                            break;
                        }
                    }
                    if (thisCastleNum == -1) {
                        // hey, we can identify the previously-unidentified castle now
                        thisCastleNum = 2;
                        castleIDs[2] = r.id;
                    }

                    int sentValue = r.castle_talk >> 2; // signed shouldn't be an issue
                    if (castleX[thisCastleNum] == -1) {
                        castleX[thisCastleNum] = sentValue;
                    } else if (castleY[thisCastleNum] == -1) {
                        castleY[thisCastleNum] = sentValue;
                        if (DEBUG) {
                            robot.log("Detected Castle with ID " + castleIDs[thisCastleNum] + "'s location as " + castleX[thisCastleNum] + "," + castleY[thisCastleNum]);
                        }
                    }
                }
            }
        }
        // this check is probably inefficient but I'm tired and don't care
        boolean anyRemaining = false;
        for (int i = 0; i < numCastles; i++) {
            if (castleY[i] == -1) {
                anyRemaining = true;
                break;
            }
        }
        receivingCastleInfo = anyRemaining;
        if (DEBUG && !anyRemaining) {
            robot.log("All done detecting!");
        }
    }


    public void sortTargets() {
        Coordinate[] assignedKarb = new Coordinate[myAssignedKarbonite.size()];
        Coordinate[] assignedFuel = new Coordinate[myAssignedFuel.size()];

        for (int i = 0; i < assignedKarb.length; i++) {
            assignedKarb[i] = myAssignedKarbonite.get(i);
        }

        for (int i = 0; i < assignedFuel.length; i++) {
            assignedFuel[i] = myAssignedFuel.get(i);
        }

        // now, insertion sort

        Coordinate tmp;

        for (int i = 1; i < assignedKarb.length; i++) {
            for (int j = i; j > 0; j--) {
                if (distanceMaps[ourCastleNum][assignedKarb[j].y][assignedKarb[j].x] 
                    < distanceMaps[ourCastleNum][assignedKarb[j-1].y][assignedKarb[j-1].x]) {
                    tmp = assignedKarb[j];
                    assignedKarb[j] = assignedKarb[j-1];
                    assignedKarb[j-1] = tmp;
                } else {
                    break;
                }
            }
        }

        for (int i = 1; i < assignedFuel.length; i++) {
            for (int j = i; j > 0; j--) {
                if (distanceMaps[ourCastleNum][assignedFuel[j].y][assignedFuel[j].x] 
                    < distanceMaps[ourCastleNum][assignedFuel[j-1].y][assignedFuel[j-1].x]) {
                    tmp = assignedFuel[j];
                    assignedFuel[j] = assignedFuel[j-1];
                    assignedFuel[j-1] = tmp;
                } else {
                    break;
                }
            }
        }

        myAssignedKarbonite = new ArrayList<Coordinate>();
        myAssignedFuel = new ArrayList<Coordinate>();

        for (int i = 0; i < assignedKarb.length; i++) {
            myAssignedKarbonite.add(assignedKarb[i]);
        }

        for (int i = 0; i < assignedFuel.length; i++) {
            myAssignedFuel.add(assignedFuel[i]);
        }

        // for debug only
        if (DEBUG) {
            robot.log("Fuel printout:");
            for (Coordinate c : myAssignedFuel) {
                robot.log("F:" + c + ": " + distanceMaps[ourCastleNum][c.y][c.x]);
            }
            robot.log("Karb printout:");
            for (Coordinate c : myAssignedKarbonite) {
                robot.log("K:" + c + ": " + distanceMaps[ourCastleNum][c.y][c.x]);
            }
        }
    }
}