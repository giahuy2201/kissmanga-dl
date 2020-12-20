package com.giahuy2201.manga_dl;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface Packable {
	boolean selfTracking();
	void addMetadata() throws IOException;
	void addResources(List<List<File>> chapterList)  throws IOException;
	void saveBook() throws IOException;
}
