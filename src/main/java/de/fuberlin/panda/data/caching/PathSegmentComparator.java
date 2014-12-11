package de.fuberlin.panda.data.caching;

/*
 * #%L
 * PANDA-DEEPLINKING
 * %%
 * Copyright (C) 2014 Freie Universitaet Berlin
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import java.util.Comparator;

/**
 * Custom comparator used in cache tree. Non negative number strings will be
 * ordered as their integer casted values
 * 
 * @author Christoph Schröder
 */
public class PathSegmentComparator implements Comparator<String> {

    @Override
    public int compare(String arg0, String arg1) {
        if (arg0.length() > arg1.length()) {
            return 1;
        } else if (arg0.length() < arg1.length()) {
            return -1;
        } else {
            return arg0.compareTo(arg1);
        }
    }

}