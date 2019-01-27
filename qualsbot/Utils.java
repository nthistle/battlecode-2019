package bc19;

import java.util.LinkedList;
import java.util.ArrayList;

public class Utils
{
    // Keep Utils constants at top of file
    public static final Direction[] directions = {new Direction( 1, 0),
        new Direction( 0, 1), new Direction(-1, 0), new Direction( 0,-1)};

    public static final Direction[] dir8 = {new Direction(1, 0),
        new Direction(1, 1), new Direction(0, 1), new Direction(-1, 1),
        new Direction(-1, 0), new Direction(-1, -1), new Direction(0, -1),
        new Direction(1, -1)};

    public static final Direction[] dir8volatile = {new Direction(1, 0),
        new Direction(1, 1), new Direction(0, 1), new Direction(-1, 1),
        new Direction(-1, 0), new Direction(-1, -1), new Direction(0, -1),
        new Direction(1, -1)};

    public static final Direction[] dir8o = {new Direction(1, 0),
        new Direction(0, 1), new Direction(-1, 0), new Direction(0, -1),
        new Direction(1, 1), new Direction(-1, 1), new Direction(-1, -1),
        new Direction(1, -1)};

    public static final Direction STATIONARY = new Direction(0, 0);

    public static final Direction[] dir9 = {STATIONARY, new Direction(1, 0),
        new Direction(1, 1), new Direction(0, 1), new Direction(-1, 1),
        new Direction(-1, 0), new Direction(-1, -1), new Direction(0, -1),
        new Direction(1, -1)};

    public static final Direction[] dir21volatile = {new Direction(0, 0), new Direction(-1, 0),
        new Direction(1, 0), new Direction(0, -1), new Direction(0, 1), new Direction(-1, -1),
        new Direction(1, 1), new Direction(-1, 1), new Direction(1, -1), new Direction(-2, 0),
        new Direction(2, 0), new Direction(0, -2), new Direction(0, 2), new Direction(-2, 1),
        new Direction(2, -1), new Direction(1, -2), new Direction(-1, 2), new Direction(-2, -1),
        new Direction(2, 1), new Direction(-1, -2), new Direction(1, 2)};

    // Utils methods

    // This direction map is indexed with [y][x], just like terrain
    public static Direction[][] getDirectionMap(boolean[][] terrain, int x, int y) {
        Direction[][] dirMap = new Direction[terrain.length][terrain[0].length];

        LinkedList<Coordinate> queue = new LinkedList<Coordinate>();
        queue.add(new Coordinate(x, y));
        dirMap[y][x] = new Direction(0, 0);

        Direction[] myDirections = new Direction[directions.length];
        for (int i = 0; i < directions.length; i++) myDirections[i] = directions[i];
        // we create a copy of directions so we can randomly shuffle it to give
        // variance to the paths

        while (queue.size() > 0) {
            Coordinate c = queue.poll();

            shuffleArray(myDirections);
            for (Direction dir : myDirections) {
                Coordinate n = c.add(dir);
                if (isInRange(terrain, n) && isPassable(terrain, n) && dirMap[n.y][n.x] == null) {
                    dirMap[n.y][n.x] = dir.reverse();
                    queue.add(n);
                }
            }
        }

        return dirMap;
    }

    public static Direction[][] getDirectionMap(boolean[][] terrain, Coordinate c) {
        return getDirectionMap(terrain, c.x, c.y);
    }

    public static Direction[][] getDirectionMap8(boolean[][] terrain, int x, int y) {
        Direction[][] dirMap = new Direction[terrain.length][terrain[0].length];

        LinkedList<Coordinate> queue = new LinkedList<Coordinate>();
        queue.add(new Coordinate(x, y));
        dirMap[y][x] = Utils.STATIONARY;

        while (queue.size() > 0) {
            Coordinate c = queue.poll();

            for (Direction dir : dir8o) {
                Coordinate n = c.add(dir);
                if (isInRange(terrain, n) && isPassable(terrain, n) && dirMap[n.y][n.x] == null) {
                    dirMap[n.y][n.x] = dir.reverse();
                    queue.add(n);
                }
            }
        }

        return dirMap;
    }

