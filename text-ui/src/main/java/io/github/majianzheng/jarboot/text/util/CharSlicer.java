/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package io.github.majianzheng.jarboot.text.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class CharSlicer {

  /** . */
  private final String value;

  /** . */
  private Pair<Integer, Integer> size;

  public CharSlicer(String value) {
    this.value = value;
    this.size = size();
  }

  public Pair<Integer, Integer> size() {
    if (size == null) {
      size = size(value);
    }
    return size;
  }

  private static Pair<Integer, Integer> size(String s) {
    int height = 1;
    int maxWidth = 0;
    int lastLineBegin = -1;
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) == '\n') {
        height++;
        if (i - lastLineBegin > maxWidth) {
          maxWidth = i - lastLineBegin - 1;
        }
        lastLineBegin = i;
      }
    }
    if (lastLineBegin < 0) {
      // the input string has no new line
      maxWidth = s.length();
    }
    return Pair.of(maxWidth, height);
  }

  public Pair<Integer, Integer>[] lines(final int width) {
    int count = getLineCount(width);
    Pair<Integer, Integer>[] lines = new Pair[count];
    Iterator<Pair<Integer, Integer>> linesIterator = linesIterator(width);
    for (int i = 0; i < lines.length; i++) {
      if (linesIterator.hasNext()) {
        lines[i] = linesIterator.next();
      }
    }
    return lines;
  }

  public int getLineCount(int width) {
    int index = 0;
    int count = 0;
    while (index < value.length()) {
      int pos = value.indexOf('\n', index);
      int nextIndex;
      if (pos == -1) {
        pos = Math.min(index + width, value.length());
        nextIndex = pos;
      } else {
        if (pos <= index + width) {
          nextIndex = pos + 1;
        } else {
          nextIndex = index + width;
        }
      }
      count ++;
      index = nextIndex;
    }
    return count;
  }

  public Iterator<Pair<Integer, Integer>> linesIterator(final int width) {
    if (width < 1) {
      throw new IllegalArgumentException("A non positive width=" + width + " cannot be accepted");
    }
    return new BaseIterator<Pair<Integer, Integer>>() {

      /** . */
      int index = 0;

      /** . */
      Pair<Integer, Integer> next = null;

      @Override
      public boolean hasNext() {
        if (next == null && index < value.length()) {
          int pos = value.indexOf('\n', index);
          int nextIndex;
          if (pos == -1) {
            pos = Math.min(index + width, value.length());
            nextIndex = pos;
          } else {
            if (pos <= index + width) {
              nextIndex = pos + 1;
            } else {
              nextIndex = pos = index + width;
            }
          }
          next = Pair.of(index, pos);
          index = nextIndex;
        }
        return next != null;
      }

      @Override
      public Pair<Integer, Integer> next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        Pair<Integer, Integer> next = this.next;
        this.next = null;
        return next;
      }
    };
  }
}
