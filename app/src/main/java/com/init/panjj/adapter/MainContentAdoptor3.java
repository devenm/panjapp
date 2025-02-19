package com.init.panjj.adapter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.init.panjj.R;
import com.init.panjj.activity.MainActivity;
import com.init.panjj.activity.SubtitlePlayer;
import com.init.panjj.model.ItemBean;
import com.init.panjj.otherclasses.CustomImageView;
import com.init.panjj.otherclasses.ProgressBarCircular;
import com.init.panjj.radioplayer.Controls;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.ArrayList;

/**
 * Created by deepak on 10/19/2015.
 */
public class MainContentAdoptor3 extends RecyclerView.Adapter<MainContentAdoptor3.MyViewHolder> {
   public static ArrayList<ItemBean> list;
  public static   ArrayList<ItemBean> urllist;
    MainActivity act;
    Intent intent;
    int coun = 0;

    public MainContentAdoptor3(Activity activity, ArrayList<ItemBean> mainlist, ArrayList<ItemBean> url4) {
        act = (MainActivity) activity;
        list = mainlist;
urllist=url4;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new MyViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.new_maincontent, viewGroup,false));
    }

    @Override
    public void onBindViewHolder(final MyViewHolder myViewHolder, final int i) {
        final ItemBean itemBean = list.get(i);

        myViewHolder.title.setText(itemBean.albumname);
      // myViewHolder.subt.setText(itemBean.albumdesp);
        myViewHolder.img.scaledImage(itemBean.albumcover);
        myViewHolder.img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*Intent it=new Intent(act,PlayVideo.class);
                it.putExtra("list",new UrlBean(urllist));
                it.putExtra("position",i);
                it.putExtra("id",itemBean.id);
                act.startActivity(it);*/
              /*  MainActivity.allurl=urllist;
                act.replaceFragment(new New_Video_home(),"zdsd",itemBean.tredcover,"videos"+i, itemBean.tredname + " " + itemBean.treddesp, itemBean.id, 1);
*/
                Controls.pauseControl(act);
                MainActivity.allurl = urllist;
                Intent it = new Intent(act, SubtitlePlayer.class);
                it.putExtra("url", urllist.get(i).m3u8);
                it.putExtra("pos", i);
                it.putExtra("id",  list.get(i).id);
                act.startActivity(it);
            }
        });

        ImageLoader.getInstance().loadImage(itemBean.albumcover, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {
                myViewHolder.img.setImageResource(R.drawable.logo_fade);
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {

                super.onLoadingComplete(imageUri, view, loadedImage);
                myViewHolder.img.setImageBitmap(loadedImage);
myViewHolder.pb.setVisibility(View.GONE);
            }
        });
    }


    @Override
    public int getItemCount() {
        Log.e("list", "" + list.size());
        return list.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView title, subt;
        CustomImageView img;
ProgressBarCircular pb;
        FrameLayout fm;
        public MyViewHolder(View itemView) {
            super(itemView);
            Display mDisplay = act.getWindowManager().getDefaultDisplay();
            final int width  = mDisplay.getWidth();
            final int height = mDisplay.getHeight();
            int framewitdh=width/3;
            Log.e("hshdfkjsf",""+height);
            fm= (FrameLayout) itemView.findViewById(R.id.mframe);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(framewitdh, (int) act.getResources().getDimension(R.dimen.trendingcontainer_height));
            fm.setLayoutParams(lp);
            pb= (ProgressBarCircular) itemView.findViewById(R.id.progress);
            title = (TextView) itemView.findViewById(R.id.title);
            img = (CustomImageView) itemView.findViewById(R.id.icon);
            subt = (TextView) itemView.findViewById(R.id.subtitle);

        }
    }
}
