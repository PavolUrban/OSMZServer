package com.kru13.httpserver;

import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Timer;


public class HttpServerActivity extends AppCompatActivity implements OnClickListener {

	private SocketServer s;
	private ArrayList<String> Messages;
	private ArrayAdapter<String> arrayAdapter;
	CameraManager cameraManager;
	Camera cameraInstance;
	CameraPreview cameraPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		StrictMode.ThreadPolicy policy = new
				StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_http_server);
        Button btn1 = (Button)findViewById(R.id.button1);
        Button btn2 = (Button)findViewById(R.id.button2);
		ListView lv = (ListView)findViewById(R.id.listView);
		this.cameraManager = new CameraManager();
		this.cameraInstance = CameraManager.getCameraInstance();
		this.cameraPreview = new CameraPreview(getApplicationContext(), this.cameraInstance);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(cameraPreview);


		this.Messages = new ArrayList<String>();

		arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_gallery_item, this.Messages);
		lv.setAdapter(arrayAdapter);
         
        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);



    }
	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {

		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.http_server, menu);
        return true;
    }

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}



	private final Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			Bundle bndl = msg.getData();
			ResMessage m = (ResMessage) bndl.getSerializable("REQUEST");
			Messages.add("HOST: " + m.Host);
			Messages.add("FILE "+m.FileName);
			Messages.add("SIZE: "+m.Size);
			arrayAdapter.notifyDataSetChanged();
		}
	};

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if (v.getId() == R.id.button1) {
			s = new SocketServer(mHandler, cameraManager, cameraInstance);

			s.start();
		}
		if (v.getId() == R.id.button2) {
			s.close();
			try {
				s.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
    
}
