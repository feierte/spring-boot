/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader.archive;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.jar.Manifest;

import org.springframework.boot.loader.Launcher;

/**
 * An archive that can be launched by the {@link Launcher}.
 *
 * @author Phillip Webb
 * @since 1.0.0
 * @see JarFileArchive
 *
 * @apiNote Archive类 表示 SpringBoot 对归档文件的抽象，抽象为统一访问归档文件资源的逻辑层。
 * 一个 Archive 可以是 jar（JarFileArchive），可以是一个文件目录（ExplodedArchive）。
 *
 * <p>Archive 概念。
 * archive 即归档文件，这个概念在 linux 下比较常见，通常就是一个 tar/zip 格式的压缩包。
 * Java 中的 jar 文件就是一个 zip 格式的压缩文件。
 *
 * <p>该接口有两个实现，分别是 {@link ExplodedArchive} 和 {@link JarFileArchive}.
 * 前者用于在文件夹目录下寻找资源，后者用于在 jar 包环境中寻找资源。而在 SpringBoot 打包的 fat Jar 压缩文件中，则是使用后者。
 */
public interface Archive extends Iterable<Archive.Entry>, AutoCloseable {

	/**
	 * Returns a URL that can be used to load the archive.
	 * @return the archive URL
	 * @throws MalformedURLException if the URL is malformed
	 *
	 * @apiNote 返回表示该 Archive 的 URL 路径。
	 */
	URL getUrl() throws MalformedURLException;

	/**
	 * Returns the manifest of the archive.
	 * @return the manifest
	 * @throws IOException if the manifest cannot be read
	 */
	Manifest getManifest() throws IOException;

	/**
	 * Returns nested {@link Archive}s for entries that match the specified filters.
	 * @param searchFilter filter used to limit when additional sub-entry searching is
	 * required or {@code null} if all entries should be considered.
	 * @param includeFilter filter used to determine which entries should be included in
	 * the result or {@code null} if all entries should be included
	 * @return the nested archives
	 * @throws IOException on IO error
	 * @since 2.3.0
	 */
	default Iterator<Archive> getNestedArchives(EntryFilter searchFilter, EntryFilter includeFilter)
			throws IOException {
		EntryFilter combinedFilter = (entry) -> (searchFilter == null || searchFilter.matches(entry))
				&& (includeFilter == null || includeFilter.matches(entry));
		List<Archive> nestedArchives = getNestedArchives(combinedFilter);
		return nestedArchives.iterator();
	}

	/**
	 * Returns nested {@link Archive}s for entries that match the specified filter.
	 * @param filter the filter used to limit entries
	 * @return nested archives
	 * @throws IOException if nested archives cannot be read
	 * @deprecated since 2.3.0 in favor of
	 * {@link #getNestedArchives(EntryFilter, EntryFilter)}
	 */
	@Deprecated
	default List<Archive> getNestedArchives(EntryFilter filter) throws IOException {
		throw new IllegalStateException("Unexpected call to getNestedArchives(filter)");
	}

	/**
	 * Return a new iterator for the archive entries.
	 * @see java.lang.Iterable#iterator()
	 * @deprecated since 2.3.0 in favor of using
	 * {@link org.springframework.boot.loader.jar.JarFile} to access entries and
	 * {@link #getNestedArchives(EntryFilter, EntryFilter)} for accessing nested archives.
	 */
	@Deprecated
	@Override
	Iterator<Entry> iterator();

	/**
	 * Performs the given action for each element of the {@code Iterable} until all
	 * elements have been processed or the action throws an exception.
	 * @see Iterable#forEach
	 * @deprecated since 2.3.0 in favor of using
	 * {@link org.springframework.boot.loader.jar.JarFile} to access entries and
	 * {@link #getNestedArchives(EntryFilter, EntryFilter)} for accessing nested archives.
	 */
	@Deprecated
	@Override
	default void forEach(Consumer<? super Entry> action) {
		Objects.requireNonNull(action);
		for (Entry entry : this) {
			action.accept(entry);
		}
	}

	/**
	 * Creates a {@link Spliterator} over the elements described by this {@code Iterable}.
	 * @see Iterable#spliterator
	 * @deprecated since 2.3.0 in favor of using
	 * {@link org.springframework.boot.loader.jar.JarFile} to access entries and
	 * {@link #getNestedArchives(EntryFilter, EntryFilter)} for accessing nested archives.
	 */
	@Deprecated
	@Override
	default Spliterator<Entry> spliterator() {
		return Spliterators.spliteratorUnknownSize(iterator(), 0);
	}

	/**
	 * Return if the archive is exploded (already unpacked).
	 * @return if the archive is exploded
	 * @since 2.3.0
	 */
	default boolean isExploded() {
		return false;
	}

	/**
	 * Closes the {@code Archive}, releasing any open resources.
	 * @throws Exception if an error occurs during close processing
	 * @since 2.2.0
	 */
	@Override
	default void close() throws Exception {

	}

	/**
	 * Represents a single entry in the archive.
	 *
	 * @apiNote 表示归档文件（Archive，在 Java 应用环境中，即是指 JAR 文件）中的一个资源（包括文件和目录）。例如：目录、文件等其他实体。
	 *
	 * <p> Entry 有两个实现类：{@link org.springframework.boot.loader.archive.JarFileArchive.JarFileEntry}
	 * 和 {@link org.springframework.boot.loader.archive.ExplodedArchive.FileEntry}，
	 * JarFileEntry 实现类是基于 java.util.jar.JarEntry 实现的，表示 FAT JAR 内的嵌入资源。，所以说类似于 Java SE 中的 java.util.jar.JarEntry
	 * 而 FileEntry 是基于当前运行环境的文件系统实现的。所以这也从实现层面上证明了 JarLauncher 支持 JAR 和文件系统的两种启动方式。
	 */
	interface Entry {

		/**
		 * Returns {@code true} if the entry represents a directory.
		 * @return if the entry is a directory
		 */
		boolean isDirectory();

		/**
		 * Returns the name of the entry.
		 * @return the name of the entry
		 *
		 * @apiNote 获取归档文件中资源的相对路径。
		 * 例如 SpringBoot 应用打成的可执行 JAR 包，里面的资源路径有：META-INF/MANIFEST.MF 等资源路径。
		 */
		String getName();

	}

	/**
	 * Strategy interface to filter {@link Entry Entries}.
	 */
	@FunctionalInterface
	interface EntryFilter {

		/**
		 * Apply the jar entry filter.
		 * @param entry the entry to filter
		 * @return {@code true} if the filter matches
		 */
		boolean matches(Entry entry);

	}

}
