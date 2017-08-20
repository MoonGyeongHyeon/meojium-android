package com.moon.meojium.database.dao;

import com.moon.meojium.base.BaseRetrofitService;
import com.moon.meojium.database.service.MuseumService;
import com.moon.meojium.model.museum.Museum;

import java.util.List;

import retrofit2.Call;

/**
 * Created by moon on 2017. 8. 17..
 */

public class MuseumDao extends BaseRetrofitService {
    private static MuseumDao dao;
    private MuseumService service;

    public static MuseumDao getInstance() {
        if (dao == null) {
            synchronized (MuseumDao.class) {
                if (dao == null) {
                    dao = new MuseumDao();
                }
            }
        }
        return dao;
    }

    private MuseumDao() {
        init();
        setClass(MuseumService.class);
    }

    private void setClass(Class<?> type) {
        service = (MuseumService) retrofit.create(type);
    }

    public Call<List<Museum>> getPopularMuseumList() {
        return service.getPopularMuseumList();
    }

    public Call<List<Museum>> getHistoryMuseumList() {
        return service.getHistoryMuseumList();
    }

    public Call<List<String>> getMuseumImageUrlList(int id) {
        return service.getMuseumImageUrlList(id);
    }

}