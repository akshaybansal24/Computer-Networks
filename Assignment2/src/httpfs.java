import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class httpfs {
	
	public static boolean isDebugOn = false;
	public static int serverPort = 8080;
	public static String filePath = System.getProperty("user.dir");
	public static ServerSocket serverSocket;

	public static void main(String[] args) throws IOException {
		for(int i=0;i<args.length;++i) {
			if(args[i].equals("-v")) {
				isDebugOn = true;
			}
			else if(args[i].equals("-p")) {
				i=i+1;
				if(i==args.length) {
					printHelpMenu();
					return;
				}
				else if("-d".equals(args[i]) || "-v".equals(args[i]) || "-p".equals(args[i])) {
					printHelpMenu();
					return;
				}
				else {
					try {
						serverPort = Integer.parseInt(args[i]);
					}catch(NumberFormatException exception) {
						printHelpMenu();
						return;
					}
				}
			}
			else if(args[i].equals("-d")) {
				i=i+1;
				if(i==args.length) {
					printHelpMenu();
					return;
				}
				else if("-d".equals(args[i]) || "-v".equals(args[i]) || "-p".equals(args[i])) {
					printHelpMenu();
					return;
				}
				else {
					filePath = args[i];
				}
			}
			else {
				printHelpMenu();
				return;
			}
		}
		serverSocket = new ServerSocket(serverPort);
		if(isDebugOn) {
			System.out.println("Listening for Connection on port " + serverPort + " ...");
		}
		Socket clientSocket;
		while(true) {
			try {
				clientSocket = serverSocket.accept();
				new HandleRequest(clientSocket).start();
			}
			catch(IOException exception) {
				if(isDebugOn) {
					System.out.println("Error in making connection...");
					exception.printStackTrace();
				}
			}
		}
	}

	private static void printHelpMenu() {
		System.out.println("\n\nPlease refer to the usage below:\n");
		System.out.println("https [-v] [-p PORT] [-d PATH-TO-DIR]");
		System.out.println("\n\n\t-v\tPrints Debugging Messages.");
		System.out.println("\n\t-p\tSpecifies the port number the server will listen and serve at.");
		System.out.println("\t  \tDefault is 8080");
		System.out.println("\n\t-d\tSpecifies the directory the server will use to read/write requested files. Default is the current directory when launching the application.");
	}
}


