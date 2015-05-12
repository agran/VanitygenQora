import java.security.SecureRandom;
import java.util.Scanner;
import java.util.Random;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import qora.crypto.Base58;
import qora.crypto.Crypto;
import qora.crypto.Ed25519;
import utils.Pair;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;


public class Vanity {

	public static boolean done = false; 
	public static String pattern = "";
	public static int counter = 0;
	public static long startTime = 0;

	@SuppressWarnings("resource")
	public static void main(String args[])
	{	
			Ed25519.load();
	
			Scanner scanner = new Scanner(System.in);
			
			System.out.println("VanitygenQora 1.2.0 (c) agran@agran.net");
				
			if(args.length>0 && args[0].length()>40)
			{
				
				byte[] seed = Base58.decode(args[0]);;
				
				int nonce = 0;
				
			    System.out.println("wallet seed: " + args[0]);

			    while(nonce<10)
				{
					byte[] accountSeed = generateAccountSeed(seed, nonce);
					
					Pair<byte[], byte[]> keyPair = Crypto.getInstance().createKeyPair(accountSeed);
					byte[] publicKey = keyPair.getB();
					String address = Crypto.getInstance().getAddress(publicKey);
					
				    String doneseedaddress = Base58.encode(accountSeed);
				    
				    System.out.println("nonce: " + nonce + " address: " + address + " address seed: " + doneseedaddress);
				    
				    nonce ++;
				}
				
			    new java.util.Scanner(System.in).nextLine();
				System.exit(0);
			}
			
			do {
				System.out.print("Enter the beginning of the Qora-address (with letter Q): ");
				
				String command = "";
				if(args.length>0)
				{
					System.out.println(args[0]);
					command = args[0];
				}
				else
				{	
					command = scanner.nextLine();
				}
				
				if(command.equals("quit"))
				{
					scanner.close();
					System.exit(0);
				}
					
				boolean check = true;
				for (int m=0; m<=command.length()-1; m++){
					String sub_str = command.substring(m, m+1);
					if(Base58.ALPHABET.indexOf(sub_str) == -1)
					{
						check = false;
					}
				}
				
				pattern = command;
				
				if(!check)
				{
					System.out.println("\"" + command + "\" contains invalid characters.");
					pattern = "";
				}
				
				if(!command.startsWith("Q"))
				{
					System.out.println("Pattern must begin with Q.");
					pattern = "";
				}
				
				if(args.length>0 && pattern == "")
				{
					scanner.close();
					System.exit(0);
				}
				
			} while (pattern == "");
			
			System.out.println("The search has begun. Please wait...");
			System.out.println();
			
			startTime = System.currentTimeMillis()/1000;
			
			int availableProcessors = Runtime.getRuntime().availableProcessors();
			for (int m=0; m<=availableProcessors-1; m++){
				Runnable r = new MyRunnable();
				Thread t = new Thread(r);
				t.setPriority( Thread.MIN_PRIORITY ); 
				t.start(); 
			}

			Timer r = new Timer();
			Thread t = new Thread(r);
			t.setPriority( Thread.MIN_PRIORITY ); 
			t.start(); 			
	}

	private static byte[] generateAccountSeed(byte[] seed, int nonce) 
	{		
		byte[] nonceBytes = Ints.toByteArray(nonce);
		byte[] accountSeed = Bytes.concat(nonceBytes, seed, nonceBytes);
		return Crypto.getInstance().doubleDigest(accountSeed);		
	}	
	
	public static class MyRunnable implements Runnable {
		public void run() {		
			String doneseed = "";
			String doneseedaddress = "";
			String doneaddr = "";
			
			byte[] seed = new byte[32];
			Random random = new SecureRandom();	
			int nonce = 0;
			
			while(!done)
			{			
				random.nextBytes(seed);
				
				nonce = 0;
				
				while(nonce<10)
				{
					byte[] accountSeed = generateAccountSeed(seed, nonce);
					
					Pair<byte[], byte[]> keyPair = Crypto.getInstance().createKeyPair(accountSeed);
					byte[] publicKey = keyPair.getB();
					String address = Crypto.getInstance().getAddress(publicKey);
					
				    if(address.startsWith(pattern))
				    {
				    	doneseed = Base58.encode(seed);
				    	doneseedaddress = Base58.encode(accountSeed);
				    	doneaddr = address;
				    	done = true;
				    }
				    nonce ++;
				}
				counter ++;
			}
			if(doneaddr != "")
			{
				System.out.println("address: " + doneaddr + " wallet seed: " + doneseed + " address seed: " + doneseedaddress);
				System.out.println("Create a new wallet restoring from wallet seed: " + doneseed);
				System.out.println("or run in console command: POST addresses " + doneseedaddress);
				System.out.println();
						
				try {
				    BufferedWriter out = new BufferedWriter(
				    		new FileWriter("result.txt", true));

				    out.write("address: " + doneaddr + " wallet seed: " + doneseed + " address seed: " + doneseedaddress + "\r\n");
				    out.close();
					System.out.println("Warning! Seed was stored in the file result.txt");
					
				} catch (IOException e) {
				    e.printStackTrace();
				}
				
				new java.util.Scanner(System.in).nextLine();
			}
		} 
	}
	
	public static class Timer implements Runnable {
		public void run() {
			int intsleep = 5;
			boolean firsttime = true;
			while(!done)
			{
				long nowTime = (System.currentTimeMillis()/1000 - startTime);
				counter = counter / intsleep;
				if(!firsttime)
				{
					System.out.print("pattern: "+ pattern + " | ");
					
					
					if(nowTime > 58)
					{
						System.out.print("time: "+ nowTime/60  + "min ");
						intsleep = 60;
					}
					else
					{
						System.out.print("time: "+ nowTime  + "sec ");
					}	
					System.out.println("| address/sec: " + counter*10);
				}
				counter = 0;
				try {
					Thread.sleep(intsleep*1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				firsttime = false;
			}
		} 
	}	
}


