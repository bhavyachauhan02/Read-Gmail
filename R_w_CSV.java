package com.MyDhan.Gmail;


import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

public class R_w_CSV {
    static int count=0;
    public static void make_header() throws IOException {
        String path = "C:\\Users\\Admin\\Desktop\\gmail.csv";
        // make file from giving path
        File file = new File(path);
        // export file
        FileWriter outputfile = new FileWriter(file);
        // convert normal file to csv
        CSVWriter writer = new CSVWriter(outputfile);
        // supply format for header
        String[] header_template = {"ID","BANK NAME","AMOUNT","SUBJECT NAME","TIME DATE","SENDER NAME","ACCOUNT NUMBER","CONTENT"};
        // write format for csv
        writer.writeNext(header_template);
        writer.close();
    }
    // This method takes Array of Strings (Data) and adds to csv
    public static void make_csv(String[] data) throws FileNotFoundException {
        if(data.length<1)   //check for null data
        {
            String path = "C:\\Users\\Admin\\Desktop\\gmail.csv";
            // make file from giving path
            File file = new File(path);
            try{
                // export file
                FileWriter outputfile = new FileWriter(file,true);
                // convert normal file to csv
                CSVWriter writer = new CSVWriter(outputfile);

                String[] template = {String.valueOf(++count),data[1],data[2],data[3],data[4],data[5],data[6],data[7]};
                // write format for csv
                writer.writeNext(template);

                // close  file stream
                writer.close();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
