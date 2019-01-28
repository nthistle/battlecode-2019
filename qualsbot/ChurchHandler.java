package bc19;

import java.util.ArrayList;

public class ChurchHandler extends RobotHandler 
{
    public ChurchHandler(MyRobot robot) {
        super(robot);
    }

    boolean aggro;
    boolean firstChurch;

    ArrayList<Coordinate> myCluster;
    int[][] seenDudes;

    public void setup() {
        aggro = false;
        firstChurch = true;
        for (Robot r : robot.getVisibleRobots()) {
            if (r.team == robot.me.team && Utils.getDistance(r.x, r.y, robot.me.x, robot.me.y) <= 2) {
                if (r.signal == 80) {
                    aggro = true;
                    break;
                }
            }
            if (r.id != robot.me.id && r.team == robot.me.team && r.unit == robot.SPECS.CHURCH && Utils.getDistance(r.x, r.y, robot.me.x, robot.me.y) <= 14) {
                firstChurch = false;
            }
        }
        //if (aggro) robot.log("I am aggro church!");

        // we have to assess our cluster state
        if (firstChurch) {
            //robot.log("I am the first church");
            boolean[][] marked = new boolean[robot.map.length][robot.map[0].length];
            for (int y = 0; y < marked.length; y++) {
                for (int x = 0; x < marked[y].length; x++) {
                    marked[y][x] = false;
                }
            }

            myCluster = new ArrayList<Coordinate>();

            Coordinate myLoc = Coordinate.fromRobot(robot.me);
            for (Direction d : Utils.dir8) {
                Coordinate co = myLoc.add(d);
                if (!Utils.isInRange(robot.map, co)) continue;
                if (robot.fuelMap[co.y][co.x] || robot.karboniteMap[co.y][co.x]) {
                    if (!marked[co.y][co.x]) {
                        marked[co.y][co.x] = true;
                        myCluster.add(co);
                        clusterize(marked, robot.karboniteMap, robot.fuelMap, co.x, co.y, 0);
                    }
                }
            }
        }
        
        seenDudes = new int[robot.map.length][robot.map[0].length];
        for (int y = 0; y < seenDudes.length; y++) {
            for (int x = 0; x < seenDudes[y].length; x++) {
                seenDudes[y][x] = 1000;
            }
        }
    }

