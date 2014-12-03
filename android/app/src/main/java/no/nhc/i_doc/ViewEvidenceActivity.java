package no.nhc.i_doc;

import android.app.ActionBar;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ViewEvidenceActivity extends FragmentActivity {

    ViewEvidencePagerAdapter mViewEvidencePagerAdapter;
    ViewPager mViewPager;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_evidence);

        mViewEvidencePagerAdapter = new ViewEvidencePagerAdapter(getSupportFragmentManager(),
                DocumentDB.get(this).getDocumentList());
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        mViewPager = (ViewPager) findViewById(R.id.activity_view_evidence);
        mViewPager.setAdapter(mViewEvidencePagerAdapter);
        mViewPager.setCurrentItem(getIntent().getIntExtra("position", 0));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This is called when the Home (Up) button is pressed in the action bar.
                // Create a simple intent that starts the hierarchical parent activity and
                // use NavUtils in the Support Package to ensure proper handling of Up.
                Intent upIntent = new Intent(this, MainActivity.class);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    // This activity is not part of the application's task, so create a new task
                    // with a synthesized back stack.
                    TaskStackBuilder.from(this)
                            // If there are ancestor activities, they should be added here.
                            .addNextIntent(upIntent)
                            .startActivities();
                    finish();
                } else {
                    // This activity is part of the application's task, so simply
                    // navigate up to the hierarchical parent activity.
                    NavUtils.navigateUpTo(this, upIntent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class ViewEvidencePagerAdapter extends FragmentStatePagerAdapter {

        private final DocumentDB.List evidenceList;

        public ViewEvidencePagerAdapter(FragmentManager fm, DocumentDB.List evidenceList) {
            super(fm);

            this.evidenceList = evidenceList;

            this.evidenceList.setListener(new DocumentDB.Listener() {
                @Override
                public void changed() {
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = new ViewEvidenceFragment();
            Document doc = evidenceList.getDocument(i);
            Bundle args = new Bundle();
            args.putString(ViewEvidenceFragment.ARG_DOCUMENT_URI, doc.getUri().toString());
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            return evidenceList.getCount();
        }
    }

    public static class ViewEvidenceFragment extends Fragment {

        public static final String ARG_DOCUMENT_URI = "docUri";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_view_evidence, container, false);
            Bundle args = getArguments();
            Document doc = DocumentDB.get(getActivity()).getDocument(Uri.parse(args.getString(ARG_DOCUMENT_URI)));

            ImageView imageView = (ImageView) rootView.findViewById(R.id.evidenceImage);
            imageView.setImageDrawable(null);
            DocumentUtils.DisplayImage(doc, imageView);

            ((TextView) rootView.findViewById(R.id.text_evidence_title)).setText(doc.getTitle());
            return rootView;
        }
    }
}
