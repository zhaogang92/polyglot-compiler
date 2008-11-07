/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.frontend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Date;

/** A <code>Source</code> represents a source file. */
public class FileSource extends Source
{
    protected final File file;
    protected Reader reader;

    public FileSource(File file) throws IOException {
        this(file, false);
    }
        
    public FileSource(File file, boolean userSpecified) throws IOException {
        super(file.getName(), userSpecified);
        this.file = file;
    
        if (! file.exists()) {
            throw new FileNotFoundException(file.getName());
        }

        path = file.getCanonicalPath();
        lastModified = new Date(file.lastModified());
    }

    public boolean equals(Object o) {
	if (o instanceof FileSource) {
	    FileSource s = (FileSource) o;
	    return file.equals(s.file);
	}

	return false;
    }

    public int hashCode() {
	return file.getPath().hashCode();
    }

    /** Open the source file. */
    public Reader open() throws IOException {
	if (reader == null) {
	    FileInputStream str = new FileInputStream(file);
	    reader = createReader(str);
	}

	return reader;
    }

    /** This method defines the character encoding used by
        a file source. By default, it is ASCII with Unicode escapes,
	but it may be overridden. */
    protected Reader createReader(InputStream str) {
      try {
	  return new InputStreamReader(str, "US-ASCII");
      } catch (UnsupportedEncodingException e) { return null; }
    }

    /** Close the source file. */
    public void close() throws IOException {
	if (reader != null) {
	    reader.close();
	    reader = null;
	}
    }

    public String toString() {
	return file.getPath();
    }
}
