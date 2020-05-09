package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import static android.content.ContentValues.TAG;

public class SimpleDynamoProvider extends ContentProvider {

	KeyValueTableDBHelper dbHelper;
	ContentResolver contentResolver;
	static final int SERVER_PORT=10000;
	static final int[] emulators=new int[]{5554,5556,5558,5560,5562};
	static final String global_identifier="*";
	static final String local_identifier="@";
	static  final String[] operations= new String[]{"JOIN","INSERT","ALL_QUERY","SINGLE_QUERY","ALL_DELETE","SINGLE_DELETE","CHAIN"};
	int clientPort=0;
	int clientEmulator=0;
	Cursor cursor=null;
	String outputQuery="";
	AtomicBoolean flag=new AtomicBoolean(true);
	boolean failedAVD;

	public HashMap<String,List<String[]>>getChordDetails(int emulator){
		HashMap<String,List<String[]>> chordMap=new HashMap<String, List<String[]>>();
		List<String[]> succAndPred=new ArrayList<String[]>();
		String[] successors=new String[2];
		String[] predecessors=new String[2];
		try {
			if (emulator == emulators[0]) {
				successors[0] = Integer.toString(emulators[2]);
				successors[1] = Integer.toString(emulators[3]);
				predecessors[0] = Integer.toString(emulators[1]);
				predecessors[1]= Integer.toString(emulators[4]);
				succAndPred.add(successors);
				succAndPred.add(predecessors);
				chordMap.put(Integer.toString(emulator), succAndPred);
				return chordMap;
			} else if (emulator == emulators[1]) {
				successors[0] = Integer.toString(emulators[0]);
				successors[1] = Integer.toString(emulators[2]);
				predecessors[0] = Integer.toString(emulators[4]);
				predecessors[1]= Integer.toString(emulators[3]);
				succAndPred.add(successors);
				succAndPred.add(predecessors);
				chordMap.put(Integer.toString(emulator), succAndPred);
				return chordMap;
			} else if (emulator == emulators[2]) {
				successors[0] = Integer.toString(emulators[3]);
				successors[1] = Integer.toString(emulators[4]);
				predecessors[0] = Integer.toString(emulators[0]);
				predecessors[1]= Integer.toString(emulators[1]);
				succAndPred.add(successors);
				succAndPred.add(predecessors);
				chordMap.put(Integer.toString(emulator), succAndPred);
				return chordMap;
			} else if (emulator == emulators[3]) {
				successors[0] = Integer.toString(emulators[4]);
				successors[1] = Integer.toString(emulators[1]);
				predecessors[0] = Integer.toString(emulators[2]);
				predecessors[1]= Integer.toString(emulators[0]);
				succAndPred.add(successors);
				succAndPred.add(predecessors);
				chordMap.put(Integer.toString(emulator), succAndPred);
				return chordMap;
			} else if (emulator == emulators[4]) {
				successors[0] = Integer.toString(emulators[1]);
				successors[1] = Integer.toString(emulators[0]);
				predecessors[0] = Integer.toString(emulators[3]);
				predecessors[1]= Integer.toString(emulators[2]);
				succAndPred.add(successors);
				succAndPred.add(predecessors);
				chordMap.put(Integer.toString(emulator), succAndPred);
				return chordMap;
			}
		}
		catch (Exception ex){
			ex.getMessage();
		}
		return chordMap;
	}

