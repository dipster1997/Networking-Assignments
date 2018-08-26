 /*
 Name: SUBHODIP MANDAL
 
 */


import java.net.*;
import java.nio.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;


class Bob 
{
  private byte currentAck = 0;//intiially stored as 0 to correspond to first packet sent by Alice.
  DatagramSocket socket;
  FileOutputStream fos = null ;
  
  public static void main(String[] args) throws Exception {
    // Do not modify this method
    if (args.length != 1) {
      System.out.println("Usage: java Bob <port>");
      System.exit(1);
    }
    new Bob(Integer.parseInt(args[0]));
  }
  
  public Bob(int port) throws Exception {  //Bob's constructor
    
    socket = new DatagramSocket(port);
    while(true) //set loop to infinitely wait for packet
    {      
      byte[] packet = new byte[510];
      DatagramPacket rcvPacket = new DatagramPacket(packet,packet.length);
      socket.receive(rcvPacket);//we have now recieved the packet
      
      ByteBuffer actualPacket = ByteBuffer.wrap(packet);//extracted the true contents of the packet
      if(rcvPacket.getLength()!=0)//if the packet is not empty
      {
        long checksum = actualPacket.getLong(0);//index at 8 now
        byte seqNum = actualPacket.get(8);
        byte fileType = actualPacket.get(9);
        byte[] content =
        Arrays.copyOfRange(rcvPacket.getData(),10,rcvPacket.getLength());
        
        boolean corrupt;
        if(checksum == getCheckSum(content))
          corrupt = false;
        else 
          corrupt =true;
        
        if(corrupt)
        {
          //do nothing and let timeOut happen
        }
        else //packet is not corrupt
        {
          if(seqNum==currentAck)//we have the packet we expected
          {
            send(currentAck,socket,rcvPacket.getAddress(),rcvPacket.getPort());//sent corresponding ack
            
            if(fileType==1)//1 means its a file
               giveBobFile(content);
            else//its a message
               printMessage(new String(content));
            
            updateAck();
          }
          else//we have a duplicate packet
          {
            send(previousAck(),socket,rcvPacket.getAddress(),rcvPacket.getPort());
            //don't do anything with packet data
          }
        }
      }
      else closeBobFile();//close the fileOutpuTStream.
     }         
 }
  private void send(byte ack,DatagramSocket socket,InetAddress address,int port)throws IOException
  {
    ByteBuffer packet = ByteBuffer.allocate(9);
    packet.putLong(getCheckSum(ack));
    packet.put(ack);
    DatagramPacket sendPkt = new DatagramPacket(packet.array(),9,address,port);
    socket.send(sendPkt);
  }
  private byte previousAck()
  {
    if(currentAck==1)
      return 0;
    else 
      return 1;
  }
  
  private void updateAck()
  {
    if (currentAck==0)
      currentAck =1;
    else //currentAck=1
      currentAck=0;
  }
  
 private void closeBobFile()throws IOException//empty packt consideration
 {
   fos.close();
   fos=null;   
 }
 public void giveBobFile(byte[] content)throws IOException
 {     
   if(fos==null)//if we have to begin writing a file now
   {
     fos = new FileOutputStream("output");
     fos.write(content);
   }
   else
     fos.write(content);      
 }

 public void printMessage(String message) 
 {
  // Do not modify this method
  System.out.println(message);
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
