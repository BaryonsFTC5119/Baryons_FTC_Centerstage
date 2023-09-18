package org.firstinspires.ftc.teamcode;

import static org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.DEGREES;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesOrder.XYZ;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesOrder.YZX;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesReference.EXTRINSIC;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;

import java.util.ArrayList;
import java.util.List;

/**
 * Hardware definitions and access for a robot with a four-motor
 * drive train and a gyro sensor.
 */
public class RealRobot {

    static final double     COUNTS_PER_MOTOR_REV    = 145.6 ;    // eg: TETRIX Motor Encoder
    static final double     DRIVE_GEAR_REDUCTION    = 0.5;     // This is < 1.0 if geared UP
    static final double     WHEEL_DIAMETER_INCHES   = 3.77953 ;     // For figuring circumference || Previous value of 3.93701
    static final double     COUNTS_PER_INCH         = (COUNTS_PER_MOTOR_REV * DRIVE_GEAR_REDUCTION) / (WHEEL_DIAMETER_INCHES * 3.1415);

    private final HardwareMap hardwareMap;
    private final Telemetry telemetry;

    public final DcMotor lf, lr, rf, rr;


    public ElapsedTime elapsed = new ElapsedTime();

    //public final Servo grabber,track, trayL, trayR;

    private final BNO055IMU imu;

    private double headingOffset = 0.0;
    private Orientation angles;
    private Acceleration gravity;




    // Class Members
    private OpenGLMatrix lastLocation = null;
    private boolean targetVisible = false;
    private float phoneXRotate    = 0;
    private float phoneYRotate    = 0;
    private float phoneZRotate    = 0;

    static final int MOTOR_TICK_COUNTS = 145;


    public double shooterPos = .56;
    public boolean shooterReady = true;


    public RealRobot(final HardwareMap _hardwareMap, final Telemetry _telemetry) {
        hardwareMap = _hardwareMap;
        telemetry = _telemetry;


        lf = hardwareMap.dcMotor.get("lf");
        rf = hardwareMap.dcMotor.get("rf");
        lr = hardwareMap.dcMotor.get("lr");
        rr = hardwareMap.dcMotor.get("rr");

        lf.setDirection(DcMotorSimple.Direction.FORWARD);
        lr.setDirection(DcMotorSimple.Direction.FORWARD);
        rf.setDirection(DcMotorSimple.Direction.REVERSE);
        rr.setDirection(DcMotorSimple.Direction.REVERSE);//reverses the motors



        setMotorZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE, lf, lr, rf, rr);
        setMotorMode(DcMotor.RunMode.RUN_USING_ENCODER, lf, rf, rr, lr);



