package no.nhc.i_doc;

import android.test.AndroidTestCase;

public class DocumentDBTest extends AndroidTestCase
{
    public void testDocumentListener() {
        DocumentDB db = DocumentDB.get(getContext());
        Document orig = db.createDocument();

        orig.setTitle("test");
        db.saveDocument(orig);

        // Make two documents that represent the original one
        Document d1 = db.getDocument(orig.getUri());
        Document d2 = db.getDocument(orig.getUri());

        assertNotNull(d1);
        assertNotNull(d2);
        assertEquals(d1.getUri(), d2.getUri());
    }
}
