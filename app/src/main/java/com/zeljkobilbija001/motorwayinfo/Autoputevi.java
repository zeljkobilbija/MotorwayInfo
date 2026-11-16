package com.zeljkobilbija001.motorwayinfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Spisak autoputeva po zemljama (DE, FR, IT – JSON-i iz AutobahnInfo, AutoroutesInfo i AutostradeInfo; NL, BE, LU). Novi autoput: dodaj JSON u assets/autoputevi
 * (node alati/benelux_skripta.js NL-A2) i upiši ime datoteke umesto null ("autoputevi/nl-a2.json").
 * Za Belgiju se u nazivu rute navodi i evropska oznaka (E40), jer je na tablama češća od A-broja.
 */
public final class Autoputevi {

    private Autoputevi() { }

    public static final List<Autoput> SVE = Arrays.asList(
            new Autoput("GB", "M1", "London – Leicester – Sheffield – Leeds", "autoputevi/gb-m1.json"),
            new Autoput("GB", "M2", "Rochester – Faversham", "autoputevi/gb-m2.json"),
            new Autoput("GB", "M3", "London – Basingstoke – Southampton", "autoputevi/gb-m3.json"),
            new Autoput("GB", "M4", "London – Reading – Bristol – Cardiff – Swansea", "autoputevi/gb-m4.json"),
            new Autoput("GB", "M5", "Birmingham – Bristol – Exeter", "autoputevi/gb-m5.json"),
            new Autoput("GB", "M6", "Rugby – Birmingham – Manchester – Preston – Carlisle", "autoputevi/gb-m6.json"),
            new Autoput("GB", "M8", "Edinburgh – Glasgow – Greenock", "autoputevi/gb-m8.json"),
            new Autoput("GB", "M9", "Edinburgh – Stirling", "autoputevi/gb-m9.json"),
            new Autoput("GB", "M11", "London – Cambridge", "autoputevi/gb-m11.json"),
            new Autoput("GB", "M18", "Rotherham – Doncaster – Goole", "autoputevi/gb-m18.json"),
            new Autoput("GB", "M20", "London – Maidstone – Folkestone", "autoputevi/gb-m20.json"),
            new Autoput("GB", "M23", "London – Gatwick – Crawley", "autoputevi/gb-m23.json"),
            new Autoput("GB", "M25", "London Orbital", "autoputevi/gb-m25.json"),
            new Autoput("GB", "M27", "Southampton – Portsmouth", "autoputevi/gb-m27.json"),
            new Autoput("GB", "M40", "London – Oxford – Birmingham", "autoputevi/gb-m40.json"),
            new Autoput("GB", "M42", "Bromsgrove – Birmingham – Tamworth", "autoputevi/gb-m42.json"),
            new Autoput("GB", "M50", "Ross Spur", "autoputevi/gb-m50.json"),
            new Autoput("GB", "M53", "Wirral – Chester", "autoputevi/gb-m53.json"),
            new Autoput("GB", "M54", "Wolverhampton – Telford", "autoputevi/gb-m54.json"),
            new Autoput("GB", "M56", "Manchester – Chester", "autoputevi/gb-m56.json"),
            new Autoput("GB", "M60", "Manchester Outer Ring", "autoputevi/gb-m60.json"),
            new Autoput("GB", "M61", "Manchester – Preston", "autoputevi/gb-m61.json"),
            new Autoput("GB", "M62", "Liverpool – Manchester – Leeds – Hull", "autoputevi/gb-m62.json"),
            new Autoput("GB", "M65", "Preston – Blackburn – Burnley – Colne", "autoputevi/gb-m65.json"),
            new Autoput("GB", "M74", "Glasgow – Abington", "autoputevi/gb-m74.json"),
            new Autoput("GB", "M77", "Glasgow – Kilmarnock", "autoputevi/gb-m77.json"),
            new Autoput("GB", "M80", "Glasgow – Stirling", "autoputevi/gb-m80.json"),
            new Autoput("GB", "M90", "Edinburgh – Perth", "autoputevi/gb-m90.json"),
            new Autoput("GB", "A1(M)", "London – Peterborough; Doncaster – Newcastle", "autoputevi/gb-a1m.json"),
            new Autoput("GB", "A74(M)", "Abington – Gretna", "autoputevi/gb-a74m.json"),
            new Autoput("GB", "M1", "Northern Ireland: Belfast – Lisburn – Dungannon", "autoputevi/ni-m1.json"),
            new Autoput("GB", "M2", "Northern Ireland: Belfast – Antrim", "autoputevi/ni-m2.json")
    );

    /** Redosled zemalja na početnom ekranu. */
    public static final String[] ZEMLJE = {"GB"};

    /** Autoputevi jedne zemlje ("NL", "BE" ili "LU"), redom kao u spisku. */
    public static List<Autoput> zaZemlju(String zemlja) {
        List<Autoput> l = new ArrayList<>();
        for (Autoput a : SVE) if (a.zemlja.equals(zemlja)) l.add(a);
        return l;
    }

    public static Autoput poId(String id) {
        for (Autoput a : SVE) if (a.id.equals(id)) return a;
        return null;
    }
}
