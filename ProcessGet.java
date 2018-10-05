import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

public class ProcessGet {
	
	public static boolean isVerbose = false;
	public static boolean isHeader = false;
	public static int headerCount = 0;
	public static String headers = "";
	public static String URL = "";
	public static boolean isValidRequest = true;
	public static void processGetRequest(String[] args) throws UnknownHostException, URISyntaxException, IOException {
		URL = args[args.length-1];
		checkIsVerbose(args);
		checkForHeaders(args);
		if(!isValidRequest) {
			System.out.println("Please enter \"httpc help get\" to check the usage");
			return;
		}
		makeGetCall();
	}

	private static void makeGetCall() throws URISyntaxException, UnknownHostException, IOException {
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
		String requestToBeMade = "GET " + path + " HTTP/1.0\r\n";
		requestToBeMade += "Host: " + host + "\r\n";
		requestToBeMade += "User-Agent: Concordia-HTTP/1.0" + "\r\n";
		if(isHeader && headerCount>0) {
			requestToBeMade += headers;
		}
		requestToBeMade += "\r\n";
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

	private static void checkIsVerbose(String[] args) {
		for(int i=0; i<args.length-1;++i) {
			if(args[i].equals("-v"))
				isVerbose = true;
		}
	}

	private static void checkForHeaders(String args[]) {
		if("-h".equals(args[args.length-2])){
			isValidRequest = false;
			return;
		}
		for(int i=0; i<args.length-2;++i) {
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
			if(args[i].equals("-d") || args[i].equals("-f")) {
				isValidRequest = false;
			}
		}	
	}

}
