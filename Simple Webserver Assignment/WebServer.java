/*
 Name:Dipy
 
 */

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.io.*;

public class WebServer 
{
  public static void main(String[] args)throws IOException
  {
    // dummy value that is overwritten below
    int port = 8080;
    try {
      port = Integer.parseInt(args[0]);
    } catch (Exception e) {
      System.out.println("Usage: java webserver <port> ");
      System.exit(0);
    }
    
    WebServer serverInstance = new WebServer();
    serverInstance.start(port);
  }
  
  //to setup the server and recieve connections from clients
  public void start(int port)throws IOException
  {
    System.out.println("Starting server on port " + port);
    ServerSocket welcomeSocket = new ServerSocket(port);//setting up server to listen for client connections
    while(true)//server will infinitely handle requests assuming its an http 1.1
    {
      Socket connectionSocket = welcomeSocket.accept();//creates a socket to listen and wait for a client's connection and finally accept it
      System.out.println("Connected to client");
      handleClientSocket(connectionSocket);//sending the socket to handle client requests
      connectionSocket.close();
    }
  }
      
  /**
   * Handles requests sent by a client
   * @param  client Socket that handles the client connection
   */
  public void handleClientSocket(Socket client)throws IOException 
  {
    //establishing input communication streams for the socket
    BufferedReader hearClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
    boolean isPersistent;
    
    HttpRequest request = new HttpRequest();
    request.parse(hearClient.readLine());//input and process the 1st part client request instruction
    isPersistent = (request.getHttpType().equals("HTTP/1.0"))?false:true;//if request is http1.1,we use this as a flag to repeat the loop
      
    while(true)//reading all the headers prvoided after the client request
      {
        String read = hearClient.readLine();
   
        if (read.equals("") || read == null)
        {   break;    }
      }
      
      byte[] finalResponse = formHttpResponse(request);
      sendHttpResponse(client,finalResponse);
      
      if(isPersistent)  //if it is http1.1 then we recursively loop the server to keep recieveing requests
        try
      {       
        client.setSoTimeout(2000);
        handleClientSocket(client);
      }
      catch (Exception E)
      {
        System.out.println("Connection Temrinated");
        client.close();
      }
      else//if its HTTP 1.0
      {
        client.close();
      }
    }
  
  
  /**
   * Form a response to an HttpRequest
   * @param  request the HTTP request
   * @return a byte[] that contains the data that should be send to the client
   */
  public byte[] formHttpResponse(HttpRequest request)throws IOException 
  {
    byte[] reply ;
    byte[] webObject;
    
    try  //assuming the file exists in the given filepath
    {
      File file = new File(request.getFilePath().substring(1));
      
      //read file contents into byte array
      webObject = Files.readAllBytes(file.toPath());
      
      //prepare reply
      String phrase = request.getHttpType() + " " + "200 OK" + "\r\n" + "Content-Length:" + " " + webObject.length + "\r\n" + "\r\n";
      reply = phrase.getBytes();
      
      
    }
    catch(IOException e)//404 file not found
    {
      webObject = new byte[0]; //no contents to transfer
      
      //prepare reply
      reply = form404Response(request);
    }
    
    return concatenate(reply,webObject);
  }
  
  
  /**
   * Form a 404 response for a HttpRequest
   * @param  request a HTTP request
   * @return a byte[] that contains the data that should be send to the client
   */
  public byte[] form404Response(HttpRequest request) 
  {
    String content = get404Content(request.getFilePath());
    byte[] contentBytes = content.getBytes();
    String phrase = request.getHttpType() + " " + "404 Not Found" + "\r\n" + "Content-Length:" + " " + contentBytes.length + "\r\n" + "\r\n";
    return concatenate(phrase.getBytes(),contentBytes);
  }
  
  /**
   * Returns a string that represents a 404 error
   * You should use this string as the return website
   * for 404 errors.
   * @param  filePath path of the file that caused the 404
   * @return a String that represents a 404 error website
   */
  public String get404Content(String filePath) 
  {
    // You should not change this function. Use it as it is.
    StringBuilder sb = new StringBuilder();
    sb.append("<html>");
    sb.append("<head>");
    sb.append("<title>");
    sb.append("404 Not Found");
    sb.append("</title>");
    sb.append("</head>");
    sb.append("<body>");
    sb.append("<h1>404 Not Found</h1> ");
    sb.append("<p>The requested URL <i>" + filePath + "</i> was not found on this server</p>");
    sb.append("</body>");
    sb.append("</html>");
    
    return sb.toString();
  }
  
  
  /**
   * Concatenates 2 byte[] into a single byte[]
   * This is a function provided for your convenience.
   * @param  buffer1 a byte array
   * @param  buffer2 another byte array
   * @return concatenation of the 2 buffers
   */
  public byte[] concatenate(byte[] buffer1, byte[] buffer2) 
  {
    byte[] returnBuffer = new byte[buffer1.length + buffer2.length];
    System.arraycopy(buffer1, 0, returnBuffer, 0, buffer1.length);
    System.arraycopy(buffer2, 0, returnBuffer, buffer1.length, buffer2.length);
    return returnBuffer;
  }
  
  
  /**
   * Sends a response back to the client
   @param  client Socket that handles the client connection
   * @param  response the response that should be send to the client
   */
  public void sendHttpResponse(Socket client, byte[] response)throws IOException 
  {
    OutputStream tellClient = client.getOutputStream();
    tellClient.write(response);
  }
  
  
  class HttpRequest 
  {
    
    //data attributes
    public String filePath;
    public String httpType;
    
    //member methods
    
    //to break down request and extract important data components form the 1st part of client request
    public void parse(String str)
    {
      System.out.println("parsing " + str);
      String[] parts = str.split(" ");
        
      filePath = parts[1]; //the file path directory is the 2nd token
      httpType = parts[2]; //the 3rd token contains the http request type
      httpType = httpType.substring(0,8);//only extracting the first 8 characters which state the type of http request      
    }
    
    public String getFilePath()
    {
      return filePath;
    }
    public String getHttpType()
    {
      return httpType;
    }
    
  }
}
