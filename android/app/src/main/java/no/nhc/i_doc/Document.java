package no.nhc.i_doc;

import java.util.List;
import java.util.ArrayList;


/**
 *  Document is a more or less abstract definition that
 *  represents an instance of "evidence".
 *  The Document has a title e.g. for display in a list of documents.
 *  Other details about documents are handled by other classes.
 */
public class Document
{
    private String mTitle;
    private List<String> mFiles;

    private class Metadata
    {
    }

    Document() {
        mFiles = new ArrayList<String>();
    }

    /**
     *  Retrieve Document title
     */
    public String getTitle()
    {
        return mTitle;
    }

    /**
     *  Set or update title
     */
    public void setTitle(String title)
    {
        mTitle = title;
    }

    /**
     *  Retrieve the metadata associated with the document
     */
    public Metadata getMetadata()
    {
        return null;
    }

    /**
     *  Set associated Metadata with the Document
     */
    public void setMetadata(Metadata data)
    {
    }

    /**
     * Add a file to this document.
     */
    public void addFile(String file) {
        mFiles.add(file);
    }

    /**
     * Get the files held by this document.
     */
    public List<String> getFiles() {
        return mFiles;
    }

}
