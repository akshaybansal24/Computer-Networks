import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

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
		Socket socket = new Socket(host, port);
		PrintWriter request = new PrintWriter(socket.getOutputStream());
		String requestToBeMade = "POST " + path + " HTTP/1.0\r\n";
		requestToBeMade += "Host: " + host + "\r\n";
		requestToBeMade += "User-Agent: Concordia-HTTP/1.0" + "\r\n";
		requestToBeMade += "Content-Length: "+inlineData.length() + "\r\n";
		if(isHeader && headerCount>0) {
			requestToBeMade += headers;
		}
		requestToBeMade += "\r\n";
		requestToBeMade += inlineData + "\r\n";
		System.out.println(requestToBeMade);
		request.print(requestToBeMade);
		request.flush();
		InputStream inStream = socket.getInputStream( ); 
		BufferedReader rd = new BufferedReader(
		        new InputStreamReader(inStream));
		String line;
		String response = "";
		while ((line = rd.readLine()) != null) {
		   response += line+"\n";
		}
		if(isVerbose)
			System.out.println(response);
		else {
			String nonVerboseResp = response.substring(response.indexOf("{"));
			System.out.println(nonVerboseResp);
		}
		socket.close();
		
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
