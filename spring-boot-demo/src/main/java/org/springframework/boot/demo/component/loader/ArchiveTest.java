package org.springframework.boot.demo.component.loader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.JarFileArchive;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

/**
 * @author Jie Zhao
 * @date 2023/1/5 23:05
 */
@Slf4j
public class ArchiveTest {

	private static File file = new File("spring-boot-demo/file/springboot-example-1.0-SNAPSHOT.jar");;
	private static Archive jarFileArchive;

	static {
		try {
			jarFileArchive = new JarFileArchive(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		// iterator();
		getUrl();
	}

	public static void iterator() throws IOException {

		Iterator<Archive.Entry> iterator = jarFileArchive.iterator();
		while (iterator.hasNext()) {
			Archive.Entry entry = iterator.next();
			log.info("资源名称：{}", entry.getName());
		}
	}

	public static void getUrl() throws MalformedURLException {
		URL url = jarFileArchive.getUrl();
		System.out.println(url.toString());
	}

}
