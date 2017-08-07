package acl.replication;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import acl.command.*;

public class BroadcasterThrift extends Broadcaster {

	private HashSet<BroadcastService.Client> replicas = new HashSet<BroadcastService.Client>();
	
	
	private void log(String hostAddr) {
		try {
			System.out.println("Will add " + hostAddr);
			TTransport transport = new TFramedTransport(new TSocket(hostAddr, 5052));
			while(!openTransport(transport)){
				System.out.println("Sleeping...");
				
				TimeUnit.SECONDS.sleep(5);
					
			}

				
			TProtocol protocol = new TBinaryProtocol(transport);
			BroadcastService.Client cl = new BroadcastService.Client(protocol);
			replicas.add(cl);
				
			System.out.println("Added " + hostAddr);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	  
	
	private boolean openTransport(TTransport transport){
		try{
			transport.open();
			return true;
		} catch(Exception e){
			return false;
		}
	}
	
	
	//should return sucess/failure?
	public void broadcast(CommandData data){
		BroadcastCommand cmd = new BroadcastCommand(CommandData.getIntFromType(data.getCmd()),
													data.getArguments());
		for(BroadcastService.Client cl: replicas){
			
			ThreadMethod r = new ThreadMethod(cl, cmd);
			new Thread(r).start();
		}

		Debug.delay = false;
	}
	
	public void initialize(){		String retAddr = System.getenv("ACL_LINKS");
		for(String addr: retAddr.split(":")){
			log(addr);
		}		
	}
	
	class ThreadMethod implements Runnable{
		ThreadMethod(BroadcastService.Client cl, BroadcastCommand cmd){
			this.cl = cl;
			this.cmd = cmd;
		}
		
		private BroadcastService.Client cl;
		private BroadcastCommand cmd;
		private boolean delay = Debug.delay;
		
		public void run(){
			if(delay){
				try {
					TimeUnit.SECONDS.sleep(60);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			try{
				cl.send(cmd);
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	}
}
