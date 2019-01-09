package bc19;

public class Utils
{
    // Keep Utils constants at top of file
    public static final Direction[] directions = {new Direction( 1, 0),
        new Direction( 0, 1), new Direction(-1, 0), new Direction( 0,-1)};


    // Utils methods

    // Random from 4-directions (r2 distance 1)
    public static Direction getRandomDirection() {
        return directions[(int)(Math.random() * directions.length)];
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
}