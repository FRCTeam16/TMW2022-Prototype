package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.Subsystems;

public class RunDMSCommand extends CommandBase {
  /** Creates a new RunDMSCommand. */
  public RunDMSCommand() {
    addRequirements(Subsystems.ledSubsystem, Subsystems.drivetrainSubsystem);
  }

  // Called when the command is initially scheduled.
  @Override
  public void initialize() {
    Subsystems.ledSubsystem.startDMS();
  }

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    return Subsystems.ledSubsystem.isStopped();
  }
}
