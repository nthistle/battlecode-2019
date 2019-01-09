package bc19;

public class CrusaderHandler extends RobotHandler 
{
    public CrusaderHandler(MyRobot robot) {
        super(robot);
    }

    public void setup() {
        robot.log("Crusader setup called!");
    }

    public Action turn() {
        robot.log("Crusader turn called!");

        Direction d = Utils.getRandomDirection();
        return robot.move(d.dx, d.dy);
    }
}