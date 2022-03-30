package com.nic.PMAYSurvey.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.VolleyError;
import com.nic.PMAYSurvey.Fragment.SlideshowDialogFragment;
import com.nic.PMAYSurvey.R;
import com.nic.PMAYSurvey.adapter.FullImageAdapter;
import com.nic.PMAYSurvey.api.Api;
import com.nic.PMAYSurvey.api.ApiService;
import com.nic.PMAYSurvey.api.ServerResponse;
import com.nic.PMAYSurvey.constant.AppConstant;
import com.nic.PMAYSurvey.dataBase.dbData;
import com.nic.PMAYSurvey.databinding.FullImageRecyclerBinding;
import com.nic.PMAYSurvey.model.PMAYSurvey;
import com.nic.PMAYSurvey.session.PrefManager;
import com.nic.PMAYSurvey.utils.UrlGenerator;
import com.nic.PMAYSurvey.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class FullImageActivity extends AppCompatActivity implements View.OnClickListener, Api.ServerResponseListener {
    private FullImageRecyclerBinding fullImageRecyclerBinding;
    private FullImageAdapter fullImageAdapter;
    private PrefManager prefManager;
    private static  ArrayList<PMAYSurvey> activityImage = new ArrayList<>();
    private com.nic.PMAYSurvey.dataBase.dbData dbData = new dbData(this);
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fullImageRecyclerBinding = DataBindingUtil.setContentView(this, R.layout.full_image_recycler);
        fullImageRecyclerBinding.setActivity(this);
        prefManager = new PrefManager(this);
//        work_id = ;

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getApplicationContext(),2);
        fullImageRecyclerBinding.imagePreviewRecyclerview.setLayoutManager(mLayoutManager);
        fullImageRecyclerBinding.imagePreviewRecyclerview.setItemAnimator(new DefaultItemAnimator());
        fullImageRecyclerBinding.imagePreviewRecyclerview.setHasFixedSize(true);
        fullImageRecyclerBinding.imagePreviewRecyclerview.setNestedScrollingEnabled(false);
        fullImageRecyclerBinding.imagePreviewRecyclerview.setFocusable(false);
        fullImageRecyclerBinding.imagePreviewRecyclerview.setAdapter(fullImageAdapter);
        new fetchImagetask().execute();
        if(Utils.isOnline()){
            getOnlineImage();
        }


    }
    public class fetchImagetask extends AsyncTask<Void, Void,
            ArrayList<PMAYSurvey>> {
        @Override
        protected ArrayList<PMAYSurvey> doInBackground(Void... params) {

            final String pmay_id = getIntent().getStringExtra(AppConstant.PMAY_ID);

            dbData.open();
            activityImage = new ArrayList<>();
            activityImage = dbData.getSavedPMAYImages(pmay_id,"");


            Log.d("IMAGE_COUNT", String.valueOf(activityImage.size()));
            return activityImage;
        }

        @Override
        protected void onPostExecute(final ArrayList<PMAYSurvey> imageList) {
            super.onPostExecute(imageList);
            setAdapter();
        }
    }
    public void homePage() {
        Intent intent = new Intent(this, HomePage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("Home", "Home");
        startActivity(intent);
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
    }

    public void onBackPress() {
        super.onBackPressed();
        setResult(Activity.RESULT_CANCELED);
        overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
    }

    @Override
    public void onClick(View view) {

    }

    public void getOnlineImage() {
        try {
            new ApiService(this).makeJSONObjectRequest("OnlineImage", Api.Method.POST, UrlGenerator.getPMAYListUrl(), ImagesJsonParams(), "not cache", this);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject ImagesJsonParams() throws JSONException {
        String authKey = Utils.encrypt(prefManager.getUserPassKey(), getResources().getString(R.string.init_vector), ImagesListJsonParams().toString());
        JSONObject dataSet = new JSONObject();
        dataSet.put(AppConstant.KEY_USER_NAME, prefManager.getUserName());
        dataSet.put(AppConstant.DATA_CONTENT, authKey);
        Log.d("utils_ImageEncrydataSet", "" + authKey);
        return dataSet;
    }

    public JSONObject ImagesListJsonParams() throws JSONException {
        JSONObject dataSet = new JSONObject();
        dataSet.put(AppConstant.KEY_SERVICE_ID, AppConstant.KEY_PMAY_SOURCE_DATA_PHOTO);
        dataSet.put(AppConstant.DISTRICT_CODE, prefManager.getDistrictCode());
        dataSet.put(AppConstant.BLOCK_CODE, prefManager.getBlockCode());
        dataSet.put(AppConstant.PV_CODE, getIntent().getStringExtra(AppConstant.PV_CODE));
        dataSet.put(AppConstant.HAB_CODE, getIntent().getStringExtra(AppConstant.HAB_CODE));
        dataSet.put(AppConstant.SECC_ID, getIntent().getStringExtra(AppConstant.SECC_ID));
        Log.d("utils_imageDataset", "" + dataSet);
        return dataSet;
    }

    @Override
    public void OnMyResponse(ServerResponse serverResponse) {
        try {
            String urlType = serverResponse.getApi();
            JSONObject responseObj = serverResponse.getJsonResponse();

            if ("OnlineImage".equals(urlType) && responseObj != null) {
                String key = responseObj.getString(AppConstant.ENCODE_DATA);
                String responseDecryptedBlockKey = Utils.decrypt(prefManager.getUserPassKey(), key);
                JSONObject jsonObject = new JSONObject(responseDecryptedBlockKey);
                if (jsonObject.getString("STATUS").equalsIgnoreCase("OK") && jsonObject.getString("RESPONSE").equalsIgnoreCase("OK")) {
                    generateImageArrayList(jsonObject.getJSONArray(AppConstant.JSON_DATA));
                }
                Log.d("resp_OnlineImage", "" + responseDecryptedBlockKey);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void generateImageArrayList(JSONArray jsonArray){
        if(jsonArray.length() > 0){
            activityImage = new ArrayList<>();
            for(int i = 0; i < jsonArray.length(); i++ ) {
                try {
                    PMAYSurvey imageOnline = new PMAYSurvey();

                    byte[] decodedString = Base64.decode(jsonArray.getJSONObject(i).getString(AppConstant.KEY_IMAGE), Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                    imageOnline.setImage(decodedByte);

                    activityImage.add(imageOnline);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            setAdapter();
        }
    }
    public void setAdapter(){
        fullImageAdapter = new FullImageAdapter(FullImageActivity.this,
                activityImage, dbData);
        fullImageRecyclerBinding.imagePreviewRecyclerview.addOnItemTouchListener(new FullImageAdapter.RecyclerTouchListener(getApplicationContext(), fullImageRecyclerBinding.imagePreviewRecyclerview, new FullImageAdapter.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("images", activityImage);
                bundle.putInt("position", position);

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                SlideshowDialogFragment newFragment = SlideshowDialogFragment.newInstance();
                newFragment.setArguments(bundle);
                newFragment.show(ft, "slideshow");
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
        fullImageRecyclerBinding.imagePreviewRecyclerview.setAdapter(fullImageAdapter);
    }



    @Override
    public void OnError(VolleyError volleyError) {

    }
}
