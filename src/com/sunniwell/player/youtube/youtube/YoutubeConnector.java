package com.sunniwell.player.youtube.youtube;

import android.content.Context;
import android.util.Log;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.sunniwell.player.youtube.R;
import com.sunniwell.player.youtube.bean.VideoBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/8/16.
 */
public class YoutubeConnector {
    private static final Long NUMBER_OF_VIDEO = 50L;

    private YouTube mYoutube;
    private YouTube.Search.List mQuery;
    SearchListResponse mResponse;

    public static final String KEY
            = "AIzaSyCXMzog2jsnKSy62lpYemc5x4ofOiHNl_8";

    public YoutubeConnector(Context context) {
        mYoutube = new YouTube.Builder(new NetHttpTransport(),
                new JacksonFactory(), new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest hr) throws IOException {}
        }).setApplicationName(context.getString(R.string.app_name)).build();

        try{
            mQuery = mYoutube.search().list("id,snippet");
            mQuery.setKey(KEY);
            mQuery.setType("video");
            mQuery.setFields("items(id/videoId,snippet/title,snippet/description,snippet/thumbnails/default/url),nextPageToken");
            mQuery.setMaxResults(NUMBER_OF_VIDEO);
        }catch(IOException e){
            Log.d("YC", "Could not initialize: "+e);
        }
    }

    public List<VideoBean> search(String keywords){
        mQuery.setQ(keywords);
        try{
            mResponse = mQuery.execute();
            List<SearchResult> results = mResponse.getItems();

            List<VideoBean> items = new ArrayList<VideoBean>();
            for(SearchResult result:results){
                VideoBean item = new VideoBean();
                item.setTitle(result.getSnippet().getTitle());
                item.setDescription(result.getSnippet().getDescription());
                item.setThumbnailURL(result.getSnippet().getThumbnails().getDefault().getUrl());
                item.setId(result.getId().getVideoId());
                items.add(item);
            }
            return items;
        }catch(IOException e){
            Log.d("YC", "Could not search: "+e);
            return null;
        }
    }

    public List<VideoBean> getNextPage() {
        String nextPage = mResponse.getNextPageToken();
        Log.d("YC", "nextPage: " + nextPage);
        mQuery.setPageToken(nextPage);
        try{
            mResponse = mQuery.execute();
            List<SearchResult> results = mResponse.getItems();

            List<VideoBean> items = new ArrayList<VideoBean>();
            for(SearchResult result:results){
                VideoBean item = new VideoBean();
                item.setTitle(result.getSnippet().getTitle());
                item.setDescription(result.getSnippet().getDescription());
                item.setThumbnailURL(result.getSnippet().getThumbnails().getDefault().getUrl());
                item.setId(result.getId().getVideoId());
                items.add(item);
            }
            return items;
        }catch(IOException e){
            Log.d("YC", "Could not search: "+e);
            return null;
        }
    }
}
