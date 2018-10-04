import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

public class httpc 
{
	public static void main(String[] args) throws UnknownHostException, URISyntaxException, IOException {
		
		if(args.length == 0) {
			System.out.println("Please enter \"httpc help\" to check the usage");
			return;
		}
		if(args.length == 1) {
			if("help".equals(args[0])) {
				printHelp();
			}
			else {
				System.out.println("Please enter \"httpc help\" to check the usage");
			}
			return;
		}
		if(args.length>=2) {
			if("get".equals(args[0]))
				ProcessGet.processGetRequest(args);
			else if("post".equals(args[0]))
				ProcessPost.processPostRequest(args);
			else {
				if(args.length==2 && "help".equals(args[0]) && "get".equals(args[1]))
					printGetHelp();
				else if(args.length==2 && "help".equals(args[0]) && "post".equals(args[1]))
					printPostHelp();
				else {
					System.out.println("Please enter \"httpc help\" to check the usage");
					return;
				}
			}
		}
	}
	
	private static void printPostHelp() {
		System.out.println("usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL");
		System.out.println("Get executes a HTTP GET request for a given URL with inline data or from file.");
		System.out.println("-v \t          Prints the detail of the response such as protocol, status and headers.");
		System.out.println("-h key:value  \tAssociates headers to HTTP Request with the format \'key:value\'");
		System.out.println("-d inline-data\tAssociates an inline data to the body HTTP POST request.");
		System.out.println("-f file       \tAssociates the content of a file to the body HTTP POST request.");
		System.out.println("\nEither [-d] or [-f] can be used but not both.");
	}

	private static void printGetHelp() {
		System.out.println("usage: httpc get [-v] [-h key:value] URL");
		System.out.println("Get executes a HTTP GET request for a given URL.");
		System.out.println("-v\t          Prints the detail of the response such as protocol, status and headers.");
		System.out.println("-h key:value\tAssociates headers to HTTP Request with the format \'key:value\'");
	}

	public static void printHelp() {
		System.out.println("httpc is a curl like application but supports HTTP protocol only");
		System.out.println("Usage:");
		System.out.println("\thttpc command [arguments]");
		System.out.println("The commands are:");
		System.out.println("get \texecutes a HTTP GET request and prints the response.");
		System.out.println("post\texecutes a HTTP POST request and prints the response.");
		System.out.println("help\tprints the screen");
		System.out.println("\nUse \"httpc help [command]\" for more information about a command.");
	}
}
