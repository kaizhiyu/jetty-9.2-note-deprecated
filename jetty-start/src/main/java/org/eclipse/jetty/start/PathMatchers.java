//
//  ========================================================================
//  Copyright (c) 1995-2017 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.start;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

/**
 * Common PathMatcher implementations.
 *
 *
 * 通用的路径匹配器
 */
public class PathMatchers {

    /**
     * 非隐藏文件的匹配器
     */
    private static class NonHiddenMatcher implements PathMatcher {
        @Override
        public boolean matches(Path path) {
            try {
                return !Files.isHidden(path);
            } catch (IOException e) {
                StartLog.debug(e);
                return false;
            }
        }
    }

    /**
     * glob模式的字符
     */
    private static final char GLOB_CHARS[] = "*?".toCharArray();

    /**
     * glob语法字符
     */
    private static final char SYNTAXED_GLOB_CHARS[] = "{}[]|:".toCharArray();

    /**
     * 空路径
     */
    private static final Path EMPTY_PATH = new File(".").toPath();

    /**
     * Convert a pattern to a Path object.
     *
     * 转换一个模式到一个路径对象
     * 
     * @param pattern
     *            the raw pattern (can contain "glob:" or "regex:" syntax indicator)
     * @return the Path version of the pattern provided.
     */
    private static Path asPath(final String pattern) {
        String test = pattern;
        if (test.startsWith("glob:")) {
            test = test.substring("glob:".length());
        } else if (test.startsWith("regex:")) {
            test = test.substring("regex:".length());
        }
        return new File(test).toPath();
    }

    /**
     * 根据模式来获取路径匹配器
     *
     * @param rawpattern
     * @return
     */
    public static PathMatcher getMatcher(final String rawpattern) {
        FileSystem fs = FileSystems.getDefault();
        
        String pattern = rawpattern;
        
        // Strip trailing slash (if present)
        // 移除末尾的斜线
        int lastchar = pattern.charAt(pattern.length() - 1);
        if (lastchar == '/' || lastchar == '\\') {
            pattern = pattern.substring(0,pattern.length() - 1);
        }

        // If using FileSystem.getPathMatcher() with "glob:" or "regex:"
        // use FileSystem default pattern behavior
        if (pattern.startsWith("glob:") || pattern.startsWith("regex:")) {
            StartLog.debug("Using Standard " + fs.getClass().getName() + " pattern: " + pattern);
            return fs.getPathMatcher(pattern);
        }

        // If the pattern starts with a root path then its assumed to
        // be a full system path
        if (isAbsolute(pattern)) {
            String pat = "glob:" + pattern;
            StartLog.debug("Using absolute path pattern: " + pat);
            return fs.getPathMatcher(pat);
        }

        // Doesn't start with filesystem root, then assume the pattern
        // is a relative file path pattern.
        String pat = "glob:**/" + pattern;
        StartLog.debug("Using relative path pattern: " + pat);
        return fs.getPathMatcher(pat);
    }

    /**
     * 非隐藏文件的提取器
     *
     * @return
     */
    public static PathMatcher getNonHidden() {
        return new NonHiddenMatcher();
    }

    /**
     * Provide the non-glob / non-regex prefix on the pattern as a Path reference.
     *
     * 获取一个非glob和非正则表达式的模式的引用
     *
     * @param pattern
     *            the pattern to test
     * @return the Path representing the search root for the pattern provided.
     */
    public static Path getSearchRoot(final String pattern) {
        StringBuilder root = new StringBuilder();

        int start = 0;
        boolean syntaxed = false;
        if (pattern.startsWith("glob:")) {
            start = "glob:".length();
            syntaxed = true;
        } else if (pattern.startsWith("regex:")) {
            start = "regex:".length();
            syntaxed = true;
        }
        int len = pattern.length();
        int lastSep = 0;
        for (int i = start; i < len; i++) {
            int cp = pattern.codePointAt(i);
            if (cp < 127) {
                char c = (char)cp;

                // unix path case
                if (c == '/') {
                    root.append(c);
                    lastSep = root.length();
                } else if (c == '\\') {
                    root.append("\\");
                    lastSep = root.length();

                    // possible escaped sequence.
                    // only really interested in windows escape sequences "\\"
                    int count = countChars(pattern,i+1,'\\');
                    if (count > 0) {
                        // skip extra slashes
                        i += count;
                    }
                } else {
                    if (isGlob(c,syntaxed)) {
                        break;
                    }
                    root.append(c);
                }
            } else {
                root.appendCodePoint(cp);
            }
        }

        String rootPath = root.substring(0,lastSep);
        if (rootPath.length() <= 0) {
            return EMPTY_PATH;
        }

        return asPath(rootPath);
    }

    /**
     * 获取某个字符的个数
     *
     * @param pattern
     * @param offset
     * @param c
     * @return
     */
    private static int countChars(String pattern, int offset, char c) {
        int count = 0;
        int len = pattern.length();
        for (int i = offset; i < len; i++) {
            if (pattern.charAt(i) == c) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * Tests if provided pattern is an absolute reference (or not)
     *
     * 是否是绝对路径
     *
     * @param pattern
     *            the pattern to test
     * @return true if pattern is an absolute reference.
     */
    public static boolean isAbsolute(final String pattern) {
        Path searchRoot = getSearchRoot(pattern);
        if (searchRoot == EMPTY_PATH) {
            return false;
        }
        return searchRoot.isAbsolute();
    }

    /**
     * Determine if part is a glob pattern.
     *
     * 是否是简化的正则表达式
     *
     * @param part
     *            the string to check
     * @param syntaxed
     *            true if overall pattern is syntaxed with <code>"glob:"</code> or <code>"regex:"</code>
     * @return true if part has glob characters
     */
    private static boolean isGlob(char c, boolean syntaxed) {
        for (char g : GLOB_CHARS) {
            if (c == g) {
                return true;
            }
        }
        if (syntaxed) {
            for (char g : SYNTAXED_GLOB_CHARS) {
                if (c == g) {
                    return true;
                }
            }
        }
        return false;
    }
}
