package ngn.controller;

import Preload.BackendTimers;
import Preload.LocalDB;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import ngn.text.Text;
import javax.swing.Timer;
import jssc.SerialPort;
import jssc.SerialPortException;
import mail.SendMail;
import ngn.model.DB;
import ngn.text.Paths;
import ngn.view.*;
import static ngn.view.Footer.CardDate;
import java.io.*;
import static ngn.controller.GasStation.StopStartCom3;
import ngn.text.Config;

/**
 *
 * @author Офис
 */
public class Timers {

    static Timer errorCardLength;
    static Timer errorCard;
    static Timer errorStation;
    static Timer errorPin;
    static Timer errorLitrs;
    static Timer Success;
    static Timer WaitForClient;
    static Timer ChangeSecondsValue;
    static Timer ForceMajor;
    static Timer WaitForServer;
    static Timer TryToConnect;
    static Timer ServerWaiting;
    static Timer KeyPadWorks;
    static Timer KeyPadNotWorks;
    static Timer DateTime;

    private static final int TIMER_TIME = 1000;
    private static final int ERRORTIME = 3000;
    private static final int SUCCESSTIME = 6000;
    private static final int WAIT_TIME = 60000;
    private static int SECONDSVALUE = 15;
    
    public static int stuck_counter=0;

    SimpleDateFormat ft = new SimpleDateFormat("HH:mm:ss");