	private int whereTo(String key){
		HashMap<String,List<String[]>> chordMap;
		String rightPartition=" ";
		List<String[]> succAndPred;
		try {
			String keyhash=genHash(key);
			if(keyhash.compareTo(genHash("5560"))>0||keyhash.compareTo(genHash("5562"))<=0){
				rightPartition = "5562";
			}
			else if(keyhash.compareTo(genHash("5562"))>0&&keyhash.compareTo(genHash("5556"))<=0){
				rightPartition = "5556";
			}
			else if(keyhash.compareTo(genHash("5556"))>0&&keyhash.compareTo(genHash("5554"))<=0){
				rightPartition = "5554";
			}
			else if(keyhash.compareTo(genHash("5554"))>0&&keyhash.compareTo(genHash("5558"))<=0){
				rightPartition = "5558";
			}
			else if(keyhash.compareTo(genHash("5558"))>0&&keyhash.compareTo(genHash("5560"))<=0){
				rightPartition = "5560";
			}
			return Integer.parseInt(rightPartition);
//			for(int i:emulators){
//				chordMap=getChordDetails(i);
//				succAndPred=chordMap.get(Integer.toString(i));
//				String[] predecessor=succAndPred.get(1);
//				Log.d("Insert",predecessor[0]);
//				String pred=predecessor[0];
//				if(hashedKey.compareTo(genHash(pred))>0 && hashedKey.compareTo(genHash(Integer.toString(i)))<=0){
//					return i;
//				}
//			}
		}
		catch (Exception ex){
			ex.printStackTrace();
		}
		return 0;
	}

	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		// Gets the data repository in write mode
		// https://stackoverflow.com/questions/9599741/how-to-delete-all-records-from-table-in-sqlite-with-android
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int response=0;
		if(selection.equals(local_identifier)){
			//Log.d("Delete","Deleting local AVD");
			response = db.delete(KeyValueTableContract.KeyValueTableEntry.TABLE_NAME, null, null);
			return response;
		}
		else if(selection.equals(global_identifier)){
			//Log.d("Delete","Deleting my own query");
			db.delete(KeyValueTableContract.KeyValueTableEntry.TABLE_NAME, null, null);
			String deleteQuery=clientPort+":"+"ALL_DELETE";
			//Log.d("Delete",deleteQuery);
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,deleteQuery);
			try {
				if (flag.get()) {
					//Log.d("Waiting", Boolean.toString(flag.get()));
					//Log.d("Wait","Waiting Here");
					Thread.sleep(200);
				}
			}
			catch (Exception ex){
				ex.printStackTrace();
			}
			return response;
		}
		else{
			//Log.d("Key to be Deleted",selection);
			HashMap<String,List<String[]>> chordMap;
			List<String[]> succAndPred;
			String key=selection;
			int deleteEmu=whereTo(key);
			int currentEmu=clientPort/2;
			chordMap=getChordDetails(deleteEmu);
			succAndPred=chordMap.get(Integer.toString(deleteEmu));
			//Log.d("Delete","Delete belongs to"+" "+deleteEmu);
			String[] successors=succAndPred.get(0);
			//Log.d("Delete","Delete belongs to"+" "+deleteEmu);
			if(deleteEmu==currentEmu){
				String[] whereCondition = {selection};
				db.delete(KeyValueTableContract.KeyValueTableEntry.TABLE_NAME, KeyValueTableContract.KeyValueTableEntry.COLUMN_NAME_KEY + "=?", whereCondition);
				replicateDelete(successors[0],key);
				replicateDelete(successors[1],key);
			}
			else {
				String deleteMessage=clientPort+":"+"SINGLE_DELETE"+":"+deleteEmu+":"+key;
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,deleteMessage);
				replicateDelete(successors[0],key);
				replicateDelete(successors[1],key);
			}
			try {
				if (flag.get()) {
					//Log.d("Waiting", Boolean.toString(flag.get()));
					//Log.d("Wait","Waiting Here");
					Thread.sleep(200);
				}
			}
			catch (Exception ex){
				ex.printStackTrace();
			}
			return response;
		}
	}
	//-----------All DELETE HELPERS-------------------
	private synchronized void replicateDelete(String emulator,String key){
		String deleteMessage=clientPort+":"+"SINGLE_DELETE"+":"+emulator+":"+key;
		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,deleteMessage);
	}
	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public synchronized Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		/*
		 * SQLite. But this is not a requirement. You can use other storage options, such
		 * TODO: You need to implement this method. Note that values will have two columns (a key
		 * column and a value column) and one row that contains the actual (key, value) pair to be
		 * inserted.
		 *
		 * For actual storage, you can use any option. If you know how to use SQL, then you can useas the
		 * internal storage option that we used in PA1. If you want to use that option, please
		 * take a look at the code for PA1.
		 */
		// Gets the data repository in write mode
		while (failedAVD);
		try {
			HashMap<String, List<String[]>> chordMap;
			List<String[]> succAndPred;
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			String key = (String) values.get("key");
			String value = (String) values.get("value");
			//Log.d("Insert", key);
			int insertEmu = whereTo(key);
			int currentEmu = clientPort / 2;
			chordMap = getChordDetails(insertEmu);
			succAndPred = chordMap.get(Integer.toString(insertEmu));
			//Log.d("Insert", "Insert belongs to" + " " + insertEmu);
			String[] successors = succAndPred.get(0);
			//Log.d("Insert", "Insert belongs to" + " " + insertEmu);
			synchronized (this) {
				if (insertEmu == currentEmu) {
					db.insertWithOnConflict(KeyValueTableContract.KeyValueTableEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
					//Log.d("INSERT", "Inserted in Same AVD " + insertEmu + " : " + key);
					//Log.d("INSERT", "Sending for Replication Key " + successors[0] + key);
					replicateInserts(successors[0], key, value);
					//Log.d("INSERT", "Sending for Replication Key " + successors[1] + key);
					replicateInserts(successors[1], key, value);
					try {
						if (flag.get()) {
							//Log.d("Waiting", Boolean.toString(flag.get()));
							//Log.d("Wait", "Waiting Here");
							Thread.sleep(200);
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				} else {
					String insertMessage = clientPort + ":" + "INSERT" + ":" + insertEmu + ":" + key + ":" + value;
					new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, insertMessage);
					//Log.d("INSERT", "Sending for Replication Key " + successors[0] + key);
					replicateInserts(successors[0], key, value);
					//Log.d("INSERT", "Sending for Replication Key " + successors[1] + key);
					replicateInserts(successors[1], key, value);
					try {
						if (flag.get()) {
							//Log.d("Waiting", Boolean.toString(flag.get()));
							//Log.d("Wait", "Waiting Here");
							Thread.sleep(200);
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
			try {
				if (flag.get()) {
					//Log.d("Waiting", Boolean.toString(flag.get()));
					//Log.d("Wait", "Waiting Here");
					Thread.sleep(200);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		catch (Exception ex){
			ex.printStackTrace();
		}
		return null;
	}

	//-----------All INSERT HELPERS-------------------
	private synchronized void replicateInserts(String emulator,String key,String value){
		String insertMessage=clientPort+":"+"INSERT"+":"+emulator+":"+key+":"+value;
		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,insertMessage);
	}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		dbHelper = new KeyValueTableDBHelper(getContext());
		contentResolver=(this.getContext()).getContentResolver();
		try {
			/*
			 * Create a server socket as well as a thread (AsyncTask) that listens on the server
			 * port.
			 *
			 * AsyncTask is a simplified thread construct that Android provides. Please make sure
			 * you know how it works by reading
			 * http://developer.android.com/reference/android/os/AsyncTask.html
			 */
			//Log.d("ServerSocket","Creating a Server Socket");
			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
		} catch (IOException e) {
			Log.e(TAG, "Can't create a ServerSocket");
		}

		/*
		 * Calculate the port number that this AVD listens on.
		 * It is just a hack that I came up with to get around the networking limitations of AVDs.
		 * The explanation is provided in the PA1 spec.
		 */
		TelephonyManager tel = (TelephonyManager)getContext().getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		//Log.d("AVD",portStr);
		final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
		clientPort=Integer.parseInt(myPort);
		failedAVD=true;
		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,myPort+":"+"JOIN");
		if(flag.get()){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//Log.d("On Create","Not in Failure");
		return false;
	}

	@Override
	public synchronized Cursor query(Uri uri, String[] projection, String selection,
									 String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		//https://developer.android.com/training/data-storage/sqlite.html
		while (failedAVD);
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		SQLiteQueryBuilder queryBuilder=new SQLiteQueryBuilder();
		queryBuilder.setTables(KeyValueTableContract.KeyValueTableEntry.TABLE_NAME);
		String key=selection;
		//Log.d("Select",selection);
		HashMap<String,List<String[]>> chordMap;
		List<String[]> succAndPred;

		if(selection.equals(local_identifier)){
			Cursor localCursor=null;
			//Log.d("Query", "here");
			localCursor = db.query(KeyValueTableContract.KeyValueTableEntry.TABLE_NAME, null, null, null, null, null, null, null);
			//Log.d("query", DatabaseUtils.dumpCursorToString(localCursor));
			return localCursor;
		}
		else if(selection.equals(global_identifier)){
			String queryResult=" ";
			String inputQuery=" ";
			cursor = db.query(KeyValueTableContract.KeyValueTableEntry.TABLE_NAME, null, null, null, null, null, null, null);
			//Log.d("query", DatabaseUtils.dumpCursorToString(cursor));
			if (cursor.moveToFirst()) {
				//Log.d("Query", "Converting Cursor");
				do {
					queryResult += cursor.getString(cursor.getColumnIndex(KeyValueTableContract.KeyValueTableEntry.COLUMN_NAME_KEY)) + "/";
					queryResult += cursor.getString(cursor.getColumnIndex(KeyValueTableContract.KeyValueTableEntry.COLUMN_NAME_VALUE)) + "%";
				} while (cursor.moveToNext());
				inputQuery = queryResult.substring(0, queryResult.length() - 2);

			}
			outputQuery=inputQuery+"@";
			//Log.d("Query",outputQuery);
			String queryMessage = clientPort + ":" + "ALL_QUERY";
			//Log.d("Query",queryMessage);
			cursor=null;
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, queryMessage);
			try {
				if (flag.get()) {
					//Log.d("Waiting", Boolean.toString(flag.get()));
					//Log.d("Wait","Waiting Here");
					Thread.sleep(8000);
				}
			}
			catch (Exception ex){
				ex.printStackTrace();
			}
			return cursor;
		}
		else {
			// Key Query
			// Chain replication
			int queryEmu=whereTo(key);
			chordMap=getChordDetails(queryEmu);
			succAndPred=chordMap.get(Integer.toString(queryEmu));
			String[] successors=succAndPred.get(0);
			cursor = queryBuilder.query(db, null, "key=" + "'" + selection + "'", null, null, null, null);
			if(queryEmu==(clientPort/2) || (clientPort/2)==Integer.parseInt(successors[0]) || (clientPort/2)==Integer.parseInt(successors[1]) && cursor!=null) {
				cursor.moveToFirst();
				return cursor;
			}
			else {
				cursor=null;
				//Log.d("Select Before Sending",key);
				String queryMessage = clientPort + ":" + "SINGLE_QUERY" + ":" + successors[0] + ":"+ successors[1]+":"+key;
				//Log.d("Query", queryMessage);
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, queryMessage);
				try {
					if (flag.get()) {
						//Log.d("Waiting", Boolean.toString(flag.get()));
						//Log.d("Wait","Waiting Here");
						Thread.sleep(2000);
					}
				}
				catch (Exception ex){
					ex.printStackTrace();
				}
				cursor.moveToFirst();
				return cursor;
			}
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
					  String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	private String genHash(String input) throws NoSuchAlgorithmException {
		MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
		byte[] sha1Hash = sha1.digest(input.getBytes());
		Formatter formatter = new Formatter();
		for (byte b : sha1Hash) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}
	/***
	 * ServerTask is an AsyncTask that should handle incoming messages. It is created by
	 * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
	 */

	private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

		@Override
		protected Void doInBackground(ServerSocket... sockets) {
			ServerSocket serverSocket = sockets[0];
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			SQLiteQueryBuilder queryBuilder=new SQLiteQueryBuilder();
			queryBuilder.setTables(KeyValueTableContract.KeyValueTableEntry.TABLE_NAME);
			Uri providerUri=Uri.parse("content://edu.buffalo.cse.cse486586.simpledynamo.provider");
			try {
				while (true) {
					//Log.d("Server","Message in Server");
					//Log.d("Server","Trying to connect");
					Socket socket = serverSocket.accept();
					//Log.d("Server", "Connection Successful");
					DataInputStream inputStream = new DataInputStream(socket.getInputStream());
					String receivedMessage=inputStream.readUTF();
					//Log.d("Server",receivedMessage);
					String[] messages=receivedMessage.split(":");
					//Operation
					String operation=messages[1];
					try {
						if(operations[0].equals(operation)){
							//Log.d("Server","In Join");
							// Normal Query in My Database
							Cursor serverCursor=null;
							try {
								int curPort=Integer.parseInt(messages[0])/2;
								DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
								synchronized (this) {
									serverCursor = db.query(KeyValueTableContract.KeyValueTableEntry.TABLE_NAME, null, null, null, null, null, null, null);
									//Log.d("JR", DatabaseUtils.dumpCursorToString(serverCursor));
									//https://stackoverflow.com/questions/7420783/androids-sqlite-how-to-turn-cursors-into-strings
									//https://developer.android.com/reference/android/database/sqlite/SQLiteCursor
									String inputQuery = " ";
									ConcurrentHashMap<String,String> keyValue=new ConcurrentHashMap<String, String>();
									if (serverCursor.moveToFirst()) {
										//Log.d("Query", "Converting Cursor");
										do {
											String key = serverCursor.getString(serverCursor.getColumnIndex(KeyValueTableContract.KeyValueTableEntry.COLUMN_NAME_KEY));
											String value = serverCursor.getString(serverCursor.getColumnIndex(KeyValueTableContract.KeyValueTableEntry.COLUMN_NAME_VALUE));
											HashMap<String, List<String[]>> chordMap;
											List<String[]> succAndPred;
											int queryEmu = whereTo(key);
											chordMap = getChordDetails(queryEmu);
											succAndPred = chordMap.get(Integer.toString(queryEmu));
											String[] successors = succAndPred.get(0);
											//Log.d("Belongs to",Integer.toString(queryEmu));
											if (queryEmu == curPort || Integer.parseInt(successors[0])==curPort || Integer.parseInt(successors[1])==curPort) {
												keyValue.put(key, value);
											}
										} while (serverCursor.moveToNext());
									}
									inputQuery=hashMapToString(keyValue);
									//Log.d("Server",inputQuery);
									if(!inputQuery.equals("") && !inputQuery.equals(" ") && !inputQuery.equals(null)){
										inputQuery = inputQuery;
									}
									else {
										inputQuery="Dummy";
									}
									outputStream.writeUTF(inputQuery);
									//Log.d("Query", inputQuery);
								}
								outputStream.flush();
								serverCursor.close();
							}
							catch (Exception ex){
								ex.printStackTrace();
							}
						}
						else if (operations[1].equals(operation)) {
							try {
								String key = messages[3];
								String value = messages[4];
								//Log.d("Server", "In Inside Sequence");
								//Storing Value to the Database Using Content Provider
								ContentValues keyValueToInsert = new ContentValues();
								// Calling Insert
								keyValueToInsert.put("key", key);
								keyValueToInsert.put("value", value);
								synchronized (this) {
									db.insertWithOnConflict(KeyValueTableContract.KeyValueTableEntry.TABLE_NAME, null, keyValueToInsert, SQLiteDatabase.CONFLICT_REPLACE);
								}
								//Log.d("INSERT","Inserted in Same AVD "+Integer.parseInt(messages[0])/2+" : "+key);
							}
							catch(Exception ex){
								ex.printStackTrace();
							}
						}
						else if(operations[2].equals(operation)){
							// Normal Query in My Database

							try {
								DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
								ConcurrentHashMap<String,String> keyValue=new ConcurrentHashMap<String, String>();

								synchronized (this) {
									Cursor serverCursor1;
									serverCursor1 = db.query(KeyValueTableContract.KeyValueTableEntry.TABLE_NAME, null, null, null, null, null, null, null);
									serverCursor1.moveToFirst();
									//serverCursor1=query(providerUri,null,"@",null,null,null);
									//Log.d("Server Response", DatabaseUtils.dumpCursorToString(serverCursor1));
									//Log.d("Response", DatabaseUtils.dumpCursorToString(serverCursor));
									//https://stackoverflow.com/questions/7420783/androids-sqlite-how-to-turn-cursors-into-strings
									//https://developer.android.com/reference/android/database/sqlite/SQLiteCursor
									//https://developer.android.com/reference/android/database/sqlite/SQLiteCursor
									String queryResult = " ";
									String inputQuery = "";
									if (serverCursor1.moveToFirst()) {
										do {
											queryResult += serverCursor1.getString(serverCursor1.getColumnIndex(KeyValueTableContract.KeyValueTableEntry.COLUMN_NAME_KEY)) + "/";
											queryResult += serverCursor1.getString(serverCursor1.getColumnIndex(KeyValueTableContract.KeyValueTableEntry.COLUMN_NAME_VALUE)) + "%";
										} while (serverCursor1.moveToNext());
										inputQuery = queryResult.substring(0, queryResult.length() - 1);
										byte[] data=inputQuery.getBytes("UTF-8");
										outputStream.writeInt(data.length);
										outputStream.write(data);
									}
								}
								outputStream.flush();
							}
							catch (Exception ex){
								ex.printStackTrace();
							}
						}
						else if(operations[3].equals(operation)){
							// Normal Query in My Database
							Cursor serverCursor=null;
							try {
								DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
								ConcurrentHashMap<String,String> keyValue=new ConcurrentHashMap<String, String>();
								synchronized (this) {
									serverCursor = queryBuilder.query(db, null, "key=" + "'" + messages[4] + "'", null, null, null, null);
									//Log.d("Server Response", DatabaseUtils.dumpCursorToString(serverCursor));
									//https://stackoverflow.com/questions/7420783/androids-sqlite-how-to-turn-cursors-into-strings
									//https://developer.android.com/reference/android/database/sqlite/SQLiteCursor
									String inputQuery = " ";
									try {
										if (serverCursor.moveToFirst()) {
											String key=" ";
											String value=" ";
											//Log.d("Query", "Converting Cursor");
											do {
												key = serverCursor.getString(serverCursor.getColumnIndex(KeyValueTableContract.KeyValueTableEntry.COLUMN_NAME_KEY));
												value = serverCursor.getString(serverCursor.getColumnIndex(KeyValueTableContract.KeyValueTableEntry.COLUMN_NAME_VALUE));
												keyValue.put(key, value);
											} while (serverCursor.moveToNext());
										}
										inputQuery = hashMapToString(keyValue);
										outputStream.writeUTF(inputQuery);
										//Log.d("Final", inputQuery);
									}
									catch (Exception ex){
										ex.printStackTrace();
									}
								}
								outputStream.flush();
							}
							catch (Exception ex){
								ex.printStackTrace();
							}
						}
						else if(operations[4].equals(operation)){
							synchronized (this) {
								db.delete(KeyValueTableContract.KeyValueTableEntry.TABLE_NAME, null, null);
							}
						}
						else if(operations[5].equals(operation)){
							synchronized (this) {
								String[] whereCondition = {messages[3]};
								db.delete(KeyValueTableContract.KeyValueTableEntry.TABLE_NAME, KeyValueTableContract.KeyValueTableEntry.COLUMN_NAME_KEY + "=?", whereCondition);
							}
						}
					}
					catch (Exception ex){
						ex.printStackTrace();
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			return null;
		}
	}
	private String hashMapToString(ConcurrentHashMap<String, String> hashMap){
		String hashMapStr = "";
		String modifiedString="";

		if(hashMap == null){
			return  hashMapStr;
		}
		for (Map.Entry<String, String> entry : hashMap.entrySet()) {
			hashMapStr += entry.getKey()+ "//";
			hashMapStr += entry.getValue()+ "%%";
		}
		if(!hashMapStr.equals("") && !hashMapStr.equals(" ") && !hashMapStr.equals(null)) {
			modifiedString=hashMapStr.substring(0, hashMapStr.length() - 2);
		}
		return modifiedString;
	}
	/***
	 * ClientTask is an AsyncTask that should send a string over the network.
	 * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
	 * an enter key press event.
	 *
	 * @author stevko
	 *
	 */
	private class ClientTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... msgs) {
			String requested_message=msgs[0];
			String[] messages=requested_message.split((":"));
			String operation= messages[1];
			//Log.d("Operation",operation);
			if (operation.equals(operations[0])){
				String query=" ";
				String key;
				String value;
				SQLiteDatabase db = dbHelper.getWritableDatabase();
				//Join Request of an AVD
				int receivedPort=Integer.parseInt(messages[0]);
				//Log.d("Client Operation & Port",operation+" "+receivedPort);
				clientEmulator=receivedPort/2;
				//Log.d("I failed",Integer.toString(clientEmulator));
				try {
					for (int port : emulators ) {
						int sendingPort=port*2;
						//Log.d("Port",Integer.toString(sendingPort));
						Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), sendingPort);
						DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
						String outputToServer = clientPort+":"+"JOIN";
						//Log.d("Client Sending String", outputToServer);
						outputStream.writeUTF(outputToServer);
						DataInputStream inputStream = new DataInputStream(socket.getInputStream());
						query = inputStream.readUTF();
						//Log.d("From",query+" "+port);
						synchronized (this){
							Cursor test;
							test= db.query(KeyValueTableContract.KeyValueTableEntry.TABLE_NAME, null, null, null, null, null, null, null);
							//Log.d("Server Response", DatabaseUtils.dumpCursorToString(test));
							if (!query.equals(null) && !query.equals(" ") && !query.equals("") && !query.equals("Dummy")) {
								String keyValue[] = query.split("%%");
								for (String subentry : keyValue) {
									//Log.d("Query2", subentry);
									subentry=subentry.trim();
									if(!subentry.equals(" ") && !subentry.equals("")) {
										String rows[] = subentry.split("//");
										key = rows[0];
										value = rows[1];
										//Storing Value to the Database Using Content Provider
										ContentValues keyValueToInsert = new ContentValues();
										keyValueToInsert.put("key", key);
										keyValueToInsert.put("value", value);
										//Log.d("KeyValue",key);
										//Log.d("ValueKey",value);
										db.insertWithOnConflict(KeyValueTableContract.KeyValueTableEntry.TABLE_NAME, null, keyValueToInsert, SQLiteDatabase.CONFLICT_REPLACE);
									}
								}
							}
						}
						Cursor test1;
						test1= db.query(KeyValueTableContract.KeyValueTableEntry.TABLE_NAME, null, null, null, null, null, null, null);
						//Log.d("Server Response", DatabaseUtils.dumpCursorToString(test1));
						failedAVD=false;
					}
					//Log.d("Client","Here");
				}
				catch (EOFException ex){
					//Log.d("Error","End of File");
					failedAVD=false;
				}
				catch (Exception ex){
					ex.printStackTrace();
				}
			}
			else if(operation.equals(operations[1])){
				try {
					String fromClient=messages[2];
					int sendingPort=Integer.parseInt(fromClient)*2;
					//Log.d("Client",fromClient);
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), sendingPort);
					DataOutputStream outputStream=new DataOutputStream(socket.getOutputStream());
					String outputToServer=requested_message;
					//Log.d("Client Sending String",outputToServer);
					outputStream.writeUTF(outputToServer);
				}
				catch (Exception ex){
					ex.printStackTrace();
				}
			}
			else if(operation.equals(operations[6])){
				try {
					String fromClient=messages[2];
					int sendingPort=Integer.parseInt(fromClient)*2;
					//Log.d("Client",fromClient);
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), sendingPort);
					DataOutputStream outputStream=new DataOutputStream(socket.getOutputStream());
					String outputToServer=requested_message;
					//Log.d("Client Sending String",outputToServer);
					outputStream.writeUTF(outputToServer);
				}
				catch (Exception ex){
					ex.printStackTrace();
				}
			}
			else if(operation.equals(operations[2])){
				try {
					ConcurrentHashMap<String, String> keyValueHashMap = new ConcurrentHashMap<String, String>();
					for (int i:emulators){
						try {
							int sendingPort = i * 2;
							if(clientPort!=sendingPort) {
								Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), sendingPort);
								DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
								String outputToServer = requested_message;
								//Log.d("Client Sending String", outputToServer + " " + i);
								outputStream.writeUTF(outputToServer);
								outputStream.flush();
								DataInputStream inputStream = new DataInputStream(socket.getInputStream());
								int length=inputStream.readInt();
								byte[] data=new byte[length];
								inputStream.readFully(data);
								String query=new String(data,"UTF-8");
								Log.d("Received String", query);
								if (!query.equals("Dummy")) {
									outputQuery += query + "@";
								}
								//Log.d("Result", outputQuery);
							}
						}
						catch (EOFException ex){
							//Log.e(TAG, "Exception in FOr! Server not up!");
							ex.printStackTrace();
						}
					}
					//Log.d("Query","Return All the Query Received");
					// https://stackoverflow.com/questions/28936424/converting-multidimentional-string-array-to-cursor
					String keyColumn= KeyValueTableContract.KeyValueTableEntry.COLUMN_NAME_KEY;
					String valueColumn= KeyValueTableContract.KeyValueTableEntry.COLUMN_NAME_VALUE;
					String[] columns = new String[] { keyColumn,valueColumn};
					String[] nodeQueries=outputQuery.split("@");
					MatrixCursor matrixCursor = new MatrixCursor(columns);
					for (String entry : nodeQueries) {
						entry=entry.trim();
						Log.d("Query1", entry);
						if (!entry.equals(" ") && !entry.equals(null) && !entry.equals("")) {
							String keyValue[] = entry.split("%");
							for (String subentry : keyValue) {
								Log.d("Query2", subentry);
								subentry=subentry.trim();
								if(!subentry.equals(" ") && !subentry.equals("")) {
									String rows[] = subentry.split("/");
									String[] outputs = new String[]{rows[0].trim(), rows[1].trim()};
									matrixCursor.addRow(outputs);
								}
							}
						}
					}
					cursor=matrixCursor;
					//Log.d("Final Result",outputQuery);
					//Log.d("Response", DatabaseUtils.dumpCursorToString(cursor));
				}
				catch (UnknownHostException ex){
					ex.printStackTrace();
				}
				catch (EOFException ex){
					ex.printStackTrace();
				}
				catch (Exception ex){
					ex.printStackTrace();
				}
			}
			else if(operation.equals(operations[3])){
				try {
					String successor2=messages[3];
					int sendingPort=Integer.parseInt(successor2)*2;
					//Log.d("Client",successor2);
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), sendingPort);
					DataOutputStream outputStream=new DataOutputStream(socket.getOutputStream());
					String outputToServer=requested_message;
					//Log.d("Client Sending String",outputToServer);
					outputStream.writeUTF(outputToServer);
					DataInputStream inputStream=new DataInputStream(socket.getInputStream());
					String query=inputStream.readUTF();
					//Log.d("Received Query",query);
					String[] queryType=query.split("//");
					String[] outputs = new String[]{queryType[0].trim(), queryType[1]};
					//Log.d("Response",Integer.toString(queryType.length));
					//Log.d("Response",queryType[0]);
					//Log.d("Response",queryType[1]);
					// https://stackoverflow.com/questions/28936424/converting-mult5mentional-string-array-to-cursor
					String keyColumn = KeyValueTableContract.KeyValueTableEntry.COLUMN_NAME_KEY;
					String valueColumn = KeyValueTableContract.KeyValueTableEntry.COLUMN_NAME_VALUE;
					String[] columns = new String[]{keyColumn, valueColumn};
					MatrixCursor matrixCursor = new MatrixCursor(columns);
					matrixCursor.addRow(outputs);
					cursor=matrixCursor;
					//Log.d("Response", DatabaseUtils.dumpCursorToString(matrixCursor));
				}
				catch (Exception ex){
					try {
						String successor1=messages[2];
						int sendingPort=Integer.parseInt(successor1)*2;
						//Log.d("Client",successor1);
						Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), sendingPort);
						DataOutputStream outputStream=new DataOutputStream(socket.getOutputStream());
						String outputToServer=requested_message;
						//Log.d("Client Sending String",outputToServer);
						outputStream.writeUTF(outputToServer);
						DataInputStream inputStream=new DataInputStream(socket.getInputStream());
						String query=inputStream.readUTF();
						//Log.d("Received Query",query);
						String[] queryType=query.split("//");
						String[] outputs = new String[]{queryType[0].trim(), queryType[1]};
						//Log.d("Response",Integer.toString(queryType.length));
						//Log.d("Response",queryType[0]);
						//Log.d("Response",queryType[1]);
						// https://stackoverflow.com/questions/28936424/converting-mult5mentional-string-array-to-cursor
						String keyColumn = KeyValueTableContract.KeyValueTableEntry.COLUMN_NAME_KEY;
						String valueColumn = KeyValueTableContract.KeyValueTableEntry.COLUMN_NAME_VALUE;
						String[] columns = new String[]{keyColumn, valueColumn};
						MatrixCursor matrixCursor = new MatrixCursor(columns);
						matrixCursor.addRow(outputs);
						cursor=matrixCursor;
						//Log.d("Response", DatabaseUtils.dumpCursorToString(matrixCursor));
					}
					catch (Exception ex1){
						ex1.printStackTrace();
					}

				}
			}
			else if(operation.equals(operations[4])){
				try {
					for (int i : emulators) {
						int sendingPort = i * 2;
						if (sendingPort != clientPort) {
							Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), sendingPort);
							DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
							String outputToServer = requested_message;
							//Log.d("Client Sending String", outputToServer);
							outputStream.writeUTF(outputToServer);
						}
					}
				}
				catch (Exception ex){
					ex.printStackTrace();
				}
			}
			else if(operation.equals(operations[5])){
				try {
					String port=messages[2];
					int sendingPort=Integer.parseInt(port)*2;
					//Log.d("Client",port);
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), sendingPort);
					DataOutputStream outputStream=new DataOutputStream(socket.getOutputStream());
					String outputToServer=requested_message;
					//Log.d("Client Sending String",outputToServer);
					outputStream.writeUTF(outputToServer);
				}
				catch (Exception ex){
					ex.printStackTrace();
				}
			}
			return null;
		}
	}
}
