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
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ViewEvidenceActivity extends FragmentActivity {

    ViewEvidencePagerAdapter mViewEvidencePagerAdapter;
    ViewPager mViewPager;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_evidence);

        mViewEvidencePagerAdapter = new ViewEvidencePagerAdapter(getSupportFragmentManager(),
                this, DocumentDB.get(this).getDocumentList());
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
        case R.id.evidence_delete: {
            Document doc = mViewEvidencePagerAdapter.getDocument(mViewPager.getCurrentItem());
            DocumentDB.get(this).deleteDocument(doc);
            break;
        }
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

        private final DocumentDB.List<Document> evidenceList;
        private final DocumentDB.Listener mListener;
        private final ViewEvidenceActivity mOwner;
        private final FragmentManager mFragmentManager;

        public ViewEvidencePagerAdapter(FragmentManager fm, ViewEvidenceActivity owner, final DocumentDB.List evidenceList) {
            super(fm);

            this.mOwner = owner;
            this.evidenceList = evidenceList;
            this.mFragmentManager = fm;

            this.evidenceList.addListener(mListener = new DocumentDB.Listener() {
                @Override
                public void changed() {
                    if (evidenceList.getCount() == 0) {
                        mOwner.finish();
                    } else {
                        notifyDataSetChanged();
                    }
                }
            });
        }

        public Document getDocument(int i) {
            return evidenceList.getObject(i);
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = new ViewEvidenceFragment();
            Document doc = evidenceList.getObject(i);
            Bundle args = new Bundle();
            args.putString(ViewEvidenceFragment.ARG_DOCUMENT_URI, doc.getUri().toString());
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getItemPosition(Object item) {
            if(mFragmentManager.getFragments().contains(item)) {
                return POSITION_NONE;
            } else {
                return POSITION_UNCHANGED;
            }
        }

        @Override
        public int getCount() {
            int count = evidenceList.getCount();
            return count;
        }
    }

    public static class ViewEvidenceFragment extends Fragment {

        public static final String ARG_DOCUMENT_URI = "docUri";
        Document mDocument;
        Document.Listener mDocumentListener;


        private ViewGroup addGroup(CharSequence header, ViewGroup container) {
            LinearLayout layoutView = new LinearLayout(getActivity());
            LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                              LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutView.setLayoutParams(layoutParams);
            layoutView.setOrientation(LinearLayout.VERTICAL);
            TextView headerView = new TextView(getActivity());
            headerView.setTextAppearance(getActivity(), android.R.style.TextAppearance_Medium);
            headerView.setText(header);
            layoutView.addView(headerView);
            container.addView(layoutView);
            return layoutView;
        }

        private void addGroupData(String data, ViewGroup group) {
            TextView dataView = new TextView(getActivity());
            dataView.setPadding(20, 0, 0, 0);
            dataView.setText(data);
            group.addView(dataView);
        }

        private void populateView(View rootView) {
            ImageView imageView = (ImageView) rootView.findViewById(R.id.evidenceImage);
            ImageView videoPlayView = (ImageView) rootView.findViewById(R.id.videoPlayImage);
            DocumentUtils.DisplayImage(mDocument, imageView, videoPlayView);

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
                        case MEDIA_TYPE_UNKNOWN:
                            break;
                    }
                }
            });

            ((TextView)rootView.findViewById(R.id.text_evidence_title)).setText(mDocument.getTitle());

            ViewGroup metaContainer = (ViewGroup) rootView.findViewById(R.id.metadataContainer);
            metaContainer.removeAllViews();
            JSONObject jsonDocument = DocumentUtils.documentToJSON(mDocument);
            JSONArray metadata = null;
            try {
                metadata = jsonDocument.getJSONArray("metadata");
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
            Map<String, ViewGroup> groupMap = new HashMap<String, ViewGroup>();
            for (int i = 0; i < metadata.length(); ++i) {
                try {
                    JSONObject obj = metadata.getJSONObject(i);
                    String groupType = obj.getString("type");
                    ViewGroup group;

                    if (!groupMap.containsKey(groupType)) {
                        group = addGroup(groupType, metaContainer);
                        groupMap.put(groupType, group);
                    } else {
                        group = groupMap.get(groupType);
                    }
                    
                    String data = "";
                    if (obj.has("Person")) {
                        JSONObject person = obj.getJSONObject("Person");
                        data = person.getString("FamilyName") + ", " + person.getString("GivenName");
                    } else if (obj.has("Name")) {
                        data = obj.getString("Name");
                    }
                    addGroupData(data, group);
                } catch (JSONException e) {
                    e.printStackTrace();
                    continue;
                }
            }
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
