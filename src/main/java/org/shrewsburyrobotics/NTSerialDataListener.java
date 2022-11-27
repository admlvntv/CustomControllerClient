package org.shrewsburyrobotics;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import edu.wpi.first.networktables.BooleanPublisher;
import edu.wpi.first.networktables.RawPublisher;
import org.shrewsburyrobotics.controller.CustomController;

public class NTSerialDataListener implements SerialPortDataListener {
  private boolean isConnected = true;
  private final CustomController controller;
  private final RawPublisher newResponseEntry;
  private final BooleanPublisher newHasResponseEntry;

  public NTSerialDataListener(
      CustomController controller, RawPublisher responseEntry, BooleanPublisher hasResponseEntry) {
    this.controller = controller;
    this.newResponseEntry = responseEntry;
    this.newHasResponseEntry = hasResponseEntry;
  }

  @Override
  public int getListeningEvents() {
    return SerialPort.LISTENING_EVENT_DATA_RECEIVED | SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
  }

  @Override
  public void serialEvent(SerialPortEvent serialPortEvent) {
    switch (serialPortEvent.getEventType()) {
      case SerialPort.LISTENING_EVENT_DATA_RECEIVED:
        this.newResponseEntry.set(serialPortEvent.getReceivedData());
        this.newHasResponseEntry.set(true);
        break;

      case SerialPort.LISTENING_EVENT_PORT_DISCONNECTED:
        isConnected = false;
        controller.close();
        break;

      default:
        break;
    }
  }

  public boolean isConnected() {
    return isConnected;
  }
}
