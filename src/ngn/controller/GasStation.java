package ngn.controller;

import Preload.PortCheck;
import java.awt.event.ActionEvent;
import java.util.Locale;
import javax.swing.Timer;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import mail.SendMail;
import ngn.Ngn;
import static ngn.controller.Timers.Success;
import ngn.model.DB;
import ngn.text.Config;
import ngn.text.Paths;
import ngn.text.Text;
import ngn.view.Bye;
import ngn.view.Info;
import ngn.view.Litrs;
import ngn.view.Work;

/**
 *
 * @author Офис
 */
public class GasStation {

    public static boolean PistolStatus = true;
    public static boolean TestInGSSignal = false;
    public static boolean TestOutGSSignal = false;
    public static boolean Litrcomsent = false;
    public static boolean GasOFF = false;

    private static SerialPort KolonkaCOM3;
    private static Boolean uponce=false;
    private static String laststate="";
    static int komanda;
    static Timer KolonkaStart;
    static Timer KolonkaStartNotWorks;
    static Timer ZaderzkaDoza;
    static String PolozheniePistoleta;
    static String SchetLitrov;
    static String WorkingCardCode; //MoneySchetLitrov
    static String OtvetPoDoze;
    static String OtvetKolonki;

    public GasStation() {
        GasStationSettings();
        TimerKolonkaStart();
    }