        imu = hardwareMap.get(BNO055IMU.class, "imu");
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit = BNO055IMU.AngleUnit.RADIANS;
        parameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.calibrationDataFile = "BNO055IMUCalibration.json"; // see the calibration sample opmode
        parameters.loggingEnabled = true;
        parameters.loggingTag = "IMU";
        parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();
        imu.initialize(parameters);
        angles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.RADIANS);
        //zeroPosition = slide.getCurrentPosition();
    }

    public void setMotorMode(DcMotor.RunMode mode, DcMotor... motors) {
        for (DcMotor motor : motors) {
            motor.setMode(mode);
        }
    }

    private void setMotorZeroPowerBehavior(DcMotor.ZeroPowerBehavior mode, DcMotor... motors) {
        for (DcMotor motor : motors) {
            motor.setZeroPowerBehavior(mode);
        }
    }

    public void runUsingEncoders() {
        setMotorMode(DcMotor.RunMode.RUN_USING_ENCODER, lf, lr, rf, rr);
    }

    public void runWithoutEncoders() {
        setMotorMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER, lf, lr, rf, rr);
    }

    /**
     * @return true if the gyro is fully calibrated, false otherwise
     */
    public boolean isGyroCalibrated() {
        return imu.isGyroCalibrated();
    }

    /**
     * Fetch all once-per-time-slice values.
     * <p>
     * Call this either in your OpMode::loop function or in your while(opModeIsActive())
     * loops in your autonomous. It refresh gyro and other values that are computationally
     * expensive.
     */
    public void loop() {
        angles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.RADIANS);
        gravity = imu.getGravity();
    }

    /**
     * @return the raw heading along the desired axis
     */
    private double getRawHeading() {
        return angles.firstAngle;
    }

    /**
     * @return the robot's current heading in radians
     */
    public double getHeading() {
        return (getRawHeading() - headingOffset) % (2.0 * Math.PI);
    }

    /**
     * @return the robot's current heading in degrees
     */
    public double getHeadingDegrees() { return Math.toDegrees(getHeading()); }

    /**
     * Set the current heading to zero.
     */
    public void resetHeading() {
        headingOffset = getRawHeading();
    }

    /**
     * Find the maximum absolute value of a set of numbers.
     *
     * @param xs Some number of double arguments
     * @return double maximum absolute value of all arguments
     */
    private static double maxAbs(double... xs) {
        double ret = Double.MIN_VALUE;
        for (double x : xs) {
            if (Math.abs(x) > ret) {
                ret = Math.abs(x);
            }
        }
        return ret;
    }

    /**
     * Set motor powers
     * <p>
     * All powers will be scaled by the greater of 1.0 or the largest absolute
     * value of any motor power.
     *
     * @param _lf Left front motor
     * @param _lr Left rear motor
     * @param _rf Right front motor
     * @param _rr Right rear motor
     */

    public void setMotors(double _lf, double _lr, double _rf, double _rr) {
        final double scale = maxAbs(1.0, _lf, _lr, _rf, _rr);
        lf.setPower(_lf / scale);
        lr.setPower(_lr / scale);
        rf.setPower(_rf / scale);
        rr.setPower(_rr / scale);
    }

    public void driveInches(double speed, double inches){
        lf.setPower(speed);
        lr.setPower(speed);
        rf.setPower(speed);
        rr.setPower(speed);

        lf.setTargetPosition((int) (inches * COUNTS_PER_INCH));
        lr.setTargetPosition((int) (inches * COUNTS_PER_INCH));
        rf.setTargetPosition((int) (inches * COUNTS_PER_INCH));
        rr.setTargetPosition((int) (inches * COUNTS_PER_INCH));

        lf.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        lr.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rf.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rr.setMode(DcMotor.RunMode.RUN_TO_POSITION);

    }


    public void stopRobot(){
        lf.setPower(0);
        rf.setPower(0);
        lr.setPower(0);
        rr.setPower(0);
    }

    public void drive(double speed) {
        stopRobot();
        setMotors(speed, -speed, speed, speed);

    }

    public void gyroDrive(double x, double y) {
        //final double rotation = Math.pow(controller.right_stick_x, 3.0)/1.5;
        double direction = Math.atan2(x, y) + (getHeading());
        double speed = Math.min(1.0, Math.sqrt(x * x + y * y));

        double lf = speed * Math.sin(direction + Math.PI / 4.0);
        double rf = speed * Math.cos(direction + Math.PI / 4.0);
        double lr = speed * Math.cos(direction + Math.PI / 4.0);
        double rr = speed * Math.sin(direction + Math.PI / 4.0);

        setMotors(lf, lr, rf, rr);
    }

    public void speedGyroDrive(double x, double y, double robotSpeed) {
        //final double rotation = Math.pow(controller.right_stick_x, 3.0)/1.5;
        double direction = Math.atan2(x, y) + (getHeading());
        double speed = Math.min(1.0, robotSpeed);

        double lf = speed * Math.sin(direction + Math.PI / 4.0);
        double rf = speed * Math.cos(direction + Math.PI / 4.0);
        double lr = speed * Math.cos(direction + Math.PI / 4.0);
        double rr = speed * Math.sin(direction + Math.PI / 4.0);

        setMotors(lf, lr, rf, rr);

        telemetry.addData("RF & LR Speed", Math.cos(direction + Math.PI / 4.0));
        telemetry.update();
    }

    public void gyroDriveSlow(double x, double y) {
        //final double rotation = Math.pow(controller.right_stick_x, 3.0)/1.5;
        double direction = Math.atan2(x, y) + (getHeading());
        double speed = Math.min(1.0, Math.sqrt(x * x + y * y));

        double lf = .5 * speed * Math.sin(direction + Math.PI / 4.0);
        double rf = .5 * speed * Math.cos(direction + Math.PI / 4.0);
        double lr = .5 * speed * Math.cos(direction + Math.PI / 4.0);
        double rr = .5 * speed * Math.sin(direction + Math.PI / 4.0);

        setMotors(lf, lr, rf, rr);
    }

    public void setUpGyroDrive(int distance){

        lf.setTargetPosition(distance + lf.getCurrentPosition());
        rf.setTargetPosition(distance + rf.getCurrentPosition());
        lr.setTargetPosition(distance + lr.getCurrentPosition());
        rr.setTargetPosition(distance + rr.getCurrentPosition());

    }

    /**
     *
     * @param power // power (Decimal) .0-1.0
     * @param distance // distance (in inches)
     * @param direction // direction (F = forward, B = back, L = Strafe left, R = Strafe Right)
     */

    public void encoderDrive(double power, double distance, char direction) {
        distance *= 3;
        setMotorZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // How many turns do I need the wheels to go [distance] inches?

        // The distance you drive with one turn of the wheel is the circumference of the wheel

        //Re-measure
        double circumference = ((direction == 'F' || direction == 'B') ? Math.PI*WHEEL_DIAMETER_INCHES : 11.4);
        double TICKS_PER_INCH = MOTOR_TICK_COUNTS/circumference;

        int eTarget = (int)(TICKS_PER_INCH*distance);



        ((DcMotorEx)lf).setTargetPositionTolerance(12);
        ((DcMotorEx)rf).setTargetPositionTolerance(12);
        ((DcMotorEx)lr).setTargetPositionTolerance(12);
        ((DcMotorEx)rr).setTargetPositionTolerance(12);

        if(direction == 'R')
        {
            lf.setTargetPosition(-eTarget + lf.getCurrentPosition());
            rf.setTargetPosition(eTarget + rf.getCurrentPosition());
            lr.setTargetPosition(eTarget + lr.getCurrentPosition());
            rr.setTargetPosition(-eTarget + rr.getCurrentPosition());
        }
        else if (direction == 'L')
        {
            lf.setTargetPosition(eTarget + lf.getCurrentPosition());
            rf.setTargetPosition(-eTarget + rf.getCurrentPosition());
            lr.setTargetPosition(-eTarget + lr.getCurrentPosition());
            rr.setTargetPosition(eTarget + rr.getCurrentPosition());
        }
        else if (direction == 'B')
        {
            lf.setTargetPosition(eTarget + lf.getCurrentPosition());
            rf.setTargetPosition(eTarget + rf.getCurrentPosition());
            lr.setTargetPosition(eTarget + lr.getCurrentPosition());
            rr.setTargetPosition(eTarget + rr.getCurrentPosition());
        }
        else if (direction == 'F')
        {
            lf.setTargetPosition(-eTarget + lf.getCurrentPosition());
            rf.setTargetPosition(-eTarget + rf.getCurrentPosition());
            lr.setTargetPosition(-eTarget + lr.getCurrentPosition());
            rr.setTargetPosition(-eTarget + rr.getCurrentPosition());
        }

        //set the power desired for the motors
        lf.setPower(power*.7*(direction == 'R' || direction == 'F' ? 1.3 : 1));
        rf.setPower(power*.7*(direction == 'R' || direction == 'F' ? 1.3 : 1));
        lr.setPower(power*.7);
        rr.setPower(power*.7);

        // set the motors to RUN_TO_POSITION
        lf.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rf.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        lr.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rr.setMode(DcMotor.RunMode.RUN_TO_POSITION);

//        lf.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
//        lr.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
//        rf.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
//        rr.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        while(lf.isBusy() || rf.isBusy() || lr.isBusy() || rr.isBusy())
        {
            loop();
            // make sure to not do anything while the motors are running
            telemetry.addData("Path", "Driving " + distance + " inches");
            /*telemetry.addData("Slide position:",slide.getCurrentPosition());
            telemetry.addData("Slide target:",slide.getTargetPosition());*/
            telemetry.addData("Current position", lf.getCurrentPosition());
            telemetry.addData("Target position", lf.getTargetPosition());
            telemetry.addData("Heading", getHeadingDegrees());

            telemetry.update();
        }
        telemetry.addData("Path", "Complete");
        telemetry.update();
    }

    /**
     *
     * @param degrees //degrees (-360 to 360) you want to rotate (Positive is clockwise)
     */
    public void encoderRotate(int degrees, double power) {

        // RESETS ENCODERS

        // Circumference of the circle made by the robot (19-inch diameter * pi)
        double rotationLength = 34.5565;

        // Length the wheels would have to travel in order to rotate 1 degree in length (distance / 360)
        double degreeLength = rotationLength/360.0;

        double distance = Math.abs(degrees)*degreeLength;
        // The distance you drive with one turn of the wheel is the circumference of the wheel
        double circumference = (28/14)*3.14*WHEEL_DIAMETER_INCHES;

        double rotationsNeeded = distance/circumference;

        int eTarget = (int)(rotationsNeeded * MOTOR_TICK_COUNTS);

        ((DcMotorEx)lf).setTargetPositionTolerance(12);
        ((DcMotorEx)rf).setTargetPositionTolerance(12);
        ((DcMotorEx)lr).setTargetPositionTolerance(12);
        ((DcMotorEx)rr).setTargetPositionTolerance(12);
        //Set target position
        if(degrees > 0)
        {
            lf.setTargetPosition(eTarget    + lf.getCurrentPosition());
            rf.setTargetPosition(eTarget*-1 + rf.getCurrentPosition());
            lr.setTargetPosition(eTarget    + lr.getCurrentPosition());
            rr.setTargetPosition(eTarget*-1 + rr.getCurrentPosition());
        }
        else if(degrees < 0)
        {
            lf.setTargetPosition(eTarget*-1 + lf.getCurrentPosition());
            rf.setTargetPosition(eTarget    + rf.getCurrentPosition());
            lr.setTargetPosition(eTarget*-1 + lr.getCurrentPosition());
            rr.setTargetPosition(eTarget    + rr.getCurrentPosition());
        }

        //set the power desired for the motors
        lf.setPower(power*-1);//power used to be *-0.7
        rf.setPower(power*-1);
        lr.setPower(power*-1);
        rr.setPower(power*-1);

//        lf.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
//        lf.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
//        lf.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // set the motors to RUN_TO_POSITION
        lf.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rf.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        lr.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rr.setMode(DcMotor.RunMode.RUN_TO_POSITION);

//        while(lf.isBusy() || rf.isBusy() || lr.isBusy() || rr.isBusy())
//        {
//            loop();
//            // make sure to not do anything while the motors are running
//            telemetry.addData("Path", "Driving " + distance + " inches");
//            telemetry.addData("Heading", getHeadingDegrees());
//            telemetry.update();
//        }



//        lf.setPower(0);
//        rf.setPower(0);
//        lr.setPower(0);
//        rr.setPower(0);

        while(lf.isBusy() || rf.isBusy() || lr.isBusy() || rr.isBusy())
        {
            loop();
            // make sure to not do anything while the motors are running
            telemetry.addData("Path", "Driving " + distance + " inches");
            /*telemetry.addData("Slide position:",slide.getCurrentPosition());
            telemetry.addData("Slide target:",slide.getTargetPosition());*/
            telemetry.addData("Current position", lf.getCurrentPosition());
            telemetry.addData("Target position", lf.getTargetPosition());
            telemetry.addData("Heading", getHeadingDegrees());



            telemetry.update();
        }
        telemetry.addData("Path", "Complete");
        //telemetry.addData("Heading: ", getHeadingDegrees());
        telemetry.update();
    }

    public void rotateToHeading(int degrees, double power){
        int head = (int)getHeadingDegrees();
        int diff=head-degrees;
        encoderRotate(-diff, power);
        loop();
        telemetry.addData("Heading: ", getHeadingDegrees());
        while(!(head>degrees-3&&head<degrees+3)){
            encoderRotate(-10, power);
            head = (int)getHeadingDegrees();
            loop();
            telemetry.addData("Heading: ", getHeadingDegrees());
        }
    }

    public void mecanumEncoders()
    {
        lf.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        lr.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rf.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rr.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    public void sleep(int millis)
    {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    /**
     @param degrees heading that you are rotating to
     */
    public void rotate(double degrees, double power)
    {
        //double endHeading = getHeadingDegrees()+degrees;
        double endHeading = degrees - getHeadingDegrees();
        if(endHeading<-180)
            endHeading+=360;
        else if(endHeading>180)
            endHeading-=360;
        if(degrees > 0)
        {
            lf.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            lr.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            rf.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            rr.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

            lf.setPower(-power);
            lr.setPower(-power);
            rf.setPower(power);
            rr.setPower(power);
            do {
                loop();
                telemetry.addData("Heading", getHeadingDegrees());
                telemetry.addData("Absolute", Math.abs(degrees-getHeadingDegrees()));
                telemetry.addData("Degrees", degrees);
                telemetry.update();

            } while(Math.abs(getHeadingDegrees()-endHeading) > 4);
            lf.setPower(0);
            lr.setPower(0);
            rf.setPower(0);
            rr.setPower(0);
        }
        else if(degrees < 0)
        {
            lf.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            lr.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            rf.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            rr.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

            lf.setPower(power);
            lr.setPower(power);
            rf.setPower(-power);
            rr.setPower(-power);
            do {
                loop();
                telemetry.addData("Heading", getHeadingDegrees());
                telemetry.addData("Absolute", Math.abs(degrees-getHeadingDegrees()));
                telemetry.addData("Degrees", degrees);
                telemetry.update();

            }while(Math.abs(getHeadingDegrees()-endHeading) > 4);
            lf.setPower(0);
            lr.setPower(0);
            rf.setPower(0);
            rr.setPower(0);
        }
    }

    public double convertHeading(double degrees) {
        if(degrees < -180) return degrees + 360;
        else if(degrees > 180) return degrees - 360;
        else return degrees;
    }

    public void rotateTo(double degrees, double power)
    {
        double currHeading = convertHeading(getHeadingDegrees());
        //double endHeading = getHeadingDegrees()+degrees;
        double endHeading = convertHeading(degrees);

        double diff = convertHeading(endHeading - currHeading);
        if(diff > 0)
        {
            lf.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            lr.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            rf.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            rr.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

            lf.setPower(0.25);
            lr.setPower(0.25);
            rf.setPower(-0.25);
            rr.setPower(-0.25);
            do {
                loop();

                double newPow = Math.abs(currHeading-convertHeading(degrees)) / 300.0;
                lf.setPower(newPow);
                lr.setPower(newPow);
                rf.setPower(-newPow);
                rr.setPower(-newPow);
                currHeading = convertHeading(getHeadingDegrees());
                telemetry.addData("Heading", getHeadingDegrees());
                telemetry.addData("Absolute", Math.abs(degrees));
                telemetry.addData("Degrees", diff);
                telemetry.update();

            } while(Math.abs(currHeading-convertHeading(degrees)) > 4);
            lf.setPower(0);
            lr.setPower(0);
            rf.setPower(0);
            rr.setPower(0);
        }
        else if(diff < 0)
        {
            lf.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            lr.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            rf.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            rr.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

            lf.setPower(-0.25);
            lr.setPower(-0.25);
            rf.setPower(0.25);
            rr.setPower(0.25);
            do {
                loop();

                double newPow = Math.abs(currHeading-convertHeading(degrees)) / 400.0;
                lf.setPower(-newPow);
                lr.setPower(-newPow);
                rf.setPower(newPow);
                rr.setPower(newPow);
                currHeading = convertHeading(getHeadingDegrees());
                telemetry.addData("Heading", getHeadingDegrees());
                telemetry.addData("Absolute", Math.abs(degrees));
                telemetry.addData("Degrees", diff);
                telemetry.update();

            }while(Math.abs(currHeading-convertHeading(degrees)) > 4);
            lf.setPower(0);
            lr.setPower(0);
            rf.setPower(0);
            rr.setPower(0);
        }
    }

    public void useEncoders()
    {
        lf.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        lr.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rf.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rr.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }




    public boolean approxServo(double actualPos, double supposedPos) {
        return (actualPos < supposedPos+0.001 && actualPos > supposedPos-0.001);
    }

    public boolean approxMotor(int actualPos, double supposedPos) {
        return (actualPos < supposedPos + 5 && actualPos > supposedPos - 5);
    }




}

