import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Date;

public class HandleRequest extends Thread {
	private Socket clientSocket;
	private String response;
	private String responseMsg;
	public HandleRequest(Socket clientSocket) {
		this.clientSocket = clientSocket;
		response = "";
		responseMsg = "";
	}
	
	public void run() {
		
		InputStream inputStream = null;
		BufferedReader bufferedReader = null;
		DataOutputStream outputStream = null;
		try {
			inputStream = clientSocket.getInputStream();
			bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			outputStream = new DataOutputStream(clientSocket.getOutputStream());
		} catch (IOException e) {
			response += "HTTP/1.0 500 Internal Server Error\r\n";
			response += "Content-Type: Text/HTML\r\n";
			response += "Content-Disposition: inline\r\n";
			responseMsg = "Was not able to eatablish proper connection.";
			response += "Content-Length: "+responseMsg.length() + "\r\n";
			response += "Date : " + new Date() + "\r\n\r\n";
			response += responseMsg + "\r\n";
			if(httpfs.isDebugOn)
				e.printStackTrace();
		}
		StringBuffer request = new StringBuffer("");
		String line="";
		try {
			while((line = bufferedReader.readLine()) != null) {
				if(line.equals(""))
					break;
				request.append(line);
			}
			
		}
		catch(IOException exception) {
			response += "HTTP/1.0 500 Internal Server Error\r\n";
			response += "Content-Type: Text/HTML\r\n";
			response += "Content-Disposition: inline\r\n";
			responseMsg = "Not able to read the request.";
			response += "Content-Length: "+responseMsg.length() + "\r\n";
			response += "Date : " + new Date() + "\r\n\r\n";
			response += responseMsg + "\r\n";
			if(httpfs.isDebugOn)
				exception.printStackTrace();
		}
		if(httpfs.isDebugOn) {
			System.out.println("Request to be processed is : \n" + request.toString());
		}
		String requestString = request.toString();
		if(requestString.startsWith("GET")) {
			handleGet(requestString);
		}else if(requestString.startsWith("POST")) {
			handlePost(requestString);
		}
		try {
			if(httpfs.isDebugOn) {
				System.out.println(response);
			}
			outputStream.writeBytes(response + "\n\r");
			outputStream.flush();
			clientSocket.close();
		} catch (IOException e) {
			if(httpfs.isDebugOn)
				e.printStackTrace();
		}
	}
	
