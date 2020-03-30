package com.kru13.httpserver;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;

public class ClientHandler extends Thread implements Camera.PreviewCallback {

    public static String NEWLINE = "\r\n";
    private ServerSocket serverSocket;
    private boolean bRunning = false;

    private Handler messageHandler;
    String boundary = "--boundary";
    boolean streaming = false;

    CameraManager cameraManager;
    Camera cameraInstance;
    DataOutputStream stream;
    ByteArrayOutputStream imageBuffer;


    public ClientHandler(ServerSocket socket, Handler handler, CameraManager cameraManager, Camera cameraInstance) {
        this.serverSocket = socket;
        this.messageHandler = handler;
        this.cameraManager = cameraManager;
        this.cameraInstance = cameraInstance;
        cameraInstance.setPreviewCallback(this);
    }


    private void createHTMLPage(BufferedWriter res, OutputStream o, File file, Socket s) throws IOException {
        res.write("HTTP/1.1 200 OK" + NEWLINE);
        res.write("Date: " + Calendar.getInstance().getTime() + NEWLINE);
        res.write("Server: localhost:12345" + NEWLINE);
        res.write("Content-Length: " + String.valueOf(file.length()) + NEWLINE);
        res.write("Connection: Closed" + NEWLINE);
        res.write(NEWLINE);
        res.flush();

        byte[] buf = new byte[1024];
        int len = 0;
        FileInputStream fis = new FileInputStream(file);
        while ((len = fis.read(buf)) > 0) {
            o.write(buf, 0, len);
        }
    }

    private void createMessage(ResMessage resMessage, File outFile, Socket s, String responseType) {
        resMessage.Size = outFile.length();
        resMessage.FileName = outFile.getName();
        resMessage.Host = s.getRemoteSocketAddress().toString();
        resMessage.ResponseType = responseType;
    }

    private void sendMessage(ResMessage resMessage)
    {
        Message msg = messageHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putSerializable("REQUEST", (Serializable) resMessage);
        msg.setData(bundle);
        messageHandler.sendMessage(msg);

    }

    public void run() {

        try {

            bRunning = true;
            while (bRunning) {

                Log.d("SERVER", "Socket Waiting for connection");
                Socket s = serverSocket.accept();
                Log.d("SERVER", "Socket Accepted");

                OutputStream o = s.getOutputStream();
                BufferedWriter res = new BufferedWriter(new OutputStreamWriter(o));
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

                String req = in.readLine();
                String[] reqData = req.split("\\s+");
                String requestType = reqData[0];
                String requestedFile = reqData[1];
                ResMessage resMessage = new ResMessage();

                if (requestType.equals("GET")) {

                    File outFile = null;

                    if (requestedFile.contains("/stream")) {
                        stream = new DataOutputStream(s.getOutputStream());
                        if (stream != null) {
                            try {
                                stream.write(("HTTP/1.0 200 OK\r\n" +
                                        "Server: localhost/12345" + NEWLINE +
                                        "Cache-Control:  no-cache" + NEWLINE +
                                        "Cache-Control:  private" + NEWLINE +
                                        "Content-Type: multipart/x-mixed-replace;boundary=" + boundary + NEWLINE).getBytes());

                                stream.flush();

                                streaming = true;
                                imageBuffer = new ByteArrayOutputStream();
                            } catch (IOException e) {
                                Log.d("ERROR:", e.getLocalizedMessage());
                                streaming = false;
                            }
                        }

                    }
                    else if(requestedFile.contains("/cgi-bin")){
                        String commands[] = requestedFile.split("/");

                        if (commands.length < 3)
                            break;

                        String command = commands[2];

                        try{
                            Process process = Runtime.getRuntime().exec(command);
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                            res.write("HTTP/1.0 200 Ok" + NEWLINE);
                            res.write("Date: " + Calendar.getInstance().getTime() + NEWLINE);
//                                out.write("Content-Length: " + String.valueOf(bufferedReader) + NEWLINE);
                            res.write("Content-Type: text/html" + NEWLINE);
                            res.write(NEWLINE);
                            res.write("<html>");
                            String line;
                            while ((line = bufferedReader.readLine()) != null){
                                res.write("<h3>" + line + "</h3>");
                            }
                            res.write(NEWLINE);

                            res.write("</html>");
                            res.flush();

                        }
                        catch (Exception e){
                            Log.d("ProcessOutput", "just failed: " + e.getMessage());

                        }

                        sendMessage(resMessage);

                        s.close();
                        Log.d("SERVER", "Socket Closed");

                    }
                    else {

                        //get outfile
                        if (requestedFile.contains("/webcam?"))
                        {
                            makeCameraPhoto();
                            outFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "/photos/camera.jpg");
                        }

                        else if(requestedFile.equalsIgnoreCase("/"))
                            outFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "index.html");

                        else
                            outFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), requestedFile);


                        //check if outfile exists
                        if (outFile.exists())
                        {
                            createHTMLPage(res, o, outFile, s);
                            createMessage(resMessage, outFile, s, "Webcam");
                        }
                        else
                        {
                            File notFoundFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "not-found.html");
                            createHTMLPage(res, o, notFoundFile, s);
                            createMessage(resMessage, notFoundFile, s, "404 NOT FOUND");

                            Log.d("SERVER", "File not found");
                        }

                        sendMessage(resMessage);
                        s.close();
                        Log.d("SERVER", "Socket Closed");
                    }
                }

                else
                    Log.d("SERVER", "bad request method!");
            }

        } catch (Exception e) {
            Log.d("SERVER ERROR", e.toString());
        } finally {
            serverSocket = null;
            bRunning = false;
        }
    }

    private void makeCameraPhoto() throws Exception {
        if (cameraManager == null) {
            throw new Exception("NO CAMERA");
        }
        takePicture();
    }

    public void takePicture() {
        try {
            if (cameraManager == null) {
                Log.d("takePictureFromCamera", "no camera hardware found");
                return;
            }

            if (cameraInstance == null) {
                Log.d("takePictureFromCamera", "could not get access to camera");
                return;
            }

            cameraInstance.takePicture(null, null, this.cameraManager);

        } catch (Exception e) {
            Log.d("Take picture error: ", e.getLocalizedMessage());
        }

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (this.streaming == true) {
            try {
                byte[] baos = convertYuvToJpeg(data, camera);
                // buffer is a ByteArrayOutputStream
                imageBuffer.reset();
                imageBuffer.write(baos);
                imageBuffer.flush();
                // write the content header
                stream.write((NEWLINE + boundary + NEWLINE +
                        "Content-type: image/jpeg" + NEWLINE +
                        "Content-Length: " + imageBuffer.size() + NEWLINE + NEWLINE).getBytes());

                stream.write(imageBuffer.toByteArray());
                stream.write((NEWLINE).getBytes());

                stream.flush();
            } catch (IOException e) {
                Log.d("onPreviewFrame error:  ", e.getLocalizedMessage());
            }
        }
    }

    public byte[] convertYuvToJpeg(byte[] data, Camera camera) {

        YuvImage image = new YuvImage(data, ImageFormat.NV21, camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 20; //set quality
        image.compressToJpeg(new Rect(0, 0, camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height), quality, baos);//this line decreases the image quality

        return baos.toByteArray();
    }
}