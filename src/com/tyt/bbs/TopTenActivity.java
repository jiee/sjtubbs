﻿package com.tyt.bbs;


import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.tyt.bbs.adapter.TopTenAdapter;
import com.tyt.bbs.entity.TopicItem;
import com.tyt.bbs.parser.TopTenParser;
import com.tyt.bbs.parser.WebTopTenParser;
import com.tyt.bbs.utils.FileOperate;
import com.tyt.bbs.utils.Property;
import com.tyt.bbs.utils.XmlOperate;
import com.tyt.bbs.view.LoadingDrawable;
import com.tyt.bbs.view.PullToRefreshListView;
import com.tyt.bbs.view.PullToRefreshListView.OnRefreshListener;

public class TopTenActivity extends BaseActivity implements OnClickListener{

	public static final String TAG = "TopTenActivity";
	private ListView mToptenList;
	private ProgressBar mProgressBar;
	private ImageButton btn_top10refresh;
	private Animation animation;


	private WebTopTenParser mColumnParser;
	private TopTenAdapter mAdapter;

	private final int STYLE_TOPTEN=1;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_topten);
		animation = AnimationUtils.loadAnimation(this,R.anim.rotate);
		mProgressBar=(ProgressBar)findViewById(R.id.progressbar_topten);
		mProgressBar.setIndeterminateDrawable(new LoadingDrawable(0,
				Color.parseColor("#c6e0f2"), Color.parseColor("#337fd3"), Color.TRANSPARENT, 200));
		setListView();
	}

	public void setListView(){
		btn_top10refresh = (ImageButton)findViewById(R.id.btn_top10refresh);
		btn_top10refresh.setOnClickListener(this);
		mToptenList=(ListView)findViewById(R.id.lv_topten);
		mToptenList.setCacheColorHint(Color.TRANSPARENT);
		try {
			java.io.File xmlFile=FileOperate.readFromSDcard("topten.xml");
			mColumnParser=new WebTopTenParser(STYLE_TOPTEN);
			mProgressBar.setVisibility(View.VISIBLE);
			//若不为第一次登陆，则先从文件中读取xml
			if(xmlFile!=null){

				ArrayList<TopicItem> tmp = 	XmlOperate.getInstance().readXML(xmlFile);
				mAdapter = new TopTenAdapter(TopTenActivity.this,tmp,STYLE_TOPTEN);
				mToptenList.setAdapter(mAdapter);
				mProgressBar.setVisibility(View.GONE);
				
				long logininterval= Property.getPreferences(this).getLong(Property.LoginTime, 0);
				if(logininterval>0)
					logininterval =System.currentTimeMillis()/1000- logininterval;
				Log.v(TAG, "当前时间差="+logininterval);
				if(logininterval>600){
					new ListLoad().execute();
					Editor edit=Property.getPreferences(this).edit();
					edit.putLong(Property.LoginTime, System.currentTimeMillis()/1000);
					//							Log.v(TAG, "当前时间="+System.currentTimeMillis()/1000);
					edit.commit();
				}
			}else{
				Editor edit=Property.getPreferences(this).edit();
				edit.putLong(Property.LoginTime, System.currentTimeMillis()/1000);
				//							Log.v(TAG, "当前时间="+System.currentTimeMillis()/1000);
				edit.commit();
				 new ListLoad().execute();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	

		mToptenList.setOnItemClickListener(topTenItemOnClick);
	}

	private OnItemClickListener topTenItemOnClick=new OnItemClickListener(){

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// TODO Auto-generated method stub

			Log.i("onItemClick ", "onItemClick ");
			TopicItem tempdata=mAdapter.getItem(position);
			String link=tempdata.getLinkUrl();
			Intent i=new Intent(TopTenActivity.this,ArticleActivity.class);
			i.putExtra("link", link+"&showall=true");
			i.putExtra("reid",link.substring(link.indexOf("reid=")).replace("reid=", ""));	
			i.putExtra("board",link.substring(link.indexOf("board="),link.indexOf("&")).replace("board=", ""));	
			startActivity(i);
		}

	};

	private class ListLoad extends AsyncTask<Void,Integer,Void>
	{
		@Override
		protected void onPreExecute() {
			mToptenList.setClickable(false);
			mProgressBar.setVisibility(View.VISIBLE);
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			try {

				mColumnParser.parser();
				mAdapter.setListTitles(mColumnParser.getList());

				//总的topTen写入SDCard
				String xmString=XmlOperate.getInstance().writeXml(mAdapter.getListTitles());
				if(xmString!=null){
					if(FileOperate.writeToSDcard("topten.xml", xmString)){
						Log.i("XMLFileWrite", "success");
					}
					else 
						Log.i("XMLFileWrite", "failed");
				}else{
					Log.e(TAG, "########xmString is null##############");
				}
				
			} catch (Exception e) {
				Log.e(TAG, "##############################");
				Log.e("TopTen",e.toString());
				Log.e("TopTen", "##############################");
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if(mAdapter!=null)mAdapter.notifyDataSetChanged();
			mProgressBar.setVisibility(View.GONE);
			btn_top10refresh.clearAnimation();
			super.onPostExecute(result);
		}
	}

	/* (non-Javadoc)
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()){
		case R.id.btn_top10refresh:
			animation.start();
			btn_top10refresh.setAnimation(animation);
			new ListLoad().execute();
			break;
		}
	}


}
