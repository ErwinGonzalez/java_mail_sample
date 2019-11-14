import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MailClient {
    public static void main (String[] args){
        String serverResponse = "";
        String clientMessage = "";
        boolean finished = false;
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        String[] states = {"BEGIN", "LOGIN","CREATE","MAIL", "INBOX", "OUTBOX", "SEND"};
        String currState = "BEGIN";

        try{
            Socket socket = new Socket("localhost", 9001);
            PrintWriter outSocket = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader inSocket = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader userReader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println(inSocket.readLine());
            System.out.println(inSocket.readLine());
            
            while (!finished){
                printStateHeader(currState);
                if(currState.equals("BEGIN")){
                    System.out.print("Select an Option: ");
                    clientMessage = userReader.readLine();
                    outSocket.println(clientMessage);
                    serverResponse = inSocket.readLine();
                    if(serverResponse.equalsIgnoreCase("OK LOGIN") || serverResponse.equalsIgnoreCase("OK CREATE"))
                        currState = clientMessage.toUpperCase();
                    else
                        printMessage(serverResponse);
                    
                }else if(currState.equals("LOGIN")){
                    printMessage("Please insert your e-mail: ");
                    clientMessage = userReader.readLine();
                    outSocket.println(clientMessage);
                    //serverResponse = inSocket.readLine();
                    printMessage("Please insert your password: ");
                    clientMessage = userReader.readLine();
                    outSocket.println(clientMessage);
                    serverResponse = inSocket.readLine();
                    if(serverResponse.equalsIgnoreCase("OK")){
                        printMessage("Login Successful!");
                        currState = "MAIL";
                    }else{
                        printMessage("Error with e-mail or password, please try again");
                    }
                } else if(currState.equals("CREATE")){
                    printMessage("Please insert a new e-mail");
                    clientMessage = userReader.readLine();
                    outSocket.println(clientMessage);
                    printMessage(currState);
                    serverResponse = inSocket.readLine();
                    printMessage(serverResponse);
                    if(serverResponse.equals("EMAIL OK")){
                        printMessage("please enter a password");
                        clientMessage = userReader.readLine();
                        outSocket.println(clientMessage);
                        serverResponse = inSocket.readLine();
                        if(serverResponse.equals("OK")){
                            printMessage("Email saved!");
                            currState = "MAIL";
                        }else{
                            //serverResponse = inSocket.readLine();
                            printMessage("Username already in use");
                        }
                    }else{
                        printMessage(serverResponse);
                    }
                }else if(currState.equals("MAIL")){
                    printMessage("Welcome, select SEND, INBOX or OUTBOX");
                    clientMessage = userReader.readLine();
                    if(clientMessage.equalsIgnoreCase("SEND")){
                        currState = "SEND";
                        outSocket.println("SEND");

                    }
                    else if(clientMessage.equalsIgnoreCase("INBOX")){
                        currState = "INBOX";
                        outSocket.println("INBOX");

                    }
                    else if(clientMessage.equalsIgnoreCase("OUTBOX")){
                        currState = "OUTBOX";
                        outSocket.println("OUTBOX");
                    }else{
                        printMessage("ERROR reading command");
                    }

                    /*outSocket.println(clientMessage);
                    printMessage(currState);
                    serverResponse = inSocket.readLine();
                    printMessage(serverResponse);
                    break;*/
                }
                else if(currState.equals("SEND")){
                    printMessage("Please insert the recipient email");
                    clientMessage = userReader.readLine();
                    outSocket.println(clientMessage);
                    printMessage("Please add the content, end message with ENTER");
                    clientMessage = userReader.readLine();
                    outSocket.println(clientMessage);
                    serverResponse = inSocket.readLine();
                    if(serverResponse.equals("OK")){
                        printMessage("Message successfully sent");
                    }else{
                        printMessage("ERROR sending the message, please check the address");
                    }
                    currState = "MAIL";
                }
                else if(currState.equals("INBOX")){
                    String response= inSocket.readLine();


                    while(!response.equals("\0")){
                        System.out.println(response);
                        response = inSocket.readLine();
                    }
                    /*System.out.println("\\0 response eq"+response.equals("\0"));
                    System.out.println("\\0\\n response eq"+response.equals("\0\n"));
                    System.out.println("\\0\\r\\n response eq"+response.equals("\0\r\n"));*/
                    currState = "MAIL";
                }
                else if(currState.equals("OUTBOX")){
                    String response = inSocket.readLine();
                    while(!response.equals("\0")){
                        System.out.println(response);
                        response = inSocket.readLine();
                    }
                    currState = "MAIL";
                }
                if(clientMessage.toLowerCase().equals("exit")) {
                    finished = true;
                }
            }

            inSocket.close();
            outSocket.close();
            socket.close();
            
        }catch(UnknownHostException uhe){
            System.err.print(uhe);
        }catch(IOException ioe){
            System.err.print(ioe);
        }
    }
    static void printMessage(String message){
        System.out.println(message);
    }
    static void printStateHeader(String currState){
        System.out.println("------------------"+currState+"------------------");
    }
}