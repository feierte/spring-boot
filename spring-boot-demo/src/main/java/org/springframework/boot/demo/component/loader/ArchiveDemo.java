package org.springframework.boot.demo.component.loader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.JarFileArchive;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Jie Zhao
 * @date 2023/1/5 23:05
 */
@Slf4j
public class ArchiveDemo {

	public static void main(String[] args) throws IOException {
		File file = new File("build/libs/spring-boot-demo-2.3.6.BUILD-SNAPSHOT.jar");
		Archive jarFileArchive = new JarFileArchive(file);

		Iterator<Archive.Entry> iterator = jarFileArchive.iterator();
		while (iterator.hasNext()) {
			Archive.Entry entry = iterator.next();
			// log.info("资源名称：{}", entry.getName());
			System.out.println("资源名称: " + entry.getName());
		}
	}
}