    public static void GasStationSettings() {
        if (KolonkaCOM3!=null){
        if (KolonkaCOM3.isOpened()){
            try{
                KolonkaCOM3.closePort();
            }
            catch(Exception exc){
                System.out.println(exc);
            }
        }
        }
        KolonkaCOM3 = new SerialPort(PortCheck.GSPort);
        try {
            if (KolonkaCOM3.isOpened()){
                KolonkaCOM3.purgePort(SerialPort.PURGE_RXCLEAR);
                KolonkaCOM3.purgePort(SerialPort.PURGE_TXCLEAR);
                KolonkaCOM3.closePort();
            }
            KolonkaCOM3.openPort();
            KolonkaCOM3.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_MARK);
            KolonkaCOM3.setEventsMask(SerialPort.MASK_RXCHAR);
            KolonkaCOM3.addEventListener(new EventListener());
        } catch (SerialPortException ex) {
            System.out.println(ex.getExceptionType());
        }
    }

    public static void TimerZaderzkaDoza(String komDoza) {
        ZaderzkaDoza = new Timer(600, (ActionEvent e) -> {
            try {
                ZaderzkaDoza.stop();
                komanda = 1;
                Litrcomsent=true;
                if (KolonkaCOM3.isOpened()){
                    KolonkaCOM3.writeString(komDoza);// Отправляем на колонку количество литров на отдачу
                }
                /*System.out.println(komDoza);*/
            } catch (SerialPortException ex) {
                System.out.println(ex);
               
            }
        });
        KolonkaStart.stop();
        ZaderzkaDoza.restart();  // Запуск команды "ЗАПРОС СОСТОЯНИЯ"
    }

    public static void TimerKolonkaStart() {
        Litrcomsent=false;
        Timers.stuck_counter=0;
        System.out.println("KOLONKA START WORK!");
        KolonkaStart = new Timer(600, (ActionEvent e) -> {
            try {
                if (KolonkaCOM3.isOpened()){
                    TestInGSSignal = KolonkaCOM3.writeString("@10510045#");
                    komanda = 0;
                }
                if (!TestInGSSignal && !TestOutGSSignal) {
                    Ngn.StatusBar(Paths.PISTOLOFF, 3);
                    PistolStatus = false;
                    GasOFF=true;
                    KolonkaStart.stop();
                    KolonkaStartNotWorks.restart();
                } else {
                    Ngn.StatusBar(Paths.PISTOLON, 3);
                    GasOFF=false;
                    PistolStatus = true;
                }
                TestOutGSSignal = false;
            } catch (SerialPortException ex) {
                Ngn.StatusBar(Paths.PISTOLOFF, 3);
                PistolStatus = false;
                System.out.println(ex.getExceptionType());
                Config.detaillog(String.valueOf(ex) + " Gas Station error! " + DB.MODULENAME);
                
            }
        });
        KolonkaStart.restart();  // Запуск команды "ЗАПРОС СОСТОЯНИЯ"

        KolonkaStartNotWorks = new Timer(1000, (ActionEvent e) -> {
            GasStationSettings();
            if (KolonkaCOM3.isOpened()) {
                KolonkaStart.restart();
                KolonkaStartNotWorks.stop();
            }
        });
    }

    private static class EventListener implements SerialPortEventListener {

        @Override
        public void serialEvent(SerialPortEvent event) {
            TestOutGSSignal = true;
            if (event.isRXCHAR() && event.getEventValue() != 0) {
                String data = "";
                String oneSymbol;
                try {
                    String StartWork = KolonkaCOM3.readString(1);
                    if (StartWork.indexOf("@") == 0) {
                        // Get data //
                        do {
                            oneSymbol = KolonkaCOM3.readString(1);
                            data += oneSymbol;
                        } while (!"#".equals(oneSymbol));
                        OtvetKolonki = StartWork + data;
                        Integer dataLenghth = OtvetKolonki.length();
                        String dataOffCrx = new String(OtvetKolonki.toCharArray(), 0, dataLenghth - 3);
                        String controlNumber = new String(OtvetKolonki.toCharArray(), dataLenghth - 3, 2);
                        // Check data //
                        int decnum = dataOffCrx.length();
                        char[] decimal = new char[decnum];
                        int crc = 0;
                        for (int col = 0; col < decnum; col++) {
                            decimal[col] = dataOffCrx.charAt(col);
                            crc ^= decimal[col]; // CRC (контрольная сумма)
                        }
                        String pihex = Integer.toHexString(crc);
                        if (pihex.equals(controlNumber)) {
                            //System.out.println(OtvetKolonki);
                        }
                        if (!laststate.equals(OtvetKolonki)){
                            //System.out.println(OtvetKolonki);
                            laststate=OtvetKolonki;
                        }
                        
                        //Config.detaillog(OtvetKolonki);
                            
                        if (komanda == 0) {
                            switch (OtvetKolonki.indexOf("#")) {
                                case 11:
                                    if (OtvetKolonki.equals("@0151010044#")) {
                                        PolozheniePistoleta = "ПИСТОЛЕТ ПОВЕШЕН";
                                        if (uponce){
                                            //Config.detaillog("ПИСТОЛЕТ ПОВЕШЕН ХЕШ:"+OtvetKolonki);
                                            uponce=false;}
                                    } else {
                                        PolozheniePistoleta = "ПОВЕСЬТЕ ПИСТОЛЕТ!";
                                    }
                                    break;
                                //String poluchenieOtcheta = KolonkaCOM3.readString(8);
                                //String schetLitrov = proverkaSvyazi + poluchenieOtcheta;
                                ////////////////////////////Читаем 19 байт и проверяем наличие решетки в конце//////////////////////////////////////
                                case 19:
                                    Litrcomsent=false;
                                    String hexNUM = new String(OtvetKolonki.toCharArray(), 9, 8);
                                    double litrbez = Integer.decode("0x" + hexNUM) / 100.0;
                                    PolozheniePistoleta = "ИДЕТ ПРОЦЕСС ЗАПРАВКИ...";
                                    SchetLitrov = String.valueOf(litrbez);
                                    //Config.detaillog("ИДЕТ ПРОЦЕСС ЗАПРАВКИ... " +"Литров:"+String.valueOf(litrbez)+" ХЕШ:"+OtvetKolonki);
                                    uponce=true;
                                    //MoneySchetLitrov = String.format(Locale.ENGLISH, "%.2f", Variables.customerPrice * litrbez);
                                    break;
                                default:
                                    komanda = 1;
                                    break;
                            }
                        }
                        
                        
                        
                        if (komanda == 1) {
                            try {
                                //OtvetPoDoze = KolonkaCOM3.readString(11);
                                
                                if (OtvetKolonki.equals("@0144010141#")) {
                                    KolonkaCOM3.writeString("@1047010142#"); //PUSK
                                    TimerKolonkaStart();
                                } else if (OtvetKolonki.equals("@0145015440#")) {
                                    System.out.println("Gotcha");
                                    
                                    Info.ErrorMassage.setText(Text.pistol);
                                    ChangePanel.ShowPanel(Info.InfoMassage);
                                    Litrs.LitrsInput.setText("");
                                    Work.SchetLitrov.setText("");
                                    Timers.ForceMajor.stop();
                                    Success();
                                    GasStation.CustomerInfoToZero();
                                    ToZero.CustomerInfo();
                                    StopStartCom3(true);
                                    Config.detaillog("Fast pistoll on/off error");
                                }else{
                                    //ZaderzkaDoza.restart();
                                    System.out.println(OtvetKolonki);
                                }
                                //
                            } catch (SerialPortException ex) {
                                SendMail.sendEmail(String.valueOf(ex), "Gas Station error! " + DB.MODULENAME, false);
                                System.out.println(ex);
                                Config.detaillog(String.valueOf(ex) + " Gas Station error! " + DB.MODULENAME);
                            }
                        }
                    }
                } catch (SerialPortException ex) {
                    SendMail.sendEmail(String.valueOf(ex), "Gas Station error! " + DB.MODULENAME, false);
                    System.out.println(ex);
                    Config.detaillog(String.valueOf(ex) + " Gas Station error! " + DB.MODULENAME);
                }
            }
        }
    }

    public static void StopStartCom3(boolean value) {
        if (value) {
            KolonkaStart.stop();
        } else {
            KolonkaStart.restart();
        }
    }
    
    public static void ReopenCom3(){
        try{
            if (KolonkaCOM3.isOpened()){
                KolonkaCOM3.purgePort(SerialPort.PURGE_RXCLEAR);
                KolonkaCOM3.purgePort(SerialPort.PURGE_TXCLEAR);
                KolonkaCOM3.closePort();
                KolonkaCOM3.openPort();
            }else{
                KolonkaCOM3.openPort();
            }
        }
        catch(Exception ex){
            System.out.println(ex);
        }
    }
    
    public static boolean CheckConnGS(){
        try{
            boolean test=KolonkaCOM3.writeString("@10510045#");
            if (test){
                return true;
            }
        }
        catch(Exception ex){
            System.out.println(ex);
        }
        return false;
    }

    public static void CustomerInfoToZero() {
        SchetLitrov = "";
        WorkingCardCode = "";
        //MoneySchetLitrov = "";
        //Work.MoneySchetLitrov.setText(GasStation.MoneySchetLitrov);
    }

    public static void KomandaStop() {
        try {
            KolonkaCOM3.writeString("@015801014C#");
        } catch (SerialPortException ex) {
            System.out.println(ex);
        }
    }
}