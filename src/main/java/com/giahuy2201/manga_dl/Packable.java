package com.giahuy2201.manga_dl;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * All the methods Bundler needs to control bundling
 */
public interface Packable {

	/**
	 * Need a separate thread for ProgressBar ?
	 * @return
	 */
	boolean selfTracking();

	/**
	 * Add manga title, authors
	 * @throws IOException
	 */
	void addMetadata() throws IOException;

	/**
	 * Add images and chapters
	 * @param chapterList A list of images separated by chapters
	 * @throws IOException
	 */
	void addResources(List<List<File>> chapterList)  throws IOException;

	/**
	 * Save manga to a file
	 * @throws IOException
	 */
	void saveBook() throws IOException;
}
