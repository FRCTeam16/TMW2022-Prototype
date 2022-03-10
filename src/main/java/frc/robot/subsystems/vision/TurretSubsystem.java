package frc.robot.subsystems.vision;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMax.ControlType;
import com.revrobotics.CANSparkMax.SoftLimitDirection;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Subsystems;
import frc.robot.subsystems.vision.VisionSubsystem.VisionInfo;

public class TurretSubsystem extends SubsystemBase {
  private final CANSparkMax turretMotor = new CANSparkMax(Constants.TURRET_MOTOR_ID, MotorType.kBrushless);
  private final double DEFAULT_TURRET_SPEED = -0.2;
  private final double VISION_THRESHOLD = 1.0;
  private final PIDController visionpPID;
  private final float SOFT_LIMIT = 10.61f;

  enum RunState {
    OpenLoop, ClosedLoop, Vision
  }

  public enum TurretPositions {
    Center(0);

    private final double value;
    private TurretPositions(double value) {
      this.value = value;
    }
  }

  private RunState runState = RunState.OpenLoop;
  private double openLoopSpeed = 0.0;

  private double vision_kP = 0.015;
  private double vision_kI = 0.0;
  private double vision_kD = 0.0;

  private double position_kP = 0.00166;
  private double position_kI = 0.0;
  private double position_kD = 0.0;
  private double targetPosition = 0.0;

  public TurretSubsystem() {
    turretMotor.restoreFactoryDefaults();
    turretMotor.setIdleMode(CANSparkMax.IdleMode.kBrake);
    SmartDashboard.setDefaultNumber("Turret/Open/DefaultSpeed", DEFAULT_TURRET_SPEED);
    SmartDashboard.setDefaultNumber("Turret/Vision/Threshold", VISION_THRESHOLD);

    SmartDashboard.setDefaultNumber("Turret/Vision/P", vision_kP);
    SmartDashboard.setDefaultNumber("Turret/Vision/I", vision_kI);
    SmartDashboard.setDefaultNumber("Turret/Vision/D", vision_kD);

    SmartDashboard.putNumber("Turret/Vision/Threshold", VISION_THRESHOLD);
    visionpPID = new PIDController(vision_kP, vision_kI, vision_kD);
    visionpPID.setSetpoint(0.0);

    SmartDashboard.setDefaultNumber("Turret/Position/P", position_kP);
    SmartDashboard.setDefaultNumber("Turret/Position/I", position_kI);
    SmartDashboard.setDefaultNumber("Turret/Position/D", position_kD);

    turretMotor.setSoftLimit(SoftLimitDirection.kReverse, -SOFT_LIMIT);
    turretMotor.setSoftLimit(SoftLimitDirection.kForward, SOFT_LIMIT);
    enableSoftLimits();
  }

  public void enableSoftLimits() {
    turretMotor.enableSoftLimit(SoftLimitDirection.kReverse, true);
    turretMotor.enableSoftLimit(SoftLimitDirection.kForward, true);
  }

  public void disableSoftLimits() {
    turretMotor.enableSoftLimit(SoftLimitDirection.kReverse, false);
    turretMotor.enableSoftLimit(SoftLimitDirection.kForward, false);
  }

  public void enableVisionTracking() {
    runState = RunState.Vision;
    openLoopSpeed = 0.0;
  }

  public void disableVisionTracking() {
    runState = RunState.OpenLoop;
    openLoopSpeed = 0.0;
  }

  public boolean hasVisionTarget() {
    var visionInfo = Subsystems.visionSubsystem.getVisionInfo();
    return (visionInfo.hasTarget && visionpPID.atSetpoint());
  }

  public void openForward() {
    runState = RunState.OpenLoop;
    openLoopSpeed = SmartDashboard.getNumber("Turret/Open/DefaultSpeed", DEFAULT_TURRET_SPEED);
  }

  public void openBackwards() {
    runState = RunState.OpenLoop;
    openLoopSpeed = -SmartDashboard.getNumber("Turret/Open/DefaultSpeed", DEFAULT_TURRET_SPEED);
  }

  public void openStop() {
    runState = RunState.OpenLoop;
    openLoopSpeed = 0.0;
  }

