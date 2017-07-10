import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.Vector;

class Matching extends Thread {
	static Vector<SocketState> MatchingVector;
	InputStream in;
	
	Matching() {
		MatchingVector = new Vector<SocketState>();
	}
	public void run() {
		while(true) {
			if(Server.WaitVector.size()<2)
				continue;
			if(Server.WaitVector.elementAt(0).isConnected==false) {
				try {
					Server.WaitVector.elementAt(0).socket.close();
					Server.WaitVector.remove(0);
					continue;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(Server.WaitVector.elementAt(1).isConnected==false) {
				try {
					Server.WaitVector.elementAt(1).socket.close();
					Server.WaitVector.remove(1);
					continue;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			MatchingVector.add(Server.WaitVector.elementAt(0));
			MatchingVector.add(Server.WaitVector.elementAt(1));
			
			//매칭이 되었으므로 대기 벡터에서 제거
			Server.WaitVector.remove(1);
			Server.WaitVector.remove(0);
			
			int lastIndex = MatchingVector.size()-1;
			
			//각각 클라이언트의 상대방의 인덱스를 지정
			MatchingVector.elementAt(lastIndex).opponent_index = lastIndex-1;
			MatchingVector.elementAt(lastIndex-1).opponent_index = lastIndex;
			
			//상대방의 ID를 지정
			MatchingVector.elementAt(lastIndex).opponent_nickname = MatchingVector.elementAt(lastIndex-1).nickname;
			MatchingVector.elementAt(lastIndex-1).opponent_nickname= MatchingVector.elementAt(lastIndex).nickname;
			
			//각각 상대방의 닉네임 및 진영정보를 보내기 위한 준비작업
			//전체길이 + 진영정보(1 or 2) + 상대방 닉네임
			DecimalFormat df = new DecimalFormat("000");
        	String nickname = "1" + MatchingVector.elementAt(lastIndex-1).opponent_nickname;
        	String nickname_len = df.format(nickname.length());
        	
        	DecimalFormat df2 = new DecimalFormat("000");
        	String nickname2 = "2" + MatchingVector.elementAt(lastIndex).opponent_nickname;
        	String nickname_len2 = df2.format(nickname2.length());
        	
        	//문자열을 각각의 클라이언트에게 보낸다.
        	BufferedWriter out;
        	BufferedWriter out2;
        	
			try {
				out = new BufferedWriter(new OutputStreamWriter(MatchingVector.elementAt(lastIndex-1).socket.getOutputStream()));
				out.write(nickname_len+nickname);
	        	out.flush();
	        	
	        	out2 = new BufferedWriter(new OutputStreamWriter(MatchingVector.elementAt(lastIndex).socket.getOutputStream()));
				out2.write(nickname_len2+nickname2);
	        	out2.flush();
	        	
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			System.out.println("Matching Success!");
			
			ReceiveTextThread t1 = new ReceiveTextThread(MatchingVector.elementAt(lastIndex-1));
			ReceiveTextThread t2 = new ReceiveTextThread(MatchingVector.elementAt(lastIndex));
			t1.start();
			t2.start();
			
		}
	}
}


class ReceiveTextThread extends Thread {
	SocketState socketState;
	ReceiveTextThread(SocketState _socketState) {
		socketState = _socketState;
	}
	public void run() {
		while(true) {
			try {
				//클라이언트로부터 받은 메시지를 해당 클라이언트와 게임중인 클라이언트에게 전달
				
				InputStream in = socketState.socket.getInputStream();
				byte arrlen[] = new byte[3];
				if(in.read(arrlen)==-1) {
					socketState.isConnected = false;
					socketState.socket.close();
					//연결이 끊겼음을 상대방에게 알리는 부분을 작성한다.
					
					
					return;
				}
				
				int textlen = Integer.parseInt(new String(arrlen));
				byte textBuffer[] = new byte[textlen];
				if (in.read(textBuffer)==-1) {
					socketState.isConnected = false;
					socketState.socket.close();
					return;
				}
				String received_text = new String(textBuffer);
				
				
				//해당 클라이언트와 게임중인 상대방 클라이언트에게 전달
				SendToClient(received_text);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	void SendToClient(String received_text) {
		DecimalFormat df = new DecimalFormat("000");
		
		String textlen = df.format(received_text.getBytes().length);
		try {
			//Matching 벡터에서 opponent index값을 통해 상대방 socketState를 얻는다.
			SocketState opponent = Matching.MatchingVector.elementAt(socketState.opponent_index);
			
			//상대방 클라이언트에게 메시지 전송
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(opponent.socket.getOutputStream()));
			out.write(textlen+received_text);
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}