    public static Direction[][] getDirectionMap8(boolean[][] terrain, Coordinate c) {
        return getDirectionMap8(terrain, c.x, c.y);
    }

    public static int[][] getDistanceMap(boolean[][] terrain, int x, int y) {
        int[][] distMap = new int[terrain.length][terrain[0].length];

        for (int i = 0; i < distMap.length; i++) {
            for (int j = 0; j < distMap[i].length; j++) {
                distMap[i][j] = 5000;
            }
        }

        LinkedList<Coordinate> queue = new LinkedList<Coordinate>();
        queue.add(new Coordinate(x, y));

        distMap[y][x] = 0;

        while (queue.size() > 0) {
            Coordinate c = queue.poll();

            for (Direction dir : directions) {
                Coordinate n = c.add(dir);
                if (isInRange(terrain, n) && isPassable(terrain, n) && distMap[n.y][n.x] == 5000) {
                    distMap[n.y][n.x] = distMap[c.y][c.x] + 1;
                    queue.add(n);
                }
            }
        }

        return distMap;
    }

    public static int[][] getDistanceMap(boolean[][] terrain, Coordinate c) {
        return getDistanceMap(terrain, c.x, c.y);
    }

    // Tries to follow dirMap as far as possible, given distanceAllowed and robotMap
    // Note that this doesn't converge to optimal pathing behavior, but works well enough.
    public static Direction followDirectionMap(Direction[][] dirMap, int[][] robotMap, 
            int distanceAllowed, int x, int y) {

        if (dirMap[y][x] == null) return null;

        ArrayList<Direction> cumulativeDirs = new ArrayList<Direction>();
        Coordinate originalLoc = new Coordinate(x, y);
        Coordinate cumulativeLoc = originalLoc;

        Direction nextDir;

        while (getDistance(originalLoc, cumulativeLoc) < distanceAllowed) {
            nextDir = dirMap[cumulativeLoc.y][cumulativeLoc.x];
            if (nextDir.dx == 0 && nextDir.dy == 0) break;
            cumulativeDirs.add(nextDir);
            cumulativeLoc = cumulativeLoc.add(nextDir);
        }

        // go backwards until we hit unoccupied
        int lastIdx = cumulativeDirs.size();
        while ((getDistance(originalLoc, cumulativeLoc) > distanceAllowed || isOccupied(robotMap, cumulativeLoc)) && lastIdx > 0) {
            cumulativeLoc = cumulativeLoc.subtract(cumulativeDirs.get(--lastIdx));
        }

        return originalLoc.dirTo(cumulativeLoc);
    }


    public static Direction followDirectionMap(Direction[][] dirMap, int[][] robotMap,
        int distanceAllowed, Coordinate c) {
        return followDirectionMap(dirMap, robotMap, distanceAllowed, c.x, c.y);
    }

