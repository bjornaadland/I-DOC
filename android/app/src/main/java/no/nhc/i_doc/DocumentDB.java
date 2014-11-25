package no.nhc.i_doc;

import java.util.List;
import java.util.ArrayList;

/**
 *  DocumentDB concerns the set of stored documents,
 *  and manages adding and removing of documents from the database.
 */
public class DocumentDB
{
    /**
     *  Get the full list of stored documents.
     */
    public List<Document> getDocumentList()
    {
        List<Document> l = new ArrayList<Document>();
        for (int i = 0; i < 3; ++i) {
            Document d = new Document();
            d.setTitle("" + ('a' + i));
            l.add(d);
        }
        return l;
    }

    /**
     *  Save a new or update an already existing Document
     */
    public void saveDocument(Document d)
    {
    }
}
