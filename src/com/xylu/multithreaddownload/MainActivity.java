package com.xylu.multithreaddownload;

import java.io.File;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	
	private EditText urlEt;
	private EditText downEt;
	private Button downBtn;
	private ProgressBar downBar;
	
	private DownUtil downUtil;
	private int downStatus;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		urlEt = (EditText)findViewById(R.id.url_et);
		downEt = (EditText)findViewById(R.id.down_et);
		downBtn = (Button)findViewById(R.id.down_btn);
		downBar = (ProgressBar)findViewById(R.id.down_bar);
		
		downBtn.setOnClickListener(this);
		
		urlEt.setText("http://gdown.baidu.com/data/wisegame/c66ca745eba19a17/shoujibaidu_16787720.apk");
		downEt.setText(Environment.getExternalStorageDirectory().getPath() + "/test/shoujibaidu_16787720.apk");
	}

	@Override
	public void onClick(View v) {
		
		if (v == downBtn) {
			String url = urlEt.getText().toString();
			String down = downEt.getText().toString();
			File file = new File(down);
			String dir = file.getParent();
			String filename = file.getName();
			downUtil = new DownUtil(url, dir, filename, 3);
			downBar.setVisibility(View.VISIBLE);
			
			AsyncTask<String, Integer, Boolean> downloadTask = new AsyncTask<String, Integer, Boolean>() {

				@Override
				protected Boolean doInBackground(String... params) {
					try {
						//开始下载
						downUtil.download();
						//更新进度
						while (downStatus < 100) {
							double completeRate = downUtil.getCompleteRate();
							System.out.println(completeRate + " m64");
							downStatus = (int)(completeRate * 100);
							System.out.println(downStatus + " m66");
							publishProgress(downStatus);
							Thread.sleep(500);
						}
						return true;
					} catch (Exception e) {
						e.printStackTrace();
						return false;
					}
				}

				@Override
				protected void onProgressUpdate(Integer... values) {
					downBar.setProgress(values[0]);
				}

				@Override
				protected void onPostExecute(Boolean result) {
					if (result) {
						Toast.makeText(MainActivity.this, "download complete", Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(MainActivity.this, "download fail", Toast.LENGTH_SHORT).show();
					}
					
				}
			};
			downloadTask.execute();
		}
	}
}
