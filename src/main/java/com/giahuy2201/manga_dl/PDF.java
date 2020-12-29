package com.giahuy2201.manga_dl;

import me.tongfei.progressbar.ProgressBar;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF bundler
 */
public class PDF implements Packable {

	private PDDocument book;
	private PDDocumentOutline bookmark;

	PDF() {
		this.book = new PDDocument();
		this.bookmark = new PDDocumentOutline();
		this.book.getDocumentCatalog().setDocumentOutline(bookmark);
		this.book.getDocumentCatalog().setPageMode(PageMode.USE_OUTLINES);
	}

	public boolean selfTracking() {
		return true;
	}

	@Override
	public void addMetadata() throws IOException {
		String title = MangaDL.extractor.getTitle();
		PDDocumentInformation metadata = new PDDocumentInformation();
		metadata.setTitle(title);
		metadata.setAuthor(MangaDL.extractor.getAuthors());
		metadata.setCreator("manga-dl");
		book.setDocumentInformation(metadata);
		// add cover
		addFrame(new File(MangaDL.extractor.getMangaDirectory(), MangaDL.extractor.getCover() + ".png"));
	}

	@Override
	public void addResources(List<List<File>> chapterList) throws IOException {
		List<File> PNGs = new ArrayList<>();
		for (int i = 0; i < chapterList.size(); i++) {
			PNGs.addAll(chapterList.get(i));
		}
		Iterable<File> PNGs2 = PNGs;
		if (!MangaDL.verbose) {
			PNGs2 = ProgressBar.wrap(PNGs, "Bundling");
		}
		for (File image : PNGs2) {
			MangaDL.logger.info("Adding frame " + image);
			addFrame(image);
		}
		for (int i = 0; i < chapterList.size(); i++) {
			String chapterName = MangaDL.extractor.getChaptersNames().get(i);
			MangaDL.logger.info("Adding chapter \"" + chapterName + "\"");
			PDPageDestination chapterCover = new PDPageFitWidthDestination();
			int chapterCoverNumber = 1; // skip cover page
			for (int j = 0; j < i; j++) {
				chapterCoverNumber += chapterList.get(j).size();
			}
			chapterCover.setPage(book.getPages().get((chapterCoverNumber)));
			PDOutlineItem chapterMark = new PDOutlineItem();
			chapterMark.setDestination(chapterCover);
			chapterMark.setTitle(chapterName);
			bookmark.addLast(chapterMark);
		}
	}

	@Override
	public void saveBook() throws IOException {
		book.save(MangaDL.extractor.getTitle() + ".pdf");
		book.close();
	}

	/**
	 * Add new page with image embedded to the book
	 * @param png png file of the image to be added
	 * @throws IOException
	 */
	private void addFrame(File png) throws IOException {
		PDImageXObject image = PDImageXObject.createFromFile(png.getAbsolutePath(), book);
		int OriginalWidth = image.getWidth();
		int width = 210;
		int height = Math.round(image.getHeight() * (float) width / OriginalWidth);
		PDRectangle frame = new PDRectangle(width, height);
		PDPage page = new PDPage(frame);
		this.book.addPage(page);
		PDPageContentStream stream = new PDPageContentStream(book, page);
		stream.drawImage(image, 0, 0, width, height);
		stream.close();
	}
}
