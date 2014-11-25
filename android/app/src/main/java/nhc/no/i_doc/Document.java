package no.nhc.idoc;

/**
 *  Document is a more or less abstract definition that
 *  represents an instance of "evidence".
 *  The Document has a title e.g. for display in a list of documents.
 *  Other details about documents are handled by other classes.
 */
public class Document
{
    private String title;

    private class Metadata
    {
    }

    /**
     *  Retrieve Document title
     */
    public String getTitle()
    {
        return title;
    }

    /**
     *  Set or update title
     */
    public void setTitle(String title)
    {
        this.title = title;
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
}
