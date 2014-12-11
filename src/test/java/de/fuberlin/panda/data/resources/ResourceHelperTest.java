package de.fuberlin.panda.data.resources;

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


import static org.junit.Assert.*;

import org.junit.Test;

import de.fuberlin.panda.data.resources.ResourceHelper.TableArea;

public class ResourceHelperTest {

    @Test
    public void testConvertColNumToColRefA() {
        // first column
        String a = ResourceHelper.convertColNumToColRef(0);
        assertEquals("A", a);
    }

    @Test
    public void testConvertColNumToColRefZ() {
        // end of alphabet
        String z = ResourceHelper.convertColNumToColRef(25);
        assertEquals("Z", z);
    }

    @Test
    public void testConvertColNumToColRefAA() {
        // start of 2 letter reference
        String aa = ResourceHelper.convertColNumToColRef(26);
        assertEquals("AA", aa);
    }

    @Test
    public void testConvertColNumToColRefHBS() {
        // random reference
        String hbs = ResourceHelper.convertColNumToColRef(5478);
        assertEquals("HBS", hbs);
    }

    @Test
    public void testConvertColNumToColRefXFD() {
        // last excel (2007) column
        String xfd = ResourceHelper.convertColNumToColRef(16383);
        assertEquals("XFD", xfd);
    }

    /** ########################### **/

    @Test
    public void testConvertColRefToColNumA() {
        // first column
        Integer a = ResourceHelper.convertColRefToColNum("A");
        assertEquals(new Integer(0), a);
    }

    @Test
    public void testConvertColRefToColNumZ() {
        // first column
        Integer z = ResourceHelper.convertColRefToColNum("Z");
        assertEquals(new Integer(25), z);
    }

    @Test
    public void testConvertColRefToColNumAA() {
        // first column
        Integer aa = ResourceHelper.convertColRefToColNum("AA");
        assertEquals(new Integer(26), aa);
    }

    @Test
    public void testConvertColRefToColNumHBS() {
        // first column
        Integer hbs = ResourceHelper.convertColRefToColNum("HBS");
        assertEquals(new Integer(5478), hbs);
    }

    @Test
    public void testConvertColRefToColNumXFD() {
        // first column
        Integer xfd = ResourceHelper.convertColRefToColNum("XFD");
        assertEquals(new Integer(16383), xfd);
    }

    /** ########################### **/

    @Test
    public void testTableAreaEquals() {
        TableArea table = new TableArea(-1, -1, -1, -1);
        assertEquals(table, table);
        assertEquals(table, new TableArea(-1, -1, -1, -1));

    }

    @Test
    public void testTableAreaNotEquals() {
        TableArea table = new TableArea(-1, -1, -1, -1);
        assertNotEquals(table, null);
        assertNotEquals(table, new TableArea(1, -1, -1, -1));
        assertNotEquals(table, new TableArea(-1, 1, -1, -1));
        assertNotEquals(table, new TableArea(-1, -1, 1, -1));
        assertNotEquals(table, new TableArea(-1, -1, -1, 1));

    }

    /** ########################### **/

    @Test
    public void tesTevalTableReferenceAll() {
        TableArea table = ResourceHelper.evalTableReference("*");
        assertEquals(table, new TableArea(-1, -1, -1, -1));
    }

    @Test
    public void tesTevalTableReferenceRow() {
        TableArea table = ResourceHelper.evalTableReference("*4");
        assertEquals(table, new TableArea(3, 3, -1, -1));
    }

    @Test
    public void tesTevalTableReferenceColumn() {
        TableArea table = ResourceHelper.evalTableReference("D*");
        assertEquals(table, new TableArea(-1, -1, 3, 3));
    }

    @Test
    public void tesTevalTableReferenceCell() {
        TableArea table = ResourceHelper.evalTableReference("D4");
        assertEquals(table, new TableArea(3, 3, 3, 3));
    }

    @Test
    public void tesTevalTableReferenceArea() {
        TableArea table = ResourceHelper.evalTableReference("D4:G8");
        assertEquals(table, new TableArea(3, 7, 3, 6));
    }
}
