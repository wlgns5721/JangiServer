import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.Vector;


class Server {
	final int PORT_MAX_NUMBER = 51000;
	final int PORT_FIRST_SOCKET = 50000;    //임시로 설정
	
	final int PORT_EXTERNAL_PORT_NUMBER = 52000; 
	
	static Vector<SocketState> WaitVector;
	private Vector<Integer> UsablePort;
	private int portNumber;
	private int client_id; //클라이언트에게 부여할 id
	void InitServer() {
		UsablePort = new Vector<Integer>();
		
		//사용가능한 포트번호 벡터에 50000~51000을 넣는다.
		for(int i=PORT_FIRST_SOCKET; i<=PORT_MAX_NUMBER; i++)
			UsablePort.add(i);
		
		//client에게 부여할 id는 1부터 시작한다. client가 자신의 id가 0이라고 한다면, 신규 클라이언트
		client_id = 1;
		WaitVector = new Vector<SocketState>();
	}
	
	//client와 연결을 한다. 연결이 되는 동시에 클라이언트로부터 nickname, ip정보를 받는다.
	void ConnectWithClient() {
		ServerSocket firstServerSocket;  //모든 클라이언트가 첫번째로 접속하는 서버소켓 
		ServerSocket secondServerSocket; //각 클라이언트마다 할당되는 두번째 소켓
		BufferedWriter out;
		int received_id;
		String nickname;
		int id_index;
		
		try {
			//사용가능한 포트가 없다면 대기한다.
			while(UsablePort.isEmpty());
			
			//우선 52000포트로 연결이 성립이 된다면 클라이언트에게 통신을 위한 포트 번호를 알려준다.
			firstServerSocket = new ServerSocket(PORT_EXTERNAL_PORT_NUMBER);
			Socket firstSocket = firstServerSocket.accept();
			
			//UsablePort의 마지막 값 pop
			portNumber = UsablePort.lastElement();
			UsablePort.remove(UsablePort.size()-1);
			
			System.out.println("processing connect to client....");
			DecimalFormat df = new DecimalFormat("000");
        	String portNum_str = Integer.toString(portNumber);
        	String portNum_len = df.format(portNum_str.length());
  
        	//각 클라이언트에게 두번째 포트번호를 알려준다.
        	out = new BufferedWriter(new OutputStreamWriter(firstSocket.getOutputStream()));
        	out.write(portNum_len+portNum_str);
        	out.flush();
        	
        	System.out.println("allocate new port to client.....port number :"+portNumber);
			
			secondServerSocket = new ServerSocket(portNumber);
			Socket socket = secondServerSocket.accept();
			
			System.out.println("second socket conneted!");
			
			//첫번째 소켓은 연결을 끊는다.
			firstServerSocket.close();
			
			InputStream in = socket.getInputStream();
			
			
			//client로부터 받는 문자열의 포맷은 전체문자열길이+nickname길이+nickname+id
			byte bytelen[] = new byte[3];
			in.read(bytelen);
			int len = Integer.parseInt(new String(bytelen));
			byte textBuffer[] = new byte[len];
			in.read(textBuffer);
			String received_text = new String(textBuffer);
			
			//받은 문자열 파싱
			id_index = Integer.parseInt(received_text.substring(0, 2)) + 2;
            nickname = received_text.substring(2, id_index);
            received_id = Integer.parseInt(received_text.substring(id_index));
            
            //id가 0이라면 새로운 클라이언트
            if(received_id==0) {
            	//대기 백터에 저장
            	WaitVector.add(new SocketState(socket,nickname,received_id,portNumber));
            	
            	//문자열 형식으로 보내기 위해 변환하는 작업.
            	//문자열의길이+문자열 형식으로 보낸다.
            	df = new DecimalFormat("00000");
            	String id_str = Integer.toString(client_id);
            	String idlen = df.format(id_str.length());
            	
            	//새로운 클라이언트에게 새로운 id를 할당한다.
            	out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            	out.write(idlen+id_str);
            	out.flush();
            	
            	//client_id값을 증가시켜 다음 클라이언트에게는 다른 아이디를 부여하도록 한다.
            	client_id++;
            }
            //이미 있는 아이디일 경우, 다른 정보들을 갱신한다.
            else {          
            	for(int i=0; i<Matching.MatchingVector.size(); i++) {
            		if(Matching.MatchingVector.elementAt(i).id==received_id) {
            			Matching.MatchingVector.elementAt(i).socket = socket;
            			Matching.MatchingVector.elementAt(i).isConnected=true;
            			Matching.MatchingVector.elementAt(i).port = portNumber;
            			Matching.MatchingVector.elementAt(i).nickname = nickname;
            		}
            	}
            }
            System.out.println("Success!");
            //다음 클라이언트를 받기 위해 재귀적으로 실행
            ConnectWithClient();
            
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
}

class SocketState {
	Socket socket;
	String nickname;
	int id;
	int port;
	int opponent_index;
	String opponent_nickname;
	boolean isConnected;
	SocketState(Socket _socket, String _nickname, int _id, int _port) {
		socket = _socket;
		nickname = _nickname;
		id = _id;
		port = _port;
		opponent_nickname=null;
		isConnected = true;
	}
	
}