    // adds a random vector at each step, in the hopes of letting it move around
    // obstacles. in particular, supposed to help with map with seed 23, where other
    // fuel/karb deposits block the path of bots trying to reach the castle
    public static Direction followDirectionMapFuzz(Direction[][] dirMap, int[][] robotMap, 
            int distanceAllowed, int x, int y) {

        if (dirMap[y][x] == null) return null;

        ArrayList<Direction> cumulativeDirs = new ArrayList<Direction>();
        Coordinate originalLoc = new Coordinate(x, y);
        Coordinate cumulativeLoc = originalLoc;

        Coordinate tentative;
        Direction randDir;

        Direction nextDir;

        while (getDistance(originalLoc, cumulativeLoc) < distanceAllowed) {
            nextDir = dirMap[cumulativeLoc.y][cumulativeLoc.x];
            if (nextDir.dx == 0 && nextDir.dy == 0) break;
            cumulativeDirs.add(nextDir);
            cumulativeLoc = cumulativeLoc.add(nextDir);

            randDir = directions[(int)(Math.random() * directions.length)];
            tentative = cumulativeLoc.add(randDir);
            if (dirMap[tentative.y][tentative.x] != null) {
                cumulativeDirs.add(randDir);
                cumulativeLoc = tentative;
            }
        }

        // go backwards until we hit unoccupied
        int lastIdx = cumulativeDirs.size();
        while ((getDistance(originalLoc, cumulativeLoc) > distanceAllowed || isOccupied(robotMap, cumulativeLoc)) && lastIdx > 0) {
            cumulativeLoc = cumulativeLoc.subtract(cumulativeDirs.get(--lastIdx));
        }

        return originalLoc.dirTo(cumulativeLoc);
    }

    public static Direction followDirectionMapFuzz(Direction[][] dirMap, int[][] robotMap,
        int distanceAllowed, Coordinate c) {
        return followDirectionMap(dirMap, robotMap, distanceAllowed, c.x, c.y);
    }


    // shuffles array in place
    public static <E> void shuffleArray(E[] array) {
        E tmp;
        int swapIndex;
        for (int i = 0; i < array.length - 1; i ++) {
            swapIndex = i + (int)(Math.random() * (array.length - i));
            tmp = array[i];
            array[i] = array[swapIndex];
            array[swapIndex] = tmp;
        }
    }

    public static void setStationary(Direction[][] dirMap, int x, int y, int xR, int yR) {
        for (int yT = Utils.max(0, y - yR); yT < Utils.min(dirMap.length, y + yR + 1); yT++) {
            for (int xT = Utils.max(0, x - xR); xT < Utils.min(dirMap[0].length, x + xR + 1); xT++) {
                dirMap[yT][xT] = STATIONARY;
            }
        }
    }

    // Random from 4-directions (r2 distance 1)
    public static Direction getRandomDirection() {
        return directions[(int)(Math.random() * directions.length)];
    }

    public static Direction getRandom8Direction() {
        return dir8[(int)(Math.random() * dir8.length)];
    }

    public static int getDistance(int x1, int y1, int x2, int y2) {
        return (x2-x1)*(x2-x1) + (y2-y1)*(y2-y1);
    }

    public static int getDistance(Coordinate c1, Coordinate c2) {
        return getDistance(c1.x, c1.y, c2.x, c2.y);
    }

    public static boolean isInRange(boolean[][] terrain, int x, int y) {
        return x >= 0 && y >= 0 && y < terrain.length && x < terrain[y].length;
    }

    public static boolean isInRange(boolean[][] terrain, Coordinate c) {
        return isInRange(terrain, c.x, c.y);
    }

    public static boolean isPassable(boolean[][] terrain, int x, int y) {
        return terrain[y][x];
    }

    public static boolean isPassable(boolean[][] terrain, Coordinate c) {
        return isPassable(terrain, c.x, c.y);
    }

    public static boolean isOccupied(int[][] robotMap, int x, int y) {
        return robotMap[y][x] > 0; // doesn't work for squares out of vision range
    }

    public static boolean isOccupied(int[][] robotMap, Coordinate c) {
        return isOccupied(robotMap, c.x, c.y);
    }

    public static int max(int a, int b) {
        return a > b ? a : b;
    }

    public static int min(int a, int b) {
        return a < b ? a : b;
    }

    public static int abs(int a) {
        return a > 0 ? a : -a;
    }

    // TODO: add explicit horizontal symmetry and specific case logic
    // for both horizontal and vertical symmetry (low priority)
    public static boolean checkVerticalSymmetry(boolean[][] m) {
        for (int y = 0; y < m.length; y++) {
            for (int x = 0; x < m[y].length; x++) {
                if (m[y][x] != m[y][m[y].length - 1 - x]) return false;
            }
        }
        return true;
    }

