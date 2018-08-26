/*
Name: DIPY
*/

import java.net.*;
import java.nio.*;
import java.nio.file.*;

import java.util.zip.*;
import java.util.*;
import java.io.*;

class Alice 
{
    private byte seqNum = 0;//initially we begin this with 0,which will alternate between 0 and 1.
    private DatagramSocket socket;

    public static void main(String[] args) throws Exception {
        // Do not modify this method
        if (args.length != 2) {
            System.out.println("Usage: java Alice <host> <unreliNetPort>");
            System.exit(1);
        }
        InetAddress address = InetAddress.getByName(args[0]);//retrieving the IP address if the host
        new Alice(address, Integer.parseInt(args[1]));//sending IP address and port of Alice 
    }

    public Alice(InetAddress address, int port) throws Exception {//constructor
        // Do not modify this method
        socket = new DatagramSocket();//binds Alice to any port numbered process available in the localhost
        socket.setSoTimeout(100);

        Scanner sc = new Scanner(System.in);
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            handleLine(line, socket, address, port);
            // Sleep a bit. Otherwise (if we type very very fast)
            // sunfire might get so busy that it actually drops UDP packets.
            Thread.sleep(20);
        }
    }

    public void handleLine(String line, DatagramSocket socket, InetAddress address, int port) throws Exception {
        // Do not modify this method
        if (line.startsWith("/send ")) {//alice is asking to send a file
            String path = line.substring("/send ".length());//storing the name of the file assumed to be in the current working directory. Hence we don't need the current path but the file name itself
            System.err.println("Sending file: " + path);
            try {
                File file = new File(path);
                if (!(file.isFile() && file.canRead())) {
                    System.out.println("Path is not a file or not readable: " + path);
                    return;
                }
            } catch (Exception e) {
                System.out.println("Could not read " + path);
                return;
            }
            sendFile(path, socket, address, port);
            System.err.println("Sent file.");
        } else {  //alice is asking to send a String message
            sendMessage(line, socket, address, port);
        }
    }

    public void sendFile(String path, DatagramSocket socket, InetAddress address, int port) throws IOException
    {
      FileInputStream fis = new FileInputStream(path);
      byte[] content;
      byte fileType =1;//file is coded 1
      while(fis.available()>0)
      {
        if(fis.available()>500)//need more than 1 packet to get the job done
        {
          content = new byte[500];
          fis.read(content);
          ByteBuffer packet = ByteBuffer.allocate(8+1+1+content.length);
          packet.putLong(getCheckSum(content));
          packet.put(seqNum);
          packet.put(fileType);
          packet.put(content);
          
          DatagramPacket sendPkt = new DatagramPacket(packet.array(),packet.capacity(),address,port);
          
          byte[] rcvAck = new byte[9];
          DatagramPacket rcvPkt = new DatagramPacket(rcvAck,9);
          ///packet ready to sedn/////////////////////////////////////////////
          send(socket,sendPkt,rcvPkt,rcvAck);      
          
          updateSeq();
        }
        else//only 1 more packet needed to get the job done
        {
          content = new byte[fis.available()];
          fis.read(content);
          ByteBuffer packet = ByteBuffer.allocate(8+1+1+content.length);
          packet.putLong(getCheckSum(content));
          packet.put(seqNum);
          packet.put(fileType);
          packet.put(content);
          
          DatagramPacket sendPkt = new DatagramPacket(packet.array(),packet.capacity(),address,port);
          
          byte[] rcvAck = new byte[9];
          DatagramPacket rcvPkt = new DatagramPacket(rcvAck,9);
          ///packet ready to sedn/////////////////////////////////////////////
          send(socket,sendPkt,rcvPkt,rcvAck);      
          
          updateSeq();
          
          byte[] last = new byte[0];//last empty packet being sent
          DatagramPacket lastPkt = new DatagramPacket(last,0,address,port);
          socket.send(lastPkt);
        } 
      } 
      fis.close();
    }
    
    public void sendMessage(String message, DatagramSocket socket, InetAddress address, int port) throws Exception 
    {
      //packet preparation///////////////////////////////////////////////////////////
            
      byte[] messageData = message.getBytes();
      ByteBuffer stream = ByteBuffer.allocate(8+1+1+messageData.length);
      byte fileType = 0;//message is coded 0 & doesen't change for the entire method
      
      stream.putLong(getCheckSum(messageData));
      stream.put(seqNum);
      stream.put(fileType);
      stream.put(messageData);//stream contains the message in byte form
      
      DatagramPacket sendPkt = new DatagramPacket(stream.array(),stream.capacity(),address,port);
      byte[] rcvAck = new byte[9];
      DatagramPacket rcvPkt = new DatagramPacket(rcvAck,9);
      ///packet ready to sedn/////////////////////////////////////////////
      send(socket,sendPkt,rcvPkt,rcvAck);      
     updateSeq();
      
   }
    private void send(DatagramSocket socket,DatagramPacket sendPkt,DatagramPacket rcvPkt,byte[] rcvAck )
    {
      while(true)
      {
        try
        {    
          socket.send(sendPkt);
          socket.setSoTimeout(100);
          socket.receive(rcvPkt);
        }
        catch(Exception E)//not received acknowledgement in timeout period
        {
          continue;//again send the packet       
        }
      
        //we have received packet by now and must check if corrupt
      
        ByteBuffer acknowledgement = ByteBuffer.wrap(rcvAck);
        long checksum = acknowledgement.getLong();
        byte ack = acknowledgement.get();
        boolean corrupt=true;
        
        if(checksum== getCheckSum(ack))//packet is not corrupt
          corrupt = false;
        
        if(corrupt)//corrupted acknowledgement so resend
          continue;
        //now ack is not corrupted
        else
        {
          if(ack==seqNum)//we have the correct packet
            break;//we have successful sent the message to Bob
          else 
            continue;//resend this packet as Bob is telling us it was corrupt when we sent before
        }
     }
    }
    private void updateSeq()
    {
      if(seqNum==0)
        seqNum=1;
      else //seqNum=1;
        seqNum=0;
    }
    public long getCheckSum(byte[] arr)
    {
      CRC32 checksum = new CRC32();
      checksum.update(arr);
      return checksum.getValue();
    }
    public long getCheckSum(byte arr)
    {
      byte[] arrArray = new byte[1];
      arrArray[0]=arr;
      CRC32 checksum = new CRC32();
      checksum.update(arrArray);
      return checksum.getValue();
    }
}      



//1.first prepare the packet.
// 2.set up and establish the sending protocol.
