
package com.camelight.android.view;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
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
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.camelight.android.R;
import com.camelight.android.business.BackLightInteraction;
import com.camelight.android.business.BusinessMode;
import com.camelight.android.business.BusinessState;
import com.camelight.android.business.DetectModeInteraction;
import com.camelight.android.business.FrontLightGuideInteraction;
import com.camelight.android.business.Interactor;
import com.camelight.android.business.NightSceneGuideInteraction;
import com.camelight.android.model.BackLightCacheBean;
import com.camelight.android.model.CalculateDistanceCacheBean;
import com.camelight.android.model.CameraFrame;
import com.camelight.android.model.DetectDegreeCacheBean;
import com.camelight.android.model.DetectModeCacheBean;
import com.camelight.android.util.FrameProcessor;
import com.camelight.android.util.InteractionUtil;
import com.camelight.android.util.OrientationUtil;
import com.camelight.android.view.util.CameDialog;
import com.camelight.android.view.util.CameraView;

public class CameraActivity extends FragmentActivity {

	public ConfirmModeFragment confirmModeFragment = null;
	
	private CameraView camera_;
	private View controlBar_;
	private ImageView btnCapture_;
	private ImageView btnSwitchCamera_;
	private ImageView btnGuide_;
	private FrameLayout cameraLayout_;
	private Interactor interactor_;

	ImageView testImage_ = null;
	ViewGroup rootView_ = null;
	
	private Runnable onConfirmModeFinish_ = new Runnable() {
		@Override
		public void run() {
			if(detectModeCacheBean_.mode_ != BusinessMode.NULL) {
				startGuide(detectModeCacheBean_.mode_);	
			} else {
				CameraActivity.this.showControlBar();
			}
		}
	};
	
