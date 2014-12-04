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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_evidence, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.evidence_edit: {
            Intent intent = new Intent(this, EditEvidenceActivity.class);
            Document doc = mViewEvidencePagerAdapter.getDocument(mViewPager.getCurrentItem());
            intent.setData(doc.getUri());
            startActivity(intent);
            break;
        }            
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

        public Document getDocument(int i) {
            return evidenceList.getDocument(i);
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
        Document mDocument;
        Document.Listener mDocumentListener;


        private ViewGroup addGroup(CharSequence header, ViewGroup container) {
            ViewGroup layoutView = new LinearLayout(getActivity());
            TextView headerView = new TextView(getActivity());
            headerView.setText(header);
            layoutView.addView(headerView);
            container.addView(layoutView);
            return layoutView;
        }

        private void populateView(View rootView) {
            ImageView imageView = (ImageView) rootView.findViewById(R.id.evidenceImage);
            imageView.setImageDrawable(null);
            DocumentUtils.DisplayImage(mDocument, imageView);

            imageView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String sourceUri = DocumentUtils.GetSourceUri((ImageView)v);
                    switch (DocumentUtils.GetMediaType(sourceUri)) {
                        case MEDIA_TYPE_IMAGE:
                            try {
                                Intent intent = new Intent();
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.parse(sourceUri), "image/*");
                                startActivity(intent);
                            } catch (Exception e) {
                                // No valid handler
                            }
                            break;
                        case MEDIA_TYPE_VIDEO:
                            try {
                                Intent intent = new Intent();
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.parse(sourceUri), "video/*");
                                startActivity(intent);
                            } catch (Exception e) {
                                // No valid handler
                            }
                            break;
                        case MEDIA_TYPE_AUDIO:
                            break;
                        case MEDIA_TYPE_UNKNOWN:
                            break;
                    }
                }
            });

            ((TextView)rootView.findViewById(R.id.text_evidence_title)).setText(mDocument.getTitle());

            ViewGroup metaContainer = (ViewGroup) rootView.findViewById(R.id.metadataContainer);
            metaContainer.removeAllViews();
            ViewGroup group;
            group = addGroup(getText(R.string.victims), metaContainer);
            group = addGroup(getText(R.string.suspects), metaContainer);
            group = addGroup(getText(R.string.witnesses), metaContainer);
            group = addGroup(getText(R.string.context), metaContainer);
            group = addGroup(getText(R.string.incident), metaContainer);
            group = addGroup(getText(R.string.protected_object), metaContainer);
            group = addGroup(getText(R.string.organizational_unit), metaContainer);
            
        }

        @Override
        public void onDestroyView() {
            mDocument.removeChangeListener(mDocumentListener);
            mDocument = null;
            mDocumentListener = null;
            super.onDestroyView();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_view_evidence, container, false);
            mDocument = DocumentDB.get(getActivity()).getDocument(Uri.parse(getArguments().getString(ARG_DOCUMENT_URI)));
            populateView(rootView);
            mDocumentListener = new Document.Listener() {
                    public void changed() {
                        Log.d("CouchDocumentDB", "changed");
                        populateView(rootView);
                    }
                };
            mDocument.addChangeListener(mDocumentListener);
            
            return rootView;
        }
    }
}
