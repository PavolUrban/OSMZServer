package com.kru13.httpserver;

import java.io.IOException;
import java.net.ServerSocket;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;

public class SocketServer extends Thread {
	
	ServerSocket serverSocket;
	Handler messageHandler;
	CameraManager cameraManager;
	Camera cameraInstance;

	public final int port = 12345;
	boolean bRunning;


	public SocketServer(Handler handler, CameraManager cameraManager, Camera cameraInstance)
	{
		this.messageHandler = handler;
		this.cameraManager = cameraManager;
		this.cameraInstance = cameraInstance;
	}

	public void close() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			Log.d("SERVER", "Error, probably interrupted in accept(), see log");
			e.printStackTrace();
		}
		bRunning = false;
	}
	
	public void run() {
		
		try {

        	Log.d("SERVER", "Creating Socket");
			serverSocket = new ServerSocket(port);
			ClientHandler client = new ClientHandler(serverSocket, messageHandler, cameraManager, cameraInstance);
			client.run();
			//client.join();

		} catch (IOException e) {
			if (serverSocket != null && serverSocket.isClosed())
				Log.d("SERVER", "Normal exit");
			else {
				Log.d("SERVER", "Error");
				e.printStackTrace();
			}
		}
    }

}
