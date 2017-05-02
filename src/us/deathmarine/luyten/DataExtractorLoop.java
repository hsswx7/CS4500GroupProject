package us.deathmarine.luyten;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataExtractorLoop {


    private boolean checkYears(UploadedFilesContainer filesContainer) {
        return true;
    }

    private boolean checkStationName(UploadedFilesContainer filesContainer) {
//        byte[] b = new byte[]{3};
        int bitSize = filesContainer.getMaxFilesAllowed();
        BitSet bitSet = new BitSet(bitSize);

        bitSet.set(0, bitSize);

        for (File file : filesContainer.getAllFiles()){
            try {
                BufferedReader buf = new BufferedReader(new FileReader(file.getAbsolutePath()));
                String lineFetched = null;
                lineFetched = buf.readLine();//make sure a valid file is uploaded.

                if (lineFetched.contains("Peoria")) {
                    bitSet.set(0,false);
                } else if (lineFetched.contains("Havana")) {
                    bitSet.set(1,false);
                } else if (lineFetched.contains("Beardstown")) {
                    bitSet.set(2,false);
                }

            }catch (Exception e){
                Luyten.showExceptionDialog("checkStationName", e);
            }
        }

        
        if(bitSet.length() == 0){
        	return true;
        }
        return false;
    }


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

                        }
                    }

   


                    buf.close();

                    //TODO Send 2D array to Paul's Function hopefully he has a function
                    
                }
                
                return riverData;
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.out.println("File not Found");
            }
        }
        else{
            StringBuilder errorString = new StringBuilder();

            if(!yearsValid){
                errorString.append("Location Years not Consistent.\n");
            }
            if(!stationValid){
                errorString.append("Your Bases are belong to us.");
            }
            Luyten.showErrorDialog(errorString.toString());
        }
		return null;
    }

}
