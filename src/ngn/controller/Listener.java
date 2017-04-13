package ngn.controller;

import java.awt.event.ActionEvent;
import java.util.Locale;
import mail.SendMail;
import ngn.text.Config;
import ngn.view.Card;
import ngn.view.Litrs;
import static ngn.view.Litrs.ClientLitrs;
import ngn.view.Pin;
import ngn.view.Work;

/**
 *
 * @author Офис
 */
public class Listener {
    

    public static String LitrsInput;

    public static void CardCodeAction(ActionEvent e) {
        String CardCode = Converter.DeleteSymbols(e.getActionCommand());
        switch (CardCode){
            case "testmail":System.out.println(CardCode);
            SendMail.sendEmail("test", "test", Boolean.TRUE); Card.CardCode.setText("");
            break;
            case "testlog":ngn.text.Config.detaillog("Log exists"); Card.CardCode.setText("");
            break;
            case "sendlog":Preload.Update.uploadlog(); Card.CardCode.setText("");
            break;
            case "clearlog":ngn.text.Config.clearlog(); Card.CardCode.setText("");
            break;
            default:
        if (!GasStation.GasOFF){
        if (CardCode.length() == 10 && ReadWI.FindCardName(CardCode)) {
            Variables.cardCode = CardCode;
            Variables.customerId = Integer.valueOf(ReadWI.PersonalInfo[1]);
            Variables.pin = ReadWI.PersonalInfo[2];
            Variables.name = ReadWI.PersonalInfo[3];
            Variables.litrnum = String.format(Locale.ENGLISH, "%.2f", Double.valueOf(ReadWI.PersonalInfo[4]));
            Variables.code = ReadWI.PersonalInfo[5];
            Variables.customerPrice = Double.valueOf(ReadWI.PersonalInfo[6]);
            Variables.uahBalance = Double.valueOf(ReadWI.PersonalInfo[7]);
            Variables.purse = ReadWI.PersonalInfo[8];
            Variables.limitDay = Integer.valueOf(ReadWI.PersonalInfo[9]);
            Variables.limitLitrs = Double.valueOf(ReadWI.PersonalInfo[10]);
            Variables.usedLimitLitrs = Double.valueOf(ReadWI.PersonalInfo[11]);
            Variables.BalanceOneCardZero = Integer.valueOf(ReadWI.PersonalInfo[12]);
            Variables.couponId = Integer.valueOf(ReadWI.PersonalInfo[13]);
            Variables.credit = Double.valueOf(ReadWI.PersonalInfo[14]);
            Variables.customerBalance = Double.valueOf(ReadWI.PersonalInfo[15]);
            Variables.credit_days = Integer.valueOf(ReadWI.PersonalInfo[16]);
            
            //Work.WorkingCardCode.setText(Variables.cardCode);
            Work.WorkingCardCode.setText(Variables.code);
            Converter.chekLimit();
            ChangePanel.ShowPanel(Pin.EnterPin);
            ChangePanel.FocusPassword(Pin.PinCode);
            Timers.WaitForClient();
            System.out.println("Balance: " + ReadWI.PersonalInfo[15] + "Credit: " + ReadWI.PersonalInfo[14]+"Credit days:"+ReadWI.PersonalInfo[16]);
            
        } else {
            Timers.errorCard();
        }
        
        }
        else{
            Timers.errorStation();
        }
        break;
        }
    }

    public static void PinCodeAction(ActionEvent e) {
        String PinCode = Converter.DeleteSymbols(e.getActionCommand());
        if (GasStation.GasOFF){
            Timers.errorStation();
            return;
        }
        if (PinCode.equals(Variables.pin)) {
            if (Variables.credit_days>0){ //Проверка на просроченный кредит
                Timers.errorCredit();
            }
            else{
            Litrs.ClientName.setText(Variables.name);
            Litrs.ClientCard.setText(Variables.code);
            if (Variables.isLimitClient) {
                ClientLitrs.setText(Converter.lessNumber(Double.valueOf(Variables.litrnum), Variables.limitLitrnum));
            } else {
                ClientLitrs.setText(Variables.litrnum);
            }
            if (Variables.BalanceOneCardZero == 1) { // Балансовая карта
                if (Variables.isLimitClient) { // Есть лимиты по заправке
                    ClientLitrs.setText(Converter.lessNumber(Variables.customerBalance, Variables.limitLitrnum));
                } else {
                    ClientLitrs.setText(String.format(Locale.ENGLISH, "%.2f", Variables.customerBalance));
                }
            }
            ChangePanel.ShowPanel(Litrs.EnterLitrs);
            ChangePanel.FocusLitrsInput();
            Config.last_transaction(Variables.code);}
        } else {
            Timers.WaitForClient();
            Timers.errorPin();
        }
    }

    public static void LitrsInputAction(ActionEvent e) {
        if (GasStation.GasOFF){
            Timers.errorStation();
            return;
        }
        
        LitrsInput = Converter.DeleteSymbols(e.getActionCommand());
        if (LitrsInput.length() != 0 && Integer.valueOf(LitrsInput) > 0) { // Строка не пустая и значение строки больше ноля
            String eqHex = Converter.ConvertToHex(LitrsInput); // Передаем вводимое число литров на обработку для получения хексового значения     
            if (Converter.ConvertToDouble(LitrsInput, ClientLitrs.getText())) { // Проверяем разницу имеющихся литров на карте и вводимого (больше или равно нулю)
                if (GasStation.PolozheniePistoleta.equals("ПИСТОЛЕТ ПОВЕШЕН")) { // Ожидаем снятия пистолета
                    Timers.errorLitrs("getpistol"); // Пистолет не подняли после ввода количества литров
                } else {
                    Timers.WaitForClient.stop(); // Останавливаем проверку наличия клиента на время заправки
                    int blockhold=0;
                    boolean hold =false;
                    try{
                        //GasStation.getGasCounter(false,true);
                        while (!GasStation.gotfirstcounter){
                            GasStation.getGasCounter(false,true);
                            Thread.sleep(200);
                            blockhold++;
                            System.out.println(blockhold);
                            if (blockhold>200){
                                hold=true;
                                GasStation.gotfirstcounter=false;
                                break;
                            }
                        }
                    }
                    catch(Exception ex){
                        System.out.println(ex);
                        GasStation.gotfirstcounter=false;
                    }
                    
                        ChangePanel.ShowPanel(Work.Working);
                        Work.Working.requestFocusInWindow(); // Отображаем окно процесса заправки
                        String komDoza = Converter.HexDozaForKolonka(eqHex); // Получили команду для старта
                        GasStation.TimerZaderzkaDoza(komDoza);
                        Work.SchetLitrov.setText("");
                        Timers.ForceMajor();
                    /* Нужно проверить человеческий фактор дергания руки. Существует
                       возможность после поднятия пистолета, его мгновенное опускание.*/
                    
                }
            } else if (Variables.isLimitClient) {
                Timers.errorLitrs("needlitres");
            } else {
                Timers.errorLitrs("notenoughlitres");
                
            }
        } else {
            Timers.errorLitrs("numlitres");
        }
    }
}