	public void handleGet(String requestString) {
		if(httpfs.isDebugOn) {
			System.out.println("Here to process get request...");
		}
		String splitRequest[] = requestString.split(" ");
		String path = splitRequest[1];
		if(path.length()==1 && path.equals("/")) {
			File folder = new File(httpfs.filePath);
			File listOfFiles[] = folder.listFiles();
			for(int i=0;i<listOfFiles.length;++i) {
				if(listOfFiles[i].isFile()) {
					if(!listOfFiles[i].getName().contains(".java"))
						responseMsg += listOfFiles[i].getName() + "\n";
				}
			}
			response += "HTTP/1.0 200 OK\r\n";
			response += "Connection: close\r\n";
			response += "Server: localhost:8080\r\n";
			response += "Content-Type: Text/Plain\r\n";
			response += "Content-Disposition: inline\r\n";
			response += "Content-Length: "+responseMsg.length() + "\r\n\r\n";
			response += responseMsg + "\r\n";
			return;
		}
		else if(path.length()>1) {
			
			String checkFilePath[] = path.split("/");
			if(checkFilePath.length>2)
			{
				response += "HTTP/1.0 401 Access Denied\r\n";
				response += "Content-Type: Text/HTML\r\n";
				response += "Content-Disposition: inline\r\n";
				responseMsg = "The access to requested file is denied.";
				response += "Content-Length: "+responseMsg.length() + "\r\n";
				response += "Date : " + new Date() + "\r\n\r\n";
				response += responseMsg + "\r\n";
			}
			else{
				String pathToRead = httpfs.filePath + path;
				String fileType = "";
				if(path.contains("."))
					fileType = path.substring(path.lastIndexOf("."));
				//System.out.println(fileType);
				File fileToRead = new File(pathToRead);
				try {
					BufferedReader reader = new BufferedReader(new FileReader(fileToRead));
					String fileContent;
					while((fileContent = reader.readLine()) != null) {
						responseMsg += fileContent + "\n";
					}
					reader.close();
					response += "HTTP/1.0 200 OK\r\n";
					response += "Connection: close\r\n";
					response += "Server: localhost:8080\r\n";
					if(fileType.equalsIgnoreCase(".html"))
						response += "Content-Type: Text/HTML\r\n";
					else if(fileType.equalsIgnoreCase(".json"))
						response += "Content-Type: Application/JSON\r\n";
					else
						response += "Content-Type: Text/HTML\r\n";
					response += "Content-Disposition: inline\r\n";
					response += "Content-Length: "+responseMsg.length() + "\r\n\r\n";
					response += responseMsg + "\r\n";
				} catch (FileNotFoundException e) {
					response += "HTTP/1.0 404 File Not Found\r\n";
					response += "Content-Type: Text/HTML\r\n";
					responseMsg = "The requested file is not found.";
					response += "Content-Length: "+responseMsg.length() + "\r\n";
					response += "Date : " + new Date() + "\r\n\r\n";
					response += responseMsg + "\r\n";
					if(httpfs.isDebugOn)
						e.printStackTrace();
				} catch (IOException e) {
					response += "HTTP/1.0 500 Internal Server Error\r\n";
					response += "Content-Type: Text/HTML\r\n";
					responseMsg = "Not able to read the request.";
					response += "Content-Length: "+responseMsg.length() + "\r\n";
					response += "Date : " + new Date() + "\r\n\r\n";
					response += responseMsg + "\r\n";
					if(httpfs.isDebugOn)
						e.printStackTrace();
				}
			}
			return;
		}
	}
	
	public void handlePost(String requestString) {
		if(httpfs.isDebugOn)
			System.out.println(requestString);
		if(httpfs.isDebugOn) {
			System.out.println("Here to process get request...");
		}
		String dataToBeWritten = requestString.split("INLINE_DATA")[1];
		String splitRequest[] = requestString.split(" ");
		String path = splitRequest[1];
		String checkFilePath[] = path.split("/");
		if(checkFilePath.length>2)
		{
			response += "HTTP/1.0 401 Access Denied\r\n";
			response += "Content-Type: Text/HTML\r\n";
			responseMsg = "The access to requested file is denied.";
			response += "Content-Length: "+responseMsg.length() + "\r\n";
			response += "Date : " + new Date() + "\r\n\r\n";
			response += responseMsg + "\r\n";
		}
		else{
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
				response += "Content-Length: "+responseMsg.length() + "\r\n\r\n";
				response += responseMsg + "\r\n";
			} catch (FileNotFoundException e) {
				response += "HTTP/1.0 500 Unable to create the file\r\n";
				response += "Content-Type: Text/HTML\r\n";
				responseMsg = "The requested file is not found.";
				response += "Content-Length: "+responseMsg.length() + "\r\n";
				response += "Date : " + new Date() + "\r\n\r\n";
				response += responseMsg + "\r\n";
				if(httpfs.isDebugOn)
					e.printStackTrace();
			} catch (IOException e) {
				response += "HTTP/1.0 500 Internal Server Error\r\n";
				response += "Content-Type: Text/HTML\r\n";
				responseMsg = "Not able to read the request.";
				response += "Content-Length: "+responseMsg.length() + "\r\n";
				response += "Date : " + new Date() + "\r\n\r\n";
				response += responseMsg + "\r\n";
				if(httpfs.isDebugOn)
					e.printStackTrace();
			}
		}
	}
}
