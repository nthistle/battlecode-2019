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


    // Utils methods

    // Random from 4-directions (r2 distance 1)
    public static Direction getRandomDirection() {
        return directions[(int)(Math.random() * directions.length)];
    }

    public static Direction getRandom8Direction() {
        return dir8[(int)(Math.random() * dir8.length)];
    }

    // This direction map is indexed with [y][x], just like terrain
    public static Direction[][] getDirectionMap(boolean[][] terrain, int x, int y) {
        Direction[][] dirMap = new Direction[terrain.length][terrain[0].length];

        LinkedList<Coordinate> queue = new LinkedList<Coordinate>();
        queue.offer(new Coordinate(x, y));
        dirMap[y][x] = new Direction(0, 0);

        while(queue.size() > 0) {
            Coordinate c = queue.poll();

            for(Direction dir : directions) {
                Coordinate n = c.add(dir);
                if (isInRange(terrain, n) && isPassable(terrain, n) && dirMap[n.y][n.x] == null) {
                    dirMap[n.y][n.x] = dir.reverse();
                    queue.offer(n);
                }
            }
        }

        return dirMap;
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

    public boolean equals(Object other) {
        if (!(other instanceof Coordinate)) return false;
        Coordinate c = (Coordinate)other;
        return c.x == x && c.y == y;
    }

    public String toString() {
        return "(" + x + "," + y + ")";
    }
}