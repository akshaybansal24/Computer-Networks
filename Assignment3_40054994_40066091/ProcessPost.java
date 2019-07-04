import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Set;

public class ProcessPost {
	
	public static boolean isVerbose = false;
	public static boolean isHeader = false;
	public static boolean isFile = false;
	public static boolean isInlineData = false;
	public static int headerCount = 0;
	public static String headers = "";
	public static String URL = "";
	public static boolean isValidRequest = true;
	public static String inlineData = "";

	public static void processPostRequest(String[] args) throws UnknownHostException, IOException, URISyntaxException {
		URL = args[args.length-1];
		checkRequestFormat(args);
		makePostCall();
	}

	private static void makePostCall() throws UnknownHostException, IOException, URISyntaxException {
		System.out.println("here");
		URI uri = new URI(URL);
		String host = uri.getHost();
		String path = uri.getPath();
		if(path==null || path.length()==0) {
			path=path+"/";
		}
		String query = uri.getQuery();
		if(query!=null && query.length()>0) {
			path = path + "?" + query;
		}
		String protocol = uri.getScheme();
		int port = uri.getPort();
		if(port == -1) {
			if("http".equalsIgnoreCase(protocol))
				port=80;
			else if("https".equalsIgnoreCase(protocol))
				port = 443;
		}
		String requestToBeMade = "POST " + path + " HTTP/1.0\r\n";
		requestToBeMade += "Host: " + host + "\r\n";
		requestToBeMade += "User-Agent: Concordia-HTTP/1.0" + "\r\n";
		requestToBeMade += "Content-Length: "+inlineData.length() + "\r\n";
		if(isHeader && headerCount>0) {
			requestToBeMade += headers;
		}
		requestToBeMade += "INLINE_DATA";
		requestToBeMade += inlineData + "\r\n\r\n";
		System.out.println(requestToBeMade);
		DatagramChannel channel = DatagramChannel.open();
		makeUDPCall(requestToBeMade, host, port,channel);
		channel.bind(channel.getLocalAddress());
		String line;
		String response = "";
		 ByteBuffer buf = ByteBuffer
                 .allocate(Packet.MAX_LEN)
                 .order(ByteOrder.BIG_ENDIAN);
		while(true) {
			buf.clear();
            SocketAddress router = channel.receive(buf);

            // Parse a packet from the received raw data.
            buf.flip();
            Packet packet = Packet.fromBuffer(buf);
            buf.flip();
            line = new String(packet.getPayload(), UTF_8);
            response = response + line + "\n";
            Packet resp = packet.toBuilder().setPayload("".getBytes()).setType(1).create();
			channel.send(resp.toBuffer(), router);
            if(packet.getType() == 5)
            	break;
		}
		
		/*InputStream inStream = socket.getInputStream( ); 
		BufferedReader rd = new BufferedReader(
		        new InputStreamReader(inStream));
		String line;
		String response = "";
		while ((line = rd.readLine()) != null) {
		   response += line+"\n";
		}*/
		if(isVerbose)
			System.out.println(response);
		else {
			String nonVerboseResp = response.substring(response.indexOf("{"));
			System.out.println(nonVerboseResp);
		}
		
	}