    public Timers() {

        WaitForClient = new Timer(WAIT_TIME, (ActionEvent e) -> {
            Wait.WaitingSeconds.setText(Text.h1ClickIfUHere);
            ChangePanel.ShowPanel(Wait.Waiting);
            ToZero.FocusOff();
            Wait.Waiting.setFocusable(true);
            Wait.Waiting.requestFocusInWindow();
            WaitForClient.stop();
            ChangeSecondsValue();
        });

        errorCard = new Timer(ERRORTIME, (ActionEvent e) -> {
            ChangePanel.ShowPanel(Card.EnterCard);
            ChangePanel.FocusPassword(Card.CardCode);
            Card.CardCode.setText("");
            Info.InfoMassage.setFocusable(false);
            errorCard.stop();
        });
        
        errorStation = new Timer(ERRORTIME, (ActionEvent e) -> {
            ChangePanel.ShowPanel(Card.EnterCard);
            ChangePanel.FocusPassword(Card.CardCode);
            Card.CardCode.setText("");
            Pin.PinCode.setText("");
            Info.InfoMassage.setFocusable(false);
            errorStation.stop();
        });

        errorPin = new Timer(ERRORTIME, (ActionEvent e) -> {
            ChangePanel.ShowPanel(Pin.EnterPin);
            ChangePanel.FocusPassword(Pin.PinCode);
            Pin.PinCode.setText("");
            errorPin.stop();
        });

        errorLitrs = new Timer(ERRORTIME, (ActionEvent e) -> {
            ChangePanel.ShowPanel(Litrs.EnterLitrs);
            ChangePanel.FocusLitrsInput();
            ToZero.TextOff();
            errorLitrs.stop();
        });

        ChangeSecondsValue = new Timer(TIMER_TIME, (ActionEvent e) -> {
            if (SECONDSVALUE <= 0) {
                ChangePanel.ShowPanel(Card.EnterCard);
                ChangePanel.FocusPassword(Card.CardCode);
                ToZero.TextOff();
                Wait.Waiting.setFocusable(false);
                ChangeSecondsValue.stop();
            } else {
                SECONDSVALUE--;
                Wait.WaitingSeconds.setText(Text.WaitingText + SECONDSVALUE + " секунд.</p>");
            }
        });

        ForceMajor = new Timer(600, (ActionEvent e) -> { // Через секунду начало обработки процесса заправки
            
            Work.PolozheniePistoleta.setText(Text.rememberAboutPistol);
            Work.SchetLitrov.setText(GasStation.SchetLitrov);
            Config.last_transaction(Variables.code+":"+GasStation.SchetLitrov);
            //Work.MoneySchetLitrov.setText(GasStation.MoneySchetLitrov);
            //ngn.text.Config.detaillog(GasStation.PolozheniePistoleta);
            //System.out.println(GasStation.PolozheniePistoleta);
            //System.out.println(stuck_counter);
            try{
            if (GasStation.PolozheniePistoleta.equals(Text.pistolOnGS)) { // Ждем повешанья пистолета после заправки
                // Если что, проблему искать тут. Форс мажор таймер.
                
                if (Work.SchetLitrov.getText().equals("")) { //Исправление бага "моментальное повешанье пистолета"
                    Work.SchetLitrov.setText("0.0");
                }
                ForceMajor.stop();
                double litriDouble = Double.valueOf(Work.SchetLitrov.getText());
                double formatnewln = Double.valueOf(Variables.litrnum) - litriDouble;
                // NEWLN - Разница между литрами на карте и заправленными
                Variables.leftlitr = String.format(Locale.ENGLISH, "%.2f", litriDouble);
                Variables.newln = String.format(Locale.ENGLISH, "%.2f", formatnewln);
                // Date 
                java.util.Date udate = new java.util.Date();
                Variables.sdate = new java.sql.Timestamp(udate.getTime());
                // Transaction Data
                String[] Transaction = new String[]{
                    String.valueOf(Variables.BalanceOneCardZero),
                    String.valueOf(Variables.customerId),
                    Variables.name,
                    Variables.code,
                    Variables.leftlitr,
                    String.valueOf(Variables.sdate),
                    String.valueOf(Variables.couponId)
                };
                /*
                //Try to send transaction with internet
                if (BackendTimers.InternetCheck) {
                } else {
                    SendMail.sendEmail("No Internet", "Wasn't Internet, when trying to send transaction, after client put on gas pistol! " + DB.MODULENAME);
                    System.out.println("No Internet");
                }
                 */
                if (Variables.cardCode.equals(Text.HFP)) {
                    ChangePanel.ShowPanel(Bye.GoodBye);
                    Litrs.LitrsInput.setText("");
                    Work.SchetLitrov.setText("");
                    Success();
                    GasStation.CustomerInfoToZero();
                    ToZero.CustomerInfo();
                } else {
                    WriteWI.CounterWriter(litriDouble);// Записываем отданные литры в счетчик
                    GasStation.getGasCounter(false);
                    WriteWI.Write(Transaction, Paths.TRANSACTIONPATH, true);// Записываем операцию в FillingData.txt
                    LocalDB.WriteToLocalDB();// Записываем в LocalDB
                    ChangePanel.ShowPanel(Bye.GoodBye);
                    Litrs.LitrsInput.setText("");
                    Work.SchetLitrov.setText("");
                    Success();
                    GasStation.CustomerInfoToZero();
                    ToZero.CustomerInfo();
                }
            }
            
            if ((GasStation.PolozheniePistoleta.equals("ПОВЕСЬТЕ ПИСТОЛЕТ!"))&&(Work.Working.isVisible())&&(GasStation.Litrcomsent)){
                if (Work.SchetLitrov.getText()==null){
                    stuck_counter++;
                }
                else if (Work.SchetLitrov.getText().equals("")){
                    stuck_counter++;
                }
                
                if (stuck_counter>10){
                    stuck_counter=0;
                    ngn.text.Config.detaillog("Ошибка связи с колонкой в момент пуска насоса!");
                    Info.ErrorMassage.setText("Обрыв связи с колонкой");
                    ChangePanel.ShowPanel(Info.InfoMassage);
                    Litrs.LitrsInput.setText("");
                    Work.SchetLitrov.setText("");
                    Timers.ForceMajor.stop();
                    //GasStation.ReopenCom3();
                    GasStation.StopStartCom3(true);
                    Success();
                    GasStation.CustomerInfoToZero();
                    ToZero.CustomerInfo();
                    if (!((GasStation.KolonkaStart.isRunning())||(GasStation.KolonkaStartNotWorks.isRunning()))){
                        GasStation.TimerKolonkaStart();
                    }
                    //GasStation.TimerKolonkaStart();
                }
            }
//////////////////////////////////////////KONETS KOLONKI/////////////////////////////////////////////////
            }
            catch (Exception ex){
                ngn.text.Config.detaillog("Exception in ForseMajor Timer:"+ex+"");
                System.out.println("Exception in ForseMajor Timer:"+ex+"");
            }
        });

        Success = new Timer(SUCCESSTIME, (ActionEvent e) -> {
            ChangePanel.ShowPanel(Card.EnterCard);
            ChangePanel.FocusPassword(Card.CardCode);
            ToZero.TextOff();
            GasStation.SchetLitrov = "";
            GasStation.WorkingCardCode = "";
            //GasStation.MoneySchetLitrov = "";
            Success.stop();
        });

        ServerWaiting = new Timer(1000, (ActionEvent p) -> {
            if (SECONDSVALUE <= 0) {
                ServerWaiting.stop();
                GasStation.StopStartCom3(false);
                noInternetInEnd(
                        Variables.newln,
                        Variables.code,
                        Variables.name,
                        Variables.leftlitr,
                        Variables.sdate
                );
            } else {
                SECONDSVALUE--;
                Info.ErrorMassage.setText(Text.ServerText + SECONDSVALUE + " СЕКУНД.</p>");
                ServerWaiting.restart();
            }
        });

        WaitForServer = new Timer(1000, (ActionEvent e) -> {
            if (SECONDSVALUE <= 0) {
                ChangePanel.ShowPanel(Card.EnterCard);
                ChangePanel.FocusPassword(Card.CardCode);
                ToZero.TextOff();
                GasStation.StopStartCom3(false);
                WaitForServer.stop();
            } else {
                SECONDSVALUE--;
                Info.ErrorMassage.setText(Text.ServerText + SECONDSVALUE + " СЕКУНД.</p>");
                WaitForServer.restart();
            }
        });

        TryToConnect = new Timer(15000, (ActionEvent e) -> {
            if (BackendTimers.InternetCheck) {
                GasStation.StopStartCom3(false);
                ChangePanel.ShowPanel(Card.EnterCard);
                ChangePanel.FocusPassword(Card.CardCode);
                ToZero.TextOff();
                TryToConnect.stop();
            } else {
                TryToConnect.restart();
            }
        });

        KeyPadWorks = new Timer(1000, (ActionEvent e) -> {
            try {
                Boolean testSignal = KeyPad.KeyPadCOM4.writeString("00");
                if (!testSignal) {
                    KeyPadWorks.stop();
                    KeyPadNotWorks.restart();
                }
            } catch (SerialPortException ex_kpw) {
                System.out.println(ex_kpw);
            }
        });

        KeyPadNotWorks = new Timer(1000, (ActionEvent f) -> {
            try {
                KeyPad.KeyPadCOM4 = new SerialPort("COM4");
                try {
                    KeyPad.KeyPadCOM4.openPort();
                    KeyPad.KeyPadCOM4.setParams(2400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                    KeyPad.KeyPadCOM4.setEventsMask(SerialPort.MASK_RXCHAR);
                    KeyPad.KeyPadCOM4.addEventListener(new KeyPad.EventListener());
                } catch (SerialPortException ex) {
                    System.out.println(ex);
                }
                if (KeyPad.KeyPadCOM4.writeString("00")) {
                    KeyPadWorks.restart();
                    KeyPadNotWorks.stop();
                }
            } catch (SerialPortException ex_kpdw) {
                System.out.println(ex_kpdw);
            }
        });

        DateTime = new Timer(1000, (ActionEvent f) -> {
            CardDate.setText(Text.DatePadding + ft.format(new Date()));
        });
    }

    public static void errorCardLength() {
        errorCardLength.restart();
    }

    public static void errorCard() {
        Info.ErrorMassage.setText(Text.cardvalid);
        ChangePanel.ShowPanel(Info.InfoMassage);
        Info.InfoMassage.setFocusable(true);
        WaitForClient.restart();
        errorCard.restart();
    }
    
    public static void errorStation() {
        Info.ErrorMassage.setText("Ошибка связи с колонкой");
        ChangePanel.ShowPanel(Info.InfoMassage);
        Info.InfoMassage.setFocusable(true);
        WaitForClient.restart();
        errorStation.restart();
    }

    public static void errorPin() {
        Info.ErrorMassage.setText(Text.pin);
        ChangePanel.ShowPanel(Info.InfoMassage);
        errorPin.restart();
    }

    public static void errorLitrs(String error) {
        switch (error) {
            case "numlitres":
                Info.ErrorMassage.setText(Text.numlitres);
                break;
            case "notenoughlitres":
                Info.ErrorMassage.setText(Text.notenoughlitres);
                break;
            case "getpistol":
                Info.ErrorMassage.setText(Text.getpistol);
                break;
            case "needlitres":
                Info.ErrorMassage.setText(Text.needlitres);
                break;
        }

        WaitForClient.restart();
        ChangePanel.ShowPanel(Info.InfoMassage);
        errorLitrs.restart();
    }

// One Timer For Open Waiting Panel And Another One For Change Seconds Value <    
    public static void WaitForClient() {
        WaitForClient.restart();
    }

    public static void ChangeSecondsValue() {
        SECONDSVALUE = 15;
        ChangeSecondsValue.restart();
    }

    public static void Success() {
        Success.restart();
    }
// >

    public static void ForceMajor() {
        ForceMajor.restart();
    }

    public static void ServerWaiting() {
        SECONDSVALUE = 15;
        ServerWaiting.restart();
    }

    private void noInternetInEnd(String newln, String code, String name, String leftlitr, Object sdate) {
        Info.ErrorMassage.setText(Text.nointernetinend);
        ChangePanel.ShowPanel(Info.InfoMassage);
        GasStation.StopStartCom3(true);
        errorCardLength.restart();
    }

    public static void WaitForServer() {
        Info.ErrorMassage.setText(Text.nointernetatstart);
        ChangePanel.ShowPanel(Info.InfoMassage);
        GasStation.StopStartCom3(true);
        SECONDSVALUE = 15;
        WaitForServer.restart();
    }

    public static void TryToConnect() {
        GasStation.StopStartCom3(true);
        ChangePanel.ShowPanel(Load.LoadingPanel);
        ToZero.FocusOff();
        TryToConnect.restart();
    }

    public static void KyePadWorks() {
        KeyPadWorks.restart();
    }

    public static void DateTime() {
        DateTime.restart();
    }

    public static void WriteTransaction(boolean state) {
   
        ForceMajor.stop();
        if (Work.SchetLitrov.getText().equals("")) { //Исправление бага "моментальное повешанье пистолета"
            Work.SchetLitrov.setText("0.0");
        }
        double litriDouble = Double.valueOf(Work.SchetLitrov.getText());
        double formatnewln = Double.valueOf(Variables.litrnum) - litriDouble;
        // NEWLN - Разница между литрами на карте и заправленными
        Variables.leftlitr = String.format(Locale.ENGLISH, "%.2f", litriDouble);
        Variables.newln = String.format(Locale.ENGLISH, "%.2f", formatnewln);
        // Date 
        java.util.Date udate = new java.util.Date();
        Variables.sdate = new java.sql.Timestamp(udate.getTime());
        // Transaction Data
        
        String[] Transaction = new String[]{
            String.valueOf(Variables.BalanceOneCardZero),
            String.valueOf(Variables.customerId),
            Variables.name,
            Variables.code,
            Variables.leftlitr,
            String.valueOf(Variables.sdate),
            String.valueOf(Variables.couponId)
        };
        /*
                //Try to send transaction with internet
                if (BackendTimers.InternetCheck) {
                } else {
                    SendMail.sendEmail("No Internet", "Wasn't Internet, when trying to send transaction, after client put on gas pistol! " + DB.MODULENAME);
                    System.out.println("No Internet");
                }
         */
        if (Variables.cardCode.equals(Text.HFP)) {
            ChangePanel.ShowPanel(Bye.GoodBye);
            Litrs.LitrsInput.setText("");
            Work.SchetLitrov.setText("");
            Success();
            GasStation.CustomerInfoToZero();
            ToZero.CustomerInfo();
        } else {
            WriteWI.CounterWriter(litriDouble);// Записываем отданные литры в счетчик
            WriteWI.Write(Transaction, Paths.TRANSACTIONPATH, true);// Записываем операцию в FillingData.txt
            LocalDB.WriteToLocalDB();// Записываем в LocalDB
            Litrs.LitrsInput.setText("");
            Work.SchetLitrov.setText("");
            Success();
            GasStation.CustomerInfoToZero();
            ToZero.CustomerInfo();
            ChangePanel.ShowPanel(Bye.GoodBye);
        }
        if (!state) {
            SendMail.sendEmail("Клиент: " + Variables.name + "\nВвел литров: " + Listener.LitrsInput + "\nНомер карты: " + Variables.code, Text.GSPortOff + " на АЗС  " + DB.MODULENAME, true);
            BackendTimers.FastReloadSystem();
        }
    }
}
