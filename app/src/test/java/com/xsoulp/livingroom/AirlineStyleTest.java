package com.xsoulp.livingroom;
import org.junit.Test;
import static org.junit.Assert.*;
public final class AirlineStyleTest {
    @Test public void easyJetOperatingCompaniesGetOrangeTails() {
        for(String id:new String[]{"EZY123"," EJU45AB ","ezs012"}) {
            AirlineStyle s=AirlineStyle.forCallsign(id);assertEquals("easyJet",s.name);
            assertTrue(((s.tail>>16)&255)>((s.tail)&255));
        }
    }
    @Test public void ryanairFamilyGetsNavyAndYellow() {
        for(String id:new String[]{"RYR123","RUK42","MAY123","RYS321"})assertEquals("Ryanair",AirlineStyle.forCallsign(id).name);
        assertNotEquals(AirlineStyle.forCallsign("RYR1").tail,AirlineStyle.forCallsign("RYR1").accent);
    }
    @Test public void vuelingAndTapHaveTheirOwnPalettes() {
        assertEquals("Vueling",AirlineStyle.forCallsign("VLG234").name);
        assertEquals("TAP",AirlineStyle.forCallsign("TAP473H").name);
        assertNotEquals(AirlineStyle.forCallsign("VLG1").tail,AirlineStyle.forCallsign("TAP1").tail);
    }
    @Test public void unknownOperatorIsNotInventedFromAircraftIdentity() {
        assertEquals("",AirlineStyle.forCallsign("4ca123").name);
        assertEquals("",AirlineStyle.forCallsign(null).name);
        assertEquals("",AirlineStyle.forCallsign("VLP123").name);
    }
}
