package com.camelight.android.view;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.R.anim;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.camelight.android.R;
import com.camelight.android.business.BusinessMode;
import com.camelight.android.business.BusinessState;
import com.camelight.android.business.DetectModeInteraction;
import com.camelight.android.business.FrontLightGuideInteraction;
import com.camelight.android.business.Interaction;
import com.camelight.android.business.Interactor;
import com.camelight.android.business.NightSceneGuideInteraction;
import com.camelight.android.model.CalculateDistanceCacheBean;
import com.camelight.android.model.CameraFrame;
import com.camelight.android.model.DetectDegreeCacheBean;
import com.camelight.android.model.DetectModeCacheBean;
import com.camelight.android.util.FrameProcessor;
import com.camelight.android.util.ImageProcessor;
import com.camelight.android.view.util.CameraView;

public class CameraActivity extends FragmentActivity {

	public ConfirmModeFragment confirmModeFragment = null;
	
	private CameraView camera_;
	private ImageView btnCapture_;
	private ImageView preView_;
	private ImageView btnGuide_;
	private FrameLayout cameraLayout_;
	private Interactor interactor_;

	
	private DetectModeCacheBean detectModeCacheBean_ = new DetectModeCacheBean();
	private Handler businessHandler_ = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what == BusinessState.DETECT_FACE_FINISH) {
				if(detectModeCacheBean_.faces_ != null) {
					String text = "����ʶ��ɹ���ģʽ:"+detectModeCacheBean_.mode_.description_;
					Toast toast =  Toast.makeText(CameraActivity.this, text, Toast.LENGTH_SHORT);
					toast.show();
					startGuide(detectModeCacheBean_.mode_);
				}
				getSupportFragmentManager();
			}
		}
	};
	
	private BaseLoaderCallback loaderCallback_ = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			System.loadLibrary("frame_processor");
            try {
                // load cascade file from application resources
                InputStream is = getResources().openRawResource(R.raw.model);
                File dir = getDir("model", Context.MODE_PRIVATE);
                File model_file = new File(dir, "model.txt");
                FileOutputStream os = new FileOutputStream(model_file);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                is.close();
                os.close();

                dir.delete();
                
                FrameProcessor.setSVMModel(model_file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("camera_file", "Failed to load cascade. Exception thrown: " + e);
            }
            
		}
	};
	
	private OnClickListener onStartGuideListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if(interactor_.isRunning()) {
				interactor_.stopInteract();
			} else {
				startDetectFace();
			}
		}
	};
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean res = super.onTouchEvent(event);
		return res;
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity_view);
        camera_ = (CameraView) findViewById(R.id.camera_surface);
        btnCapture_ = (ImageView) findViewById(R.id.btn_take_photo);
        btnGuide_ = (ImageView) findViewById(R.id.btn_start_guide);
        cameraLayout_ = (FrameLayout) findViewById(R.id.camera_frame);
        Handler handler = new Handler();
        interactor_ = new Interactor(handler);
        
        confirmModeFragment = ConfirmModeFragment.createInstance(detectModeCacheBean_);
        
        btnGuide_.setOnClickListener(onStartGuideListener);
        btnCapture_.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				camera_.takePicture();
			}
		});
        
    }

    @Override
    protected void onResume(){
    	super.onResume();
    	boolean res = OpenCVLoader.initDebug();
    	if(res){
    		loaderCallback_.onManagerConnected(BaseLoaderCallback.SUCCESS);
    	}
    }
    
    @Override
    protected void onStop(){
    	if(interactor_.isRunning()) {
    		interactor_.stopInteract();
    	}
    	super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.camera, menu);
        return true;
    }
    
    public Handler getBusinessHandler(){
    	return businessHandler_;
    }
    
    public void startDetectFace(){
		DetectModeCacheBean bean = detectModeCacheBean_;
		bean.camera_ = camera_;
		bean.context_ = CameraActivity.this;
		bean.layout_ = cameraLayout_;
		interactor_.setParam(bean);
		DetectModeInteraction detect_mode = new DetectModeInteraction();
		interactor_.setInteraction(detect_mode);
		interactor_.startInteract(100);
    }
    
    public void startFrontLightGuide(){
    	DetectDegreeCacheBean bean = new DetectDegreeCacheBean();
    	bean.camera_ = camera_;
    	bean.context_ = this;
    	bean.layout_ = cameraLayout_;
    	interactor_.setParam(bean);
    	FrontLightGuideInteraction front_light = new FrontLightGuideInteraction();
    	interactor_.setInteraction(front_light);
    	interactor_.startInteract(30);
    }
    
    
    private void startNightSceneGuide() {
		CalculateDistanceCacheBean bean = new CalculateDistanceCacheBean();
		bean.camera_ = camera_;
		bean.context_ = this;
		bean.layout_ = cameraLayout_;
		interactor_.setParam(bean);
		NightSceneGuideInteraction night_scene = new NightSceneGuideInteraction();
		interactor_.setInteraction(night_scene);
		interactor_.startInteract(30);
		
	}
    
    public void startGuide(BusinessMode mode) {
    	switch(mode) {
    	case BACKLIGHT:
    		break;
    	case FRONTLIGHT:
    		startFrontLightGuide();
    		break;
    	case NIGHT:
    		startNightSceneGuide();
    		break;
    	default: break;
    	}
    }
    
    public void confirmMode(){
    	ConfirmModeFragment fragment = confirmModeFragment;
    	FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    	ft.add(android.R.id.content, fragment, ConfirmModeFragment.TAG);
    	ft.addToBackStack(ConfirmModeFragment.TAG);
    	ft.commit();
    }
}
