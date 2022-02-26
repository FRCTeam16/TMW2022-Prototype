package frc.robot.subsystems;

import com.swervedrivespecialties.swervelib.Mk4SwerveModuleHelper;
import com.swervedrivespecialties.swervelib.SdsModuleConfigurations;
import com.swervedrivespecialties.swervelib.SwerveModule;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInLayouts;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.subsystems.gyro.BSGyro;
import frc.robot.subsystems.gyro.PigeonGyro;

import static frc.robot.Constants.*;

public class DrivetrainSubsystem extends SubsystemBase {
    /**
     * The maximum voltage that will be delivered to the drive motors.
     * <p>
     * This can be reduced to cap the robot's maximum speed. Typically, this is
     * useful during initial testing of the robot.
     */
    public static final double MAX_VOLTAGE = 12.0;

    // FIXME Measure the drivetrain's maximum velocity or calculate the theoretical.
    // The formula for calculating the theoretical maximum velocity is:
    // <Motor free speed RPM> / 60 * <Drive reduction> * <Wheel diameter meters> *
    // pi
    // By default this value is setup for a Mk3 standard module using Falcon500s to
    // drive.
    // An example of this constant for a Mk4 L2 module with NEOs to drive is:
    // 5880.0 / 60.0 / SdsModuleConfigurations.MK4_L2.getDriveReduction() *
    // SdsModuleConfigurations.MK4_L2.getWheelDiameter() * Math.PI
    /**
     * The maximum velocity of the robot in meters per second.
     * <p>
     * This is a measure of how fast the robot should be able to drive in a straight
     * line.
     */
    public static final double MAX_VELOCITY_METERS_PER_SECOND = 6380.0 / 60.0 *
            SdsModuleConfigurations.MK4_L2.getDriveReduction() *
            SdsModuleConfigurations.MK4_L2.getWheelDiameter() * Math.PI;

    public static final double MAX_ACCELERATION_METERS_PER_SECOND_SQUARED = 2.5;

    /**
     * The maximum angular velocity of the robot in radians per second.
     * <p>
     * This is a measure of how fast the robot can rotate in place.
     */
    // Here we calculate the theoretical maximum angular velocity. You can also
    // replace this with a measured amount.
    public static final double MAX_ANGULAR_VELOCITY_RADIANS_PER_SECOND = MAX_VELOCITY_METERS_PER_SECOND /
            Math.hypot(DRIVETRAIN_TRACKWIDTH_METERS / 2.0, DRIVETRAIN_WHEELBASE_METERS / 2.0);

    public static final double MAX_ANGULAR_VELOCITY_DEGREES_PER_SECOND = MAX_ANGULAR_VELOCITY_RADIANS_PER_SECOND * 180.0
            / Math.PI;

    public static final double MAX_ANGULAR_ACCELERATION_DEGREES_PER_SECOND_SQUARED = 720.0;
    public static final double MAX_ANGULAR_ACCELERATION_RADIANS_PER_SECOND_SQUARED = MAX_ANGULAR_ACCELERATION_DEGREES_PER_SECOND_SQUARED
            * Math.PI / 180.0;

    private final SwerveDriveKinematics m_kinematics = new SwerveDriveKinematics(
            // Front left
            new Translation2d(DRIVETRAIN_TRACKWIDTH_METERS / 2.0, DRIVETRAIN_WHEELBASE_METERS / 2.0),
            // Front right
            new Translation2d(DRIVETRAIN_TRACKWIDTH_METERS / 2.0, -DRIVETRAIN_WHEELBASE_METERS / 2.0),
            // Back left
            new Translation2d(-DRIVETRAIN_TRACKWIDTH_METERS / 2.0, DRIVETRAIN_WHEELBASE_METERS / 2.0),
            // Back right
            new Translation2d(-DRIVETRAIN_TRACKWIDTH_METERS / 2.0, -DRIVETRAIN_WHEELBASE_METERS / 2.0));

