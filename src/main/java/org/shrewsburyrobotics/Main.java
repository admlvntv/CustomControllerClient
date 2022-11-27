package org.shrewsburyrobotics;

import edu.wpi.first.networktables.BooleanEntry;
import edu.wpi.first.networktables.BooleanPublisher;
import edu.wpi.first.networktables.DoubleArrayEntry;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.RawEntry;
import edu.wpi.first.networktables.RawPublisher;
import java.util.Arrays;
import org.shrewsburyrobotics.controller.CustomController;
import org.shrewsburyrobotics.controller.ScanResults;

public class Main {
  public static void main(String[] args) throws InterruptedException {
    NetworkTableInstance inst = NetworkTableInstance.getDefault();

    inst.setServerTeam(467);
    inst.startDSClient();

    try {
      Thread.sleep(10);
    } catch (InterruptedException ex) {
      System.out.println("interrupted");
      return;
    }

    System.out.println(inst.isConnected());
    NetworkTable controllerTable = inst.getTable("Controller");

    RawEntry commandQueueEntry =
        controllerTable.getRawTopic("CommandQueue").getEntry("raw", new byte[0]);
    DoubleArrayEntry commandQueueLengthEntry =
        controllerTable.getDoubleArrayTopic("CommandQueueLength").getEntry(new double[0]);
    BooleanEntry hasCommandEntry = controllerTable.getBooleanTopic("HasCommand").getEntry(false);

    // TODO: Remove
    RawPublisher responseEntry =
        controllerTable.getRawTopic("response").publish("raw"); // raw bytes
    BooleanPublisher hasResponseEntry =
        controllerTable.getBooleanTopic("hasResponse").publish(); // bool

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    Thread.sleep(200);
                    System.out.println("Shutting down...");
                    inst.close();

                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                  }
                }));

    while (true) {
      ScanResults scan = CustomController.scan();
      if (scan.getValidControllers().length > 0) {
        CustomController controller = scan.getValidControllers()[0];
        controller.open(responseEntry, hasResponseEntry);
        while (controller.isOpen()) {
          if (hasCommandEntry.get(false)) {
            byte[] queue = commandQueueEntry.get(new byte[0]);
            double[] queueLength = commandQueueLengthEntry.get(new double[0]);
            int queuePointer = 0;
            for (double length : queueLength) {
              byte[] data = Arrays.copyOfRange(queue, queuePointer, queuePointer + (int) length);
              controller.send(data);
              queuePointer += (int) length;
            }
            commandQueueEntry.set(new byte[0]);
            commandQueueLengthEntry.set(new double[0]);
            hasCommandEntry.set(false);
          }
        }
      }

      System.out.println("No controller detected, trying again in 1 second");
      Thread.sleep(1000);
    }
  }
}
