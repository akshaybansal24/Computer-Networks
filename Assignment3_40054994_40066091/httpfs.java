import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public class httpfs {

	public static boolean isDebugOn = false;
	public static int serverPort = 8080;
	public static String filePath = System.getProperty("user.dir");
	public static ServerSocket serverSocket;
	public static String response = "";
	public static String responseMsg = "";
	public static Packet ClientPacket;

	public static void main(String[] args) throws IOException {
		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("-v")) {
				isDebugOn = true;
			} else if (args[i].equals("-p")) {
				i = i + 1;
				if (i == args.length) {
					printHelpMenu();
					return;
				} else if ("-d".equals(args[i]) || "-v".equals(args[i]) || "-p".equals(args[i])) {
					printHelpMenu();
					return;
				} else {
					try {
						serverPort = Integer.parseInt(args[i]);
					} catch (NumberFormatException exception) {
						printHelpMenu();
						return;
					}
				}
			} else if (args[i].equals("-d")) {
				i = i + 1;
				if (i == args.length) {
					printHelpMenu();
					return;
				} else if ("-d".equals(args[i]) || "-v".equals(args[i]) || "-p".equals(args[i])) {
					printHelpMenu();
					return;
				} else {
					filePath = args[i];
				}
			} else {
				printHelpMenu();
				return;
			}
		}

		DatagramChannel channel = DatagramChannel.open();
		channel.bind(new InetSocketAddress(serverPort));
		if (isDebugOn) {
			System.out.println("Listening for Connection on port " + serverPort + " ...");
		}
		ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN).order(ByteOrder.BIG_ENDIAN);
		String request = "";
		for (;;) {
			buf.clear();
			SocketAddress router = channel.receive(buf);

			// Parse a packet from the received raw data.
			buf.flip();
			Packet packet = Packet.fromBuffer(buf);
			buf.flip();
			int type = packet.getType();
			String payload = new String(packet.getPayload(), UTF_8);
			//System.out.println(payload);
			if (type == 0 && !payload.equals("Hi")) {
				request = request + payload;
				Packet resp = packet.toBuilder().setPayload("".getBytes()).setType(1).create();
				channel.send(resp.toBuffer(), router);
			}
			if (type == 0 && payload.equals("Hi")) {
				Packet resp = packet.toBuilder().setPayload("Hi".getBytes()).setType(1).create();
				channel.send(resp.toBuffer(), router);
			}
			if (type == 5) {
				request = request + payload;
				Packet resp = packet.toBuilder().setPayload("".getBytes()).setType(1).create();
				channel.send(resp.toBuffer(), router);
				ClientPacket = packet.toBuilder().create();
				System.out.println(request);
				if (request.startsWith("GET")) {
					handleGet(request);
				} else if (request.startsWith("POST")) {
					handlePost(request);
				}
				makeUDPCall(response, ClientPacket.getPeerAddress(), ClientPacket.getPeerPort(), channel);
				request = "";
			}
		}

		/*
		 * serverSocket = new ServerSocket(serverPort); if(isDebugOn) {
		 * System.out.println("Listening for Connection on port " + serverPort +
		 * " ..."); } Socket clientSocket; while(true) { try { clientSocket =
		 * serverSocket.accept(); new HandleRequest(clientSocket).start(); }
		 * catch(IOException exception) { if(isDebugOn) {
		 * System.out.println("Error in making connection...");
		 * exception.printStackTrace(); } } }
		 */
	}

	private static void printHelpMenu() {
		System.out.println("\n\nPlease refer to the usage below:\n");
		System.out.println("https [-v] [-p PORT] [-d PATH-TO-DIR]");
		System.out.println("\n\n\t-v\tPrints Debugging Messages.");
		System.out.println("\n\t-p\tSpecifies the port number the server will listen and serve at.");
		System.out.println("\t  \tDefault is 8080");
		System.out.println(
				"\n\t-d\tSpecifies the directory the server will use to read/write requested files. Default is the current directory when launching the application.");
	}

	public static void handleGet(String requestString) {
		if (httpfs.isDebugOn) {
			System.out.println("Here to process get request...");
		}
		String splitRequest[] = requestString.split(" ");
		String path = splitRequest[1];
		if (path.length() == 1 && path.equals("/")) {
			File folder = new File(httpfs.filePath);
			File listOfFiles[] = folder.listFiles();
			for (int i = 0; i < listOfFiles.length; ++i) {
				if (listOfFiles[i].isFile()) {
					if (!listOfFiles[i].getName().contains(".java"))
						responseMsg += listOfFiles[i].getName() + "\n";
				}
			}
			response += "HTTP/1.0 200 OK\r\n";
			response += "Connection: close\r\n";
			response += "Server: localhost:8080\r\n";
			response += "Content-Type: Text/Plain\r\n";
			response += "Content-Disposition: inline\r\n";
			response += "Content-Length: " + responseMsg.length() + "\r\n\r\n";
			response += responseMsg + "\r\n";
			return;
		} else if (path.length() > 1) {

			String checkFilePath[] = path.split("/");
			if (checkFilePath.length > 2) {
				response += "HTTP/1.0 401 Access Denied\r\n";
				response += "Content-Type: Text/HTML\r\n";
				response += "Content-Disposition: inline\r\n";
				responseMsg = "The access to requested file is denied.";
				response += "Content-Length: " + responseMsg.length() + "\r\n";
				response += "Date : " + new Date() + "\r\n\r\n";
				response += responseMsg + "\r\n";
			} else {
				String pathToRead = httpfs.filePath + path;
				String fileType = "";
				if (path.contains("."))
					fileType = path.substring(path.lastIndexOf("."));
				// System.out.println(fileType);
				File fileToRead = new File(pathToRead);
				try {
					BufferedReader reader = new BufferedReader(new FileReader(fileToRead));
					String fileContent;
					while ((fileContent = reader.readLine()) != null) {
						responseMsg += fileContent + "\n";
					}
					reader.close();
					response += "HTTP/1.0 200 OK\r\n";
					response += "Connection: close\r\n";
					response += "Server: localhost:8080\r\n";
					if (fileType.equalsIgnoreCase(".html"))
						response += "Content-Type: Text/HTML\r\n";
					else if (fileType.equalsIgnoreCase(".json"))
						response += "Content-Type: Application/JSON\r\n";
					else
						response += "Content-Type: Text/HTML\r\n";
					response += "Content-Disposition: inline\r\n";
					response += "Content-Length: " + responseMsg.length() + "\r\n\r\n";
					response += responseMsg + "\r\n";
				} catch (FileNotFoundException e) {
					response += "HTTP/1.0 404 File Not Found\r\n";
					response += "Content-Type: Text/HTML\r\n";
					responseMsg = "The requested file is not found.";
					response += "Content-Length: " + responseMsg.length() + "\r\n";
					response += "Date : " + new Date() + "\r\n\r\n";
					response += responseMsg + "\r\n";
					if (httpfs.isDebugOn)
						e.printStackTrace();
				} catch (IOException e) {
					response += "HTTP/1.0 500 Internal Server Error\r\n";
					response += "Content-Type: Text/HTML\r\n";
					responseMsg = "Not able to read the request.";
					response += "Content-Length: " + responseMsg.length() + "\r\n";
					response += "Date : " + new Date() + "\r\n\r\n";
					response += responseMsg + "\r\n";
					if (httpfs.isDebugOn)
						e.printStackTrace();
				}
			}
			return;
		}
	}

	public static void handlePost(String requestString) {
		if (httpfs.isDebugOn)
			System.out.println(requestString);
		if (httpfs.isDebugOn) {
			System.out.println("Here to process get request...");
		}
		String dataToBeWritten = requestString.split("INLINE_DATA")[1];
		String splitRequest[] = requestString.split(" ");
		String path = splitRequest[1];
		String checkFilePath[] = path.split("/");
		if (checkFilePath.length > 2) {
			response += "HTTP/1.0 401 Access Denied\r\n";
			response += "Content-Type: Text/HTML\r\n";
			responseMsg = "The access to requested file is denied.";
			response += "Content-Length: " + responseMsg.length() + "\r\n";
			response += "Date : " + new Date() + "\r\n\r\n";
			response += responseMsg + "\r\n";
		} else {
			String pathToRead = httpfs.filePath + path;
			File fileToWrite = new File(pathToRead);
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(fileToWrite));
				FileChannel channel = new RandomAccessFile(fileToWrite, "rw").getChannel();
				FileLock lock = channel.lock();
				writer.write(dataToBeWritten);
				writer.close();
				lock.release();
				channel.close();
				responseMsg += "Successfully written in the file.";
				response += "HTTP/1.0 200 OK\r\n";
				response += "Connection: close\r\n";
				response += "Server: localhost:8080\r\n";
				response += "Content-Type: Text/HTML\r\n";
				response += "Content-Length: " + responseMsg.length() + "\r\n\r\n";
				response += responseMsg + "\r\n";
			} catch (FileNotFoundException e) {
				response += "HTTP/1.0 500 Unable to create the file\r\n";
				response += "Content-Type: Text/HTML\r\n";
				responseMsg = "The requested file is not found.";
				response += "Content-Length: " + responseMsg.length() + "\r\n";
				response += "Date : " + new Date() + "\r\n\r\n";
				response += responseMsg + "\r\n";
				if (httpfs.isDebugOn)
					e.printStackTrace();
			} catch (IOException e) {
				response += "HTTP/1.0 500 Internal Server Error\r\n";
				response += "Content-Type: Text/HTML\r\n";
				responseMsg = "Not able to read the request.";
				response += "Content-Length: " + responseMsg.length() + "\r\n";
				response += "Date : " + new Date() + "\r\n\r\n";
				response += responseMsg + "\r\n";
				if (httpfs.isDebugOn)
					e.printStackTrace();
			}
		}
	}

	private static void makeUDPCall(String requestToBeMade, InetAddress host, int port, DatagramChannel channel)
			throws IOException {
		ArrayList<Packet> packetsToBeSent = new ArrayList<Packet>();
		byte[] totalRequestData = requestToBeMade.getBytes();
		InetSocketAddress hostAddress = new InetSocketAddress(host, port);
		long sequenceNumber = 0;
		int dataSize = totalRequestData.length;
		int numOfPackets = 0;
		if (dataSize % 1024 == 0) {
			numOfPackets = dataSize / 1024;
			for (int i = 0; i < numOfPackets; ++i) {
				byte[] packetData = new byte[1024];
				for (int j = 0; j < 1024; ++j) {
					packetData[j] = totalRequestData[1024 * i + j];
				}
				Packet packet;
				if (i < numOfPackets - 1) {
					packet = new Packet.Builder().setType(0).setSequenceNumber(sequenceNumber++)
							.setPortNumber(hostAddress.getPort()).setPeerAddress(hostAddress.getAddress())
							.setPayload(packetData).create();
				} else {
					packet = new Packet.Builder().setType(5).setSequenceNumber(sequenceNumber++)
							.setPortNumber(hostAddress.getPort()).setPeerAddress(hostAddress.getAddress())
							.setPayload(packetData).create();
				}
				packetsToBeSent.add(packet);
			}
		} else {
			numOfPackets = dataSize / 1024 + 1;
			for (int i = 0; i < numOfPackets - 1; ++i) {
				byte[] packetData = new byte[1024];
				for (int j = 0; j < 1024; ++j) {
					packetData[j] = totalRequestData[1024 * i + j];
				}
				Packet packet = new Packet.Builder().setType(0).setSequenceNumber(sequenceNumber++)
						.setPortNumber(hostAddress.getPort()).setPeerAddress(hostAddress.getAddress())
						.setPayload(packetData).create();
				packetsToBeSent.add(packet);
			}
			byte[] packetData = new byte[1024];
			int baseIndex = 1024 * (numOfPackets - 1);
			for (int i = 0; i < dataSize % 1024; ++i) {
				packetData[i] = totalRequestData[baseIndex + i];
			}
			Packet packet = new Packet.Builder().setType(5).setSequenceNumber(sequenceNumber)
					.setPortNumber(hostAddress.getPort()).setPeerAddress(hostAddress.getAddress())
					.setPayload(packetData).create();
			packetsToBeSent.add(packet);
		}
		boolean isAck = false;

		for (int i = 0; i < packetsToBeSent.size(); ++i) {
			isAck = false;
			while (!isAck) {

				channel.send(packetsToBeSent.get(i).toBuffer(), httpc.routerAddress);
				System.out.println("Sending packet : " + packetsToBeSent.get(i).getSequenceNumber());

				channel.configureBlocking(false);
				Selector selector = Selector.open();
				channel.register(selector, OP_READ);
				selector.select(5000);

				Set<SelectionKey> keys = selector.selectedKeys();
				if (keys.isEmpty()) {
					System.out.println("Resending packet : " + packetsToBeSent.get(i).getSequenceNumber());
				}

				// We just want a single response.
				ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
				SocketAddress router = channel.receive(buf);
				buf.flip();
				Packet resp = Packet.fromBuffer(buf);
				int type = resp.getType();
				if (type == 1)
					isAck = true;

				keys.clear();
			}
		}
	}
}