    private final BSGyro m_gyro = new PigeonGyro(Constants.PIGEON_ID);

    // These are our modules. We initialize them in the constructor.
    private final SwerveModule m_frontLeftModule;
    private final SwerveModule m_frontRightModule;
    private final SwerveModule m_backLeftModule;
    private final SwerveModule m_backRightModule;

    private ChassisSpeeds m_chassisSpeeds = new ChassisSpeeds(0.0, 0.0, 0.0);
    private SwerveDriveOdometry m_odometry = new SwerveDriveOdometry(m_kinematics, this.getGyroscopeRotation());
    private final Field2d m_field = new Field2d();

    private final RotationController rotationController = new RotationController();

    public DrivetrainSubsystem() {
        ShuffleboardTab tab = Shuffleboard.getTab("Drivetrain");
        SmartDashboard.putData("Field", m_field);

        m_frontLeftModule = Mk4SwerveModuleHelper.createFalcon500(
                // This parameter is optional, but will allow you to see the current state of
                // the module on the dashboard.
                tab.getLayout("Front Left Module", BuiltInLayouts.kList)
                        .withSize(2, 4)
                        .withPosition(0, 0),
                // This can either be STANDARD or FAST depending on your gear configuration
                Mk4SwerveModuleHelper.GearRatio.L2,
                // This is the ID of the drive motor
                FRONT_LEFT_MODULE_DRIVE_MOTOR,
                // This is the ID of the steer motor
                FRONT_LEFT_MODULE_STEER_MOTOR,
                // This is the ID of the steer encoder
                FRONT_LEFT_MODULE_STEER_ENCODER,
                // This is how much the steer encoder is offset from true zero (In our case,
                // zero is facing straight forward)
                FRONT_LEFT_MODULE_STEER_OFFSET);

        // We will do the same for the other modules
        m_frontRightModule = Mk4SwerveModuleHelper.createFalcon500(
                tab.getLayout("Front Right Module", BuiltInLayouts.kList)
                        .withSize(2, 4)
                        .withPosition(2, 0),
                Mk4SwerveModuleHelper.GearRatio.L2,
                FRONT_RIGHT_MODULE_DRIVE_MOTOR,
                FRONT_RIGHT_MODULE_STEER_MOTOR,
                FRONT_RIGHT_MODULE_STEER_ENCODER,
                FRONT_RIGHT_MODULE_STEER_OFFSET);

        m_backLeftModule = Mk4SwerveModuleHelper.createFalcon500(
                tab.getLayout("Back Left Module", BuiltInLayouts.kList)
                        .withSize(2, 4)
                        .withPosition(4, 0),
                Mk4SwerveModuleHelper.GearRatio.L2,
                BACK_LEFT_MODULE_DRIVE_MOTOR,
                BACK_LEFT_MODULE_STEER_MOTOR,
                BACK_LEFT_MODULE_STEER_ENCODER,
                BACK_LEFT_MODULE_STEER_OFFSET);

        m_backRightModule = Mk4SwerveModuleHelper.createFalcon500(
                tab.getLayout("Back Right Module", BuiltInLayouts.kList)
                        .withSize(2, 4)
                        .withPosition(6, 0),
                Mk4SwerveModuleHelper.GearRatio.L2,
                BACK_RIGHT_MODULE_DRIVE_MOTOR,
                BACK_RIGHT_MODULE_STEER_MOTOR,
                BACK_RIGHT_MODULE_STEER_ENCODER,
                BACK_RIGHT_MODULE_STEER_OFFSET);

        storeContantsInNT();
    }

