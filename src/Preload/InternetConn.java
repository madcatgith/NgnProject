package Preload;

import java.io.IOException;
import java.net.*;
import ngn.controller.WriteWI;
import ngn.text.Paths;

/**
 *
 * @author Валерик
 */
public class InternetConn {
    public static boolean InternetConn() {
        try {
            URL url = new URL("http://www.n-g-n.com/");
            HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
            urlc.setRequestProperty("User-Agent", "test");
            urlc.setRequestProperty("Connection", "close");
            urlc.setConnectTimeout(8000);
            urlc.setReadTimeout(12000);
            urlc.connect();
            //System.out.println(urlc.getResponseCode()); //Ответ сервера
            String[] resp={"n-g-n.com:",""+urlc.getResponseCode()};
            //WriteWI.Write(resp,Paths.LOGPATH, true);
            return urlc.getResponseCode() == 200;

        } catch (IOException e) {
            System.out.println("Ошибка проверки подключения к интернету: " + e);
            String[] resp={"n-g-n.com:",""+e};
            //WriteWI.Write(resp,Paths.LOGPATH, true);
            return false;
        }
    }
}


    /*
    public static boolean InternetConn() {
        Boolean result = false;
        try {
            URL url = new URL("http://google.com/");
            InputStream inputStream = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "windows-1251"));
            StringBuilder allText = new StringBuilder();
            char[] buff = new char[1];

            int count;
            while ((count = reader.read(buff)) != -1) {
                allText.append(buff, 0, count);
            }
            Integer intStart = allText.indexOf("<title>");
            Integer intEnd = allText.indexOf("</title>", intStart);
            String google = allText.substring(intStart + 7, intEnd);
            if ("Google".equals(google)) {
                result = true;
            }
        reader.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        return result ;
    }
     */