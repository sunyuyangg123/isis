package org.nakedobjects.application.value;

import org.nakedobjects.application.value.DateTime;


public class DateTimeTest extends ValueTestCase {
    private DateTime actual;

    protected void setUp() throws Exception {
        super.setUp();
        actual = new DateTime(2000, 2, 1, 10, 59, 30);
     }


    public void testGetDay() {
        assertEquals(1, actual.getDay());
    }

    public void testGetMonth() {
        assertEquals(2, actual.getMonth());
    }

    public void testGetYear() {
        assertEquals(2000, actual.getYear());
    }

    public void testGetMinute() {
        assertEquals(59, actual.getMinute());
    }
    
    public void testGetHour() {
        assertEquals(10, actual.getHour());
    }



    public void testSaveRestore() throws Exception {
    	DateTime timeStamp1 = new DateTime();
    	timeStamp1.parseUserEntry("2003-1-4 10:45");
    	assertFalse(timeStamp1.isEmpty());
    	
    	DateTime timeStamp2 = new DateTime();
    	timeStamp2.restoreFromEncodedString(timeStamp1.asEncodedString());
    	assertEquals(timeStamp1.longValue(), timeStamp2.longValue());
    	assertFalse(timeStamp2.isEmpty());
    }
    
    public void testSaveRestorOfNull() throws Exception {
    	DateTime timeStamp1 = new DateTime();
    	timeStamp1.clear();
    	assertTrue("DateTime isEmpty", timeStamp1.isEmpty());
    	
    	DateTime timeStamp2 = new DateTime();
    	timeStamp2.restoreFromEncodedString(timeStamp1.asEncodedString());
    	assertEquals(timeStamp1.longValue(), timeStamp2.longValue());
    	assertTrue(timeStamp2.isEmpty());
    }

    public void testNew() {
        DateTime expected = new DateTime(2003, 8, 17, 21, 30, 25);
        DateTime actual = new DateTime();
        assertEquals(expected, actual);
    }
    
    public void testNow() {
        DateTime expected = new DateTime(2003, 8, 17, 21, 30, 25);
        DateTime actual = new DateTime();
        actual.reset();
        assertEquals(expected, actual);
    }
}


/*
Naked Objects - a framework that exposes behaviourally complete
business objects directly to the user.
Copyright (C) 2000 - 2005  Naked Objects Group Ltd

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

The authors can be contacted via www.nakedobjects.org (the
registered address of Naked Objects Group is Kingsway House, 123 Goldworth
Road, Woking GU21 1NR, UK).
*/