    /**
     * Stores drivetrain constants in network tables for review
     */
    private void storeContantsInNT() {
        NetworkTableInstance nt = NetworkTableInstance.getDefault();
        NetworkTable table = nt.getTable("drivetrainConstants");

        table.getEntry("MAX_VELOCITY_METERS_PER_SECOND").setDouble(MAX_VELOCITY_METERS_PER_SECOND);
        table.getEntry("MAX_ANGULAR_VELOCITY_RADIANS_PER_SECOND").setDouble(MAX_ANGULAR_VELOCITY_RADIANS_PER_SECOND);
        table.getEntry("MAX_ANGULAR_VELOCITY_DEGREES_PER_SECOND").setDouble(MAX_ANGULAR_VELOCITY_DEGREES_PER_SECOND);
        table.getEntry("MAX_ANGULAR_ACCELERATION_DEGREES_PER_SECOND_SQUARED")
                .setDouble(MAX_ANGULAR_ACCELERATION_DEGREES_PER_SECOND_SQUARED);
        table.getEntry("MAX_ANGULAR_ACCELERATION_RADIANS_PER_SECOND_SQUARED")
                .setDouble(MAX_ANGULAR_ACCELERATION_RADIANS_PER_SECOND_SQUARED);
    }

    public SwerveDriveKinematics getSwerveDriveKinematics() {
        return this.m_kinematics;
    }

    /**
     * Sets the gyroscope angle to zero. This can be used to set the direction the
     * robot is currently facing to the
     * 'forwards' direction.
     */
    public void zeroGyroscope() {
        m_gyro.zeroGyroscope();
        resetOdometry(this.getPose(), new Rotation2d());
    }

    public Rotation2d getGyroscopeRotation() {
        return m_gyro.getGyroscopeRotation();
    }

    public void drive(ChassisSpeeds chassisSpeeds) {
        m_chassisSpeeds = chassisSpeeds;
    }

    @Override
    public void periodic() {
        SwerveModuleState[] states = m_kinematics.toSwerveModuleStates(m_chassisSpeeds);
        setSwerveModuleStates(states);
    }

    public void setSwerveModuleStates(SwerveModuleState[] states) {
        SwerveDriveKinematics.desaturateWheelSpeeds(states, MAX_VELOCITY_METERS_PER_SECOND);

        m_frontLeftModule.set(states[0].speedMetersPerSecond / MAX_VELOCITY_METERS_PER_SECOND * MAX_VOLTAGE,
                states[0].angle.getRadians());
        m_frontRightModule.set(states[1].speedMetersPerSecond / MAX_VELOCITY_METERS_PER_SECOND * MAX_VOLTAGE,
                states[1].angle.getRadians());
        m_backLeftModule.set(states[2].speedMetersPerSecond / MAX_VELOCITY_METERS_PER_SECOND * MAX_VOLTAGE,
                states[2].angle.getRadians());
        m_backRightModule.set(states[3].speedMetersPerSecond / MAX_VELOCITY_METERS_PER_SECOND * MAX_VOLTAGE,
                states[3].angle.getRadians());

        Pose2d newPose = m_odometry.update(this.getGyroscopeRotation(), states);
        m_field.setRobotPose(newPose); // odomotry.getPoseMeters
    }

    public Pose2d getPose() {
        return m_odometry.getPoseMeters();
    }

    /**
     * Sets the robot pose to a new position and current gyro rotation.
     * 
     * @param pose
     */
    public void resetOdometry(Pose2d pose) {
        resetOdometry(pose, this.getGyroscopeRotation());
    }

    /**
     * Sets the robot pose to a new position and specified gyro rotation.
     * 
     * @param pose
     * @param rotation
     */
    public void resetOdometry(Pose2d pose, Rotation2d rotation) {
        m_odometry.resetPosition(pose, rotation);
    }

    public RotationController getRotationController() {
        return this.rotationController;
    }

    /**
     * Utility method to use get the angular outputs in degree units for closed loop
     * rotation
     * 
     * @param setpoint the target angle in degrees
     * @return the angular output in degrees
     */
    public double getRotationOutput(double setpoint) {
        return this.rotationController.calculate(
                this.getGyroscopeRotation().getDegrees(),
                setpoint);
    }

}
