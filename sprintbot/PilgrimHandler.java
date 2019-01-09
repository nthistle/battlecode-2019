package bc19;

public class PilgrimHandler extends RobotHandler 
{
    public PilgrimHandler(MyRobot robot) {
        super(robot);
    }

    public void setup() {
        robot.log("Pilgrim setup called!");
    }

    public Action turn() {
        robot.log("Pilgrim turn called!");

        Direction d = Utils.getRandomDirection();
        return robot.move(d.dx, d.dy);
    }
}