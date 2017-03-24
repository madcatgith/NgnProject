package Preload;

import static Preload.Update.CheckLocalDB;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;
import mail.SendMail;
import ngn.Ngn;
import ngn.text.Paths;
import ngn.text.Text;
import static ngn.view.BeforeStart.BSLoadingText;

/**
 *
 * @author Офис
 */
public class PortCheck {

    static String[] portNames;
    static String PN;
    static SerialPort PortToCheck;
    public static String GSPort;
    public static String KPPort;
    static String data;
    static Integer NumberOfSymbols = 0;

    public static void PortCheck() {

        portNames = SerialPortList.getPortNames();
        for (String portName : portNames) {
            System.out.println(portName);
            PN = portName;
            PortToCheck = new SerialPort(portName);
            try {
                PortToCheck.openPort();
                PortToCheck.purgePort(SerialPort.PURGE_RXCLEAR);
                PortToCheck.purgePort(SerialPort.PURGE_TXCLEAR);
                PortToCheck.addEventListener(new PortListener());
                DoWithPort("GS");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
                BSLoadingText.setText(Text.h1CheckFacilities);

                DoWithPort("KP");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
            } catch (SerialPortException ex) {
                System.out.println(ex);
            } finally {
                try {
                    PortToCheck.closePort();
                } catch (SerialPortException ex) {
                    System.out.println(ex);
                }
            }
        }
        if (GSPort != null) {
            if (KPPort != null) {
                BSLoadingText.setText(Text.PortsON);
                if (BackendTimers.InternetCheck) {
                    Threads.LOCALDB();
                } else if (CheckLocalDB()) {
                    BSLoadingText.setText(Text.LDBdone);
                }
            } else {
                BSLoadingText.setText(Text.KPPortOff);
                Ngn.StatusBar(Paths.KEYPADOFF, 2);
            }
        } else {
            BSLoadingText.setText(Text.GSPortOff);
            Ngn.StatusBar(Paths.PISTOLOFF, 3);
        }
    }

    public static void DoWithPort(String port) {
        try {
            if (port.equals("KP")) {
                System.out.println("2400");
                NumberOfSymbols = 20;
                PortToCheck.setParams(2400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                if (PortToCheck.isOpened()){
                    PortToCheck.writeString("programming");
                }
                //PortToCheck.writeString("00");
            }
            if (port.equals("GS")) {
                System.out.println("9600");
                NumberOfSymbols = 12;
                PortToCheck.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_MARK);
                PortToCheck.setEventsMask(SerialPort.MASK_RXCHAR);
                if (PortToCheck.isOpened()){
                    PortToCheck.writeString("@10510045#");
                }
            }
        } catch (SerialPortException ex) {
            System.out.println(ex);
        }
    }

    public static void ClosePorts() {
        for (String portName : portNames) {
            PortToCheck = new SerialPort(portName);
            if (PortToCheck.isOpened()) {
                try {
                    PortToCheck.closePort();
                } catch (SerialPortException ex) {
                    System.out.println(ex);
                }
            }
        }
    }

    private static class PortListener implements SerialPortEventListener {

        @Override
        public void serialEvent(SerialPortEvent spe) {
            if (spe.isRXCHAR() && spe.getEventValue() != 0) {
                try {
                    data = PortToCheck.readString(NumberOfSymbols);
                    //System.out.println(data);
                    if (data.contains("V")) {
                        KPPort = PN;
                        Ngn.StatusBar(Paths.KEYPADON, 2);
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            System.out.println(e);
                        }
                        PortToCheck.writeString("1Q");
                    }
                    if (data.contains("@")) {
                        GSPort = PN;
                        Ngn.StatusBar(Paths.PISTOLON, 3);
                    }
                } catch (SerialPortException ex) {
                    System.out.println(ex);
                }
            }

        }
    }
}
