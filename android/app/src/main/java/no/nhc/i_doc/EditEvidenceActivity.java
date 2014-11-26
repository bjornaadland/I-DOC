package no.nhc.i_doc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

public class EditEvidenceActivity extends Activity {
    static final String TAG = "EditEvidenceActivity";

    private Document mDocument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_evidence);

        mDocument = getDocument();

        EditText t = (EditText) findViewById(R.id.editTitle);

        if (mDocument != null) {
            Log.d(TAG, "setting title to " + mDocument.getTitle());
            t.setText(mDocument.getTitle(), TextView.BufferType.EDITABLE);
        } else {
            Log.d(TAG, "no document");
            t.setText("[unknown evidence]", TextView.BufferType.NORMAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mDocument != null) {
            Log.d(TAG, "saving document...");
            EditText t = (EditText) findViewById(R.id.editTitle);
            mDocument.setTitle(t.getText().toString());

            DocumentDB.get(this).saveDocument(mDocument);
            Log.d(TAG, "... done saving");
        }
    }

    private Document getDocument() {
        return DocumentDB.get(this).getDocument(getIntent().getData());
    }
}