    public int clusterize(boolean[][] markedMap, boolean[][] karbMap, boolean[][] fuelMap, int x, int y, int depth) {
        if (depth > 15) return 0;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if ((y+i > 0 && y+i < markedMap.length && x+j > 0 && x+j < markedMap[0].length) && !markedMap[y+i][x+j] && (karbMap[y+i][x+j] || fuelMap[y+i][x+j])) {
                    markedMap[y+i][x+j] = true;
                    myCluster.add(new Coordinate(x+j, y+i));
                    clusterize(markedMap, karbMap, fuelMap, x + j, y + i, depth + 1);
                }
            }
        }
    }

    int numPilgrimsBuilt = 0;
    int numSoldiersBuilt = 0;

    int numChurchesMade = 0;

    int sinceLastAssign = 5; // countdown so we don't put two guys on same spot, ish
    int sinceLastChurchBuild = 25;

    Coordinate myLocation;

    public Action turn() {
        myLocation = Coordinate.fromRobot(robot.me);
        Action a;

        if (sinceLastChurchBuild < 25) sinceLastChurchBuild++;
        if (sinceLastAssign      <  5) sinceLastAssign++;

        if ((robot.me.turn % 4) == 0) {
            updateSeenDudes();
        }

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
            if (firstChurch) {
                if (robot.karbonite >= 25 && robot.fuel >= 50) {
                    if ((numPilgrimsBuilt >= 2 * (1 + numSoldiersBuilt)) ||
                            (numPilgrimsBuilt > myCluster.size() && Math.random() < 0.5)) {
                        a = attemptBuildProphet();
                        if (a != null) {
                            numSoldiersBuilt++;
                            return a;
                        }
                    }
                }
                if (robot.me.turn > 50) {
                    if (sinceLastChurchBuild >= 25 && robot.karbonite >= 25 && robot.fuel >= 150 && numChurchesMade < 4) {
                        // build a new church pilgrim
                        a = attemptBuildChurcher();
                        if (a != null) {
                            sinceLastChurchBuild = 0;
                            numPilgrimsBuilt++;
                            numChurchesMade++;
                            return a;
                        }
                    }
                }
                if (robot.karbonite >= 10 && robot.fuel >= 52 && sinceLastAssign >= 5 && (numPilgrimsBuilt < (1.5 * myCluster.size()))) {
                    a = attemptBuildMiner();
                    if (a != null) {
                        sinceLastAssign = 0;
                        numPilgrimsBuilt++;
                        return a;
                    }
                }
            } else {
                //robot.log("We are not the first church.");
                if (robot.karbonite >= 50 && robot.fuel >= 50) {
                    if (Math.random() < 0.3) {
                        a = attemptBuildProphet();
                        if (a != null) {
                            numSoldiersBuilt++;
                            return a;
                        }
                    }
                }
            }
        }
        return null;
    }

    public void updateSeenDudes() {
        for (Robot r : robot.getVisibleRobots()) {
            if (robot.isVisible(r) && r.team == robot.me.team && r.unit == robot.SPECS.PILGRIM) {
                seenDudes[r.y][r.x] = 0;
            }
        }
        for (int y = Utils.max(0, robot.me.y - 5); y < Utils.min(robot.map.length, robot.me.y + 6); y++) {
            for (int x = Utils.max(0, robot.me.x - 5); x < Utils.min(robot.map.length, robot.me.x + 6); x++) {
                seenDudes[y][x] += 1;
            }
        }
    }

    public Action attemptBuildProphet() {
        if (!(robot.karbonite >= 25 && robot.fuel >= 50)) return null;

        int ctr = 0;
        Direction bd = Utils.dir8[(int)(Math.random() * 8)];
        Coordinate buildLoc = myLocation.add(bd);
        while ((!Utils.isInRange(robot.map, buildLoc) || robot.getVisibleRobotMap()[buildLoc.y][buildLoc.x] != 0) && (ctr++)<10) {
            bd = Utils.dir8[(int)(Math.random() * 8)];
            buildLoc = myLocation.add(bd);
        }

        if (ctr >= 10) return null;

        return robot.buildUnit(robot.SPECS.PROPHET, bd.dx, bd.dy);
    }

    public Action attemptBuildChurcher() {
        if (!(robot.karbonite >= 10 && robot.fuel >= 52)) return null;

        Coordinate churchLocation = chooseChurchHome();
        if (churchLocation == null) return null;

        int ctr = 0;

        Direction bd = myLocation.directionTo(churchLocation).normalize();
        while ((!Utils.isInRange(robot.map, myLocation.add(bd)) || !robot.map[myLocation.y + bd.dy][myLocation.x + bd.dx] || robot.getVisibleRobotMap()[myLocation.y + bd.dy][myLocation.x + bd.dx] != 0) && (ctr++) < 10) {
            bd = Utils.dir8[(int)(Math.random() * 8)];
        }

        if (ctr >= 10) return null;

        robot.signal((churchLocation.x << 10) | (churchLocation.y << 4) | 5, 2);
        return robot.buildUnit(robot.SPECS.PILGRIM, bd.dx, bd.dy);
    }

    public Action attemptBuildMiner() {
        if (!(robot.karbonite >= 10 && robot.fuel >= 52)) return null;

        int ctr = 0;
        Coordinate target = myCluster.get((int)(Math.random() * myCluster.size()));
        while (robot.getVisibleRobotMap()[target.y][target.x] != 0 && (ctr++) < 10) {
            target = myCluster.get((int)(Math.random() * myCluster.size()));
        }
        if (ctr >= 10) return null; // okay this is obo but it doesn't matter

        ctr = 0;

        Direction bd = myLocation.directionTo(target).normalize();
        while ((!Utils.isInRange(robot.map, myLocation.add(bd)) || !robot.map[myLocation.y + bd.dy][myLocation.x + bd.dx] || seenDudes[myLocation.y + bd.dy][myLocation.x + bd.dx] > 10
            || robot.getVisibleRobotMap()[myLocation.y + bd.dy][myLocation.x + bd.dx] != 0) && (ctr++) < 10) {
            bd = Utils.dir8[(int)(Math.random() * 8)];
        }

        if (ctr >= 10) return null;

        robot.signal((target.x << 10) | (target.y << 4) | 7, 2);
        return robot.buildUnit(robot.SPECS.PILGRIM, bd.dx, bd.dy);
    }

    public Coordinate chooseChurchHome() {
        int ctr = 0;
        Coordinate base = myCluster.get((int)(Math.random() * myCluster.size()));
        Direction bd = Utils.dir8[(int)(Math.random() * 8)];
        Coordinate chosenLoc = base.add(bd);
        while (!isValidChurchLocation(chosenLoc) && (ctr++) < 20) {
            base = myCluster.get((int)(Math.random() * myCluster.size()));
            bd = Utils.dir8[(int)(Math.random() * 8)];
            chosenLoc = base.add(bd);
        }
        if (ctr >= 20) return null;
        return chosenLoc;
    }

    public boolean isValidChurchLocation(Coordinate c) {

        if (!Utils.isInRange(robot.map, c) || !robot.map[c.y][c.x] || robot.fuelMap[c.y][c.x] || robot.karboniteMap[c.y][c.x])
            return false;

        for (Direction d : Utils.dir9) {
            Coordinate n = c.add(d);
            if (Utils.isInRange(robot.map, c)) {
                int tid = robot.getVisibleRobotMap()[n.y][n.x];
                if (tid > 0 && robot.getRobot(tid).unit == robot.SPECS.CHURCH)
                    return false;
                // adjacent to or already has a church
            }
        }

        return true;
    }
}