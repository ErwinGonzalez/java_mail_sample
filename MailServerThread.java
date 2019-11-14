import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.Writer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.lang.reflect.Type;

import com.google.gson.reflect.TypeToken;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

class MailServerThread implements Runnable {
    private Socket connectedSocket;
    private BufferedReader inputReader;
    private DataOutputStream outputStream;

    public MailServerThread(Socket connection) throws Exception {
        this.connectedSocket = connection;
        try {
            inputReader = new BufferedReader(new InputStreamReader(connectedSocket.getInputStream()));
            outputStream = new DataOutputStream(connectedSocket.getOutputStream());
        } catch (IOException e) {
            System.err.print(e.getMessage());
        }
    }

    @Override
    public void run() {

        String inputMessage = "";
        String[] states = {"BEGIN", "LOGIN", "CREATE", "MAIL", "INBOX", "OUTBOX", "SEND"};
        String currState = "BEGIN";
        boolean finished = false;
        String currEmail ="";
        //email validation pattern
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9]+\\@[a-zA-Z0-9]+[.][a-z]{2,3}$");
        Matcher matcher;
        try {
            writeOutput("Connected to the Server!\nChoose LOGIN or CREATE, type EXIT at any time to quit");

            while (!finished) {

                if (currState.equalsIgnoreCase("BEGIN")) {
                    inputMessage = inputReader.readLine();
                    printMessage(inputMessage);
                    if (inputMessage.equalsIgnoreCase("CREATE")) {
                        currState = "CREATE";
                        writeOutput("OK CREATE");
                    } else if (inputMessage.equalsIgnoreCase("LOGIN")) {
                        currState = "LOGIN";
                        writeOutput("OK LOGIN");
                    } else
                        writeOutput("Please select LOGIN or CREATE");
                }
                if (currState.equalsIgnoreCase("LOGIN")) {
                    inputMessage = inputReader.readLine();
                    printMessage(inputMessage);
                    String email = inputMessage;
                    inputMessage = inputReader.readLine();
                    printMessage(inputMessage);
                    String password = inputMessage;

                    if (findUser(email, password)) {
                        writeOutput("OK");
                        currState = "MAIL";
                        currEmail = email;
                    } else {
                        writeOutput("enter a valid email, i.e. test@domain.com");
                        printMessage("message2");
                    }
                }
                if (currState.equalsIgnoreCase("CREATE")) {
                    //writeOutput("Please insert your e-mail");
                    inputMessage = inputReader.readLine();
                    printMessage(inputMessage);
                    matcher = pattern.matcher(inputMessage);
                    String email = "";
                    String password = "";
                    if (matcher.find()) {
                        writeOutput("EMAIL OK");
                        printMessage("message");
                        email = inputMessage;
                        printMessage(inputMessage);
                        inputMessage = inputReader.readLine();
                        password = inputMessage.toString();
                        printMessage(inputMessage);

                        if (saveUser(email, password)) {
                            writeOutput("OK");
                            currState = "MAIL";
                            currEmail=email;
                        } else {
                            writeOutput("ERROR");
                            //printMessage("ERROR");
                        }

                    } else {
                        writeOutput("enter a valid email, i.e. test@domain.com");
                        printMessage("message2");
                    }
                    printMessage(currState);
                } else if (currState.equals("MAIL")) {
                    printMessage("REACHED INBOX");
                    //writeOutput("select SEND, INBOX or OUTBOX");
                    inputMessage = inputReader.readLine();
                    if(inputMessage.equalsIgnoreCase("SEND")){
                        currState = "SEND";
                    }else if(inputMessage.equalsIgnoreCase("INBOX")){
                        currState = "INBOX";
                    }else if(inputMessage.equalsIgnoreCase("OUTBOX")){
                        currState = "OUTBOX";
                    }

                } else if(currState.equals("SEND")){
                    String emailRecipient = inputReader.readLine();
                    printMessage(emailRecipient);
                    inputMessage = inputReader.readLine();
                    printMessage(inputMessage);
                    if(sendEmail(currEmail, emailRecipient, inputMessage)){
                        printMessage("message Saved");
                        writeOutput("OK");
                    }else{
                        printMessage("error saving message");
                        writeOutput("ERROR");
                    }
                    currState = "MAIL";
                } else if(currState.equals("INBOX")){
                    ArrayList<Email> emailList = readBox(currEmail,true);
                    if(emailList != null){
                        System.out.println(emailList);
                        for(Email email: emailList){
                            writeOutput(email.toString());
                        }
                        writeOutput("\0");

                    }else{
                        printMessage("Error");
                    }
                    currState = "MAIL";
                } else if(currState.equals("OUTBOX")){
                    ArrayList<Email> emailList = readBox(currEmail,false);
                    if(emailList != null){
                        System.out.println(emailList);
                        for(Email email: emailList){
                            writeOutput(email.toString());
                        }
                        writeOutput("\0");
                    }else{
                        printMessage("Error");
                    }
                    currState = "MAIL";

                } else if (inputMessage.equalsIgnoreCase("exit")) {
                    System.out.println("goodbye");
                    finished = true;
                }
                printMessage(currState);
            }

        } catch (
                Exception es) {

        }
    }

    private void writeOutput(String message) throws Exception {
        outputStream.writeBytes(message + "\r\n");
        outputStream.flush();
    }

    static void printMessage(String message) {
        System.out.println(message);
    }

    private ArrayList<Email> readBox(String user, boolean boxType) throws  IOException{
        String userBox;
        if(boxType)
            userBox = "./"+user+"/inbox.json";
        else
            userBox = "./"+user+"/outbox.json";

        File userInbox = new File(userBox);
        Gson gson = new Gson();

        if(userInbox.exists()){
            JsonReader jsonReader = new JsonReader(new FileReader(userBox));
            Type emailType = new TypeToken<ArrayList<Email>>() {}.getType();
            ArrayList<Email> emailList = gson.fromJson(jsonReader, emailType);
            if(emailList == null)
                emailList = new ArrayList<Email>();
            return emailList;
        }else{
            return null;
        }
    }

    private boolean sendEmail(String emailSender, String emailReceiver, String content) throws IOException{
        System.out.println(emailSender+" : "+emailReceiver+" : "+content);
        String senderOutboxPath = "./"+emailSender+"/outbox.json";
        File senderOutbox = new File(senderOutboxPath);
        String receiverInboxPath = "./"+emailReceiver+"/inbox.json";
        File receiverInbox = new File(receiverInboxPath);
        Gson gson = new Gson();
        if(senderOutbox.exists()){
            JsonReader jsonReader = new JsonReader(new FileReader(senderOutboxPath));
            Type emailType = new TypeToken<ArrayList<Email>>() {}.getType();
            ArrayList<Email> emailList = gson.fromJson(jsonReader, emailType);
            if(emailList == null)
                emailList = new ArrayList<Email>();
            Email newEmail = new Email(emailSender, emailReceiver, content);
            emailList.add(0,newEmail);
            Writer writer = new FileWriter(senderOutboxPath, false);
            gson.toJson(emailList, writer);
            writer.flush(); //flush data to file   <---
            writer.close();
        }else{
            return false;
        }
        if(receiverInbox.exists()){
            JsonReader jsonReader = new JsonReader(new FileReader(receiverInboxPath));
            Type emailType = new TypeToken<ArrayList<Email>>() {}.getType();
            ArrayList<Email> emailList = gson.fromJson(jsonReader, emailType);
            if(emailList == null)
                emailList = new ArrayList<Email>();
            Email newEmail = new Email(emailSender, emailReceiver, content);
            emailList.add(0,newEmail);
            Writer writer = new FileWriter(receiverInboxPath, false);
            gson.toJson(emailList, writer);
            writer.flush(); //flush data to file   <---
            writer.close();
        }else{
            return false;
        }

        System.out.println(senderOutbox.exists());
        System.out.println(receiverInbox.exists());
        return true;
    }
    private boolean findUser(String email, String password){
        String filepath = "./users.json";

        File file = new File(filepath);
        try{
            if(!file.createNewFile()){
                Gson gson = new Gson();
                JsonReader jsonReader = new JsonReader(new FileReader(filepath));

                Type emailType = new TypeToken<ArrayList<EmailUser>>() {}.getType();
                ArrayList<EmailUser> emailList = gson.fromJson(jsonReader, emailType);
                for(EmailUser e: emailList) {
                    System.out.println(e);
                    if(e.name.equals(email) && e.password.equals(password))
                        return true;
                }
                return false;
            }
        }catch (IOException ioe){
            System.err.println(ioe);
        }
        return false;
    }
    private boolean saveUser(String email, String password) {

        String filepath = "./users.json";
        File file = new File(filepath);
        try {
            file.createNewFile();
            EmailUser mail = new EmailUser(email, password);

            Gson gson = new Gson();

            JsonReader jsonReader = new JsonReader(new FileReader(filepath));
            Type emailType = new TypeToken<ArrayList<EmailUser>>() {}.getType();
            ArrayList<EmailUser> emailList = gson.fromJson(jsonReader, emailType);

            System.out.println(emailList);
            if (emailList == null)
                emailList = new ArrayList<EmailUser>();

            for (EmailUser em : emailList) {
                if (em.name.equals(mail.name)) {
                    System.out.println("username in use");
                    return false;
                }
            }
            System.out.println(emailList);
            mail.setId(emailList.size());
            emailList.add(mail);
            Writer writer = new FileWriter("./users.json", false);
            gson.toJson(emailList, writer);
            writer.flush(); //flush data to file   <---
            writer.close();
            file = new File("./" + mail.name);
            file.mkdir();
            file = new File("./" + mail.name + "/inbox.json");
            file.createNewFile();
            file = new File("./" + mail.name + "/outbox.json");
            file.createNewFile();
            return true;
        } catch (IOException ioe) {
            System.err.println(ioe);
        }
        return false;
    }

    class EmailUser {
        int id;
        String name;
        String password;

        public EmailUser(String name, String password) {
            this.name = name;
            this.password = password;
        }

        public void setId(int id) {
            this.id = id;
        }
        public String toString(){
            return this.id+" email: "+this.name+" pass: "+this.password;
        }
    }
    class Email {
        String date;
        String sender;
        String receiver;
        String content;

        public Email(String sender, String receiver, String content){
            this.sender = sender;
            this.receiver = receiver;
            this.content = content;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            this.date = dtf.format(now);
        }

            public String toString(){
            return this.date+"\nSender: "+this.sender+"\nReceiver: "+this.receiver+"\n"+this.content;
            }

    }
}