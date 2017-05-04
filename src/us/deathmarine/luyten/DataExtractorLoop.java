package us.deathmarine.luyten;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataExtractorLoop {

    private MainWindow mainWindow; //Gives access to function of the MainWindow


    public float[][] getData(UploadedFilesContainer filesUploaded) {
        //location of files
        ArrayList<File> Files = filesUploaded.getAllFiles();
        boolean yearsValid = checkYears(filesUploaded);
        boolean stationValid = checkStationName(filesUploaded);

        if (yearsValid && stationValid ) {
            //try and open file, if file does not exist then throw exception
            try {
                float riverData[][] = new float[365][3];//store riverHeight values for each substation.
                for (File file : Files) {

                    BufferedReader buf = new BufferedReader(new FileReader(file));

                    String stationName = null;
                    String lineFetched = null;
                    String[] stringArray;
                    String patternString = "^\\d{1,2}\\.\\d{1,2}";// regular expression dates .*25.56*.
                    Pattern pattern = Pattern.compile(patternString);//find multiple cases of patern
                    int counter = 0;


                    lineFetched = buf.readLine();//make sure a valid file is uploaded.
                    if (!lineFetched.contains("Historic Data For Illinois River"))
                        throw new Exception("Not valid file");
                    else {

                        if (lineFetched.contains("Peoria")) {
                            stationName = "Peoria";//put in the proper order
                        } else if (lineFetched.contains("Havana")) {
                            stationName = "Havana";
                        } else if (lineFetched.contains("Beardstown")) {
                            stationName = "Beardstown";
                        } else {
                            Luyten.showErrorDialog("No known Station Name");
                            throw new Exception("No known Station Name");
                        }
                    }
                }
                //System.out.println(stationName);


                while (true) {
                    lineFetched = buf.readLine();
                    if (lineFetched == null) {
                        break;
                    } else {


                        stringArray = lineFetched.split("\t");


                        for (String each : stringArray) {
                            //int counter = 0;
                            if (each.contentEquals("M")) {
                                each = "00.00";//get missing number
                            }
                            Matcher matcher = pattern.matcher(each);//compare the WHOLE line with regular expression
                            boolean matches = matcher.matches();//T/F if line matches
                            if (!"".equals(each) && matches) {
                                float riverheight = Float.parseFloat(each);
                                if (stationName.equals("Peoria")) {
                                    riverData[counter][0] = riverheight;
                                    counter++;


                                } else if (stationName.equals("Havana")) {
                                    riverData[counter][1] = riverheight;
                                    counter++;

                                } else if (stationName.equals("Beardstown")) {
                                    riverData[counter][2] = riverheight;
                                    counter++;
                                }
                            }
                        }


   

                for (int i = 0; i < 3; i++) {
                    Float x = riverData[i][2];
                    System.out.println(x);
                }
                    //TODO Send 2D array to Paul's Function hopefully he has a function
                    
                }
                buf.close();
                return riverData;



}
