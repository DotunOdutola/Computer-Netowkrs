/*
 * Created by: Dotun Odutola 3/5/2017
*/

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Scanner;
import java.io.*;
import java.util.Random;
import static java.lang.System.exit;


class UDPClient {
	//initialize grem probability
	private static double gremProb;
	
	public static void main(String args[]) throws Exception {

		
		
		//Reads in the keyboard stream by bytes and converts them into ASCII.
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress IPAddress;
		byte[] sendData = new byte[128];
		byte[] receiveData = new byte[128];
		String inputIP;
		int portNumber;
		Scanner scan = new Scanner(System.in);
		
		if (args.length == 0) { // check cmd args
			System.out.println("No command line arg set for gremlin probability!\n");
			
		} else {
			gremProb = Double.parseDouble(args[0]);
		}
		
		while(true){		
			System.out.print("Enter IP Address of designated Server: ");
			inputIP = scan.nextLine();
			try{
				IPAddress = InetAddress.getByName(inputIP);
			} catch(UnknownHostException e){
				System.out.println("Invalid host name or the host name is unreachable.");
				continue;
			}
			break;
		}

		System.out.print("Enter Server's port number: ");
		portNumber = scan.nextInt();
		String sentence;
		String[] request;
		do{
			System.out.print("Enter HTTP Request: ");
			sentence = inFromUser.readLine();
			request = sentence.trim().split("\\s");
		}while(request.length != 3);
		
		//path to file on my mac
		File file = new File(System.getProperty("/Users/dotunodutola/Documents/Eclipse/Web Service/src/"), request[1]);

		//sending the data to the server.
		sendData = sentence.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, portNumber);
		clientSocket.send(sendPacket); 
		scan.close();

		DatagramPacket receivePacket;

		receivePacket = new DatagramPacket(receiveData, receiveData.length); 
		clientSocket.setSoTimeout(2000);
		try{
		clientSocket.receive(receivePacket);
		} catch(SocketTimeoutException e){
			System.out.println("Timed Out! Connection closing.");
			clientSocket.close();
		}
		if(!clientSocket.isClosed()){			
		

		String receivedPacketData = new String(receivePacket.getData());
		String[] receivedPacketHeaderData = receivedPacketData.split("[ ]");
		if(receivedPacketHeaderData.length > 3){
			//Analyze http status code from server
            byte[] statusCode = receiveData;
            int s = new String(statusCode).lastIndexOf(':');
            int e = new String(statusCode).indexOf("\r", s);
            int fileNumber = Integer.parseInt(new String(statusCode).substring(s + 1, e).trim());
            System.out.println("HTTP response from server: \n\n" + new String(receiveData));
            
            //Handle 404 and 400 response code        
            String convertedByte = new String(receiveData, StandardCharsets.UTF_8);           
            String badRequestSearch  = "400";
            String fileNotFoundSearch = "404";         
            
            //handle 400 http response
            if (convertedByte.toLowerCase().indexOf(badRequestSearch.toLowerCase()) != -1 ) {

               System.out.println("Request returned bad http request.. Please try again! \n");
               exit(1);
            } 
            
            //handle 404 http response
            if (convertedByte.toLowerCase().indexOf(fileNotFoundSearch.toLowerCase()) != -1 ) {

                System.out.println("Requested file could not be found.. Please Try again! \n");
                exit(1);
             } 
            
            //end

			String fileData = "";			
			int eos=-1;			
			while(eos == -1 && fileData.length() < fileNumber){

				receivePacket = new DatagramPacket(receiveData, receiveData.length); 
				clientSocket.receive(receivePacket);
				
				//trim
	            byte[] packet = trim(receiveData);
	            //header from server
	            String header = new String(packet).substring(0, new String(packet).indexOf('+')); 
	            int cs = Integer.parseInt(header.substring(header.lastIndexOf('#') + 1, header.lastIndexOf('\r')));

	            //gremlin function
	            packet = gremlin(packet, header.length());
	            //end of header
	            String packetData = new String(packet).substring(new String(packet).indexOf('+') + 3); 
	            
	            //compute checksum
	            byte[] packetBytes = packetData.getBytes();
	            int checkSum = 0;
	            for(Byte b : packetBytes) {
	            	checkSum += b.intValue();
	            }
	            	            
                if (checkSum != cs) {
                	System.out.println("Damaged Packet!");
                }
	            
	            //end here
	                
	            System.out.println("\nData: \n" + new String(packet));
	            fileData += packetData;
	            packet = null;
				
				receivedPacketData = new String(receivePacket.getData());

				// check for 0 byte packet
				 if (receiveData[0] != 0) {
		                continue;
		            } else if (receiveData[receiveData.length - 1] != 0) {
		                continue;
		            }
		                else {
		                   eos = 0;

		            }
			}
	
			clientSocket.close();
			Writer writer = null;

			try {
			    // write file  
	            fileData = fileData.trim();
				writer = new BufferedWriter(new OutputStreamWriter(	new FileOutputStream(file), "UTF-8"));
				writer.write(fileData);
				System.out.println("Successfully Wrote data to file!");
				writer.close();

			} catch (IOException ex) {
			} finally {
				try {
					writer.close();
				} catch (Exception ex) {
				}
			}
		} else {
			file =null;
			System.out.println("Invalid response from Server.");
		}
	} 
	}
	

	private static byte[] gremlin(byte[] packet, int hl) {
        // Check for damage
        Random ran = new Random();
        double generateNumber = ran.nextDouble();

        if (generateNumber > gremProb) {
            return packet;
        }

        double randomProb = ran.nextDouble();

        // Generate if packet is damaged
        if (randomProb > 0.0 && randomProb <= 0.2) {
            int gremOne = ran.nextInt(((packet.length - 1) - hl) + hl);
            int gremTwo = ran.nextInt(((packet.length - 1) - hl) + hl);
            int gremThree = ran.nextInt(((packet.length - 1) - hl) + hl);
            packet[gremOne] = 0;
            packet[gremTwo] = 0;
            packet[gremThree] = 0;
          
        } else if (randomProb > 0.2 && randomProb <= 0.5) {
            int gremOne = ran.nextInt(((packet.length - 1) - hl) + hl);
            int gremTwo = ran.nextInt(((packet.length - 1) - hl) + hl);
            packet[gremOne] = 0;
            packet[gremTwo] = 0;
            
        } else {
            int gremFour = ran.nextInt(((packet.length - 1) - hl) + hl);
            packet[gremFour] = 0;
        }

        return packet;
    }
	
	
    private static byte[] trim(byte[] bytes) {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0) {
            --i;
        }
        return Arrays.copyOf(bytes, i + 1);
   
    }

	
} 