// Author: SUBHODIP MANDAL.

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;
import javax.crypto.SealedObject;

/**********************************************************************
  * This skeleton program is prepared for weak and average students.  *
  *                                                                   *
  * If you are very strong in programming, DIY!                       *
  *                                                                   *
  * Feel free to modify this program.                                 *
  *********************************************************************/

// Alice knows Bob's public key
// Alice sends Bob session (AES) key
// Alice receives messages from Bob, decrypts and saves them to file

class Alice { // Alice is a TCP  client
    
    private ObjectOutputStream toBob;   // to send session key to Bob
    private ObjectInputStream fromBob;  // to read encrypted messages from Bob
    private Crypto crypto;        // object for encryption and decryption procedures
    public static final String MESSAGE_FILE = "msgs.txt"; // file to store messages
    private Socket aliceSocket;
    
    public static void main(String[] args)throws IOException 
    {        
        // Check if the number of command line argument is 2
        if (args.length != 2) {
            System.err.println("Usage: java Alice BobIP BobPort");
            System.exit(1);
        }
        
        new Alice(args[0], args[1]);
    }
    
    // Constructor
    public Alice(String ip, String port)throws IOException 
    {
        aliceSocket = new Socket(ip,Integer.parseInt(port));
        
        this.crypto = new Crypto();
        
        //wrap the AES key with in RSA style sealedObject
         SealedObject toSend = crypto.getSessionKey();
        
        // Send session key to Bob
        sendSessionKey(toSend);
        
        // Receive encrypted messages from Bob,
        // decrypt and save them to file
        receiveMessages(crypto);
        
        aliceSocket.close();//alice has recieved messages and now will close socket
    }
    
    // Send session key to Bob
    public void sendSessionKey(SealedObject toSend)throws IOException 
    {
      toBob = new ObjectOutputStream(aliceSocket.getOutputStream());
      toBob.writeObject(toSend);       //1 time send of session key
    }
    
    // Receive messages one by one from Bob, decrypt and write to file
    public void receiveMessages(Crypto crypto) 
    {
      PrintWriter pw=null;
      try
      {
        fromBob = new ObjectInputStream(aliceSocket.getInputStream());
        pw = new PrintWriter(new File("msgs.txt"));
      }
      catch(IOException e)
      {
        System.out.println("problem reading bob's input");
      }
      
      try
      {
        System.out.println("Alice recieves - ");
        while(true)//keep reading,decrypting and saving the lines from Bob till Exception is incurred
        {        
          SealedObject encryptedmsg = (SealedObject)fromBob.readObject();
          String text = crypto.decryptMessage(encryptedmsg);
          System.out.println(text);//just to see what the lovebirds have to say to each other
          pw.println(text);
        }
      }
      catch(Exception e)//if bob has no more messages to send and has closed his tcp server connection hence we have recieved an exception
      {
         pw.close();//we are done with the writing to new text file
      }
      
     // How to detect Bob has no more data to send// check if the tcp connection from his side is closed.
    }
    
    /*****************/
    /** inner class **/
    /*****************/
    class Crypto 
    {
        
        // Bob's public key, to be read from file
        private PublicKey pubKey;
        // Alice generates a new session key for each communication session
        private SecretKey sessionKey;
        // File that contains Bob's public key
        public static final String PUBLIC_KEY_FILE = "public.key";
        
        // Constructor
        public Crypto()throws IOException 
        {
            // Read Bob's public key from file
            readPublicKey();
            // Generate session key dynamically
            initSessionKey();
        }
        
        // Read Bob's public key from file
        public void readPublicKey() 
        {          
          try
          {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(PUBLIC_KEY_FILE));
            pubKey = (PublicKey)ois.readObject();
            System.out.println("public key -" +pubKey);
          }
          catch (IOException ois)
          {
            System.out.println("Error reading Public key");
          }
          catch(ClassNotFoundException e)
          {
            System.out.println("datatype PublicKey does not exist");
          }
        }
        
        // Generate a session key
        public void initSessionKey() 
        {
          try
          {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(128);          // suggested AES key length is 128 bits
            sessionKey = generator.generateKey();
            //System.out.println("sessionKey" + sessionKey);
          }
          catch(NoSuchAlgorithmException e)
          {
            System.out.println("you didn;t give the write algorithm to the keygen");
          }
          
        }
        
        // Seal session key with RSA public key in a SealedObject and return
        public SealedObject getSessionKey()throws IOException 
        {
          SealedObject sendAES = null;
           try
           {
            // Alice must use the same RSA key/transformation as Bob specified
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE,this.pubKey);
            byte[] encodedKey = sessionKey.getEncoded();//we send the encoded form of the key
            
            //System.out.println("encoded key" + encodedKey);
            
            sendAES = new SealedObject(encodedKey, cipher);//final encapsulation
           }            
           catch(GeneralSecurityException gse)
           {
             System.out.println("Key is wrong");
           }
           return sendAES;        
           
            // RSA imposes size restriction on the object being encrypted (117 bytes).
            // Instead of sealing a Key object which is way over the size restriction,
            // we shall encrypt AES key in its byte format (using getEncoded() method).           
        }
        
        // Decrypt and extract a message from SealedObject
        public String decryptMessage(SealedObject encryptedMsgObject)
        {
          String plainText = null;
          try
          {            
               // Alice and Bob use the same AES key/transformation
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE,sessionKey);
            plainText = (String)encryptedMsgObject.getObject(cipher);
          }
          catch(GeneralSecurityException gse)
          {
            System.out.println("he hah");
          }
          catch(ClassNotFoundException e)
          {
            System.out.println("couldn't convert to string");
          }
          catch(IOException e)
          {
            System.out.println("IOEception he had");
          }
          return plainText;
          
        }
    }
}