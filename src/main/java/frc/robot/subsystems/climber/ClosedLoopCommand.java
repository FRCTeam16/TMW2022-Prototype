// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems.climber;

import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.SparkMaxPIDController;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.CommandBase;

public class ClosedLoopCommand extends CommandBase {
  /** Creates a new ClosedLoopCommand. */
  private SparkMaxPIDController m_pidController;
  private RelativeEncoder m_encoder;
  public double kP, kI, kD, kIz, kFF, kMaxOutput, kMinOutput, maxRPM, maxVel, minVel, maxAcc, allowedErr;
  private final ClimberSubsystem climberSystem;

  public ClosedLoopCommand(ClimberSubsystem climberSubsystem) {
    // Use addRequirements() here to declare subsystem dependencies.
    this.climberSystem = climberSubsystem;
    var climberMotor = climberSubsystem.getClimberMotor();
    m_pidController = climberMotor.getPIDController();
    m_encoder = climberMotor.getEncoder();

    kP = 5e-5;
    kI = 1e-6;
    kD = 0;
    kIz = 0;
    kFF = 0.000156;
    kMaxOutput = 1;
    kMinOutput = -1;
    maxRPM = 5700;

    // Smart Motion Coefficients
    maxVel = 2000; // rpm
    maxAcc = 1500;

    // set PID coefficients
    m_pidController.setP(kP);
    m_pidController.setI(kI);
    m_pidController.setD(kD);
    m_pidController.setIZone(kIz);
    m_pidController.setFF(kFF);
    m_pidController.setOutputRange(kMinOutput, kMaxOutput);

    int smartMotionSlot = 0;
    // limits the Velocity in RPM of the PID
    m_pidController.setSmartMotionMaxVelocity(maxVel, smartMotionSlot);

    // lower bound in RPM
    m_pidController.setSmartMotionMinOutputVelocity(minVel, smartMotionSlot);

    // limit the acceleration in RPM squared of the PID
    m_pidController.setSmartMotionMaxAccel(maxAcc, smartMotionSlot);

    // set the max allowed error for the PID
    m_pidController.setSmartMotionAllowedClosedLoopError(allowedErr, smartMotionSlot);

    // display PID coefficients on SmartDashboard
    SmartDashboard.putNumber("Climber/P Gain", kP);
    SmartDashboard.putNumber("Climber/I Gain", kI);
    SmartDashboard.putNumber("Climber/D Gain", kD);
    SmartDashboard.putNumber("Climber/I Zone", kIz);
    SmartDashboard.putNumber("Climber/Feed Forward", kFF);
    SmartDashboard.putNumber("Climber/Max Output", kMaxOutput);
    SmartDashboard.putNumber("Climber/Min Output", kMinOutput);

    // display Smart Motion coefficients
    SmartDashboard.putNumber("Climber/Max Velocity", maxVel);
    SmartDashboard.putNumber("Climber/Min Velocity", minVel);
    SmartDashboard.putNumber("Climber/Max Acceleration", maxAcc);
    SmartDashboard.putNumber("Climber/Allowed Closed Loop Error", allowedErr);
    SmartDashboard.putNumber("Climber/Set Position", 0);
    SmartDashboard.putNumber("Climber/Set Velocity", 0);
  }

  // Called when the command is initially scheduled.
  @Override
  public void initialize() {}

  // Called every time the scheduler runs while the command is scheduled.
  @Override
  public void execute() {
     // read PID coefficients from SmartDashboard
     double p = SmartDashboard.getNumber("Climber/P Gain", 0);
     double i = SmartDashboard.getNumber("Climber/I Gain", 0);
     double d = SmartDashboard.getNumber("Climber/D Gain", 0);
     double iz = SmartDashboard.getNumber("Climber/I Zone", 0);
     double ff = SmartDashboard.getNumber("Climber/Feed Forward", 0);
     double max = SmartDashboard.getNumber("Climber/Max Output", 0);
     double min = SmartDashboard.getNumber("Climber/Min Output", 0);
     double maxV = SmartDashboard.getNumber("Climber/Max Velocity", 0);
     double minV = SmartDashboard.getNumber("Climber/Min Velocity", 0);
     double maxA = SmartDashboard.getNumber("Climber/Max Acceleration", 0);
     double allE = SmartDashboard.getNumber("Climber/Allowed Closed Loop Error", 0);
 
     // if PID coefficients on SmartDashboard have changed, write new values to
     // controller
     if ((p != kP)) {
       m_pidController.setP(p);
       kP = p;
     }
     if ((i != kI)) {
       m_pidController.setI(i);
       kI = i;
     }
     if ((d != kD)) {
       m_pidController.setD(d);
       kD = d;
     }
     if ((iz != kIz)) {
       m_pidController.setIZone(iz);
       kIz = iz;
     }
     if ((ff != kFF)) {
       m_pidController.setFF(ff);
       kFF = ff;
     }
     if ((max != kMaxOutput) || (min != kMinOutput)) {
       m_pidController.setOutputRange(min, max);
       kMinOutput = min;
       kMaxOutput = max;
     }
     if ((maxV != maxVel)) {
       m_pidController.setSmartMotionMaxVelocity(maxV, 0);
       maxVel = maxV;
     }
     if ((minV != minVel)) {
       m_pidController.setSmartMotionMinOutputVelocity(minV, 0);
       minVel = minV;
     }
     if ((maxA != maxAcc)) {
       m_pidController.setSmartMotionMaxAccel(maxA, 0);
       maxAcc = maxA;
     }
     if ((allE != allowedErr)) {
       m_pidController.setSmartMotionAllowedClosedLoopError(allE, 0);
       allowedErr = allE;
     }
 
     double setPoint, processVariable;
     
     
       setPoint = SmartDashboard.getNumber("Climber/Set Position", 0);
       /**
        * As with other PID modes, Smart Motion is set by calling the
        * setReference method on an existing pid object and setting
        * the control type to kSmartMotion
        */
       m_pidController.setReference(setPoint, CANSparkMax.ControlType.kSmartMotion);
       processVariable = m_encoder.getPosition();
     
 
     SmartDashboard.putNumber("Climber/SetPoint", setPoint);
     SmartDashboard.putNumber("Climber/Process Variable", processVariable);
     SmartDashboard.putNumber("Climber/Output", climberSystem.getClimberMotor().getAppliedOutput());
 
     
  }

  // Called once the command ends or is interrupted.
  @Override
  public void end(boolean interrupted) {}

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    return false;
  }
}