	private static void makeUDPCall(String requestToBeMade, String host, int port, DatagramChannel channel) throws IOException {
		ArrayList<Packet> packetsToBeSent = new ArrayList<Packet>();
		byte[] totalRequestData = requestToBeMade.getBytes();
		InetSocketAddress hostAddress = new InetSocketAddress(host, port);
		long sequenceNumber = 0;
		int dataSize = totalRequestData.length;
		int numOfPackets = 0;
		if(dataSize%1024==0) {
			numOfPackets = dataSize/1024;
			for(int i=0;i<numOfPackets;++i) {
				byte[] packetData = new byte[1024];
				for(int j=0;j<1024;++j) {
					packetData[j] = totalRequestData[1024*i+ j];
				}
				Packet packet;
				if(i<numOfPackets-1) {
					 packet = new Packet.Builder()
						.setType(0)
	                    .setSequenceNumber(sequenceNumber++)
	                    .setPortNumber(hostAddress.getPort())
	                    .setPeerAddress(hostAddress.getAddress())
	                    .setPayload(packetData)
	                    .create();
				}
				else {
					packet = new Packet.Builder()
							.setType(5)
		                    .setSequenceNumber(sequenceNumber++)
		                    .setPortNumber(hostAddress.getPort())
		                    .setPeerAddress(hostAddress.getAddress())
		                    .setPayload(packetData)
		                    .create();
				}
				packetsToBeSent.add(packet);
			}
		}
		else {
			numOfPackets = dataSize/1024 + 1;
			for(int i=0;i<numOfPackets-1;++i) {
				byte[] packetData = new byte[1024];
				for(int j=0;j<1024;++j) {
					packetData[j] = totalRequestData[1024*i+ j];
				}
				Packet packet = new Packet.Builder()
						.setType(0)
	                    .setSequenceNumber(sequenceNumber++)
	                    .setPortNumber(hostAddress.getPort())
	                    .setPeerAddress(hostAddress.getAddress())
	                    .setPayload(packetData)
	                    .create();
				packetsToBeSent.add(packet);
			}
			byte[] packetData = new byte[1024];
			int baseIndex = 1024 * (numOfPackets-1);
			for(int i=0;i<dataSize%1024; ++i) {
				packetData[i] = totalRequestData[baseIndex+i];
			}
			Packet packet = new Packet.Builder()
					.setType(5)
                    .setSequenceNumber(sequenceNumber)
                    .setPortNumber(hostAddress.getPort())
                    .setPeerAddress(hostAddress.getAddress())
                    .setPayload(packetData)
                    .create();
			packetsToBeSent.add(packet);
		}
		boolean isAck = false;
		String msg = "Hi " + packetsToBeSent.size();
		Packet p = new Packet.Builder()
                .setType(0)
                .setSequenceNumber(1L)
                .setPortNumber(hostAddress.getPort())
                .setPeerAddress(hostAddress.getAddress())
                .setPayload(msg.getBytes())
                .create();
		
		while(!isAck) {
			channel.send(p.toBuffer(), httpc.routerAddress);
			System.out.println("Sending Hi");
			
			channel.configureBlocking(false);
            Selector selector = Selector.open();
            channel.register(selector, OP_READ);
            selector.select(5000);

            Set<SelectionKey> keys = selector.selectedKeys();
            if(keys.isEmpty()){
                System.out.println("Resending Hi...");
            }

            // We just want a single response.
            ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
            SocketAddress router = channel.receive(buf);
            buf.flip();
            Packet resp = Packet.fromBuffer(buf);
            int type = resp.getType();
            if(type==1)
            	isAck = true;

            keys.clear();
		}
		for(int i=0;i<packetsToBeSent.size()-1;++i) {
			isAck = false;
			while(!isAck) {
				try(DatagramChannel channel2 = DatagramChannel.open()){
					channel2.send(packetsToBeSent.get(i).toBuffer(), httpc.routerAddress);
					System.out.println("Sending packet : " + packetsToBeSent.get(i).getSequenceNumber());
					
					channel2.configureBlocking(false);
		            Selector selector = Selector.open();
		            channel2.register(selector, OP_READ);
		            selector.select(5000);

		            Set<SelectionKey> keys = selector.selectedKeys();
		            if(keys.isEmpty()){
		                System.out.println("Resending packet : " + packetsToBeSent.get(i).getSequenceNumber());
		            }

		            // We just want a single response.
		            ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
		            SocketAddress router = channel2.receive(buf);
		            buf.flip();
		            Packet resp = Packet.fromBuffer(buf);
		            int type = resp.getType();
		            if(type==1)
		            	isAck = true;

		            keys.clear();
				}
			}
		}
		isAck = false;
		while(!isAck) {
				channel.send(packetsToBeSent.get(packetsToBeSent.size()-1).toBuffer(), httpc.routerAddress);
				System.out.println("Sending packet : " + packetsToBeSent.get(packetsToBeSent.size()-1).getSequenceNumber());
				
				channel.configureBlocking(false);
	            Selector selector = Selector.open();
	            channel.register(selector, OP_READ);
	            selector.select(5000);

	            Set<SelectionKey> keys = selector.selectedKeys();
	            if(keys.isEmpty()){
	                System.out.println("Resending packet : " + packetsToBeSent.get(packetsToBeSent.size()-1).getSequenceNumber());
	            }

	            // We just want a single response.
	            ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
	            SocketAddress router = channel.receive(buf);
	            buf.flip();
	            Packet resp = Packet.fromBuffer(buf);
	            int type = resp.getType();
	            if(type==1)
	            	isAck = true;

	            keys.clear();
		}
	}
	
	private static void checkRequestFormat(String[] args) {
		for(int i=0;i<args.length-1;++i) {
			if("-v".equalsIgnoreCase(args[i]))
				isVerbose = true;
			if(args[i].equals("-h")) {
				isHeader = true;
				String[] splitHeader = args[i+1].split(":");
				if(splitHeader.length==2) {
					headerCount = headerCount+1;
					headers = headers  + splitHeader[0].trim() + ":" + splitHeader[1] + "\r\n";
					isValidRequest = true;
				}
				else
					isValidRequest = false;
				i=i+1;
			}
			if(args[i].equals("-d")) {
				isInlineData = true;
				processInlineData(args[i+1]);
				i=i+1;
			}
			if(args[i].equals("-f")) {
				isFile = true;
				processFileData(args[i+1]);
				i=i+1;
			}
		}
		if((isFile && isInlineData) || (!isInlineData && !isFile))
			isValidRequest = false;
		
	}

	private static void processFileData(String rawData) {
		if(rawData.equals("-d") || rawData.equals("-h") || rawData.equals("-f") || rawData.equals("-v")) {
			isValidRequest = false;
			return;
		}
		inlineData = "data from file";
		isFile = true;
	}

	private static void processInlineData(String rawData) {
		if(rawData.equals("-d") || rawData.equals("-h") || rawData.equals("-f") || rawData.equals("-v")) {
			isValidRequest = false;
			return;
		}
		//rawData.replaceAll("\"", "\\\"");
		inlineData = rawData;
		System.out.println(inlineData);
		isInlineData = true;
	}
}