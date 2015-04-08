package com.camelight.android.view.util;

import com.camelight.android.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

public class CameDialog extends Fragment{
	static public final int SINGLE_DIALOG = 1;
	static public final int EXECUTE_DIALOG = 2;
	public String TAG = "CameDialog";
	private int dialogType_;
	
	private View mainContainer_ = null;
	private View mainDialog_ = null;	
	private TextView contentView_ = null;
	private TextView positiveBtn_ = null;
	private TextView negativeBtn_ = null;
	private TextView singleBtn_ = null;
	
	private String strSingleText_ = "";
	private String strPositiveText_ = "";
	private String strNegativeText_ = "";
	private String strContentText_ = "";
	
	private OnClickListener onSingleClick_ = null;
	private OnClickListener onPositiveClick_ = null;
	private OnClickListener onNegativeClick_ = null;
	
	private OnClickListener privateClickSingle_ = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if(onSingleClick_ != null) {
				onSingleClick_.onClick(v);	
			}
			finish();
		}
	};
	private OnClickListener privateClickPositive_ = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if(onPositiveClick_ != null) {
				onPositiveClick_.onClick(v);		
			}
			finish();
		}
	};
	private OnClickListener privateClickNegative_ = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if(onNegativeClick_ != null) {
				onNegativeClick_.onClick(v);	
			}
			finish();
		}
	};
	
	public CameDialog() {
		super();
		dialogType_ = SINGLE_DIALOG;
	}
	
	/*
	 * @PARAM: dialog_type must be SINGLE_DIALOG or EXECUTE_DIALOG
	 * */
	public void setDialogType(int dialog_type) {
		dialogType_ = dialog_type;
	} 
	
	public void setOnSingleListener(OnClickListener single) {
		onSingleClick_ = single;
	}
	
	public void setOnPositiveListener(OnClickListener positive) {
		onPositiveClick_ = positive;
	}
	
	public void setOnNegativeClick(OnClickListener negative) {
		onNegativeClick_ = negative;
	}
	
	public void setTag(String tag) {
		TAG = tag;
	}
	
	public void setSingleText(String single) {
		strSingleText_ = single;
	}
	
	public void setPositiveText(String pos) {
		strPositiveText_ = pos;
	}
	
	public void setNegativeText(String neg) {
		strNegativeText_ = neg;
	}
	
	public void setDialogContent(String content) {
		strContentText_ = content;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
		mainContainer_ = inflater.inflate(R.layout.came_dialog_base_view, null);
		if(dialogType_ == SINGLE_DIALOG) {
			mainDialog_ = mainContainer_.findViewById(R.id.single_dialog_view);
			singleBtn_ = (TextView)mainDialog_.findViewById(R.id.single_text);
			singleBtn_.setText(strSingleText_);
			singleBtn_.setOnClickListener(privateClickSingle_);
			mainDialog_.setVisibility(View.VISIBLE);
		} else if(dialogType_ == EXECUTE_DIALOG) {
			mainDialog_ = mainContainer_.findViewById(R.id.execute_dialog_view);
			positiveBtn_ = (TextView)mainDialog_.findViewById(R.id.positive_text);
			positiveBtn_.setText(strPositiveText_);
			positiveBtn_.setOnClickListener(privateClickPositive_);
			negativeBtn_ = (TextView)mainDialog_.findViewById(R.id.negative_text);
			negativeBtn_.setText(strNegativeText_);
			negativeBtn_.setOnClickListener(privateClickNegative_);
			mainDialog_.setVisibility(View.VISIBLE);
		} else {
			return null;
		}
		contentView_ = (TextView)mainDialog_.findViewById(R.id.content_text);
		contentView_.setText(strContentText_);
		/** Add an empty OnClickListener to block click event*/
		mainContainer_.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {}
		});
		return mainContainer_;
	}
	
	public void show(FragmentActivity act) {
		FragmentTransaction ft = act.getSupportFragmentManager().beginTransaction();
		ft.add(android.R.id.content, this, TAG);
		ft.addToBackStack(TAG);
		ft.commit();
	}
	
	public void finish() {
		getActivity().onBackPressed();
	}
}
