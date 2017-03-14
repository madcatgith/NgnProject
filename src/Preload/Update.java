package Preload;

import static Preload.PortCheck.PortCheck;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Scanner;
import mail.SendMail;
import ngn.Ngn;
import ngn.controller.ReadWI;
import ngn.model.DB;
import ngn.text.Config;
import ngn.text.Paths;
import ngn.text.Text;
import static ngn.text.Text.*;
import static ngn.view.BeforeStart.BSLoadingText;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Update {

    private static final Double VER = 0.37;

    private static final String URL = Config.URL;
    private static final String USER = Config.USER;
    private static final String PASS = Config.PASS;
    private static final boolean UPD = Config.UPD;
    private static final String KEYWORD = "ver";
    private static URL con;

    public static void Update() {
        if (BackendTimers.InternetCheck && UPD) {
            BSLoadingText.setText(h1CheckUpdate);
            try {
                con = new URL("ftp://" + USER + ":" + PASS + "@" + URL + "/");
            } catch (MalformedURLException ex) {
                SendMail.sendEmail(String.valueOf(ex), Text.cantConn + " " + DB.MODULENAME, false);
                BSLoadingText.setText(cantConn);
                System.out.println(ex);
            }
            try {
                Ngn.StatusBar(Paths.SERVERON, 4);
                Ngn.StatusBar(Paths.SERVERCON, 5);
                BSLoadingText.setText(h1CheckUpdate);
                Scanner scan = new Scanner(con.openStream());
                Boolean FileExist = false;
                while (scan.hasNext()) {
                    String line = scan.nextLine();
                    if (line.contains(KEYWORD)) {
                        FileExist = true;
                        String ZipVer = line.substring(line.length() - 8, line.length() - 4);
                        String ZipName = KEYWORD + line.substring(line.length() - 9, line.length());
                        CheckNewVersion(ZipVer, ZipName);
                    }
                }
                if (!FileExist) {
                    runnOldVer();
                }
            } catch (IOException ex) {
                BSLoadingText.setText(authNOT);
                SendMail.sendEmail(String.valueOf(ex), Text.authNOT + " " + DB.MODULENAME, false);
                System.out.println(ex);
            }
        } else if (!UPD) {runnOldVer();}  else {
            BSLoadingText.setText(tryConnInet);
            BackendTimers.WaitForInternet();
        }
    }

    public static boolean CheckServer() {
        try {
            Scanner testscan = new Scanner(con.openStream());
            return testscan.hasNext();
        } catch (IOException ex) {
            System.out.println(ex);
            return false;
        }
    }

    private static void CheckNewVersion(String ZipVer, String ZipName) {

        BSLoadingText.setText(InetOkTryDownload);
        if (Double.parseDouble(ZipVer) > VER) {// перевірка на нову версію
            String upload = con + ZipName;
            String place = "C:/NgnUpdater/Updates/" + ZipName;
            BSLoadingText.setText(downlNEW);
            try {
                BSLoadingText.setText(InetOkTryDownload);
                download(upload, place);
            } catch (IOException ex) {
                System.out.println(ex);
                SendMail.sendEmail(String.valueOf(ex), Text.cantdownlNEW + " " + DB.MODULENAME, false);
                BSLoadingText.setText(cantdownlNEW);
            }
        } else {
            runnOldVer();
        }
    }
    
    public static void uploadlog() {
        String server = URL;
        int port = 21;
        String user = USER;
        String pass = PASS;
        String curDir = new File("").getAbsolutePath();
        String filename=curDir+"/detaillog.txt";
 
        FTPClient ftpClient = new FTPClient();
        try {
            Config.detaillog("start transfer");
            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();
 
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
 
            // APPROACH #1: uploads first file using an InputStream
            File firstLocalFile = new File(filename);
            byte ptext[] = DB.MODULENAME.getBytes("Windows-1251"); 
            String modname = new String(ptext, "UTF-8");
            modname=Data.Translit.toTranslitNew(DB.MODULENAME);
            //System.out.println(modname);
 
            String firstRemoteFile = "logs/detaillog_"+modname+".txt";
            System.out.println(firstRemoteFile);
            InputStream inputStream = new FileInputStream(firstLocalFile);
 
            System.out.println("Start uploading first file");
            boolean done = ftpClient.storeFile(firstRemoteFile, inputStream);
            inputStream.close();
            if (done) {
                //System.out.println("The first file is uploaded successfully.");
                Config.detaillog("The first file is uploaded successfully.");
            }
 
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
            Config.detaillog("Error: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void runnOldVer() {
        File file = new File(Paths.TRANSACTIONPATH);
        if (file.exists() && CreateCounter()) {
            ReadWI.ReadWI();
            Threads.CHECKPORTS();
        } else {
            BSLoadingText.setText(cantFIND + Paths.TRANSACTIONPATH);
            try {
                if (file.createNewFile() && CreateCounter()) {
                    BSLoadingText.setText(createFile + Paths.TRANSACTIONPATH);
                    Threads.CHECKPORTS();
                } else {
                    SendMail.sendEmail(Text.workingFIREWALL, Text.cantCREATE + " " + Paths.TRANSACTIONPATH + " " + DB.MODULENAME, false);
                }
            } catch (IOException ex) {
                System.out.println(ex);
                BSLoadingText.setText(cantCREATE + Paths.TRANSACTIONPATH);
                SendMail.sendEmail(String.valueOf(ex), Text.cantCREATE + " " + Paths.TRANSACTIONPATH + " " + DB.MODULENAME, false);
            }
        }
    }

    private static boolean CreateCounter() {
        File file = new File(Paths.COUNTERPATH);
        if (file.exists()) {
            return true;
        } else {
            try {
                if (file.createNewFile()) {
                    return true;
                }
            } catch (IOException ex) {
                System.out.println(ex);
                SendMail.sendEmail(String.valueOf(ex), Text.cantCREATE + " " + Paths.COUNTERPATH + " " + DB.MODULENAME, false);
                return false;
            }
        }
        return false;
    }

    private static void download(String urlStr, String file) throws MalformedURLException, IOException {
        URL url = new URL(urlStr);
        try (ReadableByteChannel rbc = Channels.newChannel(url.openStream()); FileOutputStream fos = new FileOutputStream(file)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        OpenandShut();
    }

    public static void OpenandShut() {
        try {
            Runtime.getRuntime().exec(Paths.UPDATEPATH); // Запуск програми РОЗАРХІВУВАННЯ
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                System.out.println(ex);
            }
        } catch (IOException ex) {
            BSLoadingText.setText(cantRunProg);
            SendMail.sendEmail(String.valueOf(ex), Text.cantRunProg + " " + DB.MODULENAME, false);
            System.out.println(ex);
        }
        Runtime.getRuntime().exit(0);
    }

    public static boolean CheckLocalDB() {
        File file = new File(Paths.LDBPATH);
        if (file.exists()) {
            return true;
        }
        return false;
    }
}
