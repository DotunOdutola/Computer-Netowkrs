/*
 * Created by: Dotun Odutola 3/5/2017
*/

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;


import static java.lang.System.exit;

/*
 * Receives GET message
 */

public class UDPServer {
	private static DatagramSocket serverSocket;
	private static final int PACKET_SIZE = 128;
	private static final int HEADER_SIZE = 40;
	private static byte[] receiveData = new byte[1024]; 
	private static byte[] sendData;
	private static byte[] fileData;
	private static InetAddress IPAddress;
	private static int portNumber;
	private static String[] trimRequest;
	
	
	public static void main(String args[]) throws Exception {
		//Attempt to open Socket
		
        try {
             serverSocket = new DatagramSocket(23000);  
            
        } catch (SocketException e) {
            System.out.println("Unable to open socket... " + e.getLocalizedMessage());
            throw e;
        }
		
        receiveRequest();
        
		}
	
	
	static void receiveRequest() throws IOException {
	
		while(true)
		{
		System.out.println("Waiting on request... ");
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		try {
		serverSocket.receive(receivePacket);
		} catch (IOException e) {
			System.out.println("error...");
		}
		
		 
		
		String incomingRequest = new String(receivePacket.getData()); 
        System.out.println("Received request to " + incomingRequest);
        trimRequest = incomingRequest.trim().split("\\s"); 
        
        //Sets file name
        String fileName = trimRequest[1];
        fileData = new byte[0];
        readFile(fileName);      
        portNumber = receivePacket.getPort();
        IPAddress = receivePacket.getAddress();
		      
        isValidRequest();
        boolean result = UDPServer.isValidRequest(); 
        String response = UDPServer.generateStatusCode(result);
        sendData = new byte[PACKET_SIZE];
        sendData = response.getBytes();
        System.out.println("FROM SERVER: \n");
        System.out.println(response);

        sendRequest();
        partitionFile();
        packetFinished();
	}	
		
}

	static void sendRequest() throws IOException {
		/*
		 * Attempt to send packet
		 */
		
		serverSocket = new DatagramSocket();

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, portNumber);
		try {
			
			serverSocket.send(sendPacket);
			} catch (IOException e) {
				System.out.println("Error sending packet! ");
				throw e;
			}		
		
	}
	
	static boolean isValidRequest() {
		boolean Result;
		String thirdWord = trimRequest[2];
		thirdWord.toLowerCase();
		
		if (thirdWord.length() < 6) {

			Result = false;
		}
		else if(thirdWord.charAt(0) != 'H' && thirdWord.charAt(0) != 'h') {

			Result = false;

		}
		else if(thirdWord.charAt(1) != 'T' && thirdWord.charAt(1) != 't') {

			Result = false;

		}
		
		else if(thirdWord.charAt(2) != 'T' && thirdWord.charAt(2) != 't') {

			Result = false;

		}
		
		else if(thirdWord.charAt(3) != 'P' && thirdWord.charAt(3) != 'p') {
	
			Result = false;

		}
		
		else if (thirdWord.charAt(4) != '/') {
	
			Result = false;

		}
		
		else if(thirdWord.charAt(5) < 48 && thirdWord.charAt(6) > 57) {

			Result = false;
		}
		
		else if(thirdWord.charAt(6) != 46) {

			Result = false;
		}
		
		else if(thirdWord.charAt(7) < 48 && thirdWord.charAt(6) > 57)  {

			Result = false;
		}
		else {
			Result = true;
		}
		return Result;
		
	}
	
	/*
	 * Function for segmentation (break files into smaller segments)
	 */
	static void partitionFile() {
		//Packet header
        String header;
        String data;
        
		int fileByteCounter = 0;
        int sequenceNumber = 0;
        byte[] packet;
		
        while(fileByteCounter < fileData.length) {
            byte[] tempFileBytes;
            int packetSize = fileData.length - fileByteCounter;
            int dLength = PACKET_SIZE - HEADER_SIZE;

            if (packetSize > dLength) {
                tempFileBytes = Arrays.copyOfRange(fileData, fileByteCounter, fileByteCounter += dLength);
            } else {
                tempFileBytes = Arrays.copyOfRange(fileData, fileByteCounter, fileByteCounter += packetSize);
            }

            // calculate checksum
            int checkSum = 0;
            for (Byte b: tempFileBytes) {
                checkSum += b.intValue();
            }
            
            //Set the header for Server response to client      
            header = generateHeader(sequenceNumber, checkSum);
            data = header + new String(tempFileBytes);

            packet = data.getBytes();

            System.out.println("Sending packet with data \n" + new String(packet));
            sendData = new byte[PACKET_SIZE];
            sendData = packet;

           try {          
                sendRequest(); 
                sequenceNumber++;
            } catch (IOException e) {
                System.out.println("Unable to Send... ");
                return;
            }
        
        
        }
	}
	
	
	static String generateStatusCode(boolean key) {
	
		//Request is 400 error. Set 400 header
		 if (key == false) {
				System.out.println("");
	            String response = "HTTP/1.0 400 Bad Request \r\n";
	            response += "Content-Type: text/plain\r\n";
	            response += "Content-Length: " + fileData.length + "\r\n";
	            response += "\r\n";
	          return response;  
		}
		
		//Request is Okay. Set 200 header
		else if (key == true) {
			System.out.println("");
            String response = "HTTP/1.0 200 Document Follows\r\n";
            response += "Content-Type: text/plain\r\n";
            response += "Content-Length: " + fileData.length + "\r\n";
            response += "\r\n";
            return response;        
		}
		
		 return "";
		
	}
	
	
	 static String generateHeader(int sequenceNumber, int checksum) {
		 String ph;
		 /*
		  * Set packet header
		  */
	        ph = ("Sequence #" + sequenceNumber + "\r\n");
	        ph += ("Checksum #" + checksum + "\r\n");
	        ph += "\t+\r\n";
		return ph;
	}
	 
	 static void readFile(String file) {
		 try {
		 File receivedFile = new File("/Users/dotunodutola/Documents/Eclipse/Web Service/src/" + file);
		  	if(!receivedFile.isFile()) {
		  		//file not found. 404 error
		  		System.out.println("");
		        String response = "HTTP/1.0 404 File not found\r\n";
		        response += "Content-Type: text/plain\r\n";
		        response += "Content-Length: " + fileData.length + "\r\n";
		        response += "\r\n";
		        System.out.println(response);
		        receiveRequest();
		  	}
		  	
		  	// File found
		 fileData = Files.readAllBytes(receivedFile.toPath());
		 System.out.println(trimRequest[1] + " Located! Attempting to read file... ");
		 
		 } catch(IOException e) {
			 System.out.println("Could not read file.");
		 }
		 
		 
	 }
	 
	 private static void packetFinished() {
	        System.out.println("End of file.. Attempting to send NULL packet");
	        sendData = new byte[1];
	        sendData[0] = 0;
	        try {
	            sendRequest();
	        } catch (IOException e) {
	            System.out.println("Unable to send NULL packet... ");
	        }
	 }
}
	
		
