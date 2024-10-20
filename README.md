# 6364 - 2023/2024 Season Code

This is the code used by 6364 during the 2023-2024 FRC season.
This project is **CLOSED SOURCE**, at least until the end of season.
If you have been granted access to this repo, do not share any of the source unless necessary (e.g asking for help in Chief Delphi).

## Contributing
See [contributing.md](contributing.md) on how to contribute to the project. It contains instructions on how to download all the necessary software

## Automation

No Note Detected in Indexer - angler down
Note Detected in Indexer - angler up

## Controls
| Button      | Action    | Command                                          |
| ----------- | --------- | ------------------------------------------------ |
| RT          | whileTrue | Intake Deploy & Intake & indexer load            |
| RT          | onFalse   | Intake Stop & Indexer Stop & deployer stays down |
| RB          | onTrue    | deployer Up                                      |
| LB          | onTrue    | Shoot                                            |
| D-Up        | onTrue    | climber up                                       |
| D-Down      | onTrue    | climber down                                     |

## CAN IDs
| Device      | CAN ID      | Bus         |
| ----------- | ----------- | ----------- |
| FL Drive    | 3           | drivecan    |
| FL Steering | 7           | drivecan    |
| FL CANcoder | 11          | drivecan    |
| FR Drive    | 1           | drivecan    |
| FR Steering | 5           | drivecan    |
| FR CANcoder | 9           | drivecan    |
| BL Drive    | 2           | drivecan    |
| BL Steering | 6           | drivecan    |
| BL CANcoder | 10          | drivecan    |
| BR Drive    | 4           | drivecan    |
| BR Steering | 8           | drivecan    |
| BR CANcoder | 12          | drivecan    |
| Pigeon 2.0  | 13          | drivecan    |

#### Subsystems
| Device        | CAN ID      | Bus         |
| ------------- | ----------- | ----------- |
| Upper Shooter | 14          | rio         |
| Lower Shooter | 15          | rio         |
| Indexer       | 19          | rio         |
| Climber       | 16          | rio         |
| Angler        | 17          | rio         |
| Deployer      | 18          | rio         |
| Intake        | 20          | rio         |

## Phoenix Pro
We use the Phoenix Pro vendordep for interacting with CTRE hardware, such as cancoders and TalonFX motors. It has several advantageous over Phoenix 6:

1. FOC commutation. 15% more power on motors
2. FOC commutation (again). Uses current instead of voltage to control velocity. The relationship is linear, unlike voltage which is only mostly linear.
3. TalonFX - CanCoder sensor fusion. More accurate swerve module positioning without compromising on closed loop controller polling rate.
4. Dynamic Motion Magic - change motion magic parameters while in motion. Runs on motor hardware, so it has a blazlingly fast polling rate
5. Device timestamps - gives the ability to compensate for latency for more accurate odometry

## CANivores
We use CANivores for all drive/steer motors and encoders. A swerve module cancoder will always be on the same CANivore bus as the drive and steer motors of that module, to minimize latency for sensor fusion.

CANivores enable us to poll sensors much faster than the RIO CAN bus (250hz vs 100hz). However, it is limited on how many devices can be on a single bus. Therefore we use 2 CANivores, one for each side of the drivetrain. All other motors and sensors are on the RIO can bus. We would have used a third CANivore, but the RIO only has 2 USB ports, so we are limited to a maximum of 2 CANivores.

## Swerve Module Control
We use TalonFX motors on the swerve modules. Each module has a steering motor and a driving module. These use different methods of control.

__**Steering motors: Position Motion Magic**__
Position Motion Magic is just a fancy motion profile that is calculated by the motor itself. It uses a positional PID, along with the other constants (kV, kA, etc.)

__**Drive motors: Open Loop Voltage / ClosedLoopVelociy (or simply Velocity)**__
The drive motors use Open Loop Voltage during teleop, so the driver has more control over the motors. Closed Loop Velocity is used during autonomous, where high consistency is necessary.
Closed Loop Velocity uses a PID and the other constants (kV, kA, etc.) including kF. Tuning the Velocity controller does take the longest time to tune, but it will improve performance during autonomous.

## Motor Controller Policy
Unless absolutely necessary, all motors should be using their onboard PID controllers. It reduces CAN bus utilization and performs better thanks to the 1000hz polling rate.

## Path Planning
We use [Choreo](https://sleipnirgroup.github.io/Choreo) for path planning. It performs better than the WPILib path planner, thanks to features such as numerical optimization.
