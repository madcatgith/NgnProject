package ngn.text;

import java.util.Properties;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;
import java.nio.file.Files;
import java.io.*;
import java.util.Properties;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.io.PrintWriter;


/**
 *
 * @author Валерик
 */
public class Config {

//___________S E R V E R___________//2
    public static final String URL = "176.111.58.218";//HOST
    public static final String USER = "ngnweb";//LOGIN
    public static final String PASS = "MAINNGNweb479";//PASSWORD
    public static final boolean UPD = true; // Enable update

//___________D A T A B A S E___________//
    public static final String DB_URL = "jdbc:mysql://176.111.58.218:3306/ngnsite?useUnicode=true&characterEncoding=UTF-8";//DB_HOST
    public static final String DB_USER = "ngntest";//DB_LOGIN
    public static final String DB_PASS = "NGNtest";//DP_PASSWORD
    public static final String DB_PREFIX = "ngn_";//DB_PREFIX
    /*public static final String DB_USER = "ngnsite";//DB_LOGIN
    public static final String DB_PASS = "NGNSITEmysql";//DP_PASSWORD
    public static final String DB_PREFIX = "ngn_";//DB_PREFIX*/

//___________A D M I N P A S S___________//
    public static final String ADMIN_PASS = "92486513"; //BACKDOOR FREE GAS 13791469
    public static final Properties SetProp() {

        
        Properties prop = new Properties();
        prop.setProperty("user", DB_USER);
        prop.setProperty("password", DB_PASS);
        prop.setProperty("useUnicode", "true");
        prop.setProperty("characterEncoding", "UTF-8");

        return prop;
    }
    
    public static void detaillog(String line){
        String curDir = new File("").getAbsolutePath();
        String filename=curDir+"/detaillog.txt";
        try {
                if (line!=null){
                    line=LocalDateTime.now()+" "+line+"\n";
                    Files.write(Paths.get(filename), line.getBytes(), StandardOpenOption.APPEND);}
                
        }
        catch (IOException ex){
            System.out.println(ex.getMessage());
        }
    }
    
    public static void clearlog(){
        String curDir = new File("").getAbsolutePath();
        String filename=curDir+"/detaillog.txt";
        try{
            PrintWriter pw = new PrintWriter(filename);
            pw.close();
        }
        catch(IOException e){
            detaillog(""+e);
        }
    }

}