  public void zeroEncoder() {
    this.turretMotor.getEncoder().setPosition(0.0);
  }

  public double getEncoderPosition() {
    return this.turretMotor.getEncoder().getPosition();
  }

  public void holdTurretPosition() {
    setTurretPosition(getEncoderPosition());
  }

  public void setTurretPosition(TurretPositions position) {
    this.setTurretPosition(position.value);
  }

  public void setTurretPosition(double positionValue) {
    runState = RunState.ClosedLoop;
    this.targetPosition = positionValue;
  }

  @Override
  public void periodic() {
    SmartDashboard.putNumber("Turret/EncPosition", turretMotor.getEncoder().getPosition());
    SmartDashboard.putString("Turret/RunState", runState.name());
    
    if (runState == RunState.ClosedLoop) {
      positionPIDPeriodic();
    } else {
      // Open Loop Approaches
      double speed = 0.0;
      if (runState == RunState.OpenLoop) {
        speed = openLoopSpeed;
      } else if (runState == RunState.Vision) {
        speed = simpleVisionPeriodic();
        // speed = visionPIDPeriodic();
      }
      turretMotor.set(speed);
    } 
  }

  /**
   * Basic p-gain correction
   * @return
   */
  private double simpleVisionPeriodic() {
    double threshold = SmartDashboard.getNumber("Turret/Vision/Threshold", VISION_THRESHOLD);
    double speed = SmartDashboard.getNumber("Turret/Open/DefaultSpeed", DEFAULT_TURRET_SPEED);
    VisionInfo info = Subsystems.visionSubsystem.getVisionInfo();

    if (info.hasTarget && (Math.abs(info.xOffset) > threshold)) {
      double p = SmartDashboard.getNumber("Turret/Vision/P", vision_kP);
      speed = MathUtil.clamp(info.xOffset * -p, -0.4 , 0.4);
    } else {
      // No target or within threshold
      speed = 0.0;
    }
    return speed;
  }

  /**
   * Bug in here somewhere, turret was only moving one direction
   */
  private double visionPIDPeriodic() {
    double maxSpeed = SmartDashboard.getNumber("Turret/Open/DefaultSpeed", DEFAULT_TURRET_SPEED);
    double p = SmartDashboard.getNumber("Turret/Vision/P", vision_kP);
    double i = SmartDashboard.getNumber("Turret/Vision/I", vision_kI);
    double d = SmartDashboard.getNumber("Turret/Vision/D", vision_kD);
    

    if (vision_kP != p) {
      vision_kP = p;
      visionpPID.setP(vision_kP);
    }
    if (vision_kI != i) {
      vision_kI = i;
      visionpPID.setI(vision_kI);
    }
    if (vision_kD != d) {
      vision_kD = d;
      visionpPID.setD(vision_kD);
    }

    double threshold = SmartDashboard.getNumber("Turret/Vision/Threshold", VISION_THRESHOLD);
    visionpPID.setTolerance(threshold);

    double speed = 0.0;
    var visionInfo = Subsystems.visionSubsystem.getVisionInfo();
    SmartDashboard.putBoolean("Turret/Vision/HasTarget", visionInfo.hasTarget);
    SmartDashboard.putNumber("Turret/Vision/xOffset", visionInfo.xOffset);
    
    if (visionInfo.hasTarget) {
      speed = MathUtil.clamp(visionpPID.calculate(visionInfo.xOffset, 0.0), -maxSpeed, maxSpeed);
    }
    return speed;
  }

  private void positionPIDPeriodic() {
    var positionPID = turretMotor.getPIDController();

    double p = SmartDashboard.getNumber("Turret/Position/P", position_kP);
    double i = SmartDashboard.getNumber("Turret/Position/I", position_kI);
    double d = SmartDashboard.getNumber("Turret/Position/D", position_kD);

    if (position_kP != p) {
      position_kP = p;
      positionPID.setP(position_kP);
    }
    if (position_kI != i) {
      position_kI = i;
      positionPID.setI(position_kI);
    }
    if (position_kD != d) {
      position_kD = d;
      positionPID.setD(position_kD);
    }

    positionPID.setReference(targetPosition, ControlType.kPosition);
   
  }
}
