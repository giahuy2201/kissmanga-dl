package com.giahuy2201.manga_dl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class KissmangaTest {
	static final String[] mangaUrls = {"https://kissmanga.org/manga/jk921826/", "https://kissmanga.org/manga/kimi_no_tsuku_uso_to_hontou", "https://kissmanga.org/manga/kxqh9261558062112", "https://kissmanga.org/manga/read_nisekoi_manga_online_for_free2"};
	static final String[] uncheckedUrls = {"https://kissmanga.org/manga/jk921826//", "http://kissmanga.org/manga//kimi_no_tsuku_uso_to_hontou", "https://kissmanga.org/manga/kxqh926155806211/2", "https://kissmanga.org/maxnga/read_nisekoi_manga_online_for_free2"};
	static final String[] titles = {"22|7 (Nanabun No Nijyuuni) +Α", "Kimi No Tsuku Uso To Hontou", "Attack On Titan", "Nisekoi"};
	static final String[] authors = {"Miyajima Reiji", "Ajimine Sakufu", "Isayama Hajime", "Komi Naoshi"};
	static final String[] chapterUrls = {"https://kissmanga.org/chapter/jk921826/chapter_1", "https://kissmanga.org/chapter/kimi_no_tsuku_uso_to_hontou/chapter_1", "https://kissmanga.org/chapter/kxqh9261558062112/chapter_1", "https://kissmanga.org/chapter/read_nisekoi_manga_online_for_free2/chapter_1"};
	static final String[] chapterNames = {"22|7 (Nanabun No Nijyuuni) +Α - Chapter 1: Takigawa Miu", "Vol.1 Chapter 1", "Vol.1 Chapter 1: To You, 2,000 Years From Now", "Chapter 1"};

	@Test
	public void testKE() throws Exception {
		Extractor kex = new Extractor(mangaUrls[0], new Kissmanga());
		Assertions.assertEquals(kex.getAuthors(), authors[0]);
	}

	@ParameterizedTest
	@MethodSource("urlTitles")
	public void testGetTitle(String url, String expected) throws Exception {
		Extractor kex = new Extractor(url, new Kissmanga());
		Assertions.assertEquals(expected, kex.getTitle());
	}

	private static Stream<Arguments> urlTitles() {
		Stream<Arguments> str = Stream.empty();
		for (int i = 0; i < mangaUrls.length; i++) {
			str = Stream.concat(str, Stream.of(Arguments.of(mangaUrls[i], titles[i])));
		}
		return str;
	}
}

