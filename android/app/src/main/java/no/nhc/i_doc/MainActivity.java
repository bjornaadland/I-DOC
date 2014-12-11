package no.nhc.i_doc;


import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class MainActivity extends FragmentActivity implements ActionBar.TabListener
{
    static final String TAG = "MainActivity";
    AppSectionsPagerAdapter mAppSectionsPagerAdapter;
    ViewPager mViewPager;
    ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager());

        final ActionBar actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAppSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        for (int i = 0; i < mAppSectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mAppSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }

        ImageLoader imageLoader = ImageLoader.getInstance();
        if (imageLoader.isInited() == false) {
            imageLoader.init(ImageLoaderConfiguration.createDefault(this));
        }

        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);
        mProgressBar.setMax(100);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void sync() {
        final ProgressDialog progressDialog = ProgressDialog.show(MainActivity.this, "Please wait ...", "Syncing", false);
        DocumentDB.get(this).sync(
            new DocumentDB.SyncListener() {
                @Override
                public void onEvent(DocumentDB.SyncEvent event) {
                    switch(event.getEvent()) {
                    case DocumentDB.SyncEvent.STARTED:
                        break;
                    case DocumentDB.SyncEvent.STOPPED:
                        progressDialog.dismiss();
                        break;
                    case DocumentDB.SyncEvent.PROGRESS:
                        progressDialog.setMax(event.getMax());
                        progressDialog.setProgress(event.getProgress());
                        break;
                    }
                }
            });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_settings:
            startActivity(new Intent(this, SettingsActivity.class));
            break;
        case R.id.action_sync:
            sync();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private class AppSectionsPagerAdapter extends FragmentPagerAdapter {

        public AppSectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return new CaptureFragment();
                case 1:
                    return new EvidenceListFragment();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
            case 0:
                return getText(R.string.gather_evidence);
            default:
                return getText(R.string.evidence);
            }
        }
    }

    public void showProgress() {
        mProgressBar.setVisibility(ProgressBar.VISIBLE);
    }

    public void hideProgress() {
        mProgressBar.setVisibility(ProgressBar.GONE);
    }

    public void progress(int progress) {
        mProgressBar.setProgress(progress);
    }
}
