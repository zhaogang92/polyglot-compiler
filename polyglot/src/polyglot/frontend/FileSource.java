package jltools.frontend;

import java.io.*;
import java.util.*;
import jltools.util.*;

/** A <code>Source</code> represents a source file. */
public class FileSource extends Source
{
    File file;
    FileReader reader;

    public FileSource(String name) throws IOException {
        super(name);

	this.file = new File(name);

	if (! file.exists()) {
	    throw new FileNotFoundException(name);
	}

        path = file.getPath();
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
	return file.getAbsolutePath().hashCode();
    }

    /** Open the source file. */
    public Reader open() throws IOException {
	if (reader == null) {
	    reader = new FileReader(file);
	}

	return reader;
    }

    /** Close the source file. */
    public void close() throws IOException {
	if (reader != null) {
	    reader.close();
	    reader = null;
	}
    }

    public String toString() {
	return file.getName();
    }
}
