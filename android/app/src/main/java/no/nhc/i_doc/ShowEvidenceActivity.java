package no.nhc.i_doc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuItem;


public class ShowEvidenceActivity extends Activity {
    static final String TAG = "ShowEvidenceActivity";

    private Document mDocument;

    private ViewGroup addGroup(String header) {
        ViewGroup layoutView = new LinearLayout(this);
        TextView headerView = new TextView(this);
        headerView.setText(header);
        layoutView.addView(headerView);
        ViewGroup container = (ViewGroup) findViewById(R.id.metadataContainer);
        container.addView(layoutView);
        return layoutView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_evidence);

        mDocument = getDocument();
        if (mDocument == null) {
            Log.d(TAG, "no document");
            return;
        }

        TextView titleView = (TextView) findViewById(R.id.text_evidence_title);
        titleView.setText(mDocument.getTitle());

        ImageView imageView = (ImageView) findViewById(R.id.evidenceImage);
        imageView.setImageDrawable(null);

        ViewGroup group;
        group = addGroup("Victims");
        group = addGroup("Suspects");
        group = addGroup("Witnesses");
        group = addGroup("Context");
        group = addGroup("Incident");
        group = addGroup("Protected object");
        group = addGroup("Organizational unit");
        DocumentUtils.DisplayImage(mDocument, imageView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_evidence, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.show_evidence_delete) {
            Intent intent = new Intent(this, EditEvidenceActivity.class);
            intent.setData(mDocument.getUri());
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }



    private Document getDocument() {
        return DocumentDB.get(this).getDocument(getIntent().getData());
    }
}