	private DetectModeCacheBean detectModeCacheBean_ = new DetectModeCacheBean();
	private Handler businessHandler_ = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what == BusinessState.SWITCH_MODE_FRONTLIGHT) {
				startGuide(BusinessMode.FRONTLIGHT);
			} else if(msg.what == BusinessState.SWITCH_MODE_BACKLIGHT) {
				startGuide(BusinessMode.BACKLIGHT);
			} else if(msg.what == BusinessState.SWITCH_MODE_NIGHT) {
				startGuide(BusinessMode.NIGHT);
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
			if(InteractionUtil.isDoubleClick()) {
				return ;
			}
			if(interactor_.isRunning()) {
				interactor_.stopInteract();
			} else {
				startGuide(BusinessMode.NIGHT);
				startDetectMode();
			}
		}
	};
	
	public void cancelCurrentInteraction() {
		if(interactor_!= null && interactor_.isRunning()) {
			interactor_.stopInteract();
		}
	}
	
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
        controlBar_ = findViewById(R.id.control_bar);
        btnCapture_ = (ImageView) findViewById(R.id.btn_take_photo);
        btnGuide_ = (ImageView) findViewById(R.id.btn_start_guide);
        cameraLayout_ = (FrameLayout) findViewById(R.id.camera_frame);
        btnSwitchCamera_ = (ImageView)findViewById(R.id.btn_switch_camera);
        Handler handler = new Handler();
        interactor_ = new Interactor(handler);
        
        btnGuide_.setOnClickListener(onStartGuideListener);
        btnCapture_.setOnClickListener(new OnClickListener() {		
			@Override
			public void onClick(View v) {
				if(InteractionUtil.isDoubleClick() == false) {
					camera_.takePicture();
				}
			}
		});
        btnSwitchCamera_.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(InteractionUtil.isDoubleClick() == false) {
					switchCamera();
				}
			}
		});
        
        OrientationUtil.register(this);
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
    
    public void startDetectMode(){
		DetectModeCacheBean bean = detectModeCacheBean_;
		bean.camera_ = camera_;
		bean.context_ = CameraActivity.this;
		bean.layout_ = cameraLayout_;
		interactor_.setParam(bean);
		DetectModeInteraction detect_mode = new DetectModeInteraction();
		interactor_.setInteraction(detect_mode);
		interactor_.startInteract(300);
    }
    
    public void startFrontLightGuide() {
    	DetectDegreeCacheBean bean = new DetectDegreeCacheBean();
    	bean.camera_ = camera_;
    	bean.context_ = this;
    	bean.layout_ = cameraLayout_;
    	interactor_.setParam(bean);
    	FrontLightGuideInteraction front_light = new FrontLightGuideInteraction();
    	interactor_.setInteraction(front_light);
    	interactor_.startInteract(30);
    }
    
    private void startBackLightGuide() {
    	BackLightCacheBean bean = new BackLightCacheBean();
    	bean.context_ = this;
    	bean.camera_ = camera_;
    	bean.layout_ = cameraLayout_;
    	interactor_.setParam(bean);
    	BackLightInteraction back_light = new BackLightInteraction();
    	interactor_.setInteraction(back_light);
    	interactor_.startInteract(100);
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
    	case FRONTLIGHT:
    		startFrontLightGuide();
    		break;
    	case BACKLIGHT:
    		startBackLightGuide();
    		break;
    	case NIGHT:
    		startNightSceneGuide();
    		break;
    	default: break;
    	}
    }
    
    public void confirmMode(){
        confirmModeFragment = ConfirmModeFragment.createInstance(detectModeCacheBean_);
        confirmModeFragment.setOnFinish(onConfirmModeFinish_);
    	FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    	ft.add(android.R.id.content, confirmModeFragment, ConfirmModeFragment.TAG);
    	ft.addToBackStack(ConfirmModeFragment.TAG);
    	ft.setCustomAnimations(FragmentTransaction.TRANSIT_FRAGMENT_FADE, FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    	ft.commit();
    }
    
    //yw_sun debug
    public void updatePreview(String str) {
    	TextView text = (TextView)findViewById(R.id.test_info);
    	text.setText(str);
    	return ;
//    	if(bitmap == null) {
//    		return ;
//    	}
//    	Bitmap bmp = Bitmap.createBitmap(bitmap);
//    	BitmapDrawable drawable = new BitmapDrawable(bmp);
//    	preView_.setBackgroundDrawable(drawable);
    }
    
    public void stopCurrentInteraction() {
    	if(interactor_ != null && interactor_.isRunning()) {
    		interactor_.stopInteract();
    	}
    }
    
    public void hideControlBar() {
    	Animation anim = new TranslateAnimation(
    			Animation.RELATIVE_TO_SELF, 0.f,
    			Animation.RELATIVE_TO_SELF, 0.f,
    			Animation.RELATIVE_TO_SELF, 0.f,
    			Animation.RELATIVE_TO_SELF, 1.f);
    	anim.setAnimationListener(new AnimationListener() {	
			@Override
			public void onAnimationStart(Animation animation) {}
			@Override
			public void onAnimationRepeat(Animation animation) {}
			@Override
			public void onAnimationEnd(Animation animation) {
				controlBar_.setVisibility(View.GONE);
			}
		});
    	anim.setDuration(700);
    	controlBar_.startAnimation(anim);
    }
    public void showControlBar() {
    	Animation anim = new TranslateAnimation(
    			Animation.RELATIVE_TO_SELF, 0.f,
    			Animation.RELATIVE_TO_SELF, 0.f,
    			Animation.RELATIVE_TO_SELF, 1.f,
    			Animation.RELATIVE_TO_SELF, 0.f);
    	anim.setDuration(700);
    	controlBar_.setVisibility(View.VISIBLE);
    	controlBar_.startAnimation(anim);
    }
    
    public void switchCamera() {
    	if(camera_.getCameraFacing() == CameraView.CAMERA_FACE_BACK) {
    		camera_.switchCamera(CameraView.CAMERA_FACE_FRONT);
    	} else {
    		camera_.switchCamera(CameraView.CAMERA_FACE_BACK);
    	}
    }
}