    // true means vertical symmetry
    public static boolean getSymmetry(boolean[][] map, boolean[][] karboniteMap, boolean[][] fuelMap) {
        return checkVerticalSymmetry(map) && checkVerticalSymmetry(karboniteMap) && checkVerticalSymmetry(fuelMap);
    }

    // true means vertical symmetry
    public static Coordinate getReflected(boolean[][] map, Coordinate c, boolean symmetry) {
        return symmetry ? new Coordinate(map[c.y].length - 1 - c.x, c.y) : new Coordinate(c.x, map.length - 1 - c.y);
    }

    // returns a map where true means it's on "our side"
    public static boolean[][] getTerritoryMap(boolean[][] map, Coordinate knownFriendly, boolean symmetry) {
        boolean[][] territoryMap = new boolean[map.length][map[0].length];
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                territoryMap[i][j] = false;
            }
        }
        if (symmetry) {
            if (knownFriendly.x < (map[0].length/2)) {
                for (int i = 0; i < map.length; i++) {
                    for (int j = 0; j < (map[0].length/2); j++) {
                        territoryMap[i][j] = true;
                    }
                }
            } else {
                for (int i = 0; i < map.length; i++) {
                    for (int j = (map[0].length/2); j < map[0].length; j++) {
                        territoryMap[i][j] = true;
                    }
                }
            }
        } else {
            if (knownFriendly.y < (map.length/2)) {
                for (int i = 0; i < (map.length/2); i++) {
                    for (int j = 0; j < map[0].length; j++) {
                        territoryMap[i][j] = true;
                    }
                }
            } else {
                for (int i = (map.length/2); i < map.length; i++) {
                    for (int j = 0; j < map[0].length; j++) {
                        territoryMap[i][j] = true;
                    }
                }
            }
        }
        return territoryMap;
    }
}

class Direction
{
    public final int dx;
    public final int dy;

    public Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public Direction reverse() {
        return new Direction(-this.dx, -this.dy);
    }

    // normalizes to 4 cardinal, 
    public Direction normalize() {
        if (Utils.abs(dx) > Utils.abs(dy)) { // x is dominant
            return new Direction(dx/Utils.abs(dx),0);
        } else { // y is dominant
            return new Direction(0,dy/Utils.abs(dy));
        }
    }

    public boolean equals(Object other) {
        if (!(other instanceof Direction)) return false;
        Direction d = (Direction)other;
        return d.dx == dx && d.dy == dy;
    }

    public String toString() {
        return "<" + dx + "," + dy + ">";
    }
}

class Coordinate
{
    public final int x;
    public final int y;

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static Coordinate fromRobot(Robot r) {
        return new Coordinate(r.x, r.y);
    }

    public Coordinate add(Direction d) {
        return new Coordinate(this.x + d.dx, this.y + d.dy);
    }

    public Coordinate subtract(Direction d) {
        return new Coordinate(this.x - d.dx, this.y - d.dy);
    }

    public Direction dirTo(Coordinate c) {
        return new Direction(c.x - this.x, c.y - this.y);
    }

    public Direction directionTo(Coordinate c) {
        return this.dirTo(c);
    }

    public boolean equals(Object other) {
        if (!(other instanceof Coordinate)) return false;
        Coordinate c = (Coordinate)other;
        return c.x == x && c.y == y;
    }

    public String toString() {
        return "(" + x + "," + y + ")";
    }
}

class Cluster
{
    public int x;
    public int y;
    public int count;

    public Cluster(int x, int y) {
        this.x = x;
        this.y = y;
        this.count = 1;
    }

    public Cluster(int x, int y, int count) {
        this.x = x;
        this.y = y;
        this.count = count;
    }

    public String toString() {
        return "{(" + x + "," + y + ")," + count + "}";
    }
}