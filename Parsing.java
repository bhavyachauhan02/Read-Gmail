package com.MyDhan.Gmail;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parsing {

    //This method returns JSON Object
    public static JSONObject get_object(String data) throws ParseException {
            JSONParser jsonParser = new JSONParser();
            Object obj = jsonParser.parse(data);
            JSONObject jsonobj = (JSONObject) obj;
            return jsonobj;

    }

    // This method gives boolean output for validation of mail is it bank mails or temporary mails
    public static boolean is_bank_mail(String mail_name) throws FileNotFoundException {
        String[] bank_names;
        // name of banks which we have to extract
        bank_names  = new String[]{"axisbank.", "kotak.", "hdfcbank.", "icicibank." ,"yesbank.","sbi.","bankofindia.","bankofbaroda.","unionbankofindia."};

        // pattern for parsing the mails
        String pattern_prefix = "[a-zA-Z0-9_.+-]+@";
        String pattern_suffix="[a-zA-Z]+";
        String curr_pattern="";
        // iterating through list for checking is it the mail from list
        for(int i=0;i<bank_names.length;i++){
            curr_pattern = pattern_prefix + bank_names[i]+pattern_suffix;

            Pattern pattern = Pattern.compile(curr_pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(mail_name);
            boolean matchFound = matcher.find();
            // if pattern found with any of the mails of bank names then return true
            if(matchFound) {
                return true;
            }
        }
        // iterating to list but not getting any match means the mails is not for us so return false
        return false;
    }
    // This method returns true if there is transactional mail
    public static boolean is_bank_mail_transactional(String content){
//        amount send= credited
//        amount received= debited
//        amount send to= to A/c no.
//                amount received from= from A/c no.
//                date= on dd/mm/yy
//        time= at hr:min:sec
//        Available balance after credit or debit= Avl Bal or Available balance
        String[] keyword_list = {"credited","debited","amount received","Avl Bal","balance"};
        for(String keyword: keyword_list){
            if(content.contains(keyword)){
                return true;
            }
        }
        return false;

    }

    // This method fetch amount from the paragraph
    public static String get_amount(String paragraph) {
        String pattern1 = "Rs.[0-9]+\\,[0-9]+\\.[0-9]{2}";
//        INR 669374.27
        String pattern2 = "INR [0-9]+\\.[0-9]{2}";
        ArrayList<String> arr = new ArrayList<>();
        arr.add(pattern1);
        arr.add(pattern2);
        for (String pattern : arr) {
            Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = p.matcher(paragraph);
            boolean matchFound = matcher.find();
            String amount = "";
            // if pattern found with any of the mails of bank names then return true
            if (matchFound) {
                int s = matcher.start();
                int e = matcher.end();
                amount = paragraph.substring(s, e + 1);
                return amount;
            }
        }
        return "";
    }

    public static String[] extract_data(String data) throws IOException, ParseException {
        JSONObject msg=Parsing.get_object(data);    //Json Object corresponding message content
            String arr[]=new String[8];
            arr[2]="amt";
            arr[6]="acc";
            arr[7]= (String) msg.get("snippet");    // Get Message Content
            JSONObject payload = (JSONObject) msg.get("payload");
            JSONArray array = (JSONArray) payload.get("headers");   //Get array of headers
            for (int i = 0; i < array.size(); i++) {
                JSONObject curr = (JSONObject) array.get(i);
                if (curr.get("name").equals("From"))    //To check if header is from
                {
                    String temp[] = ((String) curr.get("value")).split("<");    //get value corresponding to from header which contains <sender-email> in this format
                    if(temp==null){
                        temp = new String[]{"NA", "NAA"};
                    }
                    else if(1 == temp.length){
                        temp = new String[]{temp[0],"NAA"};
                    }
                    arr[1] = temp[0];
                    arr[5] = temp[1].substring(0,temp[1].length()-1);
                }
                else if (curr.get("name").equals("Subject"))    //To check if header is subject
                {
                    arr[3]= (String) curr.get("value");     //get value of subject header
                }
                else if (curr.get("name").equals("Date"))   //To check if header is date
                {
                    arr[4]= (String) curr.get("value");     //get value of date header
                }
            }

        // to filter out bank mails from arraylist(data) of all mails
            if (Parsing.is_bank_mail(arr[5]) && Parsing.is_bank_mail_transactional(arr[7]))
            {
                arr[2] = Parsing.get_amount(arr[7]);
                return arr; // return string of data corresponding to this msg further to be added to csv
            }
            return new String[0];   //if not bank transaction, return nothing
        }
    }


