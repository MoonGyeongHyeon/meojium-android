package com.moon.meojium.ui.home;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;
import com.moon.meojium.R;
import com.moon.meojium.base.util.SharedPreferencesService;
import com.moon.meojium.database.dao.MuseumDao;
import com.moon.meojium.database.dao.TastingDao;
import com.moon.meojium.database.dao.UserDao;
import com.moon.meojium.model.UpdateResult;
import com.moon.meojium.model.museum.Museum;
import com.moon.meojium.model.tasting.Tasting;
import com.moon.meojium.model.user.Info;
import com.moon.meojium.ui.allmuseum.AllMuseumActivity;
import com.moon.meojium.ui.interested.InterestedActivity;
import com.moon.meojium.ui.login.LoginActivity;
import com.moon.meojium.ui.login.LoginType;
import com.moon.meojium.ui.login.naver.NaverLogin;
import com.moon.meojium.ui.nearby.NearbyActivity;
import com.moon.meojium.ui.search.SearchActivity;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by moon on 2017. 8. 6..
 */

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        SwipeRefreshLayout.OnRefreshListener, View.OnTouchListener {
    private static final int TASTING_COUNT = 4;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.drawerlayout)
    DrawerLayout drawerLayout;
    @BindView(R.id.navigationview)
    NavigationView navigationView;
    @BindView(R.id.viewpager_home_popular_museum)
    ViewPager popularMuseumViewPager;
    @BindView(R.id.viewpager_home_history_museum)
    ViewPager historyMuseumViewPager;
    @BindView(R.id.recyclerview_home_tasting)
    RecyclerView tastingRecyclerView;
    @BindView(R.id.imageview_home_nearby)
    ImageView nearbyImageView;
    @BindView(R.id.swipe_refresh_home)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.textview_home_popular_fail_connection)
    TextView popularFailConnectionTextView;
    @BindView(R.id.textview_home_tasting_fail_connection)
    TextView tastingFailConnectionTextView;
    @BindView(R.id.textview_home_history_fail_connection)
    TextView historyFailConnectionTextView;
    @BindView(R.id.recyclerview_home_area)
    RecyclerView areaRecyclerView;
    @BindView(R.id.textview_home_area_fail_connection)
    TextView areaFailConnectionTextView;

    @OnClick(R.id.imageview_home_nearby)
    public void onClick(View view) {
        Intent intent = new Intent(this, NearbyActivity.class);
        startActivity(intent);
    }

    private TextView nicknameTextView;
    private TextView registeredDateTextView;
    private TextView favoriteTextView;
    private TextView stampTextView;
    private BackPressCloseHandler backPressCloseHandler;
    private List<Museum> popularMuseumList;
    private List<Museum> historyMuseumList;
    private List<Tasting> tastingMuseumList;
    private List<Museum> areaMuseumList;
    private SharedPreferencesService sharedPreferencesService;
    private MuseumDao museumDao;
    private TastingDao tastingDao;
    private UserDao userDao;
    private TastingRecyclerViewAdapter tastingAdapter;
    private AreaRecyclerViewAdapter areaAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);

        backPressCloseHandler = new BackPressCloseHandler(this);

        museumDao = MuseumDao.getInstance();
        tastingDao = TastingDao.getInstance();
        userDao = UserDao.getInstance();

        sharedPreferencesService = SharedPreferencesService.getInstance();

        initNavigationView();

        requestPopularMuseumData();
        requestHistoryMuseumData();
        requestTastingMuseumData();
        requestAreaMuseumData();

        initToolbar();
        initDrawerLayout();
        initNearbyImageView();
        initSwipeRefreshLayout();
    }

    @Override
    protected void onResume() {
        super.onResume();

        requestUserInfo();
    }

    private void initNavigationView() {

        navigationView.setNavigationItemSelectedListener(this);

        View header = navigationView.getHeaderView(0);
        nicknameTextView = header.findViewById(R.id.textview_navigation_nickname);
        registeredDateTextView = header.findViewById(R.id.textview_navigation_registered_date);
        favoriteTextView = header.findViewById(R.id.textview_navigation_favorite);
        stampTextView = header.findViewById(R.id.textview_navigation_stamp);

        nicknameTextView.setText(String.format(getResources().getString(R.string.navigation_nickname),
                sharedPreferencesService.getStringData(SharedPreferencesService.KEY_NICKNAME)));

        ImageView imageView = header.findViewById(R.id.imageview_navigation_change_nickname);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText editText = new EditText(HomeActivity.this);
                editText.setText(sharedPreferencesService.getStringData(SharedPreferencesService.KEY_NICKNAME));
                new AlertDialog.Builder(HomeActivity.this)
                        .setTitle("닉네임 변경")
                        .setView(editText)
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                int len;
                                try {
                                    len = editText.getText().toString().trim().getBytes("MS949").length;
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                    len = 100;
                                }
                                Log.d("Test", "" + len);
                                if (len == 0) {
                                    Toasty.info(HomeActivity.this, "닉네임을 입력해주세요.").show();
                                } else if (len > 15) {
                                    Toasty.info(HomeActivity.this, "닉네임은 한글 7자, 알파벳 15자까지 가능합니다.").show();
                                } else {
                                    final String nickname = editText.getText().toString();
                                    Call<UpdateResult> call = userDao.updateNickname(sharedPreferencesService.getStringData(SharedPreferencesService.KEY_ENC_ID),
                                            nickname);
                                    call.enqueue(new Callback<UpdateResult>() {
                                        @Override
                                        public void onResponse(Call<UpdateResult> call, Response<UpdateResult> response) {
                                            UpdateResult result = response.body();

                                            if (result.getCode() == UpdateResult.RESULT_OK) {
                                                Log.d("Meojium/Home", "Success Updating Nickname");
                                                sharedPreferencesService.putData(SharedPreferencesService.KEY_NICKNAME,
                                                        nickname);
                                                nicknameTextView.setText(String.format(getResources().getString(R.string.navigation_nickname),
                                                        nickname));
                                            } else {
                                                Log.d("Meojium/Home", "Fail Updating Nickname");
                                                Toasty.info(HomeActivity.this, getResources().getString(R.string.fail_connection)).show();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<UpdateResult> call, Throwable t) {
                                            Toasty.info(HomeActivity.this, getResources().getString(R.string.fail_connection)).show();
                                        }
                                    });
                                }

                            }
                        })
                        .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        })
                        .create()
                        .show();
            }
        });
    }

    private void requestPopularMuseumData() {
        Call<List<Museum>> call = museumDao.getPopularMuseumList();
        call.enqueue(new Callback<List<Museum>>() {
            @Override
            public void onResponse(Call<List<Museum>> call, Response<List<Museum>> response) {
                Log.d("Meojium/Home", "Success Getting Popular Museum List");
                popularMuseumList = response.body();

                popularFailConnectionTextView.setVisibility(View.GONE);
                initPopularMuseumViewPager();
            }

            @Override
            public void onFailure(Call<List<Museum>> call, Throwable t) {
                Toasty.info(HomeActivity.this, getResources().getString(R.string.fail_connection)).show();
                popularFailConnectionTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void initPopularMuseumViewPager() {
        MuseumViewPagerAdapter popularAdapter = new MuseumViewPagerAdapter(getSupportFragmentManager(), popularMuseumList);
        popularMuseumViewPager.setAdapter(popularAdapter);
        popularMuseumViewPager.setPageMargin(32);
        popularMuseumViewPager.setOnTouchListener(this);
    }

    private void requestHistoryMuseumData() {
        Call<List<Museum>> call = museumDao.getHistoryMuseumList();
        call.enqueue(new Callback<List<Museum>>() {
            @Override
            public void onResponse(Call<List<Museum>> call, Response<List<Museum>> response) {
                Log.d("Meojium/Home", "Success Getting History Museum List");
                historyMuseumList = response.body();

                historyFailConnectionTextView.setVisibility(View.GONE);
                initHistoryMuseumViewPager();
            }

            @Override
            public void onFailure(Call<List<Museum>> call, Throwable t) {
                Toasty.info(HomeActivity.this, getResources().getString(R.string.fail_connection)).show();
                historyFailConnectionTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void initHistoryMuseumViewPager() {
        MuseumViewPagerAdapter historyAdapter = new MuseumViewPagerAdapter(getSupportFragmentManager(), historyMuseumList);
        historyMuseumViewPager.setAdapter(historyAdapter);
        historyMuseumViewPager.setPageMargin(32);
        historyMuseumViewPager.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_MOVE:
                swipeRefreshLayout.setEnabled(false);
                break;
            case MotionEvent.ACTION_UP:
                swipeRefreshLayout.setEnabled(true);
                break;
        }
        return false;
    }

    private void requestTastingMuseumData() {
        Call<List<Tasting>> call = tastingDao.getTastingMuseumList();
        call.enqueue(new Callback<List<Tasting>>() {
            @Override
            public void onResponse(Call<List<Tasting>> call, Response<List<Tasting>> response) {
                List<Tasting> list = response.body();

                tastingMuseumList = new ArrayList<>();
                handleDuplicatedMuseum(list);

                tastingFailConnectionTextView.setVisibility(View.GONE);
                initTastingMuseumRecyclerView();
            }

            @Override
            public void onFailure(Call<List<Tasting>> call, Throwable t) {
                Toasty.info(HomeActivity.this, getResources().getString(R.string.fail_connection)).show();
                tastingFailConnectionTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void handleDuplicatedMuseum(List<Tasting> target) {
        boolean flag;

        for (Tasting baseTasting : target) {
            flag = true;

            for (Tasting tasting : tastingMuseumList) {
                if (baseTasting.getMuseum().getId() == tasting.getMuseum().getId()) {
                    flag = false;
                    break;
                }
            }

            if (flag) {
                tastingMuseumList.add(baseTasting);
            }

            if (tastingMuseumList.size() == TASTING_COUNT) {
                break;
            }
        }
    }

    private void initTastingMuseumRecyclerView() {
        tastingAdapter = new TastingRecyclerViewAdapter(tastingMuseumList, this);
        tastingRecyclerView.setAdapter(tastingAdapter);

        RecyclerView.LayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        tastingRecyclerView.setLayoutManager(manager);

        tastingRecyclerView.setNestedScrollingEnabled(false);
    }

    private void requestAreaMuseumData() {
        Call<List<Museum>> call = museumDao.getAreaMuseumList();
        call.enqueue(new Callback<List<Museum>>() {
            @Override
            public void onResponse(Call<List<Museum>> call, Response<List<Museum>> response) {
                Log.d("Meojium/Home", "Success Getting AreaMuseum");
                areaMuseumList = response.body();

                areaFailConnectionTextView.setVisibility(View.GONE);
                initAreaMuseumRecyclerView();
            }

            @Override
            public void onFailure(Call<List<Museum>> call, Throwable t) {
                Log.d("Meojium/Home", "Fail Getting AreaMuseum");
                Toasty.info(HomeActivity.this, getResources().getString(R.string.fail_connection)).show();
                areaFailConnectionTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void initAreaMuseumRecyclerView() {
        areaAdapter = new AreaRecyclerViewAdapter(areaMuseumList, this);
        areaRecyclerView.setAdapter(areaAdapter);

        RecyclerView.LayoutManager manager = new GridLayoutManager(this, 2, LinearLayoutManager.VERTICAL, false);
        areaRecyclerView.setLayoutManager(manager);

        areaRecyclerView.setNestedScrollingEnabled(false);
    }

    private void requestUserInfo() {
        Call<Info> call = userDao.getUserInfo(sharedPreferencesService.getStringData(SharedPreferencesService.KEY_ENC_ID));
        call.enqueue(new Callback<Info>() {
            @Override
            public void onResponse(Call<Info> call, Response<Info> response) {
                Log.d("Meojium/Home", "Success Getting User Info");

                Info info = response.body();

                registeredDateTextView.setText(info.getRegisteredDate());
                favoriteTextView.setText(String.valueOf(info.getFavoriteCount()));
                stampTextView.setText(String.valueOf(info.getStampCount()));
            }

            @Override
            public void onFailure(Call<Info> call, Throwable t) {
                Log.d("Meojium/Home", "Fail Getting User Info");
                Toasty.info(HomeActivity.this, getResources().getString(R.string.fail_connection)).show();
            }
        });
    }

    private void initToolbar() {
        toolbar.setTitle("머지엄");
        setSupportActionBar(toolbar);
    }

    private void initDrawerLayout() {
        ActionBarDrawerToggle toggle =
                new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void initNearbyImageView() {
        Glide.with(this)
                .load(R.drawable.img_nearby_museum)
                .placeholder(R.drawable.img_placeholder)
                .into(nearbyImageView);
    }

    private void initSwipeRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener(this);
    }

    @Override
    public void onRefresh() {
        if (historyMuseumList != null) {
            historyMuseumList.clear();
        }
        if (popularMuseumList != null) {
            popularMuseumList.clear();
        }
        if (tastingMuseumList != null) {
            tastingMuseumList.clear();
            tastingAdapter.notifyDataSetChanged();
        }
        if (areaMuseumList != null) {
            areaMuseumList.clear();
            areaAdapter.notifyDataSetChanged();
        }

        requestHistoryMuseumData();
        requestPopularMuseumData();
        requestTastingMuseumData();
        requestAreaMuseumData();
        requestUserInfo();

        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            backPressCloseHandler.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                Intent searchIntent = new Intent(this, SearchActivity.class);
                searchIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(searchIntent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.navigation_home:
                break;
            case R.id.navigation_all_museum:
                Handler allMuseumHandler = new Handler();
                allMuseumHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(HomeActivity.this, AllMuseumActivity.class);
                        startActivity(intent);
                    }
                }, 200);
                break;
            case R.id.navigation_interested_museum:
                Handler interestedMuseumHandler = new Handler();
                interestedMuseumHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent interestedIntent = new Intent(HomeActivity.this, InterestedActivity.class);
                        startActivity(interestedIntent);
                    }
                }, 200);
                break;
            case R.id.navigation_logout:
                Log.d("Meojium/Home", "Try Logout");
                new AlertDialog.Builder(this)
                        .setTitle("로그아웃")
                        .setMessage("로그아웃 하시겠습니까?")
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                logout();

                                Intent logoutIntent = new Intent(HomeActivity.this, LoginActivity.class);
                                logoutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(logoutIntent);
                            }
                        })
                        .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        })
                        .create()
                        .show();
                break;
        }

        drawerLayout.closeDrawers();

        return true;
    }

    private void logout() {
        String type = sharedPreferencesService.getStringData(SharedPreferencesService.KEY_TYPE);

        Log.d("Meojium/Home", SharedPreferencesService.KEY_TYPE + ": " + type);
        Log.d("Meojium/Home", SharedPreferencesService.KEY_NICKNAME + ": " +
                sharedPreferencesService.getStringData(SharedPreferencesService.KEY_NICKNAME));

        switch (type) {
            case LoginType.NAVER:
                NaverLogin naverLogin = new NaverLogin(this);
                naverLogin.logout();
                break;
            case LoginType.KAKAO:
                UserManagement.requestLogout(new LogoutResponseCallback() {
                    @Override
                    public void onCompleteLogout() {
                        Log.d("Meojium/KakaoLogin", "Logout Kakao");
                    }
                });
                break;
        }

        sharedPreferencesService.removeData(SharedPreferencesService.KEY_ENC_ID,
                SharedPreferencesService.KEY_TYPE, SharedPreferencesService.KEY_NICKNAME);
    }
}
