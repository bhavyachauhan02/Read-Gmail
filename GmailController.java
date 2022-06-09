package com.MyDhan.Gmail;

import okhttp3.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@RestController
public class GmailController
{
    @RequestMapping(value = "/get_auth")
    public void get_authcode(HttpServletResponse res) throws IOException    //Function to get authorization code
    {
        String url = "https://accounts.google.com/o/oauth2/v2/auth?" +  //google authentication server end point
                "client_id=804977942217-4olcfqll6u2685fk48sdmmun03ngi5t2.apps.googleusercontent.com" +  //Query parameter: ClientID of application by developer console
                "&response_type=code" + //Query parameter: response type for authorization code
                "&redirect_uri=http://localhost:63342/Gmail/static/index.html" +    //Query parameter: Redirect uri where server will send authorization code response (It should match with the one provided at developer console)
                "&response_mode=query" +    //Query parameter: Authorization code should be sent as query parameter
                "&scope=https://mail.google.com/"+  //Query parameter: scope/permission to read gmail
                "&prompt=select_account";   //Query parameter: select gmail account
        res.sendRedirect(url);
        /* Redirects the user to google authorization page
           User enter credentials
           Allows permission from consent screen
           Response auth code
         */
    }



    @RequestMapping(value = "/get_token/4/{code}")
    public static String get_token(@PathVariable String code) throws IOException, ParseException  //Function to use auth code to get access token
    {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(1,TimeUnit.HOURS)
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        //Post Body
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("client_id","804977942217-4olcfqll6u2685fk48sdmmun03ngi5t2.apps.googleusercontent.com")    //ClientID of application by developer console
                .addFormDataPart("code","4/"+code)  //Auth code previously obtained by get_authcode
                .addFormDataPart("redirect_uri","http://localhost:63342/Gmail/static/index.html")   //Redirect uri where server will access token response (It should match with the one provided at developer console)
                .addFormDataPart("grant_type","authorization_code")
                .addFormDataPart("client_secret","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")     //Client Secret of application by developer console
                .build();

        Request request = new Request.Builder()
                .url("https://oauth2.googleapis.com/token")     //Url endpoint to get access token
                .method("POST", body)
                .build();

        Response response = client.newCall(request).execute();
        String text=response.body().string();       //String response
        JSONObject jsonObject=Parsing.get_object(text);     //Get JSON form of response
        get_emails_csv_file((String) jsonObject.get("access_token"));   //Get access token from Json Object and call function to make csv
        return "Done!!";
    }


    //Function to get csv file
    public static void get_emails_csv_file(String token) throws IOException, ParseException
    {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(1,TimeUnit.HOURS)
                .build();

        Request request = new Request.Builder()
                .url("https://gmail.googleapis.com/gmail/v1/users/me/messages?labelIds=INBOX")  //Call Gmail api to get user's inbox messages ids
                .method("GET", null)
                .addHeader("Authorization", "Bearer "+token)
                .build();

        Response response = client.newCall(request).execute();
        String text=response.body().string();
        JSONObject object=Parsing.get_object(text);
        String nextPageToken= (String) object.get("nextPageToken"); // In one response there are only 100 message ids, nextPageToken contains token to access subsequent 100 message ids


        ArrayList<String> list=new ArrayList<>();   //List of message ids
        while (nextPageToken!=null)     //loop until end of inbox mails
        {
            JSONArray msgs= (JSONArray) object.get("messages");     //Messages is JSON array which contains an object for each msg having its id, thread id
            for(int i=0;i<msgs.size();i++)
            {
                JSONObject curr= (JSONObject) msgs.get(i);  // get current message object
                String id= (String) curr.get("id");     //get message id from current json object
                list.add(id);   //add id to list of ids
            }
            request = new Request.Builder()
                    .url("https://gmail.googleapis.com/gmail/v1/users/me/messages?labelIds=INBOX&pageToken="+nextPageToken) //request corresponding subsequent 100 messages
                    .method("GET", null)
                    .addHeader("Authorization", "Bearer "+token)
                    .build();

            response = client.newCall(request).execute();
            text=response.body().string();
            object=Parsing.get_object(text);
            nextPageToken= (String) object.get("nextPageToken");
        }


        R_w_CSV.make_header();  //Add headers or columns names to csv file


        String array[]=new String[8];
        for(String id:list)      // for each message id
        {
            request = new Request.Builder()
                    .url("https://gmail.googleapis.com/gmail/v1/users/me/messages/"+id) //request to get message content
                    .method("GET", null)
                    .addHeader("Authorization", "Bearer "+token)
                    .build();
            response = client.newCall(request).execute();
            text=response.body().string();  //String response
            array=Parsing.extract_data(text);   // Get formatted data
            R_w_CSV.make_csv(array);    //add formatted data corresponding to current message in csv
        }

    }
}
