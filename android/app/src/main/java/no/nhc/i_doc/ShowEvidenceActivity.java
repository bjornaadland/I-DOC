package no.nhc.i_doc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.ViewGroup;

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

    private Document getDocument() {
        return DocumentDB.get(this).getDocument(getIntent().getData());
    }
}
