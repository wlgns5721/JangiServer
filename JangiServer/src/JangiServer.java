
public class JangiServer {

	public static void main(String[] args) {
		Server server = new Server();
		Matching matching = new Matching();
		
		System.out.println("wait to connect.......");
		server.InitServer();
		matching.start();
		server.ConnectWithClient();		
	}

}



